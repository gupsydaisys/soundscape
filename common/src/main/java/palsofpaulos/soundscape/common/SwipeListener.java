package palsofpaulos.soundscape.common;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class SwipeListener implements OnTouchListener {

    private static final int SWIPE_THRESHOLD = 50;
    private static final int SWIPE_VELOCITY_THRESHOLD = 20;

    float downX = 0;
    float upX = 0;

    private final GestureDetector gestureDetector;

    public SwipeListener (Context ctx){
        gestureDetector = new GestureDetector(ctx, new GestureListener());
    }

    public boolean onTouch(View v, MotionEvent event) {
        //return gestureDetector.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downX = event.getX();
                return false;
            }
            case MotionEvent.ACTION_UP: {
                upX = event.getX();
                float deltaX = upX - downX;

                if (Math.abs(deltaX) > SWIPE_THRESHOLD) {
                    if (deltaX < 0) {
                        onSwipeLeft();
                        return true;
                    }
                    if (deltaX > 0) {
                        onSwipeRight();
                        return true;
                    }
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        return true;
//        gestureDetector.onTouchEvent(event);
//        return false;
    }

    private final class GestureListener extends SimpleOnGestureListener {



        @Override
        public boolean onDown(MotionEvent e) {
            onClick();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                    }
                    result = true;
                }
                else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom();
                    } else {
                        onSwipeTop();
                    }
                }
                result = true;

            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return false;
        }
    }

    public void onSwipeRight() {
    }

    public void onSwipeLeft() {
    }

    public void onSwipeTop() {
    }

    public void onSwipeBottom() {
    }

    public void onClick() {
    }
}