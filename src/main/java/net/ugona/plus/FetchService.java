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

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

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

    static final String STATUS_URL = "http://dev.car-online.ru/api/v2?get=lastinfo&skey=$1&content=json";
    private static final String EVENTS_URL = "http://dev.car-online.ru/api/v2?get=events&skey=$1&begin=$2&end=$3&content=json";
    private static final String TEMP_URL = "http://dev.car-online.ru/api/v2?get=temperaturelist&skey=$1&begin=$2&end=$3&content=json";
    private static final String VOLTAGE_URL = "http://dev.car-online.ru/api/v2?get=voltagelist&skey=$1&begin=$2&end=$3&content=json";
    private static final String GSM_URL = "http://dev.car-online.ru/api/v2?get=gsmlist&skey=$1&begin=$2&end=$3&content=json";
    private static final String GPS_URL = "http://dev.car-online.ru/api/v2?get=gps&skey=$1&id=$2&time=$3&content=json";
    private static final String SECTOR_URL = "http://dev.car-online.ru/api/v2?get=gsmsector&skey=$1&cc=$2&nc=$3&lac=$4&cid=$5&content=json";
    private static final String BALANCE_URL = "http://dev.car-online.ru/api/v2?get=balancelist&skey=$1&begin=$2&content=json";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
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
        if (startRequest())
            return START_STICKY;
        return START_NOT_STICKY;
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
        stopSelf();
        return false;
    }

    static private Map<String, ServerRequest> requests;

    static boolean isProcessed(String id) {
        if (requests == null)
            return false;
        return requests.containsKey("S" + id);
    }

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
        void result(JsonObject res) throws ParseException {
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
        void background(JsonObject res) throws ParseException {
            JsonObject event = res.get("event").asObject();
            long eventId = event.get("eventId").asLong();

            if (eventId == preferences.getLong(Names.EVENT_ID + car_id, 0)) {
                sendUpdate(ACTION_NOUPDATE, car_id);
                if (preferences.getBoolean(Names.NOSLEEP_MODE + car_id, false)) {
                    alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, SAFEMODE_TIMEOUT, SAFEMODE_TIMEOUT, pi);
                }
                return;
            }

            long eventTime = event.get("eventTime").asLong();
            ed = preferences.edit();
            ed.putLong(Names.EVENT_ID + car_id, eventId);
            ed.putLong(Names.EVENT_TIME + car_id, eventTime);

            boolean voltage_request = true;
            JsonValue voltage_value = res.get("voltage");
            if (voltage_value != null) {
                voltage_request = false;
                JsonObject voltage = voltage_value.asObject();
                ed.putString(Names.VOLTAGE_MAIN + car_id, voltage.get("main").asDouble() + "");
                ed.putString(Names.VOLTAGE_RESERVED + car_id, voltage.get("reserved").asDouble() + "");
            }
            JsonValue temp_value = res.get("temperature");
            if (temp_value != null) {
                JsonObject temp = temp_value.asObject();
                ed.putString(Names.TEMPERATURE + car_id, temp.get("value").asInt() + "");
            }

            if (preferences.getLong(Names.BALANCE_TIME + car_id, 0) == 0) {
                JsonObject balance = res.get("balance").asObject();
                Matcher m = balancePattern.matcher(balance.get("source").asString());
                if (m.find())
                    ed.putString(Names.BALANCE + car_id, m.group(0).replaceAll(",", "."));
            }

            JsonObject contact = res.get("contact").asObject();
            boolean guard = contact.get("guard").asBoolean();
            ed.putBoolean(Names.GUARD + car_id, guard);
            ed.putBoolean(Names.INPUT1 + car_id, contact.get("input1").asBoolean());
            ed.putBoolean(Names.INPUT2 + car_id, contact.get("input2").asBoolean());
            ed.putBoolean(Names.INPUT3 + car_id, contact.get("input3").asBoolean());
            ed.putBoolean(Names.INPUT4 + car_id, contact.get("input4").asBoolean());
            ed.putBoolean(Names.GUARD0 + car_id, contact.get("guardMode0").asBoolean());
            ed.putBoolean(Names.GUARD1 + car_id, contact.get("guardMode1").asBoolean());
            setState(Names.ZONE_DOOR, contact, "door", 3);
            setState(Names.ZONE_HOOD, contact, "hood", 2);
            setState(Names.ZONE_TRUNK, contact, "trunk", 1);
            setState(Names.ZONE_ACCESSORY, contact, "accessory", 7);
            setState(Names.ZONE_IGNITION, contact, "realIgnition", 4);
            boolean engine = contact.get("engine").asBoolean();
            if (engine && (msg_id == 4))
                msg_id = 0;
            ed.putBoolean(Names.ENGINE + car_id, engine);

            boolean gsm_req = true;
            JsonValue gps_value = res.get("gps");
            if (gps_value != null) {
                gsm_req = false;
                JsonObject gps = gps_value.asObject();
                ed.putString(Names.LATITUDE + car_id, gps.get("latitude").asDouble() + "");
                ed.putString(Names.LONGITUDE + car_id, gps.get("longitude").asDouble() + "");
                ed.putString(Names.SPEED + car_id, gps.get("speed").asDouble() + "");
                if (contact.get("gpsValid").asBoolean())
                    ed.putString(Names.COURSE + car_id, gps.get("course").asInt() + "");
            }


            boolean sms_alarm = preferences.getBoolean(Names.SMS_ALARM, false);
            if (sms_alarm)
                ed.remove(Names.SMS_ALARM);

            ed.commit();
            sendUpdate(ACTION_UPDATE, car_id);

            if (preferences.getBoolean(Names.NOSLEEP_MODE + car_id, false)) {
                if (!sms_alarm && (msg_id > 0) && guard) {
                    Intent alarmIntent = new Intent(FetchService.this, Alarm.class);
                    alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    alarmIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    String[] alarms = getString(R.string.alarm).split("\\|");
                    alarmIntent.putExtra(Names.ALARM, alarms[msg_id]);
                    alarmIntent.putExtra(Names.ID, car_id);
                    FetchService.this.startActivity(alarmIntent);
                }
                alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, SAFEMODE_TIMEOUT, SAFEMODE_TIMEOUT, pi);
            }

            new EventsRequest(car_id, voltage_request, gsm_req);

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

        void setState(String id, JsonObject contact, String key, int msg) throws ParseException {
            boolean state = contact.get(key).asBoolean();
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
        void background(JsonObject res) throws ParseException {
            if (res == null)
                return;
            JsonArray balances = res.get("balanceList").asArray();
            if (balances.size() == 0)
                return;
            JsonObject balance = balances.get(0).asObject();
            String data = balance.get("value").asString();
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
        void background(JsonObject res) throws ParseException {
            if (res == null)
                return;

            JsonArray events = res.get("events").asArray();
            if (events.size() > 0) {
                long last_stand = preferences.getLong(Names.LAST_STAND, 0);
                long stand = last_stand;
                long event_id = 0;
                for (int i = events.size() - 1; i >= 0; i--) {
                    JsonObject event = events.get(i).asObject();
                    int type = event.get("eventType").asInt();
                    switch (type) {
                        case 37:
                            last_stand = -event.get("eventTime").asLong();
                            break;
                        case 38:
                            last_stand = event.get("eventTime").asLong();
                            event_id = event.get("eventId").asLong();
                            break;
                    }
                }
                if ((event_id > 0) && !preferences.getString(Names.LATITUDE + car_id, "").equals(""))
                    new GPSRequest(car_id, event_id, last_stand);
                boolean changed = false;
                SharedPreferences.Editor ed = preferences.edit();
                ed.putLong(Names.LAST_EVENT + car_id, eventTime);
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

    class VoltageRequest extends ServerRequest {

        VoltageRequest(String id) {
            super("V", id);
        }

        @Override
        void background(JsonObject res) throws ParseException {
            if (res == null)
                return;
            JsonArray arr = res.get("voltageList").asArray();
            if (arr.size() == 0)
                return;
            JsonObject value = arr.get(0).asObject();
            String main = value.get("main").asDouble() + "";
            String reserved = value.get("reserved").asDouble() + "";
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

    class GPSRequest extends ServerRequest {

        final String event_id;
        final String event_time;

        GPSRequest(String id, long eventId, long eventTime) {
            super("G", id);
            event_id = eventId + "";
            event_time = eventTime + "";
        }

        @Override
        void background(JsonObject res) throws ParseException {
            if (res == null)
                return;
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(Names.COURSE + car_id, res.get("course").asInt() + "");
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

    class GsmRequest extends ServerRequest {

        GsmRequest(String id) {
            super("M", id);
        }

        @Override
        void background(JsonObject res) throws ParseException {
            if (res == null)
                return;
            JsonArray arr = res.get("gsmlist").asArray();
            if (arr.size() == 0)
                return;
            JsonObject value = arr.get(0).asObject();
            int cc = value.get("cc").asInt();
            int nc = value.get("nc").asInt();
            int cid = value.get("cid").asInt();
            int lac = value.get("lac").asInt();
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
        void background(JsonObject res) throws ParseException {
            if (res == null)
                return;
            JsonArray arr = res.get("gps").asArray();
            if (arr.size() == 0)
                return;
            double max_lat = -180;
            double min_lat = 180;
            double max_lon = -180;
            double min_lon = 180;
            Vector<Point> P = new Vector<Point>();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject point = arr.get(i).asObject();
                try {
                    Point p = new Point();
                    p.x = point.get("latitude").asDouble();
                    p.y = point.get("longitude").asDouble();
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
