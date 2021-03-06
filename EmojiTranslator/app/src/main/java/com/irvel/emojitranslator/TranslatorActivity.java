package com.irvel.emojitranslator;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.irvel.emojitranslator.views.CameraPreview;
import com.irvel.emojitranslator.views.GraphicOverlay;

import java.io.IOException;

public class TranslatorActivity extends AppCompatActivity {

    private static final String TAG = "TranslatorActivity";

    private CameraSource mCameraSource = null;
    private GraphicOverlay mGraphicOverlay;
    private CameraPreview mPreview;
    private ImageButton mPauseButton;
    private boolean mCameraPaused = false;
    private int mCameraFacing = CameraSource.CAMERA_FACING_FRONT;

    private Emoji mEmoji;
    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;


    //==============================================================================================
    // Translator Activity
    //==============================================================================================

    /**
     * Initializes the UI, creates the face detector and provides the callback for the interface
     * buttons
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translator);
        mPreview = (CameraPreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        mPauseButton = (ImageButton) findViewById(R.id.pause_button);
        mEmoji = new Emoji();
        ImageButton copyButton = (ImageButton) findViewById(R.id.copy_button);
        final ImageButton cameraButton = (ImageButton) findViewById(R.id.switch_camera_button);
        setPreviewSize(mPreview);
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Copies the current emoji to the clipboard
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Emoji", mEmoji.getEmojiChar());
                clipboard.setPrimaryClip(clip);
                Snackbar.make(mPreview, R.string.copy_success,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                            }
                        })
                        .show();
            }
        });
        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updatePauseButton();
            }
        });
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCameraFacing == CameraSource.CAMERA_FACING_FRONT) {
                    if(mCameraPaused)
                        updatePauseButton();
                    mCameraSource.release();
                    mCameraFacing = CameraSource.CAMERA_FACING_BACK;
                    cameraButton.setImageResource(R.drawable.ic_camera_front_black_24dp);
                    createCameraSource();
                    startCameraSource();
                }
                else{
                    if(mCameraPaused)
                        updatePauseButton();
                    mCameraSource.release();
                    mCameraFacing = CameraSource.CAMERA_FACING_FRONT;
                    cameraButton.setImageResource(R.drawable.ic_camera_rear_black_24dp);
                    createCameraSource();
                    startCameraSource();
                }
            }
        });
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    private void updatePauseButton() {
        if(!mCameraPaused) {
            mPauseButton.setImageResource(R.drawable.ic_play_arrow_white_24dp);
            mPreview.stop();
            mCameraPaused = true;
        }
        else{
            mPauseButton.setImageResource(R.drawable.ic_pause_black_24dp);
            startCameraSource();
            mCameraPaused = false;
        }
    }

    private void setPreviewSize(CameraPreview preview){
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float logicalDensity = metrics.density;
        int px = (int) Math.ceil(200 * logicalDensity);
        preview.setPreviewHeight(px);
        preview.setPreviewWidth(px);
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mPreview, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setProminentFaceOnly(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        detector.setProcessor(
                new LargestFaceFocusingProcessor(
                        detector,
                        new GraphicFaceTracker(mGraphicOverlay, mEmoji)));

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.download_pending_title);
            builder.setMessage(R.string.download_pending_message);
            builder.setPositiveButton(R.string.ok, null);
            builder.show();
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }
        int previewSize;
        if(mCameraFacing == CameraSource.CAMERA_FACING_BACK)
            previewSize = 800;
        else
            previewSize = 320;

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(previewSize, previewSize)
                .setFacing(mCameraFacing)
                .setRequestedFps(30.0f)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
        if(mCameraPaused)
            updatePauseButton();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
        mCameraPaused = true;
        updatePauseButton();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }




    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;
        private Emoji mEmoji;
        private TextView mEmojiDisplay;

        GraphicFaceTracker(GraphicOverlay overlay, Emoji emoji) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
            mEmojiDisplay = (TextView) findViewById(R.id.emojiDisplay);
            mEmoji = emoji;
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
            mEmoji.setExpression(face);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mEmojiDisplay.setText(mEmoji.getEmojiChar());
                }
            });
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_translator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

