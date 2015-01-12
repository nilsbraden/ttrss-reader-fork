/*
 * Copyright (c) 2015, Nils Braden
 *
 * This file is part of ttrss-reader-fork. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; If
 * not, see http://www.gnu.org/licenses/.
 */

package org.ttrssreader.gui;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.fragments.ArticleFragment;
import org.ttrssreader.gui.fragments.FeedHeadlineListFragment;
import org.ttrssreader.gui.fragments.MainListFragment;
import org.ttrssreader.utils.AsyncTask;

public class FeedHeadlineActivity extends MenuActivity {
    
    @SuppressWarnings("unused")
    private static final String TAG = FeedHeadlineActivity.class.getSimpleName();
    
    public static final int FEED_NO_ID = 37846914;
    
    private int categoryId = Integer.MIN_VALUE;
    private int feedId = Integer.MIN_VALUE;
    private boolean selectArticlesForCategory = false;
    
    private FeedHeadlineUpdater headlineUpdater = null;
    
    private static final String SELECTED = "SELECTED";
    private int selectedArticleId = Integer.MIN_VALUE;
    
    private FeedHeadlineListFragment headlineFragment;
    private ArticleFragment articleFragment;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        setContentView(R.layout.main);
        super.initTabletLayout();
        
        Bundle extras = getIntent().getExtras();
        if (instance != null) {
            categoryId = instance.getInt(FeedHeadlineListFragment.FEED_CAT_ID);
            feedId = instance.getInt(FeedHeadlineListFragment.FEED_ID);
            selectArticlesForCategory = instance.getBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES);
            selectedArticleId = instance.getInt(SELECTED, Integer.MIN_VALUE);
        } else if (extras != null) {
            categoryId = extras.getInt(FeedHeadlineListFragment.FEED_CAT_ID);
            feedId = extras.getInt(FeedHeadlineListFragment.FEED_ID);
            selectArticlesForCategory = extras.getBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES);
            selectedArticleId = extras.getInt(SELECTED, Integer.MIN_VALUE);
        }
        
        FragmentManager fm = getFragmentManager();
        headlineFragment = (FeedHeadlineListFragment) fm.findFragmentByTag(FeedHeadlineListFragment.FRAGMENT);
        articleFragment = (ArticleFragment) fm.findFragmentByTag(ArticleFragment.FRAGMENT);
        
        if (headlineFragment == null) {
            headlineFragment = FeedHeadlineListFragment.newInstance(feedId, categoryId, selectArticlesForCategory,
                    selectedArticleId);
            
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.frame_main, headlineFragment, FeedHeadlineListFragment.FRAGMENT);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(FeedHeadlineListFragment.FEED_CAT_ID, categoryId);
        outState.putInt(FeedHeadlineListFragment.FEED_ID, headlineFragment.getFeedId());
        outState.putBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES, selectArticlesForCategory);
        outState.putInt(SELECTED, selectedArticleId);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle instance) {
        categoryId = instance.getInt(FeedHeadlineListFragment.FEED_CAT_ID);
        feedId = instance.getInt(FeedHeadlineListFragment.FEED_ID);
        selectArticlesForCategory = instance.getBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES);
        selectedArticleId = instance.getInt(SELECTED, Integer.MIN_VALUE);
        super.onRestoreInstanceState(instance);
    }
    
    @Override
    public void dataLoadingFinished() {
        setTitleAndUnread();
    }
    
    @Override
    protected void doRefresh() {
        super.doRefresh();
        headlineFragment.doRefresh();
        
        setTitleAndUnread();
    }
    
    private void setTitleAndUnread() {
        // Title and unread information:
        if (headlineFragment != null) {
            setTitle(headlineFragment.getTitle());
            setUnread(headlineFragment.getUnread());
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        if (selectedArticleId != Integer.MIN_VALUE) {
            getMenuInflater().inflate(R.menu.article, menu);
            if (Controller.isTablet)
                super.onCreateOptionsMenu(menu);
        } else {
            super.onCreateOptionsMenu(menu);
        }
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.removeItem(R.id.Menu_MarkAllRead);
        menu.removeItem(R.id.Menu_MarkFeedsRead);
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        if (super.onOptionsItemSelected(item))
            return true;
        
        switch (item.getItemId()) {
            case R.id.Menu_Refresh: {
                doUpdate(true);
                return true;
            }
            default:
                return false;
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Controller.getInstance().useVolumeKeys()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_N) {
                openNextFragment(-1);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_B) {
                openNextFragment(1);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
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
    
    private void openNextFragment(int direction) {
        if (selectedArticleId != Integer.MIN_VALUE) {
            openNextArticle(direction);
        } else {
            openNextFeed(direction);
        }
    }
    
    public void openNextArticle(int direction) {
        // Open next article
        FragmentManager fm = getFragmentManager();
        articleFragment = (ArticleFragment) fm.findFragmentByTag(ArticleFragment.FRAGMENT);
        if (articleFragment instanceof ArticleFragment) {
            selectedArticleId = ((ArticleFragment) articleFragment).openNextArticle(direction);
            headlineFragment.setSelectedId(selectedArticleId);
        }
    }
    
    public void openNextFeed(int direction) {
        // Open next Feed
        headlineFragment.openNextFeed(direction);
        feedId = headlineFragment.getFeedId();
        
        FragmentManager fm = getFragmentManager();
        articleFragment = (ArticleFragment) fm.findFragmentByTag(ArticleFragment.FRAGMENT);
        if (articleFragment instanceof ArticleFragment)
            articleFragment.resetParentInformation();
    }
    
    /**
     * Updates all articles from the selected feed.
     */
    private class FeedHeadlineUpdater extends ActivityUpdater {
        private static final int DEFAULT_TASK_COUNT = 2;
        
        private FeedHeadlineUpdater(boolean forceUpdate) {
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
                Data.getInstance().updateArticles(headlineFragment.getFeedId(), displayUnread, false, false,
                        forceUpdate);
            }
            Data.getInstance().calculateCounters();
            Data.getInstance().notifyListeners();
            publishProgress(taskCount); // Move progress forward to 100%
            return null;
        }
    }
    
    @Override
    public void itemSelected(MainListFragment source, int selectedIndex, int selectedId) {
        switch (source.getType()) {
            case FEEDHEADLINE:
                displayArticle(selectedId);
                break;
            default:
                Toast.makeText(this, "Invalid request!", Toast.LENGTH_SHORT).show();
                break;
        }
    }
    
    private void displayArticle(int articleId) {
        hideArticleFragment();
        
        selectedArticleId = articleId;
        headlineFragment.setSelectedId(selectedArticleId);
        
        // Clear back stack
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        
        if (articleFragment == null) {
            articleFragment = ArticleFragment.newInstance(articleId, headlineFragment.getFeedId(), categoryId,
                    selectArticlesForCategory, ArticleFragment.ARTICLE_MOVE_DEFAULT);
            
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.frame_sub, articleFragment, ArticleFragment.FRAGMENT);
            
            // Animation
            if (Controller.isTablet)
                ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.fade_out, android.R.anim.fade_in,
                        R.anim.slide_out_left);
            else
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            
            if (!Controller.isTablet)
                ft.addToBackStack(null);
            
            ft.commit();
        } else {
            // Reuse existing ArticleFragment
            articleFragment.openArticle(articleId, headlineFragment.getFeedId(), categoryId, selectArticlesForCategory,
                    ArticleFragment.ARTICLE_MOVE_DEFAULT);
        }
    }
    
    private void hideArticleFragment() {
        if (articleFragment == null)
            return;
        
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.remove(articleFragment);
        ft.commit();
        
        articleFragment = null;
    }
    
    @Override
    public void onBackPressed() {
        selectedArticleId = Integer.MIN_VALUE;
        super.onBackPressed();
    }
    
    public int getCategoryId() {
        return categoryId;
    }
    
    public int getFeedId() {
        return feedId;
    }
    
}
