package org.osmdroid.tileprovider.tilesource;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;

import net.ugona.plus.State;

import org.osmdroid.ResourceProxy;
import org.osmdroid.ResourceProxy.string;
import org.osmdroid.tileprovider.BitmapPool;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.ReusableBitmapDrawable;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Random;

public abstract class BitmapTileSourceBase implements ITileSource,
        OpenStreetMapTileProviderConstants {

    private static final Logger logger = LoggerFactory.getLogger(BitmapTileSourceBase.class);

    private static int globalOrdinal = 0;
    protected final String mName;
    protected final String mImageFilenameEnding;
    protected final Random random = new Random();
    private final int mMinimumZoomLevel;
    private final int mMaximumZoomLevel;
    private final int mOrdinal;
    private final int mTileSizePixels;

    private final string mResourceId;

    /**
     * Constructor
     *
     * @param aName                a human-friendly name for this tile source
     * @param aResourceId          resource id used to get the localized name of this tile source
     * @param aZoomMinLevel        the minimum zoom level this tile source can provide
     * @param aZoomMaxLevel        the maximum zoom level this tile source can provide
     * @param aTileSizePixels      the tile size in pixels this tile source provides
     * @param aImageFilenameEnding the file name extension used when constructing the filename
     */
    public BitmapTileSourceBase(final String aName, final string aResourceId,
                                final int aZoomMinLevel, final int aZoomMaxLevel, final int aTileSizePixels,
                                final String aImageFilenameEnding) {
        mResourceId = aResourceId;
        mOrdinal = globalOrdinal++;
        mName = aName;
        mMinimumZoomLevel = aZoomMinLevel;
        mMaximumZoomLevel = aZoomMaxLevel;
        mTileSizePixels = aTileSizePixels;
        mImageFilenameEnding = aImageFilenameEnding;
    }

    @Override
    public int ordinal() {
        return mOrdinal;
    }

    @Override
    public String name() {
        return mName;
    }

    public String pathBase() {
        return mName;
    }

    public String imageFilenameEnding() {
        return mImageFilenameEnding;
    }

    @Override
    public int getMinimumZoomLevel() {
        return mMinimumZoomLevel;
    }

    @Override
    public int getMaximumZoomLevel() {
        return mMaximumZoomLevel;
    }

    @Override
    public int getTileSizePixels() {
        return mTileSizePixels;
    }

    @Override
    public String localizedName(final ResourceProxy proxy) {
        return proxy.getString(mResourceId);
    }

    @Override
    public Drawable getDrawable(final String aFilePath) {
        try {
            // default implementation will load the file as a bitmap and create
            // a BitmapDrawable from it
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            BitmapPool.getInstance().applyReusableOptions(bitmapOptions);
            final Bitmap bitmap = BitmapFactory.decodeFile(aFilePath, bitmapOptions);
            if (bitmap != null) {
                if ((bitmap.getWidth() != this.getTileSizePixels()) || (bitmap.getHeight() != this.getTileSizePixels()))
                    return null;
                return new ReusableBitmapDrawable(bitmap);
            } else {
                // if we couldn't load it then it's invalid - delete it
                try {
                    new File(aFilePath).delete();
                } catch (final Throwable e) {
                    logger.error("Error deleting invalid file: " + aFilePath, e);
                }
            }
        } catch (final OutOfMemoryError e) {
            logger.error("OutOfMemoryError loading bitmap: " + aFilePath);
            System.gc();
        }
        return null;
    }

    @Override
    public String getTileRelativeFilenameString(final MapTile tile) {
        final StringBuilder sb = new StringBuilder();
        sb.append(pathBase());
        sb.append('/');
        sb.append(tile.getZoomLevel());
        sb.append('/');
        sb.append(tile.getX());
        sb.append('/');
        sb.append(tile.getY());
        sb.append(imageFilenameEnding());
        return sb.toString();
    }

    @Override
    public Drawable getDrawable(final InputStream aFileInputStream) throws LowMemoryException {
        for (; ; ) {
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            BitmapPool.getInstance().applyReusableOptions(bitmapOptions);
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeStream(aFileInputStream, null, bitmapOptions);
            } catch (final IllegalArgumentException e) {
                State.appendLog("Illegal state");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    if (bitmapOptions.inBitmap == null)
                        return null;
                    continue;
                }
                return null;
            } catch (final OutOfMemoryError e) {
                State.appendLog("Out of memory");
                System.gc();
                if (!BitmapPool.getInstance().clearBitmapPool())
                    return null;
                continue;
            }
            if (bitmap != null) {
                if ((bitmap.getWidth() != this.getTileSizePixels()) || (bitmap.getHeight() != this.getTileSizePixels()))
                    return null;
                return new ReusableBitmapDrawable(bitmap);
            }
            return null;
        }
    }

    public final class LowMemoryException extends Exception {
        private static final long serialVersionUID = 146526524087765134L;

        public LowMemoryException(final String pDetailMessage) {
            super(pDetailMessage);
        }

        public LowMemoryException(final Throwable pThrowable) {
            super(pThrowable);
        }
    }
}
