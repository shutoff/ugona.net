package net.ugona.plus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class CommandsFragment extends SettingsFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        items.add(new CheckBoxItem(R.string.call, State.CMD_CALL));
        items.add(new CheckBoxItem(R.string.search, State.CMD_SEARCH));
        if (!State.isPandora(preferences, car_id)) {
            items.add(new CheckBoxItem(R.string.valet_cmd, State.CMD_VALET));
        } else {
            items.add(new CheckBoxItem(R.string.guard_on, State.CMD_GUARD));
            items.add(new CheckBoxItem(R.string.open_trunk, State.CMD_TRUNK));
        }
        items.add(new CheckBoxItem(R.string.autostart, State.CMD_AZ));
        items.add(new CheckBoxItem(R.string.rele, State.CMD_RELE));
        if (!State.isPandora(preferences, car_id)) {
            items.add(new CheckBoxItem(R.string.silent_mode, State.CMD_SOUND));
            items.add(new CheckBoxEditItem(R.string.rele1, State.CMD_RELE1, Names.Car.RELE1_NAME));
            items.add(new CheckBoxEditItem(R.string.rele1i, State.CMD_RELE1I, Names.Car.RELE1I_NAME));
            items.add(new CheckBoxEditItem(R.string.rele2, State.CMD_RELE2, Names.Car.RELE2_NAME));
            items.add(new CheckBoxEditItem(R.string.rele2i, State.CMD_RELE2I, Names.Car.RELE2I_NAME));
        }
        return v;
    }

    class CheckBoxItem extends CheckItem {

        int mask_;

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
            ed.putInt(Names.Car.COMMANDS + car_id, state);
            ed.commit();
            Intent i = new Intent(FetchService.ACTION_UPDATE_FORCE);
            i.putExtra(Names.ID, car_id);
            getActivity().sendBroadcast(i);
        }
    }

    class CheckBoxEditItem extends CheckBoxItem {

        String key_;

        CheckBoxEditItem(int n, int mask, String key) {
            super(n, mask);
            key_ = key;
        }

        @Override
        void setView(View v) {
            super.setView(v);
            View edit = v.findViewById(R.id.check_edit);
            edit.setVisibility(View.VISIBLE);
            edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setTitle(name)
                            .setMessage(R.string.command_name)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.ok, null)
                            .setView(inflater.inflate(R.layout.text_input, null))
                            .create();
                    dialog.show();
                    final EditText et = (EditText) dialog.findViewById(R.id.text);
                    et.setText(preferences.getString(key_ + car_id, name));
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String n = et.getText().toString();
                            if (n.equals(name))
                                n = "";
                            SharedPreferences.Editor ed = preferences.edit();
                            ed.putString(key_ + car_id, n);
                            ed.commit();
                            Intent i = new Intent(FetchService.ACTION_UPDATE_FORCE);
                            i.putExtra(Names.ID, car_id);
                            getActivity().sendBroadcast(i);
                            dialog.dismiss();
                            update();
                        }
                    });
                }
            });
            String n = preferences.getString(key_ + car_id, name);
            if (n.equals(""))
                n = name;
            if (!n.equals(name)) {
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.check);
                checkBox.setText(n);
                TextView tv = (TextView) v.findViewById(R.id.value);
                tv.setText(name);
                tv.setVisibility(View.VISIBLE);
            }
        }
    }
}
