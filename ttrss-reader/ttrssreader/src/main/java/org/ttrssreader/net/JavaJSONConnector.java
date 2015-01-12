/*
 * Copyright (c) 2015, Nils Braden
 *
 * This file is part of ttrss-reader-fork. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; If
 * not, see http://www.gnu.org/licenses/.
 */

package org.ttrssreader.net;

import org.json.JSONObject;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.utils.Utils;

import android.util.Base64;
import android.util.Log;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.SocketException;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;

public class JavaJSONConnector extends JSONConnector {

    private static final String TAG = JavaJSONConnector.class.getSimpleName();

    protected String base64NameAndPw = null;

    protected InputStream doRequest(Map<String, String> params) {
        try {
            if (sessionId != null)
                params.put(SID, sessionId);

            JSONObject json = new JSONObject(params);
            byte[] outputBytes = json.toString().getBytes("UTF-8");

            logRequest(json);

            URL url = Controller.getInstance().url();
            HttpURLConnection con = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);

            // Content
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Content-Length", Integer.toString(outputBytes.length));

            // Timeouts
            int timeoutSocket = (int) ((Controller.getInstance().lazyServer()) ? 15 * Utils.MINUTE : 10 * Utils.SECOND);
            con.setReadTimeout(timeoutSocket);
            con.setConnectTimeout((int) (8 * Utils.SECOND));

            // HTTP-Basic Authentication
            if (base64NameAndPw != null)
                con.setRequestProperty("Authorization", "Basic " + base64NameAndPw);

            // Add POST data
            con.getOutputStream().write(outputBytes);

            // Try to check for HTTP Status codes
            int code = con.getResponseCode();
            if (code >= 400 && code < 600) {
                hasLastError = true;
                lastError = "Server returned status: " + code + " (Message: " + con.getResponseMessage() + ")";
                return null;
            }

            // Everything is fine!
            return con.getInputStream();

        } catch (SSLPeerUnverifiedException e) {
            // Probably related: http://stackoverflow.com/questions/6035171/no-peer-cert-not-sure-which-route-to-take
            // Not doing anything here since this error should happen only when no certificate is received from the
            // server.
            Log.w(TAG, "SSLPeerUnverifiedException in doRequest(): " + formatException(e));
        } catch (SSLException e) {
            if ("No peer certificate".equals(e.getMessage())) {
                // Handle this by ignoring it, this occurrs very often when the connection is instable.
                Log.w(TAG, "SSLException in doRequest(): " + formatException(e));
            } else {
                hasLastError = true;
                lastError = "SSLException in doRequest(): " + formatException(e);
            }
        } catch (InterruptedIOException e) {
            Log.w(TAG, "InterruptedIOException in doRequest(): " + formatException(e));
        } catch (SocketException e) {
            // http://stackoverflow.com/questions/693997/how-to-set-httpresponse-timeout-for-android-in-java/1565243#1565243
            Log.w(TAG, "SocketException in doRequest(): " + formatException(e));
        } catch (Exception e) {
            hasLastError = true;
            lastError = "Exception in doRequest(): " + formatException(e);
        }

        return null;
    }

    @Override
    public void init() {
        super.init();
        if (!httpAuth)
            return;

        try {
            base64NameAndPw = Base64.encodeToString((httpUsername + ":" + httpPassword).getBytes("UTF-8"),
                    Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            base64NameAndPw = null;
        }
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(httpUsername, httpPassword.toCharArray());
            }
        });
    }

}
