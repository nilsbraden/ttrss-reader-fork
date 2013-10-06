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

import java.lang.reflect.Field;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.ProgressBarManager;
import org.ttrssreader.controllers.UpdateController;
import org.ttrssreader.gui.dialogs.ErrorDialog;
import org.ttrssreader.gui.interfaces.ICacheEndListener;
import org.ttrssreader.gui.interfaces.IDataChangedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.gui.interfaces.IUpdateEndListener;
import org.ttrssreader.imageCache.ForegroundService;
import org.ttrssreader.model.updaters.StateSynchronisationUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

/**
 * This class pulls common functionality from the three subclasses (CategoryActivity, FeedListActivity and
 * FeedHeadlineListActivity).
 */
public abstract class MenuActivity extends SherlockFragmentActivity implements IUpdateEndListener, ICacheEndListener,
        IItemSelectedListener, IDataChangedListener {
    
    protected final Context context = this;
    
    protected Updater updater;
    protected SherlockFragmentActivity activity;
    
    protected boolean isTablet = false;
    protected boolean isTabletVertical;
    
    public static final int MARK_GROUP = 42;
    public static final int MARK_READ = MARK_GROUP + 1;
    public static final int MARK_STAR = MARK_GROUP + 2;
    public static final int MARK_PUBLISH = MARK_GROUP + 3;
    public static final int MARK_PUBLISH_NOTE = MARK_GROUP + 4;
    public static final int MARK_ABOVE_READ = MARK_GROUP + 5;
    public static final int SHARE = MARK_GROUP + 6;
    public static final int UNSUBSCRIBE = MARK_GROUP + 7;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        activity = this;
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        
        Controller.getInstance().setHeadless(false);
        initActionbar();
        getOverflowMenu();
    }
    
    protected void initTabletLayout() {
        View horizontal = findViewById(R.id.frame_left);
        if (horizontal != null && horizontal.getVisibility() == View.VISIBLE) {
            isTablet = true;
            isTabletVertical = false;
        }
        View vertical = findViewById(R.id.frame_top);
        if (vertical != null && vertical.getVisibility() == View.VISIBLE) {
            isTablet = true;
            isTabletVertical = true;
        }
    }
    
    private TextView header_title;
    private TextView header_unread;
    
    private void initActionbar() {
        // Go to the CategoryActivity and clean the return-stack
        // getSupportActionBar().setHomeButtonEnabled(true);
        
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT);
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionbarView = inflator.inflate(R.layout.actionbar, null);
        
        ActionBar ab = getSupportActionBar();
        ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowCustomEnabled(true);
        ab.setDisplayShowTitleEnabled(false);
        ab.setCustomView(actionbarView, params);
        
        header_unread = (TextView) actionbarView.findViewById(R.id.head_unread);
        header_title = (TextView) actionbarView.findViewById(R.id.head_title);
        header_title.setText(getString(R.string.ApplicationName));
    }
    
    @Override
    public void setTitle(CharSequence title) {
        header_title.setText(title);
        super.setTitle(title);
    }
    
    public void setUnread(int unread) {
        if (unread > 0) {
            header_unread.setVisibility(View.VISIBLE);
        } else {
            header_unread.setVisibility(View.GONE);
        }
        header_unread.setText("( " + unread + " )");
    }
    
    /**
     * Force-display the three dots for overflow, would be disabled on devices with a menu-key.
     * 
     * @see http://stackoverflow.com/a/13098824
     */
    private void getOverflowMenu() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        initTabletLayout();
        UpdateController.getInstance().registerActivity(this);
        DBHelper.getInstance().checkAndInitializeDB(this);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        UpdateController.getInstance().unregisterActivity(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        getSupportMenuInflater().inflate(R.menu.generic, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        MenuItem offline = menu.findItem(R.id.Menu_WorkOffline);
        MenuItem refresh = menu.findItem(R.id.Menu_Refresh);
        if (offline != null) {
            if (Controller.getInstance().workOffline()) {
                offline.setTitle(getString(R.string.UsageOnlineTitle));
                offline.setIcon(R.drawable.ic_menu_play_clip);
                if (refresh != null)
                    menu.findItem(R.id.Menu_Refresh).setVisible(false);
            } else {
                offline.setTitle(getString(R.string.UsageOfflineTitle));
                offline.setIcon(R.drawable.ic_menu_stop);
                if (refresh != null)
                    menu.findItem(R.id.Menu_Refresh).setVisible(true);
            }
        }
        
        MenuItem displayUnread = menu.findItem(R.id.Menu_DisplayOnlyUnread);
        if (displayUnread != null) {
            if (Controller.getInstance().onlyUnread()) {
                displayUnread.setTitle(getString(R.string.Commons_DisplayAll));
            } else {
                displayUnread.setTitle(getString(R.string.Commons_DisplayOnlyUnread));
            }
        }
        
        if (!Controller.getInstance().markReadInMenu()) {
            // Hide button, show "Display Unread" instead which doesnt change any values on the server
            MenuItem markRead = menu.findItem(R.id.Menu_MarkAllRead);
            if (markRead != null) {
                markRead.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                if (displayUnread != null)
                    displayUnread.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            }
        }
        
        if (!(this instanceof FeedHeadlineActivity)) {
            menu.removeItem(R.id.Menu_FeedUnsubscribe);
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                // Go to the CategoryActivity and clean the return-stack
                Intent intent = new Intent(this, CategoryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
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
                invalidateOptionsMenu();
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
            case R.id.Menu_FeedSubscribe:
                startActivity(new Intent(this, SubscribeActivity.class));
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
        
        this.startService(intent);
        
        ProgressBarManager.getInstance().addProgress(this);
        setSupportProgressBarVisibility(true);
    }
    
    @Override
    public void onCacheEnd() {
        setSupportProgressBarVisibility(false);
        ProgressBarManager.getInstance().removeProgress(this);
    }
    
    @Override
    public void onCacheProgress(int taskCount, int progress) {
        setProgress((10000 / taskCount) * progress);
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
        setSupportProgressBarVisibility(false);
        ProgressBarManager.getInstance().resetProgress(this);
        Intent i = new Intent(this, ErrorActivity.class);
        i.putExtra(ErrorActivity.ERROR_MESSAGE, errorMessage);
        startActivityForResult(i, ErrorActivity.ACTIVITY_SHOW_ERROR);
    }
    
    protected void showErrorDialog(String message) {
        ErrorDialog.getInstance(this, message).show(getSupportFragmentManager(), "error");
    }
    
    protected void refreshAndUpdate() {
        initTabletLayout();
        if (Utils.checkConfig()) {
            doUpdate(false);
            doRefresh();
        }
    }
    
    @Override
    public void dataChanged() {
        doRefresh();
    }
    
    protected void doRefresh() {
        ProgressBarManager.getInstance().setIndeterminateVisibility(this);
        if (Controller.getInstance().getConnector().hasLastError())
            openConnectionErrorDialog(Controller.getInstance().getConnector().pullLastError());
    }
    
    protected abstract void doUpdate(boolean forceUpdate);
    
    /**
     * Can be used in child activities to update their data and get a UI refresh afterwards.
     */
    abstract class ActivityUpdater extends AsyncTask<Void, Integer, Void> {
        protected int taskCount = 0;
        protected boolean forceUpdate;
        
        public ActivityUpdater(boolean forceUpdate) {
            this.forceUpdate = forceUpdate;
            ProgressBarManager.getInstance().addProgress(activity);
            setSupportProgressBarVisibility(true);
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
    
}
