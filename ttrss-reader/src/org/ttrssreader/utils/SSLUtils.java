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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.ttrssreader.net.SSLSocketFactoryEx;
import android.annotation.SuppressLint;
import android.os.Environment;
import android.util.Log;

public class SSLUtils {
    
    private static final String TAG = SSLUtils.class.getSimpleName();
    
    @SuppressLint("TrulyRandom")
    public static SSLSocketFactory initSslSocketFactory(KeyManager[] km, TrustManager[] tm) throws KeyManagementException, NoSuchAlgorithmException {
        SSLSocketFactoryEx factory = new SSLSocketFactoryEx(km, tm);
        HttpsURLConnection.setDefaultSSLSocketFactory(factory);
        return factory;
    }
    
    public static void initPrivateKeystore(String password) throws GeneralSecurityException {
        Log.i(TAG, "Enabling SSLUtils to trust certificates from private keystore.");
        KeyStore keystore = SSLUtils.loadKeystore(password);
        if (keystore == null)
            return;
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keystore);
        
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keystore, password.toCharArray());
        
        initSslSocketFactory(kmf.getKeyManagers(), tmf.getTrustManagers());
    }
    
    public static KeyStore loadKeystore(String keystorePassword) throws GeneralSecurityException {
        KeyStore trusted = null;
        try {
            trusted = KeyStore.getInstance(KeyStore.getDefaultType());
            
            File file = new File(Environment.getExternalStorageDirectory() + File.separator
                    + FileUtils.SDCARD_PATH_FILES + "store.bks");
            
            if (!file.exists())
                return null;
            
            InputStream in = new FileInputStream(file);
            
            try {
                trusted.load(in, keystorePassword.toCharArray());
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        } catch (Exception e) {
            throw new GeneralSecurityException("Couldn't load keystore.", e);
        }
        return trusted;
    }
    
    public static void trustAllCert() throws KeyManagementException, NoSuchAlgorithmException {
        Log.i(TAG, "Enabling SSLUtils to trust all CERTIFICATES.");
        X509TrustManager easyTrustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }
            
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }
            
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[] {};
            }
            
        };
        
        // Create a trust manager that does not validate certificate chains
        initSslSocketFactory(null, new TrustManager[] { easyTrustManager });
    }
    
    public static void trustAllHost() {
        Log.i(TAG, "Enabling SSLUtils to trust all HOSTS.");
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (Exception e) {
            // Empty, HostnameVerifier cannot be null.
        }
    }
    
}
