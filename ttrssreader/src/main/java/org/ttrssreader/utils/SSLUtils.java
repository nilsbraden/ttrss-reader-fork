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

import android.annotation.SuppressLint;
import android.util.Log;

import org.ttrssreader.MyApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SSLUtils {

	private static final String TAG = SSLUtils.class.getSimpleName();
	public volatile static SSLSocketFactory factory;

	public static void initSslSocketFactory(KeyManager[] km, TrustManager[] tm) throws KeyManagementException, NoSuchAlgorithmException {

		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(km, tm, null);
		factory = ctx.getSocketFactory();
	}

	public static void initPrivateKeystore(String password) throws GeneralSecurityException {
		Log.i(TAG, "Enabling SSLUtils to trust certificates from private keystore.");
		KeyStore keystore = loadKeystore(password);
		if (keystore == null)
			return;

		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(keystore);

		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keystore, password.toCharArray());

		initSslSocketFactory(kmf.getKeyManagers(), tmf.getTrustManagers());
	}

	private static KeyStore loadKeystore(String keystorePassword) throws GeneralSecurityException {
		KeyStore trusted;
		try {
			trusted = KeyStore.getInstance(KeyStore.getDefaultType());

			File file = new File(MyApplication.context().getExternalFilesDir(null), "store.bks");

			if (!file.exists())
				return null;

			try (InputStream in = new FileInputStream(file)) {
				trusted.load(in, keystorePassword.toCharArray());
			}
			// Empty!
		} catch (Exception e) {
			throw new GeneralSecurityException("Couldn't load keystore.", e);
		}
		return trusted;
	}

	@SuppressLint("TrustAllX509TrustManager")
	public static void trustAllCert() throws KeyManagementException, NoSuchAlgorithmException {
		Log.i(TAG, "Enabling SSLUtils to trust all CERTIFICATES.");
		X509TrustManager easyTrustManager = new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[]{};
			}

		};

		// Create a trust manager that does not validate certificate chains
		initSslSocketFactory(null, new TrustManager[]{easyTrustManager});
	}

	public static void trustAllHost() {
		Log.i(TAG, "Enabling SSLUtils to trust all HOSTS.");
		try {
			HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> {
				// This thing is supposed to return true since it specifically ignores all errors!
				return true;
			});
		} catch (Exception e) {
			// Empty, HostnameVerifier cannot be null.
		}
	}

	public static void trustClientCert() throws NoSuchAlgorithmException, KeyManagementException {
		KeyManager km = new KeyChainKeyManager();
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(new KeyManager[]{km}, null, null);
		initSslSocketFactory(new KeyManager[]{km}, null);
	}
}
