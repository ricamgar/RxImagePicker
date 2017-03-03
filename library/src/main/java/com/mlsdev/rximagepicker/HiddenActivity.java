package com.mlsdev.rximagepicker;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class HiddenActivity extends Activity {

  private static final String KEY_CAMERA_PICTURE_URL = "cameraPictureUrl";

  public static final String IMAGE_SOURCE = "image_source";
  public static final String ALLOW_MULTIPLE_IMAGES = "allow_multiple_images";

  private static final int SELECT_PHOTO = 100;
  private static final int TAKE_PHOTO = 101;

  private Uri cameraPictureUrl;
  private View rationaleView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_hidden);
    if (savedInstanceState == null) {
      handleIntent(getIntent());
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putParcelable(KEY_CAMERA_PICTURE_URL, cameraPictureUrl);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    cameraPictureUrl = savedInstanceState.getParcelable(KEY_CAMERA_PICTURE_URL);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    handleIntent(intent);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      handleIntent(getIntent());
    } else {
      showSnackBarPermissionMessage(permissions[0]);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_OK) {
      switch (requestCode) {
        case SELECT_PHOTO:
          handleGalleryResult(data);
          break;
        case TAKE_PHOTO:
          RxImagePicker.with(this).onImagePicked(cameraPictureUrl);
          break;
      }
    }
    finish();
  }

  private void handleGalleryResult(Intent data) {
    if (getIntent().getBooleanExtra(ALLOW_MULTIPLE_IMAGES, false)) {
      ArrayList<Uri> imageUris = new ArrayList<>();
      ClipData clipData = data.getClipData();
      if (clipData != null) {
        for (int i = 0; i < clipData.getItemCount(); i++) {
          imageUris.add(clipData.getItemAt(i).getUri());
        }
      } else {
        imageUris.add(data.getData());
      }
      RxImagePicker.with(this).onImagesPicked(imageUris);
    } else {
      RxImagePicker.with(this).onImagePicked(data.getData());
    }
  }

  private void handleIntent(Intent intent) {
    Sources sourceType = Sources.values()[intent.getIntExtra(IMAGE_SOURCE, 0)];
    int chooseCode = 0;
    Intent pictureChooseIntent = null;

    switch (sourceType) {
      case CAMERA:
        if (!checkPermission(Manifest.permission.CAMERA)) {
          return;
        }
        cameraPictureUrl = createImageUri();
        pictureChooseIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //pictureChooseIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPictureUrl);
        pictureChooseIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPictureUrl);
        chooseCode = TAKE_PHOTO;
        break;
      case GALLERY:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
          pictureChooseIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
          pictureChooseIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
              getIntent().getBooleanExtra(ALLOW_MULTIPLE_IMAGES, false));
          pictureChooseIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        } else {
          pictureChooseIntent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        pictureChooseIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        pictureChooseIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pictureChooseIntent.setType("image/*");
        chooseCode = SELECT_PHOTO;
        break;
    }

    startActivityForResult(pictureChooseIntent, chooseCode);
  }

  private boolean checkPermission(String permission) {
    if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
      return true;
    } else {
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
        createAndShowPermissionRationale(permission, R.string.app_name, R.string.app_name);
      } else {
        ActivityCompat.requestPermissions(this, new String[] {permission}, 0);
      }
      return false;
    }
  }

  protected void createAndShowPermissionRationale(final String permission, int titleResId, int subtitleResId) {
    if (rationaleView == null) {
      rationaleView = ((ViewStub) findViewById(R.id.permission_rationale_stub)).inflate();
    } else {
      rationaleView.setVisibility(View.VISIBLE);
    }
    ((TextView) rationaleView.findViewById(R.id.rationale_title)).setText(titleResId);
    ((TextView) rationaleView.findViewById(R.id.rationale_subtitle)).setText(subtitleResId);
    rationaleView.findViewById(R.id.rationale_accept)
        .setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View view) {
            onAcceptRationaleClick(permission);
          }
        });
    rationaleView.findViewById(R.id.rationale_not_accept)
        .setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View view) {
            onDismissRationaleClick(view);
          }
        });
    rationaleView.setTag(permission);
  }

  public void onAcceptRationaleClick(String permission) {
    switch (permission) {
      case Manifest.permission.CAMERA:
        ActivityCompat.requestPermissions(this, new String[] {permission}, 0);
        break;
      case Manifest.permission.WRITE_EXTERNAL_STORAGE:
        ActivityCompat.requestPermissions(this, new String[] {permission}, 0);
        break;
    }
  }



  public String dismissPermissionRationale() {
    if (rationaleView != null && rationaleView.getVisibility() == View.VISIBLE) {
      rationaleView.setVisibility(View.GONE);
      finish();
      return (String) rationaleView.getTag();
    }
    finish();
    return "";
  }

  public void onDismissRationaleClick(View view) {
    dismissPermissionRationale();
  }

  protected void showSnackBarPermissionMessage(String permission) {
    switch (permission) {
      case Manifest.permission.CAMERA:
        superShowSnackBarPermissionMessage(R.string.app_name);
        break;
      case Manifest.permission.WRITE_EXTERNAL_STORAGE:
        superShowSnackBarPermissionMessage(R.string.app_name);
        break;
    }
  }

  protected void superShowSnackBarPermissionMessage(int message) {
    Snackbar snackbar =
        Snackbar.make(findViewById(android.R.id.content), getString(message), Snackbar.LENGTH_LONG)
            .setAction("Settings", new View.OnClickListener() {
              @Override public void onClick(View view) {
                Intent intent =
                    new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                startActivity(intent);
              }
            })
            .setCallback(new Snackbar.Callback() {
              @Override public void onDismissed(Snackbar snackbar, int event) {
                finish();
              }
            });
    snackbar.show();
  }

  @Override public void finish() {
    super.finish();
    overridePendingTransition(0, android.R.anim.fade_out);
  }

  private Uri createImageUri() {
    // Create an image file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";
    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    File image = new File(storageDir, imageFileName);
    return Uri.fromFile(image);
  }
}
