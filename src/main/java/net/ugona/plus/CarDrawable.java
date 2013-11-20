package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import java.util.Date;

public class CarDrawable {

    static Drawable[] drawables;

    LayerDrawable drawable;

    int[] parts_id;

    static int width;
    static int height;

    int small;

    CarDrawable(Context ctx, boolean bSmall) {

        init(ctx);

        if (bSmall)
            small = 1;

        Drawable[] parts;
        parts = new Drawable[8];
        parts_id = new int[8];

        parts[0] = drawables[1];
        parts[1] = drawables[0];
        parts[2] = drawables[0];
        parts[3] = drawables[0];
        parts[4] = drawables[0];
        parts[5] = drawables[0];
        parts[6] = drawables[0];
        parts[7] = drawables[0];

        parts_id[0] = 0;
        parts_id[1] = 0;
        parts_id[2] = 0;
        parts_id[3] = 0;
        parts_id[4] = 0;
        parts_id[5] = 0;
        parts_id[6] = 0;
        parts_id[7] = 0;

        drawable = new LayerDrawable(parts);
    }

    static void init(Context ctx) {

        if (drawables != null)
            return;

        drawables = new Drawable[31];
        drawables[0] = new ColorDrawable(Color.TRANSPARENT);

        Resources resources = ctx.getResources();
        Bitmap bmp = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.car_black);
        width = bmp.getWidth();
        height = bmp.getHeight();

        drawables[1] = resources.getDrawable(R.drawable.car_black);
        drawables[2] = resources.getDrawable(R.drawable.car_white);
        drawables[3] = resources.getDrawable(R.drawable.car_blue);
        drawables[4] = resources.getDrawable(R.drawable.car_red);
        drawables[5] = resources.getDrawable(R.drawable.doors_white);
        drawables[6] = resources.getDrawable(R.drawable.doors_blue);
        drawables[7] = resources.getDrawable(R.drawable.doors_red);
        drawables[8] = resources.getDrawable(R.drawable.doors_white_open);
        drawables[9] = resources.getDrawable(R.drawable.doors_blue_open);
        drawables[10] = resources.getDrawable(R.drawable.doors_red_open);
        drawables[11] = resources.getDrawable(R.drawable.hood_white);
        drawables[12] = resources.getDrawable(R.drawable.hood_blue);
        drawables[13] = resources.getDrawable(R.drawable.hood_red);
        drawables[14] = resources.getDrawable(R.drawable.hood_white_open);
        drawables[15] = resources.getDrawable(R.drawable.hood_blue_open);
        drawables[16] = resources.getDrawable(R.drawable.hood_red_open);
        drawables[17] = resources.getDrawable(R.drawable.trunk_white);
        drawables[18] = resources.getDrawable(R.drawable.trunk_blue);
        drawables[19] = resources.getDrawable(R.drawable.trunk_red);
        drawables[20] = resources.getDrawable(R.drawable.trunk_white_open);
        drawables[21] = resources.getDrawable(R.drawable.trunk_blue_open);
        drawables[22] = resources.getDrawable(R.drawable.trunk_red_open);
        drawables[23] = resources.getDrawable(R.drawable.lock_white);
        drawables[24] = resources.getDrawable(R.drawable.lock_white_widget);
        drawables[25] = resources.getDrawable(R.drawable.engine);
        drawables[26] = resources.getDrawable(R.drawable.engine_blue);
        drawables[27] = resources.getDrawable(R.drawable.ignition);
        drawables[28] = resources.getDrawable(R.drawable.ignition_red);
        drawables[29] = resources.getDrawable(R.drawable.valet);
        drawables[30] = resources.getDrawable(R.drawable.block);
    }

    Drawable getDrawable() {

        return drawable;
    }

    boolean update(SharedPreferences preferences, String car_id) {
        long last = preferences.getLong(Names.EVENT_TIME + car_id, 0);
        Date now = new Date();
        boolean upd = false;
        if (last < now.getTime() - 24 * 60 * 60 * 1000) {
            upd |= setLayer(0, 1);
            upd |= setLayer(1, 0);
            upd |= setLayer(2, 0);
            upd |= setLayer(3, 0);
        } else {

            boolean guard = preferences.getBoolean(Names.GUARD + car_id, false);
            upd |= setModeCar(guard, preferences.getBoolean(Names.ZONE_ACCESSORY + car_id, false));

            boolean doors_open = preferences.getBoolean(Names.INPUT1 + car_id, false);
            boolean doors_alarm = preferences.getBoolean(Names.ZONE_DOOR + car_id, false);
            upd |= setModeOpen(0, guard, doors_open, doors_alarm);

            boolean hood_open = preferences.getBoolean(Names.INPUT4 + car_id, false);
            boolean hood_alarm = preferences.getBoolean(Names.ZONE_HOOD + car_id, false);
            upd |= setModeOpen(1, guard, hood_open, hood_alarm);

            boolean trunk_open = preferences.getBoolean(Names.INPUT2 + car_id, false);
            boolean trunk_alarm = preferences.getBoolean(Names.ZONE_TRUNK + car_id, false);
            upd |= setModeOpen(2, guard, trunk_open, trunk_alarm);

            upd |= setLayer(4, guard ? 23 + small : 0);

            boolean engine = preferences.getBoolean(Names.ENGINE + car_id, false);
            upd |= setLayer(5, engine ? 25 : 0);

            int ignition = 0;
            if (preferences.getBoolean(Names.INPUT3 + car_id, false))
                ignition = 27;
            if (guard && !engine && preferences.getBoolean(Names.ZONE_IGNITION + car_id, false))
                ignition = 28;
            upd |= setLayer(6, ignition);

            int state = 0;
            if (Preferences.getValet(preferences, car_id))
                state = 29;
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
                parts[n++] = drawables[part];
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
