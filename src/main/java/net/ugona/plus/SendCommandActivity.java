package net.ugona.plus;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class SendCommandActivity
        extends FragmentActivity
        implements DialogInterface.OnDismissListener {

    public static void retry_command(final Context context, String car_id, String data) {
        Intent intent = new Intent(context, SendCommandActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Names.ID, car_id);
        intent.putExtra(Names.COMMAND, data);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SendCommandFragment fragment = new SendCommandFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        fragment.show(getSupportFragmentManager(), "send");
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
