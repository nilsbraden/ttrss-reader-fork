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

package org.ttrssreader.model.article;

import org.ttrssreader.controllers.DataController;
import org.ttrssreader.utils.Utils;
import android.os.AsyncTask;
import android.util.Log;

public class ArticleUpdateTask extends AsyncTask<String, Integer, Boolean> {
	
	protected Boolean doInBackground(String... ids) {
		
		DataController.getInstance().forceFullRefresh();
		for (int i = 0; i < ids.length; i++) {
			String id = ids[i];
			
			if (id != null && !id.equals("")) {
				Log.i(Utils.TAG, "doInBackground - getArticleWithContent(articleId: " + ids[i] + ")");
				DataController.getInstance().getArticleWithContent(ids[i]);
			}
		}
		DataController.getInstance().disableForceFullRefresh();
		
		return true;
	}
	
}
