package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    static public void appendLog(String text) {
        Log.v("v", text);

        AsyncTask<String, Void, Void> task = new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... urlParameters) {
                try {
                    URL url = new URL("https://car-online.ugona.net/log");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type",
                            "application/x-www-form-urlencoded");

                    connection.setRequestProperty("Content-Length", "" +
                            Integer.toString(urlParameters[0].getBytes().length));
                    connection.setRequestProperty("Content-Language", "en-US");

                    connection.setUseCaches(false);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);

                    //Send request
                    DataOutputStream wr = new DataOutputStream(
                            connection.getOutputStream());
                    wr.writeBytes(urlParameters[0]);
                    wr.flush();
                    wr.close();

                    //Get Response
                    InputStream is = connection.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    String line;
                    StringBuffer response = new StringBuffer();
                    while ((line = rd.readLine()) != null) {
                        response.append(line);
                        response.append('\r');
                    }
                    rd.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }
        };
        task.execute(text);

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

}
