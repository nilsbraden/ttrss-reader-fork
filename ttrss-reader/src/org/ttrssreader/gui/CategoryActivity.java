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

import java.util.Set;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.controllers.ProgressBarManager;
import org.ttrssreader.gui.dialogs.ChangelogDialog;
import org.ttrssreader.gui.dialogs.CrashreportDialog;
import org.ttrssreader.gui.dialogs.VacuumDialog;
import org.ttrssreader.gui.dialogs.WelcomeDialog;
import org.ttrssreader.gui.fragments.CategoryListFragment;
import org.ttrssreader.gui.fragments.FeedHeadlineListFragment;
import org.ttrssreader.gui.fragments.FeedListFragment;
import org.ttrssreader.model.MainAdapter;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.TopExceptionHandler;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Log;
import com.actionbarsherlock.view.MenuItem;

public class CategoryActivity extends MenuActivity {
    
    private static final String DIALOG_WELCOME = "welcome";
    private static final String DIALOG_UPDATE = "update";
    private static final String DIALOG_CRASH = "crash";
    private static final String DIALOG_VACUUM = "vacuum";
    
    private static final int SELECTED_VIRTUAL_CATEGORY = 1;
    private static final int SELECTED_CATEGORY = 2;
    private static final int SELECTED_LABEL = 3;
    
    public static final String FRAGMENT = "CATEGORY_FRAGMENT";
    
    private String applicationName = null;
    public boolean cacherStarted = false;
    
    private CategoryUpdater categoryUpdater = null;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        setContentView(R.layout.categorylist);
        
