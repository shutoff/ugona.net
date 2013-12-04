package net.ugona.plus;

/*
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
*/

import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;

public class State {

    static int telephony_state = 0;

    static boolean hasTelephony(Context context) {
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
}
