package net.ugona.plus;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.View;
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
        String title = getIntent().getStringExtra(Names.TITLE);
        ActionBar actionBar = getSupportActionBar();
        if (title == null) {
            findViewById(R.id.logo).setVisibility(View.VISIBLE);
            actionBar.setDisplayShowTitleEnabled(false);
        } else {
            actionBar.setTitle(title);
        }
    }

    @Override
    Js js() {
        return new JsInterface();
    }

    class JsInterface extends MapActivity.JsInterface {

        @JavascriptInterface
        public String getData() {
            return data;
        }

    }

}
