/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
 * Copyright (C) 2009-2010 J. Devauchelle.
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

package org.ttrssreader.model;

import java.util.Set;
import org.ttrssreader.gui.IRefreshEndListener;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

public class Refresher extends AsyncTask<String, Integer, Set<?>> {
    
    private IRefreshEndListener mParent;
    private IRefreshable mRefreshable;
    
    public Refresher(IRefreshEndListener parent, IRefreshable refreshable) {
        mParent = parent;
        mRefreshable = refreshable;
    }
    
    private Handler handler = new Handler() {
        
        public void handleMessage(Message msg) {
            mParent.onRefreshEnd();
        }
    };
    
    @Override
    protected Set<?> doInBackground(String... params) {
        Set<?> ret = mRefreshable.refreshData();
        handler.sendEmptyMessage(0);
        
        return ret;
    }
    
}
