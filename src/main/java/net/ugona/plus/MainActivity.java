package net.ugona.plus;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import static android.view.Gravity.START;

public class MainActivity extends ActionBarActivity {

    static final int DO_AUTH = 1;
    static final int DO_PHONE = 2;
    String id;

    private ActionBarDrawerToggle toggle;
    private DrawerArrowDrawable drawerArrowDrawable;
    private float offset;
    private boolean flipped;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppConfig config = AppConfig.get(this);
        id = config.getId(getIntent().getStringExtra(Names.ID));
        CarConfig carConfig = CarConfig.get(this, id);

        setContentView(R.layout.main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ImageView imageView = (ImageView) findViewById(R.id.drawer_indicator);
        final Resources resources = getResources();

        drawerArrowDrawable = new DrawerArrowDrawable(resources);
        drawerArrowDrawable.setStrokeColor(resources.getColor(R.color.caldroid_white));
        imageView.setImageDrawable(drawerArrowDrawable);

        drawer.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                offset = slideOffset;

                // Sometimes slideOffset ends up so close to but not quite 1 or 0.
                if (slideOffset >= .995) {
                    flipped = true;
                    drawerArrowDrawable.setFlip(flipped);
                } else if (slideOffset <= .005) {
                    flipped = false;
                    drawerArrowDrawable.setFlip(flipped);
                }

                drawerArrowDrawable.setParameter(offset);
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawer.isDrawerVisible(START)) {
                    drawer.closeDrawer(START);
                } else {
                    drawer.openDrawer(START);
                }
            }
        });

        if (carConfig.getKey().equals("")) {
            Intent intent = new Intent(this, AuthDialog.class);
            intent.putExtra(Names.ID, id);
            startActivityForResult(intent, DO_AUTH);
            return;
        }
        if (checkPhone())
            return;
    }

    @Override
    protected void onDestroy() {
        AppConfig.save(this);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DO_AUTH) {
            CarConfig carConfig = CarConfig.get(this, id);
            if (carConfig.getKey().equals("")) {
                finish();
                return;
            }
            if (checkPhone())
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    boolean checkPhone() {
        CarState state = CarState.get(this, id);
        if (!state.isUse_phone() || !state.getPhone().equals("") || !State.hasTelephony(this))
            return false;
        Intent intent = new Intent(this, PhoneDialog.class);
        intent.putExtra(Names.ID, id);
        startActivityForResult(intent, DO_PHONE);
        return true;
    }
}
