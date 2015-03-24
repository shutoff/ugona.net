package net.ugona.plus;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmService extends IntentService {

    public GcmService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);
        if ((extras != null) && GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            AppConfig config = AppConfig.get(this);
            String car_id = config.getId(extras.getString(Names.ID));
            Intent i = new Intent(this, FetchService.class);
            i.putExtra(Names.ID, car_id);
            startService(i);
/*
            if (extras.getString("maintenance") != null) {
                i = new Intent(this, FetchService.class);
                i.setAction(FetchService.ACTION_MAINTENANCE);
                i.putExtra(Names.ID, car_id);
                i.putExtra(Names.Car.MAINTENANCE, extras.getString("maintenance"));
                startService(i);
            }
*/
            String message = extras.getString("message");
            if (message != null) {
                String title = extras.getString("title");
                String url = extras.getString("url");
                Notification.showMessage(this, title, message, url);
            }
        }
        GcmReceiver.completeWakefulIntent(intent);
    }
}
