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

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.net.JSONConnector.SubscriptionResponse;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.CustomCursorLoader;
import org.ttrssreader.utils.Utils;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import com.actionbarsherlock.view.Menu;

public class SubscribeActivity extends MenuActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    
    private static final String PARAM_FEEDURL = "feed_url";
    private static final String PARAM_CATEGORY = "category_id";
    
    private Button okButton;
    private Button feedPasteButton;
    private EditText feedUrl;
    
    private SimpleCursorAdapter categoriesAdapter;
    private Spinner categorieSpinner;
    private int selectedCategory = -1;
    
    private ProgressDialog progress;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedsubscribe);
        
        String urlValue = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        
        if (savedInstanceState != null) {
            urlValue = savedInstanceState.getString(PARAM_FEEDURL);
            selectedCategory = savedInstanceState.getInt(PARAM_CATEGORY);
        }
        
        feedUrl = (EditText) findViewById(R.id.subscribe_url);
        feedUrl.setText(urlValue);
        
        // Map column "title" to field text1
        categoriesAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
                new String[] { "title" }, new int[] { android.R.id.text1 }, 0);
        categoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        categorieSpinner = (Spinner) findViewById(R.id.subscribe_categories);
        categorieSpinner.setAdapter(categoriesAdapter);
        getSupportLoaderManager().initLoader(0, null, this);
        
        okButton = (Button) findViewById(R.id.subscribe_ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progress = ProgressDialog.show(context, null, "Sending...");
                new MyPublisherTask().execute();
            }
        });
        
        feedPasteButton = (Button) findViewById(R.id.subscribe_paste);
        feedPasteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = Utils.getTextFromClipboard(activity);
                String text = feedUrl.getText() != null ? feedUrl.getText().toString() : "";
                int start = feedUrl.getSelectionStart();
                int end = feedUrl.getSelectionEnd();
                // Insert text at current position, replace text if selected
                text = text.substring(0, start) + url + text.substring(end, text.length());
                feedUrl.setText(text);
                feedUrl.setSelection(end);
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Enable/Disable Paste-Button:
        feedPasteButton.setEnabled(Utils.clipboardHasText(this));
        doRefresh();
    }
    
    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        
        EditText url = (EditText) findViewById(R.id.subscribe_url);
        Spinner categories = (Spinner) findViewById(R.id.subscribe_categories);
        out.putString(PARAM_FEEDURL, url.getText().toString());
        out.putInt(PARAM_CATEGORY, categories.getSelectedItemPosition());
    }
    
    public class MyPublisherTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (Controller.getInstance().workOffline()) {
                    showErrorDialog("Working offline, can't request action from server.");
                    return null;
                }
                
                String urlValue = feedUrl.getText().toString();
                int category = (int) categorieSpinner.getSelectedItemId();
                
                if (!Utils.validateURL(urlValue)) {
                    showErrorDialog("URL seems to be invalid.");
                    return null;
                }
                
                SubscriptionResponse ret = Data.getInstance().feedSubscribe(urlValue, category);
                String message = "\n\n(" + ret.message + ")";
                
                if (ret.code == 0 || ret.code == 1)
                    finishCompat();
                else if (Controller.getInstance().getConnector().hasLastError())
                    showErrorDialog(Controller.getInstance().getConnector().pullLastError());
                else if (ret.code == 2)
                    showErrorDialog("Server returned: Invalid URL." + message);
                else if (ret.code == 3)
                    showErrorDialog("Server returned: URL content is HTML, no feeds available." + message);
                else if (ret.code == 4)
                    showErrorDialog("Server returned: URL content is HTML which contains multiple feeds."
                            + "Here you should call extractfeedurls in rpc-backend to get all possible feeds."
                            + message);
                else if (ret.code == 4)
                    showErrorDialog("Server returned: Couldn't download the URL content." + message);
                else
                    showErrorDialog("Server returned code " + ret.code + " and the following message: " + message);
                
            } catch (Exception e) {
                showErrorDialog(e.getMessage());
            } finally {
                progress.dismiss();
            }
            return null;
        }
        
        private void finishCompat() {
            finishActivity(0);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (super.onCreateOptionsMenu(menu)) {
            menu.removeGroup(R.id.group_normal_primary);
            menu.removeGroup(R.id.group_normal_secondary);
        }
        return true;
    }
    
    // @formatter:off // Not needed here:
    @Override public void itemSelected(TYPE type, int selectedIndex, int oldIndex, int selectedId) { }
    @Override protected void doRefresh() {
        super.doRefresh();
    }
    @Override protected void doUpdate(boolean forceUpdate) { }
    @Override protected void onDataChanged() { }
    //@formatter:on
    
    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        // Loads uncategorized Feeds and all other categories afterwards
        StringBuilder query = new StringBuilder();
        query.append(" SELECT 1 a, id _id, title FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE id=0 ");
        query.append(" UNION ");
        query.append(" SELECT 2 a, id _id, title FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE id>0 ORDER BY a, title ASC ");
        return new CustomCursorLoader(this, query.toString(), null);
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        categoriesAdapter.changeCursor(data);
        categorieSpinner.setSelection(selectedCategory);
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        categoriesAdapter.changeCursor(null);
    }
    
}
