package net.ugona.plus;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.util.Locale;

public abstract class Address {

    abstract void result(String address);

    static SQLiteDatabase address_db;
    final static String TABLE_NAME = "address";

    class OpenHelper extends SQLiteOpenHelper {

        final static String DB_NAME = "address.db";

        final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                + "ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "Lat REAL NOT NULL, "
                + "Lng REAL NOT NULL, "
                + "Address TEXT NOT NULL, "
                + "Param TEXT NOT NULL)";

        public OpenHelper(Context context) {
            super(context, DB_NAME, null, 2);
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

    void get(final Context context, final double v_lat, final double v_lng) {

        if (address_db == null) {
            OpenHelper helper = new OpenHelper(context);
            address_db = helper.getWritableDatabase();
        }

        final double lat = Math.round(v_lat * 100000.) / 100000.;
        final double lng = Math.round(v_lng * 100000.) / 100000.;

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String p = Locale.getDefault().getLanguage();
        if (preferences.getString(Names.MAP_TYPE, "").equals("OSM"))
            p += "_";
        final String param = p;
        final String[] columns = {
                "Lat",
                "Lng",
                "Address",
                "Param",
        };

        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {

                String[] conditions = {
                        param,
                        (lat - 0.001) + "",
                        (lat + 0.001) + "",
                        (lng - 0.001) + "",
                        (lng + 0.001) + ""
                };

                Cursor cursor = address_db.query(TABLE_NAME, columns, "(Param = ?) AND (Lat BETWEEN ? AND ?) AND (Lng BETWEEN ? AND ?)", conditions, null, null, null, null);
                if (cursor.moveToFirst()) {
                    for (; ; ) {
                        double db_lat = cursor.getDouble(0);
                        double db_lon = cursor.getDouble(1);
                        double distance = calc_distance(lat, lng, db_lat, db_lon);
                        if (distance < 80) {
                            String address = cursor.getString(2);
                            return address;
                        }
                        if (!cursor.moveToNext())
                            break;
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                if (s != null) {
                    result(s);
                    return;
                }
                AddressRequest request = new AddressRequest() {
                    @Override
                    void addressResult(String address) {
                        if (address != null) {
                            ContentValues values = new ContentValues();
                            values.put(columns[0], lat);
                            values.put(columns[1], lng);
                            values.put(columns[2], address);
                            values.put(columns[3], param);
                            address_db.insert(TABLE_NAME, null, values);
                        }
                        result(address);
                    }
                };
                request.getAddress(preferences, lat, lng);
            }
        };
        task.execute();

    }

    static String getAddress(Context context, final double lat, final double lng) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String p = Locale.getDefault().getLanguage();
        if (preferences.getString(Names.MAP_TYPE, "").equals("OSM"))
            p += "_";
        final String param = p;
        final String[] columns = {
                "Lat",
                "Lng",
                "Address",
                "Param",
        };

        String[] conditions = {
                param,
                (lat - 0.001) + "",
                (lat + 0.001) + "",
                (lng - 0.001) + "",
                (lng + 0.001) + ""
        };
        Cursor cursor = address_db.query(TABLE_NAME, columns, "(Param = ?) AND (Lat BETWEEN ? AND ?) AND (Lng BETWEEN ? AND ?)", conditions, null, null, null, null);
        if (cursor.moveToFirst()) {
            for (; ; ) {
                double db_lat = cursor.getDouble(0);
                double db_lon = cursor.getDouble(1);
                double distance = calc_distance(lat, lng, db_lat, db_lon);
                if (distance < 80) {
                    String address = cursor.getString(2);
                    return address;
                }
                if (!cursor.moveToNext())
                    break;
            }
        }
        return null;
    }

    static final double D2R = 0.017453; // Константа для преобразования градусов в радианы
    static final double a = 6378137.0; // Основные полуоси
    static final double e2 = 0.006739496742337; // Квадрат эксцентричности эллипсоида

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

}
