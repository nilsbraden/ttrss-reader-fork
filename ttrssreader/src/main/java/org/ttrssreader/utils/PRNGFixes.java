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

/*
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will Google be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, as long as the origin is not misrepresented.
 */

import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Process;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.SecureRandomSpi;
import java.security.Security;

/**
 * <p>
 * Fixes for the output of the default PRNG having low entropy.
 * </p>
 * <p>
 * The fixes need to be applied via {@link #apply()} before any use of Java Cryptography Architecture primitives. A
 * good
 * place to invoke them is in the application's {@code onCreate}.
 * </p>
 * <p>
 * See: <a
 * href="http://android-developers.blogspot.de/2013/08/some-securerandom-thoughts.html">android-developers.blogspot
 * .de</a>
 * </p>
 */
public final class PRNGFixes {

	private static final String TAG = PRNGFixes.class.getSimpleName();

	private static final byte[] BUILD_FINGERPRINT_AND_DEVICE_SERIAL = getBuildFingerprintAndDeviceSerial();

	/** Hidden constructor to prevent instantiation. */
	private PRNGFixes() {
	}

	/**
	 * Applies all fixes.
	 *
	 * @throws SecurityException if a fix is needed but could not be applied.
	 */
	public static void apply() {
		applyOpenSSLFix();
		installLinuxPRNGSecureRandom();
	}

	/**
	 * Applies the fix for OpenSSL PRNG having low entropy. Does nothing if the
	 * fix is not needed.
	 *
	 * @throws SecurityException if the fix is needed but could not be applied.
	 */
	@SuppressWarnings("PrimitiveArrayArgumentToVariableArgMethod")
	private static void applyOpenSSLFix() throws SecurityException {
		if ((Build.VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN) || (Build.VERSION.SDK_INT
				> VERSION_CODES.JELLY_BEAN_MR2)) {
			// No need to apply the fix
			return;
		}

		try {
			// Mix in the device- and invocation-specific seed.
			Class.forName("org.apache.harmony.xnet.provider.jsse.NativeCrypto").getMethod("RAND_seed", byte[].class)
					.invoke(null, generateSeed());

			// Mix output of Linux PRNG into OpenSSL's PRNG
			int bytesRead = (Integer) Class.forName("org.apache.harmony.xnet.provider.jsse.NativeCrypto")
					.getMethod("RAND_load_file", String.class, long.class).invoke(null, "/dev/urandom", 1024);
			if (bytesRead != 1024) {
				throw new IOException("Unexpected number of bytes read from Linux PRNG: " + bytesRead);
			}
		} catch (Exception e) {
			throw new SecurityException("Failed to seed OpenSSL PRNG", e);
		}
	}

	/**
	 * Installs a Linux PRNG-backed {@code SecureRandom} implementation as the
	 * default. Does nothing if the implementation is already the default or if
	 * there is not need to install the implementation.
	 *
	 * @throws SecurityException if the fix is needed but could not be applied.
	 */
	private static void installLinuxPRNGSecureRandom() throws SecurityException {
		if (Build.VERSION.SDK_INT > VERSION_CODES.JELLY_BEAN_MR2) {
			// No need to apply the fix
			return;
		}

		// Install a Linux PRNG-based SecureRandom implementation as the
		// default, if not yet installed.
		Provider[] secureRandomProviders = Security.getProviders("SecureRandom.SHA1PRNG");
		if ((secureRandomProviders == null) || (secureRandomProviders.length < 1)
				|| (!LinuxPRNGSecureRandomProvider.class.equals(secureRandomProviders[0].getClass()))) {
			Security.insertProviderAt(new LinuxPRNGSecureRandomProvider(), 1);
		}

		// Assert that new SecureRandom() and
		// SecureRandom.getInstance("SHA1PRNG") return a SecureRandom backed
		// by the Linux PRNG-based SecureRandom implementation.
		SecureRandom rng1 = new SecureRandom();
		if (!LinuxPRNGSecureRandomProvider.class.equals(rng1.getProvider().getClass())) {
			throw new SecurityException(
					"new SecureRandom() backed by wrong Provider: " + rng1.getProvider().getClass());
		}

		SecureRandom rng2;
		try {
			rng2 = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			throw new SecurityException("SHA1PRNG not available", e);
		}
		if (!LinuxPRNGSecureRandomProvider.class.equals(rng2.getProvider().getClass())) {
			throw new SecurityException(
					"SecureRandom.getInstance(\"SHA1PRNG\") backed by wrong Provider: " + rng2.getProvider()
							.getClass());
		}
	}

	/**
	 * {@code Provider} of {@code SecureRandom} engines which pass through
	 * all requests to the Linux PRNG.
	 */
	private static class LinuxPRNGSecureRandomProvider extends Provider {

		private static final long serialVersionUID = 1L;

