package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
                String pointers = preferences.getString(Names.Car.POINTERS + car, "");
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
        if ((car_id == null) && (cars.length > 0))
            car_id = cars[0];
        if (car_id == null)
            car_id = "";
        return car_id;
    }

    static String getTemperature(SharedPreferences preferences, String car_id, int sensor) {
        String temp = preferences.getString(Names.Car.TEMPERATURE + car_id, "");
        if (temp.equals(""))
            return null;

        Map<Integer, TempConfig> config = new HashMap<Integer, TempConfig>();
        String[] temp_config = preferences.getString(Names.Car.TEMP_SETTINGS + car_id, "").split(",");
        for (String s : temp_config) {
            String[] data = s.split(":");
            if (data.length != 3)
                continue;
            try {
                int id = Integer.parseInt(data[0]);
                TempConfig c = new TempConfig();
                c.shift = Integer.parseInt(data[1]);
                c.where = Integer.parseInt(data[2]);
                config.put(id, c);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        String[] data = temp.split(";");
        if (sensor < 0) {
            sensor = -sensor;
            for (String s : data) {
                try {
                    String[] d = s.split(":");
                    int id = Integer.parseInt(d[0]);
                    TempConfig c = config.get(id);
                    if (c == null)
                        continue;
                    if (c.where != sensor)
                        continue;
                    int v = Integer.parseInt(d[1]) + c.shift;
                    return v + " \u00B0C";
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return null;
        }
        for (String s : data) {
            try {
                String[] d = s.split(":");
                int id = Integer.parseInt(d[0]);
                TempConfig c = config.get(id);
                if ((c != null) && (c.where > 0))
                    continue;
                int v = Integer.parseInt(d[1]);
                if (--sensor > 0)
                    continue;
                if (id == 1) {
                    v += preferences.getInt(Names.Car.TEMP_SIFT + car_id, 0);
                } else if (c != null) {
                    v += c.shift;
                }
                return v + " \u00B0C";
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    static int getTemperaturesCount(SharedPreferences preferences, String car_id) {
        return preferences.getString(Names.Car.TEMPERATURE + car_id, "").split(";").length;
    }

    static boolean getRele(SharedPreferences preferences, String car_id) {
        long delta = (new Date().getTime() - preferences.getLong(Names.Car.RELE_START + car_id, 0)) / 60000;
        return delta < preferences.getInt(Names.Car.RELE_TIME + car_id, 30);
    }

    static String getSound(SharedPreferences preferences, String key, String car_id) {
        String res = preferences.getString(key + car_id, "");
        if (!res.equals(""))
            return res;
        return preferences.getString(key, "");
    }

    static void checkBalance(Context context, String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int limit = preferences.getInt(Names.Car.LIMIT + car_id, 50);
        if (limit >= 0) {
            int balance_id = preferences.getInt(Names.Car.BALANCE_NOTIFICATION + car_id, 0);
            try {
                double value = Double.parseDouble(preferences.getString(Names.Car.BALANCE + car_id, ""));
                if (value <= limit) {
                    if (balance_id == 0) {
                        SharedPreferences.Editor ed = preferences.edit();
                        balance_id = Alarm.createNotification(context, context.getString(R.string.low_balance), R.drawable.white_balance, car_id, null, 0);
                        ed.putInt(Names.Car.BALANCE_NOTIFICATION + car_id, balance_id);
                        ed.commit();
                    }
                } else {
                    if (balance_id > 0) {
                        SharedPreferences.Editor ed = preferences.edit();
                        Alarm.removeNotification(context, car_id, balance_id);
                        ed.remove(Names.Car.BALANCE_NOTIFICATION + car_id);
                        ed.commit();
                    }
                }
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    static boolean isDevicePasswd(Context context, String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getString(Names.Car.VERSION + car_id, "").toLowerCase().contains("superagent"))
            return false;
        return preferences.getBoolean(Names.Car.DEVICE_PSWD + car_id, false);
    }

    static class TempConfig {
        int shift;
        int where;
    }

}
