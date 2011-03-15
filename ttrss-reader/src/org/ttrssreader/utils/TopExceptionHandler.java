package org.ttrssreader.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import android.app.Activity;
import android.content.Context;

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
        
        StackTraceElement[] element = e.getStackTrace();
        StringBuilder sb = new StringBuilder();
        
        sb.append(e.toString() + "\n\n");
        sb.append("--------- Stacktrace ---------\n\n");
        
        for (int i = 0; i < element.length; i++) {
            sb.append("  " + element[i].toString() + "\n");
        }
        
        sb.append("------------------------------\n\n");
        
        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        
        Throwable cause = e.getCause();
        if (cause != null) {
            sb.append("--------- Cause --------------\n\n");
            
            sb.append(cause.toString() + "\n\n");
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
        } catch (IOException ioe) {
            // ...
        }
        
        handler.uncaughtException(t, e);
    }
    
}
