package net.ugona.plus;

import android.app.Activity;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.Vector;

public class NotifyFragment
        extends MainFragment {

    static final int DO_LIMIT = 200;

    ListView vList;
    Vector<Item> items;
    Handler handler;

    @Override
    int layout() {
        return R.layout.settings;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        vList = (ListView) v.findViewById(R.id.list);
        handler = new Handler();
        fill();
        return v;
    }

    void fill() {
        items = new Vector<>();

        items.add(new SoundItem(R.string.alarm_sound, RingtoneManager.TYPE_RINGTONE, "alarm", null));

        items.add(new Item(getString(R.string.alarm_test), getString(R.string.alarm_test_sum), new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getActivity(), Alarm.class);
                intent.putExtra(Names.ID, id());
                intent.putExtra(Names.TITLE, getString(R.string.alarm_test));
                intent.putExtra(Names.ERROR, true);
                startActivity(intent);
            }
        }));

        items.add(new SoundItem(R.string.notify_sound, RingtoneManager.TYPE_NOTIFICATION, "notify", null));
        items.add(new SoundItem(R.string.notify_az_start, RingtoneManager.TYPE_NOTIFICATION, "azStart", "start"));
        items.add(new SoundItem(R.string.zone_in, RingtoneManager.TYPE_NOTIFICATION, "zoneIn", null));
        items.add(new SoundItem(R.string.zone_out, RingtoneManager.TYPE_NOTIFICATION, "zoneOut", null));

        CarState state = CarState.get(getActivity(), id());
        if (!state.getBalance().equals("")) {
            items.add(new CheckBoxItem(getString(R.string.show_balance), "showBalance"));
            items.add(new BalanceLimitItem(getString(R.string.balance_notification)));
        }

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
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    v = inflater.inflate(R.layout.auth_item, null);
                }
                Item item = items.get(position);
                item.setView(v);
                return v;
            }
        });
        vList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                vList.requestFocus();
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
        if (requestCode == DO_LIMIT) {
            int limit = 0;
            try {
                limit = Integer.parseInt(data.getStringExtra(Names.OK));
            } catch (Exception ex) {
                // ignore
            }
            CarConfig carConfig = CarConfig.get(getActivity(), id());
            carConfig.setBalance_limit(limit);
            fill();
            return;
        }
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

        void setView(View v) {
            v.findViewById(R.id.check).setVisibility(View.GONE);
            TextView tvTitle = (TextView) v.findViewById(R.id.title);
            TextView tvText = (TextView) v.findViewById(R.id.text);
            tvTitle.setText(title);
            tvText.setText(text);
            tvTitle.setVisibility(View.VISIBLE);
            tvText.setVisibility(View.VISIBLE);
        }
    }

    class CheckBoxItem extends Item {

        String name;

        CheckBoxItem(String title, String name) {
            super(title, "", null);
            this.name = name;
        }

        @Override
        void setView(View v) {
            final CheckBox checkBox = (CheckBox) v.findViewById(R.id.check);
            checkBox.setText(title);
            checkBox.setVisibility(View.VISIBLE);
            final CarConfig carConfig = CarConfig.get(getActivity(), id());
            try {
                Field field = carConfig.getClass().getDeclaredField(name);
                field.setAccessible(true);
                checkBox.setChecked(field.getBoolean(carConfig));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    try {
                        Field field = carConfig.getClass().getDeclaredField(name);
                        field.setAccessible(true);
                        if (field.getBoolean(carConfig) == isChecked)
                            return;
                        field.setBoolean(carConfig, isChecked);
                        carConfig.upd = true;
                        Intent intent = new Intent(Names.CONFIG_CHANGED);
                        intent.putExtra(Names.ID, id());
                        getActivity().sendBroadcast(intent);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            v.findViewById(R.id.title).setVisibility(View.GONE);
            v.findViewById(R.id.text).setVisibility(View.GONE);
        }

    }

    class BalanceLimitItem
            extends Item
            implements Runnable {

        BalanceLimitItem(String title) {
            super(title, "", null);
            setValue();
            action = this;
        }

        void setValue() {
            CarConfig config = CarConfig.get(getActivity(), id());
            int balance_limit = config.getBalance_limit();
            if (balance_limit == 0) {
                text = getString(R.string.balance_no_notify);
            } else {
                NumberFormat nf = NumberFormat.getCurrencyInstance();
                text = String.format(getString(R.string.balance_limit), nf.format(balance_limit));
            }
            BaseAdapter adapter = (BaseAdapter) vList.getAdapter();
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }

        @Override
        public void run() {
            InputText inputText = new InputText();
            Bundle args = new Bundle();
            args.putString(Names.TITLE, title);
            CarConfig config = CarConfig.get(getActivity(), id());
            int balance_limit = config.getBalance_limit();
            String limit = balance_limit + "";
            if (balance_limit == 0)
                limit = "";
            args.putString(Names.OK, limit);
            args.putInt(Names.FLAGS, InputType.TYPE_CLASS_NUMBER);
            inputText.setArgs(args);
            inputText.setTargetFragment(NotifyFragment.this, DO_LIMIT);
            inputText.show(getParentFragment().getFragmentManager(), "balance");
        }

    }

    class SoundItem extends Item implements Runnable {

        Uri uri;
        int type;
        String name;
        String def_sound;
        int vibro;

        SoundItem(int title, int type, String name, String def_sound) {
            super(getString(title), "", null);
            this.type = type;
            this.name = name;
            this.def_sound = def_sound;
            CarConfig carConfig = CarConfig.get(getActivity(), id());
            try {
                Field field = carConfig.getClass().getDeclaredField(name + "Sound");
                field.setAccessible(true);
                String sound = field.get(carConfig).toString();
                if (sound.equals("")) {
                    if (def_sound != null) {
                        uri = Uri.parse("android.resource://net.ugona.plus/raw/" + def_sound);
                    } else {
                        uri = RingtoneManager.getDefaultUri(type);
                    }
                } else {
                    uri = Uri.parse(sound);
                }
                field = carConfig.getClass().getDeclaredField(name + "Vibro");
                field.setAccessible(true);
                vibro = field.getInt(carConfig);
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
                    args.putInt(Names.VIBRO, vibro);
                    args.putString(Names.DEFAULT, def_sound);
                    picker.setArguments(args);
                    picker.setTargetFragment(NotifyFragment.this, i);
                    picker.show(getActivity().getSupportFragmentManager(), "ringtone");
                    break;
                }
            }
        }

        void setTitle() {
            if (def_sound != null) {
                if (uri.toString().equals("android.resource://net.ugona.plus/raw/" + def_sound)) {
                    this.text = getString(R.string.def);
                    return;
                }
                if (uri.toString().equals("-")) {
                    this.text = getString(R.string.no_sound);
                    return;
                }
            }

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
                    Field field = carConfig.getClass().getDeclaredField(name + "Sound");
                    field.setAccessible(true);
                    field.set(carConfig, sound);
                    field = carConfig.getClass().getDeclaredField(name + "Vibro");
                    field.setAccessible(true);
                    vibro = data.getIntExtra(Names.VIBRO, 0);
                    field.setInt(carConfig, vibro);
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
