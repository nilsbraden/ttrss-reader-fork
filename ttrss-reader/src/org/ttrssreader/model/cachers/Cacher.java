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

package org.ttrssreader.model.cachers;

import org.ttrssreader.gui.interfaces.ICacheEndListener;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

public class Cacher extends AsyncTask<Void, Void, Void> {
    
    private ICacheEndListener mParent;
    private ICacheable mCacheable;
    
    public Cacher(ICacheEndListener parent, ICacheable cacheable) {
        mParent = parent;
        mCacheable = cacheable;
    }
    
    private Handler handler = new Handler() {
        
        @Override
        public void handleMessage(Message msg) {
            if (mParent != null) {
                mParent.onCacheEnd();
            }
        }
    };
    
    @Override
    protected Void doInBackground(Void... params) {
        mCacheable.cache();
        handler.sendEmptyMessage(0);
        return null;
    }
    
}
