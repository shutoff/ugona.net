package net.ugona.plus;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.util.Vector;

public class PhonesFragment
        extends MainFragment
        implements AdapterView.OnItemClickListener {

    static final int DO_PHONE = 1;

    String car_id;
    String passwd;

    Vector<Phone> phones;

    ListView vList;

    @Override
    int layout() {
        return R.layout.settings;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        vList = (ListView) v.findViewById(R.id.list);

        vList.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return phones.size() + 1;
            }

            @Override
            public Object getItem(int position) {
                if (position < phones.size())
                    return phones.get(position);
                return null;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    v = inflater.inflate(R.layout.phone_item, null);
                }
                if (position < phones.size()) {
                    v.findViewById(R.id.phone_block).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.add).setVisibility(View.GONE);
                    Phone phone = phones.get(position);
                    TextView vNumber = (TextView) v.findViewById(R.id.number);
                    vNumber.setText(State.formatPhoneNumber(phone.number));
                    TextView vName = (TextView) v.findViewById(R.id.name);
                    if (phone.name != null) {
                        vName.setText(phone.name);
                        vName.setVisibility(View.VISIBLE);
                    } else {
                        vName.setVisibility(View.GONE);
                    }
                    ImageView iv = (ImageView) v.findViewById(R.id.photo);
                    if (phone.photo != null) {
                        iv.setImageBitmap(phone.photo);
                    } else {
                        iv.setImageBitmap(null);
                    }
                } else {
                    v.findViewById(R.id.phone_block).setVisibility(View.GONE);
                    v.findViewById(R.id.add).setVisibility(View.VISIBLE);
                }
                return v;
            }
        });
        vList.setOnItemClickListener(this);
        updateNames();
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.PASSWORD, passwd);
        outState.putString(Names.ID, car_id);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DO_PHONE) {
            final String phone = data.getStringExtra(Names.PHONE);
            onRefresh();
            CarConfig.Command c = new CarConfig.Command();
            c.sms = "|NEW USER OK";
            c.onAnswer = new Runnable() {
                @Override
                public void run() {
                    try {
                        refreshDone();
                        Phone p = new Phone();
                        p.number = phone;
                        phones.add(p);
                        updateNames();
                        BaseAdapter adapter = (BaseAdapter) vList.getAdapter();
                        adapter.notifyDataSetChanged();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
            String sms = "";
            if (passwd != null)
                sms = passwd + " ";
            sms += "NEW USER " + phone.replaceAll("[^0-9+]", "");
            if (Sms.send(getActivity(), car_id, c.id, sms)) {
                Commands.put(getActivity(), car_id, c, data.getStringExtra("pwd"));
            } else {
                refreshDone();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void setArgs(Bundle args) {
        String[] numbers = args.getString(Names.MESSAGE).split(",");
        phones = new Vector<>();
        for (String number : numbers) {
            Phone phone = new Phone();
            phone.number = number;
            phones.add(phone);
        }
        passwd = args.getString(Names.PASSWORD);
        car_id = args.getString(Names.ID);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position < phones.size())
            return;
        InputPhoneDialog dialog = new InputPhoneDialog();
        Bundle args = new Bundle();
        args.putString(Names.TITLE, getString(R.string.add_phone));
        dialog.setArguments(args);
        dialog.setTargetFragment(this, DO_PHONE);
        dialog.show(getActivity().getSupportFragmentManager(), "phone");
    }

    void updateNames() {
        final ContentResolver contentResolver = getActivity().getContentResolver();
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                for (Phone phone : phones) {
                    if (phone.name != null)
                        continue;
                    Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone.number));

                    Cursor contactLookup = contentResolver.query(uri, new String[]{BaseColumns._ID,
                            ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
                    try {
                        if (contactLookup != null && contactLookup.getCount() > 0) {
                            contactLookup.moveToNext();

                            phone.name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                            long contactId = contactLookup.getLong(contactLookup.getColumnIndex(BaseColumns._ID));
                            Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
                            Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                            Cursor cursor = contentResolver.query(photoUri,
                                    new String[]{ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
                            if (cursor != null) {
                                try {
                                    if (cursor.moveToFirst()) {
                                        byte[] data = cursor.getBlob(0);
                                        if (data != null) {
                                            Bitmap photo = BitmapFactory.decodeStream(new ByteArrayInputStream(data));
                                            phone.photo = photo;
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
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                try {
                    BaseAdapter adapter = (BaseAdapter) vList.getAdapter();
                    adapter.notifyDataSetChanged();
                } catch (Exception ex) {
                    // ignore
                }
            }
        };
        task.execute();
    }

    static class Phone {
        String number;
        String name;
        Bitmap photo;
    }

}
