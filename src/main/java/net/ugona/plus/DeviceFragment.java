package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

public class DeviceFragment extends SettingsFragment {

    final static String URL_SETTINGS = "https://car-online.ugona.net/settings?auth=$1";
    final static String URL_SET = "https://car-online.ugona.net/set?auth=$1&v=$2";

    SharedPreferences preferences;
    Button btn;
    int[] values;
    int[] old_values;

    class SeekBarPrefItem extends SeekItem {
        SeekBarPrefItem(int name, String id_key, int min, int max) {
            super(name, min, max, " °C");
            key = id_key;
            setValue(preferences.getInt(key + car_id, 0) + "");
        }

        @Override
        void click() {
            SharedPreferences.Editor ed = preferences.edit();
            ed.putInt(key + car_id, Integer.parseInt(getValue()));
            ed.commit();
            Intent intent = new Intent(FetchService.ACTION_UPDATE_FORCE);
            intent.putExtra(Names.ID, car_id);
            getActivity().sendBroadcast(intent);
        }

        String key;
    }

    class CheckBitItem extends CheckItem {
        CheckBitItem(int name, int word_, int bit) {
            super(name);
            word = word_;
            mask = 1 << bit;
        }

        @Override
        String getValue() {
            int v = getVal(word);
            return ((v & mask) != 0) ? "1" : "";
        }

        @Override
        void setValue(String value) {
            int v = getVal(word) & ~mask;
            if (!value.equals(""))
                v |= mask;
            setVal(word, v);
        }

        int word;
        int mask;

    }

    class SeekBarItem extends SeekItem {

        SeekBarItem(int name, int word_, int min, int max, int unit) {
            super(name, min, max, " " + getString(unit));
            word = word_;
        }

        SeekBarItem(int name, int word_, int min, int max, int unit, double k) {
            super(name, min, max, " " + getString(unit), k);
            word = word_;
        }

        @Override
        String getValue() {
            return getVal(word) + "";
        }

        @Override
        void setValue(String value) {
            setVal(word, Integer.parseInt(value));
        }

        int word;
    }

    class SeekBarListItem extends SeekBarItem {

        SeekBarListItem(int name, int word, int values_id) {
            super(name, word, 1, getResources().getStringArray(values_id).length, R.string.unit);
            values = getResources().getStringArray(values_id);
        }

        @Override
        String textValue(int progress) {
            return values[progress];
        }

        String[] values;
    }

    class ListItem extends SpinnerItem {

        ListItem(int name, int word_, int values_id) {
            super(name, values_id, values_id);
            word = word_;
            values = getResources().getStringArray(values_id);
        }

        @Override
        String getValue() {
            return values[getVal(word)];
        }

