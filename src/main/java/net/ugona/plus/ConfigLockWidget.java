package net.ugona.plus;

import android.os.Bundle;

public class ConfigLockWidget extends ConfigWidget {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        lock_widget = true;
        super.onCreate(savedInstanceState);
    }
}
