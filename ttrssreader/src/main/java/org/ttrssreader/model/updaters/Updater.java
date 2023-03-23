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

package org.ttrssreader.model.updaters;


import android.app.Activity;
import android.os.Looper;

import org.ttrssreader.gui.interfaces.IUpdateEndListener;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.WeakReferenceHandler;

public class Updater extends AsyncTask<Void, Void, Void> {

	private final IUpdatable updatable;

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
		final boolean goBackAfterUpdate;

		public MsgHandler(IUpdateEndListener parent, boolean goBackAfterUpdate) {
			super(Looper.getMainLooper(), parent);
			this.goBackAfterUpdate = goBackAfterUpdate;
		}

		@Override
		public void handleMessage(IUpdateEndListener parent) {
			parent.onUpdateEnd(goBackAfterUpdate);
		}
	}

	private MsgHandler handler;

	@Override
	protected Void doInBackground(Void... params) {
		updatable.update();
		if (handler != null)
			handler.sendEmptyMessage(0);
		return null;
	}

}
