package com.wl.function.manager;

import android.util.Log;

import com.wl.function.audioencode.AudioEncode;
import com.wl.function.audiorecord.AudioRecord;

public class AudioManager {

    private final String TAG = "AudioManager";
    private AudioRecord audioRecord;
    private AudioEncode audioEncode;

    public AudioManager() {
        Log.d(TAG, "AudioManager: ");
        initAudioManager();
    }

    public void initAudioManager() {
        Log.d(TAG, "initAudioManager: ");
        audioEncode = new AudioEncode();
        audioRecord = new AudioRecord();
        audioRecord.setOnAudioDataUpdateListener(audioEncode);
    }

    public void startAudioRecord() {
        Log.d(TAG, "startAudioRecord: ");
        audioRecord.startRecord();
        audioEncode.startEncode();
    }

    public void stopAudioRecord() {
        Log.d(TAG, "stopAudioRecord: ");
        audioEncode.stopEncode();
        audioRecord.stopRecord();
    }
}
