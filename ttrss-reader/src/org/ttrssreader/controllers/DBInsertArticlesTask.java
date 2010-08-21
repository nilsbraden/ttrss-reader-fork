/*
 * Tiny Tiny RSS Reader for Android
 * 
 * Copyright (C) 2009 J. Devauchelle and contributors.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.controllers;

import java.util.ArrayList;
import java.util.List;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.utils.Utils;
import android.os.AsyncTask;
import android.util.Log;


public class DBInsertArticlesTask extends AsyncTask<List<ArticleItem>, Integer, Void>  {
	
	private int mMaxArticles;
	long time;
	
	public DBInsertArticlesTask(int maxArticles) {
		mMaxArticles = maxArticles;
		time = System.currentTimeMillis();
	}

	@Override
	protected Void doInBackground(List<ArticleItem>... args) {
		if (args[0] == null) { 
			return null;
		}
		
		if (args[0] instanceof ArrayList<?>) {
			List<ArticleItem> list = (ArrayList<ArticleItem>) args[0];
			DBHelper.getInstance().insertArticles(list, mMaxArticles);
			Log.i(Utils.TAG, "DBInsertArticlesTask with " + list.size() +
					" articles: " + (System.currentTimeMillis() - time) + "ms");
		}
		
		return null;
	}
	
}
