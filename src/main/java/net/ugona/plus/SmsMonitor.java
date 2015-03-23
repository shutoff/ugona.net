package net.ugona.plus;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.widget.Toast;

import java.util.Set;

public class SmsMonitor extends BroadcastReceiver {
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
                    Toast toast = Toast.makeText(context, R.string.sms_error, Toast.LENGTH_LONG);
                    toast.show();
                    Commands.remove(context, id, c);
                    return;
                }
                Toast toast = Toast.makeText(context, R.string.sms_sent, Toast.LENGTH_LONG);
                toast.show();
                String[] sms = c.sms.split("\\!");
                if (sms.length == 1) {
                    if (c.done != null) {
                        CarState state = CarState.get(context, id);
                        Set<String> upd = State.update(c.done, state);
                        if (upd != null) {
                            Intent i = new Intent(Names.UPDATED);
                            i.putExtra(Names.ID, id);
                            context.sendBroadcast(i);
                            Notification.update(context, id, upd);
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
                    if (!compareNumbers(carConfig.getPhone(), sms_from) && !State.isDebug())
                        continue;
                    if (Commands.processSms(context, car_id, body)) {
                        abortBroadcast();
                        return;
                    }
                }
            }
        }
    }
}
