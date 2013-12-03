package net.ugona.plus;

import android.os.AsyncTask;
import android.util.Log;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStreamReader;

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
        for (int i = 1; i < params.length; i++) {
            url = url.replace("$" + i, params[i]);
        }
        Log.v("url", url);
        try {
            if (pause > 0)
                Thread.sleep(pause);
            error_text = null;
            HttpResponse response = httpclient.execute(new HttpGet(url));
            StatusLine statusLine = response.getStatusLine();
            int status = statusLine.getStatusCode();
            JsonObject result = JsonObject.readFrom(new InputStreamReader(response.getEntity().getContent()));
            if (status != HttpStatus.SC_OK) {
                error_text = result.get("error").asString();
                return null;
            }
            background(result);
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
            // ignore
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
