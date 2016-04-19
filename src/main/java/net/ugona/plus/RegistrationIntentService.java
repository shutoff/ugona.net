package net.ugona.plus;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.Reader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        AppConfig config = AppConfig.get(this);
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

            String reg = null;
            Reader reader = null;
            HttpURLConnection connection = null;

            JsonObject data = new JsonObject();
            data.add("reg", reg);
            String[] cars = config.getCars();
            String d = null;
            JsonObject jCars = new JsonObject();
            for (String car : cars) {
                CarConfig carConfig = CarConfig.get(this, car);
                String key = carConfig.getKey();
                if (key.equals("") || (key.equals("demo")))
                    continue;
                JsonObject c = new JsonObject();
                c.add("id", car);
                c.add("phone", carConfig.getPhone());
                c.add("auth", carConfig.getAuth());
                jCars.add(key, c);
            }
            data.add("car_data", jCars);
            Calendar cal = Calendar.getInstance();
            TimeZone tz = cal.getTimeZone();
            data.add("tz", tz.getID());
            data.add("version", getAppVer());
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String id = "";
            try {
                id = tm.getDeviceId();
            } catch (Exception ex) {
                // ignore
            }
            if (id.equals("")) {
                try {
                    id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                } catch (Exception ex) {
                    // ignore
                }
            }
            if (!id.equals(""))
                data.add("uid", id);
            data.add("lang", Locale.getDefault().getLanguage());
            data.add("os", Build.VERSION.RELEASE);
            data.add("model", Build.MODEL);
            String phone = "";
            try {
                phone = tm.getLine1Number();
            } catch (Exception ex) {
                // ignore
            }
            if (!phone.equals(""))
                data.add("phone", phone);
            String url = PhoneSettings.get().getServer() + "/reg";
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), data.toString());
            Request request = new Request.Builder().url(url).post(body).build();
            Response response = HttpTask.client.newCall(request).execute();
            if (response.code() != HttpURLConnection.HTTP_OK)
                return;
            reader = response.body().charStream();
            JsonObject res = JsonValue.readFrom(reader).asObject();
            if (res.asObject().get("error") != null)
                return;
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        config.setGCM_time(new Date().getTime());
        config.setGCM_version(getAppVer());
    }

    String getAppVer() {
        try {
            PackageManager pkgManager = getPackageManager();
            PackageInfo info = pkgManager.getPackageInfo("net.ugona.plus", 0);
            return info.versionName;
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    static class RegParams implements Serializable {
        String token;
        String name;
        int type;
        Device devices[];
    }

    static class Device implements Serializable {
        String skey;
        int id;
    }
}
