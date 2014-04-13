package net.ugona.plus;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import org.joda.time.LocalDateTime;

import java.text.DateFormat;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class StateFragment extends Fragment
        implements View.OnTouchListener, OnRefreshListener {

    static Pattern number_pattern = Pattern.compile("^[0-9]+ ?");
    final String GSM_URL = "https://car-online.ugona.net/gsm?skey=$1&cc=$2&nc=$3&lac=$4&cid=$5";
    final int AUTH_REQUEST = 1;
    String car_id;
    SharedPreferences preferences;
    CarView vCar;
    View vAddress;
    TextView tvTime;
    TextView tvLocation;
    TextView tvAddress2;
    TextView tvAddress3;
    TextView tvLast;
    TextView tvVoltage;
    TextView tvReserve;
    TextView tvBalance;
    TextView tvTemperature;
    TextView tvTemperature2;
    TextView tvTemperature3;
    TextView tvError;
    View vTemperature;
    View vTemperature2;
    View vTemperature3;
    View vError;
    ImageView imgRefresh;
    ProgressBar prgUpdate;
    View vMotor;
    View vRele;
    View vBlock;
    View vValet;
    View vPhone;
    View vSearch;
    View vRele1;
    View vRele1i;
    View vRele2;
    View vRele2i;
    View vSound;
    View pMotor;
    View pRele;
    View pBlock;
    View pValet;
    View pRele1;
    View pRele1i;
    View pRele2;
    View pRele2i;
    View pSound;
    ImageView ivMotor;
    ImageView ivRele;
    ImageView ivValet;
    ImageView ivRele1;
    ImageView ivRele2;
    ImageView ivSound;

    View vLevel;
    TextView tvLevel;
    ImageView ivLevel;

    View mValet;
    View mNet;
    View balanceBlock;
    View vTime;
    ScrollView svAddress;
    CarDrawable drawable;
    BroadcastReceiver br;
    View vPointer1;
    View vPointer2;
    TextView tvPointer1;
    TextView tvPointer2;
    ActionFragment.ActionAdapter adapter;
    PullToRefreshLayout mPullToRefreshLayout;
    CountDownTimer longTapTimer;
    boolean pointer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final Context context = getActivity();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (savedInstanceState != null)
            car_id = savedInstanceState.getString(Names.ID);

        pointer = preferences.getBoolean(Names.Car.POINTER + car_id, false);

        View v = inflater.inflate(pointer ? R.layout.pointer : R.layout.state, container, false);

        vCar = (CarView) v.findViewById(R.id.car);

        vAddress = v.findViewById(R.id.address);
        tvTime = (TextView) v.findViewById(R.id.addr_time);
        tvLocation = (TextView) v.findViewById(R.id.location);
        tvAddress2 = (TextView) v.findViewById(R.id.address2);
        svAddress = (ScrollView) v.findViewById(R.id.addr_block);

        tvAddress3 = (TextView) v.findViewById(R.id.address3);
        tvLast = (TextView) v.findViewById(R.id.last);
        tvVoltage = (TextView) v.findViewById(R.id.voltage);
        tvReserve = (TextView) v.findViewById(R.id.reserve);
        tvBalance = (TextView) v.findViewById(R.id.balance);
        tvTemperature = (TextView) v.findViewById(R.id.temperature);
        vTemperature = v.findViewById(R.id.temperature_block);
        tvTemperature2 = (TextView) v.findViewById(R.id.temperature2);
        vTemperature2 = v.findViewById(R.id.temperature2_block);
        tvTemperature3 = (TextView) v.findViewById(R.id.temperature3);
        vTemperature3 = v.findViewById(R.id.temperature3_block);

        vMotor = v.findViewById(R.id.motor);
        vRele = v.findViewById(R.id.rele);
        vBlock = v.findViewById(R.id.block);
        vValet = v.findViewById(R.id.valet);
        vPhone = v.findViewById(R.id.phone);
        vSearch = v.findViewById(R.id.search);
        vRele1 = v.findViewById(R.id.rele1);
        vRele1i = v.findViewById(R.id.rele1_impulse);
        vRele2 = v.findViewById(R.id.rele2);
        vRele2i = v.findViewById(R.id.rele2_impulse);
        vSound = v.findViewById(R.id.sound);

        pMotor = v.findViewById(R.id.motor_prg);
        pRele = v.findViewById(R.id.rele_prg);
        pBlock = v.findViewById(R.id.block_prg);
        pValet = v.findViewById(R.id.valet_prg);
        pRele1 = v.findViewById(R.id.rele1_prg);
        pRele1i = v.findViewById(R.id.rele1_impulse_prg);
        pRele2 = v.findViewById(R.id.rele2_prg);
        pRele2i = v.findViewById(R.id.rele2_impulse_prg);
        pSound = v.findViewById(R.id.sound_prg);

        ivMotor = (ImageView) v.findViewById(R.id.motor_img);
        ivValet = (ImageView) v.findViewById(R.id.valet_img);
        ivRele = (ImageView) v.findViewById(R.id.rele_img);
        ivRele1 = (ImageView) v.findViewById(R.id.rele1_img);
        ivRele2 = (ImageView) v.findViewById(R.id.rele2_img);
        ivSound = (ImageView) v.findViewById(R.id.sound_img);

        vLevel = v.findViewById(R.id.level_row);
        ivLevel = (ImageView) v.findViewById(R.id.level_img);
        tvLevel = (TextView) v.findViewById(R.id.level);

        mValet = v.findViewById(R.id.valet_warning);
        mNet = v.findViewById(R.id.net_warning);

        if (!pointer) {
            vMotor.setOnTouchListener(this);
            vRele.setOnTouchListener(this);
            vBlock.setOnTouchListener(this);
            vValet.setOnTouchListener(this);
            vPhone.setOnTouchListener(this);
            vSearch.setOnTouchListener(this);
            vRele1.setOnTouchListener(this);
            vRele1i.setOnTouchListener(this);
            vRele2.setOnTouchListener(this);
            vRele2i.setOnTouchListener(this);
            vSound.setOnTouchListener(this);
        }

        balanceBlock = v.findViewById(R.id.balance_block);

        tvError = (TextView) v.findViewById(R.id.error_text);
        vError = v.findViewById(R.id.error);
        vError.setVisibility(View.GONE);

        imgRefresh = (ImageView) v.findViewById(R.id.refresh);
        prgUpdate = (ProgressBar) v.findViewById(R.id.update);
        prgUpdate.setVisibility(View.GONE);

        vTime = v.findViewById(R.id.time);

        vAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMap();
            }
        });
        View v_addr = v.findViewById(R.id.addr_data);
        v_addr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMap();
            }
        });

        vTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startUpdate(context);
            }
        });

        drawable = new CarDrawable();

        vPointer1 = v.findViewById(R.id.pointers1);
        vPointer2 = v.findViewById(R.id.pointers2);
        if (vPointer1 != null) {
            tvPointer1 = (TextView) v.findViewById(R.id.pointer1);
            vPointer1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPointer(0);
                }
            });
            tvPointer2 = (TextView) v.findViewById(R.id.pointer2);
            vPointer2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPointer(1);
                }
            });
        }

        update(context);

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null)
                    return;
                if (intent.getAction().equals(FetchService.ACTION_UPDATE_FORCE)) {
                    update(context);
                    return;
                }
                if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    updateNetStatus(context);
                    return;
                }
                if (!car_id.equals(intent.getStringExtra(Names.ID)))
                    return;
                if (intent.getAction().equals(FetchService.ACTION_UPDATE)) {
                    vError.setVisibility(View.GONE);
                    imgRefresh.setVisibility(View.VISIBLE);
                    prgUpdate.setVisibility(View.GONE);
                    update(context);
                }
                if (intent.getAction().equals(SmsMonitor.SMS_SEND)) {
                    update(context);
                }
                if (intent.getAction().equals(SmsMonitor.SMS_ANSWER)) {
                    update(context);
                }
                if (intent.getAction().equals(FetchService.ACTION_START)) {
                    prgUpdate.setVisibility(View.VISIBLE);
                    imgRefresh.setVisibility(View.GONE);
                }
                if (intent.getAction().equals(FetchService.ACTION_NOUPDATE)) {
                    vError.setVisibility(View.GONE);
                    imgRefresh.setVisibility(View.VISIBLE);
                    prgUpdate.setVisibility(View.GONE);
                    mPullToRefreshLayout.setRefreshComplete();
                }
                if (intent.getAction().equals(FetchService.ACTION_ERROR)) {
                    String error_text = intent.getStringExtra(Names.ERROR);
                    if (error_text == null)
                        error_text = getString(R.string.data_error);
                    if (error_text.equals("Auth error")) {
                        HttpTask task = new HttpTask() {
                            @Override
                            void result(JsonObject res) throws ParseException {
                                String key = res.get("key").asString();
                                SharedPreferences.Editor ed = preferences.edit();
                                ed.putString(Names.Car.CAR_KEY + car_id, key);
                                ed.commit();
                                startUpdate(getActivity());
                            }

                            @Override
                            void error() {
                                showAuth();
                            }
                        };
                        task.execute(AuthDialog.URL_KEY, preferences.getString(Names.Car.AUTH + car_id, ""));
                        return;
                    }
                    tvError.setText(error_text);
                    vError.setVisibility(View.VISIBLE);
                    imgRefresh.setVisibility(View.VISIBLE);
                    prgUpdate.setVisibility(View.GONE);
                }
            }
        };
        IntentFilter intFilter = new IntentFilter(FetchService.ACTION_UPDATE);
        intFilter.addAction(FetchService.ACTION_NOUPDATE);
        intFilter.addAction(FetchService.ACTION_ERROR);
        intFilter.addAction(FetchService.ACTION_UPDATE_FORCE);
        intFilter.addAction(SmsMonitor.SMS_SEND);
        intFilter.addAction(SmsMonitor.SMS_ANSWER);
        intFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(br, intFilter);

        if (FetchService.isProcessed(car_id)) {
            prgUpdate.setVisibility(View.VISIBLE);
            imgRefresh.setVisibility(View.GONE);
        }

        if (pointer) {
            ListView lvActions = (ListView) v.findViewById(R.id.actions);
            adapter = new ActionFragment.ActionAdapter(getActivity(), car_id);
            adapter.actions = new Vector<ActionFragment.Action>();
            for (ActionFragment.Action action : ActionFragment.pointer_actions) {
                adapter.actions.add(action);
            }
            adapter.attach(getActivity(), lvActions);
        }
        startUpdate(getActivity());

        return v;
    }

    @Override
    public void onDestroyView() {
        if (adapter != null)
            adapter.detach(getActivity());
        getActivity().unregisterReceiver(br);
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == AUTH_REQUEST) && (requestCode == Activity.RESULT_OK))
            startUpdate(getActivity());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroup viewGroup = (ViewGroup) view;
        mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());
        ActionBarPullToRefresh.from(getActivity())
                .insertLayoutInto(viewGroup)
                .allChildrenArePullable()
                .listener(this)
                .setup(mPullToRefreshLayout);
        mPullToRefreshLayout.setPullEnabled(true);
    }

    void showAuth() {
        if (AuthDialog.is_show || (getActivity() == null))
            return;
        Intent i = new Intent(getActivity(), AuthDialog.class);
        i.putExtra(Names.ID, car_id);
        i.putExtra(Names.Car.AUTH, true);
        startActivityForResult(i, AUTH_REQUEST);
    }

    void update(Context context) {
        if (mPullToRefreshLayout != null)
            mPullToRefreshLayout.setRefreshComplete();

        long last = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
        if (last != 0) {
            DateFormat df = android.text.format.DateFormat.getDateFormat(context);
            DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
            tvLast.setText(df.format(last) + " " + tf.format(last));
        } else {
            tvLast.setText(getString(R.string.unknown));
        }

        boolean az = preferences.getBoolean(Names.Car.AZ + car_id, false);
        boolean ignition = !az && (preferences.getBoolean(Names.Car.INPUT3 + car_id, false) || preferences.getBoolean(Names.Car.ZONE_IGNITION + car_id, false));

        updateMainVoltage(tvVoltage, (pointer || az || ignition) ? 3. : 12.2);
        updateNetStatus(context);

        double lat = preferences.getFloat(Names.Car.LAT + car_id, 0);
        double lon = preferences.getFloat(Names.Car.LNG + car_id, 0);
        String location = "";
        if ((lat == 0) && (lon == 0)) {
            String gsm = preferences.getString(Names.Car.GSM_SECTOR + car_id, "");
            if (!gsm.equals("")) {
                String[] parts = gsm.split(" ");
                location = parts[0] + "-" + parts[1] + " LAC:" + parts[2] + " CID:" + parts[3];
                String gsm_zone = preferences.getString(Names.Car.GSM_ZONE + car_id, "");
                if (gsm_zone.equals("")) {
                    HttpTask task = new HttpTask() {
                        @Override
                        void result(JsonObject res) throws ParseException {
                            String sector = res.get("sector").asString();
                            SharedPreferences.Editor ed = preferences.edit();
                            ed.putString(Names.Car.GSM_ZONE + car_id, sector);
                            ed.commit();
                            updateZone(sector);
                        }

                        @Override
                        void error() {
                        }
                    };
                    String skey = preferences.getString(Names.Car.CAR_KEY + car_id, "");
                    String[] gsm_parts = gsm.split(" ");
                    task.execute(GSM_URL, skey, gsm_parts[0], gsm_parts[1], gsm_parts[2], gsm_parts[3]);
                } else {
                    updateZone(gsm_zone);
                }
            }
        } else {
            location = preferences.getFloat(Names.Car.LAT + car_id, 0) + " ";
            location += preferences.getFloat(Names.Car.LNG + car_id, 0);
            Address req = new Address() {
                @Override
                void result(String addr) {
                    updateAddress(addr);
                }
            };
            req.get(getActivity(), lat, lon);
        }
        tvLocation.setText(location);

        String balance = preferences.getString(Names.Car.BALANCE + car_id, "");
        if (!balance.equals("") && preferences.getBoolean(Names.Car.SHOW_BALANCE + car_id, true)) {
            int balance_limit = 50;
            try {
                balance_limit = preferences.getInt(Names.Car.LIMIT + car_id, 50);
            } catch (Exception ex) {
                // ignore
            }
            try {
                double b = Double.parseDouble(balance);
                tvBalance.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
                if ((b <= balance_limit) && (balance_limit > 0))
                    tvBalance.setTextColor(getResources().getColor(R.color.error));
                balance = String.format("%.2f", b);
            } catch (Exception ex) {
                // ignore
            }
            tvBalance.setText(balance);
            balanceBlock.setVisibility(View.VISIBLE);
        } else {
            balanceBlock.setVisibility(View.GONE);
        }

        if (pointer)
            return;

        int level = preferences.getInt(Names.Car.GSM_DB + car_id, 0);
        if (level == 0) {
            vLevel.setVisibility(View.GONE);
        } else {
            vLevel.setVisibility(View.VISIBLE);
            if (level > -51) {
                ivLevel.setImageResource(R.drawable.gsm_level5);
            } else if (level > -65) {
                ivLevel.setImageResource(R.drawable.gsm_level4);
            } else if (level > -77) {
                ivLevel.setImageResource(R.drawable.gsm_level3);
            } else if (level > -91) {
                ivLevel.setImageResource(R.drawable.gsm_level2);
            } else if (level > -105) {
                ivLevel.setImageResource(R.drawable.gsm_level1);
            } else {
                ivLevel.setImageResource(R.drawable.gsm_level0);
            }
            tvLevel.setText(level + " dBm");
        }

        updateReserveVoltage(tvReserve, preferences.getBoolean(Names.Car.RESERVE_NORMAL + car_id, true));

        String temperature = Preferences.getTemperature(preferences, car_id, 1);
        if (temperature == null) {
            vTemperature.setVisibility(View.GONE);
        } else {
            tvTemperature.setText(temperature);
            vTemperature.setVisibility(View.VISIBLE);
            vCar.setT1(temperature);
            vCar.setT2(temperature);
            vCar.setT3(temperature);
        }
        temperature = Preferences.getTemperature(preferences, car_id, 2);
        if (temperature == null) {
            vTemperature2.setVisibility(View.GONE);
        } else {
            tvTemperature2.setText(temperature);
            vTemperature2.setVisibility(View.VISIBLE);
        }
        temperature = Preferences.getTemperature(preferences, car_id, 3);
        if (temperature == null) {
            vTemperature3.setVisibility(View.GONE);
        } else {
            tvTemperature3.setText(temperature);
            vTemperature3.setVisibility(View.VISIBLE);
        }

        vCar.setDrawable(drawable.getDrawable(context, car_id));
        String time = "";
        long last_stand = preferences.getLong(Names.Car.LAST_STAND + car_id, 0);
        if (last_stand > 0) {
            LocalDateTime stand = new LocalDateTime(last_stand);
            LocalDateTime now = new LocalDateTime();
            if (stand.toLocalDate().equals(now.toLocalDate())) {
                DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
                time = tf.format(last_stand);
            } else {
                DateFormat df = android.text.format.DateFormat.getDateFormat(context);
                DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
                time = df.format(last_stand) + " " + tf.format(last_stand);
            }
            time += " ";
        } else if (last_stand < 0) {
            double speed = preferences.getFloat(Names.Car.SPEED + car_id, 0);
            if (speed > 0)
                time += " " + speed + " " + getString(R.string.kmh) + " ";
        }
        tvTime.setText(time);

        int commands = State.getCommands(preferences, car_id);
        boolean block = !preferences.getBoolean(Names.Car.GUARD0 + car_id, false) && preferences.getBoolean(Names.Car.GUARD1 + car_id, false);

        if (((commands & State.CMD_AZ) != 0) && (!ignition || az)) {
            vMotor.setVisibility(View.VISIBLE);
            vMotor.setTag(az);
            if (az) {
                ivMotor.setImageResource(R.drawable.icon_motor_off);
                pMotor.setVisibility(SmsMonitor.isProcessed(car_id, R.string.motor_off) ? View.VISIBLE : View.GONE);
            } else {
                ivMotor.setImageResource(R.drawable.icon_motor_on);
                pMotor.setVisibility(SmsMonitor.isProcessed(car_id, R.string.motor_on) ? View.VISIBLE : View.GONE);
            }
        } else {
            vMotor.setVisibility(View.GONE);
        }
        if (((commands & State.CMD_RELE) != 0) && !ignition) {
            vRele.setVisibility(View.VISIBLE);
            pRele.setVisibility(SmsMonitor.isProcessed(car_id, R.string.rele) ? View.VISIBLE : View.GONE);
            if (Preferences.getRele(preferences, car_id)) {
                ivRele.setImageResource(R.drawable.icon_heater_on);
            } else {
                ivRele.setImageResource(R.drawable.icon_heater);
            }
        } else {
            vRele.setVisibility(View.GONE);
        }
        if (State.hasTelephony(context) && ignition && !az && !block && !preferences.getBoolean(Names.Car.GUARD + car_id, false)) {
            vBlock.setVisibility(View.VISIBLE);
            pBlock.setVisibility(SmsMonitor.isProcessed(car_id, R.string.block) ? View.VISIBLE : View.GONE);
        } else {
            vBlock.setVisibility(View.GONE);
        }
        boolean valet = preferences.getBoolean(Names.Car.GUARD0 + car_id, false) && !preferences.getBoolean(Names.Car.GUARD1 + car_id, false);
        if ((((commands & State.CMD_VALET) != 0) || valet || block)) {
            if (valet) {
                ivValet.setImageResource(R.drawable.icon_valet_off);
                pValet.setVisibility(SmsMonitor.isProcessed(car_id, R.string.valet_off) ? View.VISIBLE : View.GONE);
            } else {
                ivValet.setImageResource(R.drawable.icon_valet_on);
                pValet.setVisibility(SmsMonitor.isProcessed(car_id, R.string.valet_on) ? View.VISIBLE : View.GONE);
            }
            vValet.setVisibility(View.VISIBLE);
        } else {
            vValet.setVisibility(View.GONE);
        }
        if (State.hasTelephony(context) && ((commands & State.CMD_CALL) != 0)) {
            vPhone.setVisibility(View.VISIBLE);
        } else {
            vPhone.setVisibility(View.GONE);
        }
        if (State.hasTelephony(context) && ((commands & State.CMD_SEARCH) != 0)) {
            vSearch.setVisibility(View.VISIBLE);
        } else {
            vSearch.setVisibility(View.GONE);
        }

        if ((commands & State.CMD_RELE1) != 0) {
            vRele1.setVisibility(View.VISIBLE);
            pRele1.setVisibility(SmsMonitor.isProcessed(car_id, R.string.rele1) ? View.VISIBLE : View.GONE);
            if (preferences.getBoolean(Names.Car.RELAY1 + car_id, false)) {
                ivRele1.setImageResource(R.drawable.rele1_off);
            } else {
                ivRele1.setImageResource(R.drawable.rele1_on);
            }
        } else {
            vRele1.setVisibility(View.GONE);
        }
        if ((commands & State.CMD_RELE1I) != 0) {
            vRele1i.setVisibility(View.VISIBLE);
            pRele1i.setVisibility(SmsMonitor.isProcessed(car_id, R.string.rele1i) ? View.VISIBLE : View.GONE);
        } else {
            vRele1i.setVisibility(View.GONE);
        }

        if ((commands & State.CMD_RELE2) != 0) {
            vRele2.setVisibility(View.VISIBLE);
            pRele2.setVisibility(SmsMonitor.isProcessed(car_id, R.string.rele2) ? View.VISIBLE : View.GONE);
            if (preferences.getBoolean(Names.Car.RELAY2 + car_id, false)) {
                ivRele2.setImageResource(R.drawable.rele2_off);
            } else {
                ivRele2.setImageResource(R.drawable.rele2_on);
            }
        } else {
            vRele2.setVisibility(View.GONE);
        }
        if ((commands & State.CMD_RELE2I) != 0) {
            vRele2i.setVisibility(View.VISIBLE);
            pRele2i.setVisibility(SmsMonitor.isProcessed(car_id, R.string.rele2i) ? View.VISIBLE : View.GONE);
        } else {
            vRele2i.setVisibility(View.GONE);
        }

        if ((commands & State.CMD_SOUND) != 0) {
            vSound.setVisibility(View.VISIBLE);
            if ((preferences.getInt("V_12_" + car_id, 0) & 8) != 0) {
                ivSound.setImageResource(R.drawable.sound);
                pSound.setVisibility(SmsMonitor.isProcessed(car_id, R.string.sound_off) ? View.VISIBLE : View.GONE);
            } else {
                ivSound.setImageResource(R.drawable.sound_off);
                pSound.setVisibility(SmsMonitor.isProcessed(car_id, R.string.sound_on) ? View.VISIBLE : View.GONE);
            }
        } else {
            vSound.setVisibility(View.GONE);
        }

        if (az) {
            if (preferences.getBoolean(Names.Car.GUARD0 + car_id, false) && preferences.getBoolean(Names.Car.GUARD1 + car_id, false)) {
                vCar.setEngine(R.drawable.engine_blue);
            } else {
                vCar.setEngine(R.drawable.engine);
            }
            vCar.setEngineVisible(true);
            startAnimation();
        } else {
            vCar.setEngineVisible(false);
        }

        mValet.setVisibility(valet ? View.VISIBLE : View.GONE);

        setPointer(vPointer1, tvPointer1, 0);
        setPointer(vPointer2, tvPointer2, 1);
    }


    void updateAddress(String addr) {
        if (addr == null)
            return;
        String parts[] = addr.split(", ");
        addr = parts[0];
        int start = 2;
        if (parts.length > 1)
            addr += ", " + parts[1];
        if (parts.length > 2) {
            Matcher matcher = number_pattern.matcher(parts[2]);
            if (matcher.matches()) {
                addr += ", " + parts[2];
                start++;
            }
        }
        tvAddress2.setText(addr);
        addr = "";
        for (int i = start; i < parts.length; i++) {
            if (!addr.equals(""))
                addr += ", ";
            addr += parts[i];
        }
        tvAddress3.setText(addr);
        svAddress.scrollTo(0, 0);
    }

    void updateZone(String zone) {
        String points[] = zone.split("_");
        double min_lat = 180;
        double max_lat = -180;
        double min_lon = 180;
        double max_lon = -180;
        for (String point : points) {
            try {
                String[] p = point.split(",");
                double p_lat = Double.parseDouble(p[0]);
                double p_lon = Double.parseDouble(p[1]);
                if (p_lat > max_lat)
                    max_lat = p_lat;
                if (p_lat < min_lat)
                    min_lat = p_lat;
                if (p_lon > max_lon)
                    max_lon = p_lon;
                if (p_lon < min_lon)
                    min_lon = p_lon;
            } catch (Exception ex) {
                // ignore
            }
        }
        Address req = new Address() {
            @Override
            void result(String addr) {
                updateAddress(addr);
            }
        };
        req.get(getActivity(), (min_lat + max_lat) / 2, (min_lon + max_lon) / 2);
    }

    void updateReserveVoltage(TextView tv, boolean normal) {
        String val = preferences.getString(Names.Car.VOLTAGE_RESERVED + car_id, "?");
        try {
            double v = Double.parseDouble(val);
            val = String.format("%.2f", v);
        } catch (Exception ex) {
            // ignore
        }
        tv.setText(val + " V");
        tv.setTextColor(normal ? getResources().getColor(android.R.color.secondary_text_dark) : getResources().getColor(R.color.error));
    }

    void updateMainVoltage(TextView tv, double limit) {
        String val = preferences.getString(Names.Car.VOLTAGE_MAIN + car_id, "?");
        boolean normal = false;
        try {
            double v = Double.parseDouble(val);
            v += preferences.getInt(Names.Car.VOLTAGE_SHIFT + car_id, 0) / 20.;
            val = String.format("%.2f", v);
            if (v > limit)
                normal = true;
        } catch (Exception ex) {
            // ignore
        }
        tv.setText(val + " V");
        tv.setTextColor(normal ? getResources().getColor(android.R.color.secondary_text_dark) : getResources().getColor(R.color.error));
    }

    void updateNetStatus(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        mNet.setVisibility(isConnected ? View.GONE : View.VISIBLE);
    }

    void startUpdate(Context context) {
        Intent intent = new Intent(context, FetchService.class);
        intent.putExtra(Names.ID, car_id);
        context.startService(intent);
        vError.setVisibility(View.GONE);
        imgRefresh.setVisibility(View.GONE);
        prgUpdate.setVisibility(View.VISIBLE);
        String pointers = preferences.getString(Names.Car.POINTERS + car_id, "");
        if (pointers.equals(""))
            return;
        for (String p : pointers.split(",")) {
            Intent i = new Intent(context, FetchService.class);
            i.putExtra(Names.ID, p);
            context.startService(i);
        }
    }

    void showMap() {
        if ((preferences.getFloat(Names.Car.LAT + car_id, 0) == 0) && (preferences.getFloat(Names.Car.LNG + car_id, 0) == 0) &&
                preferences.getString(Names.Car.GSM_ZONE + car_id, "").equals("")) {
            Toast toast = Toast.makeText(getActivity(), R.string.no_location, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        Intent intent = new Intent(getActivity(), MapView.class);
        intent.putExtra(Names.ID, car_id);
        startActivity(intent);
    }

    @Override
    public boolean onTouch(final View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                v.setBackgroundResource(R.drawable.button_pressed);
                if (longTapTimer != null)
                    longTapTimer.cancel();
                longTapTimer = new CountDownTimer(1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {

                    }

                    @Override
                    public void onFinish() {
                        longTapTimer = null;
                        Vibrator vibro = (Vibrator) v.getContext().getSystemService(Context.VIBRATOR_SERVICE);
                        vibro.vibrate(200);
                    }
                };
                longTapTimer.start();
                return true;

            case MotionEvent.ACTION_UP:
                boolean longTap = (longTapTimer == null);
                if (longTapTimer != null) {
                    longTapTimer.cancel();
                    longTapTimer = null;
                }
                v.setBackgroundResource(R.drawable.button_normal);
                doCommand(v, longTap);
                return true;


            case MotionEvent.ACTION_CANCEL:
                if (longTapTimer != null) {
                    longTapTimer.cancel();
                    longTapTimer = null;
                }
                v.setBackgroundResource(R.drawable.button_normal);
                return true;
        }
        return false;
    }

    void doCommand(View v, boolean longTap) {
        if (v == vMotor) {
            boolean az = (Boolean) vMotor.getTag();
            if (az) {
                if (SmsMonitor.isProcessed(car_id, R.string.motor_off)) {
                    SmsMonitor.cancelSMS(getActivity(), car_id, R.string.motor_off);
                    update(getActivity());
                } else {
                    Actions.motor_off(getActivity(), car_id, longTap);
                }
            } else {
                if (SmsMonitor.isProcessed(car_id, R.string.motor_on)) {
                    SmsMonitor.cancelSMS(getActivity(), car_id, R.string.motor_on);
                    update(getActivity());
                } else {
                    Actions.motor_on(getActivity(), car_id, longTap);
                }
            }
        }
        if (v == vRele) {
            if (SmsMonitor.isProcessed(car_id, R.string.rele)) {
                SmsMonitor.cancelSMS(getActivity(), car_id, R.string.rele);
                update(getActivity());
            } else {
                Actions.rele1(getActivity(), car_id, longTap);
            }
        }
        if (v == vBlock) {
            if (SmsMonitor.isProcessed(car_id, R.string.block)) {
                SmsMonitor.cancelSMS(getActivity(), car_id, R.string.block);
                update(getActivity());
            } else {
                Actions.block_motor(getActivity(), car_id);
            }
        }
        if (v == vValet) {
            if (preferences.getBoolean(Names.Car.GUARD0 + car_id, false) && !preferences.getBoolean(Names.Car.GUARD1 + car_id, false)) {
                if (SmsMonitor.isProcessed(car_id, R.string.valet_off)) {
                    SmsMonitor.cancelSMS(getActivity(), car_id, R.string.valet_off);
                    update(getActivity());
                } else {
                    Actions.valet_off(getActivity(), car_id, longTap);
                }
            } else {
                if (SmsMonitor.isProcessed(car_id, R.string.valet_on)) {
                    SmsMonitor.cancelSMS(getActivity(), car_id, R.string.valet_on);
                    update(getActivity());
                } else {
                    Actions.valet_on(getActivity(), car_id, longTap);
                }
            }
        }
        if (v == vSound) {
            if ((preferences.getInt("V_12_" + car_id, 0) & 8) != 0) {
                if (SmsMonitor.isProcessed(car_id, R.string.sound_on)) {
                    SmsMonitor.cancelSMS(getActivity(), car_id, R.string.sound_on);
                    update(getActivity());
                } else {
                    Actions.sound_on(getActivity(), car_id);
                }
            } else {
                if (SmsMonitor.isProcessed(car_id, R.string.sound_off)) {
                    SmsMonitor.cancelSMS(getActivity(), car_id, R.string.sound_off);
                    update(getActivity());
                } else {
                    Actions.sound_off(getActivity(), car_id);
                }
            }
        }
        if (v == vPhone) {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + preferences.getString(Names.Car.CAR_PHONE + car_id, "")));
            startActivity(intent);
        }
        if (v == vSearch)
            Actions.search(getActivity(), car_id);
        if (v == vRele1)
            Actions.rele(getActivity(), car_id, preferences.getBoolean(Names.Car.RELAY1 + car_id, false) ? R.string.rele1_off : R.string.rele1_on, longTap);
        if (v == vRele1i)
            Actions.rele(getActivity(), car_id, R.string.rele1i, longTap);
        if (v == vRele2)
            Actions.rele(getActivity(), car_id, preferences.getBoolean(Names.Car.RELAY2 + car_id, false) ? R.string.rele2_off : R.string.rele2_on, longTap);
        if (v == vRele2i)
            Actions.rele(getActivity(), car_id, R.string.rele2i, longTap);
    }

    void startAnimation() {
        if (vCar == null)
            return;
        vCar.startAnimation();
    }

    void openPointer(int n) {
        Intent i = new Intent(getActivity(), CarActivity.class);
        i.putExtra(Names.ID, preferences.getString(Names.Car.POINTERS + car_id, "").split(",")[n]);
        getActivity().startActivity(i);
    }

    void setPointer(View v, TextView tv, int n) {
        if (v == null)
            return;
        String p = preferences.getString(Names.Car.POINTERS + car_id, "");
        if (p.equals("")) {
            v.setVisibility(View.GONE);
            return;
        }
        String[] pid = p.split(",");
        if (n >= pid.length) {
            v.setVisibility(View.GONE);
            return;
        }
        long p_last = preferences.getLong(Names.Car.EVENT_TIME + pid[n], 0);
        if (p_last != 0) {
            DateFormat df = android.text.format.DateFormat.getDateFormat(getActivity());
            DateFormat tf = android.text.format.DateFormat.getTimeFormat(getActivity());
            tv.setText(df.format(p_last) + " " + tf.format(p_last));
        } else {
            tv.setText(getString(R.string.unknown));
        }
        int color = preferences.getBoolean(Names.Car.TIMEOUT + car_id, false) ? R.color.error : android.R.color.secondary_text_dark;
        tv.setTextColor(getResources().getColor(color));
        v.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRefreshStarted(View view) {
        if (prgUpdate.getVisibility() == View.VISIBLE)
            return;
        startUpdate(getActivity());
    }
}
