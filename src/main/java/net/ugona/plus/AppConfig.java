package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eclipsesource.json.JsonObject;

public class AppConfig extends Config {

    final static String CONFIG_KEY = "config";
    static private AppConfig config;
    String ids;
    String current_id;
    String password;
    String pattern;

    private AppConfig(Context context) {
        ids = "";
        current_id = "";
        password = "";
        pattern = "";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String s = preferences.getString(CONFIG_KEY, "");
        if (!s.equals("")) {
            try {
                update(this, JsonObject.readFrom(s));
                return;
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    static public AppConfig get(Context context) {
        if (config == null)
            config = new AppConfig(context);
        return config;
    }

    static public void save(Context context) {
        if (config == null)
            return;
        if (config.upd) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(CONFIG_KEY, save(config));
            ed.commit();
        }
        CarConfig.save(context);
        CarState.save(context);
    }

    String getId(String id) {
        if (id == null)
            id = current_id;
        String[] s_ids = ids.split(",");
        for (String s : s_ids) {
            if (id.equals(s)) {
                if (!id.equals(current_id)) {
                    current_id = id;
                    upd = true;
                }
                return id;
            }
        }
        if (s_ids.length > 0) {
            id = s_ids[0];
        } else {
            id = "";
        }
        if (!id.equals(current_id)) {
            current_id = id;
            upd = true;
        }
        return id;
    }

    String[] getCars() {
        return ids.split(",");
    }
}
