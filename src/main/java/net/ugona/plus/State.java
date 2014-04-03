package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.text.SimpleDateFormat;

public class State {

    static final int CMD_CALL = 1;
    static final int CMD_VALET = 1 << 1;
    static final int CMD_AZ = 1 << 2;
    static final int CMD_RELE = 1 << 3;
    static final int CMD_RELE1 = 1 << 4;
    static final int CMD_RELE2 = 1 << 5;
    static final int CMD_RELE1I = 1 << 6;
    static final int CMD_RELE2I = 1 << 7;
    static final int CMD_SOUND = 1 << 8;
    static final int CMD_SEARCH = 1 << 9;
    static int telephony_state = 0;

/*
    static void appendLog(String text) {
        File logFile = Environment.getExternalStorageDirectory();
        logFile = new File(logFile, "car.log");
        if (!logFile.exists()) {
            try {
                if (!logFile.createNewFile())
                    return;
            } catch (IOException e) {
                // ignore
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            Date d = new Date();
            buf.append(d.toLocaleString());
            buf.append(" ");
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // ignore
        }
    }

    static public void print(Throwable ex) {
        ex.printStackTrace();
        appendLog("Error: " + ex.toString());
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String s = sw.toString();
        appendLog(s);
    }
    */


    static boolean isDebug() {
        return Build.FINGERPRINT.startsWith("generic");
    }

    static boolean hasTelephony(Context context) {
        if (isDebug())
            return true;
        if (telephony_state == 0) {
            PackageManager pm = context.getPackageManager();
            if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                telephony_state = -1;
                return false;
            }
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm.getSimState() == TelephonyManager.SIM_STATE_ABSENT) {
                telephony_state = -1;
                return false;
            }
            telephony_state = 1;
        }
        return telephony_state > 0;
    }

    static String formatTime(Context context, long time) {
        try {
            if (Settings.System.getInt(context.getContentResolver(), Settings.System.TIME_12_24) == 12) {
                SimpleDateFormat sf = new SimpleDateFormat("KK:mm:ss a");
                return sf.format(time);
            }
        } catch (Exception ex) {
            // ignore
        }
        SimpleDateFormat sf = new SimpleDateFormat("HH:mm:ss");
        return sf.format(time);
    }

    static int getCommands(SharedPreferences preferences, String car_id) {
        int res = preferences.getInt(Names.COMMANDS + car_id, -1);
        if (res != -1)
            return res;
        res = CMD_VALET;
        if (preferences.getBoolean("autostart_" + car_id, false))
            res |= CMD_AZ;
        if (!preferences.getString(Names.CAR_RELE + car_id, "").equals(""))
            res |= CMD_RELE;
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(Names.COMMANDS + car_id, res);
        ed.commit();
        return res;
    }

}
