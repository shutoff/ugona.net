package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class SmsSettingsFragment
        extends DialogFragment
        implements DialogInterface.OnClickListener,
        RadioGroup.OnCheckedChangeListener {

    String car_id;
    String id;

    RadioGroup group;
    Button btnOk;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        CarConfig config = CarConfig.get(getActivity(), car_id);
        CarConfig.Setting[] settings = config.getSettings();
        CarConfig.Command[] commands = config.getCmd();
        for (CarConfig.Setting setting : settings) {
            if (!setting.id.equals(id))
                continue;
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.sms_settings, null);
            group = (RadioGroup) v;
            if (setting.cmd != null) {
                for (int cmd : setting.cmd) {
                    for (CarConfig.Command command : commands) {
                        if (command.id != cmd)
                            continue;
                        RadioButton rb = new RadioButton(getActivity());
                        rb.setId(cmd);
                        rb.setText(command.name);
                        group.addView(rb);
                        break;
                    }
                }
            }
            group.setOnCheckedChangeListener(this);
            return new AlertDialog.Builder(getActivity())
                    .setTitle(setting.name)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, this)
                    .setView(v)
                    .create();
        }
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        btnOk = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        onCheckedChanged(group, 0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
        outState.putString(Names.TITLE, id);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    void setArgs(Bundle args) {
        car_id = args.getString(Names.ID);
        id = args.getString(Names.TITLE);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Bundle args = new Bundle();
        args.putString(Names.ID, car_id);
        args.putInt(Names.COMMAND, group.getCheckedRadioButtonId());
        args.putBoolean(Names.NO_PROMPT, true);
        SendCommandFragment fragment = new SendCommandFragment();
        fragment.setArguments(args);
        fragment.show(getActivity().getSupportFragmentManager(), "send");
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        int checked = group.getCheckedRadioButtonId();
        if (btnOk != null)
            btnOk.setEnabled(checked != -1);
    }
}
