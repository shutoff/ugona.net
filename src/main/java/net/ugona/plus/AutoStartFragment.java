package net.ugona.plus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Vector;

public class AutoStartFragment extends DeviceFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        fill();
        return v;
    }

    @Override
    void update() {
        fill();
        super.update();
    }

    void fill() {
        items = new Vector<Item>();
        items.add(new ListItem(R.string.start_time, 20, R.array.start_timer));
        items.add(new USeekBarItem(R.string.voltage_limit, 18, 60, 93, R.string.v, 0.13) {
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
        if (!State.isPandora(preferences, car_id)) {
            items.add(new SeekBarItem(R.string.az_temp, 24, -40, 70, R.string.temp_unit));
            items.add(new CheckBitItem(R.string.soft_start, 19, 1));
            if (State.hasTelephony(getActivity()))
                items.add(new CheckBitItem(R.string.inf_sms, 23, 1) {
                    @Override
                    void setView(View v) {
                        super.setView(v);
                        TextView tv = (TextView) v.findViewById(R.id.value);
                        tv.setVisibility(View.VISIBLE);
                        tv.setText(R.string.inf_sms_msg);
                    }

                    @Override
                    String getValue() {
                        return super.getValue().equals("") ? "1" : "";
                    }

                    @Override
                    void setValue(String value) {
                        super.setValue(value.equals("") ? "1" : "");
                    }
                });
        }

        items.add(new SettingsFragment.ListItem(R.string.az_notify, R.array.notify_entries, R.array.notify_values, Names.Car.AZ_MODE, ""));

        TimerCommands cmd = new TimerCommands();
        cmd.add(new TimerCommand(1, 0, R.string.motor_on));
        cmd.add(new TimerCommand(10, 0, R.string.az_with_t));
        addTimers(cmd);
    }

}
