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

package org.ttrssreader.model.article;

import org.ttrssreader.controllers.DataController;
import org.ttrssreader.model.IUpdatable;
import android.os.AsyncTask;

public class ArticleReadStateUpdater extends AsyncTask<Object, Object, Object> implements IUpdatable {
	
	private Object mObject = null;
	
	private String mPid;
	
	public ArticleReadStateUpdater(Object object, String pid) {
		mObject = object;
		mPid = pid;
	}
	
	@Override
	protected Object doInBackground(Object... params) {
		update();
		return null;
	}
	
	@Override
	public void update() {
		DataController.getInstance().markItemRead(mObject, mPid);
	}

}
