package net.ugona.plus;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapView;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.MapTileDownloader;
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck;
import org.osmdroid.tileprovider.modules.TileWriter;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapFragment extends Fragment {

    IMapView mMapView;
    ResourceProxy mResourceProxy;

    MyLocationNewOverlay mLocationOverlay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        RelativeLayout rl = new RelativeLayout(inflater.getContext());
        rl.setBackgroundColor(getResources().getColor(R.color.caldroid_gray));

        final IRegisterReceiver registerReceiver = new SimpleRegisterReceiver(inflater.getContext().getApplicationContext());
        final String[] tiles_urls = {
                "http://otile1.mqcdn.com/tiles/1.0.0/osm/"
        };
        final ITileSource tileSource = new XYTileSource("mqcdn", ResourceProxy.string.mapnik, 1, 18, 256, ".png", tiles_urls);
        final TileWriter tileWriter = new TileWriter();
        final MapTileFilesystemProvider fileSystemProvider = new MapTileFilesystemProvider(registerReceiver, tileSource);

        final NetworkAvailabliltyCheck networkAvailabliltyCheck = new NetworkAvailabliltyCheck(inflater.getContext().getApplicationContext());
        final MapTileDownloader downloaderProvider = new MapTileDownloader(tileSource, tileWriter, networkAvailabliltyCheck);
        final MapTileProviderArray tileProviderArray = new MapTileProviderArray(tileSource, registerReceiver, new MapTileModuleProviderBase[]{fileSystemProvider, downloaderProvider});

        mResourceProxy = new ResourceProxyImpl(inflater.getContext().getApplicationContext());
        org.osmdroid.views.MapView mapView = new org.osmdroid.views.MapView(inflater.getContext(), 256, mResourceProxy, tileProviderArray);
        mapView.setUseSafeCanvas(true);
        mapView.setId(R.id.map);

        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(inflater.getContext()),
                mapView, mResourceProxy);

        mMapView = mapView;
        mMapView.setBackgroundColor(getResources().getColor(R.color.caldroid_gray));

        mapView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rl.addView(mapView);

        return rl;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MapActivity activity = (MapActivity) getActivity();
        activity.initMap(mMapView.getController());
    }

    @Override
    public void onResume() {
        super.onResume();
        mLocationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        mLocationOverlay.disableMyLocation();
        super.onPause();
    }
}
