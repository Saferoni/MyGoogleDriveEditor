package com.safercript.mygoogledriveeditor.dao;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.safercript.mygoogledriveeditor.callbacks.QueryCallbackDao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static android.content.ContentValues.TAG;

/**
 * Created by pavelsafronov on 24.08.17.
 */

public class DriveDao {
    private static final String LOG_TAG = DriveDao.class.getSimpleName();

    private QueryCallbackDao queryCallbackDao;
    private GoogleApiClient googleApiClient;

    private static DriveDao sInstance;

    private DriveDao() {
    }

    public static DriveDao get() {
        if (sInstance == null) {
            sInstance = new DriveDao();
            return sInstance;
        } else {
            return sInstance;
        }
    }

    public void setQueryCallbackDao(QueryCallbackDao queryCallbackDao){
        this.queryCallbackDao = queryCallbackDao;
    }

    public void setGoogleApiClient(GoogleApiClient googleApiClient) {
        this.googleApiClient = googleApiClient;
    }

    public void getFilesInMyDrive(){
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.MIME_TYPE, "text/plain"))
                .build();

        Drive.DriveApi.query(googleApiClient, query)
                .setResultCallback(new ResultCallback<MetadataBufferResult>() {
                    @Override
                    public void onResult(MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            queryCallbackDao.failRequest("Problem while retrieving results");
                            return;
                        }
                        Log.e(LOG_TAG, result.toString());
                        queryCallbackDao.onGetFilesInMyDrive(result);
                    }
                });
    }

    public void createFileInMainFolder(){
        Drive.DriveApi.newDriveContents(googleApiClient)
                .setResultCallback(new ResultCallback<DriveContentsResult>() {
                    @Override
                    public void onResult(DriveContentsResult result) {
                        if (!result.getStatus().isSuccess()) {
                            queryCallbackDao.failRequest("Error while trying to create new file contents");
                            return;
                        }
                        final DriveContents driveContents = result.getDriveContents();

                        // Perform I/O off the UI thread.
                        new Thread() {
                            @Override
                            public void run() {
                                // write content to DriveContents
                                OutputStream outputStream = driveContents.getOutputStream();
                                Writer writer = new OutputStreamWriter(outputStream);
                                try {
                                    writer.write("Hello World!");
                                    writer.close();
                                } catch (IOException e) {
                                    Log.e(TAG, e.getMessage());
                                }

                                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                        .setTitle("New file")
                                        .setMimeType("text/plain")
                                        .setStarred(true).build();

                                // create a file on root folder
                                Drive.DriveApi.getRootFolder(googleApiClient)
                                        .createFile(googleApiClient, changeSet, driveContents)
                                        .setResultCallback(new ResultCallback<DriveFileResult>() {
                                                                       @Override
                                                                       public void onResult(DriveFileResult result) {
                                                                           if (!result.getStatus().isSuccess()) {
                                                                               queryCallbackDao.failRequest("Error while trying to create the file");
                                                                               return;
                                                                           }
                                                                           queryCallbackDao.success("Created a file with content: " + result.getDriveFile().getDriveId());
                                                                       }
                                                                   });
                            }
                        }.start();
                    }
                });
    }

    public void deleteFileByStringDriveId(Metadata metadata){
        DriveFile driveFile = Drive.DriveApi.getFile(googleApiClient,
                metadata.getDriveId());
        // Call to delete file.
        driveFile.delete(googleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()){
                    queryCallbackDao.success(status.toString());
                    return;
                }
                queryCallbackDao.failRequest(status.toString());
            }
        });
    }


    public void openFileByIdInLog(Metadata metadata){
        DriveFile file = Drive.DriveApi.getFile(googleApiClient,metadata.getDriveId());
        file.open(googleApiClient, DriveFile.MODE_READ_ONLY, null)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.e("Error:","No se puede abrir el archivo o no se encuentra");
                            return;
                        }
                        // DriveContents object contains pointers
                        // to the actual byte stream
                        DriveContents contents = result.getDriveContents();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(contents.getInputStream()));
                        StringBuilder builder = new StringBuilder();
                        String line;
                        try {
                            while ((line = reader.readLine()) != null) {
                                builder.append(line);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String contentsAsString = builder.toString();
                        queryCallbackDao.failRequest(contentsAsString);
                        Log.e("RESULT:",contentsAsString);
                    }
                });
    }

}
