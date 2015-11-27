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
        String car_id = getIntent().getStringExtra(Names.ID);
        args.putString(Names.ID, car_id);
        String[] data = getIntent().getStringExtra(Names.COMMAND).split("\\|");
        args.putInt(Names.COMMAND, Integer.parseInt(data[0]));
        boolean longTap = !data[2].equals("");
        CarConfig carConfig = CarConfig.get(this, car_id);
        if (carConfig.isInet_cmd())
            longTap = !longTap;
        args.putBoolean(Names.ROUTE, longTap);
        args.putBoolean(Names.NO_PROMPT, true);
        fragment.setArguments(args);
        fragment.show(getSupportFragmentManager(), "send");
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
