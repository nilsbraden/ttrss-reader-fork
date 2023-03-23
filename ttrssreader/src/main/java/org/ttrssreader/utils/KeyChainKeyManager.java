package org.ttrssreader.utils;

import android.security.KeyChain;
import android.security.KeyChainException;

import org.ttrssreader.MyApplication;
import org.ttrssreader.controllers.Controller;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509ExtendedKeyManager;

/**
 * A KeyManager that retrieves its keys from the Android credential storage.
 *
 * @see KeyChain
 */
public class KeyChainKeyManager extends X509ExtendedKeyManager {
	/**
	 * Returns the currently selected certificate alias from the app preferences
	 */
	@Override
	public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
		return Controller.getInstance().getClientCertificateAlias();
	}

	/**
	 * Retrieves the certificate chain for the alias.  This takes time, so do not call from main thread.
	 */
	@Override
	public X509Certificate[] getCertificateChain(String alias) {
		try {
			return KeyChain.getCertificateChain(MyApplication.context(), alias);
		} catch (KeyChainException | InterruptedException e) {
			throw new RuntimeException("failed to get certificate chain for " + alias); // e may contain TMI
		}
	}

	/**
	 * Retrieves the private key for the alias.  This takes time, so do not call from main thread.
	 */
	@Override
	public PrivateKey getPrivateKey(String alias) {
		try {
			return KeyChain.getPrivateKey(MyApplication.context(), alias);
		} catch (KeyChainException | InterruptedException e) {
			throw new RuntimeException("failed to get private key for " + alias); // e may contain TMI
		}
	}

	/**
	 * Unsupported, should never be called.
	 */
	@Override
	public String[] getClientAliases(String s, Principal[] principals) {
		throw new UnsupportedOperationException("should never be called");
	}

	/**
	 * Unsupported, should never be called.
	 */
	@Override
	public String[] getServerAliases(String s, Principal[] principals) {
		throw new UnsupportedOperationException("should never be called");
	}

	/**
	 * Unsupported, should never be called.
	 */
	@Override
	public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
		throw new UnsupportedOperationException("should never be called");
	}
}
