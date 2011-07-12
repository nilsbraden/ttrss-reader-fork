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
import org.ttrssreader.controllers.NotInitializedException;
import org.ttrssreader.model.CategoryAdapter;
import org.ttrssreader.model.MainAdapter;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.updaters.CategoryUpdater;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.TopExceptionHandler;
import org.ttrssreader.utils.Utils;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

public class CategoryActivity extends MenuActivity {
    
    private static final int DIALOG_WELCOME = 1;
    private static final int DIALOG_UPDATE = 2;
    private static final int DIALOG_CRASH = 3;
    private static final int DIALOG_OLD_SERVER = 4;
    
    protected static final int SELECT_ARTICLES = MARK_GROUP + 54;
    
    private CategoryAdapter adapter = null;
    private CategoryUpdater updateable = null;
    private boolean cacherStarted = false;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        setContentView(R.layout.categorylist);
        
        // Register our own ExceptionHander
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));
        
        listView = getListView();
        registerForContextMenu(listView);
        notificationTextView = (TextView) findViewById(R.id.notification);
        
        if (!Utils.checkFirstRun(this)) { // Check for new installation
            showDialog(DIALOG_WELCOME);
        } else if (!Utils.checkNewVersion(this)) { // Check for update
            showDialog(DIALOG_UPDATE);
        } else if (!Utils.checkCrashReport(this)) { // Check for crash-reports
            showDialog(DIALOG_CRASH);
        } else if (!Utils.checkServerVersion(this)) { // Check if the server-version is supported
            showDialog(DIALOG_OLD_SERVER);
        } else if (!Utils.checkConfig()) {// Check if we have a server specified
            openConnectionErrorDialog((String) getText(R.string.CategoryActivity_NoServer));
        }
        
        // Delete DB if requested
        Controller.getInstance().setDeleteDBScheduled(Controller.getInstance().isDeleteDBOnStartup());
        
        // Start caching if requested, only cache articles if not caching images
        if (Controller.getInstance().cacheImagesOnStartup()) {
            cacherStarted = true;
            doCache(false); // images
        } else if (Controller.getInstance().cacheOnStartup()) {
            cacherStarted = true;
            doCache(true); // articles
        }
        
        updateable = new CategoryUpdater();
        adapter = new CategoryAdapter(this);
        listView.setAdapter(adapter);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        Controller.getInstance().lastOpenedFeed = null;
        Controller.getInstance().lastOpenedArticle = null;
        
        refreshAndUpdate();
        DBHelper.getInstance().checkAndInitializeDB(this);
    }
    
    @Override
    protected void onPause() {
        synchronized (this) {
            if (adapter != null) {
                adapter.closeCursor();
            }
        }
        super.onPause();
    }
    
    @Override
    protected void onStop() {
        synchronized (this) {
            if (adapter != null) {
                adapter.closeCursor();
            }
        }
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
        synchronized (this) {
            if (adapter != null) {
                adapter.closeCursor();
                adapter = null;
            }
        }
        super.onDestroy();
    }
    
    @Override
    protected void doRefresh() {
        setTitle(MainAdapter.formatTitle(getResources().getString(R.string.ApplicationName), updateable.unreadCount));
        
        if (adapter != null) {
            adapter.makeQuery(true);
            adapter.notifyDataSetChanged();
        }
        
        try {
            if (Controller.getInstance().getConnector().hasLastError())
                openConnectionErrorDialog(Controller.getInstance().getConnector().pullLastError());
        } catch (NotInitializedException e) {
        }
        
        if (updater == null) {
            setProgressBarIndeterminateVisibility(false);
            notificationTextView.setText(R.string.Loading_EmptyCategories);
        }
    }
    
    @Override
    protected void doUpdate() {
        // Only update if no updater already running
        if (updater != null) {
            if (updater.getStatus().equals(AsyncTask.Status.FINISHED)) {
                updater = null;
            } else {
                return;
            }
        }
        
        if (!isCacherRunning() && !cacherStarted) {
            setProgressBarIndeterminateVisibility(true);
            notificationTextView.setText(R.string.Loading_Categories);
            
            updater = new Updater(this, updateable);
            updater.execute();
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(MARK_GROUP, SELECT_ARTICLES, Menu.NONE, R.string.Commons_SelectArticles);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case MARK_READ:
                new Updater(this, new ReadStateUpdater(adapter.getId(cmi.position))).execute();
                return true;
            case SELECT_ARTICLES:
                Intent i = new Intent(this, FeedHeadlineActivity.class);
                i.putExtra(FeedHeadlineActivity.FEED_ID, FeedHeadlineActivity.FEED_NO_ID);
                i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, adapter.getId(cmi.position));
                i.putExtra(FeedHeadlineActivity.FEED_TITLE, adapter.getTitle(cmi.position));
                i.putExtra(FeedHeadlineActivity.FEED_SELECT_ARTICLES, true);
                startActivity(i);
        }
        return false;
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        int categoryId = adapter.getId(position);
        Intent i;
        
        if (categoryId < 0 && categoryId >= -4) {
            // Virtual feeds
            i = new Intent(this, FeedHeadlineActivity.class);
            i.putExtra(FeedHeadlineActivity.FEED_ID, categoryId);
            i.putExtra(FeedHeadlineActivity.FEED_TITLE, adapter.getTitle(position));
        } else {
            // Categories
            i = new Intent(this, FeedActivity.class);
            i.putExtra(FeedActivity.CATEGORY_ID, categoryId);
            i.putExtra(FeedActivity.CATEGORY_TITLE, adapter.getTitle(position));
        }
        startActivity(i);
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        boolean ret = super.onOptionsItemSelected(item);
        
        switch (item.getItemId()) {
            case R.id.Menu_Refresh:
                Data.getInstance().resetTime(new Category());
                cacherStarted = false;
                doUpdate();
                return true;
            case R.id.Menu_MarkAllRead:
                new Updater(this, new ReadStateUpdater(adapter.getCategories())).execute();
                return true;
        }
        
        if (ret) {
            refreshAndUpdate();
        }
        return true;
    }
    
    @Override
    protected final Dialog onCreateDialog(final int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setCancelable(true);
        
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
                                d.dismiss();
                            }
                        });
                break;
            
            case DIALOG_UPDATE:

                builder.setTitle(getResources().getString(R.string.Changelog_Title));
                final String[] changes = getResources().getStringArray(R.array.updates);
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < changes.length; i++) {
                    sb.append("\n\n");
                    sb.append(changes[i]);
                    if (sb.length() > 4000) // Don't include all messages, nobody reads the old stuff anyway
                        break;
                }
                builder.setMessage(sb.toString().trim());
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setNeutralButton((String) getText(R.string.CategoryActivity_Donate),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface d, final int which) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(
                                        R.string.DonateUrl))));
                                d.dismiss();
                            }
                        });
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
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
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
                                d.dismiss();
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
