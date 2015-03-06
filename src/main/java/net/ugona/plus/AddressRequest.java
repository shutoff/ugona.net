package net.ugona.plus;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.util.Locale;

abstract public class AddressRequest {

    static final String GOOGLE_URL = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$1,$2&sensor=false&language=$3";
    static final String OSM_URL = "https://nominatim.openstreetmap.org/reverse?lat=$1&lon=$2&osm_type=N&format=json&address_details=0&accept-language=$3";
    Request request;

    abstract void addressResult(String address);

    void getAddress(String map_type, double lat, double lng) {
        if (map_type.equals("Google")) {
            request = new GoogleRequest();
            request.exec(lat, lng);
            return;
        }
        request = new OsmRequest();
        request.exec(lat, lng);
    }

    abstract class Request extends HttpTask {
        abstract void exec(double lat, double lng);
    }

    class GoogleRequest extends Request {

        double latitude;
        double longitude;

        void exec(double lat, double lng) {
            latitude = lat;
            longitude = lng;
            execute(GOOGLE_URL, latitude, longitude, Locale.getDefault().getLanguage());
        }

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
                addressResult(null);
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
            String house = null;
            String postalCode = null;
            for (i = 0; i < components.size(); i++) {
                int n;
                JsonObject component = components.get(i).asObject();
                JsonArray types = component.get("types").asArray();
                for (n = 0; n < types.size(); n++) {
                    String type = types.get(n).asString();
                    if (type.equals("postal_code"))
                        postalCode = component.get("long_name").asString();
                    if (type.equals("street_number"))
                        house = component.get("long_name").asString();
                }
            }
            for (i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.equals(postalCode)) {
                    parts[i] = null;
                    continue;
                }
                if (part.equals(house)) {
                    parts[i] = null;
                    parts[i - 1] += ",\u00A0" + house;
                    continue;
                }
                if (part.equals("Unnamed Road")) {
                    parts[i] = null;
                    continue;
                }
            }
            String p = null;
            for (i = 0; i < parts.length; i++) {
                if (parts[i] == null)
                    continue;
                if (p == null) {
                    p = parts[i];
                    continue;
                }
                p += ", " + parts[i];
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
        void exec(double lat, double lon) {
            execute(OSM_URL, lat, lon, Locale.getDefault().getLanguage());
        }

        @Override
        void result(JsonObject res) throws ParseException {
            JsonObject address = res.get("address").asObject();
            String[] parts = res.get("display_name").asString().split(", ");
            try {
                String house_number = address.get("house_number").asString();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].equals(house_number)) {
                        parts[i + 1] += ",\u00A0" + house_number;
                        parts[i] = null;
                        break;
                    }
                }
            } catch (Exception ex) {
                // ignore
            }
            String result = null;
            for (int i = 0; i < parts.length - 2; i++) {
                if (parts[i] == null)
                    continue;
                if (result == null) {
                    result = parts[i];
                    continue;
                }
                result += ", " + parts[i];
            }
            addressResult(result);
        }

        @Override
        void error() {
            addressResult(null);
        }
    }

}
