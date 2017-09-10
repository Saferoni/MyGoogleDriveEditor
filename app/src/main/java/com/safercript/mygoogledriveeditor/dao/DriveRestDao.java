package com.safercript.mygoogledriveeditor.dao;

import android.content.Context;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.safercript.mygoogledriveeditor.callbacks.QueryCallbackDao;
import com.safercript.mygoogledriveeditor.entity.FileDataIdAndName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DriveRestDao {
    private static final String LOG_TAG = DriveRestDao.class.getSimpleName();

    private QueryCallbackDao queryCallbackDao;
    private GoogleApiClient mGoogleApiClient;
    private com.google.api.services.drive.Drive driveService;

    private static final String[] SCOPES_DRIVE = {DriveScopes.DRIVE};

    private static DriveRestDao sInstance;

    private DriveRestDao() {
    }

    public static DriveRestDao get() {
        if (sInstance == null) {
            sInstance = new DriveRestDao();
            return sInstance;
        } else {
            return sInstance;
        }
    }

    public void initDriveRestDao(String user, Context context, GoogleApiClient mGoogleApiClient) {
        this.queryCallbackDao = (QueryCallbackDao) context;
        this.mGoogleApiClient = mGoogleApiClient;
        initDriveService(user,context);
    }

    private void initDriveService(String user, Context context){
        GoogleAccountCredential mCredential = GoogleAccountCredential
                .usingOAuth2(context, Arrays.asList(SCOPES_DRIVE))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(user);

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        driveService = new com.google.api.services.drive.Drive.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("MyGoogleDriveEditor")
                .build();
    }

    //----- Commands -----

    public void getFilesListInMyDrive(){
        new MakeListFilesMyDriveRequestTask().execute();
    }

    public void saveImageFileToDrive(Bitmap bitmap) {
        // Start by creating a new contents, and setting a callback.
        Log.i(LOG_TAG, "Creating new contents.");
        final Bitmap image = bitmap;
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveContentsResult>() {

                    @Override
                    public void onResult(DriveContentsResult result) {
                        // If the operation was not successful, we cannot do anything
                        // and must
                        // fail.
                        if (!result.getStatus().isSuccess()) {
                            Log.i(LOG_TAG, "Failed to create new contents.");
                            return;
                        }
                        // Otherwise, we can write our data to the new contents.
                        Log.i(LOG_TAG, "New contents created.");
                        // Get an output stream for the contents.
                        OutputStream outputStream = result.getDriveContents().getOutputStream();
                        // Write the bitmap data from it.
                        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
                        image.compress(Bitmap.CompressFormat.JPEG, 100, bitmapStream);
                        try {
                            outputStream.write(bitmapStream.toByteArray());
                        } catch (IOException e1) {
                            queryCallbackDao.failRequest("Unable to write file contents.");
                            Log.i(LOG_TAG, "Unable to write file contents.");
                        }
                        // Create the initial metadata - MIME type and title.
                        // Note that the user will be able to change the title later.
                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                .setMimeType("image/jpeg").setTitle("Android Photo.JPEG").build();
                        // Create an intent for the file chooser, and start it.
                        IntentSender intentSender = Drive.DriveApi
                                .newCreateFileActivityBuilder()
                                .setInitialMetadata(metadataChangeSet)
                                .setInitialDriveContents(result.getDriveContents())
                                .build(mGoogleApiClient);

                        queryCallbackDao.createResultImageFile(intentSender);
                    }
                });
    }

    public void getFileFromDriveById(String fileId){
        new RequestGetFileByIDAsyncTask().execute(fileId);
    }

    public void deleteFileById(String fileId){
        new RequestDeleteFileByIDAsyncTask().execute(fileId);
    }

// ------ AsyncTasks commands ------

    private class MakeListFilesMyDriveRequestTask extends AsyncTask<Void, Void, List<FileDataIdAndName>> {
        private Exception mLastError = null;

        /**
         * Background task to call Drive API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<FileDataIdAndName> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of up to 10 file names and IDs.
         * @return List of Strings describing files, or an empty list if no files
         *         found.
         * @throws IOException
         */
        private List<FileDataIdAndName> getDataFromApi() throws IOException {
            // Get a list of up to 10 files.
            List<FileDataIdAndName> fileInfo = new ArrayList<>();
            FileList result = driveService.files().list()
                    .setPageSize(20)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
            List<File> files = result.getFiles();
            if (files != null) {
                for (File file : files) {
                    fileInfo.add(new FileDataIdAndName(file.getId(), file.getName()));
                }
            }
            return fileInfo;
        }

        @Override
        protected void onPostExecute(List<FileDataIdAndName> output) {
            if (output == null || output.size() == 0) {
                queryCallbackDao.failRequest("Problem while retrieving results " + mLastError);
            } else {
                queryCallbackDao.onResultFilesInMyDrive(output);
            }
        }

        @Override
        protected void onCancelled() {
            onCanceledCheckLastError(mLastError);
        }
    }

    private class RequestDeleteFileByIDAsyncTask extends AsyncTask<String , Void, Void> {
        private Exception mLastError = null;

        @Override
        protected Void doInBackground(String... params) {
            final String driveIdStr = params[0];
            try {
                driveService.files().delete(driveIdStr).execute();
            }catch (IOException e) {
                mLastError = e;
                System.out.println("An error occurred: " + e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mLastError != null ){
                queryCallbackDao.failRequest("Unable to delete app data." + mLastError);
            }else {
                queryCallbackDao.success("File deleted.");
            }
        }

        @Override
        protected void onCancelled() {
            onCanceledCheckLastError(mLastError);

        }
    }

    private class RequestGetFileByIDAsyncTask extends AsyncTask<String , Void, Void> {
        private Exception mLastError = null;
        Bitmap bitmap;

        @Override
        protected Void doInBackground(String... params) {
            final String fileIdStr = params[0];

            OutputStream outputStream = new ByteArrayOutputStream();
            try {
                driveService.files().get(fileIdStr)
                        .executeMediaAndDownloadTo(outputStream);
            }catch (IOException e) {
                mLastError = e;
            }

            byte[] bitmapData = ((ByteArrayOutputStream) outputStream).toByteArray();
            bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mLastError != null ){
                queryCallbackDao.failRequest("Fail getFile " + mLastError);
            }else {
                queryCallbackDao.onResultGetFile(bitmap);
            }
        }

        @Override
        protected void onCancelled() {
            onCanceledCheckLastError(mLastError);
        }
    }

    private void onCanceledCheckLastError(Exception mLastError){
        if (mLastError != null) {
            if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                Log.e(LOG_TAG,"ERROR 1 GooglePlayServicesAvailabilityIOException");
// showGooglePlayServicesAvailabilityErrorDialog(
//                            ((GooglePlayServicesAvailabilityIOException) mLastError)
//                                    .getConnectionStatusCode());
            } else if (mLastError instanceof UserRecoverableAuthIOException) {
                Log.e(LOG_TAG,"ERROR 2 UserRecoverableAuthIOException");
                queryCallbackDao.errorPermission(mLastError);

            } else {
                Log.e(LOG_TAG,"ERROR 3 Other Error");
                queryCallbackDao.failRequest("The following error occurred:\n"
                        + mLastError.getMessage());
            }
        } else {
            Log.e(LOG_TAG,"ERROR 4 Request Canceled");
            //mOutputText.setText("Request cancelled.");
        }
    }
}
