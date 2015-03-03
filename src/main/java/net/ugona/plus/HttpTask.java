package net.ugona.plus;

import android.os.AsyncTask;
import android.os.Build;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

public abstract class HttpTask {

    public static final OkHttpClient client = createClient();
    AsyncTask<Object, Void, JsonObject> bgTask;
    int pause = 0;
    String error_text;
    boolean canceled;

    static OkHttpClient createClient() {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(15, TimeUnit.SECONDS);
        client.setReadTimeout(40, TimeUnit.SECONDS);
        return client;
    }

    abstract void result(JsonObject res) throws ParseException;

    abstract void error();

    void background(JsonObject res) throws ParseException {
    }

    void execute(Object... params) {
        if (bgTask != null)
            return;
        bgTask = new AsyncTask<Object, Void, JsonObject>() {
            @Override
            protected JsonObject doInBackground(Object... params) {
                String url = params[0].toString();
                if (url.charAt(0) == '/')
                    url = Names.API_URL + url;
                Reader reader = null;
                try {
                    int last_param = 1;
                    for (; ; last_param++) {
                        if (!url.contains("$" + last_param))
                            break;
                    }
                    for (int i = 1; i < last_param; i++) {
                        url = url.replace("$" + i, URLEncoder.encode(params[i].toString(), "UTF-8"));
                    }
                    for (; last_param + 1 < params.length; last_param += 2) {
                        if (params[last_param + 1] == null)
                            continue;
                        url += "&" + params[last_param].toString();
                        url += "=" + URLEncoder.encode(params[last_param + 1].toString(), "UTF-8");
                    }
                    if (pause > 0)
                        Thread.sleep(pause);

                    Request.Builder builder = new Request.Builder().url(url);
                    if (last_param < params.length) {
                        String data = Config.save(params[params.length - 1]);
                        RequestBody body = RequestBody.create(MediaType.parse("application/json"), data);
                        builder = builder.post(body);
                    }

                    Request request = builder.build();
                    Response response = client.newCall(request).execute();

                    if (response.code() != HttpURLConnection.HTTP_OK) {
                        error_text = response.message();
                        return null;
                    }
                    reader = response.body().charStream();
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
                    if (result.get("error") != null) {
                        error_text = result.get("error").asString();
                        return null;
                    }
                    background(result);
                    return result;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    error_text = ex.getLocalizedMessage();
                    if (error_text != null) {
                        int pos = error_text.indexOf(":");
                        if (pos > 0)
                            error_text = error_text.substring(0, pos);
                    }
                } finally {
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
                if (canceled) {
                    canceled = false;
                    return;
                }
                if (res != null) {
                    try {
                        result(res);
                        return;
                    } catch (Exception ex) {
                        if (error_text == null) {
                            String msg = ex.getLocalizedMessage();
                            if (msg == null)
                                msg = ex.getMessage();
                            if (msg == null)
                                msg = ex.toString();
                            error_text = msg;
                        }
                        ex.printStackTrace();
                    }
                }
                error();
            }
        };
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                bgTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
            } else {
                bgTask.execute(params);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            error();
        }
    }

    void cancel() {
        canceled = true;
    }

}
