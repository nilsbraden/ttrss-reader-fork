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
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.IRefreshEndListener;
import org.ttrssreader.gui.IUpdateEndListener;
import org.ttrssreader.model.ReadStateUpdater;
import org.ttrssreader.model.Refresher;
import org.ttrssreader.model.Updater;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.model.feed.FeedListAdapter;
import org.ttrssreader.utils.Utils;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.AsyncTask.Status;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;

public class FeedListActivity extends ListActivity implements IRefreshEndListener, IUpdateEndListener {
    
    private static final int ACTIVITY_SHOW_FEED_HEADLINE = 0;
    
    private static final int MENU_REFRESH = Menu.FIRST;
    private static final int MENU_DISPLAY_ONLY_UNREAD = Menu.FIRST + 1;
    private static final int MENU_MARK_ALL_READ = Menu.FIRST + 2;
    
    public static final String CATEGORY_ID = "CATEGORY_ID";
    public static final String CATEGORY_TITLE = "CATEGORY_TITLE";
    
    private int mCategoryId;
    private String mCategoryTitle;
    
    private ListView mFeedListView;
    private FeedListAdapter mAdapter = null;
    private Refresher refresher;
    private Updater updater;
    
    @Override
    public void onCreate(Bundle instance) {
        super.onCreate(instance);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.feedlist);
        
        Controller.getInstance().checkAndInitializeController(this);
        DBHelper.getInstance().checkAndInitializeController(this);
        Data.getInstance().checkAndInitializeController(this);
        
        setProgressBarIndeterminateVisibility(false);
        mFeedListView = getListView();
        
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
        updater = new Updater(this, mAdapter);
        
        new Handler().postDelayed(new Runnable() {
            public void run() {
                setProgressBarIndeterminateVisibility(true);
                updater.execute();
            }
        }, Utils.WAIT);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        doRefresh();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (refresher != null)
            refresher.cancel(true);
        if (updater != null)
            updater.cancel(true);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CATEGORY_ID, mCategoryId);
        outState.putString(CATEGORY_TITLE, mCategoryTitle);
    }
    
    private void doRefresh() {
        setProgressBarIndeterminateVisibility(true);
        
        if (mAdapter == null) {
            mAdapter = new FeedListAdapter(this, mCategoryId);
            mFeedListView.setAdapter(mAdapter);
        }
        
        refresher = new Refresher(this, mAdapter);
        refresher.execute();
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        Intent i = new Intent(this, FeedHeadlineListActivity.class);
        i.putExtra(FeedHeadlineListActivity.FEED_ID, mAdapter.getFeedId(position));
        i.putExtra(FeedHeadlineListActivity.FEED_TITLE, mAdapter.getFeedTitle(position));
        i.putIntegerArrayListExtra(FeedHeadlineListActivity.FEED_LIST, mAdapter.getFeedIds());
        i.putStringArrayListExtra(FeedHeadlineListActivity.FEED_LIST_NAMES, mAdapter.getFeedNames());
        
        startActivityForResult(i, ACTIVITY_SHOW_FEED_HEADLINE);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        MenuItem item;
        
        item = menu.add(0, MENU_REFRESH, 0, R.string.Main_RefreshMenu);
        item.setIcon(R.drawable.refresh32);
        
        item = menu.add(0, MENU_DISPLAY_ONLY_UNREAD, 0, R.string.Commons_DisplayOnlyUnread);
        
        item = menu.add(0, MENU_MARK_ALL_READ, 0, R.string.Commons_MarkAllRead);
        
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case MENU_REFRESH:
                doForceRefresh();
                return true;
            case MENU_DISPLAY_ONLY_UNREAD:
                displayOnlyUnreadSwitch();
                return true;
            case MENU_MARK_ALL_READ:
                markAllRead();
                return true;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }
    
    private void doForceRefresh() {
        Data.getInstance().resetFeedTime();
        doRefresh();
    }
    
    private void displayOnlyUnreadSwitch() {
        boolean displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnread();
        Controller.getInstance().setDisplayOnlyUnread(!displayOnlyUnread);
        doRefresh();
    }
    
    private void markAllRead() {
        setProgressBarIndeterminateVisibility(true);
        new Updater(this, new ReadStateUpdater(mAdapter.getFeeds(), mCategoryId, 0, false)).execute();
    }
    
    private void openConnectionErrorDialog(String errorMessage) {
        Intent i = new Intent(this, ErrorActivity.class);
        i.putExtra(ErrorActivity.ERROR_MESSAGE, errorMessage);
        
        refresher.cancel(true);
        updater.cancel(true);
        
        startActivity(i);
    }
    
    @Override
    public void onRefreshEnd() {
        if (!Controller.getInstance().getTTRSSConnector().hasLastError()) {
            
            try {
                List<FeedItem> list = new ArrayList<FeedItem>();
                for (Object f : refresher.get()) {
                    list.add((FeedItem) f);
                }
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
            openConnectionErrorDialog(Controller.getInstance().getTTRSSConnector().pullLastError());
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
        if (!Controller.getInstance().getTTRSSConnector().hasLastError()) {
            mAdapter.notifyDataSetChanged();
        } else {
            openConnectionErrorDialog(Controller.getInstance().getTTRSSConnector().pullLastError());
        }
        
        doRefresh();
    }
    
}
