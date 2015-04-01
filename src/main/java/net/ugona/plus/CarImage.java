package net.ugona.plus;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.SparseArray;

import java.util.Vector;

public class CarImage {

    final static float WIDTH = 1080;
    final static float HEIGHT = 750;
    static Pictures pictures = new Pictures();
    int animation;
    String state;
    String prefix;
    Resources resources;
    String pkg;
    Bitmap bitmap;
    Paint paint;

    CarImage(Context context) {
        resources = context.getResources();
        state = "";
        prefix = "c";
        pkg = context.getPackageName();
        paint = new Paint();
        paint.setFlags(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    }

    boolean update(CarState s) {
        Vector<String> parts = new Vector<String>();
        boolean guard = s.isGuard();
        long guard_time = s.getGuard_time();
        long card_time = s.getCard();
        boolean card = false;
        animation = -1;
        if ((guard_time > 0) && (card_time > 0) && (guard_time > card_time)) {
            card = true;
            guard = false;
        }
        if (s.getGuard_mode() == 3)
            guard = false;
        if (guard) {
            parts.add(prefix + "_" + (s.isAccessory() ? "r" : "b"));
        } else {
            parts.add(prefix);
        }
        if (s.isDoor_br()) {
            String p = prefix + "_4_o";
            if (guard)
                p += "_r";
            parts.add(p);
        }
        if (s.isDoor_bl()) {
            String p = prefix + "_3_o";
            if (guard)
                p += "_r";
            parts.add(p);
        } else {
            String p = prefix + "_3";
            if (guard)
                p += "_b";
            parts.add(p);
        }
        if (s.isDoor_fr()) {
            String p = prefix + "_2_o";
            if (guard)
                p += "_r";
            parts.add(p);
        } else {
            String p = prefix + "_2";
            if (guard)
                p += "_b";
            parts.add(p);
        }
        if (s.isDoor_fl()) {
            String p = prefix + "_1_o";
            if (guard)
                p += "_r";
            parts.add(p);
        } else {
            String p = prefix + "_1";
            if (guard)
                p += "_b";
            parts.add(p);
        }
        if (s.isHood()) {
            String p = prefix + "_h_o";
            if (guard)
                p += "_r";
            parts.add(p);
        } else {
            String p = prefix + "_h";
            if (guard)
                p += "_b";
            parts.add(p);
        }
        if (s.isTrunk()) {
            String p = prefix + "_t_o";
            if (guard)
                p += "_r";
            parts.add(p);
        } else {
            String p = prefix + "_t";
            if (guard)
                p += "_b";
            parts.add(p);
        }
        boolean az = s.getAz_time() > 0;
        if (!s.isGuard())
            az = false;

        int shock = s.getShock();
        if (shock == 1)
            parts.add(prefix + "_a_hit1");
        if (shock == 2)
            parts.add(prefix + "_a_hit2");
        if (s.isMove())
            parts.add(prefix + "_a_move");
        if (s.isAz()) {
            animation = parts.size();
            parts.add(prefix + "_az");
        }
        if (s.isIn_sensor())
            parts.add(prefix + "_a_in");
        if (s.isExt_sensor())
            parts.add(prefix + "_a_out");

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
        if (s.isIgnition())
            parts.add((s.isGuard() && !az) ? "r_ignition" : "b_ignition");
        int mode = s.getGuard_mode();
        if (mode == 2) {
            parts.add("r_block");
        } else if (card) {
            parts.add("r_lock");
        } else if (s.isGuard()) {
            parts.add("b_lock");
        }
        if (s.getGuard_mode() == 1)
            parts.add("b_valet");
        if (s.isTilt())
            parts.add("r_slope");
        if (s.isTilt())
            parts.add("r_sos");

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

        if ((animation == -1) && new_state.equals(state))
            return false;
        state = new_state;
        return true;
    }

    int getBitmapId(String image) {
        return resources.getIdentifier(image, "drawable", pkg);
    }

    Bitmap getBitmap(int id) {
        return getBitmapSafely(id, 0);
    }

    Bitmap getBitmap() {
        int w = (int) (WIDTH / 4);
        int h = (int) (HEIGHT / 4);
        float k = w / WIDTH;
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        } else {
            bitmap.eraseColor(Color.TRANSPARENT);
        }
        Canvas canvas = new Canvas(bitmap);
        String[] ext = state.split("\\|");
        String[] parts = ext[0].split(";");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            int id = getBitmapId(part);
            if (id == 0)
                continue;
            Bitmap bmp = getBitmap(id);
            Point p = pictures.get(id);
            RectF rect = new RectF(p.x * k, p.y * k, (p.x + bmp.getWidth()) * k, (bmp.getHeight() + p.y) * k);
            canvas.drawBitmap(bmp, null, rect, paint);
            bmp.recycle();
        }
        return bitmap;
    }

    private Bitmap getBitmapSafely(int id, int sampleSize) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true;
        options.inSampleSize = sampleSize;
        try {
            bitmap = BitmapFactory.decodeResource(resources, id, options);
        } catch (OutOfMemoryError oom) {
            System.gc();
            bitmap = getBitmapSafely(id, sampleSize + 1);
        }

        return bitmap;
    }

    static class Pictures extends SparseArray<Point> {
        Pictures() {
            add(R.drawable.c_1, 563, 221);
            add(R.drawable.c_1_o, 569, 361);
            add(R.drawable.c_2, 337, 208);
            add(R.drawable.c_3, 715, 185);
            add(R.drawable.c_2_o, 261, 73);
            add(R.drawable.c_3_o, 725, 217);
            add(R.drawable.c_4_o, 477, 46);
            add(R.drawable.c_h, 167, 287);
            add(R.drawable.c_h_o, 167, 286);
            add(R.drawable.c_t_o, 727, 6);
            add(R.drawable.c_t, 753, 57);
            add(R.drawable.c, 36, 57);
            add(R.drawable.c_1_o_b, 570, 360);
            add(R.drawable.c_2_b, 338, 207);
            add(R.drawable.c_2_o_b, 262, 72);
            add(R.drawable.c_3_b, 717, 185);
            add(R.drawable.c_3_o_b, 726, 216);
            add(R.drawable.c_4_o_b, 478, 44);
            add(R.drawable.c_h_o_b, 168, 286);
            add(R.drawable.c_t_b, 754, 56);
            add(R.drawable.c_t_o_b, 726, 6);
            add(R.drawable.c_1_b, 564, 220);
            add(R.drawable.c_h_b, 168, 286);
            add(R.drawable.c_b, 36, 56);
            add(R.drawable.c_2_o_r, 262, 72);
            add(R.drawable.c_2_r, 338, 207);
            add(R.drawable.c_3_r, 716, 182);
            add(R.drawable.c_3_o_r, 725, 217);
            add(R.drawable.c_4_o_r, 477, 44);
            add(R.drawable.c_t_o_r, 725, 5);
            add(R.drawable.c_h_o_r, 167, 286);
            add(R.drawable.c_t_r, 754, 56);
            add(R.drawable.c_1_o_r, 570, 360);
            add(R.drawable.c_1_r, 564, 220);
            add(R.drawable.c_h_r, 168, 286);
            add(R.drawable.c_r, 36, 56);
            add(R.drawable.c_a_out, 697, 265);
            add(R.drawable.c_a_in, 325, 67);
            add(R.drawable.c_a_hit1, 169, 283);
            add(R.drawable.c_a_hit2, 169, 283);
            add(R.drawable.c_a_move, 61, 336);
            add(R.drawable.c_az1, 927, 0);
            add(R.drawable.c_az2, 920, 0);
            add(R.drawable.c_az4, 912, 0);
            add(R.drawable.c_az3, 919, 0);
            add(R.drawable.c_az6, 907, 0);
            add(R.drawable.c_az5, 907, 0);
        }

        void add(int id, int x, int y) {
            put(id, new Point(x, y));
        }
    }
}
