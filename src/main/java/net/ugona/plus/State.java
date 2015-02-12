package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.seppius.i18n.plurals.PluralResources;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    static final int CMD_THERMOCODE = 1 << 10;
    static final int CMD_GUARD = 1 << 11;
    static final int CMD_TRUNK = 1 << 12;
    static int telephony_state = 0;
    static Pattern balancePat1 = Pattern.compile("minus[^0-9]*([0-9]+([\\.,][0-9][0-9])?)");
    static Pattern balancePat2 = Pattern.compile("balans: ?(-[0-9]+([\\.,][0-9][0-9])?)");
    static Pattern balancePat3 = Pattern.compile("-?[0-9]+[\\.,][0-9][0-9]");
    static Pattern balancePat4 = Pattern.compile("(-?[0-9]+) ?R(\\. ?([0-9][0-9]))?");
    static int dual_sim = -1;

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

    static String getPlural(Context context, int id, int n) {
        if (android.os.Build.VERSION.SDK_INT < 11) {
            try {
                PluralResources pluralizer = new PluralResources(context.getResources());
                return pluralizer.getQuantityString(id, n, n);
            } catch (Throwable t) {
            }
        }
        return context.getResources().getQuantityString(id, n, n);
    }

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

    static boolean isDualSim(Context context) {
        TelephonyInfo info = TelephonyInfo.getInstance(context);
        return info.isSIM1Ready(context) && info.isSIM2Ready(context);
    }

    static String formatBalance(double d) {
        if ((int) d == d)
            return String.format("%,d", (int) d);
        return String.format("%,.2f", d);
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

    static boolean isCard(SharedPreferences preferences, String car_id) {
        String ver = preferences.getString(Names.Car.VERSION + car_id, "");
        return ver.indexOf("Logistic") < 0;
    }

    static int getCommands(SharedPreferences preferences, String car_id) {
        int res = preferences.getInt(Names.Car.COMMANDS + car_id, -1);
        if (res != -1)
            return res;
        res = CMD_VALET;
        if (preferences.getBoolean("autostart_" + car_id, false))
            res |= CMD_AZ;
        if (!preferences.getString(Names.Car.CAR_RELE + car_id, "").equals(""))
            res |= CMD_RELE;
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(Names.Car.COMMANDS + car_id, res);
        ed.commit();
        return res;
    }

    static boolean isPandora(SharedPreferences preferences, String car_id) {
        String key = preferences.getString(Names.Car.CAR_KEY + car_id, "");
        if (key.length() < 3)
            return false;
        return key.substring(0, 2).equals("P_");
    }

    static boolean getOffline(SharedPreferences preferences, String car_id) {
        if (isPandora(preferences, car_id))
            return preferences.getBoolean(Names.Car.OFFLINE + car_id, false);
        long last_event = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
        long interval = preferences.getInt(Names.Car.CAR_TIMER + car_id, 10) + 1;
        if (preferences.getBoolean(Names.Car.POINTER + car_id, false)) {
            interval = 60 * 25;
            String mode = preferences.getString(Names.Car.POINTER_MODE + car_id, "");
            if (mode.equals("a"))
                interval = 60 * 7;
            if (mode.equals("b"))
                interval = 60 * 13;
            if (mode.equals("d"))
                interval = 0;
        }
        last_event += interval * 60000;
        Date now = new Date();
        long now_time = now.getTime();
        return last_event < now_time;
    }

    static String parseBalance(String source) {
        Matcher matcher = balancePat1.matcher(source);
        if (matcher.find()) {
            try {
                double v = Double.parseDouble(matcher.group(1).replace(",", "."));
                return -v + "";
            } catch (Exception ex) {
                // ignore
            }
        }
        matcher = balancePat2.matcher(source);
        if (matcher.find()) {
            try {
                double v = Double.parseDouble(matcher.group(1).replace(",", "."));
                return v + "";
            } catch (Exception ex) {
                // ignore
            }
        }
        matcher = balancePat3.matcher(source);
        if (matcher.find()) {
            try {
                double v = Double.parseDouble(matcher.group(0).replace(",", "."));
                return v + "";
            } catch (Exception ex) {
                // ignore
            }
        }
        matcher = balancePat4.matcher(source);
        if (matcher.find()) {
            try {
                double v = Double.parseDouble(matcher.group(1));
                try {
                    if (v >= 0) {
                        v += Double.parseDouble(matcher.group(3)) / 100.;
                    } else {
                        v -= Double.parseDouble(matcher.group(3)) / 100;
                    }
                } catch (Exception e) {
                    // ignore
                }
                return v + "";
            } catch (Exception ex) {
                // ignore
            }
        }
        return null;
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

            State.appendLog("State OK " + slotID + " " + predictedMethodName);
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
