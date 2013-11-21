package net.ugona.plus;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

    static final int FLAG_AZ = 1;
    static final int FLAG_R1 = 2;

    Vector<Action> actions;

    ListView list;

    BroadcastReceiver br;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.actions, container, false);
        if (savedInstanceState != null)
            car_id = savedInstanceState.getString(Names.ID);

        fill_actions();

        list = (ListView) v.findViewById(R.id.actions);
        list.setAdapter(new BaseAdapter() {
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
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getActivity()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.action_item, null);
                }
                Action action = actions.get(position);
                TextView tv = (TextView) v.findViewById(R.id.name);
                tv.setText(action.text);
                ImageView iv = (ImageView) v.findViewById(R.id.icon);
                iv.setImageResource(action.icon);
                View ip = v.findViewById(R.id.progress);
                ip.setVisibility(SmsMonitor.isProcessed(car_id, action.text) ? View.VISIBLE : View.GONE);
                return v;
            }
        });
        list.setClickable(true);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                actions.get(position).action(getActivity(), car_id);
            }
        });
        View vLogo = v.findViewById(R.id.logo);
        vLogo.setClickable(true);
        vLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), About.class);
                startActivity(intent);
            }
        });
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String id = intent.getStringExtra(Names.ID);
                if (car_id.equals(id))
                    refresh();
            }
        };
        IntentFilter filter = new IntentFilter(SmsMonitor.SMS_SEND);
        filter.addAction(SmsMonitor.SMS_ANSWER);
        getActivity().registerReceiver(br, filter);
        return v;
    }

    @Override
    public void onDestroyView() {
        getActivity().unregisterReceiver(br);
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
        int flags = 0;
        if (preferences.getBoolean(Names.CAR_AUTOSTART + car_id, false))
            flags += FLAG_AZ;
        if (preferences.getBoolean(Names.CAR_RELE1 + car_id, false))
            flags += FLAG_R1;
        actions = new Vector<Action>();
        for (Action action : def_actions) {
            if ((action.flags > 0) && ((action.flags & flags) == 0))
                continue;
            actions.add(action);
        }
    }

    void refresh() {
        BaseAdapter adapter = (BaseAdapter) list.getAdapter();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void dateChanged(LocalDate current) {
        fill_actions();
        BaseAdapter adapter = (BaseAdapter) list.getAdapter();
        adapter.notifyDataSetChanged();
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

    static Action[] def_actions = {
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
            new Action(R.drawable.icon_motor_on, R.string.motor_on, FLAG_AZ) {
                @Override
                void action(Context context, String car_id) {
                    Actions.motor_on(context, car_id);
                }
            },
            new Action(R.drawable.icon_motor_off, R.string.motor_off, FLAG_AZ) {
                @Override
                void action(Context context, String car_id) {
                    Actions.motor_off(context, car_id);
                }
            },
            new Action(R.drawable.icon_heater, R.string.rele1, FLAG_R1) {
                @Override
                void action(Context context, String car_id) {
                    Actions.rele1(context, car_id);
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
            new Action(R.drawable.icon_reset, R.string.reset) {
                @Override
                void action(Context context, String car_id) {
                    Actions.reset(context, car_id);
                }
            },
    };

}
