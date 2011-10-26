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

import org.ttrssreader.R;
import org.ttrssreader.gui.ArticleActivity;
import org.ttrssreader.gui.FeedHeadlineActivity;
import org.ttrssreader.gui.interfaces.IConfigurable;
import org.ttrssreader.model.FeedHeadlineAdapter;
import org.ttrssreader.utils.Utils;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

public class FeedHeadlineListFragment extends MainListFragment {
    
    public static final String FEED_CAT_ID = "FEED_CAT_ID";
    public static final String FEED_ID = "ARTICLE_FEED_ID";
    public static final String FEED_TITLE = "FEED_TITLE";
    public static final String FEED_SELECT_ARTICLES = "FEED_SELECT_ARTICLES";
    public static final String FEED_INDEX = "INDEX";
    
    // Extras
    private int categoryId = -1000;
    private int feedId = -1000;
    private String feedTitle = null;
    private boolean selectArticlesForCategory = false;
    
    private FeedHeadlineAdapter adapter = null;
    
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
    
    protected void showDetails() {
        
        if (adapter == null) {
            Log.d(Utils.TAG, "Adapter shouldn't be null here...");
            return;
        }
        
        // Decide what kind of item was selected
        int categoryId = adapter.getId(selectedIndex);
        
        if (isTablet) {
            // This is a tablet. Show the recipe in the detail fragment
            Log.d(Utils.TAG, "Filling right pane... (" + selectedIndex + " " + selectedIndexOld + ")");
            
            // Set the list item as checked
            getListView().setItemChecked(selectedIndex, true);
            
            // Get the fragment instance
            ArticleFragment articleView = (ArticleFragment) getFragmentManager().findFragmentById(R.id.articleView);
            
            // Is the current selected ondex the same as the clicked? If so, there is no need to update
            if (articleView != null && selectedIndex == selectedIndexOld)
                return;
            
            articleView = ArticleFragment.newInstance(adapter.getId(selectedIndex), feedId, categoryId,
                    selectArticlesForCategory, ArticleActivity.ARTICLE_MOVE_DEFAULT);
            
            // Replace the old fragment with the new one
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.list, this);
            ft.replace(R.id.details, articleView);
            // Use a fade animation. This makes it clear that this is not a new "layer"
            // above the current, but a replacement
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
            
        } else {
            Log.d(Utils.TAG, "Showing new activity as we are not in 2-pane-mode...");
            
            // This is not a tablet - start a new activity
            // if (!flingDetected) { // TODO!
            Intent i = new Intent(context, ArticleActivity.class);
            i.putExtra(ArticleActivity.ARTICLE_ID, adapter.getId(selectedIndex));
            i.putExtra(ArticleActivity.ARTICLE_FEED_ID, feedId);
            i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, categoryId);
            i.putExtra(FeedHeadlineActivity.FEED_SELECT_ARTICLES, selectArticlesForCategory);
            i.putExtra(ArticleActivity.ARTICLE_MOVE, ArticleActivity.ARTICLE_MOVE_DEFAULT);
            if (i != null)
                startActivity(i);
            // }
            
        }
    }
    
}
