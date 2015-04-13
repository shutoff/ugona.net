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
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.io.Serializable;
import java.util.Locale;

public class AuthDialog extends DialogFragment {

    String car_id;
    CarConfig config;

    EditText etLogin;
    EditText etPass;
    View btnOk;
    TextView tvError;
    View vProgress;
    TextWatcher watcher;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.auth_dialog, null);

        etLogin = (EditText) v.findViewById(R.id.login);
        etPass = (EditText) v.findViewById(R.id.passwd);
        tvError = (TextView) v.findViewById(R.id.error);
        vProgress = v.findViewById(R.id.progress);

        config = CarConfig.get(getActivity(), car_id);

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
                if (btnOk != null)
                    btnOk.setEnabled(!bProgress &&
                            !etLogin.getText().toString().equals("") &&
                            !etPass.getText().toString().equals(""));
            }
        };
        etLogin.addTextChangedListener(watcher);
        etPass.addTextChangedListener(watcher);

        final CheckBox chkPswd = (CheckBox) v.findViewById(R.id.show_password);
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

        return new AlertDialogWrapper.Builder(getActivity())
                .setTitle(R.string.auth)
                .setView(v)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        MaterialDialog dialog = (MaterialDialog) getDialog();
        btnOk = dialog.getActionButton(DialogAction.POSITIVE);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogin(etLogin.getText().toString(), etPass.getText().toString());
            }
        });
        watcher.afterTextChanged(null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        car_id = args.getString(Names.ID);
    }

    void doLogin(final String login, String password) {
        btnOk.setEnabled(false);
        tvError.setVisibility(View.GONE);
        vProgress.setVisibility(View.VISIBLE);
        final HttpTask task = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                Config.clear(config);
                Config.update(config, res);
                CarState state = CarState.get(getActivity(), car_id);
                Config.clear(state);
                if (CarState.update(state, res.get("state").asObject()) != null) {
                    Intent intent = new Intent(Names.UPDATED);
                    intent.putExtra(Names.ID, car_id);
                    getActivity().sendBroadcast(intent);
                }
                JsonObject caps = res.get("caps").asObject();
                boolean changed = CarState.update(state, caps.get("caps").asObject()) != null;
                changed |= (CarState.update(config, caps) != null);
                if (changed) {
                    Intent intent = new Intent(Names.CONFIG_CHANGED);
                    intent.putExtra(Names.ID, car_id);
                    getActivity().sendBroadcast(intent);
                }
                config.setLogin(login);
                dismiss();
                Fragment fragment = getTargetFragment();
                if (fragment != null) {
                    Intent data = new Intent();
                    data.putExtra(Names.ID, car_id);
                    fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
                }
            }

            @Override
            void error() {
                if (Names.AUTH_ERROR.equals(error_text)) {
                    tvError.setText(R.string.auth_error);
                    etPass.setText("");
                } else {
                    tvError.setText(getActivity().getString(R.string.error) + "\n" + error_text);
                }
                tvError.setVisibility(View.VISIBLE);
                vProgress.setVisibility(View.GONE);
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
