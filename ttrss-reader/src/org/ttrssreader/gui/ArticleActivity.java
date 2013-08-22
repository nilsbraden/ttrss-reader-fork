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

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.ProgressBarManager;
import org.ttrssreader.controllers.UpdateController;
import org.ttrssreader.gui.dialogs.ArticleLabelDialog;
import org.ttrssreader.gui.fragments.ArticleFragment;
import org.ttrssreader.gui.interfaces.IDataChangedListener;
import org.ttrssreader.gui.interfaces.IUpdateEndListener;
import org.ttrssreader.gui.interfaces.TextInputAlertCallback;
import org.ttrssreader.gui.view.MyGestureDetector;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.updaters.PublishedStateUpdater;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.StarredStateUpdater;
import org.ttrssreader.model.updaters.StateSynchronisationUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class ArticleActivity extends SherlockFragmentActivity implements IUpdateEndListener, TextInputAlertCallback,
        IDataChangedListener {
    
    // public static final String ARTICLE_ID = "ARTICLE_ID";
    // public static final String ARTICLE_FEED_ID = "ARTICLE_FEED_ID";
    //
    // public static final String ARTICLE_MOVE = "ARTICLE_MOVE";
    public static final int ARTICLE_MOVE_NONE = 0;
    public static final int ARTICLE_MOVE_DEFAULT = ARTICLE_MOVE_NONE;
    // private static final int CONTEXT_MENU_SHARE_URL = 1000;
    // private static final int CONTEXT_MENU_SHARE_ARTICLE = 1001;
    // private static final int CONTEXT_MENU_DISPLAY_CAPTION = 1002;
    //
    // private final static char TEMPLATE_DELIMITER_START = '$';
    // private final static char TEMPLATE_DELIMITER_END = '$';
    //
    // private static final String TEMPLATE_ARTICLE_VAR = "article";
    // private static final String TEMPLATE_FEED_VAR = "feed";
    // private static final String MARKER_UPDATED = "UPDATED";
    // private static final String MARKER_LABELS = "LABELS";
    // private static final String MARKER_CONTENT = "CONTENT";
    // private static final String MARKER_ATTACHMENTS = "ATTACHMENTS";
    
    // Extras
    private int articleId = -1;
    private int feedId = -1;
    // private int categoryId = -1000;
    // private boolean selectArticlesForCategory = false;
    // private int lastMove = ARTICLE_MOVE_DEFAULT;
    
    private Article article = null;
    // private Feed feed = null;
    // private String content;
    // private boolean linkAutoOpened;
    // private boolean markedRead = false;
    //
    private ActionBar actionBar = null;
    
    // private FrameLayout webContainer = null;
    // private MyWebView webView;
    // private boolean webviewInitialized = false;
    // private Button buttonNext;
    // private Button buttonPrev;
    private GestureDetector gestureDetector;
    
    // private FeedHeadlineAdapter parentAdapter = null;
    // private int[] parentIDs = new int[2];
    // private String mSelectedExtra;
    // private String mSelectedAltText;
    //
    // private ArticleJSInterface articleJSInterface;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        setContentView(R.layout.articleitem);
        
        // Bundle extras = getIntent().getExtras();
        // if (extras != null) {
        // articleId = extras.getInt(ARTICLE_ID);
        // feedId = extras.getInt(ARTICLE_FEED_ID);
        // categoryId = extras.getInt(FeedHeadlineActivity.FEED_CAT_ID);
        // selectArticlesForCategory = extras.getBoolean(FeedHeadlineActivity.FEED_SELECT_ARTICLES);
        // lastMove = extras.getInt(ARTICLE_MOVE);
        // } else if (instance != null) {
        // articleId = instance.getInt(ARTICLE_ID);
        // feedId = instance.getInt(ARTICLE_FEED_ID);
        // categoryId = instance.getInt(FeedHeadlineActivity.FEED_CAT_ID);
        // selectArticlesForCategory = instance.getBoolean(FeedHeadlineActivity.FEED_SELECT_ARTICLES);
        // lastMove = instance.getInt(ARTICLE_MOVE);
        // }
        
        initActionbar();
        
        gestureDetector = new GestureDetector(this, new ArticleGestureDetector(getSupportActionBar()));
    }
    
    /**
     * Initialize ActionBar, just hide it if it already exists.
     */
    private void initActionbar() {
        if (actionBar == null) {
            actionBar = getSupportActionBar();
            actionBar.setDisplayOptions(ActionBar.DISPLAY_USE_LOGO);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        actionBar.hide();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        ProgressBarManager.getInstance().setIndeterminateVisibility(this);
        UpdateController.getInstance().registerActivity(this);
        DBHelper.getInstance().checkAndInitializeDB(this);
        doRefresh();
    }
    
    private void doRefresh() {
        Utils.doRefreshFragment(getSupportFragmentManager().findFragmentById(R.id.articleView));
    }
    
    @Override
    public void onStop() {
        UpdateController.getInstance().unregisterActivity(this);
        super.onStop();
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
        
        if (article != null) {
            MenuItem read = menu.findItem(R.id.Article_Menu_MarkRead);
            if (article.isUnread) {
                read.setTitle(getString(R.string.Commons_MarkRead));
                read.setIcon(R.drawable.ic_menu_clear_playlist);
            } else {
                read.setTitle(getString(R.string.Commons_MarkUnread));
                read.setIcon(R.drawable.ic_menu_mark);
            }
            
            MenuItem publish = menu.findItem(R.id.Article_Menu_MarkPublish);
            if (article.isPublished) {
                publish.setTitle(getString(R.string.Commons_MarkUnpublish));
                publish.setIcon(R.drawable.menu_published);
            } else {
                publish.setTitle(getString(R.string.Commons_MarkPublish));
                publish.setIcon(R.drawable.menu_publish);
            }
            
            MenuItem star = menu.findItem(R.id.Article_Menu_MarkStar);
            if (article.isStarred) {
                star.setTitle(getString(R.string.Commons_MarkUnstar));
                star.setIcon(R.drawable.menu_starred);
            } else {
                star.setTitle(getString(R.string.Commons_MarkStar));
                star.setIcon(R.drawable.ic_menu_star);
            }
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
                invalidateOptionsMenu();
                return true;
            case R.id.Article_Menu_MarkStar:
                new Updater(null, new StarredStateUpdater(article, article.isStarred ? 0 : 1)).exec();
                invalidateOptionsMenu();
                return true;
            case R.id.Article_Menu_MarkPublish:
                new Updater(null, new PublishedStateUpdater(article, article.isPublished ? 0 : 1)).exec();
                invalidateOptionsMenu();
                return true;
            case R.id.Article_Menu_MarkPublishNote:
                new TextInputAlert(this, article).show(this);
                invalidateOptionsMenu();
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
                invalidateOptionsMenu();
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
    
    @Override
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
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (Controller.getInstance().useVolumeKeys()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        boolean temp = gestureDetector.onTouchEvent(e);
        if (!temp)
            return super.dispatchTouchEvent(e);
        return temp;
    }
    
    class ArticleGestureDetector extends MyGestureDetector {
        public ArticleGestureDetector(ActionBar actionBar) {
            super(actionBar);
        }
        
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
    
    private void openNextArticle(int direction) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.articleView);
        if (fragment instanceof ArticleFragment) {
            ArticleFragment aFrag = (ArticleFragment) fragment;
            aFrag.openNextArticle(direction);
        }
    }
    
    private void openLink() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.articleView);
        if (fragment instanceof ArticleFragment) {
            ArticleFragment aFrag = (ArticleFragment) fragment;
            aFrag.openLink();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ErrorActivity.ACTIVITY_SHOW_ERROR)
            doRefresh();
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
