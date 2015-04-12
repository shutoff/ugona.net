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
import android.widget.ListView;

public class ActionFragment
        extends MainFragment
        implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {

    ListView vActions;

    boolean longTap;
    BroadcastReceiver br;

    @Override
    int layout() {
        return R.layout.actions;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        vActions = (ListView) super.onCreateView(inflater, container, savedInstanceState);

        vActions.setDivider(null);
        vActions.setDividerHeight(0);
        vActions.setVisibility(View.VISIBLE);
        vActions.setAdapter(new ActionsAdapter(getActivity(), id()));
        vActions.setOnItemClickListener(this);
        vActions.setOnItemLongClickListener(this);

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null)
                    return;
                if (!id().equals(intent.getStringExtra(Names.ID)))
                    return;
                if (intent.getAction().equals(Names.CONFIG_CHANGED)) {
                    ActionsAdapter actionsAdapter = (ActionsAdapter) vActions.getAdapter();
                    actionsAdapter.fill(getActivity(), id());
                }
                BaseAdapter adapter = (BaseAdapter) vActions.getAdapter();
                adapter.notifyDataSetChanged();
            }
        };
        IntentFilter intFilter = new IntentFilter(Names.COMMANDS);
        intFilter.addAction(Names.CONFIG_CHANGED);
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
        ActionsAdapter actionsAdapter = (ActionsAdapter) vActions.getAdapter();
        actionsAdapter.itemClick(position, longTap);
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
            dialog.show(getActivity().getSupportFragmentManager(), "settings");
        }
        return super.onOptionsItemSelected(item);
    }

}
