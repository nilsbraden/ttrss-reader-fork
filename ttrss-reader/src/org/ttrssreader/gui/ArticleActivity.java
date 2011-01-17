/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
 * Copyright (C) 2009-2010 J. Devauchelle.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.gui;

import java.util.ArrayList;
import java.util.Set;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.ArticleItem;
import org.ttrssreader.model.updaters.PublishedStateUpdater;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.StarredStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.net.ITTRSSConnector;
import org.ttrssreader.utils.Utils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

public class ArticleActivity extends Activity {
    
    public static final String ARTICLE_ID = "ARTICLE_ID";
    public static final String FEED_ID = "FEED_ID";
    public static final String ARTICLE_LIST_ID = "ARTICLE_LIST_ID";
    
    private int mArticleId;
    private int mFeedId;
    private ArrayList<Integer> mArticleIds;
    
    private ArticleItem mArticleItem = null;
    
    private WebView webview;
    private TextView webviewSwipeText;
    private GestureDetector mGestureDetector;
    private boolean useSwipe;
    private int absHeight;
    private int absWidth;
    private int swipeHeight;
    private int swipeWidth;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.articleitem);
        
        Controller.getInstance().checkAndInitializeController(this);
        DBHelper.getInstance().checkAndInitializeDB(this);
        Data.getInstance().checkAndInitializeData(this);
        
        webview = (WebView) findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setBuiltInZoomControls(true);
        
        webview.setWebViewClient(new ArticleWebViewClient());
        mGestureDetector = new GestureDetector(onGestureListener);
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        absHeight = metrics.heightPixels;
        absWidth = metrics.widthPixels;
        swipeHeight = (int) (absHeight * 0.25); // 25% height
        swipeWidth = (int) (absWidth * 0.5); // 50% width
        int padding = (int) ((swipeHeight / 2) - (16 * metrics.density));
        
        webviewSwipeText = (TextView) findViewById(R.id.webview_swipe_text);
        webviewSwipeText.setVisibility(TextView.INVISIBLE);
        webviewSwipeText.setPadding(16, padding, 16, padding);
        useSwipe = Controller.getInstance().isUseSwipe();
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mArticleId = extras.getInt(ARTICLE_ID);
            mFeedId = extras.getInt(FEED_ID);
            mArticleIds = extras.getIntegerArrayList(ARTICLE_LIST_ID);
        } else if (instance != null) {
            mArticleId = instance.getInt(ARTICLE_ID);
            mFeedId = instance.getInt(FEED_ID);
            mArticleIds = instance.getIntegerArrayList(ARTICLE_LIST_ID);
        } else {
            mArticleId = -1;
            mFeedId = -1;
            mArticleIds = new ArrayList<Integer>();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        DBHelper.getInstance().checkAndInitializeDB(getApplicationContext());
        doRefresh();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARTICLE_ID, mArticleId);
        outState.putInt(FEED_ID, mFeedId);
        outState.putIntegerArrayList(ARTICLE_LIST_ID, mArticleIds);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.article, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        MenuItem read = menu.findItem(R.id.Article_Menu_MarkRead);
        if (mArticleItem.isUnread()) {
            read.setTitle(getString(R.string.Commons_MarkRead));
        } else {
            read.setTitle(getString(R.string.Commons_MarkUnread));
        }
        
        MenuItem publish = menu.findItem(R.id.Article_Menu_MarkStar);
        if (mArticleItem.isStarred()) {
            publish.setTitle(getString(R.string.Commons_MarkUnstar));
        } else {
            publish.setTitle(getString(R.string.Commons_MarkStar));
        }
        
        MenuItem star = menu.findItem(R.id.Article_Menu_MarkPublish);
        if (mArticleItem.isPublished()) {
            star.setTitle(getString(R.string.Commons_MarkUnpublish));
        } else {
            star.setTitle(getString(R.string.Commons_MarkPublish));
        }
        
        return true;
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Article_Menu_MarkRead:
                new Updater(null, new ReadStateUpdater(mArticleItem, mFeedId, mArticleItem.isUnread() ? 0 : 1))
                        .execute();
                return true;
            case R.id.Article_Menu_MarkStar:
                new Updater(null, new StarredStateUpdater(mArticleItem, mArticleItem.isStarred() ? 0 : 1)).execute();
                return true;
            case R.id.Article_Menu_MarkPublish:
                new Updater(null, new PublishedStateUpdater(mArticleItem, mArticleItem.isPublished() ? 0 : 1))
                        .execute();
                return true;
            case R.id.Article_Menu_OpenLink:
                openLink();
                return true;
            case R.id.Article_Menu_ShareLink:
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, mArticleItem.getTitle());
                String content = (String) getText(R.string.ArticleActivity_ShareSubject);
                i.putExtra(Intent.EXTRA_TEXT, content + " " + mArticleItem.getArticleUrl());
                this.startActivity(Intent.createChooser(i, (String) getText(R.string.ArticleActivity_ShareTitle)));
                return true;
            default:
                return false;
        }
    }
    
    private void openLink() {
        if (mArticleItem != null) {
            String url = mArticleItem.getArticleUrl();
            if ((url != null) && (url.length() > 0)) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        }
    }
    
    private void doRefresh() {
        setProgressBarIndeterminateVisibility(true);
        
        if (Controller.getInstance().isWorkOffline()) {
            webview.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
        } else {
            webview.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        }
        
        if (!ITTRSSConnector.hasLastError()) {
            mArticleItem = Data.getInstance().getArticle(mArticleId);
            
            // Check if articleItem and content are null, if it is so do update the article again
            if (mArticleItem == null || mArticleItem.getContent() == null) {
                ArticleItem temp = Data.getInstance().updateArticle(mArticleId);
                if (temp != null && temp.getContent() != null) {
                    mArticleItem = temp;
                }
            }
            
            if (mArticleItem != null && mArticleItem.getContent() != null) {
                // Inject the specific code for attachments, <img> for images, http-link for Videos
                String content = injectAttachments(getApplicationContext(), mArticleItem.getContent(),
                        mArticleItem.getAttachments());
                content = Utils.injectCachedImages(content);
                
                // Use if loadDataWithBaseURL, 'cause loadData is buggy (encoding error & don't support "%" in html).
                webview.loadDataWithBaseURL(null, content, "text/html", "utf-8", "about:blank");
                
                if (mArticleItem.getTitle() != null) {
                    this.setTitle(mArticleItem.getTitle());
                } else {
                    this.setTitle(this.getResources().getString(R.string.ApplicationName));
                }
                
                if (mArticleItem.isUnread() && Controller.getInstance().isAutomaticMarkRead()) {
                    new Updater(null, new ReadStateUpdater(mArticleItem, mFeedId, 0)).execute();
                }
                
                if (content.length() < 3) {
                    if (Controller.getInstance().isOpenUrlEmptyArticle()) {
                        Log.i(Utils.TAG, "Article-Content is empty, opening URL in browser");
                        openLink();
                    }
                }
                
            }
        } else {
            openConnectionErrorDialog(ITTRSSConnector.pullLastError());
        }
        
        setProgressBarIndeterminateVisibility(false);
    }
    
    private void openNextArticle(int direction) {
        
        int index = mArticleIds.indexOf(mArticleId) + direction;
        
        // No more articles in this direction
        if (index < 0 || index >= mArticleIds.size()) {
            if (Controller.getInstance().isVibrateOnLastArticle()) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(Utils.SHORT_VIBRATE);
            }
            return;
        }
        
        Intent i = new Intent(this, ArticleActivity.class);
        i.putExtra(ArticleActivity.ARTICLE_ID, mArticleIds.get(index));
        i.putExtra(ArticleActivity.FEED_ID, mFeedId);
        i.putIntegerArrayListExtra(ArticleActivity.ARTICLE_LIST_ID, mArticleIds);
        
        Log.v(Utils.TAG, "openArticle() FeedID: " + mFeedId + ", ArticleID: " + mArticleIds.get(index));
        
        startActivityForResult(i, 0);
        this.finish();
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        super.dispatchTouchEvent(e);
        return mGestureDetector.onTouchEvent(e);
    }
    
    private OnGestureListener onGestureListener = new OnGestureListener() {
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            
            int SWIPE_BOTTOM = webview.getHeight() - swipeHeight;
            
            int dx = (int) (e2.getX() - e1.getX());
            int dy = (int) (e2.getY() - e1.getY());
            
            if (Math.abs(dy) > (int) (absHeight * 0.2)) {
                // Too much Y-Movement (20% of screen-height)
                return false;
            } else if (e1.getY() < SWIPE_BOTTOM || e2.getY() < SWIPE_BOTTOM) {
                // Only accept swipe in SWIPE_AREA so we can use scrolling as usual
                if (Math.abs(dx) > swipeWidth && Math.abs(velocityX) > Math.abs(velocityY)) {
                    
                    // Display text for swipe-area
                    webviewSwipeText.setVisibility(TextView.VISIBLE);
                    new Handler().postDelayed(timerTask, 1000);
                }
                return false;
            } else if (!useSwipe) {
                return false;
            }
            
            // don't accept the fling if it's too short as it may conflict with a button push
            if (Math.abs(dx) > swipeHeight && Math.abs(velocityX) > Math.abs(velocityY)) {
                
                // Log.d(Utils.TAG
                // String.format("Fling: (%s %s)(%s %s) dx: %s dy: %s (Direction: %s)", e1.getX(), e1.getY()
                // e2.getX(), e2.getY(), dx, dy, (velocityX > 0) ? "right" : "left"))
                // Log.d(Utils.TAG, String.format("SWIPE_HEIGHT: %s SWIPE_WIDTH: %s", swipeHeight, swipeWidth));
                
                if (velocityX > 0) {
                    Log.v(Utils.TAG, "Fling right");
                    openNextArticle(-1);
                } else {
                    Log.v(Utils.TAG, "Fling left");
                    openNextArticle(1);
                }
                return true;
            }
            return false;
        }
        
        private Runnable timerTask = new Runnable() {
            // Need this to set the text invisible after some time
            public void run() {
                webviewSwipeText.setVisibility(TextView.INVISIBLE);
            }
        };
        
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }
        
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }
        
        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }
        
        @Override
        public void onLongPress(MotionEvent e) {
        }
        
        @Override
        public void onShowPress(MotionEvent e) {
        }
    };
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Controller.getInstance().isUseVolumeKeys()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                openNextArticle(-1);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                openNextArticle(1);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (Controller.getInstance().isUseVolumeKeys()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }
    
    private void openConnectionErrorDialog(String errorMessage) {
        Intent i = new Intent(this, ErrorActivity.class);
        i.putExtra(ErrorActivity.ERROR_MESSAGE, errorMessage);
        startActivityForResult(i, ErrorActivity.ACTIVITY_SHOW_ERROR);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ErrorActivity.ACTIVITY_SHOW_ERROR) {
            doRefresh();
        }
    }
    
    private static String injectAttachments(Context context, String content, Set<String> attachments) {
        StringBuilder ret = new StringBuilder(content);
        
        for (String url : attachments) {
            if (url.length() == 0) {
                continue;
            }
            
            boolean image = false;
            for (String s : Utils.IMAGE_EXTENSIONS) {
                if (url.toLowerCase().contains(s)) {
                    image = true;
                    break;
                }
            }
            
            boolean audioOrVideo = false;
            for (String s : Utils.MEDIA_EXTENSIONS) {
                if (url.toLowerCase().contains(s)) {
                    audioOrVideo = true;
                    break;
                }
            }
            
            ret.append("<br>\n");
            if (image) {
                ret.append("<img src=\"");
                ret.append(url);
                ret.append("\" /><br>\n");
            } else if (audioOrVideo) {
                ret.append("<a href=\"");
                ret.append(url);
                ret.append("\">" + (String) context.getText(R.string.ArticleActivity_MediaPlay) + "</a>");
            } else {
                ret.append("<a href=\"");
                ret.append(url);
                ret.append("\">" + (String) context.getText(R.string.ArticleActivity_MediaDisplayLink) + "</a>");
            }
        }
        
        return ret.toString();
    }
    
}
