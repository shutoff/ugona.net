package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

    final int AUTH_REQUEST = 1;
    String car_id;
    SharedPreferences preferences;
    CarView vCar;
    TextView tvAddress;
    TextView tvLast;
    TextView tvVoltage;
    TextView tvReserve;
    TextView tvBalance;
    TextView tvTemperature;
    TextView tvTemperature2;
    TextView tvTemperature3;
    TextView tvError;
    View vReserve;
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
    View vMain;
    View vTime;

    String location;
    String time;

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

        tvAddress = (TextView) v.findViewById(R.id.addr);

        tvLast = (TextView) v.findViewById(R.id.last);
        tvVoltage = (TextView) v.findViewById(R.id.voltage);
        tvReserve = (TextView) v.findViewById(R.id.reserve);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showHistory(view.getTag().toString());
            }
        };

        vMain = v.findViewById(R.id.main_block);
        vReserve = v.findViewById(R.id.reserve_block);

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

            vMain.setTag("voltage");
            vMain.setOnClickListener(clickListener);
            vReserve.setTag("reserved");
            vReserve.setOnClickListener(clickListener);
            vTemperature.setTag("t1");
            vTemperature.setOnClickListener(clickListener);
            vTemperature2.setTag("t2");
            vTemperature2.setOnClickListener(clickListener);
            vTemperature3.setTag("t3");
            vTemperature3.setOnClickListener(clickListener);
        }

        balanceBlock = v.findViewById(R.id.balance_block);
        balanceBlock.setTag("balance");
        balanceBlock.setOnClickListener(clickListener);

        tvError = (TextView) v.findViewById(R.id.error_text);
        vError = v.findViewById(R.id.error);
        vError.setVisibility(View.GONE);

        imgRefresh = (ImageView) v.findViewById(R.id.refresh);
        prgUpdate = (ProgressBar) v.findViewById(R.id.update);
        prgUpdate.setVisibility(View.GONE);

        vTime = v.findViewById(R.id.time);

        tvAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMap();
            }
        });
        tvAddress.setMovementMethod(new ScrollingMovementMethod());

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
                        showAuth();
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

        if (getActivity() == null)
            return;

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

        location = "";
        if ((lat == 0) && (lon == 0)) {
            String gsm_sector = preferences.getString(Names.Car.GSM_SECTOR + car_id, "");
            if (!gsm_sector.equals("")) {
                String[] parts = gsm_sector.split(" ");
                if (parts.length == 4)
                    location = "LAC: " + parts[2] + " CID: " + parts[3];
            }
            String gsm_zone = preferences.getString(Names.Car.GSM_ZONE + car_id, "");
            if (!gsm_zone.equals(""))
                updateZone(gsm_zone);
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

        if (pointer) {
            time = "";
            long last_time = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
            if (last_time > 0) {
                LocalDateTime stand = new LocalDateTime(last_time);
                LocalDateTime now = new LocalDateTime();
                if (stand.toLocalDate().equals(now.toLocalDate())) {
                    DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
                    time = tf.format(last_time);
                } else {
                    DateFormat df = android.text.format.DateFormat.getDateFormat(context);
                    DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
                    time = df.format(last_time) + " " + tf.format(last_time);
                }
                time += " ";
            }
            updateAddress(null);
            return;
        }

        int level = preferences.getInt(Names.Car.GSM_DB + car_id, 0);
        if (level == 0) {
            vLevel.setVisibility(View.GONE);
        } else {
            vLevel.setVisibility(View.VISIBLE);
            if (preferences.getLong(Names.Car.LOST + car_id, 0) > preferences.getLong(Names.Car.EVENT_TIME + car_id, 0)) {
                ivLevel.setImageResource(R.drawable.gsm_level);
                tvLevel.setText("--");
                tvLevel.setTextColor(getResources().getColor(R.color.error));
            } else {
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
                tvLevel.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
            }
        }

        updateReserveVoltage(tvReserve, preferences.getBoolean(Names.Car.RESERVE_NORMAL + car_id, true));

        String temperature = Preferences.getTemperature(preferences, car_id, 1);
        if (temperature == null) {
            vTemperature.setVisibility(View.GONE);
        } else {
            tvTemperature.setText(temperature);
            vTemperature.setVisibility(View.VISIBLE);
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
        time = "";
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
        updateAddress(null);

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
        if (((commands & State.CMD_RELE) != 0) && (!ignition || preferences.getString(Names.Car.CAR_RELE + car_id, "1").equals("3"))) {
            vRele.setVisibility(View.VISIBLE);
            boolean processed = SmsMonitor.isProcessed(car_id, R.string.rele);
            processed |= SmsMonitor.isProcessed(car_id, R.string.heater_on);
            processed |= SmsMonitor.isProcessed(car_id, R.string.heater_off);
            processed |= SmsMonitor.isProcessed(car_id, R.string.heater_air);
            processed |= SmsMonitor.isProcessed(car_id, R.string.air);
            pRele.setVisibility(processed ? View.VISIBLE : View.GONE);
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
        String str = location;
        int span_start = 0;
        int span_end = 0;
        if (!time.equals(""))
            str = time + " " + str;
        if (addr != null) {
            str += "\n";
            span_start = str.length();
            String parts[] = addr.split(", ");
            str += parts[0];
            int start = 2;
            if (parts.length > 1)
                str += ", " + parts[1];
            if (parts.length > 2) {
                Matcher matcher = number_pattern.matcher(parts[2]);
                if (matcher.matches()) {
                    str += ", " + parts[2];
                    start++;
                }
            }
            span_end = str.length();
            str += "\n";
            for (int i = start; i < parts.length; i++) {
                if (i != start)
                    str += ", ";
                str += parts[i];
            }
        } else {
            str += "\n";
        }
        Spannable spannable = new SpannableString(str);
        if (span_end > span_start) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), span_start, span_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.caldroid_holo_blue_light)), span_start, span_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (!time.equals("")) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, time.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.caldroid_holo_blue_light)), 0, time.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvAddress.setText(spannable);
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
        String val = preferences.getString(Names.Car.VOLTAGE_RESERVED + car_id, "");
        try {
            double v = Double.parseDouble(val);
            if (v == 0) {
                vReserve.setVisibility(View.GONE);
                return;
            }
            val = String.format("%.2f", v);
        } catch (Exception ex) {
            // ignore
            vReserve.setVisibility(View.GONE);
            return;
        }
        tv.setText(val + " V");
        tv.setTextColor(normal ? getResources().getColor(android.R.color.secondary_text_dark) : getResources().getColor(R.color.error));
        vReserve.setVisibility(View.VISIBLE);
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
        if (preferences.getBoolean(Names.Car.POINTER + car_id, false)) {
            Intent intent = new Intent(getActivity(), MapEventActivity.class);
            String data = preferences.getFloat(Names.Car.LAT + car_id, 0) + ";";
            data += preferences.getFloat(Names.Car.LNG + car_id, 0) + ";";
            data += preferences.getFloat(Names.Car.COURSE + car_id, -1) + ";";
            long last = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
            if (last != 0) {
                DateFormat df = android.text.format.DateFormat.getDateFormat(getActivity());
                DateFormat tf = android.text.format.DateFormat.getTimeFormat(getActivity());
                data += df.format(last) + " " + tf.format(last);
            }
            data += "\n";
            data += Address.getAddress(getActivity(), preferences.getFloat(Names.Car.LAT + car_id, 0), preferences.getFloat(Names.Car.LNG + car_id, 0));
            String zone = preferences.getString(Names.Car.GSM_ZONE + car_id, "");
            if (!zone.equals(""))
                data += ";" + zone;
            intent.putExtra(Names.POINT_DATA, data);
            intent.putExtra(Names.ID, car_id);
            startActivity(intent);
            return;
        }
        Intent intent = new Intent(getActivity(), MapPointActivity.class);
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

    void doCommand(View v, final boolean longTap) {
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
            if (preferences.getString(Names.Car.CAR_RELE + car_id, "").equals("3")) {
                if (SmsMonitor.isProcessed(car_id, R.string.heater_air)) {
                    SmsMonitor.cancelSMS(getActivity(), car_id, R.string.heater_air);
                    update(getActivity());
                }
                if (SmsMonitor.isProcessed(car_id, R.string.heater_on)) {
                    SmsMonitor.cancelSMS(getActivity(), car_id, R.string.heater_on);
                    update(getActivity());
                }
                if (SmsMonitor.isProcessed(car_id, R.string.air)) {
                    SmsMonitor.cancelSMS(getActivity(), car_id, R.string.air);
                    update(getActivity());
                }
                if (SmsMonitor.isProcessed(car_id, R.string.heater_off)) {
                    SmsMonitor.cancelSMS(getActivity(), car_id, R.string.heater_off);
                    update(getActivity());
                }
                final LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.thermocode)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, null)
                        .setView(inflater.inflate(R.layout.heater, null))
                        .create();
                dialog.show();
                final Spinner spinner = (Spinner) dialog.findViewById(R.id.heater);
                final int[] values = {
                        R.string.heater_on,
                        R.string.heater_air,
                        R.string.air,
                        R.string.heater_off
                };
                spinner.setAdapter(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return values.length;
                    }

                    @Override
                    public Object getItem(int position) {
                        return values[position];
                    }

                    @Override
                    public long getItemId(int position) {
                        return position;
                    }

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View v = convertView;
                        if (v == null)
                            v = inflater.inflate(R.layout.list_item, null);
                        TextView tvName = (TextView) v.findViewById(R.id.name);
                        tvName.setText(getString(values[position]));
                        return v;
                    }

                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        View v = convertView;
                        if (v == null)
                            v = inflater.inflate(R.layout.list_dropdown_item, null);
                        TextView tvName = (TextView) v.findViewById(R.id.name);
                        tvName.setText(getString(values[position]));
                        return v;
                    }
                });
                if (Preferences.getRele(preferences, car_id))
                    spinner.setSelection(3);
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int cmd = spinner.getSelectedItemPosition();
                        dialog.dismiss();
                        switch (cmd) {
                            case 0:
                                Actions.heater_on(getActivity(), car_id, longTap, true);
                                break;
                            case 1:
                                Actions.heater_on_air(getActivity(), car_id, longTap, true);
                                break;
                            case 2:
                                Actions.heater_air(getActivity(), car_id, longTap, true);
                                break;
                            case 3:
                                Actions.heater_off(getActivity(), car_id, longTap, true);
                                break;
                        }
                    }
                });
                return;
            }
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

    void showHistory(String type) {
        if (State.isPandora(preferences, car_id))
            return;
        Intent intent = new Intent(getActivity(), HistoryActivity.class);
        intent.putExtra(Names.ID, car_id);
        intent.putExtra(Names.STATE, type);
        startActivity(intent);
    }
}
