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

    SharedPreferences preferences;

    static final int REQUEST_PHONE = 1;

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
                SettingActivity.getApiKey(getActivity(), car_id, new Runnable() {
                            @Override
                            public void run() {

                            }
                        }, new Runnable() {
                            @Override
                            public void run() {

                            }
                        }
                );
            }
        });
        items.add(new Item(R.string.phone_number, phone_number) {
            @Override
            void click() {
                Intent i = new Intent(getActivity(), PhoneNumberDialog.class);
                i.putExtra(Names.CAR_PHONE, preferences.getString(Names.CAR_PHONE + car_id, ""));
                startActivityForResult(i, REQUEST_PHONE);
            }
        });
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
        update();

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
        if (requestCode == REQUEST_PHONE) {
            String number = data.getStringExtra(Names.CAR_PHONE);
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(Names.CAR_PHONE + car_id, number);
            ed.commit();
            items.get(1).setValue(number);
            update();
        }
    }
}
