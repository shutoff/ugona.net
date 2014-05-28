package net.ugona.plus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HeaterFragment extends DeviceFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        items.add(new SettingsFragment.ListItem(R.string.connection, R.array.heater_values, R.array.heater, Names.Car.CAR_RELE, "1"));
        items.add(new CheckBoxItem(R.string.impulse, Names.Car.RELE_IMPULSE, true));
        items.add(new SeekBarItem(R.string.heater_time, 13, 0, 255, R.string.minutes));
        items.add(new CheckBitItem(R.string.heater_start, 12, 2));

        TimerCommands cmd = new TimerCommands();
        String rele = preferences.getString(Names.Car.CAR_RELE + car_id, "1");
        if (rele.equals("3")) {
            cmd.add(new TimerCommand(3, R.drawable.icon_heater, R.string.heater_on));
            cmd.add(new TimerCommand(6, R.drawable.icon_heater_air, R.string.heater_air));
            cmd.add(new TimerCommand(5, R.drawable.icon_air, R.string.air));
            cmd.add(new TimerCommand(4, R.drawable.icon_heater_on, R.string.heater_off));
        }
        addTimers(cmd);
        return v;
    }
}
