/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
 * Copyright (C) 2010 F. Bechstein.
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

import java.util.LinkedHashSet;
import java.util.Set;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.dialogs.ChangelogDialog;
import org.ttrssreader.gui.dialogs.CrashreportDialog;
import org.ttrssreader.gui.dialogs.WelcomeDialog;
import org.ttrssreader.gui.dialogs.YesNoUpdaterDialog;
import org.ttrssreader.gui.fragments.CategoryListFragment;
import org.ttrssreader.gui.fragments.FeedHeadlineListFragment;
import org.ttrssreader.gui.fragments.FeedListFragment;
import org.ttrssreader.gui.fragments.MainListFragment;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.updaters.IUpdatable;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class CategoryActivity extends MenuActivity implements IItemSelectedListener {
    
    private static final String DIALOG_WELCOME = "welcome";
    private static final String DIALOG_UPDATE = "update";
    private static final String DIALOG_CRASH = "crash";
    
    private static final int SELECTED_VIRTUAL_CATEGORY = 1;
    private static final int SELECTED_CATEGORY = 2;
    private static final int SELECTED_LABEL = 3;
    
    private boolean cacherStarted = false;
    private CategoryUpdater categoryUpdater = null;
    
    private static final String SELECTED = "SELECTED";
    private int selectedCategoryId = Integer.MIN_VALUE;
    
    private CategoryListFragment categoryFragment;
    private FeedListFragment feedFragment;
    
    @Override
    protected void onCreate(Bundle instance) {
        // Only needed to debug ANRs:
        // StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectCustomSlowCalls().detectDiskReads()
        // .detectDiskWrites().detectNetwork().penaltyLog().penaltyLog().build());
        // StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects()
        // .detectLeakedClosableObjects().penaltyLog().build());
        
        super.onCreate(instance);
        setContentView(R.layout.categorylist);
        super.initTabletLayout();
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            selectedCategoryId = extras.getInt(SELECTED, Integer.MIN_VALUE);
        } else if (instance != null) {
            selectedCategoryId = instance.getInt(SELECTED, Integer.MIN_VALUE);
        }
        
        FragmentManager fm = getSupportFragmentManager();
        categoryFragment = (CategoryListFragment) fm.findFragmentByTag(CategoryListFragment.FRAGMENT);
        feedFragment = (FeedListFragment) fm.findFragmentByTag(FeedListFragment.FRAGMENT);
        
        Fragment oldFeedFragment = feedFragment;
        
        // Handle orientation changes here, especially the case when the user switches from 2-pane-landscape to protrait
        // mode and hasn't enough space to display two panels. Also see
        // http://www.androiddesignpatterns.com/2013/08/fragment-transaction-commit-state-loss.html for explanations of
        // when onRestoreInstanceState is called and why FragmentTransaction.commit() can crash if called within
        // onStart().
        if (feedFragment != null && !Controller.isTablet) {
            feedFragment = (FeedListFragment) MainListFragment.recreateFragment(fm, feedFragment);
            // No Tablet mode but Feeds have been loaded, we have just one pane: R.id.frame_left
            
            removeOldFragment(fm, oldFeedFragment); // See http://stackoverflow.com/a/13395157
            
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.frame_left, feedFragment, FeedListFragment.FRAGMENT);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
        
        if (categoryFragment == null) {
            categoryFragment = CategoryListFragment.newInstance();
            
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.frame_left, categoryFragment, CategoryListFragment.FRAGMENT);
            
            if (feedFragment != null && Controller.isTablet && selectedCategoryId != Integer.MIN_VALUE) {
                feedFragment = (FeedListFragment) MainListFragment.recreateFragment(fm, feedFragment);
                removeOldFragment(fm, oldFeedFragment);
                ft.add(R.id.frame_right, feedFragment, FeedListFragment.FRAGMENT);
            }
            
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
        
        if (!Utils.checkFirstRun(this)) {
            WelcomeDialog.getInstance().show(fm, DIALOG_WELCOME);
        } else if (!Utils.checkNewVersion(this)) {
            ChangelogDialog.getInstance().show(fm, DIALOG_UPDATE);
        } else if (!Utils.checkCrashReport(this)) {
            CrashreportDialog.getInstance().show(fm, DIALOG_CRASH);
        } else if (!Utils.checkConfig()) {
            // Check if we have a server specified
            openConnectionErrorDialog((String) getText(R.string.CategoryActivity_NoServer));
        }
        
        // Start caching if requested
        if (Controller.getInstance().cacheImagesOnStartup()) {
            boolean startCache = true;
            
            if (Controller.getInstance().cacheImagesOnlyWifi()) {
                // Check if Wifi is connected, if not don't start the ImageCache
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                
                if (!mWifi.isConnected()) {
                    Log.i(Utils.TAG, "Preference Start ImageCache only on WIFI set, doing nothing...");
                    startCache = false;
                }
            }
            
            // Indicate that the cacher started anyway so the refresh is supressed if the ImageCache is configured but
            // only for Wifi.
            cacherStarted = true;
            
            if (startCache) {
                Log.i(Utils.TAG, "Starting ImageCache...");
                doCache(false); // images
            }
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED, selectedCategoryId);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle instance) {
        selectedCategoryId = instance.getInt(SELECTED, Integer.MIN_VALUE);
        super.onRestoreInstanceState(instance);
    }
    
    @Override
    public void dataLoadingFinished() {
        setTitleAndUnread();
    }
    
    @Override
    protected void doRefresh() {
        super.doRefresh();
        if (categoryFragment != null)
            categoryFragment.doRefresh();
        if (feedFragment != null)
            feedFragment.doRefresh();
        
        setTitleAndUnread();
    }
    
    private void setTitleAndUnread() {
        // Title and unread information:
        if (feedFragment != null) {
            setTitle(feedFragment.getTitle());
            setUnread(feedFragment.getUnread());
        } else if (categoryFragment != null) {
            setTitle(categoryFragment.getTitle());
            setUnread(categoryFragment.getUnread());
        }
    }
    
    @Override
    protected void doUpdate(boolean forceUpdate) {
        // Only update if no categoryUpdater already running
        if (categoryUpdater != null) {
            if (categoryUpdater.getStatus().equals(AsyncTask.Status.FINISHED)) {
                categoryUpdater = null;
            } else {
                return;
            }
        }
        
        if ((!isCacherRunning() && !cacherStarted) || forceUpdate) {
            categoryUpdater = new CategoryUpdater(forceUpdate);
            categoryUpdater.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean ret = super.onPrepareOptionsMenu(menu);
        if (!Controller.isTablet && selectedCategoryId != Integer.MIN_VALUE)
            menu.removeItem(R.id.Menu_MarkAllRead);
        if (selectedCategoryId == Integer.MIN_VALUE)
            menu.removeItem(R.id.Menu_MarkFeedsRead);
        // Always:
        menu.removeItem(R.id.Menu_MarkFeedRead);
        return ret;
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        if (super.onOptionsItemSelected(item))
            return true;
        
        switch (item.getItemId()) {
            case R.id.Menu_Refresh: {
                doUpdate(true);
                return true;
            }
            case R.id.Menu_MarkAllRead: {
                boolean backAfterUpdate = Controller.getInstance().goBackAfterMarkAllRead();
                IUpdatable updater = new ReadStateUpdater(ReadStateUpdater.TYPE.ALL_CATEGORIES);
                YesNoUpdaterDialog dialog = YesNoUpdaterDialog.getInstance(this, updater, R.string.Dialog_Title,
                        R.string.Dialog_MarkAllRead, backAfterUpdate);
                dialog.show(getSupportFragmentManager(), YesNoUpdaterDialog.DIALOG);
                return true;
            }
            case R.id.Menu_MarkFeedsRead:
                if (selectedCategoryId > Integer.MIN_VALUE) {
                    boolean backAfterUpdate = Controller.getInstance().goBackAfterMarkAllRead();
                    IUpdatable updateable = new ReadStateUpdater(selectedCategoryId);
                    
                    YesNoUpdaterDialog dialog = YesNoUpdaterDialog.getInstance(this, updateable, R.string.Dialog_Title,
                            R.string.Dialog_MarkFeedsRead, backAfterUpdate);
                    dialog.show(getSupportFragmentManager(), YesNoUpdaterDialog.DIALOG);
                }
                return true;
            default:
                return false;
        }
    }
    
    /**
     * This does a full update including all labels, feeds, categories and all articles.
     */
    public class CategoryUpdater extends ActivityUpdater {
        private static final int DEFAULT_TASK_COUNT = 4;
        
        public CategoryUpdater(boolean forceUpdate) {
            super(forceUpdate);
        }
        
        @Override
        protected Void doInBackground(Void... params) {
            long time = System.currentTimeMillis();
            boolean onlyUnreadArticles = Controller.getInstance().onlyUnread();
            
            Set<Feed> labels = new LinkedHashSet<Feed>();
            for (Feed f : DBHelper.getInstance().getFeeds(-2)) {
                if (f.unread == 0 && onlyUnreadArticles)
                    continue;
                labels.add(f);
            }
            
            taskCount = DEFAULT_TASK_COUNT + labels.size(); // 1 for the caching of all articles
            int progress = 0;
            publishProgress(progress);
            
            // Cache articles for all categories
            Data.getInstance().cacheArticles(false, forceUpdate);
            publishProgress(++progress);
            
            // Refresh articles for all labels
            for (Feed f : labels) {
                Data.getInstance().updateArticles(f.id, false, false, false, forceUpdate);
                publishProgress(++progress);
            }
            
            // This stuff will be done in background without UI-notification, but the progress-calls will be done anyway
            // to ensure the UI is refreshed properly.
            Data.getInstance().updateVirtualCategories();
            publishProgress(++progress);
            
            Data.getInstance().updateCategories(false);
            publishProgress(++progress);
            
            Data.getInstance().updateFeeds(Data.VCAT_ALL, false);
            publishProgress(taskCount); // Move progress forward to 100%
            System.out.println("Time 1: " + (System.currentTimeMillis() - time) + "ms");
            
            // Silently try to synchronize any ids left in TABLE_MARK:
            Data.getInstance().synchronizeStatus();
            
            // Silently remove articles which belongs to feeds which do not exist on the server anymore:
            DBHelper.getInstance().purgeOrphanedArticles();
            
            System.out.println("Time 2: " + (System.currentTimeMillis() - time) + "ms");
            return null;
        }
    }
    
    @Override
    public void itemSelected(MainListFragment source, int selectedIndex, int selectedId) {
        switch (source.getType()) {
            case CATEGORY:
                switch (decideCategorySelection(selectedId)) {
                    case SELECTED_VIRTUAL_CATEGORY:
                        displayHeadlines(selectedId, 0, false);
                        break;
                    case SELECTED_LABEL:
                        displayHeadlines(selectedId, -2, false);
                        break;
                    case SELECTED_CATEGORY:
                        if (Controller.getInstance().invertBrowsing()) {
                            displayHeadlines(FeedHeadlineActivity.FEED_NO_ID, selectedId, true);
                        } else {
                            displayFeed(selectedId);
                        }
                        break;
                }
                break;
            case FEED:
                FeedListFragment feeds = (FeedListFragment) source;
                displayHeadlines(selectedId, feeds.getCategoryId(), false);
                break;
            default:
                Toast.makeText(this, "Invalid request!", Toast.LENGTH_SHORT).show();
                break;
        }
    }
    
    public void displayHeadlines(int feedId, int categoryId, boolean selectArticles) {
        Intent i = new Intent(this, FeedHeadlineActivity.class);
        i.putExtra(FeedHeadlineListFragment.FEED_CAT_ID, categoryId);
        i.putExtra(FeedHeadlineListFragment.FEED_ID, feedId);
        i.putExtra(FeedHeadlineListFragment.FEED_SELECT_ARTICLES, selectArticles);
        i.putExtra(FeedHeadlineListFragment.ARTICLE_ID, Integer.MIN_VALUE);
        startActivity(i);
    }
    
    public void displayFeed(int categoryId) {
        selectedCategoryId = categoryId;
        
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        int targetLayout = R.id.frame_right;
        if (!Controller.isTablet) {
            targetLayout = R.id.frame_left;
            ft.addToBackStack(null);
        }
        
        feedFragment = FeedListFragment.newInstance(categoryId);
        ft.replace(targetLayout, feedFragment, FeedListFragment.FRAGMENT);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }
    
    private static int decideCategorySelection(int selectedId) {
        if (selectedId < 0 && selectedId >= -4) {
            return SELECTED_VIRTUAL_CATEGORY;
        } else if (selectedId < -10) {
            return SELECTED_LABEL;
        } else {
            return SELECTED_CATEGORY;
        }
    }
    
    @Override
    public void onBackPressed() {
        if (!Controller.isTablet) {
            FragmentManager fm = getSupportFragmentManager();
            feedFragment = (FeedListFragment) fm.findFragmentByTag(FeedListFragment.FRAGMENT);
            if (feedFragment != null)
                fm.beginTransaction().remove(feedFragment).commit();
        }
        
        selectedCategoryId = Integer.MIN_VALUE;
        feedFragment = null;
        super.onBackPressed();
    }
    
}
