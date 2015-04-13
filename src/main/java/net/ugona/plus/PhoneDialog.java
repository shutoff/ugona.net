package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Vector;

public class PhoneDialog extends DialogFragment {

    static final int DO_CONTACTS = 100;
    static final int DO_SELECT_PHONE = 101;

    String id;
    EditText etPhone;

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        id = args.getString(Names.ID);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, id);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.phonedialog, null);
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getActivity())
                .setTitle(R.string.device_phone_number)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(v);
        if (savedInstanceState != null)
            id = savedInstanceState.getString(Names.ID);
        etPhone = (EditText) v.findViewById(R.id.phone);
        final CarConfig config = CarConfig.get(getActivity(), id);
        if (savedInstanceState == null)
            etPhone.setText(config.getPhone());
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
        final View okButton = dialog.getActionButton(DialogAction.POSITIVE);
        etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                okButton.setEnabled(State.isValidPhoneNumber(s.toString()));
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String number = etPhone.getText().toString();
                if (!State.isDebug())
                    number = State.formatPhoneNumber(number);
                if (number == null)
                    return;
                final CarConfig config = CarConfig.get(getActivity(), id);
                config.setPhone(number);
                dismiss();
                Fragment fragment = getTargetFragment();
                if (fragment != null)
                    fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
            }
        });
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

}
