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
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.dialogs.FeedUnsubscribeDialog;
import org.ttrssreader.gui.fragments.ArticleFragment;
import org.ttrssreader.gui.fragments.FeedHeadlineListFragment;
import org.ttrssreader.gui.fragments.MainListFragment;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.Utils;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class FeedHeadlineActivity extends MenuActivity {
    
    public static final int FEED_NO_ID = 37846914;
    private static final String FRAGMENT = "HEADLINE_FRAGMENT";
    
    private int categoryId = -1000;
    private int feedId = -1000;
    private int articleId = -1000;
    private boolean selectArticlesForCategory = false;
    
    private FeedHeadlineUpdater headlineUpdater = null;
    private String title = "";
    private int unreadCount = 0;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        setContentView(R.layout.feedheadlinelist);
        super.initTabletLayout();
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            categoryId = extras.getInt(FeedHeadlineListFragment.FEED_CAT_ID);
            feedId = extras.getInt(FeedHeadlineListFragment.FEED_ID);
            articleId = extras.getInt(FeedHeadlineListFragment.ARTICLE_ID);
            selectArticlesForCategory = extras.getBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES);
        } else if (instance != null) {
            categoryId = instance.getInt(FeedHeadlineListFragment.FEED_CAT_ID);
            feedId = instance.getInt(FeedHeadlineListFragment.FEED_ID);
            articleId = instance.getInt(FeedHeadlineListFragment.ARTICLE_ID);
            selectArticlesForCategory = instance.getBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES);
        }
        
        if (getSupportFragmentManager().findFragmentByTag(FRAGMENT) == null) {
            Fragment fragment = FeedHeadlineListFragment.newInstance(feedId, categoryId, selectArticlesForCategory,
                    articleId);
            
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.frame_left, fragment, FRAGMENT);
            
            // Display article in right frame:
            if (Controller.isTablet && articleId != -1000) {
                transaction.add(R.id.frame_right, ArticleFragment.newInstance(articleId, feedId, categoryId,
                        selectArticlesForCategory, ArticleFragment.ARTICLE_MOVE_DEFAULT));
            }
            
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.commit();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        refreshAndUpdate();
    }
    
    @Override
    protected void doRefresh() {
        super.doRefresh();
        setTitle(title);
        setUnread(unreadCount);
        Utils.doRefreshFragment(getSupportFragmentManager().findFragmentById(R.id.frame_left));
        Utils.doRefreshFragment(getSupportFragmentManager().findFragmentById(R.id.frame_right));
    }
    
    @Override
    protected void doUpdate(boolean forceUpdate) {
        // Only update if no headlineUpdater already running
        if (headlineUpdater != null) {
            if (headlineUpdater.getStatus().equals(AsyncTask.Status.FINISHED)) {
                headlineUpdater = null;
            } else {
                return;
            }
        }
        
        if (!isCacherRunning()) {
            headlineUpdater = new FeedHeadlineUpdater(forceUpdate);
            headlineUpdater.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean ret = super.onPrepareOptionsMenu(menu);
        menu.removeItem(R.id.Menu_MarkAllRead);
        menu.removeItem(R.id.Menu_MarkFeedsRead);
        return ret;
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.Menu_Refresh:
                doUpdate(true);
                return true;
            case R.id.Menu_MarkFeedRead:
                if (selectArticlesForCategory) {
                    new Updater(this, new ReadStateUpdater(categoryId)).exec();
                } else {
                    new Updater(this, new ReadStateUpdater(feedId, 42)).exec();
                }
                
                if (Controller.getInstance().goBackAfterMakeAllRead())
                    onBackPressed();
                
                return true;
            case R.id.Menu_FeedUnsubscribe:
                FeedUnsubscribeDialog.getInstance(this, feedId).show(getSupportFragmentManager(),
                        FeedUnsubscribeDialog.DIALOG_UNSUBSCRIBE);
            default:
                return false;
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Controller.getInstance().useVolumeKeys()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_N) {
                fragmentOpenNextFeed(-1);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_B) {
                fragmentOpenNextFeed(1);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void fragmentOpenNextFeed(int direction) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame_left);
        if (fragment instanceof FeedHeadlineListFragment)
            ((FeedHeadlineListFragment) fragment).openNextFeed(direction);
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (Controller.getInstance().useVolumeKeys()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                    || keyCode == KeyEvent.KEYCODE_N || keyCode == KeyEvent.KEYCODE_B) {
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }
    
    /**
     * Updates all articles from the selected feed.
     */
    public class FeedHeadlineUpdater extends ActivityUpdater {
        private static final int DEFAULT_TASK_COUNT = 2;
        
        public FeedHeadlineUpdater(boolean forceUpdate) {
            super(forceUpdate);
        }
        
        @Override
        protected Void doInBackground(Void... params) {
            taskCount = DEFAULT_TASK_COUNT;
            
            int progress = 0;
            boolean displayUnread = Controller.getInstance().onlyUnread();
            
            publishProgress(++progress); // Move progress forward
            if (selectArticlesForCategory) {
                Data.getInstance().updateArticles(categoryId, displayUnread, true, false, forceUpdate);
            } else {
                Data.getInstance().updateArticles(feedId, displayUnread, false, false, forceUpdate);
            }
            publishProgress(taskCount); // Move progress forward to 100%
            return null;
        }
    }
    
    @Override
    public void itemSelected(MainListFragment source, int selectedIndex, int oldIndex, int selectedId) {
        Log.d(Utils.TAG, "itemSelected in FeedHeadlineActivity");
        
        if (Controller.isTablet) {
            
            switch (source.getType()) {
                case FEEDHEADLINE:
                    //
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.frame_right, ArticleFragment.newInstance(selectedId, feedId, categoryId,
                            selectArticlesForCategory, ArticleFragment.ARTICLE_MOVE_DEFAULT));
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                    ft.commit();
                    
                    break;
                default:
                    Toast.makeText(this, "Invalid request!", Toast.LENGTH_SHORT).show();
                    break;
            }
            return;
        }
        
        // Non-Tablet behaviour:
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.frame_left, ArticleFragment.newInstance(selectedId, feedId, categoryId,
                selectArticlesForCategory, ArticleFragment.ARTICLE_MOVE_DEFAULT));
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }
    
}
