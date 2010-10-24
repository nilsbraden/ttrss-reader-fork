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
import org.ttrssreader.model.ReadStateUpdater;
import org.ttrssreader.model.Refresher;
import org.ttrssreader.model.Updater;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.category.CategoryListAdapter;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.Utils;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;

public class CategoryActivity extends ListActivity implements IRefreshEndListener, IUpdateEndListener {
    
    private static final int ACTIVITY_SHOW_ERROR = 0;
    
    private static final int MENU_REFRESH = Menu.FIRST;
    private static final int MENU_SHOW_PREFERENCES = Menu.FIRST + 1;
    private static final int MENU_SHOW_ABOUT = Menu.FIRST + 2;
    private static final int MENU_DISPLAY_ONLY_UNREAD = Menu.FIRST + 3;
    private static final int MENU_MARK_ALL_READ = Menu.FIRST + 4;
    
    private static final int DIALOG_UPDATE = 1;
    
    private ListView mCategoryListView;
    private CategoryListAdapter mAdapter = null;
    private Refresher refresher;
    private Updater updater;
    
    private boolean connected = true;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.category);
        
        Controller.getInstance().checkAndInitializeController(this);
        DBHelper.getInstance().checkAndInitializeDB(this);
        
        Data.getInstance().checkAndInitializeData(this);
        
        mCategoryListView = getListView();
        mAdapter = new CategoryListAdapter(this);
        mCategoryListView.setAdapter(mAdapter);
        
        // Check if ChangeLog should be displayed
        if (Utils.newVersionInstalled(this)) {
            this.showDialog(DIALOG_UPDATE);
        }
        
        // Check if we have a server specified
        String url = Controller.getInstance().getUrl();
        if (url.equals(Constants.URL_DEFAULT + Controller.JSON_END_URL)) {
            Log.e(Utils.TAG, "ERROR: No Server specified");
            openConnectionErrorDialog("No Server specified.");
            connected = false;
        } else {
            // Only post background-task if we have a server specified
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
        
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        DBHelper.getInstance().checkAndInitializeDB(getApplicationContext());
        
        if (connected)
            doRefresh();
        else
            connected = true;
    }
    
    @Override
    protected void onRestart() {
        super.onRestart();
        DBHelper.getInstance().checkAndInitializeDB(getApplicationContext());
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
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    }
    
    private synchronized void doRefresh() {
        setProgressBarIndeterminateVisibility(true);
        
        this.setTitle(this.getResources().getString(R.string.ApplicationName));
        
        if (mAdapter == null) {
            mAdapter = new CategoryListAdapter(this);
            mCategoryListView.setAdapter(mAdapter);
        }
        
        refresher = new Refresher(this, mAdapter);
        refresher.execute();
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
        item.setIcon(R.drawable.refresh32);
        
        item = menu.add(0, MENU_DISPLAY_ONLY_UNREAD, 0, R.string.Commons_DisplayOnlyUnread);
        
        item = menu.add(0, MENU_MARK_ALL_READ, 0, R.string.Commons_MarkAllRead);
        
        item = menu.add(0, MENU_SHOW_PREFERENCES, 0, R.string.Main_ShowPreferencesMenu);
        item.setIcon(R.drawable.preferences32);
        
        item = menu.add(0, MENU_SHOW_ABOUT, 0, R.string.Main_ShowAboutMenu);
        item.setIcon(R.drawable.about32);
        
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case MENU_REFRESH:
                doForceRefresh();
                return true;
            case MENU_DISPLAY_ONLY_UNREAD:
                displayOnlyUnreadSwitch();
                return true;
            case MENU_MARK_ALL_READ:
                markAllRead();
                return true;
            case MENU_SHOW_PREFERENCES:
                openPreferences();
                return true;
            case MENU_SHOW_ABOUT:
                openAboutDialog();
                return true;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }
    
    private void doForceRefresh() {
        Data.getInstance().resetCategoriesTime();
        
        updater = new Updater(this, mAdapter);
        setProgressBarIndeterminateVisibility(true);
        updater.execute();
        
        doRefresh();
    }
    
    private void openPreferences() {
        Intent i = new Intent(this, PreferencesActivity.class);
        Log.e(Utils.TAG, "Starting PreferencesActivity");
        startActivity(i);
    }
    
    private void openAboutDialog() {
        Intent i = new Intent(this, AboutActivity.class);
        startActivity(i);
    }
    
    private void displayOnlyUnreadSwitch() {
        boolean displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnread();
        Controller.getInstance().setDisplayOnlyUnread(!displayOnlyUnread);
        doRefresh();
    }
    
    private void markAllRead() {
        setProgressBarIndeterminateVisibility(true);
        List<CategoryItem> list = mAdapter.getCategories();
        new Updater(this, new ReadStateUpdater(list, 0)).execute();
    }
    
    private void openConnectionErrorDialog(String errorMessage) {
        Intent i = new Intent(this, ErrorActivity.class);
        i.putExtra(ErrorActivity.ERROR_MESSAGE, errorMessage);
        
        if (refresher != null) {
            refresher.cancel(true);
            refresher = null;
        }
        if (updater != null) {
            updater.cancel(true);
            updater = null;
        }
        
        startActivityForResult(i, ACTIVITY_SHOW_ERROR);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final Dialog onCreateDialog(final int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setTitle(getResources().getString(R.string.Changelog_Title));
        final String[] changes = getResources().getStringArray(R.array.updates);
        final StringBuilder buf = new StringBuilder();
        for (int i = 1; i < changes.length; i++) {
            buf.append("\n\n");
            buf.append(changes[i]);
        }
        builder.setIcon(android.R.drawable.ic_menu_info_details);
        builder.setMessage(buf.toString().trim());
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNeutralButton("want to Donate?", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface d, final int which) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(
                        R.string.Changelog_DonateUrl))));
            }
        });
        return builder.create();
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTIVITY_SHOW_ERROR:
                doRefresh();
                break;
            default:
                break;
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void onRefreshEnd() {
        if (!Controller.getInstance().getConnector().hasLastError()) {
            
            try {
                List<CategoryItem> list = new ArrayList<CategoryItem>();
                list.addAll((Set<CategoryItem>) refresher.get());
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
        
        if (updater != null) {
            if (updater.getStatus().equals(Status.FINISHED)) {
                setProgressBarIndeterminateVisibility(false);
            }
        } else {
            setProgressBarIndeterminateVisibility(false);
        }
    }
    
    @Override
    public void onUpdateEnd() {
        if (!Controller.getInstance().getConnector().hasLastError()) {
            mAdapter.notifyDataSetChanged();
        } else {
            openConnectionErrorDialog(Controller.getInstance().getConnector().pullLastError());
            return;
        }
        
        doRefresh();
    }
    
}
