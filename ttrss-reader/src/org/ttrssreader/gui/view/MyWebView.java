package org.ttrssreader.gui.view;

import android.content.Context;
import android.view.View;
import android.webkit.WebView;

public class MyWebView extends WebView {
    
    public MyWebView(Context context) {
        super(context);
    }
    
    public OnEdgeReachedListener mOnTopReachedListener = null;
    public OnEdgeReachedListener mOnBottomReachedListener = null;
    private int mMinTopDistance = 0;
    private int mMinBottomDistance = 0;
    
    /**
     * Set the listener which will be called when the WebView is scrolled to within some
     * margin of the top or bottom.
     * 
     * @param bottomReachedListener
     * @param allowedDifference
     */
    public void setOnTopReachedListener(OnEdgeReachedListener topReachedListener, int allowedDifference) {
        mOnTopReachedListener = topReachedListener;
        mMinTopDistance = allowedDifference;
    }
    
    public void setOnBottomReachedListener(OnEdgeReachedListener bottomReachedListener, int allowedDifference) {
        mOnBottomReachedListener = bottomReachedListener;
        mMinBottomDistance = allowedDifference;
    }
    
    /**
     * Implement this interface if you want to be notified when the WebView has scrolled to the top or bottom.
     */
    public interface OnEdgeReachedListener {
        void onTopReached(View v, boolean reached);
        
        void onBottomReached(View v, boolean reached);
    }
    
    private boolean topReached = true;
    private boolean bottomReached = false;
    
    @Override
    protected void onScrollChanged(int left, int top, int oldLeft, int oldTop) {
        if (mOnTopReachedListener != null)
            handleTopReached(top);
        
        if (mOnBottomReachedListener != null)
            handleBottomReached(top);
        
        super.onScrollChanged(left, top, oldLeft, oldTop);
    }
    
    private void handleTopReached(int top) {
        boolean reached = false;
        if (top <= mMinTopDistance)
            reached = true;
        else if (top > (mMinTopDistance * 1.5))
            reached = false;
        else
            return;
        
        if (!reached && !topReached)
            return;
        if (reached && topReached)
            return;
        
        topReached = reached;
        
        mOnTopReachedListener.onTopReached(this, reached);
    }
    
    private void handleBottomReached(int top) {
        boolean reached = false;
        if ((getContentHeight() - (top + getHeight())) <= mMinBottomDistance)
            reached = true;
        else if ((getContentHeight() - (top + getHeight())) > (mMinBottomDistance * 1.5))
            reached = false;
        else
            return;
        
        if (!reached && !bottomReached)
            return;
        if (reached && bottomReached)
            return;
        
        bottomReached = reached;
        
        mOnBottomReachedListener.onBottomReached(this, reached);
    }
    
}
