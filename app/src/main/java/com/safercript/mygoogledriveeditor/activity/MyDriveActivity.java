package com.safercript.mygoogledriveeditor.activity;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.Metadata;
import com.safercript.mygoogledriveeditor.R;
import com.safercript.mygoogledriveeditor.adapters.ResultsAdapter;
import com.safercript.mygoogledriveeditor.callbacks.QueryCallbackDao;
import com.safercript.mygoogledriveeditor.dao.DriveDao;

public class MyDriveActivity extends BaseDriveActivity
        implements NavigationView.OnNavigationItemSelectedListener, QueryCallbackDao {

    private static final String LOG_TAG = MyDriveActivity.class.getSimpleName();

    private ListView mResultsListView;
    private ResultsAdapter mResultsAdapter;
    private DriveDao driveDao;
    private TextView userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_drive);
        driveDao = DriveDao.get();

        RelativeLayout relativeLayoutMain = (RelativeLayout) findViewById(R.id.content_my_drive);
        LayoutInflater inflater = getLayoutInflater();
        mResultsListView = (ListView) inflater.inflate(R.layout.activity_listfiles, null);
        relativeLayoutMain.addView(mResultsListView);

        mResultsAdapter = new ResultsAdapter(this);
        mResultsListView.setAdapter(mResultsAdapter);

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

        userEmail = (TextView) navigationView.getHeaderView(0).findViewById(R.id.textViewUserEmail);
        userEmail.setText(getIntent().getStringExtra("emailString"));

        setListeners();
    }

    private void setListeners() {
        mResultsAdapter.setOnClickListenerAdapter(new ResultsAdapter.OnClickListenerAdapter() {
            @Override
            public void onClick(Metadata metadata) {
                Log.d(LOG_TAG, metadata.getDriveId().getResourceId());
                driveDao.openFileByIdInLog(metadata);
            }

            @Override
            public void onClickDelete(Metadata metadata) {
                Log.d(LOG_TAG, metadata.getDriveId().getResourceId());
                driveDao.deleteFileByStringDriveId(metadata);
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
            driveDao.getFilesInMyDrive();

        } else if (id == R.id.createFile) {
            driveDao.createFileInMainFolder();

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        driveDao.setQueryCallbackDao(this);
        driveDao.setGoogleApiClient(getGoogleApiClient());

    }

    @Override
    protected void onStop() {
        super.onStop();
        mResultsAdapter.clear();
    }

    @Override
    public void onGetFilesInMyDrive(DriveApi.MetadataBufferResult result) {

        mResultsAdapter.clear();
        Log.d(LOG_TAG, result.getMetadataBuffer().toString());
        mResultsAdapter.append(result.getMetadataBuffer());
    }

    @Override
    public void failRequest(String messageError) {
        showMessage(messageError);
    }

    @Override
    public void success(String message) {
        showMessage(message);
        driveDao.getFilesInMyDrive();
    }
}
