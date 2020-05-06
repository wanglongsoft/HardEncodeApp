package com.wl.function.audiorecord;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.wl.function.common.ThreadUtils;

public class AudioRecord {

    private final String TAG = "AudioRecord";

    // 输入源 麦克风
    private final static int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    // 采样率 44.1kHz，所有设备都支持
    private final static int SAMPLE_RATE = 44100;
    // 通道 单声道
    private final static int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    // 精度 16 位，所有设备都支持
    private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private OnAudioDataUpdateListener onAudioDataUpdateListener = null;
    private android.media.AudioRecord audioRecord;
    private int audioSource;
    private int sampleRateInHz;
    private int channelConfig;
    private int audioFormat;
    private int bufferSize;
    private int audioState;
    private boolean isRecording = false;
    private RecordRunnable recordRunnable;

    public AudioRecord() {
        this.audioSource = AUDIO_SOURCE;
        this.sampleRateInHz = SAMPLE_RATE;
        this.channelConfig = CHANNEL_CONFIG;
        this.audioFormat = AUDIO_FORMAT;
        this.bufferSize = android.media.AudioRecord.getMinBufferSize(this.sampleRateInHz, this.channelConfig, this.audioFormat);
        audioRecord = new android.media.AudioRecord(this.audioSource, this.sampleRateInHz, this.channelConfig, this.audioFormat, this.bufferSize);
        audioState = audioRecord.getState();
        if(android.media.AudioRecord.STATE_INITIALIZED == audioState) {
            Log.d(TAG, "AudioRecordControl: init success");
        } else {
            Log.d(TAG, "AudioRecordControl: init fail, state: " + audioState);
        }
        recordRunnable = new RecordRunnable(this.bufferSize);
    }

    public void startRecord() {
        Log.d(TAG, "startRecord isRecording: " + isRecording);
        if(isRecording) {
            return;
        }
        if(null == this.onAudioDataUpdateListener) {
            Log.d(TAG, "startRecord fail, onAudioDataUpdateListener == null");
            return;
        }
        if(android.media.AudioRecord.STATE_INITIALIZED == audioState) {
            Log.d(TAG, "startRecord success");
            if(null != audioRecord) {
                audioRecord.startRecording();
                isRecording = true;
                ThreadUtils.getInstance().addTasks(recordRunnable);
            } else {
                Log.d(TAG, "startRecord: audioRecord == null, return");
            }
        } else {
            Log.d(TAG, "startRecord: start fail, state: " + audioState);
        }
    }

    public void stopRecord() {
        Log.d(TAG, "stopRecord isRecording: " + isRecording);
        if(!isRecording) {
            return;
        }
        if(isRecording) {
            isRecording = false;
            if(null != audioRecord) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
        }
    }

    public void setOnAudioDataUpdateListener(OnAudioDataUpdateListener onAudioDataUpdateListener) {
        this.onAudioDataUpdateListener = onAudioDataUpdateListener;
    }

    public interface OnAudioDataUpdateListener {
        void OnAudioRawDataUpdate(byte[] data);
    }

    public class RecordRunnable implements Runnable {

        private int buffreSize;
        public RecordRunnable(int bufferSize) {
            this.buffreSize = bufferSize;
        }

        @Override
        public void run() {
            byte[] data = new byte[this.buffreSize];
            while (isRecording) {
                int len = audioRecord.read(data, 0, this.buffreSize);
                Log.d(TAG, "run: RecordRunnable, record len: " + len);
                onAudioDataUpdateListener.OnAudioRawDataUpdate(data);
            }
        }
    }
}
