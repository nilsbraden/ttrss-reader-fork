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

import android.os.AsyncTask;

public class ArticleUpdateTask extends AsyncTask<String, Integer, Boolean> {
	
	protected Boolean doInBackground(String... ids) {
		
//		if (!Controller.getInstance().isRefreshSubData()) {
//			return false;
//		}
//		
//		if (!Controller.getInstance().isWorkOffline()) {
//			DataController.getInstance().forceFullRefresh();
//		}
//		
//		if (ids[0] != null && ids[1] != null) {
//			if (!ids[0].equals("")) {
//				Log.i(Utils.TAG, "doInBackground - getArticleWithContent(articleId: " + ids[0] + ")");
//				DataController.getInstance().getArticleWithContent(ids[0]);
//			}
//			
//			if (!ids[1].equals("")) {
//				Log.i(Utils.TAG, "doInBackground - getArticleWithContent(articleId: " + ids[1] + ")");
//				DataController.getInstance().getArticleWithContent(ids[1]);
//			}
//		}
//
//		if (!Controller.getInstance().isWorkOffline()) {
//			DataController.getInstance().disableForceFullRefresh();
//		}
		
		return true;
	}
	
}
