package net.ugona.plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            Intent i = new Intent(context, FetchService.class);
            context.startService(i);
        }
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (intent.getExtras() == null)
                return;
            NetworkInfo ni = (NetworkInfo) intent.getExtras().get(ConnectivityManager.EXTRA_NETWORK_INFO);
            if ((ni == null) || !ni.isConnected())
                return;
            Intent i = new Intent(context, FetchService.class);
            context.startService(i);
        }
    }

}
