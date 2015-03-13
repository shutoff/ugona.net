package net.ugona.plus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.haibison.android.lockpattern.LockPatternActivity;

public class SetPassword extends MainFragment {

    static final int REQUEST_CHECK_PATTERN = 101;
    static final int REQUEST_PATTERN = 102;

    AppConfig config;

    @Override
    int layout() {
        return R.layout.setpassword;
    }

    @Override
    String getTitle() {
        return getString(R.string.password_set);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        config = AppConfig.get(getActivity());
        if (!config.getPattern().equals("")) {
            Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                    getActivity(), LockPatternActivity.class);
            intent.putExtra(LockPatternActivity.EXTRA_PATTERN, config.getPattern().toCharArray());
            startActivityForResult(intent, REQUEST_CHECK_PATTERN);
        }

        View btnGraph = v.findViewById(R.id.graphic);
        final View btnSave = v.findViewById(R.id.set);

        final EditText etPassword = (EditText) v.findViewById(R.id.old_password);

        final EditText etPass1 = (EditText) v.findViewById(R.id.password);
        final EditText etPass2 = (EditText) v.findViewById(R.id.password1);
        final View tvConfrim = v.findViewById(R.id.invalid_confirm);
        final View tvPassword = v.findViewById(R.id.invalid_password);

        if (config.getPassword().equals("")) {
            etPassword.setVisibility(View.GONE);
        } else {
            etPassword.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    tvPassword.setVisibility(View.INVISIBLE);
                }
            });
        }

        btnGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!config.getPassword().equals("") && !config.getPassword().equals(etPassword.getText().toString())) {
                    tvPassword.setVisibility(View.VISIBLE);
                    etPassword.requestFocus();
                    return;
                }
                Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null,
                        getActivity(), LockPatternActivity.class);
                startActivityForResult(intent, REQUEST_PATTERN);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!config.getPassword().equals("") && !config.getPassword().equals(etPassword.getText().toString())) {
                    tvPassword.setVisibility(View.VISIBLE);
                    etPassword.requestFocus();
                    return;
                }
                if (!etPass1.getText().toString().equals(etPass2.getText().toString()))
                    return;
                config.setPassword(etPass1.getText().toString());
                config.setPattern("");
                getActivity().onBackPressed();
            }
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (etPass1.getText().toString().equals(etPass2.getText().toString())) {
                    tvConfrim.setVisibility(View.INVISIBLE);
                    btnSave.setEnabled(true);
                } else {
                    tvConfrim.setVisibility(View.VISIBLE);
                    btnSave.setEnabled(false);
                }
            }
        };
        etPass1.addTextChangedListener(watcher);
        etPass2.addTextChangedListener(watcher);

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHECK_PATTERN) {
            if (resultCode != Activity.RESULT_OK)
                getActivity().onBackPressed();
        }
        if (requestCode == REQUEST_PATTERN) {
            if (resultCode == Activity.RESULT_OK) {
                char[] pattern = data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN);
                config.setPattern(String.copyValueOf(pattern));
                config.setPassword("");
                getActivity().onBackPressed();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
