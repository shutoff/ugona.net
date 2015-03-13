package net.ugona.plus;

import android.app.Dialog;
import android.content.Context;
import android.view.View;

public class FABCommands extends Dialog implements View.OnClickListener {
    public FABCommands(Context context) {
        super(context, R.style.CustomDialogTheme);
        setContentView(R.layout.commands);
        findViewById(R.id.fab).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }
}
