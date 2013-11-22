package net.ugona.plus;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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

    final String USERS = "net.ugona.plus.USERS";

    String[] phones;
    String car_id;

    ListView lvPhones;
    View vMsg;
    View vProgress;

    BroadcastReceiver br;

    Menu topSubMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phones);

        car_id = getIntent().getStringExtra(Names.ID);

        lvPhones = (ListView) findViewById(R.id.list);
        vMsg = findViewById(R.id.msg);
        vProgress = findViewById(R.id.progress);

        if (savedInstanceState != null)
            phones = savedInstanceState.getStringArray(Names.CAR_PHONE);
        if (phones == null) {
            if (!SmsMonitor.isProcessed(car_id, R.string.phones)) {
                SmsMonitor.sendSMS(this, car_id, new SmsMonitor.Sms(R.string.phones, "USERS?", "USERS? ") {
                    @Override
                    boolean process_answer(Context context, String car_id, String text) {
                        Intent i = new Intent(USERS);
                        i.putExtra(Names.CAR_PHONE, text);
                        i.putExtra(Names.ID, car_id);
                        context.sendBroadcast(i);
                        return true;
                    }
                });
            }
            showProgress();
        } else {
            showList();
        }

        lvPhones.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                if (phones == null)
                    return 0;
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
                TextView tvNumber = (TextView) v.findViewById(R.id.number);
                tvNumber.setText(PhoneNumberUtils.formatNumber(number));

                ImageView ivPhoto = (ImageView) v.findViewById(R.id.photo);
                ivPhoto.setImageResource(R.drawable.unknown_contact);

                Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
                ContentResolver contentResolver = getContentResolver();
                Cursor contactLookup = contentResolver.query(uri, new String[]{BaseColumns._ID,
                        ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

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
                        if (cursor != null) {
                            try {
                                if (cursor.moveToFirst()) {
                                    byte[] data = cursor.getBlob(0);
                                    if (data != null) {
                                        Bitmap photo = BitmapFactory.decodeStream(new ByteArrayInputStream(data));
                                        ivPhoto = (ImageView) v.findViewById(R.id.photo);
                                        ivPhoto.setImageBitmap(photo);
                                    }
                                }
                            } finally {
                                cursor.close();
                            }
                        }
                    }
                } finally {
                    if (contactLookup != null) {
                        contactLookup.close();
                    }
                }

                return v;
            }
        });

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(USERS)) {
                    phones = intent.getStringExtra(Names.CAR_PHONE).split(",");
                    BaseAdapter adapter = (BaseAdapter) lvPhones.getAdapter();
                    adapter.notifyDataSetChanged();
                    showList();
                }
            }
        };
        IntentFilter filter = new IntentFilter(USERS);
        registerReceiver(br, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(br);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (phones != null)
            outState.putStringArray(Names.CAR_PHONE, phones);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        topSubMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.phones, menu);
        if ((phones == null) || (phones.length >= 5)) {
            menu.removeItem(R.id.add);
        } else {
            MenuItem item = menu.findItem(R.id.add);
            item.setEnabled(!SmsMonitor.isProcessed(car_id, R.string.add_phone));
        }
        if ((phones == null) || (phones.length < 2))
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
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.init_phone)
                        .setMessage(R.string.erase_phones)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Actions.init_phone(Phones.this, car_id);
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .create();
                dialog.show();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == ADD_PHONE) && (resultCode == RESULT_OK) && (phones != null)) {
            String number = data.getStringExtra(Names.CAR_PHONE).replaceAll("[^0-9+]+", "");
            String result = null;
            for (String phone : phones) {
                if (result == null) {
                    result = phone;
                } else {
                    result += "," + phone;
                }
            }
            if (result == null) {
                result = number;
            } else {
                result += "," + number;
            }

            final String r = result;
            SmsMonitor.Sms sms = new SmsMonitor.Sms(R.string.add_phone, "NEW USER " + number, "NEW USER OK") {
                @Override
                boolean process_answer(Context context, String car_id, String text) {
                    Intent i = new Intent(USERS);
                    i.putExtra(Names.CAR_PHONE, r);
                    i.putExtra(Names.ID, car_id);
                    context.sendBroadcast(i);
                    return true;
                }
            };

            showProgress();
            SmsMonitor.sendSMS(this, car_id, sms);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void showList() {
        vMsg.setVisibility(View.GONE);
        vProgress.setVisibility(View.GONE);
        lvPhones.setVisibility(View.VISIBLE);
        updateMenu();
    }

    void showProgress() {
        vMsg.setVisibility(View.VISIBLE);
        vProgress.setVisibility(View.VISIBLE);
        lvPhones.setVisibility(View.GONE);
        updateMenu();
    }

    void updateMenu() {
        if (topSubMenu == null)
            return;
        topSubMenu.clear();
        onCreateOptionsMenu(topSubMenu);
    }
}
