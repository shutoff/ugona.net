package net.ugona.plus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class DeviceSettingsFragment extends DeviceFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        if (State.isPandora(preferences, car_id)) {
            items.add(new SeekBitItem(R.string.shock_sensor_secondary, 0, 0, 1));
            items.add(new SeekBitItem(R.string.shock_sensor_primary, 0, 1, 2));
            items.add(new SeekBitItem(R.string.move_sensor, 0, 2, 3));
            items.add(new SeekBitItem(R.string.shock_sensor_secondary, 0, 3, 4));
            items.add(new SeekBitItem(R.string.shock_sensor_primary, 0, 4, 5));
        } else {
            items.add(new SeekBarItem(R.string.timer, 21, 1, 30, R.string.minutes));
            items.add(new CheckBitItem(R.string.guard_sound, 0, 7));
            items.add(new CheckBitItem(R.string.no_sound, 12, 3));
            items.add(new CheckBitItem(R.string.guard_partial, 2, 2));
            items.add(new CheckBitItem(R.string.comfort_enable, 9, 5));
            if (State.hasTelephony(getActivity()))
                items.add(new DeviceFragment.ListItem(R.string.alarm_shock, 22, R.array.shock_entries));
            items.add(new SeekBarItem(R.string.guard_time, 4, 15, 60, R.string.sec));
            items.add(new SeekBarItem(R.string.robbery_time, 5, 1, 30, R.string.sec, 20));
            items.add(new SeekBarItem(R.string.door_time, 10, 10, 30, R.string.sec));
            items.add(new SeekBarListItem(R.string.shock_sens, 14, R.array.levels));
            items.add(new CheckBitItem(R.string.tilt_low, 2, 1));
            items.add(new SeekBarItem(R.string.tilt_level, 1, 15, 45, R.string.unit));
            if (Preferences.getTemperaturesCount(preferences, car_id) == 1)
                items.add(new SeekBarPrefItem(R.string.temp_correct, Names.Car.TEMP_SIFT, -10, 10, "\u00B0C", 1));
            items.add(new SeekBarPrefItem(R.string.voltage_shift, Names.Car.VOLTAGE_SHIFT, -20, 20, "V", 0.05));
        }
        items.add(new Item(R.string.version, preferences.getString(Names.Car.VERSION + car_id, "")));
        return v;
    }

    class SeekBitItem extends CheckBitItem {

        int val;

        SeekBitItem(int name, int word_, int bit, int val_) {
            super(name, word_, bit);
            val = val_;
        }

        @Override
        void setView(View v) {
            super.setView(v);
            final SeekBar seekBar = (SeekBar) v.findViewById(R.id.seekbar);
            seekBar.setVisibility(View.VISIBLE);
            seekBar.setOnSeekBarChangeListener(null);
            CheckBox checkBox = (CheckBox) v.findViewById(R.id.check);
            seekBar.setEnabled(checkBox.isChecked());
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    seekBar.setEnabled(isChecked);
                }
            });
            final TextView tvVal = (TextView) v.findViewById(R.id.v1);
            tvVal.setVisibility(View.VISIBLE);
            int value = getVal(val);
            tvVal.setText(value + "%");
            seekBar.setMax(100);
            seekBar.setProgress(value);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    setVal(val, progress);
                    tvVal.setText(progress + "%");
                    click();
                    setChanged();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }

        @Override
        boolean changed() {
            if (super.changed())
                return true;
            if (getValue().equals(""))
                return false;
            return getVal(val) != getOldVal(val);
        }
    }

    class SeekBarListItem extends SeekBarItem {

        String[] values;

        SeekBarListItem(int name, int word, int values_id) {
            super(name, word, 1, getResources().getStringArray(values_id).length, R.string.unit);
            values = getResources().getStringArray(values_id);
        }

        @Override
        String textValue(int progress) {
            int i = values.length - progress - 1;
            if (i < 0)
                i = 0;
            if (i >= values.length)
                i = values.length - 1;
            return values[i];
        }

        @Override
        String getValue() {
            int v = Integer.parseInt(super.getValue());
            String res = (values.length - v + 1) + "";
            return res;
        }

        @Override
        void setValue(String value) {
            int v = values.length - Integer.parseInt(value) + 1;
            super.setValue(v + "");
        }
    }
}
