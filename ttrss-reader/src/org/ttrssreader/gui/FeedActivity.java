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
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.fragments.FeedHeadlineListFragment;
import org.ttrssreader.gui.fragments.FeedListFragment;
import org.ttrssreader.gui.fragments.MainListFragment;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.Utils;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class FeedActivity extends MenuActivity {
    
    private static final String FRAGMENT = "FEED_FRAGMENT";
    
    private int categoryId;
    private FeedUpdater feedUpdater = null;
    private String title = "";
    private int unreadCount = 0;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        setContentView(R.layout.feedlist);
        super.initTabletLayout();
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            categoryId = extras.getInt(FeedListFragment.FEED_CAT_ID);
        } else if (instance != null) {
            categoryId = instance.getInt(FeedListFragment.FEED_CAT_ID);
        } else {
            categoryId = -1;
        }
        
        if (getSupportFragmentManager().findFragmentByTag(FRAGMENT) == null) {
            Fragment fragment = FeedListFragment.newInstance(categoryId);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.frame_left, fragment, FRAGMENT);
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
    }
    
    @Override
    protected void doUpdate(boolean forceUpdate) {
        // Only update if no feedUpdater already running
        if (feedUpdater != null) {
            if (feedUpdater.getStatus().equals(AsyncTask.Status.FINISHED)) {
                feedUpdater = null;
            } else {
                return;
            }
        }
        
        if (!isCacherRunning()) {
            feedUpdater = new FeedUpdater(forceUpdate);
            feedUpdater.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
                new Updater(this, new ReadStateUpdater(categoryId)).exec();
                
                if (Controller.getInstance().goBackAfterMakeAllRead())
                    onBackPressed();
                
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Updates all feeds in the selected category and all articles within these feeds.
     */
    public class FeedUpdater extends ActivityUpdater {
        private static final int DEFAULT_TASK_COUNT = 2;
        
        public FeedUpdater(boolean forceUpdate) {
            super(forceUpdate);
        }
        
        @Override
        protected Void doInBackground(Void... params) {
            Category c = DBHelper.getInstance().getCategory(categoryId);
            taskCount = DEFAULT_TASK_COUNT + (c.unread != 0 ? 1 : 0);
            
            int progress = 0;
            publishProgress(++progress); // Move progress forward
            
            Data.getInstance().updateFeeds(categoryId, false);
            publishProgress(++progress); // Move progress forward
            
            // Update articles for current category
            if (c.unread != 0)
                Data.getInstance()
                        .updateArticles(c.id, Controller.getInstance().onlyUnread(), true, false, forceUpdate);
            publishProgress(taskCount); // Move progress forward to 100%
            return null;
        }
    }
    
    @Override
    public void itemSelected(MainListFragment source, int selectedIndex, int oldIndex, int selectedId) {
        Intent i = new Intent(context, FeedHeadlineActivity.class);
        i.putExtra(FeedHeadlineListFragment.FEED_CAT_ID, categoryId);
        i.putExtra(FeedHeadlineListFragment.FEED_ID, selectedId);
        startActivity(i);
    }
    
}
