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
import org.ttrssreader.gui.fragments.ArticleFragment;
import org.ttrssreader.gui.interfaces.TextInputAlertCallback;
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
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
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

public class FeedHeadlineActivity extends MenuActivity implements TextInputAlertCallback {
    
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
    
    private FeedHeadlineAdapter adapter = null; // Remember to explicitly check every access to adapter for it beeing
                                                // null!
    private FeedHeadlineUpdater headlineUpdater = null;
    private FeedAdapter parentAdapter = null;
    private int[] parentIDs = new int[2];
    private String[] parentTitles = new String[2];
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        // Log.d(Utils.TAG, "onCreate - FeedHeadlineActivity");
        setContentView(R.layout.feedheadlinelist);
        
        gestureDetector = new GestureDetector(getApplicationContext(), onGestureListener);
        
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
        fillParentInformation();
    }
    
    private void fillParentInformation() {
        int index = parentAdapter.getIds().indexOf(feedId);
        if (index >= 0) {
            parentIDs[0] = parentAdapter.getId(index - 1); // Previous
            parentIDs[1] = parentAdapter.getId(index + 1); // Next
            parentTitles[0] = parentAdapter.getTitle(index - 1);
            parentTitles[1] = parentAdapter.getTitle(index + 1);
            
            if (parentIDs[0] == 0) {
                parentIDs[0] = -1;
                parentTitles[0] = "";
            }
            if (parentIDs[1] == 0) {
                parentIDs[1] = -1;
                parentTitles[1] = "";
            }
        }
    }
    
    @Override
    protected void onResume() {
        if (adapter != null)
            adapter.makeQuery(true);
        
        super.onResume();
        
        if (selectArticlesForCategory) {
            UpdateController.getInstance().registerActivity(this, UpdateController.TYPE_CATEGORY, categoryId);
        } else {
            UpdateController.getInstance().registerActivity(this, UpdateController.TYPE_FEED, feedId);
        }
        DBHelper.getInstance().checkAndInitializeDB(this);
        refreshAndUpdate();
    }
    
    private void closeCursor() {
        if (adapter != null)
            adapter.closeCursor();
        if (parentAdapter != null)
            parentAdapter.closeCursor();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        if (selectArticlesForCategory) {
            UpdateController.getInstance().unregisterActivity(this, UpdateController.TYPE_CATEGORY, categoryId);
        } else {
            UpdateController.getInstance().unregisterActivity(this, UpdateController.TYPE_FEED, feedId);
        }
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
    
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        outState.putInt(FEED_CAT_ID, categoryId);
//        outState.putInt(FEED_ID, feedId);
//        outState.putString(FEED_TITLE, feedTitle);
//        outState.putBoolean(FEED_SELECT_ARTICLES, selectArticlesForCategory);
//        super.onSaveInstanceState(outState);
//    }
    
    @Override
    protected void doRefresh() {
        int unreadCount = 0;
        if (selectArticlesForCategory)
            unreadCount = DBHelper.getInstance().getUnreadCount(categoryId, true);
        else
            unreadCount = DBHelper.getInstance().getUnreadCount(feedId, false);
        
        setTitle(MainAdapter.formatTitle(feedTitle, unreadCount));
        flingDetected = false; // reset fling-status
        
        if (adapter != null)
            adapter.refreshQuery();
        
        try {
            if (Controller.getInstance().getConnector().hasLastError())
                openConnectionErrorDialog(Controller.getInstance().getConnector().pullLastError());
        } catch (NotInitializedException e) {
        }
        
        if (headlineUpdater == null) {
            setProgressBarIndeterminateVisibility(false);
            setProgressBarVisibility(false);
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
            
            headlineUpdater = new FeedHeadlineUpdater();
            if (Controller.getInstance().isExecuteOnExecutorAvailable())
                headlineUpdater.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                headlineUpdater.execute();
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
            menu.add(MARK_GROUP, MARK_PUBLISH_NOTE, Menu.NONE, R.string.Commons_MarkPublishNote);
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
                new Updater(this, new ReadStateUpdater(a, feedId, a.isUnread ? 0 : 1)).exec();
                break;
            case MARK_STAR:
                new Updater(this, new StarredStateUpdater(a, a.isStarred ? 0 : 1)).exec();
                break;
            case MARK_PUBLISH:
                new Updater(this, new PublishedStateUpdater(a, a.isPublished ? 0 : 1)).exec();
                break;
            case MARK_PUBLISH_NOTE:
                new TextInputAlert(this, a).show(this);
                break;
            default:
                return false;
        }
        return true;
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
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
                    new Updater(this, new ReadStateUpdater(categoryId)).exec();
                } else {
                    new Updater(this, new ReadStateUpdater(feedId, 42)).exec();
                }
                return true;
            default:
                return false;
        }
    }
    
    private void openNextFeed(int direction) {
        if (feedId < 0)
            return;
        
        int id = direction < 0 ? parentIDs[0] : parentIDs[1];
        String title = direction < 0 ? parentTitles[0] : parentTitles[1];
        
        if (id < 0) {
            if (Controller.getInstance().vibrateOnLastArticle())
                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(Utils.SHORT_VIBRATE);
            return;
        }
        
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
                
                Data.getInstance().updateArticles(categoryId, displayUnread, true, false);
            } else {
                publishProgress(++progress); // Move progress forward
                
                Data.getInstance().updateArticles(feedId, displayUnread, false, false);
            }
            
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
        if (adapter instanceof FeedHeadlineAdapter)
            this.adapter = (FeedHeadlineAdapter) adapter;
    }
    
    @Override
    public void itemSelected(TYPE type, int selectedIndex, int oldIndex) {
        // Log.d(Utils.TAG, this.getClass().getName() + " - itemSelected called. Type: " + type);
        if (adapter == null) {
            Log.w(Utils.TAG, "FeedHeadlineActivity: Adapter shouldn't be null here...");
            return;
        }
        
        // Find out if we are using a wide screen
        ListFragment secondPane = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.details);
        
        if (secondPane != null && secondPane.isInLayout()) {
            
            Log.d(Utils.TAG, "Filling right pane... (" + selectedIndex + " " + oldIndex + ")");
            
            // Set the list item as checked
            // getListView().setItemChecked(selectedIndex, true);
            
            // Get the fragment instance
            ArticleFragment articleView = (ArticleFragment) getSupportFragmentManager().findFragmentById(
                    R.id.articleView);
            
            // Is the current selected ondex the same as the clicked? If so, there is no need to update
            if (articleView != null && selectedIndex == oldIndex)
                return;
            
            articleView = ArticleFragment.newInstance(adapter.getId(selectedIndex), feedId, categoryId,
                    selectArticlesForCategory, ArticleActivity.ARTICLE_MOVE_DEFAULT);
            
            // Replace the old fragment with the new one
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.details, articleView);
            // Use a fade animation. This makes it clear that this is not a new "layer"
            // above the current, but a replacement
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
            
        } else {
            
            // This is not a tablet - start a new activity
            // if (!flingDetected) { // TODO: Think about what to do with the fling-gesture in a three-pane-layout.
            Intent i = new Intent(context, ArticleActivity.class);
            i.putExtra(ArticleActivity.ARTICLE_ID, adapter.getId(selectedIndex));
            i.putExtra(ArticleActivity.ARTICLE_FEED_ID, feedId);
            i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, categoryId);
            i.putExtra(FeedHeadlineActivity.FEED_SELECT_ARTICLES, selectArticlesForCategory);
            i.putExtra(ArticleActivity.ARTICLE_MOVE, ArticleActivity.ARTICLE_MOVE_DEFAULT);
            if (i != null)
                startActivity(i);
            // }
            
        }
    }
    
    public void onPublishNoteResult(Article a, String note) {
        new Updater(this, new PublishedStateUpdater(a, a.isPublished ? 0 : 1, note)).exec();
    }
    
    @Override
    protected void onDataChanged(int type) {
        if (type == UpdateController.TYPE_FEED)
            doRefresh();
    }
    
}
