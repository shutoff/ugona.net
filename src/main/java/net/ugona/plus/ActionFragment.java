package net.ugona.plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Vector;

public class ActionFragment
        extends MainFragment
        implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {

    ListView vActions;
    Vector<CarConfig.Command> commands;
    boolean longTap;
    BroadcastReceiver br;

    @Override
    int layout() {
        return R.layout.actions;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        vActions = (ListView) super.onCreateView(inflater, container, savedInstanceState);

        commands = new Vector<>();
        CarConfig config = CarConfig.get(getActivity(), id());
        CarConfig.Command[] cmds = config.getCmd();
        if (cmds != null) {
            for (CarConfig.Command cmd : cmds) {
                if ((cmd.inet != 0) || State.hasTelephony(getActivity()))
                    commands.add(cmd);
            }
        }
        vActions.setDivider(null);
        vActions.setDividerHeight(0);
        vActions.setVisibility(View.VISIBLE);
        vActions.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return commands.size();
            }

            @Override
            public Object getItem(int position) {
                return commands.get(position);
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
                    v = inflater.inflate(R.layout.action_item, null);
                }
                ImageView vIcon = (ImageView) v.findViewById(R.id.icon);
                TextView tvName = (TextView) v.findViewById(R.id.name);
                CarConfig.Command cmd = commands.get(position);
                tvName.setText(cmd.name);
                int icon = getResources().getIdentifier("icon_" + cmd.icon, "drawable", getActivity().getPackageName());
                vIcon.setImageResource(icon);
                return v;
            }
        });
        vActions.setOnItemClickListener(this);
        vActions.setOnItemLongClickListener(this);

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null)
                    return;
                if (!id().equals(intent.getStringExtra(Names.ID)))
                    return;
                BaseAdapter adapter = (BaseAdapter) vActions.getAdapter();
                adapter.notifyDataSetChanged();
            }
        };
        IntentFilter intFilter = new IntentFilter(Names.COMMANDS);
        getActivity().registerReceiver(br, intFilter);

        return vActions;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(br);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SendCommandFragment fragment = new SendCommandFragment();
        CarConfig.Command cmd = commands.get(position);
        Bundle args = new Bundle();
        args.putString(Names.ID, id());
        args.putInt(Names.COMMAND, cmd.id);
        args.putBoolean(Names.ROUTE, longTap);
        fragment.setArguments(args);
        fragment.show(getFragmentManager(), "send");
        longTap = false;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(700);
        } catch (Exception ex) {
            // ignore
        }
        longTap = true;
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.action, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            CommandsSettingsDialog dialog = new CommandsSettingsDialog();
            Bundle args = new Bundle();
            args.putString(Names.ID, id());
            dialog.setArguments(args);
            dialog.show(getFragmentManager(), "settings");
        }
        return super.onOptionsItemSelected(item);
    }
}
