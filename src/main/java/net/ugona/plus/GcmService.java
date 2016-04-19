package net.ugona.plus;

import android.content.Intent;
import android.os.Bundle;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;
import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmService extends GcmListenerService {

    @Override
    public void onMessageReceived(String from, Bundle data) {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        AppConfig config = AppConfig.get(this);
        String car_id = config.getId(data.getString(Names.ID));
        Intent i = new Intent(this, FetchService.class);
        i.putExtra(Names.ID, car_id);
        startService(i);
        if (data.getString("maintenance") != null) {
            i = new Intent(this, FetchService.class);
            i.setAction(FetchService.ACTION_MAINTENANCE);
            i.putExtra(Names.ID, car_id);
            i.putExtra(Names.MAINTENANCE, data.getString("maintenance"));
            startService(i);
        }
        if (data.getString("alarm") != null) {
            String alarm = data.getString("alarm");
            HttpTask httpTask = new HttpTask() {
                @Override
                void result(JsonObject res) throws ParseException {

                }

                @Override
                void error() {

                }
            };
            JsonObject params = new JsonObject();
            params.add("alarm", alarm);
            httpTask.execute("/alarm_ok", params);
        }
        String message = data.getString("message");
        if (message != null) {
            String title = data.getString("title");
            String url = data.getString("url");
            Notification.showMessage(this, title, message, url);
        }

    }
}
