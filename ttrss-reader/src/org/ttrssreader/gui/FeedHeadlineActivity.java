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
import org.ttrssreader.model.FeedHeadlineAdapter;
import org.ttrssreader.model.MainAdapter;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.updaters.PublishedStateUpdater;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.StarredStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
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

public class FeedHeadlineActivity extends MenuActivity {
    
    public static final String FEED_CAT_ID = "FEED_CAT_ID";
    public static final String FEED_ID = "ARTICLE_FEED_ID";
    public static final String FEED_TITLE = "FEED_TITLE";
    public static final String FEED_SELECT_ARTICLES = "FEED_SELECT_ARTICLES";
    public static final String FEED_INDEX = "INDEX";
    public static final int FEED_NO_ID = 37846914;
    
    public boolean flingDetected = false;
    
    // Extras
    private int categoryId = -1000;
    private int feedId = -1000;
    private String feedTitle = null;
    private boolean selectArticlesForCategory = false;
    
    private GestureDetector gestureDetector;
    
    private FeedHeadlineAdapter adapter = null;
    private FeedHeadlineUpdater headlineUpdater = null;
    private FeedAdapter parentAdapter = null;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        setContentView(R.layout.feedheadlinelist);
        
        listView = getListView();
        registerForContextMenu(listView);
        notificationTextView = (TextView) findViewById(R.id.notification);
        gestureDetector = new GestureDetector(onGestureListener);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            categoryId = extras.getInt(FEED_CAT_ID);
            feedId = extras.getInt(FEED_ID);
            feedTitle = extras.getString(FEED_TITLE);
            selectArticlesForCategory = extras.getBoolean(FEED_SELECT_ARTICLES);
        } else if (instance != null) {
            categoryId = instance.getInt(FEED_CAT_ID);
            feedId = instance.getInt(FEED_ID);
            feedTitle = instance.getString(FEED_TITLE);
            selectArticlesForCategory = instance.getBoolean(FEED_SELECT_ARTICLES);
        }
        
        Controller.getInstance().lastOpenedFeed = feedId;
        Controller.getInstance().lastOpenedArticle = null;
        
        parentAdapter = new FeedAdapter(getApplicationContext(), categoryId);
        adapter = new FeedHeadlineAdapter(this, feedId, categoryId, selectArticlesForCategory);
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
        if (parentAdapter != null) {
            parentAdapter.closeCursor();
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
        super.onSaveInstanceState(outState);
        outState.putInt(FEED_CAT_ID, categoryId);
        outState.putInt(FEED_ID, feedId);
        outState.putString(FEED_TITLE, feedTitle);
        outState.putBoolean(FEED_SELECT_ARTICLES, selectArticlesForCategory);
    }
    
    @Override
    protected void doRefresh() {
        int unreadCount = 0;
        if (selectArticlesForCategory)
            unreadCount = DBHelper.getInstance().getUnreadCount(categoryId, true);
        else
            unreadCount = DBHelper.getInstance().getUnreadCount(feedId, false);
        
        setTitle(MainAdapter.formatTitle(feedTitle, unreadCount));
        flingDetected = false; // reset fling-status
        
        if (adapter != null) {
            adapter.makeQuery(true);
            adapter.notifyDataSetChanged();
        }
        
        try {
            if (Controller.getInstance().getConnector().hasLastError())
                openConnectionErrorDialog(Controller.getInstance().getConnector().pullLastError());
        } catch (NotInitializedException e) {
        }
        
        if (headlineUpdater == null) {
            setProgressBarIndeterminateVisibility(false);
            setProgressBarVisibility(false);
            notificationTextView.setText(R.string.Loading_EmptyHeadlines);
        }
    }
    
    @Override
    protected void doUpdate() {
        // Only update if no headlineUpdater already running
        if (headlineUpdater != null) {
            if (headlineUpdater.getStatus().equals(AsyncTask.Status.FINISHED)) {
                headlineUpdater = null;
            } else {
                return;
            }
        }
        
        if (!isCacherRunning()) {
            setProgressBarIndeterminateVisibility(true);
            setProgressBarVisibility(false);
            notificationTextView.setText(R.string.Loading_Headlines);
            
            headlineUpdater = new FeedHeadlineUpdater();
            headlineUpdater.execute();
        }
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        if (!flingDetected) {
            Intent i = new Intent(this, ArticleActivity.class);
            i.putExtra(ArticleActivity.ARTICLE_ID, adapter.getId(position));
            i.putExtra(ArticleActivity.ARTICLE_FEED_ID, feedId);
            i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, categoryId);
            i.putExtra(FeedHeadlineActivity.FEED_SELECT_ARTICLES, selectArticlesForCategory);
            i.putExtra(ArticleActivity.ARTICLE_LAST_MOVE, ArticleActivity.ARTICLE_LAST_MOVE_DEFAULT);
            
            startActivity(i);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        // Get selected Article
        AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Article a = (Article) adapter.getItem(info.position);
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
        Article a = (Article) adapter.getItem(cmi.position);
        
        if (a == null)
            return false;
        
        switch (item.getItemId()) {
            case MARK_READ:
                new Updater(this, new ReadStateUpdater(a, feedId, a.isUnread ? 0 : 1)).execute();
                break;
            case MARK_STAR:
                new Updater(this, new StarredStateUpdater(a, a.isStarred ? 0 : 1)).execute();
                break;
            case MARK_PUBLISH:
                new Updater(this, new PublishedStateUpdater(a, a.isPublished ? 0 : 1)).execute();
                break;
            default:
                return false;
        }
        doRefresh();
        return true;
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        boolean ret = super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case R.id.Menu_Refresh:
                if (selectArticlesForCategory) {
                    Data.getInstance().resetTime(categoryId, false, true, false);
                } else {
                    Data.getInstance().resetTime(feedId, false, false, true);
                }
                doUpdate();
                return true;
            case R.id.Menu_MarkAllRead:
                if (selectArticlesForCategory) {
                    new Updater(this, new ReadStateUpdater(categoryId)).execute();
                } else {
                    new Updater(this, new ReadStateUpdater(feedId, 42)).execute();
                }
                
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
        
        int currentIndex = -2; // -2 so index is still -1 if direction is +1, avoids moving when no move possible
        int tempIndex = parentAdapter.getIds().indexOf(feedId);
        if (tempIndex >= 0)
            currentIndex = tempIndex;
        
        int index = currentIndex + direction;
        
        // No more feeds in this direction
        if (index < 0 || index >= parentAdapter.getCount()) {
            Log.d(Utils.TAG, String.format("No more feeds in this direction. (Index: %s, ID: %s)", index, feedId));
            if (Controller.getInstance().vibrateOnLastArticle())
                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(Utils.SHORT_VIBRATE);
            return;
        }
        
        int id = parentAdapter.getId(index);
        String title = parentAdapter.getTitle(index);
        
        Intent i = new Intent(this, getClass());
        i.putExtra(FEED_ID, id);
        i.putExtra(FEED_TITLE, title);
        i.putExtra(FEED_CAT_ID, categoryId);
        
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
            
            int dx = (int) (e2.getX() - e1.getX());
            int dy = (int) (e2.getY() - e1.getY());
            
            if (Math.abs(dy) > (int) (Controller.absHeight * 0.2)) {
                return false; // Too much Y-Movement (20% of screen-height)
            }
            
            // don't accept the fling if it's too short as it may conflict with a button push
            if (Math.abs(dx) > Controller.swipeWidth && Math.abs(velocityX) > Math.abs(velocityY)) {
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
    
    /**
     * 
     * 
     * @author n
     * 
     */
    public class FeedHeadlineUpdater extends AsyncTask<Void, Integer, Void> {
        
        private int taskCount = 0;
        private static final int DEFAULT_TASK_COUNT = 2;
        
        @Override
        protected Void doInBackground(Void... params) {
            taskCount = DEFAULT_TASK_COUNT;
            
            int progress = 0;
            boolean displayUnread = Controller.getInstance().onlyUnread();
            
            if (selectArticlesForCategory) {
                publishProgress(++progress); // Move progress forward
                
                Data.getInstance().updateArticles(categoryId, displayUnread, true);
            } else {
                publishProgress(++progress); // Move progress forward
                
                Data.getInstance().updateArticles(feedId, displayUnread, false);
            }
            
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
