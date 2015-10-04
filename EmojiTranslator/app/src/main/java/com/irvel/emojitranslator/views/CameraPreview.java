package com.irvel.emojitranslator.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;

import java.io.IOException;

/**
 * Created by irvel on 29/09/2015.
 */

//==============================================================================================
// Camera Preview
//==============================================================================================

/**
 * Custom view for the camera viewfinder
 * Includes fragments from the Android Vision API Samples: https://github.com/googlesamples/android-vision
 */


public class CameraPreview extends ViewGroup{
    private static final String TAG = "CameraPreview";

    private Context mContext;
    private SurfaceView mSurfaceView;
    private boolean mStartRequested;
    private boolean mSurfaceAvailable;
    private CameraSource mCameraSource;
    private int mPreviewWidth;
    private int mPreviewHeight;

    private GraphicOverlay mOverlay;


    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mStartRequested = false;
        mSurfaceAvailable = false;
        mSurfaceView = new SurfaceView(context);
        mSurfaceView.getHolder().addCallback(new SurfaceCallback());
        addView(mSurfaceView);
    }

    public void setPreviewWidth(int previewWidth) {
        this.mPreviewWidth = previewWidth;
    }

    public void setPreviewHeight(int previewHeight) {
        this.mPreviewHeight = previewHeight;
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder surface) {
            mSurfaceAvailable = true;
            try {
                startIfReady();
            } catch (IOException e) {
                Log.e(TAG, "Could not start camera source.", e);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surface) {
            mSurfaceAvailable = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }
    }

    private void startIfReady() throws IOException {
        if (mStartRequested && mSurfaceAvailable) {
            mCameraSource.start(mSurfaceView.getHolder());
            if (mOverlay != null) {
                Size size = mCameraSource.getPreviewSize();
                int min = Math.min(size.getWidth(), size.getHeight());
                int max = Math.max(size.getWidth(), size.getHeight());
                mOverlay.setCameraInfo(min, max, mCameraSource.getCameraFacing());
                mOverlay.clear();
            }
            mStartRequested = false;
        }
    }

    // Starts the camera preview
    public void start(CameraSource cameraSource, GraphicOverlay overlay) throws IOException {
        mOverlay = overlay;
        if (cameraSource == null) {
            stop();
            mCameraSource = null;
        }
        else{
            mCameraSource = cameraSource;
            mStartRequested = true;
            startIfReady();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (int i = 0; i < getChildCount(); ++i) {
            getChildAt(i).layout(0, 0, mPreviewWidth, mPreviewHeight);
        }

        try {
            startIfReady();
        } catch (IOException e) {
            Log.e(TAG, "Could not start camera source.", e);
        }
    }

    public void stop() {
        if (mCameraSource != null) {
            mCameraSource.stop();
        }
    }
}
