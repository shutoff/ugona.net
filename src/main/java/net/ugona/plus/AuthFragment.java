package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class AuthFragment extends SettingsFragment {

    static final int REQUEST_AUTH = 1;
    static final int REQUEST_PHONE = 2;
    SharedPreferences preferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String phone_number = preferences.getString(Names.CAR_PHONE + car_id, "");
        if (phone_number.equals(""))
            phone_number = getString(R.string.phone_number_summary);

        String name = preferences.getString(Names.CAR_NAME + car_id, "");
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
                i.putExtra(Names.AUTH, true);
                startActivityForResult(i, REQUEST_AUTH);
            }
        });
        if (State.hasTelephony(getActivity())) {
            items.add(new Item(R.string.device_phone_number, phone_number) {
                @Override
                void click() {
                    Intent i = new Intent(getActivity(), AuthDialog.class);
                    i.putExtra(Names.ID, car_id);
                    i.putExtra(Names.CAR_PHONE, true);
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
                        ed.putString(Names.CAR_NAME + car_id, getValue());
                        ed.commit();
                        update();
                    }
                });
            }
        });
        if (!preferences.getBoolean(Names.POINTER + car_id, false)) {
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
                        Actions.requestPassword(getActivity(), R.string.phones, R.string.phones_sum, new Runnable() {
                            @Override
                            public void run() {
                                Intent i = new Intent(getActivity(), Phones.class);
                                i.putExtra(Names.ID, car_id);
                                startActivity(i);
                            }
                        });
                    }
                });
                items.add(new ListItem(R.string.control_method, R.array.ctrl_entries, R.array.ctrl_values, Names.CONTROL + car_id, ""));
            }
            items.add(new CheckBoxItem(R.string.show_photo, Names.SHOW_PHOTO, false));
        }
        update();

        return v;
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
            items.get(1).setValue(preferences.getString(Names.CAR_PHONE + car_id, ""));
            update();
        }
    }
}
