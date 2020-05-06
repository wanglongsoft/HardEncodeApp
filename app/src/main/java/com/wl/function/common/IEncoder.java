package com.wl.function.common;

import android.media.MediaCodec;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

public interface IEncoder extends Runnable {
    /**
     * 开始编码
     */
    void startEncode();
    /**
     * 停止编码
     */
    void stopEncode();
    /**
     * 是否正在编码
     */
    boolean isEncoding();
    /**
     * 初始化编码器
     */
    void initEncode();
    /**
     * 从编码器取出数据
     */
    int pullBufferFromDecoder(MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info);
    /**
     * 给编码器输入数据
     */
    int pushBufferToEncoder(MediaCodec codec, int index);
}
