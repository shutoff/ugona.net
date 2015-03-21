package net.ugona.plus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PasswordDialog extends DialogFragment
        implements View.OnClickListener, TextWatcher {

    Listener listener;
    EditText etPasswd;
    Button btnOk;
    String password;
    View vError;

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

    void setListener(Listener listener) {
        this.listener = listener;
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
        if (listener != null) {
            if (etPasswd.getText().toString().equals(password)) {
                listener.ok();
                dismiss();
                return;
            }
            etPasswd.setText("");
            vError.setVisibility(View.VISIBLE);
            return;
        }
        dismiss();
    }

    static interface Listener {
        void ok();
    }
}
