package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eclipsesource.json.JsonObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class CarConfig extends Config implements Serializable {

    private static final String CAR_KEY = "car_";
    private static HashMap<String, CarConfig> config;
    private final String name;
    private final String key;
    private final String auth;
    private String login;
    private String phone;
    private Command[] cmd;
    private int event_filter;
    private int voltage_shift;
    private String temp_settings;
    private Setting[] settings;
    private int[] commands;
    private boolean inet_cmd;
    private int sim_cmd;
    private boolean ccode_text;
    private boolean device_password;
    private boolean hideBalance;

    private CarConfig() {
        name = "";
        key = "";
        auth = "";
        login = "";
        phone = "";
        event_filter = 3;
        temp_settings = "";
    }

    public static CarConfig get(Context context, String car_id) {
        if (config == null)
            config = new HashMap<>();
        CarConfig res = config.get(car_id);
        if (res != null)
            return res;
        res = new CarConfig();
        res.read(context, car_id);
        config.put(car_id, res);
        return res;
    }

    public static CarConfig clear(Context context, String car_id) {
        if (config != null)
            config.remove(car_id);
        CarConfig res = new CarConfig();
        res.reset(context, car_id);
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

    void read(Context context, String id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String s = preferences.getString(CAR_KEY + id, "");
        if (!s.equals("")) {
            try {
                update(this, JsonObject.readFrom(s));
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    void reset(Context context, String id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        ed.remove(CAR_KEY + id);
        ed.commit();
    }

    public String getName() {
        return name;
    }

    public String getName(Context context, String id) {
        if (!name.equals(""))
            return name;
        String res = context.getString(R.string.car);
        int n = 0;
        try {
            n = Integer.parseInt(id);
        } catch (Exception ex) {
            // ignore
        }
        return res + " " + (n + 1);
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

    public boolean isInet_cmd() {
        return inet_cmd;
    }

    public void setInet_cmd(boolean inet_cmd) {
        if (this.inet_cmd == inet_cmd)
            return;
        this.inet_cmd = inet_cmd;
        upd = true;
    }

    public int getSim_cmd() {
        return sim_cmd;
    }

    public void setSim_cmd(int sim_cmd) {
        if (this.sim_cmd == sim_cmd)
            return;
        this.sim_cmd = sim_cmd;
        upd = true;
    }

    public boolean isCcode_text() {
        return ccode_text;
    }

    public void setCcode_text(boolean ccode_text) {
        if (this.ccode_text == ccode_text)
            return;
        this.ccode_text = ccode_text;
        upd = true;
    }

    public int getEvent_filter() {
        return event_filter;
    }

    public void setEvent_filter(int event_filter) {
        if (this.event_filter == event_filter)
            return;
        this.event_filter = event_filter;
        upd = true;
    }

    public int getVoltage_shift() {
        return voltage_shift;
    }

    public void setVoltage_shift(int voltage_shift) {
        if (this.voltage_shift == voltage_shift)
            return;
        this.voltage_shift = voltage_shift;
        upd = true;
    }

    public String getTemp_settings() {
        return temp_settings;
    }

    public void setTemp_settings(String temp_settings) {
        if (this.temp_settings.equals(temp_settings))
            return;
        this.temp_settings = temp_settings;
        upd = true;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (this.phone.equals(phone))
            return;
        this.phone = phone;
        upd = true;
    }

    public boolean isDevice_password() {
        return device_password;
    }

    public void setDevice_password(boolean device_password) {
        if (this.device_password == device_password)
            return;
        this.device_password = device_password;
        upd = true;
    }

    public boolean isHideBalance() {
        return hideBalance;
    }

    public void setHideBalance(boolean hideBalance) {
        this.hideBalance = hideBalance;
    }

    public Command[] getCmd() {
        return cmd;
    }

    public Setting[] getSettings() {
        return settings;
    }

    public int[] getCommands() {
        if ((commands == null) && (cmd != null)) {
            Vector<Integer> res = new Vector<>();
            for (Command c : cmd) {
                if (c.group == null)
                    continue;
                if (!c.on)
                    continue;
                res.add(c.id);
            }
            commands = new int[res.size()];
            for (int i = 0; i < res.size(); i++) {
                commands[i] = res.get(i);
            }
        }
        return commands;
    }

    public void setCommands(int[] commands) {
        this.commands = commands;
        upd = true;
    }

    public static class Command implements Serializable {
        int id;
        String name;
        String icon;
        String sms;
        String call;
        int inet;
        boolean inet_ccode;
        boolean custom_name;
        String group;
        String condition;
        String state;
        String done;
        boolean on;
        boolean always;
    }

    public static class Setting implements Serializable {
        String id;
        String name;
        String unit;
        String values;
        String zero;
        Integer min;
        Integer max;
        double k;
    }
}
