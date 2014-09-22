/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

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
