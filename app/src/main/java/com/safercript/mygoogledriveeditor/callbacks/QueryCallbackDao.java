package com.safercript.mygoogledriveeditor.callbacks;

import android.content.IntentSender;
import android.graphics.Bitmap;

import com.safercript.mygoogledriveeditor.entity.FileDataIdAndName;

import java.util.List;

public interface QueryCallbackDao {

    void createResultImageFile(IntentSender intentSender);

    void onResultFilesInMyDrive(List<FileDataIdAndName> listFiles);

    void onResultGetFile(Bitmap bitmap);

    void failRequest(String messageError);

    void success(String message);
}
