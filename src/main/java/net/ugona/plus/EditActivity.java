package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.util.Vector;

public class EditActivity extends Activity
        implements Dialog.OnClickListener, Dialog.OnDismissListener {

    static final String EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE";
    static final String EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB";
    String[] ids;
    Vector<CarConfig.Command> cmd;
    String[] routes;
    EditText ccode;
    EditText passwd;
    Spinner car;
    Spinner commands;
    Spinner route;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.command, null);
        Dialog dialog = new AlertDialogWrapper.Builder(this)
                .setTitle(R.string.app_name)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, this)
                .setView(v)
                .create();
        dialog.setOnDismissListener(this);

        routes = getResources().getStringArray(R.array.cmd_route);

        AppConfig appConfig = AppConfig.get(this);

        car = (Spinner) v.findViewById(R.id.car);
        commands = (Spinner) v.findViewById(R.id.cmd);
        route = (Spinner) v.findViewById(R.id.route);
        ccode = (EditText) v.findViewById(R.id.ccode);
        passwd = (EditText) v.findViewById(R.id.pwd);

        ids = appConfig.getIds().split(";");
        if (ids.length < 2) {
            car.setVisibility(View.GONE);
        } else {
            final Vector<String> cars = new Vector<>();
            for (String id : ids) {
                CarConfig config = CarConfig.get(this, id);
                cars.add(config.getName());
            }
            car.setAdapter(new ArrayAdapter(car) {
                @Override
                public int getCount() {
                    return cars.size();
                }

                @Override
                public Object getItem(int i) {
                    return cars.get(i);
                }
            });
            car.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    fillCommands();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }

        fillCommands();
        commands.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                fillCommand();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        route.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setupCommand();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        fillCommand();

        dialog.show();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        int pos = commands.getSelectedItemPosition();
        if (pos < cmd.size()) {
            CarConfig.Command c = cmd.get(pos);
            Intent intent = getIntent();
            Bundle bundle = new Bundle();
            bundle.putString(Names.ID, getCar());
            bundle.putInt(Names.COMMAND, c.id);
            bundle.putInt(Names.ROUTE, route.getSelectedItemPosition());
            bundle.putString("ccode", ccode.getText().toString());
            bundle.putString("pwd", passwd.getText().toString());
            intent.putExtra(EXTRA_STRING_BLURB, c.name);
            intent.putExtra(EXTRA_BUNDLE, bundle);
            setResult(RESULT_OK, intent);
        }
        dialogInterface.dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        finish();
    }

    String getCar() {
        String car_id = null;
        if (ids.length > 0)
            car_id = ids[0];
        if (ids.length > 1)
            car_id = ids[car.getSelectedItemPosition()];
        return car_id;
    }

    void fillCommands() {
        CarConfig carConfig = CarConfig.get(this, getCar());
        CarConfig.Command[] cmds = carConfig.getCmd();
        cmd = new Vector<>();
        for (CarConfig.Command c : cmds) {
            if (c.icon == null)
                continue;
            if (!State.hasTelephony(this) && (c.inet == 0))
                continue;
            cmd.add(c);
        }
        commands.setAdapter(new ArrayAdapter(commands) {
            @Override
            public int getCount() {
                return cmd.size();
            }

            @Override
            public Object getItem(int i) {
                return cmd.get(i).name;
            }
        });
        setupCommand();
    }

    void fillCommand() {
        int pos = commands.getSelectedItemPosition();
        if (pos >= cmd.size()) {
            route.setVisibility(View.GONE);
            ccode.setVisibility(View.GONE);
            passwd.setVisibility(View.GONE);
            return;
        }
        CarConfig.Command c = cmd.get(pos);
        if ((c.inet != 0) && State.hasTelephony(this) && (c.sms != null)) {
            route.setVisibility(View.VISIBLE);
            route.setAdapter(new ArrayAdapter(route) {
                @Override
                public int getCount() {
                    return routes.length;
                }

                @Override
                public Object getItem(int i) {
                    return routes[i];
                }
            });
        } else {
            route.setVisibility(View.GONE);
        }
        setupCommand();
    }

    void setupCommand() {
        int pos = commands.getSelectedItemPosition();
        if (pos >= cmd.size()) {
            ccode.setVisibility(View.GONE);
            passwd.setVisibility(View.GONE);
            return;
        }
        CarConfig.Command c = cmd.get(pos);
        boolean show_ccode = false;
        if ((c.sms != null) && ((route.getVisibility() == View.GONE) || (route.getSelectedItemPosition() == 0)))
            show_ccode = (c.sms.indexOf("{ccode}") >= 0);
        if ((c.inet > 0) && c.inet_ccode && ((route.getVisibility() == View.GONE) || (route.getSelectedItemPosition() == 1)))
            show_ccode = true;
        ccode.setVisibility(show_ccode ? View.VISIBLE : View.GONE);
        passwd.setVisibility(View.GONE);
    }
}
