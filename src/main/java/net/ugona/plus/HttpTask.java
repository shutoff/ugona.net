package net.ugona.plus;

import android.os.AsyncTask;
import android.util.Log;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import org.apache.http.HttpStatus;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public abstract class HttpTask {

    abstract void result(JsonObject res) throws ParseException;

    abstract void error();

    AsyncTask<String, Void, JsonObject> bgTask;

    void background(JsonObject res) throws ParseException {
    }

    static int total = 0;

    int pause = 0;
    String error_text;

    void execute(String... params) {
        if (bgTask != null)
            return;
        bgTask = new AsyncTask<String, Void, JsonObject>() {
            @Override
            protected JsonObject doInBackground(String... params) {
                String url = params[0];
                Reader reader = null;
                HttpURLConnection connection = null;
                try {
                    for (int i = 1; i < params.length; i++) {
                        url = url.replace("$" + i, URLEncoder.encode(params[i], "UTF-8"));
                    }
                    Log.v("url", url);
                    if (pause > 0)
                        Thread.sleep(pause);
                    URL u = new URL(url);
                    connection = (HttpURLConnection) u.openConnection();
                    final int[] length = new int[1];
                    InputStream in = new BufferedInputStream(connection.getInputStream()) {
                        @Override
                        public synchronized int read(byte[] buffer, int offset, int byteCount) throws IOException {
                            int res = super.read(buffer, offset, byteCount);
                            length[0] += res;
                            return res;
                        }
                    };
                    int status = connection.getResponseCode();
                    reader = new InputStreamReader(in);
                    JsonValue res = JsonValue.readFrom(reader);
                    reader.close();
                    reader = null;
                    JsonObject result;
                    if (res.isObject()) {
                        result = res.asObject();
                    } else {
                        result = new JsonObject();
                        result.set("data", res);
                    }
                    total += length[0];
                    State.appendLog(url + " " + length[0] + " " + total);
                    if (status != HttpStatus.SC_OK) {
                        error_text = result.get("error").asString();
                        return null;
                    }
                    background(result);
                    return result;
                } catch (Exception ex) {
                    error_text = ex.getMessage();
                    ex.printStackTrace();
                } finally {
                    if (connection != null)
                        connection.disconnect();
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(JsonObject res) {
                bgTask = null;
                if (res != null) {
                    try {
                        result(res);
                        return;
                    } catch (Exception ex) {
                        // ignore
                        ex.printStackTrace();
                    }
                }
                error();
            }
        };
        bgTask.execute(params);
    }

}
