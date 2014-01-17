package net.ugona.plus;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Actions {

    static final String INCORRECT_MESSAGE = "Incorrect message";

    static PendingIntent piRele;

    static final int VALET_TIMEOUT = 3600000;

    static void done_motor_on(Context context, String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int id = preferences.getInt(Names.MOTOR_ON_NOTIFY + car_id, 0);
        if (id != 0)
            return;
        id = preferences.getInt(Names.MOTOR_OFF_NOTIFY + car_id, 0);
        if (id != 0)
            Alarm.removeNotification(context, car_id, id);
        id = Alarm.createNotification(context, context.getString(R.string.motor_on_ok), R.drawable.white_motor_on, car_id, "start");
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(Names.MOTOR_ON_NOTIFY + car_id, id);
        ed.remove(Names.MOTOR_OFF_NOTIFY + car_id);
        ed.commit();
    }

    static void done_motor_off(Context context, String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int id = preferences.getInt(Names.MOTOR_OFF_NOTIFY + car_id, 0);
        if (id != 0)
            return;
        id = preferences.getInt(Names.MOTOR_ON_NOTIFY + car_id, 0);
        if (id != 0)
            Alarm.removeNotification(context, car_id, id);
        String msg = context.getString(R.string.motor_off_ok);
        long az_stop = preferences.getLong(Names.AZ_STOP + car_id, 0);
        long az_start = preferences.getLong(Names.AZ_START + car_id, 0);
        long time = (az_stop - az_start) / 60000;
        if ((time > 0) && (time <= 20))
            msg += " " + context.getString(R.string.motor_time).replace("$1", time + "");
        id = Alarm.createNotification(context, msg, R.drawable.white_motor_off, car_id, null);
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(Names.MOTOR_OFF_NOTIFY + car_id, id);
        ed.remove(Names.MOTOR_ON_NOTIFY + car_id);
        ed.commit();
    }

    static void done_valet_on(Context context, String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        int id = preferences.getInt(Names.VALET_ON_NOTIFY + car_id, 0);
        if (id != 0)
            return;
        id = preferences.getInt(Names.VALET_OFF_NOTIFY + car_id, 0);
        if (id != 0)
            Alarm.removeNotification(context, car_id, id);
        id = Alarm.createNotification(context, context.getString(R.string.valet_on_ok), R.drawable.white_valet_on, car_id, "valet_on");
        ed.putInt(Names.VALET_ON_NOTIFY + car_id, id);
        ed.remove(Names.VALET_OFF_NOTIFY + car_id);
        ed.commit();
    }

    static void done_valet_off(Context context, String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        int id = preferences.getInt(Names.VALET_OFF_NOTIFY + car_id, 0);
        if (id != 0)
            return;
        id = preferences.getInt(Names.VALET_ON_NOTIFY + car_id, 0);
        if (id != 0)
            Alarm.removeNotification(context, car_id, id);
        id = Alarm.createNotification(context, context.getString(R.string.valet_off_ok), R.drawable.white_valet_off, car_id, "valet_off");
        ed.putInt(Names.VALET_OFF_NOTIFY + car_id, id);
        ed.remove(Names.VALET_ON_NOTIFY + car_id);
        ed.commit();
    }

    static void motor_on(final Context context, final String car_id) {
        requestPassword(context, R.string.motor_on, R.string.motor_on_sum, new Runnable() {
            @Override
            public void run() {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor ed = preferences.edit();
                ed.putBoolean(Names.ENGINE + car_id, false);
                ed.commit();
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.motor_on, "MOTOR ON", "", "MOTOR ON FAIL", R.string.motor_start_error) {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        if ((text == null) ||
                                SmsMonitor.compare(text, "MOTOR ON OK") ||
                                SmsMonitor.compare(text, "Remote Engine Start OK")) {
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                            SharedPreferences.Editor ed = preferences.edit();
                            ed.putBoolean(Names.AZ + car_id, true);
                            long now = new Date().getTime();
                            if (now > preferences.getLong(Names.AZ_START + car_id, 0) + 60000)
                                ed.putLong(Names.AZ_START + car_id, now);
                            ed.commit();
                            try {
                                Intent intent = new Intent(FetchService.ACTION_UPDATE);
                                intent.putExtra(Names.ID, car_id);
                                context.sendBroadcast(intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            done_motor_on(context, car_id);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    String process_error(String text) {
                        if (SmsMonitor.compare(text, "ERROR;Engine") || SmsMonitor.compare(text, error))
                            return "engine_fail";
                        return null;
                    }
                });
            }
        });
    }

    static void motor_off(final Context context, final String car_id) {
        requestPassword(context, R.string.motor_off, R.string.motor_off_sum, new Runnable() {
            @Override
            public void run() {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.motor_off, "MOTOR OFF", "MOTOR OFF OK") {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putBoolean(Names.AZ + car_id, false);
                        long now = new Date().getTime();
                        if (now > preferences.getLong(Names.AZ_STOP + car_id, 0) + 60000)
                            ed.putLong(Names.AZ_STOP + car_id, now);
                        ed.commit();
                        try {
                            Intent intent = new Intent(FetchService.ACTION_UPDATE);
                            intent.putExtra(Names.ID, car_id);
                            context.sendBroadcast(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        done_motor_off(context, car_id);
                        return true;
                    }
                });
            }
        });
    }

    static void turbo_on(final Context context, final String car_id) {
        requestPassword(context, R.string.turbo_on, R.string.turbo_on_sum, new Runnable() {
            @Override
            public void run() {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.turbo_on, "TURBO ON", "TURBO ON OK"));
            }
        });
    }

    static void turbo_off(final Context context, final String car_id) {
        requestPassword(context, R.string.turbo_off, R.string.turbo_off_sum, new Runnable() {
            @Override
            public void run() {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.turbo_off, "TURBO OFF", "TURBO OFF OK"));
            }
        });
    }

    static void internet_on(final Context context, final String car_id) {
        requestPassword(context, R.string.internet_on, R.string.internet_on_sum, new Runnable() {
            @Override
            public void run() {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.internet_on, "INTERNET ALL", "INTERNET ALL OK"));
            }
        });
    }

    static void internet_off(final Context context, final String car_id) {
        requestPassword(context, R.string.internet_off, R.string.internet_off_sum, new Runnable() {
            @Override
            public void run() {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.internet_off, "INTERNET OFF", "INTERNET OFF OK"));
            }
        });
    }

    static void reset(final Context context, final String car_id) {
        requestPassword(context, R.string.reset, R.string.reset_sum, new Runnable() {
            @Override
            public void run() {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.reset, "RESET", null));
            }
        });
    }

