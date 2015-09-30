package com.irvel.emojitranslator;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;
import com.irvel.emojitranslator.views.GraphicOverlay;

/**
 * Created by irvel on 29/09/2015.
 */

/**
 * Graphic instance for rendering landmarks and position of a detected face.
 * Includes fragments from the Android Vision API Samples: https://github.com/googlesamples/android-vision
 */

public class FaceGraphic extends GraphicOverlay.Graphic {

    private static final float BOX_STROKE_WIDTH = 4.0f;
    private final int drawColor = Color.WHITE;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;
    private int mFaceId;

    FaceGraphic(GraphicOverlay overlay) {
        super(overlay);

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(drawColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(drawColor);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(drawColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    void setId(int id) {
        mFaceId = id;
    }


    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);

        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        canvas.drawRect(left, top, right, bottom, mBoxPaint);

        Paint paint = new Paint();
        paint.setColor(drawColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(BOX_STROKE_WIDTH);

        //Draws the detected landmarks on the face
        for (Landmark landmark : mFace.getLandmarks()) {
            int cx = (int) translateX((landmark.getPosition().x));
            int cy = (int) translateY((landmark.getPosition().y));
            canvas.drawCircle(cx, cy, 10, paint);
        }
    }

}
