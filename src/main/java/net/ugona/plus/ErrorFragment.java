package net.ugona.plus;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

public class ErrorFragment
        extends DialogFragment {

    String title;
    String message;
    String actions;
    String car_id;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity())
                .title(title)
                .content(message);
        final String[] parts = actions.split(";");
        for (int i = 1; i < parts.length; i++) {
            final String[] p = parts[i].split(":");
            if (i == 1) {
                builder.positiveText(p[2]).onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Intent cmdIntent = new Intent(getContext(), FetchService.class);
                        cmdIntent.setAction(parts[0]);
                        cmdIntent.putExtra(Names.ID, car_id);
                        cmdIntent.putExtra(Names.COMMAND, p[0]);
                        getContext().startService(cmdIntent);
                    }
                });
            }
            if (i == 2) {
                builder.neutralText(p[2]).onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Intent cmdIntent = new Intent(getContext(), FetchService.class);
                        cmdIntent.setAction(parts[0]);
                        cmdIntent.putExtra(Names.ID, car_id);
                        cmdIntent.putExtra(Names.COMMAND, p[0]);
                        getContext().startService(cmdIntent);
                    }
                });
            }
        }
        return builder.show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.MESSAGE, message);
        outState.putString(Names.COMMANDS, actions);
        outState.putString(Names.TITLE, title);
        outState.putString(Names.ID, car_id);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    void setArgs(Bundle args) {
        title = args.getString(Names.TITLE);
        message = args.getString(Names.MESSAGE);
        actions = args.getString(Names.COMMANDS);
        car_id = args.getString(Names.ID);
    }

}
