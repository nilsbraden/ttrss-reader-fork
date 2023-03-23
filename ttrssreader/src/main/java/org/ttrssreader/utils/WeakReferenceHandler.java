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

package org.ttrssreader.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;

/**
 * Source: http://stackoverflow.com/a/13493726 (User: Timmmm)<br>
 * A handler which keeps a weak reference to a fragment. According to
 * Android's lint, references to Handlers can be kept around for a long
 * time - longer than Fragments for example. So we should use handlers
 * that don't have strong references to the things they are handling for.
 * <p>
 * You can use this class to more or less forget about that requirement.
 * Unfortunately you can have anonymous static inner classes, so it is a
 * little more verbose.
 * <p>
 * Example use:
 * <p>
 * private static class MsgHandler extends WeakReferenceHandler<MyFragment>
 * {
 * public MsgHandler(MyFragment fragment) { super(fragment); }
 * <p>
 * "@Override public void handleMessage(MyFragment fragment, Message msg)
 * {
 * fragment.doStuff(msg.arg1);
 * }
 * }"
 * <p>
 * // ...
 * MsgHandler handler = new MsgHandler(this);
 */
public abstract class WeakReferenceHandler<T> extends Handler {
	private final WeakReference<T> mReference;

	public WeakReferenceHandler(Looper looper, T reference) {
		super(looper);
		mReference = new WeakReference<>(reference);
	}


	@Override
	public void handleMessage(@NonNull Message msg) {
		if (mReference.get() == null)
			return;
		handleMessage(mReference.get());
	}

	protected abstract void handleMessage(T reference);
}
