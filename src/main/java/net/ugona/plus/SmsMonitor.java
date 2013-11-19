package net.ugona.plus;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import org.joda.time.DateTimeZone;

import java.util.HashMap;
import java.util.Map;

public class SmsMonitor extends BroadcastReceiver {

    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
    private static final String SMS_SENT = "net.ugona.plus.SMS_SENT";

    static final String SMS_ANSWER = "net.ugona.plus.SMS_ANSWER";
    static final int INCORRECT_MESSAGE = 10001;

    static Map<String, String> answers;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;
        String action = intent.getAction();
        if (action == null)
            return;
        if (action.equals(SMS_SENT)) {
            int result_code = getResultCode();
            if (result_code != Activity.RESULT_OK) {
                Intent i = new Intent(SMS_ANSWER);
                i.putExtra(Names.ANSWER, result_code);
                i.putExtra(Names.ID, intent.getStringExtra(Names.ID));
                context.sendBroadcast(i);
            }
            String answer = intent.getStringExtra(Names.ANSWER);
            if (answer == null) {
                Intent i = new Intent(SMS_ANSWER);
                i.putExtra(Names.ANSWER, Activity.RESULT_OK);
                i.putExtra(Names.ID, intent.getStringExtra(Names.ID));
                context.sendBroadcast(i);
                return;
            }
            if (answers == null)
                answers = new HashMap<String, String>();
            answers.put(intent.getStringExtra(Names.ID), answer);
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
            State.appendLog("sms: " + body);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String[] cars = preferences.getString(Names.CARS, "").split(",");
            for (String car : cars) {
                String phone_config = digitsOnly(preferences.getString(Names.CAR_PHONE + car, ""));
                if ((phone_config.length() > 0) && phone_config.equals(digitsOnly(sms_from))) {
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
            if (preferences.getBoolean(Names.SAFE_MODE, false)) {
                String[] cars = preferences.getString(Names.CARS, "").split(",");
                for (String id : cars) {
                    Intent i = new Intent(context, FetchService.class);
                    i.putExtra(Names.ID, id);
                    i.setAction(FetchService.ACTION_UPDATE);
                    context.startService(i);
                }
            }
        }
    }

    String digitsOnly(String phone) {
        return phone.replaceAll("[^0-9]", "");
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

    boolean processCarMessage(Context context, String body, String car_id) {
        if ((answers != null) && answers.containsKey(car_id)) {
            String answer = answers.get(car_id);
            if (body.substring(0, answer.length()).equalsIgnoreCase(answer)) {
                answers.remove(car_id);
                Intent i = new Intent(SMS_ANSWER);
                i.putExtra(Names.ANSWER, Activity.RESULT_OK);
                i.putExtra(Names.SMS_TEXT, body.substring(answer.length()));
                i.putExtra(Names.ID, car_id);
                context.sendBroadcast(i);
                return true;
            }
            if (body.equals("Incorrect Message")) {
                answers.remove(car_id);
                Intent i = new Intent(SMS_ANSWER);
                i.putExtra(Names.ANSWER, INCORRECT_MESSAGE);
                i.putExtra(Names.ID, car_id);
                context.sendBroadcast(i);
                return true;
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
        return false;
    }

    static boolean compare(String body, String message) {
        if (body.length() < message.length())
            return false;
        return body.substring(0, message.length()).equalsIgnoreCase(message);
    }

    private void showNotification(Context context, String text, String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Alarm.createNotification(context, text, car_id);
        String sound = Preferences.getNotify(preferences, car_id);
        Uri uri = Uri.parse(sound);
        Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
        if (ringtone == null)
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(context, uri);
            if (audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) != 0) {
                player.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
                player.setLooping(false);
                player.prepare();
                player.start();
            }
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null)
                vibrator.vibrate(500);
        } catch (Exception err) {
            // ignore
        }
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

    static void sendSMS(Context context, String car_id, String sms, String answer) {
        Intent intent = new Intent(SMS_SENT);
        intent.putExtra(Names.ID, car_id);
        intent.putExtra(Names.ANSWER, answer);
        PendingIntent sendPI = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        SmsManager smsManager = SmsManager.getDefault();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String phoneNumber = preferences.getString(Names.CAR_PHONE + car_id, "");
        try {
            smsManager.sendTextMessage(phoneNumber, null, sms, sendPI, null);
        } catch (Exception ex) {
            try {
                sendPI.send(context, Activity.RESULT_CANCELED, intent);
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
