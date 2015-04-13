package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.afollestad.materialdialogs.AlertDialogWrapper;

public class DialogActivity
        extends Activity
        implements DialogInterface.OnDismissListener,
        DialogInterface.OnClickListener {

    String title;
    String message;
    String point;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
        title = getIntent().getStringExtra(Names.TITLE);
        if (title != null)
            builder.setTitle(title);
        message = getIntent().getStringExtra(Names.MESSAGE);
        if (message != null)
            builder.setMessage(message);
        point = getIntent().getStringExtra(Names.POINT_DATA);
        if (point != null)
            builder.setPositiveButton(R.string.show_map, this);
        builder.setNegativeButton(R.string.cancel, null);
        Dialog dialog = builder.create();
        dialog.setOnDismissListener(this);
        dialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Intent intent = new Intent(this, MapEventActivity.class);
        String[] parts = point.split(",");
        intent.putExtra(Names.POINT_DATA, parts[0] + ";" + parts[1] + ";;" + message);
        startActivity(intent);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