/*
    static Pattern location;

    static void map_query(final Context context, final String car_id) {
        requestPassword(context, R.string.map_req, R.string.map_req, new Runnable() {
            @Override
            public void run() {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.map_req, "MAP", "") {
                    @Override
                    boolean process_answer(final Context context, final String car_id, String text) {
                        if (location == null)
                            location = Pattern.compile("\\&lat=(-?[0-9]+\\.[0-9]+)\\&lon=(-?[0-9]+\\.[0-9]+)");
                        Matcher matcher = location.matcher(text);
                        if (!matcher.find())
                            return false;
                        try {
                            double lon = Double.parseDouble(matcher.group(1));
                            double lat = Double.parseDouble(matcher.group(2));
                            double[] coord = utm2geo(lon, lat, 35, 1);

                            Intent intent = new Intent(context, StatusDialog.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra(Names.TITLE, context.getString(R.string.map_req));
                            intent.putExtra(Names.SMS_TEXT, text);
                            String s = coord[0] + "";
                            if (s.length() > 7)
                                s = s.substring(0, 7);
                            intent.putExtra(Names.LATITUDE, s);
                            s = coord[1] + "";
                            if (s.length() > 7)
                                s = s.substring(0, 7);
                            intent.putExtra(Names.LONGITUDE, lon + "");
                            intent.putExtra(Names.ID, car_id);
                            context.startActivity(intent);
                            return true;
                        } catch (Exception ex) {
                            // ignore
                        }
                        return false;
                    }
                });
            }
        });
    }

    static double[] utm2geo(double x, double y, double huso, int hemisfery){

        double a = 6378137.0;
        double f = 1/298.257223563;

        double b = a*(1-f);
        double se = Math.sqrt(((Math.pow(a,2))-(Math.pow(b,2)))/((Math.pow(b,2))));
        double se2 = Math.pow(se,2);
        double c = (Math.pow(a,2))/b;

        x = x - 500000;
        if(hemisfery == -1){y = y - 10000000.0;}
        double lonmedia = (huso*6)-183.0;
        double fip = (y/(6366197.724*0.9996));
        double v = (c*0.9996)/Math.sqrt((1+se2*Math.pow(Math.cos(fip),2)));
        double aa = (x/v);
        double A1 = Math.sin(2*fip);
        double A2 = A1*Math.pow(Math.cos(fip),2);
        double J2 = fip + (A1/2);
        double J4 = (3*J2 + A2)/4;
        double J6 = (5*J4+A2*Math.pow(Math.cos(fip),2))/3.0;
        double alfa = (3.0/4)*se2;
        double beta = (5.0/3)*Math.pow(alfa,2);
        double gamma = (35.0/27)*Math.pow(alfa,3);
        double B = 0.9996*c*(fip-(alfa*J2)+(beta*J4)-(gamma*J6));
        double bb = (y-B)/v;
        double S = (se2*Math.pow(aa,2)*Math.pow(Math.cos(fip),2))/2;
        double CC = aa*(1-(S/3.0));
        double n = (bb*(1.0-S))+fip;
        double sinh = (Math.pow(Math.E,CC)-Math.pow(Math.E,-(CC)))/2.0;
        double ilam = Math.atan(sinh/Math.cos(n));
        double tau = Math.atan(Math.cos(ilam)*Math.tan(n));
        double gilam = (ilam*180)/Math.PI;
        double lon = gilam + lonmedia;
        double lat = fip + (1 + se2*(Math.pow(Math.cos(fip),2))-(3.0/2)*se2*Math.sin(fip)*Math.cos(fip)*(tau-fip))*(tau-fip);
        double glat = (lat*180)/Math.PI;
        double lonlat[] = new double[2];
        lonlat[0] = lon;
        lonlat[1] = glat;
        return lonlat;
    }
*/

    static String[] alarms = {
            "Heavy shock",
            "Trunk",
            "Hood",
            "Doors",
            "Lock",
            "MovTilt sensor",
            "Rogue",
            "Ignition Lock"
    };

    static void status(final Context context, final String car_id) {
        requestPassword(context, R.string.status_title, R.string.status_sum, new Runnable() {
            @Override
            public void run() {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.status_title, "STATUS?", "STATUS? ") {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {

                        Intent intent = new Intent(context, StatusDialog.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Names.TITLE, context.getString(R.string.status_title));
                        intent.putExtra(Names.SMS_TEXT, text);

                        String[] parts = text.split(",");
                        if (parts.length > 1) {
                            String alarm = parts[1];
                            if (!alarm.equals("Alarm NO")) {
                                if (alarm.equals("Light shock")) {
                                    intent.putExtra(Names.ALARM, context.getString(R.string.light_shock));
                                } else {
                                    int i;
                                    for (i = 0; i < alarms.length; i++) {
                                        if (alarms[i].equals(alarm)) {
                                            intent.putExtra(Names.ALARM, context.getString(R.string.alarms).split("\\|")[i]);
                                            break;
                                        }
                                    }
                                    if (i >= alarms.length)
                                        intent.putExtra(Names.ALARM, alarm);
                                }
                            }
                        }

                        String state = "";
                        if (parts[0].equals("Guard ON"))
                            state = context.getString(R.string.guard_state) + "\n";
                        for (int i = 2; i < parts.length; i++) {
                            String part = parts[i];
                            if (part.equals("GPS"))
                                state += context.getString(R.string.gps_state) + "\n";
                            if (part.equals("GPRS: None"))
                                state += context.getString(R.string.gprs_none_state) + "\n";
                            if (part.equals("GPRS: Home"))
                                state += context.getString(R.string.gprs_home_state) + "\n";
                            if (part.equals("GPRS: Roaming"))
                                state += context.getString(R.string.gprs_roaming_state) + "\n";
                            if (part.equals("Supply regular"))
                                state += context.getString(R.string.supply_regular) + "\n";
                        }
                        intent.putExtra(Names.STATE, state);
                        context.startActivity(intent);

                        return true;
                    }
                });
            }
        });
    }

    static void balance(final Context context, final String car_id) {
        requestPassword(context, R.string.balance, R.string.balance_request, new Runnable() {
            @Override
            public void run() {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.balance, "BALANCE?", "") {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        Pattern balancePattern = Pattern.compile("-?[0-9]+[.,][0-9][0-9]");
                        Matcher matcher = balancePattern.matcher(text);
                        if (!matcher.find())
                            return false;
                        String balance = matcher.group(0).replaceAll(",", ".");
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putLong(Names.BALANCE_TIME, new Date().getTime());
                        ed.putString(Names.BALANCE + car_id, balance);
                        ed.commit();
                        Intent intent = new Intent(context, StatusDialog.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Names.SMS_TEXT, text);
                        intent.putExtra(Names.STATE, balance);
                        intent.putExtra(Names.TITLE, context.getString(R.string.balance));
                        context.startActivity(intent);
                        Intent i = new Intent(FetchService.ACTION_UPDATE);
                        i.putExtra(Names.ID, car_id);
                        context.sendBroadcast(i);
                        Preferences.checkBalance(context, car_id);
                        return true;
                    }
                });
            }
        });
    }

    static void rele_set_timer(final Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Cars.Car[] cars = Cars.getCars(context);
        long min_time = 0;
        for (Cars.Car car : cars) {
            String car_id = car.id;
            if (!preferences.getBoolean(Names.RELE_IMPULSE + car_id, true))
                continue;
            long time = preferences.getLong(Names.RELE_START + car_id, 0);
            if (time == 0)
                continue;
            time += (long) preferences.getInt(Names.RELE_TIME + car_id, 20) * 60 * 1000;
            if (min_time == 0) {
                min_time = time;
                continue;
            }
            if (time < min_time)
                continue;
        }
        if (min_time == 0)
            return;
        if (piRele != null) {
            Intent i = new Intent(context, FetchService.class);
            i.setAction(FetchService.ACTION_RELE);
            piRele = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long timeout = min_time - new Date().getTime();
        alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeout, piRele);
    }

    static void rele_timeout(final Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Cars.Car[] cars = Cars.getCars(context);
        long now = new Date().getTime();
        for (Cars.Car car : cars) {
            String car_id = car.id;
            if (!preferences.getBoolean(Names.RELE_IMPULSE + car_id, true))
                continue;
            long time = preferences.getLong(Names.RELE_START + car_id, 0);
            if (time == 0)
                continue;
            time += (long) preferences.getInt(Names.RELE_TIME + car_id, 20) * 60 * 1000 - 2000;
            if (time > now)
                continue;
            rele1(context, car_id);
        }
        rele_set_timer(context);
    }

    static void rele1(final Context context, final String car_id) {
        int id = R.string.rele1_action;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        final boolean impulse = preferences.getBoolean(Names.RELE_IMPULSE + car_id, true);
        final boolean rele_on = preferences.getLong(Names.RELE_START + car_id, 0) > 0;
        final boolean rele2 = preferences.getString(Names.CAR_RELE + car_id, "").equals("2");

        String text = rele2 ? "REL2 " : "REL1 ";
        if (impulse) {
            text += "IMPULS";
        } else {
            text += (rele_on) ? "UNLOCK" : "LOCK";
        }
        if (rele2)
            id = R.string.rele2_action;
        String answer = text + " OK";
        final SmsMonitor.Sms sms = new SmsMonitor.Sms(R.string.rele, text, answer) {
            @Override
            boolean process_answer(Context context, String car_id, String text) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor ed = preferences.edit();
                ed.putLong(Names.RELE_START + car_id, new Date().getTime());
                ed.commit();
                if (!impulse) {
                    if (rele_on) {
                        ed.remove(Names.RELE_START + car_id);
                        ed.putBoolean((rele2 ? Names.RELAY2 : Names.RELAY1) + car_id, false);
                        ed.commit();
                    } else {
                        ed.putBoolean((rele2 ? Names.RELAY2 : Names.RELAY1) + car_id, true);
                        ed.commit();
                    }
                    rele_set_timer(context);
                }
                return true;
            }
        };
        requestPassword(context, R.string.rele, id, new Runnable() {
            @Override
            public void run() {
                SmsMonitor.sendSMS(context, car_id, sms);
            }
        });
    }

    static void valet_on(final Context context, final String car_id) {
        requestCCode(context, R.string.valet_on, R.string.valet_on_msg, new Actions.Answer() {
            @Override
            void answer(String ccode) {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.valet_on, ccode + " VALET", "Valet OK", INCORRECT_MESSAGE, R.string.invalid_ccode) {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putBoolean(Names.GUARD0 + car_id, true);
                        ed.putBoolean(Names.GUARD1 + car_id, false);
                        ed.putBoolean(Names.GUARD + car_id, false);
                        ed.putLong(Names.VALET_TIME + car_id, new Date().getTime());
                        ed.remove(Names.INIT_TIME + car_id);
                        ed.commit();
                        try {
                            Intent intent = new Intent(FetchService.ACTION_UPDATE);
                            intent.putExtra(Names.ID, car_id);
                            context.sendBroadcast(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        done_valet_on(context, car_id);
                        return true;
                    }
                });
            }
        });
    }

    static void valet_off(final Context context, final String car_id) {
        requestCCode(context, R.string.valet_off, R.string.valet_off_msg, new Actions.Answer() {
            @Override
            void answer(String ccode) {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.valet_off, ccode + " INIT", "Main user OK", INCORRECT_MESSAGE, R.string.invalid_ccode) {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putBoolean(Names.GUARD0 + car_id, false);
                        ed.putBoolean(Names.GUARD1 + car_id, false);
                        ed.putLong(Names.INIT_TIME + car_id, new Date().getTime());
                        ed.remove(Names.VALET_TIME + car_id);
                        ed.commit();
                        try {
                            Intent intent = new Intent(FetchService.ACTION_UPDATE);
                            intent.putExtra(Names.ID, car_id);
                            context.sendBroadcast(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        done_valet_off(context, car_id);
                        return true;
                    }
                });
            }
        });
    }

    static void block_motor(final Context context, final String car_id) {
        requestPassword(context, R.string.block, R.string.block_msg, new Runnable() {
            @Override
            public void run() {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.block, "BLOCK MTR", "BLOCK MTR OK") {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putBoolean(Names.GUARD0 + car_id, false);
                        ed.putBoolean(Names.GUARD1 + car_id, true);
                        ed.commit();
                        try {
                            Intent intent = new Intent(FetchService.ACTION_UPDATE);
                            intent.putExtra(Names.ID, car_id);
                            context.sendBroadcast(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                });
            }
        });
    }

    static void init_phone(final Context context, final String car_id) {
        requestCCode(context, R.string.init_phone, 0, new Actions.Answer() {
            @Override
            void answer(String ccode) {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.valet_off, ccode, null, INCORRECT_MESSAGE, R.string.invalid_ccode) {
                    @Override
                    boolean process_answer(Context context, String car_id, String body) {
                        if (answer == null) {
                            text += " INIT";
                            answer = "Main user OK";
                            SmsMonitor.sendSMS(context, car_id, this);
                        }
                        return true;
                    }
                });
            }
        });
    }

    static void showMessage(Context context, int id_title, int id_message) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(id_title)
                .setMessage(id_message)
                .setPositiveButton(R.string.ok, null)
                .create();
        dialog.show();
    }

    static void requestPassword(final Context context, final int id_title, int id_message, final Runnable action) {
        requestPassword(context, id_title, (id_message == 0) ? null : context.getString(id_message), action);
    }

    static void requestPassword(final Context context, final int id_title, CharSequence message, final Runnable action) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(id_title)
                .setMessage((message == null) ? context.getString(R.string.input_password) : message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String password = preferences.getString(Names.PASSWORD, "");
        if (password.length() > 0) {
            int id = R.layout.password;
            Pattern pattern = Pattern.compile("[0-9]+");
            Matcher matcher = pattern.matcher(password);
            if (matcher.matches())
                id = R.layout.password_number;
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            builder.setView(inflater.inflate(id, null));
        } else if (message == null) {
            action.run();
            return;
        }

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();

        final EditText etPassword = (EditText) dialog.findViewById(R.id.passwd);
        if (password.length() > 0) {
            etPassword.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.toString().equals(password)) {
                        dialog.dismiss();
                        action.run();
                    }
                }
            });
        }

        dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (password.length() > 0) {
                    if (!password.equals(etPassword.getText().toString())) {
                        showMessage(context, id_title, R.string.invalid_password);
                        return;
                    }
                }
                dialog.dismiss();
                action.run();
            }
        });
    }

    static abstract class Answer {
        abstract void answer(String text);
    }

    static void requestCCode(final Context context, final int id_title, int id_message, final Answer after) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(id_title)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null)
                .setView(inflater.inflate(R.layout.ccode, null));
        if (id_message > 0)
            builder.setMessage(id_message);
        final AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
        final Button ok = dialog.getButton(Dialog.BUTTON_POSITIVE);
        ok.setEnabled(false);
        final EditText ccode = (EditText) dialog.findViewById(R.id.ccode);
        ccode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                ok.setEnabled(s.length() >= 6);
            }
        });
        int inputType = ccode.getInputType() & ~InputType.TYPE_CLASS_NUMBER;
        inputType += InputType.TYPE_CLASS_TEXT;
        ccode.setInputType(inputType);

        dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                after.answer(ccode.getText().toString());
            }
        });
    }

    static void send_sms(final Context context, final String car_id, final int id_title, final SmsMonitor.Sms sms, final Answer after) {
        final ProgressDialog smsProgress = new ProgressDialog(context);
        smsProgress.setMessage(context.getString(id_title));
        smsProgress.show();
        final BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.getStringExtra(Names.ID).equals(car_id))
                    return;
                if (SmsMonitor.isProcessed(car_id, sms.id))
                    return;
                smsProgress.dismiss();
                if ((intent.getIntExtra(Names.ANSWER, 0) == Activity.RESULT_OK) && (after != null))
                    after.answer(intent.getStringExtra(Names.SMS_TEXT));
            }
        };
        context.registerReceiver(br, new IntentFilter(SmsMonitor.SMS_ANSWER));
        smsProgress.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                context.unregisterReceiver(br);
                SmsMonitor.cancelSMS(context, car_id, sms.id);
            }
        });
        if (!SmsMonitor.sendSMS(context, car_id, sms))
            smsProgress.dismiss();
    }
}
