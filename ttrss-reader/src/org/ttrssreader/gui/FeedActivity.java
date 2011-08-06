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
import org.ttrssreader.model.FeedAdapter;
import org.ttrssreader.model.MainAdapter;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

public class FeedActivity extends MenuActivity {
    
    public static final String CATEGORY_ID = "CATEGORY_ID";
    public static final String CATEGORY_TITLE = "CATEGORY_TITLE";
    
    private int categoryId;
    private String categoryTitle;
    
    private FeedAdapter adapter = null;
    private FeedUpdater feedUpdater = null;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        setContentView(R.layout.feedlist);
        
        listView = getListView();
        registerForContextMenu(listView);
        notificationTextView = (TextView) findViewById(R.id.notification);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            categoryId = extras.getInt(CATEGORY_ID);
            categoryTitle = extras.getString(CATEGORY_TITLE);
        } else if (instance != null) {
            categoryId = instance.getInt(CATEGORY_ID);
            categoryTitle = instance.getString(CATEGORY_TITLE);
        } else {
            categoryId = -1;
            categoryTitle = null;
        }
        
        adapter = new FeedAdapter(this, categoryId);
        listView.setAdapter(adapter);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        DBHelper.getInstance().checkAndInitializeDB(this);
        doRefresh();
        doUpdate();
    }
    
    private void closeCursor() {
        if (adapter != null) {
            adapter.closeCursor();
        }
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
        outState.putInt(CATEGORY_ID, categoryId);
        outState.putString(CATEGORY_TITLE, categoryTitle);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void doRefresh() {
        int unreadCount = (feedUpdater != null ? feedUpdater.unreadCount : 0);
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
            notificationTextView.setText(R.string.Loading_EmptyFeeds);
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
            notificationTextView.setText(R.string.Loading_Feeds);
            
            feedUpdater = new FeedUpdater();
            feedUpdater.execute();
        }
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        Intent i = new Intent(this, FeedHeadlineActivity.class);
        i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, categoryId);
        i.putExtra(FeedHeadlineActivity.FEED_ID, adapter.getId(position));
        i.putExtra(FeedHeadlineActivity.FEED_TITLE, adapter.getTitle(position));
        
        startActivity(i);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == MARK_READ) {
            new Updater(this, new ReadStateUpdater(adapter.getId(cmi.position), 42)).execute();
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
                new Updater(this, new ReadStateUpdater(categoryId)).execute();
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
        
        public int unreadCount = 0;
        private int taskCount = 0;
        private static final int DEFAULT_TASK_COUNT = 2;
        
        @Override
        protected Void doInBackground(Void... params) {
            Category c = DBHelper.getInstance().getCategory(categoryId);
            taskCount = DEFAULT_TASK_COUNT + (c.unread != 0 ? 1 : 0);
            
            int progress = 0;
            unreadCount = DBHelper.getInstance().getUnreadCount(categoryId, true);
            publishProgress(++progress); // Move progress forward
            
            Data.getInstance().updateFeeds(categoryId, false);
            publishProgress(++progress); // Move progress forward
            
            // Update articles for current category
            if (c.unread != 0)
                Data.getInstance().updateArticles(c.id, Controller.getInstance().onlyUnread(), true);
            
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
    
}
