package net.ugona.plus;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;

public class CommandsSettingsDialog
        extends DialogFragment
        implements DialogInterface.OnClickListener {

    String id;
    Spinner spinner;
    Spinner simSpinner;
    CheckBox devicePswd;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            id = savedInstanceState.getString(Names.ID);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        return new AlertDialogWrapper.Builder(getActivity())
                .setTitle(R.string.action_settings)
                .setView(inflater.inflate(R.layout.command_settings_dialog, null))
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        id = args.getString(Names.ID);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, id);
    }

    @Override
    public void onStart() {
        super.onStart();
        spinner = (Spinner) getDialog().findViewById(R.id.method);
        final String[] items = getResources().getStringArray(R.array.ctrl_entries);
        spinner.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return items.length;
            }

            @Override
            public Object getItem(int position) {
                return items[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    v = inflater.inflate(R.layout.list_item, null);
                }
                TextView tv = (TextView) v;
                tv.setText(items[position]);
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    v = inflater.inflate(R.layout.list_dropdown_item, null);
                }
                TextView tv = (TextView) v;
                tv.setText(items[position]);
                return v;
            }
        });
        simSpinner = (Spinner) getDialog().findViewById(R.id.sim);
        final String[] sims = getResources().getStringArray(R.array.sim_entries);
        simSpinner.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return sims.length;
            }

            @Override
            public Object getItem(int position) {
                return sims[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    v = inflater.inflate(R.layout.list_item, null);
                }
                TextView tv = (TextView) v;
                tv.setText(sims[position]);
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    v = inflater.inflate(R.layout.list_dropdown_item, null);
                }
                TextView tv = (TextView) v;
                tv.setText(sims[position]);
                return v;
            }
        });

        final CarConfig config = CarConfig.get(getActivity(), id);
        spinner.setSelection(config.isInet_cmd() ? 1 : 0);
        simSpinner.setSelection(config.getSim_cmd());
        if (!State.isDualSim(getActivity()))
            getDialog().findViewById(R.id.sim_block).setVisibility(View.GONE);
        devicePswd = (CheckBox) getDialog().findViewById(R.id.device_passwd);
        CarState state = CarState.get(getActivity(), id);
        if (state.isDevice_password()) {
            devicePswd.setChecked(config.isDevice_password());
        } else {
            devicePswd.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final CarConfig config = CarConfig.get(getActivity(), id);
        config.setInet_cmd(spinner.getSelectedItemPosition() > 0);
        config.setSim_cmd(simSpinner.getSelectedItemPosition());
        config.setDevice_password(devicePswd.isChecked());
    }
}
