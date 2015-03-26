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
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class PhotoFragment extends MainFragment {

    final static int MAX_CACHE = 6;

    View vProgress;
    View vError;
    HoursList vPhotos;

    Vector<Photo> photos;
    File cacheDir;
    Map<String, BitmapCache> memCache;
    long loading;

    boolean loaded;

    BroadcastReceiver br;
    PhotoFetcher fetcher;
    DataFetcher data_fetcher;

    String car_id;
    String api_key;
    SharedPreferences preferences;

    @Override
    int layout() {
        return R.layout.tracks;
    }

    @Override
    boolean isShowDate() {
        return true;
    }

    @Override
    void changeDate() {
        refresh();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        v.findViewById(R.id.summary).setVisibility(View.GONE);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        vProgress = v.findViewById(R.id.first_progress);

        v.findViewById(R.id.footer).setVisibility(View.GONE);
        v.findViewById(R.id.progress).setVisibility(View.GONE);
        v.findViewById(R.id.loading).setVisibility(View.GONE);
        v.findViewById(R.id.space).setVisibility(View.GONE);

        vPhotos = (HoursList) v.findViewById(R.id.tracks);
        vError = v.findViewById(R.id.error);

        vError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });

        vPhotos.setListener(new HoursList.Listener() {
            @Override
            public int setHour(int h) {
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
                return i;
            }
        });

        cacheDir = getActivity().getCacheDir();

        vPhotos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Photo photo = photos.get(position);
                File file = new File(cacheDir, "p" + id() + "_" + photo.id);
                if (photo.loading < 0) {
                    photo.loading = ++loading;
                    startLoading();
                } else if (file.exists()) {
                    PhotoView pv = new PhotoView();
                    Bundle args = new Bundle();
                    args.putLong(Names.PHOTO, photo.id);
                    args.putLong(Names.TITLE, photo.time);
                    args.putInt(Names.CAMERA, photo.camera);
                    args.putString(Names.ID, id());
                    pv.setArguments(args);
                    MainActivity activity = (MainActivity) getActivity();
                    activity.setFragment(pv, "photo");
                }
            }
        });

        photos = new Vector<Photo>();

        refresh();

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Names.ROTATE)) {
                    String car_id = intent.getStringExtra(Names.ID);
                    if (id().equals(car_id)) {
                        memCache.clear();
                        vPhotos.notifyChanges();
                    }
                }
            }
        };
        IntentFilter intFilter = new IntentFilter(Names.ROTATE);
        getActivity().registerReceiver(br, intFilter);

        return v;
    }

    @Override
    public void onDestroy() {
        photos.clear();
        getActivity().unregisterReceiver(br);
        super.onDestroy();
    }

    @Override
    public void refresh() {
        vProgress.setVisibility(View.VISIBLE);
        vPhotos.setVisibility(View.GONE);
        vError.setVisibility(View.GONE);
        if (data_fetcher != null)
            data_fetcher.cancel();
        data_fetcher = new DataFetcher();
        loaded = false;
        memCache = new HashMap<String, BitmapCache>();
        data_fetcher.update();
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

        car_id = id();
        CarConfig config = CarConfig.get(getActivity(), car_id);
        api_key = config.getKey();

        fetcher = new PhotoFetcher();
        fetcher.execute(photo);
    }

    static class PhotosParam implements Serializable {
        String skey;
        long begin;
        long end;
    }

    static class BitmapCache {
        Bitmap bitmap;
        long counter;
    }

    static class Photo {
        long time;
        long id;
        int camera;
        long loading;
    }

    static class PhotoParam implements Serializable {
        String skey;
        long id;
    }

    class DataFetcher extends HttpTask {

        @Override
        void result(JsonObject data) throws ParseException {
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
            loaded = true;
            vProgress.setVisibility(View.GONE);
            refreshDone();
            vPhotos.setVisibility(View.VISIBLE);
            vPhotos.setAdapter(new BaseAdapter() {
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
                    if (photos == null)
                        return v;
                    Photo photo = photos.get(position);
                    TextView tvTime = (TextView) v.findViewById(R.id.time);
                    tvTime.setText(State.formatTime(getActivity(), photo.time));
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

        @Override
        void error() {
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        vProgress.setVisibility(View.GONE);
                        vError.setVisibility(View.VISIBLE);
                        vPhotos.setVisibility(View.GONE);
                        refreshDone();
                    }
                });
            }
        }

        void update() {
            DateTime start = date().toDateTime(new LocalTime(0, 0));
            LocalDate next = date().plusDays(1);
            DateTime finish = next.toDateTime(new LocalTime(0, 0));
            CarConfig config = CarConfig.get(getActivity(), id());
            PhotosParam param = new PhotosParam();
            param.skey = config.getKey();
            param.begin = start.toDate().getTime();
            param.end = finish.toDate().getTime();
            execute("/photos", param);
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
            vPhotos.notifyChanges();
        }
    }

    class PhotoFetcher extends AsyncTask<Photo, Void, Void> {

        @Override
        protected Void doInBackground(Photo... params) {
            Photo p = params[0];
            try {
                PhotoParam pp = new PhotoParam();
                pp.skey = api_key;
                pp.id = p.id;
                String data = Config.save(pp);
                RequestBody body = RequestBody.create(MediaType.parse("application/json"), data);
                Request request = new Request.Builder().url(Names.API_URL + "/photo").post(body).build();
                Response response = HttpTask.client.newCall(request).execute();
                if (response.code() != HttpURLConnection.HTTP_OK)
                    return null;
                File file = new File(cacheDir, "t" + car_id + "_" + p.id);
                file.createNewFile();
                FileOutputStream out = new FileOutputStream(file);
                InputStream in = response.body().byteStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                in.close();
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
            vPhotos.notifyChanges();
            fetcher = null;
            startLoading();
        }
    }

}
