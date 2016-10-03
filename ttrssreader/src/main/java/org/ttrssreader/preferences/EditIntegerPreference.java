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

package org.ttrssreader.preferences;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

@SuppressWarnings("UnusedDeclaration")
public class EditIntegerPreference extends EditTextPreference {

	public EditIntegerPreference(Context context) {
		super(context);
	}

	public EditIntegerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EditIntegerPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected String getPersistedString(String defaultReturnValue) {
		return String.valueOf(getPersistedInt(-1));
	}

	@Override
	protected boolean persistString(String value) {
		return !(value == null || value.length() == 0) && persistInt(Integer.parseInt(value));
	}

}
