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
import android.media.Ringtone;
import android.media.RingtoneManager;
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

import java.lang.reflect.Field;
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
    static final String ACTION_RELE_OFF = "net.ugona.plus.RELE_OFF";

    static final String AUTH_ERROR = "Auth error";

    static final String URL_STATUS = "https://car-online.ugona.net/?skey=$1&time=$2";
    static final String URL_EVENTS = "https://car-online.ugona.net/events?skey=$1&auth=$2&begin=$3&end=$4";
    static final String URL_GSM = "https://car-online.ugona.net/gsm?skey=$1&cc=$2&nc=$3&lac=$4&cid=$5";
    static final String URL_KEY = "https://car-online.ugona.net/key?auth=$1";
    static final String URL_CARD = "https://car-online.ugona.net/card?auth=$1&t=$2";

    private static final long REPEAT_AFTER_ERROR = 20 * 1000;
    private static final long REPEAT_AFTER_500 = 600 * 1000;
    private static final long LONG_TIMEOUT = 5 * 60 * 60 * 1000;
    private static final long SCAN_TIMEOUT = 10 * 1000;
    private static final long CARD_TIME = 3 * 60 * 1000;

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
                    clearNotification(car_id, intent.getIntExtra(Names.Car.NOTIFY, 0));
                if (action.equals(ACTION_UPDATE) && (car_id == null)) {
                    Cars.Car[] cars = Cars.getCars(this);
                    for (Cars.Car car : cars) {
                        new StatusRequest(Preferences.getCar(preferences, car.id));
                        for (String p : car.pointers) {
                            new StatusRequest(p);
                        }
                    }
                }
                if (action.equals(ACTION_NOTIFICATION)) {
                    String sound = intent.getStringExtra(Names.Car.NOTIFY);
                    String text = intent.getStringExtra(Names.TITLE);
                    int pictId = intent.getIntExtra(Names.Car.ALARM, 0);
                    int max_id = intent.getIntExtra(Names.Car.EVENT_ID, 0);
                    long when = intent.getLongExtra(Names.Car.EVENT_TIME, 0);
                    showNotification(car_id, text, pictId, max_id, sound, when);
                }
                if (action.equals(ACTION_RELE_OFF))
                    Actions.rele_off(this, car_id, intent.getStringExtra(Names.Car.AUTH), intent.getStringExtra(Names.PASSWORD));
            }
            if (car_id != null)
                new StatusRequest(Preferences.getCar(preferences, car_id));
        }
        if (startRequest())
            return START_STICKY;
        return START_NOT_STICKY;
    }

    void showNotification(String car_id, String text, int pictId, int max_id, String sound, long when) {
        String title = getString(R.string.app_name);
        String[] cars = preferences.getString(Names.CARS, "").split(",");
        if (cars.length > 1) {
            title = preferences.getString(Names.Car.CAR_NAME + car_id, "");
            if (title.length() == 0) {
                title = getString(R.string.car);
                if (car_id.length() > 0)
                    title += " " + car_id;
            }
        }

        int defs = Notification.DEFAULT_LIGHTS + Notification.DEFAULT_VIBRATE;
        if (sound == null)
            defs |= Notification.DEFAULT_SOUND;

        Uri uri = null;
        if (sound != null) {
            if (sound.charAt(0) == '.') {
                String key = sound;
                sound = preferences.getString(key, "");
                sound = preferences.getString(key + car_id, sound);
                if (!sound.equals("")) {
                    uri = Uri.parse(sound);
                    Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                    if (ringtone == null)
                        uri = null;
                }
            } else {
                uri = Uri.parse("android.resource://net.ugona.plus/raw/" + sound);
            }
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setDefaults(defs)
                        .setSmallIcon(pictId)
                        .setContentTitle(title)
                        .setContentText(text);
        if (when != 0)
            builder.setWhen(when);

        if (uri != null)
            builder.setSound(uri);

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
        clearIntent.putExtra(Names.Car.NOTIFY, max_id);
        data = Uri.withAppendedPath(Uri.parse("http://notification_clear/id/"), max_id + "");
        clearIntent.setData(data);
        PendingIntent deleteIntent = PendingIntent.getService(this, 0, clearIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setDeleteIntent(deleteIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = builder.build();
        if ((pictId == R.drawable.white_valet_on) || (pictId == R.drawable.gsm_lost))
            notification.flags = Notification.FLAG_ONGOING_EVENT;
        manager.notify(max_id, notification);
    }

    boolean startRequest() {
        for (Map.Entry<String, ServerRequest> entry : requests.entrySet()) {
            if (entry.getValue().started)
                continue;
            entry.getValue().start();
        }
        if (requests.size() > 0)
            return true;
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
            Intent intent = new Intent(ACTION_ERROR);
            intent.putExtra(Names.ERROR, error);
            intent.putExtra(Names.ID, car_id);
            sendBroadcast(intent);
        } catch (Exception e) {
            // ignore
        }
    }

    void clearNotification(String car_id, int id) {
        String[] ids = preferences.getString(Names.Car.N_IDS + car_id, "").split(",");
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
        Field[] fields = Names.Notify.class.getDeclaredFields();
        for (Field f : fields) {
            if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                continue;
            try {
                String val = (String) f.get(Names.Notify.class);
                if (preferences.getInt(val + car_id, 0) == id)
                    ed.remove(val + car_id);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (res == null) {
            ed.remove(Names.Car.N_IDS + car_id);
        } else {
            ed.putString(Names.Car.N_IDS + car_id, res);
        }
        ed.commit();
    }

    void zoneNotify(String car_id, boolean zone_in, String zone, long when) {
        Alarm.zoneNotify(this, car_id, zone_in, zone, true, false, when);
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
                exec(preferences.getString(Names.Car.CAR_KEY + car_id, ""));
                return;
            }
            if (AUTH_ERROR.equals(error_text)) {
                String auth = preferences.getString(Names.Car.AUTH + car_id, "");
                if (!auth.equals("")) {
                    HttpTask authTask = new HttpTask() {
                        @Override
                        void result(JsonObject res) throws ParseException {
                            String key = res.get("key").asString();
                            SharedPreferences.Editor ed = preferences.edit();
                            ed.putString(Names.Car.CAR_KEY + car_id, key);
                            ed.remove(Names.GCM_TIME);
                            ed.commit();
                            exec(preferences.getString(Names.Car.CAR_KEY + car_id, ""));
                        }

                        @Override
                        void error() {
                            requests.remove(key);
                            new StatusRequest(car_id);
                            long timeout = (error_text != null) ? REPEAT_AFTER_500 : REPEAT_AFTER_ERROR;
                            alarmMgr.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + timeout, timeout, piTimer);
                            sendError(error_text, car_id);
                        }
                    };
                    authTask.execute(URL_KEY, auth);
                    return;
                }
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
            String api_key = preferences.getString(Names.Car.CAR_KEY + car_id, "");
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
                repeatGuardNotify();
                sendUpdate(ACTION_NOUPDATE, car_id);
                return;
            }
            ed = preferences.edit();
            ed.putLong(Names.Car.EVENT_TIME + car_id, time.asLong());
            if (time.asLong() > preferences.getLong(Names.Car.LOST + car_id, 0)) {
                int id = preferences.getInt(Names.Car.LOST_NOTIFY + car_id, 0);
                if (id > 0) {
                    ed.remove(Names.Car.LOST_NOTIFY + car_id);
                    Alarm.removeNotification(FetchService.this, car_id, id);
                    id = Alarm.createNotification(FetchService.this, getString(R.string.restore), R.drawable.gsm_restore, car_id, Names.Car.RESTORE_SOUND, 0);
                    ed.putInt(Names.Notify.RESTORE + car_id, id);
                }
            }

            JsonValue first_time = res.get("first_time");
            if (first_time != null)
                ed.putLong(Names.Car.FIRST_TIME + car_id, first_time.asLong());
            JsonValue contact_value = res.get("contact");
            boolean gps_valid = false;
            boolean prev_valet = false;
            int prev_guard_mode = 0;
            if (contact_value != null) {
                JsonObject contact = contact_value.asObject();
                boolean guard = contact.get("guard").asBoolean();
                prev_guard_mode = preferences.getBoolean(Names.Car.GUARD + car_id, false) ? 0 : 1;
                prev_valet = preferences.getBoolean(Names.Car.GUARD0 + car_id, false) && !preferences.getBoolean(Names.Car.GUARD1 + car_id, false);
                if (guard && preferences.getBoolean(Names.Car.GUARD0 + car_id, false) && preferences.getBoolean(Names.Car.GUARD1 + car_id, false))
                    prev_guard_mode = 2;

                if (contact.get("gsm").asBoolean()) {
                    JsonValue gsm_value = res.get("gsm");
                    if (gsm_value != null)
                        ed.putInt(Names.Car.GSM_DB + car_id, gsm_value.asObject().get("db").asInt());
                } else {
                    ed.putInt(Names.Car.GSM_DB + car_id, 0);
                }

                ed.putBoolean(Names.Car.GUARD + car_id, guard);
                if (contact.get("input1") != null) {
                    int inputs = preferences.getInt(Names.Car.INPUTS + car_id, -1);
                    setState(Names.Car.INPUT1, contact, "input1", 3, inputs & 1);
                    setState(Names.Car.INPUT2, contact, "input2", 1, inputs & 2);
                    boolean in3 = contact.get("input3").asBoolean();
                    if ((inputs & 4) == 0)
                        in3 = false;
                    ed.putBoolean(Names.Car.INPUT3 + car_id, in3);
                    setState(Names.Car.INPUT4, contact, "input4", 2, inputs & 4);
                    ed.putBoolean(Names.Car.GUARD0 + car_id, contact.get("guardMode0").asBoolean());
                    ed.putBoolean(Names.Car.GUARD1 + car_id, contact.get("guardMode1").asBoolean());
                    ed.putBoolean(Names.Car.RELAY1 + car_id, contact.get("relay1").asBoolean());
                    ed.putBoolean(Names.Car.RELAY2 + car_id, contact.get("relay2").asBoolean());
                    ed.putBoolean(Names.Car.RELAY3 + car_id, contact.get("relay3").asBoolean());
                    ed.putBoolean(Names.Car.RELAY4 + car_id, contact.get("relay4").asBoolean());
                    ed.putBoolean(Names.Car.ENGINE + car_id, contact.get("engine").asBoolean());
                    ed.putBoolean(Names.Car.RESERVE_NORMAL + car_id, contact.get("reservePowerNormal").asBoolean());
                }

                setState(Names.Car.ZONE_DOOR, contact, "door", 3, 1);
                setState(Names.Car.ZONE_HOOD, contact, "hood", 2, 1);
                setState(Names.Car.ZONE_TRUNK, contact, "trunk", 1, 1);
                setState(Names.Car.ZONE_ACCESSORY, contact, "accessory", 7, 1);
                setState(Names.Car.ZONE_IGNITION, contact, "realIgnition", 4, 1);

                ed.putInt(Names.Car.GUARD_MSG + car_id, msg_id);

                long valet_time = preferences.getLong(Names.Car.VALET_TIME + car_id, 0);
                if (valet_time > 0) {
                    if (valet_time + 30000 < new Date().getTime()) {
                        valet_time = 0;
                        ed.remove(Names.Car.VALET_TIME + car_id);
                    }
                }
                long init_time = preferences.getLong(Names.Car.INIT_TIME + car_id, 0);
                if (init_time > 0) {
                    if (init_time + 30000 < new Date().getTime()) {
                        init_time = 0;
                        ed.remove(Names.Car.INIT_TIME + car_id);
                    }
                }
                if (valet_time > 0) {
                    guard = false;
                    ed.putBoolean(Names.Car.GUARD + car_id, guard);
                    ed.putBoolean(Names.Car.GUARD0 + car_id, true);
                    ed.putBoolean(Names.Car.GUARD1 + car_id, false);
                } else if (init_time > 0) {
                    ed.putBoolean(Names.Car.GUARD0 + car_id, false);
                    ed.putBoolean(Names.Car.GUARD1 + car_id, false);
                }
                gps_valid = contact.get("gpsValid").asBoolean();

                setDoor("door_front_left", contact, Names.Car.DOOR_FL);
                setDoor("door_front_right", contact, Names.Car.DOOR_FR);
                setDoor("door_back_left", contact, Names.Car.DOOR_BL);
                setDoor("door_back_right", contact, Names.Car.DOOR_BR);
            }

            JsonValue voltage_value = res.get("voltage");
            if (voltage_value != null) {
                JsonObject voltage = voltage_value.asObject();
                ed.putString(Names.Car.VOLTAGE_MAIN + car_id, voltage.get("main").asDouble() + "");
                if (voltage.get("reserved") != null)
                    ed.putString(Names.Car.VOLTAGE_RESERVED + car_id, voltage.get("reserved").asDouble() + "");
            }

            JsonValue gps_value = res.get("gps");
            if (gps_value != null) {
                JsonObject gps = gps_value.asObject();
                ed.putFloat(Names.Car.LAT + car_id, gps.get("latitude").asFloat());
                ed.putFloat(Names.Car.LNG + car_id, gps.get("longitude").asFloat());
                ed.putFloat(Names.Car.SPEED + car_id, gps.get("speed").asFloat());
                if (gps_valid && (gps.get("course") != null))
                    ed.putInt(Names.Car.COURSE + car_id, gps.get("course").asInt());
            } else {
                JsonValue gsm_value = res.get("gsm");
                if (gsm_value != null) {
                    JsonObject gsm = gsm_value.asObject();
                    String gsm_str = gsm.get("cc").asInt() + " ";
                    gsm_str += gsm.get("nc").asInt() + " ";
                    gsm_str += gsm.get("lac").asInt() + " ";
                    gsm_str += gsm.get("cid").asInt();
                    if (!preferences.getString(Names.Car.GSM_SECTOR + car_id, "").equals(gsm_str)) {
                        ed.putString(Names.Car.GSM_SECTOR + car_id, gsm_str);
                        ed.remove(Names.Car.GSM_ZONE + car_id);
                        ed.remove(Names.Car.LAT + car_id);
                        ed.remove(Names.Car.LNG + car_id);
                    }
                    if (preferences.getString(Names.Car.GSM_ZONE + car_id, "").equals("")) {
                        ed.commit();
                        new GsmZoneRequest(car_id);
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
                ed.putString(Names.Car.TEMPERATURE + car_id, val);
            }

            JsonValue balance_value = null;
            if (preferences.getLong(Names.Car.BALANCE_TIME + car_id, 0) + 60000 < new Date().getTime()) {
                balance_value = res.get("balance");
                if (balance_value != null) {
                    JsonObject balance = balance_value.asObject();
                    double value = balance.get("value").asDouble();
                    if (preferences.getString(Names.Car.BALANCE + car_id, "").equals(value + "")) {
                        balance_value = null;
                    } else {
                        ed.putString(Names.Car.BALANCE + car_id, value + "");
                        ed.remove(Names.Car.BALANCE_TIME + car_id);
                    }
                }
            }

            JsonValue zone = res.get("zone");
            if (zone != null) {
                ed.putLong(Names.Car.ZONE_TIME + car_id, zone.asLong());
                ed.putBoolean(Names.Car.ZONE_IN + car_id, res.get("zone_in").asBoolean());
                new ZoneRequest(car_id);
            }

            JsonValue last_stand = res.get("last_stand");
            if (last_stand != null)
                ed.putLong(Names.Car.LAST_STAND + car_id, last_stand.asLong());
            JsonValue az = res.get("az");
            if (az != null)
                ed.putBoolean(Names.Car.AZ + car_id, az.asBoolean());
            JsonValue azStart = res.get("az_start");
            if (azStart != null)
                ed.putLong(Names.Car.AZ_START + car_id, azStart.asLong());
            JsonValue azStop = res.get("az_stop");
            if (azStop != null)
                ed.putLong(Names.Car.AZ_STOP + car_id, azStop.asLong());
            JsonValue timer = res.get("timer");
            if (timer != null) {
                int timerValue = timer.asInt();
                if ((timerValue >= 1) && (timerValue <= 30))
                    ed.putInt(Names.Car.CAR_TIMER + car_id, timerValue);
            }
            JsonValue settings = res.get("settings");
            if (settings != null)
                ed.remove(Names.Car.SETTINGS_TIME + car_id);
            JsonValue guard_time = res.get("guard_time");
            if (guard_time != null)
                ed.putLong(Names.Car.GUARD_TIME + car_id, guard_time.asLong());

            JsonValue card = res.get("card");
            if (card != null)
                ed.putLong(Names.Car.CARD + car_id, card.asLong());
            ed.commit();

            if (preferences.getLong(Names.Car.RELE_START + car_id, 0) != 0) {
                boolean ignition = preferences.getBoolean(Names.Car.ZONE_IGNITION + car_id, false);
                if (preferences.getBoolean(Names.Car.INPUT3 + car_id, false))
                    ignition = true;
                if ((az != null) && az.asBoolean())
                    ignition = true;
                if (ignition) {
                    ed.remove(Names.Car.RELE_START + car_id);
                    ed.commit();
                }
            }
            if (preferences.getBoolean(Names.Car.AZ + car_id, false)) {
                long start = preferences.getLong(Names.Car.AZ_START + car_id, 0);
                if (start < time.asLong() - 30 * 60 * 1000) {
                    ed.putBoolean(Names.Car.AZ + car_id, false);
                    ed.commit();
                }
            }

            sendUpdate(ACTION_UPDATE, car_id);

            if (contact_value != null) {
                int guard_mode = preferences.getBoolean(Names.Car.GUARD + car_id, false) ? 0 : 1;
                if ((guard_mode == 0) && preferences.getBoolean(Names.Car.GUARD0 + car_id, false) && preferences.getBoolean(Names.Car.GUARD1 + car_id, false))
                    guard_mode = 2;
                if (guard_mode != prev_guard_mode)
                    guardNotify();

                boolean valet = preferences.getBoolean(Names.Car.GUARD0 + car_id, false) && !preferences.getBoolean(Names.Car.GUARD1 + car_id, false);
                if (valet != prev_valet) {
                    SmsMonitor.processMessageFromApi(FetchService.this, car_id, valet ? R.string.valet_on : R.string.valet_off, 0);
                    if (valet) {
                        int id = preferences.getInt(Names.Notify.VALET_OFF + car_id, 0);
                        if (id != 0)
                            Actions.done_valet_on(FetchService.this, car_id);
                    } else {
                        int id = preferences.getInt(Names.Car.VALET_ON_NOTIFY + car_id, 0);
                        if (id != 0)
                            Actions.done_valet_off(FetchService.this, car_id);
                    }
                }
            }
            repeatGuardNotify();

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
                    long when = 0;
                    JsonValue v = res.get("az_start");
                    if (v != null)
                        when = v.asLong();
                    processed = SmsMonitor.processMessageFromApi(FetchService.this, car_id, R.string.motor_on, when);
                    SmsMonitor.cancelSMS(FetchService.this, car_id, R.string.motor_off);
                } else {
                    long when = 0;
                    JsonValue v = res.get("az_stop");
                    if (v != null)
                        when = v.asLong();
                    processed = SmsMonitor.processMessageFromApi(FetchService.this, car_id, R.string.motor_off, when);
                    SmsMonitor.cancelSMS(FetchService.this, car_id, R.string.motor_on);
                }
                if (!processed &&
                        ((preferences.getInt(Names.Notify.MOTOR_ON + car_id, 0) != 0) || (preferences.getInt(Names.Notify.MOTOR_OFF + car_id, 0) != 0))) {
                    if (az.asBoolean()) {
                        Actions.done_motor_on(FetchService.this, car_id, 0);
                    } else {
                        Actions.done_motor_off(FetchService.this, car_id, 0);
                    }
                }
            } else if (!preferences.getBoolean(Names.Car.AZ + car_id, false) && preferences.getBoolean(Names.Car.GUARD + car_id, false) &&
                    (preferences.getBoolean(Names.Car.INPUT3 + car_id, false) || preferences.getBoolean(Names.Car.ZONE_IGNITION + car_id, false)) &&
                    SmsMonitor.isProcessed(car_id, R.string.motor_on)) {
                ed.putBoolean(Names.Car.AZ + car_id, true);
                long when = time.asLong();
                JsonValue v = res.get("az_start");
                if (v != null)
                    when = v.asLong();
                ed.putLong(Names.Car.AZ_START + car_id, when);
                ed.commit();
                SmsMonitor.processMessageFromApi(FetchService.this, car_id, R.string.motor_on, when);
            }

            if (Actions.inet_requests != null) {
                Set<Actions.InetRequest> requests = Actions.inet_requests.get(car_id);
                if (requests != null) {
                    for (Actions.InetRequest request : requests) {
                        request.check(FetchService.this);
                    }
                }
            }

            if (!preferences.getBoolean(Names.Car.GUARD + car_id, false)) {
                int id = preferences.getInt(Names.Notify.MOTOR_OFF + car_id, 0);
                if (id != 0) {
                    Alarm.removeNotification(FetchService.this, car_id, id);
                    ed.remove(Names.Notify.MOTOR_OFF + car_id);
                    ed.commit();
                }
                id = preferences.getInt(Names.Notify.MOTOR_ON + car_id, 0);
                if (id != 0) {
                    Alarm.removeNotification(FetchService.this, car_id, id);
                    ed.remove(Names.Notify.MOTOR_ON + car_id);
                    ed.commit();
                }
                SmsMonitor.cancelSMS(FetchService.this, car_id, R.string.motor_on);
                SmsMonitor.cancelSMS(FetchService.this, car_id, R.string.motor_off);
            } else {
                long card_t = preferences.getLong(Names.Car.CARD + car_id, 0);
                long guard_t = preferences.getLong(Names.Car.GUARD_TIME + car_id, 0);
                if ((card_t > 0) && (guard_t > 0) && (card_t < guard_t)) {
                    long event_t = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
                    new CardRequest(car_id);
                    if (event_t - guard_t > CARD_TIME) {
                        if (preferences.getLong(Names.Car.CARD_EVENT + car_id, 0) != card_t) {
                            ed.putLong(Names.Car.CARD_EVENT + car_id, card_t);
                            int card_id = preferences.getInt(Names.Notify.CARD + car_id, 0);
                            if (card_id == 0) {
                                card_id = Alarm.createNotification(FetchService.this, getString(R.string.card_message), R.drawable.warning, car_id, null, 0);
                                ed.putInt(Names.Notify.CARD + car_id, card_id);
                            }
                            ed.commit();
                        }
                    } else {
                        Intent iUpdate = new Intent(FetchService.this, FetchService.class);
                        iUpdate.setAction(ACTION_START);
                        iUpdate.putExtra(Names.ID, car_id);
                        Uri data = Uri.withAppendedPath(Uri.parse("http://service/update/"), car_id);
                        iUpdate.setData(data);
                        PendingIntent pi = PendingIntent.getService(FetchService.this, 0, iUpdate, 0);
                        alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + CARD_TIME, pi);
                    }
                }
            }

            int card_id = preferences.getInt(Names.Notify.CARD + car_id, 0);
            if (card_id != 0) {
                long card_t = preferences.getLong(Names.Car.CARD + car_id, 0);
                if (!preferences.getBoolean(Names.Car.GUARD + car_id, false) || (card_t <= 0)) {
                    Alarm.removeNotification(FetchService.this, car_id, card_id);
                    ed.remove(Names.Notify.CARD + car_id);
                    ed.commit();
                }
            }

            if (preferences.getBoolean(Names.Car.POINTER + car_id, false)) {
                long now = new Date().getTime();
                boolean new_state = ((now - time.asLong()) > 25 * 60 * 60 * 1000);
                if (new_state != preferences.getBoolean(Names.Car.TIMEOUT + car_id, false)) {
                    ed.putBoolean(Names.Car.TIMEOUT + car_id, new_state);
                    int timeout_id = preferences.getInt(Names.Notify.TIMEOUT + car_id, 0);
                    try {
                        if (new_state) {
                            if (timeout_id == 0) {
                                timeout_id = Alarm.createNotification(FetchService.this, getString(R.string.timeout), R.drawable.warning, car_id, null, 0);
                                ed.putInt(Names.Notify.TIMEOUT + car_id, timeout_id);
                                ed.commit();
                            }
                        } else {
                            if (timeout_id > 0) {
                                Alarm.removeNotification(FetchService.this, car_id, timeout_id);
                                ed.remove(Names.Notify.TIMEOUT + car_id);
                                ed.commit();
                            }
                        }
                    } catch (Exception ex) {
                        // ignore
                    }
                    ed.commit();
                }
                double voltage = Double.parseDouble(preferences.getString(Names.Car.VOLTAGE_MAIN + car_id, ""));
                int voltage_id = preferences.getInt(Names.Notify.VOLTAGE + car_id, 0);
                if (voltage_id != 0) {
                    Alarm.removeNotification(FetchService.this, car_id, voltage_id);
                    ed.remove(Names.Notify.VOLTAGE + car_id);
                    ed.commit();
                }
                try {
                    if (voltage < 2.5) {
                        voltage_id = Alarm.createNotification(FetchService.this, getString(R.string.timeout), R.drawable.warning, car_id, null, 0);
                        ed.putInt(Names.Notify.VOLTAGE + car_id, voltage_id);
                        ed.commit();
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
        }

        void repeatGuardNotify() {
            int notify_id = 0;
            try {
                notify_id = preferences.getInt(Names.Notify.GUARD + car_id, 0);
            } catch (Exception ex) {
                ed.remove(Names.Notify.GUARD + car_id);
                ed.commit();
            }
            if (notify_id == 0)
                return;
            long next = preferences.getLong(Names.Car.GUARD_NEXT + car_id, 0);
            if (next == 0)
                return;
            Date now = new Date();
            if (now.getTime() < next)
                return;
            guardNotify();
        }

        void guardNotify() {
            int guard_mode = preferences.getBoolean(Names.Car.GUARD + car_id, false) ? 0 : 1;
            if ((guard_mode == 0) && preferences.getBoolean(Names.Car.GUARD0 + car_id, false) && preferences.getBoolean(Names.Car.GUARD1 + car_id, false))
                guard_mode = 2;
            int notify_id = 0;
            try {
                notify_id = preferences.getInt(Names.Notify.GUARD + car_id, 0);
            } catch (Exception ex) {
                ed.remove(Names.Notify.GUARD + car_id);
                ed.commit();
            }
            if (notify_id != 0) {
                Alarm.removeNotification(FetchService.this, car_id, notify_id);
                ed.remove(Names.Notify.GUARD + car_id);
                ed.commit();
            }
            String guard_pref = preferences.getString(Names.Car.GUARD_MODE + car_id, "");
            boolean notify = false;
            if (guard_pref.equals("")) {
                notify = (guard_mode == 2);
            } else if (guard_pref.equals("all")) {
                notify = true;
            }
            Date now = new Date();
            long guard_time = preferences.getLong(Names.Car.GUARD_TIME + car_id, 0);
            if (guard_time == 0)
                guard_time = now.getTime();
            long delta = now.getTime() - guard_time;
            if (delta < 120000)
                delta = 120000;
            long next_time = now.getTime() + delta;
            if (notify) {
                int id = R.string.ps_guard;
                String sound = "warning";
                switch (guard_mode) {
                    case 0:
                        id = R.string.guard_on;
                        sound = "guard_on";
                        next_time = 0;
                        break;
                    case 1:
                        id = R.string.guard_off;
                        sound = "guard_off";
                        next_time = 0;
                        break;
                }
                notify_id = Alarm.createNotification(FetchService.this, getString(id), R.drawable.warning, car_id, sound, guard_time);
                ed.putInt(Names.Notify.GUARD + car_id, notify_id);
                ed.putLong(Names.Car.GUARD_NEXT + car_id, next_time);
                ed.commit();
            } else if (guard_mode == 0) {
                int msg_id = preferences.getInt(Names.Car.GUARD_MSG + car_id, 0);
                if (msg_id != 0) {
                    String[] msg = getString(R.string.alarm).split("\\|");
                    notify_id = Alarm.createNotification(FetchService.this, msg[msg_id], R.drawable.warning, car_id, null, guard_time);
                    ed.putInt(Names.Notify.GUARD + car_id, notify_id);
                    ed.putLong(Names.Car.GUARD_NEXT + car_id, next_time);
                    ed.commit();
                }
            }
            if ((notify_id != 0) && (next_time != 0)) {
                Intent iUpdate = new Intent(FetchService.this, FetchService.class);
                iUpdate.setAction(ACTION_START);
                iUpdate.putExtra(Names.ID, car_id);
                Uri data = Uri.withAppendedPath(Uri.parse("http://service/guard/"), car_id);
                iUpdate.setData(data);
                PendingIntent pi = PendingIntent.getService(FetchService.this, 0, iUpdate, 0);
                alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delta, pi);
            }
        }

        @Override
        void exec(String api_key) {
            sendUpdate(ACTION_START, car_id);
            execute(URL_STATUS, api_key, preferences.getLong(Names.Car.EVENT_TIME + car_id, 0));
        }

        void setDoor(String id, JsonObject contact, String key) throws ParseException {
            JsonValue v = contact.get(id);
            if (v == null) {
                ed.remove(Names.Car.DOORS_4 + car_id);
                return;
            }
            ed.putBoolean(key + car_id, v.asBoolean());
            ed.putBoolean(Names.Car.DOORS_4 + car_id, true);
        }

        void setState(String id, JsonObject contact, String key, int msg, int inputs) throws ParseException {
            boolean state = false;
            if (inputs != 0)
                state = contact.get(key).asBoolean();
            if (state)
                msg_id = msg;
            ed.putBoolean(id + car_id, state);
        }
    }

    class CardRequest extends ServerRequest {

        CardRequest(String id) {
            super("C", id);
        }

        @Override
        void result(JsonObject res) throws ParseException {
            JsonValue v = res.get("card");
            if (v != null) {
                long card_t = v.asLong();
                SharedPreferences.Editor ed = preferences.edit();
                ed.putLong(Names.Car.CARD + car_id, card_t);
                ed.commit();
                int card_id = preferences.getInt(Names.Notify.CARD + car_id, 0);
                if (card_id != 0) {
                    if (!preferences.getBoolean(Names.Car.GUARD + car_id, false) || (card_t <= 0)) {
                        Alarm.removeNotification(FetchService.this, car_id, card_id);
                        ed.remove(Names.Notify.CARD + car_id);
                        ed.commit();
                    }
                }
                sendUpdate(ACTION_UPDATE, car_id);
            }
        }

        @Override
        void exec(String api_key) {
            execute(URL_CARD, api_key, preferences.getLong(Names.Car.GUARD_TIME, 0));
        }
    }

    class GsmZoneRequest extends ServerRequest {

        GsmZoneRequest(String id) {
            super("G", id);
        }

        @Override
        void result(JsonObject res) throws ParseException {
            String sector = res.get("sector").asString();
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(Names.Car.GSM_ZONE + car_id, sector);
            ed.commit();
            sendUpdate(ACTION_UPDATE, car_id);
        }

        @Override
        void exec(String api_key) {
            String gsm = preferences.getString(Names.Car.GSM_SECTOR + car_id, "");
            String[] gsm_parts = gsm.split(" ");
            execute(URL_GSM, api_key, gsm_parts[0], gsm_parts[1], gsm_parts[2], gsm_parts[3]);
        }
    }

    class ZoneRequest extends ServerRequest {

        ZoneRequest(String id) {
            super("Z", id);
        }

        @Override
        void result(JsonObject res) throws ParseException {
            long zone_time = preferences.getLong(Names.Car.ZONE_TIME + car_id, 0);
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
                    zoneNotify(car_id, type == 86, zone, zone_time);
                    break;
                }
            }
            if (i >= events.size()) {
                zoneNotify(car_id, preferences.getBoolean(Names.Car.ZONE_IN + car_id, false), null, zone_time);
            }
            SharedPreferences.Editor ed = preferences.edit();
            ed.remove(Names.Car.ZONE_TIME + car_id);
            ed.remove(Names.Car.ZONE_IN + car_id);
            ed.commit();
            super.result(res);
        }

        @Override
        void exec(String api_key) {
            String auth = preferences.getString(Names.Car.AUTH + car_id, "");
            if (auth.equals(""))
                return;
            long zone_time = preferences.getLong(Names.Car.ZONE_TIME + car_id, 0);
            if (zone_time == 0)
                return;
            execute(URL_EVENTS, api_key, auth, zone_time - 10, zone_time + 10);
        }
    }

}
