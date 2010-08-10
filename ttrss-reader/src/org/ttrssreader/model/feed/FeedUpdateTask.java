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

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DataController;
import org.ttrssreader.utils.Utils;
import android.os.AsyncTask;
import android.util.Log;

public class FeedUpdateTask extends AsyncTask<String, Integer, Boolean> {
	
	protected Boolean doInBackground(String... ids) {
		
		Log.i(Utils.TAG, "doInBackground - getArticlesHeadlines(feedId: " + ids[0] + ")");
		
		boolean displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnread();
		
		DataController.getInstance().forceFullRefresh();
		for (FeedItem f : DataController.getInstance().getSubscribedFeeds(ids[0], displayOnlyUnread)) {
			DataController.getInstance().getArticlesHeadlines(f.getId(), displayOnlyUnread);
		}
		DataController.getInstance().disableForceFullRefresh();
		
		return true;
	}
	
}
