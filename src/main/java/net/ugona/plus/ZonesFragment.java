package net.ugona.plus;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Vector;

public class ZonesFragment
        extends MainFragment
        implements AdapterView.OnItemClickListener {

    static final int ZONE_REQUEST = 200;
    ListView vList;
    View vError;
    Vector<Zone> zones;
    Vector<Zone> saved;

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
                onRefresh();
            }
        });
        if (savedInstanceState != null) {
            byte[] data = savedInstanceState.getByteArray("zones");
            if (data != null) {
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(data);
                    ObjectInput in = new ObjectInputStream(bis);
                    zones = (Vector<Zone>) in.readObject();
                } catch (Exception ex) {
                    // ignore
                }
            }
            data = savedInstanceState.getByteArray("saved");
            if (data != null) {
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(data);
                    ObjectInput in = new ObjectInputStream(bis);
                    saved = (Vector<Zone>) in.readObject();
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        if (zones != null) {
            done();
        } else {
            onRefresh();
        }
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (zones != null) {
            byte[] data = null;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(zones);
                data = bos.toByteArray();
                out.close();
                bos.close();
            } catch (Exception ex) {
                // ignore
            }
            outState.putByteArray("zones", data);
        }
        if (saved != null) {
            byte[] data = null;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(saved);
                data = bos.toByteArray();
                out.close();
                bos.close();
            } catch (Exception ex) {
                // ignore
            }
            outState.putByteArray("saved", data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (zones != null) {
            if (item.getItemId() == android.R.id.home) {
                if (zones.size() != saved.size())
                    return true;
                for (int i = 0; i < zones.size(); i++) {
                    if (!zones.get(i).equals(saved.get(i)))
                        return true;
                }
            }
            if (item.getItemId() == R.id.ok) {
                final ProgressDialog dialog = new ProgressDialog(getActivity());
                dialog.setMessage(getString(R.string.save_settings));
                dialog.show();
                HttpTask task = new HttpTask() {
                    @Override
                    void result(JsonObject res) throws ParseException {
                        try {
                            saved = new Vector<>();
                            for (Zone z : zones) {
                                saved.add(z.copy());
                            }
                            dialog.dismiss();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    void error() {
                        try {
                            dialog.dismiss();
                            Toast toast = Toast.makeText(getActivity(), R.string.save_error, Toast.LENGTH_LONG);
                            toast.show();
                        } catch (Exception ex) {
                            // ignore
                        }
                    }
                };
                CarConfig config = CarConfig.get(getActivity(), id());
                Param params = new Param();
                params.skey = config.getKey();
                params.zones = new Zone[zones.size()];
                zones.toArray(params.zones);
                task.execute("/zones", params);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    void done() {
        if (getActivity() == null)
            return;
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
            public View getView(final int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    v = inflater.inflate(R.layout.zone_item, null);
                }
                View vAdd = v.findViewById(R.id.add);
                View vZone = v.findViewById(R.id.zone_block);
                if (position < zones.size()) {
                    Zone z = zones.get(position);
                    TextView tvName = (TextView) v.findViewById(R.id.name);
                    tvName.setText(z.name);
                    tvName.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onItemClick(vList, null, position, 0);
                        }
                    });
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

    @Override
    public void onRefresh() {
        super.onRefresh();
        vError.setVisibility(View.GONE);
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
                saved = new Vector<>();
                for (Zone z : zones) {
                    saved.add(z.copy());
                }
                done();
                refreshDone();
            }

            @Override
            void error() {
                zones = null;
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
                zone.device = true;
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
        getParentFragment().startActivityForResult(intent, ZONE_REQUEST + position);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode >= ZONE_REQUEST) && (requestCode <= ZONE_REQUEST + zones.size())) {
            requestCode -= ZONE_REQUEST;
            Zone zone = null;
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(data.getByteArrayExtra(Names.TRACK));
                ObjectInput in = new ObjectInputStream(bis);
                zone = (ZonesFragment.Zone) in.readObject();
            } catch (Exception ex) {
                // ignore
            }
            BaseAdapter adapter = (BaseAdapter) vList.getAdapter();
            if (zone == null) {
                if (requestCode < zones.size()) {
                    zones.remove(requestCode);
                    adapter.notifyDataSetChanged();
                }
                return;
            }
            if (!checkName(zone, requestCode)) {
                String name = zone.name;
                for (int i = 1; i < 20; i++) {
                    zone.name = name + i;
                    if (checkName(zone, requestCode))
                        break;
                }
            }
            if (requestCode < zones.size()) {
                zones.set(requestCode, zone);
            } else {
                zones.add(zone);
            }
            adapter.notifyDataSetChanged();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    boolean checkName(Zone z, int position) {
        for (int i = 0; i < zones.size(); i++) {
            if (i == position)
                continue;
            if (zones.get(i).name.equals(z.name))
                return false;
        }
        return true;
    }

    static class Param implements Serializable {
        String skey;
        Zone[] zones;
    }

    static class Zone extends Config implements Cloneable {
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

        Zone copy() {
            try {
                Object res = clone();
                return (Zone) res;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }

    }
}
