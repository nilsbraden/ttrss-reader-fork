/*
 * ttrss-reader-fork for Android
 * 
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

package org.ttrssreader.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.utils.Utils;
import android.os.Environment;
import android.util.Log;

/**
 * Create a HttpClient object based on the user preferences.
 */
public class HttpClientFactory {
    
    private static HttpClientFactory instance;
    private SchemeRegistry registry;
    
    public HttpClientFactory() {
        
        boolean trustAllSslCerts = Controller.getInstance().trustAllSsl();
        boolean useCustomKeyStore = Controller.getInstance().useKeystore();
        
        registry = new SchemeRegistry();
        registry.register(new Scheme("http", new PlainSocketFactory(), 80));
        
        SocketFactory socketFactory = null;
        
        if (useCustomKeyStore) {
            String keystorePassword = Controller.getInstance().getKeystorePassword();
            
            socketFactory = newSslSocketFactory(keystorePassword);
            if (socketFactory == null) {
                socketFactory = SSLSocketFactory.getSocketFactory();
                Log.w(Utils.TAG, "HttpClientFactory() - custom key store could not be opened, using default settings");
            }
            
        } else if (trustAllSslCerts) {
            socketFactory = new FakeSocketFactory();
        } else {
            socketFactory = SSLSocketFactory.getSocketFactory();
        }
        
        registry.register(new Scheme("https", socketFactory, 443));
        
    }
    
    public DefaultHttpClient getHttpClient(HttpParams httpParams) {
        DefaultHttpClient httpInstance = new DefaultHttpClient(new ThreadSafeClientConnManager(httpParams, registry),
                httpParams);
        return httpInstance;
    }
    
    public static HttpClientFactory getInstance() {
        if (instance == null) {
            instance = new HttpClientFactory();
        }
        return instance;
    }
    
    /**
     * Create a socket factory with the custom key store
     * 
     * @param keystorePassword
     *            the password to unlock the custom keystore
     * 
     * @return socket factory with custom key store
     */
    private static SSLSocketFactory newSslSocketFactory(String keystorePassword) {
        try {
            KeyStore trusted = KeyStore.getInstance("BKS");
            
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + Utils.SDCARD_PATH_FILES
                    + "store.bks");
            
            if (!file.exists())
                return null;
            
            InputStream in = new FileInputStream(file);
            
            try {
                trusted.load(in, keystorePassword.toCharArray());
            } finally {
                in.close();
            }
            
            return new SSLSocketFactory(trusted);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
}
