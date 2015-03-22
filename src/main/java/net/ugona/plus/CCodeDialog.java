package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

public class CCodeDialog
        extends DialogFragment
        implements TextWatcher,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {

    String id;
    int inet;

    Button btnOk;
    EditText etCCodeNum;
    EditText etCCodeText;
    CheckBox chkNumber;

    boolean sent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
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
        if (isChecked) {
            etCCodeNum.setVisibility(View.GONE);
            etCCodeText.setVisibility(View.VISIBLE);
            etCCodeText.setText("");
            etCCodeText.requestFocus();
        } else {
            etCCodeText.setVisibility(View.GONE);
            etCCodeNum.setVisibility(View.VISIBLE);
            etCCodeNum.setText("");
            etCCodeNum.requestFocus();
        }
        CarConfig config = CarConfig.get(getActivity(), id);
        config.setCcode_text(isChecked);
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
