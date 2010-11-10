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

import org.ttrssreader.gui.IUpdateEndListener;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

public class Updater extends AsyncTask<Void, Void, Void> {
    
    private IUpdateEndListener mParent;
    private IUpdatable mUpdatable;
    
    public Updater(IUpdateEndListener parent, IUpdatable updatable) {
        mParent = parent;
        mUpdatable = updatable;
    }
    
    private Handler handler = new Handler() {
        
        @Override
        public void handleMessage(Message msg) {
            if (mParent != null) {
                mParent.onUpdateEnd();
            }
        }
    };
    
    @Override
    protected Void doInBackground(Void... params) {
        mUpdatable.update();
        handler.sendEmptyMessage(0);
        return null;
    }
    
}
