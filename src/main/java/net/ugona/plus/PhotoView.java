package net.ugona.plus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.joda.time.DateTime;

import java.io.File;

import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoView extends MainFragment {

    PhotoViewAttacher mAttacher;

    ImageView iv;
    long time;
    long photo;
    String car_id;
    int camera;

    Bitmap bmpPhoto;

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    @Override
    int layout() {
        return R.layout.photo;
    }

    @Override
    public String getTitle() {
        DateTime t = new DateTime(time);
        return t.toString("dd.MM.yy HH:mm:ss");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.photo, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        iv = (ImageView) v.findViewById(R.id.photo);
        try {
            File file = new File(getActivity().getCacheDir(), "p" + car_id + "_" + photo);
            bmpPhoto = BitmapFactory.decodeFile(file.getAbsolutePath());
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if (preferences.getBoolean(Names.ROTATE + camera + "_" + car_id, false)) {
                Matrix matrix = new Matrix();
                matrix.postRotate(180);
                bmpPhoto = Bitmap.createBitmap(bmpPhoto, 0, 0, bmpPhoto.getWidth(), bmpPhoto.getHeight(), matrix, true);
            }
            iv.setImageBitmap(bmpPhoto);
        } catch (Exception ex) {
            // ignore
        }
        mAttacher = new PhotoViewAttacher(iv);
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(Names.TITLE, time);
        outState.putString(Names.ID, car_id);
        outState.putInt(Names.CAMERA, camera);
        outState.putLong(Names.PHOTO, photo);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share: {
                if (bmpPhoto == null)
                    return true;
                String bmp = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bmpPhoto, getTitle(), null);
                if (bmp == null)
                    return true;
                Uri bmpUri = Uri.parse(bmp);
                final Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                share.putExtra(Intent.EXTRA_STREAM, bmpUri);
                share.setType("image/png");
                startActivity(Intent.createChooser(share, getTitle()));
                return true;
            }
            case R.id.rotate: {
                if (bmpPhoto == null)
                    return true;
                Matrix matrix = new Matrix();
                matrix.postRotate(180);
                bmpPhoto = Bitmap.createBitmap(bmpPhoto, 0, 0, bmpPhoto.getWidth(), bmpPhoto.getHeight(), matrix, true);
                iv.setImageBitmap(bmpPhoto);
                mAttacher.update();
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor ed = preferences.edit();
                String key = Names.ROTATE + camera + "_" + car_id;
                if (preferences.getBoolean(key, false)) {
                    ed.remove(key);
                } else {
                    ed.putBoolean(key, true);
                }
                ed.commit();
                Intent i = new Intent(Names.ROTATE);
                i.putExtra(Names.ID, car_id);
                i.putExtra(Names.CAMERA, camera);
                getActivity().sendBroadcast(i);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    void setArgs(Bundle args) {
        time = args.getLong(Names.TITLE);
        car_id = args.getString(Names.ID);
        camera = args.getInt(Names.CAMERA);
        photo = args.getLong(Names.PHOTO);
    }

}
