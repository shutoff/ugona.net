package net.ugona.plus;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

abstract public class WebViewActivity extends ActionBarActivity {

    abstract String loadURL();

    FrameLayout holder;
    WebView webView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);
        webView = (WebView) getLastCustomNonConfigurationInstance();
        initUI();
    }

    void initUI() {
        holder = (FrameLayout) findViewById(R.id.webview);
        if (webView == null) {
            webView = new WebView(this);
            webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);

            WebChromeClient mChromeClient = new WebChromeClient() {
                @Override
                public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                    super.onConsoleMessage(message, lineNumber, sourceID);
                    Log.v("console", message);
                }

                @Override
                public boolean onConsoleMessage(ConsoleMessage cm) {
                    String message = cm.message();
                    Log.v("console", message);
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
}
