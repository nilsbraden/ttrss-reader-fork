/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
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

package org.ttrssreader.controllers;

import java.util.ArrayList;
import java.util.List;
import org.ttrssreader.gui.interfaces.IDataChangedListener;
import android.os.Handler;
import android.os.Message;

public class UpdateController {
    
    @SuppressWarnings("unused")
    private static final String TAG = UpdateController.class.getSimpleName();
    
    private static UpdateController instance = null;
    private static List<IDataChangedListener> listeners = new ArrayList<IDataChangedListener>();
    
    // Singleton
    private UpdateController() {
    }
    
    private static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            for (IDataChangedListener listener : listeners) {
                listener.dataChanged();
            }
        }
    };
    
    public static UpdateController getInstance() {
        if (instance == null) {
            synchronized (UpdateController.class) {
                if (instance == null) {
                    instance = new UpdateController();
                }
            }
        }
        return instance;
    }
    
    public void registerActivity(IDataChangedListener listener) {
        listeners.add(listener);
    }
    
    public void unregisterActivity(IDataChangedListener listener) {
        listeners.remove(listener);
    }
    
    public void notifyListeners() {
        handler.sendEmptyMessage(0);
    }
    
}
