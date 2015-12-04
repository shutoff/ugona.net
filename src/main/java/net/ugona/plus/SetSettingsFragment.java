package net.ugona.plus;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

public class SetSettingsFragment
        extends DialogFragment
        implements AdapterView.OnItemSelectedListener {

    String car_id;
    String id;
    String[] values;

    Spinner sValue;
    View btnOk;
    TextView tvError;
    View vProgress;

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
            View v = inflater.inflate(R.layout.set_value, null);
            sValue = (Spinner) v.findViewById(R.id.value);
            tvError = (TextView) v.findViewById(R.id.error);
            vProgress = v.findViewById(R.id.progress);

            values = setting.values.split("\\|");
            sValue.setAdapter(new ArrayAdapter(sValue) {
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
                String val = values[i];
                int p = val.indexOf(':');
                val = val.substring(p + 1);
                if (val.equals(setting.text))
                    sValue.setSelection(i);
            }
            sValue.setOnItemSelectedListener(this);
            return new AlertDialogWrapper.Builder(getActivity())
                    .setTitle(setting.name)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, null)
                    .setView(v)
                    .create();
        }
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        MaterialDialog dialog = (MaterialDialog) getDialog();
        btnOk = dialog.getActionButton(DialogAction.POSITIVE);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setValue();
            }
        });
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

    void setValue() {
        btnOk.setEnabled(false);
        tvError.setVisibility(View.GONE);
        vProgress.setVisibility(View.VISIBLE);
        HttpTask task = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                CarConfig config = CarConfig.get(getActivity(), car_id);
                Config.update(config, res);
                CarState state = CarState.get(getActivity(), car_id);
                if (CarState.update(state, res.get("state").asObject()) != null) {
                    Intent intent = new Intent(Names.UPDATED);
                    intent.putExtra(Names.ID, car_id);
                    getActivity().sendBroadcast(intent);
                }
                JsonObject caps = res.get("caps").asObject();
                boolean changed = CarState.update(state, caps.get("caps").asObject()) != null;
                changed |= (CarState.update(config, caps) != null);
                if (changed) {
                    Intent intent = new Intent(Names.CONFIG_CHANGED);
                    intent.putExtra(Names.ID, car_id);
                    getActivity().sendBroadcast(intent);
                }
                dismiss();
            }

            @Override
            void error() {
                tvError.setText(getActivity().getString(R.string.error) + "\n" + error_text);
                tvError.setVisibility(View.VISIBLE);
                vProgress.setVisibility(View.GONE);
            }
        };
        CarConfig carConfig = CarConfig.get(getContext(), car_id);
        JsonObject param = new JsonObject();
        param.set("skey", carConfig.getKey());
        param.set(id, sValue.getSelectedItemPosition());
        task.execute("/set", param);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        tvError.setVisibility(View.GONE);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
