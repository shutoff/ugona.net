package net.ugona.plus;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.joda.time.LocalDate;

import java.util.Vector;

public class ActionFragment extends Fragment
        implements MainActivity.DateChangeListener {

    static Action[] pointer_actions = {
            new Action(R.drawable.icon_turbo_on, R.string.find) {
                @Override
                void action(final Context context, final String car_id, boolean longTap) {
                    Actions.requestPassword(context, car_id, R.string.find, R.string.find_sum, new Actions.Answer() {
                        @Override
                        void answer(String pswd) {
                            SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.find, "FIND", null));
                        }
                    });
                }
            },
            new Action(R.drawable.icon_status, R.string.map_req) {
                @Override
                void action(final Context context, final String car_id, boolean longTap) {
                    Actions.requestPassword(context, car_id, R.string.map_req, R.string.map_sum, new Actions.Answer() {
                        @Override
                        void answer(String pswd) {
                            SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.map_req, "MAP", null));
                        }
                    });
                }
            },
            new Action(0, R.string.mode_a, R.string.mode_a_sum) {
                @Override
                void action(final Context context, final String car_id, boolean longTap) {
                    Actions.requestPassword(context, car_id, R.string.mode_a, R.string.mode_a_sum, new Actions.Answer() {
                        @Override
                        void answer(String pswd) {
                            SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.mode_a, "MODE A", null));
                        }
                    });
                }
            },
            new Action(0, R.string.mode_b, R.string.mode_b_sum) {
                @Override
                void action(final Context context, final String car_id, boolean longTap) {
                    Actions.requestPassword(context, car_id, R.string.mode_b, R.string.mode_b_sum, new Actions.Answer() {
                        @Override
                        void answer(String pswd) {
                            SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.mode_b, "MODE B", null));
                        }
                    });
                }
            },
            new Action(0, R.string.mode_c, R.string.mode_c_sum) {
                @Override
                void action(final Context context, final String car_id, boolean longTap) {
                    Actions.requestPassword(context, car_id, R.string.mode_c, R.string.mode_c_sum, new Actions.Answer() {
                        @Override
                        void answer(String pswd) {
                            SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.mode_c, "MODE C", null));
                        }
                    });
                }
            },
            new Action(0, R.string.mode_d, R.string.mode_d_sum) {
                @Override
                void action(final Context context, final String car_id, boolean longTap) {
                    Actions.requestPassword(context, car_id, R.string.mode_d, R.string.mode_d_sum, new Actions.Answer() {
                        @Override
                        void answer(String pswd) {
                            SmsMonitor.sendSMS(context, car_id, pswd, new SmsMonitor.Sms(R.string.mode_d, "MODE D", null));
                        }
                    });
                }
            },
    };

    static Action[] def_actions = {
            new Action(R.drawable.icon_phone, R.string.call) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + preferences.getString(Names.Car.CAR_PHONE + car_id, "")));
                    context.startActivity(intent);
                }
            },
            new Action(R.drawable.icon_status, R.string.search) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.search(context, car_id);
                }
            },
            new Action(R.drawable.icon_valet_on, R.string.valet_on, true) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.valet_on(context, car_id, longTap);
                }
            },
            new Action(R.drawable.icon_valet_off, R.string.valet_off, true) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.valet_off(context, car_id, longTap);
                }
            },
            new Action(R.drawable.icon_motor_on, R.string.motor_on, State.CMD_AZ) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.motor_on(context, car_id, longTap);
                }
            },
            new Action(R.drawable.icon_motor_off, R.string.motor_off, State.CMD_AZ) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.motor_off(context, car_id, longTap);
                }
            },
            new Action(R.drawable.icon_heater, R.string.rele, State.CMD_RELE) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.rele1(context, car_id, longTap);
                }
            },
            new Action(R.drawable.icon_heater, R.string.heater_on, State.CMD_THERMOCODE) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.heater_on(context, car_id, longTap, false);
                }
            },
            new Action(R.drawable.icon_heater_air, R.string.heater_air, State.CMD_THERMOCODE) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.heater_on_air(context, car_id, longTap, false);
                }
            },
            new Action(R.drawable.icon_air, R.string.air, State.CMD_THERMOCODE) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.heater_air(context, car_id, longTap, false);
                }
            },
            new Action(R.drawable.icon_heater_on, R.string.heater_off, State.CMD_THERMOCODE) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.heater_off(context, car_id, longTap, false);
                }
            },
            new Action(R.drawable.rele1_on, R.string.rele1_on, State.CMD_RELE1, Names.Car.RELE1_NAME) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.rele(context, car_id, R.string.rele1_on, longTap);
                }
            },
            new Action(R.drawable.rele1_off, R.string.rele1_off, State.CMD_RELE1, Names.Car.RELE1_NAME) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.rele(context, car_id, R.string.rele1_off, longTap);
                }
            },
            new Action(R.drawable.rele1_impulse, R.string.rele1i, State.CMD_RELE1I, Names.Car.RELE1I_NAME) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.rele(context, car_id, R.string.rele1i, longTap);
                }
            },
            new Action(R.drawable.rele2_on, R.string.rele2_on, State.CMD_RELE2, Names.Car.RELE2_NAME) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.rele(context, car_id, R.string.rele2_on, longTap);
                }
            },
            new Action(R.drawable.rele2_off, R.string.rele2_off, State.CMD_RELE2, Names.Car.RELE2_NAME) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.rele(context, car_id, R.string.rele2_off, longTap);
                }
            },
            new Action(R.drawable.rele2_impulse, R.string.rele2i, State.CMD_RELE2I, Names.Car.RELE2I_NAME) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.rele(context, car_id, R.string.rele2i, longTap);
                }
            },
            new Action(R.drawable.icon_status, R.string.status_title) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.status(context, car_id);
                }
            },
            new Action(R.drawable.icon_block, R.string.block, true) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.block_motor(context, car_id);
                }
            },
            new Action(R.drawable.sound_off, R.string.sound_off, true) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.sound_off(context, car_id);
                }
            },
            new Action(R.drawable.sound, R.string.sound_on, true) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.sound_on(context, car_id);
                }
            },
            new Action(R.drawable.icon_turbo_on, R.string.turbo_on) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.turbo_on(context, car_id);
                }
            },
            new Action(R.drawable.icon_turbo_off, R.string.turbo_off) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.turbo_off(context, car_id);
                }
            },
            new Action(R.drawable.icon_internet_on, R.string.internet_on) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.internet_on(context, car_id);
                }
            },
            new Action(R.drawable.icon_internet_off, R.string.internet_off) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.internet_off(context, car_id);
                }
            },
            new Action(R.drawable.icon_status, R.string.map_req) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.map_query(context, car_id);
                }
            },
            new Action(R.drawable.balance, R.string.balance) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.balance(context, car_id);
                }
            },
            new Action(R.drawable.icon_reset, R.string.reset) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.reset(context, car_id);
                }
            },
    };

    static Action[] pandora_actions = {
            new Action(R.drawable.icon_phone, R.string.call) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + preferences.getString(Names.Car.CAR_PHONE + car_id, "")));
                    context.startActivity(intent);
                }
            },
            new Action(R.drawable.icon_guard_on, R.string.guard_on) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.send_pandora_cmd(context, car_id, 1);
                }
            },
            new Action(R.drawable.icon_guard_off, R.string.guard_off) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.send_pandora_cmd(context, car_id, 2);
                }
            },
            new Action(R.drawable.icon_motor_on, R.string.motor_on, State.CMD_AZ) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.send_pandora_cmd(context, car_id, 4);
                }
            },
            new Action(R.drawable.icon_motor_off, R.string.motor_off, State.CMD_AZ) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.send_pandora_cmd(context, car_id, 8);
                }
            },
            new Action(R.drawable.icon_turbo_on, R.string.turbo_on) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.send_pandora_cmd(context, car_id, 0x10);
                }
            },
            new Action(R.drawable.icon_turbo_off, R.string.turbo_off) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.send_pandora_cmd(context, car_id, 0x20);
                }
            },
            new Action(R.drawable.icon_heater, R.string.rele, State.CMD_RELE) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.send_pandora_cmd(context, car_id, 0x15);
                }
            },
            new Action(R.drawable.icon_heater_on, R.string.heater_off, State.CMD_RELE) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.send_pandora_cmd(context, car_id, 0x16);
                }
            },
            new Action(R.drawable.sound, R.string.sound) {
                @Override
                void action(Context context, String car_id, boolean longTap) {
                    Actions.send_pandora_cmd(context, car_id, 0x17);
                }
            },
    };

    String car_id;
    ActionAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.actions, container, false);
        if (savedInstanceState != null)
            car_id = savedInstanceState.getString(Names.ID);

        ListView list = (ListView) v.findViewById(R.id.actions);
        adapter = new ActionAdapter(getActivity(), car_id);
        fill_actions();
        adapter.attach(getActivity(), list);

        View vLogo = v.findViewById(R.id.logo);
        vLogo.setClickable(true);
        vLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), About.class);
                startActivity(intent);
            }
        });
        return v;
    }

    @Override
    public void onDestroyView() {
        adapter.detach(getActivity());
        super.onDestroyView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        MainActivity mainActivity = (MainActivity) activity;
        mainActivity.registerDateListener(this);
    }

    @Override
    public void onDestroy() {
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.unregisterDateListener(this);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
    }

    void fill_actions() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        adapter.actions = new Vector<Action>();
        if (State.isPandora(preferences, car_id)) {
            int flags = State.getCommands(preferences, car_id);
            for (Action action : pandora_actions) {
                if ((action.flags > 0) && ((action.flags & flags) == 0))
                    continue;
                adapter.actions.add(action);
            }
            return;
        }

        int flags = State.getCommands(preferences, car_id);
        if ((flags & State.CMD_RELE) != 0) {
            if (preferences.getString(Names.Car.CAR_RELE + car_id, "").equals("3")) {
                flags |= State.CMD_THERMOCODE;
                flags &= ~State.CMD_RELE;
            }
        }
        for (Action action : def_actions) {
            if ((action.flags > 0) && ((action.flags & flags) == 0))
                continue;
            if (!State.hasTelephony(getActivity()) && !action.internet)
                continue;
            adapter.actions.add(action);
        }
    }

    @Override
    public void dateChanged(LocalDate current) {
        fill_actions();
        adapter.notifyDataSetChanged();
    }

    static class ActionAdapter extends BaseAdapter {

        Vector<Action> actions;
        String car_id;
        BroadcastReceiver br;
        LayoutInflater inflater;
        SharedPreferences preferences;

        ActionAdapter(Context context, String id) {
            car_id = id;
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
        }

        void attach(final Context context, ListView list) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            list.setAdapter(this);
            list.setClickable(true);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Action action = actions.get(position);
                    if (SmsMonitor.isProcessed(car_id, action.text)) {
                        SmsMonitor.cancelSMS(context, car_id, action.text);
                        notifyDataSetChanged();
                        return;
                    }
                    action.action(context, car_id, false);
                }
            });
            list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    Action action = actions.get(position);
                    if (SmsMonitor.isProcessed(car_id, action.text)) {
                        SmsMonitor.cancelSMS(context, car_id, action.text);
                        notifyDataSetChanged();
                        return true;
                    }
                    action.action(context, car_id, true);
                    return true;
                }
            });
            br = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String id = intent.getStringExtra(Names.ID);
                    if (car_id.equals(id))
                        notifyDataSetChanged();
                }
            };
            IntentFilter filter = new IntentFilter(SmsMonitor.SMS_SEND);
            filter.addAction(SmsMonitor.SMS_ANSWER);
            filter.addAction(FetchService.ACTION_UPDATE_FORCE);
            context.registerReceiver(br, filter);
        }

        void detach(Context context) {
            context.unregisterReceiver(br);
        }

        @Override
        public int getCount() {
            return actions.size();
        }

        @Override
        public Object getItem(int position) {
            return actions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            Action action = actions.get(position);
            if (v == null)
                v = inflater.inflate(R.layout.action_item, null);
            TextView tv = (TextView) v.findViewById(R.id.name);
            tv.setText(action.text);
            if (action.icon > 0) {
                ImageView iv = (ImageView) v.findViewById(R.id.icon);
                iv.setImageResource(action.icon);
                iv.setVisibility(View.VISIBLE);
                TextView tvSum = (TextView) v.findViewById(R.id.sum);
                if (action.name_key == null) {
                    tvSum.setVisibility(View.GONE);
                } else {
                    String n = preferences.getString(action.name_key + car_id, "");
                    if (n.equals("")) {
                        tvSum.setVisibility(View.GONE);
                    } else {
                        tvSum.setVisibility(View.VISIBLE);
                        tvSum.setText(action.text);
                        tv.setText(n);
                    }
                }
            } else {
                v.findViewById(R.id.icon).setVisibility(View.GONE);
                TextView ts = (TextView) v.findViewById(R.id.sum);
                ts.setVisibility(View.VISIBLE);
                ts.setText(action.flags);
            }
            View ip = v.findViewById(R.id.progress);
            ip.setVisibility(SmsMonitor.isProcessed(car_id, action.text) ? View.VISIBLE : View.GONE);
            return v;
        }
    }

    static abstract class Action {

        int icon;
        int text;
        int flags;
        boolean internet;
        String name_key;

        Action(int icon_, int text_) {
            text = text_;
            icon = icon_;
            flags = 0;
            internet = false;
        }

        Action(int icon_, int text_, boolean internet_) {
            text = text_;
            icon = icon_;
            flags = 0;
            internet = internet_;
        }

        Action(int icon_, int text_, int flags_) {
            text = text_;
            icon = icon_;
            flags = flags_;
            internet = true;
        }

        Action(int icon_, int text_, int flags_, String key) {
            text = text_;
            icon = icon_;
            flags = flags_;
            internet = true;
            name_key = key;
        }

        abstract void action(Context context, String car_id, boolean longTap);
    }

}
