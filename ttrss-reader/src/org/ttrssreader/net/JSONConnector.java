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
import java.net.URI;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
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
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class JSONConnector implements Connector {
    
    private static String lastError = "";
    private static boolean hasLastError = false;
    
    private static final String OP_LOGIN = "?op=login&user=%s&password=%s";
    private static final String OP_GET_CATEGORIES = "?op=getCategories";
    private static final String OP_GET_FEEDS = "?op=getFeeds&cat_id=%s";
    private static final String OP_GET_ARTICLE = "?op=getArticle&article_id=%s";
    
    // include_attachments available since 1.5.3 but is ignored on older versions
    private static final String OP_GET_FEEDHEADLINES = "?op=getHeadlines&feed_id=%s&limit=%s&view_mode=%s&show_content=1&include_attachments=1&is_cat=%s";
    
    private static final String OP_UPDATE_ARTICLE = "?op=updateArticle&article_ids=%s&mode=%s&field=%s";
    private static final String OP_CATCHUP = "?op=catchupFeed&feed_id=%s&is_cat=%s";
    private static final String OP_GET_PREF = "?op=getPref&pref_name=%s";
    private static final String OP_GET_VERSION = "?op=getVersion";
    private static final String OP_GET_COUNTERS = "?op=getCounters&output_mode=fc"; // output_mode (default: flc) - what
                                                                                    // kind of information to return
                                                                                    // (f-feeds, l-labels, c-categories,
                                                                                    // t-tags)
    
    private static final String ERROR = "{\"error\":";
    private static final String NOT_LOGGED_IN = ERROR + "\"NOT_LOGGED_IN\"}";
    private static final String API_DISABLED = ERROR + "\"API_DISABLED\"}";
    private static final String API_DISABLED_MESSAGE = "Please enable API for the user \"%s\" in the preferences of this user on the Server.";
    private static final String OK = "\"status\":\"OK\"";
    
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
    private static final String STARRED = "marked";
    private static final String PUBLISHED = "published";
    private static final String VALUE = "value";
    private static final String VERSION = "version";
    private static final String SID_APPEND = "&sid=%s";
    
    private static final String COUNTER_KIND = "kind";
    private static final String COUNTER_CAT = "cat";
    private static final String COUNTER_ID = "id";
    private static final String COUNTER_COUNTER = "counter";
    
    private String httpUsername;
    private String httpPassword;
    
    private String sessionId;
    private String sidUrl;
    private String loginLock = "";
    
    private CredentialsProvider credProvider = null;
    
    // HTTP-Stuff
    HttpPost post;
    DefaultHttpClient client;
    
    public JSONConnector() {
        this.sessionId = null;
        refreshHTTPAuth();
        this.post = new HttpPost();
    }
    
    private void refreshHTTPAuth() {
        boolean refreshNeeded = false;
        
        if (httpUsername == null || !httpUsername.equals(Controller.getInstance().httpUsername())) {
            refreshNeeded = true;
        }
        
        if (httpPassword == null || !httpPassword.equals(Controller.getInstance().httpPassword())) {
            refreshNeeded = true;
        }
        
        if (!refreshNeeded)
            return;
        
        if (Controller.getInstance().useHttpAuth()) {
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
    }
    
    private String doRequest(String url, boolean firstCall) {
        Log.v(Utils.TAG, "Request: " + url);
        
        try {
            // check if http-Auth-Settings have changed, reload values if necessary
            refreshHTTPAuth();
            
            post.setURI(new URI(url));
            HttpParams params = post.getParams();
            if (client == null) {
                client = HttpClientFactory.getInstance().getHttpClient(params);
            } else {
                client.setParams(params);
            }
            if (credProvider != null) {
                client.setCredentialsProvider(credProvider);
            }
        } catch (Exception e) {
            hasLastError = true;
            lastError = "Error creating HTTP-Connection: " + e.getMessage() + " [ " + e.getCause() + " ]";
            return null;
        }
        
        HttpResponse response = null;
        InputStream instream = null;
        
        try {
            response = client.execute(post);
        } catch (ClientProtocolException e) {
            hasLastError = true;
            lastError = e.getMessage()
                    + ", Method: doRequest(String url) threw ClientProtocolException on httpclient.execute(httpPost)"
                    + " [ " + e.getCause() + " ]";
            return null;
        } catch (IOException e) {
            /*
             * Occurs on timeout of the connection. Would be better to catch the specialized exception for that
             * case but which is it? The Reference (http://developer.android.com/reference/java/io/IOException.html)
             * lists lots of subclasses.
             */
            return null;
        }
        
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                instream = entity.getContent();
            }
        } catch (IOException e) {
            hasLastError = true;
            lastError = e.getMessage() + ", Method: doRequest(String url) threw IOException on entity.getContent()"
                    + " [ " + e.getCause() + " ]";
            return null;
        }
        
        String strResponse = "";
        long length = -1;
        try {
            strResponse = StringSupport.convertStreamToString(instream);
        } catch (IOException e) {
            hasLastError = true;
            lastError = "JSON-Data could not be parsed. Exception: " + e.getMessage() + " (" + e.getCause() + ")";
            Log.w(Utils.TAG, lastError);
            return null;
        } catch (OutOfMemoryError e2) {
            hasLastError = true;
            lastError = "Run out of memory when trying to fetch " + (length > 0 ? length + "" : "an unknown amount of")
                    + " bytes from " + url;
            Log.w(Utils.TAG, lastError);
            return null;
        }
        
        if (strResponse.contains(NOT_LOGGED_IN) && firstCall) {
            Log.w(Utils.TAG, "Not logged in, retrying...");
            hasLastError = true;
            lastError = NOT_LOGGED_IN;
            
            // Login and post request again
            String tempSessionId = (sessionId != null ? new String(sessionId) : "__TEST__");
            
            login();
            // TODO: Verify behavior, login() was removed with revision 4827ca8f7edaa449ca637134f62f212b03bae6c8 See:
            // https://code.google.com/p/ttrss-reader-fork/source/browse/ttrss-reader/src/org/ttrssreader/net/TTRSSJsonConnector.java?r=4827ca8f7edaa449ca637134f62f212b03bae6c8
            
            if (url.contains(tempSessionId)) {
                url = url.replace(tempSessionId, sessionId);
            }
            strResponse = doRequest(url, false);
            if (strResponse == null)
                strResponse = "";
        }
        
        // Check if API is enabled for the user
        if (strResponse.contains(API_DISABLED)) {
            Log.w(Utils.TAG, String.format(API_DISABLED_MESSAGE, Controller.getInstance().username()));
            hasLastError = true;
            lastError = String.format(API_DISABLED_MESSAGE, Controller.getInstance().username());
            return null;
        }
        
        // Check returned string for error-messages
        if (strResponse.contains(ERROR)) {
            hasLastError = true;
            lastError = strResponse;
            return null;
        }
        
        // Parse new output with sequence-number and status-codes
        if (strResponse.startsWith("{\"seq"))
            strResponse = parseMetadata(strResponse);
        
        // Log.v(Utils.TAG, String.format("PARSING    %s ms ( %s )", System.currentTimeMillis() - tempTime - start,
        // tempUrl));
        return strResponse;
    }
    
    private String doRequestNoAnswer(String url) {
        // Make sure we are logged in
        if (sessionId == null || lastError.equals(NOT_LOGGED_IN))
            if (!login())
                return null;
        if (hasLastError)
            return null;
        
        if (!url.contains(sidUrl))
            url += sidUrl;
        
        String ret = doRequest(url, true); // Append Session-ID to all calls except login
        
        if (hasLastError || ret == null) {
            hasLastError = false;
            lastError = "";
            ret = "";
        }
        
        return ret;
    }
    
    private JSONArray getJSONResponseAsArray(String url) {
        // Make sure we are logged in
        if (sessionId == null || lastError.equals(NOT_LOGGED_IN)) {
            if (!login())
                return null;
        }
        if (hasLastError)
            return null;
        
        if (!url.contains(sidUrl))
            url += sidUrl;
        
        String strResponse = doRequest(url, true); // Append Session-ID to all calls except login
        
        if (hasLastError || strResponse == null)
            return null;
        
        JSONArray result = null;
        if (strResponse != null && strResponse.length() > 0) {
            try {
                result = new JSONArray(strResponse);
            } catch (Exception e) {
                try {
                    result = new JSONArray("[" + strResponse + "]");
                } catch (Exception e1) {
                    hasLastError = true;
                    lastError = "An Error occurred. Message from Server: " + strResponse;
                }
            }
        }
        return result;
    }
    
    private JSONResult getJSONLoginResponse(String url) {
        // No check with assertLogin here, we are about to login so no need for this.
        String strResponse = doRequest(url, true);
        
        if (strResponse == null)
            return null;
        
        JSONResult result = null;
        if (!hasLastError) {
            try {
                result = new JSONResult(strResponse);
            } catch (Exception e) {
                hasLastError = true;
                lastError = "An Error occurred. Message from Server: " + strResponse;
            }
        }
        
        return result;
    }
    
    private boolean login() {
        // Just login once, check if already logged in after acquiring the lock on mSessionId
        synchronized (loginLock) {
            if (sessionId != null && !(lastError.equals(NOT_LOGGED_IN))) {
                return true; // Login done while we were waiting for the lock
            }
            
            sessionId = null;
            
            String url = Controller.getInstance().url() + String.format(OP_LOGIN, Controller.getInstance().username(), Controller.getInstance().password());
            JSONResult jsonResult = getJSONLoginResponse(url);
            
            if (!hasLastError && jsonResult != null) {
                
                int i = 0;
                try {
                    
                    while (i < jsonResult.getNames().length()) {
                        if (jsonResult.getNames().getString(i).equals(SESSION_ID)) {
                            sessionId = jsonResult.getValues().getString(i);
                            sidUrl = String.format(SID_APPEND, sessionId);
                            return true;
                        }
                        i++;
                    }
                } catch (Exception e) {
                    hasLastError = true;
                    lastError = e.getMessage() + ", Method: login(String url) threw Exception";
                    e.printStackTrace();
                }
                
            }
            
            // Try again with base64-encoded passphrase
            if (!loginBase64() && !hasLastError) {
                hasLastError = true;
                lastError = "Couldn't login, please check your settings.";
                return false;
            } else {
                return true;
            }
        }
    }
    
    private boolean loginBase64() {
        Log.d(Utils.TAG, "login() didn't work, trying loginBase64()...");
        sessionId = null;
        
        byte[] bytes = Controller.getInstance().password().getBytes();
        String mPasswordEncoded = Base64.encodeBytes(bytes);
        
        String url = Controller.getInstance().url() + String.format(OP_LOGIN, Controller.getInstance().username(), mPasswordEncoded);
        JSONResult jsonResult = getJSONLoginResponse(url);
        
        if (!hasLastError && jsonResult != null) {
            int i = 0;
            
            try {
                while (i < jsonResult.getNames().length()) {
                    if (jsonResult.getNames().getString(i).equals(SESSION_ID)) {
                        sessionId = jsonResult.getValues().getString(i);
                        sidUrl = String.format(SID_APPEND, sessionId);
                        return true;
                    } else {
                        i++;
                    }
                }
            } catch (Exception e) {
                hasLastError = true;
                lastError = e.getMessage() + ", Method: login(String url) threw Exception";
                e.printStackTrace();
            }
        }
        return false;
    }
    
    private String parseMetadata(String str) {
        // Cut string from content: to the end: /{"seq":0,"status":0,"content":(.*)}/\1/
        String pattern = "\"content\":";
        int start = str.indexOf(pattern) + pattern.length();
        int stop = str.length() - 1;
        if (start >= pattern.length() && stop > start) {
            return str.substring(start, stop);
        }
        
        return "";
    }
    
    // ***************** Helper-Methods **************************************************
    
    private static Set<String> parseDataForAttachments(JSONArray array) {
        Set<String> ret = new LinkedHashSet<String>();
        
        try {
            for (int j = 0; j < array.length(); j++) {
                
                JSONResult att = new JSONResult(array.getString(j));
                JSONArray names = att.getNames();
                JSONArray values = att.getValues();
                
                String attId = null;
                String attUrl = null;
                
                // Filter for id and content_url, other fields are not necessary
                for (int k = 0; k < names.length(); k++) {
                    String s = names.getString(k);
                    if (s.equals("id")) {
                        attId = values.getString(k);
                    } else if (s.equals("content_url")) {
                        attUrl = values.getString(k);
                    }
                }
                
                // Add only if both, id and url, are found
                if (attId != null && attUrl != null) {
                    ret.add(attUrl);
                }
            }
        } catch (JSONException je) {
            je.printStackTrace();
        }
        
        return ret;
    }
    
    private Set<Integer> parseArticlesAndInsertInDB(JSONArray jsonResult) {
        Set<Integer> ret = new LinkedHashSet<Integer>();
        
        SQLiteDatabase db = DBHelper.getInstance().db;
        synchronized (DBHelper.TABLE_ARTICLES) {
            db.beginTransaction();
            try {
                
                for (int j = 0; j < jsonResult.length(); j++) {
                    JSONObject object = jsonResult.getJSONObject(j);
                    JSONArray names = object.names();
                    JSONArray values = object.toJSONArray(names);
                    
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
                    
                    for (int i = 0; i < names.length(); i++) {
                        String s = names.getString(i);
                        if (s.equals(ID)) {
                            id = values.getInt(i);
                        } else if (s.equals(TITLE)) {
                            title = values.getString(i);
                        } else if (s.equals(UNREAD)) {
                            isUnread = values.getBoolean(i);
                        } else if (s.equals(UPDATED)) {
                            updated = new Date(new Long(values.getString(i) + "000").longValue());
                        } else if (s.equals(FEED_ID)) {
                            realFeedId = values.getInt(i);
                        } else if (s.equals(CONTENT)) {
                            content = values.getString(i);
                        } else if (s.equals(URL)) {
                            articleUrl = values.getString(i);
                        } else if (s.equals(COMMENT_URL)) {
                            articleCommentUrl = values.getString(i);
                        } else if (s.equals(ATTACHMENTS)) {
                            attachments = parseDataForAttachments((JSONArray) values.get(i));
                        } else if (s.equals(STARRED)) {
                            isStarred = values.getBoolean(i);
                        } else if (s.equals(PUBLISHED)) {
                            isPublished = values.getBoolean(i);
                        }
                    }
                    
                    DBHelper.getInstance().insertArticle(id, realFeedId, title, isUnread, articleUrl,
                            articleCommentUrl, updated, content, attachments, isStarred, isPublished);
                    ret.add(id);
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                hasLastError = true;
                lastError = e.getMessage() + ", Method: parseArticlesAndInsertInDB(...) threw Exception";
                e.printStackTrace();
            } finally {
                db.endTransaction();
                DBHelper.getInstance().purgeArticlesNumber();
            }
        }
        
        return ret;
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
        String url = Controller.getInstance().url() + String.format(OP_GET_COUNTERS);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null)
            return;
        
        try {
            for (int i = 0; i < jsonResult.length(); i++) {
                JSONObject object = jsonResult.getJSONObject(i);
                
                JSONArray names = object.names();
                JSONArray values = object.toJSONArray(names);
                
                // Ignore "updated" and "description", we don't need it...
                boolean cat = false;
                int id = 0;
                int counter = 0;
                
                for (int j = 0; j < names.length(); j++) {
                    if (names.getString(j).equals(COUNTER_KIND)) {
                        cat = values.getString(j).equals(COUNTER_CAT);
                    } else if (names.getString(j).equals(COUNTER_ID)) {
                        // Check if id is a string, then it would be a global counter
                        if (values.getString(j).equals("global-unread")
                                || values.getString(j).equals("subscribed-feeds")) {
                            continue;
                        } else {
                            id = values.getInt(j);
                        }
                    } else if (names.getString(j).equals(COUNTER_COUNTER)) {
                        counter = values.getInt(j);
                    }
                }
                
                if (cat && id >= 0) { // Category
                    DBHelper.getInstance().updateCategoryUnreadCount(id, counter);
                } else if (!cat && id < 0) { // Virtual Category
                    DBHelper.getInstance().updateCategoryUnreadCount(id, counter);
                } else if (!cat && id > 0) { // Feed
                    DBHelper.getInstance().updateFeedUnreadCount(id, counter);
                }
                
            }
        } catch (Exception e) {
            hasLastError = true;
            lastError = e.getMessage() + ", Method: getCounters() threw Exception";
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
        Set<Category> ret = new LinkedHashSet<Category>();
        
        String url = Controller.getInstance().url() + String.format(OP_GET_CATEGORIES);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null)
            return ret;
        
        try {
            for (int i = 0; i < jsonResult.length(); i++) {
                JSONObject object = jsonResult.getJSONObject(i);
                JSONArray names = object.names();
                JSONArray values = object.toJSONArray(names);
                
                int id = 0;
                String title = "";
                int unread = 0;
                
                for (int j = 0; j < names.length(); j++) {
                    String s = names.getString(j);
                    if (s.equals(ID)) {
                        id = values.getInt(j);
                    } else if (s.equals(TITLE)) {
                        title = values.getString(j);
                    } else if (s.equals(UNREAD)) {
                        unread = values.getInt(j);
                    }
                }
                
                ret.add(new Category(id, title, unread));
            }
        } catch (Exception e) {
            hasLastError = true;
            lastError = e.getMessage() + ", Method: getCategories() threw Exception";
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
        Set<Feed> ret = new LinkedHashSet<Feed>();;
        
        // Hardcoded -4 fetches all feeds. See http://tt-rss.org/redmine/wiki/tt-rss/JsonApiReference#getFeeds
        String url = Controller.getInstance().url() + String.format(OP_GET_FEEDS, "-4");
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null)
            return ret;
        
        try {
            for (int i = 0; i < jsonResult.length(); i++) {
                JSONObject object = jsonResult.getJSONObject(i);
                JSONArray names = object.names();
                JSONArray values = object.toJSONArray(names);
                
                int categoryId = 0;
                int id = 0;
                String title = "";
                String feedUrl = "";
                int unread = 0;
                
                for (int j = 0; j < names.length(); j++) {
                    String s = names.getString(j);
                    if (s.equals(ID)) {
                        id = values.getInt(j);
                    } else if (s.equals(CAT_ID)) {
                        categoryId = values.getInt(j);
                    } else if (s.equals(TITLE)) {
                        title = values.getString(j);
                    } else if (s.equals(FEED_URL)) {
                        feedUrl = values.getString(j);
                    } else if (s.equals(UNREAD)) {
                        unread = values.getInt(j);
                    }
                }
                
                if (id > 0)
                    ret.add(new Feed(id, categoryId, title, feedUrl, unread));
            }
        } catch (Exception e) {
            hasLastError = true;
            lastError = e.getMessage() + ", Method: getSubsribedFeeds() threw Exception";
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
        
        for (String idList : StringSupport.convertListToString(ids)) {
            if (idList.length() == 0)
                continue;
            
            String url = Controller.getInstance().url() + String.format(OP_GET_ARTICLE, idList);
            JSONArray jsonResult = getJSONResponseAsArray(url);
            
            if (jsonResult == null)
                continue;
            
            parseArticlesAndInsertInDB(jsonResult);
        }
        Log.v(Utils.TAG, "getArticle: " + (System.currentTimeMillis() - time) + "ms");
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#getHeadlinesToDatabase(Integer, int, int, java.lang.String)
     */
    @Override
    public Set<Integer> getHeadlinesToDatabase(Integer feedId, int limit, String viewMode, boolean isCategory) {
        long time = System.currentTimeMillis();
        String url = Controller.getInstance().url() + String.format(OP_GET_FEEDHEADLINES, feedId, limit, viewMode, isCategory);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null)
            return null;
        
        if (feedId == -1 && isCategory)
            DBHelper.getInstance().purgeStarredArticles();
        
        if (feedId == -2 && isCategory)
            DBHelper.getInstance().purgePublishedArticles();
        
        // Check if viewmode=unread and feedId>=0 so we can safely mark all other articles as read
        // TODO: Store list of marked articles so we can restore the state if parseArticlesAndInsertInDB() fails?
        if (viewMode.equals("unread") && feedId >= 0)
            DBHelper.getInstance().markFeedOnlyArticlesRead(feedId);
        
        Set<Integer> ret = parseArticlesAndInsertInDB(jsonResult);
        Log.v(Utils.TAG, "getHeadlinesToDatabase: " + (System.currentTimeMillis() - time) + "ms");
        return ret;
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
            String url = Controller.getInstance().url() + String.format(OP_UPDATE_ARTICLE, idList, articleState, 2);
            ret = doRequestNoAnswer(url);
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
            String url = Controller.getInstance().url() + String.format(OP_UPDATE_ARTICLE, idList, articleState, 0);
            ret = doRequestNoAnswer(url);
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
            String url = Controller.getInstance().url() + String.format(OP_UPDATE_ARTICLE, idList, articleState, 1);
            ret = doRequestNoAnswer(url);
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
        String url = Controller.getInstance().url() + String.format(OP_CATCHUP, id, (isCategory ? 1 : 0));
        String ret = doRequestNoAnswer(url);
        return (ret != null && ret.contains(OK));
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#getPref(java.lang.String)
     */
    @Override
    public String getPref(String pref) {
        String url = Controller.getInstance().url() + String.format(OP_GET_PREF, pref);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null)
            return null;
        
        try {
            for (int i = 0; i < jsonResult.length(); i++) {
                JSONObject object = jsonResult.getJSONObject(i);
                JSONArray names = object.names();
                JSONArray values = object.toJSONArray(names);
                
                for (int j = 0; j < names.length(); j++) {
                    String s = names.getString(j);
                    if (s.equals(VALUE)) {
                        return values.getString(j);
                    }
                }
            }
        } catch (Exception e) {
            hasLastError = true;
            lastError = e.getMessage() + ", Method: getPref() threw Exception";
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
        String url = Controller.getInstance().url() + OP_GET_VERSION;
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null)
            return -1;
        
        String ret = "";
        try {
            for (int i = 0; i < jsonResult.length(); i++) {
                JSONObject object = jsonResult.getJSONObject(i);
                JSONArray names = object.names();
                JSONArray values = object.toJSONArray(names);
                
                for (int j = 0; j < names.length(); j++) {
                    String s = names.getString(j);
                    if (s.equals(VERSION)) {
                        ret = values.getString(j);
                    }
                }
            }
        } catch (Exception e) {
            hasLastError = true;
            lastError = e.getMessage() + ", Method: getVersion() threw Exception";
            e.printStackTrace();
        }
        
        try {
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
