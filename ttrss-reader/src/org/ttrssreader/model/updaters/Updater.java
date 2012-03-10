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

package org.ttrssreader.model.updaters;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.ttrssreader.gui.interfaces.IUpdateEndListener;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

public class Updater extends AsyncTask<Void, Void, Void> {
    
    private static final int END = 0;
    private static final int PROGRESS = 1;
    private IUpdateEndListener parent;
    private IUpdatable updatable;
    
    public Updater(IUpdateEndListener parent, IUpdatable updatable) {
        this.parent = parent;
        this.updatable = updatable;
    }
    
    private Handler handler = new Handler() {
        
        @Override
        public void handleMessage(Message msg) {
            if (parent != null) {
                if (msg.what == END)
                    parent.onUpdateEnd();
                if (msg.what == PROGRESS)
                    parent.onUpdateProgress();
            }
        }
    };
    
    @Override
    protected Void doInBackground(Void... params) {
        updatable.update(this);
        handler.sendEmptyMessage(END);
        return null;
    }
    
    public void progress() {
        handler.sendEmptyMessage(PROGRESS);
    }
    
    @SuppressWarnings("unchecked")
    public AsyncTask<Void, Void, Void> exec() {
        
        if (sExecuteMethod != null) {
            try {
                return (AsyncTask<Void, Void, Void>) sExecuteMethod.invoke(this, AsyncTask.THREAD_POOL_EXECUTOR,
                        (Void[]) null);
            } catch (InvocationTargetException unused) {
                // fall through
            } catch (IllegalAccessException unused) {
                // fall through
            }
        }
        return this.execute();
    }
    
    private Method sExecuteMethod = findExecuteMethod();
    
    private Method findExecuteMethod() {
        // Didn't get Class.getMethod() to work so I just search for the right method-name and take the first hit.
        Class<?> cls = AsyncTask.class;
        for (Method m : cls.getMethods()) {
            if ("executeOnExecutor".equals(m.getName()))
                return m;
        }
        return null;
    }
    
}
