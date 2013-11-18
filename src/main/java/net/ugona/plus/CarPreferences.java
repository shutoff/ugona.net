package net.ugona.plus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CarPreferences extends PreferenceActivity {

    SharedPreferences preferences;

    Preference smsPref;
    Preference apiPref;
    Preference phonePref;
    Preference versionPref;

    EditTextPreference namePref;
    SeekBarPreference shiftPref;

    String car_id;

    final static int REQUEST_PHONE = 4000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        car_id = getIntent().getStringExtra(Names.ID);
        if (car_id == null)
            car_id = "";

        String title = preferences.getString(Names.CAR_NAME + car_id, "");
        String name = title;
        if (title.equals("")) {
            title = getString(R.string.new_car);
            name = getString(R.string.car);
            if (!car_id.equals(""))
                name += " " + car_id;
        }
        setTitle(title);
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt("tmp_shift", preferences.getInt(Names.TEMP_SIFT + car_id, 0));
        ed.putBoolean("show_balance", preferences.getBoolean(Names.SHOW_BALANCE + car_id, true));
        ed.putBoolean("autostart", preferences.getBoolean(Names.CAR_AUTOSTART + car_id, false));
        ed.putBoolean("rele1", preferences.getBoolean(Names.CAR_RELE1 + car_id, false));
        ed.putInt("shock_sens", preferences.getInt(Names.SHOCK_SENS + car_id, 5));
        ed.putString("name_", name);
        ed.commit();

        addPreferencesFromResource(R.xml.car_preferences);

        namePref = (EditTextPreference) findPreference("name_");
        namePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String value = newValue.toString();
                setTitle(value);
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.CAR_NAME + car_id, value);
                ed.commit();
                namePref.setSummary(value);
                return true;
            }
        });
        namePref.setSummary(name);

        smsPref = findPreference("sms_mode");
        smsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                smsMode();
                return true;
            }
        });

        phonePref = findPreference("phone");
        phonePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(CarPreferences.this, PhoneNumberDialog.class);
                i.putExtra(Names.CAR_PHONE, preferences.getString(Names.CAR_PHONE + car_id, ""));
                startActivityForResult(i, REQUEST_PHONE);
                return true;
            }
        });

        String phoneNumber = preferences.getString(Names.CAR_PHONE + car_id, "");
        setPhone(phoneNumber);

        apiPref = findPreference("api_key");
        apiPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                getApiKey();
                return true;
            }
        });

        shiftPref = (SeekBarPreference) findPreference("tmp_shift");
        shiftPref.setMin(-10);
        shiftPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof Integer) {
                    int v = (Integer) newValue;
                    SharedPreferences.Editor ed = preferences.edit();
                    ed.putInt(Names.TEMP_SIFT + car_id, v);
                    ed.commit();
                    Intent intent = new Intent(FetchService.ACTION_UPDATE);
                    intent.putExtra(Names.ID, car_id);
                    try {
                        getBaseContext().sendBroadcast(intent);
                    } catch (Exception ex) {
                        // ignore
                    }
                    return true;
                }
                return false;
            }
        });

        SeekBarPreference sensPref = (SeekBarPreference) findPreference("shock_sens");
        sensPref.mMin = 1;
        sensPref.summaryGenerator = new SeekBarPreference.SummaryGenerator() {
            @Override
            String summary(int value) {
                Resources res = getResources();
                String[] levels = res.getStringArray(R.array.levels);
                return levels[value - 1];
            }
        };
        sensPref.setSummary(sensPref.summary());
        sensPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof Integer) {
                    int v = (Integer) newValue;
                    Actions.requestPassword(CarPreferences.this, car_id, R.string.shock_sens, 0, "SET 1," + newValue, "SET OK");
                    SharedPreferences.Editor ed = preferences.edit();
                    ed.putInt(Names.SHOCK_SENS + car_id, v);
                    ed.commit();
                    return true;
                }
                return false;
            }
        });

        CheckBoxPreference balancePref = (CheckBoxPreference) findPreference("show_balance");
        balancePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof Boolean) {
                    boolean v = (Boolean) newValue;
                    SharedPreferences.Editor ed = preferences.edit();
                    ed.putBoolean(Names.SHOW_BALANCE + car_id, v);
                    ed.commit();
                    sendUpdate();
                    return true;
                }
                return false;
            }
        });

        CheckBoxPreference autoPref = (CheckBoxPreference) findPreference("autostart");
        autoPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof Boolean) {
                    boolean v = (Boolean) newValue;
                    SharedPreferences.Editor ed = preferences.edit();
                    ed.putBoolean(Names.CAR_AUTOSTART + car_id, v);
                    ed.commit();
                    sendUpdate();
                    return true;
                }
                return false;
            }
        });

        CheckBoxPreference relePref = (CheckBoxPreference) findPreference("rele1");
        relePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof Boolean) {
                    boolean v = (Boolean) newValue;
                    SharedPreferences.Editor ed = preferences.edit();
                    ed.putBoolean(Names.CAR_RELE1 + car_id, v);
                    ed.commit();
                    sendUpdate();
                    return true;
                }
                return false;
            }
        });

        Preference phonesPref = findPreference("phones");
        phonesPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Actions.users(CarPreferences.this, car_id);
                return true;
            }
        });

        Preference timerPref = findPreference("timer");
        timerPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setTimer();
                return true;
            }
        });

        versionPref = findPreference("version");

        String version = preferences.getString(Names.VERSION + car_id, "");
        versionPref.setSummary(version);

        String api_key = preferences.getString(Names.CAR_KEY + car_id, "");
        if (!api_key.equals("")) {
            HttpTask verTask = new HttpTask() {
                @Override
                void result(JSONObject res) throws JSONException {
                    try {
                        JSONArray devices = res.getJSONArray("devices");
                        JSONObject device = devices.getJSONObject(0);
                        String ver = device.getString("versionSoftPGSM");
                        versionPref.setSummary(ver);
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putString(Names.VERSION + car_id, ver);
                        ed.commit();
                    } catch (Exception ex) {
                        // ignore
                    }
                }

                @Override
                void error() {

                }
            };
            verTask.execute(PROFILE_URL, api_key);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_PHONE) && (resultCode == RESULT_OK)) {
            String number = data.getStringExtra(Names.CAR_PHONE);
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(Names.CAR_PHONE + car_id, number);
            ed.commit();
            if (number.equals(""))
                number = getString(R.string.phone_number_summary);
            phonePref.setSummary(number);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void setPhone(String phoneNumber) {
        if (phoneNumber.length() > 0) {
            phonePref.setSummary(phoneNumber);
            smsPref.setEnabled(true);
        } else {
            smsPref.setEnabled(false);
        }
    }

    void smsMode() {
        Actions.requestPassword(this, car_id, R.string.sms_mode, R.string.sms_mode_msg, "ALARM SMS", "ALARM SMS OK");
    }

    final String PROFILE_URL = "http://api.car-online.ru/v2?get=profile&skey=$1&content=json";

    void getApiKey() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.api_key)
                .setMessage(R.string.api_key_summary)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null)
                .setView(inflater.inflate(R.layout.apikeydialog, null))
                .create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
        final Button btnSave = (Button) dialog.getButton(Dialog.BUTTON_POSITIVE);
        final EditText etKey = (EditText) dialog.findViewById(R.id.api_key);
        final TextView tvMessage = (TextView) dialog.findViewById(R.id.message);
        TextWatcher watcher = new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                if (etKey.getText().toString().matches("[0-9A-Fa-f]{30}")) {
                    btnSave.setEnabled(true);
                    tvMessage.setText("");
                } else {
                    btnSave.setEnabled(false);
                    if (etKey.getText().length() == 0) {
                        tvMessage.setText("");
                    } else {
                        tvMessage.setText(getString(R.string.bad_key));
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

        };

        final BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null)
                    return;
                Object[] pduArray = (Object[]) intent.getExtras().get("pdus");
                SmsMessage[] messages = new SmsMessage[pduArray.length];
                for (int i = 0; i < pduArray.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pduArray[i]);
                }
                StringBuilder bodyText = new StringBuilder();
                for (SmsMessage m : messages) {
                    bodyText.append(m.getMessageBody());
                }
                String body = bodyText.toString();
                if (body.matches("[0-9A-Fa-f]{30}")) {
                    etKey.setText(body);
                    abortBroadcast();
                }
            }
        };
        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        filter.setPriority(5000);
        registerReceiver(br, filter);

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                unregisterReceiver(br);
            }
        });


        etKey.addTextChangedListener(watcher);
        etKey.setText(preferences.getString(Names.CAR_KEY + car_id, ""));
        watcher.afterTextChanged(etKey.getText());

        final Context context = this;

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog dlgCheck = new ProgressDialog(context);
                dlgCheck.setMessage(getString(R.string.check_api));
                dlgCheck.show();

                HttpTask checkCode = new HttpTask() {
                    @Override
                    void result(JSONObject res) throws JSONException {
                        dlgCheck.dismiss();
                        if (res != null) {
                            res.getInt("id");
                            try {
                                JSONArray devices = res.getJSONArray("devices");
                                JSONObject device = devices.getJSONObject(0);
                                String ver = device.getString("versionSoftPGSM");
                                versionPref.setSummary(ver);
                                SharedPreferences.Editor ed = preferences.edit();
                                ed.putString(Names.VERSION + car_id, ver);
                                ed.commit();
                            } catch (Exception ex) {
                                // ignore
                            }
                            SharedPreferences.Editor ed = preferences.edit();
                            ed.putString(Names.CAR_KEY + car_id, etKey.getText().toString());
                            String[] cars = preferences.getString(Names.CARS, "").split(",");
                            boolean is_new = true;
                            for (String car : cars) {
                                if (car.equals(car_id))
                                    is_new = false;
                            }
                            if (is_new)
                                ed.putString(Names.CARS, preferences.getString(Names.CARS, "") + "," + car_id);
                            ed.commit();
                            dialog.dismiss();
                            Intent intent = new Intent(context, FetchService.class);
                            intent.putExtra(Names.ID, car_id);
                            startService(intent);
                            return;
                        }
                        error();
                    }

                    @Override
                    void error() {
                        String message = getString(R.string.key_error);
                        if (error_text != null) {
                            if (error_text.equals("Security Service Error")) {
                                message = getString(R.string.invalid_key);
                                versionPref.setSummary(R.string.invalid_key);
                            } else {
                                message += " " + error_text;
                            }
                        }
                        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
                        toast.show();
                        tvMessage.setText(message);
                        dlgCheck.dismiss();
                    }
                };

                checkCode.execute(PROFILE_URL, etKey.getText().toString());
            }
        });
    }

    void sendUpdate() {
        try {
            Intent intent = new Intent(FetchService.ACTION_UPDATE_FORCE);
            intent.putExtra(Names.ID, car_id);
            sendBroadcast(intent);
        } catch (Exception e) {
            // ignore
        }
    }

    void setTimer() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.timer)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(inflater.inflate(R.layout.timer_setup, null))
                .create();
        dialog.show();
        final TextView tvLabel = (TextView) dialog.findViewById(R.id.period);
        final SeekBar seekBar = (SeekBar) dialog.findViewById(R.id.timer);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvLabel.setText((progress + 1) + " " + getString(R.string.minutes));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBar.setProgress(9);
        final CheckBox checkBox = (CheckBox) dialog.findViewById(R.id.timer_on);
        checkBox.setChecked(true);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                seekBar.setEnabled(isChecked);
                tvLabel.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        Button btnOk = dialog.getButton(Dialog.BUTTON_POSITIVE);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int timeout = 0;
                if (checkBox.isChecked())
                    timeout = seekBar.getProgress() + 1;
                dialog.dismiss();
                String text = String.format("TIMER %04d", timeout);
                Actions.send_sms(CarPreferences.this, car_id, text, "TIMER OK", R.string.timer, null);
            }
        });
    }
}
