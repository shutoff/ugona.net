package net.ugona.plus;

import android.os.AsyncTask;
import android.util.Log;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;

public abstract class HttpTask extends AsyncTask<String, Void, JsonObject> {

    abstract void result(JsonObject res) throws ParseException;

    abstract void error();

    void background(JsonObject res) throws ParseException {
    }

    int pause = 0;
    String error_text;

    @Override
    protected JsonObject doInBackground(String... params) {
        HttpClient httpclient = new DefaultHttpClient();
        String url = params[0];
        Reader reader = null;
        try {
            for (int i = 1; i < params.length; i++) {
                url = url.replace("$" + i, URLEncoder.encode(params[i], "UTF-8"));
            }
            Log.v("url", url);
            if (pause > 0)
                Thread.sleep(pause);
            HttpResponse response = httpclient.execute(new HttpGet(url));
            StatusLine statusLine = response.getStatusLine();
            int status = statusLine.getStatusCode();
            reader = new InputStreamReader(response.getEntity().getContent());
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
            if (status != HttpStatus.SC_OK) {
                error_text = result.get("error").asString();
                return null;
            }
            background(result);
            return result;
        } catch (Exception ex) {
            error_text = "Parse data error";
            ex.printStackTrace();
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

}
