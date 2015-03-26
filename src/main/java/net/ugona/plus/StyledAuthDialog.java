package net.ugona.plus;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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

public class StyledAuthDialog extends Dialog {

    String car_id;
    CarConfig config;

    EditText etLogin;
    EditText etPass;
    Button btnOk;
    Button btnDemo;
    TextView tvError;
    View vProgress;
    TextWatcher watcher;

    public StyledAuthDialog(Context context, String car_id) {
        super(context, R.style.CustomDialogTheme);
        this.car_id = car_id;
        setContentView(R.layout.styled_auth_dialog);
        etLogin = (EditText) findViewById(R.id.login);
        etPass = (EditText) findViewById(R.id.passwd);
        tvError = (TextView) findViewById(R.id.error);
        vProgress = findViewById(R.id.progress);
        btnOk = (Button) findViewById(R.id.ok);

        config = CarConfig.get(context, car_id);
        btnOk.setEnabled(false);
        String login = config.getLogin();
        if (!login.equals("demo"))
            etLogin.setText(config.getLogin());

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

        final CheckBox chkPswd = (CheckBox) findViewById(R.id.show_password);
        final int initial_type = chkPswd.getInputType();
        chkPswd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int type = initial_type;
                if (isChecked) {
                    type |= InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
                } else {
                    type = initial_type;
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

        btnDemo = (Button) findViewById(R.id.demo);
        btnDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogin("demo", "demo");
            }
        });

        boolean canDemo = config.getAuth().equals("");
        if (canDemo) {
            AppConfig appConfig = AppConfig.get(context);
            if (!appConfig.getIds().equals(""))
                canDemo = false;
        }
        if (!canDemo)
            btnDemo.setVisibility(View.GONE);

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    void doLogin(final String login, String password) {
        btnOk.setEnabled(false);
        btnDemo.setEnabled(false);
        tvError.setVisibility(View.GONE);
        vProgress.setVisibility(View.VISIBLE);
        final HttpTask task = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                Config.clear(config);
                Config.update(config, res);
                CarState state = CarState.get(getContext(), car_id);
                Config.clear(state);
                if (CarState.update(state, res.get("state").asObject()) != null) {
                    Intent intent = new Intent(Names.UPDATED);
                    intent.putExtra(Names.ID, car_id);
                    getContext().sendBroadcast(intent);
                }
                JsonObject caps = res.get("caps").asObject();
                boolean changed = (CarState.update(state, caps.get("caps").asObject()) != null);
                changed |= (CarState.update(config, caps) != null);
                if (changed) {
                    Intent intent = new Intent(Names.CONFIG_CHANGED);
                    intent.putExtra(Names.ID, car_id);
                    getContext().sendBroadcast(intent);
                }
                CarState.update(config, res);
                config.setLogin(login);
                dismiss();
            }

            @Override
            void error() {
                if (Names.AUTH_ERROR.equals(error_text)) {
                    tvError.setText(R.string.auth_error);
                    etPass.setText("");
                } else {
                    tvError.setText(getContext().getString(R.string.error) + "\n" + error_text);
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
