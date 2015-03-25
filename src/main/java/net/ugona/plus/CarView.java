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
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
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
    Paint pWhite;
    Paint pRed;
    Paint pBlue;
    String pkg;

    int frame;
    Handler handler;
    int animation;

    float pk;

    public CarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        resources = context.getResources();
        paint = new Paint();
        paint.setFlags(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        pRed = new Paint();
        pRed.setFlags(Paint.ANTI_ALIAS_FLAG);
        pRed.setColor(Color.rgb(0xC0, 0, 0));
        pBlue = new Paint();
        pBlue.setFlags(Paint.ANTI_ALIAS_FLAG);
        pBlue.setColor(resources.getColor(R.color.main));
        pWhite = new Paint();
        pWhite.setFlags(Paint.ANTI_ALIAS_FLAG);
        pWhite.setColor(Color.rgb(255, 255, 255));
        pkg = context.getPackageName();
        state = "";
        prefix = "c";
        handler = new Handler();
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        pk = metrics.densityDpi / 160f;
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
        String[] ext = state.split("\\|");
        String[] parts = ext[0].split(";");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == animation) {
                frame++;
                if (frame > 6)
                    frame = 1;
                part += frame;
            }
            int id = getResources().getIdentifier(part, "drawable", pkg);
            if (id == 0)
                continue;
            Bitmap bitmap = getBitmapSafely(resources, id, 0);
            Point p = pictures.get(id);
            RectF rect = new RectF(x + p.x * k, y + p.y * k, x + (p.x + bitmap.getWidth()) * k, y + (bitmap.getHeight() + p.y) * k);
            canvas.drawBitmap(bitmap, null, rect, paint);
            bitmap.recycle();
        }
        float radius = 20 * pk;
        float yPos = y + k * HEIGHT - radius - 4 * pk;
        float xPos = x + radius + 10 * pk;
        parts = ext[1].split(";");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            int id = getResources().getIdentifier(part, "drawable", pkg);
            if (id == 0)
                continue;
            boolean bRed = part.substring(0, 2).equals("r_");
            canvas.drawCircle(xPos, yPos, radius, bRed ? pRed : pBlue);
            canvas.drawCircle(xPos, yPos, 18 * pk, pWhite);
            Bitmap bitmap = getBitmapSafely(resources, id, 0);
            RectF rect = new RectF(xPos - radius, yPos - radius, xPos + radius, yPos + radius);
            canvas.drawBitmap(bitmap, null, rect, paint);
            bitmap.recycle();
            xPos += radius * 2 + 4 * pk;
        }
    }

    private Bitmap getBitmapSafely(Resources res, int id, int sampleSize) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true;
        options.inSampleSize = sampleSize;
        try {
            bitmap = BitmapFactory.decodeResource(res, id, options);
        } catch (OutOfMemoryError oom) {
            System.gc();
            bitmap = getBitmapSafely(res, id, sampleSize + 1);
        }

        return bitmap;
    }

    void update(CarState s) {
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
            parts.add(prefix + "_hit1");
        if (shock == 2)
            parts.add(prefix + "_hit2");
        if (s.isMove())
            parts.add(prefix + "_a_move");
        if (s.isAz()) {
            animation = parts.size();
            parts.add(prefix + "_az");
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
        if (s.isIgnition() && !az)
            parts.add(s.isGuard() ? "r_ignition" : "b_ignition");
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
            return;
        state = new_state;
        invalidate();
        next_frame();
    }

    void next_frame() {
        if (animation < 0)
            return;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                invalidate();
                next_frame();
            }
        }, 300);
    }

    static class Pictures extends HashMap<Integer, Point> {
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
            add(R.drawable.c_a_hit, 39, 183);
            add(R.drawable.c_a_hit2, 54, 197);
            add(R.drawable.c_a_move, 31, 306);
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
