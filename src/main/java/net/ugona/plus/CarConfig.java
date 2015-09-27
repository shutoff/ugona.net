package net.ugona.plus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eclipsesource.json.JsonObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class CarConfig extends Config {

    private static final String CAR_KEY = "car_";
    private static HashMap<String, CarConfig> config;
    private String name;
    private String key;
    private String auth;
    private String login;
    private String phone;
    private Command[] cmd;
    private int event_filter;
    private int voltage_shift;
    private String temp_settings;
    private Setting[] settings;
    private Sms[] sms;
    private int[] fab;
    private boolean inet_cmd;
    private int sim_cmd;
    private boolean ccode_text;
    private boolean device_password;
    private boolean showBalance;
    private int balance_limit;
    private String alarmSound;
    private String notifySound;
    private String azStartSound;
    private String azStopSound;
    private String zoneInSound;
    private String zoneOutSound;
    private int alarmVibro;
    private int notifyVibro;
    private int azStartVibro;
    private int azStopVibro;
    private int zoneInVbro;
    private int zoneOutVibro;
    private int[] pointers;
    private String customNames;
    private int leftDays;
    private int leftMileage;
    private int leftHours;
    private String maintenance;
    private long maintenance_time;
    private String command_values;
    private String theme;

    private CarConfig() {
        init();
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

    static String replace(String text, String subst, Intent data) {
        String s = "";
        if (data != null) {
            s = data.getStringExtra(subst);
            if (s == null)
                s = "";
        }
        return text.replace("{" + subst + "}", s);
    }

    public void save(Context context, String id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        ed.putString(CAR_KEY + id, save(this));
        ed.commit();
    }

    public void init() {
        name = "";
        key = "";
        auth = "";
        login = "";
        phone = "";
        event_filter = 3;
        balance_limit = 50;
        temp_settings = "";
        alarmSound = "";
        notifySound = "";
        azStartSound = "";
        azStopSound = "";
        zoneInSound = "";
        zoneOutSound = "";
        customNames = "";
        leftDays = 1000;
        leftMileage = 1000;
        leftHours = 1000;
        maintenance = "";
        theme = "";
    }

    void read(Context context, String id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String s = preferences.getString(CAR_KEY + id, "");
        if (!s.equals("")) {
            try {
                update(this, JsonObject.readFrom(s));
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        key = upgradeString(preferences, "apikey" + id);
        auth = upgradeString(preferences, "auth" + id);
        login = upgradeString(preferences, "login" + id);
        phone = upgradeString(preferences, "phone" + id);
        name = upgradeString(preferences, "name" + id);
        if (!key.equals("") && !key.equals("demo")) {
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(CAR_KEY + id, save(this));
            ed.commit();
        }
    }

    public String getName() {
        if (name.equals(""))
            return login;
        return name;
    }

    public void setName(String name) {
        if (this.name.equals(name))
            return;
        this.name = name;
        upd = true;
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

    public String getCustomNames() {
        return customNames;
    }

    public void setCustomNames(String customNames) {
        if (this.customNames.equals(customNames))
            return;
        upd = true;
        this.customNames = customNames;
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

    public boolean isShowBalance() {
        return showBalance;
    }

    public void setShowBalance(boolean showBalance) {
        this.showBalance = showBalance;
    }

    public Command[] getCmd() {
        return cmd;
    }

    public Sms[] getSms() {
        return sms;
    }

    public Setting[] getSettings() {
        return settings;
    }

    public String getAlarmSound() {
        return alarmSound;
    }

    public String getNotifySound() {
        return notifySound;
    }

    public int getAlarmVibro() {
        return alarmVibro;
    }

    public int getNotifyVibro() {
        return notifyVibro;
    }

    public int getBalance_limit() {
        return balance_limit;
    }

    public void setBalance_limit(int balance_limit) {
        if (this.balance_limit == balance_limit)
            return;
        this.balance_limit = balance_limit;
        upd = true;
    }

    public String getTheme() {
        return theme;
    }

    public int[] getFab() {
        if ((fab == null) && (cmd != null)) {
            Vector<Integer> res = new Vector<>();
            for (Command c : cmd) {
                if (c.group == null)
                    continue;
                if (!c.on)
                    continue;
                res.add(c.id);
            }
            fab = new int[res.size()];
            for (int i = 0; i < res.size(); i++) {
                fab[i] = res.get(i);
            }
        }
        return fab;
    }

    public void setFab(int[] fab) {
        this.fab = fab;
        upd = true;
    }

    public int[] getPointers() {
        return pointers;
    }

    public void setPointers(int[] pointers) {
        this.pointers = pointers;
        upd = true;
    }

    public int getLeftDays() {
        return leftDays;
    }

    public void setLeftDays(int leftDays) {
        if (this.leftDays == leftDays)
            return;
        this.leftDays = leftDays;
        upd = true;
    }

    public int getLeftMileage() {
        return leftMileage;
    }

    public void setLeftMileage(int leftMileage) {
        if (this.leftMileage == leftMileage)
            return;
        this.leftMileage = leftMileage;
        upd = true;
    }

    public int getLeftHours() {
        return leftHours;
    }

    public void setLeftHours(int leftHours) {
        if (this.leftHours == leftHours)
            return;
        this.leftHours = leftHours;
        upd = true;
    }

    public String getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(String maintenance) {
        if (this.maintenance.equals(maintenance))
            return;
        this.maintenance = maintenance;
        upd = true;
    }

    public long getMaintenance_time() {
        return maintenance_time;
    }

    public void setMaintenance_time(long maintenance_time) {
        if (this.maintenance_time == maintenance_time)
            return;
        this.maintenance_time = maintenance_time;
        upd = true;
    }

    public int getCommandValue(int cmd) {
        if (command_values == null)
            return 0;
        String[] values = command_values.split(",");
        for (String v : values) {
            String[] pair = v.split(":");
            if (pair[0].equals(cmd + ""))
                return Integer.parseInt(pair[1]);
        }
        return 0;
    }

    public void setCommandValue(int cmd, int value) {
        if (command_values == null) {
            command_values = cmd + ":" + value;
            upd = true;
            return;
        }
        String[] values = command_values.split(",");
        Vector<String> res = new Vector<>();
        for (String v : values) {
            String[] pair = v.split(":");
            if (pair[0].equals(cmd + "")) {
                if (pair[1].equals(value + ""))
                    return;
                continue;
            }
            res.add(v);
        }
        res.add(cmd + ":" + value);
        String r = null;
        for (String v : res) {
            if (r == null) {
                r = v;
                continue;
            }
            r += "," + v;
        }
        command_values = r;
        upd = true;
    }

    String getCommandName(int id) {
        Map<String, Integer> groups = new HashMap<>();
        String res = null;
        for (Command c : cmd) {
            if (c.group != null) {
                String name = c.group;
                if (name.equals(""))
                    name = c.name;
                if (groups.get(name) == null)
                    groups.put(name, c.id);
            }
            if (c.id == id) {
                res = c.name;
                if (c.group != null) {
                    String name = c.group;
                    if (name.equals(""))
                        name = c.name;
                    if (groups.get(name) != null) {
                        int c_id = groups.get(name);
                        String subst = null;
                        final String[] custom_names = customNames.split("\\|");
                        for (String custom_name : custom_names) {
                            String[] parts = custom_name.split(":");
                            if (parts.length != 2)
                                continue;
                            if (Integer.parseInt(parts[0]) == c_id)
                                subst = parts[1];
                        }
                        if (subst != null)
                            res = res.replace(name, subst);
                    }
                }
                break;
            }
        }
        return res;
    }

    public static class Command implements Serializable {
        int id;
        String name;
        String icon;
        String sms;
        String call;
        int inet;
        int next;
        boolean inet_ccode;
        boolean custom_name;
        String group;
        String condition;
        String done;
        String data;
        String time;
        boolean on;
        boolean always;
        Runnable onAnswer;

        String smsText(Intent data) {
            String text = sms.split("\\|")[0];
            text = replace(text, "ccode", data);
            text = replace(text, "pwd", data);
            text = replace(text, "ccode_new", data);
            text = replace(text, "v", data);
            return text;
        }
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
        String text;
        boolean ccode;
        int[] cmd;
    }

    public static class Sms implements Serializable {
        String sms;
        String set;
        String alarm;
        String notify;
        boolean all;
    }
}
