package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;
import java.util.Locale;

public abstract class Address {

    abstract void result(String address);

    void get(final Context context, final double lat, final double lng) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String p = Locale.getDefault().getLanguage();
        if (preferences.getString(Names.MAP_TYPE, "").equals("OSM"))
            p += "_";
        final String param = p;
        String[] conditions = {
                param,
                (lat - 0.001) + "",
                (lat + 0.001) + "",
                (lng - 0.001) + "",
                (lng + 0.001) + ""
        };
        List<AddressRecord> res = AddressRecord.find(AddressRecord.class, "(Param = ?) AND (Lat BETWEEN ? AND ?) AND (Lng BETWEEN ? AND ?)", conditions);

        for (AddressRecord addr : res) {
            double distance = calc_distance(lat, lng, addr.lat, addr.lng);
            if (distance < 80) {
                result(addr.addr);
                return;
            }
        }
        AddressRequest request = new AddressRequest() {
            @Override
            void addressResult(String address) {
                if (address != null) {
                    AddressRecord addr = new AddressRecord();
                    addr.lat = lat;
                    addr.lng = lng;
                    addr.addr = address;
                    addr.param = param;
                    addr.save();
                }
                result(address);
            }
        };
        request.getAddress(preferences, lat, lng);
    }

    static String getAddress(Context context, final double lat, final double lng) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String p = Locale.getDefault().getLanguage();
        if (preferences.getString(Names.MAP_TYPE, "").equals("OSM"))
            p += "_";
        final String param = p;
        String[] conditions = {
                param,
                (lat - 0.001) + "",
                (lat + 0.001) + "",
                (lng - 0.001) + "",
                (lng + 0.001) + ""
        };
        List<AddressRecord> res = AddressRecord.find(AddressRecord.class, "(Param = ?) AND (Lat BETWEEN ? AND ?) AND (Lng BETWEEN ? AND ?)", conditions);
        for (AddressRecord addr : res) {
            double distance = calc_distance(lat, lng, addr.lat, addr.lng);
            if (distance < 80)
                return addr.addr;
        }
        return null;
    }

    static final double D2R = 0.017453; // Константа для преобразования градусов в радианы
    static final double a = 6378137.0; // Основные полуоси
    static final double e2 = 0.006739496742337; // Квадрат эксцентричности эллипсоида

    static double calc_distance(double lat1, double lon1, double lat2, double lon2) {

        if ((lat1 == lat2) && (lon1 == lon2))
            return 0;

        double fdLambda = (lon1 - lon2) * D2R;
        double fdPhi = (lat1 - lat2) * D2R;
        double fPhimean = ((lat1 + lat2) / 2.0) * D2R;

        double fTemp = 1 - e2 * (Math.pow(Math.sin(fPhimean), 2));
        double fRho = (a * (1 - e2)) / Math.pow(fTemp, 1.5);
        double fNu = a / (Math.sqrt(1 - e2 * (Math.sin(fPhimean) * Math.sin(fPhimean))));

        double fz = Math.sqrt(Math.pow(Math.sin(fdPhi / 2.0), 2) +
                Math.cos(lat2 * D2R) * Math.cos(lat1 * D2R) * Math.pow(Math.sin(fdLambda / 2.0), 2));
        fz = 2 * Math.asin(fz);

        double fAlpha = Math.cos(lat1 * D2R) * Math.sin(fdLambda) * 1 / Math.sin(fz);
        fAlpha = Math.asin(fAlpha);

        double fR = (fRho * fNu) / ((fRho * Math.pow(Math.sin(fAlpha), 2)) + (fNu * Math.pow(Math.cos(fAlpha), 2)));

        return fz * fR;
    }

}
