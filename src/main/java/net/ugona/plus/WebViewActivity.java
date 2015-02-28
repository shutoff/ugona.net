package net.ugona.plus;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

abstract public class WebViewActivity extends ActionBarActivity {

    boolean loaded;
    private FrameLayout holder;
    private WebView webView;

    abstract Js js();

    abstract String getUrl();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        webView = (WebView) getLastCustomNonConfigurationInstance();
        initUI();
    }

    @Override
    protected void onPause() {
        webView.freeMemory();
        super.onPause();
    }

    @SuppressLint("AddJavascriptInterface")
    void initUI() {
        holder = (FrameLayout) findViewById(R.id.webview);
        if (webView == null) {
            loaded = false;
            webView = new WebView(this);
            webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setAppCacheEnabled(true);
            settings.setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);

            WebChromeClient mChromeClient = new WebChromeClient() {
                @Override
                public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                    super.onConsoleMessage(message, lineNumber, sourceID);
                    log(sourceID + ":" + lineNumber + " " + message);
                }

                @Override
                public boolean onConsoleMessage(ConsoleMessage cm) {
                    log(cm.sourceId() + ":" + cm.lineNumber() + ":" + cm.message());
                    return true;
                }
            };
            webView.setWebChromeClient(mChromeClient);
            webView.addJavascriptInterface(js(), "android");
            webView.loadUrl(getUrl());
        }
        holder.addView(webView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        if (webView != null)
            holder.removeView(webView);
        return webView;
    }

    void loadUrl(String url) {
        webView.loadUrl(url);
    }

    void callJs(String func) {
        log("call " + func);
        webView.loadUrl("javascript:" + func);
    }

    void log(String text) {
        //       State.appendLog("webview: " + text);
    }

    class Js {

    }
}
