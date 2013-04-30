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
import org.ttrssreader.controllers.ProgressBarManager;
import org.ttrssreader.controllers.UpdateController;
import org.ttrssreader.gui.dialogs.ArticleLabelDialog;
import org.ttrssreader.gui.interfaces.IDataChangedListener;
import org.ttrssreader.gui.interfaces.IUpdateEndListener;
import org.ttrssreader.gui.interfaces.TextInputAlertCallback;
import org.ttrssreader.gui.view.ArticleWebViewClient;
import org.ttrssreader.imageCache.ImageCacher;
import org.ttrssreader.model.FeedHeadlineAdapter;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.updaters.PublishedStateUpdater;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.StarredStateUpdater;
import org.ttrssreader.model.updaters.StateSynchronisationUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.DateUtils;
import org.ttrssreader.utils.FileUtils;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.TextSize;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.widget.Button;
import android.widget.CheckBox;
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
    private static final int CONTEXT_MENU_SHARE_URL = 1000;
    private static final int CONTEXT_MENU_SHARE_ARTICLE = 1001;
    
    // Extras
    private int articleId = -1;
    private int feedId = -1;
    private int categoryId = -1000;
    private boolean selectArticlesForCategory = false;
    private int lastMove = ARTICLE_MOVE_DEFAULT;
    
    private Article article = null;
    private String content;
    private boolean linkAutoOpened;
    private boolean markedRead = false;
    
    private FrameLayout webContainer = null;
    private WebView webView;
    private boolean webviewInitialized = false;
    private Button buttonNext;
    private Button buttonPrev;
    private GestureDetector gestureDetector;
    
    private TextView header_feed;
    private TextView header_date;
    private TextView header_time;
    private TextView header_title;
    private CheckBox header_starred;
    
    private String baseUrl = null;
    
    private FeedHeadlineAdapter parentAdapter = null;
    private int[] parentIDs = new int[2];
    private String mUrl;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        if (Controller.getInstance().displayArticleHeader())
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.articleitem);
        
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
        
        initData();
        initUI();
    }
    
    @SuppressWarnings("deprecation")
    private void initUI() {
        
        findViewById(R.id.article_header).setBackgroundColor(Color.WHITE);
        header_feed = (TextView) findViewById(R.id.head_feed);
        header_feed.setTextColor(Color.BLACK);
        header_title = (TextView) findViewById(R.id.head_title);
        header_title.setTextColor(Color.BLACK);
        header_title.setTextSize(Controller.getInstance().headlineSize());
        header_date = (TextView) findViewById(R.id.head_date);
        header_date.setTextColor(Color.BLACK);
        header_time = (TextView) findViewById(R.id.head_time);
        header_time.setTextColor(Color.BLACK);
        header_starred = (CheckBox) findViewById(R.id.head_starred);
        header_starred.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (article != null) {
                    new Updater(null, new StarredStateUpdater(article, article.isStarred ? 0 : 1)).exec();
                }
            }
        });
        
        // Wrap webview inside another FrameLayout to avoid memory leaks as described here:
        // http://stackoverflow.com/questions/3130654/memory-leak-in-webview
        webContainer = (FrameLayout) findViewById(R.id.article_webView_Container);
        buttonPrev = (Button) findViewById(R.id.article_buttonPrev);
        buttonNext = (Button) findViewById(R.id.article_buttonNext);
        buttonPrev.setOnClickListener(onButtonPressedListener);
        buttonNext.setOnClickListener(onButtonPressedListener);
        
        // Initialize the WebView if necessary
        if (webView == null) {
            webView = new WebView(getApplicationContext());
            // webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(new ArticleWebViewClient(this));
            webView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
            webView.getSettings().setSupportZoom(true);
            webView.getSettings().setBuiltInZoomControls(true);
            webView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
            webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
            webView.setScrollbarFadingEnabled(true);
            webView.setOnKeyListener(keyListener);
            gestureDetector = new GestureDetector(this, new MyGestureDetector());
            
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                webView.getSettings().setTextZoom(Controller.getInstance().textZoom());
            } else {
                // Use rough estimation of new size for old api levels:
                int size = Controller.getInstance().textZoom();
                TextSize newSize = TextSize.NORMAL;
                if (size < 50)
                    newSize = TextSize.SMALLEST;
                else if (size < 80)
                    newSize = TextSize.SMALLER;
                else if (size > 120)
                    newSize = TextSize.LARGER;
                else if (size > 150)
                    newSize = TextSize.LARGEST;
                webView.getSettings().setTextSize(newSize);
            }
            
            // prevent flicker in ics
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            
            if (Controller.getInstance().darkBackground()) {
                webView.setBackgroundColor(Color.BLACK);
                if (findViewById(R.id.container) instanceof ViewGroup)
                    setDarkBackground((ViewGroup) findViewById(R.id.container));
            }
        }
        
        registerForContextMenu(webView);
        // Attach the WebView to its placeholder
        webContainer.addView(webView);
        
        // mainContainer.populate(webView);
        findViewById(R.id.article_button_view).setVisibility(
                Controller.getInstance().useButtons() ? View.VISIBLE : View.GONE);
    }
    
    public void initUIHeader() {
        // Populate information-bar on top of the webView if enabled
        if (Controller.getInstance().displayArticleHeader()) {
            Feed feed = DBHelper.getInstance().getFeed(article.feedId);
            header_feed.setText(feed != null ? feed.title : "");
            header_title.setText(article.title);
            header_date.setText(DateUtils.getDate(this, article.updated));
            header_time.setText(DateUtils.getTime(this, article.updated));
            header_starred.setChecked(article.isStarred);
        } else {
            findViewById(R.id.article_header).setVisibility(View.GONE);
        }
    }
    
    private void initData() {
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
        if (article != null && article.isUnread) {
            article.isUnread = false;
            markedRead = true;
            new Updater(null, new ReadStateUpdater(article, feedId, 0)).exec();
        }
        
        // Reload content on next doRefresh()
        webviewInitialized = false;
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // Remove the WebView from the old placeholder
        if (webView != null)
            webContainer.removeView(webView);
        
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.articleitem);
        
        initUI();
        doRefresh();
        initUIHeader();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle instance) {
        super.onSaveInstanceState(instance);
        instance.putInt(ARTICLE_ID, articleId);
        instance.putInt(ARTICLE_FEED_ID, feedId);
        instance.putInt(FeedHeadlineActivity.FEED_CAT_ID, categoryId);
        instance.putBoolean(FeedHeadlineActivity.FEED_SELECT_ARTICLES, selectArticlesForCategory);
        instance.putInt(ARTICLE_MOVE, lastMove);
        webView.saveState(instance);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle instance) {
        super.onRestoreInstanceState(instance);
        articleId = instance.getInt(ARTICLE_ID);
        feedId = instance.getInt(ARTICLE_FEED_ID);
        categoryId = instance.getInt(FeedHeadlineActivity.FEED_CAT_ID);
        selectArticlesForCategory = instance.getBoolean(FeedHeadlineActivity.FEED_SELECT_ARTICLES);
        lastMove = instance.getInt(ARTICLE_MOVE);
        webView.restoreState(instance);
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
        ProgressBarManager.getInstance().setIndeterminateVisibility(this);
        UpdateController.getInstance().registerActivity(this);
        DBHelper.getInstance().checkAndInitializeDB(this);
        doRefresh();
    }
    
    @Override
    protected void onStop() {
        UpdateController.getInstance().unregisterActivity(this);
        // Check again to make sure it didnt get updated and marked as unread again in the background
        if (!markedRead) {
            if (article != null && article.isUnread)
                new Updater(null, new ReadStateUpdater(article, feedId, 0)).exec();
        }
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
        // Check again to make sure it didnt get updated and marked as unread again in the background
        if (!markedRead) {
            if (article != null && article.isUnread)
                new Updater(null, new ReadStateUpdater(article, feedId, 0)).exec();
        }
        super.onDestroy();
        webContainer.removeAllViews();
        webView.destroy();
    }
    
    private void doRefresh() {
        ProgressBarManager.getInstance().addProgress(this);
        
        if (Controller.getInstance().workOffline() || !Controller.getInstance().loadImages()) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
        } else {
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        }
        
        // No need to reload everything
        if (webviewInitialized) {
            ProgressBarManager.getInstance().removeProgress(this);
            return;
        }
        
        // Check for errors
        if (Controller.getInstance().getConnector().hasLastError()) {
            openConnectionErrorDialog(Controller.getInstance().getConnector().pullLastError());
            ProgressBarManager.getInstance().removeProgress(this);
            return;
        }
        
        if (article.content == null)
            return;
        
        initUIHeader();
        
        final int contentLength = article.content.length();
        
        // Inject the specific code for attachments, <img> for images, http-link for Videos
        StringBuilder sb = new StringBuilder(article.content);
        injectAttachments(getApplicationContext(), sb, article.attachments);
        
        // Do this anyway, article.cachedImages can be true if some images were fetched and others produced errors
        injectArticleLink(getApplicationContext(), sb);
        injectCachedImages(sb.toString(), articleId);
        
        if (Controller.getInstance().darkBackground()) {
            sb.insert(0, "<font color='white'>");
            sb.append("</font>");
        }
        
        // Load html from Controller and insert content
        content = Controller.htmlHeader.replace("MARKER", sb);
        
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
        ProgressBarManager.getInstance().removeProgress(this);
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
            
            if (vChild == null)
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        HitTestResult result = ((WebView) v).getHitTestResult();
        
        // Create your context menu here
        menu.setHeaderTitle("Share");
        // reset class member
        mUrl = null;
        if (result.getType() == HitTestResult.SRC_ANCHOR_TYPE) {
            mUrl = result.getExtra();
            Log.d(Utils.TAG, "Clicked on " + mUrl);
            menu.add(ContextMenu.NONE, CONTEXT_MENU_SHARE_URL, 0, "Share URL");
        }
        menu.add(ContextMenu.NONE, CONTEXT_MENU_SHARE_ARTICLE, 0, "Share article");
    }
    
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        Intent shareIntent = null;
        switch (item.getItemId()) {
            case CONTEXT_MENU_SHARE_URL:
                if (mUrl != null) {
                    shareIntent = getUrlShareIntent(mUrl);
                }
                // reset class member
                mUrl = null;
                break;
            /*
             * default behavior is to share the article URL
             */
            case CONTEXT_MENU_SHARE_ARTICLE:
            default:
                shareIntent = getUrlShareIntent(article.url);
                break;
        }
        startActivity(Intent.createChooser(shareIntent, "Share URL"));
        return super.onContextItemSelected(item);
    }
    
    private Intent getUrlShareIntent(String url) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL");
        i.putExtra(Intent.EXTRA_TEXT, url);
        return i;
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
    
    private void openNextArticle(int direction) {
        int id = direction < 0 ? parentIDs[0] : parentIDs[1];
        
        if (id < 0) {
            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(Utils.SHORT_VIBRATE);
            return;
        }
        
        this.articleId = id;
        this.lastMove = direction;
        initData();
        doRefresh();
    }
    
    private boolean doVibrate(int newIndex) {
        if (lastMove == 0)
            return false;
        if (parentAdapter.getIds().indexOf(articleId) == -1)
            return false;
        
        int index = parentAdapter.getIds().indexOf(articleId) + lastMove;
        if (index < 0 || index >= parentAdapter.getIds().size()) {
            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(Utils.SHORT_VIBRATE);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        boolean temp = gestureDetector.onTouchEvent(e);
        if (!temp)
            return super.dispatchTouchEvent(e);
        return temp;
    }
    
    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // Refresh metrics-data in Controller
            Controller.refreshDisplayMetrics(((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay());
            
            try {
                if (Math.abs(e1.getY() - e2.getY()) > Controller.relSwipeMaxOffPath)
                    return false;
                if (e1.getX() - e2.getX() > Controller.relSwipeMinDistance
                        && Math.abs(velocityX) > Controller.relSwipteThresholdVelocity) {
                    
                    // right to left swipe
                    openNextArticle(1);
                    
                } else if (e2.getX() - e1.getX() > Controller.relSwipeMinDistance
                        && Math.abs(velocityX) > Controller.relSwipteThresholdVelocity) {
                    
                    // left to right swipe
                    openNextArticle(-1);
                    
                }
            } catch (Exception e) {
            }
            return false;
        }
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
    
    private static void injectAttachments(Context context, StringBuilder content, Set<String> attachments) {
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
    }
    
    private void injectArticleLink(Context context, StringBuilder html) {
        if (!Controller.getInstance().injectArticleLink())
            return;
        
        if ((article.url != null) && (article.url.length() > 0)) {
            html.append("<br>\n");
            html.append("<a href=\"").append(article.url).append("\" rel=\"alternate\">");
            html.append((String) context.getText(R.string.ArticleActivity_ArticleLink));
            html.append("</a>");
        }
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
