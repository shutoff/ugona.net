package net.ugona.plus;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class PhotoFragment extends Fragment
        implements MainActivity.DateChangeListener {

    final static String ROTATE = "net.ugona.plus.ROTATE";

    final static String PHOTOS = "http://dev.car-online.ru/api/v2?get=photos&skey=$1&begin=$2&end=$3&content=json";
    final static String PHOTO = "http://dev.car-online.ru/api/v2?get=photo&skey=";

    final static String DATE = "date";

    final static int MAX_CACHE = 6;

    String car_id;
    LocalDate current;

    View vProgress;
    View vNoPhotos;
    View vError;
    View vPhotos;
    ListView list;

    boolean error;
    boolean loaded;

    SharedPreferences preferences;
    String api_key;

    Vector<Photo> photos;

    PhotoFetcher fetcher;

    BroadcastReceiver br;

    long loading;

    File cacheDir;

    static class BitmapCache {
        Bitmap bitmap;
        long counter;
    }

    Map<String, BitmapCache> memCache;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.photos, container, false);
        if (current == null)
            current = new LocalDate();
        if (savedInstanceState != null) {
            car_id = savedInstanceState.getString(Names.ID);
            current = new LocalDate(savedInstanceState.getLong(DATE));
        }
        vProgress = v.findViewById(R.id.progress);
        vNoPhotos = v.findViewById(R.id.no_photos);
        vPhotos = v.findViewById(R.id.photos);
        vError = v.findViewById(R.id.error);
        list = (ListView) v.findViewById(R.id.list);
        cacheDir = getActivity().getCacheDir();

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Photo photo = photos.get(position);
                File file = new File(cacheDir, "p" + car_id + "_" + photo.id);
                if (photo.loading < 0) {
                    photo.loading = ++loading;
                    startLoading();
                } else if (file.exists()) {
                    try {
                        Intent intent = new Intent(getActivity(), PhotoView.class);
                        intent.putExtra(Names.SHOW_PHOTO, photo.id);
                        intent.putExtra(Names.TITLE, photo.time);
                        intent.putExtra(Names.CAMERA, photo.camera);
                        intent.putExtra(Names.ID, car_id);
                        getActivity().startActivity(intent);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        api_key = preferences.getString(Names.CAR_KEY + car_id, "");
        photos = new Vector<Photo>();

        View vLogo = v.findViewById(R.id.logo);
        vLogo.setClickable(true);
        vLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), About.class);
                startActivity(intent);
            }
        });

        dateChanged(current);

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int camera = intent.getIntExtra(Names.CAMERA, 0);
                String id = intent.getStringExtra(Names.ID);
                if (car_id.equals(id)) {
                    BaseAdapter adapter = (BaseAdapter) list.getAdapter();
                    if (adapter != null)
                        adapter.notifyDataSetChanged();
                }
            }
        };
        getActivity().registerReceiver(br, new IntentFilter(ROTATE));

        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                TextView tv = (TextView) v;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        tv.setTextColor(getResources().getColor(R.color.hour_active));
                        return true;
                    case MotionEvent.ACTION_UP: {
                        int h = 0;
                        try {
                            h = Integer.parseInt(tv.getText().toString());
                        } catch (Exception ex) {
                            // ignore
                        }
                        int i;
                        for (i = 0; i < photos.size(); i++) {
                            Photo p = photos.get(i);
                            LocalTime time = new LocalTime(p.time);
                            if (time.getHourOfDay() < h)
                                break;
                        }
                        i--;
                        if (i < 0)
                            i = 0;
                        list.setSelection(i);
                    }
                    case MotionEvent.ACTION_CANCEL:
                        tv.setTextColor(getResources().getColor(R.color.hour));
                        return true;
                }
                return false;
            }
        };

        ViewGroup vHours = (ViewGroup) v.findViewById(R.id.hours);
        for (int i = 0; i < vHours.getChildCount(); i++) {
            vHours.getChildAt(i).setOnTouchListener(touchListener);
        }

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
        photos.clear();
        getActivity().unregisterReceiver(br);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
        outState.putLong(DATE, current.toDate().getTime());
    }

    @Override
    public void dateChanged(LocalDate date) {
        current = date;
        vProgress.setVisibility(View.VISIBLE);
        vPhotos.setVisibility(View.GONE);
        vNoPhotos.setVisibility(View.GONE);
        vError.setVisibility(View.GONE);
        DataFetcher fetcher = new DataFetcher();
        error = false;
        loaded = false;
        memCache = new HashMap<String, BitmapCache>();
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
                    vPhotos.setVisibility(View.GONE);
                    error = true;
                }
            });
        }
    }

    void startLoading() {
        if (fetcher != null)
            return;
        Photo photo = null;
        int pos = -1;
        for (int i = 0; i < photos.size(); i++) {
            Photo p = photos.get(i);
            if (p.loading <= 0)
                continue;
            if (photo == null) {
                photo = p;
                pos = i;
                continue;
            }
            if (p.loading > photo.loading) {
                photo = p;
                pos = i;
            }
        }
        if (photo == null)
            return;
        fetcher = new PhotoFetcher();
        fetcher.execute(photo);
    }

    class DataFetcher extends HttpTask {

        LocalDate date;

        @Override
        void result(JsonObject data) throws ParseException {
            if (!current.equals(date))
                return;
            photos.clear();
            JsonArray array = data.get("photos").asArray();
            for (int i = 0; i < array.size(); i++) {
                JsonObject p = array.get(i).asObject();
                Photo photo = new Photo();
                photo.id = p.get("id").asLong();
                photo.time = p.get("eventTime").asLong();
                photo.camera = p.get("cameraNumber").asInt();
                photos.add(photo);
            }
            error = false;
            loaded = true;
            vProgress.setVisibility(View.GONE);
            if (photos.size() == 0) {
                vNoPhotos.setVisibility(View.VISIBLE);
            } else {
                vPhotos.setVisibility(View.VISIBLE);
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
                        TextView tvCamera = (TextView) v.findViewById(R.id.camera);
                        tvCamera.setText("#" + photo.camera);
                        ImageView iv = (ImageView) v.findViewById(R.id.photo);
                        View vProgress = v.findViewById(R.id.progress);
                        View vError = v.findViewById(R.id.error);
                        String name = "p" + car_id + "_" + photo.id;
                        if (memCache.containsKey(name)) {
                            BitmapCache b = memCache.get(name);
                            b.counter = ++loading;
                            iv.setImageBitmap(b.bitmap);
                            vProgress.setVisibility(View.GONE);
                            vError.setVisibility(View.GONE);
                        } else if (photo.loading < 0) {
                            iv.setImageResource(R.drawable.photo_bg);
                            vProgress.setVisibility(View.GONE);
                            vError.setVisibility(View.VISIBLE);
                        } else {
                            iv.setImageResource(R.drawable.photo_bg);
                            vProgress.setVisibility(View.VISIBLE);
                            vError.setVisibility(View.GONE);
                            CacheLoader loader = new CacheLoader();
                            loader.execute(photo);
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

    class CacheLoader extends AsyncTask<Photo, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Photo... params) {
            Photo photo = params[0];
            String name = "p" + car_id + "_" + photo.id;
            File file = new File(cacheDir, name);
            if (file.exists()) {
                try {
                    Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                    if (preferences.getBoolean(Names.ROTATE + photo.camera + "_" + car_id, false)) {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(180);
                        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                    }
                    BitmapCache b = new BitmapCache();
                    b.bitmap = bmp;
                    b.counter = ++loading;
                    memCache.put(name, b);
                    while (memCache.size() > MAX_CACHE) {
                        Set<Map.Entry<String, BitmapCache>> entries = memCache.entrySet();
                        Map.Entry<String, BitmapCache> last = null;
                        for (Map.Entry<String, BitmapCache> entry : entries) {
                            if (last == null) {
                                last = entry;
                                continue;
                            }
                            if (last.getValue().counter < entry.getValue().counter)
                                continue;
                            last = entry;
                        }
                        if (last != null)
                            memCache.remove(last.getKey());
                    }
                    return true;
                } catch (Exception ex) {
                    file.delete();
                    photo.loading = -1;
                    return true;
                }
            }
            photo.loading = ++loading;
            startLoading();
            return false;
        }

        @Override
        protected void onPostExecute(Boolean changed) {
            BaseAdapter adapter = (BaseAdapter) list.getAdapter();
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    }

    class PhotoFetcher extends AsyncTask<Photo, Void, Void> {

        @Override
        protected Void doInBackground(Photo... params) {
            String url = PHOTO + api_key;
            url += "&id=" + params[0].id;
            Photo p = params[0];
            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response = httpclient.execute(new HttpGet(url));
                StatusLine statusLine = response.getStatusLine();
                int status = statusLine.getStatusCode();
                if (status != HttpStatus.SC_OK)
                    return null;
                File file = new File(cacheDir, "t" + car_id + "_" + p.id);
                file.createNewFile();
                FileOutputStream out = new FileOutputStream(file);
                response.getEntity().writeTo(out);
                out.close();
                File new_file = new File(cacheDir, "p" + car_id + "_" + p.id);
                new_file.delete();
                file.renameTo(new_file);
                p.loading = 0;
            } catch (Exception e) {
                p.loading = -1;
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            BaseAdapter adapter = (BaseAdapter) list.getAdapter();
            adapter.notifyDataSetChanged();
            fetcher = null;
            startLoading();
        }
    }

    static class Photo {
        long time;
        long id;
        int camera;
        long loading;
    }
}
