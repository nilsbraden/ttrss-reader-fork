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
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MotionEventCompat;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout.LayoutParams;
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
    protected boolean isVertical;
    protected static int minListSize;
    protected static int maxListSize;
    
    private View divider = null;
    private View dividerLayout = null;
    private View primaryFrame = null;
    private View secondaryFrame = null;
    private TextView header_title;
    private TextView header_unread;
    
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
        primaryFrame = findViewById(R.id.frame_left);
        secondaryFrame = findViewById(R.id.frame_right);
        dividerLayout = findViewById(R.id.list_divider_layout);
        divider = findViewById(R.id.list_divider);
        
        // Initialize values for layout changes:
        Controller
                .refreshDisplayMetrics(((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay());
        isVertical = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
        int size = Controller.displayWidth;
        if (isVertical) {
            TypedValue tv = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true);
            int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);
            size = Controller.displayHeight - actionBarHeight;
        }
        
        minListSize = (int) (size * 0.15);
        maxListSize = size - (int) (size * 0.15);
        
        if (Controller.getInstance().allowTabletLayout()) {
            if (secondaryFrame != null) {
                Controller.isTablet = true;
                dividerLayout.setVisibility(View.VISIBLE);
            } else {
                Controller.isTablet = false;
            }
        } else {
            // Tablet-layout is deactivated, hide second view and divider:
            Controller.isTablet = false;
            if (secondaryFrame != null)
                secondaryFrame.setVisibility(View.GONE);
            if (dividerLayout != null)
                dividerLayout.setVisibility(View.GONE);
        }
        
        // Resize frames and do it only if stored size is within our bounds:
        int newSize = Controller.getInstance().getViewSize(this, isVertical);
        if (newSize > minListSize && newSize < maxListSize) {
            if (isVertical)
                primaryFrame.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, newSize));
            else
                primaryFrame.setLayoutParams(new LayoutParams(newSize, LayoutParams.MATCH_PARENT));
            getWindow().getDecorView().getRootView().invalidate();
        }
    }
    
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
        if (super.onOptionsItemSelected(item))
            return true;
        
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
                supportInvalidateOptionsMenu();
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
        supportInvalidateOptionsMenu();
        doRefresh();
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
    
    protected static void removeOldFragment(FragmentManager fm, Fragment fragment) {
        FragmentTransaction ft = fm.beginTransaction();
        ft.remove(fragment);
        ft.commit();
        fm.executePendingTransactions();
    }
    
    // The ‘active pointer’ is the one currently moving our object.
    private static final int INVALID_POINTER_ID = -1;
    private int mActivePointerId = INVALID_POINTER_ID;
    
    private float mLastTouchX = 0;
    private float mLastTouchY = 0;
    private int mDeltaX = 0;
    private int mDeltaY = 0;
    
    private boolean resizing = false;
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!Controller.isTablet)
            return false;
        
        // Only handle events when the list-divider is selected or we are already resizing:
        View view = findViewAtPosition(getWindow().getDecorView().getRootView(), (int) ev.getRawX(), (int) ev.getRawY());
        if (view == null && !resizing)
            return false;
        
        if (view != null)
            if (view.getId() != R.id.list_divider && view.getId() != R.id.list_divider_layout)
                return false;
        
        final int action = MotionEventCompat.getActionMasked(ev);
        
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                divider.setSelected(true);
                resizing = true;
                final int pointerIndex = MotionEventCompat.getActionIndex(ev);
                
                // Remember where we started (for dragging)
                mLastTouchX = MotionEventCompat.getX(ev, pointerIndex);
                mLastTouchY = MotionEventCompat.getY(ev, pointerIndex);
                mDeltaX = 0;
                mDeltaY = 0;
                
                // Save the ID of this pointer (for dragging)
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                break;
            }
            
            case MotionEvent.ACTION_MOVE: {
                // Find the index of the active pointer and fetch its position
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                
                if (pointerIndex < 0)
                    break;
                
                final float x = MotionEventCompat.getX(ev, pointerIndex);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                
                // Calculate the distance moved
                mDeltaX = (int) (x - mLastTouchX);
                mDeltaY = (int) (y - mLastTouchY);
                
                // Store location for next difference
                mLastTouchX = x;
                mLastTouchY = y;
                
                handleResize();
                break;
            }
            
            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                
                handleResize();
                storeSize();
                divider.setSelected(false);
                resizing = false;
                break;
            }
        }
        return true;
    }
    
    private void handleResize() {
        if (isVertical) {
            final int size = calculateSize((int) (primaryFrame.getHeight() + mDeltaY));
            primaryFrame.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, size));
        } else {
            final int size = calculateSize((int) (primaryFrame.getWidth() + mDeltaX));
            primaryFrame.setLayoutParams(new LayoutParams(size, LayoutParams.MATCH_PARENT));
        }
        
        getWindow().getDecorView().getRootView().invalidate();
    }
    
    private int calculateSize(final int size) {
        int ret = size;
        if (ret < minListSize)
            ret = minListSize;
        if (ret > maxListSize)
            ret = maxListSize;
        return ret;
    }
    
    private void storeSize() {
        int size = isVertical ? primaryFrame.getHeight() : primaryFrame.getWidth();
        Controller.getInstance().setViewSize(this, isVertical, size);
    }
    
    private static View findViewAtPosition(View parent, int x, int y) {
        if (parent instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) parent;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                View viewAtPosition = findViewAtPosition(child, x, y);
                if (viewAtPosition != null) {
                    return viewAtPosition;
                }
            }
            return null;
        } else {
            Rect rect = new Rect();
            parent.getGlobalVisibleRect(rect);
            if (rect.contains(x, y)) {
                return parent;
            } else {
                return null;
            }
        }
    }
    
}
