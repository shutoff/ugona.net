package net.ugona.plus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class TemperatureFragment extends SettingsFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        String[] data = preferences.getString(Names.Car.TEMPERATURE + car_id, "").split(";");
        for (String d : data) {
            String[] sensor_data = d.split(":");
            try {
                int sensor = Integer.parseInt(sensor_data[0]);
                if (sensor == 1) {
                    items.add(new TempMainItem(Integer.parseInt(sensor_data[1])));
                } else {
                    items.add(new TempItem(sensor, Integer.parseInt(sensor_data[1])));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return v;
    }

    class TempItem extends SeekItem {

        TextView tvTemp;
        String[] values;
        int sensor;
        int temp;
        int shift;
        int where;

        TempItem(int id, int t) {
            super(R.string.temp_correct, -10, 10, "\u00B0C", 1);
            sensor = id;
            temp = t;

            String[] settings = preferences.getString(Names.Car.TEMP_SETTINGS + car_id, "").split(",");
            for (String s : settings) {
                String[] data = s.split(":");
                if (data.length != 3)
                    continue;
                try {
                    if (Integer.parseInt(data[0]) == sensor) {
                        shift = Integer.parseInt(data[1]);
                        where = Integer.parseInt(data[2]);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            values = getResources().getStringArray(R.array.temp_where);
        }

        @Override
        void setView(View v) {
            super.setView(v);
            v.findViewById(R.id.block_temp).setVisibility(View.VISIBLE);
            TextView tvSensor = (TextView) v.findViewById(R.id.sensor);
            tvSensor.setText(getString(R.string.sensor) + ": " + sensor);
            tvTemp = (TextView) v.findViewById(R.id.temp);
            tvTemp.setText((temp + Integer.parseInt(getValue())) + " \u00B0C");
            Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
            spinner.setVisibility(View.VISIBLE);
            spinner.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return values.length;
                }

                @Override
                public Object getItem(int position) {
                    return values[position];
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = convertView;
                    if (v == null) {
                        LayoutInflater inflater = (LayoutInflater) getActivity()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        v = inflater.inflate(R.layout.list_item, null);
                    }
                    TextView tv = (TextView) v.findViewById(R.id.name);
                    tv.setText(values[position]);
                    return v;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View v = convertView;
                    if (v == null) {
                        LayoutInflater inflater = (LayoutInflater) getActivity()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        v = inflater.inflate(R.layout.list_dropdown_item, null);
                    }
                    TextView tv = (TextView) v.findViewById(R.id.name);
                    tv.setText(values[position]);
                    return v;
                }
            });
            if (where < values.length)
                spinner.setSelection(where);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    where = position;
                    click();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        @Override
        void click() {
            String[] settings = preferences.getString(Names.Car.TEMP_SETTINGS + car_id, "").split(",");
            String res = "";
            for (String s : settings) {
                String[] data = s.split(":");
                if (data.length == 3) {
                    try {
                        if (Integer.parseInt(data[0]) == sensor) {
                            continue;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                if (!res.equals(""))
                    res += ",";
                res += s;
            }
            if (!res.equals(""))
                res += ",";
            res += sensor + ":" + shift + ":" + where;
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(Names.Car.TEMP_SETTINGS + car_id, res);
            ed.commit();
            super.click();
            tvTemp.setText((temp + shift) + " \u00B0C");
            Intent intent = new Intent(FetchService.ACTION_UPDATE_FORCE);
            intent.putExtra(Names.ID, car_id);
            getActivity().sendBroadcast(intent);
        }

        @Override
        String getValue() {
            return shift + "";
        }

        @Override
        void setValue(String value) {
            super.setValue(value);
            shift = Integer.parseInt(value);
        }
    }

    class TempMainItem extends SeekBarPrefItem {

        TextView tvTemp;
        int temp;

        TempMainItem(int t) {
            super(R.string.temp_correct, Names.Car.TEMP_SIFT, -10, 10, "\u00B0C", 1);
            temp = t;
        }

        @Override
        void setView(View v) {
            super.setView(v);
            v.findViewById(R.id.block_temp).setVisibility(View.VISIBLE);
            TextView tvSensor = (TextView) v.findViewById(R.id.sensor);
            tvSensor.setText(getString(R.string.sensor) + ": " + 1);
            tvTemp = (TextView) v.findViewById(R.id.temp);
            tvTemp.setText((temp + Integer.parseInt(getValue())) + " \u00B0C");
        }

        @Override
        void click() {
            super.click();
            tvTemp.setText((temp + Integer.parseInt(getValue())) + " \u00B0C");
        }
    }
}
