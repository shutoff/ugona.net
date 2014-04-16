package net.ugona.plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DeviceFragment extends SettingsFragment {

    BroadcastReceiver br;

    TextView tvLabel;
    View prgUpdate;
    View imgUpdate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        View control_block = v.findViewById(R.id.control_block);
        control_block.setVisibility(View.VISIBLE);

        tvLabel = (TextView) v.findViewById(R.id.control_label);
        prgUpdate = v.findViewById(R.id.control_prg);
        imgUpdate = v.findViewById(R.id.control_img);

        control_block.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingActivity activity = (SettingActivity) getActivity();
                activity.update();
            }
        });

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
            }
        };
        IntentFilter filter = new IntentFilter(SettingActivity.UPDATE_SETTINGS);
        getActivity().registerReceiver(br, filter);

        update();
        return v;
    }

    @Override
    public void onDestroyView() {
        getActivity().unregisterReceiver(br);
        super.onDestroyView();
    }

    void update() {
        super.update();
        SettingActivity activity = (SettingActivity) getActivity();
        if (activity == null)
            return;
        if (activity.values != null) {
            prgUpdate.setVisibility(View.GONE);
            imgUpdate.setVisibility(View.GONE);
            tvLabel.setText(R.string.setup);
            return;
        }
        if (activity.values_error) {
            prgUpdate.setVisibility(View.GONE);
            imgUpdate.setVisibility(View.VISIBLE);
            tvLabel.setText(R.string.error_load);
            return;
        }
        prgUpdate.setVisibility(View.VISIBLE);
        imgUpdate.setVisibility(View.GONE);
        tvLabel.setText(R.string.loading);
    }

    int getVal(int index) {
        SettingActivity activity = (SettingActivity) getActivity();
        if ((activity != null) && (activity.values != null))
            return activity.values[index];
        return preferences.getInt("V_" + index + "_" + car_id, 0);
    }

    int getOldVal(int index) {
        SettingActivity activity = (SettingActivity) getActivity();
        if ((activity != null) && (activity.old_values != null))
            return activity.old_values[index];
        return preferences.getInt("V_" + index + "_" + car_id, 0);
    }

    void setVal(int index, int v) {
        SettingActivity activity = (SettingActivity) getActivity();
        if ((activity != null) && (activity.values != null) && (activity.values[index] != v))
            activity.values[index] = v;
    }

    class CheckBitItem extends CheckItem {
        int word;
        int mask;

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
            setChanged();
        }

        @Override
        boolean changed() {
            int v = getVal(word);
            int ov = getOldVal(word);
            return (v & mask) != (ov & mask);
        }

    }

    class SeekBarItem extends SeekItem {

        int word;

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
            setChanged();
        }

        @Override
        boolean changed() {
            return getVal(word) != getOldVal(word);
        }

    }

    class ListItem extends SpinnerItem {

        String[] values;
        int word;

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
                    setChanged();
                    break;
                }
            }
        }

        @Override
        boolean changed() {
            return getVal(word) != getOldVal(word);
        }

    }
}
