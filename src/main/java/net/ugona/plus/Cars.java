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

public class Cars extends ActionBarActivity {

    final static int CAR_SETUP = 4000;

    SharedPreferences preferences;

    static class Car {
        String id;
        String name;
    }

    Car[] cars;

    ListView lvCars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        setupCars();

        lvCars = (ListView) findViewById(R.id.list);
        lvCars.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return cars.length;
            }

            @Override
            public Object getItem(int position) {
                return cars[position];
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
                tvName.setText(cars[position].name);
                tvName.setTag(cars[position].id);
                tvName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setupCar(v.getTag().toString());
                    }
                });
                ImageView ivDelete = (ImageView) v.findViewById(R.id.del);
                if (cars.length > 1) {
                    ivDelete.setTag(cars[position].id);
                    ivDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            String id = v.getTag().toString();
                            String message = getString(R.string.delete);
                            Car[] cars = getCars(Cars.this);
                            for (Car car : cars) {
                                if (car.id.equals(id)) {
                                    message += " " + car.name;
                                }
                            }
                            message += "?";
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
            ((BaseAdapter) lvCars.getAdapter()).notifyDataSetChanged();
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
        }
        return false;
    }

    void setupCar(String car_id) {
        Intent intent = new Intent(this, CarPreferences.class);
        intent.putExtra(Names.ID, car_id);
        startActivityForResult(intent, CAR_SETUP);
    }

    static void deleteCarKeys(SharedPreferences preferences, String id) {
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
        ed.remove(Names.TEMPERATURE + id);
        ed.remove(Names.GSM + id);
        ed.remove(Names.GSM_ZONE + id);
        ed.remove(Names.LOGIN + id);
        ed.commit();
    }

    void deleteCar(String id) {
        deleteCarKeys(preferences, id);
        String[] cars = preferences.getString(Names.CARS, "").split(",");
        String res = null;
        for (String car : cars) {
            if (car.equals(id))
                continue;
            if (res == null) {
                res = car;
            } else {
                res += "," + car;
            }
        }
        SharedPreferences.Editor ed = preferences.edit();
        ed.putString(Names.CARS, res);
        ed.commit();
        setupCars();
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