		public LinuxPRNGSecureRandomProvider() {
			super("LinuxPRNG", 1.0, "A Linux-specific random number provider that uses /dev/urandom");
			// Although /dev/urandom is not a SHA-1 PRNG, some apps
			// explicitly request a SHA1PRNG SecureRandom and we thus need to
			// prevent them from getting the default implementation whose output
			// may have low entropy.
			put("SecureRandom.SHA1PRNG", LinuxPRNGSecureRandom.class.getName());
			put("SecureRandom.SHA1PRNG ImplementedIn", "Software");
		}
	}

	/**
	 * {@link SecureRandomSpi} which passes all requests to the Linux PRNG
	 * ({@code /dev/urandom}).
	 */
	public static class LinuxPRNGSecureRandom extends SecureRandomSpi {

		/*
		 * IMPLEMENTATION NOTE: Requests to generate bytes and to mix in a seed
		 * are passed through to the Linux PRNG (/dev/urandom). Instances of
		 * this class seed themselves by mixing in the current time, PID, UID,
		 * build fingerprint, and hardware serial number (where available) into
		 * Linux PRNG.
		 *
		 * Concurrency: Read requests to the underlying Linux PRNG are
		 * serialized (on sLock) to ensure that multiple threads do not get
		 * duplicated PRNG output.
		 */
		private static final long serialVersionUID = 1L;

		private static final File URANDOM_FILE = new File("/dev/urandom");

		private static final Object sLock = new Object();

		/**
		 * Input stream for reading from Linux PRNG or {@code null} if not yet
		 * opened.
		 */
		private static DataInputStream sUrandomIn;

		/**
		 * Output stream for writing to Linux PRNG or {@code null} if not yet
		 * opened.
		 */
		private static OutputStream sUrandomOut;

		/**
		 * Whether this engine instance has been seeded. This is needed because
		 * each instance needs to seed itself if the client does not explicitly
		 * seed it.
		 */
		private boolean mSeeded;

		@Override
		protected void engineSetSeed(byte[] bytes) {
			try {
				OutputStream out;
				synchronized (sLock) {
					out = getUrandomOutputStream();
				}
				out.write(bytes);
				out.flush();
			} catch (IOException e) {
				Log.w(TAG, "Failed to mix seed into " + URANDOM_FILE);
			} finally {
				mSeeded = true;
			}
		}

		@Override
		protected void engineNextBytes(byte[] bytes) {
			if (!mSeeded) {
				// Mix in the device- and invocation-specific seed.
				engineSetSeed(generateSeed());
			}

			try {
				DataInputStream in;
				synchronized (sLock) {
					in = getUrandomInputStream();
					in.readFully(bytes);
				}
			} catch (IOException e) {
				throw new SecurityException("Failed to read from " + URANDOM_FILE, e);
			}
		}

		@Override
		protected byte[] engineGenerateSeed(int size) {
			byte[] seed = new byte[size];
			engineNextBytes(seed);
			return seed;
		}

		private DataInputStream getUrandomInputStream() {
			synchronized (sLock) {
				if (sUrandomIn == null) {
					// NOTE: Consider inserting a BufferedInputStream between
					// DataInputStream and FileInputStream if you need higher
					// PRNG output performance and can live with future PRNG
					// output being pulled into this process prematurely.
					try {
						sUrandomIn = new DataInputStream(new FileInputStream(URANDOM_FILE));
					} catch (IOException e) {
						throw new SecurityException("Failed to open " + URANDOM_FILE + " for reading", e);
					}
				}
				return sUrandomIn;
			}
		}

		private OutputStream getUrandomOutputStream() throws IOException {
			synchronized (sLock) {
				if (sUrandomOut == null) {
					sUrandomOut = new FileOutputStream(URANDOM_FILE);
				}
				return sUrandomOut;
			}
		}
	}

	/**
	 * Generates a device- and invocation-specific seed to be mixed into the
	 * Linux PRNG.
	 */
	private static byte[] generateSeed() {
		try {
			ByteArrayOutputStream seedBuffer = new ByteArrayOutputStream();
			DataOutputStream seedBufferOut = new DataOutputStream(seedBuffer);
			seedBufferOut.writeLong(System.currentTimeMillis());
			seedBufferOut.writeLong(System.nanoTime());
			seedBufferOut.writeInt(Process.myPid());
			seedBufferOut.writeInt(Process.myUid());
			seedBufferOut.write(BUILD_FINGERPRINT_AND_DEVICE_SERIAL);
			seedBufferOut.close();
			return seedBuffer.toByteArray();
		} catch (IOException e) {
			throw new SecurityException("Failed to generate seed", e);
		}
	}

	private static byte[] getBuildFingerprintAndDeviceSerial() {
		StringBuilder result = new StringBuilder();
		String fingerprint = Build.FINGERPRINT;
		if (fingerprint != null) {
			result.append(fingerprint);
		}
		String serial = Build.SERIAL;
		if (serial != null) {
			result.append(serial);
		}
		try {
			return result.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UTF-8 encoding not supported");
		}
	}
}
