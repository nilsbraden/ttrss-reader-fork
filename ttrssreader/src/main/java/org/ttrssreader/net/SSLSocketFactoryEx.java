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

package org.ttrssreader.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * <p>
 * Source: <a href="http://stackoverflow.com/a/27928360">stackoverflow.com</a>
 * </p>
 *
 * @author GaRRaPeTa: http://stackoverflow.com/users/789110/garrapeta
 */
public class SSLSocketFactoryEx extends SSLSocketFactory {

	private static final String TAG = SSLSocketFactoryEx.class.getSimpleName();

	private SSLSocketFactory delegate;
	private String[] m_ciphers;
	private String[] m_protocols;

	public SSLSocketFactoryEx(KeyManager[] km, TrustManager[] tm)
			throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(km, tm, null);
		delegate = ctx.getSocketFactory();

		m_protocols = getProtocolList();
		m_ciphers = getCipherList();
	}

	@Override
	public String[] getDefaultCipherSuites() {
		return delegate.getDefaultCipherSuites();
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return delegate.getSupportedCipherSuites();
	}

	private Socket makeSocketSafe(Socket socket) {
		if (socket instanceof SSLSocket) {
			SSLSocket ss = (SSLSocket) socket;
			ss.setEnabledProtocols(m_protocols);
			ss.setEnabledCipherSuites(m_ciphers);
			return ss;
		}

		return socket;
	}

	@Override
	public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
		return makeSocketSafe(delegate.createSocket(s, host, port, autoClose));
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException {
		return makeSocketSafe(delegate.createSocket(host, port));
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
		return makeSocketSafe(delegate.createSocket(host, port, localHost, localPort));
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		return makeSocketSafe(delegate.createSocket(host, port));
	}

	@Override
	public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
			throws IOException {
		return makeSocketSafe(delegate.createSocket(address, port, localAddress, localPort));
	}

	private String[] getProtocolList() {
		String[] preferredProtocols = {"TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};
		String[] availableProtocols;

		SSLSocket socket = null;

		try {
			socket = (SSLSocket) delegate.createSocket();

			availableProtocols = socket.getSupportedProtocols();
			Arrays.sort(availableProtocols);
		} catch (Exception e) {
			return new String[] {"TLSv1"};
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					// Empty!
				}
			}
		}

		List<String> aa = new ArrayList<>();
		for (String protocol : preferredProtocols) {
			if (Arrays.binarySearch(availableProtocols, protocol) >= 0) aa.add(protocol);
		}

		return aa.toArray(new String[aa.size()]);
	}

	private String[] getCipherList() {
		String[] preferredCiphers = {
				// @formatter:off
				// *_CHACHA20_POLY1305 are 3x to 4x faster than existing cipher suites.
				// http://googleonlinesecurity.blogspot.com/2014/04/speeding-up-and-strengthening-https.html
				// Use them if available. Normative names can be found at (TLS spec depends on IPSec spec):
				// http://tools.ietf.org/html/draft-nir-ipsecme-chacha20-poly1305-01
				// http://tools.ietf.org/html/draft-mavrogiannopoulos-chacha-tls-02
				"TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305",
				"TLS_ECDHE_ECDSA_WITH_CHACHA20_SHA",
				"TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305",
				"TLS_ECDHE_RSA_WITH_CHACHA20_SHA",

				"TLS_DHE_RSA_WITH_CHACHA20_POLY1305",
				"TLS_DHE_RSA_WITH_CHACHA20_SHA",
				"TLS_RSA_WITH_CHACHA20_POLY1305",
				"TLS_RSA_WITH_CHACHA20_SHA",

				// Done with bleeding edge, back to TLS v1.2 and below
				"TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
				"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
				"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
				"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",

				"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
				"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
				"TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
				"TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",

				// TLS v1.0 (with some SSLv3 interop)
				"TLS_DHE_RSA_WITH_AES_256_CBC_SHA384",
				"TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
				"TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
				"TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
				"TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
				"TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
				"TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
				"TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA",

				// Removed SSLv3 cipher suites (see
				// http://www.openssl.org/docs/apps/ciphers.html#SSL_v3_0_cipher_suites_ for lists of cipher suite
				// names):
				// "SSL_DH_RSA_WITH_3DES_EDE_CBC_SHA",
				// "SSL_DH_DSS_WITH_3DES_EDE_CBC_SHA",

				// RSA key transport sucks, but they are needed as a fallback.
				// For example, microsoft.com fails under all versions of TLS
				// if they are not included. If only TLS 1.0 is available at
				// the client, then google.com will fail too. TLS v1.3 is
				// trying to deprecate them, so it will be interesteng to see
				// what happens.
				"TLS_RSA_WITH_AES_256_CBC_SHA256",
				"TLS_RSA_WITH_AES_128_CBC_SHA256",
				"TLS_RSA_WITH_AES_256_CBC_SHA",
				"TLS_RSA_WITH_AES_128_CBC_SHA",

				// Cipher-Suites that were disabled but I couldn't find any reason why:
				"TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
				"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
				"TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",

				"TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
				"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
				"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",

				"TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
				"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
				"TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",

				"TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
				"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
				"TLS_ECDH_RSA_WITH_AES_256_CBC_SHA"
				// @formatter:on
		};

		String[] availableCiphers;

		try {
			availableCiphers = delegate.getSupportedCipherSuites();
			Arrays.sort(availableCiphers);
		} catch (Exception e) {
			return new String[] {
					// @formatter:off
					"TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
					"TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
					"TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
					"TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
					"TLS_RSA_WITH_AES_256_CBC_SHA256",
					"TLS_RSA_WITH_AES_256_CBC_SHA",
					"TLS_RSA_WITH_AES_128_CBC_SHA256",
					"TLS_RSA_WITH_AES_128_CBC_SHA",
					"TLS_EMPTY_RENEGOTIATION_INFO_SCSV"
					// @formatter:on
			};
		}

		List<String> aa = new ArrayList<>();
		for (String cipher : preferredCiphers) {
			if (Arrays.binarySearch(availableCiphers, cipher) >= 0) aa.add(cipher);
		}

		aa.add("TLS_EMPTY_RENEGOTIATION_INFO_SCSV");

		return aa.toArray(new String[aa.size()]);
	}

}
