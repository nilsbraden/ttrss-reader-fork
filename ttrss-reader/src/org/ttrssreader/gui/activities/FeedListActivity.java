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

package org.ttrssreader.gui.activities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.IRefreshEndListener;
import org.ttrssreader.gui.IUpdateEndListener;
import org.ttrssreader.model.Refresher;
import org.ttrssreader.model.Updater;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.model.feed.FeedListAdapter;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.net.ITTRSSConnector;
import org.ttrssreader.utils.Utils;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class FeedListActivity extends ListActivity implements IRefreshEndListener, IUpdateEndListener {
    
    private static final int MARK_GROUP = 42;
    private static final int MARK_READ = MARK_GROUP + 1;
    
    public static final String CATEGORY_ID = "CATEGORY_ID";
    public static final String CATEGORY_TITLE = "CATEGORY_TITLE";
    
    private int mCategoryId;
    private String mCategoryTitle;
    
    private ListView mFeedListView;
    private FeedListAdapter mAdapter = null;
    private Refresher refresher;
    private Updater updater;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.feedlist);
        
        Controller.getInstance().checkAndInitializeController(this);
        DBHelper.getInstance().checkAndInitializeDB(this);
        Data.getInstance().checkAndInitializeData(this);
        
        mFeedListView = getListView();
        registerForContextMenu(mFeedListView);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mCategoryId = extras.getInt(CATEGORY_ID);
            mCategoryTitle = extras.getString(CATEGORY_TITLE);
        } else if (instance != null) {
            mCategoryId = instance.getInt(CATEGORY_ID);
            mCategoryTitle = instance.getString(CATEGORY_TITLE);
        } else {
            mCategoryId = -1;
            mCategoryTitle = null;
        }
        
        mAdapter = new FeedListAdapter(this, mCategoryId);
        mFeedListView.setAdapter(mAdapter);
        doUpdate();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        DBHelper.getInstance().checkAndInitializeDB(getApplicationContext());
        doRefresh();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (refresher != null) {
            refresher.cancel(true);
            refresher = null;
        }
        if (updater != null) {
            updater.cancel(true);
            updater = null;
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CATEGORY_ID, mCategoryId);
        outState.putString(CATEGORY_TITLE, mCategoryTitle);
        // TODO Perhaps we can save the data from the adapter too so we dont have to read from DB?
    }
    
    private void doRefresh() {
        
        // Only update if no refresher already running
        if (refresher != null) {
            if (refresher.getStatus().equals(AsyncTask.Status.PENDING)) {
                return;
            } else if (refresher.getStatus().equals(AsyncTask.Status.FINISHED)) {
                refresher = null;
                return;
            }
        }
        
        if (mAdapter == null) {
            mAdapter = new FeedListAdapter(this, mCategoryId);
            mFeedListView.setAdapter(mAdapter);
        }
        
        setProgressBarIndeterminateVisibility(true);
        refresher = new Refresher(this, mAdapter);
        refresher.execute();
    }
    
    private synchronized void doUpdate() {
        // Only update if no updater already running
        if (updater != null) {
            if (updater.getStatus().equals(AsyncTask.Status.PENDING)) {
                return;
            } else if (updater.getStatus().equals(AsyncTask.Status.FINISHED)) {
                updater = null;
                return;
            }
        }
        
        if (mAdapter == null) {
            mAdapter = new FeedListAdapter(this, mCategoryId);
            mFeedListView.setAdapter(mAdapter);
        }
        
        updater = new Updater(this, mAdapter);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (updater != null) {
                    setProgressBarIndeterminateVisibility(true);
                    updater.execute();
                }
            }
        }, Utils.WAIT);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        menu.add(MARK_GROUP, MARK_READ, Menu.NONE, R.string.Commons_MarkRead);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        FeedItem f = (FeedItem) mAdapter.getItem(cmi.position);
        
        if (f == null) {
            return false;
        }
        
        switch (item.getItemId()) {
            case MARK_READ:
                new Updater(this, new ReadStateUpdater(f.getId(), 42)).execute();
                return true;
        }
        return false;
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        Intent i = new Intent(this, FeedHeadlineListActivity.class);
        i.putExtra(FeedHeadlineListActivity.FEED_ID, mAdapter.getFeedId(position));
        i.putExtra(FeedHeadlineListActivity.FEED_TITLE, mAdapter.getFeedTitle(position));
        i.putIntegerArrayListExtra(FeedHeadlineListActivity.FEED_LIST_ID, mAdapter.getFeedIds());
        i.putStringArrayListExtra(FeedHeadlineListActivity.FEED_LIST_NAME, mAdapter.getFeedNames());
        
        startActivity(i);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.feedlist, menu);
        return true;
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Feedlist_Menu_Refresh:
                Data.getInstance().resetFeedTime();
                doUpdate();
                doRefresh();
                return true;
            case R.id.Feedlist_Menu_DisplayOnlyUnread:
                boolean displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnread();
                Controller.getInstance().setDisplayOnlyUnread(!displayOnlyUnread);
                doRefresh();
                return true;
            case R.id.Feedlist_Menu_MarkAllRead:
                new Updater(this, new ReadStateUpdater(mCategoryId)).execute();
                return true;
            default:
                return false;
        }
    }
    
    private void openConnectionErrorDialog(String errorMessage) {
        if (refresher != null) {
            refresher.cancel(true);
            refresher = null;
        }
        if (updater != null) {
            updater.cancel(true);
            updater = null;
        }
        
        Intent i = new Intent(this, ErrorActivity.class);
        i.putExtra(ErrorActivity.ERROR_MESSAGE, errorMessage);
        startActivityForResult(i, ErrorActivity.ACTIVITY_SHOW_ERROR);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ErrorActivity.ACTIVITY_SHOW_ERROR) {
            doUpdate();
            doRefresh();
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void onRefreshEnd() {
        if (!ITTRSSConnector.hasLastError()) {
            
            try {
                List<FeedItem> list = new ArrayList<FeedItem>();
                list.addAll((Set<FeedItem>) refresher.get());
                refresher = null;
                mAdapter.setFeeds(list);
                mAdapter.notifyDataSetChanged();
            } catch (Exception e) {
            }
            
            if (mCategoryTitle != null) {
                this.setTitle(mCategoryTitle + " (" + mAdapter.getTotalUnreadCount() + ")");
            } else {
                this.setTitle(this.getResources().getString(R.string.ApplicationName) + " ("
                        + mAdapter.getTotalUnreadCount() + ")");
            }
            
        } else {
            openConnectionErrorDialog(ITTRSSConnector.pullLastError());
        }
        
        if (updater != null) {
            if (updater.getStatus().equals(Status.FINISHED)) {
                setProgressBarIndeterminateVisibility(false);
            }
        } else {
            setProgressBarIndeterminateVisibility(false);
        }
    }
    
    @Override
    public void onUpdateEnd() {
        updater = null;
        doRefresh();
    }
    
}
