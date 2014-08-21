package net.ugona.plus;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

abstract public class WebViewActivity extends ActionBarActivity {

    FrameLayout holder;
    WebView webView;
    boolean loaded;

    abstract String loadURL();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);
        webView = (WebView) getLastCustomNonConfigurationInstance();
        initUI();
    }

    @Override
    protected void onPause() {
        webView.freeMemory();
        super.onPause();
    }

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
                    log(cm.message());
                    return true;
                }
            };
            webView.setWebChromeClient(mChromeClient);
            webView.loadUrl(loadURL());
        }
        holder.addView(webView);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        if (webView != null)
            holder.removeView(webView);
        return webView;
    }

    void log(String text) {
        State.appendLog("webview: " + text);
    }
}
