package com.wl.function.videoencode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.wl.function.common.IEncoder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;


public class VideoEncode implements IEncoder {

    private final String TAG = "VideoEncode";

    private MediaCodec mVideoEncoder;
    private boolean isRecording = false;
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 25;               // 25fps
    private static final int IFRAME_INTERVAL = 5;           // 5 seconds between I-frames
    private static final int ENC_BITRATE = 6000000; // Mbps
    public static final String OUTPUT_MUXER_PATH = Environment.getExternalStorageDirectory() + File.separator + "filefilm" + File.separator + "video_encode.mp4";
    private ByteBuffer mOutputByteBuffer = null;
    private MediaCodec.BufferInfo mBufferInfo;
    private MediaMuxer mMediaMuxer;
    private int encode_width;
    private int encode_height;
    private boolean isMuxerStart;
    private int track_index;

    public VideoEncode() {
        Log.d(TAG, "VideoEncode: ");
        try {
            mVideoEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            mBufferInfo = new MediaCodec.BufferInfo();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startEncode() {
        Log.d(TAG, "startEncode isRecording: " + isRecording);
        if(isRecording) {
            return;
        }
        isRecording = true;
        mVideoEncoder.start();
    }

    @Override
    public void stopEncode() {
        Log.d(TAG, "stopEncode isRecording: " + isRecording);
        if(!isRecording) {
            return;
        }
        isRecording = false;
        if (mVideoEncoder != null) {
            mVideoEncoder.signalEndOfInputStream();
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
    }

    @Override
    public boolean isEncoding() {
        return false;
    }

    @Override
    public void initEncode() {
        Log.d(TAG, "initEncode encode_width: " + encode_width + " encode_height: " + encode_height);
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, encode_width, encode_height);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, ENC_BITRATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        try {
            this.mMediaMuxer = new MediaMuxer(OUTPUT_MUXER_PATH, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        isMuxerStart = false;
        mVideoEncoder.setCallback(new MediaCodec.Callback() {//异步方式获取编码器数据
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                Log.d(TAG, "onInputBufferAvailable:");
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                Log.d(TAG, "onOutputBufferAvailable: ");
                mOutputByteBuffer = codec.getOutputBuffer(index);
                if(isMuxerStart) {
                    Log.d(TAG, "writeSampleData offset: " + info.offset + " size: " + info.size);
                    mOutputByteBuffer.position(info.offset);
                    mOutputByteBuffer.limit(info.offset + info.size);
                    mMediaMuxer.writeSampleData(track_index, mOutputByteBuffer, info);
                }
                codec.releaseOutputBuffer(index, false);
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                Log.d(TAG, "onError : " + e.getMessage());
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                Log.d(TAG, "onOutputFormatChanged: ");
                if(isMuxerStart) {
                    Log.d(TAG, "onOutputFormatChanged addTrack twice");
                    return;
                }
                MediaFormat mediaFormat = codec.getOutputFormat();
                track_index = mMediaMuxer.addTrack(mediaFormat);
                Log.d(TAG, "onOutputFormatChanged mMediaMuxer track_index ： " + track_index);
                mMediaMuxer.start();
                isMuxerStart = true;
            }
        });
    }

    @Override
    public int pullBufferFromDecoder(MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {//同步方式，获取编码器数据
//        int index_deque = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, 1000);
//        Log.d(TAG, "pullBufferFromDecoder index_deque: " + index_deque);
//        if(index_deque >= 0) {
//            mOutputByteBuffer = mVideoEncoder.getOutputBuffer(index_deque);
//            if(isMuxerStart) {
//                Log.d(TAG, "writeSampleData offset: " + mBufferInfo.offset + " size: " + mBufferInfo.size);
//                mOutputByteBuffer.position(mBufferInfo.offset);
//                mOutputByteBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
//                this.mMediaMuxer.writeSampleData(track_index, mOutputByteBuffer, mBufferInfo);
//            }
//            mVideoEncoder.releaseOutputBuffer(index_deque, false);
//        } else if (index_deque == MediaCodec.INFO_TRY_AGAIN_LATER) {
//            Log.d(TAG, "pullBufferFromDecoder: INFO_TRY_AGAIN_LATER");
//        } else if(index_deque == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//            Log.d(TAG, "pullBufferFromDecoder: INFO_OUTPUT_FORMAT_CHANGED");
//            if(isMuxerStart) {
//                Log.d(TAG, "pullBufferFromDecoder addTrack twice");
//                return -1;
//            }
//            MediaFormat mediaFormat = mVideoEncoder.getOutputFormat();
//            track_index = mMediaMuxer.addTrack(mediaFormat);
//            Log.d(TAG, "onOutputFormatChanged: mMediaMuxer start");
//            mMediaMuxer.start();
//            isMuxerStart = true;
//
//        } else {
//            Log.d(TAG, "pullBufferFromDecoder: unexpected result from encoder, code = " + index_deque);
//        }
        return 0;
    }

    @Override
    public int pushBufferToEncoder(MediaCodec codec, int index) {
        return 0;
    }

    public Surface getEncodeSurface() {
        Log.d(TAG, "getEncodeSurface: ");
        if(mVideoEncoder == null) {
            return null;
        } else {
            return this.mVideoEncoder.createInputSurface();
        }
    }

    @Override
    public void run() {

    }

    public void drainEncoder(boolean value) {
        Log.d(TAG, "drainEncoder isRecording: " + isRecording);
        if(!isRecording) {
            return;
        }
        pullBufferFromDecoder(null, -1, null);
    }

    public void setEncodeWidth(int encode_width) {
        this.encode_width = encode_width;
    }

    public void setEncodeheight(int encode_height) {
        this.encode_height = encode_height;
    }
}
