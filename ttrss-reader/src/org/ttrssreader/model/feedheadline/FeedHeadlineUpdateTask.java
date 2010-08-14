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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.model.feedheadline;

import java.util.List;
import org.ttrssreader.model.article.ArticleItem;
import android.os.AsyncTask;

public class FeedHeadlineUpdateTask extends AsyncTask<String, Integer, List<ArticleItem>> {
	
	protected List<ArticleItem> doInBackground(String... ids) {
//		if (!Controller.getInstance().isRefreshSubData()) {
//			return false;
//		}
//		
//		Log.i(Utils.TAG, "doInBackground - getArticlesWithContent(feedId: " + ids[0] + ")");
//		
//		boolean displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnread();
//		
//		if (!Controller.getInstance().isWorkOffline()) {
//			DataController.getInstance().forceFullRefresh();
//		}
//		
//		DataController.getInstance().getArticlesWithContent(ids[0]);
//		
//		if (ids[1] != null && ids[2] != null) {
//			if (!ids[1].equals("")) {
//				Log.i(Utils.TAG, "doInBackground - getArticlesHeadlines(feedId: " + ids[1] + ")");
//				DataController.getInstance().getArticlesHeadlines(ids[1], displayOnlyUnread);
//			}
//			
//			if (!ids[2].equals("")) {
//				Log.i(Utils.TAG, "doInBackground - getArticlesHeadlines(feedId: " + ids[2] + ")");
//				DataController.getInstance().getArticlesHeadlines(ids[2], displayOnlyUnread);
//			}
//		}
//
//		if (!Controller.getInstance().isWorkOffline()) {
//			DataController.getInstance().disableForceFullRefresh();
//		}
//		
//		return true;
		return null;
	}
	
}
