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

package org.ttrssreader.gui.fragments;

import org.ttrssreader.gui.interfaces.IConfigurable;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.model.FeedHeadlineAdapter;
import org.ttrssreader.utils.Utils;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class FeedHeadlineListFragment extends ListFragment {

    private static final TYPE THIS_TYPE = TYPE.FEEDHEADLINE;
    
    public static final String FEED_CAT_ID = "FEED_CAT_ID";
    public static final String FEED_ID = "ARTICLE_FEED_ID";
    public static final String FEED_TITLE = "FEED_TITLE";
    public static final String FEED_SELECT_ARTICLES = "FEED_SELECT_ARTICLES";
    public static final String FEED_INDEX = "INDEX";

    private static final String SELECTED_INDEX = "selectedIndex";
    private static final int SELECTED_INDEX_DEFAULT = -1;
    private int selectedIndex = SELECTED_INDEX_DEFAULT;
    private int selectedIndexOld = SELECTED_INDEX_DEFAULT;
    
    // Extras
    private int categoryId = -1000;
    private int feedId = -1000;
    private String feedTitle = null;
    private boolean selectArticlesForCategory = false;
    
    private FeedHeadlineAdapter adapter = null;
    private ListView listView;
    
    public static FeedHeadlineListFragment newInstance(int id, String title, int categoryId, boolean selectArticles) {
        // Create a new fragment instance
        FeedHeadlineListFragment detail = new FeedHeadlineListFragment();
        detail.categoryId = categoryId;
        detail.feedId = id;
        detail.feedTitle = title;
        detail.selectArticlesForCategory = selectArticles;
        detail.setHasOptionsMenu(true);
        return detail;
    }
    
    @Override
    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);

        listView = getListView();
        registerForContextMenu(listView);
        
        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            categoryId = extras.getInt(FEED_CAT_ID);
            feedId = extras.getInt(FEED_ID);
            feedTitle = extras.getString(FEED_TITLE);
            selectArticlesForCategory = extras.getBoolean(FEED_SELECT_ARTICLES);
        } else if (instance != null) {
            categoryId = instance.getInt(FEED_CAT_ID);
            feedId = instance.getInt(FEED_ID);
            feedTitle = instance.getString(FEED_TITLE);
            selectArticlesForCategory = instance.getBoolean(FEED_SELECT_ARTICLES);
            selectedIndex = instance.getInt(SELECTED_INDEX, SELECTED_INDEX_DEFAULT);
        }
        
        adapter = new FeedHeadlineAdapter(getActivity().getApplicationContext(), feedId, categoryId,
                selectArticlesForCategory);
        setListAdapter(adapter);
        
        // Inject Adapter into activity. Don't know if this is the way to do stuff here...
        if (getActivity() instanceof IConfigurable)
            ((IConfigurable) getActivity()).setAdapter(adapter);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(FEED_CAT_ID, categoryId);
        outState.putInt(FEED_ID, feedId);
        outState.putString(FEED_TITLE, feedTitle);
        outState.putBoolean(FEED_SELECT_ARTICLES, selectArticlesForCategory);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (adapter == null) {
            Log.d(Utils.TAG, "Adapter shouldn't be null here...");
            return;
        }

        selectedIndexOld = selectedIndex;
        selectedIndex = position; // Set selected item
        
        if (getActivity() instanceof IItemSelectedListener)
            ((IItemSelectedListener) getActivity()).itemSelected(THIS_TYPE, selectedIndex, selectedIndexOld);
    }
    
}
