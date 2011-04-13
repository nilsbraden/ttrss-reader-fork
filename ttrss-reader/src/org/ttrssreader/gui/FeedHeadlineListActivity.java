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
import org.ttrssreader.model.FeedHeadlineListAdapter;
import org.ttrssreader.model.FeedListAdapter;
import org.ttrssreader.model.MainAdapter;
import org.ttrssreader.model.pojos.ArticleItem;
import org.ttrssreader.model.updaters.FeedHeadlineListUpdater;
import org.ttrssreader.model.updaters.PublishedStateUpdater;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.StarredStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.net.JSONConnector;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

public class FeedHeadlineListActivity extends MenuActivity {
    
    public static final String FEED_CAT_ID = "FEED_CAT_ID";
    public static final String FEED_ID = "FEED_ID";
    public static final String FEED_TITLE = "FEED_TITLE";
    public static final String FEED_SELECT_ARTICLES = "FEED_SELECT_ARTICLES";
    public static final String FEED_INDEX = "INDEX";
    public static final int FEED_NO_ID = 37846914;
    
    public boolean flingDetected = false;
    
    private int categoryId = -1000;
    private int feedId = -1000;
    private String feedTitle = null;
    private int currentIndex = 0;
    private boolean selectArticlesForCategory = false;
    
    private GestureDetector gestureDetector;
    private int absHeight;
    private int absWidth;
    
    private FeedHeadlineListAdapter adapter = null;
    private FeedHeadlineListUpdater updateable = null;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        setContentView(R.layout.feedheadlinelist);
        
