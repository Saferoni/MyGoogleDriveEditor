package com.safercript.mygoogledriveeditor.callbacks;

import android.content.IntentSender;
import android.graphics.Bitmap;

import com.safercript.mygoogledriveeditor.entity.FileDataIdAndName;

import java.util.List;

public interface QueryCallbackDao {

    void onResultFilesInMyDrive(List<FileDataIdAndName> listFiles);

    void createResultImageFile(IntentSender intentSender);

    void onResultGetFile(Bitmap bitmap);

    void errorPermission(Exception mLastError);

    void failRequest(String messageError);

    void success(String message);
}
