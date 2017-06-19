package com.borstsch.bromophone.musicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import com.borstsch.bromophone.R;
import com.borstsch.bromophone.UserType;
import com.borstsch.bromophone.connection.Message;
import com.borstsch.bromophone.connection.ReceiveMessageThread;
import com.borstsch.bromophone.connection.SendMessageThread;
import com.borstsch.bromophone.connection.Synchronizer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.borstsch.bromophone.connection.Message.PAUSE_COMMAND;
import static com.borstsch.bromophone.connection.Message.PLAY_COMMAND;
import static com.borstsch.bromophone.connection.Message.RESUME_COMMAND;
import static com.borstsch.bromophone.connection.Message.START_PLAYER_COMMAND;


public class PlayerActivity extends AppCompatActivity {

    public static final String Broadcast_PLAY_NEW_AUDIO = "com.borstsch.bromophone.musicplayer.PlayNewAudio";
    public static final String Broadcast_PAUSE = "com.borstsch.bromophone.musicplayer.PauseAudio";
    public static final String Broadcast_RESUME = "com.borstsch.bromophone.musicplayer.ResumeAudio";

    private final static int REQUEST_READ_EXTERNAL_STORAGE = 100;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;

//            Toast.makeText(PlayerActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };
    boolean serviceBound = false;
    private ArrayList<Audio> playList;
    private MusicPlayerService player;
    private int chosenTrack;

    ImageView collapsingImageView;
    int imageIndex = 0;

    private Synchronizer synchronizer;
    private UserType user;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        EventBus.getDefault().register(this);

        collapsingImageView = (ImageView) findViewById(R.id.collapsingImageView);
        loadCollapsingImage(imageIndex);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (askStoragePermissions()) {
            loadAudio();
            initRecyclerView();
        }

        Intent intent = getIntent();
        user = (UserType) intent.getSerializableExtra("user");
        synchronizer = new Synchronizer(
                    (SendMessageThread) intent.getSerializableExtra("msg thread"),
                    user);

        synchronizer.startPlayer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadAudio();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean askStoragePermissions() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need permission
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE);
            return false;
        } else {
            return true;
        }
    }

    private void initRecyclerView() {
        if (playList.size() > 0) {
            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
            RecyclerViewAdapter adapter = new RecyclerViewAdapter(playList, getApplication());
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.addOnItemTouchListener(new CustomTouchListener(this, new onItemClickListener() {
                @Override
                public void onClick(View view, int index) {
                    if (CustomTouchListener.timesTouched == 0 || index != chosenTrack) {
                        if (user == UserType.SERVER) {
                            playAudio(index);
                        }

                        CustomTouchListener.timesTouched = 0;
                        chosenTrack = index;
                    } else {
                            if (CustomTouchListener.timesTouched % 2 == 0) {
                                if (user == UserType.SERVER) {
                                    resumeAudio(index);
                                }
                            } else {
                                if (user == UserType.SERVER) {
                                    pauseAudio(index);
                                }
                            }
                        }

                    CustomTouchListener.timesTouched += 1;
                }
            }));
        }

    }

    private void loadCollapsingImage(int i) {
        TypedArray array = getResources().obtainTypedArray(R.array.images);
        collapsingImageView.setImageDrawable(array.getDrawable(i));
        array.recycle();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void playAudio(int audioIndex) {
        System.out.println("Playing audio");
        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(playList);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MusicPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        } else {
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }

        synchronizer.playTrack(playList.get(audioIndex));
    }

    private void resumeAudio(int audioIndex) {
        synchronizer.resumeTrack(playList.get(audioIndex));

        Intent broadcastIntent = new Intent(Broadcast_RESUME);
        sendBroadcast(broadcastIntent);
    }

    private void pauseAudio(int audioIndex) {
        System.out.println("Pause Audio");
        StorageUtil storage = new StorageUtil(getApplicationContext());
        storage.storeAudioIndex(audioIndex);

        synchronizer.pauseTrack(playList.get(audioIndex));

        Intent broadcastIntent = new Intent(Broadcast_PAUSE);
        sendBroadcast(broadcastIntent);
    }

    private void loadAudio() {
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);
        playList = new ArrayList<>();

        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));

                // Save to audioList
                playList.add(new Audio(data, title, album, artist));
            }
        }
        cursor.close();
    }

    @Subscribe
    public void onEvent(JSONObject message) {
        for (Iterator<String> msgIterator = message.keys(); msgIterator.hasNext();) {
            String command = msgIterator.next();
            switch (command) {
                case PLAY_COMMAND:
                    playAudio(chosenTrack);
                    break;
                case PAUSE_COMMAND:
                    pauseAudio(chosenTrack);
                    break;
                case RESUME_COMMAND:
                    resumeAudio(chosenTrack);
            }
        }
    }

    public static List<? extends Number> createUser(UserType user) {
        switch(user) {
            case SERVER:
                return new ArrayList<>();
            default:
                return new ArrayList<>();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
            synchronizer.stop();
            EventBus.getDefault().unregister(this);
        }
    }
}
