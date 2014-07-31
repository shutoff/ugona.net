package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.preference.PreferenceManager;

import java.util.Date;

public class CarDrawable {

    static Bitmap bitmap;
    String[] parts_id;
    boolean horizontal;

    CarDrawable() {
        parts_id = new String[9];
    }

    private boolean update(Context ctx, String car_id, boolean engine, boolean big) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        long last = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
        Date now = new Date();
        boolean upd = false;
        boolean doors4 = preferences.getBoolean(Names.Car.DOORS_4 + car_id, false);
        if ((last < now.getTime() - 24 * 60 * 60 * 1000)) {
            upd = setLayer(0, doors4 ? "car_black4" : "car_black");
            upd |= setLayer(1);
            upd |= setLayer(2);
            upd |= setLayer(3);
            upd |= setLayer(4);
            upd |= setLayer(5);
            upd |= setLayer(6);
            upd |= setLayer(7);
            upd |= setLayer(8);
        } else {

            boolean guard = preferences.getBoolean(Names.Car.GUARD + car_id, false);
            boolean guard0 = preferences.getBoolean(Names.Car.GUARD0 + car_id, false);
            boolean guard1 = preferences.getBoolean(Names.Car.GUARD1 + car_id, false);

            boolean white = !guard || (guard0 && guard1);

            upd = setModeCar(!white, preferences.getBoolean(Names.Car.ZONE_ACCESSORY + car_id, false), doors4);

            if (doors4) {
                boolean doors_alarm = preferences.getBoolean(Names.Car.ZONE_DOOR + car_id, false);
                boolean fl = preferences.getBoolean(Names.Car.DOOR_FL, false);
                upd |= setModeOpen(1, "door_fl", !white, fl, fl && doors_alarm, false);
                boolean fr = preferences.getBoolean(Names.Car.DOOR_FR, false);
                upd |= setModeOpen(6, "door_fr", !white, fr, fr && doors_alarm, false);
                boolean bl = preferences.getBoolean(Names.Car.DOOR_BL, false);
                upd |= setModeOpen(7, "door_bl", !white, bl, bl && doors_alarm, false);
                boolean br = preferences.getBoolean(Names.Car.DOOR_BR, false);
                upd |= setModeOpen(8, "door_br", !white, br, br && doors_alarm, false);

            } else {
                boolean doors_open = preferences.getBoolean(Names.Car.INPUT1 + car_id, false);
                boolean doors_alarm = preferences.getBoolean(Names.Car.ZONE_DOOR + car_id, false);
                if (white && doors_alarm) {
                    doors_alarm = false;
                    doors_open = true;
                }
                upd |= setModeOpen(1, "doors", !white, doors_open, doors_alarm, false);
                upd |= setLayer(6);
                upd |= setLayer(7);
                upd |= setLayer(8);
            }

            boolean hood_open = preferences.getBoolean(Names.Car.INPUT4 + car_id, false);
            boolean hood_alarm = preferences.getBoolean(Names.Car.ZONE_HOOD + car_id, false);
            if (white && hood_alarm) {
                hood_alarm = false;
                hood_open = true;
            }
            upd |= setModeOpen(2, "hood", !white, hood_open, hood_alarm, doors4);

            boolean trunk_open = preferences.getBoolean(Names.Car.INPUT2 + car_id, false);
            boolean trunk_alarm = preferences.getBoolean(Names.Car.ZONE_TRUNK + car_id, false);
            if (white && trunk_alarm) {
                trunk_alarm = false;
                trunk_open = true;
            }
            upd |= setModeOpen(3, "trunk", !white, trunk_open, trunk_alarm, doors4);

            boolean az = preferences.getBoolean(Names.Car.AZ + car_id, false);
            if (az && engine) {
                upd |= setLayer(4, "engine1", !white);
            } else if (Preferences.getRele(preferences, car_id)) {
                upd |= setLayer(4, "heater", !white);
            } else {
                String ignition_id = null;
                if (!az && (preferences.getBoolean(Names.Car.INPUT3 + car_id, false) || preferences.getBoolean(Names.Car.ZONE_IGNITION + car_id, false)))
                    ignition_id = guard ? "ignition_red" : (white ? "ignition_blue" : "ignition");
                upd |= setLayer(4, ignition_id);
            }

            String state = null;
            if (guard) {
                state = white ? "lock_blue" : "lock_white";
                long guard_t = preferences.getLong(Names.Car.GUARD_TIME + car_id, 0);
                long card_t = preferences.getLong(Names.Car.CARD + car_id, 0);
                if ((guard_t > 0) && (card_t > 0) && (card_t < guard_t))
                    state = "lock_red";
                if (!big)
                    state += "_widget";
            }
            if (guard0 && !guard1)
                state = "valet";
            if (!guard0 && guard1)
                state = "block";
            upd |= setLayer(5, state);
        }
        return upd;
    }

    Drawable getResource(Context ctx, String name) {
        String n = name;
        if (horizontal)
            n += "_h";
        try {
            int id = ctx.getResources().getIdentifier(n, "drawable", ctx.getPackageName());
            if (id != 0)
                return ctx.getResources().getDrawable(id);
            if (horizontal) {
                id = ctx.getResources().getIdentifier(name, "drawable", ctx.getPackageName());
                BitmapDrawable bitmapDrawable = (BitmapDrawable) ctx.getResources().getDrawable(id);
                Bitmap bitmap = bitmapDrawable.getBitmap();
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                return new BitmapDrawable(bitmap);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    Drawable getDrawable(Context ctx, String car_id) {
        if (!update(ctx, car_id, false, true))
            return null;

        int count = 0;
        for (String part : parts_id) {
            if (part != null)
                count++;
        }
        Drawable[] parts = new Drawable[count];
        int n = 0;
        for (String part : parts_id) {
            if (part == null)
                continue;
            parts[n++] = getResource(ctx, part);
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
        for (String part : parts_id) {
            if (part == null)
                continue;
            Drawable d = getResource(ctx, part);
            d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            d.draw(canvas);
        }
        return bitmap;
    }

    boolean setLayer(int n) {
        if (parts_id[n] == null)
            return false;
        parts_id[n] = null;
        return true;
    }

    boolean setLayer(int n, String name) {
        if (name == null) {
            if (parts_id[n] == null)
                return false;
            parts_id[n] = null;
            return true;
        }
        if (parts_id[n] == null) {
            parts_id[n] = name;
            return true;
        }
        if (parts_id[n].equals(name))
            return false;
        parts_id[n] = name;
        return true;
    }

    boolean setLayer(int n, String name, boolean white) {
        if (!white)
            name += "_blue";
        return setLayer(n, name);
    }

    boolean setModeCar(boolean guard, boolean alarm, boolean doors4) {
        String pos = guard ? "car_blue" : "car_white";
        if (alarm)
            pos = "car_red";
        if (doors4)
            pos += "4";
        return setLayer(0, pos);
    }

    boolean setModeOpen(int pos, String group, boolean guard, boolean open, boolean alarm, boolean doors4) {
        if (alarm) {
            group += "_red";
        } else if (guard) {
            group += "_blue";
        } else {
            group += "_white";
        }
        if (open || alarm)
            group += "_open";
        if (doors4)
            group += "4";
        return setLayer(pos, group);
    }

}
