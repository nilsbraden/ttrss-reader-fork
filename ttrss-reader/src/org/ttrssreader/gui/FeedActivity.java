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
import org.ttrssreader.gui.fragments.FeedHeadlineListFragment;
import org.ttrssreader.model.FeedAdapter;
import org.ttrssreader.model.MainAdapter;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.Utils;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
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
        super.onResume();
        DBHelper.getInstance().checkAndInitializeDB(this);
        doRefresh();
        doUpdate();
    }
    
    private void closeCursor() {
        if (adapter != null)
            adapter.closeCursor();
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
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(FEED_CAT_ID, categoryId);
        outState.putString(FEED_CAT_TITLE, categoryTitle);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void doRefresh() {
        int unreadCount = DBHelper.getInstance().getUnreadCount(categoryId, true);
        setTitle(MainAdapter.formatTitle(categoryTitle, unreadCount));
        
        if (adapter != null) {
            adapter.makeQuery(true);
            adapter.notifyDataSetChanged();
        }
        
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
            if (Controller.getInstance().isExecuteOnExecutorAvailable())
                feedUpdater.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                feedUpdater.execute();
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == MARK_READ) {
            new Updater(this, new ReadStateUpdater(adapter.getId(cmi.position), 42)).exec();
            return true;
        }
        return false;
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        boolean ret = super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case R.id.Menu_Refresh:
                Data.getInstance().resetTime(categoryId, false, true, false);
                doUpdate();
                return true;
            case R.id.Menu_MarkAllRead:
                new Updater(this, new ReadStateUpdater(categoryId)).exec();
                return true;
        }
        
        if (ret) {
            doRefresh();
        }
        return true;
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
                doRefresh();
                return;
            }
            
            setProgress((10000 / (taskCount + 1)) * values[0]);
            doRefresh();
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
        
        // Find out if we are using a wide screen
        ListFragment secondPane = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.details);
        
        if (secondPane != null && secondPane.isInLayout()) {
            
            Log.d(Utils.TAG, "Filling right pane... (" + selectedIndex + " " + oldIndex + ")");
            
            // Set the list item as checked
            // getListView().setItemChecked(selectedIndex, true);
            
            // Get the fragment instance
            ListFragment details = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.details);
            
            // Is the current selected ondex the same as the clicked? If so, there is no need to update
            if (details != null && selectedIndex == oldIndex)
                return;
            
            details = FeedHeadlineListFragment.newInstance(adapter.getId(selectedIndex),
                    adapter.getTitle(selectedIndex), categoryId, false);
            
            // Replace the old fragment with the new one
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.details, details);
            // Use a fade animation. This makes it clear that this is not a new "layer"
            // above the current, but a replacement
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
            
        } else {
            
            // This is not a tablet - start a new activity
            Intent i = new Intent(context, FeedHeadlineActivity.class);
            i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, categoryId);
            i.putExtra(FeedHeadlineActivity.FEED_ID, adapter.getId(selectedIndex));
            i.putExtra(FeedHeadlineActivity.FEED_TITLE, adapter.getTitle(selectedIndex));
            if (i != null)
                startActivity(i);
            
        }
    }
    
}
