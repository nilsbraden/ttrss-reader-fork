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

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.CategoryListAdapter;
import org.ttrssreader.model.cachers.Cacher;
import org.ttrssreader.model.cachers.ImageCacher;
import org.ttrssreader.model.pojos.CategoryItem;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.net.TTRSSJsonConnector;
import org.ttrssreader.utils.Utils;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

public class CategoryActivity extends MenuActivity {
    
    private static final int DIALOG_WELCOME = 1;
    private static final int DIALOG_UPDATE = 2;
    
    private CategoryListAdapter adapter = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.categorylist);
        
        Controller.getInstance().checkAndInitializeController(this);
        DBHelper.getInstance().checkAndInitializeDB(this);
        Data.getInstance().checkAndInitializeData(this);
        
        listView = getListView();
        registerForContextMenu(listView);
        notificationTextView = (TextView) findViewById(R.id.notification);
        
        // Check for update or new installation
        if (Controller.getInstance().newInstallation()) {
            showDialog(DIALOG_WELCOME);
        } else if (Utils.newVersionInstalled(this)) {
            showDialog(DIALOG_UPDATE);
        } else if (!checkConfig()) {
            // Check if we have a server specified
            openConnectionErrorDialog((String) getText(R.string.CategoryActivity_NoServer));
        }
        
        adapter = new CategoryListAdapter(this);
        listView.setAdapter(adapter);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        DBHelper.getInstance().checkAndInitializeDB(getApplicationContext());
        
        if (configChecked || checkConfig()) {
            doRefresh();
            doUpdate();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updater != null) {
            updater.cancel(true);
            updater = null;
        }
        if (imageCacher != null) {
            imageCacher.cancel(true);
            imageCacher = null;
        }
        adapter.cursor.deactivate();
        adapter.cursor.close();
    }
    
    @Override
    protected synchronized void doRefresh() {
        setTitle(String.format("%s (%s)", getResources().getString(R.string.ApplicationName), adapter.getUnread()));
        
        adapter.makeQuery();
        adapter.notifyDataSetChanged();
        
        if (TTRSSJsonConnector.hasLastError()) {
            if (imageCacher != null) {
                imageCacher.cancel(true);
                imageCacher = null;
            }
            openConnectionErrorDialog(TTRSSJsonConnector.pullLastError());
            return;
        }
        
        if (updater == null && imageCacher == null) {
            setProgressBarIndeterminateVisibility(false);
            notificationTextView.setText(R.string.Loading_EmptyCategories);
        }
    }
    
    @Override
    protected synchronized void doUpdate() {
        // Only update if no updater already running
        if (updater != null) {
            if (updater.getStatus().equals(AsyncTask.Status.FINISHED)) {
                updater = null;
            } else {
                return;
            }
        }
        
        setProgressBarIndeterminateVisibility(true);
        notificationTextView.setText(R.string.Loading_Categories);
        
        updater = new Updater(this, adapter);
        updater.execute();
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == MARK_READ) {
            new Updater(this, new ReadStateUpdater(adapter.getCategoryId(cmi.position))).execute();
            return true;
        }
        return false;
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        int categoryId = adapter.getCategoryId(position);
        Intent i;
        
        if (categoryId < 0 && categoryId >= -4) {
            // Virtual feeds
            i = new Intent(this, FeedHeadlineListActivity.class);
            i.putExtra(FeedHeadlineListActivity.FEED_ID, categoryId);
            i.putExtra(FeedHeadlineListActivity.FEED_TITLE, adapter.getCategoryTitle(position));
        } else {
            // Categories
            i = new Intent(this, FeedListActivity.class);
            i.putExtra(FeedListActivity.CATEGORY_ID, categoryId);
            i.putExtra(FeedListActivity.CATEGORY_TITLE, adapter.getCategoryTitle(position));
        }
        startActivity(i);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.category, menu);
        if (!Controller.getInstance().isDonator()) {
            menu.removeItem(R.id.Category_Menu_ImageCache);
            menu.removeItem(R.id.Category_Menu_ArticleCache);
        }
        return true;
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        boolean ret = super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case R.id.Menu_Refresh:
                Data.getInstance().resetTime(new CategoryItem());
                doUpdate();
                return true;
            case R.id.Menu_MarkAllRead:
                new Updater(this, new ReadStateUpdater(adapter.getCategories())).execute();
                return true;
            case R.id.Category_Menu_ImageCache:
                if (imageCacher == null || imageCacher.getStatus().equals(Status.FINISHED)) {
                    setProgressBarIndeterminateVisibility(true);
                    imageCacher = new Cacher(this, new ImageCacher(this, false));
                    imageCacher.execute();
                }
                return true;
            case R.id.Category_Menu_ArticleCache:
                if (imageCacher == null || imageCacher.getStatus().equals(Status.FINISHED)) {
                    setProgressBarIndeterminateVisibility(true);
                    imageCacher = new Cacher(this, new ImageCacher(this, true));
                    imageCacher.execute();
                }
                return true;
        }
        
        if (ret) {
            doRefresh();
        }
        return true;
    }
    
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
    
}
