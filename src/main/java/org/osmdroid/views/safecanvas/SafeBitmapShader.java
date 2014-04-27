package org.osmdroid.views.safecanvas;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Matrix;

import org.osmdroid.views.overlay.Overlay;

/**
 * The SafeBitmapShader class is designed to work in conjunction with {@link SafeTranslatedCanvas}
 * to work around various Android issues with large canvases. For the two classes to work together,
 * call {@link #onDrawCycleStart} at the start of the {@link Overlay#drawSafe} method of your
 * {@link Overlay}. This will set the adjustment needed to draw your BitmapShader safely on the
 * canvas without any drawing distortion at high zoom levels and without any scrolling issues.
 *
 * @author Marc Kurtz
 * @see {@link ISafeCanvas}
 */
public class SafeBitmapShader extends BitmapShader {

    private final Matrix mMatrix = new Matrix();
    private final int mBitmapWidth;
    private final int mBitmapHeight;

    public SafeBitmapShader(Bitmap bitmap, TileMode tileX, TileMode tileY) {
        super(bitmap, tileX, tileY);
        mBitmapWidth = bitmap.getWidth();
        mBitmapHeight = bitmap.getHeight();
    }

    /**
     * This method <b>must</b> be called at the start of the {@link Overlay#drawSafe} draw cycle
     * method. This will adjust the BitmapShader to the current state of the {@link ISafeCanvas}
     * passed to it.
     */
    public void onDrawCycleStart(ISafeCanvas canvas) {
        mMatrix.setTranslate(canvas.getXOffset() % mBitmapWidth, canvas.getYOffset() % mBitmapHeight);
        this.setLocalMatrix(mMatrix);
    }
}
