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

package org.ttrssreader.gui.view;

import android.view.GestureDetector;
import android.view.MotionEvent;

import org.ttrssreader.controllers.Controller;

import androidx.appcompat.app.ActionBar;

public class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {

	@SuppressWarnings("unused")
	private static final String TAG = MyGestureDetector.class.getSimpleName();

	private ActionBar actionBar;
	private boolean hideActionbar;
	private long lastShow = -1;

	public MyGestureDetector(ActionBar actionBar, boolean hideActionbar) {
		this.actionBar = actionBar;
		this.hideActionbar = hideActionbar;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if (!hideActionbar || actionBar == null)
			return false;

		if (Controller.isTablet)
			return false;

		if (System.currentTimeMillis() - lastShow < 700)
			return false;

		if (Math.abs(distanceX) > Math.abs(distanceY))
			return false;

		if (distanceY < -10) {
			actionBar.show();
			lastShow = System.currentTimeMillis();
		} else if (distanceY > 10) {
			actionBar.hide();
			lastShow = System.currentTimeMillis();
		}

		return false;
	}

}
