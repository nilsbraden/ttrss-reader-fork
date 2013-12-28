package org.ttrssreader.utils;

import java.io.FileOutputStream;
import java.util.Locale;
import org.ttrssreader.controllers.Controller;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.sqlite.SQLiteException;
import android.os.Build;

/**
 * Saves Exceptions with Stack-Trace to a file in the application-directory so it can be sent to the developer by mail
 * on the next run of the application.
 * 
 * @author Jayesh from http://jyro.blogspot.com/2009/09/crash-report-for-android-app.html
 * 
 */
public class TopExceptionHandler implements Thread.UncaughtExceptionHandler {
    
    public static final String FILE = "stack.trace";
    private Thread.UncaughtExceptionHandler handler;
    private Activity app = null;
    
    public TopExceptionHandler(Activity app) {
        this.handler = Thread.getDefaultUncaughtExceptionHandler();
        this.app = app;
    }
    
    public void uncaughtException(Thread t, Throwable e) {
        if (e instanceof IllegalStateException) {
            return;
        }
        if (e instanceof SecurityException) {
            // Cannot be reproduced, seems to be related to Cyanogenmod with Android 4.0.4 on some devices:
            // http://stackoverflow.com/questions/11025182/webview-java-lang-securityexception-no-permission-to-modify-given-thread
            if (e.getMessage().toLowerCase(Locale.ENGLISH).contains("no permission to modify given thread"))
                return;
        }
        if (e instanceof SQLiteException) {
            // SQLiteDatabase.isDbLockedByOtherThreads() was deprecated with API Level 16 so I won't spend any more time
            // fixing these bugs for old devices. The javadoc states that there shouldn't be any explicit locking
            // anymore.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN
                    && e.getMessage().toLowerCase(Locale.ENGLISH).contains("database is locked"))
                return;
        }
        
        try {
            
            PackageInfo pi = null;
            try {
                pi = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            } catch (Exception ex) {
            }
            
            // @formatter:off
            StringBuilder sb = new StringBuilder();
            sb.append("--------- Application --------\n");
            sb.append("Version:      " + Controller.getInstance().getLastVersionRun() + "\n");
            sb.append("Version-Code: " + (pi != null ? pi.versionCode : "null")       + "\n");
            sb.append("------------------------------\n\n");
            sb.append("--------- Device -------------\n");
            sb.append("Brand:        " + Build.BRAND   + "\n");
            sb.append("Device:       " + Build.DEVICE  + "\n");
            sb.append("Model:        " + Build.MODEL   + "\n");
            sb.append("Id:           " + Build.ID      + "\n");
            sb.append("Product:      " + Build.PRODUCT + "\n");
            sb.append("------------------------------\n\n");
            sb.append("--------- Firmware -----------\n");
            sb.append("SDK:          " + Build.VERSION.SDK_INT     + "\n");
            sb.append("Release:      " + Build.VERSION.RELEASE     + "\n");
            sb.append("Incremental:  " + Build.VERSION.INCREMENTAL + "\n");
            sb.append("------------------------------\n\n");
            sb.append("--------- Stacktrace ---------\n");
            sb.append(e.toString() + "\n");
            StackTraceElement[] element = e.getStackTrace();
            for (int i = 0; i < element.length; i++) {
                sb.append("  " + element[i].toString() + "\n");
            }
            sb.append("------------------------------\n\n");
            // @formatter:on
            
            // If the exception was thrown in a background thread inside
            // AsyncTask, then the actual exception can be found with getCause
            Throwable cause = e.getCause();
            if (cause != null) {
                sb.append("--------- Cause --------------\n");
                sb.append(cause.toString() + "\n");
                element = cause.getStackTrace();
                for (int i = 0; i < element.length; i++) {
                    sb.append("  " + element[i].toString() + "\n");
                }
                sb.append("------------------------------\n\n");
            }
            
            try {
                FileOutputStream trace = app.openFileOutput(FILE, Context.MODE_PRIVATE);
                trace.write(sb.toString().getBytes());
                trace.close();
            } catch (Exception ioe) {
            }
            
            handler.uncaughtException(t, e);
        } catch (Throwable tt) {
        }
    }
    
}
