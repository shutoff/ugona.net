package net.ugona.plus;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.haibison.android.lockpattern.LockPatternActivity;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Actions extends LockPatternActivity {

    static final String INCORRECT_MESSAGE = "Incorrect message";
    static final String COMMAND_URL = "https://car-online.ugona.net/command?auth=$1&command=$2";
    final static String URL_SET = "https://car-online.ugona.net/set?auth=$1&v=$2";
    final static String URL_SETTINGS = "https://car-online.ugona.net/settings?auth=$1";
    static final String URL_CMD = "https://car-online.ugona.net/command?skey=$1&cmd=$2";
    static Answer passwdActions = null;
    static Pattern location;
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
    static Map<String, Set<InetRequest>> inet_requests;

    static void done_motor_on(Context context, String car_id, long when) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!preferences.getBoolean(Names.Car.GUARD + car_id, false))
            return;
        int id = preferences.getInt(Names.Notify.MOTOR_ON + car_id, 0);
        if (id != 0)
            return;
        id = preferences.getInt(Names.Notify.MOTOR_OFF + car_id, 0);
        if (id != 0)
            Alarm.removeNotification(context, car_id, id);
        String mode = preferences.getString(Names.Car.AZ_MODE + car_id, "");
        id = Alarm.createNotification(context, context.getString(R.string.motor_on_ok), R.drawable.white_motor_on, car_id, "start", when, false, null, mode);
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(Names.Notify.MOTOR_ON + car_id, id);
        ed.remove(Names.Notify.MOTOR_OFF + car_id);
        ed.commit();
    }

    static void done_motor_off(Context context, String car_id, long when) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!preferences.getBoolean(Names.Car.GUARD + car_id, false))
            return;
        int id = preferences.getInt(Names.Notify.MOTOR_OFF + car_id, 0);
        if (id != 0)
            return;
        id = preferences.getInt(Names.Notify.MOTOR_ON + car_id, 0);
        if (id != 0)
            Alarm.removeNotification(context, car_id, id);
        String msg = context.getString(R.string.motor_off_ok);
        long az_stop = preferences.getLong(Names.Car.AZ_STOP + car_id, 0);
        long az_start = preferences.getLong(Names.Car.AZ_START + car_id, 0);
        long time = (az_stop - az_start) / 60000;
        if ((time > 0) && (time <= 20))
            msg += " " + context.getString(R.string.motor_time).replace("$1", time + "");
        String mode = preferences.getString(Names.Car.AZ_MODE + car_id, "");
        id = Alarm.createNotification(context, msg, R.drawable.white_motor_off, car_id, null, when, false, null, mode);
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(Names.Notify.MOTOR_OFF + car_id, id);
        ed.remove(Names.Notify.MOTOR_ON + car_id);
        ed.commit();
    }

    static void done_valet_on(Context context, String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        int id = preferences.getInt(Names.Car.VALET_ON_NOTIFY + car_id, 0);
        if (id != 0)
            return;
        id = preferences.getInt(Names.Notify.VALET_OFF + car_id, 0);
        if (id != 0)
            Alarm.removeNotification(context, car_id, id);
        id = Alarm.createNotification(context, context.getString(R.string.valet_on_ok), R.drawable.white_valet, car_id, "valet_on", 0, true, null, null);
        ed.putInt(Names.Car.VALET_ON_NOTIFY + car_id, id);
        ed.remove(Names.Notify.VALET_OFF + car_id);
        ed.commit();
    }

    static void done_valet_off(Context context, String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        int id = preferences.getInt(Names.Notify.VALET_OFF + car_id, 0);
        if (id != 0)
            return;
        id = preferences.getInt(Names.Car.VALET_ON_NOTIFY + car_id, 0);
        if (id != 0)
            Alarm.removeNotification(context, car_id, id);
        id = Alarm.createNotification(context, context.getString(R.string.valet_off_ok), R.drawable.white_valet, car_id, "valet_off", 0);
        ed.putInt(Names.Notify.VALET_OFF + car_id, id);
        ed.remove(Names.Car.VALET_ON_NOTIFY + car_id);
        ed.commit();
    }

    static void motor_on(final Context context, final String car_id, final boolean longTap) {
        selectRoute(context, car_id,
                new Runnable() {
                    @Override
                    public void run() {
                        requestCCode(context, car_id, R.string.motor_on, R.string.motor_on_ccode, new Answer() {
                            @Override
                            void answer(String ccode) {
                                new InetRequest(context, car_id, ccode, 768, R.string.motor_on) {

                                    @Override
                                    void error() {
                                        motor_on_sms(context, car_id, true);
                                    }

                                    @Override
                                    void ok(Context context, long when) {
                                        done_motor_on(context, car_id, when);
                                    }
                                };
                            }
                        });
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        motor_on_sms(context, car_id, true);

                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        motor_on_sms(context, car_id, false);
                    }
                }, longTap
        );
    }

    static void motor_on_sms(final Context context, final String car_id, boolean silent) {
        requestPassword(context, car_id, R.string.motor_on, silent ? 0 : R.string.motor_on_sum, new Answer() {
            @Override
            void answer(String pswd) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor ed = preferences.edit();
                ed.putBoolean(Names.Car.ENGINE + car_id, false);
                ed.commit();
                SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.motor_on, "MOTOR ON", "", "MOTOR ON FAIL", R.string.motor_start_error) {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        if ((text == null) ||
                                SmsMonitor.compare(text, "MOTOR ON OK") ||
                                SmsMonitor.compare(text, "Remote Engine Start OK")) {
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                            SharedPreferences.Editor ed = preferences.edit();
                            ed.putBoolean(Names.Car.AZ + car_id, true);
                            long now = new Date().getTime();
                            if (now > preferences.getLong(Names.Car.AZ_START + car_id, 0) + 60000)
                                ed.putLong(Names.Car.AZ_START + car_id, now);
                            ed.commit();
                            try {
                                Intent intent = new Intent(FetchService.ACTION_UPDATE);
                                intent.putExtra(Names.ID, car_id);
                                context.sendBroadcast(intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            done_motor_on(context, car_id, 0);
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

    static void motor_off(final Context context, final String car_id, final boolean longTap) {
        selectRoute(context, car_id,
                new Runnable() {
                    @Override
                    public void run() {
                        requestCCode(context, car_id, R.string.motor_off, R.string.motor_off_ccode, new Answer() {
                            @Override
                            void answer(String ccode) {
                                new InetRequest(context, car_id, ccode, 769, R.string.motor_off) {

                                    @Override
                                    void error() {
                                        motor_off_sms(context, car_id, true);
                                    }

                                    @Override
                                    void ok(Context context, long when) {
                                        done_motor_off(context, car_id, when);
                                    }
                                };
                            }
                        });
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        motor_off_sms(context, car_id, true);

                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        motor_off_sms(context, car_id, false);
                    }
                }, longTap
        );
    }

    static void motor_off_sms(final Context context, final String car_id, boolean silent) {
        requestPassword(context, car_id, R.string.motor_off, silent ? 0 : R.string.motor_off_sum, new Answer() {
            @Override
            void answer(String pswd) {
                SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.motor_off, "MOTOR OFF", "MOTOR OFF OK") {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putBoolean(Names.Car.AZ + car_id, false);
                        long now = new Date().getTime();
                        if (now > preferences.getLong(Names.Car.AZ_STOP + car_id, 0) + 60000)
                            ed.putLong(Names.Car.AZ_STOP + car_id, now);
                        ed.commit();
                        try {
                            Intent intent = new Intent(FetchService.ACTION_UPDATE);
                            intent.putExtra(Names.ID, car_id);
                            context.sendBroadcast(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        done_motor_off(context, car_id, 0);
                        return true;
                    }
                });
            }
        });
    }

    static void turbo_on(final Context context, final String car_id) {
        requestPassword(context, car_id, R.string.turbo_on, R.string.turbo_on_sum, new Answer() {
            @Override
            void answer(String pswd) {
                SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.turbo_on, "TURBO ON", "TURBO ON OK"));
            }
        });
    }

    static void turbo_off(final Context context, final String car_id) {
        requestPassword(context, car_id, R.string.turbo_off, R.string.turbo_off_sum, new Answer() {
            @Override
            void answer(String pswd) {
                SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.turbo_off, "TURBO OFF", "TURBO OFF OK"));
            }
        });
    }

    static void internet_on(final Context context, final String car_id) {
        requestPassword(context, car_id, R.string.internet_on, R.string.internet_on_sum, new Answer() {
            @Override
            void answer(String pswd) {
                SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.internet_on, "INTERNET ALL", "INTERNET ALL OK"));
            }
        });
    }

    static void internet_off(final Context context, final String car_id) {
        requestPassword(context, car_id, R.string.internet_off, R.string.internet_off_sum, new Answer() {
            @Override
            void answer(String pswd) {
                SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.internet_off, "INTERNET OFF", "INTERNET OFF OK"));
            }
        });
    }

    static void reset(final Context context, final String car_id) {
        requestPassword(context, car_id, R.string.reset, R.string.reset_sum, new Answer() {
            @Override
            void answer(String pswd) {
                SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.reset, "RESET", null));
            }
        });
    }

    static void map_query(final Context context, final String car_id) {
        requestPassword(context, car_id, R.string.map_req, R.string.map_req, new Answer() {
            @Override
            void answer(String pswd) {
                SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.map_req, "MAP", "") {
                    @Override
                    boolean process_answer(final Context context, final String car_id, String text) {
                        if (location == null)
                            location = Pattern.compile("\\&lat=(-?[0-9]+\\.[0-9]+)\\&lon=(-?[0-9]+\\.[0-9]+)");
                        Matcher matcher = location.matcher(text);
                        if (!matcher.find())
                            return false;
                        try {
                            double lat = toWGS(Double.parseDouble(matcher.group(1)));
                            double lng = toWGS(Double.parseDouble(matcher.group(2)));

                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                            SharedPreferences.Editor ed = preferences.edit();
                            ed.putFloat(Names.Car.LAT + car_id, (float) lat);
                            ed.putFloat(Names.Car.LNG + car_id, (float) lng);
                            ed.remove(Names.Car.COURSE + car_id);
                            ed.commit();
                            Intent i = new Intent(FetchService.ACTION_UPDATE);
                            i.putExtra(Names.ID, car_id);
                            context.sendBroadcast(i);

                            Intent intent = new Intent(context, MapDialog.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra(Names.TITLE, context.getString(R.string.map_req));
                            intent.putExtra(Names.Car.LAT, lat);
                            intent.putExtra(Names.Car.LNG, lng);
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

    static double toWGS(double pos) {
        boolean sign = false;
        if (pos < 0) {
            pos = -pos;
            sign = true;
        }
        double res = Math.floor(pos / 100);
        res += (pos - res * 100) / 60;
        if (sign)
            res = -res;
        return res;
    }

    static void status(final Context context, final String car_id) {
        requestPassword(context, car_id, R.string.status_title, R.string.status_sum, new Answer() {
            @Override
            void answer(String pswd) {
                SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.status_title, "STATUS?", "STATUS? ") {
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
                                    intent.putExtra(Names.Car.ALARM, context.getString(R.string.light_shock));
                                } else {
                                    int i;
                                    for (i = 0; i < alarms.length; i++) {
                                        if (alarms[i].equals(alarm)) {
                                            intent.putExtra(Names.Car.ALARM, context.getString(R.string.alarms).split("\\|")[i]);
                                            break;
                                        }
                                    }
                                    if (i >= alarms.length)
                                        intent.putExtra(Names.Car.ALARM, alarm);
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
        requestPassword(context, car_id, R.string.balance, R.string.balance_request, new Answer() {
            @Override
            void answer(String pswd) {
                SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.balance, "BALANCE?", "") {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        String balance = State.parseBalance(text);
                        if (balance == null)
                            return false;
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putLong(Names.Car.BALANCE_TIME + car_id, new Date().getTime());
                        ed.putString(Names.Car.BALANCE + car_id, balance);
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

    static void rele(final Context context, final String car_id, final int cmd_id, final boolean longTap) {
        selectRoute(context, car_id, new Runnable() {
                    @Override
                    public void run() {
                        requestCCode(context, car_id, cmd_id, R.string.rele_ccode, new Answer() {
                            @Override
                            void answer(String ccode) {
                                int cmd = 0;
                                switch (cmd_id) {
                                    case R.string.rele1_on:
                                        cmd = 256;
                                        break;
                                    case R.string.rele1_off:
                                        cmd = 257;
                                        break;
                                    case R.string.rele1i:
                                        cmd = 258;
                                        break;
                                    case R.string.rele2_on:
                                        cmd = 512;
                                        break;
                                    case R.string.rele2_off:
                                        cmd = 513;
                                        break;
                                    case R.string.rele2i:
                                        cmd = 514;
                                        break;
                                }

                                new InetRequest(context, car_id, ccode, cmd, cmd_id) {

                                    @Override
                                    void error() {
                                        rele_sms(context, car_id, cmd_id, true);
                                    }

                                    @Override
                                    void ok(Context context, long when) {

                                    }

                                    @Override
                                    void addRequest() {
                                    }
                                };
                            }
                        });
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        rele_sms(context, car_id, cmd_id, true);
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        rele_sms(context, car_id, cmd_id, false);
                    }
                }, longTap
        );
    }

    static void rele_sms(final Context context, final String car_id, int cmd_id, boolean silent) {
        String text = "";
        switch (cmd_id) {
            case R.string.rele1_on:
                text = "REL1 LOCK";
                break;
            case R.string.rele1_off:
                text = "REL1 UNLOCK";
                break;
            case R.string.rele1i:
                text = "REL1 IMPULS";
                break;
            case R.string.rele2_on:
                text = "REL2 LOCK";
                break;
            case R.string.rele2_off:
                text = "REL2 UNLOCK";
                break;
            case R.string.rele2i:
                text = "REL2 IMPULS";
                break;
        }

        String answer = text + " OK";
        final SmsMonitor.Sms sms = new SmsMonitor.Sms(cmd_id, text, answer);
        requestPassword(context, car_id, cmd_id, silent ? 0 : cmd_id, new Answer() {
            @Override
            void answer(String pswd) {
                SmsMonitor.sendSMS(context, car_id, pswd, sms);
            }
        });
    }

    static void heater_ctrl(final Context context, final String car_id, final boolean long_tap, final int heater, final int cmd, final int cmd_id, final boolean silent) {
        final Runnable done = new Runnable() {
            @Override
            public void run() {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor ed = preferences.edit();
                ed.putInt(Names.Car.HEATER + car_id, heater);
                Date now = new Date();
                ed.putLong(Names.Car.RELE_START + car_id, now.getTime());
                ed.commit();
                Intent i = new Intent(FetchService.ACTION_UPDATE);
                i.putExtra(Names.ID, car_id);
                context.sendBroadcast(i);
            }
        };
        requestPassword(context, car_id, R.string.thermocode, silent ? 0 : cmd_id, new Answer() {
            @Override
            void answer(String t) {
                selectRoute(context, car_id, new Runnable() {
                    @Override
                    public void run() {
                        new InetRequest(context, car_id, null, cmd, cmd_id) {

                            @Override
                            void error() {
                                final SmsMonitor.Sms sms = new SmsMonitor.Sms(cmd_id, "HEAT " + heater, null);
                                SmsMonitor.sendSMS(context, car_id, null, sms);
                            }

                            @Override
                            void ok(Context context, long when) {
                                done.run();
                            }

                            @Override
                            void user(Context context) {
                                ok(context, 0);
                            }

                            @Override
                            void sent(Context context) {
                                SmsMonitor.cancelSMS(context, car_id, cmd_id);
                                ok(context, 0);
                            }
                        };
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        final SmsMonitor.Sms sms = new SmsMonitor.Sms(cmd_id, "HEAT " + heater, null) {
                            @Override
                            boolean process_answer(Context context, String car_id, String text) {
                                done.run();
                                return super.process_answer(context, car_id, text);
                            }
                        };
                        SmsMonitor.sendSMS(context, car_id, null, sms);
                    }
                }, null, long_tap);

            }
        });
    }

    static void heater_on(final Context context, final String car_id, boolean long_tap, boolean silent) {
        heater_ctrl(context, car_id, long_tap, 2, 258, R.string.heater_on, silent);
    }

    static void heater_on_air(final Context context, final String car_id, boolean long_tap, boolean silent) {
        heater_ctrl(context, car_id, long_tap, 1, 514, R.string.heater_air, silent);
    }

    static void heater_air(final Context context, final String car_id, boolean long_tap, boolean silent) {
        heater_ctrl(context, car_id, long_tap, 3, 256, R.string.air, silent);
    }

    static void heater_off(final Context context, final String car_id, boolean long_tap, boolean silent) {
        heater_ctrl(context, car_id, long_tap, 0, 257, R.string.heater_off, silent);
    }

    static void rele1(final Context context, final String car_id, final boolean longTap) {
        selectRoute(context, car_id,
                new Runnable() {
                    @Override
                    public void run() {
                        requestCCode(context, car_id, R.string.rele, R.string.rele_ccode, new Answer() {
                            @Override
                            void answer(final String ccode) {
                                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

                                final boolean impulse = preferences.getBoolean(Names.Car.RELE_IMPULSE + car_id, true);
                                final boolean rele_on = preferences.getLong(Names.Car.RELE_START + car_id, 0) > 0;
                                final boolean rele2 = preferences.getString(Names.Car.CAR_RELE + car_id, "").equals("2");

                                int cmd = rele2 ? 514 : 258;
                                if (!impulse) {
                                    cmd--;
                                    if (!rele_on)
                                        cmd--;
                                }

                                new InetRequest(context, car_id, ccode, cmd, R.string.rele) {

                                    @Override
                                    void error() {
                                        rele1_sms(context, car_id, true);
                                    }

                                    @Override
                                    void ok(Context context, long when) {
                                        SharedPreferences.Editor ed = preferences.edit();
                                        ed.putLong(Names.Car.RELE_START + car_id, new Date().getTime());
                                        ed.commit();
                                        Intent i = new Intent(FetchService.ACTION_UPDATE);
                                        i.putExtra(Names.ID, car_id);
                                        context.sendBroadcast(i);
                                        if (!impulse && !rele_on) {
                                            i = new Intent(context, FetchService.class);
                                            i.setAction(FetchService.ACTION_RELE_OFF);
                                            i.putExtra(Names.ID, car_id);
                                            i.putExtra(Names.Car.AUTH, ccode);
                                            PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
                                            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                                            int timeout = preferences.getInt(Names.Car.RELE_TIME + car_id, 0) * 60000;
                                            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeout, pi);
                                        }
                                    }

                                    @Override
                                    void user(Context context) {
                                        ok(context, 0);
                                    }

                                };
                            }
                        });
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        rele1_sms(context, car_id, true);

                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        rele1_sms(context, car_id, false);
                    }
                }, longTap
        );
    }

    static void rele_off(final Context context, final String car_id, String ccode, String pswd) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getLong(Names.Car.RELE_START + car_id, 0) == 0)
            return;
        final boolean rele2 = preferences.getString(Names.Car.CAR_RELE + car_id, "").equals("2");
        if (ccode != null) {
            int cmd = rele2 ? 513 : 257;
            new InetRequest(context, car_id, ccode, cmd, R.string.rele) {

                @Override
                void error() {
                    rele1_sms(context, car_id, true);
                }

                @Override
                void ok(Context context, long when) {
                    SharedPreferences.Editor ed = preferences.edit();
                    ed.putLong(Names.Car.RELE_START + car_id, new Date().getTime());
                    ed.commit();
                    Intent i = new Intent(FetchService.ACTION_UPDATE);
                    i.putExtra(Names.ID, car_id);
                    context.sendBroadcast(i);
                }

                @Override
                void user(Context context) {
                    ok(context, 0);
                }
            };
            return;
        }

        String text = rele2 ? "REL2 " : "REL1 ";
        text += "UNLOCK";
        String answer = text + " OK";

        final SmsMonitor.Sms sms = new SmsMonitor.Sms(R.string.rele, text, answer) {
            @Override
            boolean process_answer(Context context, String car_id, String text) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor ed = preferences.edit();
                ed.putLong(Names.Car.RELE_START + car_id, new Date().getTime());
                ed.commit();
                ed.remove(Names.Car.RELE_START + car_id);
                ed.putBoolean((rele2 ? Names.Car.RELAY2 : Names.Car.RELAY1) + car_id, false);
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
        };
        SmsMonitor.sendSMS(context, car_id, pswd, sms);

    }

    static void rele1_sms(final Context context, final String car_id, boolean silent) {
        requestPassword(context, car_id, R.string.rele, silent ? 0 : R.string.rele, new Answer() {
            @Override
            void answer(final String pswd) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

                final boolean impulse = preferences.getBoolean(Names.Car.RELE_IMPULSE + car_id, true);
                final boolean rele_on = preferences.getLong(Names.Car.RELE_START + car_id, 0) > 0;
                final boolean rele2 = preferences.getString(Names.Car.CAR_RELE + car_id, "").equals("2");

                String text = rele2 ? "REL2 " : "REL1 ";
                if (impulse) {
                    text += "IMPULS";
                } else {
                    text += (rele_on) ? "UNLOCK" : "LOCK";
                }
                String answer = text + " OK";
                final SmsMonitor.Sms sms = new SmsMonitor.Sms(R.string.rele, text, answer) {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putLong(Names.Car.RELE_START + car_id, new Date().getTime());
                        ed.commit();
                        if (!impulse) {
                            if (rele_on) {
                                ed.remove(Names.Car.RELE_START + car_id);
                                ed.putBoolean((rele2 ? Names.Car.RELAY2 : Names.Car.RELAY1) + car_id, false);
                                ed.commit();
                            } else {
                                ed.putBoolean((rele2 ? Names.Car.RELAY2 : Names.Car.RELAY1) + car_id, true);
                                ed.commit();
                            }
                        }
                        try {
                            Intent intent = new Intent(FetchService.ACTION_UPDATE);
                            intent.putExtra(Names.ID, car_id);
                            context.sendBroadcast(intent);
                            if (!impulse && !rele_on) {
                                Intent i = new Intent(context, FetchService.class);
                                i.setAction(FetchService.ACTION_RELE_OFF);
                                i.putExtra(Names.ID, car_id);
                                i.putExtra(Names.PASSWORD, pswd);
                                PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
                                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                                int timeout = preferences.getInt("V_13_" + car_id, 0) * 60000;
                                alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeout, pi);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                };
                SmsMonitor.sendSMS(context, car_id, pswd, sms);
            }
        });
    }

    static void valet_on(final Context context, final String car_id, final boolean longTap) {

        final Ccode c_code = new Ccode();
        selectRoute(context, car_id,
                new Runnable() {
                    @Override
                    public void run() {
                        requestCCode(context, car_id, R.string.valet_on, R.string.valet_on_msg, new Actions.Answer() {
                            @Override
                            void answer(final String ccode) {
                                c_code.code = ccode;
                                new InetRequest(context, car_id, ccode, 1793, R.string.valet_on) {
                                    @Override
                                    void error() {
                                        valet_on_sms(context, car_id, ccode);
                                    }

                                    @Override
                                    void ok(Context context, long when) {
                                        done_valet_on(context, car_id);
                                    }
                                };
                            }
                        });
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        if (c_code.code != null) {
                            valet_on_sms(context, car_id, c_code.code);
                            return;
                        }
                        requestCCode(context, car_id, R.string.valet_on, R.string.valet_on_msg_sms, new Actions.Answer() {
                            @Override
                            void answer(String ccode) {
                                valet_on_sms(context, car_id, ccode);
                            }
                        });
                    }
                }, null, longTap
        );
    }

    static void valet_on_sms(final Context context, final String car_id, final String ccode) {
        if (Preferences.isDevicePasswd(context, car_id)) {
            requestPassword(context, car_id, R.string.valet_on, R.string.valet_on, new Answer() {
                @Override
                void answer(String pswd) {
                    valet_on_sms(context, car_id, ccode, pswd);
                }
            });
            return;
        }
        valet_on_sms(context, car_id, ccode, null);
    }

    static void valet_on_sms(final Context context, final String car_id, String ccode, String pswd) {
        SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.valet_on, ccode + " VALET", "Valet OK", INCORRECT_MESSAGE, R.string.invalid_ccode) {
            @Override
            boolean process_answer(Context context, String car_id, String text) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor ed = preferences.edit();
                ed.putBoolean(Names.Car.GUARD0 + car_id, true);
                ed.putBoolean(Names.Car.GUARD1 + car_id, false);
                ed.putBoolean(Names.Car.GUARD + car_id, false);
                ed.putLong(Names.Car.VALET_TIME + car_id, new Date().getTime());
                ed.remove(Names.Car.INIT_TIME + car_id);
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

    static void valet_off(final Context context, final String car_id, final boolean longTap) {
        final Ccode c_code = new Ccode();
        selectRoute(context, car_id,
                new Runnable() {
                    @Override
                    public void run() {
                        requestCCode(context, car_id, R.string.valet_off, R.string.valet_off_msg, new Actions.Answer() {

                            @Override
                            void answer(final String ccode) {
                                c_code.code = ccode;
                                new InetRequest(context, car_id, ccode, 1794, R.string.valet_off) {
                                    @Override
                                    void error() {
                                        valet_off_sms(context, car_id, ccode);
                                    }

                                    @Override
                                    void ok(Context context, long when) {
                                        done_valet_on(context, car_id);
                                    }
                                };
                            }
                        });

                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        if (c_code.code != null) {
                            valet_off_sms(context, car_id, c_code.code);
                            return;
                        }
                        requestCCode(context, car_id, R.string.valet_off, R.string.valet_off_msg_sms, new Actions.Answer() {
                            @Override
                            void answer(String ccode) {
                                valet_off_sms(context, car_id, ccode);
                            }
                        });
                    }
                }, null, longTap
        );


    }

    static void valet_off_sms(final Context context, final String car_id, final String ccode) {
        if (Preferences.isDevicePasswd(context, car_id)) {
            requestPassword(context, car_id, R.string.valet_on, R.string.valet_on, new Answer() {
                @Override
                void answer(String pswd) {
                    valet_off_sms(context, car_id, ccode, pswd);
                }
            });
            return;
        }
        valet_off_sms(context, car_id, ccode, null);
    }

    static void valet_off_sms(final Context context, final String car_id, String ccode, String pswd) {
        SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.valet_off, ccode + " INIT", "Main user OK", INCORRECT_MESSAGE, R.string.invalid_ccode) {
            @Override
            boolean process_answer(Context context, String car_id, String text) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor ed = preferences.edit();
                ed.putBoolean(Names.Car.GUARD0 + car_id, false);
                ed.putBoolean(Names.Car.GUARD1 + car_id, false);
                ed.putLong(Names.Car.INIT_TIME + car_id, new Date().getTime());
                ed.remove(Names.Car.VALET_TIME + car_id);
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

    static void block_motor(final Context context, final String car_id) {
        requestPassword(context, car_id, R.string.block, R.string.block_msg, new Answer() {
            @Override
            void answer(String pswd) {
                SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.block, "BLOCK MTR", "BLOCK MTR OK") {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putBoolean(Names.Car.GUARD0 + car_id, false);
                        ed.putBoolean(Names.Car.GUARD1 + car_id, true);
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
        requestCCode(context, car_id, R.string.init_phone, 0, new Actions.Answer() {
            @Override
            void answer(String ccode) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                if (preferences.getString(Names.Car.CAR_KEY + car_id, "").equals("demo")) {
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle(R.string.error)
                            .setMessage(R.string.ccode_fail)
                            .setNegativeButton(R.string.cancel, null)
                            .create();
                    dialog.show();
                    return;
                }
                SmsMonitor.sendSMS(context, car_id, null, new SmsMonitor.Sms(R.string.valet_off, ccode, null, INCORRECT_MESSAGE, R.string.invalid_ccode) {
                    @Override
                    boolean process_answer(Context context, String car_id, String body) {
                        if (answer == null) {
                            text += " INIT";
                            answer = "Main user OK";
                            SmsMonitor.sendSMS(context, car_id, null, this);
                        }
                        return true;
                    }
                });
            }
        });
    }

    static void search(Context context, final String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (State.isPandora(preferences, car_id)) {
            send_pandora_cmd(context, car_id, 0x17, R.string.sound);
            return;
        }
        String number = preferences.getString(Names.Car.CAR_PHONE + car_id, "");
        number = "tel://" + number + ",,3";
        Intent i = new Intent(android.content.Intent.ACTION_CALL, Uri.parse(number));
        context.startActivity(i);
    }

    static void sound_on(Context context, final String car_id) {
        set_sound(context, car_id, R.string.sound_on);
    }

    static void sound_off(Context context, final String car_id) {
        set_sound(context, car_id, R.string.sound_off);
    }

    static void set_sound(final Context context, final String car_id, final int id) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        requestPassword(context, car_id, id, null, new Answer() {
            @Override
            void answer(String text) {
                if (!isNetwork(context)) {
                    showMessage(context, id, R.string.no_network);
                    return;
                }
                final ProgressDialog progressDialog = new ProgressDialog(context);
                progressDialog.setMessage(context.getString(R.string.send_command));
                progressDialog.show();

                if (preferences.getLong(Names.Car.EVENT_TIME + car_id, 0) <= preferences.getLong(Names.Car.SETTINGS_TIME + car_id, 0)) {
                    int v = preferences.getInt("V_12_" + car_id, 0);
                    int new_v = v;
                    if (id == R.string.sound_off) {
                        new_v |= 8;
                    } else {
                        new_v &= ~8;
                    }
                    if (v == new_v) {
                        progressDialog.dismiss();
                        return;
                    }
                    String value = "12." + new_v;
                    final int val = new_v;
                    HttpTask setTask = new HttpTask() {
                        @Override
                        void result(JsonObject res) throws ParseException {
                            SharedPreferences.Editor ed = preferences.edit();
                            long time = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
                            final int wait_time = preferences.getInt(Names.Car.CAR_TIMER + car_id, 10);
                            time += wait_time * 90000;
                            ed.putLong(Names.Car.SETTINGS_TIME + car_id, time);
                            ed.putInt("V_12_" + car_id, val);
                            ed.commit();
                            progressDialog.dismiss();
                            Intent i = new Intent(FetchService.ACTION_UPDATE_FORCE);
                            i.putExtra(Names.ID, car_id);
                            context.sendBroadcast(i);
                        }

                        @Override
                        void error() {
                            progressDialog.dismiss();
                            showMessage(context, id, R.string.error);
                        }
                    };
                    setTask.execute(URL_SET, preferences.getString(Names.Car.AUTH + car_id, ""), value);
                    return;
                }

                HttpTask task = new HttpTask() {
                    @Override
                    void result(JsonObject res) throws ParseException {
                        SharedPreferences.Editor ed = preferences.edit();
                        for (int i = 0; i < 20; i++) {
                            int v = -1;
                            JsonValue val = res.get("v" + i);
                            if (val != null)
                                v = val.asInt();
                            ed.putInt("V_" + i + "_" + car_id, v);
                        }
                        ed.commit();
                        int v = preferences.getInt("V_12_" + car_id, 0);
                        int new_v = v;
                        if (id == R.string.sound_off) {
                            new_v |= 8;
                        } else {
                            new_v &= ~8;
                        }
                        if (v == new_v) {
                            progressDialog.dismiss();
                            return;
                        }
                        String value = "12." + new_v;
                        final int val = new_v;
                        HttpTask setTask = new HttpTask() {
                            @Override
                            void result(JsonObject res) throws ParseException {
                                SharedPreferences.Editor ed = preferences.edit();
                                long time = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
                                final int wait_time = preferences.getInt(Names.Car.CAR_TIMER + car_id, 10);
                                time += wait_time * 90000;
                                ed.putLong(Names.Car.SETTINGS_TIME + car_id, time);
                                ed.putInt("V_12_" + car_id, val);
                                ed.commit();
                                progressDialog.dismiss();
                                Intent i = new Intent(FetchService.ACTION_UPDATE_FORCE);
                                i.putExtra(Names.ID, car_id);
                                context.sendBroadcast(i);
                            }

                            @Override
                            void error() {
                                progressDialog.dismiss();
                                showMessage(context, id, R.string.error);
                            }
                        };
                        setTask.execute(URL_SET, preferences.getString(Names.Car.AUTH + car_id, ""), value);
                    }

                    @Override
                    void error() {
                        progressDialog.dismiss();
                        showMessage(context, id, R.string.error);
                    }
                };
                task.execute(URL_SETTINGS, preferences.getString(Names.Car.AUTH + car_id, ""));
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

    static void requestPassword(final Context context, String car_id, final int id_title, int id_message, final Answer action) {
        requestPassword(context, car_id, id_title, (id_message == 0) ? null : context.getString(id_message), action);
    }

    static void requestPassword(final Context context, String car_id, final int id_title, CharSequence message, final Answer action) {
        final boolean device_password = Preferences.isDevicePasswd(context, car_id);
        String msg = (message == null) ? context.getString(R.string.input_password) : message.toString();
        if (device_password)
            msg = context.getString(R.string.input_device_pswd);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(id_title)
                .setMessage(msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String password = preferences.getString(Names.PASSWORD, "");
        final String key = preferences.getString(Names.PATTERN, "");
        if (device_password || (password.length() > 0)) {
            int id = R.layout.password;
            if (!device_password) {
                Pattern pattern = Pattern.compile("[0-9]+");
                Matcher matcher = pattern.matcher(password);
                if (matcher.matches())
                    id = R.layout.password_number;
            }
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            builder.setView(inflater.inflate(id, null));
        } else if (!key.equals("")) {
            passwdActions = action;
            Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                    context, Actions.class);
            intent.putExtra(LockPatternActivity.EXTRA_PATTERN, key.toCharArray());
            context.startActivity(intent);
            return;

        } else if (message == null) {
            action.answer(null);
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
                    if (!device_password && s.toString().equals(password)) {
                        dialog.dismiss();
                        action.answer(null);
                    }
                }
            });
        }

        dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (device_password) {
                    String pswd = etPassword.getText().toString();
                    dialog.dismiss();
                    action.answer(pswd);
                    return;
                }
                if (password.length() > 0) {
                    if (!password.equals(etPassword.getText().toString())) {
                        showMessage(context, id_title, R.string.invalid_password);
                        return;
                    }
                }
                dialog.dismiss();
                action.answer(null);
            }
        });
    }

    static void requestCCode(final Context context, final String car_id, final int id_title, int id_message, final Answer after) {
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

        final EditText ccode_num = (EditText) dialog.findViewById(R.id.ccode_num);
        final EditText ccode_text = (EditText) dialog.findViewById(R.id.ccode_text);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String version = preferences.getString(Names.Car.VERSION + car_id, "").toLowerCase();

        final CheckBox checkBox = (CheckBox) dialog.findViewById(R.id.number);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ccode_text.setText(ccode_num.getText());
                    ccode_num.setVisibility(View.GONE);
                    ccode_text.setVisibility(View.VISIBLE);
                    ccode_text.requestFocus();
                } else {
                    ccode_num.setText(ccode_text.getText());
                    ccode_text.setVisibility(View.GONE);
                    ccode_num.setVisibility(View.VISIBLE);
                    ccode_num.requestFocus();
                }
            }
        });

        if (version.contains("super")) {
            checkBox.setVisibility(View.GONE);
        } else {
            checkBox.setChecked(preferences.getBoolean(Names.Car.NUMBER + car_id, false));
        }

        TextWatcher watcher = new TextWatcher() {
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
        };
        ccode_num.addTextChangedListener(watcher);
        ccode_text.addTextChangedListener(watcher);

        dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                EditText et = checkBox.isChecked() ? ccode_text : ccode_num;
                after.answer(et.getText().toString());
                SharedPreferences.Editor ed = preferences.edit();
                ed.putBoolean(Names.Car.NUMBER + car_id, checkBox.isChecked());
                ed.commit();
            }
        });
    }

    static boolean isNetwork(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info == null)
            return false;
        return info.isConnected();
    }

    static void selectRoute(final Context context, final String car_id, final Runnable asNetwork, final Runnable asSms, final Runnable asSmsFirst, final boolean longTap) {
        if (State.hasTelephony(context)) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String ctrl = preferences.getString(Names.Car.CONTROL + car_id, "");
            boolean route_sms = ctrl.equals("");
            if (longTap)
                route_sms = !route_sms;
            if (route_sms) {
                if (asSmsFirst != null) {
                    asSmsFirst.run();
                    return;
                }
                asSms.run();
                return;
            }
        }
        if (isNetwork(context)) {
            asNetwork.run();
            return;
        }
        if (State.hasTelephony(context)) {
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.send_sms)
                    .setMessage(R.string.send_sms_message)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            asSms.run();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            dialog.show();
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.error)
                .setMessage(R.string.no_network)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectRoute(context, car_id, asNetwork, asSms, asSmsFirst, longTap);
                    }
                })
                .create();
        dialog.show();
    }

    static boolean cancelRequest(Context context, String car_id, int id) {
        if (inet_requests == null)
            return false;
        Set<InetRequest> requests = inet_requests.get(car_id);
        if (requests == null)
            return false;
        for (InetRequest request : requests) {
            if (request.msg != id)
                continue;
            requests.remove(request);
            if (requests.size() == 0)
                inet_requests.remove(car_id);
            Intent i = new Intent(SmsMonitor.SMS_ANSWER);
            i.putExtra(Names.ANSWER, Activity.RESULT_CANCELED);
            i.putExtra(Names.ID, car_id);
            context.sendBroadcast(i);
            return true;
        }
        return false;
    }

    static void send_pandora_cmd(final Context context, final String car_id, final int cmd, final int id_title) {
        send_pandora_cmd(context, car_id, cmd, id_title, 0, 0, null);
    }

    static void send_pandora_cmd(final Context context, final String car_id, final int cmd, final int id_title, final int id_msg, final int id_pict, final String sound) {
        if (!isNetwork(context)) {
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.error)
                    .setMessage(R.string.no_network)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            send_pandora_cmd(context, car_id, cmd, id_title, id_msg, id_pict, sound);
                        }
                    })
                    .create();
            dialog.show();
            return;
        }
        requestPassword(context, car_id, id_title, R.string.send_cmd, new Answer() {
            @Override
            void answer(String text) {
                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                final ProgressDialog progressDialog = new ProgressDialog(context);
                progressDialog.setMessage(context.getString(R.string.send_command));
                progressDialog.show();
                HttpTask task = new HttpTask() {
                    @Override
                    void result(JsonObject res) throws ParseException {
                        try {
                            progressDialog.dismiss();
                        } catch (Exception ex) {
                            // ignore
                        }
                        Intent i = new Intent(SmsMonitor.SMS_SEND);
                        i.putExtra(Names.ID, car_id);
                        context.sendBroadcast(i);
                        i = new Intent(context, FetchService.class);
                        i.putExtra(Names.ID, car_id);
                        context.startService(i);
                        if (id_msg != 0) {
                            int id = preferences.getInt(Names.Notify.ZONE + car_id, 0);
                            if (id != 0)
                                Alarm.removeNotification(context, car_id, id);
                            id = Alarm.createNotification(context, context.getString(id_msg), id_pict, car_id, sound, 0);
                            SharedPreferences.Editor ed = preferences.edit();
                            ed.putInt(Names.Notify.ZONE + car_id, id);
                            ed.commit();
                        }
                        if (cmd == 4) {
                            if (!preferences.getBoolean(Names.Car.AZ + car_id, false)) {
                                SharedPreferences.Editor ed = preferences.edit();
                                ed.putBoolean(Names.Car.AZ + car_id, true);
                                ed.commit();
                                Intent intent = new Intent(FetchService.ACTION_UPDATE);
                                intent.putExtra(Names.ID, car_id);
                                context.sendBroadcast(intent);
                            }
                        } else if (cmd == 8) {
                            if (preferences.getBoolean(Names.Car.AZ + car_id, false)) {
                                SharedPreferences.Editor ed = preferences.edit();
                                ed.putBoolean(Names.Car.AZ + car_id, false);
                                ed.commit();
                                Intent intent = new Intent(FetchService.ACTION_UPDATE);
                                intent.putExtra(Names.ID, car_id);
                                context.sendBroadcast(intent);
                            }
                        }
                    }

                    @Override
                    void error() {
                        try {
                            progressDialog.dismiss();
                            if (error_text.equals("Can't connect to car"))
                                error_text = context.getString(R.string.cant_connect);
                            if (error_text.equals("Can't execute command"))
                                error_text = context.getString(R.string.cmd_error);
                            AlertDialog dialog = new AlertDialog.Builder(context)
                                    .setTitle(R.string.error)
                                    .setMessage(error_text)
                                    .setNegativeButton(R.string.cancel, null)
                                    .create();
                            dialog.show();
                        } catch (Exception ex) {
                            // ignore
                        }
                    }
                };
                task.execute(URL_CMD, preferences.getString(Names.Car.CAR_KEY + car_id, ""), cmd);
            }
        });
    }

    @Override
    protected void finishWithResultOk(char[] pattern) {
        finish();
        if (passwdActions != null) {
            passwdActions.answer(null);
            passwdActions = null;
        }
    }

    static class Ccode {
        String code;
    }

    static abstract class Answer {
        abstract void answer(String text);
    }

    static abstract class InetRequest {

        AlertDialog dialog;
        String car_id;
        Context ctx;
        long time;
        int msg;

        InetRequest(Context context, final String id, String ccode, int type, int message) {

            ctx = context;
            msg = message;
            car_id = id;
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

            final int wait_time = preferences.getInt(Names.Car.CAR_TIMER + car_id, 10);

            final ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(context.getString(R.string.send_command));
            progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    ctx = null;
                }
            });
            progressDialog.show();

            HttpTask task = new HttpTask() {
                @Override
                void result(JsonObject res) throws ParseException {
                    if (ctx == null)
                        return;
                    if (res != null) {
                        if (res.get("error") != null) {
                            error();
                            return;
                        }
                    }
                    addRequest();

                    time = new Date().getTime() + (wait_time + 1) * 60000;

                    final Context context = ctx;
                    progressDialog.dismiss();
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                    dialog = new AlertDialog.Builder(context)
                            .setTitle(msg)
                            .setView(inflater.inflate(R.layout.wait, null))
                            .setNegativeButton(R.string.ok, null)
                            .create();
                    dialog.show();
                    TextView tv = (TextView) dialog.findViewById(R.id.msg);
                    String msg = context.getString(R.string.wait_msg).replace("$1", wait_time + "");
                    tv.setText(msg);
                    Button btnCall = (Button) dialog.findViewById(R.id.call);
                    btnCall.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                            Intent intent = new Intent(Intent.ACTION_CALL);
                            intent.setData(Uri.parse("tel:" + preferences.getString(Names.Car.CAR_PHONE + car_id, "")));
                            context.startActivity(intent);
                        }
                    });
                    Button btnSms = (Button) dialog.findViewById(R.id.sms);
                    btnSms.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            InetRequest.this.dismiss();
                            InetRequest.this.error();
                        }
                    });
                    Intent i = new Intent(SmsMonitor.SMS_SEND);
                    i.putExtra(Names.ID, car_id);
                    context.sendBroadcast(i);
                    i = new Intent(context, FetchService.class);
                    i.putExtra(Names.ID, car_id);
                    context.startService(i);
                    sent(context);
                }

                @Override
                void error() {
                    if (ctx == null)
                        return;
                    final Context context = ctx;
                    try {
                        progressDialog.dismiss();
                        String msg = context.getString(R.string.send_sms_on_fail);
                        if (error_text != null)
                            msg = msg.replace(".", " (" + error_text + ").");
                        dialog = new AlertDialog.Builder(context)
                                .setTitle(R.string.send_sms)
                                .setMessage(msg)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        InetRequest.this.error();
                                    }
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .create();
                        dialog.show();
                        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dlg) {
                                dialog = null;
                                user(context);
                            }
                        });
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            };
            task.execute(COMMAND_URL, preferences.getString(Names.Car.AUTH + car_id, ""), type, "ccode", ccode);
        }

        abstract void error();

        abstract void ok(Context context, long when);

        void addRequest() {
            if (inet_requests == null)
                inet_requests = new HashMap<String, Set<InetRequest>>();
            Set<InetRequest> requests = inet_requests.get(car_id);
            if (requests == null) {
                requests = new HashSet<InetRequest>();
                inet_requests.put(car_id, requests);
            }
            requests.add(InetRequest.this);
        }

        void sent(Context context) {
        }

        void user(Context context) {
        }

        void done(Context context, long when) {
            dismiss();
            ok(context, when);
        }

        void check(Context context) {
            if (time > new Date().getTime())
                return;
            dismiss();
        }

        void dismiss() {
            if (dialog != null)
                dialog.dismiss();
            if (inet_requests == null)
                return;
            Set<InetRequest> requests = inet_requests.get(car_id);
            if (requests == null)
                return;
            requests.remove(this);
            if (requests.size() == 0)
                inet_requests.remove(car_id);
        }
    }

}
