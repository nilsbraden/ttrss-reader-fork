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

package org.ttrssreader.widget;

import org.ttrssreader.R;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.RemoteViewsService.RemoteViewsFactory;

public class RSSWidgetService extends RemoteViewsService {
    
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RSSRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class RSSRemoteViewsFactory implements RemoteViewsFactory {
    
    private Context context;
    private int appWidgetId;
    
    private long id;
    private boolean isCategory;
    private boolean unreadOnly;
    
    public RSSRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }
    
    @Override
    public void onCreate() {
        // TODO Initialize preferences, Database
        
        // Read configuration
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        id = p.getLong(RSSAppWidgetProvider.WIDGET_ID + appWidgetId, -1L);
        isCategory = p.getBoolean(RSSAppWidgetProvider.WIDGET_IS_CATEGORY + appWidgetId, false);
        unreadOnly = p.getBoolean(RSSAppWidgetProvider.WIDGET_UNREAD_ONLY + appWidgetId, false);
    }
    
    @Override
    public RemoteViews getViewAt(int position) {
        // Construct a remote views item based on the app widget item XML file,
        // and set the text based on the position.
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_item);
        rv.setTextViewText(R.id.title, position + " -> ID: " + id);
        rv.setTextViewText(R.id.updateDate, "Datum. isCat: " + isCategory);
        rv.setTextViewText(R.id.dataSource, "Quelle. unread: " + unreadOnly);
        
        return rv;
    }
    
    public void onDestroy() {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
    }
    
    public int getCount() {
        return -1;
    }
    
    public RemoteViews getLoadingView() {
        // You can create a custom loading view (for instance when getViewAt() is slow.) If you
        // return null here, you will get the default loading view.
        return null;
    }
    
    public int getViewTypeCount() {
        return 1;
    }
    
    public long getItemId(int position) {
        return position;
    }
    
    public boolean hasStableIds() {
        return true;
    }
    
    public void onDataSetChanged() {
        // This is triggered when you call AppWidgetManager notifyAppWidgetViewDataChanged
        // on the collection view corresponding to this factory. You can do heaving lifting in
        // here, synchronously. For example, if you need to process an image, fetch something
        // from the network, etc., it is ok to do it here, synchronously. The widget will remain
        // in its current state while work is being done here, so you don't need to worry about
        // locking up the widget.
    }
    
}
