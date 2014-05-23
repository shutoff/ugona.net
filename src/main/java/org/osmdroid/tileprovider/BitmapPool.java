package org.osmdroid.tileprovider;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import java.util.LinkedList;

public class BitmapPool {
    private static BitmapPool sInstance;
    final LinkedList<Bitmap> mPool = new LinkedList<Bitmap>();

    public static BitmapPool getInstance() {
        if (sInstance == null)
            sInstance = new BitmapPool();

        return sInstance;
    }

    public void returnDrawableToPool(ReusableBitmapDrawable drawable) {
        Bitmap b = drawable.tryRecycle();
        if (b != null && b.isMutable())
            synchronized (mPool) {
                mPool.addLast(b);
            }
    }

    public void applyReusableOptions(final BitmapFactory.Options aBitmapOptions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            aBitmapOptions.inBitmap = obtainBitmapFromPool();
            aBitmapOptions.inSampleSize = 1;
            aBitmapOptions.inMutable = true;
        }
    }

    public Bitmap obtainBitmapFromPool() {
        synchronized (mPool) {
            if (mPool.isEmpty()) {
                return null;
            } else {
                final Bitmap bitmap = mPool.removeFirst();
                if (bitmap.isRecycled()) {
                    return obtainBitmapFromPool(); // recurse
                } else {
                    return bitmap;
                }
            }
        }
    }

    public Bitmap obtainSizedBitmapFromPool(final int aWidth, final int aHeight) {
        synchronized (mPool) {
            if (mPool.isEmpty()) {
                return null;
            } else {
                for (final Bitmap bitmap : mPool) {
                    if (bitmap.isRecycled()) {
                        mPool.remove(bitmap);
                        return obtainSizedBitmapFromPool(aWidth, aHeight); // recurse to prevent ConcurrentModificationException
                    } else if (bitmap.getWidth() == aWidth && bitmap.getHeight() == aHeight) {
                        mPool.remove(bitmap);
                        return bitmap;
                    }
                }
            }
        }

        return null;
    }

    public void clearBitmapPool() {
        synchronized (sInstance.mPool) {
            while (!sInstance.mPool.isEmpty()) {
                Bitmap bitmap = sInstance.mPool.remove();
                bitmap.recycle();
            }
        }
    }
}
