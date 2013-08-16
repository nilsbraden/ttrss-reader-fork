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
import org.ttrssreader.controllers.ProgressBarManager;
import org.ttrssreader.controllers.UpdateController;
import org.ttrssreader.gui.fragments.FeedHeadlineListFragment;
import org.ttrssreader.gui.fragments.FeedListFragment;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.AsyncTask;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import com.actionbarsherlock.view.MenuItem;

public class FeedActivity extends MenuActivity {
    
    public static final String FEED_CAT_ID = "FEED_CAT_ID";
    
    private static final String FRAGMENT = "FEED_FRAGMENT";
    
    private int categoryId;
    private FeedUpdater feedUpdater = null;
    private String title = "";
    private int unreadCount = 0;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        setContentView(R.layout.feedlist);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            categoryId = extras.getInt(FEED_CAT_ID);
        } else if (instance != null) {
            categoryId = instance.getInt(FEED_CAT_ID);
        } else {
            categoryId = -1;
        }
        
        if (getSupportFragmentManager().findFragmentByTag(FRAGMENT) == null) {
            Fragment fragment = FeedListFragment.newInstance(categoryId);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.feed_list, fragment, FRAGMENT);
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
        
        doRefreshFragment(getSupportFragmentManager().findFragmentById(R.id.feed_list));
        doRefreshFragment(getSupportFragmentManager().findFragmentById(R.id.headline_list));
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
    public final boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.Menu_Refresh:
                doUpdate(true);
                return true;
            case R.id.Menu_MarkAllRead:
                new Updater(this, new ReadStateUpdater(categoryId)).exec();
                
                if (Controller.getInstance().goBackAfterMakeAllRead())
                    onBackPressed();
                
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 
     * 
     * @author n
     * 
     */
    public class FeedUpdater extends AsyncTask<Void, Integer, Void> {
        
        private int taskCount = 0;
        private static final int DEFAULT_TASK_COUNT = 2;
        boolean forceUpdate;
        
        public FeedUpdater(boolean forceUpdate) {
            this.forceUpdate = forceUpdate;
            ProgressBarManager.getInstance().addProgress(activity);
            setSupportProgressBarVisibility(true);
        }
        
        @Override
        protected Void doInBackground(Void... params) {
            UpdateController.getInstance().notifyListeners();
            
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
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values[0] == taskCount) {
                setSupportProgressBarVisibility(false);
                if (!isCacherRunning())
                    ProgressBarManager.getInstance().removeProgress(activity);
                return;
            }
            
            setProgress((10000 / (taskCount + 1)) * values[0]);
        }
        
    }
    
    @Override
    public void itemSelected(TYPE type, int selectedIndex, int oldIndex, int selectedId) {
        ListFragment secondPane = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.details);
        if (secondPane != null && secondPane.isInLayout()) {
            // Set the list item as checked
            // getListView().setItemChecked(selectedIndex, true);
            
            // Get the fragment instance
            ListFragment details = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.details);
            
            // Is the current selected ondex the same as the clicked? If so, there is no need to update
            if (details != null && selectedIndex == oldIndex)
                return;
            
            details = FeedHeadlineListFragment.newInstance(selectedId, categoryId, false);
            
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.details, details);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
            
        } else {
            // This is not a tablet - start a new activity
            Intent i = new Intent(context, FeedHeadlineActivity.class);
            i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, categoryId);
            i.putExtra(FeedHeadlineActivity.FEED_ID, selectedId);
            startActivity(i);
        }
    }
    
}
