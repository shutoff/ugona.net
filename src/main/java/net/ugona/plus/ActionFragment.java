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

    String car_id;

    ActionAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.actions, container, false);
        if (savedInstanceState != null)
            car_id = savedInstanceState.getString(Names.ID);

        ListView list = (ListView) v.findViewById(R.id.actions);
        adapter = new ActionAdapter(car_id);
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
        int flags = State.getCommands(preferences, car_id);
        adapter.actions = new Vector<Action>();
        for (Action action : def_actions) {
            if ((action.flags > 0) && ((action.flags & flags) == 0))
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

        ActionAdapter(String id) {
            car_id = id;
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
                    action.action(context, car_id);
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
            if (v == null) {

                v = inflater.inflate(R.layout.action_item, null);
            }
            TextView tv = (TextView) v.findViewById(R.id.name);
            tv.setText(action.text);
            if (action.icon > 0) {
                ImageView iv = (ImageView) v.findViewById(R.id.icon);
                iv.setImageResource(action.icon);
                iv.setVisibility(View.VISIBLE);
                v.findViewById(R.id.sum).setVisibility(View.GONE);
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

        Action(int icon_, int text_) {
            text = text_;
            icon = icon_;
            flags = 0;
        }

        Action(int icon_, int text_, int flags_) {
            text = text_;
            icon = icon_;
            flags = flags_;
        }


        abstract void action(Context context, String car_id);

        int icon;
        int text;
        int flags;
    }

    static Action[] pointer_actions = {
            new Action(R.drawable.icon_turbo_on, R.string.find) {
                @Override
                void action(final Context context, final String car_id) {
                    Actions.requestPassword(context, R.string.find, R.string.find_sum, new Runnable() {
                        @Override
                        public void run() {
                            SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.find, "FIND", null));
                        }
                    });
                }
            },
            new Action(R.drawable.icon_status, R.string.map_req) {
                @Override
                void action(final Context context, final String car_id) {
                    Actions.requestPassword(context, R.string.map_req, R.string.map_sum, new Runnable() {
                        @Override
                        public void run() {
                            SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.find, "MAP", null));
                        }
                    });
                }
            },
            new Action(0, R.string.mode_a, R.string.mode_a_sum) {
                @Override
                void action(final Context context, final String car_id) {
                    Actions.requestPassword(context, R.string.mode_a, R.string.mode_a_sum, new Runnable() {
                        @Override
                        public void run() {
                            SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.find, "MODE A", null));
                        }
                    });
                }
            },
            new Action(0, R.string.mode_b, R.string.mode_b_sum) {
                @Override
                void action(final Context context, final String car_id) {
                    Actions.requestPassword(context, R.string.mode_b, R.string.mode_b_sum, new Runnable() {
                        @Override
                        public void run() {
                            SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.find, "MODE B", null));
                        }
                    });
                }
            },
            new Action(0, R.string.mode_c, R.string.mode_c_sum) {
                @Override
                void action(final Context context, final String car_id) {
                    Actions.requestPassword(context, R.string.mode_c, R.string.mode_c_sum, new Runnable() {
                        @Override
                        public void run() {
                            SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.find, "MODE C", null));
                        }
                    });
                }
            },
            new Action(0, R.string.mode_d, R.string.mode_d_sum) {
                @Override
                void action(final Context context, final String car_id) {
                    Actions.requestPassword(context, R.string.mode_d, R.string.mode_d_sum, new Runnable() {
                        @Override
                        public void run() {
                            SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(R.string.find, "MODE D", null));
                        }
                    });
                }
            },
    };

    static Action[] def_actions = {
            new Action(R.drawable.icon_phone, R.string.call) {
                @Override
                void action(Context context, String car_id) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + preferences.getString(Names.CAR_PHONE + car_id, "")));
                    context.startActivity(intent);
                }
            },
            new Action(R.drawable.icon_valet_on, R.string.valet_on) {
                @Override
                void action(Context context, String car_id) {
                    Actions.valet_on(context, car_id);
                }
            },
            new Action(R.drawable.icon_valet_off, R.string.valet_off) {
                @Override
                void action(Context context, String car_id) {
                    Actions.valet_off(context, car_id);
                }
            },
            new Action(R.drawable.icon_motor_on, R.string.motor_on, State.CMD_AZ) {
                @Override
                void action(Context context, String car_id) {
                    Actions.motor_on(context, car_id);
                }
            },
            new Action(R.drawable.icon_motor_off, R.string.motor_off, State.CMD_AZ) {
                @Override
                void action(Context context, String car_id) {
                    Actions.motor_off(context, car_id);
                }
            },
            new Action(R.drawable.icon_heater, R.string.rele, State.CMD_RELE) {
                @Override
                void action(Context context, String car_id) {
                    Actions.rele1(context, car_id);
                }
            },
            new Action(R.drawable.icon_status, R.string.status_title) {
                @Override
                void action(Context context, String car_id) {
                    Actions.status(context, car_id);
                }
            },
            new Action(R.drawable.icon_block, R.string.block) {
                @Override
                void action(Context context, String car_id) {
                    Actions.block_motor(context, car_id);
                }
            },
            new Action(R.drawable.icon_turbo_on, R.string.turbo_on) {
                @Override
                void action(Context context, String car_id) {
                    Actions.turbo_on(context, car_id);
                }
            },
            new Action(R.drawable.icon_turbo_off, R.string.turbo_off) {
                @Override
                void action(Context context, String car_id) {
                    Actions.turbo_off(context, car_id);
                }
            },
            new Action(R.drawable.icon_internet_on, R.string.internet_on) {
                @Override
                void action(Context context, String car_id) {
                    Actions.internet_on(context, car_id);
                }
            },
            new Action(R.drawable.icon_internet_off, R.string.internet_off) {
                @Override
                void action(Context context, String car_id) {
                    Actions.internet_off(context, car_id);
                }
            },
            new Action(R.drawable.icon_status, R.string.map_req) {
                @Override
                void action(Context context, String car_id) {
                    Actions.map_query(context, car_id);
                }
            },
            new Action(R.drawable.balance, R.string.balance) {
                @Override
                void action(Context context, String car_id) {
                    Actions.balance(context, car_id);
                }
            },
            new Action(R.drawable.icon_reset, R.string.reset) {
                @Override
                void action(Context context, String car_id) {
                    Actions.reset(context, car_id);
                }
            },
    };

}
