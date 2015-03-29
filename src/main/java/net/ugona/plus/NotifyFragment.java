package net.ugona.plus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.Vector;

public class NotifyFragment extends MainFragment {

    ListView vList;
    Vector<Item> items;

    @Override
    int layout() {
        return R.layout.settings;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        vList = (ListView) v.findViewById(R.id.list);
        fill();
        return v;
    }

    void fill() {
        items = new Vector<>();

        items.add(new SoundItem(R.string.alarm_sound, RingtoneManager.TYPE_RINGTONE, "alarmSound"));

        items.add(new Item(getString(R.string.alarm_test), getString(R.string.alarm_test_sum), new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getActivity(), Alarm.class);
                intent.putExtra(Names.ID, id());
                intent.putExtra(Names.TITLE, getString(R.string.alarm_test));
                startActivity(intent);
            }
        }));

        items.add(new SoundItem(R.string.notify_sound, RingtoneManager.TYPE_NOTIFICATION, "notifySound"));

        vList.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return items.size();
            }

            @Override
            public Object getItem(int position) {
                return items.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getActivity()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.auth_item, null);
                }
                Item item = items.get(position);
                TextView tvTitle = (TextView) v.findViewById(R.id.title);
                TextView tvText = (TextView) v.findViewById(R.id.text);
                tvTitle.setText(item.title);
                tvText.setText(item.text);
                return v;
            }
        });
        vList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Runnable action = items.get(position).action;
                if (action != null)
                    action.run();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
        SoundItem item = (SoundItem) items.get(requestCode);
        item.setData(data);
    }

    static class Item {
        String title;
        String text;
        Runnable action;

        Item(String title, String text, Runnable action) {
            this.title = title;
            this.text = text;
            this.action = action;
        }
    }

    class SoundItem extends Item implements Runnable {

        Uri uri;
        int type;
        String name;

        SoundItem(int title, int type, String name) {
            super(getString(title), "", null);
            this.type = type;
            this.name = name;
            CarConfig carConfig = CarConfig.get(getActivity(), id());
            try {
                Field field = carConfig.getClass().getDeclaredField(name);
                field.setAccessible(true);
                String sound = field.get(carConfig).toString();
                if (sound.equals("")) {
                    uri = RingtoneManager.getDefaultUri(type);
                } else {
                    uri = Uri.parse(sound);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            setTitle();
            action = this;
        }

        @Override
        public void run() {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i) == this) {
                    RingTonePicker picker = new RingTonePicker();
                    Bundle args = new Bundle();
                    args.putString(Names.TITLE, title);
                    args.putInt(Names.TYPE, type);
                    if (uri != null)
                        args.putString(Names.SOUND, uri.toString());
                    picker.setArguments(args);
                    picker.setTargetFragment(NotifyFragment.this, i);
                    picker.show(getActivity().getSupportFragmentManager(), "ringtone");
                    break;
                }
            }
        }

        void setTitle() {
            Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
            if (ringtone == null) {
                uri = RingtoneManager.getDefaultUri(type);
                ringtone = RingtoneManager.getRingtone(getActivity(), uri);
            }
            if (ringtone != null) {
                this.text = ringtone.getTitle(getActivity());
            } else {
                this.text = getString(R.string.def);
            }
        }

        void setData(Intent data) {
            String sound = data.getStringExtra(Names.SOUND);
            if (sound != null) {
                try {
                    uri = Uri.parse(sound);
                    CarConfig carConfig = CarConfig.get(getActivity(), id());
                    Field field = carConfig.getClass().getDeclaredField(name);
                    field.setAccessible(true);
                    field.set(carConfig, sound);
                    carConfig.upd = true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                setTitle();
                BaseAdapter adapter = (BaseAdapter) vList.getAdapter();
                adapter.notifyDataSetChanged();
            }
        }
    }
}
