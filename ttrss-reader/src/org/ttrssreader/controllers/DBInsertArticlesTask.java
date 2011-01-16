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

package org.ttrssreader.controllers;

import java.util.LinkedHashSet;
import java.util.Set;
import org.ttrssreader.model.pojos.ArticleItem;
import android.os.AsyncTask;

public class DBInsertArticlesTask extends AsyncTask<Set<ArticleItem>, Void, Void> {
    
    private int mMaxArticles;
    long time;
    
    public DBInsertArticlesTask(int maxArticles) {
        mMaxArticles = maxArticles;
        time = System.currentTimeMillis();
    }
    
    @Override
    protected Void doInBackground(Set<ArticleItem>... args) {
        if (args[0] != null && args[0] instanceof LinkedHashSet<?>) {
            
            Set<ArticleItem> set = args[0];
            
            if (set.size() > 0) {
                DBHelper.getInstance().insertArticles(set, mMaxArticles);
            }
        }
        return null;
    }
    
}
