package com.irvel.emojitranslator;

import com.google.android.gms.vision.face.Face;

/**
 * Created by irvel on 29/09/2015.
 */
public class Emoji {
    private float mLeftEyeOpen;
    private float mRightEyeOpen;
    private float mSmiling;
    private String mEmojiChar;

    public Emoji(){
        mLeftEyeOpen = Face.UNCOMPUTED_PROBABILITY;
        mRightEyeOpen = Face.UNCOMPUTED_PROBABILITY;
        mSmiling = Face.UNCOMPUTED_PROBABILITY;
        mEmojiChar = "";
    }

    public String getEmojiChar() {
        return mEmojiChar;
    }

    public void setExpression(Face face){
        mLeftEyeOpen = face.getIsLeftEyeOpenProbability();
        mRightEyeOpen = face.getIsRightEyeOpenProbability();
        mSmiling = face.getIsSmilingProbability();
        calculateEmoji();
    }

    private void calculateEmoji() {
        //If there is no smile, display a sad emoji
        if(mSmiling == Face.UNCOMPUTED_PROBABILITY){
            mEmojiChar = "\uD83D\uDE1F";
        }
    }
}

