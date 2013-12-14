package net.ugona.plus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Vector;

public class Cars extends ActionBarActivity {

    final static int CAR_SETUP = 4000;

    SharedPreferences preferences;

    static class Car {
        String id;
        String name;
        String[] pointers;
    }

    Car[] cars;

    static class CarId {
        Car car;
        boolean pointer;
    }

    Vector<CarId> carsId;

    ListView lvCars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        setupCars();

        fillCarsId();
        lvCars = (ListView) findViewById(R.id.list);
        lvCars.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return carsId.size();
            }

            @Override
            public Object getItem(int position) {
                return carsId.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getBaseContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.car_item, null);
                }
                TextView tvName = (TextView) v.findViewById(R.id.name);
                CarId id = carsId.get(position);
                tvName.setText(id.car.name);
                tvName.setTag(id.car.id);
                tvName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setupCar(v.getTag().toString());
                    }
                });
                ImageView ivDelete = (ImageView) v.findViewById(R.id.del);
                if (id.pointer || cars.length > 1) {
                    ivDelete.setTag(id.car.id);
                    ivDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            String id = v.getTag().toString();
                            String message = getString(R.string.delete) + " " + preferences.getString(Names.CAR_NAME + id, "") + "?";
                            AlertDialog dialog = new AlertDialog.Builder(Cars.this)
                                    .setTitle(R.string.delete)
                                    .setMessage(message)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            deleteCar(v.getTag().toString());
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, null)
                                    .create();
                            dialog.show();
                        }
                    });
                    ivDelete.setVisibility(View.VISIBLE);
                } else {
                    ivDelete.setVisibility(View.GONE);
                }
                v.findViewById(R.id.pointer).setVisibility(id.pointer ? View.VISIBLE : View.GONE);
                return v;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cars, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                for (int i = 1; ; i++) {
                    if (!isId(i + "")) {
                        deleteCarKeys(this, i + "");
                        setupCar(i + "");
                        break;
                    }
                }
                return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAR_SETUP) {
            setupCars();
            fillCarsId();
            ((BaseAdapter) lvCars.getAdapter()).notifyDataSetChanged();
        }
    }

    void fillCarsId() {
        carsId = new Vector<CarId>();
        for (Car car : cars) {
            CarId id = new CarId();
            id.car = car;
            id.pointer = false;
            carsId.add(id);
            for (String p : car.pointers) {
                CarId pid = new CarId();
                pid.car = new Car();
                pid.car.id = p;
                pid.car.name = preferences.getString(Names.CAR_NAME + p, "");
                pid.pointer = true;
                carsId.add(pid);
            }
        }
    }

    static Car[] getCars(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String cars_data = preferences.getString(Names.CARS, "");
        String[] cars_id = cars_data.split(",");
        Car[] cars = new Car[cars_id.length];
        for (int i = 0; i < cars_id.length; i++) {
            String id = cars_id[i];
            String name = preferences.getString(Names.CAR_NAME + id, "");
            if (name.equals("")) {
                name = context.getString(R.string.car);
                if (!id.equals(""))
                    name += " " + id;
            }
            Car car = new Car();
            car.id = id;
            car.name = name;
            car.pointers = new String[0];
            String pointers = preferences.getString(Names.POINTERS + id, "");
            if (!pointers.equals(""))
                car.pointers = pointers.split(",");
            cars[i] = car;
        }
        return cars;
    }

    void setupCars() {
        cars = getCars(this);
    }

    boolean isId(String id) {
        for (Car car : cars) {
            if (car.id.equals(id))
                return true;
            for (String p : car.pointers)
                if (p.equals(id))
                    return true;
        }
        return false;
    }

    void setupCar(String car_id) {
        Intent intent = new Intent(this, CarPreferences.class);
        intent.putExtra(Names.ID, car_id);
        startActivityForResult(intent, CAR_SETUP);
    }

    static void deleteCarKeys(Context context, String id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        ed.remove(Names.CAR_NAME + id);
        ed.remove(Names.CAR_KEY + id);
        ed.remove(Names.CAR_PHONE + id);
        ed.remove(Names.EVENT_ID + id);
        ed.remove(Names.EVENT_TIME + id);
        ed.remove(Names.VOLTAGE_MAIN + id);
        ed.remove(Names.VOLTAGE_RESERVED + id);
        ed.remove(Names.BALANCE + id);
        ed.remove(Names.LATITUDE + id);
        ed.remove(Names.LONGITUDE + id);
        ed.remove(Names.COURSE + id);
        ed.remove(Names.SPEED + id);
        ed.remove(Names.GUARD + id);
        ed.remove(Names.GUARD0 + id);
        ed.remove(Names.GUARD1 + id);
        ed.remove(Names.INPUT1 + id);
        ed.remove(Names.INPUT2 + id);
        ed.remove(Names.INPUT3 + id);
        ed.remove(Names.INPUT4 + id);
        ed.remove(Names.ZONE_DOOR + id);
        ed.remove(Names.ZONE_HOOD + id);
        ed.remove(Names.ZONE_TRUNK + id);
        ed.remove(Names.ZONE_ACCESSORY + id);
        ed.remove(Names.ZONE_IGNITION + id);
        ed.remove(Names.LAST_EVENT + id);
        ed.remove(Names.ENGINE + id);
        ed.remove(Names.AZ + id);
        ed.remove(Names.TEMPERATURE + id);
        ed.remove(Names.GSM + id);
        ed.remove(Names.GSM_ZONE + id);
        ed.remove(Names.LOGIN + id);
        ed.remove(Names.POINTER + id);
        ed.remove(Names.POINTERS + id);
        ed.commit();
        File cache = context.getCacheDir();
        final String prefix = "p" + id + "_";
        File[] files = cache.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if (filename.length() < prefix.length())
                    return false;
                return filename.substring(0, prefix.length()).equals(prefix);
            }
        });
        for (File f : files) {
            f.delete();
        }
    }

    void deleteCar(String id) {
        deleteCarKeys(this, id);
        String[] cars = preferences.getString(Names.CARS, "").split(",");
        SharedPreferences.Editor ed = preferences.edit();
        String res = null;
        for (String car : cars) {
            if (car.equals(id))
                continue;
            if (res == null) {
                res = car;
            } else {
                res += "," + car;
            }
            String pointer = preferences.getString(Names.POINTERS + car, "");
            if (pointer.equals(""))
                continue;
            String pointers = "";
            for (String p : pointer.split(",")) {
                if (p.equals(id))
                    continue;
                if (pointers.equals("")) {
                    pointers = p;
                } else {
                    pointers += "," + p;
                }
            }
            ed.putString(Names.POINTERS + car, "");
        }
        ed.putString(Names.CARS, res);
        ed.commit();
        setupCars();
        fillCarsId();
        ((BaseAdapter) lvCars.getAdapter()).notifyDataSetChanged();
        try {
            Intent intent = new Intent(FetchService.ACTION_UPDATE_FORCE);
            intent.putExtra(Names.ID, id);
            sendBroadcast(intent);
        } catch (Exception e) {
            // ignore
        }
    }

}
