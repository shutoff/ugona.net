package net.ugona.plus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CommandsFragment extends SettingsFragment {

    class CheckBoxItem extends CheckItem {

        CheckBoxItem(int n, int mask) {
            super(n);
            mask_ = mask;
        }

        @Override
        String getValue() {
            return ((State.getCommands(preferences, car_id) & mask_) != 0) ? "1" : "";
        }

        @Override
        void setValue(String value) {
            SharedPreferences.Editor ed = preferences.edit();
            int state = State.getCommands(preferences, car_id);
            state &= ~mask_;
            if (!value.equals(""))
                state |= mask_;
            ed.putInt(Names.COMMANDS + car_id, state);
            ed.commit();
            Intent i = new Intent(FetchService.ACTION_UPDATE_FORCE);
            i.putExtra(Names.ID, car_id);
            getActivity().sendBroadcast(i);
        }

        int mask_;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        items.add(new CheckBoxItem(R.string.call, State.CMD_CALL));
        items.add(new CheckBoxItem(R.string.valet_cmd, State.CMD_VALET));
        items.add(new CheckBoxItem(R.string.autostart, State.CMD_AZ));
        items.add(new CheckBoxItem(R.string.rele, State.CMD_RELE));
        items.add(new CheckBoxItem(R.string.rele1, State.CMD_RELE1));
        items.add(new CheckBoxItem(R.string.rele1i, State.CMD_RELE1I));
        items.add(new CheckBoxItem(R.string.rele2, State.CMD_RELE2));
        items.add(new CheckBoxItem(R.string.rele2i, State.CMD_RELE2I));
        return v;
    }
}
