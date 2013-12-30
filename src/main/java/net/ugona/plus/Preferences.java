package net.ugona.plus;

import android.content.SharedPreferences;

import java.util.Date;

public class Preferences {

    static String getCar(SharedPreferences preferences, String car_id) {
        String[] cars = preferences.getString(Names.CARS, "").split(",");
        if (car_id != null) {
            boolean car_ok = false;
            for (String car : cars) {
                if (car.equals(car_id)) {
                    car_ok = true;
                    break;
                }
                String pointers = preferences.getString(Names.POINTERS + car, "");
                if (pointers.equals(""))
                    continue;
                for (String p : pointers.split(",")) {
                    if (p.equals(car_id)) {
                        car_ok = true;
                        break;
                    }
                }
            }
            if (!car_ok)
                car_id = null;
        }
        if (car_id == null)
            car_id = cars[0];
        return car_id;
    }

    static String getTemperature(SharedPreferences preferences, String car_id, int sensor) {
        try {
            String key = Names.TEMPERATURE;
            if (sensor == 2)
                key = Names.TEMPERATURE2;
            if (sensor == 3)
                key = Names.TEMPERATURE3;
            String s = preferences.getString(key + car_id, "");
            if (s.length() == 0)
                return null;
            double v = Double.parseDouble(s);
            v += preferences.getInt(Names.TEMP_SIFT + car_id, 0);
            return v + " \u00B0C";
        } catch (Exception ex) {
        }
        return null;
    }

    static boolean getRele(SharedPreferences preferences, String car_id) {
        final boolean rele2 = preferences.getString(Names.CAR_RELE + car_id, "").equals("2");
        if (preferences.getBoolean(Names.RELE_IMPULSE + car_id, false)) {
            if (rele2)
                return preferences.getBoolean(Names.RELAY2 + car_id, false);
            return preferences.getBoolean(Names.RELAY1 + car_id, false);
        }
        long delta = new Date().getTime() - preferences.getLong(Names.RELE_START + car_id, 0) / 60000;
        return delta < preferences.getInt(Names.RELE_TIME, 30);
    }

    static String getAlarm(SharedPreferences preferences, String car_id) {
        String res = preferences.getString(Names.ALARM + car_id, "");
        if (!res.equals(""))
            return res;
        return preferences.getString(Names.ALARM, "");
    }

    static String getNotify(SharedPreferences preferences, String car_id) {
        String res = preferences.getString(Names.NOTIFY + car_id, "");
        if (!res.equals(""))
            return res;
        return preferences.getString(Names.NOTIFY, "");
    }

}
