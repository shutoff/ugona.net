package net.ugona.plus;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.io.Serializable;
import java.util.Vector;

public class ZonesFragment extends MainFragment {

    ListView vList;
    View vProgress;
    View vError;
    Vector<Zone> zones;

    @Override
    int layout() {
        return R.layout.settings;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ok, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        vList = (ListView) v.findViewById(R.id.list);
        vError = v.findViewById(R.id.error);
        vError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });
        vProgress = v.findViewById(R.id.progress);
        refresh();
        return v;
    }

    void done() {
        if (getActivity() == null)
            return;
        vProgress.setVisibility(View.GONE);
        vError.setVisibility(View.GONE);
        vList.setVisibility(View.VISIBLE);
        vList.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return zones.size() + 1;
            }

            @Override
            public Object getItem(int position) {
                if (position < zones.size())
                    return zones.get(position);
                return null;
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
                    v = inflater.inflate(R.layout.zone_item, null);
                }
                View vAdd = v.findViewById(R.id.add);
                View vZone = v.findViewById(R.id.zone_block);
                if (position < zones.size()) {
                    Zone z = zones.get(position);
                    TextView tvName = (TextView) v.findViewById(R.id.name);
                    tvName.setText(z.name);
                    CheckBox checkBox = (CheckBox) v.findViewById(R.id.check);
                    checkBox.setChecked(z.device);
                    v.findViewById(R.id.sms).setVisibility(z.sms ? View.VISIBLE : View.INVISIBLE);
                    vAdd.setVisibility(View.GONE);
                    vZone.setVisibility(View.VISIBLE);
                } else {
                    vAdd.setVisibility(View.VISIBLE);
                    vZone.setVisibility(View.GONE);
                }
                return v;
            }
        });
    }

    void refresh() {
        vProgress.setVisibility(View.VISIBLE);
        vError.setVisibility(View.GONE);
        vList.setVisibility(View.GONE);
        HttpTask task = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                zones = new Vector<>();
                JsonArray v_zones = res.get("zones").asArray();
                for (int i = 0; i < v_zones.size(); i++) {
                    Zone z = new Zone();
                    Config.update(z, v_zones.get(i).asObject());
                    zones.add(z);
                }
                done();
                refreshDone();
            }

            @Override
            void error() {
                vProgress.setVisibility(View.GONE);
                vError.setVisibility(View.VISIBLE);
                vList.setVisibility(View.GONE);
                refreshDone();
            }
        };
        CarConfig config = CarConfig.get(getActivity(), id());
        Param param = new Param();
        param.skey = config.getKey();
        task.execute("/zones", param);
    }

    static class Param implements Serializable {
        String skey;
    }

    static class Zone implements Serializable {
        int id;
        double lat1;
        double lng1;
        double lat2;
        double lng2;
        String name;
        boolean device;
        boolean sms;

        Zone() {
            name = "";
        }
    }
}
