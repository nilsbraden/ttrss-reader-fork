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

import java.util.Locale;
import java.util.Set;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.controllers.UpdateController;
import org.ttrssreader.gui.dialogs.ArticleLabelDialog;
import org.ttrssreader.gui.interfaces.IDataChangedListener;
import org.ttrssreader.gui.interfaces.IUpdateEndListener;
import org.ttrssreader.gui.interfaces.TextInputAlertCallback;
import org.ttrssreader.gui.view.ArticleHeaderView;
import org.ttrssreader.gui.view.ArticleView;
import org.ttrssreader.gui.view.ArticleWebViewClient;
import org.ttrssreader.imageCache.ImageCacher;
import org.ttrssreader.model.FeedHeadlineAdapter;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.updaters.PublishedStateUpdater;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.StarredStateUpdater;
import org.ttrssreader.model.updaters.StateSynchronisationUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.FileUtils;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class ArticleActivity extends SherlockFragmentActivity implements IUpdateEndListener, TextInputAlertCallback,
        IDataChangedListener {
    
    public static final String ARTICLE_ID = "ARTICLE_ID";
    public static final String ARTICLE_FEED_ID = "ARTICLE_FEED_ID";
    
    public static final String ARTICLE_MOVE = "ARTICLE_MOVE";
    public static final int ARTICLE_MOVE_NONE = 0;
    public static final int ARTICLE_MOVE_DEFAULT = ARTICLE_MOVE_NONE;
    
    // Extras
    private int articleId = -1;
    private int feedId = -1;
    private int categoryId = -1000;
    private boolean selectArticlesForCategory = false;
    private int lastMove = ARTICLE_MOVE_DEFAULT;
    
    private ArticleHeaderView headerContainer;
    private ArticleView mainContainer;
    
    private Article article = null;
    private String content;
    private boolean linkAutoOpened;
    private boolean markedRead = false;
    
    private FrameLayout webContainer;
    private WebView webView;
    private boolean webviewInitialized = false;
    private TextView swipeView;
    private Button buttonNext;
    private Button buttonPrev;
    private GestureDetector gestureDetector;
    
    private String baseUrl = null;
    
    private FeedHeadlineAdapter parentAdapter = null;
    private int[] parentIDs = new int[2];
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        // Log.d(Utils.TAG, "onCreate - ArticleActivity");
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        Controller.getInstance().checkAndInitializeController(this, getWindowManager().getDefaultDisplay());
        DBHelper.getInstance().checkAndInitializeDB(this);
        Data.getInstance().checkAndInitializeData(this);
        
        if (Controller.getInstance().displayArticleHeader())
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.articleitem);
        headerContainer = (ArticleHeaderView) findViewById(R.id.article_header_container);
        mainContainer = (ArticleView) findViewById(R.id.article_main_layout);
        
        // Wrap webview inside another FrameLayout to avoid memory leaks as described here:
        // http://stackoverflow.com/questions/3130654/memory-leak-in-webview
        // Layout-Files are changed due to this and the onDestroy-Method now calls container.removeAllViews() and
        // webview.destory()...
        webContainer = (FrameLayout) findViewById(R.id.webView_Container);
        webView = new WebView(getApplicationContext());
        webContainer.addView(webView);
        
        buttonPrev = (Button) findViewById(R.id.buttonPrev);
        buttonNext = (Button) findViewById(R.id.buttonNext);
        swipeView = (TextView) findViewById(R.id.swipeView);
        
        // webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.setWebViewClient(new ArticleWebViewClient(this));
        
        // Detect gestures
        gestureDetector = new GestureDetector(getApplicationContext(), onGestureListener);
        webView.setOnKeyListener(keyListener);
        
        buttonNext.setOnClickListener(onButtonPressedListener);
        buttonPrev.setOnClickListener(onButtonPressedListener);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            articleId = extras.getInt(ARTICLE_ID);
            feedId = extras.getInt(ARTICLE_FEED_ID);
            categoryId = extras.getInt(FeedHeadlineActivity.FEED_CAT_ID);
            selectArticlesForCategory = extras.getBoolean(FeedHeadlineActivity.FEED_SELECT_ARTICLES);
            lastMove = extras.getInt(ARTICLE_MOVE);
        } else if (instance != null) {
            articleId = instance.getInt(ARTICLE_ID);
            feedId = instance.getInt(ARTICLE_FEED_ID);
            categoryId = instance.getInt(FeedHeadlineActivity.FEED_CAT_ID);
            selectArticlesForCategory = instance.getBoolean(FeedHeadlineActivity.FEED_SELECT_ARTICLES);
            lastMove = instance.getInt(ARTICLE_MOVE);
        }
        
        initializeArticle();
    }
    
    private void initializeArticle() {
        Controller.getInstance().lastOpenedFeeds.add(feedId);
        Controller.getInstance().lastOpenedArticles.add(articleId);
        parentAdapter = new FeedHeadlineAdapter(getApplicationContext(), feedId, categoryId, selectArticlesForCategory);
        fillParentInformation();
        doVibrate(0);
        
        // Get article from DB
        article = DBHelper.getInstance().getArticle(articleId);
        if (article == null) {
            finish();
            return;
        }
        
        // Mark as read if necessary, do it here because in doRefresh() it will be done several times even if you set
        // it to "unread" in the meantime.
        if (article != null && article.isUnread && Controller.getInstance().automaticMarkRead()) {
            article.isUnread = false;
            new Updater(null, new ReadStateUpdater(article, feedId, 0)).exec();
            markedRead = true;
        }
        
        fillParentInformation();
        
        // Initialize mainContainer with buttons or swipe-view
        mainContainer.populate(webView);
        webviewInitialized = false;
    }
    
    private void fillParentInformation() {
        int index = parentAdapter.getIds().indexOf(articleId);
        if (index >= 0) {
            parentIDs[0] = parentAdapter.getId(index - 1); // Previous
            parentIDs[1] = parentAdapter.getId(index + 1); // Next
            
            if (parentIDs[0] == 0)
                parentIDs[0] = -1;
            if (parentIDs[1] == 0)
                parentIDs[1] = -1;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        UpdateController.getInstance().registerActivity(this);
        DBHelper.getInstance().checkAndInitializeDB(this);
        doRefresh();
    }
    
    @Override
    protected void onPause() {
        // First call super.onXXX, then do own clean-up. It actually makes a difference but I got no idea why.
        super.onPause();
        
        UpdateController.getInstance().unregisterActivity(this);
    }
    
    @Override
    protected void onStop() {
        // Check again to make sure it didnt get updated and marked as unread again in the background
        if (!markedRead) {
            if (article != null && article.isUnread && Controller.getInstance().automaticMarkRead())
                new Updater(null, new ReadStateUpdater(article, feedId, 0)).exec();
        }
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
        // Check again to make sure it didnt get updated and marked as unread again in the background
        if (!markedRead) {
            if (article != null && article.isUnread && Controller.getInstance().automaticMarkRead())
                new Updater(null, new ReadStateUpdater(article, feedId, 0)).exec();
        }
        super.onDestroy();
        webContainer.removeAllViews();
        webView.destroy();
        webView = null;
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(ARTICLE_ID, articleId);
        outState.putInt(ARTICLE_FEED_ID, feedId);
        outState.putInt(FeedHeadlineActivity.FEED_CAT_ID, categoryId);
        outState.putBoolean(FeedHeadlineActivity.FEED_SELECT_ARTICLES, selectArticlesForCategory);
        outState.putInt(ARTICLE_MOVE, lastMove);
        super.onSaveInstanceState(outState);
    }
    
    private void doRefresh() {
        if (webView == null)
            return;
        
        setProgressBarIndeterminateVisibility(true);
        
        if (Controller.getInstance().workOffline() || !Controller.getInstance().loadImages()) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
        } else {
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        }
        
        // No need to reload everything
        if (webviewInitialized) {
            setProgressBarIndeterminateVisibility(false);
            return;
        }
        
        // Check for errors
        if (Controller.getInstance().getConnector().hasLastError()) {
            openConnectionErrorDialog(Controller.getInstance().getConnector().pullLastError());
            setProgressBarIndeterminateVisibility(false);
            return;
        }
        
        if (article.content == null)
            return;
        
        // Populate information-bar on top of the webView if enabled
        if (Controller.getInstance().displayArticleHeader()) {
            headerContainer.populate(article);
        } else {
            headerContainer.setVisibility(View.GONE);
        }
        
        final int contentLength = article.content.length();
        
        // Inject the specific code for attachments, <img> for images, http-link for Videos
        StringBuilder contentTmp = injectAttachments(getApplicationContext(), new StringBuilder(article.content),
                article.attachments);
        
        // Do this anyway, article.cachedImages can be true if some images were fetched and others produced errors
        contentTmp = injectArticleLink(getApplicationContext(), contentTmp);
        content = injectCachedImages(contentTmp.toString(), articleId);
        
        // Load html from Controller and insert content
        content = Controller.htmlHeader.replace("MARKER", content);
        
        // TODO: Whole "switch background-color-thing" needs to be refactored.
        if (Controller.getInstance().darkBackground()) {
            webView.setBackgroundColor(Color.BLACK);
            content = "<font color='white'>" + content + "</font>";
            
            setDarkBackground(headerContainer);
        }
        
        // Use if loadDataWithBaseURL, 'cause loadData is buggy (encoding error & don't support "%" in html).
        baseUrl = StringSupport.getBaseURL(article.url);
        webView.loadDataWithBaseURL(baseUrl, content, "text/html", "utf-8", "about:blank");
        
        setTitle(article.title);
        
        if (!linkAutoOpened && contentLength < 3) {
            if (Controller.getInstance().openUrlEmptyArticle()) {
                Log.i(Utils.TAG, "Article-Content is empty, opening URL in browser");
                linkAutoOpened = true;
                openLink();
            }
        }
        
        // Everything did load, we dont have to do this again.
        webviewInitialized = true;
        
        setProgressBarIndeterminateVisibility(false);
    }
    
    /**
     * Recursively walks all viewGroups and their Views inside the given ViewGroup and sets the background to black and,
     * in case a TextView is found, the Text-Color to white.
     * 
     * @param v
     *            the ViewGroup to walk through
     */
    private void setDarkBackground(ViewGroup v) {
        v.setBackgroundColor(Color.BLACK);
        
        for (int i = 0; i < v.getChildCount(); i++) { // View at index 0 seems to be this view itself.
            View vChild = v.getChildAt(i);
            
            if (vChild == null || vChild.getId() == v.getId())
                continue;
            
            if (vChild instanceof TextView)
                ((TextView) vChild).setTextColor(Color.WHITE);
            
            if (vChild instanceof ViewGroup)
                setDarkBackground(((ViewGroup) vChild));
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
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
        
        MenuItem offline = menu.findItem(R.id.Article_Menu_WorkOffline);
        if (Controller.getInstance().workOffline()) {
            offline.setTitle(getString(R.string.UsageOnlineTitle));
            offline.setIcon(R.drawable.ic_menu_play_clip);
        } else {
            offline.setTitle(getString(R.string.UsageOfflineTitle));
            offline.setIcon(R.drawable.ic_menu_stop);
        }
        
        return true;
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Article_Menu_MarkRead:
                // new Updater(null, new ReadStateUpdater(article, feedId, article.isUnread ? 0 : 1)).exec();
                
                new Updater(null, new ReadStateUpdater(article, feedId, article.isUnread ? 0 : 1)).exec();
                return true;
            case R.id.Article_Menu_MarkStar:
                new Updater(null, new StarredStateUpdater(article, article.isStarred ? 0 : 1)).exec();
                return true;
            case R.id.Article_Menu_MarkPublish:
                new Updater(null, new PublishedStateUpdater(article, article.isPublished ? 0 : 1)).exec();
                return true;
            case R.id.Article_Menu_MarkPublishNote:
                new TextInputAlert(this, article).show(this);
                return true;
            case R.id.Article_Menu_AddArticleLabel:
                DialogFragment dialog = ArticleLabelDialog.newInstance(articleId);
                dialog.show(getSupportFragmentManager(), "Edit Labels");
                return true;
            case R.id.Article_Menu_WorkOffline:
                Controller.getInstance().setWorkOffline(!Controller.getInstance().workOffline());
                // Synchronize status of articles with server
                if (!Controller.getInstance().workOffline())
                    new Updater(this, new StateSynchronisationUpdater()).execute((Void[]) null);
                return true;
            case R.id.Article_Menu_OpenLink:
                openLink();
                return true;
            case R.id.Article_Menu_ShareLink:
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, article.url);
                i.putExtra(Intent.EXTRA_SUBJECT, article.title);
                startActivity(Intent.createChooser(i, (String) getText(R.string.ArticleActivity_ShareTitle)));
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Starts a new activity with the url of the current article. This should open a webbrowser in most cases. If the
     * url contains spaces or newline-characters it is first trim()'ed.
     */
    private void openLink() {
        if (article.url == null || article.url.length() == 0)
            return;
        
        String url = article.url;
        if (article.url.contains(" ") || article.url.contains("\n"))
            url = url.trim();
        
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Log.e(Utils.TAG, "Couldn't find a suitable activity for the uri: " + url);
        }
    }
    
    public void onZoomChanged() {
        if (content == null)
            content = "";
        content = Controller.htmlHeaderZoom.replace("MARKER", content);
        webView.loadDataWithBaseURL(baseUrl, content, "text/html", "utf-8", "about:blank");
    }
    
    private void openNextArticle(int direction) {
        int id = direction < 0 ? parentIDs[0] : parentIDs[1];
        
        if (id < 0) {
            if (Controller.getInstance().vibrateOnLastArticle())
                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(Utils.SHORT_VIBRATE);
            return;
        }
        
        // Intent i = new Intent(this, ArticleActivity.class);
        // i.putExtra(ARTICLE_ID, id);
        // i.putExtra(ARTICLE_FEED_ID, feedId);
        // i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, categoryId);
        // i.putExtra(FeedHeadlineActivity.FEED_SELECT_ARTICLES, selectArticlesForCategory);
        // i.putExtra(ARTICLE_MOVE, direction); // Store direction so next article can evaluate if we are running into a "wall"
        //
        // startActivityForResult(i, 0);
        // finish();
        
        this.articleId = id;
        this.lastMove = direction;
        initializeArticle();
        doRefresh();
    }
    
    private boolean doVibrate(int newIndex) {
        if (lastMove != 0) {
            if (parentAdapter.getIds().indexOf(articleId) != -1) {
                int index = parentAdapter.getIds().indexOf(articleId) + lastMove;
                
                if (index < 0 || index >= parentAdapter.getIds().size()) {
                    if (Controller.getInstance().vibrateOnLastArticle())
                        ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(Utils.SHORT_VIBRATE);
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        boolean temp = false;
        
        if (Controller.getInstance().useSwipe())
            temp = gestureDetector.onTouchEvent(e);
        
        if (!temp)
            return super.dispatchTouchEvent(e);
        
        return temp;
    }
    
    private OnGestureListener onGestureListener = new OnGestureListener() {
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!Controller.getInstance().useSwipe())
                return false;
            
            float movement = 0;
            boolean isSwipe = false;
            
            int dx = (int) (e2.getX() - e1.getX());
            int dy = (int) (e2.getY() - e1.getY());
            
            // Refresh metrics-data in Controller
            Controller.refreshDisplayMetrics(((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay());
            
            if (Controller.landscape) {
                
                // LANDSCAPE
                // Don't accept the fling if it's too short as it may conflict with a button push
                if (Math.abs(dy) > Controller.swipeHeight && Math.abs(velocityY) > Math.abs(velocityX))
                    isSwipe = true;
                
                if (Math.abs(dx) > (int) (Controller.absWidth * 0.30))
                    return false; // Too much X-Movement (30% of screen-width)
                    
                // Swipe-Area on RIGHT side of the screen
                int swipeAreaPosition = webView.getWidth() - Controller.swipeAreaWidth;
                if (e1.getX() < swipeAreaPosition || e2.getX() < swipeAreaPosition) {
                    if (isSwipe) {
                        // Display text for swipe-area
                        swipeView.setVisibility(TextView.VISIBLE);
                        new Handler().postDelayed(timerTask, Utils.SECOND);
                    }
                    return false;
                }
                
                if (isSwipe)
                    movement = velocityY;
                
            } else {
                
                // PORTRAIT
                // Don't accept the fling if it's too short as it may conflict with a button push
                if (Math.abs(dx) > Controller.swipeWidth && Math.abs(velocityX) > Math.abs(velocityY))
                    isSwipe = true;
                
                if (Math.abs(dy) > (int) (Controller.absHeight * 0.2))
                    return false; // Too much Y-Movement (20% of screen-height)
                    
                // Check if Swipe-Motion is inside the Swipe-Area
                int swipeAreaPosition = webView.getHeight() - Controller.swipeAreaHeight;
                if (e1.getY() < swipeAreaPosition || e2.getY() < swipeAreaPosition) {
                    if (isSwipe) {
                        // Display text for swipe-area
                        swipeView.setVisibility(TextView.VISIBLE);
                        new Handler().postDelayed(timerTask, Utils.SECOND);
                    }
                    return false;
                }
                
                if (isSwipe)
                    movement = velocityX;
            }
            
            if (isSwipe && movement != 0) {
                if (movement > 0)
                    openNextArticle(-1);
                else
                    openNextArticle(1);
                
                return true;
            }
            return false;
            
        }
        
        // @formatter:off
        private Runnable timerTask = new Runnable() {
            public void run() { // Need this to set the text invisible after some time
                swipeView.setVisibility(TextView.INVISIBLE);
            }
        };
        @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { return false; }
        @Override public boolean onSingleTapUp(MotionEvent e) { return false; }
        @Override public boolean onDown(MotionEvent e) { return false; }
        @Override public void onLongPress(MotionEvent e) { }
        @Override public void onShowPress(MotionEvent e) { }
        // @formatter:on
    };
    
    private OnClickListener onButtonPressedListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.equals(buttonNext))
                openNextArticle(-1);
            else if (v.equals(buttonPrev))
                openNextArticle(1);
        }
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
    
    private static StringBuilder injectAttachments(Context context, StringBuilder content, Set<String> attachments) {
        if (content == null)
            content = new StringBuilder();
        
        for (String url : attachments) {
            if (url.length() == 0)
                continue;
            
            boolean image = false;
            for (String s : FileUtils.IMAGE_EXTENSIONS) {
                if (url.toLowerCase(Locale.getDefault()).contains("." + s))
                    image = true;
            }
            
            boolean audioOrVideo = false;
            for (String s : FileUtils.AUDIO_EXTENSIONS) {
                if (url.toLowerCase(Locale.getDefault()).contains("." + s))
                    audioOrVideo = true;
            }
            for (String s : FileUtils.VIDEO_EXTENSIONS) {
                if (url.toLowerCase(Locale.getDefault()).contains("." + s))
                    audioOrVideo = true;
            }
            
            content.append("<br>\n");
            if (image) {
                content.append("<img src=\"").append(url).append("\" /><br>\n");
            } else if (audioOrVideo) {
                content.append("<a href=\"").append(url).append("\">");
                content.append((String) context.getText(R.string.ArticleActivity_MediaPlay)).append("</a>");
            } else {
                content.append("<a href=\"").append(url).append("\">");
                content.append((String) context.getText(R.string.ArticleActivity_MediaDisplayLink)).append("</a>");
            }
        }
        return content;
    }
    
    private StringBuilder injectArticleLink(Context context, StringBuilder html) {
        if (!Controller.getInstance().injectArticleLink())
            return html;
        
        if ((article.url != null) && (article.url.length() > 0)) {
            html.append("<br>\n");
            html.append("<a href=\"").append(article.url).append("\" rel=\"alternate\">");
            html.append((String) context.getText(R.string.ArticleActivity_ArticleLink));
            html.append("</a>");
        }
        return html;
    }
    
    /**
     * Injects the local path to every image which could be found in the local cache, replacing the original URLs in the
     * html.
     * 
     * @param html
     *            the original html
     * @return the altered html with the URLs replaced so they point on local files if available
     */
    private static String injectCachedImages(String html, int articleId) {
        if (html == null || html.length() < 40) // Random. Chosen by fair dice-roll.
            return html;
        
        for (String url : ImageCacher.findAllImageUrls(html, articleId)) {
            String localUrl = ImageCacher.getCachedImageUrl(url);
            if (localUrl != null) {
                html = html.replace(url, localUrl);
            }
        }
        return html;
    }
    
    @Override
    public void onUpdateEnd() { /* Not necessary here */
    }
    
    @Override
    public void onPublishNoteResult(Article a, String note) {
        new Updater(null, new PublishedStateUpdater(a, a.isPublished ? 0 : 1, note)).exec();
    }
    
    @Override
    public void dataChanged() {
        doRefresh();
    }
    
}
