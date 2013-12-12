package net.ugona.plus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.util.Date;

public class CarPreferences extends PreferenceActivity {

    SharedPreferences preferences;

    Preference smsPref;
    Preference apiPref;
    Preference phonePref;
    Preference versionPref;
    Preference phonesPref;
    Preference timerPref;
    Preference notifyPref;
    Preference alarmPref;
    Preference mainPref;
    Preference limitPref;
    SeekBarPreference sensPref;
    CheckBoxPreference photoPref;
    Preference relePref;

    String alarmUri;
    String notifyUri;

    EditTextPreference namePref;
    SeekBarPreference shiftPref;

    String car_id;

    final static int REQUEST_PHONE = 4000;
    private static final int GET_ALARM_SOUND = 3008;
    private static final int GET_NOTIFY_SOUND = 3009;

    final static String KEY_URL = "http://dev.car-online.ru/api/v2?get=securityKey&login=$1&password=$2&content=json";
    final static String PROFILE_URL = "http://dev.car-online.ru/api/v2?get=profile&skey=$1&content=json";
    final static String PHOTOS_URL = "http://dev.car-online.ru/api/v2?get=photos&skey=$1&begin=$2&content=json";

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
        if (preferences.getString(Names.CAR_KEY + car_id, "").equals("")) {
            Cars.deleteCarKeys(this, car_id);
            getApiKey();
        }

        setTitle(title);
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt("tmp_shift", preferences.getInt(Names.TEMP_SIFT + car_id, 0));
        ed.putBoolean("show_balance", preferences.getBoolean(Names.SHOW_BALANCE + car_id, true));
        ed.putBoolean("show_photo", preferences.getBoolean(Names.SHOW_PHOTO + car_id, false));
        ed.putInt("shock_sens", preferences.getInt(Names.SHOCK_SENS + car_id, 5));
        ed.putString("name_", name);
        ed.putString("call_mode", preferences.getString(Names.ALARM_MODE + car_id, ""));
        ed.putString("balance_limit", preferences.getInt(Names.LIMIT + car_id, 50) + "");
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

        smsPref = findPreference("call_mode");
        smsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String mode = newValue.toString();
                if (mode.equals("CALL")) {
                    callMode();
                } else {
                    smsMode();
                }
                return false;
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

