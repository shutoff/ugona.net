package net.ugona.plus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

public class CarView extends View {

    final static int XC_LEFT = 20;
    final static int YC_BOTTOM = 15;

    Paint pWhite;
    Paint pRed;
    Paint pBlue;

    int frame;
    Handler handler;

    CarImage carImage;

    float pk;

    public CarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        carImage = new CarImage(context);
        pRed = new Paint();
        pRed.setFlags(Paint.ANTI_ALIAS_FLAG);
        pRed.setColor(Color.rgb(0xC0, 0, 0));
        pBlue = new Paint();
        pBlue.setFlags(Paint.ANTI_ALIAS_FLAG);
        pBlue.setColor(carImage.resources.getColor(R.color.main));
        pWhite = new Paint();
        pWhite.setFlags(Paint.ANTI_ALIAS_FLAG);
        pWhite.setColor(Color.rgb(255, 255, 255));
        DisplayMetrics metrics = carImage.resources.getDisplayMetrics();
        pk = metrics.densityDpi / 160f;
        handler = new Handler();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if ((getWidth() == 0) || (getHeight() == 0))
            return;
        float add_x = XC_LEFT * pk;
        float add_y = YC_BOTTOM * pk;
        float k = (getWidth() + add_x) / carImage.WIDTH;
        float x = 0;
        float y = 0;
        float h = carImage.HEIGHT * k;
        if (h > getHeight() + add_y) {
            k = (getHeight() + add_y) / carImage.HEIGHT;
            float w = k * carImage.WIDTH;
            x = (getWidth() + add_x - w) / 2.f;
        } else {
            y = (getHeight() + add_y - h) / 2.f;
        }
        x -= add_x;
        String[] ext = carImage.state.split("\\|");
        String[] parts = ext[0].split(";");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == carImage.animation) {
                frame++;
                if (frame > 6)
                    frame = 1;
                part += frame;
            }
            int id = carImage.getBitmapId(part);
            if (id == 0)
                continue;
            Bitmap bitmap = carImage.getBitmap(id);
            Point p = carImage.pictures.get(id);
            RectF rect = new RectF(x + p.x * k, y + p.y * k, x + (p.x + bitmap.getWidth()) * k, y + (bitmap.getHeight() + p.y) * k);
            canvas.drawBitmap(bitmap, null, rect, carImage.paint);
            bitmap.recycle();
        }

    }

    void update(CarState s) {
        if (!carImage.update(s))
            return;
        invalidate();
        next_frame();
    }

    void next_frame() {
        if (carImage.animation < 0)
            return;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                invalidate();
                next_frame();
            }
        }, 300);
    }


}
