package com.wl.function.manager;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;

import com.wl.function.common.CodecInputSurface;
import com.wl.function.common.SurfaceTextureManager;
import com.wl.function.common.ThreadUtils;
import com.wl.function.videoencode.VideoEncode;
import com.wl.function.videorecord.VideoRecord;

public class VideoManager {

    private final String TAG = "VideoManager";
    private VideoRecord videoRecord;
    private SurfaceTextureManager surfaceTextureManager;
    private CodecInputSurface codecInputSurface;
    private VideoEncode videoEncode;

    private static final boolean VERBOSE = true;  // lots of logging

    private Surface surface_encode = null;
    private Surface surface_view = null;

    private boolean is_start_preview;
    private boolean is_start_record;
    private boolean is_black_white;
    private boolean is_update_fragment;

    // Fragment shader that swaps color channels around.
    private static final String SWAPPED_FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    vec4 nColor = texture2D(sTexture, vTextureCoord);\n" +
                    "    float average_color = nColor.r * 0.40 + nColor.g * 0.20 + nColor.b * 0.20;\n" +
                    "    gl_FragColor = vec4(average_color, average_color, average_color, 1);\n" +
                    "}\n";

    public VideoManager() {
        Log.d(TAG, "VideoManager: ");
        initVideoManager();
    }

    private void initVideoManager() {
        is_start_preview = false;
        is_start_record = false;
        is_black_white = false;
        is_update_fragment = false;
        videoRecord = new VideoRecord(Camera.CameraInfo.CAMERA_FACING_FRONT);
        videoEncode = new VideoEncode();
    }

    public void setSurfaceEncode(Surface surface_encode) {
        Log.d(TAG, "setSurfaceEncode: ");
        this.surface_encode = surface_encode;
    }

    public void setViewSurface(Surface view_surface) {
        Log.d(TAG, "setViewSurface: ");
        this.surface_view = view_surface;
    }

    public void startPreview() {
        Log.d(TAG, "startPreview is_start_preview: " + is_start_preview);
        ThreadUtils.getInstance().addTasks(new Runnable() {
            @Override
            public void run() {
                startWorking();
            }
        });
    }

    private void startWorking() {
        Log.d(TAG, "startWorking: ");
        if(is_start_preview) {
            return;
        }

        if(null == codecInputSurface) {
            Log.d(TAG, "startPreview surface_view == null: " + (this.surface_view == null)
                    + "  surface_encode == null : " + (this.surface_encode == null));
            codecInputSurface = new CodecInputSurface(this.surface_view, null);
            codecInputSurface.makeActiveCurrent();
        }

        if(null == surfaceTextureManager) {
            surfaceTextureManager = new SurfaceTextureManager();
            videoRecord.setSurfaceTexture(surfaceTextureManager.getSurfaceTexture());
        }

        videoRecord.startPreview();
        is_start_preview = true;

        SurfaceTexture st = surfaceTextureManager.getSurfaceTexture();
        while (is_start_preview || is_start_record) {
            if(is_update_fragment) {
                if(is_black_white) {
                    surfaceTextureManager.changeFragmentShader(SWAPPED_FRAGMENT_SHADER);
                } else {
                    surfaceTextureManager.changeFragmentShader(null);
                }
                is_update_fragment = false;
            }
            surfaceTextureManager.awaitNewImage();
            if(!surfaceTextureManager.getSurfaceAvailable()) {
                Log.d(TAG, "startWorking: surface is not available, break");
                break;
            }
            codecInputSurface.makeFrontCurrent();
            surfaceTextureManager.drawImage();
            codecInputSurface.setFrontPresentationTime(st.getTimestamp());
            codecInputSurface.swapFrontBuffers();
//            drainEncoder(false);
        }
//        drainEncoder(true);
    }

    private void drainEncoder(boolean value) {
        Log.d(TAG, "drainEncoder is_start_record: " + is_start_record);
        if(is_start_record) {
            videoEncode.drainEncoder(value);
        }
    }

