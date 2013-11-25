package net.ugona.plus;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public abstract class HttpTask extends AsyncTask<String, Void, JSONObject> {

    abstract void result(JSONObject res) throws JSONException;

    abstract void error();

    void background(JSONObject res) throws JSONException {
    }

    int pause = 0;
    String error_text;

    @Override
    protected JSONObject doInBackground(String... params) {
        HttpClient httpclient = new DefaultHttpClient();
        String url = params[0];
        for (int i = 1; i < params.length; i++) {
            url = url.replace("$" + i, params[i]);
        }
        Log.v("url", url);
        String res = null;
        try {
            if (pause > 0)
                Thread.sleep(pause);
            error_text = null;
            HttpResponse response = httpclient.execute(new HttpGet(url));
            StatusLine statusLine = response.getStatusLine();
            int status = statusLine.getStatusCode();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            res = out.toString();
            if (status != HttpStatus.SC_OK) {
                JSONObject result = new JSONObject(res);
                error_text = result.getString("error");
                return null;
            }
            if (res.length() == 0) {
                error_text = "empty answer";
                return null;
            }
            if (res.substring(0, 1).equals("\""))
                res = "{ \"data\": " + res + "}";
            JSONObject result = new JSONObject(res);
            background(result);
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
            // ignore
        }
        return null;
    }

    @Override
    protected void onPostExecute(JSONObject res) {
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
