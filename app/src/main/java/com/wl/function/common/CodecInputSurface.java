package com.wl.function.common;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.Surface;

public class CodecInputSurface {

    private static final String TAG = "CodecInputSurface";
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mFrontEGLSurface = EGL14.EGL_NO_SURFACE;
    private EGLSurface mBackEGLSurface = EGL14.EGL_NO_SURFACE;

    private Surface mFrontSurface;
    private Surface mBackSurface;

    /**
     * Creates a CodecInputSurface from a Surface.
     */
    public CodecInputSurface(Surface front_surface, Surface back_surface) {
        Log.d(TAG, "CodecInputSurface: ");
        if (front_surface == null && back_surface == null) {
            throw new NullPointerException();
        }
        mFrontSurface = front_surface;
        mBackSurface = back_surface;
        eglSetup();
    }

    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports recording.
     */
    private void eglSetup() {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            throw new RuntimeException("unable to initialize EGL14");
        }

        // Configure EGL for recording and OpenGL ES 2.0.
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0);
        checkEglError("eglChooseConfig");

        // Configure context for OpenGL ES 2.0.
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };

        mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                attrib_list, 0);
        checkEglError("eglCreateContext");

        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };

        Log.d(TAG, "eglSetup mFrontSurface: " + (mFrontSurface != null) + " mBackSurface : " + (mBackSurface != null));
        if(null != mFrontSurface) {
            mFrontEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mFrontSurface, surfaceAttribs, 0);
        }
        if(null != mBackSurface) {
            mBackEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mBackSurface, surfaceAttribs, 0);
        }
        Log.d(TAG, "eglSetup mFrontEGLSurface: " + (mFrontEGLSurface != EGL14.EGL_NO_SURFACE) + " mBackEGLSurface : " + (mBackEGLSurface != EGL14.EGL_NO_SURFACE));
        checkEglError("eglCreateWindowSurface");
    }

    /**
     * Discards all resources held by this class, notably the EGL context.  Also releases the
     * Surface that was passed to our constructor.
     */
    public void release() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
            if(mFrontEGLSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(mEGLDisplay, mFrontEGLSurface);
            }
            if(mBackEGLSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(mEGLDisplay, mBackEGLSurface);
            }
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
        }

        if(null != mFrontSurface) {
            mFrontSurface.release();
            mFrontSurface = null;
        }
        if(null != mBackSurface) {
            mBackSurface.release();
            mBackSurface = null;
        }
        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
        mFrontEGLSurface = EGL14.EGL_NO_SURFACE;
        mBackEGLSurface = EGL14.EGL_NO_SURFACE;
    }

    /**
     * Makes our EGL context and surface current.
     */
    public void makeFrontCurrent() {
        if(mFrontEGLSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(mEGLDisplay, mFrontEGLSurface, mFrontEGLSurface, mEGLContext);
        }
        checkEglError("makeFrontCurrent");
    }

    /**
     * Makes our EGL context and surface current.
     */
    public void makeBackCurrent() {
        if(mBackEGLSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(mEGLDisplay, mBackEGLSurface, mBackEGLSurface, mEGLContext);
        }
        checkEglError("makeBackCurrent");
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     */
    public boolean swapFrontBuffers() {
        if(mFrontEGLSurface != EGL14.EGL_NO_SURFACE) {
            boolean result = EGL14.eglSwapBuffers(mEGLDisplay, mFrontEGLSurface);
            checkEglError("swapFrontBuffers");
            return result;
        } else {
            return false;
        }
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     */
    public boolean swapBackBuffers() {
        if(mBackEGLSurface != EGL14.EGL_NO_SURFACE) {
            boolean result = EGL14.eglSwapBuffers(mEGLDisplay, mBackEGLSurface);
            checkEglError("swapBackBuffers");
            return result;
        } else {
            return false;
        }
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    public void setFrontPresentationTime(long nsecs) {
        if(mFrontEGLSurface != EGL14.EGL_NO_SURFACE) {
            EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mFrontEGLSurface, nsecs);
            checkEglError("setFrontPresentationTime");
        }
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    public void setBackPresentationTime(long nsecs) {
        if(mBackEGLSurface != EGL14.EGL_NO_SURFACE) {
            EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mBackEGLSurface, nsecs);
            checkEglError("setBackPresentationTime");
        }
    }

    /**
     * Checks for EGL errors.  Throws an exception if one is found.
     */
    private void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    public void makeActiveCurrent() {
        Log.d(TAG, "makeActiveCurrent: ");
        if(mBackEGLSurface != EGL14.EGL_NO_SURFACE) {
            makeBackCurrent();
        } else if(mFrontEGLSurface != EGL14.EGL_NO_SURFACE) {
            makeFrontCurrent();
        } else {
            Log.d(TAG, "makeActiveCurrent: no eglsurface");
        }
    }
}
