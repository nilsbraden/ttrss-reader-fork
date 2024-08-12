package org.ttrssreader.gui.view;

import android.app.Activity;
import android.view.MotionEvent;
import androidx.appcompat.app.ActionBar;
import org.ttrssreader.controllers.Controller;

public abstract class SwipeGestureListener extends MyGestureListener {
    protected SwipeGestureListener(ActionBar actionBar, boolean hideActionbar, Activity activity) {
        super(actionBar, hideActionbar, activity);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (Math.abs(e1.getX() - e2.getX()) > Controller.relSwipeMinDistance && Math.abs(velocityX) > Controller.relSwipeThresholdVelocity) {
            return onSwipe(e1, e2, velocityX, velocityY);
        } else {
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    public abstract boolean onSwipe(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);
}
