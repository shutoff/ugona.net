package net.ugona.plus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

public abstract class Address {

    final static String TABLE_NAME = "address";
    static final double D2R = 0.017453; // Константа для преобразования градусов в радианы
    static final double a = 6378137.0; // Основные полуоси
    static final double e2 = 0.006739496742337; // Квадрат эксцентричности эллипсоида
    static final String[] columns = {
            "Lat",
            "Lng",
            "Address",
            "Param",
            "Time"
    };

    static SQLiteDatabase address_db;
    static Map<String, Vector<AnswerQueue>> requests = new HashMap<>();

    static String get(Context context, final double v_lat, final double v_lng, final Answer answer) {
        return get(context, v_lat, v_lng, answer, false);
    }

    static String get(Context context, final double v_lat, final double v_lng, final Answer answer, final boolean async) {
        if ((v_lat == 0) && (v_lng == 0))
            return null;
        String p = Locale.getDefault().getLanguage();
        AppConfig config = AppConfig.get(context);
        String map_type = config.getMap_type();
        if (map_type.equals("OSM"))
            p += "_";
        final String param = p;

        final double lat = Math.round(v_lat * 100000.) / 100000.;
        final double lng = Math.round(v_lng * 100000.) / 100000.;

        String[] conditions = {
                param,
                (lat - 0.001) + "",
                (lat + 0.001) + "",
                (lng - 0.001) + "",
                (lng + 0.001) + ""
        };
        if (address_db == null) {
            try {
                OpenHelper helper = new OpenHelper(context);
                address_db = helper.getWritableDatabase();
            } catch (Exception ex) {
            }
        }

        String result = null;
        double best = 400;
        if (address_db != null) {
            int limit = (int) (new Date().getTime() / 864000) - 30;
            Cursor cursor = address_db.query(TABLE_NAME, columns, "(Param = ?) AND (Lat BETWEEN ? AND ?) AND (Lng BETWEEN ? AND ?)", conditions, null, null, null, null);
            if (cursor.moveToFirst()) {
                for (; ; ) {
                    double db_lat = cursor.getDouble(0);
                    double db_lon = cursor.getDouble(1);
                    int time = cursor.getInt(4);
                    if (time < limit) {
                        address_db.delete(TABLE_NAME, "(Param = ?) AND (Lat=?) AND (Lng=?)",
                                new String[]{param, db_lat + "", db_lon + ""});
                        continue;
                    }
                    double distance = State.distance(lat, lng, db_lat, db_lon);
                    if (distance < best) {
                        result = cursor.getString(2);
                        best = distance;
                    }
                    if (!cursor.moveToNext())
                        break;
                }
            }
            cursor.close();
        }
        if ((best > 80) && (answer != null)) {
            final String key = lat + "," + lng + "," + param;
            if (!requests.containsKey(key)) {
                requests.put(key, new Vector<AnswerQueue>());
                AddressRequest request = new AddressRequest() {
                    @Override
                    void addressResult(String address) {
                        if (address != null) {
                            String[] parts = address.split(", ");
                            address = parts[0];
                            for (int i = 1; i < parts.length; i++) {
                                String p = parts[i];
                                if (p.equals(""))
                                    continue;
                                address += ",";
                                p = setA0(p);
                                if ((i > 1) && (p.length() < 6)) {
                                    address += "\u00A0";
                                    address += p.replaceAll(" ", "\u00A0");
                                    continue;
                                }
                                address += " " + p;
                            }
                            if (address_db != null) {
                                ContentValues values = new ContentValues();
                                values.put(columns[0], lat);
                                values.put(columns[1], lng);
                                values.put(columns[2], address);
                                values.put(columns[3], param);
                                values.put(columns[4], (int) (new Date().getTime() / 864000));
                                address_db.insert(TABLE_NAME, null, values);
                            }
                        }
                        Vector<AnswerQueue> answerQueues = requests.get(key);
                        requests.remove(key);
                        for (AnswerQueue queue : answerQueues) {
                            if ((address != null) || queue.async)
                                queue.answer.result(address);
                        }
                    }
                };
                request.getAddress(map_type, lat, lng);
            }
            AnswerQueue answerQueue = new AnswerQueue();
            answerQueue.answer = answer;
            answerQueue.async = async;
            requests.get(key).add(answerQueue);
            if (async)
                return null;
        }
        if (answer != null)
            answer.result(result);
        return result;
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

    static class AnswerQueue {
        Answer answer;
        boolean async;
    }

    static class OpenHelper extends SQLiteOpenHelper {

        final static String DB_NAME = "address.db";
        final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                + "ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "Lat REAL NOT NULL, "
                + "Lng REAL NOT NULL, "
                + "Address TEXT NOT NULL, "
                + "Time INTEGER NOT NULL, "
                + "Param TEXT NOT NULL)";

        public OpenHelper(Context context) {
            super(context, DB_NAME, null, 42);
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
