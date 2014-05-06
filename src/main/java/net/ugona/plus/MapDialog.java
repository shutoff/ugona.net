package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.widget.TextView;

public class MapDialog extends Activity {
    static String fmt(double a) {
        String res = a + "";
        if (res.length() > 7)
            res = res.substring(0, 7);
        return res;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getIntent().getStringExtra(Names.TITLE))
                .setView(inflater.inflate(R.layout.status, null));

        final String car_id = getIntent().getStringExtra(Names.ID);

        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.map, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MapDialog.this);
                Intent i = new Intent(MapDialog.this, MapEventActivity.class);
                double lat = preferences.getFloat(Names.Car.LAT + car_id, 0);
                double lng = preferences.getFloat(Names.Car.LNG + car_id, 0);
                String data = lat + ";" + lng + ";-1;";
                data += fmt(lat) + " " + fmt(lng) + ",";
                String address = Address.getAddress(MapDialog.this, lat, lng);
                if (address != null)
                    data += address;
                i.putExtra(Names.POINT_DATA, data);
                i.putExtra(Names.ID, car_id);
                startActivity(i);
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });

        final double lat = getIntent().getDoubleExtra(Names.Car.LAT, 0);
        final double lng = getIntent().getDoubleExtra(Names.Car.LNG, 0);

        TextView tvState = (TextView) dialog.findViewById(R.id.state);
        tvState.setText(fmt(lat) + "," + fmt(lng));

        Address request = new Address() {
            @Override
            void result(String res) {
                TextView tvAddr = (TextView) dialog.findViewById(R.id.sms);
                tvAddr.setText(res);
            }
        };
        request.get(this, lat, lng);
    }

}
