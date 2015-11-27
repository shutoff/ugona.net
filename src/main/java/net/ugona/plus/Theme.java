package net.ugona.plus;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Theme {

    static final int BUF_SIZE = 0x1000;

    static HashMap<String, Theme> themes;
    static boolean download = false;
    HashMap<String, Position> parts;
    int width;
    int height;
    private ZipFile zip;

    static Theme getTheme(Context context, String name) {
        if (themes == null)
            themes = new HashMap<>();
        if (name.length() == 0)
            name = "theme";
        Theme res = themes.get(name);
        if (res != null)
            return res;
        res = new Theme();
        if (!res.open(context, name))
            return null;
        themes.put(name, res);
        return res;
    }

    boolean open(final Context context, String name) {

        final File dir = context.getFilesDir();
        File path = new File(dir, name);

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
                pkg_time -= TimeZone.getDefault().getOffset(pkg_time);
                zf.close();
            } catch (Exception ex) {
                // ignore
            }
            long now = System.currentTimeMillis();
            if (now < pkg_time)
                pkg_time = now;
        }

        if (pkg_time >= theme_time) {
            try {
                InputStream from = context.getAssets().open(name);
                OutputStream to = context.openFileOutput(name, Context.MODE_PRIVATE);
                try {
                    byte[] buf = new byte[BUF_SIZE];
                    while (true) {
                        int r = from.read(buf);
                        if (r == -1) {
                            break;
                        }
                        to.write(buf, 0, r);
                    }
                    CarPictureProvider.themeInstalled(context, name);
                } finally {
                    try {
                        from.close();
                        to.close();
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (!download) {
                    download = true;
                    AsyncTask<String, Void, String> downloader = new AsyncTask<String, Void, String>() {
                        @Override
                        protected String doInBackground(String... params) {
                            try {
                                String name = params[0];
                                String url = PhoneSettings.get().getServer() + "/themes/" + params[0];
                                Request request = new Request.Builder().url(url).build();
                                Response response = HttpTask.client.newCall(request).execute();
                                if (response.code() != HttpURLConnection.HTTP_OK)
                                    return null;
                                File part = new File(dir, name + ".part");
                                part.createNewFile();
                                FileOutputStream out = new FileOutputStream(part);

                                InputStream in = response.body().byteStream();
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, bytesRead);
                                }
                                in.close();
                                out.close();

                                File new_file = new File(dir, name);
                                new_file.delete();
                                part.renameTo(new_file);

                                return name;

                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(String res) {
                            download = false;
                            if (res == null)
                                return;
                            try {
                                CarPictureProvider.themeInstalled(context, res);
                                AppConfig appConfig = AppConfig.get(context);
                                String[] cars = appConfig.getIds().split(";");
                                for (String car : cars) {
                                    CarConfig carConfig = CarConfig.get(context, car);
                                    if (res.equals(carConfig.getTheme())) {
                                        Intent intent = new Intent(Names.UPDATED_THEME);
                                        intent.putExtra(Names.ID, car);
                                        context.sendBroadcast(intent);
                                    }
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    };
                    downloader.execute(name);
                }
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
