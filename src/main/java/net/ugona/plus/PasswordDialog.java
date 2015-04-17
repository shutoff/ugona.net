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

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

public class PasswordDialog extends DialogFragment
        implements View.OnClickListener, TextWatcher {

    EditText etPasswd;
    View btnOk;
    String password;
    String title;
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
        afterTextChanged(etPasswd.getText());
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        password = args.getString(Names.MESSAGE);
        title = args.getString(Names.TITLE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.MESSAGE, password);
        outState.putString(Names.TITLE, title);
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
}
