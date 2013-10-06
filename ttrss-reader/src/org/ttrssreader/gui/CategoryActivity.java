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
import org.ttrssreader.gui.dialogs.ChangelogDialog;
import org.ttrssreader.gui.dialogs.CrashreportDialog;
import org.ttrssreader.gui.dialogs.WelcomeDialog;
import org.ttrssreader.gui.fragments.ArticleFragment;
import org.ttrssreader.gui.fragments.CategoryListFragment;
import org.ttrssreader.gui.fragments.FeedHeadlineListFragment;
import org.ttrssreader.gui.fragments.FeedListFragment;
import org.ttrssreader.gui.fragments.MainListFragment;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
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
import android.util.Log;
import android.widget.Toast;
import com.actionbarsherlock.view.MenuItem;

public class CategoryActivity extends MenuActivity implements IItemSelectedListener {
    
    private static final String DIALOG_WELCOME = "welcome";
    private static final String DIALOG_UPDATE = "update";
    private static final String DIALOG_CRASH = "crash";
    
    private static final int SELECTED_VIRTUAL_CATEGORY = 1;
    private static final int SELECTED_CATEGORY = 2;
    private static final int SELECTED_LABEL = 3;
    
    private static final String FRAGMENT = "CATEGORY_FRAGMENT";
    
    private boolean cacherStarted = false;
    
    private CategoryUpdater categoryUpdater = null;
    
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
        
