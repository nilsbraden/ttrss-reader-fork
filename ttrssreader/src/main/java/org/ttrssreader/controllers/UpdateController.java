/*
 * Copyright (c) 2015, Nils Braden
 *
 * This file is part of ttrss-reader-fork. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; If
 * not, see http://www.gnu.org/licenses/.
 */

package org.ttrssreader.controllers;

import org.ttrssreader.gui.interfaces.IDataChangedListener;

import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

public class UpdateController {

    @SuppressWarnings("unused")
    private static final String TAG = UpdateController.class.getSimpleName();

    private static UpdateController instance = null;
    private static List<IDataChangedListener> listeners = new ArrayList<>();

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

    void notifyListeners() {
        handler.sendEmptyMessage(0);
    }

}
