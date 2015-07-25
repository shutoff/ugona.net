package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class PasswordDialog extends DialogFragment
        implements View.OnClickListener,
        TextWatcher,
        SeekBar.OnSeekBarChangeListener {

    EditText etPasswd;
    View btnOk;
    String password;
    String title;
    String data;
    String units;
    TextView tvUnits;
    int min_value;
    View vError;
    boolean sent;
    SeekBar vValue;
    String car_id;
    int id;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            password = savedInstanceState.getString(Names.MESSAGE);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        if (title == null)
            title = getString(R.string.password);
        return new AlertDialogWrapper.Builder(getActivity())
                .setTitle(title)
                .setView(inflater.inflate(R.layout.password_dialog, null))
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setIcon(R.drawable.bl_password)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        MaterialDialog dialog = (MaterialDialog) getDialog();
        etPasswd = (EditText) dialog.findViewById(R.id.passwd);
        vError = dialog.findViewById(R.id.error);
        btnOk = dialog.getActionButton(DialogAction.POSITIVE);
        btnOk.setOnClickListener(this);
        etPasswd.addTextChangedListener(this);
        if (data != null) {
            try {
                JsonObject def = JsonValue.readFrom(data).asObject();
                String title = def.getString("title", "");
                min_value = def.getInt("min", 0);
                int max_value = def.getInt("max", 1);
                units = def.getString("units", "");
                vValue = (SeekBar) dialog.findViewById(R.id.value);
                vValue.setMax(max_value - min_value);
                TextView tvValueTitle = (TextView) dialog.findViewById(R.id.value_title);
                tvValueTitle.setText(title);
                tvUnits = (TextView) dialog.findViewById(R.id.value_text);
                vValue.setOnSeekBarChangeListener(this);
                CarConfig carConfig = CarConfig.get(getActivity(), car_id);
                int v = carConfig.getCommandValue(id);
                if (v < min_value)
                    v = min_value;
                if (v > max_value)
                    v = max_value;
                tvUnits.setText(v + " " + units);
                vValue.setProgress(v - min_value);
                dialog.findViewById(R.id.value_block).setVisibility(View.VISIBLE);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        afterTextChanged(etPasswd.getText());
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        password = args.getString(Names.MESSAGE);
        title = args.getString(Names.TITLE);
        data = args.getString(Names.VALUE);
        car_id = args.getString(Names.ID);
        id = args.getInt(Names.COMMAND);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.MESSAGE, password);
        outState.putString(Names.TITLE, title);
        outState.putString(Names.VALUE, data);
        outState.putString(Names.ID, car_id);
        outState.putInt(Names.COMMAND, id);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        btnOk.setEnabled(s.length() > 0);
        vError.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onClick(View v) {
        if ((password == null) || etPasswd.getText().toString().equals(password)) {
            Fragment fragment = getTargetFragment();
            if (fragment != null)
                sent = true;
            dismiss();
            if (fragment != null) {
                Intent data = new Intent();
                data.putExtra("pwd", etPasswd.getText().toString() + " ");
                if (vValue != null)
                    data.putExtra("v", vValue.getProgress() + min_value + "");
                fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
            }
            return;
        }
        etPasswd.setText("");
        vError.setVisibility(View.VISIBLE);
        return;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Fragment fragment = getTargetFragment();
        if ((fragment != null) && !sent)
            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        progress += min_value;
        tvUnits.setText(progress + " " + units);
        CarConfig carConfig = CarConfig.get(getActivity(), car_id);
        carConfig.setCommandValue(id, progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
