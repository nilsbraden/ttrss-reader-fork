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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.Base64;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class JSONConnector implements Connector {
    
    private static String lastError = "";
    private static boolean hasLastError = false;
    
    private static final String PARAM_OP = "op";
    private static final String PARAM_USER = "user";
    private static final String PARAM_PW = "password";
    private static final String PARAM_CAT_ID = "cat_id";
    private static final String PARAM_FEED_ID = "feed_id";
    private static final String PARAM_ARTICLE_ID = "article_id";
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
    private static final String VALUE_GET_ARTICLE = "getArticle";
    private static final String VALUE_GET_HEADLINES = "getHeadlines";
    private static final String VALUE_UPDATE_ARTICLE = "updateArticle";
    private static final String VALUE_CATCHUP = "catchupFeed";
    private static final String VALUE_GET_PREF = "getPref";
    private static final String VALUE_GET_VERSION = "getVersion";
    private static final String VALUE_GET_COUNTERS = "getCounters";
    private static final String VALUE_OUTPUT_MODE = "flc"; // f - feeds, l - labels, c - categories, t - tags
    
    private static final String ERROR = "error";
    private static final String NOT_LOGGED_IN = "NOT_LOGGED_IN";
    private static final String API_DISABLED = "API_DISABLED";
    private static final String API_DISABLED_MESSAGE = "Please enable API for the user \"%s\" in the preferences of this user on the Server.";
    private static final String OK = "OK";
    
    private static final String SESSION_ID = "session_id";
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
    
    private String httpUsername;
    private String httpPassword;
    
    private String sessionId;
    private String loginLock = "";
    
    private CredentialsProvider credProvider = null;
    private HttpPost post;
    private DefaultHttpClient client;
    
    public JSONConnector() {
        refreshHTTPAuth();
        this.sessionId = null;
        this.post = new HttpPost();
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
    
    private InputStream doRequest(Map<String, String> map, boolean firstCall) {
        try {
            if (sessionId != null)
                map.put(SESSION_ID, sessionId);
            
            // check if http-Auth-Settings have changed, reload values if necessary
            refreshHTTPAuth();
            
            // Set Address
            if (Controller.getInstance().url() != null) {
                post.setURI(Controller.getInstance().url());
                post.addHeader("Accept-Encoding", "gzip");
            } else {
                hasLastError = true;
                lastError = "Server-URL could not be parsed, please check your preferences.";
                return null;
            }
            
            // Add POST data
            JSONObject json = new JSONObject(map);
            StringEntity jsonData = new StringEntity(json.toString(), "UTF-8");
            jsonData.setContentType("application/json");
            post.setEntity(jsonData);
            
            // LOG-Output
            if (!Controller.getInstance().logSensitiveData()) {
                // Filter password and session-id
                Object paramPw = json.remove(PARAM_PW);
                Object paramSID = json.remove(SESSION_ID);
                Log.i(Utils.TAG, "Request: " + json);
                json.put(PARAM_PW, paramPw);
                json.put(SESSION_ID, paramSID);
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
            lastError = "Error creating HTTP-Connection: " + e.getMessage() + " [ " + e.getCause() + " ]";
            return null;
        }
        
        HttpResponse response = null;
        try {
            response = client.execute(post); // Execute the request
        } catch (ClientProtocolException e) {
            hasLastError = true;
            lastError = "ClientProtocolException on client.execute(httpPost) [ " + e.getCause() + " ]";
            return null;
        } catch (IOException e) {
            /*
             * Occurs on timeout of the connection. Would be better to catch the specialized exception for that
             * case but which is it? The Reference (http://developer.android.com/reference/java/io/IOException.html)
             * lists lots of subclasses.
             */
            Log.w(Utils.TAG, "IOException (" + e.getMessage() + ") occurred in doRequest()...");
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
            hasLastError = true;
            lastError = "Exception: " + e.getMessage();
            return null;
        }
        
        return instream;
    }
    
    private boolean handleError(JsonReader reader) throws IOException, NotLoggedInException {
        reader.beginObject();
        if (reader.nextName().equals(ERROR)) {
            String message = reader.nextString();
            if (message.contains(NOT_LOGGED_IN)) {
                sessionId = null;
                login();
                throw new NotLoggedInException("");
            }
            
            if (message.contains(API_DISABLED)) {
                hasLastError = true;
                lastError = String.format(API_DISABLED_MESSAGE, Controller.getInstance().username());
                return false;
            }
            
            hasLastError = true;
            lastError = message;
        }
        return false;
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
    
    private String doRequestNoAnswer(Map<String, String> map) {
        if (!sessionAlive())
            return ERROR;
        
        try {
            JsonReader reader = prepareReader(map);
            
            if (reader == null) {
                // TODO
            }
            
            reader.beginArray();
            while (reader.hasNext()) {
                
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals(OK)) {
                        return OK;
                    } else {
                        reader.skipValue();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (hasLastError) {
            hasLastError = false;
            lastError = "";
        }
        return ERROR;
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
                JsonReader reader = prepareReader(params, true, true);
                
                if (reader == null) {
                    // TODO
                }
                
                // No beginArray() here since API gives Object here, login isnt wrapped in an Array. Errors too?
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals(SESSION_ID)) {
                        sessionId = reader.nextString();
                        Log.v(Utils.TAG, "login: " + (System.currentTimeMillis() - time) + "ms");
                        return true;
                    } else if (name.equals(ERROR)) {
                        String error = reader.nextString();
                        error = (error.length() > 200 ? error.substring(0, 200) : error);
                        hasLastError = true;
                        lastError = "An Error occurred: " + error;
                        return false;
                    } else {
                        reader.skipValue();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            // Login didnt succeed
            return false;
        }
    }
    
    // ***************** Helper-Methods **************************************************
    
    private boolean parseCounterData(Map<String, String> params) throws JSONException {
        SQLiteDatabase db = DBHelper.getInstance().db;
        synchronized (DBHelper.TABLE_ARTICLES) {
            db.beginTransaction();
            try {
                
                JsonReader reader = prepareReader(params);
                
                if (reader == null) {
                    // TODO
                }
                
                reader.beginArray();
                while (reader.hasNext()) {
                    
                    boolean cat = false;
                    int id = 0;
                    int counter = 0;
                    
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        
                        if (name.equals(COUNTER_KIND)) {
                            cat = reader.nextString().equals(COUNTER_CAT);
                        } else if (name.equals(COUNTER_ID)) {
                            // Check if id is a string, then it would be a global counter
                            String value = reader.nextString();
                            if (value.equals("global-unread") || value.equals("subscribed-feeds")) {
                                continue;
                            }
                            id = Integer.parseInt(value);
                            
                        } else if (name.equals(COUNTER_COUNTER)) {
                            // Check if null because of an API-bug
                            String value = reader.nextString();
                            if (value.equals("null"))
                                counter = Integer.parseInt(value);
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                    
                    ContentValues cv = new ContentValues();
                    cv.put("unread", counter);
                    
                    if (cat && id >= 0) { // Category
                        db.update(DBHelper.TABLE_CATEGORIES, cv, "id=?", new String[] { id + "" });
                    } else if (!cat && id < 0 && id >= -4) { // Virtual Category
                        db.update(DBHelper.TABLE_CATEGORIES, cv, "id=?", new String[] { id + "" });
                    } else if (!cat && id > 0) { // Feed
                        db.update(DBHelper.TABLE_FEEDS, cv, "id=?", new String[] { id + "" });
                    } else if (!cat && id < -10) { // Label
                        db.update(DBHelper.TABLE_FEEDS, cv, "id=?", new String[] { id + "" });
                    }
                    
                }
                reader.endArray();
                
                db.setTransactionSuccessful();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                DBHelper.getInstance().purgeArticlesNumber();
            }
        }
        return true; // Everything went fine
    }
    
    private Set<String> parseAttachments(JsonReader reader) throws IOException {
        Set<String> ret = new HashSet<String>();
        reader.beginArray();
        while (reader.hasNext()) {
            
            String attId = null;
            String attUrl = null;
            
            reader.beginObject();
            while (reader.hasNext()) {
                
                String name = reader.nextName();
                if (name.equals(CONTENT_URL)) {
                    attUrl = reader.nextString();
                } else if (name.equals(ID)) {
                    attId = reader.nextString();
                } else {
                    reader.skipValue();
                }
                
            }
            
            if (attId != null && attUrl != null)
                ret.add(attUrl);
        }
        return ret;
    }
    
    private JsonReader prepareReader(Map<String, String> params) throws IOException {
        return prepareReader(params, false, true);
    }
    
    private JsonReader prepareReader(Map<String, String> params, boolean login, boolean firstCall) throws IOException {
        try {
            InputStream in = doRequest(params, true);
            JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
            
            // Check if content contains array or object, array indicates login-response or error, object is content
            
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("content")) {
                    JsonToken t = reader.peek();
                    
                    if (t.equals(JsonToken.BEGIN_OBJECT)) {
                        if (login) {
                            return reader;
                        } else {
                            // Error
                            handleError(reader);
                        }
                    }
                    
                    // We should have an BEGIN_ARRAY here...
                    return reader;
                } else {
                    reader.skipValue();
                }
            }
        } catch (NotLoggedInException nlie) {
            if (firstCall)
                prepareReader(params, login, false);
            else {
                hasLastError = true;
                lastError = "Login geht nicht!";
            }
        }
        return null;
    }
    
    private boolean parseArticle(JsonReader reader, int labelId) throws NotLoggedInException {
        
        SQLiteDatabase db = DBHelper.getInstance().db;
        synchronized (DBHelper.TABLE_ARTICLES) {
            db.beginTransaction();
            try {
                reader.beginArray();
                while (reader.hasNext()) {
                    
                    int id = 0;
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
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        
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
                        }
                        
                    }
                    reader.endObject();
                    DBHelper.getInstance().insertArticle(id, realFeedId, title, isUnread, articleUrl,
                            articleCommentUrl, updated, content, attachments, isStarred, isPublished, labelId);
                }
                reader.endArray();
                
                db.setTransactionSuccessful();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
                DBHelper.getInstance().purgeArticlesNumber();
            }
        }
        return true; // Everything went fine
    }
    
    // ***************** Retrieve-Data-Methods **************************************************
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#getCounters()
     */
    @Override
    public void getCounters() {
        long time = System.currentTimeMillis();
        if (!sessionAlive())
            return;
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_GET_COUNTERS);
        params.put(PARAM_OUTPUT_MODE, VALUE_OUTPUT_MODE);
        
        try {
            boolean ret = parseCounterData(params);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.v(Utils.TAG, "getCounters: " + (System.currentTimeMillis() - time) + "ms");
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#getCategories()
     */
    @Override
    public Set<Category> getCategories() {
        long time = System.currentTimeMillis();
        if (!sessionAlive())
            return null;
        
        Set<Category> ret = new LinkedHashSet<Category>();
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_GET_CATEGORIES);
        
        try {
            JsonReader reader = prepareReader(params);
            
            if (reader == null) {
                // TODO
            }
            
            reader.beginArray();
            while (reader.hasNext()) {
                
                int id = 0;
                String title = "";
                int unread = 0;
                
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    
                    if (name.equals(ID)) {
                        id = reader.nextInt();
                    } else if (name.equals(TITLE)) {
                        title = reader.nextString();
                    } else if (name.equals(UNREAD)) {
                        unread = reader.nextInt();
                    }
                    
                }
                reader.endObject();
                ret.add(new Category(id, title, unread));
            }
            reader.endArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        Log.v(Utils.TAG, "getCategories: " + (System.currentTimeMillis() - time) + "ms");
        return ret;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#getFeeds()
     */
    @Override
    public Set<Feed> getFeeds() {
        long time = System.currentTimeMillis();
        if (!sessionAlive())
            return null;
        
        Set<Feed> ret = new LinkedHashSet<Feed>();;
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_GET_FEEDS);
        params.put(PARAM_CAT_ID, "-4"); // Hardcoded -4 fetches all feeds. See
                                        // http://tt-rss.org/redmine/wiki/tt-rss/JsonApiReference#getFeeds
        
        try {
            JsonReader reader = prepareReader(params);
            
            if (reader == null) {
                // TODO
            }
            
            reader.beginArray();
            while (reader.hasNext()) {
                
                int categoryId = 0;
                int id = 0;
                String title = "";
                String feedUrl = "";
                int unread = 0;
                
                reader.beginObject();
                while (reader.hasNext()) {
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
                    }
                    
                }
                reader.endObject();
                if (id > 0 || categoryId == -2) // normal feed (>0) or label (-2)
                    ret.add(new Feed(id, categoryId, title, feedUrl, unread));
            }
            reader.endArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        Log.v(Utils.TAG, "getFeeds: " + (System.currentTimeMillis() - time) + "ms");
        return ret;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#getArticle(java.util.Set)
     */
    @Override
    public void getArticlesToDatabase(Set<Integer> ids) {
        long time = System.currentTimeMillis();
        if (ids.size() == 0)
            return;
        if (!sessionAlive())
            return;
        
        for (String idList : StringSupport.convertListToString(ids)) {
            if (idList.length() == 0)
                continue;
            
            Map<String, String> params = new HashMap<String, String>();
            params.put(PARAM_OP, VALUE_GET_ARTICLE);
            params.put(PARAM_ARTICLE_ID, idList);
            
            try {
                
                JsonReader reader = prepareReader(params);
                
                if (reader == null) {
                    // TODO
                }
                
                boolean ret = parseArticle(reader, -1);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NotLoggedInException e) {
                e.printStackTrace();
            }
        }
        Log.v(Utils.TAG, "getArticle: " + (System.currentTimeMillis() - time) + "ms");
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#getHeadlinesToDatabase(java.lang.Integer, int, int, java.lang.String)
     */
    @Override
    public void getHeadlinesToDatabase(Integer id, int limit, String viewMode, boolean isCategory) {
        long time = System.currentTimeMillis();
        
        if (!sessionAlive())
            return;
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_GET_HEADLINES);
        params.put(PARAM_FEED_ID, id + "");
        params.put(PARAM_LIMIT, limit + "");
        params.put(PARAM_VIEWMODE, viewMode);
        params.put(PARAM_SHOW_CONTENT, "1");
        params.put(PARAM_INC_ATTACHMENTS, "1");
        params.put(PARAM_IS_CAT, (isCategory ? "1" : "0"));
        
        if (id == -1 && isCategory)
            DBHelper.getInstance().purgeStarredArticles();
        
        if (id == -2 && isCategory)
            DBHelper.getInstance().purgePublishedArticles();
        
        // People are complaining about not all articles beeing marked the right way, so just overwrite all unread
        // states and fetch new articles...
        DBHelper.getInstance().markFeedOnlyArticlesRead(id, isCategory);
        
        try {
            
            JsonReader reader = prepareReader(params);
            
            if (reader == null) {
                // TODO
            }
            
            boolean ret = parseArticle(reader, -1);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NotLoggedInException e) {
            e.printStackTrace();
        }
        
        Log.v(Utils.TAG, "getHeadlinesToDatabase: " + (System.currentTimeMillis() - time) + "ms");
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#setArticleRead(java.util.Set, int)
     */
    @Override
    public boolean setArticleRead(Set<Integer> ids, int articleState) {
        if (ids.size() == 0)
            return true;
        
        String ret = "";
        
        for (String idList : StringSupport.convertListToString(ids)) {
            Map<String, String> params = new HashMap<String, String>();
            params.put(PARAM_OP, VALUE_UPDATE_ARTICLE);
            params.put(PARAM_ARTICLE_IDS, idList);
            params.put(PARAM_MODE, articleState + "");
            params.put(PARAM_FIELD, "2");
            ret = doRequestNoAnswer(params);
        }
        return (ret != null && ret.contains(OK));
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#setArticleStarred(java.util.Set, int)
     */
    @Override
    public boolean setArticleStarred(Set<Integer> ids, int articleState) {
        if (ids.size() == 0)
            return true;
        
        String ret = "";
        
        for (String idList : StringSupport.convertListToString(ids)) {
            Map<String, String> params = new HashMap<String, String>();
            params.put(PARAM_OP, VALUE_UPDATE_ARTICLE);
            params.put(PARAM_ARTICLE_IDS, idList);
            params.put(PARAM_MODE, articleState + "");
            params.put(PARAM_FIELD, "0");
            ret = doRequestNoAnswer(params);
        }
        return (ret != null && ret.contains(OK));
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#setArticlePublished(java.util.Set, int)
     */
    @Override
    public boolean setArticlePublished(Set<Integer> ids, int articleState) {
        if (ids.size() == 0)
            return true;
        
        String ret = "";
        
        for (String idList : StringSupport.convertListToString(ids)) {
            Map<String, String> params = new HashMap<String, String>();
            params.put(PARAM_OP, VALUE_UPDATE_ARTICLE);
            params.put(PARAM_ARTICLE_IDS, idList);
            params.put(PARAM_MODE, articleState + "");
            params.put(PARAM_FIELD, "1");
            ret = doRequestNoAnswer(params);
        }
        return (ret != null && ret.contains(OK));
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#setRead(int, boolean)
     */
    @Override
    public boolean setRead(int id, boolean isCategory) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_CATCHUP);
        params.put(PARAM_FEED_ID, id + "");
        params.put(PARAM_IS_CAT, (isCategory ? "1" : "0"));
        String ret = doRequestNoAnswer(params);
        return (ret != null && ret.contains(OK));
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#getPref(java.lang.String)
     */
    @Override
    public String getPref(String pref) {
        if (!sessionAlive())
            return null;
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_GET_PREF);
        params.put(PARAM_PREF, pref);
        
        try {
            JsonReader reader = prepareReader(params);
            
            if (reader == null) {
                // TODO
            }
            
            reader.beginArray();
            while (reader.hasNext()) {
                
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    
                    if (name.equals(VALUE))
                        return reader.nextString();
                    
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#getVersion()
     */
    @Override
    public int getVersion() {
        if (!sessionAlive())
            return -1;
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_GET_VERSION);
        
        String ret = "";
        try {
            JsonReader reader = prepareReader(params);
            
            if (reader == null) {
                // TODO
            }
            
            reader.beginArray();
            while (reader.hasNext()) {
                
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    
                    if (name.equals(VERSION))
                        ret = reader.nextString();
                    
                }
            }
            
            // Replace dots, parse integer
            return Integer.parseInt(ret.replace(".", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return -1;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#hasLastError()
     */
    @Override
    public boolean hasLastError() {
        return hasLastError;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#pullLastError()
     */
    @Override
    public String pullLastError() {
        String ret = new String(lastError);
        lastError = "";
        hasLastError = false;
        return ret;
    }
    
}
