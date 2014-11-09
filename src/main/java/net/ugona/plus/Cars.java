package net.ugona.plus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Vector;

public class Cars extends ActionBarActivity {

    final static int CAR_SETUP = 4000;
    final static int NEW_CAR = 4001;

    final static String URL_PHOTOS = "/photos?skey=$1&begin=$2";

    SharedPreferences preferences;
    Car[] cars;
    Vector<CarId> carsId;
    ListView lvCars;

    static Car[] getCars(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String cars_data = preferences.getString(Names.CARS, "");
        String[] cars_id = cars_data.split(",");
        Car[] cars = new Car[cars_id.length];
        for (int i = 0; i < cars_id.length; i++) {
            String id = cars_id[i];
            String name = preferences.getString(Names.Car.CAR_NAME + id, "");
            if (name.equals("")) {
                name = context.getString(R.string.car);
                if (!id.equals(""))
                    name += " " + id;
            }
            Car car = new Car();
            car.id = id;
            car.name = name;
            car.pointers = new String[0];
            String pointers = preferences.getString(Names.Car.POINTERS + id, "");
            if (!pointers.equals(""))
                car.pointers = pointers.split(",");
            cars[i] = car;
        }
        return cars;
    }

    static void deleteCarKeys(Context context, String id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        Field[] fields = Names.Car.class.getDeclaredFields();
        for (Field f : fields) {
            if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                continue;
            try {
                String val = (String) f.get(Names.Car.class);
                ed.remove(val + id);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        ed.remove(Names.GCM_TIME);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        setupCars();

        fillCarsId();
        lvCars = (ListView) findViewById(R.id.list);
        lvCars.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return carsId.size() + 1;
            }

            @Override
            public Object getItem(int position) {
                if (position < carsId.size())
                    return carsId.get(position);
                return null;
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
                if (position < carsId.size()) {
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
                                String message = getString(R.string.delete) + " " + preferences.getString(Names.Car.CAR_NAME + id, "") + "?";
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
                    v.findViewById(R.id.block_add).setVisibility(View.GONE);
                    v.findViewById(R.id.block_car).setVisibility(View.VISIBLE);
                } else {
                    v.findViewById(R.id.block_add).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.block_car).setVisibility(View.GONE);
                }
                return v;
            }
        });
        lvCars.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i >= carsId.size()) {
                    for (i = 1; ; i++) {
                        if (!isId(i + "")) {
                            deleteCarKeys(Cars.this, i + "");
                            Intent intent = new Intent(Cars.this, AuthDialog.class);
                            intent.putExtra(Names.ID, i + "");
                            intent.putExtra(Names.Car.AUTH, true);
                            intent.putExtra(Names.Car.CAR_PHONE, true);
                            startActivityForResult(intent, NEW_CAR);
                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAR_SETUP) {
            setupCars();
            fillCarsId();
            ((BaseAdapter) lvCars.getAdapter()).notifyDataSetChanged();
        }
        if ((requestCode == NEW_CAR) && (resultCode == RESULT_OK)) {
            setupCars();
            fillCarsId();
            ((BaseAdapter) lvCars.getAdapter()).notifyDataSetChanged();
            final String car_id = data.getStringExtra(Names.ID);
            if (checkPointer(car_id))
                return;
            HttpTask task = new HttpTask() {
                @Override
                void result(JsonObject res) throws ParseException {
                    JsonArray array = res.get("photos").asArray();
                    if (array.size() > 0) {
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putBoolean(Names.Car.SHOW_PHOTO + car_id, true);
                        ed.commit();
                        Intent i = new Intent(FetchService.ACTION_UPDATE_FORCE);
                        i.putExtra(Names.ID, car_id);
                        sendBroadcast(i);
                    }
                }

                @Override
                void error() {

                }
            };
            task.execute(URL_PHOTOS, preferences.getString(Names.Car.CAR_KEY + car_id, ""), new Date().getTime() - 3 * 24 * 60 * 60 * 1000);
            setupCar(car_id);
        }
    }

    boolean checkPointer(final String car_id) {
        if (!preferences.getBoolean(Names.Car.POINTER + car_id, false))
            return false;
        Cars.Car[] full_cars = Cars.getCars(this);
        final Vector<Cars.Car> tmp_cars = new Vector<Cars.Car>();
        for (Cars.Car car : cars) {
            if (car.id.equals(car_id))
                continue;
            if (preferences.getString(Names.Car.CAR_KEY + car_id, "demo").equals("demo"))
                continue;
            tmp_cars.add(car);
        }
        if (tmp_cars.size() == 0)
            return false;

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pointer)
                .setMessage(R.string.pointer_msg)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(inflater.inflate(R.layout.cars, null))
                .create();
        dialog.show();

        final Spinner spinner = (Spinner) dialog.findViewById(R.id.cars);
        spinner.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return tmp_cars.size();
            }

            @Override
            public Object getItem(int position) {
                return tmp_cars.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.list_item, null);
                }
                TextView tv = (TextView) v.findViewById(R.id.name);
                tv.setText(tmp_cars.get(position).name);
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.list_dropdown_item, null);
                }
                TextView tv = (TextView) v.findViewById(R.id.name);
                tv.setText(tmp_cars.get(position).name);
                return v;
            }
        });
        if (tmp_cars.size() < 2)
            spinner.setVisibility(View.GONE);
        dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor ed = preferences.edit();
                String id = cars[spinner.getSelectedItemPosition()].id;
                String new_cars = null;
                for (Cars.Car car : tmp_cars) {
                    if (new_cars == null) {
                        new_cars = car.id;
                        continue;
                    }
                    new_cars += "," + car.id;
                }
                ed.putString(Names.CARS, new_cars);
                ed.putString(Names.Car.CAR_NAME + car_id, "Pointer " + car_id);
                String pointers = preferences.getString(Names.Car.POINTERS + id, "");
                if (!pointers.equals(""))
                    pointers += ",";
                pointers += car_id;
                ed.putString(Names.Car.POINTERS + id, pointers);
                ed.commit();
                Intent i = new Intent(Cars.this, FetchService.class);
                i.putExtra(Names.ID, car_id);
                startService(i);
                i.putExtra(Names.ID, id);
                startService(i);
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                setupCar(car_id);
            }
        });
        return true;
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
                pid.car.name = preferences.getString(Names.Car.CAR_NAME + p, "");
                pid.pointer = true;
                carsId.add(pid);
            }
        }
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
        String[] cars = preferences.getString(Names.CARS, "").split(",");
        for (String car : cars) {
            if (car.equals(car_id))
                continue;
            if (preferences.getString(Names.Car.CAR_KEY + car, "").equals("demo")) {
                deleteCar(car);
                break;
            }
        }
        Intent intent = new Intent(this, SettingActivity.class);
        intent.putExtra(Names.ID, car_id);
        startActivityForResult(intent, CAR_SETUP);
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
            String pointer = preferences.getString(Names.Car.POINTERS + car, "");
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
            ed.putString(Names.Car.POINTERS + car, pointers);
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

    static class Car {
        String id;
        String name;
        String[] pointers;
    }

    static class CarId {
        Car car;
        boolean pointer;
    }

}