        sensPref = (SeekBarPreference) findPreference("shock_sens");
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
            public boolean onPreferenceChange(Preference preference, final Object newValue) {
                if (newValue instanceof Integer) {
                    int v = (Integer) newValue;
                    Actions.requestPassword(CarPreferences.this, R.string.shock_sens, R.string.shock_sens_msg, new Runnable() {
                        @Override
                        public void run() {
                            SmsMonitor.Sms sms = new SmsMonitor.Sms(R.string.shock_sens, "SET 1," + newValue, "SET OK");
                            Actions.send_sms(CarPreferences.this, car_id, R.string.shock_sens, sms, null);
                        }
                    });
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

        relePref = findPreference("rele");
        relePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setupRele();
                return true;
            }
        });

        photoPref = (CheckBoxPreference) findPreference("show_photo");
        photoPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof Boolean) {
                    boolean v = (Boolean) newValue;
                    SharedPreferences.Editor ed = preferences.edit();
                    ed.putBoolean(Names.SHOW_PHOTO + car_id, v);
                    ed.commit();
                    sendUpdate();
                    return true;
                }
                return false;
            }
        });

        phonesPref = findPreference("phones");
        phonesPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Actions.requestPassword(CarPreferences.this, R.string.phones, R.string.phones_sum, new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(CarPreferences.this, Phones.class);
                        i.putExtra(Names.ID, car_id);
                        startActivity(i);
                    }
                });
                return true;
            }
        });

        timerPref = findPreference("timer");
        timerPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setTimer();
                return true;
            }
        });

        limitPref = findPreference("balance_limit");
        limitPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    SharedPreferences.Editor ed = preferences.edit();
                    ed.putInt(Names.LIMIT + car_id, Integer.parseInt(newValue.toString()));
                    ed.commit();
                    setBalance();
                    sendUpdate();
                } catch (Exception ex) {
                    // ignore
                }
                return true;
            }
        });
        setBalance();

        Preference testPref = findPreference("alarm_test");
        testPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getBaseContext(), Alarm.class);
                intent.putExtra(Names.ID, car_id);
                intent.putExtra(Names.ALARM, getString(R.string.alarm_test));
                startActivity(intent);
                return true;
            }
        });

        versionPref = findPreference("version");

        String version = preferences.getString(Names.VERSION + car_id, "");
        versionPref.setSummary(version);

        String api_key = preferences.getString(Names.CAR_KEY + car_id, "");
        if (!api_key.equals(""))
            getVersion(api_key);

        alarmPref = findPreference("alarm");
        alarmPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                if (alarmUri != null) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(alarmUri));
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                }

                startActivityForResult(intent, GET_ALARM_SOUND);
                return true;
            }
        });

        alarmUri = Preferences.getAlarm(preferences, car_id);
        setAlarmTitle();

        notifyPref = findPreference("notify");
        notifyPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                if (notifyUri != null) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(notifyUri));
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                }

                startActivityForResult(intent, GET_NOTIFY_SOUND);
                return true;
            }
        });

        notifyUri = Preferences.getNotify(preferences, car_id);
        setNotifyTitle();

        mainPref = findPreference("main_phone");
        mainPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Actions.init_phone(CarPreferences.this, car_id);
                return true;
            }
        });

        Preference cmdPref = findPreference("cmd");
        cmdPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                selectCommands();
                return true;
            }
        });

        if (State.hasTelephony(this)) {
            String phoneNumber = preferences.getString(Names.CAR_PHONE + car_id, "");
            setPhone(phoneNumber);
        } else {
            PreferenceScreen ps = getPreferenceScreen();
            ps.removePreference(smsPref);
            ps.removePreference(phonePref);
            ps.removePreference(sensPref);
            ps.removePreference(relePref);
            ps.removePreference(mainPref);
            ps.removePreference(phonesPref);
            ps.removePreference(timerPref);
            ps.removePreference(alarmPref);
            ps.removePreference(notifyPref);
            ps.removePreference(testPref);
        }

        setupCommands();
        setupPointer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case GET_ALARM_SOUND: {
                    Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        alarmUri = uri.toString();
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putString(Names.ALARM, alarmUri);
                        ed.commit();
                        setAlarmTitle();
                    }
                    return;
                }
                case GET_NOTIFY_SOUND: {
                    Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        notifyUri = uri.toString();
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putString(Names.NOTIFY, notifyUri);
                        ed.commit();
                        setNotifyTitle();
                    }
                    return;
                }
                case REQUEST_PHONE: {
                    String number = data.getStringExtra(Names.CAR_PHONE);
                    SharedPreferences.Editor ed = preferences.edit();
                    ed.putString(Names.CAR_PHONE + car_id, number);
                    ed.commit();
                    setPhone(number);
                    return;
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void setPhone(String phoneNumber) {
        if (phoneNumber.length() > 0) {
            phonePref.setSummary(Phones.formatPhoneNumber(phoneNumber));
            smsPref.setEnabled(true);
            phonesPref.setEnabled(true);
            timerPref.setEnabled(true);
            sensPref.setEnabled(true);
            mainPref.setEnabled(true);
        } else {
            phonePref.setSummary(getString(R.string.phone_number_summary));
            smsPref.setEnabled(false);
            phonesPref.setEnabled(false);
            timerPref.setEnabled(false);
            sensPref.setEnabled(false);
            mainPref.setEnabled(false);
        }
    }

    void setBalance() {
        try {
            int v = preferences.getInt(Names.LIMIT + car_id, 50);
            if (v < 0) {
                limitPref.setSummary(getResources().getStringArray(R.array.balance_limit)[0]);
            } else {
                limitPref.setSummary(v + "");
            }
        } catch (Exception ex) {
            // ignore
        }
    }

    void smsMode() {
        Runnable send = new Runnable() {
            @Override
            public void run() {
                SmsMonitor.Sms sms = new SmsMonitor.Sms(R.string.sms_mode, "ALARM SMS", "ALARM SMS OK") {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putString(Names.ALARM_MODE + car_id, "SMS");
                        ed.commit();
                        return true;
                    }
                };
                Actions.send_sms(CarPreferences.this, car_id, R.string.sms_mode, sms, null);
            }
        };
        if (preferences.getString(Names.PASSWORD, "").equals("")) {
            send.run();
            return;
        }
        Actions.requestPassword(this, R.string.sms_mode, R.string.sms_mode_msg, send);
    }

    void callMode() {
        Runnable send = new Runnable() {
            @Override
            public void run() {
                SmsMonitor.Sms sms = new SmsMonitor.Sms(R.string.call_mode, "ALARM CALL", "ALARM CALL OK") {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putString(Names.ALARM_MODE + car_id, "CALL");
                        ed.commit();
                        return true;
                    }
                };
                Actions.send_sms(CarPreferences.this, car_id, R.string.call_mode, sms, null);
            }
        };
        if (preferences.getString(Names.PASSWORD, "").equals("")) {
            send.run();
            return;
        }
        Actions.requestPassword(this, R.string.call_mode, R.string.call_mode_msg, send);
    }

    void setupCommands() {
        int flags = State.getCommands(preferences, car_id);
        relePref.setEnabled((flags & State.CMD_RELE) != 0);
    }

    final static String[] rele_connection = {
            "RELE1",
            "RELE2"
    };

    void setupRele() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.rele)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null)
                .setView(inflater.inflate(R.layout.prestarter, null))
                .create();
        dialog.show();
        final Spinner rele = (Spinner) dialog.findViewById(R.id.rele);
        rele.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return rele_connection.length;
            }

            @Override
            public Object getItem(int position) {
                return rele_connection[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.car_list_item, null);
                }
                TextView tv = (TextView) v.findViewById(R.id.name);
                tv.setText(rele_connection[position]);
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.car_list_dropdown_item, null);
                }
                TextView tv = (TextView) v.findViewById(R.id.name);
                tv.setText(rele_connection[position]);
                return v;
            }
        });
        rele.setSelection(preferences.getString(Names.CAR_RELE + car_id, "").equals("2") ? 1 : 0);
        final TextView tvTime = (TextView) dialog.findViewById(R.id.time);
        final SeekBar seek = (SeekBar) dialog.findViewById(R.id.seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvTime.setText((progress + 10) + " " + getString(R.string.minutes));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seek.setProgress(preferences.getInt(Names.RELE_TIME, 30) - 10);
        dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.CAR_RELE + car_id, (rele.getSelectedItemPosition() == 1) ? "1" : "2");
                ed.putInt(Names.RELE_TIME + car_id, seek.getProgress() + 10);
                ed.commit();
                dialog.dismiss();
            }
        });
    }

    final static int[] Command = {
            R.string.call,
            R.string.valet_cmd,
            R.string.block,
            R.string.autostart,
            R.string.rele
    };

    int commands;

    void selectCommands() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.commands)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putInt(Names.COMMANDS + car_id, commands);
                        ed.commit();
                        setupCommands();
                        sendUpdate();
                    }
                })
                .setView(inflater.inflate(R.layout.checklist, null))
                .create();
        dialog.show();
        commands = State.getCommands(preferences, car_id);
        ListView lv = (ListView) dialog.findViewById(R.id.list);
        lv.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return Command.length;
            }

            @Override
            public Object getItem(int position) {
                return Command[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.checklist_item, null);
                }
                int mask = 1 << position;
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.item);
                checkBox.setText(Command[position]);
                checkBox.setTag(mask);
                checkBox.setChecked((commands & mask) != 0);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        int mask = (Integer) buttonView.getTag();
                        commands &= ~mask;
                        if (isChecked)
                            commands |= mask;
                    }
                });
                return v;
            }
        });
    }

    void getApiKey() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.auth)
                .setMessage(R.string.auth_summary)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null)
                .setView(inflater.inflate(R.layout.apikeydialog, null))
                .create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();

        final EditText edLogin = (EditText) dialog.findViewById(R.id.login);
        final EditText edPasswd = (EditText) dialog.findViewById(R.id.passwd);
        final TextView tvError = (TextView) dialog.findViewById(R.id.error);

        final Button btnSave = dialog.getButton(Dialog.BUTTON_POSITIVE);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                btnSave.setEnabled(!edLogin.getText().toString().equals("") && !edPasswd.getText().toString().equals(""));
                tvError.setVisibility(View.GONE);
            }
        };
        edLogin.addTextChangedListener(watcher);
        edPasswd.addTextChangedListener(watcher);
        btnSave.setEnabled(false);
        edLogin.setText(preferences.getString(Names.LOGIN + car_id, ""));

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog dlgCheck = new ProgressDialog(CarPreferences.this);
                dlgCheck.setMessage(getString(R.string.check_auth));
                dlgCheck.show();

                final String login = edLogin.getText().toString();

                HttpTask apiTask = new HttpTask() {
                    @Override
                    void result(JsonObject res) throws ParseException {
                        dlgCheck.dismiss();
                        String key = res.get("data").asString();
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putString(Names.CAR_KEY + car_id, key);
                        ed.putString(Names.LOGIN + car_id, login);
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
                        Intent intent = new Intent(CarPreferences.this, FetchService.class);
                        intent.putExtra(Names.ID, car_id);
                        startService(intent);
                        getVersion(key);
                        getPhotos(key);
                    }

                    @Override
                    void error() {
                        Toast toast = Toast.makeText(CarPreferences.this, getString(R.string.auth_error), Toast.LENGTH_LONG);
                        toast.show();
                        tvError.setText(R.string.auth_error);
                        tvError.setVisibility(View.VISIBLE);
                        dlgCheck.dismiss();
                    }
                };

                apiTask.execute(KEY_URL, login, edPasswd.getText().toString());
            }
        });
    }

    void getPhotos(String api_key) {
        HttpTask verTask = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                try {
                    JsonArray array = res.get("photos").asArray();
                    boolean is_photo = array.size() > 0;
                    photoPref.setChecked(is_photo);
                    SharedPreferences.Editor ed = preferences.edit();
                    ed.putBoolean(Names.SHOW_PHOTO + car_id, is_photo);
                    ed.commit();
                    sendUpdate();
                } catch (Exception ex) {
                    // ignore
                }
            }

            @Override
            void error() {

            }
        };
        Date now = new Date();
        verTask.execute(PHOTOS_URL, api_key, (now.getTime() - 3 * 24 * 60 * 60 * 1000) + "");
    }

    void setupPointer() {
        if (!preferences.getBoolean(Names.POINTER + car_id, false))
            return;
        removePreference("call_mode");
        removePreference("main_phone");
        removePreference("phones");
        removePreference("shock_sens");
        removePreference("timer");
        removePreference("autostart");
        removePreference("rele");
        removePreference("show_photo");
        removePreference("alarm");
        removePreference("alarm_test");
        removePreference("notify");
        removePreference("tmp_shift");
        removePreference("cmd");
    }

    void removePreference(String key) {
        Preference pref = getPreferenceScreen().findPreference(key);
        if (pref == null)
            return;
        getPreferenceScreen().removePreference(pref);
    }

    void getVersion(String api_key) {
        HttpTask verTask = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                try {
                    JsonArray devices = res.get("devices").asArray();
                    JsonObject device = devices.get(0).asObject();
                    String ver = device.get("versionSoftPGSM").asString();
                    versionPref.setSummary(ver);
                    SharedPreferences.Editor ed = preferences.edit();
                    if (ver.toUpperCase().substring(0, 5).equals("MS-TR")) {
                        ed.putBoolean(Names.POINTER + car_id, true);
                        setupPointer();
                    } else {
                        ed.remove(Names.POINTER + car_id);
                    }
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

    void setAlarmTitle() {
        Uri uri = Uri.parse(alarmUri);
        Ringtone ringtone = RingtoneManager.getRingtone(getBaseContext(), uri);
        if (ringtone == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(getBaseContext(), uri);
        }
        if (ringtone != null) {
            String name = ringtone.getTitle(getBaseContext());
            alarmPref.setSummary(name);
        }
    }

    void setNotifyTitle() {
        Uri uri = Uri.parse(notifyUri);
        Ringtone ringtone = RingtoneManager.getRingtone(getBaseContext(), uri);
        if (ringtone == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ringtone = RingtoneManager.getRingtone(getBaseContext(), uri);
        }
        if (ringtone != null) {
            String name = ringtone.getTitle(getBaseContext());
            notifyPref.setSummary(name);
        }
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
        int timeout = preferences.getInt(Names.CAR_TIMER + car_id, 10);
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
        seekBar.setProgress((timeout > 0) ? timeout - 1 : 9);
        final CheckBox checkBox = (CheckBox) dialog.findViewById(R.id.timer_on);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                seekBar.setEnabled(isChecked);
                tvLabel.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        checkBox.setChecked(timeout > 0);
        Button btnOk = dialog.getButton(Dialog.BUTTON_POSITIVE);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int timeout = (checkBox.isChecked()) ? seekBar.getProgress() + 1 : 0;
                dialog.dismiss();
                final String text = String.format("TIMER %04d", timeout);
                Actions.requestPassword(CarPreferences.this, R.string.timer, R.string.timer_sum,
                        new Runnable() {
                            @Override
                            public void run() {
                                SmsMonitor.Sms sms = new SmsMonitor.Sms(R.string.timer, text, "TIMER OK") {
                                    @Override
                                    boolean process_answer(Context context, String car_id, String text) {
                                        SharedPreferences.Editor ed = preferences.edit();
                                        ed.putInt(Names.CAR_TIMER + car_id, timeout);
                                        ed.commit();
                                        return true;
                                    }
                                };
                                Actions.send_sms(CarPreferences.this, car_id, R.string.timer, sms, null);
                            }
                        });
            }
        });
    }
}
