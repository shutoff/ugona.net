package net.ugona.plus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.Vector;

public class CarImage {

    int animation;
    String state;
    Paint paint;
    Theme theme;
    String name;

    CarImage(Context context, String theme_name) {
        state = "";
        paint = new Paint();
        paint.setFlags(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        theme = Theme.getTheme(context, theme_name);
        name = theme_name;
    }

    boolean update(CarState s, boolean compact) {
        Vector<String> parts = new Vector<String>();
        boolean guard = s.isGuard();
        long guard_time = s.getGuard_time();
        long card_time = s.getCard();
        boolean card = false;
        String text = null;
        animation = -1;
        if ((guard_time > 0) && (card_time > 0) && (guard_time > card_time)) {
            card = true;
            guard = false;
            text = "card";
        }
        if (s.getGuard_mode() == 3) {
            guard = false;
            text = "ps_guard";
        }
        if (guard) {
            parts.add(s.isAccessory() ? "r" : "b");
        } else {
            parts.add("_");
        }
        if (s.isDoor_br()) {
            String p = "4_o";
            if (guard)
                p += "_r";
            parts.add(p);
        } else {
            String p = "4";
            if (guard)
                p += "_b";
            parts.add(p);
        }
        if (s.isDoor_bl()) {
            String p = "3_o";
            if (guard)
                p += "_r";
            parts.add(p);
        } else {
            String p = "3";
            if (guard)
                p += "_b";
            parts.add(p);
        }
        if (s.isDoor_fr()) {
            String p = "2_o";
            if (guard)
                p += "_r";
            parts.add(p);
        } else {
            String p = "2";
            if (guard)
                p += "_b";
            parts.add(p);
        }
        if (s.isDoor_fl()) {
            String p = "1_o";
            if (guard)
                p += "_r";
            parts.add(p);
        } else {
            String p = "1";
            if (guard)
                p += "_b";
            parts.add(p);
        }
        if (s.isHood()) {
            String p = "h_o";
            if (guard)
                p += "_r";
            parts.add(p);
        } else {
            String p = "h";
            if (guard)
                p += "_b";
            parts.add(p);
        }
        if (s.isTrunk()) {
            String p = "t_o";
            if (guard)
                p += "_r";
            parts.add(p);
        } else {
            String p = "t";
            if (guard)
                p += "_b";
            parts.add(p);
        }
        boolean az = s.getAz_time() > 0;
        if (!s.isGuard())
            az = false;

        if (compact) {
            int mode = s.getGuard_mode();
            if (mode == 2) {
                parts.add("valet");
            } else if (card) {
                parts.add("lock_r");
            } else if ((s.isGuard() || (mode == 3))) {
                if (guard) {
                    parts.add("lock");
                } else {
                    parts.add("lock_b");
                }
            }
            if (az) {
                String ign = "az";
                if (!guard)
                    ign += "_b";
                parts.add(ign);
            } else if (s.isIgnition()) {
                String ign = "ignition";
                if (guard) {
                    if (s.isGuard())
                        ign += "_r";
                } else {
                    ign += "_b";
                }
                parts.add(ign);
            }
        } else {
            int shock = s.getShock();
            if (shock == 1)
                parts.add("a_hit1");
            if (shock == 2)
                parts.add("a_hit2");
            if (s.isMove())
                parts.add("a_move");
            if (s.isAz()) {
                animation = parts.size();
                parts.add("az");
                text = "autostart";
            }
            if (s.isIn_sensor())
                parts.add("a_in");
            if (s.isExt_sensor())
                parts.add("a_out");
        }

        String new_state = null;
        for (String part : parts) {
            if (new_state == null) {
                new_state = part;
                continue;
            }
            new_state += ";" + part;
        }

        new_state += "|";
        parts = new Vector<String>();
        if (s.isIgnition()) {
            parts.add((s.isGuard() && !az) ? "r_ignition" : "b_ignition");
            if (text == null)
                text = "ignition";
        } else if (s.isHeater()) {
            parts.add("b_heater");
            text = "rele";
        }
        if (s.isGuard() && (text == null))
            text = "guard";
        int mode = s.getGuard_mode();
        if (mode == 1) {
            parts.add("r_block");
            text = "block";
        } else if (card) {
            parts.add("r_lock");
        } else if ((s.isGuard() || (mode == 3))) {
            parts.add("b_lock");
        }
        if (mode == 2) {
            parts.add("b_valet");
            text = "valet_mode";
        }
        if (s.isTilt()) {
            parts.add("r_slope");
            text = "tilt";
        }
        if (s.isSos()) {
            parts.add("r_sos");
            text = "sos";
        }

        String ext_state = null;
        for (String part : parts) {
            if (ext_state == null) {
                ext_state = part;
                continue;
            }
            ext_state += ";" + part;
        }
        if (ext_state != null)
            new_state += ext_state;
        if (text != null)
            new_state += "|" + text;

        if ((animation == -1) && new_state.equals(state))
            return false;
        state = new_state;
        return true;
    }

    Bitmap getBitmap() {
        String[] ext = state.split("\\|");
        String[] parts = ext[0].split(";");

        int w = theme.width;
        int h = theme.height;
        while (w > 512) {
            w = w >> 1;
        }
        float k = (float) w / theme.width;
        h = (int) ((float) h * k);

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.TRANSPARENT);
        Canvas canvas = new Canvas(bitmap);

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            Theme.Pict pict = theme.get(part);
            if (pict == null)
                continue;
            Bitmap bmp = pict.bitmap;
            RectF rect = new RectF(pict.x * k, pict.y * k, (pict.x + bmp.getWidth()) * k, (bmp.getHeight() + pict.y) * k);
            canvas.drawBitmap(bmp, null, rect, paint);
            bmp.recycle();
        }
        return bitmap;
    }

}
