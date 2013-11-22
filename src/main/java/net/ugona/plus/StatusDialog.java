package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class StatusDialog extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getIntent().getStringExtra(Names.TITLE))
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
        tvSms.setText(getIntent().getStringExtra(Names.SMS_TEXT));

        String alarm = getIntent().getStringExtra(Names.ALARM);
        TextView tvAlarm = (TextView) dialog.findViewById(R.id.alarm);
        if (alarm == null) {
            tvAlarm.setVisibility(View.GONE);
        } else {
            tvAlarm.setVisibility(View.VISIBLE);
            tvAlarm.setText(alarm);
        }

        TextView tvState = (TextView) dialog.findViewById(R.id.state);
        tvState.setText(getIntent().getStringExtra(Names.STATE));
    }
}
