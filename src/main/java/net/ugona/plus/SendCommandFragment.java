package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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

import java.util.Locale;


public class SendCommandFragment extends DialogFragment {

    static final int DO_CCODE_INET = 1;
    static final int DO_INET = 2;
    static final int DO_CCODE_SMS = 3;
    static final int DO_SMS = 4;
    static final int DO_PHONE = 5;

    Handler handler;
    String car_id;
    int cmd_id;
    boolean longTap;
    boolean no_prompt;

    public static void send_inet(final Context context, final CarConfig.Command c, final String car_id, final String ccode) {
        HttpTask task = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                Toast toast = Toast.makeText(context, R.string.command_sent, Toast.LENGTH_LONG);
                toast.show();
                if (c.done == null)
                    Commands.remove(context, car_id, c);
                if (res.get("result") != null) {
                    Commands.setCommandResult(context, car_id, c.id, res.getInt("result", 0), res.getString("message", null));
                    return;
                }
                int id = res.getInt("id", 0);
                if (id != 0) {
                    Commands.setId(context, car_id, c, id);
                    return;
                }
                Intent intent = new Intent(context, FetchService.class);
                intent.setAction(FetchService.ACTION_UPDATE);
                intent.putExtra(Names.ID, car_id);
                context.startService(intent);
            }

