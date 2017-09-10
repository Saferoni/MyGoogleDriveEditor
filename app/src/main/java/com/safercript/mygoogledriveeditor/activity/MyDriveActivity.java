package com.safercript.mygoogledriveeditor.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.safercript.mygoogledriveeditor.LoginActivity;
import com.safercript.mygoogledriveeditor.R;
import com.safercript.mygoogledriveeditor.adapters.ResultsAdapter;
import com.safercript.mygoogledriveeditor.callbacks.QueryCallbackDao;
import com.safercript.mygoogledriveeditor.dao.DriveRestDao;
import com.safercript.mygoogledriveeditor.entity.FileDataIdAndName;
import com.safercript.mygoogledriveeditor.views.DialogExitApp;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.content.Intent.ACTION_GET_CONTENT;

public class MyDriveActivity extends BaseDriveActivity
        implements NavigationView.OnNavigationItemSelectedListener, QueryCallbackDao {

    private static final String LOG_TAG = MyDriveActivity.class.getSimpleName();

    private static final int REQUEST_CODE_CREATOR = 101;
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 102;
    private static final int REQUEST_CODE_IMAGE_FROM_GALLERY = 103;

    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 104;
    private static final int REQUEST_CODE_PERMISSION = 105;
    private static final int REQUEST_AUTHORIZATION = 106;

    private ProgressDialog mProgress;
    private ResultsAdapter mResultsAdapter;

    private DriveRestDao driveRestDao;

    String emailAccountName;
    String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_drive);
        driveRestDao = DriveRestDao.get();

        RelativeLayout relativeLayoutMain = (RelativeLayout) findViewById(R.id.content_my_drive);
        LayoutInflater inflater = getLayoutInflater();

        ListView mResultsListView = (ListView) inflater.inflate(R.layout.activity_listfiles, null);
        relativeLayoutMain.addView(mResultsListView);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Drive API ...");

        mResultsAdapter = new ResultsAdapter(this);
        mResultsListView.setAdapter(mResultsAdapter);
        setListeners();

        initBackButton(relativeLayoutMain,this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        TextView userEmail = (TextView) navigationView.getHeaderView(0).findViewById(R.id.textViewUserEmail);
        if (getIntent().getStringExtra("emailString")!= null){
            emailAccountName = getIntent().getStringExtra("emailString");
            userEmail.setText(emailAccountName);
        }
    }

    private void setListeners() {
        mResultsAdapter.setOnClickListenerAdapter(new ResultsAdapter.OnClickListenerAdapter() {
            @Override
            public void onClick(FileDataIdAndName fileDataIdAndName) {
                Log.d(LOG_TAG, fileDataIdAndName.getId());
                driveRestDao.getFileFromDriveById(fileDataIdAndName.getId());
                mProgress.show();
            }

            @Override
            public void onClickDelete(FileDataIdAndName fileDataIdAndName) {
                Log.d(LOG_TAG, fileDataIdAndName.getId());
                driveRestDao.deleteFileById(fileDataIdAndName.getId());
                mProgress.show();
            }
        });
    }

    private void initBackButton(View view, final Activity activity) {
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                    new DialogExitApp(activity).show();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_drive, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.query_files) {
            driveRestDao.getFilesListInMyDrive();
            mProgress.show();

        } else if (id == R.id.createFile) {
            showSourceImageDialog();

        } else if (id == R.id.nav_share) {
            showMessage("Кнопка пока не назначина");
        } else if (id == R.id.nav_send) {
            signOut();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void signOut() {
        final GoogleApiClient mGoogleApiClient = getGoogleApiClient();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            getGoogleApiClient().clearDefaultAccountAndReconnect().setResultCallback(new ResultCallback<Status>() {

                @Override
                public void onResult(Status status) {
                    mGoogleApiClient.disconnect();

                    Intent intent = new Intent(MyDriveActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        driveRestDao.initDriveRestDao(emailAccountName, this, getGoogleApiClient());
    }

    @Override
    protected void onStop() {
        super.onStop();
        mResultsAdapter.clear();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Bitmap mBitmapToSave;

        switch (requestCode) {
            case REQUEST_CODE_CAPTURE_IMAGE:
                // Called after a photo has been taken.
                if (resultCode == Activity.RESULT_OK) {
                    // Store the image data as a bitmap for writing later.
                    try {
                        mBitmapToSave = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(mCurrentPhotoPath));
                    }catch (IOException e){
                        Log.e(LOG_TAG,"Error get Bitmap from Uri File" + e);
                        showMessage("Wrong Format Image" + e);
                        break;
                    }
                    driveRestDao.saveImageFileToDrive(mBitmapToSave);
                }
                break;
            case REQUEST_CODE_IMAGE_FROM_GALLERY:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        mBitmapToSave = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                    }catch (IOException e){
                        Log.e(LOG_TAG,"Error get Bitmap from Uri File" + e);
                        showMessage("Wrong Format Image");
                        break;
                    }
                    driveRestDao.saveImageFileToDrive(mBitmapToSave);
                }
                break;
            case REQUEST_CODE_CREATOR:
                mProgress.hide();
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(LOG_TAG, "Image successfully saved.");
                    success("Image successfully saved.");
                }
                break;
            case REQUEST_CODE_PERMISSION:
                if (resultCode == Activity.RESULT_OK) {
                    showMessage("Permission Allowed");
                }
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }
// ------- Permission Request----
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (! isDeviceOnline()) {
            showMessage("No network connection available.");
        } else {
            showMessage("Permission OK");
            mProgress.hide();
            driveRestDao.getFilesListInMyDrive();
            //new MakeRequestTask(mCredential).execute();
        }
    }
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }
    private void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MyDriveActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