        @Override
        void setValue(String value) {
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(value)) {
                    setVal(word, i);
                    break;
                }
            }
        }

        String[] values;
        int word;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        btn = (Button) v.findViewById(R.id.control);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        items.add(new SeekBarItem(R.string.timer, 21, 1, 120, R.string.minutes));
        items.add(new ListItem(R.string.start_time, 20, R.array.start_timer));
        items.add(new CheckBitItem(R.string.guard_sound, 0, 6));
        items.add(new CheckBitItem(R.string.tilt_low, 2, 1));
        items.add(new CheckBitItem(R.string.guard_partial, 2, 2));
        items.add(new CheckBitItem(R.string.comfort_enable, 9, 5));
        items.add(new SeekBarItem(R.string.tilt_level, 1, 15, 45, R.string.unit));
        items.add(new SeekBarItem(R.string.guard_time, 4, 15, 60, R.string.sec));
        items.add(new SeekBarItem(R.string.robbery_time, 5, 1, 30, R.string.sec, 20));
        items.add(new CheckBitItem(R.string.heater_start, 12, 2));
        items.add(new CheckBitItem(R.string.no_sound, 12, 3));
        items.add(new SeekBarItem(R.string.heater_time, 13, 0, 255, R.string.minutes));
        items.add(new SeekBarListItem(R.string.shock_sens, 14, R.array.levels));
        items.add(new SeekBarItem(R.string.voltage_limit, 18, 60, 93, R.string.v, 0.13) {
            @Override
            String textValue(int progress) {
                if (progress <= 0)
                    return getString(R.string.no_start);
                return super.textValue(progress);
            }

            @Override
            String getValue() {
                if ((getVal(12) & (1 << 7)) == 0)
                    return "0";
                return super.getValue();
            }

            @Override
            void setValue(String value) {
                int v = Integer.parseInt(value);
                int val = getVal(12);
                int mask = 1 << 7;
                val &= ~mask;
                if (v <= min_value) {
                    setVal(12, val);
                    return;
                }
                val |= mask;
                setVal(12, val);
                super.setValue(value);
            }
        });
        items.add(new CheckBitItem(R.string.soft_start, 19, 1));
        items.add(new SeekBarPrefItem(R.string.temp_correct, Names.TEMP_SIFT, -10, 10));
        items.add(new Item(R.string.version, preferences.getString(Names.VERSION + car_id, "")));

        v.findViewById(R.id.control_block).setVisibility(View.VISIBLE);

        if (savedInstanceState != null)
            values = savedInstanceState.getIntArray("values");

        if (values == null) {
            updateSettings();
        } else {
            btn.setText(R.string.ok);
        }

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (values == null)
                    return;
                String val = "";
                for (int i = 0; i < values.length; i++) {
                    if (values[i] == old_values[i])
                        continue;
                    if (!val.equals(""))
                        val += ",";
                    val += i + "." + values[i];
                }
                if (val.equals(""))
                    return;
                final String value = val;
                Actions.requestPassword(getActivity(), R.string.setup, "", new Runnable() {
                    @Override
                    public void run() {
                        Context context = getActivity();
                        final ProgressDialog progressDialog = new ProgressDialog(context);
                        progressDialog.setMessage(context.getString(R.string.send_command));
                        progressDialog.show();

                        HttpTask task = new HttpTask() {

                            @Override
                            void result(JsonObject res) throws ParseException {
                                final Context context = getActivity();
                                if (context == null)
                                    return;
                                progressDialog.dismiss();
                                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                                final AlertDialog dialog = new AlertDialog.Builder(context)
                                        .setTitle(R.string.setup)
                                        .setView(inflater.inflate(R.layout.wait, null))
                                        .setNegativeButton(R.string.ok, null)
                                        .create();
                                dialog.show();
                                TextView tv = (TextView) dialog.findViewById(R.id.msg);
                                final int wait_time = preferences.getInt(Names.CAR_TIMER + car_id, 10);
                                String msg = context.getString(R.string.wait_msg).replace("$1", wait_time + "");
                                tv.setText(msg);
                                Button btnCall = (Button) dialog.findViewById(R.id.call);
                                btnCall.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        dialog.dismiss();
                                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                                        Intent intent = new Intent(Intent.ACTION_CALL);
                                        intent.setData(Uri.parse("tel:" + preferences.getString(Names.CAR_PHONE + car_id, "")));
                                        context.startActivity(intent);
                                    }
                                });
                                Button btnSms = (Button) dialog.findViewById(R.id.sms);
                                btnSms.setVisibility(View.GONE);
                            }

                            @Override
                            void error() {
                                if (getActivity() == null)
                                    return;
                                progressDialog.dismiss();
                                Toast toast = Toast.makeText(getActivity(), R.string.data_error, Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        };
                        task.execute(URL_SET, preferences.getString(Names.AUTH + car_id, ""), value);

                    }
                });
            }
        });

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (values != null)
            outState.putIntArray("values", values);
    }

    void updateSettings() {
        HttpTask task = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                SharedPreferences.Editor ed = preferences.edit();
                values = new int[22];
                old_values = new int[22];
                for (int i = 0; i < 22; i++) {
                    int v = res.get("v" + i).asInt();
                    ed.putInt("V_" + i + "_" + car_id, v);
                    values[i] = v;
                    old_values[i] = v;
                }
                ed.commit();
                update();
                btn.setText(R.string.setup);
            }

            @Override
            void error() {
                btn.setText(R.string.error_read);
            }
        };
        task.execute(URL_SETTINGS, preferences.getString(Names.AUTH + car_id, ""));
    }

    int getVal(int index) {
        if (values != null)
            return values[index];
        return preferences.getInt("V_" + index + "_" + car_id, 0);
    }

    void setVal(int index, int v) {
        if ((values != null) && (values[index] != v))
            values[index] = v;
    }
}
