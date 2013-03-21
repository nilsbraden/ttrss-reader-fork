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

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class ArticleFragment extends Fragment {
    
    public static final String ARTICLE_ID = "ARTICLE_ID";
    public static final String ARTICLE_FEED_ID = "ARTICLE_FEED_ID";
    
    public static final String ARTICLE_MOVE = "ARTICLE_MOVE";
    public static final int ARTICLE_MOVE_NONE = 0;
    public static final int ARTICLE_MOVE_DEFAULT = ARTICLE_MOVE_NONE;
    
    // private int articleId = -1;
    // private int feedId = -1;
    // private int categoryId = -1000;
    // private boolean selectArticlesForCategory = false;
    // private int lastMove = ARTICLE_MOVE_DEFAULT;
    
    // private WebView webview;
    
    public static ArticleFragment newInstance(int id, int feedId, int categoryId, boolean selectArticles, int lastMove) {
        // Create a new fragment instance
        ArticleFragment detail = new ArticleFragment();
        // detail.articleId = id;
        // detail.feedId = feedId;
        // detail.categoryId = categoryId;
        // detail.selectArticlesForCategory = selectArticles;
        // detail.lastMove = lastMove;
        detail.setHasOptionsMenu(true);
        detail.setRetainInstance(true);
        return detail;
    }
    
    @Override
    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);
        
        // Bundle extras = getActivity().getIntent().getExtras();
        // if (extras != null) {
        // articleId = extras.getInt(ARTICLE_ID);
        // feedId = extras.getInt(ARTICLE_FEED_ID);
        // categoryId = extras.getInt(FeedHeadlineActivity.FEED_CAT_ID);
        // selectArticlesForCategory = extras.getBoolean(FeedHeadlineActivity.FEED_SELECT_ARTICLES);
        // lastMove = extras.getInt(ARTICLE_MOVE);
        // } else if (instance != null) {
        // articleId = instance.getInt(ARTICLE_ID);
        // feedId = instance.getInt(ARTICLE_FEED_ID);
        // categoryId = instance.getInt(FeedHeadlineActivity.FEED_CAT_ID);
        // selectArticlesForCategory = instance.getBoolean(FeedHeadlineActivity.FEED_SELECT_ARTICLES);
        // lastMove = instance.getInt(ARTICLE_MOVE);
        // }
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // outState.putInt(ARTICLE_ID, articleId);
        // outState.putInt(ARTICLE_FEED_ID, feedId);
        // outState.putInt(FeedHeadlineActivity.FEED_CAT_ID, categoryId);
        // outState.putBoolean(FeedHeadlineActivity.FEED_SELECT_ARTICLES, selectArticlesForCategory);
        // outState.putInt(ARTICLE_MOVE, lastMove);
    }
    
}
