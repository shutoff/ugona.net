package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eclipsesource.json.JsonObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CarState extends Config implements Serializable {

    static final String CAR_KEY = "state_";
    private static HashMap<String, CarState> state;
    private long time;
    private long guard_time;
    private long last_stand;
    private long az_time;
    private long az_start;
    private long zone;
    private long card;
    private boolean door_fl;
    private boolean door_fr;
    private boolean door_bl;
    private boolean door_br;
    private boolean relay1;
    private boolean relay2;
    private boolean hood;
    private boolean trunk;
    private boolean ignition;
    private boolean accessory;
    private boolean online;
    private int power_state;
    private int reserved_state;
    private double power;
    private double reserved;
    private int gsm_level;
    private boolean tilt;
    private boolean move;
    private int shock;
    private String gsm;
    private String gps;
    private boolean gps_valid;
    private int speed;
    private int course;
    private String temperature;
    private String balance;
    private boolean guard;
    private int guard_mode;
    private double fuel;
    private boolean use_phone;
    private boolean show_photo;
    private boolean show_tracks;
    private boolean pointer;
    private boolean history;
    private boolean device_password;
    private long check_time;
    private String version;

    private boolean alert_doors;
    private boolean alert_hood;
    private boolean alert_trunk;
    private boolean valet;

    private CarState(Context context, String id) {

        gsm = "";
        gps = "";
        temperature = "";
        version = "";
        balance = "";

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

    public static CarState get(Context context, String car_id) {
        if (state == null)
            state = new HashMap<String, CarState>();
        CarState res = state.get(car_id);
        if (res != null)
            return res;
        res = new CarState(context, car_id);
        state.put(car_id, res);
        return res;
    }

    public static void save(Context context) {
        if (state == null)
            return;
        SharedPreferences.Editor ed = null;
        Set<Map.Entry<String, CarState>> entries = state.entrySet();
        for (Map.Entry<String, CarState> entry : entries) {
            CarState cfg = entry.getValue();
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

    public long getTime() {
        return time;
    }

    public long getGuard_time() {
        return guard_time;
    }

    public long getLast_stand() {
        return last_stand;
    }

    public long getAz_time() {
        return az_time;
    }

    public long getAz_start() {
        return az_start;
    }

    public long getZone() {
        return zone;
    }

    public long getCard() {
        return card;
    }

    public boolean isDoor_fl() {
        return door_fl;
    }

    public boolean isDoor_fr() {
        return door_fr;
    }

    public boolean isDoor_bl() {
        return door_bl;
    }

    public boolean isDoor_br() {
        return door_br;
    }

    public boolean isHood() {
        return hood;
    }

    public boolean isTrunk() {
        return trunk;
    }

    public boolean isIgnition() {
        return ignition;
    }

    public boolean isRelay1() {
        return relay1;
    }

    public boolean isRelay2() {
        return relay2;
    }

    public int getPower_state() {
        return power_state;
    }

    public int getReserved_state() {
        return reserved_state;
    }

    public double getPower() {
        return power;
    }

    public double getReserved() {
        return reserved;
    }

    public boolean isAccessory() {
        return accessory;
    }

    public int getGsm_level() {
        return gsm_level;
    }

    public String getGsm() {
        return gsm;
    }

    public String getGps() {
        return gps;
    }

    public int getSpeed() {
        return speed;
    }

    public int getCourse() {
        return course;
    }

    public boolean isGps_valid() {
        return gps_valid;
    }

    public String getTemperature() {
        return temperature;
    }

    public String getBalance() {
        return balance;
    }

    public boolean isGuard() {
        return guard;
    }

    public int getGuard_mode() {
        return guard_mode;
    }

    public double getFuel() {
        return fuel;
    }

    public boolean isUse_phone() {
        return use_phone;
    }

    public boolean isShow_photo() {
        return show_photo;
    }

    public boolean isShow_tracks() {
        return show_tracks;
    }

    public boolean isHistory() {
        return history;
    }

    public boolean isDevice_password() {
        return device_password;
    }

    public long getCheck_time() {
        return check_time;
    }

    public void setCheck_time(long check_time) {
        if (this.check_time == check_time)
            return;
        this.check_time = check_time;
        upd = true;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean isTilt() {
        return tilt;
    }

    public boolean isMove() {
        return move;
    }

    public boolean isAlert_doors() {
        return alert_doors;
    }

    public void setAlert_doors(boolean alert_doors) {
        this.alert_doors = alert_doors;
    }

    public boolean isAlert_hood() {
        return alert_hood;
    }

    public void setAlert_hood(boolean alert_hood) {
        this.alert_hood = alert_hood;
    }

    public boolean isAlert_trunk() {
        return alert_trunk;
    }

    public void setAlert_trunk(boolean alert_trunk) {
        this.alert_trunk = alert_trunk;
    }

    public boolean isValet() {
        return valet;
    }

    public void setValet(boolean valet) {
        this.valet = valet;
    }

    public int getShock() {
        return shock;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        if (this.version.equals(version))
            return;
        this.version = version;
        upd = true;
    }

    public boolean isPointer() {
        return pointer;
    }

    public boolean isAz() {
        if (az_time <= 0)
            return false;
        if (!guard)
            return false;
        return guard_time < az_time;
    }
}
