package net.ugona.plus;

import android.content.SharedPreferences;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.util.Locale;

abstract public class AddressRequest {

    static final String GOOGLE_URL = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$1,$2&sensor=false&language=$3";
    static final String OSM_URL = "https://nominatim.openstreetmap.org/reverse?lat=$1&lon=$2&osm_type=N&format=json&address_details=0&accept-language=$3";
    static final String BING_URL = "http://dev.virtualearth.net/REST/v1/Locations/$1,$2?o=json&key=Avl_WlFuKVJbmBFOcG3s4A2xUY1DM2LFYbvKTcNfvIhJF7LqbVW-VsIE4IJQB0Nc&culture=$3";
    static final String YANDEX_URL = "http://geocode-maps.yandex.ru/1.x/?geocode=$2,$1&format=json&lang=$3";
    Request request;

    abstract void addressResult(String address);

    void getAddress(SharedPreferences preferences, double lat, double lng) {
        if (preferences.getString("map_type", "").equals("OSM")) {
            request = new OsmRequest();
            request.exec(lat, lng);
            return;
        }
        if (preferences.getString("map_type", "").equals("Bing")) {
            request = new BingRequest();
            request.exec(lat, lng);
            return;
        }
        if (preferences.getString("map_type", "").equals("Yandex")) {
            request = new YandexRequest();
            request.exec(lat, lng);
            return;
        }
        request = new GoogleRequest();
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
                        parts[i] = parts[i + 1];
                        parts[i + 1] = house_number;
                        break;
                    }
                }
            } catch (Exception ex) {
                // ignore
            }
            String result = null;
            for (int i = 0; i < parts.length - 2; i++) {
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

    class BingRequest extends Request {

        @Override
        void exec(double lat, double lon) {
            execute(BING_URL, lat, lon, Locale.getDefault().getLanguage());
        }

        @Override
        void result(JsonObject res) throws ParseException {
            try {
                String addr = res.get("resourceSets").asArray()
                        .get(0).asObject()
                        .get("resources").asArray()
                        .get(0).asObject()
                        .get("address").asObject()
                        .get("formattedAddress").asString();
                addr = addr.replace(", [0-9]{6}", "");
                addressResult(addr);
            } catch (Exception ex) {
                // ignore
            }
        }

        @Override
        void error() {
            addressResult(null);
        }
    }

    class YandexRequest extends Request {

        @Override
        void exec(double lat, double lon) {
            execute(YANDEX_URL, lat, lon, Locale.getDefault().getLanguage());
        }

        @Override
        void result(JsonObject res) throws ParseException {
            String addr = res.get("response").asObject()
                    .get("GeoObjectCollection").asObject()
                    .get("featureMember").asArray()
                    .get(0).asObject()
                    .get("GeoObject").asObject()
                    .get("metaDataProperty").asObject()
                    .get("GeocoderMetaData").asObject()
                    .get("AddressDetails").asObject()
                    .get("Country").asObject()
                    .get("AddressLine").asString();
            addressResult(addr);
        }

        @Override
        void error() {
            addressResult(null);
        }
    }
}
