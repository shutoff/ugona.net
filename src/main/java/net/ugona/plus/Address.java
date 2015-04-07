package net.ugona.plus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

public abstract class Address {

    final static String TABLE_NAME = "address";

    static final String[] columns = {
            "Lat",
            "Lng",
            "Address",
            "Param",
            "Time"
    };

    static final String OSM_URL = "https://nominatim.openstreetmap.org/reverse?lat=$1&lon=$2&osm_type=N&format=json&address_details=0&accept-language=$3";
    static final String GOOGLE_URL = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$1,$2&sensor=false&language=$3";

    static Map<String, Vector<Answer>> requests = new HashMap<>();
    static DbHelper dbHelper = null;

    static void get(Context context, final double v_lat, final double v_lng, final Answer answer) {
        AppConfig config = AppConfig.get(context);
        get(context, v_lat, v_lng, config.getMap_type(), answer);
    }

    static void get(Context context, final double v_lat, final double v_lng, String type, final Answer answer) {
        if ((v_lat == 0) && (v_lng == 0))
            return;

        if (dbHelper == null)
            dbHelper = new DbHelper(context);

        AddressParam param = new AddressParam();
        param.lang = Locale.getDefault().getLanguage();
        param.lat = Math.round(v_lat * 100000.) / 100000.;
        param.lon = Math.round(v_lng * 100000.) / 100000.;

        param.type = type.equals("OSM") ? '_' : 'g';

        final String key = param.lat + "," + param.lon + "," + param.lang + param.type;
        Vector<Answer> queue = requests.get(key);
        if (queue != null) {
            queue.add(answer);
            return;
        }
        queue = new Vector<>();
        queue.add(answer);
        requests.put(key, queue);

        AsyncTask<AddressParam, Void, String> task = new AsyncTask<AddressParam, Void, String>() {
            @Override
            protected String doInBackground(AddressParam... params) {
                AddressParam p = params[0];
                String[] conditions = {
                        p.lang + p.type,
                        (p.lat - 0.001) + "",
                        (p.lat + 0.001) + "",
                        (p.lon - 0.001) + "",
                        (p.lon + 0.001) + ""
                };
                int now = (int) (new Date().getTime() / 864000);
                int limit = now - 30;
                double best = 400;
                String result = null;
                Cursor cursor = dbHelper.getReadableDatabase().query(TABLE_NAME, columns, "(Param = ?) AND (Lat BETWEEN ? AND ?) AND (Lng BETWEEN ? AND ?)", conditions, null, null, null, null);
                if (cursor.moveToFirst()) {
                    for (; ; ) {
                        double db_lat = cursor.getDouble(0);
                        double db_lon = cursor.getDouble(1);
                        int time = cursor.getInt(4);
                        if (time < limit) {
                            dbHelper.getWritableDatabase().delete(TABLE_NAME, "(Param = ?) AND (Lat=?) AND (Lng=?)",
                                    new String[]{p.lang + p.type, db_lat + "", db_lon + ""});
                            continue;
                        }
                        double distance = State.distance(p.lat, p.lon, db_lat, db_lon);
                        if (distance < best) {
                            result = cursor.getString(2);
                            best = distance;
                        }
                        if (!cursor.moveToNext())
                            break;
                    }
                }
                cursor.close();
                if (best < 80)
                    return result;
                try {
                    if (p.type == 'g') {
                        for (; ; ) {
                            JsonObject data = HttpTask.request(GOOGLE_URL, p.lat, p.lon, p.lang);
                            JsonArray res = data.get("results").asArray();
                            if (res.size() == 0) {
                                String status = data.get("status").asString();
                                if ((status != null) && status.equals("OVER_QUERY_LIMIT")) {
                                    Thread.sleep(2000);
                                    continue;
                                }
                            }
                            if (res.size() == 0)
                                return null;
                            int i;
                            for (i = 0; i < res.size(); i++) {
                                JsonObject addr = res.get(i).asObject();
                                JsonArray types = addr.get("types").asArray();
                                int n;
                                for (n = 0; n < types.size(); n++) {
                                    if (types.get(n).asString().equals("street_address"))
                                        break;
                                }
                                if (n < types.size())
                                    break;
                            }
                            if (i >= res.size())
                                i = 0;
                            JsonObject addr = res.get(i).asObject();
                            String[] parts = addr.get("formatted_address").asString().split(", ");
                            JsonArray components = addr.get("address_components").asArray();
                            String house = null;
                            String postalCode = null;
                            for (i = 0; i < components.size(); i++) {
                                int n;
                                JsonObject component = components.get(i).asObject();
                                JsonArray types = component.get("types").asArray();
                                for (n = 0; n < types.size(); n++) {
                                    String type = types.get(n).asString();
                                    if (type.equals("postal_code"))
                                        postalCode = component.get("long_name").asString();
                                    if (type.equals("street_number"))
                                        house = component.get("long_name").asString();
                                }
                            }
                            for (i = 0; i < parts.length; i++) {
                                String part = parts[i];
                                if (part.equals(postalCode)) {
                                    parts[i] = null;
                                    continue;
                                }
                                if (part.equals(house)) {
                                    parts[i] = null;
                                    parts[i - 1] += ",\u00A0" + house;
                                    continue;
                                }
                                if (part.equals("Unnamed Road")) {
                                    parts[i] = null;
                                    continue;
                                }
                            }
                            result = null;
                            for (i = 0; i < parts.length; i++) {
                                if (parts[i] == null)
                                    continue;
                                if (result == null) {
                                    result = parts[i];
                                    continue;
                                }
                                result += ", " + parts[i];
                            }
                            break;
                        }
                    } else {
                        JsonObject res = HttpTask.request(OSM_URL, p.lat, p.lon, p.lang);
                        JsonObject address = res.get("address").asObject();
                        String[] parts = res.get("display_name").asString().split(", ");
                        try {
                            String house_number = address.get("house_number").asString();
                            for (int i = 0; i < parts.length - 1; i++) {
                                if (parts[i].equals(house_number)) {
                                    parts[i + 1] += ",\u00A0" + house_number;
                                    parts[i] = null;
                                    break;
                                }
                            }
                        } catch (Exception ex) {
                            // ignore
                        }
                        result = null;
                        for (int i = 0; i < parts.length - 2; i++) {
                            if (parts[i] == null)
                                continue;
                            if (result == null) {
                                result = parts[i];
                                continue;
                            }
                            result += ", " + parts[i];
                        }
                    }

                    String[] parts = result.split(", ");
                    result = parts[0];
                    for (int i = 1; i < parts.length; i++) {
                        String part = parts[i];
                        if (part.equals(""))
                            continue;
                        result += ",";
                        part = setA0(part);
                        if ((i > 1) && (part.length() < 6)) {
                            result += "\u00A0";
                            result += part.replaceAll(" ", "\u00A0");
                            continue;
                        }
                        result += " " + part;
                    }

                    ContentValues values = new ContentValues();
                    values.put(columns[0], p.lat);
                    values.put(columns[1], p.lon);
                    values.put(columns[2], result);
                    values.put(columns[3], p.lang + p.type);
                    values.put(columns[4], now);
                    dbHelper.getWritableDatabase().insert(TABLE_NAME, null, values);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
                return result;
            }

            @Override
            protected void onPostExecute(String s) {
                Vector<Answer> answers = requests.remove(key);
                if (answers == null)
                    return;
                for (Answer a : answers) {
                    a.result(s);
                }

            }
        };
        task.execute(param);
    }

    static String setA0(String s) {
        String[] parts = s.split(" ");
        String res = parts[0];
        for (int i = 1; i < parts.length; i++) {
            if (parts[i - 1].length() < 5) {
                res += "\u00A0";
            } else {
                res += " ";
            }
            res += parts[i];
        }
        return res;
    }

    public interface Answer {
        abstract void result(String address);
    }

    static class AddressParam {
        double lat;
        double lon;
        String lang;
        char type;
    }

    static class DbHelper extends SQLiteOpenHelper {

        final static String DB_NAME = "address.db";

        final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                + "ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "Lat REAL NOT NULL, "
                + "Lng REAL NOT NULL, "
                + "Address TEXT NOT NULL, "
                + "Time INTEGER NOT NULL, "
                + "Param TEXT NOT NULL)";

        public DbHelper(Context context) {
            super(context, DB_NAME, null, 45);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE);
            db.execSQL("CREATE INDEX \"index_lng\" on address (Lng ASC)");
            db.execSQL("CREATE INDEX \"index_lat\" on address (Lat ASC)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS address");
            onCreate(db);
        }


    }

}
