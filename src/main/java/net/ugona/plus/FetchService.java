package net.ugona.plus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetchService extends Service {

    private static final long REPEAT_AFTER_ERROR = 20 * 1000;
    private static final long REPEAT_AFTER_500 = 600 * 1000;
    private static final long SAFEMODE_TIMEOUT = 300 * 1000;

    private BroadcastReceiver mReceiver;
    private PendingIntent pi;

    private SharedPreferences preferences;
    private ConnectivityManager conMgr;
    private PowerManager powerMgr;
    private AlarmManager alarmMgr;

    static final String ACTION_UPDATE = "net.ugona.plus.UPDATE";
    static final String ACTION_NOUPDATE = "net.ugona.plus.NO_UPDATE";
    static final String ACTION_ERROR = "net.ugona.plus.ERROR";
    static final String ACTION_START = "net.ugona.plus.START";
    static final String ACTION_UPDATE_FORCE = "net.ugona.plus.UPDATE_FORCE";

    static final Pattern balancePattern = Pattern.compile("-?[0-9]+[\\.,][0-9][0-9]");

    static final String STATUS_URL = "http://api.car-online.ru/v2?get=lastinfo&skey=$1&content=json";
    private static final String EVENTS_URL = "http://api.car-online.ru/v2?get=events&skey=$1&begin=$2&end=$3&content=json";
    private static final String TEMP_URL = "http://api.car-online.ru/v2?get=temperaturelist&skey=$1&begin=$2&end=$3&content=json";
    private static final String VOLTAGE_URL = "http://api.car-online.ru/v2?get=voltagelist&skey=$1&begin=$2&end=$3&content=json";
    private static final String GSM_URL = "http://api.car-online.ru/v2?get=gsmlist&skey=$1&begin=$2&end=$3&content=json";
    private static final String GPS_URL = "http://api.car-online.ru/v2?get=gps&skey=$1&id=$2&time=$3&content=json";
    private static final String SECTOR_URL = "http://api.car-online.ru/v2?get=gsmsector&skey=$1&cc=$2&nc=$3&lac=$4&cid=$5&content=json";
    private static final String BALANCE_URL = "http://api.car-online.ru/v2?get=balancelist&skey=$1&begin=$2&content=json";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                State.print(ex);
            }
        });

        super.onCreate();
        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        powerMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        pi = PendingIntent.getService(this, 0, new Intent(this, FetchService.class), 0);
        mReceiver = new ScreenReceiver();
        requests = new HashMap<String, ServerRequest>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String car_id = intent.getStringExtra(Names.ID);
            if (car_id != null)
                new StatusRequest(Preferences.getCar(preferences, car_id));
        }
        startRequest();
        return START_STICKY;
    }

    void startRequest() {
        for (Map.Entry<String, ServerRequest> entry : requests.entrySet()) {
            if (entry.getValue().started)
                return;
        }
        for (Map.Entry<String, ServerRequest> entry : requests.entrySet()) {
            entry.getValue().start();
            return;
        }
    }

    private Map<String, ServerRequest> requests;

    abstract class ServerRequest extends HttpTask {

        final String key;
        final String car_id;
        boolean started;

        ServerRequest(String type, String id) {
            key = type + id;
            car_id = id;
            if (requests.get(key) != null)
                return;
            requests.put(key, this);
        }

        @Override
        void result(JSONObject res) throws JSONException {
            requests.remove(key);
            startRequest();
        }

        @Override
        void error() {
            requests.remove(key);
            new StatusRequest(car_id);
            long timeout = (error_text != null) ? REPEAT_AFTER_500 : REPEAT_AFTER_ERROR;
            alarmMgr.setInexactRepeating(preferences.getBoolean(Names.NOSLEEP_MODE + car_id, false) ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC,
                    System.currentTimeMillis() + timeout, timeout, pi);
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
            final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
            if ((activeNetwork == null) || !activeNetwork.isConnected()) {
                IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
                registerReceiver(mReceiver, filter);
                return;
            }
            if (!preferences.getBoolean(Names.NOSLEEP_MODE + car_id, false) && !powerMgr.isScreenOn()) {
                IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
                registerReceiver(mReceiver, filter);
                return;
            }
            try {
                unregisterReceiver(mReceiver);
            } catch (Exception e) {
                // ignore
            }
            alarmMgr.cancel(pi);
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
        void background(JSONObject res) throws JSONException {
            JSONObject event = res.getJSONObject("event");
            long eventId = event.getLong("eventId");

            if (eventId == preferences.getLong(Names.EVENT_ID + car_id, 0)) {
                sendUpdate(ACTION_NOUPDATE, car_id);
                if (preferences.getBoolean(Names.NOSLEEP_MODE + car_id, false)) {
                    alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, SAFEMODE_TIMEOUT, SAFEMODE_TIMEOUT, pi);
                }
                return;
            }

            long eventTime = event.getLong("eventTime");
            ed = preferences.edit();
            ed.putLong(Names.EVENT_ID + car_id, eventId);
            ed.putLong(Names.EVENT_TIME + car_id, eventTime);

            boolean voltage_req = true;
            boolean gsm_req = true;
            if (res.has("voltage")) {
                voltage_req = false;
                JSONObject voltage = res.getJSONObject("voltage");
                ed.putString(Names.VOLTAGE_MAIN + car_id, voltage.getString("main"));
                ed.putString(Names.VOLTAGE_RESERVED + car_id, voltage.getString("reserved"));
            }

            if (preferences.getLong(Names.BALANCE_TIME + car_id, 0) == 0) {
                JSONObject balance = res.getJSONObject("balance");
                Matcher m = balancePattern.matcher(balance.getString("source"));
                if (m.find())
                    ed.putString(Names.BALANCE + car_id, m.group(0).replaceAll(",", "."));
            }

            JSONObject contact = res.getJSONObject("contact");
            boolean guard = contact.getBoolean("stGuard");
            ed.putBoolean(Names.GUARD + car_id, guard);
            ed.putBoolean(Names.INPUT1 + car_id, contact.getBoolean("stInput1"));
            ed.putBoolean(Names.INPUT2 + car_id, contact.getBoolean("stInput2"));
            ed.putBoolean(Names.INPUT3 + car_id, contact.getBoolean("stInput3"));
            ed.putBoolean(Names.INPUT4 + car_id, contact.getBoolean("stInput4"));
            setState(Names.ZONE_DOOR, contact, "stZoneDoor", 3);
            setState(Names.ZONE_HOOD, contact, "stZoneHood", 2);
            setState(Names.ZONE_TRUNK, contact, "stZoneTrunk", 1);
            setState(Names.ZONE_ACCESSORY, contact, "stZoneAccessoryOn", 7);
            setState(Names.ZONE_IGNITION, contact, "stZoneIgnitionOn", 4);

            if (res.has("gps")) {
                gsm_req = false;
                JSONObject gps = res.getJSONObject("gps");
                ed.putString(Names.LATITUDE + car_id, gps.getString("latitude"));
                ed.putString(Names.LONGITUDE + car_id, gps.getString("longitude"));
                ed.putString(Names.SPEED + car_id, gps.getString("speed"));
                if (contact.getBoolean("stGPS") && contact.getBoolean("stGPSValid"))
                    ed.putString(Names.COURSE + car_id, gps.getString("course"));
            }


            boolean sms_alarm = preferences.getBoolean(Names.SMS_ALARM, false);
            if (sms_alarm)
                ed.remove(Names.SMS_ALARM);

            ed.commit();
            sendUpdate(ACTION_UPDATE, car_id);

            if (!sms_alarm && (msg_id > 0) && guard) {
                Intent alarmIntent = new Intent(FetchService.this, Alarm.class);
                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                String[] alarms = getString(R.string.alarm).split("\\|");
                alarmIntent.putExtra(Names.ALARM, alarms[msg_id]);
                alarmIntent.putExtra(Names.ID, car_id);
                FetchService.this.startActivity(alarmIntent);
            }

            if (preferences.getBoolean(Names.NOSLEEP_MODE + car_id, false))
                alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, SAFEMODE_TIMEOUT, SAFEMODE_TIMEOUT, pi);

            new EventsRequest(car_id, voltage_req, gsm_req);

            long balance_time = preferences.getLong(Names.BALANCE_TIME + car_id, 0);
            if (balance_time > 0) {
                long now = new Date().getTime() - 6 * 3600 * 1000;
                if (balance_time < now)
                    balance_time = now;
                new BalanceRequest(car_id, balance_time);
            }

        }

        @Override
        void exec(String api_key) {
            sendUpdate(ACTION_START, car_id);
            execute(STATUS_URL, api_key);
        }

        void setState(String id, JSONObject contact, String key, int msg) throws JSONException {
            boolean state = contact.getBoolean(key);
            if (state) {
                if (!preferences.getBoolean(id + car_id, false))
                    msg_id = msg;
            }
            ed.putBoolean(id + car_id, state);
        }

    }

    class BalanceRequest extends ServerRequest {

        long begin;

        BalanceRequest(String id, long time) {
            super("B", id);
            begin = time;
        }

        @Override
        void background(JSONObject res) throws JSONException {
            if (res == null)
                return;
            JSONArray balances = res.getJSONArray("balanceList");
            if (balances.length() == 0)
                return;
            JSONObject balance = balances.getJSONObject(0);
            String data = balance.getString("value");
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(Names.BALANCE, data);
            ed.remove(Names.BALANCE_TIME);
            ed.commit();
            sendUpdate(ACTION_UPDATE, car_id);
        }

        @Override
        void exec(String api_key) {
            execute(BALANCE_URL, api_key, begin + "");
        }
    }

    class EventsRequest extends ServerRequest {

        boolean voltage;
        boolean gsm;

        EventsRequest(String id, boolean voltage_request, boolean gsm_request) {
            super("E", id);
            voltage = voltage_request;
            gsm = gsm_request;
        }

        @Override
        void background(JSONObject res) throws JSONException {
            if (res == null)
                return;

            JSONArray events = res.getJSONArray("events");
            if (events.length() > 0) {
                boolean valet_state = preferences.getBoolean(Names.VALET + car_id, false);
                boolean valet = valet_state;
                boolean engine_state = preferences.getBoolean(Names.ENGINE + car_id, false);
                boolean engine = engine_state;
                long last_stand = preferences.getLong(Names.LAST_STAND, 0);
                long stand = last_stand;
                long event_id = 0;
                for (int i = events.length() - 1; i >= 0; i--) {
                    JSONObject event = events.getJSONObject(i);
                    int type = event.getInt("eventType");
                    switch (type) {
                        case 120:
                            valet_state = true;
                            engine_state = false;
                            break;
                        case 110:
                        case 24:
                        case 25:
                            valet_state = false;
                            engine_state = false;
                            break;
                        case 45:
                        case 46:
                            engine_state = true;
                            break;
                        case 47:
                        case 48:
                            engine_state = false;
                            break;
                        case 37:
                            last_stand = -event.getLong("eventTime");
                            break;
                        case 38:
                            last_stand = event.getLong("eventTime");
                            event_id = event.getLong("eventId");
                            break;
                    }
                }
                if ((event_id > 0) && !preferences.getString(Names.LATITUDE + car_id, "").equals(""))
                    new GPSRequest(car_id, event_id, last_stand);
                boolean changed = false;
                SharedPreferences.Editor ed = preferences.edit();
                ed.putLong(Names.LAST_EVENT + car_id, eventTime);
                if (valet_state != valet) {
                    ed.putBoolean(Names.VALET + car_id, valet_state);
                    changed = true;
                }
                if (engine_state != engine) {
                    ed.putBoolean(Names.ENGINE + car_id, engine_state);
                    changed = true;
                }
                if (last_stand != stand) {
                    ed.putLong(Names.LAST_STAND + car_id, last_stand);
                    changed = true;
                }
                ed.commit();
                if (changed)
                    sendUpdate(ACTION_UPDATE, car_id);
            }

            if (voltage)
                new VoltageRequest(car_id);
            if (gsm)
                new GsmRequest(car_id);

            new TemperatureRequest(car_id);
        }

        @Override
        void exec(String api_key) {
            eventTime = preferences.getLong(Names.EVENT_TIME + car_id, 0);
            long begin = preferences.getLong(Names.LAST_EVENT + car_id, 0);
            long bound = eventTime - 2 * 24 * 60 * 60 * 1000;
            if (begin < bound)
                begin = bound;
            execute(EVENTS_URL, api_key, begin + "", eventTime + "");
        }

        long eventTime;

    }

    class GPSRequest extends ServerRequest {

        final String event_id;
        final String event_time;

        GPSRequest(String id, long eventId, long eventTime) {
            super("G", id);
            event_id = eventId + "";
            event_time = eventTime + "";
        }

        @Override
        void background(JSONObject res) throws JSONException {
            if (res == null)
                return;
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(Names.COURSE + car_id, res.getString("course"));
            ed.commit();
        }

        @Override
        void exec(String api_key) {
            execute(GPS_URL, api_key, event_id, event_time);
        }

        @Override
        void error() {
            requests.remove(key);
        }
    }

    class TemperatureRequest extends ServerRequest {

        TemperatureRequest(String id) {
            super("T", id);
        }

        @Override
        void background(JSONObject res) throws JSONException {
            if (res == null)
                return;
            JSONArray arr = res.getJSONArray("temperatureList");
            if (arr.length() == 0)
                return;
            JSONObject value = arr.getJSONObject(0);
            String temp = value.getString("value");
            if (temp.equals(preferences.getString(Names.TEMPERATURE + car_id, "")))
                return;
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(Names.TEMPERATURE + car_id, temp);
            ed.commit();
            sendUpdate(ACTION_UPDATE, car_id);
        }

        @Override
        void exec(String api_key) {
            long eventTime = preferences.getLong(Names.LAST_EVENT + car_id, 0);
            execute(TEMP_URL, api_key,
                    (eventTime - 24 * 60 * 60 * 1000) + "",
                    eventTime + "");
        }
    }

    class VoltageRequest extends ServerRequest {

        VoltageRequest(String id) {
            super("V", id);
        }

        @Override
        void background(JSONObject res) throws JSONException {
            if (res == null)
                return;
            JSONArray arr = res.getJSONArray("voltageList");
            if (arr.length() == 0)
                return;
            JSONObject value = arr.getJSONObject(0);
            String main = value.getString("main");
            String reserved = value.getString("reserved");
            if (main.equals(preferences.getString(Names.VOLTAGE_MAIN + car_id, "")) &&
                    reserved.equals(preferences.getString(Names.VOLTAGE_RESERVED + car_id, "")))
                return;
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(Names.VOLTAGE_MAIN + car_id, main);
            ed.putString(Names.VOLTAGE_RESERVED + car_id, reserved);
            ed.commit();
            sendUpdate(ACTION_UPDATE, car_id);
        }

        @Override
        void exec(String api_key) {
            long eventTime = preferences.getLong(Names.LAST_EVENT + car_id, 0);
            execute(VOLTAGE_URL, api_key,
                    (eventTime - 24 * 60 * 60 * 1000) + "",
                    eventTime + "");
        }
    }

    class GsmRequest extends ServerRequest {

        GsmRequest(String id) {
            super("M", id);
        }

        @Override
        void background(JSONObject res) throws JSONException {
            if (res == null)
                return;
            JSONArray arr = res.getJSONArray("gsmlist");
            if (arr.length() == 0)
                return;
            JSONObject value = arr.getJSONObject(0);
            int cc = value.getInt("cc");
            int nc = value.getInt("nc");
            int cid = value.getInt("cid");
            int lac = value.getInt("lac");
            String gsm = cc + " " + nc + " " + lac + " " + cid;
            if (gsm.equals(preferences.getString(Names.GSM + car_id, "")) &&
                    !preferences.getString(Names.GSM_ZONE + car_id, "").equals(""))
                return;
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(Names.GSM + car_id, gsm);
            ed.putString(Names.GSM_ZONE + car_id, "");
            ed.putString(Names.ADDRESS + car_id, "");
            ed.commit();
            sendUpdate(ACTION_UPDATE, car_id);
            new SectorRequest(car_id);
        }

        @Override
        void exec(String api_key) {
            long eventTime = preferences.getLong(Names.LAST_EVENT + car_id, 0);
            execute(GSM_URL, api_key,
                    (eventTime - 24 * 60 * 60 * 1000) + "",
                    eventTime + "");
        }
    }

    class SectorRequest extends ServerRequest {

        SectorRequest(String id) {
            super("Z", id);
        }

        @Override
        void background(JSONObject res) throws JSONException {
            if (res == null)
                return;
            JSONArray arr = res.getJSONArray("gps");
            if (arr.length() == 0)
                return;
            double max_lat = -180;
            double min_lat = 180;
            double max_lon = -180;
            double min_lon = 180;
            Vector<Point> P = new Vector<Point>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject point = arr.getJSONObject(i);
                try {
                    Point p = new Point();
                    p.x = point.getDouble("latitude");
                    p.y = point.getDouble("longitude");
                    if (p.x > max_lat)
                        max_lat = p.x;
                    if (p.x < min_lat)
                        min_lat = p.x;
                    if (p.y > max_lon)
                        max_lon = p.y;
                    if (p.y < min_lon)
                        min_lon = p.y;
                    P.add(p);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // ignore
                }
            }
            double lat = (min_lat + max_lat) / 2;
            double lon = (min_lon + max_lon) / 2;
            new Address.Request(FetchService.this, car_id, lat + "", lon + "");
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(Names.GSM_ZONE + car_id, convexHull(P));
            ed.commit();
            sendUpdate(ACTION_UPDATE, car_id);
        }

        @Override
        void exec(String api_key) {
            String[] sector = preferences.getString(Names.GSM + car_id, "").split(" ");
            if (sector.length < 4)
                return;
            execute(SECTOR_URL, api_key, sector[0], sector[1], sector[2], sector[3]);
        }
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

    static class Point {
        double x;
        double y;
    }

    static String convexHull(Vector<Point> P) {
        Collections.sort(P, new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                if (lhs.x < rhs.x)
                    return -1;
                if (lhs.x > rhs.x)
                    return 1;
                if (lhs.y < rhs.y)
                    return -1;
                if (lhs.y > rhs.y)
                    return 1;
                return 0;
            }
        });

        int bot = 0;

        // Get the indices of points with min x-coord and min|max y-coord
        int minmin = 0;
        int minmax;

        double xmin = P.get(0).x;
        int i;
        for (i = 1; i < P.size(); i++)
            if (P.get(i).x != xmin) break;
        minmax = i - 1;

        Vector<Point> H = new Vector<Point>();

        if (minmax == P.size() - 1) {       // degenerate case: all x-coords == xmin
            H.add(P.get(minmin));
            if (P.get(minmax).y != P.get(minmin).y) // a nontrivial segment
                H.add(P.get(minmax));
            H.add(P.get(minmin));           // add polygon endpoint
        } else {

            // Get the indices of points with max x-coord and min|max y-coord
            int maxmin;
            int maxmax = P.size() - 1;
            double xmax = P.get(P.size() - 1).x;
            for (i = P.size() - 2; i >= 0; i--)
                if (P.get(i).x != xmax) break;
            maxmin = i + 1;

            // Compute the lower hull on the stack H
            H.add(P.get(minmin));      // push minmin point onto stack
            i = minmax;
            while (++i <= maxmin) {
                // the lower line joins P[minmin] with P[maxmin]
                if (isLeft(P.get(minmin), P.get(maxmin), P.get(i)) >= 0 && i < maxmin)
                    continue;          // ignore P[i] above or on the lower line

                while (H.size() > 1)        // there are at least 2 points on the stack
                {
                    // test if P[i] is left of the line at the stack top
                    int top = H.size() - 1;
                    if (isLeft(H.get(top - 1), H.get(top), P.get(i)) > 0)
                        break;         // P[i] is a new hull vertex
                    H.remove(top);     // pop top point off stack
                }
                H.add(P.get(i));            // push P[i] onto stack
            }

            // Next, compute the upper hull on the stack H above the bottom hull
            if (maxmax != maxmin)      // if distinct xmax points
                H.add(P.get(maxmax));      // push maxmax point onto stack
            bot = H.size() - 1;        // the bottom point of the upper hull stack
            i = maxmin;
            while (--i >= minmax) {
                // the upper line joins P[maxmax] with P[minmax]
                if (isLeft(P.get(maxmax), P.get(minmax), P.get(i)) >= 0 && i > minmax)
                    continue;          // ignore P[i] below or on the upper line

                while (H.size() - 1 > bot)    // at least 2 points on the upper stack
                {
                    // test if P[i] is left of the line at the stack top
                    int top = H.size() - 1;
                    if (isLeft(H.get(top - 1), H.get(top), P.get(i)) > 0)
                        break;         // P[i] is a new hull vertex
                    H.remove(top);
                }
                H.add(P.get(i));       // push P[i] onto stack
            }
            if (minmax != minmin)
                H.add(P.get(minmin));  // push joining endpoint onto stack
        }
        String res = null;
        for (Point p : H) {
            String part = p.x + "," + p.y;
            if (res == null) {
                res = part;
            } else {
                res += "_" + part;
            }
        }
        return res;
    }

    static double isLeft(Point P0, Point P1, Point P2) {
        return (P1.x - P0.x) * (P2.y - P0.y) - (P2.x - P0.x) * (P1.y - P0.y);
    }

}
