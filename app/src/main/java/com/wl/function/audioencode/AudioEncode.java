package com.wl.function.audioencode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import com.wl.function.audiorecord.AudioRecord;
import com.wl.function.common.IEncoder;
import com.wl.function.common.ThreadUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioEncode implements IEncoder, AudioRecord.OnAudioDataUpdateListener {

    private final String TAG = "AudioEncode";
    private final String MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    public static final String OUTPUT_MUXER_PATH = Environment.getExternalStorageDirectory() + File.separator + "filefilm" + File.separator + "audio_encode.mp4";
    private final static int SAMPLE_RATE = 44100;
    private final int CHANNEL_COUNT = 2;
    private final int BIT_RATE = 96000;

    private MediaCodec mAudioEncoder;
    private MediaFormat mAudioFormat;
    private MediaMuxer mMediaMuxer;
    private ByteBuffer mInputBuffers = null;
    private ByteBuffer mOutputBuffers = null;
    private boolean isEncoding;
    private boolean isMuxerStart;
    private int track_index;
    private long startTime = -1;
    private MediaCodec input_codec = null;
    private int input_index = -1;
    private volatile boolean input_update = false;

    private LinkedBlockingQueue<byte[]> mEncodeInputQueue;

    public AudioEncode() {
        this.mAudioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, CHANNEL_COUNT);
        this.mAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        this.mAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        this.mAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 9000);  //设置输入数据缓冲区的最大大小，双通道数据较大
        try {
            this.mAudioEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.mAudioEncoder.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                Log.d(TAG, "onInputBufferAvailable index : " + index);
                input_codec = codec;
                input_index = index;
                input_update = true;
//                pushBufferToEncoder(codec, index);   //放在其它线程网编码器推数据，否则卡编码器
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                Log.d(TAG, "onOutputBufferAvailable index: " + index);
                pullBufferFromDecoder(codec, index, info);
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                Log.d(TAG, "onError: " + e.getMessage());
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                Log.d(TAG, "onOutputFormatChanged: ");
                if(isMuxerStart) {
                    Log.d(TAG, "pullBufferFromDecoder addTrack twice");
                    return;
                }
                MediaFormat mediaFormat = codec.getOutputFormat();
                track_index = mMediaMuxer.addTrack(mediaFormat);
                Log.d(TAG, "onOutputFormatChanged mMediaMuxer track_index ： " + track_index);
                mMediaMuxer.start();
                isMuxerStart = true;
            }
        });

        this.mAudioEncoder.configure(this.mAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    @Override
    public void startEncode() {
        Log.d(TAG, "startEncode isEncoding: " + isEncoding);
        if (this.isEncoding) {
            return;
        }
        this.isEncoding = true;
        initEncode();
        this.mAudioEncoder.start();
    }

    @Override
    public void stopEncode() {
        Log.d(TAG, "stopEncode this.isEncoding: " + this.isEncoding);
        if (!this.isEncoding) {
            return;
        }
        if (this.mAudioEncoder != null) {
            this.isEncoding = false;
            this.mAudioEncoder.stop();
            this.mAudioEncoder.release();
            this.mAudioEncoder = null;
        }
        if(this.mMediaMuxer != null) {
            this.mMediaMuxer.stop();
            this.mMediaMuxer.release();
        }
        if(this.mEncodeInputQueue != null) {
            this.mEncodeInputQueue.clear();
        }
    }

    @Override
    public boolean isEncoding() {
        return this.isEncoding;
    }

    @Override
    public void initEncode() {
        try {
            this.mMediaMuxer = new MediaMuxer(OUTPUT_MUXER_PATH, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        isMuxerStart = false;
        mEncodeInputQueue = new LinkedBlockingQueue<>(20);
    }

    @Override
    public int pullBufferFromDecoder(MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
        Log.d(TAG, "pullBufferFromDecoder: ");
        mOutputBuffers = codec.getOutputBuffer(index);
        if(info.size != 0) {
            if(isMuxerStart) {
                Log.d(TAG, "writeSampleData offset: " + info.offset + " size: " + info.size);
                mOutputBuffers.position(info.offset);
                mOutputBuffers.limit(info.offset + info.size);
                this.mMediaMuxer.writeSampleData(track_index, mOutputBuffers, info);
            }
        }
        codec.releaseOutputBuffer(index, false);
        return 0;
    }

    @Override
    public int pushBufferToEncoder(MediaCodec codec, int index) {
        if(null == codec || index == -1) {
            Log.d(TAG, "pushBufferToEncoder codec index == -1, return");
            return -1;
        }
        if(!isEncoding) {
            Log.d(TAG, "pushBufferToEncoder codec isEncoding == false, return");
            return -1;
        }

        if(!input_update) {
            Log.d(TAG, "pushBufferToEncoder codec input_update == false, return");
            return -1;
        }

        mInputBuffers = codec.getInputBuffer(index);
        input_update = false;
        try {
            byte[] data = mEncodeInputQueue.take();
            mInputBuffers.put(data);
            mInputBuffers.limit(data.length);
            Log.d(TAG, "pushBufferToEncoder: ");
            if (data.length <= 0) {
                //如果数据已经取完，压入数据结束标志：BUFFER_FLAG_END_OF_STREAM
                this.mAudioEncoder.queueInputBuffer(index, 0,
                        0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                if(startTime == -1) {
                    startTime = System.nanoTime();
                }
                //presentationTimeUs不能设置为0，否则编码输出时间戳错乱，复用器写入时抛异常
                this.mAudioEncoder.queueInputBuffer(index, 0,
                        data.length,  (System.nanoTime() - startTime) / 1000, 0);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void run() {
        Log.d(TAG, "run: AudioEncode");
    }

    @Override
    public void OnAudioRawDataUpdate(final byte[] data) {
        if(null != mEncodeInputQueue) {
            Log.d(TAG, "OnAudioRawDataUpdate size: " + mEncodeInputQueue.size());
            try {
                mEncodeInputQueue.put(data);
                ThreadUtils.getInstance().addTasks(new Runnable() {
                    @Override
                    public void run() {
                        pushBufferToEncoder(input_codec, input_index);
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
