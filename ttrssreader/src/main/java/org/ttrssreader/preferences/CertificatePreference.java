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

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.Build;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.util.AttributeSet;
import android.util.Log;

import org.ttrssreader.controllers.Controller;

import androidx.preference.Preference;

/**
 * Preference that uses the Android certificate selection dialog to select and authorize a cert
 */
public class CertificatePreference extends Preference {
	private static final String TAG = CertificatePreference.class.getSimpleName();

	public CertificatePreference(Context context) {
		super(context);
	}

	public CertificatePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CertificatePreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onClick() {
		// have the Android cert selection dialog show the connection url
		Uri connectionUri = null;
		String connectionUrl = Controller.getInstance().hostname();
		try {
			connectionUri = Uri.parse(connectionUrl);
		} catch (Exception e) {
			Log.w(TAG, "could not parse " + connectionUrl, e);
		}
		// also have it preselect the current cert if any
		String preselectCert = getPersistedString(null);
		Log.d(TAG, "preselected was " + preselectCert);

		// show the dialog
		KeyChainAliasCallbackImpl response = new KeyChainAliasCallbackImpl();

		// Use old method if Uri could not be parsed of for lower API levels
		if (connectionUri == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			KeyChain.choosePrivateKeyAlias(getActivity(getContext()), response, null, null, connectionUrl, -1, preselectCert);
		} else {
			KeyChain.choosePrivateKeyAlias(getActivity(getContext()), response, null, null, connectionUri, preselectCert);
		}
	}

	private static Activity getActivity(Context context) {
		if (context instanceof Activity)
			return (Activity) context;
		if (context instanceof ContextWrapper)
			return getActivity(((ContextWrapper) context).getBaseContext());
		return null;
	}

	/**
	 * Callback from the Android cert selection dialog, to inform us of what has been selected
	 */
	class KeyChainAliasCallbackImpl implements KeyChainAliasCallback {
		@Override
		public void alias(String alias) {
			persistString(alias);
		}
	}
}
