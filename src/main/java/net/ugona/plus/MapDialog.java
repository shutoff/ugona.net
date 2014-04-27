package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
                Intent i = new Intent(MapDialog.this, MapWebView.class);
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
