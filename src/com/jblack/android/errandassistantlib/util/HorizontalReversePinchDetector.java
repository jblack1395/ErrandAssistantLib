package com.jblack.android.errandassistantlib.util;

import android.util.Log;
import android.view.MotionEvent;

public class HorizontalReversePinchDetector {
	private static String GESTURE = HorizontalReversePinchDetector.class.getSimpleName();
	
    public boolean onTouchEvent(MotionEvent e) {

        int pointerCount = e.getPointerCount();

        if(pointerCount != 2) {
            Log.d(GESTURE, "not pinching - exactly 2 fingers are needed but have " + pointerCount);
            clearPinch();
            return false;
        }

        int firstIndex = e.getX(0) < e.getX(1) ? 0: 1;
        int secondIndex = e.getX(0) < e.getX(1) ? 1: 0;

        Finger currentLeftFinger = new Finger(e.getX(firstIndex), e.getY(firstIndex));
        Finger currentRightFinger = new Finger(e.getX(secondIndex), e.getY(secondIndex));

        float yDifference = Math.abs(currentLeftFinger.getY() - currentRightFinger.getY());
        if(yDifference > 80) {
            Log.d(GESTURE, "not pinching - fingers too vertically-oriented");
            clearPinch();
            return false;
        }

        if(initialLeftFinger == null) {
            initialLeftFinger = currentLeftFinger;
            initialRightFinger = currentRightFinger;
            Log.d(GESTURE, "not pinching, but might be starting a pinch...");
            return false;
        }

        float leftFingerDistance = initialLeftFinger.getX() - currentLeftFinger.getX();
        float rightFingerDistance = currentRightFinger.getX() - initialRightFinger.getX();

        float xDistanceBetweenFingers = Math.abs(currentLeftFinger.getX() - currentRightFinger.getX());
        if(xDistanceBetweenFingers < minimumDistanceBetweenFingers) {
            Log.d(GESTURE, "pinching, but fingers are not far enough apart...");
            return true;
        }

        if(leftFingerDistance < minimumDistanceForEachFinger) {
            Log.d(GESTURE, "pinching, but left finger has not moved enough...");
            return true;
        }
        if(rightFingerDistance < minimumDistanceForEachFinger) {
            Log.d(GESTURE, "pinching, but right finger has not moved enough...");
            return true;
        }

        pinchCompleted();
        return true;
    }

    private void pinchCompleted() {
        Log.d(GESTURE, "pinch completed");
        if(pinchListener != null) pinchListener.onPinch();
        clearPinch();
    }

    public static interface OnPinchListener {
        void onPinch();
    }

    private void clearPinch() {
        initialLeftFinger = null;
        initialRightFinger = null;
    }

    public void setPinchListener(OnPinchListener pinchListener) {
        this.pinchListener = pinchListener;
    }

    private static class Finger {

        private Finger(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        private float x;
        private float y;
    }

    private Finger initialLeftFinger;
    private Finger initialRightFinger;
    private OnPinchListener pinchListener;
    private static final float minimumDistanceForEachFinger = 30;
    private static final float minimumDistanceBetweenFingers = 50;
}