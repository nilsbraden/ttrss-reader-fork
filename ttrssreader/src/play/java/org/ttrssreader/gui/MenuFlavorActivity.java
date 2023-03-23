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

package org.ttrssreader.gui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.dialogs.IgnorableErrorDialog;

import androidx.appcompat.app.AppCompatActivity;

/**
 * This class provides functionality for using the play services library in the build flavour "play".
 */
public abstract class MenuFlavorActivity extends AppCompatActivity implements ProviderInstaller.ProviderInstallListener {

	private static final String TAG = MenuFlavorActivity.class.getSimpleName();

	private static final int ERROR_DIALOG_REQUEST_CODE = 1;
	private boolean mRetryProviderInstall;

	@Override
	protected void onCreate(Bundle instance) {
		super.onCreate(instance);
		if (Controller.getInstance().useProviderInstaller())
			ProviderInstaller.installIfNeededAsync(this, this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ERROR_DIALOG_REQUEST_CODE) {
			// Adding a fragment via GooglePlayServicesUtil.showErrorDialogFragment
			// before the instance state is restored throws an error. So instead,
			// set a flag here, which will cause the fragment to delay until
			// onPostResume.
			mRetryProviderInstall = true;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * After onResume, check to see if we flagged that we need to reinstall the
	 * provider.
	 */
	@Override
	protected void onPostResume() {
		super.onPostResume();
		if (mRetryProviderInstall && Controller.getInstance().useProviderInstaller()) {
			// We can now safely retry installation.
			ProviderInstaller.installIfNeededAsync(this, this);
		}
		mRetryProviderInstall = false;
	}

	@Override
	public void onProviderInstalled() {
		// Provider is up-to-date, app can make secure network calls. Call can be ignored.
		Log.d(TAG, "GooglePlay services: ProviderInstall successfull!");
	}

	@Override
	public void onProviderInstallFailed(int errorCode, Intent recoveryIntent) {
		GoogleApiAvailability api = GoogleApiAvailability.getInstance();
		// Don't annoy the user every time:
		if (!Controller.getInstance().ignoreUnsafeConnectionError()) {

			if (api.isUserResolvableError(errorCode)) {
				// Recoverable error. Show a dialog prompting the user to install/update/enable Google Play services.
				api.showErrorDialogFragment(this, errorCode, ERROR_DIALOG_REQUEST_CODE, dialog -> {
					// The user chose not to take the recovery action
					onProviderInstallerNotAvailable();
				});
			} else {
				// Google Play services is not available.
				onProviderInstallerNotAvailable();
			}

		}
	}

	private void onProviderInstallerNotAvailable() {
		/* This is reached if the provider cannot be updated for some reason. App should consider all HTTP
		communication to be vulnerable, and take appropriate action. */
		Log.w(TAG, getString(R.string.Error_UnsafeConnection));
		if (!Controller.getInstance().ignoreUnsafeConnectionError()) {
			IgnorableErrorDialog.getInstance(getString(R.string.Error_UnsafeConnection)).show(getSupportFragmentManager(), "error");
		}
	}

}