//    QueryCallbackDao

    @Override
    public void onResultFilesInMyDrive(List<FileDataIdAndName> listFiles) {
        mProgress.hide();
        mResultsAdapter.clear();
        mResultsAdapter.addAll(listFiles);
    }

    @Override
    public void createResultImageFile(IntentSender intentSender) {
        try {
            startIntentSenderForResult(
                    intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
            mProgress.show();
        } catch (SendIntentException e) {
            Log.i(LOG_TAG, "Failed to launch file chooser.");
        }
    }

    @Override
    public void onResultGetFile(Bitmap bitmap) {
        mProgress.hide();
        showImage(bitmap);
    }

    @Override
    public void errorPermission(Exception mLastError) {
        startActivityForResult(
                ((UserRecoverableAuthIOException) mLastError).getIntent(),
                REQUEST_AUTHORIZATION);
    }

    @Override
    public void failRequest(String messageError) {
        mProgress.hide();
        mResultsAdapter.clear();
        showMessage(messageError);
    }

    @Override
    public void success(String message) {
        showMessage(message);
        driveRestDao.getFilesListInMyDrive();
        mProgress.show();
    }

//    work with image

    private void showSourceImageDialog() {
        new MaterialDialog.Builder(this)
                .title("Выбери ресурс:")
                .items(new String[]{"Из галереи", "Из камеры"})
                .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                        switch (which) {
                            case 0:
                                Intent localPhotoIntent = new Intent();
                                localPhotoIntent.setType("image/*");
                                localPhotoIntent.setAction(ACTION_GET_CONTENT);
                                startActivityForResult(Intent.createChooser(localPhotoIntent, "Select Picture"), REQUEST_CODE_IMAGE_FROM_GALLERY);
                                break;
                            case 1:
                                if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.CAMERA)
                                            == PackageManager.PERMISSION_GRANTED
                                            && (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                            == PackageManager.PERMISSION_GRANTED)) {
                                        getImageFromCamera();
                                    } else {
                                        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                                    }
                                } else {
                                    getImageFromCamera();
                                }
                                break;
                        }
                        return false;
                    }
                })
                .show();
    }
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showMessage("Отказано в доступе к камере");
                return;
            }
            if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                showMessage("Отказано в доступе к записи на диск");
                return;
            }
            getImageFromCamera();
        }
    }
    private void getImageFromCamera() {
        File photoFile = null;
        try {
            photoFile = createImageFile();
            Log.e(LOG_TAG, "Profile New File ");
        } catch (IOException e) {
            Log.e(LOG_TAG, "Wrong create File");
        }
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this, this.getPackageName(), photoFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(cameraIntent, REQUEST_CODE_CAPTURE_IMAGE);
        }
    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    public void showImage(Bitmap bitmap) {
        Dialog builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                //nothing;
            }
        });

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        builder.addContentView(imageView, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        builder.show();
    }
}
