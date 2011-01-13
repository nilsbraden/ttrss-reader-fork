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

import java.util.ArrayList;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.interfaces.IUpdateEndListener;
import org.ttrssreader.model.ArticleItem;
import org.ttrssreader.model.FeedHeadlineListAdapter;
import org.ttrssreader.model.updaters.PublishedStateUpdater;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.StarredStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.net.ITTRSSConnector;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class FeedHeadlineListActivity extends MenuActivity implements IUpdateEndListener {
    
    private static final int MARK_GROUP = 42;
    private static final int MARK_STAR = MARK_GROUP + 1;
    private static final int MARK_PUBLISH = MARK_GROUP + 2;
    private static final int MARK_UNREAD = MARK_GROUP + 3;
    
    public static final String FEED_ID = "FEED_ID";
    public static final String FEED_TITLE = "FEED_TITLE";
    public static final String FEED_LIST = "FEED_LIST";
    public static final String FEED_LIST_ID = "FEED_LIST_ID";
    public static final String FEED_LIST_NAME = "FEED_LIST_NAME";
    
    public boolean flingDetected = false;
    
    private int mFeedId;
    private String mFeedTitle;
    private ArrayList<Integer> mFeedListIds;
    private ArrayList<String> mFeedListNames;
    private GestureDetector mGestureDetector;
    private int absHeight;
    private int absWidth;
    
    private ListView mFeedHeadlineListView;
    private FeedHeadlineListAdapter mAdapter = null;
    private Updater updater;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.feedheadlinelist);
        
        Controller.getInstance().checkAndInitializeController(this);
        DBHelper.getInstance().checkAndInitializeDB(this);
        Data.getInstance().checkAndInitializeData(this);
        
        mFeedHeadlineListView = getListView();
        registerForContextMenu(mFeedHeadlineListView);
        mGestureDetector = new GestureDetector(onGestureListener);
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        absHeight = metrics.heightPixels;
        absWidth = metrics.widthPixels;
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mFeedId = extras.getInt(FEED_ID);
            mFeedTitle = extras.getString(FEED_TITLE);
            mFeedListIds = extras.getIntegerArrayList(FEED_LIST_ID);
            mFeedListNames = extras.getStringArrayList(FEED_LIST_NAME);
        } else if (instance != null) {
            mFeedId = instance.getInt(FEED_ID);
            mFeedTitle = instance.getString(FEED_TITLE);
            mFeedListIds = instance.getIntegerArrayList(FEED_LIST_ID);
            mFeedListNames = instance.getStringArrayList(FEED_LIST_NAME);
        } else {
            mFeedId = -1;
            mFeedTitle = null;
            mFeedListIds = null;
            mFeedListNames = null;
        }
        mAdapter = new FeedHeadlineListAdapter(this, mFeedId);
        mFeedHeadlineListView.setAdapter(mAdapter);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        DBHelper.getInstance().checkAndInitializeDB(getApplicationContext());
        doRefresh();
        doUpdate();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updater != null) {
            updater.cancel(true);
            updater = null;
        }
        mAdapter.cursor.deactivate();
        mAdapter.cursor.close();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(FEED_ID, mFeedId);
        outState.putString(FEED_TITLE, mFeedTitle);
        outState.putIntegerArrayList(FEED_LIST_ID, mFeedListIds);
        outState.putStringArrayList(FEED_LIST_NAME, mFeedListNames);
    }
    
    private void doRefresh() {
        flingDetected = false; // reset fling-status
        
        mAdapter.makeQuery();
        mAdapter.notifyDataSetChanged();
        if (!ITTRSSConnector.hasLastError()) {
            setTitle(this.getResources().getString(R.string.ApplicationName) + " (" + mAdapter.getUnreadCount() + ")");
        } else {
            setProgressBarIndeterminateVisibility(false);
            openConnectionErrorDialog(ITTRSSConnector.pullLastError());
            return;
        }
        
        if (updater != null) {
            if (updater.getStatus().equals(Status.FINISHED)) {
                updater = null;
                setProgressBarIndeterminateVisibility(false);
            }
        } else {
            setProgressBarIndeterminateVisibility(false);
        }
    }
    
    private synchronized void doUpdate() {
        // Only update if no updater already running
        if (updater != null) {
            if (updater.getStatus().equals(AsyncTask.Status.FINISHED)) {
                updater = null;
            } else {
                return;
            }
        }
        
        setProgressBarIndeterminateVisibility(true);
        updater = new Updater(this, mAdapter);
        updater.execute();
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        Intent i = new Intent(this, ArticleActivity.class);
        i.putExtra(ArticleActivity.ARTICLE_ID, mAdapter.getFeedItemId(position));
        i.putExtra(ArticleActivity.FEED_ID, mFeedId);
        i.putIntegerArrayListExtra(ArticleActivity.ARTICLE_LIST_ID, mAdapter.getFeedItemIds());
        
        if (!flingDetected) {
            startActivity(i);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        // Get selected Article
        AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        ArticleItem a = (ArticleItem) mAdapter.getItem(info.position);
        
        if (a.isUnread()) {
            menu.add(MARK_GROUP, MARK_UNREAD, Menu.NONE, R.string.Commons_MarkRead);
        } else {
            menu.add(MARK_GROUP, MARK_UNREAD, Menu.NONE, R.string.Commons_MarkUnread);
        }
        
        if (a.isStarred()) {
            menu.add(MARK_GROUP, MARK_STAR, Menu.NONE, R.string.Commons_MarkUnstar);
        } else {
            menu.add(MARK_GROUP, MARK_STAR, Menu.NONE, R.string.Commons_MarkStar);
        }
        
        if (a.isPublished()) {
            menu.add(MARK_GROUP, MARK_PUBLISH, Menu.NONE, R.string.Commons_MarkUnpublish);
        } else {
            menu.add(MARK_GROUP, MARK_PUBLISH, Menu.NONE, R.string.Commons_MarkPublish);
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        ArticleItem a = (ArticleItem) mAdapter.getItem(cmi.position);
        
        if (a == null) {
            return false;
        }
        
        switch (item.getItemId()) {
            case MARK_UNREAD:
                new Updater(this, new ReadStateUpdater(a, mFeedId, a.isUnread() ? 0 : 1)).execute();
                return true;
            case MARK_STAR:
                new Updater(this, new StarredStateUpdater(a, a.isStarred() ? 0 : 1)).execute();
                return true;
            case MARK_PUBLISH:
                new Updater(this, new PublishedStateUpdater(a, a.isPublished() ? 0 : 1)).execute();
                return true;
            default:
                return false;
        }
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        boolean ret = super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case R.id.Menu_Refresh:
                Data.getInstance().resetTime(mFeedId);
                doUpdate();
                return true;
            case R.id.Menu_MarkAllRead:
                new Updater(this, new ReadStateUpdater(mFeedId, 0)).execute();
                return true;
            default:
                break;
        }
        
        if (ret) {
            doRefresh();
        }
        return true;
    }
    
    private void openNextFeed(int direction) {
        if (mFeedId < 0)
            return;
        
        int index = mFeedListIds.indexOf(mFeedId) + direction;
        
        // No more feeds in this direction
        if (index < 0 || index >= mFeedListIds.size()) {
            if (Controller.getInstance().isVibrateOnLastArticle()) {
                Log.i(Utils.TAG, "No more feeds, vibrate..");
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(Utils.SHORT_VIBRATE);
            }
            return;
        }
        
        Intent i = new Intent(this, this.getClass());
        i.putExtra(FEED_ID, mFeedListIds.get(index));
        i.putExtra(FEED_TITLE, mFeedListNames.get(index));
        i.putIntegerArrayListExtra(FEED_LIST_ID, mFeedListIds);
        i.putStringArrayListExtra(FEED_LIST_NAME, mFeedListNames);
        
        startActivity(i);
        this.finish(); // finish() because we don't want to go back through all feeds, we want to go back directly to
                       // the FeedList
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        super.dispatchTouchEvent(e);
        return mGestureDetector.onTouchEvent(e);
    }
    
    private OnGestureListener onGestureListener = new OnGestureListener() {
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            
            // SWIPE_WIDTH must be more then 50% of the screen
            int SWIPE_WIDTH = (int) (absWidth * 0.5);
            
            int dx = (int) (e2.getX() - e1.getX());
            int dy = (int) (e2.getY() - e1.getY());
            
            if (Math.abs(dy) > (int) (absHeight * 0.2)) {
                // Too much Y-Movement (20% of screen-height)
                return false;
            } else if (!Controller.getInstance().isUseSwipe()) {
                return false;
            }
            
            // don't accept the fling if it's too short as it may conflict with a button push
            if (Math.abs(dx) > SWIPE_WIDTH && Math.abs(velocityX) > Math.abs(velocityY)) {
                flingDetected = true;
                
                // Log.d(Utils.TAG,
                // String.format("Fling: (%s %s)(%s %s) dx: %s dy: %s (Direction: %s)", e1.getX(), e1.getY(),
                // e2.getX(), e2.getY(), dx, dy, (velocityX > 0) ? "right" : "left"));
                // Log.d(Utils.TAG, String.format("SWIPE_WIDTH: %s", SWIPE_WIDTH));
                
                if (velocityX > 0) {
                    openNextFeed(-1);
                } else {
                    openNextFeed(1);
                }
                return true;
            }
            
            return false;
        }
        
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }
        
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }
        
        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }
        
        @Override
        public void onLongPress(MotionEvent e) {
        }
        
        @Override
        public void onShowPress(MotionEvent e) {
        }
    };
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Controller.getInstance().isUseVolumeKeys()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                openNextFeed(-1);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                openNextFeed(1);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (Controller.getInstance().isUseVolumeKeys()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }
    
    private void openConnectionErrorDialog(String errorMessage) {
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
            doRefresh();
            doUpdate();
        }
    }
    
    @Override
    public void onUpdateEnd() {
        updater = null;
        doRefresh();
    }
    
}
