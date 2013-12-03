package net.ugona.plus;

import android.content.SharedPreferences;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.util.Locale;

abstract public class AddressRequest {

    static final String GOOGLE_URL = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$1,$2&sensor=false&language=$3";
    static final String OSM_URL = "http://nominatim.openstreetmap.org/reverse?lat=$1&lon=$2&osm_type=N&format=json&address_details=0&accept-language=$3";

    abstract void addressResult(String[] address);

    Request request;

    void getAddress(SharedPreferences preferences, String lat, String lng) {
        if (preferences.getString("map_type", "").equals("OSM")) {
            request = new OsmRequest();
            request.exec(lat, lng);
            return;
        }
        request = new GoogleRequest();
        request.exec(lat, lng);
    }

    abstract class Request extends HttpTask {
        abstract void exec(String lat, String lng);
    }

    class GoogleRequest extends Request {

        void exec(String lat, String lng) {
            latitude = lat;
            longitude = lng;
            execute(GOOGLE_URL, latitude, longitude, Locale.getDefault().getLanguage());
        }

        String latitude;
        String longitude;

        @Override
        void result(JsonObject data) throws ParseException {

            JsonArray res = data.get("results").asArray();
            if (res.size() == 0) {
                String status = data.get("status").asString();
                if ((status != null) && status.equals("OVER_QUERY_LIMIT")) {
                    request = new GoogleRequest();
                    request.pause = 1000;
                    request.exec(latitude, longitude);
                    return;
                }
            }

            if (res.size() == 0) {
                String[] p = new String[1];
                p[0] = latitude + "," + longitude;
                addressResult(p);
                return;
            }

            int i;
            for (i = 0; i < res.size(); i++) {
                JsonObject addr = res.get(i).asObject();
                JsonArray types = addr.get("types").asArray();
                int n;
                for (n = 0; n < types.size(); n++) {
                    if (types.get(n).asString().equals("street_address"))
                        break;
                }
                if (n < types.size())
                    break;
            }
            if (i >= res.size())
                i = 0;

            JsonObject addr = res.get(i).asObject();
            String[] parts = addr.get("formatted_address").asString().split(", ");
            JsonArray components = addr.get("address_components").asArray();
            for (i = 0; i < components.size(); i++) {
                JsonObject component = components.get(i).asObject();
                JsonArray types = component.get("types").asArray();
                int n;
                for (n = 0; n < types.size(); n++) {
                    if (types.get(n).asString().equals("postal_code"))
                        break;
                }
                if (n >= types.size())
                    continue;
                String name = component.get("long_name").asString();
                for (n = 0; n < parts.length; n++) {
                    if (name.equals(parts[n]))
                        parts[n] = null;
                }
            }
            int to = 0;
            for (i = 0; i < parts.length; i++) {
                if (parts[i] == null)
                    continue;
                to++;
            }
            String[] p = new String[to];
            to = 0;
            for (i = 0; i < parts.length; i++) {
                if (parts[i] == null)
                    continue;
                p[to++] = parts[i];
            }
            addressResult(p);
        }

        @Override
        void error() {
            addressResult(null);
        }
    }

    class OsmRequest extends Request {

        @Override
        void exec(String lat, String lon) {
            execute(OSM_URL, lat, lon, Locale.getDefault().getLanguage());
        }

        @Override
        void result(JsonObject res) throws ParseException {
            JsonObject address = res.get("address").asObject();
            String[] parts = res.get("display_name").asString().split(", ");
            String[] result = new String[parts.length - 2];
            for (int i = 0; i < result.length; i++) {
                result[i] = parts[i];
            }
            try {
                String house_number = address.get("house_number").asString();
                for (int i = 0; i < result.length - 1; i++) {
                    if (result[i].equals(house_number)) {
                        result[i] = result[i + 1];
                        result[i + 1] = house_number;
                        break;
                    }
                }
            } catch (Exception ex) {
                // ignore
            }
            addressResult(result);
        }

        @Override
        void error() {
            addressResult(null);
        }
    }
}
