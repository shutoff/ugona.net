package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.preference.PreferenceManager;

import java.util.Date;

public class CarDrawable {

    static final int[] drawablesId = {
            R.drawable.car_black,           // 1
            R.drawable.car_white,           // 2
            R.drawable.car_blue,            // 3
            R.drawable.car_red,             // 4
            R.drawable.doors_white,         // 5
            R.drawable.doors_blue,          // 6
            R.drawable.doors_red,           // 7
            R.drawable.doors_white_open,    // 8
            R.drawable.doors_blue_open,     // 9
            R.drawable.doors_red_open,      // 10
            R.drawable.hood_white,          // 11
            R.drawable.hood_blue,           // 12
            R.drawable.hood_red,            // 13
            R.drawable.hood_white_open,     // 14
            R.drawable.hood_blue_open,      // 15
            R.drawable.hood_red_open,       // 16
            R.drawable.trunk_white,         // 17
            R.drawable.trunk_blue,          // 18
            R.drawable.trunk_red,           // 19
            R.drawable.trunk_white_open,    // 20
            R.drawable.trunk_blue_open,     // 21
            R.drawable.trunk_red_open,      // 22
            R.drawable.engine1,             // 23
            R.drawable.engine1_blue,        // 24
            R.drawable.ignition,            // 25
            R.drawable.ignition_blue,       // 26
            R.drawable.ignition_red,        // 27
            R.drawable.lock_white,          // 28
            R.drawable.lock_white_widget,   // 29
            R.drawable.lock_blue,           // 30
            R.drawable.lock_blue_widget,    // 31
            R.drawable.valet,               // 32
            R.drawable.block,               // 33
            R.drawable.heater,              // 34
            R.drawable.heater_blue,         // 35
            R.drawable.lock_red,            // 36
            R.drawable.lock_red_widget,     // 37
    };
    static Bitmap bitmap;
    int[] parts_id;

    CarDrawable() {
        parts_id = new int[6];

        parts_id[0] = 0;
        parts_id[1] = 0;
        parts_id[2] = 0;
        parts_id[3] = 0;
        parts_id[4] = 0;
        parts_id[5] = 0;
    }

    private boolean update(Context ctx, String car_id, boolean engine, boolean big) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        long last = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
        Date now = new Date();
        boolean upd = false;
        if (last < now.getTime() - 24 * 60 * 60 * 1000) {
            upd = setLayer(0, 1);
            upd |= setLayer(1, 0);
            upd |= setLayer(2, 0);
            upd |= setLayer(3, 0);
            upd |= setLayer(4, 0);
            upd |= setLayer(5, 0);
        } else {

            boolean guard = preferences.getBoolean(Names.Car.GUARD + car_id, false);
            boolean guard0 = preferences.getBoolean(Names.Car.GUARD0 + car_id, false);
            boolean guard1 = preferences.getBoolean(Names.Car.GUARD1 + car_id, false);

            boolean white = !guard || (guard0 && guard1);

            upd = setModeCar(!white, preferences.getBoolean(Names.Car.ZONE_ACCESSORY + car_id, false));

            boolean doors_open = preferences.getBoolean(Names.Car.INPUT1 + car_id, false);
            boolean doors_alarm = preferences.getBoolean(Names.Car.ZONE_DOOR + car_id, false);
            if (white && doors_alarm) {
                doors_alarm = false;
                doors_open = true;
            }
            upd |= setModeOpen(0, !white, doors_open, doors_alarm);

            boolean hood_open = preferences.getBoolean(Names.Car.INPUT4 + car_id, false);
            boolean hood_alarm = preferences.getBoolean(Names.Car.ZONE_HOOD + car_id, false);
            if (white && hood_alarm) {
                hood_alarm = false;
                hood_open = true;
            }
            upd |= setModeOpen(1, !white, hood_open, hood_alarm);

            boolean trunk_open = preferences.getBoolean(Names.Car.INPUT2 + car_id, false);
            boolean trunk_alarm = preferences.getBoolean(Names.Car.ZONE_TRUNK + car_id, false);
            if (white && trunk_alarm) {
                trunk_alarm = false;
                trunk_open = true;
            }
            upd |= setModeOpen(2, !white, trunk_open, trunk_alarm);

            boolean az = preferences.getBoolean(Names.Car.AZ + car_id, false);
            if (az && engine) {
                upd |= setLayer(4, white ? 24 : 23);
            } else if (Preferences.getRele(preferences, car_id)) {
                upd |= setLayer(4, white ? 35 : 34);
            } else {
                int ignition_id = 0;
                if (!az && (preferences.getBoolean(Names.Car.INPUT3 + car_id, false) || preferences.getBoolean(Names.Car.ZONE_IGNITION + car_id, false)))
                    ignition_id = guard ? 27 : (white ? 26 : 25);
                upd |= setLayer(4, ignition_id);
            }

            int state = 0;
            if (guard) {
                state = white ? 30 : 28;
                long guard_t = preferences.getLong(Names.Car.GUARD_TIME + car_id, 0);
                long card_t = preferences.getLong(Names.Car.CARD + car_id, 0);
                if ((guard_t > 0) && (card_t > 0))
                    state = 36;
                if (!big)
                    state++;
            }
            if (guard0 && !guard1)
                state = 32;
            if (!guard0 && guard1)
                state = 33;
            upd |= setLayer(5, state);
        }
        return upd;
    }

    Drawable getDrawable(Context ctx, String car_id) {
        if (!update(ctx, car_id, false, true))
            return null;

        int count = 0;
        for (int part : parts_id) {
            if (part > 0)
                count++;
        }
        Drawable[] parts = new Drawable[count];
        int n = 0;
        for (int part : parts_id) {
            if (part > 0) {
                try {
                    parts[n++] = ctx.getResources().getDrawable(drawablesId[part - 1]);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return new LayerDrawable(parts);
    }

    Bitmap getBitmap(Context ctx, String car_id, boolean bigPict) {
        if (!update(ctx, car_id, true, bigPict) && (bitmap != null))
            return bitmap;

        if (bitmap == null) {
            Bitmap bmp = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.car_black);
            int width = bmp.getWidth();
            int height = bmp.getHeight();
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } else {
            bitmap.eraseColor(Color.TRANSPARENT);
        }
        Canvas canvas = new Canvas(bitmap);
        for (int part : parts_id) {
            if (part == 0)
                continue;
            Drawable d = ctx.getResources().getDrawable(drawablesId[part - 1]);
            d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            d.draw(canvas);
        }
        return bitmap;
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
        if (open || alarm)
            pos += 3;
        return setLayer(group + 1, group * 6 + pos + 5);
    }

}
