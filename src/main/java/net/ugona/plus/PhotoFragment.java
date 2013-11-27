package net.ugona.plus;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Vector;

public class PhotoFragment extends Fragment
        implements MainActivity.DateChangeListener {

    final static String PHOTOS = "http://dev.car-online.ru/api/v2?get=photos&skey=$1&begin=$2&end=$3&content=json";
    final static String PHOTO = "http://dev.car-online.ru/api/v2?get=photo&skey=";

    String car_id;
    LocalDate current;

    View vProgress;
    View vNoPhotos;
    View vError;
    ListView list;

    boolean error;
    boolean loaded;

    SharedPreferences preferences;
    String api_key;

    Vector<Photo> photos;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.actions, container, false);
        if (savedInstanceState != null)
            car_id = savedInstanceState.getString(Names.ID);
        vProgress = v.findViewById(R.id.progress);
        vNoPhotos = v.findViewById(R.id.no_photos);
        vError = v.findViewById(R.id.error);
        list = (ListView) v.findViewById(R.id.actions);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        api_key = preferences.getString(Names.CAR_KEY + car_id, "");
        photos = new Vector<Photo>();

        dateChanged(current);
        return v;
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

    @Override
    public void dateChanged(LocalDate date) {
        current = date;
        vProgress.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
        vNoPhotos.setVisibility(View.GONE);
        vError.setVisibility(View.GONE);
        DataFetcher fetcher = new DataFetcher();
        error = false;
        loaded = false;
        fetcher.update();
    }

    void showError() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    vNoPhotos.setVisibility(View.GONE);
                    vProgress.setVisibility(View.GONE);
                    vError.setVisibility(View.VISIBLE);
                    error = true;
                }
            });
        }
    }

    class DataFetcher extends HttpTask {

        LocalDate date;

        @Override
        void result(JSONObject data) throws JSONException {
            if (!current.equals(date))
                return;
            photos.clear();
            JSONArray array = data.getJSONArray("photos");
            for (int i = 0; i < array.length(); i++) {
                JSONObject p = array.getJSONObject(i);
                Photo photo = new Photo();
                photo.id = p.getLong("id");
                photo.time = p.getLong("eventTime");
                photos.add(photo);
            }
            error = false;
            loaded = true;
            vProgress.setVisibility(View.GONE);
            if (photos.size() == 0) {
                vNoPhotos.setVisibility(View.VISIBLE);
            } else {
                list.setVisibility(View.VISIBLE);
                list.setAdapter(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return photos.size();
                    }

                    @Override
                    public Object getItem(int position) {
                        return photos.get(position);
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
                            v = inflater.inflate(R.layout.photo_item, null);
                        }
                        Photo photo = photos.get(position);
                        DateTime time = new DateTime(photo.time);
                        TextView tvTime = (TextView) v.findViewById(R.id.time);
                        tvTime.setText(time.toString("HH:mm:ss"));
                        ImageView iv = (ImageView) v.findViewById(R.id.photo);
                        if (photo.photo != null){
                            iv.setImageBitmap(photo.photo);
                            Log.v("vvv", "photo != null");
                        }else {
                            iv.setImageResource(R.drawable.photo_bg);
                            PhotoFetcher fetcher = new PhotoFetcher();
                            fetcher.execute(photo);
                        }
                        return v;
                    }
                });
            }
        }

        @Override
        void error() {
            showError();
        }

        void update() {
            date = current;
            DateTime start = date.toDateTime(new LocalTime(0, 0));
            LocalDate next = date.plusDays(1);
            DateTime finish = next.toDateTime(new LocalTime(0, 0));
            execute(PHOTOS,
                    api_key,
                    start.toDate().getTime() + "",
                    finish.toDate().getTime() + "");
        }
    }

    class PhotoFetcher extends AsyncTask<Photo, Void, Void> {

        @Override
        protected Void doInBackground(Photo... params) {
            String url = PHOTO + api_key;
            url += "&id=" + params[0].id;
            Log.v("vvv", "url=" + url);
            Bitmap bmp = null;
            try {
                InputStream in = new java.net.URL(url).openStream();
                bmp = BitmapFactory.decodeStream(in);
                params[0].photo = bmp;
                Log.v("vvv", "photo get");
                Log.v("vvv", bmp.getWidth() + "*" + bmp.getHeight());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            BaseAdapter adapter = (BaseAdapter) list.getAdapter();
            adapter.notifyDataSetChanged();
        }
    }

    static class Photo {
        long time;
        long id;
        Bitmap photo;
    }
}
