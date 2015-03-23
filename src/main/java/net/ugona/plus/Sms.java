package net.ugona.plus;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.mediatek.telephony.SmsManagerEx;

import java.lang.reflect.Method;
import java.util.ArrayList;

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

        if (sim > 0) {
            try {
                // MediaTek
                SmsManagerEx smsManagerEx = SmsManagerEx.getDefault();
                smsManagerEx.sendTextMessage(phone, null, text, sendPI, null, sim - 1);
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
                aobj[0] = Integer.valueOf(sim - 1);
                SmsManager smsManager = (SmsManager) sms_get_default.invoke(null, aobj);
                ArrayList<PendingIntent> pis = new ArrayList<PendingIntent>();
                pis.add(sendPI);
                smsManager.sendMultipartTextMessage(phone, null, smsManager.divideMessage(text), pis, null);
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
                if (sim == 2)
                    sim_id_str = "ID_ONE";
                Object aobj[] = new Object[1];
                aobj[0] = Enum.valueOf(RILConstants(), sim_id_str);
                SmsManager smsManager = (SmsManager) sms_get_default.invoke(null, aobj);
                ArrayList<PendingIntent> pis = new ArrayList<PendingIntent>();
                pis.add(sendPI);
                smsManager.sendMultipartTextMessage(phone, null, smsManager.divideMessage(text), pis, null);
                return true;
            } catch (Exception ex) {
                // ignore
            }
        }

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


    public static Class RILConstants() {
        try {
            return Class.forName("com.android.internal.telephony.RILConstants$SimCardID");
        } catch (Throwable throwable) {
            // ignore
        }
        return null;
    }
}
