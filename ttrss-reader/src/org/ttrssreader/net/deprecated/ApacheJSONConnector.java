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

package org.ttrssreader.net.deprecated;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.net.JSONConnector;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.util.Log;

public class ApacheJSONConnector extends JSONConnector {
    
    protected static final String TAG = ApacheJSONConnector.class.getSimpleName();
    
    protected CredentialsProvider credProvider = null;
    protected DefaultHttpClient client;
    
    public ApacheJSONConnector(Context context) {
        super(context);
    }
    
    protected InputStream doRequest(Map<String, String> params) {
        HttpPost post = new HttpPost();
        
        try {
            if (sessionId != null)
                params.put(SID, sessionId);
            
            // check if http-Auth-Settings have changed, reload values if necessary
            refreshHTTPAuth();
            
            // Set Address
            post.setURI(Controller.getInstance().uri());
            post.addHeader("Accept-Encoding", "gzip");
            
            // Add POST data
            JSONObject json = new JSONObject(params);
            StringEntity jsonData = new StringEntity(json.toString(), "UTF-8");
            jsonData.setContentType("application/json");
            post.setEntity(jsonData);
            
            // Add timeouts for the connection
            {
                HttpParams httpParams = post.getParams();
                
                // Set the timeout until a connection is established.
                int timeoutConnection = (int) (8 * Utils.SECOND);
                HttpConnectionParams.setConnectionTimeout(httpParams, timeoutConnection);
                
                // Set the default socket timeout (SO_TIMEOUT) which is the timeout for waiting for data.
                // use longer timeout when lazyServer-Feature is used
                int timeoutSocket = (int) ((Controller.getInstance().lazyServer()) ? 15 * Utils.MINUTE
                        : 10 * Utils.SECOND);
                HttpConnectionParams.setSoTimeout(httpParams, timeoutSocket);
                
                post.setParams(httpParams);
            }
            
            logRequest(json);
            
            if (client == null)
                client = HttpClientFactory.getInstance().getHttpClient(post.getParams());
            else
                client.setParams(post.getParams());
            
            // Add SSL-Stuff
            if (credProvider != null)
                client.setCredentialsProvider(credProvider);
            
        } catch (URISyntaxException e) {
            hasLastError = true;
            lastError = "Invalid URI.";
            return null;
        } catch (Exception e) {
            hasLastError = true;
            lastError = "Error creating HTTP-Connection in (old) doRequest(): " + formatException(e);
            e.printStackTrace();
            return null;
        }
        
        HttpResponse response = null;
        try {
            response = client.execute(post); // Execute the request
        } catch (ClientProtocolException e) {
            hasLastError = true;
            lastError = "ClientProtocolException in (old) doRequest(): " + formatException(e);
            return null;
        } catch (SSLPeerUnverifiedException e) {
            // Probably related: http://stackoverflow.com/questions/6035171/no-peer-cert-not-sure-which-route-to-take
            // Not doing anything here since this error should happen only when no certificate is received from the
            // server.
            Log.w(TAG, "SSLPeerUnverifiedException in (old) doRequest(): " + formatException(e));
            return null;
        } catch (SSLException e) {
            if ("No peer certificate".equals(e.getMessage())) {
                // Handle this by ignoring it, this occurrs very often when the connection is instable.
                Log.w(TAG, "SSLException in (old) doRequest(): " + formatException(e));
            } else {
                hasLastError = true;
                lastError = "SSLException in (old) doRequest(): " + formatException(e);
            }
            return null;
        } catch (InterruptedIOException e) {
            Log.w(TAG, "InterruptedIOException in (old) doRequest(): " + formatException(e));
            return null;
        } catch (SocketException e) {
            // http://stackoverflow.com/questions/693997/how-to-set-httpresponse-timeout-for-android-in-java/1565243#1565243
            Log.w(TAG, "SocketException in (old) doRequest(): " + formatException(e));
            return null;
        } catch (Exception e) {
            hasLastError = true;
            lastError = "Exception in (old) doRequest(): " + formatException(e);
            return null;
        }
        
        // Try to check for HTTP Status codes
        int code = response.getStatusLine().getStatusCode();
        if (code >= 400 && code < 600) {
            hasLastError = true;
            lastError = "Server returned status: " + code;
            return null;
        }
        
        InputStream instream = null;
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null)
                instream = entity.getContent();
            
            // Try to decode gzipped instream, if it is not gzip we stay to normal reading
            Header contentEncoding = response.getFirstHeader("Content-Encoding");
            if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip"))
                instream = new GZIPInputStream(instream);
            
            // Header size = response.getFirstHeader("Api-Content-Length");
            // Log.d(TAG, "SIZE: " + size.getValue());
            
            if (instream == null) {
                hasLastError = true;
                lastError = "Couldn't get InputStream in (old) Method doRequest(String url) [instream was null]";
                return null;
            }
        } catch (Exception e) {
            if (instream != null)
                try {
                    instream.close();
                } catch (IOException e1) {
                }
            hasLastError = true;
            lastError = "Exception in (old) doRequest(): " + formatException(e);
            return null;
        }
        
        return instream;
    }
    
    protected void refreshHTTPAuth() {
        super.refreshHTTPAuth();
        if (!httpAuth) {
            credProvider = null;
            return;
        }
        
        // Refresh Credentials-Provider
        if (httpUsername.equals(Constants.EMPTY) || httpPassword.equals(Constants.EMPTY)) {
            credProvider = null;
        } else {
            credProvider = new BasicCredentialsProvider();
            credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(httpUsername, httpPassword));
        }
    }
}
