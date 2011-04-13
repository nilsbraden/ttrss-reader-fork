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
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.FeedListAdapter;
import org.ttrssreader.model.MainAdapter;
import org.ttrssreader.model.pojos.FeedItem;
import org.ttrssreader.model.updaters.FeedListUpdater;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.net.JSONConnector;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

public class FeedListActivity extends MenuActivity {
    
    public static final String CATEGORY_ID = "CATEGORY_ID";
    public static final String CATEGORY_TITLE = "CATEGORY_TITLE";
    
    private int categoryId;
    private String categoryTitle;
    
    private FeedListAdapter adapter = null;
    private FeedListUpdater updateable = null;
    
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
        
        updateable = new FeedListUpdater(categoryId);
        adapter = new FeedListAdapter(this, categoryId);
        listView.setAdapter(adapter);
    }
    
    protected void doDestroy() {
        if (adapter != null)
            adapter.closeCursor();
    }
    
    @Override
    protected void onDestroy() {
        if (adapter != null)
            adapter.closeCursor();
        super.onDestroy();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        DBHelper.getInstance().checkAndInitializeDB(this);
        doRefresh();
        doUpdate();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CATEGORY_ID, categoryId);
        outState.putString(CATEGORY_TITLE, categoryTitle);
    }
    
    @Override
    protected synchronized void doRefresh() {
        setTitle(MainAdapter.formatTitle(categoryTitle, updateable.unreadCount));
        
        adapter.makeQuery(true);
        adapter.notifyDataSetChanged();
        
        if (JSONConnector.hasLastError()) {
            openConnectionErrorDialog(JSONConnector.pullLastError());
            return;
        }
        
        if (updater == null) {
            setProgressBarIndeterminateVisibility(false);
            notificationTextView.setText(R.string.Loading_EmptyFeeds);
        }
    }
    
    @Override
    protected synchronized void doUpdate() {
        // Only update if no updater already running
        if (updater != null) {
            if (updater.getStatus().equals(AsyncTask.Status.FINISHED)) {
                updater = null;
            } else {
                return;
            }
        }
        
        setProgressBarIndeterminateVisibility(true);
        notificationTextView.setText(R.string.Loading_Feeds);
        
        updater = new Updater(this, updateable);
        updater.execute();
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        Intent i = new Intent(this, FeedHeadlineListActivity.class);
        i.putExtra(FeedHeadlineListActivity.FEED_CAT_ID, categoryId);
        i.putExtra(FeedHeadlineListActivity.FEED_ID, adapter.getId(position));
        i.putExtra(FeedHeadlineListActivity.FEED_TITLE, adapter.getTitle(position));
        
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
                Data.getInstance().resetTime(new FeedItem());
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

}
