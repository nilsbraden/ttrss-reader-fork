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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

public class FakeTrustManager implements X509TrustManager {
    
    private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[] {};
    
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }
    
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }
    
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return _AcceptedIssuers;
    }
    
}
