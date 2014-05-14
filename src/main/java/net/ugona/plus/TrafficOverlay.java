package net.ugona.plus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.modules.MapTileDownloader;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.InputStream;

public class TrafficOverlay extends TilesOverlay {

    static final int TRAFFIC_ALPHA = 160;

    public TrafficOverlay(net.ugona.plus.MapView mapView, Context aContext) {
        super(createTileProvider(mapView, aContext), aContext);
        setLoadingBackgroundColor(Color.TRANSPARENT);
    }

    static MapTileProviderBase createTileProvider(MapView mapView, Context aContext) {
        final String[] tiles_urls = {
                "http://jn0maps.mail.ru/tiles/newjams/%z/%y/%x.png",
                "http://jn1maps.mail.ru/tiles/newjams/%z/%y/%x.png",
                "http://jn2maps.mail.ru/tiles/newjams/%z/%y/%x.png",
                "http://jn3maps.mail.ru/tiles/newjams/%z/%y/%x.png",
                "http://jn4maps.mail.ru/tiles/newjams/%z/%y/%x.png",
                "http://jn5maps.mail.ru/tiles/newjams/%z/%y/%x.png"
        };

        ITileSource tileSource = new TileSource(aContext, tiles_urls);
        final IRegisterReceiver registerReceiver = new SimpleRegisterReceiver(aContext.getApplicationContext());

        final NetworkAvailabliltyCheck networkAvailabliltyCheck = new NetworkAvailabliltyCheck(aContext.getApplicationContext());
        final MapTileDownloader downloaderProvider = new MapTileDownloader(tileSource, null, networkAvailabliltyCheck);
        MapTileProviderBase providerBase = new MapTileProviderArray(tileSource, registerReceiver, new MapTileModuleProviderBase[]{downloaderProvider});
        providerBase.setTileRequestCompleteHandler(mapView.getTileRequestCompleteHandler());
        return providerBase;
    }

    @Override
    protected void draw(Canvas c, MapView osmv, boolean shadow) {
        super.draw(c, osmv, shadow);
    }

    @Override
    public boolean isDrawingShadowLayer() {
        return false;
    }

    static class TileSource extends XYTileSource {

        public TileSource(Context ctx, String[] baseUrl) {
            super("traffic", ResourceProxy.string.mapnik, 2, 17, 256, ".png", baseUrl);
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
            if (res != null)
                res.setAlpha(TRAFFIC_ALPHA);
            return res;
        }

        @Override
        public Drawable getDrawable(String aFilePath) {
            Drawable res = super.getDrawable(aFilePath);
            if (res == null)
                return null;
            res.setAlpha(TRAFFIC_ALPHA);
            return res;
        }
    }
}
