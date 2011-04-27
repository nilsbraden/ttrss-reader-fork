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

import java.util.Set;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.FeedHeadlineAdapter;
import org.ttrssreader.model.cachers.ImageCacher;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.updaters.PublishedStateUpdater;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.StarredStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.net.JSONConnector;
import org.ttrssreader.utils.StringSupport;
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
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

public class ArticleActivity extends Activity {
    
    public static final String ARTICLE_ID = "ARTICLE_ID";
    public static final String FEED_ID = "FEED_ID";
    
    private int articleId = -1;
    private int feedId = -1;
    
    private ArticleHeaderView headerContainer;
    
    private Article article = null;
    private String content;
    private boolean linkAutoOpened;
    private int categoryId = -1000;
    private boolean selectArticlesForCategory = false;
    
    private WebView webview;
    private TextView webviewSwipeText;
    private GestureDetector mGestureDetector;
    private boolean useSwipe;
    
    private String baseUrl = null;
    
    private FeedHeadlineAdapter parentAdapter = null;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        Controller.getInstance().checkAndInitializeController(this);
        Controller.initializeArticleViewStuff(getWindowManager().getDefaultDisplay(), new DisplayMetrics());
        DBHelper.getInstance().checkAndInitializeDB(this);
        Data.getInstance().checkAndInitializeData(this);
        
