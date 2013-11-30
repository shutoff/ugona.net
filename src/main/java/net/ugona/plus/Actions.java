package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Matcher;

public class Actions {

    static final String INCORRECT_MESSAGE = "Incorrect message";

    static void done(final Context context, int id) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(1000);
        } catch (Exception ex) {
            // ignore
        }
        Toast toast = Toast.makeText(context, id, Toast.LENGTH_LONG);
        toast.show();
    }

    static void motor_on(final Context context, final String car_id) {
        requestPassword(context, R.string.motor_on, R.string.motor_on_sum, new Runnable() {
            @Override
            public void run() {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.motor_on, "MOTOR ON", "", "ERROR;Engine", R.string.motor_start_error) {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        if (SmsMonitor.compare(text, "MOTOR ON OK") ||
                                SmsMonitor.compare(text, "Remote Engine Start OK")) {
                            done(context, R.string.motor_on_ok);
                            return true;
                        }
                        return false;
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
                        done(context, R.string.motor_off_ok);
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
                        Matcher matcher = FetchService.balancePattern.matcher(text);
                        if (!matcher.find())
                            return false;
                        String balance = matcher.group(0).replaceAll(",", ".");
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putLong(Names.BALANCE_TIME + car_id, preferences.getLong(Names.EVENT_TIME + car_id, 0));
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
                        return true;
                    }
                });
            }
        });
    }

    static void rele1(final Context context, final String car_id) {
        requestPassword(context, R.string.rele1, R.string.rele1_action, new Runnable() {
            @Override
            public void run() {
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.rele1, "REL1 IMPULS", "REL1 IMPULS OK"));
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
                        ed.putBoolean(Names.GUARD1 + car_id, true);
                        ed.putBoolean(Names.GUARD + car_id, false);
                        ed.commit();
                        try {
                            Intent intent = new Intent(FetchService.ACTION_UPDATE);
                            intent.putExtra(Names.ID, car_id);
                            context.sendBroadcast(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        done(context, R.string.valet_on_ok);
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
                        ed.commit();
                        try {
                            Intent intent = new Intent(FetchService.ACTION_UPDATE);
                            intent.putExtra(Names.ID, car_id);
                            context.sendBroadcast(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        done(context, R.string.valet_off_ok);
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
                SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.valet_off, ccode + " INIT", null, INCORRECT_MESSAGE, R.string.invalid_ccode) {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(id_title)
                .setMessage((id_message == 0) ? R.string.input_password : id_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String password = preferences.getString(Names.PASSWORD, "");
        if (password.length() > 0) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            builder.setView(inflater.inflate(R.layout.password, null));
        } else if (id_message == 0) {
            action.run();
            return;
        }

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
        dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (password.length() > 0) {
                    EditText etPassword = (EditText) dialog.findViewById(R.id.passwd);
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
                ok.setEnabled(s.length() == 6);
            }
        });
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
