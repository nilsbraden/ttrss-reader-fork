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
import org.ttrssreader.model.pojos.CategoryItem;
import org.ttrssreader.model.pojos.FeedItem;
import org.ttrssreader.utils.Base64;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TTRSSJsonConnector {
    
    private static String mLastError = "";
    private static boolean mHasLastError = false;
    
    private static final String OP_LOGIN = "?op=login&user=%s&password=%s";
    private static final String OP_GET_CATEGORIES = "?op=getCategories";
    private static final String OP_GET_FEEDS = "?op=getFeeds&cat_id=%s";
    private static final String OP_GET_ARTICLE = "?op=getArticle&article_id=%s";
    private static final String OP_GET_FEEDHEADLINES = "?op=getHeadlines&feed_id=%s&limit=%s&view_mode=%s&show_content=%s";
    private static final String OP_UPDATE_ARTICLE = "?op=updateArticle&article_ids=%s&mode=%s&field=%s";
    private static final String OP_CATCHUP = "?op=catchupFeed&feed_id=%s&is_cat=%s";
    private static final String OP_GET_PREF = "?op=getPref&pref_name=%s";
    private static final String OP_GET_COUNTERS = "?op=getCounters&output_mode=fc"; // output_mode (default: flc) - what
                                                                                    // kind of information to return
                                                                                    // (f-feeds, l-labels, c-categories,
                                                                                    // t-tags)
    
    private static final String PASSWORD_MATCH = "&password=";
    private static final String ERROR = "{\"error\":";
    private static final String NOT_LOGGED_IN = ERROR + "\"NOT_LOGGED_IN\"}";
    private static final String UNKNOWN_METHOD = ERROR + "\"UNKNOWN_METHOD\"}";
    private static final String API_DISABLED = ERROR + "\"API_DISABLED\"}";
    private static final String API_DISABLED_MESSAGE = "Please enable API for this user in its preferences on the Server. (User: %s, URL: %s)";
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
    private static final String SID_APPEND = "&sid=%s";
    
    private static final String COUNTER_KIND = "kind";
    private static final String COUNTER_CAT = "cat";
    private static final String COUNTER_ID = "id";
    private static final String COUNTER_COUNTER = "counter";
    
    private String mServerUrl;
    private String mUserName;
    private String mPassword;
    private String httpUserName;
    private String httpPassword;
    
    private String mSessionId;
    private String sidUrl;
    private String loginLock = "";
    
    private CredentialsProvider credProvider;
    
    public TTRSSJsonConnector(String serverUrl, String userName, String password, String httpUser, String httpPw) {
        mServerUrl = serverUrl;
        mUserName = userName;
        mPassword = password;
        mSessionId = null;
        httpUserName = httpUser;
        httpPassword = httpPw;
        
        credProvider = new BasicCredentialsProvider();
        credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(httpUserName, httpPassword));
    }
    
    private String doRequest(String url, boolean firstCall) {
        long start = System.currentTimeMillis();
        String strResponse = null;
        
        HttpPost httpPost;
        HttpParams httpParams;
        DefaultHttpClient httpclient;
        try {
            httpPost = new HttpPost(url);
            httpParams = httpPost.getParams();
            httpclient = HttpClientFactory.createInstance(httpParams);
            httpclient.setCredentialsProvider(credProvider);
        } catch (Exception e) {
            mHasLastError = true;
            mLastError = "Error creating HTTP-Connection: " + e.getMessage();
            return null;
        }
        
        try {
            HttpResponse response = httpclient.execute(httpPost);
            
            // Begin: Log-output
            String tUrl = new String(url);
            if (url.contains(PASSWORD_MATCH))
                tUrl = tUrl.substring(0, tUrl.length() - mPassword.length()) + "*";
            
            Log.d(Utils.TAG, String.format("Requesting URL: %s (took %s ms)", tUrl, System.currentTimeMillis() - start));
            // End: Log-output
            
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                strResponse = Utils.convertStreamToString(instream); // TODO: See if this can be and/or needs to be
                                                                     // optimized.
                
                if (strResponse.contains(NOT_LOGGED_IN) && firstCall) {
                    Log.w(Utils.TAG, "Not logged in, retrying...");
                    mHasLastError = true;
                    mLastError = NOT_LOGGED_IN;
                    
                    // Login and post request again
                    String tempSessionId = new String(mSessionId);
                    if (url.contains(tempSessionId)) {
                        url = url.replace(tempSessionId, mSessionId);
                    }
                    strResponse = doRequest(url, false);
                }
                
                // Check if API is enabled for the user
                if (strResponse.contains(API_DISABLED)) {
                    Log.w(Utils.TAG, String.format(API_DISABLED_MESSAGE, mUserName, mServerUrl));
                    mHasLastError = true;
                    mLastError = String.format(API_DISABLED_MESSAGE, mUserName, mServerUrl);
                    return null;
                }
                
                // Check returned string for error-messages
                if (strResponse.startsWith(ERROR)) {
                    mHasLastError = true;
                    mLastError = strResponse;
                    return null;
                }
            }
        } catch (ClientProtocolException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: doRequest(String url) threw ClientProtocolException";
            return null;
        } catch (IOException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: doRequest(String url) threw IOException";
            return null;
        }
        
        // Parse new output with sequence-number and status-codes
        if (strResponse.contains("{\"seq\":"))
            strResponse = parseMetadata(strResponse);
        
        return strResponse;
    }
    
    private String doRequestNoAnswer(String url) {
        // Make sure we are logged in
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN))
            login();
        if (mHasLastError)
            return null;
        
        mHasLastError = false;
        mLastError = "";
        
        if (!url.contains(sidUrl))
            url += sidUrl;
        
        String ret = doRequest(url, true); // Append Session-ID to all calls except login
        
        if (mHasLastError) {
            mHasLastError = false;
            mLastError = "";
            ret = "";
        }
        
        return ret;
    }
    
    private JSONArray getJSONResponseAsArray(String url) {
        // Make sure we are logged in
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN))
            login();
        if (mHasLastError)
            return null;
        
        mHasLastError = false;
        mLastError = "";
        
        if (!url.contains(sidUrl))
            url += sidUrl;
        
        String strResponse = doRequest(url, true); // Append Session-ID to all calls except login
        
        if (mHasLastError)
            return null;
        
        JSONArray result = null;
        if (strResponse != null && strResponse.length() > 0) {
            try {
                result = new JSONArray(strResponse);
            } catch (JSONException e) {
                mHasLastError = true;
                mLastError = "An Error occurred. Message from Server: " + strResponse;
            }
        }
        return result;
    }
    
    private TTRSSJsonResult getJSONLoginResponse(String url) {
        // No check with assertLogin here, we are about to login so no need for this.
        mHasLastError = false;
        mLastError = "";
        
        TTRSSJsonResult result = null;
        String strResponse = doRequest(url, true);
        
        if (!mHasLastError) {
            try {
                result = new TTRSSJsonResult(strResponse);
            } catch (JSONException e) {
                mHasLastError = true;
                mLastError = "An Error occurred. Message from Server: " + strResponse;
            }
        }
        
        return result;
    }
    
    private boolean login() {
        // Just login once, check if already logged in after acquiring the lock on mSessionId
        synchronized (loginLock) {
            if (mSessionId != null && !(mLastError.equals(NOT_LOGGED_IN))) {
                return true;
            }
            
            boolean result = true;
            mSessionId = null;
            
            String url = mServerUrl + String.format(OP_LOGIN, mUserName, mPassword);
            TTRSSJsonResult jsonResult = getJSONLoginResponse(url);
            
            if (!mHasLastError || jsonResult != null) {
                
                int i = 0;
                try {
                    
                    while ((i < jsonResult.getNames().length())) {
                        if (jsonResult.getNames().getString(i).equals(SESSION_ID)) {
                            mSessionId = jsonResult.getValues().getString(i);
                            sidUrl = String.format(SID_APPEND, mSessionId);
                            break;
                        }
                        i++;
                    }
                } catch (JSONException e) {
                    result = false;
                    mHasLastError = true;
                    mLastError = e.getMessage() + ", Method: login(String url) threw JSONException";
                    e.printStackTrace();
                }
                
            } else {
                result = false;
            }
            
            if (result == false) {
                mHasLastError = false;
                mLastError = "";
                
                // Try again with base64-encoded passphrase
                result = loginBase64();
            }
            
            return result;
        }
    }
    
    private boolean loginBase64() {
        Log.d(Utils.TAG, "login() didn't work, trying loginBase64()...");
        mSessionId = null;
        
        byte[] bytes = mPassword.getBytes();
        String mPasswordEncoded = Base64.encodeBytes(bytes);
        
        String url = mServerUrl + String.format(OP_LOGIN, mUserName, mPasswordEncoded);
        TTRSSJsonResult jsonResult = getJSONLoginResponse(url);
        
        if (jsonResult == null)
            return false;
        
        if (!mHasLastError) {
            int i = 0;
            boolean stop = false;
            
            try {
                while ((i < jsonResult.getNames().length()) && (!stop)) {
                    if (jsonResult.getNames().getString(i).equals(SESSION_ID)) {
                        stop = true;
                        mSessionId = jsonResult.getValues().getString(i);
                        sidUrl = String.format(SID_APPEND, mSessionId);
                    } else {
                        i++;
                    }
                }
            } catch (JSONException e) {
                mHasLastError = true;
                mLastError = e.getMessage() + ", Method: login(String url) threw JSONException";
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }
    
    private String parseMetadata(String str) {
        // Cut string from content: to the end: /{"seq":0,"status":0,"content":(.*)}/\1/
        String pattern = "\"content\":";
        int start = str.indexOf(pattern) + pattern.length();
        int stop = str.length() - 2;
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
                
                TTRSSJsonResult att = new TTRSSJsonResult(array.getString(j));
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
            } catch (JSONException e) {
                mHasLastError = true;
                mLastError = e.getMessage() + ", Method: parseArticlesAndInsertInDB(...) threw JSONException";
                e.printStackTrace();
            } finally {
                db.endTransaction();
                DBHelper.getInstance().purgeArticlesNumber(Controller.getInstance().getArticleLimit());
            }
        }
        
        return ret;
    }
    
    // ***************** Retrieve-Data-Methods **************************************************
    
    /**
     * Retrieves a Set of Maps which map Strings to the information, e.g. "id" -> 42, containing the counters for every
     * category and feed.
     * 
     * @return set of Name-Value-Pairs stored in maps
     */
    public void getCounters() {
        String url = mServerUrl + String.format(OP_GET_COUNTERS);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null) {
            return;
        } else if (mHasLastError && mLastError.contains(ERROR)) {
            if (mLastError.contains(UNKNOWN_METHOD)) {
                mLastError = "";
                mHasLastError = false;
            }
        }
        
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
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getCounters() threw JSONException";
            e.printStackTrace();
        }
    }
    
    /**
     * Retrieves all categories.
     * 
     * @return a list of categories.
     */
    public Set<CategoryItem> getCategories() {
        Set<CategoryItem> ret = new LinkedHashSet<CategoryItem>();
        
        String url = mServerUrl + String.format(OP_GET_CATEGORIES);
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
                
                ret.add(new CategoryItem(id, title, unread));
            }
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getCategories() threw JSONException";
            e.printStackTrace();
        }
        
        return ret;
    }
    
    /**
     * Retrieves all feeds, mapped to their categories.
     * 
     * @return a map of all feeds for every category.
     */
    public Set<FeedItem> getFeeds() {
        Set<FeedItem> ret = new LinkedHashSet<FeedItem>();;
        
        // Hardcoded -4 fetches all feeds. See http://tt-rss.org/redmine/wiki/tt-rss/JsonApiReference#getFeeds
        String url = mServerUrl + String.format(OP_GET_FEEDS, "-4");
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
                    ret.add(new FeedItem(id, categoryId, title, feedUrl, unread));
            }
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getSubsribedFeeds() threw JSONException";
            e.printStackTrace();
        }
        
        return ret;
    }
    
    /**
     * Retrieves the specified articles and inserts them into the Database
     * 
     * @param articleIds
     *            the ids of the articles.
     */
    public void getArticle(Set<Integer> ids) {
        if (ids.size() == 0)
            return;
        
        String idList = StringSupport.convertListToString(ids);
        
        String url = mServerUrl + String.format(OP_GET_ARTICLE, idList);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null)
            return;
        
        parseArticlesAndInsertInDB(jsonResult);
    }
    
    /**
     * Retrieves the specified articles and directly stores them in the database.
     * 
     * @param articleIds
     *            the ids of the articles.
     */
    public Set<Integer> getHeadlinesToDatabase(int feedId, int limit, int filter, String viewMode, boolean withContent) {
        String url = mServerUrl + String.format(OP_GET_FEEDHEADLINES, feedId, limit, viewMode, withContent ? 1 : 0);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null)
            return null;
        
        return parseArticlesAndInsertInDB(jsonResult);
    }
    
    /**
     * Marks the given list of article-Ids as read/unread depending on int articleState.
     * 
     * @param articlesIds
     *            the list of ids.
     * @param articleState
     *            the new state of the article (0 -> mark as read; 1 -> mark as unread).
     */
    public boolean setArticleRead(Set<Integer> ids, int articleState) {
        if (ids.size() == 0)
            return true;
        
        String idList = StringSupport.convertListToString(ids);
        
        String url = mServerUrl + String.format(OP_UPDATE_ARTICLE, idList, articleState, 2);
        String ret = doRequestNoAnswer(url);
        return ret.contains(OK);
    }
    
    /**
     * Marks the given Article as "starred"/"not starred" depending on int articleState.
     * 
     * @param articlesId
     *            the article.
     * @param articleState
     *            the new state of the article (0 -> not starred; 1 -> starred; 2 -> toggle).
     */
    public boolean setArticleStarred(Set<Integer> ids, int articleState) {
        if (ids.size() == 0)
            return true;
        
        String idList = StringSupport.convertListToString(ids);
        
        String url = mServerUrl + String.format(OP_UPDATE_ARTICLE, idList, articleState, 0);
        String ret = doRequestNoAnswer(url);
        return ret.contains(OK);
    }
    
    /**
     * Marks the given Article as "published"/"not published" depending on int articleState.
     * 
     * @param articlesId
     *            the article.
     * @param articleState
     *            the new state of the article (0 -> not published; 1 -> published; 2 -> toggle).
     */
    public boolean setArticlePublished(Set<Integer> ids, int articleState) {
        if (ids.size() == 0)
            return true;
        
        String idList = StringSupport.convertListToString(ids);
        
        String url = mServerUrl + String.format(OP_UPDATE_ARTICLE, idList, articleState, 1);
        String ret = doRequestNoAnswer(url);
        return ret.contains(OK);
    }
    
    /**
     * Marks a feed or a category with all its feeds as read.
     * 
     * @param id
     *            the feed-id/category-id.
     * @param isCategory
     *            indicates whether id refers to a feed or a category.
     */
    public boolean setRead(int id, boolean isCategory) {
        String url = mServerUrl + String.format(OP_CATCHUP, id, (isCategory ? 1 : 0));
        String ret = doRequestNoAnswer(url);
        return ret.contains(OK);
    }
    
    /**
     * Returns the value for the given preference-name as a string.
     * 
     * @param pref
     *            the preferences name
     * @return the value of the preference or null if it ist not set or unknown
     */
    public String getPref(String pref) {
        String url = mServerUrl + String.format(OP_GET_PREF, pref);
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
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getPref() threw JSONException";
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Returns true if there was an error.
     * 
     * @return true if there was an error.
     */
    public static boolean hasLastError() {
        return mHasLastError;
    }
    
    /**
     * Returns the last error-message and resets the error-state of the connector.
     * 
     * @return a string with the last error-message.
     */
    public static String pullLastError() {
        String ret = new String(mLastError);
        mLastError = "";
        mHasLastError = false;
        return ret;
    }
    
}
