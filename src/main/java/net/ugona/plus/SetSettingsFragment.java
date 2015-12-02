package net.ugona.plus;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.widget.Spinner;

import com.afollestad.materialdialogs.AlertDialogWrapper;

public class SetSettingsFragment
        extends DialogFragment
        implements DialogInterface.OnClickListener {

    String car_id;
    String id;
    String[] values;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        CarConfig config = CarConfig.get(getActivity(), car_id);
        CarConfig.Setting[] settings = config.getSettings();
        CarConfig.Command[] commands = config.getCmd();
        for (CarConfig.Setting setting : settings) {
            if (!setting.id.equals(id))
                continue;
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            Spinner spinner = (Spinner) inflater.inflate(R.layout.spinner, null);
            values = setting.values.split("\\|");
            spinner.setAdapter(new ArrayAdapter(spinner) {
                @Override
                public int getCount() {
                    return values.length;
                }

                @Override
                public Object getItem(int position) {
                    String v = values[position];
                    int p = v.indexOf(':');
                    return v.substring(p + 1);
                }
            });
            for (int i = 0; i < values.length; i++) {
                String v = values[i];
                int p = v.indexOf(':');
                v = v.substring(p + 1);
                if (v.equals(setting.text))
                    spinner.setSelection(i);
            }
            return new AlertDialogWrapper.Builder(getActivity())
                    .setTitle(setting.name)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, this)
                    .setView(spinner)
                    .create();
        }
        return null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
        outState.putString(Names.TITLE, id);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    void setArgs(Bundle args) {
        car_id = args.getString(Names.ID);
        id = args.getString(Names.TITLE);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

    }
}
