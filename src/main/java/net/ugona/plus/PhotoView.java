package net.ugona.plus;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;

import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;

import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoView extends ActionBarActivity {

    PhotoViewAttacher mAttacher;
    Bitmap bmpPhoto;
    String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo);
        ImageView iv = (ImageView) findViewById(R.id.photo);
        byte[] data = getIntent().getByteArrayExtra(Names.SHOW_PHOTO);
        long time = getIntent().getLongExtra(Names.TITLE, 0);
        DateTime t = new DateTime(time);
        title = t.toString("dd.MM.yy HH:mm:ss");
        setTitle(title);
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            bmpPhoto = BitmapFactory.decodeStream(bis);
            bis.close();
            iv.setImageBitmap(bmpPhoto);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
                String bmp = MediaStore.Images.Media.insertImage(getContentResolver(), bmpPhoto, title, null);
                Uri bmpUri = Uri.parse(bmp);
                final Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                share.putExtra(Intent.EXTRA_STREAM, bmpUri);
                share.setType("image/png");
                startActivity(Intent.createChooser(share, title));
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