        // Register our own ExceptionHander
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));
        
        FragmentManager fm = getSupportFragmentManager();
        
        if (fm.findFragmentByTag(FRAGMENT) == null) {
            int targetLayout = R.id.list;
            if (isTabletVertical)
                targetLayout = R.id.frame_top;
            else if (isTablet)
                targetLayout = R.id.frame_left;
            Fragment fragment = CategoryListFragment.newInstance();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(targetLayout, fragment, FRAGMENT);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.commit();
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
    protected void onResume() {
        super.onResume();
        refreshAndUpdate();
    }
    
    @Override
    protected void doRefresh() {
        super.doRefresh();
        Utils.doRefreshFragment(getSupportFragmentManager().findFragmentById(R.id.list));
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
                new Updater(this, new ReadStateUpdater(ReadStateUpdater.TYPE.ALL_CATEGORIES)).exec();
                return true;
            default:
                return false;
        }
    }
    
    /**
     * This does a full update including all labels, feeds, categories and all articles.
     */
    public class CategoryUpdater extends ActivityUpdater {
        private static final int DEFAULT_TASK_COUNT = 5;
        
        public CategoryUpdater(boolean forceUpdate) {
            super(forceUpdate);
        }
        
        @Override
        protected Void doInBackground(Void... params) {
            boolean onlyUnreadArticles = Controller.getInstance().onlyUnread();
            
            Set<Feed> labels = DBHelper.getInstance().getFeeds(-2);
            taskCount = DEFAULT_TASK_COUNT + labels.size() + 1; // 1 for the caching of all articles
            
            int progress = 0;
            publishProgress(++progress); // Move progress forward
            
            // Data.getInstance().updateCounters(false, forceUpdate);
            
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
    }
    
    @Override
    public void itemSelected(MainListFragment source, int selectedIndex, int oldIndex, int selectedId) {
        Log.d(Utils.TAG, "itemSelected in CategoryActivity");
        
        if (isTablet) {
            // Set the list item as checked
            // getListView().setItemChecked(selectedIndex, true);
            
            // Is the current selected ondex the same as the clicked? If so, there is no need to update
            // if (selectedIndex == oldIndex)
            // return;
            
            int newTargetLayout;
            Fragment newFragment = null;
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            
            switch (source.getType()) {
                case CATEGORY:
                    // Show Feeds or headlines in middle or bottom frame
                    newTargetLayout = isTabletVertical ? R.id.frame_bottom : R.id.frame_middle;
                    newFragment = decideFragment(selectedId);
                    ft.replace(newTargetLayout, newFragment);
                    ft.addToBackStack(null);
                    
                    // Display empty fragment in right frame:
                    if (!isTabletVertical)
                        ft.replace(R.id.frame_right,
                                FeedHeadlineListFragment.newInstance(Integer.MIN_VALUE, Integer.MIN_VALUE, false));
                    
                    break;
                case FEED:
                    // Show Headlines in right or bottom frame, show feeds in top-frame when displaying vertical layout
                    FeedListFragment feeds = (FeedListFragment) source;
                    
                    newTargetLayout = isTabletVertical ? R.id.frame_bottom : R.id.frame_right;
                    newFragment = FeedHeadlineListFragment.newInstance(selectedId, feeds.getCategoryId(), false);
                    ft.replace(newTargetLayout, newFragment);
                    ft.addToBackStack(null);
                    
                    if (isTabletVertical)
                        ft.replace(R.id.frame_top, FeedListFragment.newInstance(feeds));
                    
                    break;
                case FEEDHEADLINE:
                    // Show content in middle+rigth frame when displaying horizontal layout
                    // Show content in bottom frame and headlines in top frame when displaying vertical layout
                    FeedHeadlineListFragment headlines = (FeedHeadlineListFragment) source;
                    
                    if (isTabletVertical) {
                        newTargetLayout = isTabletVertical ? R.id.frame_bottom : R.id.frame_right;
                        newFragment = ArticleFragment.newInstance(selectedId, headlines.getFeedId(),
                                headlines.getCategoryId(), headlines.getSelectArticlesForCategory(),
                                ArticleFragment.ARTICLE_MOVE_DEFAULT);
                        ft.replace(newTargetLayout, newFragment);
                        ft.replace(R.id.frame_top, FeedHeadlineListFragment.newInstance(headlines));
                    } else {
                        // When using 3-column-layout we display headlines+article in a separate layout with only two
                        // columns, managed by FeedHeadlineActivity:
                        Intent i = new Intent(context, FeedHeadlineActivity.class);
                        i.putExtra(FeedHeadlineListFragment.FEED_CAT_ID, headlines.getCategoryId());
                        i.putExtra(FeedHeadlineListFragment.FEED_ID, selectedId);
                        i.putExtra(FeedHeadlineListFragment.ARTICLE_ID, selectedId);
                        startActivity(i);
                    }
                    
                    break;
                default:
                    Toast.makeText(this, "Invalid request!", Toast.LENGTH_SHORT).show();
                    break;
            }
            
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
            
        } else {
            
            // Non-Tablet behaviour:
            Intent i = null;
            switch (decideCategorySelection(selectedId)) {
                case SELECTED_VIRTUAL_CATEGORY:
                    i = new Intent(context, FeedHeadlineActivity.class);
                    i.putExtra(FeedHeadlineListFragment.FEED_ID, selectedId);
                    break;
                case SELECTED_LABEL:
                    i = new Intent(context, FeedHeadlineActivity.class);
                    i.putExtra(FeedHeadlineListFragment.FEED_ID, selectedId);
                    i.putExtra(FeedHeadlineListFragment.FEED_CAT_ID, -2);
                    break;
                case SELECTED_CATEGORY:
                    if (Controller.getInstance().invertBrowsing()) {
                        i = new Intent(context, FeedHeadlineActivity.class);
                        i.putExtra(FeedHeadlineListFragment.FEED_ID, FeedHeadlineActivity.FEED_NO_ID);
                        i.putExtra(FeedHeadlineListFragment.FEED_CAT_ID, selectedId);
                        i.putExtra(FeedHeadlineListFragment.FEED_SELECT_ARTICLES, true);
                    } else {
                        i = new Intent(context, FeedActivity.class);
                        i.putExtra(FeedListFragment.FEED_CAT_ID, selectedId);
                    }
                    break;
            }
            startActivity(i);
            
        }
    }
    
    private Fragment decideFragment(int selectedId) {
        switch (decideCategorySelection(selectedId)) {
            case SELECTED_VIRTUAL_CATEGORY:
                return FeedHeadlineListFragment.newInstance(selectedId, 0, false);
            case SELECTED_LABEL:
                return FeedHeadlineListFragment.newInstance(selectedId, -2, false);
            case SELECTED_CATEGORY:
                if (Controller.getInstance().invertBrowsing()) {
                    return FeedHeadlineListFragment.newInstance(FeedHeadlineActivity.FEED_NO_ID, selectedId, true);
                } else {
                    return FeedListFragment.newInstance(selectedId);
                }
            default:
                return null;
        }
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
}
