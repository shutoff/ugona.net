package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;
import com.haibison.android.lockpattern.LockPatternActivity;


public class SendCommandFragment extends DialogFragment {

    static final int DO_CCODE_INET = 1;
    static final int DO_INET = 2;
    static final int DO_CCODE_SMS = 3;
    static final int DO_SMS = 4;

    Handler handler;
    String car_id;
    int cmd_id;
    boolean longTap;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        View v = inflater.inflate(R.layout.send_command, container);
        if (savedInstanceState == null) {
            handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    process();
                }
            });
        }
        return v;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
        outState.putInt(Names.COMMAND, cmd_id);
        outState.putBoolean(Names.ROUTE, longTap);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case DO_CCODE_INET:
                    send_command_inet(data.getStringExtra(Names.VALUE));
                    return;
                case DO_INET:
                    send_command_inet(null);
                    return;
                case DO_CCODE_SMS:
                    send_command_sms(data.getStringExtra(Names.VALUE));
                    return;
                case DO_SMS:
                    send_command_sms(null);
                    return;
            }
        }
        if (resultCode == Activity.RESULT_CANCELED) {
            switch (requestCode) {
                case DO_CCODE_INET:
                case DO_INET:
                case DO_CCODE_SMS:
                case DO_SMS:
                    try {
                        dismiss();
                    } catch (Exception ex) {
                        // ignore
                    }
                    return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void setArgs(Bundle args) {
        car_id = args.getString(Names.ID);
        cmd_id = args.getInt(Names.COMMAND);
        longTap = args.getBoolean(Names.ROUTE);
    }

    void process() {
        CarConfig config = CarConfig.get(getActivity(), car_id);
        CarConfig.Command[] cmd = config.getCmd();
        for (CarConfig.Command c : cmd) {
            if (c.id == cmd_id) {
                process(c);
                return;
            }
        }
        dismiss();
    }

    void process(CarConfig.Command cmd) {
        CarConfig config = CarConfig.get(getActivity(), car_id);
        if (cmd.call != null) {
            String phone = config.getPhone();
            if (phone.equals(""))
                return;
            phone = cmd.call.replace("{phone}", phone);
            Intent i = new Intent(android.content.Intent.ACTION_CALL, Uri.parse("tel:" + phone));
            startActivity(i);
            dismiss();
            return;
        }
        boolean is_inet = config.isInet_cmd();
        if (longTap)
            is_inet = !is_inet;
        if (is_inet && do_command_inet(cmd))
            return;
        if (do_command_sms(cmd))
            return;
        if (do_command_inet(cmd))
            return;
        dismiss();
    }

    boolean do_command_inet(CarConfig.Command cmd) {
        if (cmd.inet == 0)
            return false;
        if (cmd.inet_ccode) {
            CCodeDialog dialog = new CCodeDialog();
            Bundle args = new Bundle();
            args.putString(Names.ID, car_id);
            dialog.setArguments(args);
            dialog.setTargetFragment(this, DO_CCODE_INET);
            dialog.show(getFragmentManager(), "ccode");
            return true;
        }
        AppConfig config = AppConfig.get(getActivity());
        if (!config.getPattern().equals("")) {
            Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                    getActivity(), LockPatternActivity.class);
            intent.putExtra(LockPatternActivity.EXTRA_PATTERN, config.getPattern().toCharArray());
            startActivityForResult(intent, DO_INET);
            return true;
        }
        if (!config.getPassword().equals("")) {
            PasswordDialog dialog = new PasswordDialog();
            Bundle args = new Bundle();
            args.putString(Names.MESSAGE, config.getPassword());
            dialog.setArguments(args);
            dialog.setTargetFragment(this, DO_INET);
            dialog.show(getFragmentManager(), "password");
            return true;
        }
        Alert dialog = new Alert();
        Bundle args = new Bundle();
        args.putString(Names.TITLE, cmd.name);
        args.putString(Names.MESSAGE, getString(R.string.run_command));
        dialog.setArguments(args);
        dialog.setTargetFragment(this, DO_INET);
        dialog.show(getFragmentManager(), "alert");
        return true;
    }

    boolean do_command_sms(CarConfig.Command cmd) {
        if (cmd.sms == null)
            return false;
        if (!State.hasTelephony(getActivity()))
            return false;
        String sms = cmd.sms.split("\\|")[0];
        if (sms.indexOf("{ccode}") >= 0) {
            CCodeDialog dialog = new CCodeDialog();
            Bundle args = new Bundle();
            args.putString(Names.ID, car_id);
            dialog.setArguments(args);
            dialog.setTargetFragment(this, DO_CCODE_SMS);
            dialog.show(getFragmentManager(), "ccode");
            return true;
        }
        if (sms.indexOf("{pwd}") > 0) {
            CarConfig config = CarConfig.get(getActivity(), car_id);
            CarState state = CarState.get(getActivity(), car_id);
            if (config.isDevice_password() && state.isDevice_password()) {
                CCodeDialog dialog = new CCodeDialog();
                Bundle args = new Bundle();
                args.putString(Names.ID, car_id);
                dialog.setArguments(args);
                dialog.setTargetFragment(this, DO_CCODE_SMS);
                dialog.show(getFragmentManager(), "ccode");
                return true;
            }
        }
        AppConfig config = AppConfig.get(getActivity());
        if (!config.getPattern().equals("")) {
            Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                    getActivity(), LockPatternActivity.class);
            intent.putExtra(LockPatternActivity.EXTRA_PATTERN, config.getPattern().toCharArray());
            startActivityForResult(intent, DO_SMS);
            return true;
        }
        if (!config.getPassword().equals("")) {
            PasswordDialog dialog = new PasswordDialog();
            Bundle args = new Bundle();
            args.putString(Names.MESSAGE, config.getPassword());
            dialog.setArguments(args);
            dialog.setTargetFragment(this, DO_SMS);
            dialog.show(getFragmentManager(), "password");
            return true;
        }
        Alert dialog = new Alert();
        Bundle args = new Bundle();
        args.putString(Names.TITLE, cmd.name);
        args.putString(Names.MESSAGE, getString(R.string.run_command));
        dialog.setArguments(args);
        dialog.setTargetFragment(this, DO_SMS);
        dialog.show(getFragmentManager(), "alert");
        return true;
    }

    void send_command_inet(String ccode) {
        final Context context = getActivity().getBaseContext();
        CarConfig config = CarConfig.get(getActivity(), car_id);
        CarConfig.Command[] cmd = config.getCmd();
        for (final CarConfig.Command c : cmd) {
            if (c.id == cmd_id) {
                HttpTask task = new HttpTask() {
                    @Override
                    void result(JsonObject res) throws ParseException {
                        Toast toast = Toast.makeText(context, R.string.command_sent, Toast.LENGTH_LONG);
                        toast.show();
                        if (c.done == null)
                            Commands.remove(context, car_id, c);
                    }

                    @Override
                    void error() {
                        String text = getString(R.string.send_command_error);
                        if (error_text != null) {
                            text += ": ";
                            text += error_text;
                        }
                        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                        toast.show();
                        Commands.remove(context, car_id, c);
                    }
                };
                JsonObject params = new JsonObject();
                params.add("skey", config.getKey());
                params.add("command", c.inet);
                if (ccode != null)
                    params.add("ccode", ccode);
                task.execute("/command", params);
                Commands.put(getActivity(), car_id, c);
                break;
            }
        }
        dismiss();
    }

    void send_command_sms(String subst) {
        dismiss();
        CarConfig config = CarConfig.get(getActivity(), car_id);
        CarConfig.Command[] cmd = config.getCmd();
        for (final CarConfig.Command c : cmd) {
            if (c.id == cmd_id) {
                String sms = c.sms.split("\\|")[0];
                if (subst == null)
                    subst = "";
                sms = sms.replace("{ccode}", subst);
                sms = sms.replace("{pwd}", subst);
                if (Sms.send(getActivity(), car_id, c.id, sms))
                    Commands.put(getActivity(), car_id, c);
                break;
            }
        }
    }
}
