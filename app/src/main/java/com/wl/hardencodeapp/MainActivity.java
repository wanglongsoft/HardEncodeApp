package com.wl.hardencodeapp;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.wl.function.avmuxer.AVMuxer;
import com.wl.function.common.BaseActivity;
import com.wl.function.common.ThreadUtils;
import com.wl.function.manager.AudioManager;
import com.wl.function.manager.VideoManager;

public class MainActivity extends BaseActivity {

    private final String TAG = "HardEncodeApp";

    private Button mStartPreview;
    private Button mStartRecord;
    private Button mStartRecordBlackWhite;
    private Button mStartRecordNormal;
    private Button mStopRecord;
    private Button mSaveFile;
    private SurfaceView surfaceView;

    private VideoManager videoManager;
    private AudioManager audioManager;

    private AVMuxer avMuxer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        setContentView(R.layout.activity_main);
        requestRunTimePermission(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE}, null);

        videoManager = new VideoManager();
        audioManager = new AudioManager();

        mStartPreview = findViewById(R.id.preview_av);
        mStartPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoManager.startPreview();
            }
        });

        mStartRecord = findViewById(R.id.start_av);
        mStartRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoManager.startVideoRecord();
                audioManager.startAudioRecord();
            }
        });

        mStartRecordBlackWhite = findViewById(R.id.record_bw);
        mStartRecordBlackWhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoManager.blackWhitePreview();
            }
        });

        mStartRecordNormal = findViewById(R.id.record_normal);
        mStartRecordNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoManager.normalWhitePreview();
            }
        });

        mStopRecord = findViewById(R.id.stop_av);
        mStopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoManager.stopVideoManager();
                audioManager.stopAudioRecord();
            }
        });

        mSaveFile = findViewById(R.id.save_av);
        mSaveFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != avMuxer) {
                    avMuxer = null;
                }
                avMuxer = new AVMuxer(MainActivity.this);
                avMuxer.startAVMuxer();
            }
        });
        surfaceView = findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated: ");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged width: " + width + " height: " + height);
                videoManager.setViewSurface(holder.getSurface());
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed: ");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        videoManager.stopVideoManager();
        audioManager.stopAudioRecord();
        ThreadUtils.getInstance().showDownAllThread();
    }
}
