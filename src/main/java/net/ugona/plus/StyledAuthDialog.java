package net.ugona.plus;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.io.Serializable;

public class StyledAuthDialog
        extends Dialog
        implements ViewTreeObserver.OnGlobalLayoutListener {

    EditText etLogin;
    EditText etPass;
    Button btnOk;
    Button btnDemo;
    TextView tvError;
    View vProgress;
    TextWatcher watcher;
    float pk;

    public StyledAuthDialog(Context context) {
        super(context, R.style.CustomDialogTheme);
        setContentView(R.layout.styled_auth_dialog);

        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        pk = metrics.densityDpi / 160f;

        View v = findViewById(R.id.root);
        v.getViewTreeObserver().addOnGlobalLayoutListener(this);

        etLogin = (EditText) findViewById(R.id.login);
        etPass = (EditText) findViewById(R.id.passwd);
        tvError = (TextView) findViewById(R.id.error);
        vProgress = findViewById(R.id.progress);
        btnOk = (Button) findViewById(R.id.ok);

        btnOk.setEnabled(false);
        CarConfig config = CarConfig.get(getContext(), "");
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
        etPass.setTransformationMethod(PasswordTransformationMethod.getInstance());

        final CheckBox chkPswd = (CheckBox) findViewById(R.id.show_password);
        chkPswd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                etPass.setTransformationMethod(isChecked ? null : PasswordTransformationMethod.getInstance());
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
                String ids = "";
                if (res.get("data") != null) {
                    JsonArray r = res.get("data").asArray();
                    for (int i = 0; i < r.size(); i++) {
                        String car_id = i + "";
                        if (i == 0) {
                            car_id = "";
                        } else {
                            ids += ";" + car_id;
                        }
                        addDevice(r.get(i).asObject(), car_id, login);
                    }
                } else {
                    addDevice(res, "", login);
                }
                AppConfig appConfig = AppConfig.get(getContext());
                appConfig.setIds(ids);
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
        authParam.lang = getContext().getResources().getConfiguration().locale.getLanguage();
        task.execute("/key", authParam);
    }

    void addDevice(JsonObject res, String car_id, String login) {
        CarConfig config = CarConfig.get(getContext(), car_id);
        Config.clear(config);
        Config.update(config, res);
        CarState state = CarState.get(getContext(), car_id);
        Config.clear(state);
        if (CarState.update(state, res.get("state").asObject()) != null) {
            Intent intent = new Intent(Names.UPDATED);
            intent.putExtra(Names.ID, "");
            getContext().sendBroadcast(intent);
        }
        JsonObject caps = res.get("caps").asObject();
        boolean changed = (CarState.update(state, caps.get("caps").asObject()) != null);
        changed |= (CarState.update(config, caps) != null);
        if (changed) {
            Intent intent = new Intent(Names.CONFIG_CHANGED);
            intent.putExtra(Names.ID, "");
            getContext().sendBroadcast(intent);
        }
        CarState.update(config, res);
        config.setLogin(login);
    }

    @Override
    public void onGlobalLayout() {
        Rect rect = new Rect();
        btnDemo.getLocalVisibleRect(rect);
        if (btnDemo.getHeight() < 20 * pk) {
            View vHeader = findViewById(R.id.header);
            if (vHeader.getVisibility() != View.GONE) {
                vHeader.setVisibility(View.GONE);
                View vMain = findViewById(R.id.main);
                vMain.setBackgroundResource(R.drawable.dialog_bg_bottom_top);
            }
        }
    }

    static class AuthParam implements Serializable {
        String login;
        String password;
        String lang;
    }


}
