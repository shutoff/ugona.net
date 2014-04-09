package net.ugona.plus;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CarProvider extends ContentProvider {

    static final String AUTHORITY = "net.ugona.plus";

    static final int URI_CARS = 1;
    static final int URI_CARS_ID = 2;

    static final String CAR_PATH = "car";

    static final String CARS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
            + AUTHORITY + "." + CAR_PATH;

    static final String CARS_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
            + AUTHORITY + "." + CAR_PATH;

    static final int TYPE_STRING = 0;
    static final int TYPE_ID = 1;
    static final int TYPE_BOOL = 2;
    static final int TYPE_LONG = 3;
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, CAR_PATH, URI_CARS);
        uriMatcher.addURI(AUTHORITY, CAR_PATH + "/#", URI_CARS_ID);
    }

    static Map<String, Integer> field_types;

    @Override
    public boolean onCreate() {
        field_types = new HashMap<String, Integer>();
        field_types.put(Names.ID, TYPE_ID);
        field_types.put(Names.Car.CAR_NAME, TYPE_STRING);
        field_types.put(Names.Car.EVENT_TIME, TYPE_LONG);
        field_types.put(Names.Car.ENGINE, TYPE_BOOL);
        field_types.put(Names.Car.LAST_EVENT, TYPE_LONG);
        field_types.put(Names.Car.LAST_STAND, TYPE_LONG);
        field_types.put(Names.Car.GUARD, TYPE_BOOL);
        field_types.put(Names.Car.GUARD0, TYPE_BOOL);
        field_types.put(Names.Car.GUARD1, TYPE_BOOL);
        field_types.put(Names.Car.INPUT1, TYPE_BOOL);
        field_types.put(Names.Car.INPUT2, TYPE_BOOL);
        field_types.put(Names.Car.INPUT3, TYPE_BOOL);
        field_types.put(Names.Car.INPUT4, TYPE_BOOL);
        field_types.put(Names.Car.ZONE_DOOR, TYPE_BOOL);
        field_types.put(Names.Car.ZONE_HOOD, TYPE_BOOL);
        field_types.put(Names.Car.ZONE_TRUNK, TYPE_BOOL);
        field_types.put(Names.Car.ZONE_ACCESSORY, TYPE_BOOL);
        field_types.put(Names.Car.ZONE_IGNITION, TYPE_BOOL);
        field_types.put(Names.Car.RELAY1, TYPE_BOOL);
        field_types.put(Names.Car.RELAY2, TYPE_BOOL);
        field_types.put(Names.Car.RELAY3, TYPE_BOOL);
        field_types.put(Names.Car.RELAY4, TYPE_BOOL);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (projection == null) {
            projection = new String[field_types.size()];
            Set<String> names = field_types.keySet();
            int i = 0;
            for (String name : names) {
                projection[i] = name;
            }
        }
        MatrixCursor cursor = new MatrixCursor(projection);
        switch (uriMatcher.match(uri)) {
            case URI_CARS:
                Cars.Car[] cars = Cars.getCars(getContext());
                for (Cars.Car car : cars) {
                    fill(cursor, projection, car.id);
                }
                break;
            case URI_CARS_ID:
                fill(cursor, projection, uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        return cursor;
    }

    void fill(MatrixCursor cursor, String[] fields, String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        Object[] row = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String name = fields[i];
            Integer type = field_types.get(name);
            if (type == null)
                type = TYPE_STRING;
            switch (type) {
                case TYPE_ID:
                    row[i] = car_id;
                    break;
                case TYPE_BOOL:
                    row[i] = preferences.getBoolean(name + car_id, false);
                    break;
                case TYPE_LONG:
                    row[i] = preferences.getLong(name + car_id, 0);
                    break;
                default:
                    try {
                        row[i] = preferences.getString(name + car_id, "");
                    } catch (Exception ex) {
                        try {
                            row[i] = preferences.getBoolean(name + car_id, false);
                        } catch (Exception ex1) {
                            try {
                                row[i] = preferences.getInt(name + car_id, 0);
                            } catch (Exception ex2) {
                                // // ignore
                            }
                        }
                    }
            }
        }
        cursor.addRow(row);
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case URI_CARS:
                return CARS_CONTENT_TYPE;
            case URI_CARS_ID:
                return CARS_CONTENT_ITEM_TYPE;
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
