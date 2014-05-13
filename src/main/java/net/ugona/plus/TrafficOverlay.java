package net.ugona.plus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.InputStream;

public class TrafficOverlay extends TilesOverlay {

    static final int TRAFFIC_ALPHA = 160;

    public TrafficOverlay(Context aContext) {
        super(createTileProvider(aContext), aContext);
        setLoadingBackgroundColor(Color.TRANSPARENT);
    }

    static MapTileProviderBase createTileProvider(Context aContext) {
        final String[] tiles_urls = {
                "http://jn0maps.mail.ru/tiles/newjams/%z/%y/%x.png",
                "http://jn1maps.mail.ru/tiles/newjams/%z/%y/%x.png",
                "http://jn2maps.mail.ru/tiles/newjams/%z/%y/%x.png",
                "http://jn3maps.mail.ru/tiles/newjams/%z/%y/%x.png",
                "http://jn4maps.mail.ru/tiles/newjams/%z/%y/%x.png",
                "http://jn5maps.mail.ru/tiles/newjams/%z/%y/%x.png"
        };

        ITileSource tileSource = new TileSource(aContext, tiles_urls);
        return new MapTileProviderBasic(aContext, tileSource);
    }

    @Override
    protected void draw(Canvas c, MapView osmv, boolean shadow) {
        super.draw(c, osmv, shadow);
    }

    static class TileSource extends XYTileSource {

        public TileSource(Context ctx, String[] baseUrl) {
            super("traffic", ResourceProxy.string.mapnik, 2, 17, (int) (256 * ctx.getResources().getDisplayMetrics().density), ".png", baseUrl);
        }

        @Override
        public String getTileURLString(MapTile aTile) {
            return getBaseUrl()
                    .replace("%x", aTile.getX() + "")
                    .replace("%y", aTile.getY() + "")
                    .replace("%z", aTile.getZoomLevel() + "");
        }

        @Override
        public Drawable getDrawable(InputStream aFileInputStream) throws LowMemoryException {
            Drawable res = super.getDrawable(aFileInputStream);
            res.setAlpha(TRAFFIC_ALPHA);
            return res;
        }

        @Override
        public Drawable getDrawable(String aFilePath) {
            Drawable res = super.getDrawable(aFilePath);
            res.setAlpha(TRAFFIC_ALPHA);
            return res;
        }
    }
}
