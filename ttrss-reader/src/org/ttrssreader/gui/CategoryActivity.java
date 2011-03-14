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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.ttrssreader.utils.TopExceptionHandler;
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
import android.view.ContextMenu;
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
    private static final int DIALOG_CRASH = 3;
    private static final int DIALOG_OLD_SERVER = 4;
    
    protected static final int MARK_ARTICLES = MARK_GROUP + 4;
    
    private CategoryListAdapter adapter = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.categorylist);
        
        // Register our own ExceptionHander
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));
        
        // Initialize Singletons for Config, Data-Access and DB
        Controller.getInstance().checkAndInitializeController(this);
        DBHelper.getInstance().checkAndInitializeDB(this);
        Data.getInstance().checkAndInitializeData(this);
        
        listView = getListView();
        registerForContextMenu(listView);
        notificationTextView = (TextView) findViewById(R.id.notification);
        
        // Check for new installation
        if (Utils.checkFirstRun(this))
            showDialog(DIALOG_WELCOME);
        
        // Check for update
        if (Utils.checkNewVersion(this))
            showDialog(DIALOG_UPDATE);
        
        // Check for crash-reports
        if (Utils.checkCrashReport(this))
            showDialog(DIALOG_CRASH);
        
        // Check if the server-version is supported
        if (Utils.checkServerVersion(this))
            showDialog(DIALOG_OLD_SERVER);
        
        // Check if we have a server specified
        if (!checkConfig()) {
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(MARK_GROUP, MARK_ARTICLES, Menu.NONE, R.string.Commons_SelectArticles);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case MARK_READ:
                new Updater(this, new ReadStateUpdater(adapter.getCategoryId(cmi.position))).execute();
                return true;
            case MARK_ARTICLES:
                Intent i = new Intent(this, FeedHeadlineListActivity.class);
                i.putExtra(FeedHeadlineListActivity.FEED_ID, FeedHeadlineListActivity.FEED_NO_ID);
                i.putExtra(FeedHeadlineListActivity.FEED_CAT_ID, adapter.getCategoryId(cmi.position));
                i.putExtra(FeedHeadlineListActivity.FEED_TITLE, adapter.getCategoryTitle(cmi.position));
                i.putExtra(FeedHeadlineListActivity.FEED_SELECT_ARTICLES, true);
                startActivity(i);
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
        // if (!Controller.getInstance().isDonator()) {
        // menu.removeItem(R.id.Category_Menu_ImageCache);
        // menu.removeItem(R.id.Category_Menu_ArticleCache);
        // }
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
                // if (!Controller.getInstance().isDonator()) {
                builder.setNeutralButton((String) getText(R.string.CategoryActivity_Donate),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface d, final int which) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(
                                        R.string.DonateUrl))));
                            }
                        });
                // }
                break;
            
            case DIALOG_CRASH:

                builder.setTitle(getResources().getString(R.string.ErrorActivity_Title));
                builder.setMessage(getResources().getString(R.string.Check_Crash));
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface d, final int which) {
                        sendReport();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface d, final int which) {
                        deleteFile(TopExceptionHandler.FILE);
                        d.dismiss();
                    }
                });
                break;
            
            case DIALOG_OLD_SERVER:

                builder.setTitle(getResources().getString(R.string.ErrorActivity_Title));
                builder.setMessage(getResources().getString(R.string.Check_OldVersion));
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface d, final int which) {
                        finish();
                    }
                });
                builder.setNeutralButton((String) getText(R.string.Preferences_Btn),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface d, final int which) {
                                Intent i = new Intent(context, PreferencesActivity.class);
                                startActivity(i);
                            }
                        });
                break;
            
        }
        return builder.create();
    }
    
    public void sendReport() {
        String line = "";
        StringBuilder sb = new StringBuilder();
        
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(TopExceptionHandler.FILE)));
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (FileNotFoundException fnfe) {
            // ...
        } catch (IOException ioe) {
            // ...
        }
        
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        String subject = "Error report";
        String mail = getResources().getString(R.string.About_mail);
        String body = "Please mail this to " + mail + ": " + "\n\n" + sb.toString() + "\n\n";
        
        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { mail });
        sendIntent.putExtra(Intent.EXTRA_TEXT, body);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        sendIntent.setType("message/rfc822");
        
        startActivity(Intent.createChooser(sendIntent, "Title:"));
        
        deleteFile(TopExceptionHandler.FILE);
    }
    
}
