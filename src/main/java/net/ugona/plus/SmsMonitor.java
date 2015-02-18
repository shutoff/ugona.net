package net.ugona.plus;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;

import com.mediatek.telephony.SmsManagerEx;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsMonitor extends BroadcastReceiver {

    static final String SMS_SENT = "net.ugona.plus.SMS_SENT";
    static final String SMS_ANSWER = "net.ugona.plus.SMS_ANSWER";
    static final String SMS_SEND = "net.ugona.plus.SMS_SEND";
    static final String SMS_ACTION = "net.ugona.plus.ACTION";
    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
    static Map<String, SmsQueues> processed;
    static String[] notifications = {
            "Low Card Battery",
            "Supply reserve",
            "Supply regular",
            "ERROR LAN-devices",
            "Low reserve voltage",
            "Roaming. Internet OFF"
    };
    static String[] alarms = {
            "ALARM Heavy shock",
            "ALARM Trunk",
            "ALARM Hood",
            "ALARM Doors",
            "ALARM Lock",
            "ALARM MovTilt sensor",
            "ALARM Rogue",
            "ALARM Ignition Lock"
    };
    static String[] msg = {
            "MOTOR ON OK",
            "Remote Engine Start OK",
            "MOTOR OFF OK",
    };

    static Pattern lostChannel = Pattern.compile("\\[(.*)\\] Control channel.BAD lost in ([0-9]{4})\\.([0-9]{2})\\.([0-9]{2}) ([0-9]{2}):([0-9]{2}):([0-9]{2})");
    static Pattern restoreChannel = Pattern.compile("\\[(.*)\\] Control channel.GOOD in ([0-9]{4})\\.([0-9]{2})\\.([0-9]{2}) ([0-9]{2}):([0-9]{2}):([0-9]{2})");

    static boolean compareNumbers(String config, String from) {
        if (config.length() == 4)
            return from.substring(from.length() - 4).equals(config);
        return PhoneNumberUtils.compare(config, from);
    }

    static boolean processMessageFromApi(Context context, String car_id, int id, long when) {
        if (Actions.inet_requests != null) {
            Set<Actions.InetRequest> requests = Actions.inet_requests.get(car_id);
            if (requests != null) {
                for (Actions.InetRequest request : requests) {
                    if (request.msg == id) {
                        request.done(context, when);
                        return true;
                    }
                }
            }
        }
        if (processed == null)
            return false;
        SmsQueues queues = processed.get(car_id);
        SmsQueue wait = queues.wait;
        if (wait == null)
            return false;
        if (!wait.containsKey(id))
            return false;
        Sms sms = wait.get(id);
        if (!sms.process_answer(context, car_id, null))
            return false;
        wait.remove(id);
        Intent i = new Intent(SMS_ANSWER);
        i.putExtra(Names.ANSWER, Activity.RESULT_OK);
        i.putExtra(Names.SMS_TEXT, sms.answer);
        i.putExtra(Names.ID, car_id);
        context.sendBroadcast(i);
        return true;
    }

    static boolean compare(String body, String message) {
        if (body.length() < message.length())
            return false;
        return body.substring(0, message.length()).equalsIgnoreCase(message);
    }

    static void showNotification(Context context, String text, String car_id) {
        showNotification(context, text, R.drawable.warning, car_id, null);
    }

    static void showNotification(Context context, String text, int picId, String car_id, String sound) {
        Alarm.createNotification(context, text, picId, car_id, sound, 0);
    }

    public static Class RILConstants() {
        try {
            return Class.forName("com.android.internal.telephony.RILConstants$SimCardID");
        } catch (Throwable throwable) {
            // ignore
        }
        return null;
    }

    static boolean sendSMS(Context context, String car_id, String pswd, Sms sms) {
        if (processed == null)
            processed = new HashMap<String, SmsQueues>();
        SmsQueues queues = processed.get(car_id);
        if (queues == null) {
            queues = new SmsQueues();
            processed.put(car_id, queues);
        }
        if (queues.send == null)
            queues.send = new SmsQueue();
        SmsQueue send = queues.send;
        if (send.containsKey(sms.id))
            return false;
        if ((queues.wait != null) && queues.wait.containsKey(sms.id))
            return false;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int sim_id = 0;
        if (State.isDualSim(context)) {
            try {
                sim_id = Integer.parseInt(preferences.getString(Names.Car.SIM + car_id, ""));
            } catch (Exception ex) {
                // ignore
            }
        }
        String phoneNumber = preferences.getString(Names.Car.CAR_PHONE + car_id, "");
        String text = sms.text;
        if (pswd != null)
            text = pswd + " " + text;

        Intent intent = new Intent(SMS_SENT);
        intent.putExtra(Names.ID, car_id);
        intent.putExtra(Names.ANSWER, sms.id);
        if (sim_id > 0)
            intent.putExtra("gear", (sim_id == 2));
        PendingIntent sendPI = PendingIntent.getBroadcast(context, sms.id, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String ver = "";
        try {
            PackageManager pkgManager = context.getPackageManager();
            PackageInfo info = pkgManager.getPackageInfo("net.ugona.plus", 0);
            ver = info.versionName;
        } catch (Exception ex) {
            // ignore
        }

        State.appendLog(sim_id + ": " + Build.MANUFACTURER + " " + Build.MODEL + " " + Build.VERSION.RELEASE + " " + ver);
        if (sim_id > 0) {
            try {
                // MediaTek
                SmsManagerEx smsManagerEx = SmsManagerEx.getDefault();
                smsManagerEx.sendTextMessage(phoneNumber, null, text, sendPI, null, sim_id - 1);
                Intent i = new Intent(SMS_SEND);
                i.putExtra(Names.ID, car_id);
                context.sendBroadcast(i);
                send.put(sms.id, sms);
                State.appendLog("MediaTek OK");
                return true;
            } catch (Exception ex) {
                // ignore
            }

            try {
                Class msm = ClassLoader.getSystemClassLoader().loadClass("android.telephony.MultiSimSmsManager");
                Class[] aclass = new Class[1];
                aclass[0] = Integer.TYPE;
                Method sms_get_default = msm.getMethod("getDefault", aclass);
                Object[] aobj = new Object[1];
                aobj[0] = Integer.valueOf(sim_id - 1);
                SmsManager smsManager = (SmsManager) sms_get_default.invoke(null, aobj);
                ArrayList<PendingIntent> pis = new ArrayList<PendingIntent>();
                pis.add(sendPI);
                smsManager.sendMultipartTextMessage(phoneNumber, null, smsManager.divideMessage(text), pis, null);
                Intent i = new Intent(SMS_SEND);
                i.putExtra(Names.ID, car_id);
                context.sendBroadcast(i);
                send.put(sms.id, sms);
                State.appendLog("GrandDuos OK");
                return true;
            } catch (Exception ex) {
                // ignore
            }

            try {
                // Galaxy Duos
                Class ril_class[] = new Class[1];
                ril_class[0] = RILConstants();
                Class sms_class = Class.forName("android.telephony.SmsManager");
                Method sms_get_default = sms_class.getMethod("getDefault", ril_class);
                String sim_id_str = "ID_ZERO";
                if (sim_id == 2)
                    sim_id_str = "ID_ONE";
                Object aobj[] = new Object[1];
                aobj[0] = Enum.valueOf(RILConstants(), sim_id_str);
                SmsManager smsManager = (SmsManager) sms_get_default.invoke(null, aobj);
                ArrayList<PendingIntent> pis = new ArrayList<PendingIntent>();
                pis.add(sendPI);
                smsManager.sendMultipartTextMessage(phoneNumber, null, smsManager.divideMessage(text), pis, null);
                Intent i = new Intent(SMS_SEND);
                i.putExtra(Names.ID, car_id);
                context.sendBroadcast(i);
                send.put(sms.id, sms);
                State.appendLog("Galaxy Duos OK");
                return true;
            } catch (Exception ex) {
                // ignore
            }
            State.appendLog("Sent default " + sim_id);
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, text, sendPI, null);
            Intent i = new Intent(SMS_SEND);
            i.putExtra(Names.ID, car_id);
            context.sendBroadcast(i);
            send.put(sms.id, sms);
            return true;
        } catch (Exception ex) {
            Toast toast = Toast.makeText(context, ex.getLocalizedMessage(), Toast.LENGTH_LONG);
            toast.show();
            ex.printStackTrace();
        }
        return false;
    }

    static boolean cancelSMS(Context context, String car_id, int id) {
        if (Actions.cancelRequest(context, car_id, id))
            return true;
        if (processed == null)
            return false;
        SmsQueues queues = processed.get(car_id);
        if (queues == null)
            return false;
        boolean result = false;
        if (queues.send != null)
            result = (queues.send.remove(id) != null);
        if (queues.wait != null)
            result |= (queues.wait.remove(id) != null);
        if (!result)
            return false;
        Intent i = new Intent(SMS_ANSWER);
        i.putExtra(Names.ANSWER, Activity.RESULT_CANCELED);
        i.putExtra(Names.ID, car_id);
        context.sendBroadcast(i);
        return true;
    }

    static boolean isProcessed(String car_id, int id) {
        if (isSmsProcessed(car_id, id))
            return true;
        if (Actions.inet_requests == null)
            return false;
        Set<Actions.InetRequest> requests = Actions.inet_requests.get(car_id);
        if (requests == null)
            return false;
        for (Actions.InetRequest request : requests) {
            if (request.msg == id)
                return true;
        }
        return false;
    }

    static boolean isSmsProcessed(String car_id, int id) {
        if (processed == null)
            return false;
        SmsQueues queues = processed.get(car_id);
        if (queues == null)
            return false;
        if ((queues.send != null) && queues.send.containsKey(id))
            return true;
        if ((queues.wait != null) && queues.wait.containsKey(id))
            return true;
        return false;
    }

    static boolean haveProcessed(String car_id) {
        if ((Actions.inet_requests != null) && (Actions.inet_requests.get(car_id) != null))
            return true;
        if (processed == null)
            return false;
        SmsQueues queues = processed.get(car_id);
        if (queues == null)
            return false;
        if ((queues.send != null) && !queues.send.isEmpty())
            return true;
        if ((queues.wait != null) && !queues.wait.isEmpty())
            return true;
        return false;

    }

    static boolean processCarMessage(Context context, String body, String car_id) {
        SmsQueues queues = null;
        if (processed != null)
            queues = processed.get(car_id);
        SmsQueue wait = null;
        if (queues != null)
            wait = queues.wait;
        if (wait != null) {
            Set<Map.Entry<Integer, Sms>> entries = wait.entrySet();
            for (Map.Entry<Integer, Sms> entry : entries) {
                String answer = entry.getValue().answer;
                if (compare(body, answer)) {
                    if (entry.getValue().process_answer(context, car_id, body.substring(answer.length()))) {
                        wait.remove(entry.getKey());
                        Intent i = new Intent(SMS_ANSWER);
                        i.putExtra(Names.ANSWER, Activity.RESULT_OK);
                        i.putExtra(Names.SMS_TEXT, body.substring(answer.length()));
                        i.putExtra(Names.ID, car_id);
                        context.sendBroadcast(i);
                        return true;
                    }
                }
                String sound = entry.getValue().process_error(body);
                if (sound != null) {
                    wait.remove(entry.getKey());
                    Intent i = new Intent(SMS_ANSWER);
                    i.putExtra(Names.ANSWER, Activity.RESULT_CANCELED);
                    i.putExtra(Names.ID, car_id);
                    context.sendBroadcast(i);
                    showNotification(context, context.getString(entry.getValue().error_msg), R.drawable.warning, car_id, sound);
                    return true;
                }
            }
        }
        for (int i = 0; i < notifications.length; i++) {
            if (compare(body, notifications[i])) {
                String[] msg = context.getString(R.string.notification).split("\\|");
                showNotification(context, msg[i], car_id);
                return true;
            }
        }
        for (int i = 0; i < alarms.length; i++) {
            if (compare(body, alarms[i])) {
                String[] msg = context.getString(R.string.alarm).split("\\|");
                showAlarm(context, msg[i], car_id);
                return true;
            }
        }
        if (compare(body, "ALARM Light shock")) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (preferences.getBoolean(Names.Car.LIGHT_SHOCK, true)) {
                showAlarm(context, context.getString(R.string.light_shock), car_id);
            } else {
                showNotification(context, context.getString(R.string.light_shock), car_id);
            }
            return true;
        }
        for (int i = 0; i < msg.length; i++) {
            if (compare(body, msg[i]))
                return true;
        }
        if (compare(body, "InGPSZone:")) {
            Alarm.zoneNotify(context, car_id, true, body.substring(10), true, false, 0);
            return true;
        }
        if (compare(body, "OutGPSZone:")) {
            Alarm.zoneNotify(context, car_id, false, body.substring(11), true, false, 0);
            return true;
        }

/*
        Matcher matcher = gps_pat.matcher(body);
        if (matcher.find()) {
            double lat = Actions.toWGS(Double.parseDouble(matcher.group(1)));
            double lng = Actions.toWGS(Double.parseDouble(matcher.group(2)));
            return true;
        }
        matcher = gsm_pat.matcher(body);
        if (matcher.find()) {
            int mc = Integer.parseInt(matcher.group(1));
            int nc = Integer.parseInt(matcher.group(1));
            int lac = Integer.parseInt(matcher.group(1));
            int cid = Integer.parseInt(matcher.group(1));
            return true;
        }
*/

        return false;
    }

    static private void showAlarm(Context context, String text, String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        ed.putBoolean(Names.SMS_ALARM, true);
        ed.commit();
        Intent alarmIntent = new Intent(context, Alarm.class);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        alarmIntent.putExtra(Names.Car.ALARM, text);
        alarmIntent.putExtra(Names.ID, car_id);
        context.startActivity(alarmIntent);
    }

