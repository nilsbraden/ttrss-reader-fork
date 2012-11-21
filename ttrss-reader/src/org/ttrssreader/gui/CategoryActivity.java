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
import java.util.Set;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.controllers.NotInitializedException;
import org.ttrssreader.controllers.UpdateController;
import org.ttrssreader.gui.fragments.FeedHeadlineListFragment;
import org.ttrssreader.gui.fragments.FeedListFragment;
import org.ttrssreader.model.CategoryAdapter;
import org.ttrssreader.model.MainAdapter;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.TopExceptionHandler;
import org.ttrssreader.utils.Utils;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import com.actionbarsherlock.view.MenuItem;

public class CategoryActivity extends MenuActivity {
    
    private static final int DIALOG_WELCOME = 1;
    private static final int DIALOG_UPDATE = 2;
    private static final int DIALOG_CRASH = 3;
    private static final int DIALOG_VACUUM = 4;
    
    private static final int SELECTED_VIRTUAL_CATEGORY = 1;
    private static final int SELECTED_CATEGORY = 2;
    private static final int SELECTED_LABEL = 3;
    
    private static final int SELECT_ARTICLES = MenuActivity.MARK_GROUP + 54;
    
    private String applicationName = null;
    public boolean cacherStarted = false;
    
    private CategoryAdapter adapter = null; // Remember to explicitly check every access to adapter for it beeing null!
    private CategoryUpdater categoryUpdater = null;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        // Log.d(Utils.TAG, "onCreate - CategoryActivity");
        setContentView(R.layout.categorylist);
        
