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

import org.ttrssreader.gui.MenuActivity;

public class ProgressBarManager {

	@SuppressWarnings("unused")
	private static final String TAG = ProgressBarManager.class.getSimpleName();

	private volatile int progressIndeterminateCount = 0;

	// Singleton (see http://stackoverflow.com/a/11165926)
	private ProgressBarManager() {
	}

	private static class InstanceHolder {
		private static final ProgressBarManager instance = new ProgressBarManager();
	}

	public static synchronized ProgressBarManager getInstance() {
		return InstanceHolder.instance;
	}

	public void addProgress(MenuActivity activity) {
		progressIndeterminateCount++;
		setIndeterminateVisibility(activity);
	}

	public void removeProgress(MenuActivity activity) {
		progressIndeterminateCount--;
		if (progressIndeterminateCount <= 0) progressIndeterminateCount = 0;
		setIndeterminateVisibility(activity);
	}

	public void resetProgress(MenuActivity activity) {
		progressIndeterminateCount = 0;
		setIndeterminateVisibility(activity);
	}

	public void setIndeterminateVisibility(MenuActivity activity) {
		boolean visible = (progressIndeterminateCount > 0);
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(visible);
			activity.setSupportProgressBarVisibility(!visible);

			if (!visible) activity.setSupportProgress(0);
		}
	}

}
