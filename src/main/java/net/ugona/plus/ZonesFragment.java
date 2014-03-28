package net.ugona.plus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Vector;

public class ZonesFragment extends DeviceFragment {

    final static int ZONE_EDIT = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        fill();
        return v;
    }

    @Override
    void update() {
        fill();
        super.update();
    }

    void fill() {
        SettingActivity activity = (SettingActivity) getActivity();
        if ((activity == null) || (activity.zones == null))
            return;
        items = new Vector<Item>();
        for (SettingActivity.Zone zone : activity.zones) {
            items.add(new ZoneItem(zone));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == ZONE_EDIT) && (resultCode == Activity.RESULT_OK)) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(data.getByteArrayExtra(Names.TRACK));
                ObjectInput in = new ObjectInputStream(bis);
                SettingActivity.Zone zone = (SettingActivity.Zone) in.readObject();
                SettingActivity activity = (SettingActivity) getActivity();
                boolean found = false;
                for (SettingActivity.Zone z : activity.zones) {
                    if (z.id == zone.id) {
                        z.set(zone);
                        found = true;
                        break;
                    }
                }
                if (!found)
                    activity.zones.add(zone);
                listUpdate();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    class ZoneItem extends CheckItem {

        SettingActivity.Zone zone;

        ZoneItem(SettingActivity.Zone z) {
            super(z.name);
            zone = z;
        }

        @Override
        void setView(View v) {
            super.setView(v);
            View edit = v.findViewById(R.id.check_edit);
            edit.setVisibility(View.VISIBLE);
            edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(getActivity(), ZoneEdit.class);
                    try {
                        byte[] data = null;
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutput out = new ObjectOutputStream(bos);
                        out.writeObject(zone);
                        data = bos.toByteArray();
                        out.close();
                        bos.close();
                        i.putExtra(Names.TRACK, data);
                    } catch (Exception ex) {
                        // ignore
                    }
                    startActivityForResult(i, ZONE_EDIT);
                }
            });
            ImageView sms = (ImageView) v.findViewById(R.id.check_sms);
            sms.setVisibility(View.VISIBLE);
            sms.setImageResource(zone.sms ? R.drawable.sms_on : R.drawable.sms_off);
            sms.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    zone.sms = !zone.sms;
                    listUpdate();
                }
            });
        }

        @Override
        String getValue() {
            return zone.device ? "1" : "";
        }

        @Override
        void setValue(String value) {
            zone.device = !value.equals("");
        }

        @Override
        void click() {
            super.click();
            listUpdate();
        }

        @Override
        boolean changed() {
            return zone.isChanged();
        }
    }
}
