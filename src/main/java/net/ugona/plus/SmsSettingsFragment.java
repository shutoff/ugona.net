package net.ugona.plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SmsSettingsFragment extends SettingsFragment {

    BroadcastReceiver br;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        items.add(new SmsItem(R.string.prior4, "ALRM PRIOR 4", "ALRM PRIOR OK"));
        items.add(new SmsItem(R.string.prior3, "ALRM PRIOR 3", "ALRM PRIOR OK"));
        items.add(new SmsItem(R.string.prior1, "ALRM PRIOR 1", "ALRM PRIOR OK"));
        items.add(new SmsItem(R.string.inf_sms_no, "INFSMS=NO", "INFSMS=NOT"));
        items.add(new SmsItem(R.string.inf_sms_yes, "INFSMS=YES", "INFSMS YES OK"));

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
            }
        };
        IntentFilter filter = new IntentFilter(SmsMonitor.SMS_SEND);
        filter.addAction(SmsMonitor.SMS_ANSWER);
        filter.addAction(FetchService.ACTION_UPDATE_FORCE);
        getActivity().registerReceiver(br, filter);

        return v;
    }

    @Override
    public void onDestroyView() {
        getActivity().unregisterReceiver(br);
        super.onDestroyView();
    }

    class SmsItem extends Item {

        String sms_text;
        String sms_answer;
        int id;

        SmsItem(int n, String sms, String answer) {
            super(n, "");
            id = n;
            sms_text = sms;
            sms_answer = answer;
        }

        @Override
        void setView(View v) {
            super.setView(v);
            TextView tv = (TextView) v.findViewById(R.id.title1);
            tv.setText(name);
            tv.setVisibility(View.VISIBLE);
            v.findViewById(R.id.title).setVisibility(View.GONE);
            v.findViewById(R.id.progress).setVisibility(SmsMonitor.isProcessed(car_id, id) ? View.VISIBLE : View.GONE);
        }

        @Override
        void click() {
            if (SmsMonitor.isProcessed(car_id, id)) {
                SmsMonitor.cancelSMS(getActivity(), car_id, id);
                update();
                return;
            }
            final Context context = getActivity();
            Actions.requestPassword(context, id, null, new Runnable() {
                @Override
                public void run() {
                    SmsMonitor.sendSMS(context, car_id, new SmsMonitor.Sms(id, sms_text, sms_answer));
                    update();
                }
            });
        }
    }
}
