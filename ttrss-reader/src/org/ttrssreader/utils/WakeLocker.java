package org.ttrssreader.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.PowerManager;

@SuppressLint("Wakelock")
public abstract class WakeLocker {
    
    private static final String TAG = WakeLocker.class.getSimpleName();
    private static PowerManager.WakeLock wakeLock;
    
    public static void acquire(Context ctx) {
        if (wakeLock != null)
            wakeLock.release();
        
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();
    }
    
    public static void release() {
        if (wakeLock != null)
            wakeLock.release();
        wakeLock = null;
    }
    
}
