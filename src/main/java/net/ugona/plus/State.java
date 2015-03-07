package net.ugona.plus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.joda.time.LocalDate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class State {

    static private int telephony_state = 0;

/*
    static public void appendLog(String text) {
        Log.v("v", text);

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
            try {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm.getSimState() == TelephonyManager.SIM_STATE_ABSENT) {
                    telephony_state = -1;
                    return false;
                }
                telephony_state = 1;
            } catch (Exception ex) {
                telephony_state = -1;
            }
        }
        return telephony_state > 0;
    }

    static boolean isValidPhoneNumber(String number) {
        if (isDebug())
            return true;
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber n = phoneUtil.parse(number, Locale.getDefault().getCountry());
            return phoneUtil.isValidNumber(n);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    static String formatPhoneNumber(String number) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber n = phoneUtil.parse(number, Locale.getDefault().getCountry());
            return phoneUtil.format(n, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (isDebug())
            return number;
        return null;
    }

    static String getVersion(Context context) {
        try {
            PackageManager pkgManager = context.getPackageManager();
            PackageInfo info = pkgManager.getPackageInfo("net.ugona.plus", 0);
            return info.versionName;
        } catch (Exception ex) {
            // ignore
        }
        return "";
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
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sf = new SimpleDateFormat("HH:mm:ss");
        return sf.format(time);
    }

    static String formatDateTime(Context context, long time) {
        LocalDate now = new LocalDate();
        LocalDate date = new LocalDate(time);
        if (now.equals(date))
            return formatTime(context, time);
        DateFormat df = android.text.format.DateFormat.getDateFormat(context);
        return formatTime(context, time) + " " + df.format(time);
    }

    static int GsmLevel(int level) {
        if (level > -51)
            return 5;
        if (level > -65)
            return 4;
        if (level > -77)
            return 3;
        if (level > -91)
            return 2;
        if (level > -105)
            return 1;
        return 0;
    }

}
