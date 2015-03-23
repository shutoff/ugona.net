package net.ugona.plus;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.widget.Toast;

public class Sms {

    static boolean send(Context context, String id, int cmd_id, String text) {
        CarConfig config = CarConfig.get(context, id);
        String phone = config.getPhone();
        int sim = config.getSim_cmd();
        if (!State.isDualSim(context))
            sim = 0;
        Intent intent = new Intent(Names.SMS_SENT);
        intent.putExtra(Names.ID, id);
        intent.putExtra(Names.COMMAND, cmd_id);
        PendingIntent sendPI = PendingIntent.getBroadcast(context, cmd_id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phone, null, text, sendPI, null);
            return true;
        } catch (Exception ex) {
            Toast toast = Toast.makeText(context, ex.getLocalizedMessage(), Toast.LENGTH_LONG);
            toast.show();
            ex.printStackTrace();
        }
        return false;
    }
}
