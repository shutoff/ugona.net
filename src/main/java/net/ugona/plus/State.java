package net.ugona.plus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.joda.time.LocalDate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class State {

    static final Pattern ok_bool = Pattern.compile("^([A-Za-z0-9_]+)$");

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
    static final Pattern not_bool = Pattern.compile("^\\!([A-Za-z0-9_]+)$");
    static final Pattern eq_int = Pattern.compile("^([A-Za-z0-9_]+)=([0-9]+)$");
    static final Pattern ne_int = Pattern.compile("^([A-Za-z0-9_]+)\\!=([0-9]+)$");
    static private int telephony_state = 0;

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
        return 1;
    }

    static Spannable createSpans(String str, int color, boolean bold) {
        String[] parts = str.split("\\|");
        StringBuilder builder = new StringBuilder();
        for (String p : parts) {
            builder.append(p);
        }
        Spannable spannable = new SpannableString(builder.toString());
        int pos = 0;
        for (int i = 0; i + 1 < parts.length; i += 2) {
            pos += parts[i].length();
            int end = pos + parts[i + 1].length();
            if (bold)
                spannable.setSpan(new StyleSpan(Typeface.BOLD), pos, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(color), pos, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos = end;
        }
        return spannable;
    }

    static void forEachViews(ViewGroup group, ViewCallback cb) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            cb.view(v);
            if (v instanceof ViewGroup)
                forEachViews((ViewGroup) v, cb);
        }
    }

    static boolean checkCondition(String condition, Object o) {
        Matcher m = ok_bool.matcher(condition);
        try {
            if (m.find()) {
                String id = m.group(1);
                Field field = o.getClass().getDeclaredField(id);
                if (field.getType() != boolean.class) {
                    Log.v("check", id + " is not boolean");
                    return false;
                }
                field.setAccessible(true);
                return field.getBoolean(o);
            }
            m = not_bool.matcher(condition);
            if (m.find()) {
                String id = m.group(1);
                Field field = o.getClass().getDeclaredField(id);
                if (field.getType() != boolean.class) {
                    Log.v("check", id + " is not boolean");
                    return false;
                }
                field.setAccessible(true);
                return !field.getBoolean(o);
            }
            m = eq_int.matcher(condition);
            if (m.find()) {
                String id = m.group(1);
                Field field = o.getClass().getDeclaredField(id);
                if (field.getType() != int.class) {
                    Log.v("check", id + " is not integer");
                    return false;
                }
                field.setAccessible(true);
                int v = field.getInt(o);
                int test = Integer.parseInt(m.group(2));
                return v == test;
            }
            m = ne_int.matcher(condition);
            if (m.find()) {
                String id = m.group(1);
                Field field = o.getClass().getDeclaredField(id);
                if (field.getType() != int.class) {
                    Log.v("check", id + " is not integer");
                    return false;
                }
                field.setAccessible(true);
                int v = field.getInt(o);
                int test = Integer.parseInt(m.group(2));
                return v != test;
            }
            Log.v("check", "Bad condition: " + condition);
        } catch (Exception ex) {
            Log.v("check", ex.getMessage());
        }
        return false;
    }

    static boolean isDualSim(Context context) {
        TelephonyInfo info = TelephonyInfo.getInstance(context);
        return info.isSIM1Ready(context) && info.isSIM2Ready(context);
    }

    public static interface ViewCallback {
        void view(View v);
    }

    static public final class TelephonyInfo {

        private static TelephonyInfo telephonyInfo;
        Object msim;
        Method msim_getState;

        private TelephonyInfo() {
            try {
                Class msim_class = Class.forName("android.telephony.MSimTelephonyManager");
                Method msim_default = msim_class.getDeclaredMethod("getDefault", new Class[0]);
                msim = msim_default.invoke(null, new Object[0]);
                Class aclass[] = new Class[1];
                aclass[0] = Integer.TYPE;
                msim_getState = msim_class.getDeclaredMethod("getSimState", aclass);
            } catch (Exception ex) {
                // ignore
            }
        }

        public static TelephonyInfo getInstance(Context context) {

            if (telephonyInfo == null) {
                telephonyInfo = new TelephonyInfo();
                TelephonyManager telephonyManager = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
            }
            return telephonyInfo;
        }

        private boolean getSIMStateBySlot(Context context, String predictedMethodName, int slotID) throws Exception {

            if ((msim != null) && (msim_getState != null)) {
                try {
                    Object aobj[] = new Object[1];
                    aobj[0] = Integer.valueOf(slotID);
                    return ((Integer) msim_getState.invoke(msim, aobj)).intValue() == TelephonyManager.SIM_STATE_READY;
                } catch (Exception ex) {
                    // ignore
                }
            }

            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Class<?> telephonyClass = Class.forName(telephony.getClass().getName());

            Class<?>[] parameter = new Class[1];
            parameter[0] = int.class;
            Method getSimStateGemini = telephonyClass.getMethod(predictedMethodName, parameter);

            Object[] obParameter = new Object[1];
            obParameter[0] = slotID;
            Object ob_phone = getSimStateGemini.invoke(telephony, obParameter);

            if (ob_phone == null)
                return false;

            int simState = Integer.parseInt(ob_phone.toString());
            return (simState == TelephonyManager.SIM_STATE_READY);
        }

        public boolean isSIM1Ready(Context context) {
            try {
                return getSIMStateBySlot(context, "getSimStateGemini", 0);
            } catch (Exception ex) {
                // ignore
            }
            try {
                return getSIMStateBySlot(context, "getSimState", 0);
            } catch (Exception ex) {
                // ignore
            }

            TelephonyManager telephonyManager = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
            return telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
        }

        public boolean isSIM2Ready(Context context) {
            try {
                return getSIMStateBySlot(context, "getSimStateGemini", 1);
            } catch (Exception ex) {
                // ignore
            }
            try {
                return getSIMStateBySlot(context, "getSimState", 1);
            } catch (Exception ex) {
                // ignore
            }
            return false;
        }

    }


}
