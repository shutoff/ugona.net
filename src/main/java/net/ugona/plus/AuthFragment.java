package net.ugona.plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
                PhoneDialog phoneDialog = new PhoneDialog();
                Bundle args = new Bundle();
                args.putString(Names.ID, id());
                phoneDialog.setArguments(args);
                phoneDialog.setTargetFragment(AuthFragment.this, DO_PHONE);
                phoneDialog.show(getParentFragment().getFragmentManager(), "phone");
            }
        }));

        CarConfig.Setting[] settings = config.getSettings();
        final CarConfig.Command[] commands = config.getCmd();
        if (settings != null) {
            for (final CarConfig.Setting setting : settings) {
                if ((setting.id.length() < 5) || !setting.id.substring(0, 5).equals("auth_"))
                    continue;
                items.add(new Item(setting.name, setting.text, new Runnable() {
                    @Override
                    public void run() {
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

        CarState state = CarState.get(getActivity(), id());
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
                Item item = items.get(position);
                TextView tvTitle = (TextView) v.findViewById(R.id.title);
                TextView tvText = (TextView) v.findViewById(R.id.text);
                tvTitle.setText(item.title);
                tvText.setText(item.text);
                View vProgress = v.findViewById(R.id.progress);
                vProgress.setVisibility(item.isProgress() ? View.VISIBLE : View.GONE);
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
        if (requestCode == DO_NAME) {
            CarConfig carConfig = CarConfig.get(getActivity(), id());
            carConfig.setName(data.getStringExtra(Names.OK));
            Intent intent = new Intent(Names.CAR_CHANGED);
            getActivity().sendBroadcast(intent);
        }
        fill();
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

    }
}
