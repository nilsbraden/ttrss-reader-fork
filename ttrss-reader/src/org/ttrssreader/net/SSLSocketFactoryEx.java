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

package org.ttrssreader.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * <h1>Which Cipher Suites to enable for SSL Socket?</h1>
 * <p>
 * Below is the Java class I use to enforce cipher suites and protocols. Prior to SSLSocketFactoryEx, I was modifying
 * properties on the SSLSocket when I had access to them. The Java folks on Stack Overflow helped with it, so its nice
 * to be able to post it here.
 * </p>
 * 
 * <p>
 * SSLSocketFactoryEx prefers stronger cipher suites (like ECDHE and DHE), and it omits weak and wounded cipher suites
 * (like RC4 and MD5). It does have to enable four RSA key transport ciphers for interop with Google and Microsoft when
 * TLS 1.2 is not available. They are TLS_RSA_WITH_AES_256_CBC_SHA256, TLS_RSA_WITH_AES_256_CBC_SHA and two friends.
 * </p>
 * 
 * <p>
 * Keep the cipher suite list as small as possible. If you advertise all available ciphers (similar to Flaschen's list),
 * then your list will be 80+. That takes up 160 bytes in the ClientHello, and it can cause some appliances to fail
 * because they have a small, fixed-size buffer for processing the ClientHello. Broken appliances include F5 and
 * Ironport.
 * </p>
 * 
 * <p>
 * In practice, the list in the code below is paired down to 10 or 15 cipher suites once the preferred list intersects
 * with Java's supported cipher suites. For example, here's the list I get when preparing to connect or microsoft.com or
 * google.com with an unlimited JCE policy in place:
 * </p>
 * 
 * <ul>
 * <li>TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384</li>
 * <li>TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384</li>
 * <li>TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256</li>
 * <li>TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256</li>
 * <li>TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384</li>
 * <li>TLS_DHE_DSS_WITH_AES_256_GCM_SHA384</li>
 * <li>TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256</li>
 * <li>TLS_DHE_DSS_WITH_AES_128_GCM_SHA256</li>
 * <li>TLS_DHE_DSS_WITH_AES_256_CBC_SHA256</li>
 * <li>TLS_DHE_RSA_WITH_AES_128_CBC_SHA</li>
 * <li>TLS_DHE_DSS_WITH_AES_128_CBC_SHA</li>
 * <li>TLS_RSA_WITH_AES_256_CBC_SHA256</li>
 * <li>TLS_RSA_WITH_AES_256_CBC_SHA</li>
 * <li>TLS_RSA_WITH_AES_128_CBC_SHA256</li>
 * <li>TLS_RSA_WITH_AES_128_CBC_SHA</li>
 * <li>TLS_EMPTY_RENEGOTIATION_INFO_SCSV</li>
 * </ul>
 * 
 * <p>
 * The list will be smaller with the default JCE policy because the policy removes AES-256 and some others. I think its
 * about 7 cipher suites with the restricted policy.
 * </p>
 * 
 * <p>
 * The SSLSocketFactoryEx class also ensures protocols TLS 1.0 and above are used. Java clients prior to Java 8 disable
 * TLS 1.1 and 1.2. SSLContext.getInstance("TLS") will also sneak in SSLv3 (even in Java 8), so steps have to be taken
 * to remove it.
 * </p>
 * 
 * <p>
 * Finally, the class below is TLS 1.3 aware, so it should work when Java provides it. The *_CHACHA20_POLY1305 cipher
 * suites are preferred if available because they are so much faster than some of the current suites and they have
 * better security properties. Google has already rolled it out on its servers. I'm not sure when Oracle will provide
 * them. OpenSSL will provide them with OpenSSL 1.0.2.
 * </p>
 * 
 * <p>
 * You can use it like so:
 * </p>
 * 
 * <pre>
 * {@code
 * URL url = new URL("https://www.google.com:443");
 * HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
 * 
 * SSLSocketFactoryEx factory = new SSLSocketFactoryEx();
 * connection.setSSLSocketFactory(factory);
 * connection.setRequestProperty("charset", "utf-8");
 * 
 * InputStream input = connection.getInputStream();
 * InputStreamReader reader = new InputStreamReader(input, "utf-8");
 * BufferedReader buffer = new BufferedReader(reader);
 * ...
 * }
 * </pre>
 * 
 * <p>
 * Source: <a href="http://stackoverflow.com/a/23365536">stackoverflow.com/a/23365536</a>
 * </p>
 * 
 * @author jww: http://stackoverflow.com/users/608639/jww
 * 
 */
public class SSLSocketFactoryEx extends SSLSocketFactory {
    
    public SSLSocketFactoryEx() throws NoSuchAlgorithmException, KeyManagementException {
        initSSLSocketFactoryEx(null, null, null);
    }
    
    public SSLSocketFactoryEx(KeyManager[] km, TrustManager[] tm, SecureRandom random) throws NoSuchAlgorithmException,
            KeyManagementException {
        initSSLSocketFactoryEx(km, tm, random);
    }
    
    @Override
    public String[] getDefaultCipherSuites() {
        return m_ciphers;
    }
    
    @Override
    public String[] getSupportedCipherSuites() {
        return m_ciphers;
    }
    
    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        SSLSocketFactory factory = m_ctx.getSocketFactory();
        SSLSocket ss = (SSLSocket) factory.createSocket(s, host, port, autoClose);
        
        ss.setEnabledProtocols(m_protocols);
        ss.setEnabledCipherSuites(m_ciphers);
        
        return ss;
    }
    
    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        SSLSocketFactory factory = m_ctx.getSocketFactory();
        SSLSocket ss = (SSLSocket) factory.createSocket(address, port, localAddress, localPort);
        
        ss.setEnabledProtocols(m_protocols);
        ss.setEnabledCipherSuites(m_ciphers);
        
        return ss;
    }
    
    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        SSLSocketFactory factory = m_ctx.getSocketFactory();
        SSLSocket ss = (SSLSocket) factory.createSocket(host, port, localHost, localPort);
        
        ss.setEnabledProtocols(m_protocols);
        ss.setEnabledCipherSuites(m_ciphers);
        
        return ss;
    }
    
    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        SSLSocketFactory factory = m_ctx.getSocketFactory();
        SSLSocket ss = (SSLSocket) factory.createSocket(host, port);
        
        ss.setEnabledProtocols(m_protocols);
        ss.setEnabledCipherSuites(m_ciphers);
        
        return ss;
    }
    
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        SSLSocketFactory factory = m_ctx.getSocketFactory();
        SSLSocket ss = (SSLSocket) factory.createSocket(host, port);
        
        ss.setEnabledProtocols(m_protocols);
        ss.setEnabledCipherSuites(m_ciphers);
        
        return ss;
    }
    
    private void initSSLSocketFactoryEx(KeyManager[] km, TrustManager[] tm, SecureRandom random) throws NoSuchAlgorithmException, KeyManagementException {
        m_ctx = SSLContext.getInstance("TLS");
        m_ctx.init(km, tm, random);
        
        m_protocols = getProtocolList();
        m_ciphers = getCipherList();
    }
    
    private String[] getProtocolList() {
        String[] preferredProtocols = { "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3" };
        String[] availableProtocols = null;
        
        SSLSocket socket = null;
        
        try {
            SSLSocketFactory factory = m_ctx.getSocketFactory();
            socket = (SSLSocket) factory.createSocket();
            
            availableProtocols = socket.getSupportedProtocols();
            Arrays.sort(availableProtocols);
        } catch (Exception e) {
            return new String[] { "TLSv1" };
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
        
        List<String> aa = new ArrayList<String>();
        for (int i = 0; i < preferredProtocols.length; i++) {
            int idx = Arrays.binarySearch(availableProtocols, preferredProtocols[i]);
            if (idx >= 0)
                aa.add(preferredProtocols[i]);
        }
        
        return aa.toArray(new String[0]);
    }
    
    private String[] getCipherList() {
        String[] preferredCiphers = {
                // @formatter:off
                // *_CHACHA20_POLY1305 are 3x to 4x faster than existing cipher suites.
                // http://googleonlinesecurity.blogspot.com/2014/04/speeding-up-and-strengthening-https.html
                // Use them if available. Normative names can be found at (TLS spec depends on IPSec spec):
                // http://tools.ietf.org/html/draft-nir-ipsecme-chacha20-poly1305-01
                // http://tools.ietf.org/html/draft-mavrogiannopoulos-chacha-tls-02
                "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305",
                "TLS_ECDHE_ECDSA_WITH_CHACHA20_SHA",
                "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305",
                "TLS_ECDHE_RSA_WITH_CHACHA20_SHA",
                
                "TLS_DHE_RSA_WITH_CHACHA20_POLY1305",
                "TLS_DHE_RSA_WITH_CHACHA20_SHA",
                "TLS_RSA_WITH_CHACHA20_POLY1305",
                "TLS_RSA_WITH_CHACHA20_SHA",
                
                // Done with bleeding edge, back to TLS v1.2 and below
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
                
                // TLS v1.0 (with some SSLv3 interop)
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
                "TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                "TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
                
                // Removed SSLv3 cipher suites (see
                // http://www.openssl.org/docs/apps/ciphers.html#SSL_v3_0_cipher_suites_ for lists of cipher suite
                // names):
                // "SSL_DH_RSA_WITH_3DES_EDE_CBC_SHA",
                // "SSL_DH_DSS_WITH_3DES_EDE_CBC_SHA",
                
                // RSA key transport sucks, but they are needed as a fallback.
                // For example, microsoft.com fails under all versions of TLS
                // if they are not included. If only TLS 1.0 is available at
                // the client, then google.com will fail too. TLS v1.3 is
                // trying to deprecate them, so it will be interesteng to see
                // what happens.
                "TLS_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_RSA_WITH_AES_256_CBC_SHA",
                "TLS_RSA_WITH_AES_128_CBC_SHA",
                
                // Cipher-Suites that were disabled but I couldn't find any reason why:
                "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                
                "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                
                "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
                "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
                
                "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
                "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA"
                // @formatter:on
        };
        
        String[] availableCiphers = null;
        
        try {
            SSLSocketFactory factory = m_ctx.getSocketFactory();
            availableCiphers = factory.getSupportedCipherSuites();
            Arrays.sort(availableCiphers);
        } catch (Exception e) {
            return new String[] {
                    // @formatter:off
                    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
                    "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
                    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                    "TLS_RSA_WITH_AES_256_CBC_SHA256",
                    "TLS_RSA_WITH_AES_256_CBC_SHA",
                    "TLS_RSA_WITH_AES_128_CBC_SHA256",
                    "TLS_RSA_WITH_AES_128_CBC_SHA",
                    "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"
                    // @formatter:on
            };
        }
        
        List<String> aa = new ArrayList<String>();
        for (int i = 0; i < preferredCiphers.length; i++) {
            int idx = Arrays.binarySearch(availableCiphers, preferredCiphers[i]);
            if (idx >= 0)
                aa.add(preferredCiphers[i]);
        }
        
        aa.add("TLS_EMPTY_RENEGOTIATION_INFO_SCSV");
        
        return aa.toArray(new String[0]);
    }
    
    private SSLContext m_ctx;
    
    private String[] m_ciphers;
    private String[] m_protocols;
    
}
