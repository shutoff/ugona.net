package net.ugona.plus;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URLEncoder;

public class CarPictureProvider extends ContentProvider {

    CarImage carImage;

    @Override
    public boolean onCreate() {
        carImage = new CarImage(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new RuntimeException("CarPictureProvider.query not supported");
    }

    @Override
    public String getType(Uri uri) {
        return "image/png";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new RuntimeException("CarPictureProvider.insert not supported");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new RuntimeException("CarPictureProvider.delete not supported");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new RuntimeException("CarPictureProvider.update not supported");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        try {
            File outputDir = getContext().getCacheDir();
            String state = uri.getPath().substring(1);
            File file = new File(outputDir, URLEncoder.encode(state, "utf-8"));
            if (!file.exists()) {
                synchronized (carImage) {
                    Bitmap bitmap = null;
                    if ((state.length() > 2) && (state.substring(0, 2).equals("__"))) {
                        int id = carImage.getBitmapId(state.substring(2));
                        bitmap = carImage.getBitmap(id);
                    } else {
                        carImage.state = state;
                        bitmap = carImage.getBitmap();
                    }
                    FileOutputStream out = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();
                    bitmap.recycle();
                }
            }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
