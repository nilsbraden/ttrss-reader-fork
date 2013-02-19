package org.ttrssreader.utils;

import java.io.FileOutputStream;
import org.ttrssreader.controllers.Controller;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
        if (e instanceof IllegalStateException)
            return;
        
        StackTraceElement[] element = e.getStackTrace();
        StringBuilder sb = new StringBuilder();
        
        sb.append(e.toString() + "\n\n");
        sb.append("--------- Stacktrace ---------\n");
        
        for (int i = 0; i < element.length; i++) {
            sb.append("  " + element[i].toString() + "\n");
        }
        
        sb.append("------------------------------\n\n");
        
        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        
        Throwable cause = e.getCause();
        if (cause != null) {
            sb.append("--------- Cause --------------\n");
            
            sb.append(cause.toString() + "\n\n");
            element = cause.getStackTrace();
            
            for (int i = 0; i < element.length; i++) {
                sb.append("  " + element[i].toString() + "\n");
            }
            
            sb.append("------------------------------\n\n");
        }
        
        sb.append("--------- Device -------------\n");
        sb.append("Brand: " + Build.BRAND + "\n");
        sb.append("Device: " + Build.DEVICE + "\n");
        sb.append("Model: " + Build.MODEL + "\n");
        sb.append("Id: " + Build.ID + "\n");
        sb.append("Product: " + Build.PRODUCT + "\n");
        sb.append("------------------------------\n\n");
        
        sb.append("--------- Firmware -----------\n");
        sb.append("SDK: " + Build.VERSION.SDK + "\n");
        sb.append("Release: " + Build.VERSION.RELEASE + "\n");
        sb.append("Incremental: " + Build.VERSION.INCREMENTAL + "\n");
        sb.append("------------------------------\n\n");
        
        PackageManager pm = app.getPackageManager();
        PackageInfo pi = null;
        try {
            pi = pm.getPackageInfo(app.getPackageName(), 0);
        } catch (Exception ex) {
        }
        
        sb.append("--------- Application --------\n");
        sb.append("Version: " + Controller.getInstance().getLastVersionRun() + "\n");
        sb.append("Version-Code: " + (pi != null ? pi.versionCode : "null") + "\n");
        sb.append("------------------------------\n\n");
        
        try {
            FileOutputStream trace = app.openFileOutput(FILE, Context.MODE_PRIVATE);
            trace.write(sb.toString().getBytes());
            trace.close();
        } catch (Exception ioe) {
        }
        
        handler.uncaughtException(t, e);
    }
    
}
