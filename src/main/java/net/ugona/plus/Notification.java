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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Notification extends Config {

    private static final String CAR_KEY = "notification_";
    static int max_id;
    private static HashMap<String, Notification> notifications;
    private int az;
    private int valet;
    private int part_guard;
    private int doors;
    private int hood;
    private int trunk;
    private int zone;
    private int info;
    private int alarm;
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
                int v = f.getInt(o);
                if (v > 0) {
                    if (f.getName().equals("valet")) {
                        CarState state = CarState.get(context, car_id);
                        if (state.getGuard_mode() == 1)
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
            State.appendLog("changed az " + state.getAz_time() + " " + state.isIgnition());
            if (notification.az != 0) {
                remove(context, notification.az);
                notification.az = 0;
            }
            if (state.getAz_time() > 0) {
                notification.az = create(context, context.getString(R.string.motor_on_ok), R.drawable.white_motor_on, car_id, "start", state.getAz_start(), false, null);
            } else if ((state.getAz_time() < 0) && !state.isIgnition()) {
                String msg = context.getString(R.string.motor_off_ok);
                long az_stop = -state.getAz_time();
                long az_start = state.getAz_start();
                long time = (az_stop - az_start) / 60000;
                if ((time > 0) && (time <= 20))
                    msg += " " + context.getString(R.string.motor_time).replace("$1", time + "");
                notification.az = create(context, msg, R.drawable.white_motor_off, car_id, null, -state.getAz_time(), false, null);
            }
        }
        if (names.contains("guard_mode")) {
            if (state.isValet() != (state.getGuard_mode() == 1)) {
                if (notification.valet != 0) {
                    remove(context, notification.valet);
                    notification.valet = 0;
                }
                state.setValet(state.getGuard_mode() == 1);
                if (state.isValet()) {
                    notification.valet = create(context, context.getString(R.string.valet_on_ok), R.drawable.white_valet, car_id, "valet_on", 0, true, null);
                } else {
                    notification.valet = create(context, context.getString(R.string.valet_off_ok), R.drawable.white_valet, car_id, "valet_off", 0, false, null);
                }
            }
        }
        if (names.contains("zone")) {
            if (notification.zone != 0) {
                remove(context, notification.zone);
                notification.zone = 0;
            }
            String text;
            long time = state.getZone_time();
            if (time > 0) {
                text = context.getString(R.string.zone_in);
            } else {
                text = context.getString(R.string.zone_out);
                time = -time;
            }
            String zone = state.getZone();
            if (zone.length() >= 1)
                zone = zone.substring(1);
            if (!zone.equals(""))
                text += " " + zone;
            notification.zone = create(context, text, R.drawable.white_zone, car_id, null, time, false, null);
        }
        if (names.contains("guard_mode")) {
            if (notification.part_guard != 0) {
                remove(context, notification.part_guard);
                notification.part_guard = 0;
            }
            if (state.getGuard_mode() == 3)
                notification.part_guard = create(context, context.getString(R.string.ps_guard), R.drawable.white_zone, car_id, null, 0, false, null);
        }
        boolean doors = state.isDoor_fl() || state.isDoor_fr() || state.isDoor_bl() || state.isDoor_br();
        if (state.isGuard() && doors != state.isAlert_doors()) {
            state.setAlert_doors(state.isGuard() && doors);
            if (state.isAlert_doors()) {
                notification.doors = create(context, context.getString(R.string.open_door), R.drawable.w_warning_light, car_id, null, 0, false, null);
            } else {
                if (notification.doors != 0) {
                    remove(context, notification.doors);
                    notification.doors = 0;
                }
            }
        }
        if (state.isGuard() && state.isHood() != state.isAlert_hood()) {
            state.setAlert_hood(state.isGuard() && state.isHood());
            if (state.isAlert_hood()) {
                notification.hood = create(context, context.getString(R.string.open_hood), R.drawable.w_warning_light, car_id, null, 0, false, null);
            } else {
                if (notification.hood != 0) {
                    remove(context, notification.hood);
                    notification.hood = 0;
                }
            }
        }
        if (state.isGuard() && state.isTrunk() != state.isAlert_trunk()) {
            state.setAlert_trunk(state.isGuard() && state.isTrunk());
            if (state.isAlert_trunk()) {
                notification.trunk = create(context, context.getString(R.string.open_trunk), R.drawable.w_warning_light, car_id, null, 0, false, null);
            } else {
                if (notification.trunk != 0) {
                    remove(context, notification.trunk);
                    notification.trunk = 0;
                }
            }
        }
    }

    static int create(Context context, String text, int pictId, String car_id, String sound, long when, boolean outgoing, String title) {
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
        if (notification.info > 0) {
            remove(context, notification.info);
        }
        notification.info = create(context, message, R.drawable.info, "", null, 0, false, title);
        save(context);
    }

    static void showAlarm(Context context, String car_id, String text) {
        Notification notification = Notification.get(context, car_id);
        if (notification.alarm != 0) {
            remove(context, notification.alarm);
            notification.alarm = 0;
        }
        notification.alarm = create(context, text, R.drawable.w_warning_light, car_id, null, 0, false, null);
    }

    static void show(Context context, String car_id, String text, String title, int pictId, int max_id, String sound, long when, boolean outgoing) {
        if (title == null)
            title = context.getString(R.string.app_name);

        if (sound == null) {
            CarConfig carConfig = CarConfig.get(context, car_id);
            sound = carConfig.getNotifySound();
            if (sound.equals(""))
                sound = null;
        }

        int defs = android.app.Notification.DEFAULT_LIGHTS;
        defs |= android.app.Notification.DEFAULT_VIBRATE;
        if (sound == null)
            defs |= android.app.Notification.DEFAULT_SOUND;

        Uri uri = null;
        if (sound != null)
            uri = Uri.parse("android.resource://net.ugona.plus/raw/" + sound);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setDefaults(defs)
                        .setSmallIcon(pictId)
                        .setContentTitle(title);
        if (when != 0)
            builder.setWhen(when);

        if (uri != null)
            builder.setSound(uri);

        builder.setContentTitle(title);
        if (text.length() > 80) {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        } else {
            builder.setContentText(text);
        }

        Intent notificationIntent = new Intent(context, MainActivity.class);
        if (pictId == R.drawable.info)
            notificationIntent = new Intent(context, MessageDialog.class);
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
