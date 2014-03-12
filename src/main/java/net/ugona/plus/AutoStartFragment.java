package net.ugona.plus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AutoStartFragment extends DeviceFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        items.add(new ListItem(R.string.start_time, 20, R.array.start_timer));
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
        return v;
    }
}
