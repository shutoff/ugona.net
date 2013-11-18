package net.ugona.plus;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.ByteArrayInputStream;

public class Phones extends ActionBarActivity {

    final static int ADD_PHONE = 3000;

    String[] phones;
    String car_id;

    ListView lvPhones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phones);
        phones = getIntent().getStringExtra(Names.SMS_TEXT).split(",");
        car_id = getIntent().getStringExtra(Names.ID);
        lvPhones = (ListView) findViewById(R.id.list);
        lvPhones.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                int len = phones.length;
                if (len >= 5)
                    return len + 1;
                if (len == 1)
                    return 2;
                return len + 2;
            }

            @Override
            public Object getItem(int position) {
                return phones[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getBaseContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.phone_item, null);
                }

                View vPhone = v.findViewById(R.id.phone);
                View vAdd = v.findViewById(R.id.add_phone);
                View vErase = v.findViewById(R.id.erase_phones);
                if ((phones.length < 5) && (position == phones.length)) {
                    vPhone.setVisibility(View.GONE);
                    vErase.setVisibility(View.GONE);
                    vAdd.setVisibility(View.VISIBLE);
                    return v;
                }
                if (position >= phones.length) {
                    vPhone.setVisibility(View.GONE);
                    vAdd.setVisibility(View.GONE);
                    vErase.setVisibility(View.VISIBLE);
                    return v;
                }

                vAdd.setVisibility(View.GONE);
                vErase.setVisibility(View.GONE);
                vPhone.setVisibility(View.VISIBLE);

                String number = phones[position];
                Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
                ContentResolver contentResolver = getContentResolver();
                Cursor contactLookup = contentResolver.query(uri, new String[]{BaseColumns._ID,
                        ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

                TextView tvNumber = (TextView) v.findViewById(R.id.number);
                tvNumber.setText(PhoneNumberUtils.formatNumber(number));
                TextView tvName = (TextView) v.findViewById(R.id.name);
                tvName.setText("");

                try {
                    if (contactLookup != null && contactLookup.getCount() > 0) {
                        contactLookup.moveToNext();

                        tvName.setText(contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)));
                        long contactId = contactLookup.getLong(contactLookup.getColumnIndex(BaseColumns._ID));
                        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
                        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                        Cursor cursor = getContentResolver().query(photoUri,
                                new String[]{ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
                        if (cursor == null) {
                            return null;
                        }
                        try {
                            if (cursor.moveToFirst()) {
                                byte[] data = cursor.getBlob(0);
                                if (data != null) {
                                    Bitmap photo = BitmapFactory.decodeStream(new ByteArrayInputStream(data));
                                    ImageView ivPhoto = (ImageView) v.findViewById(R.id.photo);
                                    ivPhoto.setImageBitmap(photo);
                                }
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                } catch (Exception ex) {
                    ImageView ivPhoto = (ImageView) v.findViewById(R.id.photo);
                    ivPhoto.setImageResource(R.drawable.unknown_contact);
                } finally {
                    if (contactLookup != null) {
                        contactLookup.close();
                    }
                }

                return v;
            }
        });
        lvPhones.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                View vAdd = view.findViewById(R.id.add_phone);
                if (vAdd.getVisibility() == View.VISIBLE) {
                    Intent i = new Intent(Phones.this, PhoneNumberDialog.class);
                    startActivityForResult(i, ADD_PHONE);
                    return;
                }
                View vErase = view.findViewById(R.id.erase_phones);
                if (vErase.getVisibility() == View.VISIBLE) {
                    Actions.requestCCode(Phones.this, R.string.clear, R.string.erase_phones, new Actions.Answer() {

                        @Override
                        void answer(final String ccode) {

                            Actions.send_sms(Phones.this, car_id, ccode, null, R.string.clear, new Actions.Answer() {
                                @Override
                                void answer(String body) {
                                    Actions.send_sms(Phones.this, car_id, ccode + " INIT", "Main user OK", R.string.clear, new Actions.Answer() {

                                        @Override
                                        void answer(String body) {
                                            finish();
                                        }
                                    });
                                }
                            });

                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == ADD_PHONE) && (resultCode == RESULT_OK)) {
            final String number = getIntent().getStringExtra(Names.CAR_PHONE);
            Actions.send_sms(this, car_id, "NEW USER " + number, "NEW USER OK", R.string.add_phone, new Actions.Answer() {
                @Override
                void answer(String body) {
                    String[] new_phones = new String[phones.length + 1];
                    System.arraycopy(phones, 0, new_phones, 0, phones.length);
                    new_phones[phones.length] = number;
                    phones = new_phones;
                    BaseAdapter adapter = (BaseAdapter) lvPhones.getAdapter();
                    adapter.notifyDataSetChanged();
                }
            });
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
