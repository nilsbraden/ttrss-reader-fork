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

import android.support.v4.app.Fragment;

public class ArticleFragment extends Fragment {

    public static final int ARTICLE_LAST_MOVE_DEFAULT = 0;
    
    private int articleId = -1;
    private int feedId = -1;
    private int categoryId = -1000;
    private boolean selectArticlesForCategory = false;
    private int lastMove = ARTICLE_LAST_MOVE_DEFAULT;
    
    public static ArticleFragment newInstance(int id, int feedId, int categoryId, boolean selectArticles, int lastMove) {
        // Create a new fragment instance
        ArticleFragment detail = new ArticleFragment();
        detail.articleId = id;
        detail.feedId = feedId;
        detail.categoryId = categoryId;
        detail.selectArticlesForCategory = selectArticles;
        detail.lastMove = lastMove;
        detail.setHasOptionsMenu(true);
        return detail;
    }
    
}
