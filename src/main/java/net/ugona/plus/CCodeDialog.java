package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

public class CCodeDialog
        extends DialogFragment
        implements TextWatcher,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {

    final String TEXT = "text";

    String id;
    int inet;

    Button btnOk;
    EditText etCCodeNum;
    EditText etCCodeText;
    CheckBox chkNumber;
    String init_string;
    Handler handler;

    boolean sent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            setArgs(savedInstanceState);
            init_string = savedInstanceState.getString(TEXT);
        }
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.require_ccode)
                .setView(inflater.inflate(R.layout.ccode_dialog, null))
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        return dialog;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    void setArgs(Bundle args) {
        id = args.getString(Names.ID);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, id);
        EditText e = chkNumber.isChecked() ? etCCodeText : etCCodeNum;
        outState.putString(TEXT, e.getText().toString());
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        btnOk = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        btnOk.setOnClickListener(this);
        etCCodeNum = (EditText) dialog.findViewById(R.id.ccode_num);
        etCCodeText = (EditText) dialog.findViewById(R.id.ccode_text);
        etCCodeNum.addTextChangedListener(this);
        etCCodeText.addTextChangedListener(this);
        chkNumber = (CheckBox) dialog.findViewById(R.id.number);
        chkNumber.setOnCheckedChangeListener(this);
        afterTextChanged(null);
        CarConfig config = CarConfig.get(getActivity(), id);
        chkNumber.setChecked(config.isCcode_text());
        if (init_string != null) {
            EditText e = chkNumber.isChecked() ? etCCodeText : etCCodeNum;
            e.setText(init_string);
            e.setSelection(init_string.length(), init_string.length());
        }
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        EditText et = chkNumber.isChecked() ? etCCodeText : etCCodeNum;
        String ccode = et.getText().toString();
        btnOk.setEnabled(ccode.length() > 0);
        if (ccode.length() == 6)
            onClick(btnOk);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        EditText e = null;
        if (isChecked) {
            etCCodeNum.setVisibility(View.GONE);
            e = etCCodeText;
        } else {
            etCCodeText.setVisibility(View.GONE);
            e = etCCodeNum;
        }
        e.setVisibility(View.VISIBLE);
        e.setText("");
        e.requestFocus();
        CarConfig config = CarConfig.get(getActivity(), id);
        config.setCcode_text(isChecked);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onClick(View v) {
        Fragment fragment = getTargetFragment();
        if (fragment != null)
            sent = true;
        dismiss();
        if (fragment != null) {
            EditText et = chkNumber.isChecked() ? etCCodeText : etCCodeNum;
            Intent i = new Intent();
            i.putExtra(Names.ID, id);
            i.putExtra(Names.VALUE, et.getText().toString());
            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, i);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Fragment fragment = getTargetFragment();
        if ((fragment != null) && !sent)
            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
    }

}