            @Override
            void error() {
                String text = context.getString(R.string.send_command_error);
                if (error_text != null) {
                    text += ": ";
                    text += error_text;
                }
                String actions = FetchService.ACTION_COMMAND + ";";
                actions += c.id + "|" + ccode + "|1:" + android.R.drawable.ic_popup_sync + ":" + context.getString(R.string.retry);
                if (c.sms != null)
                    actions += ";" + c.id + "||:" + android.R.drawable.ic_menu_call + ":" + context.getString(R.string.send_sms);
                Notification.create(context, text, R.drawable.w_warning_light, car_id, null, 0, false, c.name, actions);
                Commands.remove(context, car_id, c);
            }
        };
        CarConfig config = CarConfig.get(context, car_id);
        JsonObject params = new JsonObject();
        params.add("skey", config.getKey());
        params.add("command", c.inet);
        params.add("lang", Locale.getDefault().getLanguage());
        if (ccode != null)
            params.add("ccode", ccode);
        task.execute("/command", params);
        Commands.put(context, car_id, c, null);
    }

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
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        final Activity activity = getActivity();
        if (activity instanceof DialogInterface.OnDismissListener) {
            ((DialogInterface.OnDismissListener) activity).onDismiss(dialog);
        }
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
        outState.putBoolean(Names.NO_PROMPT, no_prompt);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case DO_CCODE_INET:
                    send_command_inet(data.getStringExtra("ccode"));
                    return;
                case DO_INET:
                    send_command_inet(null);
                    return;
                case DO_CCODE_SMS:
                    send_command_sms(data);
                    return;
                case DO_SMS:
                    send_command_sms(null);
                    return;
                case DO_PHONE:
                    CarConfig carConfig = CarConfig.get(getActivity(), car_id);
                    carConfig.setPhone(data.getStringExtra(Names.PHONE));
                    Intent intent = new Intent(Names.CAR_CHANGED);
                    getActivity().sendBroadcast(intent);
                    process();
                    return;
            }
        }
        switch (requestCode) {
            case DO_CCODE_INET:
            case DO_INET:
            case DO_CCODE_SMS:
            case DO_SMS:
            case DO_PHONE:
                try {
                    dismiss();
                } catch (Exception ex) {
                    // ignore
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void setArgs(Bundle args) {
        car_id = args.getString(Names.ID);
        cmd_id = args.getInt(Names.COMMAND);
        longTap = args.getBoolean(Names.ROUTE);
        no_prompt = args.getBoolean(Names.NO_PROMPT);
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
            if (phone.equals("")) {
                InputPhoneDialog phoneDialog = new InputPhoneDialog();
                Bundle args = new Bundle();
                args.putString(Names.TITLE, getString(R.string.device_phone_number));
                args.putString(Names.ID, car_id);
                phoneDialog.setArguments(args);
                phoneDialog.setTargetFragment(this, DO_PHONE);
                phoneDialog.show(getFragmentManager(), "phone");
                return;
            }
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
        if (no_prompt) {
            onActivityResult(DO_INET, Activity.RESULT_OK, null);
            return true;
        }
        show_alert(cmd, DO_INET);
        return true;
    }

    boolean do_command_sms(CarConfig.Command cmd) {
        if (cmd.sms == null)
            return false;
        if (!State.hasTelephony(getActivity()))
            return false;
        String sms = cmd.sms.split("\\|")[0];
        if (sms.indexOf("{ccode}") >= 0) {
            if (cmd.data != null) {
                CCodeDialog dialog = new CCodeDialog();
                Bundle args = new Bundle();
                args.putString(Names.ID, car_id);
                args.putString(Names.MESSAGE, cmd.data);
                args.putString(Names.TITLE, cmd.name);
                dialog.setArguments(args);
                dialog.setTargetFragment(this, DO_CCODE_SMS);
                dialog.show(getFragmentManager(), "ccode");
                return true;
            }
            CCodeDialog dialog = new CCodeDialog();
            Bundle args = new Bundle();
            args.putString(Names.ID, car_id);
            dialog.setArguments(args);
            dialog.setTargetFragment(this, DO_CCODE_SMS);
            dialog.show(getFragmentManager(), "ccode");
            return true;
        }
        if (sms.indexOf("{pwd}") >= 0) {
            CarConfig config = CarConfig.get(getActivity(), car_id);
            CarState state = CarState.get(getActivity(), car_id);
            if (state.isDevice_pswd() ||
                    (config.isDevice_password() && state.isDevice_password())) {
                PasswordDialog dialog = new PasswordDialog();
                Bundle args = new Bundle();
                args.putString(Names.TITLE, getString(R.string.input_device_pswd));
                if (sms.indexOf("{v}") >= 0) {
                    args.putString(Names.VALUE, cmd.data);
                    args.putString(Names.ID, car_id);
                    args.putInt(Names.COMMAND, cmd.id);
                }
                dialog.setArguments(args);
                dialog.setTargetFragment(this, DO_CCODE_SMS);
                dialog.show(getFragmentManager(), "password");
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
        if (no_prompt) {
            onActivityResult(DO_SMS, Activity.RESULT_OK, null);
            return true;
        }
        show_alert(cmd, DO_SMS);
        return true;
    }

    void send_command_inet(String ccode) {
        final Context context = getActivity().getBaseContext();
        CarConfig config = CarConfig.get(getActivity(), car_id);
        CarConfig.Command[] cmd = config.getCmd();
        for (final CarConfig.Command c : cmd) {
            if (c.id == cmd_id) {
                send_inet(context, c, car_id, ccode);
                break;
            }
        }
        dismiss();
    }

    void send_command_sms(Intent data) {
        dismiss();
        CarConfig config = CarConfig.get(getActivity(), car_id);
        CarConfig.Command[] cmd = config.getCmd();
        for (final CarConfig.Command c : cmd) {
            if (c.id == cmd_id) {
                String sms = c.smsText(data);
                if (Sms.send(getActivity(), car_id, c.id, sms)) {
                    Commands.put(getActivity(), car_id, c, data);
                    Intent intent = new Intent(getActivity(), FetchService.class);
                    intent.setAction(FetchService.ACTION_UPDATE);
                    intent.putExtra(Names.ID, car_id);
                    getActivity().startService(intent);
                }
                break;
            }
        }
    }

    void show_alert(CarConfig.Command cmd, int requestCode) {
        String name = cmd.name;
        int pos = name.indexOf('\n');
        if (pos > 0)
            name = name.substring(0, pos);
        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.run_command));
        builder.append(" \u00AB");
        builder.append(name);
        builder.append("\u00BB?");
        Alert dialog = new Alert();
        Bundle args = new Bundle();
        args.putString(Names.TITLE, name);
        args.putString(Names.MESSAGE, builder.toString());
        dialog.setArguments(args);
        dialog.setTargetFragment(this, requestCode);
        dialog.show(getFragmentManager(), "alert");
    }
}
