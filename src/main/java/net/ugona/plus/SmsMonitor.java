package net.ugona.plus;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import org.joda.time.DateTimeZone;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SmsMonitor extends BroadcastReceiver {

    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

    static final String SMS_SENT = "net.ugona.plus.SMS_SENT";
    static final String SMS_ANSWER = "net.ugona.plus.SMS_ANSWER";
    static final String SMS_SEND = "net.ugona.plus.SMS_SEND";

    static class Sms {
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

        int id;
        int error_msg;

        String text;
        String answer;
        String error;

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

    static Map<String, SmsQueues> processed;

    static boolean compareNumbers(String config, String from) {
        if (config.length() == 4)
            return from.substring(from.length() - 4).equals(config);
        return PhoneNumberUtils.compare(config, from);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;
        String action = intent.getAction();
        if (action == null)
            return;
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
                String phone_config = preferences.getString(Names.CAR_PHONE + car, "");
                if (compareNumbers(phone_config, sms_from) || State.isDebug()) {
                    if (processCarMessage(context, body, car))
                        abortBroadcast();
                    return;
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

    static String[] notifications = {
            "ALARM Light shock",
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

    static boolean processMessageFromApi(Context context, String car_id, int id) {
        if (Actions.inet_requests != null) {
            State.appendLog("!= null " + car_id);
            Set<Actions.InetRequest> requests = Actions.inet_requests.get(car_id);
            if (requests != null) {
                State.appendLog("! null");
                for (Actions.InetRequest request : requests) {
                    State.appendLog("> " + request.msg + ", " + id);
                    if (request.msg == id) {
                        State.appendLog("done....");
                        request.done(context);
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

    boolean processCarMessage(Context context, String body, String car_id) {
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
        for (int i = 0; i < msg.length; i++) {
            if (compare(body, msg[i]))
                return true;
        }
        return false;
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
        Alarm.createNotification(context, text, picId, car_id, sound);
    }

    private void showAlarm(Context context, String text, String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        ed.putBoolean(Names.SMS_ALARM, true);
        ed.commit();
        Intent alarmIntent = new Intent(context, Alarm.class);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        alarmIntent.putExtra(Names.ALARM, text);
        alarmIntent.putExtra(Names.ID, car_id);
        context.startActivity(alarmIntent);
    }

    static boolean sendSMS(Context context, String car_id, Sms sms) {
        if (processed == null)
            processed = new HashMap<String, SmsQueues>();
        if (!processed.containsKey(car_id))
            processed.put(car_id, new SmsQueues());
        SmsQueues queues = processed.get(car_id);
        if (queues.send == null)
            queues.send = new SmsQueue();
        SmsQueue send = queues.send;
        if (send.containsKey(sms.id))
            return false;
        if ((queues.wait != null) && queues.wait.containsKey(sms.id))
            return false;
        Intent intent = new Intent(SMS_SENT);
        intent.putExtra(Names.ID, car_id);
        intent.putExtra(Names.ANSWER, sms.id);
        PendingIntent sendPI = PendingIntent.getBroadcast(context, sms.id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        SmsManager smsManager = SmsManager.getDefault();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String phoneNumber = preferences.getString(Names.CAR_PHONE + car_id, "");
        try {
            smsManager.sendTextMessage(phoneNumber, null, sms.text, sendPI, null);
            Intent i = new Intent(SMS_SEND);
            i.putExtra(Names.ID, car_id);
            context.sendBroadcast(i);
        } catch (Exception ex) {
            return false;
        }
        send.put(sms.id, sms);
        return true;
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

}
