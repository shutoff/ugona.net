package net.ugona.plus;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.eclipsesource.json.JsonObject;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Notification extends Config {

    private static final String CAR_KEY = "notification_";
    public static long[] SHORT_PATTERN = {0, 100, 500, 100};
    public static long[] LONG_PATTERN = {0, 900, 500, 900};
    static int max_id;
    private static HashMap<String, Notification> notifications;
    private int az;
    private int valet_on;
    private int valet_off;
    private int part_guard;
    private int doors;
    private int hood;
    private int trunk;
    private int zone;
    private int info;
    private int alarm;
    private int balance;
    private int maintenance;
    private int no_gsm;
    private String message;
    private String title;
    private String url;

    Notification() {
        message = "";
        title = "";
        url = "";
    }

    public static Notification get(Context context, String car_id) {
        if (notifications == null)
            notifications = new HashMap<>();
        Notification res = notifications.get(car_id);
        if (res != null)
            return res;
        res = new Notification();
        res.read(context, car_id);
        notifications.put(car_id, res);
        return res;
    }

    public static void save(Context context) {
        if (notifications == null)
            return;
        SharedPreferences.Editor ed = null;
        Set<Map.Entry<String, Notification>> entries = notifications.entrySet();
        for (Map.Entry<String, Notification> entry : entries) {
            Notification cfg = entry.getValue();
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

    static boolean clear(Context context, String car_id) {
        Notification o = Notification.get(context, car_id);
        Field[] fields = o.getClass().getDeclaredFields();
        try {
            for (Field f : fields) {
                f.setAccessible(true);
                Class<?> c = f.getType();
                if (c != int.class)
                    continue;
                if (f.getName().equals("max_id"))
                    continue;
                int v = f.getInt(o);
                if (v > 0) {
                    if (f.getName().equals("valet_on")) {
                        CarState state = CarState.get(context, car_id);
                        if (state.isValet())
                            continue;
                    }
                    f.setInt(o, 0);
                    remove(context, v);
                }
            }
        } catch (Exception ex) {
            // ignore
        }
        o = Notification.get(context, "");
        if (o.info > 0) {
            remove(context, o.info);
            o.info = 0;
        }
        save(context);
        return false;
    }

    static void update(Context context, String car_id, Set<String> names) {
        if (names == null)
            return;
        CarState state = CarState.get(context, car_id);
        Notification notification = Notification.get(context, car_id);
        if (names.contains("az_time") || names.contains("az")) {
            if (notification.getAz() != 0) {
                remove(context, notification.az);
                notification.setAz(0);
            }
            long now = new Date().getTime();
            long az_time = state.getAz_time();
            if (az_time < 0)
                az_time = -az_time;
            if (az_time + 1200000 > now) {
                if (state.getAz_time() > 0) {
                    notification.setAz(create(context, context.getString(R.string.motor_on_ok), R.drawable.white_motor_on, car_id, "azStart", state.getAz_start(), false, null, null));
                } else if ((state.getAz_time() < 0) && !state.isGuard()) {
                    String msg = context.getString(R.string.motor_off_ok);
                    long az_stop = -state.getAz_time();
                    long az_start = state.getAz_start();
                    long time = (az_stop - az_start) / 60000;
                    if ((time > 0) && (time <= 20))
                        msg += " " + context.getString(R.string.motor_time).replace("$1", time + "");
                    notification.setAz(create(context, msg, R.drawable.white_motor_off, car_id, null, -state.getAz_time(), false, null, null));
                }
            }
        }
        if (names.contains("no_gsm")) {
            if (notification.getNo_gsm() != 0)
                remove(context, notification.getNo_gsm());
            if (state.isNo_gsm()) {
                notification.setNo_gsm(create(context, context.getString(R.string.lost), R.drawable.w_warning_light, car_id, null, 0, true, null, null));
            } else {
                notification.setNo_gsm(create(context, context.getString(R.string.restore), R.drawable.w_warning_light, car_id, null, 0, false, null, null));
            }
        }
        if (names.contains("guard") || names.contains("ignition")) {
            if ((notification.getAz() != 0) && (!state.isGuard() || (state.isIgnition() && !state.isAz()))) {
                remove(context, notification.getAz());
                notification.setAz(0);
            }
        }
        if (names.contains("time")) {
            if ((state.getNo_gsm_time() > 0) && (state.getTime() > state.getNo_gsm_time())) {
                state.setNo_gsm(false);
                if (notification.getNo_gsm() != 0)
                    remove(context, notification.az);
                notification.setNo_gsm(create(context, context.getString(R.string.restore), R.drawable.w_warning_light, car_id, null, 0, false, null, null));
            }
        }
        if (names.contains("guard_mode")) {
            if (state.isValet() != (state.getGuard_mode() == 2)) {
                if (notification.getValet_on() != 0) {
                    remove(context, notification.getValet_on());
                    notification.setValet_on(0);
                }
                if (notification.getValet_off() != 0) {
                    remove(context, notification.getValet_off());
                    notification.setValet_off(0);
                }
                state.setValet(state.getGuard_mode() == 2);
                if (state.isValet()) {
                    notification.setValet_on(create(context, context.getString(R.string.valet_on_ok), R.drawable.white_valet, car_id, "valet_on", 0, true, null, null));
                } else {
                    notification.setValet_off(create(context, context.getString(R.string.valet_off_ok), R.drawable.white_valet, car_id, "valet_off", 0, false, null, null));
                }
            }
        }
        if (names.contains("zone")) {
            if (notification.getZone() != 0) {
                remove(context, notification.getZone());
                notification.setZone(0);
            }
            String text;
            String grp;
            long time = state.getZone_time();
            if (time > 0) {
                text = context.getString(R.string.zone_in);
                grp = "zoneIn";
            } else {
                text = context.getString(R.string.zone_out);
                grp = "zoneOut";
                time = -time;
            }
            String zone = state.getZone();
            if (zone.length() >= 1)
                zone = zone.substring(1);
            if (!zone.equals(""))
                text += " " + zone;
            notification.setZone(create(context, text, R.drawable.white_zone, car_id, grp, time, false, null, null));
        }
        if (names.contains("guard_mode")) {
            if (notification.getPart_guard() != 0) {
                remove(context, notification.getPart_guard());
                notification.setPart_guard(0);
            }
            if (state.getGuard_mode() == 3)
                notification.setPart_guard(create(context, context.getString(R.string.ps_guard), R.drawable.white_zone, car_id, null, 0, false, null, null));
        }
        if (names.contains("balance")) {
            try {
                double balance = Double.parseDouble(state.getBalance());
                if (balance != state.getNotify_balance()) {
                    CarConfig carConfig = CarConfig.get(context, car_id);
                    if (carConfig.isShowBalance() && (carConfig.getBalance_limit() > 0) && (balance <= carConfig.getBalance_limit())) {
                        Calendar calendar = Calendar.getInstance();
                        int h = calendar.get(Calendar.HOUR_OF_DAY);
                        if ((h >= 9) && (h <= 21)) {
                            state.setNotify_balance(balance);
                            if (notification.getBalance() == 0)
                                notification.setBalance(create(context, context.getString(R.string.low_balance), R.drawable.white_balance, car_id, null, 0, false, null, null));
                        }
                    } else {
                        if (notification.getBalance() != 0) {
                            remove(context, notification.getBalance());
                            notification.setBalance(0);
                        }
                    }
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        boolean doors = state.isDoor_fl() || state.isDoor_fr() || state.isDoor_bl() || state.isDoor_br();
        if (state.isGuard() && doors != state.isAlert_doors()) {
            state.setAlert_doors(state.isGuard() && doors);
            if (state.isAlert_doors()) {
                notification.setDoors(create(context, context.getString(R.string.open_door), R.drawable.w_warning_light, car_id, null, 0, false, null, null));
            } else {
                if (notification.getDoors() != 0) {
                    remove(context, notification.getDoors());
                    notification.setDoors(0);
                }
            }
        }
        if (state.isGuard() && state.isHood() != state.isAlert_hood()) {
            state.setAlert_hood(state.isGuard() && state.isHood());
            if (state.isAlert_hood()) {
                notification.setHood(create(context, context.getString(R.string.open_hood), R.drawable.w_warning_light, car_id, null, 0, false, null, null));
            } else {
                if (notification.getHood() != 0) {
                    remove(context, notification.getHood());
                    notification.setHood(0);
                }
            }
        }
        if (state.isGuard() && state.isTrunk() != state.isAlert_trunk()) {
            state.setAlert_trunk(state.isGuard() && state.isTrunk());
            if (state.isAlert_trunk()) {
                notification.setTrunk(create(context, context.getString(R.string.open_trunk), R.drawable.w_warning_light, car_id, null, 0, false, null, null));
            } else {
                if (notification.getTrunk() != 0) {
                    remove(context, notification.getTrunk());
                    notification.setTrunk(0);
                }
            }
        }
    }

    static int create(Context context, String text, int pictId, String car_id, String sound, long when, boolean outgoing, String title, String actions) {
        Intent iNotification = new Intent(context, FetchService.class);
        iNotification.setAction(FetchService.ACTION_NOTIFICATION);
        iNotification.putExtra(Names.ID, car_id);
        if (sound != null)
            iNotification.putExtra(Names.SOUND, sound);
        iNotification.putExtra(Names.MESSAGE, text);
        iNotification.putExtra(Names.TITLE, title);
        iNotification.putExtra(Names.PICTURE, pictId);
        iNotification.putExtra(Names.NOTIFY_ID, ++max_id);
        iNotification.putExtra(Names.OUTGOING, outgoing);
        if (actions != null)
            iNotification.putExtra(Names.EXTRA, actions);
        if (when != 0)
            iNotification.putExtra(Names.WHEN, when);
        Uri data = Uri.withAppendedPath(Uri.parse("http://service/notification/"), car_id);
        iNotification.setData(data);
        PendingIntent pi = PendingIntent.getService(context, 0, iNotification, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500, pi);
        return max_id;
    }

    static void showMessage(Context context, String title, String message, String url) {
        Notification notification = Notification.get(context, "");
        if (notification.info > 0)
            remove(context, notification.info);
        notification.info = create(context, message, R.drawable.info, "", null, 0, false, title, null);
        save(context);
        AppConfig config = AppConfig.get(context);
        if (title == null)
            title = "";
        if (url == null)
            url = "";
        config.setInfo_title(title);
        config.setInfo_message(message);
        config.setInfo_url(url);
        AppConfig.save(context);
    }

    static void showAlarm(Context context, String car_id, String text) {
        Notification notification = Notification.get(context, car_id);
        if (notification.alarm != 0) {
            remove(context, notification.alarm);
            notification.alarm = 0;
        }
        notification.alarm = create(context, text, R.drawable.w_warning_light, car_id, null, 0, false, null, null);
    }

    static void showMaintenance(Context context, String car_id) {
        Notification notification = Notification.get(context, car_id);
        if (notification.maintenance != 0) {
            remove(context, notification.maintenance);
            notification.maintenance = 0;
        }

        CarConfig carConfig = CarConfig.get(context, car_id);
        String s = null;
        int left_days = carConfig.getLeftDays();
        int left_mileage = carConfig.getLeftMileage();
        if (left_days <= 15) {
            s = carConfig.getMaintenance() + "\n";
            if (left_days >= 0) {
                s += context.getString(R.string.left);
            } else {
                left_days = -left_days;
                s += context.getString(R.string.delay);
            }
            s += " ";
            if (left_days < 30) {
                s += State.getPlural(context, R.plurals.days, left_days);
            } else {
                int month = (int) Math.round(left_days / 30.);
                if (month < 12) {
                    s += State.getPlural(context, R.plurals.months, month);
                } else {
                    int years = (int) Math.round(left_days / 265.25);
                    s += State.getPlural(context, R.plurals.years, years);
                }
            }
        }
        if (left_mileage <= 500) {
            s = carConfig.getMaintenance() + "\n";
            if (left_mileage >= 0) {
                s += context.getString(R.string.left);
            } else {
                left_mileage = -left_mileage;
                s += context.getString(R.string.rerun);
            }
            s += String.format(" %,d ", left_mileage) + context.getString(R.string.km);
        }
        if (s != null) {
            String[] p = s.split("\n");
            notification.maintenance = create(context, p[0], R.drawable.info, car_id, null, 0, false, p[1], null);
        }
    }

    static void show(Context context, String car_id, String text, String title, int pictId, int max_id, String grp, long when, boolean outgoing, String actions) {
        CarConfig carConfig = CarConfig.get(context, car_id);
        if (title == null)
            title = carConfig.getName();

        String sound = null;
        try {
            Field field = carConfig.getClass().getDeclaredField(grp + "Sound");
            field.setAccessible(true);
            sound = field.get(carConfig).toString();
        } catch (Exception ex) {
            if (grp != null) {
                int id = context.getResources().getIdentifier("raw/" + grp, null, context.getPackageName());
                if (id != 0)
                    sound = "android.resource://net.ugona.plus/raw/" + grp;
            }
        }
        int vibro = 0;
        try {
            if (grp == null)
                grp = "notify";
            Field field = carConfig.getClass().getDeclaredField(grp + "Vibro");
            field.setAccessible(true);
            vibro = field.getInt(carConfig);
        } catch (Exception ex) {
            // ignore
        }
        if ((sound != null) && sound.equals(""))
            sound = null;
        if ((sound == null) && grp.equals("azStart"))
            sound = "android.resource://net.ugona.plus/raw/start";

        int defs = android.app.Notification.DEFAULT_LIGHTS;
        if (vibro == 0)
            defs |= android.app.Notification.DEFAULT_VIBRATE;
        if (sound == null)
            defs |= android.app.Notification.DEFAULT_SOUND;

        Uri uri = null;
        if (sound != null) {
            try {
                uri = Uri.parse(sound);
            } catch (Exception ex) {
                // ignore
            }
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setDefaults(defs)
                        .setSmallIcon(pictId)
                        .setContentTitle(title);
        if (when != 0)
            builder.setWhen(when);

        if (uri != null)
            builder.setSound(uri);

        if (vibro == 2)
            builder.setVibrate(SHORT_PATTERN);
        if (vibro == 3)
            builder.setVibrate(LONG_PATTERN);


        builder.setContentTitle(title);
        if ((text.length() > 80) || (actions != null)) {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        } else {
            builder.setContentText(text);
        }
        if (actions != null) {
            String[] parts = actions.split(";");
            for (int i = 1; i < parts.length; i++) {
                String[] p = parts[i].split(":");
                int id = Integer.parseInt(p[0]);
                int icon = Integer.parseInt(p[1]);
                Intent cmdIntent = new Intent(context, FetchService.class);
                cmdIntent.setAction(parts[0]);
                cmdIntent.putExtra(Names.ID, car_id);
                cmdIntent.putExtra(Names.NOTIFY_ID, max_id);
                cmdIntent.putExtra(Names.COMMAND, id);
                Uri data = Uri.withAppendedPath(Uri.parse("http://notification_cmd/id/"), max_id + "_" + id + "_" + icon);
                cmdIntent.setData(data);
                PendingIntent piCmd = PendingIntent.getService(context, 0, cmdIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.addAction(icon, p[2], piCmd);
            }
        }

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra(Names.ID, car_id);
        Uri data = Uri.withAppendedPath(Uri.parse("http://notification/id/"), car_id);
        notificationIntent.setData(data);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        Intent clearIntent = new Intent(context, FetchService.class);
        clearIntent.setAction(FetchService.ACTION_CLEAR);
        clearIntent.putExtra(Names.ID, car_id);
        clearIntent.putExtra(Names.NOTIFY_ID, max_id);
        data = Uri.withAppendedPath(Uri.parse("http://notification_clear/id/"), max_id + "");
        clearIntent.setData(data);
        PendingIntent deleteIntent = PendingIntent.getService(context, 0, clearIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setDeleteIntent(deleteIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        android.app.Notification notification = builder.build();
        if (outgoing)
            notification.flags = android.app.Notification.FLAG_ONGOING_EVENT;

        manager.notify(max_id, notification);

        Notification n = Notification.get(context, car_id);
        save(context);
    }

    static void remove(Context context, int id) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(id);
    }

    static void clear(Context context, String car_id, int id) {
        Notification o = Notification.get(context, car_id);
        Field[] fields = o.getClass().getDeclaredFields();
        try {
            for (Field f : fields) {
                f.setAccessible(true);
                Class<?> c = f.getType();
                if (c != int.class)
                    continue;
                int v = f.getInt(o);
                if (v == id)
                    f.setInt(o, 0);
            }
        } catch (Exception ex) {
            // ignore
        }
        save(context);
        remove(context, id);
    }

    public int getAz() {
        return az;
    }

    public void setAz(int az) {
        if (this.az == az)
            return;
        this.az = az;
        upd = true;
    }

    public int getValet_on() {
        return valet_on;
    }

    public void setValet_on(int valet_on) {
        if (this.valet_on == valet_on)
            return;
        this.valet_on = valet_on;
        upd = true;
    }

    public int getValet_off() {
        return valet_off;
    }

    public void setValet_off(int valet_off) {
        if (this.valet_off == valet_off)
            return;
        this.valet_off = valet_off;
        upd = true;
    }

    public int getPart_guard() {
        return part_guard;
    }

    public void setPart_guard(int part_guard) {
        if (this.part_guard == part_guard)
            return;
        this.part_guard = part_guard;
        upd = true;
    }

    public int getDoors() {
        return doors;
    }

    public void setDoors(int doors) {
        if (this.doors == doors)
            return;
        this.doors = doors;
        upd = true;
    }

    public int getHood() {
        return hood;
    }

    public void setHood(int hood) {
        if (this.hood == hood)
            return;
        this.hood = hood;
        upd = true;
    }

    public int getTrunk() {
        return trunk;
    }

    public void setTrunk(int trunk) {
        if (this.trunk == trunk)
            return;
        this.trunk = trunk;
        upd = true;
    }

    public int getZone() {
        return zone;
    }

    public void setZone(int zone) {
        if (this.zone == zone)
            return;
        this.zone = zone;
        upd = true;
    }

    public int getInfo() {
        return info;
    }

    public void setInfo(int info) {
        if (this.info == info)
            return;
        this.info = info;
        upd = true;
    }

    public int getAlarm() {
        return alarm;
    }

    public void setAlarm(int alarm) {
        if (this.alarm == alarm)
            return;
        this.alarm = alarm;
        upd = true;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        if (this.balance == balance)
            return;
        this.balance = balance;
        upd = true;
    }

    public int getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(int maintenance) {
        if (this.maintenance == maintenance)
            return;
        this.maintenance = maintenance;
        upd = true;
    }

    public int getNo_gsm() {
        return no_gsm;
    }

    public void setNo_gsm(int no_gsm) {
        if (this.no_gsm == no_gsm)
            return;
        this.no_gsm = no_gsm;
        upd = true;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        if (this.message.equals(message))
            return;
        this.message = message;
        upd = true;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (this.title.equals(title))
            return;
        this.title = title;
        upd = true;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (this.url.equals(url))
            return;
        this.url = url;
        upd = true;
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

}
