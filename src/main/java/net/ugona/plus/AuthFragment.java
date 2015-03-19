package net.ugona.plus;

import android.content.Context;
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

public class AuthFragment extends MainFragment implements DialogListener {

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
        CarConfig config = CarConfig.get(getActivity(), id());
        items = new Vector<>();
        items.add(new Item(R.string.auth, config.getLogin(), new Runnable() {
            @Override
            public void run() {
                AuthDialog authDialog = new AuthDialog();
                Bundle args = new Bundle();
                args.putString(Names.ID, id());
                authDialog.setArguments(args);
                authDialog.show(getFragmentManager(), "auth");
            }
        }));
        items.add(new Item(R.string.phone_number, config.getPhone(), new Runnable() {
            @Override
            public void run() {
                PhoneDialog phoneDialog = new PhoneDialog();
                Bundle args = new Bundle();
                args.putString(Names.ID, id());
                phoneDialog.setArguments(args);
                phoneDialog.show(getFragmentManager(), "phone");
            }
        }));
        CarState state = CarState.get(getActivity(), id());
        items.add(new Item(R.string.version, state.getVersion(), null));
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
    public void ok() {
        BaseAdapter adapter = (BaseAdapter) vList.getAdapter();
        adapter.notifyDataSetChanged();
    }

    static class Item {

        int title;
        String text;
        Runnable action;

        Item(int title, String text, Runnable action) {
            this.title = title;
            this.text = text;
            this.action = action;
        }
    }
}
