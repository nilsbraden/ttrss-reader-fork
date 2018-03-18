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

import android.os.Handler;
import android.os.Message;

import org.ttrssreader.gui.interfaces.IDataChangedListener;

import java.util.ArrayList;
import java.util.List;

public class UpdateController {

	@SuppressWarnings("unused")
	private static final String TAG = UpdateController.class.getSimpleName();

	private static List<IDataChangedListener> listeners = new ArrayList<>();
	private static Handler handler = new UpdateControllerHandler();

	// Singleton (see http://stackoverflow.com/a/11165926)
	private UpdateController() {
	}

	private static class InstanceHolder {
		private static final UpdateController instance = new UpdateController();
	}

	public static UpdateController getInstance() {
		return InstanceHolder.instance;
	}

	private static class UpdateControllerHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			for (IDataChangedListener listener : UpdateController.listeners) {
				listener.dataChanged();
			}
		}
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