        if (Controller.getInstance().displayArticleHeader())
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.articleitem);
        headerContainer = (ArticleHeaderView) findViewById(R.id.article_header_container);
        
        webview = (WebView) findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setBuiltInZoomControls(true);
        
        webview.setWebViewClient(new ArticleWebViewClient(this));
        mGestureDetector = new GestureDetector(onGestureListener);
        webview.setOnKeyListener(keyListener);
        
        webviewSwipeText = (TextView) findViewById(R.id.webview_swipe_text);
        webviewSwipeText.setVisibility(TextView.INVISIBLE);
        webviewSwipeText.setPadding(16, Controller.padding, 16, Controller.padding);
        useSwipe = Controller.getInstance().useSwipe();
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            articleId = extras.getInt(ARTICLE_ID);
            feedId = extras.getInt(FEED_ID);
            categoryId = extras.getInt(FeedHeadlineActivity.FEED_CAT_ID);
            selectArticlesForCategory = extras.getBoolean(FeedHeadlineActivity.FEED_SELECT_ARTICLES);
        } else if (instance != null) {
            articleId = instance.getInt(ARTICLE_ID);
            feedId = instance.getInt(FEED_ID);
            categoryId = instance.getInt(FeedHeadlineActivity.FEED_CAT_ID);
            selectArticlesForCategory = instance.getBoolean(FeedHeadlineActivity.FEED_SELECT_ARTICLES);
        }
        
        parentAdapter = new FeedHeadlineAdapter(getApplicationContext(), feedId, categoryId, selectArticlesForCategory);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Controller.getInstance().lastOpenedArticle = articleId;
        DBHelper.getInstance().checkAndInitializeDB(this);
        doRefresh();
    }
    
    private void closeCursor() {
        if (parentAdapter != null) {
            parentAdapter.closeCursor();
        }
    }
    
    @Override
    protected void onPause() {
        // First call super.onXXX, then do own clean-up. It actually makes a difference but I got no idea why.
        super.onPause();
        closeCursor();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        closeCursor();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCursor();
    }
    
    private void doRefresh() {
        setProgressBarIndeterminateVisibility(true);
        
        if (Controller.getInstance().workOffline()) {
            webview.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
        } else {
            webview.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        }
        
        if (!JSONConnector.hasLastError()) {
            article = DBHelper.getInstance().getArticle(articleId);
            
            if (article != null && article.content != null) {
                
                // Populate information-bar on top of the webview if enabled
                if (Controller.getInstance().displayArticleHeader()) {
                    headerContainer.populate(article);
                } else {
                    headerContainer.setVisibility(View.GONE);
                }
                
                // Inject the specific code for attachments, <img> for images, http-link for Videos
                content = injectAttachments(getApplicationContext(), article.content, article.attachments);
                
                if (article.cachedImages)
                    content = injectCachedImages(content);
                
                // Load html from Controller and insert content
                String text = Controller.htmlHeader.replace("MARKER", content);
                
                // Use if loadDataWithBaseURL, 'cause loadData is buggy (encoding error & don't support "%" in html).
                baseUrl = StringSupport.getBaseURL(article.url);
                webview.loadDataWithBaseURL(baseUrl, text, "text/html", "utf-8", "about:blank");
                
                setTitle(article.title);
                
                if (article.isUnread && Controller.getInstance().automaticMarkRead())
                    new Updater(null, new ReadStateUpdater(article, feedId, 0)).execute();
                
                if (!linkAutoOpened && content.length() < 3) {
                    if (Controller.getInstance().openUrlEmptyArticle()) {
                        Log.i(Utils.TAG, "Article-Content is empty, opening URL in browser");
                        linkAutoOpened = true;
                        openLink();
                    }
                }
                
            }
        } else {
            openConnectionErrorDialog(JSONConnector.pullLastError());
        }
        
        setProgressBarIndeterminateVisibility(false);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARTICLE_ID, articleId);
        outState.putInt(FEED_ID, feedId);
        outState.putInt(FeedHeadlineActivity.FEED_CAT_ID, categoryId);
        outState.putBoolean(FeedHeadlineActivity.FEED_SELECT_ARTICLES, selectArticlesForCategory);
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
        if (article.isUnread) {
            read.setTitle(getString(R.string.Commons_MarkRead));
        } else {
            read.setTitle(getString(R.string.Commons_MarkUnread));
        }
        
        MenuItem publish = menu.findItem(R.id.Article_Menu_MarkStar);
        if (article.isStarred) {
            publish.setTitle(getString(R.string.Commons_MarkUnstar));
        } else {
            publish.setTitle(getString(R.string.Commons_MarkStar));
        }
        
        MenuItem star = menu.findItem(R.id.Article_Menu_MarkPublish);
        if (article.isPublished) {
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
                new Updater(null, new ReadStateUpdater(article, feedId, article.isUnread ? 0 : 1)).execute();
                return true;
            case R.id.Article_Menu_MarkStar:
                new Updater(null, new StarredStateUpdater(article, article.isStarred ? 0 : 1)).execute();
                return true;
            case R.id.Article_Menu_MarkPublish:
                new Updater(null, new PublishedStateUpdater(article, article.isPublished ? 0 : 1)).execute();
                return true;
            case R.id.Article_Menu_OpenLink:
                openLink();
                return true;
            case R.id.Article_Menu_ShareLink:
                String content = (String) getText(R.string.ArticleActivity_ShareSubject);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.putExtra(Intent.EXTRA_TEXT, content + " " + article.url);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, article.title);
                startActivity(Intent.createChooser(i, (String) getText(R.string.ArticleActivity_ShareTitle)));
                return true;
            default:
                return false;
        }
    }
    
    private void openLink() {
        if (article != null) {
            String url = article.url;
            if ((url != null) && (url.length() > 0)) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        }
    }
    
    public void onZoomChanged() {
        // Load html from Raw-Ressources and insert content
        String temp = getResources().getString(R.string.INJECT_HTML_HEAD_ZOOM);
        String text = temp.replace("MARKER", content);
        webview.loadDataWithBaseURL(baseUrl, text, "text/html", "utf-8", "about:blank");
    }
    
    private void openNextArticle(int direction) {
        
        int currentIndex = -2; // -2 so index is still -1 if direction is +1, avoids moving when no move possible
        int tempIndex = parentAdapter.getIds().indexOf(articleId);
        if (tempIndex >= 0)
            currentIndex = tempIndex;
        
        int index = currentIndex + direction;
        
        // No more articles in this direction
        if (index < 0 || index >= parentAdapter.getCount()) {
            Log.d(Utils.TAG, String.format("No more articles in this direction. (Index: %s, ID: %s)", index, articleId));
            if (Controller.getInstance().vibrateOnLastArticle())
                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(Utils.SHORT_VIBRATE);
            return;
        }
        
        Intent i = new Intent(this, ArticleActivity.class);
        i.putExtra(ARTICLE_ID, parentAdapter.getId(index));
        i.putExtra(FEED_ID, feedId);
        i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, categoryId);
        i.putExtra(FeedHeadlineActivity.FEED_SELECT_ARTICLES, selectArticlesForCategory);
        
        startActivityForResult(i, 0);
        finish();
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        super.dispatchTouchEvent(e);
        return mGestureDetector.onTouchEvent(e);
    }
    
    private OnGestureListener onGestureListener = new OnGestureListener() {
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            
            int SWIPE_BOTTOM = webview.getHeight() - Controller.swipeHeight;
            
            int dx = (int) (e2.getX() - e1.getX());
            int dy = (int) (e2.getY() - e1.getY());
            
            // don't accept the fling if it's too short as it may conflict with a button push
            boolean isSwype = false;
            if (Math.abs(dx) > Controller.swipeWidth && Math.abs(velocityX) > Math.abs(velocityY)) {
                isSwype = true;
            }
            
            if (Math.abs(dy) > (int) (Controller.absHeight * 0.2)) {
                
                return false; // Too much Y-Movement (20% of screen-height)
                
            } else if (e1.getY() < SWIPE_BOTTOM || e2.getY() < SWIPE_BOTTOM) {
                
                if (isSwype) {
                    // Display text for swipe-area
                    webviewSwipeText.setVisibility(TextView.VISIBLE);
                    new Handler().postDelayed(timerTask, 1000);
                }
                return false;
                
            } else if (!useSwipe) {
                return false;
            }
            
            if (isSwype) {
                if (velocityX > 0) {
                    openNextArticle(-1);
                } else {
                    openNextArticle(1);
                }
                return true;
            }
            return false;
        }
        
        // @formatter:off
        private Runnable timerTask = new Runnable() {
            public void run() { // Need this to set the text invisible after some time
                webviewSwipeText.setVisibility(TextView.INVISIBLE);
            }
        };
        @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { return false; }
        @Override public boolean onSingleTapUp(MotionEvent e) { return false; }
        @Override public boolean onDown(MotionEvent e) { return false; }
        @Override public void onLongPress(MotionEvent e) { }
        @Override public void onShowPress(MotionEvent e) { }
        // @formatter:on
    };
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Controller.getInstance().useVolumeKeys()) {
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
        if (Controller.getInstance().useVolumeKeys()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }
    
    private OnKeyListener keyListener = new OnKeyListener() {
        
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (Controller.getInstance().useVolumeKeys()) {
                if (keyCode == KeyEvent.KEYCODE_N) {
                    openNextArticle(-1);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_B) {
                    openNextArticle(1);
                    return true;
                }
            }
            return false;
        }
    };
    
    private void openConnectionErrorDialog(String errorMessage) {
        Intent i = new Intent(this, ErrorActivity.class);
        i.putExtra(ErrorActivity.ERROR_MESSAGE, errorMessage);
        startActivityForResult(i, ErrorActivity.ACTIVITY_SHOW_ERROR);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ErrorActivity.ACTIVITY_SHOW_ERROR)
            doRefresh();
    }
    
    private static String injectAttachments(Context context, String content, Set<String> attachments) {
        StringBuilder ret = new StringBuilder(content);
        
        for (String url : attachments) {
            if (url.length() == 0)
                continue;
            
            boolean image = false;
            for (String s : Utils.IMAGE_EXTENSIONS) {
                if (url.toLowerCase().contains("." + s))
                    image = true;
            }
            
            boolean audioOrVideo = false;
            for (String s : Utils.MEDIA_EXTENSIONS) {
                if (url.toLowerCase().contains("." + s))
                    audioOrVideo = true;
            }
            
            ret.append("<br>\n");
            if (image) {
                ret.append("<img src=\"").append(url).append("\" /><br>\n");
            } else if (audioOrVideo) {
                ret.append("<a href=\"").append(url).append("\">");
                ret.append((String) context.getText(R.string.ArticleActivity_MediaPlay)).append("</a>");
            } else {
                ret.append("<a href=\"").append(url).append("\">");
                ret.append((String) context.getText(R.string.ArticleActivity_MediaDisplayLink)).append("</a>");
            }
        }
        return ret.toString();
    }
    
    /**
     * Injects the local path to every image which could be found in the local cache, replacing the original URLs in the
     * html.
     * 
     * @param html
     *            the original html
     * @return the altered html with the URLs replaced so they point on local files if available
     */
    public static String injectCachedImages(String html) {
        if (html == null || html.length() < 40)
            return html;
        
        for (String url : ImageCacher.findAllImageUrls(html)) {
            String localUrl = ImageCacher.getCachedImageUrl(url);
            if (localUrl != null) {
                Log.d(Utils.TAG, "Replacing image: " + localUrl);
                html = html.replace(url, localUrl);
            }
        }
        return html;
    }
    
}
