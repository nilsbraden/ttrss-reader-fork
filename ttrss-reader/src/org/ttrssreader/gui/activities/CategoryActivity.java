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

package org.ttrssreader.gui.activities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.IRefreshEndListener;
import org.ttrssreader.gui.IUpdateEndListener;
import org.ttrssreader.model.Refresher;
import org.ttrssreader.model.Updater;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.category.CategoryListAdapter;
import org.ttrssreader.model.updaters.ImageCacheUpdater;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.Utils;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class CategoryActivity extends ListActivity implements IRefreshEndListener, IUpdateEndListener {
    
    private static final int MENU_REFRESH = Menu.FIRST;
    private static final int MENU_SHOW_PREFERENCES = Menu.FIRST + 1;
    private static final int MENU_SHOW_ABOUT = Menu.FIRST + 2;
    private static final int MENU_DISPLAY_ONLY_UNREAD = Menu.FIRST + 3;
    private static final int MENU_MARK_ALL_READ = Menu.FIRST + 4;
    private static final int MENU_DOWNLOAD_CACHE = Menu.FIRST + 5;
    
    private static final int MARK_GROUP = 42;
    private static final int MARK_READ = MARK_GROUP + 1;
    
    private static final int DIALOG_WELCOME = 1;
    private static final int DIALOG_UPDATE = 2;
    
    private ListView mCategoryListView;
    private CategoryListAdapter mAdapter = null;
    private Refresher refresher;
    private Updater updater;
    private Updater imageCacher;
    
    private boolean configChecked = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.categorylist);
        
        Controller.getInstance().checkAndInitializeController(this);
        DBHelper.getInstance().checkAndInitializeDB(this);
        
        Data.getInstance().checkAndInitializeData(this);
        
        mCategoryListView = getListView();
        registerForContextMenu(mCategoryListView);
        mAdapter = new CategoryListAdapter(this);
        mCategoryListView.setAdapter(mAdapter);
        
        // Check for update or new installation
        if (Controller.getInstance().isNewInstallation()) {
            this.showDialog(DIALOG_WELCOME);
        } else if (Utils.newVersionInstalled(this)) {
            this.showDialog(DIALOG_UPDATE);
        } else if (!checkConfig()) {
            // Check if we have a server specified
            openConnectionErrorDialog((String) getText(R.string.CategoryActivity_NoServer));
        }
        
        if (configChecked || checkConfig())
            doUpdate();
    }
    
    private boolean checkConfig() {
        String url = Controller.getInstance().getUrl();
        if (url.equals(Constants.URL_DEFAULT + Controller.JSON_END_URL)) {
            return false;
        }
        
        configChecked = true;
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        DBHelper.getInstance().checkAndInitializeDB(getApplicationContext());
        
        if (configChecked || checkConfig())
            doRefresh();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (refresher != null) {
            refresher.cancel(true);
            refresher = null;
        }
        if (updater != null) {
            updater.cancel(true);
            updater = null;
        }
        if (imageCacher != null) {
            imageCacher.cancel(true);
            imageCacher = null;
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
    
    private synchronized void doRefresh() {
        // Only update if no refresher already running
        if (refresher != null) {
            if (refresher.getStatus().equals(AsyncTask.Status.PENDING)) {
                return;
            } else if (refresher.getStatus().equals(AsyncTask.Status.FINISHED)) {
                refresher = null;
                return;
            }
        }
        
        if (mAdapter == null) {
            mAdapter = new CategoryListAdapter(this);
            mCategoryListView.setAdapter(mAdapter);
        }
        
        this.setTitle(this.getResources().getString(R.string.ApplicationName));
        
        setProgressBarIndeterminateVisibility(true);
        refresher = new Refresher(this, mAdapter);
        refresher.execute();
    }
    
    private synchronized void doUpdate() {
        // Only update if no updater already running
        if (updater != null) {
            if (updater.getStatus().equals(AsyncTask.Status.PENDING)) {
                return;
            } else if (updater.getStatus().equals(AsyncTask.Status.FINISHED)) {
                updater = null;
                return;
            }
        }
        
        if (mAdapter == null) {
            mAdapter = new CategoryListAdapter(this);
            mCategoryListView.setAdapter(mAdapter);
        }
        
        updater = new Updater(this, mAdapter);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (updater != null) {
                    setProgressBarIndeterminateVisibility(true);
                    updater.execute();
                }
            }
        }, Utils.WAIT);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        menu.add(MARK_GROUP, MARK_READ, Menu.NONE, R.string.Commons_MarkRead);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        CategoryItem c = (CategoryItem) mAdapter.getItem(cmi.position);
        
        if (c == null) {
            return false;
        }
        
        switch (item.getItemId()) {
            case MARK_READ:
                new Updater(this, new ReadStateUpdater(c.getId())).execute();
                return true;
        }
        return false;
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        int categoryId = mAdapter.getCategoryId(position);
        Intent i;
        
        if (categoryId < 0 && categoryId >= -4) {
            // Virtual feeds
            i = new Intent(this, FeedHeadlineListActivity.class);
            i.putExtra(FeedHeadlineListActivity.FEED_ID, categoryId);
            i.putExtra(FeedHeadlineListActivity.FEED_TITLE, mAdapter.getCategoryTitle(position));
        } else {
            // Categories
            i = new Intent(this, FeedListActivity.class);
            i.putExtra(FeedListActivity.CATEGORY_ID, categoryId);
            i.putExtra(FeedListActivity.CATEGORY_TITLE, mAdapter.getCategoryTitle(position));
        }
        
        startActivity(i);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        MenuItem item;
        item = menu.add(0, MENU_REFRESH, 0, R.string.Main_RefreshMenu);
        item.setIcon(R.drawable.ic_menu_refresh);
        item = menu.add(0, MENU_MARK_ALL_READ, 0, R.string.Commons_MarkAllRead);
        item.setIcon(R.drawable.ic_menu_mark);
        item = menu.add(0, MENU_DISPLAY_ONLY_UNREAD, 0, R.string.Commons_DisplayOnlyUnread);
        item.setIcon(android.R.drawable.ic_menu_view);
        item = menu.add(0, MENU_SHOW_PREFERENCES, 0, R.string.Main_ShowPreferencesMenu);
        item.setIcon(android.R.drawable.ic_menu_preferences);
        if (Controller.getInstance().isDonator()) {
            item = menu.add(0, MENU_DOWNLOAD_CACHE, 0, R.string.Main_StartDownloadForCache);
            item.setIcon(android.R.drawable.ic_menu_save);
        }
        item = menu.add(0, MENU_SHOW_ABOUT, 0, R.string.Main_ShowAboutMenu);
        item.setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case MENU_REFRESH:
                Data.getInstance().resetCategoriesTime();
                doUpdate();
                doRefresh();
                return true;
            case MENU_DISPLAY_ONLY_UNREAD:
                boolean displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnread();
                Controller.getInstance().setDisplayOnlyUnread(!displayOnlyUnread);
                doRefresh();
                return true;
            case MENU_MARK_ALL_READ:
                new Updater(this, new ReadStateUpdater(mAdapter.getCategories())).execute();
                return true;
            case MENU_SHOW_PREFERENCES:
                startActivityForResult(new Intent(this, PreferencesActivity.class),
                        PreferencesActivity.ACTIVITY_SHOW_PREFERENCES);
                return true;
            case MENU_SHOW_ABOUT:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case MENU_DOWNLOAD_CACHE:
                if (imageCacher == null || imageCacher.getStatus().equals(Status.FINISHED)) {
                    setProgressBarIndeterminateVisibility(true);
                    imageCacher = new Updater(null, new ImageCacheUpdater(this));
                    imageCacher.execute();
                }
        }
        return super.onMenuItemSelected(featureId, item);
    }
    
    private void openConnectionErrorDialog(String errorMessage) {
        if (refresher != null) {
            refresher.cancel(true);
            refresher = null;
        }
        if (updater != null) {
            updater.cancel(true);
            updater = null;
        }
        if (imageCacher != null) {
            imageCacher.cancel(true);
            imageCacher = null;
        }
        
        Intent i = new Intent(this, ErrorActivity.class);
        i.putExtra(ErrorActivity.ERROR_MESSAGE, errorMessage);
        startActivityForResult(i, ErrorActivity.ACTIVITY_SHOW_ERROR);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(Utils.TAG, "onActivityResult. requestCode: " + requestCode + " resultCode: " + resultCode);
        if (resultCode == ErrorActivity.ACTIVITY_SHOW_ERROR) {
            if (configChecked || checkConfig()) {
                doUpdate();
                doRefresh();
            }
        } else if (resultCode == PreferencesActivity.ACTIVITY_SHOW_PREFERENCES) {
            if (configChecked || checkConfig()) {
                doUpdate();
                doRefresh();
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final Dialog onCreateDialog(final int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, null);
        
        final Context context = this;
        
        switch (id) {
            case DIALOG_WELCOME:
                builder.setTitle(getResources().getString(R.string.Welcome_Title));
                builder.setMessage(getResources().getString(R.string.Welcome_Message));
                builder.setNeutralButton((String) getText(R.string.Preferences_Btn),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface d, final int which) {
                                Intent i = new Intent(context, PreferencesActivity.class);
                                Log.e(Utils.TAG, "Starting PreferencesActivity");
                                startActivity(i);
                            }
                        });
                break;
            case DIALOG_UPDATE:
                builder.setTitle(getResources().getString(R.string.Changelog_Title));
                final String[] changes = getResources().getStringArray(R.array.updates);
                final StringBuilder buf = new StringBuilder();
                for (int i = 0; i < changes.length; i++) {
                    buf.append("\n\n");
                    buf.append(changes[i]);
                }
                builder.setMessage(buf.toString().trim());
                if (!Controller.getInstance().isDonator()) {
                    builder.setNeutralButton((String) getText(R.string.CategoryActivity_Donate),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface d, final int which) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(
                                            R.string.DonateUrl))));
                                }
                            });
                }
                break;
        }
        return builder.create();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void onRefreshEnd() {
        if (!Controller.getInstance().getConnector().hasLastError()) {
            
            try {
                List<CategoryItem> list = new ArrayList<CategoryItem>();
                list.addAll((Set<CategoryItem>) refresher.get());
                refresher = null;
                mAdapter.setCategories(list);
                mAdapter.notifyDataSetChanged();
            } catch (Exception e) {
            }
            
        } else {
            openConnectionErrorDialog(Controller.getInstance().getConnector().pullLastError());
            return;
        }
        
        if (mAdapter.getTotalUnread() >= 0) {
            this.setTitle(this.getResources().getString(R.string.ApplicationName) + " (" + mAdapter.getTotalUnread()
                    + ")");
        }
        
        boolean somethingRunning = false;
        if (updater != null && !updater.getStatus().equals(Status.FINISHED)) {
            somethingRunning = true;
        }
        if (imageCacher != null && !imageCacher.getStatus().equals(Status.FINISHED)) {
            somethingRunning = true;
        }
        if (refresher != null && !refresher.getStatus().equals(Status.FINISHED)) {
            somethingRunning = true;
        }
        
        if (!somethingRunning) {
            setProgressBarIndeterminateVisibility(false);
        }
    }
    
    @Override
    public void onUpdateEnd() {
        if (updater != null) {
            if (updater.getStatus().equals(Status.FINISHED)) {
                updater = null;
            }
        }
        if (imageCacher != null) {
            if (imageCacher.getStatus().equals(Status.FINISHED)) {
                imageCacher = null;
            }
        }
        doRefresh();
    }
    
}
