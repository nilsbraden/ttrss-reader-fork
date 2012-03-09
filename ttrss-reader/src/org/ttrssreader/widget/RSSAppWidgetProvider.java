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
import org.ttrssreader.utils.Utils;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

public class RSSAppWidgetProvider extends AppWidgetProvider {
    
    public final static String WIDGET_ID = "WIDGET_ID_";
    public final static String WIDGET_IS_CATEGORY = "WIDGET_IS_CATEGORY_";
    public final static String WIDGET_UNREAD_ONLY = "WIDGET_UNREAD_ONLY_";
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(Utils.TAG, "Widget: onUpdate");
        
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }
    
    static void updateWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId) {
        
        Log.d(Utils.TAG, "updateWidget(" + appWidgetId + ")");
        
        // Set up the intent that starts the StackViewService, which will
        // provide the views for this collection.
        Intent intent = new Intent(context, RSSWidgetService.class);
        
        // Add the app widget ID to the intent extras.
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        
        // Instantiate the RemoteViews object for the App Widget layout.
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
        
        // Set up the RemoteViews object to use a RemoteViews adapter.
        // This adapter connects
        // to a RemoteViewsService through the specified intent.
        // This is how you populate the data.
        rv.setRemoteAdapter(appWidgetId, R.id.list_view, intent);
        
        // The empty view is displayed when the collection has no items.
        // It should be in the same layout used to instantiate the RemoteViews
        // object above.
        rv.setEmptyView(R.id.list_view, R.id.list_view); // TODO
        
        //
        // Do additional processing specific to this app widget...
        // ...
        // 
        
        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }
    
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(Utils.TAG, "Widget: onDeleted");
        super.onDeleted(context, appWidgetIds);
    }
    
    @Override
    public void onDisabled(Context context) {
        Log.d(Utils.TAG, "Widget: onDisabled");
        super.onDisabled(context);
    }
    
    @Override
    public void onEnabled(Context context) {
        Log.d(Utils.TAG, "Widget: onEnabled");
        super.onEnabled(context);
    }
    
    // Workaround for bug in android 1.5 from https://groups.google.com/group/android-developers/msg/e405ca19df2170e2
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Utils.TAG, "Widget: onReceive");
        final String action = intent.getAction();
        if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
            final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                this.onDeleted(context, new int[] { appWidgetId });
            }
        } else {
            super.onReceive(context, intent);
        }
        
    }
}