        listView = getListView();
        registerForContextMenu(listView);
        notificationTextView = (TextView) findViewById(R.id.notification);
        gestureDetector = new GestureDetector(onGestureListener);
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        absHeight = metrics.heightPixels;
        absWidth = metrics.widthPixels;
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            categoryId = extras.getInt(FEED_CAT_ID);
            feedId = extras.getInt(FEED_ID);
            feedTitle = extras.getString(FEED_TITLE);
            currentIndex = extras.getInt(FEED_INDEX);
            selectArticlesForCategory = extras.getBoolean(FEED_SELECT_ARTICLES);
        } else if (instance != null) {
            categoryId = instance.getInt(FEED_CAT_ID);
            feedId = instance.getInt(FEED_ID);
            feedTitle = instance.getString(FEED_TITLE);
            currentIndex = instance.getInt(FEED_INDEX);
            selectArticlesForCategory = instance.getBoolean(FEED_SELECT_ARTICLES);
        }
        
        if (selectArticlesForCategory) {
            updateable = new FeedHeadlineListUpdater(selectArticlesForCategory, categoryId);
        } else {
            updateable = new FeedHeadlineListUpdater(feedId);
        }
        adapter = new FeedHeadlineListAdapter(this, feedId, categoryId, selectArticlesForCategory);
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
        outState.putInt(FEED_CAT_ID, categoryId);
        outState.putInt(FEED_ID, feedId);
        outState.putString(FEED_TITLE, feedTitle);
        outState.putInt(FEED_INDEX, currentIndex);
        outState.putBoolean(FEED_SELECT_ARTICLES, selectArticlesForCategory);
    }
    
    @Override
    protected synchronized void doRefresh() {
        setTitle(MainAdapter.formatTitle(feedTitle, updateable.unreadCount));
        flingDetected = false; // reset fling-status
        
        adapter.makeQuery(true);
        adapter.notifyDataSetChanged();
        
        FeedListAdapter feedListAdapter = new FeedListAdapter(getApplicationContext(), categoryId);
        feedListAdapter.makeQuery(true);
        
        // Store current index in ID-List so we can jump between articles
        if (feedListAdapter.getIds().indexOf(feedId) >= 0)
            currentIndex = feedListAdapter.getIds().indexOf(feedId);
        
        feedListAdapter.closeCursor();
        
        if (JSONConnector.hasLastError()) {
            openConnectionErrorDialog(JSONConnector.pullLastError());
            return;
        }
        
        if (updater == null) {
            setProgressBarIndeterminateVisibility(false);
            notificationTextView.setText(R.string.Loading_EmptyHeadlines);
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
        notificationTextView.setText(R.string.Loading_Headlines);
        
        updater = new Updater(this, updateable);
        updater.execute();
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        if (!flingDetected) {
            Intent i = new Intent(this, ArticleActivity.class);
            i.putExtra(ArticleActivity.ARTICLE_ID, adapter.getId(position));
            i.putExtra(ArticleActivity.FEED_ID, feedId);
            i.putExtra(FeedHeadlineListActivity.FEED_CAT_ID, categoryId);
            i.putExtra(FeedHeadlineListActivity.FEED_SELECT_ARTICLES, selectArticlesForCategory);
            
            startActivity(i);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        // Get selected Article
        AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        ArticleItem a = (ArticleItem) adapter.getItem(info.position);
        menu.removeItem(MARK_READ); // Remove "Mark read" from super-class
        
        if (a.isUnread) {
            menu.add(MARK_GROUP, MARK_READ, Menu.NONE, R.string.Commons_MarkRead);
        } else {
            menu.add(MARK_GROUP, MARK_READ, Menu.NONE, R.string.Commons_MarkUnread);
        }
        
        if (a.isStarred) {
            menu.add(MARK_GROUP, MARK_STAR, Menu.NONE, R.string.Commons_MarkUnstar);
        } else {
            menu.add(MARK_GROUP, MARK_STAR, Menu.NONE, R.string.Commons_MarkStar);
        }
        
        if (a.isPublished) {
            menu.add(MARK_GROUP, MARK_PUBLISH, Menu.NONE, R.string.Commons_MarkUnpublish);
        } else {
            menu.add(MARK_GROUP, MARK_PUBLISH, Menu.NONE, R.string.Commons_MarkPublish);
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        ArticleItem a = (ArticleItem) adapter.getItem(cmi.position);
        
        if (a == null)
            return false;
        
        switch (item.getItemId()) {
            case MARK_READ:
                new Updater(this, new ReadStateUpdater(a, feedId, a.isUnread ? 0 : 1)).execute();
                return true;
            case MARK_STAR:
                new Updater(this, new StarredStateUpdater(a, a.isStarred ? 0 : 1)).execute();
                return true;
            case MARK_PUBLISH:
                new Updater(this, new PublishedStateUpdater(a, a.isPublished ? 0 : 1)).execute();
                return true;
        }
        return false;
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        boolean ret = super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case R.id.Menu_Refresh:
                Data.getInstance().resetTime(feedId);
                doUpdate();
                return true;
            case R.id.Menu_MarkAllRead:
                if (feedId >= 0)
                    new Updater(this, new ReadStateUpdater(feedId, 0)).execute();
                return true;
        }
        
        if (ret) {
            doRefresh();
        }
        return true;
    }
    
    private void openNextFeed(int direction) {
        if (feedId < 0)
            return;
        
        FeedListAdapter feedListAdapter = new FeedListAdapter(getApplicationContext(), categoryId);
        int index = currentIndex + direction;
        
        // No more feeds in this direction
        if (index < 0 || index >= feedListAdapter.getCount()) {
            if (Controller.getInstance().vibrateOnLastArticle()) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(Utils.SHORT_VIBRATE);
            }
            return;
        }
        
        Intent i = new Intent(this, getClass());
        i.putExtra(FEED_CAT_ID, categoryId);
        i.putExtra(FEED_ID, feedListAdapter.getId(index));
        i.putExtra(FEED_TITLE, feedListAdapter.getTitle(index));
        
        feedListAdapter.closeCursor();
        
        startActivityForResult(i, 0);
        finish();
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        super.dispatchTouchEvent(e);
        return gestureDetector.onTouchEvent(e);
    }
    
    private OnGestureListener onGestureListener = new OnGestureListener() {
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!Controller.getInstance().useSwipe())
                return false;
            
            // SWIPE_WIDTH must be more then 50% of the screen
            int SWIPE_WIDTH = (int) (absWidth * 0.5);
            
            int dx = (int) (e2.getX() - e1.getX());
            int dy = (int) (e2.getY() - e1.getY());
            
            if (Math.abs(dy) > (int) (absHeight * 0.2)) {
                return false; // Too much Y-Movement (20% of screen-height)
            }
            
            // don't accept the fling if it's too short as it may conflict with a button push
            if (Math.abs(dx) > SWIPE_WIDTH && Math.abs(velocityX) > Math.abs(velocityY)) {
                flingDetected = true;
                
                if (velocityX > 0) {
                    openNextFeed(-1);
                } else {
                    openNextFeed(1);
                }
                return true;
            }
            return false;
        }
        
        // @formatter:off
        @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { return false; }
        @Override public boolean onSingleTapUp(MotionEvent e) { return false; }
        @Override public boolean onDown(MotionEvent e) { return false; }
        @Override public void onLongPress(MotionEvent e) { }
        @Override public void onShowPress(MotionEvent e) { }
        // @formatter:on
    };
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Controller.getInstance().useVolumeKeys()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_N) {
                openNextFeed(-1);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_B) {
                openNextFeed(1);
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
    
}
