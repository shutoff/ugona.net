package net.ugona.plus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public class CarsFragment
        extends MainFragment
        implements AdapterView.OnItemClickListener,
        View.OnClickListener {

    static final int DO_AUTH = 1;
    static final int DO_DELETE = 2;
    static final int DO_POINTER = 3;

    ListView vList;

    Vector<String> ids;

    @Override
    int layout() {
        return R.layout.actions;
    }

    @Override
    String getTitle() {
        return getString(R.string.cars);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        vList = (ListView) super.onCreateView(inflater, container, savedInstanceState);
        AppConfig appConfig = AppConfig.get(getActivity());
        refresh();
        vList.setOnItemClickListener(this);
        return vList;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position >= ids.size()) {
            String new_id;
            for (int i = 1; ; i++) {
                new_id = i + "";
                boolean found = false;
                for (String car_id : ids) {
                    if (car_id.equals(new_id)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    break;
            }
            CarConfig carConfig = CarConfig.get(getActivity(), new_id);
            Config.clear(carConfig);
            AuthDialog authDialog = new AuthDialog();
            Bundle args = new Bundle();
            args.putString(Names.ID, new_id);
            authDialog.setArguments(args);
            authDialog.setTargetFragment(this, DO_AUTH);
            authDialog.show(getActivity().getSupportFragmentManager(), "auth");
            return;
        }
        String car_id = ids.get(position);
        if (car_id.indexOf("_") < 0) {
            MainActivity activity = (MainActivity) getActivity();
            activity.setCarId(car_id);
            activity.setFragment(new SettingsFragment());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == DO_AUTH) && (resultCode == Activity.RESULT_OK)) {
            String id = data.getStringExtra(Names.ID);
            AppConfig config = AppConfig.get(getActivity());
            config.setIds(config.getIds() + ";" + id);
            config.save(getActivity());
            refresh();
            CarState state = CarState.get(getActivity(), id);
            if (state.isPointer()) {
                PointerDialog dialog = new PointerDialog();
                Bundle args = new Bundle();
                args.putString(Names.ID, id);
                dialog.setArguments(args);
                dialog.setTargetFragment(this, DO_POINTER);
                dialog.show(getActivity().getSupportFragmentManager(), "pointer");
                return;
            }
            checkPhone(id);
            return;
        }
        if ((requestCode == DO_DELETE) && (resultCode == Activity.RESULT_OK)) {
            String del_id = data.getStringExtra(Names.ID);
            if (del_id.indexOf("_") < 0) {
                AppConfig config = AppConfig.get(getActivity());
                config.removeId(del_id);
                config.save(getActivity());
                refresh();
                if (id().equals(del_id)) {
                    MainActivity activity = (MainActivity) getActivity();
                    activity.setCarId(ids.get(0));
                }
                return;
            }
            String[] parts = del_id.split("_");
            CarConfig carConfig = CarConfig.get(getActivity(), parts[0]);
            int p = Integer.parseInt(parts[1]);
            int[] pointers = carConfig.getPointers();
            if (pointers != null) {
                Set<Integer> pset = new HashSet<>();
                for (int pointer : pointers) {
                    pset.add(pointer);
                }
                pset.remove(p);
                pointers = new int[pset.size()];
                int n = 0;
                for (int pointer : pset) {
                    pointers[n++] = pointer;
                }
                carConfig.setPointers(pointers);
            }
            return;
        }
        if (requestCode == DO_POINTER) {
            refresh();
            checkPhone(data.getStringExtra(Names.ID));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        String id = (String) v.getTag();
        if (id == null)
            return;
        CarConfig config = CarConfig.get(getActivity(), id);
        String name = config.getName();
        if (name.equals(""))
            name = config.getLogin();
        Alert alert = new Alert();
        Bundle args = new Bundle();
        args.putString(Names.ID, id);
        args.putString(Names.TITLE, getString(R.string.delete));
        args.putString(Names.MESSAGE, String.format(getString(R.string.delete_car), name));
        alert.setArguments(args);
        alert.setTargetFragment(this, DO_DELETE);
        alert.show(getActivity().getSupportFragmentManager(), "alert");
    }

    void refresh() {
        ids = new Vector<>();
        AppConfig appConfig = AppConfig.get(getActivity());
        String[] cars = appConfig.getIds().split(";");
        for (String car : cars) {
            ids.add(car);
            CarConfig carConfig = CarConfig.get(getActivity(), car);
            int[] pointers = carConfig.getPointers();
            if (pointers == null)
                continue;
            for (int p : pointers) {
                String p_id = car + "_" + p;
                ids.add(p_id);
            }
        }
        vList.setAdapter(new CarsAdapter());
    }

    void checkPhone(String id) {
        CarConfig carConfig = CarConfig.get(getActivity(), id);
        CarState carState = CarState.get(getActivity(), id);
        if (!carState.isUse_phone() || !carConfig.getPhone().equals("") || !State.hasTelephony(getActivity()))
            return;
        PhoneDialog dialog = new PhoneDialog();
        Bundle args = new Bundle();
        args.putString(Names.ID, id);
        dialog.setArguments(args);
        dialog.show(getActivity().getSupportFragmentManager(), "phone");
    }

    class CarsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return ids.size() + 1;
        }

        @Override
        public Object getItem(int i) {
            if (i < ids.size())
                return ids.get(i);
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View v = view;
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.car_item, null);
                if (i < ids.size()) {
                    v.findViewById(R.id.car_block).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.add).setVisibility(View.GONE);
                    String id = ids.get(i);
                    CarConfig carConfig = CarConfig.get(getActivity(), id);
                    String name = carConfig.getName();
                    if (name.equals(""))
                        name = carConfig.getLogin();
                    TextView tv = (TextView) v.findViewById(R.id.name);
                    tv.setText(name);
                    View vDelete = v.findViewById(R.id.delete);
                    boolean bDelete = true;
                    if (id.indexOf("_") > 0) {
                        v.findViewById(R.id.pointer).setVisibility(View.VISIBLE);
                    } else {
                        v.findViewById(R.id.pointer).setVisibility(View.GONE);
                        AppConfig appConfig = AppConfig.get(getActivity());
                        bDelete = appConfig.getIds().split(";").length > 1;
                    }
                    if (bDelete) {
                        vDelete.setVisibility(View.VISIBLE);
                        vDelete.setTag(id);
                        vDelete.setOnClickListener(CarsFragment.this);
                    } else {
                        vDelete.setVisibility(View.INVISIBLE);
                        vDelete.setTag(null);
                    }
                } else {
                    v.findViewById(R.id.car_block).setVisibility(View.GONE);
                    v.findViewById(R.id.add).setVisibility(View.VISIBLE);
                }
            }
            return v;
        }
    }
}
