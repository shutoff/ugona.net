package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
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
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.io.Serializable;
import java.util.Locale;

public class ChangeAuthDialog extends DialogFragment implements TextWatcher {

    String car_id;
    CarConfig config;

    EditText etLogin;
    EditText etPass;
    EditText etConfirm;
    EditText etOld;
    View btnOk;
    TextView tvError;
    View vProgress;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.change_auth, null);

        etLogin = (EditText) v.findViewById(R.id.login);
        etPass = (EditText) v.findViewById(R.id.passwd);
        etConfirm = (EditText) v.findViewById(R.id.confirm);
        etOld = (EditText) v.findViewById(R.id.old);

        tvError = (TextView) v.findViewById(R.id.error);
        vProgress = v.findViewById(R.id.progress);

        config = CarConfig.get(getActivity(), car_id);

        String login = config.getLogin();
        if (!login.equals("demo"))
            etLogin.setText(config.getLogin());

        etLogin.addTextChangedListener(this);
        etPass.addTextChangedListener(this);
        etConfirm.addTextChangedListener(this);
        etOld.addTextChangedListener(this);

        return new AlertDialogWrapper.Builder(getActivity())
                .setTitle(R.string.change_auth)
                .setView(v)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setIcon(R.drawable.bl_password)
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
                doChangeLogin(etLogin.getText().toString(), etPass.getText().toString(), etOld.getText().toString());
            }
        });
        afterTextChanged(null);
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

    void doChangeLogin(final String login, String password, String old) {
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
                afterTextChanged(null);
            }
        };
        CarConfig carConfig = CarConfig.get(getActivity(), car_id);
        SetAuthParam authParam = new SetAuthParam();
        authParam.skey = carConfig.getKey();
        authParam.login = login;
        authParam.password = password;
        authParam.old = old;
        authParam.lang = Locale.getDefault().getLanguage();
        task.execute("/set_auth", authParam);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (vProgress.getVisibility() == View.VISIBLE) {
            btnOk.setEnabled(false);
            return;
        }
        if (btnOk == null)
            return;
        String login = etLogin.getText().toString();
        String pass = etPass.getText().toString();
        String confirm = etConfirm.getText().toString();
        String old = etOld.getText().toString();
        if (login.equals("") || pass.equals("") || confirm.equals("") || old.equals("")) {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText("");
            btnOk.setEnabled(false);
            return;
        }
        if (!pass.equals(confirm)) {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(R.string.invalid_confirm);
            btnOk.setEnabled(false);
            return;
        }
        tvError.setVisibility(View.VISIBLE);
        tvError.setText("");
        btnOk.setEnabled(true);
    }

    static class SetAuthParam implements Serializable {
        String skey;
        String login;
        String password;
        String old;
        String lang;
    }
}
