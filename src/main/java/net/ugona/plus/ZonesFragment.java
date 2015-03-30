package net.ugona.plus;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Vector;

public class ZonesFragment
        extends MainFragment
        implements AdapterView.OnItemClickListener {

    static final int ZONE_REQUEST = 1;
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
        vList.setOnItemClickListener(this);
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Zone zone = null;
        if (position < zones.size())
            zone = zones.get(position);
        if (zone == null) {
            zone = new Zone();
            zone.name = "Zone";
            CarState state = CarState.get(getActivity(), id());
            try {
                String[] pos = state.getGps().split(",");
                double lat = Double.parseDouble(pos[0]);
                double lng = Double.parseDouble(pos[1]);
                zone.lat1 = lat - 0.001;
                zone.lat2 = lat + 0.001;
                double d1 = State.distance(lat, lng, zone.lat1, lng);
                double d2 = State.distance(lat, lng, lat, lng + 0.001);
                zone.lng1 = lng - 0.001 * d1 / d2;
                zone.lng2 = lng + 0.001 * d1 / d2;
            } catch (Exception ex) {
                // ignore
            }
        }
        Intent intent = new Intent(getActivity(), ZoneEdit.class);
        byte[] data = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(zone);
            data = bos.toByteArray();
            out.close();
            bos.close();
        } catch (Exception ex) {
            // ignore
        }
        intent.putExtra(Names.TRACK, data);
        startActivityForResult(intent, ZONE_REQUEST);
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
