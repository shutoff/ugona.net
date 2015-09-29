package net.ugona.plus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CarView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    final static int XC_LEFT = 20;
    final static int YC_BOTTOM = 15;

    Paint pWhite;
    Paint pRed;
    Paint pBlue;

    int frame;
    Handler handler;

    CarImage carImage;
    SurfaceHolder holder;
    Thread thread;

    boolean running;
    Object lock;

    float pk;

    public CarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        pRed = new Paint();
        pRed.setFlags(Paint.ANTI_ALIAS_FLAG);
        pRed.setColor(Color.rgb(0xC0, 0, 0));
        pBlue = new Paint();
        pBlue.setFlags(Paint.ANTI_ALIAS_FLAG);
        pBlue.setColor(ContextCompat.getColor(context, R.color.main));
        pWhite = new Paint();
        pWhite.setFlags(Paint.ANTI_ALIAS_FLAG);
        pWhite.setColor(Color.rgb(255, 255, 255));
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        pk = metrics.densityDpi / 160f;
        handler = new Handler();
        holder = getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);
        setZOrderOnTop(true);
        holder.addCallback(this);
        lock = new Object();
    }

    void draw() {
        if ((carImage == null) || (carImage.theme == null))
            return;
        if ((getWidth() == 0) || (getHeight() == 0))
            return;
        float add_x = XC_LEFT * pk;
        float add_y = YC_BOTTOM * pk;
        float k = (getWidth() + add_x) / carImage.theme.width;
        float x = 0;
        float y = 0;
        float h = carImage.theme.height * k;
        if (h > getHeight() + add_y) {
            k = (getHeight() + add_y) / carImage.theme.height;
            float w = k * carImage.theme.width;
            x = (getWidth() + add_x - w) / 2.f;
        } else {
            y = (getHeight() + add_y - h) / 2.f;
        }
        x -= add_x;
        String[] ext = carImage.state.split("\\|");
        String[] parts = ext[0].split(";");
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas();
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (i == carImage.animation) {
                    frame++;
                    if (frame > 6)
                        frame = 1;
                    part += frame;
                }
                Theme.Pict pict = carImage.theme.get(part);
                if (pict == null)
                    continue;
                Bitmap bitmap = pict.bitmap;
                RectF rect = new RectF(x + pict.x * k, y + pict.y * k, x + (pict.x + bitmap.getWidth()) * k, y + (bitmap.getHeight() + pict.y) * k);
                canvas.drawBitmap(bitmap, null, rect, carImage.paint);
                bitmap.recycle();
            }
        } finally {
            if (canvas != null)
                holder.unlockCanvasAndPost(canvas);
        }
    }

    void update(CarState s, CarConfig config) {
        if ((carImage != null) && !carImage.name.equals(config.getTheme()))
            carImage = null;
        if (carImage == null)
            carImage = new CarImage(getContext(), config.getTheme());
        if (!carImage.update(s, false))
            return;
        sendUpdate();
        next_frame();
    }

    void next_frame() {
        if (carImage.animation < 0)
            return;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendUpdate();
                next_frame();
            }
        }, 300);
    }

    void sendUpdate() {
        synchronized (lock) {
            lock.notify();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        thread = new Thread(this);
        running = true;
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        running = false;
        sendUpdate();
        try {
            notify();
        } catch (Exception ex) {
            // ignore
        }
        for (; ; ) {
            try {
                thread.join();
                return;
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    @Override
    public void run() {
        while (running) {
            draw();
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
