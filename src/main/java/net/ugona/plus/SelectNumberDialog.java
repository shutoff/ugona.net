package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.util.Vector;

public class SelectNumberDialog extends DialogFragment {

    String id;

    static Vector<PhoneWithType> getPhones(Context context, String id) {
        final Vector<PhoneWithType> allNumbers = new Vector<PhoneWithType>();
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", new String[]{id}, null);
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
        return allNumbers;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (savedInstanceState != null)
            id = savedInstanceState.getString(Names.ID);
        final Vector<PhoneWithType> allNumbers = getPhones(getActivity(), id);

        ListView list = new ListView(getActivity());
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
                    LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dismiss();
                Fragment fragment = getTargetFragment();
                if (fragment != null) {
                    Intent data = new Intent();
                    data.putExtra(Names.VALUE, allNumbers.get(position).number);
                    fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
                }
            }
        });

        return new AlertDialogWrapper.Builder(getActivity())
                .setTitle(R.string.select_phone)
                .setNegativeButton(R.string.cancel, null)
                .setView(list)
                .create();
    }

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

    static class PhoneWithType {
        String number;
        int type;
    }
}
