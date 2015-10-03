package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.io.Serializable;
import java.util.Vector;

public class InputPhoneDialog
        extends DialogFragment
        implements TextWatcher, CompoundButton.OnCheckedChangeListener {

    static final int DO_CONTACTS = 100;
    static final int DO_SELECT_PHONE = 101;

    String title;
    String phone;
    String id;

    boolean bSet;

    EditText etPhone;
    View vProgress;
    TextView tvError;

    EditText etCCodeNum;
    EditText etCCodeText;
    CheckBox chkNumber;
    View okButton;

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    void setArgs(Bundle args) {
        title = args.getString(Names.TITLE);
        phone = args.getString(Names.PHONE);
        id = args.getString(Names.ID);
        bSet = args.getBoolean(Names.SET);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.TITLE, title);
        outState.putString(Names.PHONE, etPhone.getText().toString());
        outState.putString(Names.ID, id);
        outState.putBoolean(Names.SET, bSet);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.phonedialog, null);
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getActivity())
                .setTitle(title)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(v);
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        etPhone = (EditText) v.findViewById(R.id.phone);
        vProgress = v.findViewById(R.id.progress);
        tvError = (TextView) v.findViewById(R.id.error);
        if (bSet) {
            tvError.setVisibility(View.VISIBLE);
            v.findViewById(R.id.ccode_block).setVisibility(View.VISIBLE);
            etCCodeNum = (EditText) v.findViewById(R.id.ccode_num);
            etCCodeText = (EditText) v.findViewById(R.id.ccode_text);
            etCCodeNum.addTextChangedListener(this);
            etCCodeText.addTextChangedListener(this);
            chkNumber = (CheckBox) v.findViewById(R.id.number);
            chkNumber.setOnCheckedChangeListener(this);
            afterTextChanged(null);
            CarConfig config = CarConfig.get(getActivity(), id);
            chkNumber.setChecked(config.isCcode_text());
            chkNumber.setVisibility(View.VISIBLE);
        }
        if (phone != null) {
            etPhone.setText(phone);
            if (savedInstanceState == null)
                etPhone.setSelection(0, phone.length());
        }
        v.findViewById(R.id.contacts).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                    startActivityForResult(intent, DO_CONTACTS);
                } catch (Exception ex) {
                }
            }
        });
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        MaterialDialog dialog = (MaterialDialog) getDialog();
        okButton = dialog.getActionButton(DialogAction.POSITIVE);
        etPhone.addTextChangedListener(this);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String number = etPhone.getText().toString();
                if (!State.isDebug())
                    number = State.formatPhoneNumber(number);
                if (number == null)
                    return;
                if (bSet) {
                    HttpTask task = new HttpTask() {
                        @Override
                        void result(JsonObject res) throws ParseException {
                            dismiss();
                        }

                        @Override
                        void error() {
                            vProgress.setVisibility(View.GONE);
                            tvError.setVisibility(View.VISIBLE);
                            tvError.setText(getActivity().getString(R.string.error) + "\n" + error_text);
                        }
                    };
                    tvError.setVisibility(View.GONE);
                    vProgress.setVisibility(View.VISIBLE);
                    SetPhoneParams params = new SetPhoneParams();
                    CarConfig config = CarConfig.get(getContext(), id);
                    params.skey = config.getKey();
                    params.phone = number;
                    params.ccode = getCCode();
                    task.execute("/set", params);
                    return;
                }
                dismiss();
                Fragment fragment = getTargetFragment();
                Intent data = new Intent();
                data.putExtra(Names.PHONE, number);
                data.putExtra(Names.ID, id);
                if (fragment != null)
                    fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
            }
        });
        afterTextChanged(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == DO_CONTACTS) && (resultCode == Activity.RESULT_OK)) {
            Uri result = data.getData();
            String id = result.getLastPathSegment();
            Vector<SelectNumberDialog.PhoneWithType> allNumbers = SelectNumberDialog.getPhones(getActivity(), id);
            if (allNumbers.size() == 0) {
                Toast toast = Toast.makeText(getActivity(), R.string.no_phone, Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
            if (allNumbers.size() == 1) {
                etPhone.setText(allNumbers.get(0).number);
                return;
            }
            SelectNumberDialog dialog = new SelectNumberDialog();
            Bundle args = new Bundle();
            args.putString(Names.ID, id);
            dialog.setArguments(args);
            dialog.setTargetFragment(this, DO_SELECT_PHONE);
            dialog.show(getActivity().getSupportFragmentManager(), "number");
            return;
        }
        if ((requestCode == DO_SELECT_PHONE) && (resultCode == Activity.RESULT_OK)) {
            etPhone.setText(data.getStringExtra(Names.VALUE));
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        tvError.setText("");
        boolean okEnabled = State.isValidPhoneNumber(etPhone.getText().toString());
        if (bSet && okEnabled)
            okEnabled = (getCCode().length() > 0);
        if (okButton != null)
            okButton.setEnabled(okEnabled);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        EditText e = null;
        if (isChecked) {
            etCCodeNum.setVisibility(View.GONE);
            e = etCCodeText;
        } else {
            etCCodeText.setVisibility(View.GONE);
            e = etCCodeNum;
        }
        e.setVisibility(View.VISIBLE);
        e.setText("");
        e.requestFocus();
        CarConfig config = CarConfig.get(getActivity(), id);
        config.setCcode_text(isChecked);
    }

    String getCCode() {
        EditText et = chkNumber.isChecked() ? etCCodeText : etCCodeNum;
        return et.getText().toString();
    }

    static class SetPhoneParams implements Serializable {
        String skey;
        String phone;
        String ccode;
    }

}
