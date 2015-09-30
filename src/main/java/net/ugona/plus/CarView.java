package net.ugona.plus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ImageView;

public class CarView extends ImageView {

    final static int XC_LEFT = 20;
    final static int YC_BOTTOM = 15;
    static Bitmap bmpEmpty;
    Paint pWhite;
    Paint pRed;
    Paint pBlue;
    int frame;
    Handler handler;
    CarImage carImage;
    float pk;
    boolean bDrawing;
    boolean bUpdate;
    Bitmap bmpImage;

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
        setScaleType(ScaleType.CENTER_INSIDE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        sendUpdate();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (bmpImage != null) {
            if (bmpEmpty == null) {
                bmpEmpty = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmpEmpty);
                c.drawColor(Color.TRANSPARENT);
            }
            setImageBitmap(bmpEmpty);
            bmpImage.recycle();
        }
        super.onDetachedFromWindow();
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

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        sendUpdate();
    }

    void sendUpdate() {
        if ((getWidth() == 0) || (getHeight() == 0))
            return;
        if ((carImage == null) || (carImage.theme == null))
            return;

        if (bDrawing) {
            bUpdate = true;
            return;
        }

        bUpdate = false;
        if (bmpImage == null) {
            bmpImage = createBitmap(getWidth(), getHeight());
            Bitmap bitmap = bmpImage;
            if (bitmap == null) {
                if (bmpEmpty == null) {
                    bmpEmpty = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bmpEmpty);
                    c.drawColor(Color.TRANSPARENT);
                }
                bitmap = bmpEmpty;
            }
            setImageBitmap(bitmap);
            return;
        }

        bDrawing = true;

        AsyncTask<Integer, Void, Bitmap> task = new AsyncTask<Integer, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Integer... params) {
                return createBitmap(params[0], params[1]);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bmpImage != null)
                    bmpImage.recycle();
                bmpImage = bitmap;
                if (bitmap == null) {
                    if (bmpEmpty == null) {
                        bmpEmpty = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                        Canvas c = new Canvas(bmpEmpty);
                        c.drawColor(Color.TRANSPARENT);
                    }
                    bitmap = bmpEmpty;
                }
                setImageBitmap(bitmap);
                bDrawing = false;
                if (bUpdate)
                    sendUpdate();
            }
        };
        task.execute(getWidth(), getHeight());
    }

    Bitmap createBitmap(int width, int height) {
        if ((width == 0) || (height == 0))
            return null;
        if ((carImage == null) || (carImage.theme == null))
            return null;
        try {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.TRANSPARENT);

            float add_x = XC_LEFT * pk;
            float add_y = YC_BOTTOM * pk;
            float k = (width + add_x) / carImage.theme.width;
            float x = 0;
            float y = 0;
            float h = carImage.theme.height * k;
            if (h > height + add_y) {
                k = (height + add_y) / carImage.theme.height;
                float w = k * carImage.theme.width;
                x = (width + add_x - w) / 2.f;
            } else {
                y = (height + add_y - h) / 2.f;
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
                Theme.Pict pict = carImage.theme.get(part);
                if (pict == null)
                    continue;
                Bitmap src = pict.bitmap;
                RectF rect = new RectF(x + pict.x * k, y + pict.y * k, x + (pict.x + src.getWidth()) * k, y + (src.getHeight() + pict.y) * k);
                canvas.drawBitmap(src, null, rect, carImage.paint);
                src.recycle();
            }
            return bitmap;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

}
