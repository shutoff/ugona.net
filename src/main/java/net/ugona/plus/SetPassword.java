package net.ugona.plus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.haibison.android.lockpattern.LockPatternActivity;

public class SetPassword extends ActionBarActivity {

    static final int REQUEST_CHECK_PATTERN = 1;
    static final int REQUEST_PATTERN = 2;

    AppConfig config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setpassword);

        try {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception ex) {
            // ignore
        }

        config = AppConfig.get(this);
        if (!config.pattern.equals("")) {
            Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                    this, LockPatternActivity.class);
            intent.putExtra(LockPatternActivity.EXTRA_PATTERN, config.pattern.toCharArray());
            startActivityForResult(intent, REQUEST_CHECK_PATTERN);
        }

        View btnGraph = findViewById(R.id.graphic);
        final View btnSave = findViewById(R.id.set);

        final EditText etPassword = (EditText) findViewById(R.id.old_password);

        final EditText etPass1 = (EditText) findViewById(R.id.password);
        final EditText etPass2 = (EditText) findViewById(R.id.password1);
        final View tvConfrim = findViewById(R.id.invalid_confirm);
        final View tvPassword = findViewById(R.id.invalid_password);

        if (config.password.equals("")) {
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
                if (!config.password.equals("") && !config.password.equals(etPassword.getText().toString())) {
                    tvPassword.setVisibility(View.VISIBLE);
                    etPassword.requestFocus();
                    return;
                }
                Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null,
                        SetPassword.this, LockPatternActivity.class);
                startActivityForResult(intent, REQUEST_PATTERN);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!config.password.equals("") && !config.password.equals(etPassword.getText().toString())) {
                    tvPassword.setVisibility(View.VISIBLE);
                    etPassword.requestFocus();
                    return;
                }
                if (!etPass1.getText().toString().equals(etPass2.getText().toString()))
                    return;
                config.password = etPass1.getText().toString();
                config.pattern = "";
                finish();
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHECK_PATTERN) {
            if (resultCode != RESULT_OK)
                finish();
        }
        if (requestCode == REQUEST_PATTERN) {
            if (resultCode == RESULT_OK) {
                char[] pattern = data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN);
                config.pattern = String.copyValueOf(pattern);
                config.password = "";
            }
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
