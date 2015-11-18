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

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FetchService extends Service {

    static final String ACTION_UPDATE = "net.ugona.plus.UPDATE";
    static final String ACTION_NOTIFICATION = "net.ugona.plus.NOTIFICATION";
    static final String ACTION_CLEAR = "net.ugona.plus.CLEAR_NOTIFICATION";
    static final String ACTION_MAINTENANCE = "net.ugona.plus.MAINTENANCE";
    static final String ACTION_ADD_TIMER = "net.ugona.plus.ADD_TIMER";
    static final String ACTION_COMMAND = "net.ugona.plus.COMMAND";

    private static final long REPEAT_AFTER_ERROR = 20 * 1000;
    private static final long REPEAT_AFTER_500 = 600 * 1000;
    private static final long REPEAT_COMMANDS = 40 * 1000;
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

    public static void updateMaintenance(Context context, String car_id, JsonObject res) {
        JsonArray data = res.get("data").asArray();
        long score = 550;
        CarConfig carConfig = CarConfig.get(context, car_id);
        Date now = new Date();
        int left_days = carConfig.getLeftDays();
        int left_mileage = carConfig.getLeftMileage();
        carConfig.setLeftDays(1000);
        carConfig.setLeftMileage(1000);
        for (int i = 0; i < data.size(); i++) {
            JsonObject v = data.get(i).asObject();
            JsonValue vPeriod = v.get("period");
            JsonValue vLast = v.get("last");
            if ((vPeriod != null) && (vLast != null)) {
                Date last = new Date(vLast.asLong() * 1000);
                Calendar cal = Calendar.getInstance();
                cal.setTime(last);
                cal.add(Calendar.MONTH, vPeriod.asInt());
                long days = (cal.getTime().getTime() - now.getTime()) / 86400000;
                if (days * 30 < score) {
                    score = days * 30;
                    carConfig.setMaintenance(v.get("name").asString());
                    carConfig.setLeftDays((int) days);
                    carConfig.setLeftMileage(1000);
                }
            }
            JsonValue vMileage = v.get("mileage");
            JsonValue vCurrent = v.get("current");
            if ((vMileage != null) && (vCurrent != null)) {
                double delta = vMileage.asLong() - vCurrent.asLong();
                if (delta < score) {
                    score = (long) delta;
                    boolean minus = false;
                    if (delta < 0) {
                        delta = -delta;
                        minus = true;
                    }
                    if (delta > 10) {
                        double k = Math.floor(Math.log10(delta));
                        if (k < 2)
                            k = 2;
                        k = Math.pow(10, k) / 2;
                        delta = Math.round(Math.round(delta / k) * k);
                    }
                    if (minus)
                        delta = -delta;
                    carConfig.setMaintenance(v.get("name").asString());
                    carConfig.setLeftDays(1000);
                    carConfig.setLeftMileage((int) delta);
                }
            }
        }
        carConfig.setMaintenance_time(now.getTime());
        if ((left_days != carConfig.getLeftDays()) || (left_mileage != carConfig.getLeftMileage())) {
            try {
                Intent intent = new Intent(ACTION_UPDATE);
                intent.putExtra(Names.ID, car_id);
                context.sendBroadcast(intent);
            } catch (Exception e) {
                // ignore
            }
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
        String action = null;
        if (intent != null) {
            String car_id = intent.getStringExtra(Names.ID);
            if (car_id != null) {
                new StatusRequest(car_id, intent.getExtras().containsKey(Names.CONNECT));
                Intent i = new Intent(Names.START_UPDATE);
                i.putExtra(Names.ID, car_id);
                sendBroadcast(i);
            }
            action = intent.getAction();
        }
        if (action != null) {
            if (action.equals(ACTION_COMMAND)) {
                String car_id = intent.getStringExtra(Names.ID);
                String data = intent.getStringExtra(Names.COMMAND);
                int notify = intent.getIntExtra(Names.NOTIFY_ID, 0);
                Notification.remove(this, notify);
                SendCommandFragment.retry_command(this, car_id, data);
            }
            if (action.equals(ACTION_ADD_TIMER)) {
                String car_id = intent.getStringExtra(Names.ID);
                String[] timer = intent.getStringExtra(Names.COMMAND).split("\\|");
                new SettingsRequest(car_id, timer[0], timer[1]);
            }
            if (action.equals(ACTION_NOTIFICATION)) {
                String car_id = intent.getStringExtra(Names.ID);
                String sound = intent.getStringExtra(Names.SOUND);
                String text = intent.getStringExtra(Names.MESSAGE);
                String title = intent.getStringExtra(Names.TITLE);
                int pictId = intent.getIntExtra(Names.PICTURE, 0);
                int max_id = intent.getIntExtra(Names.NOTIFY_ID, 0);
                long when = intent.getLongExtra(Names.WHEN, 0);
                boolean outgoing = intent.getBooleanExtra(Names.OUTGOING, false);
                String actions = intent.getStringExtra(Names.EXTRA);
                Notification.show(this, car_id, text, title, pictId, max_id, sound, when, outgoing, actions);
            }
            if (action.equals(ACTION_CLEAR)) {
                String car_id = intent.getStringExtra(Names.ID);
                int id = intent.getIntExtra(Names.NOTIFY_ID, 0);
                Notification.clear(this, car_id, id);
            }
            if (action.equals(ACTION_UPDATE) && (intent.getStringExtra(Names.ID) == null)) {
                AppConfig config = AppConfig.get(this);
                String[] ids = config.getIds().split(";");
                for (String id : ids) {
                    new StatusRequest(id, false);
                    Intent i = new Intent(Names.START_UPDATE);
                    i.putExtra(Names.ID, id);
                    sendBroadcast(i);
                }
            }
            if (action.equals(ACTION_MAINTENANCE))
                new MaintenanceRequest(intent.getStringExtra(Names.ID), intent.getStringExtra(Names.MAINTENANCE) != null);
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
        AppConfig config = AppConfig.get(this);
        String[] ids = config.getIds().split(";");
        for (String id : ids) {
            if (Commands.haveProcessed(this, id)) {
                Intent iUpd = new Intent(this, FetchService.class);
                iUpd.setAction(FetchService.ACTION_UPDATE);
                iUpd.putExtra(Names.ID, id);
                PendingIntent piUpd = PendingIntent.getService(this, 0, iUpd, 0);
                alarmMgr.set(AlarmManager.RTC, System.currentTimeMillis() + REPEAT_COMMANDS, piUpd);
            }
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

    static class MaintenanceParams implements Serializable {
        String skey;
        String lang;
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
            alarmMgr.set(AlarmManager.RTC, System.currentTimeMillis() + timeout, piTimer);
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
                Commands.check(FetchService.this, car_id);
                Notification.update(FetchService.this, car_id, upd);
            } else {
                sendUpdate(Names.NO_UPDATED, car_id);
            }
            JsonValue server_time = res.get("server_time");
            if (server_time != null) {
                Date now = new Date();
                long delta = server_time.asLong() - now.getTime();
                AppConfig appConfig = AppConfig.get(FetchService.this);
                appConfig.setTime_delta(delta);
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
            if (state.getTime() > 0) {
                params.time = state.getTime();
                if ((state.getCard() > 0) && (state.getGuard_time() > 0) && (state.getGuard_time() > state.getCard()))
                    params.time = state.getGuard_time();
            }
            if (connect)
                params.connect = 1;
            execute("/", params);
        }
    }

    class MaintenanceRequest extends ServerRequest {

        boolean notify_;

        MaintenanceRequest(String id, boolean notify) {
            super("M", id);
            notify_ = notify;
        }

        @Override
        void result(JsonObject res) throws ParseException {
            updateMaintenance(FetchService.this, car_id, res);
            if (!notify_)
                return;
            Notification.showMaintenance(FetchService.this, car_id);
        }

        @Override
        void exec(String api_key) {
            MaintenanceParams params = new MaintenanceParams();
            CarConfig carConfig = CarConfig.get(FetchService.this, car_id);
            params.skey = carConfig.getKey();
            params.lang = Locale.getDefault().getLanguage();
            execute("/maintenance", params);
        }
    }

    class SettingsRequest extends ServerRequest {

        String name;
        String set;

        SettingsRequest(String id, String name, String set) {
            super("T", id);
            this.name = name;
            this.set = set;
        }

        @Override
        void result(JsonObject res) throws ParseException {
            long delta = res.get(name).asInt() * 60000;
            long time = new Date().getTime() + delta;
            CarState carState = CarState.get(FetchService.this, car_id);
            JsonObject obj = new JsonObject();
            obj.add(name, time);
            Config.update(carState, obj);
            Intent iUpd = new Intent(FetchService.this, FetchService.class);
            iUpd.setAction(FetchService.ACTION_UPDATE);
            iUpd.putExtra(Names.ID, car_id);
            PendingIntent piUpd = PendingIntent.getService(FetchService.this, 0, iUpd, 0);
            alarmMgr.set(AlarmManager.RTC, System.currentTimeMillis() + delta, piUpd);
        }

        @Override
        void exec(String api_key) {
            MaintenanceParams params = new MaintenanceParams();
            CarConfig carConfig = CarConfig.get(FetchService.this, car_id);
            params.skey = carConfig.getKey();
            params.lang = Locale.getDefault().getLanguage();
            execute("/settings", params);
        }
    }

}
