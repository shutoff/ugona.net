package net.ugona.plus;

import android.os.Environment;

import com.eclipsesource.json.Json;

import java.io.File;
import java.io.FileReader;

public class PhoneSettings extends Config {

    static private PhoneSettings config;

    private String server;

    private PhoneSettings() {
        server = "http://178.62.226.138";
        File settings = Environment.getExternalStorageDirectory();
        settings = new File(settings, "car.cfg");
        if (settings.exists()) {
            try {
                FileReader reader = new FileReader(settings);
                update(this, Json.parse(reader).asObject());
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    static public PhoneSettings get() {
        if (config == null)
            config = new PhoneSettings();
        return config;
    }

    public String getServer() {
        return server;
    }
}
