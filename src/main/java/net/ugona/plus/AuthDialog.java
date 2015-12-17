package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.io.Serializable;
import java.util.Locale;

public class AuthDialog extends DialogFragment implements TextWatcher {

    String car_id;
    CarConfig config;

    EditText etLogin;
    EditText etPass;
    View btnOk;
    TextView tvError;
    View vProgress;
    JsonArray data;

    View vAuthBlock;
    View vDevicesBlock;
    Spinner sDevices;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.auth_dialog, null);

        etLogin = (EditText) v.findViewById(R.id.login);
        etPass = (EditText) v.findViewById(R.id.passwd);
        tvError = (TextView) v.findViewById(R.id.error);
        vProgress = v.findViewById(R.id.progress);
        vAuthBlock = v.findViewById(R.id.auth_block);
        vDevicesBlock = v.findViewById(R.id.device_block);
        sDevices = (Spinner) v.findViewById(R.id.devices);

        config = CarConfig.get(getActivity(), car_id);

        String login = config.getLogin();
        if (!login.equals("demo"))
            etLogin.setText(config.getLogin());

        etLogin.addTextChangedListener(this);
        etPass.addTextChangedListener(this);

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
                doLogin(etLogin.getText().toString(), etPass.getText().toString());
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

    void doLogin(final String login, String password) {
        if (data != null) {
            setResult(data.get(sDevices.getSelectedItemPosition()).asObject(), login);
            return;
        }

        btnOk.setEnabled(false);
        tvError.setVisibility(View.GONE);
        vProgress.setVisibility(View.VISIBLE);
        final HttpTask task = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                if (res.get("data") != null) {
                    data = res.get("data").asArray();
                    vAuthBlock.setVisibility(View.GONE);
                    vDevicesBlock.setVisibility(View.VISIBLE);
                    sDevices.setAdapter(new ArrayAdapter(sDevices) {
                        @Override
                        public int getCount() {
                            return data.size();
                        }

                        @Override
                        public Object getItem(int position) {
                            return data.get(position).asObject().getString("name", "???");
                        }
                    });
                    vProgress.setVisibility(View.GONE);
                    btnOk.setEnabled(true);
                    for (int i = 0; i < data.size(); i++) {
                        if (config.getName().equals(data.get(i).asObject().getString("name", ""))) {
                            sDevices.setSelection(i);
                            break;
                        }
                    }
                    return;
                }
                setResult(res, login);
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
        AuthParam authParam = new AuthParam();
        authParam.login = login;
        authParam.password = password;
        authParam.lang = Locale.getDefault().getLanguage();
        task.execute("/key", authParam);
    }

    void setResult(JsonObject res, String login) {
        CarState state = CarState.get(getActivity(), car_id);
        if (!config.getLogin().equals(login)) {
            Config.clear(config);
            Config.clear(state);
        }
        Config.update(config, res);
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
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        boolean bProgress = (vProgress.getVisibility() == View.VISIBLE);
        if (btnOk != null)
            btnOk.setEnabled(!bProgress &&
                    !etLogin.getText().toString().equals("") &&
                    !etPass.getText().toString().equals(""));
    }

    static class AuthParam implements Serializable {
        String login;
        String password;
        String lang;
    }


}
