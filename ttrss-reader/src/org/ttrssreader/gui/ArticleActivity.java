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
import org.ttrssreader.gui.fragments.FeedHeadlineListFragment;
import org.ttrssreader.gui.interfaces.IDataChangedListener;
import org.ttrssreader.gui.interfaces.IUpdateEndListener;
import org.ttrssreader.gui.interfaces.TextInputAlertCallback;
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
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class ArticleActivity extends SherlockFragmentActivity implements IUpdateEndListener, TextInputAlertCallback,
        IDataChangedListener {
    
    private static final String FRAGMENT = "ARTICLE_FRAGMENT";
    public static final int ARTICLE_MOVE_NONE = 0;
    public static final int ARTICLE_MOVE_DEFAULT = ARTICLE_MOVE_NONE;
    
    private ActionBar actionBar = null;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        setContentView(R.layout.articleitem);
        
        int articleId = -1;
        int feedId = -1;
        int categoryId = -1000;
        boolean selectForCategory = false;
        int lastMove = ARTICLE_MOVE_DEFAULT;
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            articleId = extras.getInt(ArticleFragment.ARTICLE_ID);
            feedId = extras.getInt(ArticleFragment.ARTICLE_FEED_ID);
            categoryId = extras.getInt(FeedHeadlineListFragment.FEED_CAT_ID);
            selectForCategory = extras.getBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES);
            lastMove = extras.getInt(ArticleFragment.ARTICLE_MOVE);
        } else if (instance != null) {
            articleId = instance.getInt(ArticleFragment.ARTICLE_ID);
            feedId = instance.getInt(ArticleFragment.ARTICLE_FEED_ID);
            categoryId = instance.getInt(FeedHeadlineListFragment.FEED_CAT_ID);
            selectForCategory = instance.getBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES);
            lastMove = instance.getInt(ArticleFragment.ARTICLE_MOVE);
        }
        
        if (getSupportFragmentManager().findFragmentByTag(FRAGMENT) == null) {
            Fragment fragment = ArticleFragment.newInstance(articleId, feedId, categoryId, selectForCategory, lastMove);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.article_view, fragment, FRAGMENT);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.commit();
        }
        
        initActionbar();
    }
    
    /**
     * Initialize ActionBar.
     */
    private void initActionbar() {
        if (actionBar == null) {
            actionBar = getSupportActionBar();
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setDisplayHomeAsUpEnabled(true);
            // actionBar.setDisplayShowCustomEnabled(true);
            // actionBar.setDisplayShowTitleEnabled(true);
        }
        
        // action bar should be invisible when hideActionbar() is activated?
        if (actionBar.isShowing() && Controller.getInstance().hideActionbar()) {
            actionBar.hide();
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
    
    private void doRefresh() {
        Utils.doRefreshFragment(getSupportFragmentManager().findFragmentById(R.id.article_view));
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
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        prepareArticleMenu(this, menu, getArticle());
        
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
    
    public static void prepareArticleMenu(final Context context, final Menu menu, final Article article) {
        if (article != null) {
            MenuItem read = menu.findItem(R.id.Article_Menu_MarkRead);
            if (article.isUnread) {
                read.setTitle(context.getString(R.string.Commons_MarkRead));
                read.setIcon(R.drawable.ic_menu_clear_playlist);
            } else {
                read.setTitle(context.getString(R.string.Commons_MarkUnread));
                read.setIcon(R.drawable.ic_menu_mark);
            }
            
            MenuItem publish = menu.findItem(R.id.Article_Menu_MarkPublish);
            if (article.isPublished) {
                publish.setTitle(context.getString(R.string.Commons_MarkUnpublish));
                publish.setIcon(R.drawable.menu_published);
            } else {
                publish.setTitle(context.getString(R.string.Commons_MarkPublish));
                publish.setIcon(R.drawable.menu_publish);
            }
            
            MenuItem star = menu.findItem(R.id.Article_Menu_MarkStar);
            if (article.isStarred) {
                star.setTitle(context.getString(R.string.Commons_MarkUnstar));
                star.setIcon(R.drawable.menu_starred);
            } else {
                star.setTitle(context.getString(R.string.Commons_MarkStar));
                star.setIcon(R.drawable.ic_menu_star);
            }
        }
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        Article article = getArticle();
        switch (item.getItemId()) {
            case R.id.Article_Menu_MarkRead:
                new Updater(this, new ReadStateUpdater(article, article.feedId, article.isUnread ? 0 : 1)).exec();
                return true;
            case R.id.Article_Menu_MarkStar:
                new Updater(this, new StarredStateUpdater(article, article.isStarred ? 0 : 1)).exec();
                return true;
            case R.id.Article_Menu_MarkPublish:
                new Updater(this, new PublishedStateUpdater(article, article.isPublished ? 0 : 1)).exec();
                return true;
            case R.id.Article_Menu_MarkPublishNote:
                new TextInputAlert(this, article).show(this);
                return true;
            case R.id.Article_Menu_AddArticleLabel:
                DialogFragment dialog = ArticleLabelDialog.newInstance(article.id);
                dialog.show(getSupportFragmentManager(), "Edit Labels");
                return true;
            case R.id.Article_Menu_WorkOffline:
                Controller.getInstance().setWorkOffline(!Controller.getInstance().workOffline());
                // Synchronize status of articles with server
                if (!Controller.getInstance().workOffline())
                    new Updater(this, new StateSynchronisationUpdater()).execute((Void[]) null);
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
    
    private Article getArticle() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.article_view);
        if (fragment instanceof ArticleFragment) {
            ArticleFragment aFrag = (ArticleFragment) fragment;
            return aFrag.getArticle();
        }
        return null;
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
    
    private void openNextArticle(int direction) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.article_view);
        if (fragment instanceof ArticleFragment) {
            ArticleFragment aFrag = (ArticleFragment) fragment;
            aFrag.openNextArticle(direction);
            initActionbar();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ErrorActivity.ACTIVITY_SHOW_ERROR)
            doRefresh();
    }
    
    @Override
    public void onUpdateEnd() {
        invalidateOptionsMenu();
    }
    
    @Override
    public void onPublishNoteResult(Article a, String note) {
        new Updater(this, new PublishedStateUpdater(a, a.isPublished ? 0 : 1, note)).exec();
    }
    
    @Override
    public void dataChanged() {
        doRefresh();
    }
    
}
