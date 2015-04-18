package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public class PointerDialog
        extends DialogFragment
        implements DialogInterface.OnClickListener {

    Spinner vCars;
    Vector<String> cars;
    String car_id;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.pointerdialog, null);
        vCars = (Spinner) v.findViewById(R.id.cars);
        AppConfig config = AppConfig.get(getActivity());
        String[] ids = config.getIds().split(";");
        cars = new Vector<>();
        for (String id : ids) {
            if (id.equals(car_id))
                continue;
            cars.add(id);
        }
        vCars.setAdapter(new ArrayAdapter(vCars) {
            @Override
            public int getCount() {
                return cars.size();
            }

            @Override
            public Object getItem(int position) {
                CarConfig carConfig = CarConfig.get(getActivity(), cars.get(position));
                return carConfig.getName();
            }

        });
        return new AlertDialogWrapper.Builder(getActivity())
                .setTitle(R.string.pointer)
                .setView(v)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        String id = cars.get(vCars.getSelectedItemPosition());
        CarConfig carConfig = CarConfig.get(getActivity(), id);
        Set<Integer> p = new HashSet<>();
        int[] pointers = carConfig.getPointers();
        if (pointers != null) {
            for (int pointer : pointers) {
                p.add(pointer);
            }
        }
        int i = 0;
        for (i = 1; ; i++) {
            if (!p.contains(i))
                break;
        }
        p.add(i);
        String new_id = id + "_" + i;
        pointers = new int[p.size()];
        int n = 0;
        for (int pp : p) {
            pointers[n++] = pp;
        }
        carConfig.setPointers(pointers);
        carConfig = CarConfig.get(getActivity(), car_id);
        carConfig.save(getActivity(), new_id);
        CarState carState = CarState.get(getActivity(), car_id);
        carState.save(getActivity(), new_id);
        AppConfig appConfig = AppConfig.get(getActivity());
        appConfig.removeId(car_id);
        appConfig.save(getActivity());
        Fragment fragment = getTargetFragment();
        car_id = null;
        if (fragment != null) {
            Intent intent = new Intent();
            intent.putExtra(Names.ID, new_id);
            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        }
        Intent intent = new Intent(Names.CAR_CHANGED);
        getActivity().sendBroadcast(intent);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (car_id == null)
            return;
        Fragment fragment = getTargetFragment();
        if (fragment != null) {
            Intent intent = new Intent();
            intent.putExtra(Names.ID, car_id);
            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, intent);
        }
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    void setArgs(Bundle args) {
        car_id = args.getString(Names.ID);
    }
}
