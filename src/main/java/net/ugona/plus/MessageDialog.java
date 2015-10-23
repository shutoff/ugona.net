package net.ugona.plus;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.afollestad.materialdialogs.AlertDialogWrapper;

public class MessageDialog
        extends DialogFragment
        implements DialogInterface.OnClickListener {

    String title;
    String message;
    String more;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getActivity());
        if (title != null)
            builder.setTitle(title);
        builder.setMessage(message);
        builder.setNegativeButton(R.string.cancel, this);
        builder.setPositiveButton(R.string.ok, this);
        if (more != null)
            builder.setNeutralButton(more, this);
        AppConfig.save(getActivity());
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.MESSAGE, message);
        if (title != null)
            outState.putString(Names.TITLE, title);
        if (more != null)
            outState.putString(Names.MORE, more);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    void setArgs(Bundle args) {
        title = args.getString(Names.TITLE);
        message = args.getString(Names.MESSAGE);
        more = args.getString(Names.MORE);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        getTargetFragment().onActivityResult(getTargetRequestCode(), which, null);
        dismiss();
    }

}
