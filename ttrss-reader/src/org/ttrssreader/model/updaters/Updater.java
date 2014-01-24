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
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.WeakReferenceHandler;
import android.app.Activity;
import android.os.Message;

public class Updater extends AsyncTask<Void, Void, Void> {
    
    private IUpdatable updatable;

    public Updater(Activity parent, IUpdatable updatable) {
        this(parent, updatable, false);
    }
    
    public Updater(Activity parent, IUpdatable updatable, boolean goBackAfterUpdate) {
        this.updatable = updatable;
        if (parent instanceof IUpdateEndListener)
            this.handler = new MsgHandler((IUpdateEndListener) parent, goBackAfterUpdate);
    }
    
    // Use handler with weak reference on parent object
    private static class MsgHandler extends WeakReferenceHandler<IUpdateEndListener> {
        boolean goBackAfterUpdate;
        public MsgHandler(IUpdateEndListener parent, boolean goBackAfterUpdate) {
            super(parent);
            this.goBackAfterUpdate = goBackAfterUpdate;
        }
        
        @Override
        public void handleMessage(IUpdateEndListener parent, Message msg) {
            parent.onUpdateEnd(goBackAfterUpdate);
        }
    }
    
    private MsgHandler handler;
    
    @Override
    protected Void doInBackground(Void... params) {
        updatable.update(this);
        if (handler != null)
            handler.sendEmptyMessage(0);
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public AsyncTask<Void, Void, Void> exec() {
        
        if (sExecuteMethod != null) {
            try {
                return (AsyncTask<Void, Void, Void>) sExecuteMethod.invoke(this, AsyncTask.THREAD_POOL_EXECUTOR,
                        (Void[]) null);
            } catch (InvocationTargetException unused) {
            } catch (IllegalAccessException unused) {
            }
        }
        return this.execute();
    }
    
    private Method sExecuteMethod = findExecuteMethod();
    
    // Why do we use this here though we have a method for this in Controller.isExecuteOnExecutorAvailable()?
    // -> There are many places we would have to alter and it would be the same code over and over again, so just leave
    // this here, it works.
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
