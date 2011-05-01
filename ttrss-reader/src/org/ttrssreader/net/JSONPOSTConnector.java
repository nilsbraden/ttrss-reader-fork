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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
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

public class JSONPOSTConnector implements Connector {
    
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
    
    private static final String COUNTER_KIND = "kind";
    private static final String COUNTER_CAT = "cat";
    private static final String COUNTER_ID = "id";
    private static final String COUNTER_COUNTER = "counter";
    
    private URI serverUrl;
    private String userName;
    private String password;
    private String httpUserName;
    private String httpPassword;
    
    private String sessionId;
    private String loginLock = "";
    
    private CredentialsProvider credProvider = null;
    
    // HTTP-Stuff
    HttpPost post;
    DefaultHttpClient client;
    
    public JSONPOSTConnector(String serverUrl, String userName, String password, String httpUser, String httpPw) {
        try {
            this.serverUrl = new URI(serverUrl);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        this.userName = userName;
        this.password = password;
        this.sessionId = null;
        this.httpUserName = httpUser;
        this.httpPassword = httpPw;
        
        if (!httpUserName.equals(Constants.EMPTY) && !httpPassword.equals(Constants.EMPTY)) {
            
            this.credProvider = new BasicCredentialsProvider();
            this.credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(httpUserName, httpPassword));
            
        }
        
        post = new HttpPost();
    }
    
    private String doRequest(List<NameValuePair> nameValuePairs, boolean firstCall) {
        // long start = System.currentTimeMillis();
        
        // Build Log-Output
        StringBuilder paramString = new StringBuilder();
        for (NameValuePair nvp : nameValuePairs) {
            if (!SESSION_ID.equals(nvp.getName()) && !PARAM_PW.equals(nvp.getName()))
                paramString.append("(" + nvp.getName() + ": " + nvp.getValue() + "),");
        }
        paramString.deleteCharAt(paramString.lastIndexOf(","));
        Log.v(Utils.TAG, "Request: " + paramString.toString());
        
        try {
            // Set Address
            post.setURI(serverUrl);
            
            // Add POST data
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            
            HttpParams params = post.getParams();
            
            if (client == null) {
                client = HttpClientFactory.getInstance().getHttpClient(params);
            } else {
                client.setParams(params);
            }
            
            // Add SSL-Stuff
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
            // Execute the request
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
        
        // Begin: Log-output
        // String tempUrl = new String(url);
        // if (url.contains("&password="))
        // tempUrl = tempUrl.substring(0, tempUrl.length() - password.length()) + "*";
        //
        // long tempTime = System.currentTimeMillis() - start;
        // Log.v(Utils.TAG, String.format("REQUESTING %s ms ( %s )", tempTime, tempUrl));
        // End: Log-output
        
        String strResponse;
        try {
            
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                instream = entity.getContent();
            }
            
            if (instream == null) {
                hasLastError = true;
                lastError = "Couldn't get InputStream in Method doRequest(String url) [instream was null]";
                return null;
            }
            
            strResponse = StringSupport.convertStreamToString(instream);
            
        } catch (IOException e) {
            hasLastError = true;
            lastError = "JSON-Data could not be parsed. Exception: " + e.getMessage() + " (" + e.getCause() + ")";
            Log.w(Utils.TAG, lastError);
            return null;
        }
        
        if (strResponse.contains(NOT_LOGGED_IN) && firstCall) {
            
            Log.w(Utils.TAG, "Not logged in, retrying...");
            hasLastError = true;
            lastError = NOT_LOGGED_IN;
            
            // Login and post request again
            List<NameValuePair> newPostData = new ArrayList<NameValuePair>();
            for (NameValuePair nvp : nameValuePairs) {
                if (nvp.getName().equals(SESSION_ID)) {
                    newPostData.add(new BasicNameValuePair(SESSION_ID, new String(sessionId)));
                } else {
                    newPostData.add(nvp);
                }
            }
            strResponse = doRequest(newPostData, false);
            if (strResponse == null)
                strResponse = "";
            
        }
        
        // Check if API is enabled for the user
        if (strResponse.contains(API_DISABLED)) {
            Log.w(Utils.TAG, String.format(API_DISABLED_MESSAGE, userName));
            
            hasLastError = true;
            lastError = String.format(API_DISABLED_MESSAGE, userName);
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
    
    private String doRequestNoAnswer(List<NameValuePair> nvpList) {
        // Make sure we are logged in
        if (sessionId == null || lastError.equals(NOT_LOGGED_IN))
            if (!login())
                return null;
        if (hasLastError)
            return null;
        
        // Add Session-ID
        nvpList.add(new BasicNameValuePair(SESSION_ID, sessionId));
        
        String ret = doRequest(nvpList, true); // Append Session-ID to all calls except login
        
        if (hasLastError || ret == null) {
            hasLastError = false;
            lastError = "";
            ret = "";
        }
        
        return ret;
    }
    
    private JSONArray getJSONResponseAsArray(List<NameValuePair> nvpList) {
        // Make sure we are logged in
        if (sessionId == null || lastError.equals(NOT_LOGGED_IN)) {
            if (!login())
                return null;
        }
        if (hasLastError)
            return null;
        
        // Add Session-ID
        nvpList.add(new BasicNameValuePair(SESSION_ID, sessionId));
        
        String strResponse = doRequest(nvpList, true); // Append Session-ID to all calls except login
        
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
    
    private JSONResult getJSONLoginResponse(List<NameValuePair> nvpList) {
        // No check with assertLogin here, we are about to login so no need for this.
        String strResponse = doRequest(nvpList, true);
        
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
            
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(PARAM_OP, VALUE_LOGIN));
            params.add(new BasicNameValuePair(PARAM_USER, userName));
            params.add(new BasicNameValuePair(PARAM_PW, password));
            
            JSONResult jsonResult = getJSONLoginResponse(params);
            
            if (!hasLastError && jsonResult != null) {
                
                int i = 0;
                try {
                    
                    while (i < jsonResult.getNames().length()) {
                        if (jsonResult.getNames().getString(i).equals(SESSION_ID)) {
                            sessionId = jsonResult.getValues().getString(i);
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
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_OP, VALUE_LOGIN));
        params.add(new BasicNameValuePair(PARAM_USER, userName));
        params.add(new BasicNameValuePair(PARAM_PW, Base64.encodeBytes(password.getBytes())));
        
        JSONResult jsonResult = getJSONLoginResponse(params);
        
        if (!hasLastError && jsonResult != null) {
            int i = 0;
            
            try {
                while (i < jsonResult.getNames().length()) {
                    if (jsonResult.getNames().getString(i).equals(SESSION_ID)) {
                        sessionId = jsonResult.getValues().getString(i);
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
                DBHelper.getInstance().purgeArticlesNumber(Controller.getInstance().getArticleLimit());
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
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_OP, VALUE_GET_COUNTERS));
        params.add(new BasicNameValuePair(PARAM_OUTPUT_MODE, "fc"));
        JSONArray jsonResult = getJSONResponseAsArray(params);
        
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
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_OP, VALUE_GET_CATEGORIES));
        JSONArray jsonResult = getJSONResponseAsArray(params);
        
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
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_OP, VALUE_GET_FEEDS));
        params.add(new BasicNameValuePair(PARAM_CAT_ID, "-4")); // Hardcoded -4 fetches all feeds. See
                                                                // http://tt-rss.org/redmine/wiki/tt-rss/JsonApiReference#getFeeds
        JSONArray jsonResult = getJSONResponseAsArray(params);
        
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
            
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(PARAM_OP, VALUE_GET_ARTICLE));
            params.add(new BasicNameValuePair(PARAM_ARTICLE_ID, idList));
            JSONArray jsonResult = getJSONResponseAsArray(params);
            
            if (jsonResult == null)
                continue;
            
            parseArticlesAndInsertInDB(jsonResult);
        }
        Log.v(Utils.TAG, "getArticle: " + (System.currentTimeMillis() - time) + "ms");
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.ttrssreader.net.Connector#getHeadlinesToDatabase(java.lang.Integer, int, int, java.lang.String)
     */
    @Override
    public Set<Integer> getHeadlinesToDatabase(Integer feedId, int limit, String viewMode, boolean isCategory) {
        long time = System.currentTimeMillis();
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_OP, VALUE_GET_HEADLINES));
        params.add(new BasicNameValuePair(PARAM_FEED_ID, feedId + ""));
        params.add(new BasicNameValuePair(PARAM_LIMIT, limit + ""));
        params.add(new BasicNameValuePair(PARAM_VIEWMODE, viewMode));
        params.add(new BasicNameValuePair(PARAM_SHOW_CONTENT, "1"));
        params.add(new BasicNameValuePair(PARAM_INC_ATTACHMENTS, "1"));
        params.add(new BasicNameValuePair(PARAM_IS_CAT, (isCategory ? "1" : "0")));
        
