package net.ugona.plus;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class CarsFragment extends MainFragment {

    ListView vList;

    String[] ids;

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
        ids = appConfig.getIds().split(";");
        vList.setAdapter(new CarsAdapter());
        return vList;
    }

    class CarsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return ids.length + 1;
        }

        @Override
        public Object getItem(int i) {
            if (i < ids.length)
                return ids[i];
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
                if (i < ids.length) {
                    v.findViewById(R.id.car_block).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.add).setVisibility(View.GONE);
                } else {
                    v.findViewById(R.id.car_block).setVisibility(View.GONE);
                    v.findViewById(R.id.add).setVisibility(View.VISIBLE);
                }
            }
            return v;
        }
    }
}
