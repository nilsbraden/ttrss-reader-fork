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

import java.util.ArrayList;
import java.util.List;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.net.JSONConnector.SubscriptionResponse;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.Utils;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.actionbarsherlock.view.Menu;

public class SubscribeActivity extends MenuActivity {
    
    private static final String PARAM_FEEDURL = "feed_url";
    
    private Button okButton;
    private Button feedPasteButton;
    private EditText feedUrl;
    
    private ArrayAdapter<Category> categoriesAdapter;
    private Spinner categorieSpinner;
    
    private ProgressDialog progress;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedsubscribe);
        setTitle(R.string.IntentSubscribe);
        
        String urlValue = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        
        if (savedInstanceState != null) {
            urlValue = savedInstanceState.getString(PARAM_FEEDURL);
        }
        
        feedUrl = (EditText) findViewById(R.id.subscribe_url);
        feedUrl.setText(urlValue);
        
        List<Category> catList = new ArrayList<Category>();
        catList.addAll(DBHelper.getInstance().getAllCategories());
        
        categoriesAdapter = new SimpleCategoryAdapter(context, catList);
        categoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        categorieSpinner = (Spinner) findViewById(R.id.subscribe_categories);
        categorieSpinner.setAdapter(categoriesAdapter);
        
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
        out.putString(PARAM_FEEDURL, url.getText().toString());
    }
    
    public class MyPublisherTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (Controller.getInstance().workOffline()) {
                    showErrorDialog(getResources().getString(R.string.SubscribeActivity_offline));
                    return null;
                }
                
                String urlValue = feedUrl.getText().toString();
                int category = (int) categorieSpinner.getSelectedItemId();
                
                if (!Utils.validateURL(urlValue)) {
                    showErrorDialog(getResources().getString(R.string.SubscribeActivity_invalidUrl));
                    return null;
                }
                
                SubscriptionResponse ret = Data.getInstance().feedSubscribe(urlValue, category);
                String message = "\n\n(" + ret.message + ")";
                
                if (ret.code == 0 || ret.code == 1)
                    finish();
                else if (Controller.getInstance().getConnector().hasLastError())
                    showErrorDialog(Controller.getInstance().getConnector().pullLastError());
                else if (ret.code == 2)
                    showErrorDialog(getResources().getString(R.string.SubscribeActivity_invalidUrl) + " " + message);
                else if (ret.code == 3)
                    showErrorDialog(getResources().getString(R.string.SubscribeActivity_contentIsHTML) + " " + message);
                else if (ret.code == 4)
                    showErrorDialog(getResources().getString(R.string.SubscribeActivity_multipleFeeds) + " " + message);
                else if (ret.code == 5)
                    showErrorDialog(getResources().getString(R.string.SubscribeActivity_cannotDownload) + " " + message);
                else
                    showErrorDialog(String.format(getResources().getString(R.string.SubscribeActivity_errorCode),
                            ret.code) + " " + message);
                
            } catch (Exception e) {
                showErrorDialog(e.getMessage());
            } finally {
                progress.dismiss();
            }
            return null;
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
    @Override protected void doUpdate(boolean forceUpdate) { }
    @Override protected void onDataChanged() { }
    //@formatter:on
    
    class SimpleCategoryAdapter extends ArrayAdapter<Category> {
        public SimpleCategoryAdapter(Context context, List<Category> objects) {
            super(context, android.R.layout.simple_list_item_1, objects);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return initView(position, convertView);
        }
        
        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return initView(position, convertView);
        }
        
        private View initView(int position, View convertView) {
            if (convertView == null)
                convertView = View.inflate(getContext(), android.R.layout.simple_list_item_1, null);
            TextView tvText1 = (TextView) convertView.findViewById(android.R.id.text1);
            tvText1.setText(getItem(position).title);
            return convertView;
        }
    }
    
}
