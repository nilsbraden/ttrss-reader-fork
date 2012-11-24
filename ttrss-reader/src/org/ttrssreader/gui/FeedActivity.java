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
import org.ttrssreader.controllers.NotInitializedException;
import org.ttrssreader.controllers.UpdateController;
import org.ttrssreader.model.FeedAdapter;
import org.ttrssreader.model.MainAdapter;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.Utils;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class FeedActivity extends MenuActivity {
    
    public static final String FEED_CAT_ID = "FEED_CAT_ID";
    public static final String FEED_CAT_TITLE = "FEED_CAT_TITLE";
    
    // Extras
    private int categoryId;
    private String categoryTitle;
    
    private FeedAdapter adapter = null; // Remember to explicitly check every access to adapter for it beeing null!
    private FeedUpdater feedUpdater = null;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        // Log.d(Utils.TAG, "onCreate - FeedActivity");
        setContentView(R.layout.feedlist);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            categoryId = extras.getInt(FEED_CAT_ID);
            categoryTitle = extras.getString(FEED_CAT_TITLE);
        } else if (instance != null) {
            categoryId = instance.getInt(FEED_CAT_ID);
            categoryTitle = instance.getString(FEED_CAT_TITLE);
        } else {
            categoryId = -1;
            categoryTitle = null;
        }
    }
    
    @Override
    protected void onResume() {
        if (adapter != null)
            adapter.makeQuery(true);
        
        super.onResume();
        
        UpdateController.getInstance().registerActivity(this);
        refreshAndUpdate();
    }
    
    private void closeCursor() {
        if (adapter != null)
            adapter.closeCursor();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        UpdateController.getInstance().unregisterActivity(this);
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
    
    // @Override
    // protected void onSaveInstanceState(Bundle outState) {
    // outState.putInt(FEED_CAT_ID, categoryId);
    // outState.putString(FEED_CAT_TITLE, categoryTitle);
    // super.onSaveInstanceState(outState);
    // }
    
    @Override
    protected void doRefresh() {
        int unreadCount = DBHelper.getInstance().getUnreadCount(categoryId, true);
        setTitle(MainAdapter.formatTitle(categoryTitle, unreadCount));
        
        if (adapter != null)
            adapter.refreshQuery();
        
        try {
            if (Controller.getInstance().getConnector().hasLastError())
                openConnectionErrorDialog(Controller.getInstance().getConnector().pullLastError());
        } catch (NotInitializedException e) {
        }
        
        if (feedUpdater == null) {
            setProgressBarIndeterminateVisibility(false);
            setProgressBarVisibility(false);
        }
    }
    
    @Override
    protected void doUpdate() {
        // Only update if no feedUpdater already running
        if (feedUpdater != null) {
            if (feedUpdater.getStatus().equals(AsyncTask.Status.FINISHED)) {
                feedUpdater = null;
            } else {
                return;
            }
        }
        
        if (!isCacherRunning()) {
            setProgressBarIndeterminateVisibility(true);
            setProgressBarVisibility(false);
            
            feedUpdater = new FeedUpdater();
            feedUpdater.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
    
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == MARK_READ) {
            new Updater(this, new ReadStateUpdater(adapter.getId(cmi.position), 42)).exec();
            return true;
        }
        return false;
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.Menu_Refresh:
                Data.getInstance().resetTime(categoryId, Data.TIME_FEED);
                doUpdate();
                return true;
            case R.id.Menu_MarkAllRead:
                new Updater(this, new ReadStateUpdater(categoryId)).exec();
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
                Data.getInstance().updateArticles(c.id, Controller.getInstance().onlyUnread(), true, false);
            
            publishProgress(taskCount); // Move progress forward to 100%
            return null;
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values[0] == taskCount) {
                setProgressBarIndeterminateVisibility(false);
                setProgressBarVisibility(false);
                return;
            }
            
            setProgress((10000 / (taskCount + 1)) * values[0]);
        }
        
    }
    
    @Override
    public void setAdapter(MainAdapter adapter) {
        if (adapter instanceof FeedAdapter)
            this.adapter = (FeedAdapter) adapter;
    }
    
    @Override
    public void itemSelected(TYPE type, int selectedIndex, int oldIndex) {
        // Log.d(Utils.TAG, this.getClass().getName() + " - itemSelected called. Type: " + type);
        if (adapter == null) {
            Log.w(Utils.TAG, "FeedActivity: Adapter shouldn't be null here...");
            return;
        }
        
        // This is not a tablet - start a new activity
        Intent i = new Intent(context, FeedHeadlineActivity.class);
        i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, categoryId);
        i.putExtra(FeedHeadlineActivity.FEED_ID, adapter.getId(selectedIndex));
        i.putExtra(FeedHeadlineActivity.FEED_TITLE, adapter.getTitle(selectedIndex));
        if (i != null)
            startActivity(i);
    }
    
    @Override
    protected void onDataChanged() {
        doRefresh();
    }
    
}
