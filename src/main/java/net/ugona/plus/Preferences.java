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

            }
            if (!car_ok)
                car_id = null;
        }
        if (car_id == null)
            car_id = cars[0];
        return car_id;
    }

    static String getTemperature(SharedPreferences preferences, String car_id) {
        try {
            String s = preferences.getString(Names.TEMPERATURE + car_id, "");
            if (s.length() == 0)
                return null;
            double v = Double.parseDouble(s);
            v += preferences.getInt(Names.TEMP_SIFT + car_id, 0);
            return v + " \u00B0C";
        } catch (Exception ex) {
        }
        return null;
    }

    static boolean getValet(SharedPreferences preferences, String car_id) {
        Date now = new Date();
        long d = now.getTime() / 1000;
        if (d - preferences.getLong(Names.VALET_TIME + car_id, 0) < 30)
            return true;
        if (d - preferences.getLong(Names.INIT_TIME + car_id, 0) < 30)
            return false;
        return preferences.getBoolean(Names.VALET + car_id, false);
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