        // Register our own ExceptionHander
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));
        
        // Delete DB if requested
        Controller.getInstance().setDeleteDBScheduled(Controller.getInstance().isDeleteDBOnStartup());
        
        if (!Utils.checkFirstRun(this)) { // Check for new installation
            showDialog(DIALOG_WELCOME);
        } else if (!Utils.checkNewVersion(this)) { // Check for update
            showDialog(DIALOG_UPDATE);
        } else if (!Utils.checkCrashReport(this)) { // Check for crash-reports
            showDialog(DIALOG_CRASH);
        } else if (Utils.checkVacuumDB(this)) { // Check for scheduled VACUUM
            showDialog(DIALOG_VACUUM);
        } else if (!Utils.checkConfig()) {// Check if we have a server specified
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
        if (adapter != null)
            adapter.makeQuery(true);
        
        super.onResume();
        
        UpdateController.getInstance().registerActivity(this, UpdateController.TYPE_CATEGORY, UpdateController.ID_ALL);
        refreshAndUpdate();
    }
    
    private void closeCursor() {
        if (adapter != null)
            adapter.closeCursor();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        UpdateController.getInstance()
                .unregisterActivity(this, UpdateController.TYPE_CATEGORY, UpdateController.ID_ALL);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        closeCursor();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCursor();
    }
    
    @Override
    protected void doRefresh() {
        if (applicationName == null)
            applicationName = getResources().getString(R.string.ApplicationName);
        int unreadCount = DBHelper.getInstance().getUnreadCount(Data.VCAT_ALL, true);
        setTitle(MainAdapter.formatTitle(applicationName, unreadCount));
        
        if (adapter != null)
            adapter.refreshQuery();
        
        try {
            if (Controller.getInstance().getConnector().hasLastError())
                openConnectionErrorDialog(Controller.getInstance().getConnector().pullLastError());
        } catch (NotInitializedException e) {
        }
        
        if (categoryUpdater == null && !isCacherRunning()) {
            setSupportProgressBarIndeterminateVisibility(false);
            setSupportProgressBarVisibility(false);
        }
    }
    
    @Override
    protected void doUpdate() {
        // Only update if no categoryUpdater already running
        if (categoryUpdater != null) {
            if (categoryUpdater.getStatus().equals(AsyncTask.Status.FINISHED)) {
                categoryUpdater = null;
            } else {
                return;
            }
        }
        
        if (!isCacherRunning() && !cacherStarted) {
            setSupportProgressBarIndeterminateVisibility(true);
            setSupportProgressBarVisibility(true);
            
            categoryUpdater = new CategoryUpdater();
            categoryUpdater.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(MARK_GROUP, SELECT_ARTICLES, Menu.NONE, R.string.Commons_SelectArticles);
    }
    
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        Log.d(Utils.TAG, "CategoryActivity: onContextItemSelected called");
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        if (adapter == null)
            return false;
        
        int id = adapter.getId(cmi.position);
        
        switch (item.getItemId()) {
            case MARK_READ:
                if (id < -10)
                    new Updater(this, new ReadStateUpdater(id, 42)).exec();
                new Updater(this, new ReadStateUpdater(id)).exec();
                return true;
            case SELECT_ARTICLES:
                if (id < 0)
                    return false; // Do nothing for Virtual Category or Labels
                Intent i = new Intent(context, FeedHeadlineActivity.class);
                i.putExtra(FeedHeadlineActivity.FEED_ID, FeedHeadlineActivity.FEED_NO_ID);
                i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, id);
                i.putExtra(FeedHeadlineActivity.FEED_TITLE, adapter.getTitle(cmi.position));
                i.putExtra(FeedHeadlineActivity.FEED_SELECT_ARTICLES, true);
                startActivity(i);
        }
        return false;
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.Menu_Refresh:
                Data.getInstance().resetTime(-1, true, false, false);
                cacherStarted = false;
                doUpdate();
                return true;
            case R.id.Menu_MarkAllRead:
                if (adapter != null) {
                    new Updater(this, new ReadStateUpdater(adapter.getCategories())).exec();
                    return true;
                }
            default:
                return false;
        }
    }
    
    public class CategoryUpdater extends AsyncTask<Void, Integer, Void> {
        
        private int taskCount = 0;
        private static final int DEFAULT_TASK_COUNT = 4;
        
        @Override
        protected Void doInBackground(Void... params) {
            boolean onlyUnreadArticles = Controller.getInstance().onlyUnread();
            
            Set<Feed> labels = DBHelper.getInstance().getFeeds(-2);
            taskCount = DEFAULT_TASK_COUNT + labels.size() + 1; // 1 for the caching of all articles
            
            int progress = 0;
            publishProgress(++progress); // Move progress forward
            
            Data.getInstance().updateCounters(false);
            
            // Cache articles for all categories
            publishProgress(++progress);
            Data.getInstance().cacheArticles(false);
            
            // Refresh articles for all labels
            for (Feed f : labels) {
                if (f.unread == 0 && onlyUnreadArticles)
                    continue;
                publishProgress(++progress); // Move progress forward
                Data.getInstance().updateArticles(f.id, onlyUnreadArticles, false, false);
            }
            
            publishProgress(++progress); // Move progress forward to 100%
            
            // This stuff will be done in background without UI-notification, but the progress-calls will be done anyway
            // to ensure the UI is refreshed properly. ProgressBar is rendered invisible with the call to
            // publishProgress(taskCount).
            Data.getInstance().updateVirtualCategories();
            publishProgress(++progress);
            Data.getInstance().updateCategories(false);
            publishProgress(taskCount);
            Data.getInstance().updateFeeds(Data.VCAT_ALL, false);
            
            return null;
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values[0] == taskCount) {
                setSupportProgressBarIndeterminateVisibility(false);
                setSupportProgressBarVisibility(false);
                return;
            }
            
            setProgress((10000 / (taskCount + 1)) * values[0]);
        }
        
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
            
            case DIALOG_VACUUM:
                
                builder.setTitle("VACUUM");
                builder.setMessage("The DB should sometimes be vacuumed to free space. Do you want to start this process now? It may take up to several minutes.");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface d, final int which) {
                        new VacuumTask(ProgressDialog.show(context, "VACUUM", "Cleaning the database...", true)).execute();
                        d.dismiss();
                    }
                });
                builder.setNegativeButton("Not now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface d, final int which) {
                        d.dismiss();
                    }
                });
                break;
                
        }
        return builder.create();
    }
    
    private class VacuumTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog dialog = null;
        public VacuumTask(ProgressDialog dialog) {
            this.dialog = dialog;
        }
        protected Void doInBackground(Void... args) {
            try {
                DBHelper.getInstance().vacuum();
            } finally {
                // Reset scheduling-data
                Controller.getInstance().setVacuumDBScheduled(false);
                Controller.getInstance().setLastVacuumDate();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();
        }
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
    
    @Override
    public void setAdapter(MainAdapter adapter) {
        if (adapter instanceof CategoryAdapter)
            this.adapter = (CategoryAdapter) adapter;
    }
    
    @Override
    public void itemSelected(TYPE type, int selectedIndex, int oldIndex) {
        // Log.d(Utils.TAG, this.getClass().getName() + " - itemSelected called. Type: " + type);
        if (adapter == null) {
            Log.w(Utils.TAG, "CategoryActivity: Adapter shouldn't be null here...");
            return;
        }
        
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
        int selectedId = adapter.getId(selectedIndex);
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
            
            Log.d(Utils.TAG, "Filling right pane... (" + selectedIndex + " " + oldIndex + ")");
            
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
                    feedHeadlineFragment = FeedHeadlineListFragment.newInstance(selectedId,
                            adapter.getTitle(selectedIndex), 0, false);
                    ft.replace(R.id.feed_headline_list, feedHeadlineFragment);
                    break;
                case SELECTED_LABEL:
                    feedHeadlineFragment = FeedHeadlineListFragment.newInstance(selectedId,
                            adapter.getTitle(selectedIndex), -2, false);
                    ft.replace(R.id.feed_headline_list, feedHeadlineFragment);
                    break;
                case SELECTED_CATEGORY:
                    feedFragment = FeedListFragment.newInstance(selectedId, adapter.getTitle(selectedIndex));
                    ft.replace(R.id.feed_list, feedFragment);
                    break;
            }
            
            // Replace the old fragment with the new one
            // Use a fade animation. This makes it clear that this is not a new "layer"
            // above the current, but a replacement
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
            
        } else {
            
            // This is not a tablet - start a new activity
            Intent i = null;
            switch (selection) {
                case SELECTED_VIRTUAL_CATEGORY:
                    i = new Intent(context, FeedHeadlineActivity.class);
                    i.putExtra(FeedHeadlineActivity.FEED_ID, selectedId);
                    i.putExtra(FeedHeadlineActivity.FEED_TITLE, adapter.getTitle(selectedIndex));
                    break;
                case SELECTED_LABEL:
                    i = new Intent(context, FeedHeadlineActivity.class);
                    i.putExtra(FeedHeadlineActivity.FEED_ID, selectedId);
                    i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, -2);
                    i.putExtra(FeedHeadlineActivity.FEED_TITLE, adapter.getTitle(selectedIndex));
                    break;
                case SELECTED_CATEGORY:
                    i = new Intent(context, FeedActivity.class);
                    i.putExtra(FeedActivity.FEED_CAT_ID, selectedId);
                    i.putExtra(FeedActivity.FEED_CAT_TITLE, adapter.getTitle(selectedIndex));
                    break;
            }
            if (i != null)
                startActivity(i);
            
        }
    }
    
    @Override
    protected void onDataChanged(int type, int id, int superId) {
        if (type == UpdateController.TYPE_CATEGORY)
            doRefresh();
        // Listen for Feed-Events too, Virtual Categories are defined as feeds
        if (type == UpdateController.TYPE_FEED && id < 1)
            doRefresh();
    }
    
}
