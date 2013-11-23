package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.preference.PreferenceManager;

import java.util.Date;

public class CarDrawable {

    LayerDrawable drawable;

    int[] parts_id;

    static int width;
    static int height;

    int small;

    CarDrawable(Context ctx, boolean bSmall) {

        init(ctx);

        if (bSmall)
            small = 1;

        parts_id = new int[8];

        parts_id[0] = 0;
        parts_id[1] = 0;
        parts_id[2] = 0;
        parts_id[3] = 0;
        parts_id[4] = 0;
        parts_id[5] = 0;
        parts_id[6] = 0;
        parts_id[7] = 0;

        Drawable[] parts = new Drawable[1];
        parts[0] = ctx.getResources().getDrawable(drawablesId[0]);
        drawable = new LayerDrawable(parts);
    }

    static final int[] drawablesId = {
            R.drawable.car_black,
            R.drawable.car_white,
            R.drawable.car_blue,
            R.drawable.car_red,
            R.drawable.doors_white,
            R.drawable.doors_blue,
            R.drawable.doors_red,
            R.drawable.doors_white_open,
            R.drawable.doors_blue_open,
            R.drawable.doors_red_open,
            R.drawable.hood_white,
            R.drawable.hood_blue,
            R.drawable.hood_red,
            R.drawable.hood_white_open,
            R.drawable.hood_blue_open,
            R.drawable.hood_red_open,
            R.drawable.trunk_white,
            R.drawable.trunk_blue,
            R.drawable.trunk_red,
            R.drawable.trunk_white_open,
            R.drawable.trunk_blue_open,
            R.drawable.trunk_red_open,
            R.drawable.lock_white,
            R.drawable.lock_white_widget,
            R.drawable.engine,
            R.drawable.ignition,
            R.drawable.ignition_red,
            R.drawable.valet,
            R.drawable.block,
    };

    static void init(Context ctx) {

        if (width > 0)
            return;

        Bitmap bmp = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.car_black);
        width = bmp.getWidth();
        height = bmp.getHeight();
    }

    Drawable getDrawable() {

        return drawable;
    }

    boolean update(Context ctx, String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        long last = preferences.getLong(Names.EVENT_TIME + car_id, 0);
        Date now = new Date();
        boolean upd = false;
        if (last < now.getTime() - 24 * 60 * 60 * 1000) {
            upd = setLayer(0, 1);
            upd |= setLayer(1, 0);
            upd |= setLayer(2, 0);
            upd |= setLayer(3, 0);
        } else {

            boolean guard = preferences.getBoolean(Names.GUARD + car_id, false);
            upd = setModeCar(guard, preferences.getBoolean(Names.ZONE_ACCESSORY + car_id, false));

            boolean doors_open = preferences.getBoolean(Names.INPUT1 + car_id, false);
            boolean doors_alarm = preferences.getBoolean(Names.ZONE_DOOR + car_id, false);
            if (!guard && doors_alarm) {
                doors_alarm = false;
                doors_open = true;
            }
            upd |= setModeOpen(0, guard, doors_open, doors_alarm);

            boolean hood_open = preferences.getBoolean(Names.INPUT4 + car_id, false);
            boolean hood_alarm = preferences.getBoolean(Names.ZONE_HOOD + car_id, false);
            if (!guard && hood_alarm) {
                hood_alarm = false;
                hood_open = true;
            }
            upd |= setModeOpen(1, guard, hood_open, hood_alarm);

            boolean trunk_open = preferences.getBoolean(Names.INPUT2 + car_id, false);
            boolean trunk_alarm = preferences.getBoolean(Names.ZONE_TRUNK + car_id, false);
            if (!guard && trunk_alarm) {
                trunk_alarm = false;
                trunk_open = true;
            }
            upd |= setModeOpen(2, guard, trunk_open, trunk_alarm);

            upd |= setLayer(4, guard ? 23 + small : 0);

            boolean engine = preferences.getBoolean(Names.ENGINE + car_id, false);
            upd |= setLayer(5, engine ? 25 : 0);

            int ignition = 0;
            if (preferences.getBoolean(Names.INPUT3 + car_id, false))
                ignition = 26;
            if (!engine && preferences.getBoolean(Names.ZONE_IGNITION + car_id, false)) {
                ignition = guard ? 27 : 26;
            }
            upd |= setLayer(6, ignition);

            int state = 0;
            if (Preferences.getValet(preferences, car_id))
                state = 28;
            upd |= setLayer(7, state);
        }

        if (!upd)
            return false;

        int count = 0;
        for (int part : parts_id) {
            if (part > 0)
                count++;
        }
        Drawable[] parts = new Drawable[count];
        int n = 0;
        for (int part : parts_id) {
            if (part > 0)
                parts[n++] = ctx.getResources().getDrawable(drawablesId[part - 1]);
        }
        drawable = new LayerDrawable(parts);
        return true;
    }

    boolean setLayer(int n, int id) {
        if (parts_id[n] == id)
            return false;
        parts_id[n] = id;
        return true;
    }

    boolean setModeCar(boolean guard, boolean alarm) {
        int pos = guard ? 1 : 0;
        if (alarm)
            pos = 2;
        return setLayer(0, pos + 2);
    }

    boolean setModeOpen(int group, boolean guard, boolean open, boolean alarm) {
        int pos = guard ? 1 : 0;
        if (alarm)
            pos = 2;
        if (open)
            pos += 3;
        return setLayer(group + 1, group * 6 + pos + 5);
    }

}