        // Register our own ExceptionHander
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));
        
        // Delete DB if requested
        Controller.getInstance().setDeleteDBScheduled(Controller.getInstance().isDeleteDBOnStartup());
        
        FragmentManager fm = getSupportFragmentManager();
        
        if (fm.findFragmentByTag(FRAGMENT) == null) {
            Fragment fragment = CategoryListFragment.newInstance();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.category_list, fragment, FRAGMENT);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.commit();
        }
        
        if (!Utils.checkFirstRun(this)) {
            WelcomeDialog.getInstance().show(fm, DIALOG_WELCOME);
        } else if (!Utils.checkNewVersion(this)) {
            ChangelogDialog.getInstance().show(fm, DIALOG_UPDATE);
        } else if (!Utils.checkCrashReport(this)) {
            CrashreportDialog.getInstance().show(fm, DIALOG_CRASH);
        } else if (Utils.checkVacuumDB(this)) {
            VacuumDialog.getInstance().show(fm, DIALOG_VACUUM);
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
    protected void onResume() {
        super.onResume();
        refreshAndUpdate();
    }
    
    @Override
    protected void doRefresh() {
        super.doRefresh();
        if (applicationName == null)
            applicationName = getResources().getString(R.string.ApplicationName);
        int unreadCount = DBHelper.getInstance().getUnreadCount(Data.VCAT_ALL, true);
        setTitle(MainAdapter.formatTitle(applicationName, unreadCount));
        
        doRefreshFragment(getSupportFragmentManager().findFragmentById(R.id.category_list));
        doRefreshFragment(getSupportFragmentManager().findFragmentById(R.id.feed_list));
        doRefreshFragment(getSupportFragmentManager().findFragmentById(R.id.headline_list));
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
    public final boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.Menu_Refresh:
                doUpdate(true);
                return true;
            case R.id.Menu_MarkAllRead:
                new Updater(this, new ReadStateUpdater(DBHelper.getInstance().getCategoriesIncludingUncategorized()))
                        .exec();
                return true;
            default:
                return false;
        }
    }
    
    public class CategoryUpdater extends AsyncTask<Void, Integer, Void> {
        
        private int taskCount = 0;
        private static final int DEFAULT_TASK_COUNT = 5;
        private boolean forceUpdate;
        
        public CategoryUpdater(boolean forceUpdate) {
            this.forceUpdate = forceUpdate;
            ProgressBarManager.getInstance().addProgress(activity);
            setSupportProgressBarVisibility(true);
        }
        
        @Override
        protected Void doInBackground(Void... params) {
            boolean onlyUnreadArticles = Controller.getInstance().onlyUnread();
            
            Set<Feed> labels = DBHelper.getInstance().getFeeds(-2);
            taskCount = DEFAULT_TASK_COUNT + labels.size() + 1; // 1 for the caching of all articles
            
            int progress = 0;
            publishProgress(++progress); // Move progress forward
            
            Data.getInstance().updateCounters(false, forceUpdate);
            
            // Cache articles for all categories
            publishProgress(++progress);
            Data.getInstance().cacheArticles(false, forceUpdate);
            
            // Refresh articles for all labels
            for (Feed f : labels) {
                if (f.unread == 0 && onlyUnreadArticles)
                    continue;
                publishProgress(++progress);
                Data.getInstance().updateArticles(f.id, false, false, false, forceUpdate);
            }
            
            publishProgress(++progress); // Move progress to 100%
            
            // This stuff will be done in background without UI-notification, but the progress-calls will be done anyway
            // to ensure the UI is refreshed properly.
            Data.getInstance().updateVirtualCategories();
            publishProgress(++progress);
            Data.getInstance().updateCategories(false);
            publishProgress(++progress);
            Data.getInstance().updateFeeds(Data.VCAT_ALL, false);
            publishProgress(taskCount); // Move progress forward to 100%
            
            // Silently try to synchronize any ids left in TABLE_MARK
            Data.getInstance().synchronizeStatus();
            return null;
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values[0] == taskCount) {
                setSupportProgressBarVisibility(false);
                if (!isCacherRunning())
                    ProgressBarManager.getInstance().removeProgress(activity);
                return;
            }
            setProgress((10000 / (taskCount + 1)) * values[0]);
        }
    }
    
    @Override
    public void itemSelected(TYPE type, int selectedIndex, int oldIndex, int selectedId) {
        // Who is calling?
        switch (type) {
            case CATEGORY:
                Log.d(Utils.TAG, "CATEGORY selected. Index: " + selectedIndex);
                break;
            case FEED:
                Log.d(Utils.TAG, "FEED selected. Index: " + selectedIndex);
                break;
            case FEEDHEADLINE:
                Log.d(Utils.TAG, "FEEDHEADLINE selected. Index: " + selectedIndex);
                break;
            case NONE:
                break;
        }
        
        // Decide what kind of item was selected
        final int selection;
        if (selectedId < 0 && selectedId >= -4) {
            selection = SELECTED_VIRTUAL_CATEGORY;
        } else if (selectedId < -10) {
            selection = SELECTED_LABEL;
        } else {
            selection = SELECTED_CATEGORY;
        }
        
        // Find out if we are using a wide screen
        ListFragment secondPane = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.details);
        if (secondPane != null && secondPane.isInLayout()) {
            // Set the list item as checked
            // getListView().setItemChecked(selectedIndex, true);
            
            // Is the current selected ondex the same as the clicked? If so, there is no need to update
            // if (details != null && selectedIndex == oldIndex)
            // return;
            
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            FeedHeadlineListFragment feedHeadlineFragment = null;
            FeedListFragment feedFragment = null;
            
            switch (selection) {
                case SELECTED_VIRTUAL_CATEGORY:
                    feedHeadlineFragment = FeedHeadlineListFragment.newInstance(selectedId, 0, false);
                    ft.replace(R.id.headline_list, feedHeadlineFragment);
                    break;
                case SELECTED_LABEL:
                    feedHeadlineFragment = FeedHeadlineListFragment.newInstance(selectedId, -2, false);
                    ft.replace(R.id.headline_list, feedHeadlineFragment);
                    break;
                case SELECTED_CATEGORY:
                    feedFragment = FeedListFragment.newInstance(selectedId);
                    ft.replace(R.id.feed_list, feedFragment);
                    break;
            }
            
            // Replace the old fragment with the new one
            // Use a fade animation. This makes it clear that this is not a new "layer"
            // above the current, but a replacement
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
            
        } else {
            Intent i = null;
            switch (selection) {
                case SELECTED_VIRTUAL_CATEGORY:
                    i = new Intent(context, FeedHeadlineActivity.class);
                    i.putExtra(FeedHeadlineActivity.FEED_ID, selectedId);
                    break;
                case SELECTED_LABEL:
                    i = new Intent(context, FeedHeadlineActivity.class);
                    i.putExtra(FeedHeadlineActivity.FEED_ID, selectedId);
                    i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, -2);
                    break;
                case SELECTED_CATEGORY:
                    i = new Intent(context, FeedActivity.class);
                    i.putExtra(FeedActivity.FEED_CAT_ID, selectedId);
                    break;
            }
            startActivity(i);
        }
    }
    
    @Override
    protected void onDataChanged() {
        doRefresh();
    }
    
}
