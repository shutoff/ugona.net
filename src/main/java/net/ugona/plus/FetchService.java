package net.ugona.plus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FetchService extends Service {

    static final String ACTION_UPDATE = "net.ugona.plus.UPDATE";

    private static final long REPEAT_AFTER_ERROR = 20 * 1000;
    private static final long REPEAT_AFTER_500 = 600 * 1000;
    private static final long LONG_TIMEOUT = 5 * 60 * 60 * 1000;
    static private Map<String, ServerRequest> requests;
    private BroadcastReceiver mReceiver;
    private PendingIntent piTimer;
    private PendingIntent piUpdate;
    private ConnectivityManager conMgr;
    private AlarmManager alarmMgr;

    public static boolean isProcessed(String id) {
        if (requests == null)
            return false;
        synchronized (requests) {
            return requests.containsKey("S" + id);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent iTimer = new Intent(this, FetchService.class);
        piTimer = PendingIntent.getService(this, 0, iTimer, 0);
        mReceiver = new ScreenReceiver();
        requests = new HashMap<String, ServerRequest>();
        if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO)
            System.setProperty("http.keepAlive", "false");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String car_id = intent.getStringExtra(Names.ID);
            if (car_id != null) {
                new StatusRequest(car_id, intent.getExtras().containsKey(Names.CONNECT));
                Intent i = new Intent(Names.START_UPDATE);
                i.putExtra(Names.ID, car_id);
                sendBroadcast(i);
            }
        }
        if (startRequest())
            return START_STICKY;
        return START_NOT_STICKY;
    }

    synchronized boolean startRequest() {
        synchronized (requests) {
            for (Map.Entry<String, ServerRequest> entry : requests.entrySet()) {
                if (entry.getValue().started)
                    continue;
                entry.getValue().start();
            }
            if (requests.size() > 0)
                return true;
        }
        if (piUpdate == null) {
            Intent iUpdate = new Intent(this, FetchService.class);
            iUpdate.setAction(ACTION_UPDATE);
            piUpdate = PendingIntent.getService(this, 0, iUpdate, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmMgr.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + LONG_TIMEOUT, LONG_TIMEOUT, piUpdate);
        }
        return false;
    }

    void sendUpdate(String action, String car_id) {
        try {
            Intent intent = new Intent(action);
            intent.putExtra(Names.ID, car_id);
            sendBroadcast(intent);
        } catch (Exception e) {
            // ignore
        }
    }

    void sendError(String error, String car_id) {
        try {
            Intent intent = new Intent(Names.ERROR);
            intent.putExtra(Names.ERROR, error);
            intent.putExtra(Names.ID, car_id);
            sendBroadcast(intent);
        } catch (Exception e) {
            // ignore
        }
    }

    static class StatusParams implements Serializable {
        String skey;
        Long time;
        Integer connect;
    }

    abstract class ServerRequest extends HttpTask {

        final String key;
        final String car_id;
        boolean started;
        NetworkInfo m_activeNetwork;

        ServerRequest(String type, String id) {
            key = type + id;
            car_id = id;
            synchronized (requests) {
                if (requests.get(key) != null)
                    return;
                requests.put(key, this);
            }
        }

        @Override
        void result(JsonObject res) throws ParseException {
            synchronized (requests) {
                requests.remove(key);
            }
            startRequest();
        }

        @Override
        void error() {
            NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
            if ((activeNetwork == null) || !activeNetwork.isConnected()) {
                started = false;
                IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
                registerReceiver(mReceiver, filter);
                try {
                    sendBroadcast(new Intent("com.latedroid.juicedefender.action.ENABLE_APN")
                            .putExtra("tag", "ugona.net+")
                            .putExtra("reply", true));
                } catch (Exception ex) {
                    // ignore
                }
                return;
            }
            if ((m_activeNetwork.getType() != activeNetwork.getType()) || (m_activeNetwork.getSubtype() != activeNetwork.getSubtype())) {
                m_activeNetwork = activeNetwork;
                CarConfig config = CarConfig.get(FetchService.this, car_id);
                exec(config.getKey());
                return;
            }
            synchronized (requests) {
                requests.remove(key);
            }
            new StatusRequest(car_id, false);
            long timeout = (error_text != null) ? REPEAT_AFTER_500 : REPEAT_AFTER_ERROR;
            alarmMgr.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + timeout, timeout, piTimer);
            sendError(error_text, car_id);
        }

        void start() {
            if (started)
                return;
            CarConfig state = CarConfig.get(FetchService.this, car_id);
            if (state.getKey().equals("")) {
                synchronized (requests) {
                    requests.remove(key);
                }
                return;
            }
            m_activeNetwork = conMgr.getActiveNetworkInfo();
            if ((m_activeNetwork == null) || !m_activeNetwork.isConnected()) {
                IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
                registerReceiver(mReceiver, filter);
                try {
                    sendBroadcast(new Intent("com.latedroid.juicedefender.action.ENABLE_APN")
                            .putExtra("tag", "ugona.net+")
                            .putExtra("reply", true));
                } catch (Exception ex) {
                    // ignore
                }
                return;
            }
            try {
                unregisterReceiver(mReceiver);
            } catch (Exception e) {
                // ignore
            }
            alarmMgr.cancel(piTimer);
            started = true;
            exec(state.getKey());
        }

        abstract void exec(String api_key);
    }

    class StatusRequest extends ServerRequest {

        boolean connect;

        StatusRequest(String id, boolean refresh) {
            super("S", id);
            connect = refresh;
        }

        @Override
        void result(JsonObject res) throws ParseException {
            super.result(res);
            CarState state = CarState.get(FetchService.this, car_id);
            Set<String> upd = Config.update(state, res);
            if (upd != null) {
                sendUpdate(Names.UPDATED, car_id);
            } else {
                sendUpdate(Names.NO_UPDATED, car_id);
            }
        }

        @Override
        void error() {
            super.error();
            sendUpdate(Names.ERROR, car_id);
        }

        @Override
        void exec(String api_key) {
            StatusParams params = new StatusParams();
            params.skey = api_key;
            CarState state = CarState.get(FetchService.this, car_id);
            if (state.getTime() > 0)
                params.time = state.getTime();
            if (connect)
                params.connect = 1;
            execute("/", params);
        }
    }

}
