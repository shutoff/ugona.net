package net.ugona.plus;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Theme {

    static final int BUF_SIZE = 0x1000;

    static HashMap<String, Theme> themes;
    HashMap<String, Position> parts;
    int width;
    int height;
    private ZipFile zip;

    static Theme getTheme(Context context, String name) {
        if (themes == null)
            themes = new HashMap<>();
        Theme res = themes.get(name);
        if (res != null)
            return res;
        if (name == "")
            name = "theme";
        res = new Theme();
        if (!res.open(context, name))
            return null;
        themes.put(name, res);
        return res;
    }

    boolean open(Context context, String name) {
        File path = context.getFilesDir();
        path = new File(path, name);

        long theme_time = 0;
        long pkg_time = 0;

        if (path.exists()) {
            theme_time = path.lastModified();
            pkg_time = theme_time;
            try {
                ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
                ZipFile zf = new ZipFile(ai.sourceDir);
                ZipEntry ze = zf.getEntry("classes.dex");
                pkg_time = ze.getTime();
                zf.close();
            } catch (Exception ex) {
                // ignore
            }
        }

        if (pkg_time >= theme_time) {
            try {
                InputStream from = context.getAssets().open(name);
                OutputStream to = context.openFileOutput(name, Context.MODE_PRIVATE);
                byte[] buf = new byte[BUF_SIZE];
                while (true) {
                    int r = from.read(buf);
                    if (r == -1) {
                        break;
                    }
                    to.write(buf, 0, r);
                }
                from.close();
                to.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        try {
            zip = new ZipFile(path);
            InputStream in = zip.getInputStream(zip.getEntry("theme.json"));
            InputStreamReader reader = new InputStreamReader(in);
            JsonObject root = Json.parse(reader).asObject();
            parts = new HashMap<>();
            for (JsonObject.Member member : root.get("parts").asObject()) {
                JsonArray v = member.getValue().asArray();
                Position pos = new Position();
                pos.x = v.get(0).asInt();
                pos.y = v.get(1).asInt();
                parts.put(member.getName(), pos);
            }
            width = root.getInt("width", 0);
            height = root.getInt("height", 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    Pict get(String name) {
        if (parts == null)
            return null;
        Position pos = parts.get(name);
        if (pos == null)
            return null;
        try {
            Bitmap bitmap = getBitmapSafely(name, 0);
            if (bitmap == null)
                return null;
            Pict res = new Pict();
            res.bitmap = bitmap;
            res.x = pos.x;
            res.y = pos.y;
            return res;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private Bitmap getBitmapSafely(String name, int sampleSize) {
        InputStream in;
        try {
            in = zip.getInputStream(zip.getEntry(name + ".png"));
        } catch (Exception ex) {
            return null;
        }
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true;
        options.inSampleSize = sampleSize;
        try {
            bitmap = BitmapFactory.decodeStream(in);
        } catch (OutOfMemoryError oom) {
            System.gc();
            bitmap = getBitmapSafely(name, sampleSize + 1);
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return bitmap;
    }

    static class Position {
        int x;
        int y;
    }

    static class Pict {
        Bitmap bitmap;
        int x;
        int y;
    }

}
