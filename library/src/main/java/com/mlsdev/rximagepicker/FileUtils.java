package com.mlsdev.rximagepicker;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {
  public static final int PHOTOS_SIZE = 500;

  public static File createFileFromContentUri(Context context, Uri contentUri) {
    try {
      InputStream inputStream = context.getContentResolver().openInputStream(contentUri);
      byte[] buffer = new byte[inputStream.available()];
      inputStream.read(buffer);
      File imageFile = createImageFile(context);
      FileOutputStream fo = new FileOutputStream(imageFile);
      fo.write(buffer);
      fo.flush();
      fo.close();
      inputStream.close();
      return imageFile;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static File createImageFile(Context context) {
    String timeStamp =
        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    String imageFileName = "PNG_" + timeStamp + ".png";
    File storageDir = context.getExternalCacheDir();
    return new File(storageDir, imageFileName);
  }

  public static File resizeImage(Context context, Uri imageUri) throws IOException {
    // First we get the the dimensions of the file on disk
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(imageUri.getPath(), options);

    // Now we will load the image and have BitmapFactory resize it for us.
    options.inSampleSize = calculateInSampleSize(options, PHOTOS_SIZE, PHOTOS_SIZE);
    options.inJustDecodeBounds = false;

    Bitmap resizedBitmap = BitmapFactory.decodeFile(imageUri.getPath(), options);
    resizedBitmap = checkAndRotateBitmapIfNeeded(context, imageUri, resizedBitmap);

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);

    File f = new File(imageUri.getPath());
    f.createNewFile();
    FileOutputStream fo = new FileOutputStream(f);
    fo.write(bytes.toByteArray());
    fo.flush();
    fo.close();

    return f;
  }

  private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
      int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {
      final int halfHeight = height / 2;
      final int halfWidth = width / 2;

      // Calculate the largest inSampleSize value that is a power of 2 and keeps both
      // height and width larger than the requested height and width.
      while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
        inSampleSize *= 2;
      }
    }

    return inSampleSize;
  }

  private static Bitmap checkAndRotateBitmapIfNeeded(Context context, Uri uri, Bitmap bitmap) {
    try {
      int rotation = getImageOrientation(context, uri);
      return rotateBitmap(bitmap, rotation);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return bitmap;
  }

  private static Bitmap rotateBitmap(Bitmap source, float angle) {
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
  }

  private static int getImageOrientation(Context context, Uri imageUri) {
    int orientation = getOrientationFromExif(imageUri.getPath());
    if (orientation < 0) {
      orientation = getOrientationFromMediaStore(context, imageUri);
    }
    return orientation;
  }

  private static int getOrientationFromExif(String imagePath) {
    int orientation = -1;
    try {
      ExifInterface exif = new ExifInterface(imagePath);
      int exifOrientation =
          exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

      switch (exifOrientation) {
        case ExifInterface.ORIENTATION_ROTATE_270:
          orientation = 270;
          break;
        case ExifInterface.ORIENTATION_ROTATE_180:
          orientation = 180;
          break;
        case ExifInterface.ORIENTATION_ROTATE_90:
          orientation = 90;
          break;
        case ExifInterface.ORIENTATION_NORMAL:
          orientation = 0;
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return orientation;
  }

  private static int getOrientationFromMediaStore(Context context, Uri imageUri) {
    int orientation = 0;
    String[] projection = {MediaStore.Images.ImageColumns.ORIENTATION};
    Cursor cursor = context.getContentResolver().query(imageUri, projection, null, null, null);
    if (cursor != null && cursor.moveToFirst()) {
      orientation = cursor.getInt(0);
      cursor.close();
    }
    return orientation;
  }
}
