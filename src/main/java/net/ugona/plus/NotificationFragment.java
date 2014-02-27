package net.ugona.plus;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class NotificationFragment extends SettingsFragment {

    SharedPreferences preferences;

    class SoundItem extends Item {

        SoundItem(int name, int type, String init_uri, String item_key) {
            super(name, R.string.def);
            type_ringtone = type;
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
            startActivityForResult(intent, type_ringtone);
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

        int type_ringtone;
        Uri uri;
        String key;
    }

    class CheckBoxItem extends CheckItem {
        CheckBoxItem(int name, String item_key, boolean def_value) {
            super(name);
            key = item_key;
            setValue(preferences.getBoolean(key + car_id, def_value) ? "1" : "");
        }

        @Override
        void click() {
            SharedPreferences.Editor ed = preferences.edit();
            ed.putBoolean(key + car_id, !getValue().equals(""));
            ed.commit();
            Intent intent = new Intent(FetchService.ACTION_UPDATE_FORCE);
            intent.putExtra(Names.ID, car_id);
            getActivity().sendBroadcast(intent);
        }

        String key;
    }

    class ListItem extends SpinnerItem {
        ListItem(int name, int values, int entries, String item_key, String def_value) {
            super(name, values, entries);
            key = item_key;
            setValue(preferences.getString(key + car_id, def_value));
        }

        @Override
        void click() {
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(key + car_id, getValue());
            ed.commit();
        }

        String key;
    }

    class ListIntItem extends SpinnerItem {
        ListIntItem(int name, int values, int entries, String item_key, int def_value) {
            super(name, values, entries);
            key = item_key;
            try {
                setValue(preferences.getInt(key + car_id, def_value) + "");
            } catch (Exception ex) {
                setValue(def_value + "");
            }
        }

        @Override
        void click() {
            SharedPreferences.Editor ed = preferences.edit();
            ed.putInt(key + car_id, Integer.parseInt(getValue()));
            ed.commit();
        }

        String key;
    }


    SoundItem alarm_sound;
    SoundItem notify_sound;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        alarm_sound = new SoundItem(R.string.alarm_sound, RingtoneManager.TYPE_RINGTONE, Preferences.getAlarm(preferences, car_id), Names.ALARM);
        notify_sound = new SoundItem(R.string.notify_sound, RingtoneManager.TYPE_NOTIFICATION, Preferences.getNotify(preferences, car_id), Names.NOTIFY);

        items.add(alarm_sound);
        items.add(new Item(R.string.alarm_test, R.string.alarm_test_sum) {
            @Override
            void click() {
                Intent intent = new Intent(getActivity(), Alarm.class);
                intent.putExtra(Names.ID, car_id);
                intent.putExtra(Names.ALARM, getString(R.string.alarm_test));
                startActivity(intent);
            }
        });
        items.add(notify_sound);
        items.add(new ListItem(R.string.guard_notify, R.array.guard_values, R.array.guard_entries, Names.GUARD_NOTIFY, ""));
        items.add(new CheckBoxItem(R.string.show_balance, Names.SHOW_BALANCE, true));
        items.add(new ListIntItem(R.string.balance_notification, R.array.balance_limit, R.array.balance_values, Names.LIMIT, 50));
        items.add(new CheckBoxItem(R.string.show_photo, Names.SHOW_PHOTO, false));

        update();
        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
        if (requestCode == RingtoneManager.TYPE_RINGTONE)
            alarm_sound.setData(data);
        if (requestCode == RingtoneManager.TYPE_NOTIFICATION)
            notify_sound.setData(data);
    }
}
