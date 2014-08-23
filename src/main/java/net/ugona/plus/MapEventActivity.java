package net.ugona.plus;

import android.os.Bundle;
import android.webkit.JavascriptInterface;

public class MapEventActivity extends MapActivity {

    String data;

    @Override
    int menuId() {
        return R.menu.map;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        data = getIntent().getStringExtra(Names.POINT_DATA);
    }

    @Override
    JsInterface js() {
        return new JsInterface();
    }

    class JsInterface extends MapActivity.JsInterface {

        @JavascriptInterface
        public String getData() {
            return data;
        }

        @JavascriptInterface
        public String init() {
            return super.init() + "\nshowPoints();center()";
        }
    }

}
