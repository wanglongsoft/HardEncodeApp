package com.wl.function.avmuxer;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.wl.function.audioencode.AudioEncode;
import com.wl.function.common.ThreadUtils;
import com.wl.function.videoencode.VideoEncode;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AVMuxer {//    封装音频文件(mp4)和视频文件(mp4)为新的mp4文件
    private final String TAG = "AVMuxer";
    private final String OUTPUT_MUXER_PATH = Environment.getExternalStorageDirectory() + File.separator + "filefilm" + File.separator + "audio_video.mp4";
    private MediaMuxer mediaMuxer;

    private AudioExtractor audioExtractor;
    private VideoExtractor videoExtractor;

    private MediaFormat audioMediaFormat = null;
    private MediaFormat videoMediaFormat = null;

    private int audio_index = -1;
    private int video_index = -1;

    MediaCodec.BufferInfo audio_info = new MediaCodec.BufferInfo();
    MediaCodec.BufferInfo video_info = new MediaCodec.BufferInfo();

    ByteBuffer audio_buffer;
    ByteBuffer video_buffer;

    private boolean media_audio_add_track;
    private boolean media_video_add_track;

    private boolean media_audio_muxer_start;
    private boolean media_video_muxer_start;

    private int out_audio_index = -1;
    private int out_video_index = -1;

    // 最大缓冲区(1024 x 1024 = 1048576、1920 x 1080 = 2073600)
    private static final int MAX_BUFF_SIZE = 2073600;

    private Activity activity;
    public AVMuxer(Activity activity) {
        this.activity = activity;
        initAVMuxer();
    }

    private void initAVMuxer() {
        Log.d(TAG, "initAVMuxer: ");
        audioExtractor = new AudioExtractor(AudioEncode.OUTPUT_MUXER_PATH);
        videoExtractor = new VideoExtractor(VideoEncode.OUTPUT_MUXER_PATH);

        audioMediaFormat = audioExtractor.getFormat();
        videoMediaFormat = videoExtractor.getFormat();

        audio_index = audioExtractor.getTrack();
        video_index = videoExtractor.getTrack();

        media_audio_add_track = false;
        media_video_add_track = false;

        audio_buffer = ByteBuffer.allocate(MAX_BUFF_SIZE);
        video_buffer = ByteBuffer.allocate(MAX_BUFF_SIZE);
        Log.d(TAG, "initAVMuxer audio_index: " + audio_index + " video_index: " + video_index);
        Log.d(TAG, "initAVMuxer audioMediaFormat: " + (audioMediaFormat != null) + " videoMediaFormat: " + (videoMediaFormat != null));
    }

    public void startAVMuxer() {
        Log.d(TAG, "startAVMuxer: ");
        if(audio_index < 0 || video_index < 0) {
            showUIMessage("File parse fail, audio_index : " + audio_index + " video_index: " + video_index);
            return;
        }

        try {
            mediaMuxer = new MediaMuxer(OUTPUT_MUXER_PATH, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if((audio_index >= 0) && (audioMediaFormat != null)) {
            Log.d(TAG, "startAVMuxer add audioMediaFormat");
            out_audio_index = mediaMuxer.addTrack(audioMediaFormat);
            media_audio_add_track = true;
        }
        if((video_index >= 0) && (videoMediaFormat != null)) {
            Log.d(TAG, "startAVMuxer add videoMediaFormat");
            out_video_index = mediaMuxer.addTrack(videoMediaFormat);
            media_video_add_track = true;
        }

        if(media_audio_add_track || media_video_add_track) {
            Log.d(TAG, "startAVMuxer mediaMuxer start");
            mediaMuxer.start();
        }

        Log.d(TAG, "startAVMuxer out_audio_index: " + out_audio_index + " out_video_index: " + out_video_index);
        if(media_audio_add_track) {
            ThreadUtils.getInstance().addTasks(new Runnable() {
                @Override
                public void run() {
                    writeAudioData();
                }
            });
        }
        if(media_video_add_track) {
            ThreadUtils.getInstance().addTasks(new Runnable() {
                @Override
                public void run() {
                    writeVideoData();
                }
            });
        }
    }

    private void writeAudioData() {
        Log.d(TAG, "writeAudioData: in");
        media_audio_muxer_start = true;
        int sample_size = -1;
        while ((sample_size = audioExtractor.readBuffer(audio_buffer)) >= 0) {
            audio_info.offset = 0;
            audio_info.flags = audioExtractor.getSampleFlag();
            audio_info.presentationTimeUs = audioExtractor.getCurrentTimestamp();
            audio_info.size = sample_size;  //  必须设置，否则 mediaMuxer.stop报异常
            mediaMuxer.writeSampleData(out_audio_index, audio_buffer, audio_info);
        }
        media_audio_muxer_start = false;
        Log.d(TAG, "writeAudioData: out");
        if(media_audio_muxer_start || media_video_muxer_start) {
            return;
        }
        stopAVMuxer();
    }

    private void writeVideoData() {
        Log.d(TAG, "writeVideoData: in");
        media_video_muxer_start = true;
        int sample_size = -1;
        while ((sample_size = videoExtractor.readBuffer(video_buffer)) >= 0) {
            video_info.offset = 0;
            video_info.flags = videoExtractor.getSampleFlag();
            video_info.presentationTimeUs = videoExtractor.getCurrentTimestamp();
            video_info.size = sample_size;  //  必须设置，否则 mediaMuxer.stop报异常
            mediaMuxer.writeSampleData(out_video_index, video_buffer, video_info);
        }
        media_video_muxer_start = false;
        Log.d(TAG, "writeVideoData: out");
        if(media_audio_muxer_start || media_video_muxer_start) {
            return;
        }
        stopAVMuxer();
    }

    public void stopAVMuxer() {
        Log.d(TAG, "stopAVMuxer: ");
        audioExtractor.stop();
        videoExtractor.stop();
        if(this.mediaMuxer != null) {
            this.mediaMuxer.stop();
            this.mediaMuxer.release();
        }
        showUIMessage("File merge success");
    }

    public void showUIMessage(String msg) {
        final String ui_msg = msg;
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, ui_msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
