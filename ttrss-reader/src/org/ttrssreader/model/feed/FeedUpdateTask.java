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

package org.ttrssreader.model.feed;

import java.util.List;
import android.os.AsyncTask;

public class FeedUpdateTask extends AsyncTask<String, Integer, List<FeedItem>> {
	
	protected List<FeedItem> doInBackground(String... ids) {

//		if (!Controller.getInstance().isRefreshSubData()) {
//			return false;
//		}
//		
//		Log.i(Utils.TAG, "doInBackground - getArticlesHeadlines(feedId: " + ids[0] + ")");
//		
//		boolean displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnread();
//		
//		if (!Controller.getInstance().isWorkOffline()) {
//			DataController.getInstance().forceFullRefresh();
//		}
//		
//		for (FeedItem f : DataController.getInstance().getSubscribedFeeds(ids[0], displayOnlyUnread)) {
//			DataController.getInstance().getArticlesHeadlines(f.getId(), displayOnlyUnread);
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
