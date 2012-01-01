/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
 * Copyright (C) 2009-2010 J. Devauchelle.
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.SSLException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.Base64;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class JSONConnector implements Connector {
    // other units in milliseconds
    static final int SECOND = 1000;
    static final int MINUTE = 60 * SECOND;
    
    private static String lastError = "";
    private static boolean hasLastError = false;
    
    private static final String PARAM_OP = "op";
    private static final String PARAM_USER = "user";
    private static final String PARAM_PW = "password";
    private static final String PARAM_CAT_ID = "cat_id";
    private static final String PARAM_FEED_ID = "feed_id";
    private static final String PARAM_ARTICLE_IDS = "article_ids";
    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_VIEWMODE = "view_mode";
    private static final String PARAM_SHOW_CONTENT = "show_content";
    private static final String PARAM_INC_ATTACHMENTS = "include_attachments"; // include_attachments available since
                                                                               // 1.5.3 but is ignored on older versions
    private static final String PARAM_MODE = "mode";
    private static final String PARAM_FIELD = "field";
    private static final String PARAM_IS_CAT = "is_cat";
    private static final String PARAM_PREF = "pref_name";
    private static final String PARAM_OUTPUT_MODE = "output_mode"; // output_mode (default: flc) - what kind of
                                                                   // information to return (f-feeds, l-labels,
                                                                   // c-categories, t-tags)
    
    private static final String VALUE_LOGIN = "login";
    private static final String VALUE_GET_CATEGORIES = "getCategories";
    private static final String VALUE_GET_FEEDS = "getFeeds";
    private static final String VALUE_GET_HEADLINES = "getHeadlines";
    private static final String VALUE_UPDATE_ARTICLE = "updateArticle";
    private static final String VALUE_CATCHUP = "catchupFeed";
    private static final String VALUE_UPDATE_FEED = "updateFeed";
    private static final String VALUE_GET_PREF = "getPref";
    private static final String VALUE_GET_VERSION = "getVersion";
    private static final String VALUE_GET_COUNTERS = "getCounters";
    private static final String VALUE_OUTPUT_MODE = "flc"; // f - feeds, l - labels, c - categories, t - tags
    
    private static final String ERROR = "error";
    private static final String ERROR_TEXT = "Error: ";
    private static final String NOT_LOGGED_IN = "NOT_LOGGED_IN";
    private static final String NOT_LOGGED_IN_MESSAGE = "Couldn't login to your account, please check your credentials.";
    private static final String API_DISABLED = "API_DISABLED";
    private static final String API_DISABLED_MESSAGE = "Please enable API for the user \"%s\" in the preferences of this user on the Server.";
    private static final String STATUS = "status";
    
    private static final String SESSION_ID = "session_id"; // session id as an out parameter
    private static final String SID = "sid"; // session id as an in parameter
    private static final String ID = "id";
    private static final String TITLE = "title";
    private static final String UNREAD = "unread";
    private static final String CAT_ID = "cat_id";
    private static final String FEED_ID = "feed_id";
    private static final String UPDATED = "updated";
    private static final String CONTENT = "content";
    private static final String URL = "link";
    private static final String FEED_URL = "feed_url";
    private static final String COMMENT_URL = "comments";
    private static final String ATTACHMENTS = "attachments";
    private static final String CONTENT_URL = "content_url";
    private static final String STARRED = "marked";
    private static final String PUBLISHED = "published";
    private static final String VALUE = "value";
    private static final String VERSION = "version";
    
    private static final String COUNTER_KIND = "kind";
    private static final String COUNTER_CAT = "cat";
    private static final String COUNTER_ID = "id";
    private static final String COUNTER_COUNTER = "counter";
    
    private static final int MAX_ID_LIST_LENGTH = 100;
    
    private String httpUsername;
    private String httpPassword;
    
    private String sessionId;
    private String loginLock = "";
    
    private CredentialsProvider credProvider = null;
    private DefaultHttpClient client;
    
    public JSONConnector() {
        refreshHTTPAuth();
        this.sessionId = null;
    }
    
    private void refreshHTTPAuth() {
        if (!Controller.getInstance().useHttpAuth())
            return;
        
        boolean refreshNeeded = false;
        
        if (httpUsername == null || !httpUsername.equals(Controller.getInstance().httpUsername()))
            refreshNeeded = true;
        
        if (httpPassword == null || !httpPassword.equals(Controller.getInstance().httpPassword()))
            refreshNeeded = true;
        
        if (!refreshNeeded)
            return;
        
        // Refresh data
        httpUsername = Controller.getInstance().httpUsername();
        httpPassword = Controller.getInstance().httpPassword();
        
        // Refresh Credentials-Provider
        if (!httpUsername.equals(Constants.EMPTY) && !httpPassword.equals(Constants.EMPTY)) {
            credProvider = new BasicCredentialsProvider();
            credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(httpUsername, httpPassword));
        }
    }
    
    private InputStream doRequest(Map<String, String> params, boolean firstCall) {
        HttpPost post = new HttpPost();
        
        try {
            if (sessionId != null) {
                params.put(SID, sessionId);
            }
            
            // check if http-Auth-Settings have changed, reload values if necessary
            refreshHTTPAuth();
            
            // Set Address
            post.setURI(Controller.getInstance().url());
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
                int timeoutConnection = 5 * SECOND;
                HttpConnectionParams.setConnectionTimeout(httpParams, timeoutConnection);
                
                // Set the default socket timeout (SO_TIMEOUT) which is the timeout for waiting for data.
                // use longer timeout when lazyServer-Feature is used
                int timeoutSocket = (Controller.getInstance().lazyServer()) ? 15 * MINUTE : 8 * SECOND;
                HttpConnectionParams.setSoTimeout(httpParams, timeoutSocket);
                
                post.setParams(httpParams);
            }
            
            // LOG-Output
            if (!Controller.getInstance().logSensitiveData()) {
                // Filter password and session-id
                Object paramPw = json.remove(PARAM_PW);
                Object paramSID = json.remove(SID);
                Log.i(Utils.TAG, "Request: " + json);
                json.put(PARAM_PW, paramPw);
                json.put(SID, paramSID);
            } else {
                Log.i(Utils.TAG, "Request: " + json);
            }
            
            if (client == null)
                client = HttpClientFactory.getInstance().getHttpClient(post.getParams());
            else
                client.setParams(post.getParams());
            
            // Add SSL-Stuff
            if (credProvider != null)
                client.setCredentialsProvider(credProvider);
            
        } catch (Exception e) {
            hasLastError = true;
            lastError = "Error creating HTTP-Connection [ " + e.getMessage() + " ]";
            return null;
        }
        
        HttpResponse response = null;
        try {
            response = client.execute(post); // Execute the request
        } catch (ClientProtocolException e) {
            hasLastError = true;
            lastError = "ClientProtocolException on client.execute(post) [ " + e.getMessage() + " ]";
            return null;
        } catch (SSLException e) {
            if ("No peer certificate".equals(e.getMessage())) {
                // Handle this by ignoring it, this occurrs very often when the connection is instable.
                Log.w(Utils.TAG, "SSLException on client.execute(post) [ " + e.getMessage() + " ]");
            } else {
                hasLastError = true;
                lastError = "SSLException on client.execute(post) [ " + e.getMessage() + " ]";
            }
            return null;
        } catch (InterruptedIOException e) {
            // http://stackoverflow.com/questions/693997/how-to-set-httpresponse-timeout-for-android-in-java/1565243#1565243
            Log.w(Utils.TAG, "InterruptedIOException (" + e.getMessage() + ") in doRequest()");
            return null;
        } catch (SocketException e) {
            // http://stackoverflow.com/questions/693997/how-to-set-httpresponse-timeout-for-android-in-java/1565243#1565243
            Log.w(Utils.TAG, "SocketException (" + e.getMessage() + ") in doRequest()");
            return null;
        } catch (IOException e) {
            Log.w(Utils.TAG, "IOException (" + e.getMessage() + ") in doRequest()");
            return null;
        }
        
        // Try to check for HTTP Status codes
        int code = response.getStatusLine().getStatusCode();
        if (code == HttpStatus.SC_UNAUTHORIZED) {
            hasLastError = true;
            lastError = "Couldn't connect to server. returned status: \"401 Unauthorized (HTTP/1.0 - RFC 1945)\"";
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
            
            if (instream == null) {
                hasLastError = true;
                lastError = "Couldn't get InputStream in Method doRequest(String url) [instream was null]";
                return null;
            }
        } catch (Exception e) {
            if (instream != null)
                try {
                    instream.close();
                } catch (IOException e1) {
                }
            hasLastError = true;
            lastError = "Exception: " + e.getMessage();
            return null;
        }
        
        return instream;
    }
    
    private String readResult(Map<String, String> params, boolean login, boolean firstCall) throws IOException {
        InputStream in = doRequest(params, true);
        if (in == null)
            return null;
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        String ret = "";
        
        // Check if content contains array or object, array indicates login-response or error, object is content
        
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("content")) {
                JsonToken t = reader.peek();
                
                if (t.equals(JsonToken.BEGIN_OBJECT)) {
                    
                    JsonObject object = new JsonObject();
                    reader.beginObject();
                    while (reader.hasNext()) {
                        object.addProperty(reader.nextName(), reader.nextString());
                    }
                    reader.endObject();
                    
                    if (object.get(SESSION_ID) != null) {
                        ret = object.get(SESSION_ID).getAsString();
                        break;
                    } else if (object.get(STATUS) != null) {
                        ret = object.get(STATUS).getAsString();
                        break;
                    } else if (object.get(VALUE) != null) {
                        ret = object.get(VALUE).getAsString();
                        break;
                    } else if (object.get(ERROR) != null) {
                        String message = object.get(ERROR).getAsString();
                        
                        if (message.contains(NOT_LOGGED_IN)) {
                            sessionId = null;
                            if (login())
                                return readResult(params, login, false); // Just do the same request again
                            else
                                return null;
                        }
                        
                        if (message.contains(API_DISABLED)) {
                            hasLastError = true;
                            lastError = String.format(API_DISABLED_MESSAGE, Controller.getInstance().username());
                            return null;
                        }
                        
                        // Any other error
                        hasLastError = true;
                        lastError = ERROR_TEXT + message;
                        return null;
                    }
                }
                
            } else {
                reader.skipValue();
            }
        }
        
        reader.close();
        if (ret.startsWith("\""))
            ret = ret.substring(1, ret.length());
        if (ret.endsWith("\""))
            ret = ret.substring(0, ret.length() - 1);
        
        return ret;
    }
    
    private JsonReader prepareReader(Map<String, String> params) throws IOException {
        return prepareReader(params, true);
    }
    
    private JsonReader prepareReader(Map<String, String> params, boolean firstCall) throws IOException {
        InputStream in = doRequest(params, true);
        if (in == null)
            return null;
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        
        // Check if content contains array or object, array indicates login-response or error, object is content
        
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("content")) {
                JsonToken t = reader.peek();
                
                if (t.equals(JsonToken.BEGIN_OBJECT)) {
                    // Handle error
                    JsonObject object = new JsonObject();
                    reader.beginObject();
                    while (reader.hasNext()) {
                        object.addProperty(reader.nextName(), reader.nextString());
                    }
                    reader.endObject();
                    
                    if (object.get(ERROR) != null) {
                        String message = object.get(ERROR).toString();
                        
                        if (message.contains(NOT_LOGGED_IN)) {
                            sessionId = null;
                            if (login())
                                return prepareReader(params, false); // Just do the same request again
                            else
                                return null;
                        }
                        
                        if (message.contains(API_DISABLED)) {
                            hasLastError = true;
                            lastError = String.format(API_DISABLED_MESSAGE, Controller.getInstance().username());
                            return null;
                        }
                        
                        // Any other error
                        hasLastError = true;
                        lastError = ERROR_TEXT + message;
                    }
                } else if (t.equals(JsonToken.BEGIN_ARRAY)) {
                    return reader;
                }
                
            } else {
                reader.skipValue();
            }
        }
        return null;
    }
    
    private boolean sessionAlive() {
        // Make sure we are logged in
        if (sessionId == null || lastError.equals(NOT_LOGGED_IN))
            if (!login())
                return false;
        if (hasLastError)
            return false;
        return true;
    }
    
    /**
     * Does an API-Call and ignores the result.
     * 
     * @param params
     * @return true if the call was successful.
     */
    private boolean doRequestNoAnswer(Map<String, String> params) {
        if (!sessionAlive())
            return false;
        
        try {
            boolean avoidJSONParsing = false;
            if (avoidJSONParsing) {
                // TODO: remove this branch, if there are no regressions.
                InputStream in = doRequest(params, true);
                if (in != null) {
                    in.close();
                }
            } else {
                readResult(params, false, true);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (!hasLastError) {
                hasLastError = true;
                lastError = ERROR_TEXT + e.getMessage();
            }
        }
        
        // TODO: Why was this here???
        // if (hasLastError) {
        // hasLastError = false;
        // lastError = "";
        // }
        return false;
    }
    
    /**
     * Tries to login to the ttrss-server with the base64-encoded password.
     * 
     * @return true on success, false otherwise
     */
    private boolean login() {
        long time = System.currentTimeMillis();
        
        // Just login once, check if already logged in after acquiring the lock on mSessionId
        synchronized (loginLock) {
            if (sessionId != null && !(lastError.equals(NOT_LOGGED_IN)))
                return true; // Login done while we were waiting for the lock
                
            Map<String, String> params = new HashMap<String, String>();
            params.put(PARAM_OP, VALUE_LOGIN);
            params.put(PARAM_USER, Controller.getInstance().username());
            params.put(PARAM_PW, Base64.encodeBytes(Controller.getInstance().password().getBytes()));
            
            // No check with assertLogin here, we are about to login so no need for this.
            sessionId = null;
            
            try {
                sessionId = readResult(params, true, true);
                if (sessionId != null) {
                    Log.v(Utils.TAG, "login: " + (System.currentTimeMillis() - time) + "ms");
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (!hasLastError) {
                    hasLastError = true;
                    lastError = ERROR_TEXT + e.getMessage();
                }
            }
            
            if (!hasLastError) {
                // Login didnt succeed, write message
                hasLastError = true;
                lastError = NOT_LOGGED_IN_MESSAGE;
            }
            return false;
        }
    }
    
    // ***************** Helper-Methods **************************************************
    
    private void parseCounter(JsonReader reader) {
        SQLiteDatabase db = DBHelper.getInstance().db;
        synchronized (DBHelper.TABLE_ARTICLES) {
            db.beginTransaction();
            try {
                reader.beginArray();
                while (reader.hasNext()) {
                    
                    boolean cat = false;
                    int id = Integer.MAX_VALUE;
                    int counter = 0;
                    
                    reader.beginObject();
                    while (reader.hasNext()) {
                        
                        try {
                            String name = reader.nextName();
                            
                            if (name.equals(COUNTER_KIND)) {
                                cat = reader.nextString().equals(COUNTER_CAT);
                            } else if (name.equals(COUNTER_ID)) {
                                String value = reader.nextString();
                                // Check if id is a string, then it would be a global counter
                                if (value.equals("global-unread") || value.equals("subscribed-feeds"))
                                    continue;
                                id = Integer.parseInt(value);
                            } else if (name.equals(COUNTER_COUNTER)) {
                                String value = reader.nextString();
                                // Check if null because of an API-bug
                                if (!value.equals("null"))
                                    counter = Integer.parseInt(value);
                            } else {
                                reader.skipValue();
                            }
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            reader.skipValue();
                            continue;
                        }
                        
                    }
                    reader.endObject();
                    
                    ContentValues cv = new ContentValues();
                    cv.put("unread", counter);
                    
                    if (id == Integer.MAX_VALUE)
                        continue;
                    
                    if (cat && id >= 0) {
                        // Category
                        db.update(DBHelper.TABLE_CATEGORIES, cv, "id=?", new String[] { id + "" });
                        Data.getInstance().setFeedsChanged(id, System.currentTimeMillis());
                    } else if (!cat && id < 0 && id >= -4) {
                        // Virtual Category
                        db.update(DBHelper.TABLE_CATEGORIES, cv, "id=?", new String[] { id + "" });
                    } else if (!cat && id > 0) {
                        // Feed
                        db.update(DBHelper.TABLE_FEEDS, cv, "id=?", new String[] { id + "" });
                    } else if (!cat && id < -10) {
                        // Label
                        db.update(DBHelper.TABLE_FEEDS, cv, "id=?", new String[] { id + "" });
                    }
                    
                }
                reader.endArray();
                
                db.setTransactionSuccessful();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                Data.getInstance().setCategoriesChanged(System.currentTimeMillis());
                DBHelper.getInstance().purgeArticlesNumber();
            }
        }
    }
    
    private Set<String> parseAttachments(JsonReader reader) throws IOException {
        Set<String> ret = new HashSet<String>();
        reader.beginArray();
        while (reader.hasNext()) {
            
            String attId = null;
            String attUrl = null;
            
            reader.beginObject();
            while (reader.hasNext()) {
                
                try {
                    String name = reader.nextName();
                    if (name.equals(CONTENT_URL)) {
                        attUrl = reader.nextString();
                    } else if (name.equals(ID)) {
                        attId = reader.nextString();
                    } else {
                        reader.skipValue();
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    reader.skipValue();
                    continue;
                }
                
            }
            reader.endObject();
            
            if (attId != null && attUrl != null)
                ret.add(attUrl);
        }
        reader.endArray();
        return ret;
    }
    
    private void parseArticle(JsonReader reader, int labelId, int catId, boolean isCategory) {
        
        SQLiteDatabase db = DBHelper.getInstance().db;
        synchronized (DBHelper.TABLE_ARTICLES) {
            db.beginTransaction();
            try {
                
                // People are complaining about not all articles being marked the right way, so just overwrite all
                // unread
                // states and fetch new articles...
                // Moved this inside the transaction to make sure this only happens if the transaction is successful
                DBHelper.getInstance().markFeedOnlyArticlesRead(catId, isCategory);
                
                reader.beginArray();
                while (reader.hasNext()) {
                    
                    int id = -1;
                    String title = null;
                    boolean isUnread = false;
                    Date updated = null;
                    int realFeedId = 0;
                    String content = null;
                    String articleUrl = null;
                    String articleCommentUrl = null;
                    Set<String> attachments = null;
                    boolean isStarred = false;
                    boolean isPublished = false;
                    
                    reader.beginObject();
                    while (reader.hasNext() && reader.peek().equals(JsonToken.NAME)) { // ?
                        String name = reader.nextName();
                        
                        try {
                            if (name.equals(ID)) {
                                id = reader.nextInt();
                            } else if (name.equals(TITLE)) {
                                title = reader.nextString();
                            } else if (name.equals(UNREAD)) {
                                isUnread = reader.nextBoolean();
                            } else if (name.equals(UPDATED)) {
                                updated = new Date(new Long(reader.nextString() + "000").longValue());
                            } else if (name.equals(FEED_ID)) {
                                realFeedId = reader.nextInt();
                            } else if (name.equals(CONTENT)) {
                                content = reader.nextString();
                            } else if (name.equals(URL)) {
                                articleUrl = reader.nextString();
                            } else if (name.equals(COMMENT_URL)) {
                                articleCommentUrl = reader.nextString();
                            } else if (name.equals(ATTACHMENTS)) {
                                attachments = parseAttachments(reader);
                            } else if (name.equals(STARRED)) {
                                isStarred = reader.nextBoolean();
                            } else if (name.equals(PUBLISHED)) {
                                isPublished = reader.nextBoolean();
                            } else {
                                reader.skipValue();
                            }
                        } catch (IllegalArgumentException e) {
                            Log.w(Utils.TAG, "Result contained illegal value for entry \"" + name + "\".");
                            reader.skipValue();
                            continue;
                        }
                        
                    }
                    reader.endObject();
                    
                    if (id != -1 && title != null)
                        DBHelper.getInstance().insertArticle(id, realFeedId, title, isUnread, articleUrl,
                                articleCommentUrl, updated, content, attachments, isStarred, isPublished, labelId);
                }
                reader.endArray();
                
                db.setTransactionSuccessful();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                
                if (isCategory)
                    Data.getInstance().setCategoriesChanged(System.currentTimeMillis());
                else
                    Data.getInstance().setFeedsChanged(catId, System.currentTimeMillis());
                
                DBHelper.getInstance().purgeArticlesNumber();
            }
        }
    }
    
    // ***************** Retrieve-Data-Methods **************************************************
    
    @Override
    public boolean getCounters() {
        boolean ret = true;
        makeLazyServerWork(); // otherwise the unread counters may be outdated
        
        if (!sessionAlive())
            return false;
        
        long time = System.currentTimeMillis();
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_GET_COUNTERS);
        params.put(PARAM_OUTPUT_MODE, VALUE_OUTPUT_MODE);
        
        JsonReader reader = null;
        try {
            reader = prepareReader(params);
            if (reader == null)
                return false;
            
            parseCounter(reader);
            
        } catch (IOException e) {
            e.printStackTrace();
            ret = false;
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e1) {
                }
        }
        
        Log.v(Utils.TAG, "getCounters: " + (System.currentTimeMillis() - time) + "ms");
        return ret;
    }
    
    @Override
    public Set<Category> getCategories() {
        long time = System.currentTimeMillis();
        Set<Category> ret = new LinkedHashSet<Category>();
        if (!sessionAlive())
            return ret;
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_GET_CATEGORIES);
        
        JsonReader reader = null;
        try {
            reader = prepareReader(params);
            
            if (reader == null)
                return ret;
            
            reader.beginArray();
            while (reader.hasNext()) {
                
                int id = -1;
                String title = null;
                int unread = 0;
                
                reader.beginObject();
                while (reader.hasNext()) {
                    
                    try {
                        String name = reader.nextName();
                        
                        if (name.equals(ID)) {
                            id = reader.nextInt();
                        } else if (name.equals(TITLE)) {
                            title = reader.nextString();
                        } else if (name.equals(UNREAD)) {
                            unread = reader.nextInt();
                        } else {
                            reader.skipValue();
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        reader.skipValue();
                        continue;
                    }
                    
                }
                reader.endObject();
                
                if (id != -1 && title != null)
                    ret.add(new Category(id, title, unread));
            }
            reader.endArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e1) {
                }
        }
        
        Log.v(Utils.TAG, "getCategories: " + (System.currentTimeMillis() - time) + "ms");
        return ret;
    }
    
    private Set<Feed> getFeeds(boolean tolerateWrongUnreadInformation) {
        long time = System.currentTimeMillis();
        Set<Feed> ret = new LinkedHashSet<Feed>();;
        if (!sessionAlive())
            return ret;
        
        if (!tolerateWrongUnreadInformation) {
            makeLazyServerWork();
        }
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_GET_FEEDS);
        params.put(PARAM_CAT_ID, Data.VCAT_ALL + ""); // Hardcoded -4 fetches all feeds. See
                                                      // http://tt-rss.org/redmine/wiki/tt-rss/JsonApiReference#getFeeds
        
        JsonReader reader = null;
        try {
            reader = prepareReader(params);
            
            if (reader == null)
                return ret;
            
            reader.beginArray();
            while (reader.hasNext()) {
                
                int categoryId = -1;
                int id = 0;
                String title = null;
                String feedUrl = null;
                int unread = 0;
                
                reader.beginObject();
                while (reader.hasNext()) {
                    
                    try {
                        String name = reader.nextName();
                        
                        if (name.equals(ID)) {
                            id = reader.nextInt();
                        } else if (name.equals(CAT_ID)) {
                            categoryId = reader.nextInt();
                        } else if (name.equals(TITLE)) {
                            title = reader.nextString();
                        } else if (name.equals(FEED_URL)) {
                            feedUrl = reader.nextString();
                        } else if (name.equals(UNREAD)) {
                            unread = reader.nextInt();
                        } else {
                            reader.skipValue();
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        reader.skipValue();
                        continue;
                    }
                    
                }
                reader.endObject();
                
                if (id != -1 || categoryId == -2) // normal feed (>0) or label (-2)
                    if (title != null) // Dont like complicated if-statements..
                        ret.add(new Feed(id, categoryId, title, feedUrl, unread));
                
            }
            reader.endArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e1) {
                }
        }
        
        Log.v(Utils.TAG, "getFeeds: " + (System.currentTimeMillis() - time) + "ms");
        return ret;
    }
    
    @Override
    public Set<Feed> getFeeds() {
        return getFeeds(false);
    }
    
    private boolean makeLazyServerWork(Integer feedId) {
        if (Controller.getInstance().lazyServer()) {
            Map<String, String> taskParams = new HashMap<String, String>();
            taskParams.put(PARAM_OP, VALUE_UPDATE_FEED);
            taskParams.put(PARAM_FEED_ID, feedId + "");
            return doRequestNoAnswer(taskParams);
        }
        return true;
    }
    
    private long noTaskUntil = 0;
    final static private long minTaskIntervall = 30 * MINUTE; // TODO: Maybe it does not hurt to reduce this to 1 or 2
                                                              // minutes.
    
    private boolean makeLazyServerWork() {
        boolean ok = true;
        final long time = System.currentTimeMillis();
        if (Controller.getInstance().lazyServer() && (noTaskUntil < time)) {
            noTaskUntil = time + minTaskIntervall;
            Set<Feed> feedset = getFeeds(true);
            Iterator<Feed> feeds = feedset.iterator();
            while (feeds.hasNext()) {
                final Feed f = feeds.next();
                ok = makeLazyServerWork(f.id) && ok;
            }
        }
        return ok;
    }
    
    @Override
    public boolean getHeadlinesToDatabase(Integer id, int limit, String viewMode, boolean isCategory) {
        long time = System.currentTimeMillis();
        boolean ret = true;
        
        if (!sessionAlive())
            return false;
        
        makeLazyServerWork(id);
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_GET_HEADLINES);
        params.put(PARAM_FEED_ID, id + "");
        params.put(PARAM_LIMIT, limit + "");
        params.put(PARAM_VIEWMODE, viewMode);
        params.put(PARAM_SHOW_CONTENT, "1");
        params.put(PARAM_INC_ATTACHMENTS, "1");
        params.put(PARAM_IS_CAT, (isCategory ? "1" : "0"));
        
        if (id == Data.VCAT_STAR && isCategory)
            DBHelper.getInstance().purgeStarredArticles();
        
        if (id == Data.VCAT_PUB && isCategory)
            DBHelper.getInstance().purgePublishedArticles();
        
        JsonReader reader = null;
        try {
            reader = prepareReader(params);
            if (reader == null)
                return false;
            
            parseArticle(reader, (!isCategory && id < -10 ? id : -1), id, isCategory);
            
        } catch (IOException e) {
            e.printStackTrace();
            ret = false;
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e1) {
                }
        }
        
        Log.v(Utils.TAG, "getHeadlinesToDatabase: " + (System.currentTimeMillis() - time) + "ms");
        return ret;
    }
    
    @Override
    public boolean setArticleRead(Set<Integer> ids, int articleState) {
        boolean ret = true;
        if (ids.size() == 0)
            return ret;
        
        for (String idList : StringSupport.convertListToString(ids, MAX_ID_LIST_LENGTH)) {
            Map<String, String> params = new HashMap<String, String>();
            params.put(PARAM_OP, VALUE_UPDATE_ARTICLE);
            params.put(PARAM_ARTICLE_IDS, idList);
            params.put(PARAM_MODE, articleState + "");
            params.put(PARAM_FIELD, "2");
            ret = ret && doRequestNoAnswer(params);
        }
        return ret;
    }
    
    @Override
    public boolean setArticleStarred(Set<Integer> ids, int articleState) {
        boolean ret = true;
        if (ids.size() == 0)
            return ret;
        
        for (String idList : StringSupport.convertListToString(ids, MAX_ID_LIST_LENGTH)) {
            Map<String, String> params = new HashMap<String, String>();
            params.put(PARAM_OP, VALUE_UPDATE_ARTICLE);
            params.put(PARAM_ARTICLE_IDS, idList);
            params.put(PARAM_MODE, articleState + "");
            params.put(PARAM_FIELD, "0");
            ret = ret && doRequestNoAnswer(params);
        }
        return ret;
    }
    
    @Override
    public boolean setArticlePublished(Set<Integer> ids, int articleState) {
        boolean ret = true;
        if (ids.size() == 0)
            return ret;
        
        for (String idList : StringSupport.convertListToString(ids, MAX_ID_LIST_LENGTH)) {
            Map<String, String> params = new HashMap<String, String>();
            params.put(PARAM_OP, VALUE_UPDATE_ARTICLE);
            params.put(PARAM_ARTICLE_IDS, idList);
            params.put(PARAM_MODE, articleState + "");
            params.put(PARAM_FIELD, "1");
            ret = ret && doRequestNoAnswer(params);
        }
        return ret;
    }
    
    @Override
    public boolean setRead(int id, boolean isCategory) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_CATCHUP);
        params.put(PARAM_FEED_ID, id + "");
        params.put(PARAM_IS_CAT, (isCategory ? "1" : "0"));
        return doRequestNoAnswer(params);
    }
    
    @Override
    public String getPref(String pref) {
        if (!sessionAlive())
            return null;
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_GET_PREF);
        params.put(PARAM_PREF, pref);
        
        try {
            String ret = readResult(params, false, true);
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    @Override
    public int getVersion() {
        int ret = -1;
        if (!sessionAlive())
            return ret;
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_GET_VERSION);
        
        String response = "";
        JsonReader reader = null;
        try {
            reader = prepareReader(params);
            if (reader == null)
                return ret;
            
            reader.beginArray();
            while (reader.hasNext()) {
                try {
                    
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        
                        if (name.equals(VERSION)) {
                            response = reader.nextString();
                        } else {
                            reader.skipValue();
                        }
                        
                    }
                    
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
            
            // Replace dots, parse integer
            ret = Integer.parseInt(response.replace(".", ""));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e1) {
                }
        }
        
        return ret;
    }
    
    @Override
    public boolean hasLastError() {
        return hasLastError;
    }
    
    @Override
    public String pullLastError() {
        String ret = new String(lastError);
        lastError = "";
        hasLastError = false;
        return ret;
    }
    
}