        JSONArray jsonResult = getJSONResponseAsArray(params);
        
        if (jsonResult == null)
            return null;
        
        if (feedId == -1 && isCategory)
            DBHelper.getInstance().purgeStarredArticles();
        
        if (feedId == -2 && isCategory)
            DBHelper.getInstance().purgePublishedArticles();
        
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
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(PARAM_OP, VALUE_UPDATE_ARTICLE));
            params.add(new BasicNameValuePair(PARAM_ARTICLE_IDS, idList));
            params.add(new BasicNameValuePair(PARAM_MODE, articleState + ""));
            params.add(new BasicNameValuePair(PARAM_FIELD, "2"));
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
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(PARAM_OP, VALUE_UPDATE_ARTICLE));
            params.add(new BasicNameValuePair(PARAM_ARTICLE_IDS, idList));
            params.add(new BasicNameValuePair(PARAM_MODE, articleState + ""));
            params.add(new BasicNameValuePair(PARAM_FIELD, "0"));
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
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(PARAM_OP, VALUE_UPDATE_ARTICLE));
            params.add(new BasicNameValuePair(PARAM_ARTICLE_IDS, idList));
            params.add(new BasicNameValuePair(PARAM_MODE, articleState + ""));
            params.add(new BasicNameValuePair(PARAM_FIELD, "1"));
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
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_OP, VALUE_CATCHUP));
        params.add(new BasicNameValuePair(PARAM_FEED_ID, id + ""));
        params.add(new BasicNameValuePair(PARAM_IS_CAT, (isCategory ? "1" : "0")));
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
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_OP, VALUE_GET_PREF));
        params.add(new BasicNameValuePair(PARAM_PREF, pref));
        JSONArray jsonResult = getJSONResponseAsArray(params);
        
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
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_OP, VALUE_GET_VERSION));
        JSONArray jsonResult = getJSONResponseAsArray(params);
        
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
    
    /**
     * Returns true if there was an error.
     * 
     * @return true if there was an error.
     */
    public static boolean hasLastError() {
        return hasLastError;
    }
    
    /**
     * Returns the last error-message and resets the error-state of the connector.
     * 
     * @return a string with the last error-message.
     */
    public static String pullLastError() {
        String ret = new String(lastError);
        lastError = "";
        hasLastError = false;
        return ret;
    }
    
}
