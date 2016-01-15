package net.ugona.plus;

import android.os.Environment;

import com.eclipsesource.json.Json;

import java.io.File;
import java.io.FileReader;

public class PhoneSettings extends Config {

    static private PhoneSettings config;

    private String server;
    private boolean debug;

    private PhoneSettings() {
        server = "https://car-online.ugona.net";
        if (State.isDebug())
            server = "http://v5.shutoff.ru";
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

    public boolean isDebug() {
        return debug;
    }
}
