package net.ugona.plus;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import java.util.Locale;

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
    };

    static SQLiteDatabase address_db;

    static String get(Context context, final double v_lat, final double v_lng, final Answer answer) {
        return get(context, v_lat, v_lng, answer, false);
    }

    static String get(Context context, final double v_lat, final double v_lng, final Answer answer, final boolean async) {
        if ((v_lat == 0) && (v_lng == 0))
            return null;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String p = Locale.getDefault().getLanguage();
        if (preferences.getString(Names.MAP_TYPE, "").equals("OSM"))
            p += "_";
        if (preferences.getString(Names.MAP_TYPE, "").equals("Bing"))
            p += "a";
        if (preferences.getString(Names.MAP_TYPE, "").equals("Yandex"))
            p += "y";
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
            OpenHelper helper = new OpenHelper(context);
            address_db = helper.getWritableDatabase();
        }

        String result = null;
        Cursor cursor = address_db.query(TABLE_NAME, columns, "(Param = ?) AND (Lat BETWEEN ? AND ?) AND (Lng BETWEEN ? AND ?)", conditions, null, null, null, null);
        double best = 400;
        if (cursor.moveToFirst()) {
            for (; ; ) {
                double db_lat = cursor.getDouble(0);
                double db_lon = cursor.getDouble(1);
                double distance = calc_distance(lat, lng, db_lat, db_lon);
                if (distance < best) {
                    result = cursor.getString(2);
                    best = distance;
                }
                if (!cursor.moveToNext())
                    break;
            }
        }
        cursor.close();
        if (best > 80) {
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
                        ContentValues values = new ContentValues();
                        values.put(columns[0], lat);
                        values.put(columns[1], lng);
                        values.put(columns[2], address);
                        values.put(columns[3], param);
                        address_db.insert(TABLE_NAME, null, values);
                        if (answer != null)
                            answer.result(address);
                        return;
                    }
                    if (async)
                        answer.result(null);
                }
            };
            request.getAddress(preferences, lat, lng);
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

    static double calc_distance(double lat1, double lon1, double lat2, double lon2) {

        if ((lat1 == lat2) && (lon1 == lon2))
            return 0;

        double fdLambda = (lon1 - lon2) * D2R;
        double fdPhi = (lat1 - lat2) * D2R;
        double fPhimean = ((lat1 + lat2) / 2.0) * D2R;

        double fTemp = 1 - e2 * (Math.pow(Math.sin(fPhimean), 2));
        double fRho = (a * (1 - e2)) / Math.pow(fTemp, 1.5);
        double fNu = a / (Math.sqrt(1 - e2 * (Math.sin(fPhimean) * Math.sin(fPhimean))));

        double fz = Math.sqrt(Math.pow(Math.sin(fdPhi / 2.0), 2) +
                Math.cos(lat2 * D2R) * Math.cos(lat1 * D2R) * Math.pow(Math.sin(fdLambda / 2.0), 2));
        fz = 2 * Math.asin(fz);

        double fAlpha = Math.cos(lat1 * D2R) * Math.sin(fdLambda) * 1 / Math.sin(fz);
        fAlpha = Math.asin(fAlpha);

        double fR = (fRho * fNu) / ((fRho * Math.pow(Math.sin(fAlpha), 2)) + (fNu * Math.pow(Math.cos(fAlpha), 2)));

        return fz * fR;
    }

    public interface Answer {
        abstract void result(String address);
    }

    static class OpenHelper extends SQLiteOpenHelper {

        final static String DB_NAME = "address.db";

        public OpenHelper(Context context) {
            super(context, DB_NAME, null, 32);
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

        final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                + "ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "Lat REAL NOT NULL, "
                + "Lng REAL NOT NULL, "
                + "Address TEXT NOT NULL, "
                + "Param TEXT NOT NULL)";


    }

}
