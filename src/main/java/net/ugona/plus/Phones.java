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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
    Menu topSubMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);
        phones = getIntent().getStringExtra(Names.SMS_TEXT).split(",");
        car_id = getIntent().getStringExtra(Names.ID);
        lvPhones = (ListView) findViewById(R.id.list);
        lvPhones.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return phones.length;
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        topSubMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.phones, menu);
        if (phones.length >= 5)
            menu.removeItem(R.id.add);
        if (phones.length < 2)
            menu.removeItem(R.id.erase);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add: {
                Intent i = new Intent(Phones.this, PhoneNumberDialog.class);
                startActivityForResult(i, ADD_PHONE);
                return true;
            }
            case R.id.erase: {
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
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == ADD_PHONE) && (resultCode == RESULT_OK)) {
            final String number = data.getStringExtra(Names.CAR_PHONE).replaceAll("[^0-9+]+", "");
            Actions.send_sms(this, car_id, "NEW USER " + number, "NEW USER OK", R.string.add_phone, new Actions.Answer() {
                @Override
                void answer(String body) {
                    String[] new_phones = new String[phones.length + 1];
                    System.arraycopy(phones, 0, new_phones, 0, phones.length);
                    new_phones[phones.length] = number;
                    phones = new_phones;
                    BaseAdapter adapter = (BaseAdapter) lvPhones.getAdapter();
                    adapter.notifyDataSetChanged();
                    updateMenu();
                }
            });
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void updateMenu() {
        if (topSubMenu == null)
            return;
        topSubMenu.clear();
        onCreateOptionsMenu(topSubMenu);
    }
}
