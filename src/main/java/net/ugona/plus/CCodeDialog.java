package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class CCodeDialog
        extends DialogFragment
        implements TextWatcher,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {

    final String TEXT = "text";

    String id;
    int inet;

    View btnOk;
    EditText etCCodeNum;
    EditText etCCodeText;
    CheckBox chkNumber;
    String init_string;
    String title;

    JsonObject data;

    boolean sent;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            setArgs(savedInstanceState);
            init_string = savedInstanceState.getString(TEXT);
        }
        if (title == null)
            title = getString(R.string.require_ccode);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        Dialog dialog = new AlertDialogWrapper.Builder(getActivity())
                .setTitle(title)
                .setView(inflater.inflate(R.layout.ccode_dialog, null))
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        return dialog;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    void setArgs(Bundle args) {
        id = args.getString(Names.ID);
        title = args.getString(Names.TITLE);
        String d = args.getString(Names.MESSAGE);
        if (d != null) {
            try {
                data = JsonValue.readFrom(d).asObject();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, id);
        EditText e = chkNumber.isChecked() ? etCCodeText : etCCodeNum;
        outState.putString(TEXT, e.getText().toString());
        outState.putString(Names.TITLE, title);
        if (data != null)
            outState.putString(Names.MESSAGE, data.toString());
    }

    @Override
    public void onStart() {
        super.onStart();
        MaterialDialog dialog = (MaterialDialog) getDialog();
        btnOk = dialog.getActionButton(DialogAction.POSITIVE);
        btnOk.setOnClickListener(this);
        etCCodeNum = (EditText) dialog.findViewById(R.id.ccode_num);
        etCCodeText = (EditText) dialog.findViewById(R.id.ccode_text);
        etCCodeNum.addTextChangedListener(this);
        etCCodeText.addTextChangedListener(this);
        chkNumber = (CheckBox) dialog.findViewById(R.id.number);
        chkNumber.setOnCheckedChangeListener(this);
        afterTextChanged(null);
        CarConfig config = CarConfig.get(getActivity(), id);
        chkNumber.setChecked(config.isCcode_text());
        if (init_string != null) {
            EditText e = chkNumber.isChecked() ? etCCodeText : etCCodeNum;
            e.setText(init_string);
            e.setSelection(init_string.length(), init_string.length());
        }
        if (data != null) {
            JsonValue v = data.get("title");
            if (v != null) {
                TextView tv = (TextView) dialog.findViewById(R.id.text);
                tv.setVisibility(View.VISIBLE);
                tv.setText(v.asString());
            }
            v = data.get("ccode");
            if (v != null) {
                etCCodeNum.setHint(v.asString());
                etCCodeText.setHint(v.asString());
            }
            v = data.get("ccode_new_prompt");
            if (v != null) {
                dialog.findViewById(R.id.new_block).setVisibility(View.VISIBLE);
                EditText et = (EditText) dialog.findViewById(R.id.ccode_new);
                et.setHint(v.asString());
                et.addTextChangedListener(this);
                et = (EditText) dialog.findViewById(R.id.ccode_new1);
                et.addTextChangedListener(this);
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        EditText et = chkNumber.isChecked() ? etCCodeText : etCCodeNum;
        String ccode = et.getText().toString();
        boolean okEnabled = ccode.length() > 0;
        if (data != null) {
            EditText et1 = (EditText) getDialog().findViewById(R.id.ccode_new);
            EditText et2 = (EditText) getDialog().findViewById(R.id.ccode_new1);
            boolean bConfirm = et1.getText().toString().equals(et2.getText().toString());
            View vErr = getDialog().findViewById(R.id.err_confirm);
            vErr.setVisibility(bConfirm ? View.INVISIBLE : View.VISIBLE);
            if (!bConfirm)
                okEnabled = false;
            if (et1.getText().toString().equals(""))
                okEnabled = false;
        }
        btnOk.setEnabled(okEnabled);
        if ((ccode.length() == 6) && (data == null))
            onClick(btnOk);
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onClick(View v) {
        Fragment fragment = getTargetFragment();
        if (fragment != null)
            sent = true;
        EditText et = chkNumber.isChecked() ? etCCodeText : etCCodeNum;
        Intent i = new Intent();
        i.putExtra(Names.ID, id);
        i.putExtra("ccode", et.getText().toString());
        if (data != null) {
            et = (EditText) getDialog().findViewById(R.id.ccode_new);
            i.putExtra("ccode_new", et.getText().toString());
        }
        dismiss();
        if (fragment != null)
            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, i);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Fragment fragment = getTargetFragment();
        if ((fragment != null) && !sent)
            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
    }

}
