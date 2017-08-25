package com.safercript.mygoogledriveeditor.callbacks;

import com.google.android.gms.drive.DriveApi.MetadataBufferResult;

/**
 * Created by pavelsafronov on 24.08.17.
 */

public interface QueryCallbackDao {

    void onGetFilesInMyDrive(MetadataBufferResult result);

    void failRequest(String messageError);

    void success(String message);
}
