package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class StatusDialog extends Activity {

    static String[] alarms = {
            "Heavy shock",
            "Trunk",
            "Hood",
            "Doors",
            "Lock",
            "MovTilt sensor",
            "Rogue",
            "Ignition Lock"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String body = getIntent().getStringExtra(Names.SMS_TEXT);
        String[] parts = body.split(",");
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.status_title)
                .setView(inflater.inflate(R.layout.status, null))
                .setNegativeButton(R.string.ok, null)
                .create();
        dialog.show();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        TextView tvSms = (TextView) dialog.findViewById(R.id.sms);
        tvSms.setText(body);
        if (parts.length > 1) {
            String alarm = parts[1];
            if (!alarm.equals("Alarm NO")) {
                TextView tvAlarm = (TextView) dialog.findViewById(R.id.alarm);
                tvAlarm.setVisibility(View.VISIBLE);
                if (alarm.equals("Light shock")) {
                    tvAlarm.setText(R.string.light_shock);
                } else {
                    int i;
                    for (i = 0; i < alarms.length; i++) {
                        if (alarms[i].equals(alarm)) {
                            tvAlarm.setText(getString(R.string.alarms).split("\\|")[i]);
                            break;
                        }
                    }
                    if (i >= alarms.length)
                        tvAlarm.setText(alarm);
                }
            }
        }
        String state = "";
        if (parts[0].equals("Guard ON"))
            state = getString(R.string.guard_state) + "\n";
        for (int i = 2; i < parts.length; i++) {
            String part = parts[i];
            if (part.equals("GPS"))
                state += getString(R.string.gps_state) + "\n";
            if (part.equals("GPRS: None"))
                state += getString(R.string.gprs_none_state) + "\n";
            if (part.equals("GPRS: Home"))
                state += getString(R.string.gprs_home_state) + "\n";
            if (part.equals("GPRS: Roaming"))
                state += getString(R.string.gprs_roaming_state) + "\n";
        }
        TextView tvState = (TextView) dialog.findViewById(R.id.state);
        tvState.setText(state);
    }
}
