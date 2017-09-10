package com.safercript.mygoogledriveeditor.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.safercript.mygoogledriveeditor.R;
import com.safercript.mygoogledriveeditor.activity.MyDriveActivity;

/**
 * Created by pavelsafronov on 08.05.17.
 */

public class DialogExitApp implements DialogInterface.OnClickListener {

    private final AlertDialog.Builder alertDialog;
    private Activity activity;

    public DialogExitApp(Activity activity) {
        this.activity = activity;
        this.alertDialog = new AlertDialog.Builder(activity);
        this.alertDialog.setCancelable(false);
        this.alertDialog.setTitle(R.string.dialog_exit_title);
        this.alertDialog.setMessage(R.string.dialog_exit_massage);
        this.alertDialog.setPositiveButton(R.string.dialog_exit_button_positive, this);
        this.alertDialog.setNegativeButton(R.string.dialog_exit_button_negative, this);
    }

    public void show() {
        this.alertDialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            ((MyDriveActivity) activity).signOut();
        }
    }
}
