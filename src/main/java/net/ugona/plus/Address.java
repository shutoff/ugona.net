package net.ugona.plus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Locale;

public class Address {

    static Request addr_request;

    static class Request extends AddressRequest {

        String id;
        String latitude;
        String longitude;
        Context context;
        SharedPreferences preferences;

        Request(Context ctx, String car_id, String lat, String lng) {
            context = ctx;
            id = car_id;
            latitude = lat;
            longitude = lng;
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
            getAddress(preferences, lat, lng);
        }

        @Override
        void addressResult(String[] parts) {
            addr_request = null;
            if (parts == null)
                return;

            String address = parts[0];
            for (int i = 1; i < parts.length - 1; i++) {
                address += ", " + parts[i];
            }

            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(Names.ADDR_LAT + id, latitude);
            ed.putString(Names.ADDR_LNG + id, longitude);
            ed.putString(Names.ADDRESS + id, address);
            ed.putString(Names.ADDR_LANG + id, Locale.getDefault().getLanguage());
            ed.commit();

            try {
                Intent intent = new Intent(FetchService.ACTION_UPDATE);
                intent.putExtra(Names.ID, id);
                context.sendBroadcast(intent);
            } catch (Exception e) {
                // ignore
            }
        }
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

    static String getAddress(Context context, String car_id) {
        try {
            String result = "";
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            double lat1 = Double.parseDouble(preferences.getString(Names.LATITUDE + car_id, "0"));
            double lng1 = Double.parseDouble(preferences.getString(Names.LONGITUDE + car_id, "0"));
            try {
                double lat2 = Double.parseDouble(preferences.getString(Names.ADDR_LAT + car_id, "0"));
                double lng2 = Double.parseDouble(preferences.getString(Names.ADDR_LNG + car_id, "0"));
                double distance = calc_distance(lat1, lng1, lat2, lng2);
                result = preferences.getString(Names.ADDRESS + car_id, "");
                if (!preferences.getString(Names.ADDR_LANG + car_id, "").equals(Locale.getDefault().getLanguage()))
                    result = "";
                if (distance > 250)
                    result = "";
                if ((distance < 80) && (result.length() > 0))
                    return result;
            } catch (Exception ex) {
                // ignore
            }
            if ((addr_request != null) && addr_request.id.equals(car_id))
                return result;
            addr_request = new Request(context, car_id, lat1 + "", lng1 + "");
        } catch (Exception ex) {
            // ignore
        }
        return "";
    }
}
