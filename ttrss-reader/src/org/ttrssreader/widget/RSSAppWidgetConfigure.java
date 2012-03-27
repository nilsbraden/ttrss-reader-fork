/*
 * Copyright (C) 2010-2011 Felix Bechstein
 * 
 * This file is part of Call Meter 3G.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package org.ttrssreader.widget;

import org.ttrssreader.R;
import org.ttrssreader.utils.Utils;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

/**
 * Configure a stats widget.
 * 
 * @author flx
 */
public final class RSSAppWidgetConfigure extends Activity implements OnClickListener {
    
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    
    private Spinner spinnerCategories;
    private Spinner spinnerFeeds;
    private CheckBox cbUnreadOnly;
    private boolean isExistingWidget = false;
    
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setResult(Activity.RESULT_CANCELED);
        
        setTitle(R.string.Widget_widget_config_title);
        setContentView(R.layout.widget_config);
        spinnerCategories = (Spinner) findViewById(R.id.spinnerCategories);
        spinnerFeeds = (Spinner) findViewById(R.id.spinnerFeeds);
        cbUnreadOnly = (CheckBox) findViewById(R.id.unread_only);
        setAdapter();
        findViewById(R.id.ok).setOnClickListener(this);
        findViewById(R.id.cancel).setOnClickListener(this);
    }
    
    private void setAdapter() {
        // final Cursor c = this.getContentResolver().query(DataProvider.Plans.CONTENT_URI, PROJ_ADAPTER,
        // DataProvider.Plans.WHERE_PLANS, null, DataProvider.Plans.NAME);
        
        final Cursor c = null; // TODO: ContentResolver nutzen? Würde sich anbieten...
        String[] fieldName = null;
        final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, c,
                fieldName, new int[] { android.R.id.text1 }, SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        
        int pos = spinnerCategories.getSelectedItemPosition();
        spinnerCategories.setAdapter(adapter);
        spinnerCategories.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                spinnerFeeds.setSelection(-1);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        if (pos >= 0 && pos < spinnerCategories.getCount()) {
            spinnerCategories.setSelection(pos);
        }
        
        // TODO: Das gleiche nochmal für Feed-Liste und spinnerFeeds
    }
    
    // Checks if the widget exists, if it does it calls the load() method to load preferences...
    @Override
    protected void onResume() {
        super.onResume();
        
        final Intent intent = this.getIntent();
        if (intent != null) {
            appWidgetId = intent
                    .getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        isExistingWidget = appWidgetId > 0;
        Log.d(Utils.TAG, "isExistingWidget: " + isExistingWidget);
        load();
    }
    
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.ok:
                long idCat = spinnerCategories.getSelectedItemId();
                long idFeed = spinnerFeeds.getSelectedItemId();
                long id = (idCat > -1 ? idCat : idFeed);
                
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putLong(RSSAppWidgetProvider.WIDGET_ID + appWidgetId, id);
                
                editor.putBoolean(RSSAppWidgetProvider.WIDGET_IS_CATEGORY + appWidgetId, (idCat > -1 ? true : false));
                editor.putBoolean(RSSAppWidgetProvider.WIDGET_UNREAD_ONLY + appWidgetId, this.cbUnreadOnly.isChecked());
                editor.commit();
                
                final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                RSSAppWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId);
                
                final Intent resultIntent = new Intent();
                resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultIntent);
                finish();
                break;
            case R.id.cancel:
                finish();
                break;
            default:
                break;
        }
    }
    
    /**
     * Load widget's configuration.
     */
    private void load() {
        // SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        // long pid = p.getLong(StatsAppWidgetProvider.WIDGET_PLANID + this.mAppWidgetId, -1);
        // SpinnerAdapter adapter = this.spinner.getAdapter();
        // int l = this.spinner.getCount();
        // for (int i = 0; i < l; i++) {
        // if (adapter.getItemId(i) == pid) {
        // this.spinner.setSelection(i);
        // break;
        // }
        // }
        // this.cbHideName.setChecked(p.getBoolean(StatsAppWidgetProvider.WIDGET_HIDETNAME + this.mAppWidgetId, false));
    }
}
