/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
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
import org.ttrssreader.controllers.UpdateController;
import org.ttrssreader.gui.interfaces.ICacheEndListener;
import org.ttrssreader.gui.interfaces.IConfigurable;
import org.ttrssreader.gui.interfaces.IDataChangedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.gui.interfaces.IUpdateEndListener;
import org.ttrssreader.imageCache.ForegroundService;
import org.ttrssreader.model.updaters.StateSynchronisationUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

/**
 * This class pulls common functionality from the three subclasses (CategoryActivity, FeedListActivity and
 * FeedHeadlineListActivity).
 */
public abstract class MenuActivity extends FragmentActivity implements IUpdateEndListener, ICacheEndListener,
        IConfigurable, IItemSelectedListener, IDataChangedListener {
    
    protected Updater updater;
    protected Context context = null;
    protected boolean isTablet = false;
    
    protected static final int MARK_GROUP = 42;
    protected static final int MARK_READ = MARK_GROUP + 1;
    protected static final int MARK_STAR = MARK_GROUP + 2;
    protected static final int MARK_PUBLISH = MARK_GROUP + 3;
    protected static final int MARK_PUBLISH_NOTE = MARK_GROUP + 4;
    protected static final int MARK_ABOVE_READ = MARK_GROUP + 5;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        
        context = getApplicationContext();
        
        // Initialize Singletons for Config, Data-Access and DB
        Controller.getInstance().checkAndInitializeController(this, getWindowManager().getDefaultDisplay());
        DBHelper.getInstance().checkAndInitializeDB(this);
        Data.getInstance().checkAndInitializeData(this);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Register to be notified when counters were updated
        UpdateController.getInstance().registerActivity(this);
        
        // Register for callback of the ImageCache
        Controller.getInstance().registerActivity(this);
        DBHelper.getInstance().checkAndInitializeDB(this);
        this.setVisible(true);
    }
    
    @Override
    protected void onRestart() {
        super.onRestart();
        this.setVisible(true);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        this.setVisible(false);
        Controller.getInstance().unregisterActivity(this);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        this.setVisible(false);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.setVisible(false);
        
        UpdateController.getInstance().unregisterActivity(this);
        
        if (updater != null) {
            updater.cancel(true);
            updater = null;
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == ErrorActivity.ACTIVITY_SHOW_ERROR) {
            refreshAndUpdate();
        } else if (resultCode == PreferencesActivity.ACTIVITY_SHOW_PREFERENCES) {
            refreshAndUpdate();
        } else if (resultCode == ErrorActivity.ACTIVITY_EXIT) {
            finish();
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(MARK_GROUP, MARK_READ, Menu.NONE, R.string.Commons_MarkRead);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.generic, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        MenuItem offline = menu.findItem(R.id.Menu_WorkOffline);
        if (Controller.getInstance().workOffline()) {
            offline.setTitle(getString(R.string.UsageOnlineTitle));
            offline.setIcon(R.drawable.ic_menu_play_clip);
        } else {
            offline.setTitle(getString(R.string.UsageOfflineTitle));
            offline.setIcon(R.drawable.ic_menu_stop);
        }
        
        MenuItem displayUnread = menu.findItem(R.id.Menu_DisplayOnlyUnread);
        if (Controller.getInstance().onlyUnread()) {
            displayUnread.setTitle(getString(R.string.Commons_DisplayAll));
        } else {
            displayUnread.setTitle(getString(R.string.Commons_DisplayOnlyUnread));
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.Menu_DisplayOnlyUnread:
                Controller.getInstance().setDisplayOnlyUnread(!Controller.getInstance().onlyUnread());
                doRefresh();
                return true;
            case R.id.Menu_InvertSort:
                if (this instanceof FeedHeadlineActivity) {
                    Controller.getInstance()
                            .setInvertSortArticleList(!Controller.getInstance().invertSortArticlelist());
                } else {
                    Controller.getInstance().setInvertSortFeedsCats(!Controller.getInstance().invertSortFeedscats());
                }
                doRefresh();
                return true;
            case R.id.Menu_WorkOffline:
                Controller.getInstance().setWorkOffline(!Controller.getInstance().workOffline());
                if (!Controller.getInstance().workOffline()) {
                    // Synchronize status of articles with server
                    new Updater(this, new StateSynchronisationUpdater()).exec();
                }
                doRefresh();
                return true;
            case R.id.Menu_ShowPreferences:
                startActivityForResult(new Intent(this, PreferencesActivity.class),
                        PreferencesActivity.ACTIVITY_SHOW_PREFERENCES);
                return true;
            case R.id.Menu_About:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.Category_Menu_ImageCache:
                doCache(false);
                return true;
            default:
                return false;
        }
    }
    
    @Override
    public void onUpdateEnd() {
        updater = null;
    }
    
    /* ############# BEGIN: Cache */
    protected void doCache(boolean onlyArticles) {
        // Register for progress-updates
        ForegroundService.registerCallback(this);
        
        if (isCacherRunning()) {
            if (!onlyArticles) // Tell cacher to do images too
                ForegroundService.loadImagesToo();
            else
                // Running and already caching images, no need to do anything
                return;
        }
        
        // Start new cacher
        Intent intent;
        if (onlyArticles) {
            intent = new Intent(ForegroundService.ACTION_LOAD_ARTICLES);
        } else {
            intent = new Intent(ForegroundService.ACTION_LOAD_IMAGES);
        }
        intent.setClass(this.getApplicationContext(), ForegroundService.class);
        
        setProgressBarVisibility(true);
        this.startService(intent);
    }
    
    @Override
    public void onCacheEnd() {
        setProgressBarVisibility(false);
    }
    
    @Override
    public void onCacheProgress(int taskCount, int progress) {
        if (taskCount == progress) {
            setProgressBarIndeterminateVisibility(false);
            setProgressBarVisibility(false);
        } else {
            setProgress((10000 / (taskCount + 1)) * progress);
        }
    }
    
    protected boolean isCacherRunning() {
        return ForegroundService.isInstanceCreated();
    }
    
    /* ############# END: Cache */
    
    protected void openConnectionErrorDialog(String errorMessage) {
        if (updater != null) {
            updater.cancel(true);
            updater = null;
        }
        setProgressBarIndeterminateVisibility(false);
        Intent i = new Intent(this, ErrorActivity.class);
        i.putExtra(ErrorActivity.ERROR_MESSAGE, errorMessage);
        startActivityForResult(i, ErrorActivity.ACTIVITY_SHOW_ERROR);
        // finish();
    }
    
    protected void refreshAndUpdate() {
        if (Utils.checkConfig()) {
            doRefresh();
            doUpdate();
        }
    }
    
    @Override
    public void dataChanged() {
        doRefresh();
    }
    
    protected abstract void doRefresh();
    
    protected abstract void doUpdate();
    
    protected abstract void onDataChanged();
    
}
