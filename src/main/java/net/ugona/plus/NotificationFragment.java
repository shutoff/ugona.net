package net.ugona.plus;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class NotificationFragment extends SettingsFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        items.add(new SoundItem(R.string.alarm_sound, RingtoneManager.TYPE_RINGTONE, Names.Car.ALARM));
        items.add(new Item(R.string.alarm_test, R.string.alarm_test_sum) {
            @Override
            void click() {
                Intent intent = new Intent(getActivity(), Alarm.class);
                intent.putExtra(Names.ID, car_id);
                intent.putExtra(Names.Car.ALARM, getString(R.string.alarm_test));
                startActivity(intent);
            }
        });
        items.add(new SoundItem(R.string.notify_sound, RingtoneManager.TYPE_NOTIFICATION, Names.Car.NOTIFY));
        items.add(new SoundItem(R.string.zone_in_sound, RingtoneManager.TYPE_NOTIFICATION, Names.Car.ZONE_IN_SOUND));
        items.add(new SoundItem(R.string.zone_out_sound, RingtoneManager.TYPE_NOTIFICATION, Names.Car.ZONE_OUT_SOUND));

        items.add(new ListItem(R.string.guard_notify, R.array.guard_values, R.array.guard_entries, Names.Car.GUARD_MODE, "", R.string.notify_msg));
        items.add(new CheckBoxItem(R.string.show_balance, Names.Car.SHOW_BALANCE, true));
        items.add(new ListIntItem(R.string.balance_notification, R.array.balance_limit, R.array.balance_values, Names.Car.LIMIT, 50));

        update();
        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
        SoundItem item = (SoundItem) items.get(requestCode);
        item.setData(data);
    }

    class SoundItem extends Item {

        int type_ringtone;
        Uri uri;
        String key;

        SoundItem(int name, int type, String item_key) {
            super(name, R.string.def);
            type_ringtone = type;
            String init_uri = Preferences.getSound(preferences, item_key, car_id);
            if (init_uri.equals("")) {
                uri = RingtoneManager.getDefaultUri(type_ringtone);
            } else {
                uri = Uri.parse(init_uri);
            }
            key = item_key;
            setTitle();
        }

        void setTitle() {
            Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
            if (ringtone == null) {
                uri = RingtoneManager.getDefaultUri(type_ringtone);
                ringtone = RingtoneManager.getRingtone(getActivity(), uri);
            }
            if (ringtone != null) {
                setValue(ringtone.getTitle(getActivity()));
            } else {
                setValue(getString(R.string.def));
            }
        }

        @Override
        void click() {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, type_ringtone);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            if (uri != null) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri);
            } else {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
            }
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i) == this) {
                    startActivityForResult(intent, i);
                    break;
                }
            }
        }

        void setData(Intent data) {
            Uri new_uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (new_uri != null) {
                uri = new_uri;
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(key + car_id, uri.toString());
                ed.commit();
                setTitle();
                update();
            }
        }
    }
}
