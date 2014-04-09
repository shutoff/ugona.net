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
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;

import org.joda.time.DateTime;

import java.io.File;

import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoView extends ActionBarActivity {

    PhotoViewAttacher mAttacher;
    ImageView iv;
    String title;
    String car_id;
    int camera;
    Bitmap bmpPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo);
        iv = (ImageView) findViewById(R.id.photo);
        byte[] data = getIntent().getByteArrayExtra(Names.Car.SHOW_PHOTO);
        long time = getIntent().getLongExtra(Names.TITLE, 0);
        car_id = getIntent().getStringExtra(Names.ID);
        camera = getIntent().getIntExtra(Names.CAMERA, 0);
        try {
            File file = new File(getCacheDir(), "p" + car_id + "_" + getIntent().getLongExtra(Names.Car.SHOW_PHOTO, 0));
            bmpPhoto = BitmapFactory.decodeFile(file.getAbsolutePath());
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (preferences.getBoolean(Names.ROTATE + camera + "_" + car_id, false)) {
                Matrix matrix = new Matrix();
                matrix.postRotate(180);
                bmpPhoto = Bitmap.createBitmap(bmpPhoto, 0, 0, bmpPhoto.getWidth(), bmpPhoto.getHeight(), matrix, true);
            }
            iv.setImageBitmap(bmpPhoto);
        } catch (Exception ex) {
            // ignore
        }
        DateTime t = new DateTime(time);
        title = t.toString("dd.MM.yy HH:mm:ss");
        setTitle(title);
        mAttacher = new PhotoViewAttacher(iv);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.photo, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share: {
                if (bmpPhoto == null)
                    return true;
                String bmp = MediaStore.Images.Media.insertImage(getContentResolver(), bmpPhoto, title, null);
                Uri bmpUri = Uri.parse(bmp);
                final Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                share.putExtra(Intent.EXTRA_STREAM, bmpUri);
                share.setType("image/png");
                startActivity(Intent.createChooser(share, title));
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
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor ed = preferences.edit();
                String key = Names.ROTATE + camera + "_" + car_id;
                if (preferences.getBoolean(key, false)) {
                    ed.remove(key);
                } else {
                    ed.putBoolean(key, true);
                }
                ed.commit();
                Intent i = new Intent(PhotoFragment.ROTATE);
                i.putExtra(Names.ID, car_id);
                i.putExtra(Names.CAMERA, camera);
                sendBroadcast(i);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
