package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Vector;

public class PhoneDialog extends DialogFragment implements SelectNumberDialog.Listener {

    static final int DO_CONTACTS = 100;
    String id;
    EditText etPhone;
    DialogListener listener;

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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
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
        AlertDialog dialog = (AlertDialog) getDialog();
        final Button okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
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
                String number = State.formatPhoneNumber(etPhone.getText().toString());
                if (number == null)
                    return;
                final CarConfig config = CarConfig.get(getActivity(), id);
                config.setPhone(State.formatPhoneNumber(number));
                dismiss();
                if (listener != null)
                    listener.ok();
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
            dialog.setListener(this);
            dialog.show(getActivity().getSupportFragmentManager(), "number");
            return;

        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void selectNumber(String number) {
        etPhone.setText(number);
    }

    void setListener(DialogListener listener) {
        this.listener = listener;
    }
}
