package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eclipsesource.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CarConfig extends Config {

    static final String CAR_KEY = "car_";
    private static HashMap<String, CarConfig> config;
    private String name;
    private String key;
    private String auth;
    private String login;

    private CarConfig(Context context, String id) {
        name = "";
        key = "";
        auth = "";
        login = "";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String s = preferences.getString(CAR_KEY + id, "");
        if (!s.equals("")) {
            try {
                update(this, JsonObject.readFrom(s));
                return;
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    public static CarConfig get(Context context, String car_id) {
        if (config == null)
            config = new HashMap<String, CarConfig>();
        CarConfig res = config.get(car_id);
        if (res != null)
            return res;
        res = new CarConfig(context, car_id);
        config.put(car_id, res);
        return res;
    }

    public static void save(Context context) {
        if (config == null)
            return;
        SharedPreferences.Editor ed = null;
        Set<Map.Entry<String, CarConfig>> entries = config.entrySet();
        for (Map.Entry<String, CarConfig> entry : entries) {
            CarConfig cfg = entry.getValue();
            if (!cfg.upd)
                continue;
            if (ed == null) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                ed = preferences.edit();
            }
            ed.putString(CAR_KEY + entry.getKey(), save(cfg));
        }
        if (ed != null)
            ed.commit();
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public String getAuth() {
        return auth;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        if (this.login.equals(login))
            return;
        this.login = login;
        upd = true;
    }
}
