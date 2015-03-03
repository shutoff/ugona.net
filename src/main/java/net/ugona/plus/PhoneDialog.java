package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Vector;

public class PhoneDialog extends Activity {

    final static int DO_CONTACTS = 1;
    AlertDialog dialog;
    String id;
    EditText etPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        id = AppConfig.get(this).getId(getIntent().getStringExtra(Names.ID));
        final CarConfig config = CarConfig.get(this, id);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.device_phone_number)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(inflater.inflate(R.layout.phonedialog, null));
        dialog = builder.create();
        dialog.show();

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });

        final Button okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        etPhone = (EditText) dialog.findViewById(R.id.phone);
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
        etPhone.setText(config.getPhone());

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String number = State.formatPhoneNumber(etPhone.getText().toString());
                if (number == null)
                    return;
                config.setPhone(State.formatPhoneNumber(number));
                setResult(RESULT_OK);
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.contacts).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                    startActivityForResult(intent, DO_CONTACTS);
                } catch (Exception ex) {
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((resultCode == DO_CONTACTS) && (resultCode == RESULT_OK)) {
            final Vector<PhoneWithType> allNumbers = new Vector<PhoneWithType>();
            Cursor cursor = null;
            try {
                Uri result = data.getData();
                String id = result.getLastPathSegment();
                cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", new String[]{id}, null);
                int phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA);
                int typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA2);

                if (cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        PhoneWithType phone = new PhoneWithType();
                        phone.number = cursor.getString(phoneIdx);
                        phone.type = cursor.getInt(typeIdx);
                        allNumbers.add(phone);
                        cursor.moveToNext();
                    }
                }
            } catch (Exception ex) {
                // ignore
            } finally {
                if (cursor != null)
                    cursor.close();
            }
            if (allNumbers.size() == 0) {
                Toast toast = Toast.makeText(this, R.string.no_phone, Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
            if (allNumbers.size() == 1) {
                etPhone.setText(allNumbers.get(0).number);
                return;
            }
            ListView list = new ListView(this);
            list.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return allNumbers.size();
                }

                @Override
                public Object getItem(int position) {
                    return allNumbers.get(position);
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = convertView;
                    if (v == null) {
                        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        v = inflater.inflate(R.layout.item_with_title, null);
                    }
                    TextView tvNumber = (TextView) v.findViewById(R.id.title);
                    tvNumber.setText(allNumbers.get(position).number);
                    TextView tvType = (TextView) v.findViewById(R.id.text);
                    String type = "";
                    switch (allNumbers.get(position).type) {
                        case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                            type = getString(R.string.phone_home);
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                            type = getString(R.string.phone_work);
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                            type = getString(R.string.phone_mobile);
                            break;
                    }
                    tvType.setText(type);
                    return v;
                }
            });
            final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.select_phone)
                    .setNegativeButton(R.string.cancel, null)
                    .setView(list)
                    .create();
            dialog.show();
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    etPhone.setText(allNumbers.get(position).number);
                    dialog.dismiss();
                }
            });
            return;

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    static class PhoneWithType {
        String number;
        int type;
    }
}
