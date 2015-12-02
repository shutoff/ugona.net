package net.ugona.plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.io.Serializable;
import java.util.Vector;

public class AuthFragment extends MainFragment {

    static final int DO_AUTH = 1;
    static final int DO_PHONE = 2;
    static final int DO_NAME = 3;

    ListView vList;
    Vector<Item> items;

    BroadcastReceiver br;

    @Override
    int layout() {
        return R.layout.settings;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        vList = (ListView) v.findViewById(R.id.list);

        fill();

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BaseAdapter adapter = (BaseAdapter) vList.getAdapter();
                adapter.notifyDataSetChanged();
            }
        };
        IntentFilter intFilter = new IntentFilter(Names.COMMANDS);
        getActivity().registerReceiver(br, intFilter);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(br);
    }

    void fill() {
        final CarConfig config = CarConfig.get(getActivity(), id());
        items = new Vector<>();
        items.add(new Item(getString(R.string.name), config.getName(), new Runnable() {
            @Override
            public void run() {
                InputText inputText = new InputText();
                Bundle args = new Bundle();
                args.putString(Names.TITLE, getString(R.string.name));
                args.putString(Names.OK, config.getName());
                inputText.setArguments(args);
                inputText.setTargetFragment(AuthFragment.this, DO_NAME);
                inputText.show(getParentFragment().getFragmentManager(), "name");
            }
        }));
        items.add(new Item(getString(R.string.auth), config.getLogin(), new Runnable() {
            @Override
            public void run() {
                AuthDialog authDialog = new AuthDialog();
                Bundle args = new Bundle();
                args.putString(Names.ID, id());
                authDialog.setArguments(args);
                authDialog.setTargetFragment(AuthFragment.this, DO_AUTH);
                authDialog.show(getParentFragment().getFragmentManager(), "auth_dialog");
            }
        }));
        items.add(new Item(getString(R.string.phone_number), config.getPhone(), new Runnable() {
            @Override
            public void run() {
                CarConfig carConfig = CarConfig.get(getActivity(), id());
                InputPhoneDialog phoneDialog = new InputPhoneDialog();
                Bundle args = new Bundle();
                args.putString(Names.TITLE, getString(R.string.device_phone_number));
                args.putString(Names.PHONE, carConfig.getPhone());
                phoneDialog.setArguments(args);
                phoneDialog.setTargetFragment(AuthFragment.this, DO_PHONE);
                phoneDialog.show(getParentFragment().getFragmentManager(), "phone");
            }
        }));
        final CarState state = CarState.get(getActivity(), id());
        if (state.isSet_phone()) {
            items.add(new Item(getString(R.string.set_phone), "", new Runnable() {
                @Override
                public void run() {
                    String my_number = null;
                    try {
                        TelephonyManager manager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
                        my_number = manager.getLine1Number();
                    } catch (Exception ex) {
                        // ignore
                    }
                    InputPhoneDialog phoneDialog = new InputPhoneDialog();
                    Bundle args = new Bundle();
                    args.putString(Names.TITLE, getString(R.string.set_phone));
                    if (my_number != null)
                        args.putString(Names.PHONE, my_number);
                    args.putBoolean(Names.SET, true);
                    args.putString(Names.ID, id());
                    phoneDialog.setArguments(args);
                    phoneDialog.show(getParentFragment().getFragmentManager(), "phone");
                }
            }));
        }
        if (state.isSet_auth()) {
            items.add(new Item(getString(R.string.change_auth), "", new Runnable() {
                @Override
                public void run() {
                    ChangeAuthDialog authDialog = new ChangeAuthDialog();
                    Bundle args = new Bundle();
                    args.putString(Names.ID, id());
                    authDialog.setArguments(args);
                    authDialog.setTargetFragment(AuthFragment.this, DO_AUTH);
                    authDialog.show(getParentFragment().getFragmentManager(), "auth_dialog");
                }
            }));
        }

        int theme = -1;
        final String[] themes = config.getThemesNames();
        for (int i = 0; i < themes.length; i++) {
            if (themes[i].equals(config.getTheme())) {
                theme = i;
                break;
            }
        }
        if ((theme < 0) && (themes.length > 0)) {
            theme = 0;
            config.setTheme(themes[0]);
            HttpTask task = new HttpTask() {
                @Override
                void result(JsonObject res) throws ParseException {

                }

                @Override
                void error() {

                }
            };
            ThemeParams params = new ThemeParams();
            params.skey = config.getKey();
            params.theme = themes[0];
            task.execute("/set", params);
            Intent intent = new Intent(Names.UPDATED_THEME);
            intent.putExtra(Names.ID, id());
            getActivity().sendBroadcast(intent);
        }

        if (config.getThemesNames().length > 1) {
            items.add(new ValuesItem(getString(R.string.appearance), config.getThemesTitles(), theme) {
                @Override
                void onChanged() {
                    config.setTheme(themes[current]);
                    HttpTask task = new HttpTask() {
                        @Override
                        void result(JsonObject res) throws ParseException {

                        }

                        @Override
                        void error() {

                        }
                    };
                    ThemeParams params = new ThemeParams();
                    params.skey = config.getKey();
                    params.theme = themes[current];
                    task.execute("/set", params);
                }
            });
        }

        CarConfig.Setting[] settings = config.getSettings();
        final CarConfig.Command[] commands = config.getCmd();
        if (settings != null) {
            for (final CarConfig.Setting setting : settings) {
                if ((setting.id.length() < 5) || !setting.id.substring(0, 5).equals("auth_"))
                    continue;
                items.add(new Item(setting.name, setting.text, new Runnable() {
                    @Override
                    public void run() {
                        if (setting.values != null) {
                            Bundle args = new Bundle();
                            args.putString(Names.ID, id());
                            args.putString(Names.TITLE, setting.id);
                            SetSettingsFragment fragment = new SetSettingsFragment();
                            fragment.setArguments(args);
                            fragment.show(getActivity().getSupportFragmentManager(), "set");
                            return;
                        }
                        if (setting.cmd == null)
                            return;
                        if (commands != null) {
                            for (int cmd : setting.cmd) {
                                for (CarConfig.Command c : commands) {
                                    if (c.id != cmd)
                                        continue;
                                    if (Commands.isProcessed(id(), c)) {
                                        Commands.cancel(getActivity(), id(), c);
                                        return;
                                    }
                                }
                            }
                        }
                        if (setting.cmd.length == 1) {
                            SendCommandFragment fragment = new SendCommandFragment();
                            Bundle args = new Bundle();
                            args.putString(Names.ID, id());
                            args.putInt(Names.COMMAND, setting.cmd[0]);
                            args.putBoolean(Names.NO_PROMPT, true);
                            fragment.setArguments(args);
                            fragment.show(getActivity().getSupportFragmentManager(), "send");
                            return;
                        }
                        Bundle args = new Bundle();
                        args.putString(Names.ID, id());
                        args.putString(Names.TITLE, setting.id);
                        SmsSettingsFragment fragment = new SmsSettingsFragment();
                        fragment.setArguments(args);
                        fragment.show(getActivity().getSupportFragmentManager(), "sms");
                    }
                }) {
                    @Override
                    boolean isProgress() {
                        if (commands == null)
                            return false;
                        if (setting.cmd == null)
                            return false;
                        for (int cmd : setting.cmd) {
                            for (CarConfig.Command c : commands) {
                                if (c.id != cmd)
                                    continue;
                                if (Commands.isProcessed(id(), c))
                                    return true;
                            }
                        }
                        return false;
                    }
                });
            }
        }

        if (!state.getVersion().equals(""))
            items.add(new Item(getString(R.string.version), state.getVersion(), null));
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
                return items.get(position).getView(v);
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
        if (requestCode == DO_NAME) {
            CarConfig carConfig = CarConfig.get(getActivity(), id());
            carConfig.setName(data.getStringExtra(Names.OK));
            Intent intent = new Intent(Names.CAR_CHANGED);
            getActivity().sendBroadcast(intent);
            fill();
        }
        if (requestCode == DO_PHONE) {
            CarConfig carConfig = CarConfig.get(getActivity(), id());
            carConfig.setPhone(data.getStringExtra(Names.PHONE));
            Intent intent = new Intent(Names.CAR_CHANGED);
            getActivity().sendBroadcast(intent);
            fill();
        }
        if (requestCode == DO_AUTH) {
            fill();
        }
        super.onActivityResult(requestCode, resultCode, data);
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

        boolean isProgress() {
            return false;
        }

        View getView(View v) {
            TextView tvTitle = (TextView) v.findViewById(R.id.title);
            TextView tvText = (TextView) v.findViewById(R.id.text);
            Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
            tvTitle.setText(title);
            spinner.setVisibility(View.GONE);
            tvText.setText(text);
            tvText.setVisibility(View.VISIBLE);
            View vProgress = v.findViewById(R.id.progress);
            vProgress.setVisibility(isProgress() ? View.VISIBLE : View.GONE);
            return v;
        }

    }

    static class ValuesItem extends Item implements AdapterView.OnItemSelectedListener {
        String[] values;
        int current;

        ValuesItem(String title, String[] values, int current) {
            super(title, null, null);
            this.values = values;
            this.current = current;
        }

        @Override
        View getView(View v) {
            TextView tvTitle = (TextView) v.findViewById(R.id.title);
            TextView tvText = (TextView) v.findViewById(R.id.text);
            Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
            tvTitle.setText(title);
            spinner.setVisibility(View.VISIBLE);
            tvText.setVisibility(View.GONE);
            View vProgress = v.findViewById(R.id.progress);
            vProgress.setVisibility(View.GONE);
            spinner.setAdapter(new ArrayAdapter(spinner) {
                @Override
                public int getCount() {
                    return values.length;
                }

                @Override
                public Object getItem(int position) {
                    return values[position];
                }
            });
            spinner.setOnItemSelectedListener(this);
            spinner.setSelection(current);
            return v;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (current == position)
                return;
            current = position;
            onChanged();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

        void onChanged() {

        }
    }

    static class ThemeParams implements Serializable {
        String skey;
        String theme;
    }
}
