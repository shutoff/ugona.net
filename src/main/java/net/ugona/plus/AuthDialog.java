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

import java.io.Serializable;
import java.util.Locale;

public class AuthDialog extends Activity {

    String car_id;
    CarConfig config;

    int max_count;
    int bad_count;

    AlertDialog dialog;

    EditText etLogin;
    EditText etPass;
    Button btnOk;
    Button btnDemo;
    TextView tvError;
    View vProgress;
    TextWatcher watcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        car_id = AppConfig.get(this).getId(getIntent().getStringExtra(Names.ID));
        config = CarConfig.get(this, car_id);

        max_count = 8;
        boolean canDemo = config.getAuth().equals("");
        if (canDemo) {
            AppConfig appConfig = AppConfig.get(this);
            if (!appConfig.getIds().equals(""))
                canDemo = false;
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.auth)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(inflater.inflate(R.layout.apikeydialog, null));

        if (canDemo)
            builder = builder.setNeutralButton(R.string.demo, null);

        dialog = builder.create();
        dialog.show();

        etLogin = (EditText) dialog.findViewById(R.id.login);
        etPass = (EditText) dialog.findViewById(R.id.passwd);
        btnOk = dialog.getButton(Dialog.BUTTON_POSITIVE);
        btnDemo = dialog.getButton(Dialog.BUTTON_NEUTRAL);
        tvError = (TextView) dialog.findViewById(R.id.error);
        vProgress = dialog.findViewById(R.id.progress);

        btnOk.setEnabled(false);
        String login = config.getLogin();
        if (!login.equals("demo"))
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

        watcher = new TextWatcher() {
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
                doLogin(etLogin.getText().toString(), etPass.getText().toString());
            }
        });
        if (btnDemo != null) {
            btnDemo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doLogin("demo", "demo");
                }
            });
        }
    }

    void doLogin(final String login, String password) {
        btnOk.setEnabled(false);
        btnDemo.setEnabled(false);
        tvError.setVisibility(View.GONE);
        vProgress.setVisibility(View.VISIBLE);
        final HttpTask task = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                if (dialog == null)
                    return;
                Config.update(config, res);
                CarState state = CarState.get(AuthDialog.this, car_id);
                if (CarState.update(state, res.get("state").asObject())) {
                    Intent intent = new Intent(Names.UPDATED);
                    intent.putExtra(Names.ID, car_id);
                    sendBroadcast(intent);
                }
                JsonObject caps = res.get("caps").asObject();
                CarState.update(state, caps.get("caps").asObject());
                if (CarState.update(config, caps)) {
                    Intent intent = new Intent(Names.CONFIG_CHANGED);
                    intent.putExtra(Names.ID, car_id);
                    sendBroadcast(intent);
                }
                CarState.update(config, res);
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
                btnDemo.setEnabled(true);
                watcher.afterTextChanged(null);
            }
        };
        AuthParam authParam = new AuthParam();
        authParam.login = login;
        authParam.password = password;
        authParam.lang = Locale.getDefault().getLanguage();
        task.execute("/key", authParam);
    }

    static class AuthParam implements Serializable {
        String login;
        String password;
        String lang;
    }
}
