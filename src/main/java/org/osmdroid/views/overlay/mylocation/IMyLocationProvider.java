package org.osmdroid.views.overlay.mylocation;

import android.location.Location;

public interface IMyLocationProvider {
    boolean startLocationProvider(IMyLocationConsumer myLocationConsumer, boolean enable_gps, boolean enable_net);

    void stopLocationProvider();

    Location getLastKnownLocation();
}
