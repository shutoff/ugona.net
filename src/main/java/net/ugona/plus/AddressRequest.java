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
    static final String YANDEX_URL = "http://geocode-maps.yandex.ru/1.x/?geocode=$2,$1&format=json&lang=$3&kind=house";
    static final String YANDEX1_URL = "http://geocode-maps.yandex.ru/1.x/?geocode=$2,$1&format=json&lang=$3";
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

    class BingRequest extends Request {

        @Override
        void exec(double lat, double lon) {
            execute(BING_URL, lat, lon, Locale.getDefault().getLanguage());
        }

        @Override
        void result(JsonObject res) throws ParseException {
            JsonArray resources = res.get("resourceSets").asArray()
                    .get(0).asObject()
                    .get("resources").asArray();
            String addr = null;
            String postalCode = null;
            for (int i = 0; i < resources.size(); i++) {
                JsonObject address = resources.get(i)
                        .asObject()
                        .get("address").asObject();
                String a = address.get("formattedAddress").asString();
                if (addr == null) {
                    addr = a;
                    try {
                        postalCode = address.get("postalCode").asString();
                    } catch (Exception ex) {
                        // ignore
                    }
                    continue;
                }
                if (a.length() > addr.length()) {
                    addr = a;
                    postalCode = null;
                    try {
                        postalCode = address.get("postalCode").asString();
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            }
            if ((postalCode != null) && (addr != null)) {
                String[] parts = addr.split(", ");
                addr = null;
                for (String part : parts) {
                    if (part.equals(postalCode))
                        continue;
                    if (addr == null) {
                        addr = part;
                        continue;
                    }
                    addr += ", " + part;
                }
            }
            addressResult(addr);
        }

        @Override
        void error() {
            addressResult(null);
        }
    }

    class YandexRequest extends Request {

        String url;
        double latitude;
        double longitude;

        YandexRequest() {
            url = YANDEX_URL;
        }

        YandexRequest(String url_) {
            url = url_;
        }

        @Override
        void exec(double lat, double lon) {
            latitude = lat;
            longitude = lon;
            execute(url, lat, lon, Locale.getDefault().getLanguage());
        }

        @Override
        void result(JsonObject res) throws ParseException {
            JsonArray results = res.get("response").asObject()
                    .get("GeoObjectCollection").asObject()
                    .get("featureMember").asArray();
            if (results.size() == 0) {
                if (url.equals(YANDEX1_URL)) {
                    addressResult(null);
                    return;
                }
                request = new YandexRequest(YANDEX1_URL);
                request.exec(latitude, longitude);
                return;
            }
            JsonObject data = results.get(0).asObject()
                    .get("GeoObject").asObject()
                    .get("metaDataProperty").asObject()
                    .get("GeocoderMetaData").asObject()
                    .get("AddressDetails").asObject()
                    .get("Country").asObject();
            String[] parts = data.get("AddressLine").asString().split(", ");
            String addr = null;
            if (parts.length > 2) {
                try {
                    String house = data.get("AdministrativeArea").asObject()
                            .get("SubAdministrativeArea").asObject()
                            .get("Locality").asObject()
                            .get("Thoroughfare").asObject()
                            .get("Premise").asObject()
                            .get("PremiseNumber").asString();
                    int i = parts.length - 1;
                    if (parts[i].equals(house)) {
                        parts[i - 1] += ",\u00A0" + house;
                        parts[i] = null;
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
            for (int i = parts.length - 1; i >= 0; i--) {
                String part = parts[i];
                if (part == null)
                    continue;
                if (addr == null) {
                    addr = part;
                    continue;
                }
                addr += ", ";
                addr += part;
            }
            addressResult(addr);
        }

        @Override
        void error() {
            addressResult(null);
        }
    }
}
