package net.ugona.plus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

    static final int DO_AUTH = 1;
    static final int DO_PHONE = 2;
    String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppConfig config = AppConfig.get(this);
        id = config.getId(getIntent().getStringExtra(Names.ID));
        CarConfig carConfig = CarConfig.get(this, id);
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
