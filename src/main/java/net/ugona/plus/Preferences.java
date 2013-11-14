package net.ugona.plus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Date;

public class Preferences extends PreferenceActivity {

    SharedPreferences preferences;
    Preference alarmPref;
    Preference notifyPref;
    Preference testPref;
    Preference pswdPref;

    String alarmUri;
    String notifyUri;

    private static final int GET_ALARM_SOUND = 3008;
    private static final int GET_NOTIFY_SOUND = 3009;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        addPreferencesFromResource(R.xml.preferences);

        alarmPref = findPreference("alarm");
        alarmPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
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

        alarmUri = preferences.getString(Names.ALARM, "");
        setAlarmTitle();

        notifyPref = findPreference("notify");
        notifyPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
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

        notifyUri = preferences.getString(Names.NOTIFY, "");
        setNotifyTitle();

        pswdPref = findPreference("password");
        pswdPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                setPassword();
                return true;
            }
        });

        testPref = findPreference("alarm_test");
        testPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getBaseContext(), Alarm.class);
                intent.putExtra(Names.ALARM, getString(R.string.alarm_test));
                startActivity(intent);
                return true;
            }
        });

        Preference carPref = findPreference("cars");
        carPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getBaseContext(), Cars.class);
                startActivity(intent);
                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK)
            return;

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
                break;
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
                break;
            }
            default:
                break;
        }
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

    void setPassword() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.password)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .setView(inflater.inflate(R.layout.setpassword, null))
                .create();
        dialog.show();
        final String password = preferences.getString(Names.PASSWORD, "");
        final EditText etOldPswd = (EditText) dialog.findViewById(R.id.old_password);
        if (password.length() == 0) {
            TextView tvOldLabel = (TextView) dialog.findViewById(R.id.old_password_label);
            tvOldLabel.setVisibility(View.GONE);
            etOldPswd.setVisibility(View.GONE);
        }
        final EditText etPasswd1 = (EditText) dialog.findViewById(R.id.password);
        final EditText etPasswd2 = (EditText) dialog.findViewById(R.id.password1);
        final TextView tvConfrim = (TextView) dialog.findViewById(R.id.invalid_confirm);
        final Button btnSave = dialog.getButton(Dialog.BUTTON_POSITIVE);
        final Context context = this;

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (etPasswd1.getText().toString().equals(etPasswd2.getText().toString())) {
                    tvConfrim.setVisibility(View.INVISIBLE);
                    btnSave.setEnabled(true);
                } else {
                    tvConfrim.setVisibility(View.VISIBLE);
                    btnSave.setEnabled(false);
                }
            }
        };

        etPasswd1.addTextChangedListener(watcher);
        etPasswd2.addTextChangedListener(watcher);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!password.equals(etOldPswd.getText().toString())) {
                    Actions.showMessage(context, R.string.password, R.string.invalid_password);
                    return;
                }
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.PASSWORD, etPasswd1.getText().toString());
                ed.commit();
                dialog.dismiss();
            }
        });
    }

    static String getCar(SharedPreferences preferences, String car_id) {
        String[] cars = preferences.getString(Names.CARS, "").split(",");
        if (car_id != null) {
            boolean car_ok = false;
            for (String car : cars) {
                if (car.equals(car_id)) {
                    car_ok = true;
                    break;
                }

            }
            if (!car_ok)
                car_id = null;
        }
        if (car_id == null)
            car_id = cars[0];
        return car_id;
    }

    static String getTemperature(SharedPreferences preferences, String car_id) {
        try {
            String s = preferences.getString(Names.TEMPERATURE + car_id, "");
            if (s.length() == 0)
                return null;
            double v = Double.parseDouble(s);
            v += preferences.getInt(Names.TEMP_SIFT + car_id, 0);
            return v + " \u00B0C";
        } catch (Exception ex) {
        }
        return null;
    }

    static boolean getValet(SharedPreferences preferences, String car_id) {
        Date now = new Date();
        long d = now.getTime() / 1000;
        if (d - preferences.getLong(Names.VALET_TIME + car_id, 0) < 30)
            return true;
        if (d - preferences.getLong(Names.INIT_TIME + car_id, 0) < 30)
            return false;
        return preferences.getBoolean(Names.VALET + car_id, false);
    }

}