//    Pattern gps_pat = Pattern.compile("\\&LAT=(-?[0-9]+\\.[0-9]+)\\&LON=(-?[0-9]+\\.[0-9]+)");
//    Pattern gsm_pat = Pattern.compile("\\&mcc=([0-9]+)\\&mnc=([0-9]+)\\&lac=([0-9]+)\\&cid=([0-9]+)");

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;
        String action = intent.getAction();
        if (action == null)
            return;
        if (action.equals(SMS_ACTION)) {
            String car_id = intent.getStringExtra(Names.ID);
            action = intent.getStringExtra("ACTION");
            if (action.equals("refresh")) {
                Intent i = new Intent(context, FetchService.class);
                i.putExtra(Names.ID, car_id);
                context.startService(i);
            }
            if (action.equals("motor_on"))
                Actions.motor_on_sms(context, car_id, true);
            if (action.equals("motor_off"))
                Actions.motor_off_sms(context, car_id, true);
            return;
        }
        if (action.equals(SMS_SENT)) {
            int result_code = getResultCode();
            String car_id = intent.getStringExtra(Names.ID);
            int id = intent.getIntExtra(Names.ANSWER, 0);
            if (processed == null)
                return;
            SmsQueues queues = processed.get(car_id);
            if (queues == null)
                return;
            Sms sms = queues.send.get(id);
            if (sms == null)
                return;
            queues.send.remove(id);
            if (result_code != Activity.RESULT_OK) {
                showNotification(context, context.getString(R.string.sms_error), car_id);
                Intent i = new Intent(SMS_ANSWER);
                i.putExtra(Names.ANSWER, result_code);
                i.putExtra(Names.ID, car_id);
                context.sendBroadcast(i);
                return;
            }
            if (sms.answer == null) {
                sms.process_answer(context, car_id, null);
                Intent i = new Intent(SMS_ANSWER);
                i.putExtra(Names.ANSWER, Activity.RESULT_OK);
                i.putExtra(Names.ID, car_id);
                context.sendBroadcast(i);
                return;
            }
            if (queues.wait == null)
                queues.wait = new SmsQueue();
            queues.wait.put(sms.id, sms);
            Intent i = new Intent(context, FetchService.class);
            i.putExtra(Names.ID, car_id);
            context.startService(i);
            Toast toast = Toast.makeText(context, context.getString(R.string.sms_sent), Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        if (action.equals(ACTION)) {
            Object[] pduArray = (Object[]) intent.getExtras().get("pdus");
            SmsMessage[] messages = new SmsMessage[pduArray.length];
            for (int i = 0; i < pduArray.length; i++) {
                messages[i] = SmsMessage.createFromPdu((byte[]) pduArray[i]);
            }
            String sms_from = messages[0].getOriginatingAddress();
            StringBuilder bodyText = new StringBuilder();
            for (SmsMessage m : messages) {
                bodyText.append(m.getMessageBody());
            }
            String body = bodyText.toString();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String[] cars = preferences.getString(Names.CARS, "").split(",");
            for (String car : cars) {
                String phone_config = preferences.getString(Names.Car.CAR_PHONE + car, "");
                if (compareNumbers(phone_config, sms_from) || State.isDebug()) {
                    if (processCarMessage(context, body, car)) {
                        abortBroadcast();
                        return;
                    }
                }
            }
            boolean restore = false;
            Matcher matcher = lostChannel.matcher(body);
            if (!matcher.matches()) {
                restore = true;
                matcher = restoreChannel.matcher(body);
            }
            if (matcher.matches()) {
                String login = matcher.group(1);
                for (String car : cars) {
                    String car_login = preferences.getString(Names.Car.LOGIN + car, "");
                    if (car_login.equals(login)) {
                        int year = Integer.parseInt(matcher.group(2));
                        int month = Integer.parseInt(matcher.group(4));
                        int day = Integer.parseInt(matcher.group(3));
                        int hour = Integer.parseInt(matcher.group(5));
                        int min = Integer.parseInt(matcher.group(6));
                        int sec = Integer.parseInt(matcher.group(7));
                        LocalDateTime time = new LocalDateTime(year, month, day, hour, min, sec);
                        SharedPreferences.Editor ed = preferences.edit();
                        if (restore) {
                            ed.remove(Names.Car.LOST + car);
                            int id = preferences.getInt(Names.Car.LOST_NOTIFY + car, 0);
                            if (id > 0) {
                                ed.remove(Names.Car.LOST_NOTIFY + car);
                                Alarm.removeNotification(context, car, id);
                                id = preferences.getInt(Names.Notify.RESTORE + car, 0);
                                if (id == 0) {
                                    id = Alarm.createNotification(context, context.getString(R.string.restore), R.drawable.gsm_restore, car, Names.Car.RESTORE_SOUND, 0);
                                    ed.putInt(Names.Notify.RESTORE + car, id);
                                }
                            }
                        } else {
                            ed.putLong(Names.Car.LOST + car, time.toDate().getTime());
                            int id = preferences.getInt(Names.Notify.RESTORE + car, 0);
                            if (id > 0) {
                                ed.remove(Names.Notify.RESTORE + car);
                                Alarm.removeNotification(context, car, id);
                            }
                            id = preferences.getInt(Names.Car.LOST_NOTIFY + car, 0);
                            if (id > 0) {
                                ed.remove(Names.Car.LOST_NOTIFY + car);
                                Alarm.removeNotification(context, car, id);
                            }
                            id = Alarm.createNotification(context, context.getString(R.string.lost), R.drawable.gsm_lost, car, Names.Car.LOST_SOUND, time.toDate().getTime(), true, null, null);
                            ed.putInt(Names.Car.LOST_NOTIFY + car, id);
                        }
                        ed.commit();
                        Intent i = new Intent(FetchService.ACTION_UPDATE_FORCE);
                        i.putExtra(Names.ID, car);
                        context.sendBroadcast(i);
                        abortBroadcast();
                        return;
                    }
                }
            }
        }
        if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
            DateTimeZone tz = DateTimeZone.getDefault();
            DateTimeZone.setDefault(tz);
            try {
                Intent i = new Intent(FetchService.ACTION_UPDATE_FORCE);
                context.sendBroadcast(i);
            } catch (Exception ex) {
                // ignore
            }
        }
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String[] cars = preferences.getString(Names.CARS, "").split(",");
            for (String id : cars) {
                Intent i = new Intent(context, FetchService.class);
                i.putExtra(Names.ID, id);
                i.setAction(FetchService.ACTION_UPDATE);
                context.startService(i);
            }
        }
    }

    static class Sms {
        int id;
        int error_msg;
        String text;
        String answer;
        String error;

        Sms(int id_, String text_, String answer_) {
            id = id_;
            text = text_;
            answer = answer_;
        }

        Sms(int id_, String text_, String answer_, String error_, int error_msg_) {
            id = id_;
            text = text_;
            answer = answer_;
            error = error_;
            error_msg = error_msg_;
        }

        boolean process_answer(Context context, String car_id, String text) {
            return true;
        }

        String process_error(String text) {
            if ((error != null) && compare(text, error))
                return "fail";
            return null;
        }
    }

    static class SmsQueue extends HashMap<Integer, Sms> {
    }

    static class SmsQueues {
        SmsQueue send;
        SmsQueue wait;
    }

}