    public void startVideoRecord() {
        Log.d(TAG, "startVideoRecord is_start_record: " + is_start_record);
        if(is_start_record) {
            return;
        }

        if(!is_start_preview) {
            startPreviewEncode();
        } else {

        }
    }

    private void startPreviewEncode() {
        Log.d(TAG, "startPreviewEncode is_start_preview: " + is_start_preview);
        ThreadUtils.getInstance().addTasks(new Runnable() {
            @Override
            public void run() {
                startWorkingEncode();
            }
        });
    }

    private void startWorkingEncode() {
        Log.d(TAG, "startWorkingEncode: ");
        if(is_start_preview) {
            return;
        }

        videoEncode.setEncodeWidth(videoRecord.getPreviewHeight());
        videoEncode.setEncodeheight(videoRecord.getPreviewWidth());
        videoEncode.initEncode();
        setSurfaceEncode(videoEncode.getEncodeSurface());

        if(null == codecInputSurface) {
            Log.d(TAG, "startWorkingEncode surface_view == null: " + (this.surface_view == null)
                    + "  surface_encode == null : " + (this.surface_encode == null));
            codecInputSurface = new CodecInputSurface(this.surface_view, this.surface_encode);
            codecInputSurface.makeActiveCurrent();
        }

        if(null == surfaceTextureManager) {
            surfaceTextureManager = new SurfaceTextureManager();
            videoRecord.setSurfaceTexture(surfaceTextureManager.getSurfaceTexture());
        }

        videoRecord.startPreview();
        is_start_preview = true;

        videoEncode.startEncode();
        is_start_record = true;

        SurfaceTexture st = surfaceTextureManager.getSurfaceTexture();
        while (is_start_preview || is_start_record) {
            if(is_update_fragment) {
                if(is_black_white) {
                    surfaceTextureManager.changeFragmentShader(SWAPPED_FRAGMENT_SHADER);
                } else {
                    surfaceTextureManager.changeFragmentShader(null);
                }
                is_update_fragment = false;
            }
            surfaceTextureManager.awaitNewImage();
            if((surfaceTextureManager == null) || (!surfaceTextureManager.getSurfaceAvailable())) {
                Log.d(TAG, "startWorkingEncode: surface is not available, break");
                break;
            }

            codecInputSurface.makeFrontCurrent();
            surfaceTextureManager.drawImage();
            codecInputSurface.setFrontPresentationTime(st.getTimestamp());
            codecInputSurface.swapFrontBuffers();

            codecInputSurface.makeBackCurrent();
            surfaceTextureManager.drawImage();
            codecInputSurface.setBackPresentationTime(st.getTimestamp());
            codecInputSurface.swapBackBuffers();
//            drainEncoder(false);
        }
//        drainEncoder(true);
    }

    public void stopVideoRecord() {
        Log.d(TAG, "stopVideoRecord is_start_record: " + is_start_record);
        if(!is_start_record) {
            return;
        }
        videoEncode.stopEncode();
        is_start_record = false;
    }

    public void stopPreview() {
        Log.d(TAG, "stopPreview is_start_preview: " + is_start_preview);
        if(!is_start_preview) {
            return;
        }
        is_start_preview = false;
        videoRecord.stopPreview();
        surfaceTextureManager.setSurfaceAvailable(false);
        surfaceTextureManager.release();
        codecInputSurface.release();
        surfaceTextureManager = null;
        codecInputSurface = null;
    }

    public void stopVideoManager() {
        Log.d(TAG, "stopVideoManager: ");
        if(!is_start_preview && !is_start_record) {
            return;
        }
        stopVideoRecord();
        stopPreview();
    }

    public void blackWhitePreview() {
        Log.d(TAG, "blackWhitePreview: ");
        is_black_white = true;
        is_update_fragment = true;
    }

    public void normalWhitePreview() {
        Log.d(TAG, "normalWhitePreview: ");
        is_black_white = false;
        is_update_fragment = true;
    }
}
