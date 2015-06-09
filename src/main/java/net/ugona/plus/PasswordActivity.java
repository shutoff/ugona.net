package net.ugona.plus;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

public class PasswordActivity extends Activity
        implements View.OnClickListener, TextWatcher, DialogInterface.OnDismissListener {

    EditText etPasswd;
    View btnOk;
    View vError;
    String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        LayoutInflater inflater = LayoutInflater.from(this);
        MaterialDialog dialog = (MaterialDialog) new AlertDialogWrapper.Builder(this)
                .setTitle(getString(R.string.password))
                .setView(inflater.inflate(R.layout.password_dialog, null))
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setIcon(R.drawable.bl_password)
                .create();
        dialog.show();
        dialog.setOnDismissListener(this);

        etPasswd = (EditText) dialog.findViewById(R.id.passwd);
        vError = dialog.findViewById(R.id.error);
        btnOk = dialog.getActionButton(DialogAction.POSITIVE);
        btnOk.setOnClickListener(this);
        etPasswd.addTextChangedListener(this);
        AppConfig appConfig = AppConfig.get(this);
        password = appConfig.getPassword();
        afterTextChanged(etPasswd.getText());
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
        if (password.equals(s.toString())) {
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        etPasswd.setText("");
        vError.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
