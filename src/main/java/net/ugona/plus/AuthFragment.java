package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

public class AuthFragment extends SettingsFragment {

    static final int REQUEST_AUTH = 1;
    static final int REQUEST_PHONE = 2;
    SharedPreferences preferences;
    BroadcastReceiver br;
    CheckBoxItem photo_item;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String phone_number = preferences.getString(Names.Car.CAR_PHONE + car_id, "");
        if (phone_number.equals(""))
            phone_number = getString(R.string.phone_number_summary);

        String name = preferences.getString(Names.Car.CAR_NAME + car_id, "");
        if (name.equals("")) {
            name = getString(R.string.car);
            if (!car_id.equals(""))
                name += " " + car_id;
        }

        items.add(new Item(R.string.auth, R.string.auth_summary) {
            @Override
            void click() {
                Intent i = new Intent(getActivity(), AuthDialog.class);
                i.putExtra(Names.ID, car_id);
                i.putExtra(Names.Car.AUTH, true);
                startActivityForResult(i, REQUEST_AUTH);
            }
        });
        if (State.hasTelephony(getActivity())) {
            items.add(new Item(R.string.device_phone_number, phone_number) {
                @Override
                void click() {
                    Intent i = new Intent(getActivity(), AuthDialog.class);
                    i.putExtra(Names.ID, car_id);
                    i.putExtra(Names.Car.CAR_PHONE, true);
                    startActivityForResult(i, REQUEST_PHONE);
                }
            });
        }
        items.add(new Item(R.string.name, name) {
            @Override
            void click() {
                LayoutInflater inflater = getLayoutInflater(null);
                final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.name)
                        .setView(inflater.inflate(R.layout.name, null))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, null)
                        .create();
                dialog.show();
                final EditText et = (EditText) dialog.findViewById(R.id.text);
                et.setText(getValue());
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setValue(et.getText().toString());
                        dialog.dismiss();
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putString(Names.Car.CAR_NAME + car_id, getValue());
                        ed.commit();
                        update();
                    }
                });
            }
        });
        if (!preferences.getBoolean(Names.Car.POINTER + car_id, false)) {
            if (State.hasTelephony(getActivity())) {
                items.add(new Item(R.string.main_phone, R.string.init_phone) {
                    @Override
                    void click() {
                        Actions.init_phone(getActivity(), car_id);
                    }
                });
                items.add(new Item(R.string.phones, R.string.phones_sum) {
                    @Override
                    void click() {
                        Actions.requestPassword(getActivity(), car_id, R.string.phones, R.string.phones_sum, new Actions.Answer() {
                            @Override
                            void answer(String pswd) {
                                Intent i = new Intent(getActivity(), Phones.class);
                                i.putExtra(Names.ID, car_id);
                                if (pswd != null)
                                    i.putExtra(Names.PASSWORD, pswd);
                                startActivity(i);
                            }
                        });
                    }
                });
                items.add(new ListItem(R.string.control_method, R.array.ctrl_entries, R.array.ctrl_values, Names.Car.CONTROL + car_id, ""));
                items.add(new Item(R.string.alarm_mode, R.string.sms_mode_summary) {
                    @Override
                    void click() {
                        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.alarm_mode)
                                .setView(inflater.inflate(R.layout.alarm_mode, null))
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.ok, null)
                                .create();
                        dialog.show();
                        final RadioGroup grp = (RadioGroup) dialog.findViewById(R.id.mode);
                        final Button ok = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                        ok.setEnabled(false);
                        grp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                                ok.setEnabled(i != -1);
                            }
                        });
                        ok.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(final View view) {
                                final int mode = grp.getCheckedRadioButtonId();
                                final int id = (mode == R.id.sms) ? R.string.sms_mode : R.string.call_mode;
                                final String text = (mode == R.id.sms) ? "ALARM SMS" : "ALARM CALL";
                                Actions.requestPassword(view.getContext(), car_id, R.string.alarm_mode, id, new Actions.Answer() {
                                    @Override
                                    void answer(String pswd) {
                                        SmsMonitor.sendSMS(view.getContext(), car_id, pswd, new SmsMonitor.Sms(id, text, text + " OK"));
                                    }
                                });
                                dialog.dismiss();
                            }
                        });
                    }
                });
            }
            photo_item = new CheckBoxItem(R.string.show_photo, Names.Car.SHOW_PHOTO, false);
            items.add(photo_item);

            if (!preferences.getString(Names.Car.VERSION + car_id, "").toLowerCase().contains("superagent"))
                items.add(new CheckBoxItem(R.string.device_pswd, Names.Car.DEVICE_PSWD, false));
        }
        update();

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (photo_item == null)
                    return;
                photo_item.update();
                update();
            }
        };
        IntentFilter filter = new IntentFilter(FetchService.ACTION_UPDATE_FORCE);
        getActivity().registerReceiver(br, filter);

        return v;
    }

    @Override
    public void onDestroyView() {
        if (br != null)
            getActivity().unregisterReceiver(br);
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
        if (requestCode == REQUEST_AUTH) {
            SettingActivity activity = (SettingActivity) getActivity();
            Intent intent = new Intent(activity, FetchService.class);
            intent.putExtra(Names.ID, car_id);
            activity.startService(intent);
            activity.updateSettings();
        }
        if (requestCode == REQUEST_PHONE) {
            items.get(1).setValue(preferences.getString(Names.Car.CAR_PHONE + car_id, ""));
            update();
        }
    }
}
