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
            String car_id = Preferences.getCar(preferences, extras.getString(Names.ID));
            State.appendLog("GCM message " + car_id);
            Intent i = new Intent(this, FetchService.class);
            i.putExtra(Names.ID, car_id);
            startService(i);
            String message = extras.getString("message");
            if (message != null)
                Alarm.createNotification(this, message, car_id);
        }
        GcmReceiver.completeWakefulIntent(intent);
    }
}
