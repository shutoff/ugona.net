package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

public class PasswordDialog extends DialogFragment
        implements View.OnClickListener, TextWatcher {

    EditText etPasswd;
    Button btnOk;
    String password;
    View vError;
    boolean sent;

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
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.passwd)
                .setView(inflater.inflate(R.layout.password_dialog, null))
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        etPasswd = (EditText) dialog.findViewById(R.id.passwd);
        vError = dialog.findViewById(R.id.error);
        btnOk = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        btnOk.setOnClickListener(this);
        etPasswd.addTextChangedListener(this);
        afterTextChanged(etPasswd.getText());
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        password = args.getString(Names.MESSAGE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.MESSAGE, password);
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
        if (etPasswd.getText().toString().equals(password)) {
            Fragment fragment = getTargetFragment();
            if (fragment != null)
                sent = true;
            dismiss();
            if (fragment != null)
                fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
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
}
