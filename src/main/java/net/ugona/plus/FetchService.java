package net.ugona.plus;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FetchService extends Service {

    static final String ACTION_UPDATE = "net.ugona.plus.UPDATE";
    static final String ACTION_NOUPDATE = "net.ugona.plus.NO_UPDATE";
    static final String ACTION_ERROR = "net.ugona.plus.ERROR";
    static final String ACTION_START = "net.ugona.plus.START";
    static final String ACTION_UPDATE_FORCE = "net.ugona.plus.UPDATE_FORCE";
    static final String ACTION_CLEAR = "net.ugona.plus.CLEAR";
    static final String ACTION_NOTIFICATION = "net.ugona.plus.NOTIFICATION";

    static final String URL_STATUS = "https://car-online.ugona.net/?skey=$1&time=$2";
    static final String URL_EVENTS = "https://car-online.ugona.net/events?skey=$1&auth=$2&begin=$3&end=$4";

    private static final long REPEAT_AFTER_ERROR = 20 * 1000;
    private static final long REPEAT_AFTER_500 = 600 * 1000;
    private static final long LONG_TIMEOUT = 5 * 60 * 60 * 1000;
    private static final long SCAN_TIMEOUT = 10 * 1000;
    static private Map<String, ServerRequest> requests;
    private BroadcastReceiver mReceiver;
    private PendingIntent piTimer;
    private PendingIntent piUpdate;
    private SharedPreferences preferences;
    private ConnectivityManager conMgr;
    private AlarmManager alarmMgr;

    static boolean isProcessed(String id) {
        if (requests == null)
            return false;
        return requests.containsKey("S" + id);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

/*
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                State.print(ex);
            }
        });
*/

        super.onCreate();
        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
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
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(ACTION_CLEAR))
                    clearNotification(car_id, intent.getIntExtra(Names.NOTIFY, 0));
                if (action.equals(ACTION_UPDATE)) {
                    Cars.Car[] cars = Cars.getCars(this);
                    for (Cars.Car car : cars) {
                        new StatusRequest(Preferences.getCar(preferences, car.id));
                        for (String p : car.pointers) {
                            new StatusRequest(p);
                        }
                    }
                }
                if (action.equals(ACTION_NOTIFICATION)) {
                    String sound = intent.getStringExtra(Names.NOTIFY);
                    String text = intent.getStringExtra(Names.TITLE);
                    int pictId = intent.getIntExtra(Names.ALARM, 0);
                    int max_id = intent.getIntExtra(Names.EVENT_ID, 0);
                    showNotification(car_id, text, pictId, max_id, sound);
                }
            }
            if (car_id != null)
                new StatusRequest(Preferences.getCar(preferences, car_id));
        }
        if (startRequest())
            return START_STICKY;
        return START_NOT_STICKY;
    }

    void showNotification(String car_id, String text, int pictId, int max_id, String sound) {
        String title = getString(R.string.app_name);
        String[] cars = preferences.getString(Names.CARS, "").split(",");
        if (cars.length > 1) {
            title = preferences.getString(Names.CAR_NAME + car_id, "");
            if (title.length() == 0) {
                title = getString(R.string.car);
                if (car_id.length() > 0)
                    title += " " + car_id;
            }
        }

        int defs = Notification.DEFAULT_LIGHTS + Notification.DEFAULT_VIBRATE;
        if (sound == null)
            defs |= Notification.DEFAULT_SOUND;
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setDefaults(defs)
                        .setSmallIcon(pictId)
                        .setContentTitle(title)
                        .setContentText(text);
        if (sound != null)
            builder.setSound(Uri.parse("android.resource://net.ugona.plus/raw/" + sound));

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra(Names.ID, car_id);
        Uri data = Uri.withAppendedPath(Uri.parse("http://notification/id/"), car_id);
        notificationIntent.setData(data);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        Intent clearIntent = new Intent(this, FetchService.class);
        clearIntent.setAction(FetchService.ACTION_CLEAR);
        clearIntent.putExtra(Names.ID, car_id);
        clearIntent.putExtra(Names.NOTIFY, max_id);
        data = Uri.withAppendedPath(Uri.parse("http://notification_clear/id/"), max_id + "");
        clearIntent.setData(data);
        PendingIntent deleteIntent = PendingIntent.getService(this, 0, clearIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setDeleteIntent(deleteIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = builder.build();
        if (pictId == R.drawable.white_valet_on)
            notification.flags = Notification.FLAG_ONGOING_EVENT;
        manager.notify(max_id, notification);
    }

    boolean startRequest() {
        for (Map.Entry<String, ServerRequest> entry : requests.entrySet()) {
            if (entry.getValue().started)
                return true;
        }
        for (Map.Entry<String, ServerRequest> entry : requests.entrySet()) {
            entry.getValue().start();
            return true;
        }
        if (piUpdate == null) {
            Intent iUpdate = new Intent(this, FetchService.class);
            iUpdate.setAction(ACTION_UPDATE);
            piUpdate = PendingIntent.getService(this, 0, iUpdate, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmMgr.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + LONG_TIMEOUT, LONG_TIMEOUT, piUpdate);
        }
        stopSelf();
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
            Intent intent = new Intent(ACTION_ERROR);
            intent.putExtra(Names.ERROR, error);
            intent.putExtra(Names.ID, car_id);
            sendBroadcast(intent);
        } catch (Exception e) {
            // ignore
        }
    }

    void clearNotification(String car_id, int id) {
        String[] ids = preferences.getString(Names.N_IDS + car_id, "").split(",");
        String res = null;
        for (String n_id : ids) {
            if (n_id.equals(id))
                continue;
            if (res == null) {
                res = n_id;
                continue;
            }
            res += ",";
            res += n_id;
        }
        SharedPreferences.Editor ed = preferences.edit();
        if (id == preferences.getInt(Names.BALANCE_NOTIFICATION + car_id, 0))
            ed.remove(Names.BALANCE_NOTIFICATION + car_id);
        if (id == preferences.getInt(Names.GUARD_NOTIFY + car_id, 0))
            ed.remove(Names.GUARD_NOTIFY + car_id);
        if (id == preferences.getInt(Names.MOTOR_ON_NOTIFY + car_id, 0))
            ed.remove(Names.MOTOR_ON_NOTIFY + car_id);
        if (id == preferences.getInt(Names.MOTOR_OFF_NOTIFY + car_id, 0))
            ed.remove(Names.MOTOR_OFF_NOTIFY + car_id);
        if (id == preferences.getInt(Names.VALET_ON_NOTIFY + car_id, 0))
            ed.remove(Names.VALET_ON_NOTIFY + car_id);
        if (id == preferences.getInt(Names.VALET_OFF_NOTIFY + car_id, 0))
            ed.remove(Names.VALET_OFF_NOTIFY + car_id);
        if (id == preferences.getInt(Names.ZONE_NOTIFY + car_id, 0))
            ed.remove(Names.ZONE_NOTIFY + car_id);
        if (res == null) {
            ed.remove(Names.N_IDS + car_id);
        } else {
            ed.putString(Names.N_IDS + car_id, res);
        }
        ed.commit();
    }

    abstract class ServerRequest extends HttpTask {

        final String key;
        final String car_id;
        boolean started;
        NetworkInfo m_activeNetwork;

        ServerRequest(String type, String id) {
            key = type + id;
            car_id = id;
            if (requests.get(key) != null)
                return;
            requests.put(key, this);
        }

        @Override
        void result(JsonObject res) throws ParseException {
            requests.remove(key);
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
                exec(preferences.getString(Names.CAR_KEY + car_id, ""));
                return;
            }
            requests.remove(key);
            new StatusRequest(car_id);
            long timeout = (error_text != null) ? REPEAT_AFTER_500 : REPEAT_AFTER_ERROR;
            alarmMgr.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + timeout, timeout, piTimer);
            sendError(error_text, car_id);
        }

        void start() {
            if (started)
                return;
            String api_key = preferences.getString(Names.CAR_KEY + car_id, "");
            if (api_key.length() == 0) {
                requests.remove(key);
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
            exec(api_key);
        }

        abstract void exec(String api_key);
    }

    class StatusRequest extends ServerRequest {

        SharedPreferences.Editor ed;
        int msg_id;

        StatusRequest(String id) {
            super("S", id);
        }

        @Override
        void background(JsonObject res) throws ParseException {
            JsonValue error = res.get("error");
            if (error != null) {
                sendError(error.asString(), car_id);
                return;
            }

            JsonValue time = res.get("time");
            if (time == null) {
                if (SmsMonitor.haveProcessed(car_id)) {
                    Intent iUpdate = new Intent(FetchService.this, FetchService.class);
                    iUpdate.setAction(ACTION_UPDATE);
                    iUpdate.putExtra(Names.ID, car_id);
                    Uri data = Uri.withAppendedPath(Uri.parse("http://service/update/"), car_id);
                    iUpdate.setData(data);
                    PendingIntent pi = PendingIntent.getService(FetchService.this, 0, iUpdate, 0);
                    alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + SCAN_TIMEOUT, pi);
                }
                sendUpdate(ACTION_NOUPDATE, car_id);
                return;
            }
            ed = preferences.edit();
            ed.putLong(Names.EVENT_TIME + car_id, time.asLong());
            JsonValue contact_value = res.get("contact");
            boolean gps_valid = false;
            boolean prev_valet = false;
            int prev_guard_mode = 0;
            if (contact_value != null) {
                JsonObject contact = contact_value.asObject();
                boolean guard = contact.get("guard").asBoolean();
                prev_guard_mode = preferences.getBoolean(Names.GUARD + car_id, false) ? 0 : 1;
                prev_valet = preferences.getBoolean(Names.GUARD0 + car_id, false) && !preferences.getBoolean(Names.GUARD1 + car_id, false);
                if (guard && preferences.getBoolean(Names.GUARD0 + car_id, false) && preferences.getBoolean(Names.GUARD1 + car_id, false))
                    prev_guard_mode = 2;

                if (contact.get("gsm").asBoolean()) {
                    JsonValue gsm_value = res.get("gsm");
                    if (gsm_value != null)
                        ed.putInt(Names.GSM_DB + car_id, gsm_value.asObject().get("db").asInt());
                } else {
                    ed.putInt(Names.GSM_DB + car_id, 0);
                }

                ed.putBoolean(Names.GUARD + car_id, guard);
                setState(Names.INPUT1, contact, "input1", 3);
                setState(Names.INPUT2, contact, "input2", 1);
                ed.putBoolean(Names.INPUT3 + car_id, contact.get("input3").asBoolean());
                setState(Names.INPUT4, contact, "input4", 2);
                ed.putBoolean(Names.GUARD0 + car_id, contact.get("guardMode0").asBoolean());
                ed.putBoolean(Names.GUARD1 + car_id, contact.get("guardMode1").asBoolean());
                ed.putBoolean(Names.RELAY1 + car_id, contact.get("relay1").asBoolean());
                ed.putBoolean(Names.RELAY2 + car_id, contact.get("relay2").asBoolean());
                ed.putBoolean(Names.RELAY3 + car_id, contact.get("relay3").asBoolean());
                ed.putBoolean(Names.RELAY4 + car_id, contact.get("relay4").asBoolean());
                ed.putBoolean(Names.ENGINE + car_id, contact.get("engine").asBoolean());
                ed.putBoolean(Names.RESERVE_NORMAL + car_id, contact.get("reservePowerNormal").asBoolean());

                setState(Names.ZONE_DOOR, contact, "door", 3);
                setState(Names.ZONE_HOOD, contact, "hood", 2);
                setState(Names.ZONE_TRUNK, contact, "trunk", 1);
                setState(Names.ZONE_ACCESSORY, contact, "accessory", 7);
                setState(Names.ZONE_IGNITION, contact, "realIgnition", 4);

                long valet_time = preferences.getLong(Names.VALET_TIME + car_id, 0);
                if (valet_time > 0) {
                    if (valet_time + 30000 < new Date().getTime()) {
                        valet_time = 0;
                        ed.remove(Names.VALET_TIME + car_id);
                    }
                }
                long init_time = preferences.getLong(Names.INIT_TIME + car_id, 0);
                if (init_time > 0) {
                    if (init_time + 30000 < new Date().getTime()) {
                        init_time = 0;
                        ed.remove(Names.INIT_TIME + car_id);
                    }
                }
                if (valet_time > 0) {
                    guard = false;
                    ed.putBoolean(Names.GUARD + car_id, guard);
                    ed.putBoolean(Names.GUARD0 + car_id, true);
                    ed.putBoolean(Names.GUARD1 + car_id, false);
                } else if (init_time > 0) {
                    ed.putBoolean(Names.GUARD0 + car_id, false);
                    ed.putBoolean(Names.GUARD1 + car_id, false);
                }
                gps_valid = contact.get("gpsValid").asBoolean();
            }

            JsonValue voltage_value = res.get("voltage");
            if (voltage_value != null) {
                JsonObject voltage = voltage_value.asObject();
                ed.putString(Names.VOLTAGE_MAIN + car_id, voltage.get("main").asDouble() + "");
                ed.putString(Names.VOLTAGE_RESERVED + car_id, voltage.get("reserved").asDouble() + "");
            }

            JsonValue gps_value = res.get("gps");
            if (gps_value != null) {
                JsonObject gps = gps_value.asObject();
                ed.putFloat(Names.LAT + car_id, gps.get("latitude").asFloat());
                ed.putFloat(Names.LNG + car_id, gps.get("longitude").asFloat());
                ed.putFloat(Names.SPEED + car_id, gps.get("speed").asFloat());
                if (gps_valid)
                    ed.putInt(Names.COURSE + car_id, gps.get("course").asInt());
            } else {
                JsonValue gsm_value = res.get("gsm");
                if (gsm_value != null) {
                    JsonObject gsm = gsm_value.asObject();
                    String gsm_str = gsm.get("cc").asInt() + " ";
                    gsm_str += gsm.get("nc").asInt() + " ";
                    gsm_str += gsm.get("lac").asInt() + " ";
                    gsm_str += gsm.get("cid").asInt();
                    if (!preferences.getString(Names.GSM_SECTOR + car_id, "").equals(gsm_str)) {
                        ed.putString(Names.GSM_SECTOR + car_id, gsm_str);
                        ed.remove(Names.GSM_ZONE + car_id);
                        ed.remove(Names.LAT + car_id);
                        ed.remove(Names.LNG + car_id);
                    }
                }
            }

            JsonValue temp_value = res.get("temperature");
            if (temp_value != null) {
                Map<Integer, Integer> values = new HashMap<Integer, Integer>();
                JsonObject temp = temp_value.asObject();
                List<String> names = temp.names();
                for (String name : names) {
                    try {
                        values.put(Integer.parseInt(name), temp.get(name).asInt());
                    } catch (Exception ex) {
                        // ignore
                    }
                }
                ArrayList<Integer> sensors = new ArrayList<Integer>(values.keySet());
                Collections.sort(sensors);
                String val = null;
                for (int i : sensors) {
                    String p = i + ":" + values.get(i);
                    if (val == null) {
                        val = p;
                        continue;
                    }
                    val += ";" + p;
                }
                ed.putString(Names.TEMPERATURE + car_id, val);
            }

            JsonValue balance_value = null;
            if (preferences.getLong(Names.BALANCE_TIME + car_id, 0) + 60000 < new Date().getTime()) {
                balance_value = res.get("balance");
                if (balance_value != null) {
                    JsonObject balance = balance_value.asObject();
                    double value = balance.get("value").asDouble();
                    if (preferences.getString(Names.BALANCE + car_id, "").equals(value + "")) {
                        balance_value = null;
                    } else {
                        ed.putString(Names.BALANCE + car_id, value + "");
                        ed.remove(Names.BALANCE_TIME + car_id);
                    }
                }
            }

            JsonValue zone = res.get("zone");
            if (zone != null) {
                ed.putLong(Names.ZONE_TIME + car_id, zone.asLong());
                ed.putBoolean(Names.ZONE_IN + car_id, res.get("zone_in").asBoolean());
                State.appendLog("Zone: " + zone.asLong() + "," + res.get("zone_in").asBoolean());
                new ZoneRequest(car_id);
            }

            JsonValue last_stand = res.get("last_stand");
            if (last_stand != null)
                ed.putLong(Names.LAST_STAND + car_id, last_stand.asLong());
            JsonValue az = res.get("az");
            if (az != null)
                ed.putBoolean(Names.AZ + car_id, az.asBoolean());
            JsonValue azStart = res.get("az_start");
            if (azStart != null)
                ed.putLong(Names.AZ_START + car_id, azStart.asLong());
            JsonValue azStop = res.get("az_stop");
            if (azStop != null)
                ed.putLong(Names.AZ_STOP + car_id, azStop.asLong());
            if (preferences.getBoolean(Names.AZ + car_id, false)) {
                long az_start = preferences.getLong(Names.AZ_START + car_id, 0);
                if (time.asLong() - az_start > 130000)
                    ed.putBoolean(Names.AZ, false);
            }
            JsonValue timer = res.get("timer");
            if (timer != null) {
                int timerValue = timer.asInt();
                if ((timerValue >= 1) && (timerValue <= 30))
                    ed.putInt(Names.CAR_TIMER + car_id, timerValue);
            }
            JsonValue settings = res.get("settings");
            if (settings != null)
                ed.remove(Names.SETTINGS_TIME + car_id);

            ed.commit();

            if (preferences.getLong(Names.RELE_START + car_id, 0) != 0) {
                boolean ignition = preferences.getBoolean(Names.ZONE_IGNITION + car_id, false);
                if (preferences.getBoolean(Names.INPUT3 + car_id, false))
                    ignition = true;
                if ((az != null) && az.asBoolean())
                    ignition = true;
                if (ignition) {
                    ed.remove(Names.RELE_START + car_id);
                    ed.commit();
                }
            }
            if (preferences.getBoolean(Names.AZ + car_id, false)) {
                long start = preferences.getLong(Names.AZ_START + car_id, 0);
                if (start < time.asLong() - 30 * 60 * 1000) {
                    ed.putBoolean(Names.AZ + car_id, false);
                    ed.commit();
                }
            }

            sendUpdate(ACTION_UPDATE, car_id);

            if (contact_value != null) {
                int guard_mode = preferences.getBoolean(Names.GUARD + car_id, false) ? 0 : 1;
                if ((guard_mode == 0) && preferences.getBoolean(Names.GUARD0 + car_id, false) && preferences.getBoolean(Names.GUARD1 + car_id, false))
                    guard_mode = 2;
                if (guard_mode != prev_guard_mode) {
                    int notify_id = 0;
                    try {
                        notify_id = preferences.getInt(Names.GUARD_NOTIFY + car_id, 0);
                    } catch (Exception ex) {
                        ed.remove(Names.GUARD_NOTIFY + car_id);
                        ed.commit();
                    }
                    if (notify_id != 0) {
                        Alarm.removeNotification(FetchService.this, car_id, notify_id);
                        ed.remove(Names.GUARD_NOTIFY + car_id);
                        ed.commit();
                    }
                    String guard_pref = preferences.getString(Names.GUARD_MODE + car_id, "");
                    boolean notify = false;
                    if (guard_pref.equals("")) {
                        notify = (guard_mode == 2);
                    } else if (guard_pref.equals("all")) {
                        notify = true;
                    }
                    if (notify) {
                        int id = R.string.ps_guard;
                        String sound = "warning";
                        switch (guard_mode) {
                            case 0:
                                id = R.string.guard_on;
                                sound = "guard_on";
                                break;
                            case 1:
                                id = R.string.guard_off;
                                sound = "guard_off";
                                break;
                        }
                        notify_id = Alarm.createNotification(FetchService.this, getString(id), R.drawable.warning, car_id, sound);
                        ed.putInt(Names.GUARD_NOTIFY + car_id, notify_id);
                        ed.commit();
                    } else if ((guard_mode == 0) && (msg_id != 0)) {
                        String[] msg = getString(R.string.alarm).split("\\|");
                        notify_id = Alarm.createNotification(FetchService.this, msg[msg_id], R.drawable.warning, car_id, null);
                        ed.putInt(Names.GUARD_NOTIFY + car_id, notify_id);
                        ed.commit();
                    }
                }
                boolean valet = preferences.getBoolean(Names.GUARD0 + car_id, false) && !preferences.getBoolean(Names.GUARD1 + car_id, false);
                if (valet != prev_valet) {
                    SmsMonitor.processMessageFromApi(FetchService.this, car_id, valet ? R.string.valet_on : R.string.valet_off);
                    if (valet) {
                        int id = preferences.getInt(Names.VALET_OFF_NOTIFY + car_id, 0);
                        if (id != 0)
                            Actions.done_valet_on(FetchService.this, car_id);
                    } else {
                        int id = preferences.getInt(Names.VALET_ON_NOTIFY + car_id, 0);
                        if (id != 0)
                            Actions.done_valet_off(FetchService.this, car_id);
                    }
                }
            }

            if (balance_value != null)
                Preferences.checkBalance(FetchService.this, car_id);

            if (SmsMonitor.haveProcessed(car_id)) {
                Intent iUpdate = new Intent(FetchService.this, FetchService.class);
                iUpdate.setAction(ACTION_UPDATE);
                iUpdate.putExtra(Names.ID, car_id);
                Uri data = Uri.withAppendedPath(Uri.parse("http://service/update/"), car_id);
                iUpdate.setData(data);
                PendingIntent pi = PendingIntent.getService(FetchService.this, 0, iUpdate, 0);
                alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + SCAN_TIMEOUT, pi);
            }

            if (az != null) {
                boolean processed = false;
                if (az.asBoolean()) {
                    processed = SmsMonitor.processMessageFromApi(FetchService.this, car_id, R.string.motor_on);
                    SmsMonitor.cancelSMS(FetchService.this, car_id, R.string.motor_off);
                } else {
                    processed = SmsMonitor.processMessageFromApi(FetchService.this, car_id, R.string.motor_off);
                    SmsMonitor.cancelSMS(FetchService.this, car_id, R.string.motor_on);
                }
                if (!processed &&
                        ((preferences.getInt(Names.MOTOR_ON_NOTIFY + car_id, 0) != 0) || (preferences.getInt(Names.MOTOR_OFF_NOTIFY + car_id, 0) != 0))) {
                    if (az.asBoolean()) {
                        Actions.done_motor_on(FetchService.this, car_id);
                    } else {
                        Actions.done_motor_off(FetchService.this, car_id);
                    }
                }
            }

            if (Actions.inet_requests != null) {
                Set<Actions.InetRequest> requests = Actions.inet_requests.get(car_id);
                if (requests != null) {
                    for (Actions.InetRequest request : requests) {
                        request.check(FetchService.this);
                    }
                }
            }

            if (!preferences.getBoolean(Names.GUARD + car_id, false)) {
                int id = preferences.getInt(Names.MOTOR_OFF_NOTIFY + car_id, 0);
                if (id != 0) {
                    Alarm.removeNotification(FetchService.this, car_id, id);
                    ed.remove(Names.MOTOR_OFF_NOTIFY + car_id);
                    ed.commit();
                }
                id = preferences.getInt(Names.MOTOR_ON_NOTIFY + car_id, 0);
                if (id != 0) {
                    Alarm.removeNotification(FetchService.this, car_id, id);
                    ed.remove(Names.MOTOR_ON_NOTIFY + car_id);
                    ed.commit();
                }
                SmsMonitor.cancelSMS(FetchService.this, car_id, R.string.motor_on);
                SmsMonitor.cancelSMS(FetchService.this, car_id, R.string.motor_off);
            }

            if (preferences.getBoolean(Names.POINTER + car_id, false)) {
                long now = new Date().getTime();
                boolean new_state = ((now - time.asLong()) > 25 * 60 * 60 * 1000);
                if (new_state != preferences.getBoolean(Names.TIMEOUT + car_id, false)) {
                    ed.putBoolean(Names.TIMEOUT + car_id, new_state);
                    int timeout_id = preferences.getInt(Names.TIMEOUT_NOTIFICATION + car_id, 0);
                    try {
                        if (new_state) {
                            if (timeout_id == 0) {
                                timeout_id = Alarm.createNotification(FetchService.this, getString(R.string.timeout), R.drawable.warning, car_id, null);
                                ed.putInt(Names.TIMEOUT_NOTIFICATION + car_id, timeout_id);
                                ed.commit();
                            }
                        } else {
                            if (timeout_id > 0) {
                                Alarm.removeNotification(FetchService.this, car_id, timeout_id);
                                ed.remove(Names.TIMEOUT_NOTIFICATION + car_id);
                                ed.commit();
                            }
                        }
                    } catch (Exception ex) {
                        // ignore
                    }
                    ed.commit();
                }
                double voltage = Double.parseDouble(preferences.getString(Names.VOLTAGE_MAIN + car_id, ""));
                int voltage_id = preferences.getInt(Names.VOLTAGE_NOTIFY + car_id, 0);
                if (voltage_id != 0) {
                    Alarm.removeNotification(FetchService.this, car_id, voltage_id);
                    ed.remove(Names.VOLTAGE_NOTIFY + car_id);
                    ed.commit();
                }
                try {
                    if (voltage < 2.5) {
                        voltage_id = Alarm.createNotification(FetchService.this, getString(R.string.timeout), R.drawable.warning, car_id, null);
                        ed.putInt(Names.VOLTAGE_NOTIFY + car_id, voltage_id);
                        ed.commit();
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
        }

        @Override
        void exec(String api_key) {
            sendUpdate(ACTION_START, car_id);
            execute(URL_STATUS, api_key, preferences.getLong(Names.EVENT_TIME + car_id, 0));
        }

        void setState(String id, JsonObject contact, String key, int msg) throws ParseException {
            boolean state = contact.get(key).asBoolean();
            if (state)
                msg_id = msg;
            ed.putBoolean(id + car_id, state);
        }
    }

    class ZoneRequest extends ServerRequest {

        ZoneRequest(String id) {
            super("Z", id);
        }

        @Override
        void result(JsonObject res) throws ParseException {
            JsonArray events = res.get("events").asArray();
            int i;
            for (i = 0; i < events.size(); i++) {
                JsonObject e = events.get(i).asObject();
                int type = e.get("type").asInt();
                if ((type == 86) || (type == 87)) {
                    String zone = null;
                    JsonValue vName = e.get("zone");
                    if (vName != null)
                        zone = vName.asString();
                    State.appendLog("Zone res " + type + " " + zone);
                    Alarm.zoneNotify(FetchService.this, car_id, type == 86, zone, true);
                    break;
                }
            }
            if (i >= events.size()) {
                State.appendLog("Zone no event");
                Alarm.zoneNotify(FetchService.this, car_id, preferences.getBoolean(Names.ZONE_IN + car_id, false), null, true);
            }
            SharedPreferences.Editor ed = preferences.edit();
            ed.remove(Names.ZONE_TIME + car_id);
            ed.remove(Names.ZONE_IN + car_id);
            ed.commit();
            super.result(res);
        }

        @Override
        void exec(String api_key) {
            String auth = preferences.getString(Names.AUTH + car_id, "");
            if (auth.equals(""))
                return;
            long zone_time = preferences.getLong(Names.ZONE_TIME + car_id, 0);
            if (zone_time == 0)
                return;
            execute(URL_EVENTS, api_key, auth, zone_time - 10, zone_time + 10);
        }
    }

}
