package net.ugona.plus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HeaterFragment extends DeviceFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        items.add(new SettingsFragment.ListItem(R.string.connection, R.array.heater_values, R.array.heater, Names.CAR_RELE, "1"));
        items.add(new CheckBoxItem(R.string.impulse, Names.RELE_IMPULSE, true));
        items.add(new SeekBarItem(R.string.heater_time, 13, 0, 255, R.string.minutes));
        items.add(new CheckBitItem(R.string.heater_start, 12, 2));
        return v;
    }
}
