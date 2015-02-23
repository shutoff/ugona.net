package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

public class AuthDialog extends Activity {

    String car_id;
    CarConfig config;

    int max_count;
    int bad_count;

    AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        car_id = AppConfig.get(this).getId(getIntent().getStringExtra(Names.ID));
        config = CarConfig.get(this, car_id);

        max_count = 8;

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.auth)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(inflater.inflate(R.layout.apikeydialog, null));
        dialog = builder.create();
        dialog.show();

        final EditText etLogin = (EditText) dialog.findViewById(R.id.login);
        final EditText etPass = (EditText) dialog.findViewById(R.id.passwd);
        final Button btnOk = dialog.getButton(Dialog.BUTTON_POSITIVE);
        final TextView tvError = (TextView) dialog.findViewById(R.id.error);
        final View vProgress = dialog.findViewById(R.id.progress);
        btnOk.setEnabled(false);
        etLogin.setText(config.getLogin());

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialog = null;
                if ((max_count > 0) && (bad_count > max_count)) {
                    AlertDialog bad = new AlertDialog.Builder(AuthDialog.this)
                            .setTitle(R.string.auth_error)
                            .setMessage(R.string.auth_message)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.send_msg, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.setType("text/html");
                                    intent.putExtra(Intent.EXTRA_EMAIL, "info@ugona.net");
                                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.auth_error));
                                    startActivity(Intent.createChooser(intent, getString(R.string.send_msg)));
                                }
                            })
                            .create();
                    bad.show();
                    bad.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    });
                    return;
                }
                finish();
            }
        });

        final TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean bProgress = (vProgress.getVisibility() == View.VISIBLE);
                btnOk.setEnabled(!bProgress &&
                        !etLogin.getText().toString().equals("") &&
                        !etPass.getText().toString().equals(""));
            }
        };
        etLogin.addTextChangedListener(watcher);
        etPass.addTextChangedListener(watcher);

        final CheckBox chkPswd = (CheckBox) dialog.findViewById(R.id.show_password);
        final int initial_type = chkPswd.getInputType();
        chkPswd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int type = initial_type;
                if (isChecked) {
                    type |= InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
                    chkPswd.setVisibility(View.GONE);
                }
                etPass.setInputType(type);
                etPass.setSelection(etPass.getText().length());
            }
        });

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnOk.setEnabled(false);
                tvError.setVisibility(View.GONE);
                vProgress.setVisibility(View.VISIBLE);
                final String login = etLogin.getText().toString();
                final HttpTask task = new HttpTask() {
                    @Override
                    void result(JsonObject res) throws ParseException {
                        if (dialog == null)
                            return;
                        Config.update(config, res);
                        CarState state = CarState.get(AuthDialog.this, car_id);
                        CarState.update(state, res.get("state").asObject());
                        CarState.update(state, res.get("caps").asObject());
                        config.setLogin(login);
                        setResult(RESULT_OK);
                        dialog.dismiss();
                    }

                    @Override
                    void error() {
                        if (dialog == null)
                            return;
                        if (Names.AUTH_ERROR.equals(error_text)) {
                            bad_count++;
                            tvError.setText(R.string.auth_error);
                            etPass.setText("");
                            if ((max_count > 0) && (bad_count > max_count))
                                dialog.dismiss();
                        } else {
                            tvError.setText(getString(R.string.error) + "\n" + error_text);
                        }
                        tvError.setVisibility(View.VISIBLE);
                        vProgress.setVisibility(View.GONE);
                        watcher.afterTextChanged(null);
                    }
                };
                AuthParam authParam = new AuthParam();
                authParam.login = etLogin.getText().toString();
                authParam.password = etPass.getText().toString();
                task.execute("/key", authParam);
            }
        });
    }

    static class AuthParam {
        String login;
        String password;
    }
}
