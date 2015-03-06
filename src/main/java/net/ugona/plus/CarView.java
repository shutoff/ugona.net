package net.ugona.plus;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.HashMap;
import java.util.Vector;

public class CarView extends View {

    final static float WIDTH = 1080;
    final static float HEIGHT = 750;
    static Pictures pictures = new Pictures();
    String state;
    String prefix;
    Resources resources;
    Paint paint;
    String pkg;

    public CarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        resources = context.getResources();
        paint = new Paint();
        paint.setFlags(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        pkg = context.getPackageName();
        state = "";
        prefix = "c";
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float x = 0;
        float y = 0;
        float k = canvas.getWidth() / WIDTH;
        float h = HEIGHT * k;
        if (h > canvas.getHeight()) {
            k = canvas.getHeight() / HEIGHT;
            float w = k * WIDTH;
            x = (canvas.getWidth() - w) / 2.f;
        } else {
            y = (canvas.getHeight() - h) / 2.f;
        }
        String[] parts = state.split(";");
        for (String part : parts) {
            int id = getResources().getIdentifier(part, "drawable", pkg);
            if (id == 0)
                continue;
            Bitmap bitmap = BitmapFactory.decodeResource(resources, id);
            Point p = pictures.get(id);
            RectF rect = new RectF(x + p.x * k, y + p.y * k, x + (p.x + bitmap.getWidth()) * k, y + (bitmap.getHeight() + p.y) * k);
            canvas.drawBitmap(bitmap, null, rect, paint);

        }
    }

    void update(CarState s) {
        Vector<String> parts = new Vector<String>();
        boolean guard = s.isGuard();
        long guard_time = s.getGuard_time();
        long card_time = s.getCard();
        boolean card = false;
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
        if (s.isIgnition() && !az)
            parts.add(prefix + "_m5");

        String new_state = null;
        for (String part : parts) {
            if (new_state == null) {
                new_state = part;
                continue;
            }
            new_state += ";" + part;
        }
        if (new_state.equals(state))
            return;
        state = new_state;
        invalidate();
    }

    static class Pictures extends HashMap<Integer, Point> {
        Pictures() {
            add(R.drawable.c_1, 563, 221);
            add(R.drawable.c_1_o, 569, 361);
            add(R.drawable.c_2, 337, 208);
            add(R.drawable.c_3, 715, 183);
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
            add(R.drawable.c_3_b, 716, 182);
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
            add(R.drawable.c_a_hit, 39, 183);
            add(R.drawable.c_a_hit2, 54, 197);
            add(R.drawable.c_a_move, 32, 306);
            add(R.drawable.c_a_slope, 31, 25);
            add(R.drawable.c_m1, 631, 19);
            add(R.drawable.c_m2, 631, 19);
            add(R.drawable.c_m3, 651, 64);
            add(R.drawable.c_m4, 651, 64);
            add(R.drawable.c_m5, 427, 131);
            add(R.drawable.c_m6, 52, 164);

        }

        void add(int id, int x, int y) {
            put(id, new Point(x, y));
        }
    }

}
