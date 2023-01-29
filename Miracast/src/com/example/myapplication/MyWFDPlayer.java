package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.io.IOException;

/**
 * Created by anushanker on 29/1/16.
 */
public class MyWFDPlayer extends Activity implements OnPreparedListener {

    private final String TAG = "WFDPlayer";
    private MediaPlayer mPlayer;
    private Context mContext;
    private SurfaceView mSFView;
    private SurfaceHolder mHolder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.player_main);

        mContext = this.getApplicationContext();

        mPlayer = new MediaPlayer();
        addPlayerListners();

        mSFView = (SurfaceView) findViewById(R.id.player_video);
        mHolder = mSFView.getHolder();
        mSFView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        mHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.v(TAG, "Surface created");
                if (mHolder.getSurface() != null) {
                    if (mPlayer != null) {
                        mPlayer.setDisplay(mHolder);
                        // Prepare MediaPlayer
                        preparePlayer();
                    } else {
                        Log.e(TAG, "Media Player is NULL");
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.v(TAG, "Surface changed : format : " + format + "width" + width + "height" + height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.v(TAG, "Surface Destroyed");

                releasePlayer();
                mHolder.getSurface().release();
                mSFView = null;
                mHolder = null;
                if (!isFinishing()) {
                    finish();
                }
            }
        });


    }

    private void addPlayerListners() {
        mPlayer.setOnErrorListener(new OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "Media Player error occured. What : " + what + "  " + extra);
                return false;
            }
        });

        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.v(TAG, "Playback complete");
                releasePlayer();
                mSFView = null;
                finish();
            }
        });

        mPlayer.setOnVideoSizeChangedListener(new OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                Log.v(TAG, "Video Size changed. width : " + width + "height" + height);
            }
        });
    }

    private void preparePlayer() {
        Intent intent = this.getIntent();
        Uri uri = intent.getData();
        Log.v(TAG, "Uri : " + uri);

        try {
            //String path = "/sdcard/DCIM/Camera/test.mp4";
            //mPlayer.setDataSource(path);
            mPlayer.setDataSource(uri.toString());
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        AudioManager mAudManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setScreenOnWhilePlaying(true);
        mPlayer.prepareAsync();
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
            }
            mPlayer.reset();
            mPlayer.release();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        //start media player
        if (!mPlayer.isPlaying()) {
            mPlayer.start();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            releasePlayer();
        } catch(Exception e) {

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            releasePlayer();
        } catch(Exception e) {

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            releasePlayer();
        } catch(Exception e) {

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try {
            releasePlayer();
        } catch(Exception e) {

        }
    }
}
