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

import org.ttrssreader.controllers.Controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
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

/**
 * Exception report delivery via email and user interaction. Avoids giving an app the
 * permission to access the Internet.
 *
 * @author Ryan Fischbach <br>
 *         Blackmoon Info Tech Services<br>
 *
 *         Source has been released to the public as is and without any warranty.
 */
@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class PostMortemReportExceptionHandler implements UncaughtExceptionHandler, Runnable {

    private static final String TAG = PostMortemReportExceptionHandler.class.getSimpleName();
    private static final String ExceptionReportFilename = "postmortem.trace";

    // "app label + this tag" = email subject
    private static final String MSG_SUBJECT_TAG = "Exception Report";

    // email will be sent to this account the following may be something you wish to consider localizing
    private static final String MSG_SENDTO = "ttrss@nilsbraden.de";

    private static final String MSG_BODY = "Please help by sending this email. "
            + "No personal information is being sent (you can check by reading the rest of the email).";

    private Thread.UncaughtExceptionHandler mDefaultUEH;
    private Activity mAct = null;

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

        // Ignore crashreport if user has chosen to ignore it
        if (Controller.getInstance().isNoCrashreports()) {
            Log.w(TAG, "User has disabled error reporting.");
            return;
        }

        // Ignore crashreport if this version isn't the newest from market
        int latest = Controller.getInstance().appLatestVersion();
        int current = Utils.getAppVersionCode(mAct);
        if (latest > current) {
            Log.w(TAG, "App is not updated, error reports are disabled.");
            return;
        }

        if ((mAct.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            Log.w(TAG, "Application runs with DEBUGGABLE=true, error reports are disabled.");
            return;
        }

        sendDebugReportToAuthor(); // in case a previous error did not get sent to the email app
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * Call this method at the end of the protected code, usually in {@link #finalize()}.
     */
    public void restoreOriginalHandler() {
        if (Thread.getDefaultUncaughtExceptionHandler().equals(this))
            Thread.setDefaultUncaughtExceptionHandler(mDefaultUEH);
    }

    @Override
    protected void finalize() throws Throwable {
        restoreOriginalHandler();
        super.finalize();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        boolean handleException = true;

        if (e instanceof SecurityException) {
            // Cannot be reproduced, seems to be related to Cyanogenmod with Android 4.0.4 on some devices:
            // http://stackoverflow.com/questions/11025182/webview-java-lang-securityexception-no-permission-to-modify-given-thread
            if (e.getMessage().toLowerCase(Locale.ENGLISH).contains("no permission to modify given thread")) {
                Log.w(TAG, "Error-Reporting for Exception \"no permission to modify given thread\" is disabled.");
                handleException = false;
            }
        }
        if (e instanceof SQLiteException) {
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
    public String getDeviceEnvironment() {
        // app environment
        PackageManager pm = mAct.getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(mAct.getPackageName(), 0);
        } catch (NameNotFoundException nnfe) {
            // doubt this will ever run since we want info about our own package
            pi = new PackageInfo();
            pi.versionName = "unknown";
            pi.versionCode = 69;
        }
        Date theDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy_HH.mm.ss_zzz", Locale.ENGLISH);
        StringBuilder s = new StringBuilder();

        s.append("--------- Application ---------------------\n");
        s.append("Version     = " + Controller.getInstance().getLastVersionRun() + "\n");
        s.append("VersionCode = " + (pi != null ? pi.versionCode : "null") + "\n");
        s.append("-------------------------------------------\n\n");

        s.append("--------- Environment ---------------------\n");
        s.append("Time        = " + sdf.format(theDate) + "\n");
        try {
            Field theMfrField = Build.class.getField("MANUFACTURER");
            s.append("Make        = " + theMfrField.get(null) + "\n");
        } catch (Exception e) {
            // Empty!
        }
        s.append("Brand       = " + Build.BRAND + "\n");
        s.append("Device      = " + Build.DEVICE + "\n");
        s.append("Model       = " + Build.MODEL + "\n");
        s.append("Id          = " + Build.ID + "\n");
        s.append("Fingerprint = " + Build.FINGERPRINT + "\n");
        s.append("Product     = " + Build.PRODUCT + "\n");
        s.append("Locale      = " + mAct.getResources().getConfiguration().locale.getDisplayName() + "\n");
        s.append("Res         = " + mAct.getResources().getDisplayMetrics().toString() + "\n");
        s.append("-------------------------------------------\n\n");

        s.append("--------- Firmware -----------------------\n");
        s.append("SDK         = " + Build.VERSION.SDK_INT + "\n");
        s.append("Release     = " + Build.VERSION.RELEASE + "\n");
        s.append("Inc         = " + Build.VERSION.INCREMENTAL + "\n");
        s.append("-------------------------------------------\n\n");

        return s.toString();
    }

    /**
     * Return the application's friendly name.
     *
     * @return Returns the application name as defined by the android:name attribute.
     */
    public CharSequence getAppName() {
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
    private String getDebugReport(Throwable aException) {
        StringBuilder theErrReport = new StringBuilder();

        theErrReport.append(getDeviceEnvironment());
        theErrReport.append(getAppName() + " generated the following exception:\n");
        theErrReport.append(aException.toString() + "\n\n");

        // activity stack trace
        List<CharSequence> theActivityTrace = getActivityTrace(null);
        if (theActivityTrace != null && theActivityTrace.size() > 0) {
            theErrReport.append("--------- Activity Stacktrace -------------\n");
            for (int i = 0; i < theActivityTrace.size(); i++) {
                theErrReport.append("    " + theActivityTrace.get(i) + "\n");
            }// for
            theErrReport.append("-------------------------------------------\n\n");
        }

        // instruction stack trace
        StackTraceElement[] theStackTrace = aException.getStackTrace();
        if (theStackTrace.length > 0) {
            theErrReport.append("--------- Instruction Stacktrace ----------\n");
            for (StackTraceElement se : theStackTrace) {
                theErrReport.append("    " + se.toString() + "\n");
            }
            theErrReport.append("-------------------------------------------\n\n");
        }

        // if the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        Throwable theCause = aException.getCause();
        if (theCause != null) {
            theErrReport.append("--------- Cause ---------------------------\n");
            theErrReport.append(theCause.toString() + "\n\n");
            theStackTrace = theCause.getStackTrace();
            for (StackTraceElement se : theStackTrace) {
                theErrReport.append("    " + se.toString() + "\n");
            }// for
            theErrReport.append("-------------------------------------------\n\n");
        }

        theErrReport.append("END REPORT.");
        return theErrReport.toString();
    }

    /**
     * Write the given debug report to the file system.
     *
     * @param aReport - the debug report
     */
    private void saveDebugReport(String aReport) {
        // save report to file
        try {
            FileOutputStream theFile = mAct.openFileOutput(ExceptionReportFilename, Context.MODE_PRIVATE);
            theFile.write(aReport.getBytes());
            theFile.close();
        } catch (IOException ioe) {
            // error during error report needs to be ignored, do not wish to start infinite loop
        }
    }

    /**
     * Read in saved debug report and send to email app.
     */
    private void sendDebugReportToAuthor() {
        String theLine;
        String theTrace = "";
        try {
            BufferedReader theReader = new BufferedReader(new InputStreamReader(
                    mAct.openFileInput(ExceptionReportFilename)));
            while ((theLine = theReader.readLine()) != null) {
                theTrace += theLine + "\n";
            }
            if (sendDebugReportToAuthor(theTrace)) {
                mAct.deleteFile(ExceptionReportFilename);
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
            Boolean hasSendRecipients = (mAct.getPackageManager().queryIntentActivities(theIntent, 0).size() > 0);
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
        String theErrReport = getDebugReport(e);
        saveDebugReport(theErrReport);
        // try to send file contents via email (need to do so via the UI thread)
        mAct.runOnUiThread(this);
    }
}
