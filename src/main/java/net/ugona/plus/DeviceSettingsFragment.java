package net.ugona.plus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DeviceSettingsFragment extends DeviceFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        items.add(new SeekBarItem(R.string.timer, 21, 1, 30, R.string.minutes));
        items.add(new CheckBitItem(R.string.guard_sound, 0, 6));
        items.add(new CheckBitItem(R.string.no_sound, 12, 3));
        items.add(new CheckBitItem(R.string.guard_partial, 2, 2));
        items.add(new CheckBitItem(R.string.comfort_enable, 9, 5));
        items.add(new SeekBarItem(R.string.guard_time, 4, 15, 60, R.string.sec));
        items.add(new SeekBarItem(R.string.robbery_time, 5, 1, 30, R.string.sec, 20));
        items.add(new SeekBarListItem(R.string.shock_sens, 14, R.array.levels));
        items.add(new CheckBitItem(R.string.tilt_low, 2, 1));
        items.add(new SeekBarItem(R.string.tilt_level, 1, 15, 45, R.string.unit));
        items.add(new SeekBarPrefItem(R.string.temp_correct, Names.TEMP_SIFT, -10, 10));
        items.add(new Item(R.string.version, preferences.getString(Names.VERSION + car_id, "")));

        return v;
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
