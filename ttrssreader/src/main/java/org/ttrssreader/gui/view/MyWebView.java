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

import android.content.Context;
import android.view.View;
import android.webkit.WebView;

public class MyWebView extends WebView {

	//	private static final String TAG = MyWebView.class.getSimpleName();

	public MyWebView(Context context) {
		super(context);
	}

	//	private final OnEdgeReachedListener mOnTopReachedListener = null;
	//	private final OnEdgeReachedListener mOnBottomReachedListener = null;

	@SuppressWarnings("FieldCanBeLocal")
	private final int mMinTopDistance = 0;
	@SuppressWarnings("FieldCanBeLocal")
	private final int mMinBottomDistance = 0;

	/**
	 * Implement this interface if you want to be notified when the WebView has scrolled to the top or bottom.
	 */
	private interface OnEdgeReachedListener {
		void onTopReached(View v, boolean reached);

		void onBottomReached(View v, boolean reached);
	}

	private boolean topReached = true;
	private boolean bottomReached = false;

	@Override
	protected void onScrollChanged(int left, int top, int oldLeft, int oldTop) {
		//		if (mOnTopReachedListener != null)
		//			handleTopReached(top);
		//
		//		if (mOnBottomReachedListener != null)
		//			handleBottomReached(top);

		super.onScrollChanged(left, top, oldLeft, oldTop);
	}

	//	private void handleTopReached(int top) {
	//		boolean reached;
	//		if (top <= mMinTopDistance)
	//			reached = true;
	//		else if (top > (mMinTopDistance * 1.5))
	//			reached = false;
	//		else
	//			return;
	//
	//		if (!reached && !topReached)
	//			return;
	//		if (reached && topReached)
	//			return;
	//
	//		topReached = reached;
	//
	//		mOnTopReachedListener.onTopReached(this, reached);
	//	}
	//
	//	private void handleBottomReached(int top) {
	//		boolean reached;
	//		if ((getContentHeight() - (top + getHeight())) <= mMinBottomDistance)
	//			reached = true;
	//		else if ((getContentHeight() - (top + getHeight())) > (mMinBottomDistance * 1.5))
	//			reached = false;
	//		else
	//			return;
	//
	//		if (!reached && !bottomReached)
	//			return;
	//		if (reached && bottomReached)
	//			return;
	//
	//		bottomReached = reached;
	//
	//		mOnBottomReachedListener.onBottomReached(this, reached);
	//	}

}
