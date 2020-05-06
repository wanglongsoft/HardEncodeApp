package com.wl.function.videorecord;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;
import java.util.List;

//https://juejin.im/post/5d19a60f6fb9a07efd47243b

public class VideoRecord {

    private final String TAG = "VideoRecord";

    private int mCameraId;
    private Camera mCamera;
    private SurfaceTexture surfaceTexture = null;
    private boolean shut = false;
    private int video_rotation;

    private int preview_width;
    private int preview_height;

    public VideoRecord(int cameraId) {
        Log.d(TAG, "VideoRecord: ");
        mCameraId = cameraId;
        this.preview_width = 1920;
        this.preview_height = 1080;
    }

    public void switchCamera() {
        Log.d(TAG, "switchCamera: ");
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        startPreview();
    }

    public void startPreview() {
        Log.d(TAG, "startPreview: ");
        try {
            mCamera = Camera.open(mCameraId);
            setCameraParameters(mCamera, mCameraId);
            Camera.Parameters parameters = mCamera.getParameters();
            getSupportParameter(parameters);
            parameters.setPreviewFrameRate(25);
            choosePreviewSize(parameters);
            mCamera.setParameters(parameters);
            if(null != this.surfaceTexture) {
                mCamera.setPreviewTexture(surfaceTexture);
            } else {
                Log.d(TAG, "startPreview: surfaceTexture == null");
            }
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "start preview failed");
            e.printStackTrace();
        }
    }

    private void choosePreviewSize(Camera.Parameters parameters) {
        Camera.Size ppsfv = parameters.getPreferredPreviewSizeForVideo();
        Log.d(TAG, "choosePreviewSize PreferredPreviewSizeForVideo width: " + ppsfv.width + " height : " + ppsfv.height);
        if (ppsfv != null) {
            parameters.setPreviewSize(this.preview_width, this.preview_height);
        }
        Log.d(TAG, "choosePreviewSize preview_width: " + preview_width + " preview_height: " + preview_height);
    }

    private void getSupportParameter(Camera.Parameters parameters) {
        List<Camera.Size> camera_size = parameters.getSupportedPreviewSizes();
        int camera_size_length = camera_size.size();
        for (int i = 0; i < camera_size_length; i++) {
            Log.d(TAG, "getSupportedPreviewSizes width: " + camera_size.get(i).width + " height : " + camera_size.get(i).height);
        }
        List<Integer> format_size = parameters.getSupportedPreviewFormats();
        int format_size_length = format_size.size();
        for (int i = 0; i < format_size_length; i++) {
            Log.d(TAG, "getSupportedPreviewFormats width: " + format_size.get(i));
        }
    }

    public void stopPreview() {
        Log.d(TAG, "stopPreview: ");
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public Camera.Size getPreviewSize(){
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        return previewSize;
    }

    public void setCameraParameters(Camera camera, int camera_id) {
        final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(camera_id, cameraInfo);
        video_rotation = cameraInfo.orientation;
        Log.d(TAG, "setCameraParameters video_rotation: " + video_rotation);
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
    }

    public int getPreviewWidth() {
        return this.preview_width;
    }

    public int getPreviewHeight() {
        return this.preview_height;
    }
}
