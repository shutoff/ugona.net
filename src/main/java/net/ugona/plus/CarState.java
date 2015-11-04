package net.ugona.plus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eclipsesource.json.JsonObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CarState extends Config {

    static final String CAR_KEY = "state_";
    private static HashMap<String, CarState> state;
    private long time;
    private long guard_time;
    private long last_stand;
    private long az_time;
    private long az_start;
    private long ignition_time;
    private long heater_time;
    private String zone;
    private long zone_time;
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
    private boolean sos;
    private boolean in_sensor;
    private boolean ext_sensor;
    private int shock;
    private String gsm;
    private String gps;
    private String gsm_region;
    private boolean gps_valid;
    private int speed;
    private int course;
    private String temperature;
    private String balance;
    private boolean guard;
    private int guard_mode;
    private double fuel;
    private double card_voltage;
    private double card_level;
    private boolean use_phone;
    private boolean show_photo;
    private boolean show_tracks;
    private boolean set_phone;
    private boolean pointer;
    private boolean history;
    private boolean zones;
    private boolean device_password;
    private boolean device_pswd;
    private boolean set_auth;
    private long check_time;
    private long no_gsm_time;
    private String check_version;
    private String version;

    private boolean alert_doors;
    private boolean alert_hood;
    private boolean alert_trunk;
    private boolean valet;
    private double notify_balance;

    private String address;
    private double addr_lat;
    private double addr_lon;
    private String address_type;

    private String[] events;

    private CarState(Context context, String id) {

        gsm = "";
        gps = "";
        temperature = "";
        version = "";
        balance = "";
        zone = "";
        check_version = "";
        gsm_region = "";
        address = "";
        address_type = "";

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

    void save(Context context, String id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        ed.putString(CAR_KEY + id, save(this));
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

    public long getIgnition_time() {
        return ignition_time;
    }

    public long getNo_gsm_time() {
        return no_gsm_time;
    }

    public void setNo_gsm_time(long no_gsm_time) {
        if (this.no_gsm_time == no_gsm_time)
            return;
        this.no_gsm_time = no_gsm_time;
        upd = true;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        if (this.zone.equals(zone))
            return;
        this.zone = zone;
        Date now = new Date();
        zone_time = now.getTime();
        if (zone.substring(0, 1).equals("-"))
            zone_time = -zone_time;
        upd = true;
    }

    public long getZone_time() {
        return zone_time;
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

    public String getGsm_region() {
        return gsm_region;
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
        return guard && (guard_mode != 2);
    }

    public int getGuard_mode() {
        return guard_mode;
    }

    public double getFuel() {
        return fuel;
    }

    public double getCard_voltage() {
        return card_voltage;
    }

    public double getCard_level() {
        return card_level;
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

    public boolean isSet_phone() {
        return set_phone;
    }

    public boolean isHistory() {
        return history;
    }

    public boolean isZones() {
        return zones;
    }

    public boolean isDevice_password() {
        return device_password;
    }

    public boolean isDevice_pswd() {
        return device_pswd;
    }

    public boolean isSet_auth() {
        return set_auth;
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

    public void setTilt(boolean tilt) {
        this.tilt = tilt;
    }

    public boolean isMove() {
        return move;
    }

    public void setMove(boolean move) {
        this.move = move;
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

    public void setShock(int shock) {
        this.shock = shock;
    }

    public void setSos(boolean sos) {
        this.sos = sos;
    }

    public boolean isIn_sensor() {
        return in_sensor;
    }

    public void setIn_sensor(boolean in_sensor) {
        this.in_sensor = in_sensor;
    }

    public boolean isExt_sensor() {
        return ext_sensor;
    }

    public void setExt_sensor(boolean ext_sensor) {
        this.ext_sensor = ext_sensor;
    }

    public String getVersion() {
        return version;
    }

    public boolean isPointer() {
        return pointer;
    }

    public String getCheck_version() {
        return check_version;
    }

    public void setCheck_version(String check_version) {
        this.check_version = check_version;
    }

    public boolean isAz() {
        if (az_time <= 0)
            return false;
        if (!guard)
            return false;
        if (ignition_time < 0)
            return false;
        if (guard_time >= az_time)
            return false;
        return az_time >= new Date().getTime() - 1500000;
    }

    public void setAz(boolean az) {
        long now = new Date().getTime();
        if (az) {
            if (az_time < now - 150000) {
                az_time = now;
                az_start = now;
                ignition_time = now;
            }
            ignition = true;
        } else {
            if (az_time > 0) {
                az_time = -now;
                ignition_time = -now;
            }
            ignition = false;
        }
        upd = true;
    }

    public String[] getEvents() {
        return events;
    }

    public boolean isNo_gsm() {
        return no_gsm_time != 0;
    }

    public void setNo_gsm(boolean no_gsm) {
        if (no_gsm) {
            if (no_gsm_time == 0)
                no_gsm_time = new Date().getTime();
            upd = true;
            return;
        }
        no_gsm_time = 0;
        upd = true;
    }

    public double getNotify_balance() {
        return notify_balance;
    }

    public void setNotify_balance(double notify_balance) {
        if (this.notify_balance == notify_balance)
            return;
        this.notify_balance = notify_balance;
        upd = true;
    }

    public String getAddress(final Context context) {
        try {
            String[] parts = gps.split(",");
            final double lat = Double.parseDouble(parts[0]);
            final double lon = Double.parseDouble(parts[1]);
            double distance = 1000;
            AppConfig appConfig = AppConfig.get(context);
            final String type = appConfig.getMap_type();
            if (!type.equals(address_type))
                address = "";
            if (!address.equals(""))
                distance = State.distance(lat, lon, addr_lat, addr_lon);
            if (distance > 400) {
                if (!address.equals("")) {
                    address = "";
                    upd = true;
                }
            }
            if (distance > 80) {
                Address.get(context, lat, lon, type, new Address.Answer() {
                    @Override
                    public void result(String res) {
                        if (res == null)
                            return;
                        addr_lat = lat;
                        addr_lon = lon;
                        address_type = type;
                        upd = true;
                        if (res.equals(address))
                            return;
                        address = res;
                        Intent intent = new Intent(Names.ADDRESS_UPDATE);
                        context.sendBroadcast(intent);
                    }
                });
            }
        } catch (Exception ex) {
            return null;
        }
        if (address.equals(""))
            return null;
        return address;
    }

    public boolean isHeater() {
        if (heater_time == 0)
            return false;
        if (ignition)
            return false;
        return new Date().getTime() < heater_time;
    }

    public void setHeater(boolean heater) {
        if (heater) {
            heater_time = new Date().getTime() + 600000;
        } else {
            heater_time = 0;
        }
        upd = true;
    }
}
