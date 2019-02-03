package net.ugona.plus;

import android.content.Intent;
import android.os.Bundle;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class GcmService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Map data = message.getData();
        AppConfig config = AppConfig.get(this);
        String car_id = config.getId((String)data.get(Names.ID));
        Intent i = new Intent(this, FetchService.class);
        i.putExtra(Names.ID, car_id);
        startService(i);
        if (data.get("maintenance") != null) {
            i = new Intent(this, FetchService.class);
            i.setAction(FetchService.ACTION_MAINTENANCE);
            i.putExtra(Names.ID, car_id);
            i.putExtra(Names.MAINTENANCE, (String)data.get("maintenance"));
            startService(i);
        }
        if (data.get("alarm") != null) {
            String alarm = (String)data.get("alarm");
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
        String msg = (String)data.get("message");
        if (msg != null) {
            String title = (String)data.get("title");
            String url = (String)data.get("url");
            Notification.showMessage(this, title, msg, url);
        }

    }
}
