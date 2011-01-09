/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Felix Bechstein
 * Copyright (C) 2010 N. Braden.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.content.Context;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

public class DonationHelper {
    
    public static final String URL = "http://ttrss-donation-helper.appspot.com/";
    
    public static boolean checkDonationStatus(Context context, String mail) {
        String url = URL + "?mail=" + Uri.encode(mail) + "&hash=" + getImeiHash(context);
        
        final HttpGet request = new HttpGet(url);
        try {
            Log.d(Utils.TAG, "DonationHelper - url: " + url);
            final HttpResponse response = new DefaultHttpClient().execute(request);
            int resp = response.getStatusLine().getStatusCode();
            if (resp != HttpStatus.SC_OK) {
                return false;
            }
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity()
                    .getContent()), 512);
            final String line = bufferedReader.readLine();
            if (line.equals("OK")) {
                return true;
            }
        } catch (IOException e) {
            Log.e(Utils.TAG, "error loading sig", e);
        }
        
        return false;
    }
    
    /**
     * Get MD5 hash of the IMEI (device id).
     * 
     * @param context
     *            {@link Context}
     * @return MD5 hash of IMEI
     */
    public static String getImeiHash(final Context context) {
        String imeiHash = "";
        TelephonyManager mTelephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        final String did = mTelephonyMgr.getDeviceId();
        if (did != null) {
            imeiHash = DonationHelper.md5(did);
        }
        return imeiHash;
    }
    
    /**
     * Calculate MD5 Hash from String.
     * 
     * @param s
     *            input
     * @return hash
     */
    public static String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();
            // Create Hex String
            StringBuilder hexString = new StringBuilder(32);
            int b;
            for (int i = 0; i < messageDigest.length; i++) {
                b = 0xFF & messageDigest[i];
                if (b < 0x10) {
                    hexString.append('0' + Integer.toHexString(b));
                } else {
                    hexString.append(Integer.toHexString(b));
                }
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(Utils.TAG, null, e);
        }
        return "";
    }
}
