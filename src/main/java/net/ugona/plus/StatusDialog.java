package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class StatusDialog extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getIntent().getStringExtra(Names.TITLE))
                .setView(inflater.inflate(R.layout.status, null));

        String state = getIntent().getStringExtra(Names.STATE);
        if (state == null) {
            final double lat = getIntent().getDoubleExtra(Names.LATITUDE, 0);
            final double lon = getIntent().getDoubleExtra(Names.LONGITUDE, 0);
            final String car_id = getIntent().getStringExtra(Names.ID);
            builder.setPositiveButton(R.string.show_map, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent i = new Intent(StatusDialog.this, MapView.class);
                    String point_data = ";" + lat + ";" + lon + ";;" + lat + "," + lon;
                    i.putExtra(Names.POINT_DATA, point_data);
                    i.putExtra(Names.ID, car_id);
                    startActivity(i);
                }
            });
        } else {
            builder.setPositiveButton(R.string.ok, null);
        }

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        TextView tvSms = (TextView) dialog.findViewById(R.id.sms);
        tvSms.setText(getIntent().getStringExtra(Names.SMS_TEXT));

        String alarm = getIntent().getStringExtra(Names.ALARM);
        TextView tvAlarm = (TextView) dialog.findViewById(R.id.alarm);
        if (alarm == null) {
            tvAlarm.setVisibility(View.GONE);
        } else {
            tvAlarm.setVisibility(View.VISIBLE);
            tvAlarm.setText(alarm);
        }

        if (state != null) {
            TextView tvState = (TextView) dialog.findViewById(R.id.state);
            tvState.setText(state);
        }
    }
}
