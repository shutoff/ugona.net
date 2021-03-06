package net.ugona.plus;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.widget.Toast;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsMonitor extends BroadcastReceiver {
    static boolean compareNumbers(String config, String from) {
        if (config.length() == 4)
            return from.substring(from.length() - 4).equals(config);
        return PhoneNumberUtils.compare(config, from);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent == null)
            return;
        String action = intent.getAction();
        if (action == null)
            return;
        if (action.equals(Names.SMS_SENT)) {
            String id = intent.getStringExtra(Names.ID);
            int cmd_id = intent.getIntExtra(Names.COMMAND, 0);
            CarConfig config = CarConfig.get(context, id);
            CarConfig.Command[] cmd = config.getCmd();
            for (CarConfig.Command c : cmd) {
                if (c.id != cmd_id)
                    continue;
                if (!Commands.isProcessed(id, c))
                    continue;
                if (c.sms == null)
                    return;
                if (getResultCode() != Activity.RESULT_OK) {
                    String text = context.getString(R.string.sms_error);
                    String actions = FetchService.ACTION_COMMAND + ";";
                    actions += c.id + "|" + "|:" + android.R.drawable.ic_popup_sync + ":" + context.getString(R.string.retry);
                    Notification.create(context, text, R.drawable.w_warning_light, id, null, 0, false, c.name, actions);
                    Commands.remove(context, id, c);
                    return;
                }
                Toast toast = Toast.makeText(context, R.string.sms_sent, Toast.LENGTH_LONG);
                toast.show();
                String[] sms = c.sms.split("\\|");
                if (sms.length == 1) {
                    Intent data = Commands.remove(context, id, c);
                    if (c.next > 0) {
                        for (CarConfig.Command next : cmd) {
                            if (next.id == c.next) {
                                String sms_text = next.smsText(data);
                                if (Sms.send(context, id, c.next, sms_text))
                                    Commands.put(context, id, next, data);
                            }
                        }
                    }
                    if (c.done != null) {
                        CarState state = CarState.get(context, id);
                        Set<String> upd = State.update(context, id, c, c.done, state, null, null);
                        if (upd != null) {
                            Intent i = new Intent(Names.UPDATED);
                            i.putExtra(Names.ID, id);
                            context.sendBroadcast(i);
                            Notification.update(context, id, upd, true);
                        }
                    }
                }
            }
        }
        if (action.equals("android.provider.Telephony.SMS_RECEIVED")) {
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
            AppConfig appConfig = AppConfig.get(context);
            String[] cars = appConfig.getCars();
            for (String car_id : cars) {
                CarConfig carConfig = CarConfig.get(context, car_id);
                if (!compareNumbers(carConfig.getPhone(), sms_from) && !State.isDebug()) {
                    if (processSms(context, car_id, body, false))
                        return;
                    continue;
                }
                if (Commands.processSms(context, car_id, body)) {
                    abortBroadcast();
                    return;
                }
                if (processSms(context, car_id, body, true))
                    return;
            }
        }
        if (action.equals("com.twofortyfouram.locale.intent.action.FIRE_SETTING")) {
            Bundle data = intent.getBundleExtra(EditActivity.EXTRA_BUNDLE);
            String car_id = data.getString(Names.ID);
            CarConfig carConfig = CarConfig.get(context, car_id);
            int command = data.getInt(Names.COMMAND);
            CarConfig.Command[] cmds = carConfig.getCmd();
            for (CarConfig.Command cmd : cmds) {
                if (cmd.id != command)
                    continue;
                if (cmd.call != null) {
                    String phone = carConfig.getPhone();
                    if (phone.equals(""))
                        return;
                    phone = cmd.call.replace("{phone}", phone);
                    Intent i = new Intent(android.content.Intent.ACTION_CALL, Uri.parse("tel:" + phone));
                    context.startActivity(i);
                    return;
                }
                boolean inet = (cmd.inet > 0);
                boolean sms = (cmd.sms != null);
                if (!State.hasTelephony(context))
                    sms = false;
                if (inet && sms) {
                    if (data.getInt(Names.ROUTE) > 0) {
                        sms = false;
                    } else {
                        inet = false;
                    }
                }
                if (sms) {
                    Intent d = new Intent();
                    d.putExtra("ccode", data.getString("ccode"));
                    d.putExtra("pwd", data.getString("pwd"));
                    String sms_text = cmd.smsText(d);
                    if (Sms.send(context, car_id, cmd.id, sms_text)) {
                        Commands.put(context, car_id, cmd, d);
                        Intent i = new Intent(context, FetchService.class);
                        intent.setAction(FetchService.ACTION_UPDATE);
                        intent.putExtra(Names.ID, car_id);
                        context.startService(intent);
                    }
                    return;
                }
                if (inet)
                    SendCommandFragment.send_inet(context, cmd, car_id, data.getString("ccode"));
                return;
            }
        }
    }

    boolean processSms(Context context, String car_id, String body, boolean all) {
        CarConfig carConfig = CarConfig.get(context, car_id);
        CarConfig.Sms[] sms = carConfig.getSms();
        if (sms == null)
            return false;
        for (CarConfig.Sms s : sms) {
            if (!all && !s.all)
                continue;
            try {
                Pattern pattern = Pattern.compile(s.sms);
                Matcher matcher = pattern.matcher(body);
                if (!matcher.find())
                    continue;
                Set<String> changed = null;
                if (s.set != null) {
                    CarState state = CarState.get(context, car_id);
                    changed = State.update(context, car_id, null, s.set, state, matcher, null);
                }
                if (s.alarm != null) {
                    Intent alarmIntent = new Intent(context, Alarm.class);
                    alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    alarmIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    alarmIntent.putExtra(Names.TITLE, s.alarm);
                    alarmIntent.putExtra(Names.ID, car_id);
                    context.startActivity(alarmIntent);
                    abortBroadcast();
                    return true;
                }
                if (s.notify != null) {
                    Notification.showAlarm(context, car_id, s.notify);
                    abortBroadcast();
                    return true;
                }
                if (changed != null)
                    Notification.update(context, car_id, changed, false);
                abortBroadcast();
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

}
