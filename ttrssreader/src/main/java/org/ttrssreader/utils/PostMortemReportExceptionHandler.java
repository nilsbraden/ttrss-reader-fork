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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.util.Log;

import org.ttrssreader.controllers.Controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;

/**
 * Exception report delivery via email and user interaction. Avoids giving an app the
 * permission to access the Internet.
 *
 * @author Ryan Fischbach <br>
 * Blackmoon Info Tech Services<br>
 * <p>
 * Source has been released to the public as is and without any warranty.
 */
@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class PostMortemReportExceptionHandler implements UncaughtExceptionHandler, Runnable {

	private static final String TAG = PostMortemReportExceptionHandler.class.getSimpleName();
	private static final String EXCEPTION_REPORT_FILENAME = "postmortem.trace";
	private static final String EXCEPTION_REPORT_EXCLUDE_PREFIX = "postmortem.exclude.";

	// "app label + this tag" = email subject
	private static final String MSG_SUBJECT_TAG = "Exception Report";

	// email will be sent to this account the following may be something you wish to consider localizing
	private static final String MSG_SENDTO = "ttrss@nilsbraden.de";

	private static final String MSG_BODY = "Please help by sending this email. " + "No personal information is being sent (you can check by reading the rest of the email).";

	private final Thread.UncaughtExceptionHandler mDefaultUEH;
	private final Activity mAct;

	public PostMortemReportExceptionHandler(Activity aAct) {
		mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
		mAct = aAct;
	}

	/**
	 * Call this method after creation to start protecting all code thereafter.
	 */
	public void initialize() {
		if (mAct == null)
			throw new NullPointerException();

		// Ignore reports if
		// - app is not signed with the key of Nils Braden
		// - app is not installed from play store
		// - app is running in an emulator
		// - app is running with debuggable=true
		if (!Controller.getInstance().isValidInstallation()) {
			Log.i(TAG, "Error reporting disabled, invalid installation.");
			return;
		}

		// Ignore crashreport if user has chosen to ignore it
		if (Controller.getInstance().isNoCrashreports()) {
			Log.i(TAG, "User has disabled error reporting.");
			return;
		}

		// Ignore crashreport if this version isn't the newest from market
		long latest = Controller.getInstance().appLatestVersion();
		long current = Utils.getAppVersionCode(mAct);
		if (latest > current) {
			Log.i(TAG, "App is not updated, error reports are disabled.");
			return;
		}

		sendDebugReportToAuthor(); // in case a previous error did not get sent to the email app
		Thread.setDefaultUncaughtExceptionHandler(this);
	}

	/**
	 * Call this method at the end of the protected code, usually in {@link #finalize()}.
	 */
	public void restoreOriginalHandler() {
		if (Objects.equals(Thread.getDefaultUncaughtExceptionHandler(), this))
			Thread.setDefaultUncaughtExceptionHandler(mDefaultUEH);
	}

	@Override
	protected void finalize() throws Throwable {
		restoreOriginalHandler();
		super.finalize();
	}

	@Override
	public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
		boolean handleException = true;

		if (e instanceof SecurityException && e.getMessage() != null) {
			// Cannot be reproduced, seems to be related to Cyanogenmod with Android 4.0.4 on some devices:
			// http://stackoverflow.com/questions/11025182/webview-java-lang-securityexception-no-permission-to-modify-given-thread
			if (e.getMessage().toLowerCase(Locale.ENGLISH).contains("no permission to modify given thread")) {
				Log.w(TAG, "Error-Reporting for Exception \"no permission to modify given thread\" is disabled.");
				handleException = false;
			}
		}
		if (e instanceof SQLiteException && e.getMessage() != null) {
			if (e.getMessage().toLowerCase(Locale.ENGLISH).contains("database is locked")) {
				Log.w(TAG, "Error-Reporting for Exception \"database is locked\" is disabled.");
				handleException = false;
			}
		}

		if (handleException)
			submit(e);

		// do not forget to pass this exception through up the chain
		bubbleUncaughtException(t, e);
	}

	/**
	 * Send the Exception up the chain, skipping other handlers of this type so only 1 report is sent.
	 *
	 * @param t - thread object
	 * @param e - exception being handled
	 */
	private void bubbleUncaughtException(Thread t, Throwable e) {
		if (mDefaultUEH != null) {
			if (mDefaultUEH instanceof PostMortemReportExceptionHandler)
				((PostMortemReportExceptionHandler) mDefaultUEH).bubbleUncaughtException(t, e);
			else
				mDefaultUEH.uncaughtException(t, e);
		}
	}

	/**
	 * Return a string containing the device environment.
	 *
	 * @return Returns a string with the device info used for debugging.
	 */
	private String getDeviceEnvironment() {
		// app environment
		PackageManager pm = mAct.getPackageManager();
		PackageInfo pi;
		try {
			pi = pm.getPackageInfo(mAct.getPackageName(), 0);
		} catch (NameNotFoundException nnfe) {
			// doubt this will ever run since we want info about our own package
			pi = new PackageInfo();
			pi.versionName = "unknown";
		}
		Date theDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy_HH.mm.ss_zzz", Locale.ENGLISH);
		StringBuilder s = new StringBuilder();

		s.append("--- Application ---------------------\n");
		s.append("Version     = " + Controller.getInstance().getLastVersionRun() + "\n");
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
			s.append("VersionCode = " + (pi != null ? pi.versionCode : "null") + "\n");
		else
			s.append("VersionCode = " + (pi != null ? pi.getLongVersionCode() : "null") + "\n");
		s.append("-------------------------------------\n\n");

		s.append("--- Environment ---------------------\n");
		s.append("Time        = " + sdf.format(theDate) + "\n");
		try {
			Field theMfrField = Build.class.getField("MANUFACTURER");
			s.append("Make    = " + theMfrField.get(null) + "\n");
		} catch (Exception e) {
			// Empty!
		}
		s.append("Brand       = " + Build.BRAND + "\n");
		s.append("Device      = " + Build.DEVICE + "\n");
		s.append("Model       = " + Build.MODEL + "\n");
		s.append("Id          = " + Build.ID + "\n");
		s.append("Fingerprint = " + Build.FINGERPRINT + "\n");
		s.append("Product     = " + Build.PRODUCT + "\n");

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
			s.append("Locale      = " + mAct.getResources().getConfiguration().locale.getDisplayName() + "\n");
		else
			s.append("Locale      = " + mAct.getResources().getConfiguration().getLocales().get(0).getDisplayName() + "\n");

		s.append("Res         = " + mAct.getResources().getDisplayMetrics().toString() + "\n");
		s.append("-------------------------------------\n\n");

		s.append("--- Firmware -----------------------\n");
		s.append("SDK         = " + Build.VERSION.SDK_INT + "\n");
		s.append("Release     = " + Build.VERSION.RELEASE + "\n");
		s.append("Inc         = " + Build.VERSION.INCREMENTAL + "\n");
		s.append("-------------------------------------\n\n");

		return s.toString();
	}

	/**
	 * Return the application's friendly name.
	 *
	 * @return Returns the application name as defined by the android:name attribute.
	 */
	private CharSequence getAppName() {
		PackageManager pm = mAct.getPackageManager();
		PackageInfo pi;
		try {
			pi = pm.getPackageInfo(mAct.getPackageName(), 0);
			return pi.applicationInfo.loadLabel(pm);
		} catch (NameNotFoundException nnfe) {
			// doubt this will ever run since we want info about our own package
			return mAct.getPackageName();
		}
	}

	/**
	 * If subactivities create their own report handler, report all Activities as a trace list.
	 * A separate line is included if a calling activity/package is detected with the Intent it supplied.
	 *
	 * @param aTrace - pass in null to force a new list to be created
	 * @return Returns the list of Activities in the handler chain.
	 */
	private LinkedList<CharSequence> getActivityTrace(LinkedList<CharSequence> aTrace) {
		if (aTrace == null)
			aTrace = new LinkedList<>();
		aTrace.add(mAct.getLocalClassName() + " (" + mAct.getTitle() + ")");
		if (mAct.getCallingActivity() != null)
			aTrace.add(mAct.getCallingActivity().toString() + " (" + mAct.getIntent().toString() + ")");
		else if (mAct.getCallingPackage() != null)
			aTrace.add(mAct.getCallingPackage() + " (" + mAct.getIntent().toString() + ")");
		if (mDefaultUEH != null && mDefaultUEH instanceof PostMortemReportExceptionHandler)
			((PostMortemReportExceptionHandler) mDefaultUEH).getActivityTrace(aTrace);
		return aTrace;
	}

	/**
	 * Create a report based on the given exception.
	 *
	 * @param aException - exception to report on
	 * @return Returns a string with a lot of debug information.
	 */
	private String[] getDebugReport(Throwable aException) {
		StringBuilder theErrReport = new StringBuilder();

		theErrReport.append(getDeviceEnvironment());
		theErrReport.append(getAppName() + " generated the following exception:\n");
		theErrReport.append(aException.toString() + "\n\n");
		String exceptionHash = String.valueOf(aException.toString().hashCode());

		// activity stack trace
		List<CharSequence> theActivityTrace = getActivityTrace(null);
		if (theActivityTrace.size() > 0) {
			theErrReport.append("--- Activity Stacktrace -------------\n");
			for (int i = 0; i < theActivityTrace.size(); i++) {
				theErrReport.append("  " + theActivityTrace.get(i) + "\n");
			}
			theErrReport.append("-------------------------------------\n\n");
		}

		// instruction stack trace
		StackTraceElement[] theStackTrace = aException.getStackTrace();
		if (theStackTrace.length > 0) {
			theErrReport.append("--- Instruction Stacktrace ----------\n");
			for (StackTraceElement se : theStackTrace) {
				theErrReport.append("  " + se.toString() + "\n");
			}
			theErrReport.append("-------------------------------------\n\n");
		}

		// if the exception was thrown in a background thread inside
		// AsyncTask, then the actual exception can be found with getCause
		Throwable theCause = aException.getCause();
		if (theCause != null) {
			theErrReport.append("--- Cause ---------------------------\n");
			theErrReport.append(theCause + "\n\n");
			theStackTrace = theCause.getStackTrace();
			for (StackTraceElement se : theStackTrace) {
				theErrReport.append("  " + se.toString() + "\n");
			}
			theErrReport.append("-------------------------------------\n\n");
		}

		theErrReport.append("END REPORT.");
		return new String[]{theErrReport.toString(), exceptionHash};
	}

	/**
	 * Write the given debug report to the file system.
	 *
	 * @param aReport - the debug report
	 */
	private void saveDebugReport(String[] aReport) {
		// save report to file
		try {
			FileOutputStream theFile = mAct.openFileOutput(EXCEPTION_REPORT_FILENAME, Context.MODE_PRIVATE);
			theFile.write(aReport[0].getBytes());
			theFile.close();
			setReportHasBeenSent(aReport[1]);
		} catch (IOException ioe) {
			// error during error report needs to be ignored, do not wish to start infinite loop
		}
	}

	/**
	 * Read in saved debug report and send to email app.
	 */
	private void sendDebugReportToAuthor() {
		String theLine;
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader theReader = new BufferedReader(new InputStreamReader(mAct.openFileInput(EXCEPTION_REPORT_FILENAME)));
			while ((theLine = theReader.readLine()) != null) {
				sb.append(theLine + "\n");
			}
			if (sendDebugReportToAuthor(sb.toString())) {
				mAct.deleteFile(EXCEPTION_REPORT_FILENAME);
			}
		} catch (IOException eIo) {
			// Empty!
		}
	}

	/**
	 * Send the given report to email app.
	 *
	 * @param aReport - the debug report to send
	 * @return Returns true if the email app was launched regardless if the email was sent.
	 */
	private Boolean sendDebugReportToAuthor(String aReport) {
		if (aReport != null) {
			Intent theIntent = new Intent(Intent.ACTION_SEND);
			String theSubject = getAppName() + " " + MSG_SUBJECT_TAG;
			String theBody = "\n" + MSG_BODY + "\n\n" + aReport + "\n\n";
			theIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{MSG_SENDTO});
			theIntent.putExtra(Intent.EXTRA_TEXT, theBody);
			theIntent.putExtra(Intent.EXTRA_SUBJECT, theSubject);
			theIntent.setType("message/rfc822");

			boolean hasSendRecipients = (mAct.getPackageManager().queryIntentActivities(theIntent, 0).size() > 0);
			if (hasSendRecipients) {
				mAct.startActivity(theIntent);
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	@Override
	public void run() {
		sendDebugReportToAuthor();
	}

	/**
	 * Create an exception report and start an email with the contents of the report.
	 *
	 * @param e - the exception
	 */
	private void submit(Throwable e) {
		String[] report = getDebugReport(e);
		if (!hasReportBeenSent(report[1])) {
			saveDebugReport(report);
			// try to send file contents via email (need to do so via the UI thread)
			mAct.runOnUiThread(this);
		}
	}

	private boolean hasReportBeenSent(String hash) {
		File file = new File(mAct.getFilesDir(), EXCEPTION_REPORT_EXCLUDE_PREFIX + hash);
		return file.exists();
	}

	private void setReportHasBeenSent(String hash) {
		try {
			// Store file with name postmortem.exclude.xyz to indicate that this report has been sent already
			FileOutputStream sentReport = mAct.openFileOutput(EXCEPTION_REPORT_EXCLUDE_PREFIX + hash, Context.MODE_PRIVATE);
			sentReport.write("1".getBytes());
			sentReport.close();
		} catch (IOException e) {
			// Empty
		}
	}

}
