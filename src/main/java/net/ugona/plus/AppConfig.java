package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eclipsesource.json.JsonObject;

import java.io.Serializable;

public class AppConfig extends Config implements Serializable {

    final static String CONFIG_KEY = "config";
    static private AppConfig config;

    private String ids;
    private String current_id;
    private String password;
    private String pattern;
    private boolean use_gps;
    private boolean show_traffic;
    private boolean show_speed;
    private String map_type;

    private AppConfig(Context context) {
        ids = "";
        current_id = "";
        password = "";
        pattern = "";
        map_type = "OSM";
        show_speed = true;
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

    public String getIds() {
        return ids;
    }

    public void setIds(String ids) {
        if (this.ids.equals(ids))
            return;
        this.ids = ids;
        upd = true;
    }

    public String getCurrent_id() {
        return current_id;
    }

    public void setCurrent_id(String current_id) {
        if (this.current_id.equals(current_id))
            return;
        this.current_id = current_id;
        upd = true;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (this.password.equals(password))
            return;
        this.password = password;
        upd = true;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        if (this.pattern.equals(pattern))
            return;
        this.pattern = pattern;
        upd = true;
    }

    public boolean isUse_gps() {
        return use_gps;
    }

    public void setUse_gps(boolean use_gps) {
        if (this.use_gps == use_gps)
            return;
        this.use_gps = use_gps;
        upd = true;
    }

    public String getMap_type() {
        return map_type;
    }

    public void setMap_type(String map_type) {
        if (this.map_type.equals(map_type))
            return;
        this.map_type = map_type;
        upd = true;
    }

    public boolean isShow_traffic() {
        return show_traffic;
    }

    public void setShow_traffic(boolean show_traffic) {
        if (this.show_traffic == show_traffic)
            return;
        this.show_traffic = show_traffic;
        upd = true;
    }

    public boolean isShow_speed() {
        return show_speed;
    }

    public void setShow_speed(boolean show_speed) {
        if (this.show_speed == show_speed)
            return;
        this.show_speed = show_speed;
    }
}
