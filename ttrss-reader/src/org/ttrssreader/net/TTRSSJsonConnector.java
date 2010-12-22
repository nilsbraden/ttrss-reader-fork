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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.utils.Base64;
import org.ttrssreader.utils.Utils;
import android.util.Log;

public class TTRSSJsonConnector implements ITTRSSConnector {
    
    private static final String OP_LOGIN = "?op=login&user=%s&password=%s";
    private static final String OP_GET_CATEGORIES = "?op=getCategories&sid=%s";
    private static final String OP_GET_FEEDS = "?op=getFeeds&sid=%s&cat_id=%s";
    private static final String OP_GET_FEEDHEADLINES = "?op=getHeadlines&sid=%s&feed_id=%s&limit=%s&view_mode=%s";
    private static final String OP_GET_NEW_ARTICLES = "?op=getNewArticles&sid=%s&unread=%s&time=%s";
    private static final String OP_GET_ARTICLE = "?op=getArticle&sid=%s&article_id=%s";
    private static final String OP_UPDATE_ARTICLE = "?op=updateArticle&sid=%s&article_ids=%s&mode=%s&field=%s";
    private static final String OP_CATCHUP = "?op=catchupFeed&sid=%s&feed_id=%s&is_cat=%s";
    private static final String OP_GET_COUNTERS = "?op=getCounters&sid=%s";
    private static final String OP_GET_PREF = "?op=getPref&sid=%s&pref_name=%s";
    
    private static final String ERROR = "{\"error\":";
    private static final String NOT_LOGGED_IN = ERROR + "\"NOT_LOGGED_IN\"}";
    private static final String UNKNOWN_METHOD = ERROR + "\"UNKNOWN_METHOD\"}";
    
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
    
    public static final String COUNTER_KIND = "kind";
    public static final String COUNTER_CAT = "cat";
    public static final String COUNTER_ID = "id";
    public static final String COUNTER_COUNTER = "counter";
    
    private String mServerUrl;
    private String mUserName;
    private String mPassword;
    
    private String mSessionId;
    private String loginLock = "";
    
    private String mLastError = "";
    private boolean mHasLastError = false;
    
    public TTRSSJsonConnector(String serverUrl, String userName, String password) {
        mServerUrl = serverUrl;
        mUserName = userName;
        mPassword = password;
        mSessionId = null;
    }
    
    private String doRequest(String url) {
        long start = System.currentTimeMillis();
        String strResponse = "";
        
        HttpPost httpPost;
        HttpParams httpParams;
        HttpClient httpclient;
        try {
            
            // Authenticator.setDefault(new Authenticator() {
            // protected PasswordAuthentication getPasswordAuthentication() {
            // return new PasswordAuthentication("myuser", "mypass".toCharArray());
            // }
            // });
            
            httpPost = new HttpPost(url);
            httpParams = httpPost.getParams();
            httpclient = HttpClientFactory.createInstance(httpParams);
        } catch (Exception e) {
            Log.e(Utils.TAG, "Error creating HTTP-Connection: " + e.getMessage());
            mHasLastError = true;
            mLastError = "Error creating HTTP-Connection: " + e.getMessage();
            return "";
        }
        
        try {
            HttpResponse response = httpclient.execute(httpPost);
            
            int end = url.lastIndexOf("&password=");
            if (end < 1) {
                end = url.length();
            }
            String tempUrl = url.substring(0, end);
            
            long time = System.currentTimeMillis() - start;
            Log.d(Utils.TAG, String.format("Hidden URL: %s (took %s ms)", tempUrl, time));
            
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                strResponse = Utils.convertStreamToString(instream);
                
                if (strResponse.contains(ERROR + NOT_LOGGED_IN)) {
                    Log.w(Utils.TAG, "Not logged in, retrying...");
                    // Login and post request again
                    String tempSessionId = new String(mSessionId);
                    login();
                    if (url.contains(tempSessionId)) {
                        url = url.replace(tempSessionId, mSessionId);
                    }
                    strResponse = doRequest(url);
                }
                
                // Check returned string for error-messages
                if (strResponse.startsWith(ERROR)) {
                    mHasLastError = true;
                    mLastError = strResponse;
                }
            }
        } catch (IOException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: doRequest(String url), threw IOException";
            e.printStackTrace();
        }
        
        // Parse new start with sequence-number and status-codes
        if (strResponse.startsWith("{\"seq\":")) {
            strResponse = parseMetadata(strResponse);
        }
        
        return strResponse;
    }
    
    private JSONArray getJSONResponseAsArray(String url) {
        mHasLastError = false;
        mLastError = "";
        
        JSONArray result = null;
        String strResponse = doRequest(url);
        
        if (!mHasLastError) {
            if (strResponse != null && strResponse.length() > 0) {
                try {
                    result = new JSONArray(strResponse);
                } catch (JSONException e) {
                    mHasLastError = true;
                    mLastError = e.getMessage() + ", Method: getJSONResponseAsArray(String url)";
                }
            }
        }
        
        return result;
    }
    
    private TTRSSJsonResult getJSONResponse(String url) {
        
        mHasLastError = false;
        mLastError = "";
        
        TTRSSJsonResult result = null;
        String strResponse = doRequest(url);
        
        if (!mHasLastError) {
            try {
                result = new TTRSSJsonResult(strResponse);
            } catch (JSONException e) {
                mHasLastError = true;
                mLastError = e.getMessage() + ", Method: getJSONResponse(String url)";
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
            TTRSSJsonResult jsonResult = getJSONResponse(url);
            
            if (!mHasLastError || jsonResult != null) {
                
                int i = 0;
                try {
                    
                    while ((i < jsonResult.getNames().length())) {
                        if (jsonResult.getNames().getString(i).equals(SESSION_ID)) {
                            mSessionId = jsonResult.getValues().getString(i);
                            break;
                        }
                        i++;
                    }
                } catch (JSONException e) {
                    result = false;
                    mHasLastError = true;
                    mLastError = e.getMessage() + ", Method: login(String url), threw JSONException";
                    e.printStackTrace();
                }
                
            } else {
                result = false;
            }
            
            if (!result) {
                mHasLastError = false;
                mLastError = "";
                
                // Try again with base64-encoded passphrase
                result = loginBase64();
            }
            
            return result;
        }
    }
    
    private boolean loginBase64() {
        boolean result = true;
        mSessionId = null;
        
        byte[] bytes = mPassword.getBytes();
        String mPasswordEncoded = Base64.encodeBytes(bytes);
        
        String url = mServerUrl + String.format(OP_LOGIN, mUserName, mPasswordEncoded);
        TTRSSJsonResult jsonResult = getJSONResponse(url);
        
        if (jsonResult == null) {
            return result;
        }
        
        if (!mHasLastError) {
            
            int i = 0;
            boolean stop = false;
            
            try {
                while ((i < jsonResult.getNames().length()) && (!stop)) {
                    
                    if (jsonResult.getNames().getString(i).equals(SESSION_ID)) {
                        stop = true;
                        mSessionId = jsonResult.getValues().getString(i);
                    } else {
                        i++;
                    }
                    
                }
            } catch (JSONException e) {
                result = false;
                mHasLastError = true;
                mLastError = e.getMessage() + ", Method: login(String url), threw JSONException";
                e.printStackTrace();
            }
            
        } else {
            result = false;
        }
        
        return result;
    }
    
    private String parseMetadata(String str) {
        // TODO: Perhaps add seq-nr-check some day?
        String ret = "";
        
        TTRSSJsonResult result = null;
        
        try {
            if (!mHasLastError) {
                if (str != null && str.length() > 0) {
                    result = new TTRSSJsonResult(str);
                }
            }
            
            if (result == null) {
                return "";
            }
            
            int i = 0;
            while ((i < result.getNames().length())) {
                if (result.getNames().getString(i).equals(CONTENT)) {
                    ret = result.getValues().getString(i);
                    break;
                }
                i++;
            }
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: parseMetadata(String str)";
        }
        
        return ret;
    }
    
    // ***************** Helper-Methods **************************************************
    
    public ArticleItem parseDataForArticle(JSONArray names, JSONArray values) {
        ArticleItem ret = null;
        
        try {
            String realFeedId = "";
            String id = "";
            String title = "";
            boolean isUnread = false;
            String updated = "";
            String content = "";
            String articleUrl = "";
            String articleCommentUrl = "";
            Set<String> attachments = new LinkedHashSet<String>();
            boolean isStarred = false;
            boolean isPublished = false;
            
            for (int i = 0; i < names.length(); i++) {
                
                String s = names.getString(i);
                if (s.equals(ID)) {
                    id = values.getString(i);
                } else if (s.equals(TITLE)) {
                    title = values.getString(i);
                } else if (s.equals(UNREAD)) {
                    isUnread = values.getBoolean(i);
                } else if (s.equals(UPDATED)) {
                    updated = values.getString(i);
                } else if (s.equals(FEED_ID)) {
                    realFeedId = values.getString(i);
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
            
            Date date = new Date(new Long(updated + "000").longValue());
            
            ret = new ArticleItem(id, realFeedId, title, isUnread, articleUrl, articleCommentUrl, date, content,
                    attachments, isStarred, isPublished);
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: parseDataForArticle(...), threw JSONException";
            e.printStackTrace();
        }
        
        return (ret == null ? new ArticleItem() : ret);
    }
    
    public FeedItem parseDataForFeed(JSONArray names, JSONArray values, JSONArray articleValues) {
        FeedItem ret = null;
        
        try {
            String categoryId = "";
            String id = "";
            String title = "";
            String url = "";
            int unread = 0;
            
            for (int i = 0; i < names.length(); i++) {
                
                String s = names.getString(i);
                if (s.equals(CAT_ID)) {
                    categoryId = values.getString(i);
                } else if (s.equals(ID)) {
                    id = values.getString(i);
                } else if (s.equals(TITLE)) {
                    title = values.getString(i);
                } else if (s.equals(FEED_URL)) {
                    url = values.getString(i);
                } else if (s.equals(UNREAD)) {
                    unread = values.getInt(i);
                } else if (s.equals("articles")) {
                    articleValues = (JSONArray) values.get(i);
                }
            }
            
            ret = new FeedItem(categoryId, id, title, url, unread);
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: parseDataForFeed(...), threw JSONException";
            e.printStackTrace();
        }
        
        return (ret == null ? new FeedItem() : ret);
    }
    
    public CategoryItem parseDataForCategory(JSONArray names, JSONArray values, JSONArray feedValues) {
        CategoryItem ret = null;
        
        try {
            String id = "";
            String title = "";
            int unreadCount = 0;
            
            for (int i = 0; i < names.length(); i++) {
                
                String s = names.getString(i);
                if (s.equals(ID)) {
                    id = values.getString(i);
                } else if (s.equals(TITLE)) {
                    title = values.getString(i);
                } else if (s.equals(UNREAD)) {
                    unreadCount = values.getInt(i);
                } else if (s.equals("feeds")) {
                    feedValues = (JSONArray) values.get(i);
                }
            }
            
            ret = new CategoryItem(id, title, unreadCount);
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: parseDataForCategory(...), threw JSONException";
            e.printStackTrace();
        }
        
        return (ret == null ? new CategoryItem() : ret);
    }
    
    public Set<String> parseDataForAttachments(JSONArray array) {
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
    
    // ***************** Retrieve-Data-Methods **************************************************
    
    @Override
    public Set<Map<String, Object>> getCounters() {
        Set<Map<String, Object>> ret = new LinkedHashSet<Map<String, Object>>();
        Map<String, Object> m;
        
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError) {
                return ret;
            }
        }
        
        String url = mServerUrl + String.format(OP_GET_COUNTERS, mSessionId);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null) {
            return ret;
        } else if (mHasLastError && mLastError.contains(ERROR)) {
            // Catch unknown-method error, see comment above
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
                Boolean cat = false;
                Integer id = Integer.MIN_VALUE;
                Integer counter = 0;
                
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
                
                // Only add entrys with integer-ID
                if (id != Integer.MIN_VALUE) {
                    m = new HashMap<String, Object>();
                    m.put(COUNTER_CAT, cat);
                    m.put(COUNTER_ID, id);
                    m.put(COUNTER_COUNTER, counter);
                    ret.add(m);
                }
                
            }
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getCounters(), threw JSONException";
            e.printStackTrace();
        }
        
        return ret;
    }
    
    @Override
    public Set<CategoryItem> getCategories() {
        Set<CategoryItem> ret = new LinkedHashSet<CategoryItem>();
        
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError) {
                return ret;
            }
        }
        
        String url = mServerUrl + String.format(OP_GET_CATEGORIES, mSessionId);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null) {
            return ret;
        }
        
        try {
            for (int i = 0; i < jsonResult.length(); i++) {
                JSONObject object = jsonResult.getJSONObject(i);
                
                JSONArray names = object.names();
                JSONArray values = object.toJSONArray(names);
                
                CategoryItem categoryItem = parseDataForCategory(names, values, null);
                ret.add(categoryItem);
            }
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getCategories(), threw JSONException";
            e.printStackTrace();
        }
        
        return ret;
    }
    
    @Override
    public Map<Integer, Set<FeedItem>> getFeeds() {
        Map<Integer, Set<FeedItem>> ret = new HashMap<Integer, Set<FeedItem>>();;
        
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError) {
                return ret;
            }
        }
        
        // TODO: Hardcoded -4 fetches all feeds. See http://tt-rss.org/redmine/wiki/tt-rss/JsonApiReference#getFeeds
        String url = mServerUrl + String.format(OP_GET_FEEDS, mSessionId, "-4");
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null) {
            return ret;
        }
        
        try {
            for (int i = 0; i < jsonResult.length(); i++) {
                JSONObject object = jsonResult.getJSONObject(i);
                
                JSONArray names = object.names();
                JSONArray values = object.toJSONArray(names);
                
                FeedItem f = parseDataForFeed(names, values, null);
                
                if (f.getId() <= 0)
                    continue;
                
                Set<FeedItem> feedItems = ret.get(f.getCategoryId());
                if (feedItems == null) {
                    feedItems = new LinkedHashSet<FeedItem>();
                    ret.put(f.getCategoryId(), feedItems);
                }
                feedItems.add(f);
            }
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getSubsribedFeeds(), threw JSONException";
            e.printStackTrace();
        }
        
        return ret;
    }
    
    @Override
    public Set<ArticleItem> getFeedHeadlines(int feedId, int limit, int filter, String viewMode) {
        Set<ArticleItem> ret = new LinkedHashSet<ArticleItem>();
        
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError) {
                return ret;
            }
        }
        
        String url = mServerUrl + String.format(OP_GET_FEEDHEADLINES, mSessionId, feedId, limit, viewMode);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null) {
            return ret;
        }
        
        try {
            for (int i = 0; i < jsonResult.length(); i++) {
                JSONObject object = jsonResult.getJSONObject(i);
                
                JSONArray names = object.names();
                JSONArray values = object.toJSONArray(names);
                
                ret.add(parseDataForArticle(names, values));
            }
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getFeedHeadlines(...), threw JSONException";
            e.printStackTrace();
        }
        
        return ret;
    }
    
    @Override
    public Set<ArticleItem> getArticle(Set<Integer> articleIds) {
        Set<ArticleItem> ret = new LinkedHashSet<ArticleItem>();
        
        if (articleIds.size() == 0)
            return ret;
        
        StringBuilder sb = new StringBuilder();
        for (Integer i : articleIds) {
            sb.append(i);
            sb.append(",");
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }
        
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError) {
                return ret;
            }
        }
        
        String url = mServerUrl + String.format(OP_GET_ARTICLE, mSessionId, sb.toString());
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null) {
            return ret;
        }
        
        try {
            for (int i = 0; i < jsonResult.length(); i++) {
                JSONObject object = jsonResult.getJSONObject(i);
                
                JSONArray names = object.names();
                JSONArray values = object.toJSONArray(names);
                
                ret.add(parseDataForArticle(names, values));
            }
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getArticle(...), threw JSONException";
            e.printStackTrace();
        }
        
        return ret;
    }
    
    @Override
    public Map<CategoryItem, Map<FeedItem, Set<ArticleItem>>> getNewArticles(int articleState, long time) {
        /* Not integrated into Tiny Tiny RSS, handle with care so nobody get hurt */
        Map<CategoryItem, Map<FeedItem, Set<ArticleItem>>> ret = new HashMap<CategoryItem, Map<FeedItem, Set<ArticleItem>>>();
        
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError) {
                return ret;
            }
        }
        
        String url = mServerUrl + String.format(OP_GET_NEW_ARTICLES, mSessionId, articleState, time);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (mHasLastError && mLastError.contains(ERROR)) {
            // Catch unknown-method error, see comment above
            if (mLastError.contains(UNKNOWN_METHOD)) {
                mLastError = "";
                mHasLastError = false;
            }
            return ret;
        } else if (jsonResult == null) {
            return ret;
        }
        
        try {
            for (int i = 0; i < jsonResult.length(); i++) {
                JSONObject object = jsonResult.getJSONObject(i);
                
                Map<FeedItem, Set<ArticleItem>> feedMap = new HashMap<FeedItem, Set<ArticleItem>>();
                
                JSONArray names = object.names();
                JSONArray values = object.toJSONArray(names);
                JSONArray feedValues = new JSONArray();
                
                CategoryItem c = parseDataForCategory(names, values, feedValues);
                
                if (feedValues.length() < 1) {
                    continue;
                }
                
                TTRSSJsonResult resultFeeds = new TTRSSJsonResult(feedValues.getString(0));
                JSONArray feedNames = resultFeeds.getNames();
                feedValues = resultFeeds.getValues();
                
                for (int j = 0; j < feedNames.length(); j++) {
                    Set<ArticleItem> articles = new LinkedHashSet<ArticleItem>();
                    
                    JSONArray articleValues = new JSONArray();
                    FeedItem f = parseDataForFeed(names, values, articleValues);
                    
                    if (articleValues.length() < 1) {
                        continue;
                    }
                    
                    TTRSSJsonResult resultArts = new TTRSSJsonResult(articleValues.getString(0));
                    JSONArray articleNames = resultArts.getNames();
                    articleValues = resultArts.getValues();
                    
                    for (int k = 0; k < articleNames.length(); k++) {
                        articles.add(parseDataForArticle(articleNames, articleValues));
                    }
                    
                    feedMap.put(f, articles);
                }
                
                ret.put(c, feedMap);
            }
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getNewArticles(...), threw JSONException";
            e.printStackTrace();
        }
        
        return ret;
    }
    
    @Override
    public void setArticleRead(Set<Integer> articlesIds, int articleState) {
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError) {
                return;
            }
        }
        
        StringBuilder sb = new StringBuilder();
        for (Integer s : articlesIds) {
            sb.append(s + ",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        
        String url = mServerUrl + String.format(OP_UPDATE_ARTICLE, mSessionId, sb, articleState, 2);
        doRequest(url);
    }
    
    @Override
    public void setArticleStarred(int articlesId, int articleState) {
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError) {
                return;
            }
        }
        
        String url = mServerUrl + String.format(OP_UPDATE_ARTICLE, mSessionId, articlesId, articleState, 0);
        doRequest(url);
    }
    
    @Override
    public void setArticlePublished(int articlesId, int articleState) {
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError) {
                return;
            }
        }
        
        String url = mServerUrl + String.format(OP_UPDATE_ARTICLE, mSessionId, articlesId, articleState, 1);
        doRequest(url);
    }
    
    @Override
    public void setRead(int id, boolean isCategory) {
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError) {
                return;
            }
        }
        
        // TODO: replace 1|0 by boolean value, at the moment the value isn't correctly parsed in the API
        String url = mServerUrl + String.format(OP_CATCHUP, mSessionId, id, (isCategory ? 1 : 0));
        doRequest(url);
    }
    
    @Override
    public String getPref(String pref) {
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError) {
                return null;
            }
        }
        
        String ret = null;
        String url = mServerUrl + String.format(OP_GET_PREF, mSessionId, pref);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null) {
            return ret;
        }
        
        try {
            for (int i = 0; i < jsonResult.length(); i++) {
                JSONObject object = jsonResult.getJSONObject(i);
                
                JSONArray names = object.names();
                JSONArray values = object.toJSONArray(names);
                
                for (int j = 0; j < names.length(); j++) {
                    
                    String s = names.getString(j);
                    if (s.equals(VALUE)) {
                        ret = values.getString(j);
                    }
                }
                
            }
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getPref(), threw JSONException";
            e.printStackTrace();
        }
        
        return ret;
    }
    
    @Override
    public boolean hasLastError() {
        return mHasLastError;
    }
    
    @Override
    public String pullLastError() {
        String ret = new String(mLastError);
        mLastError = "";
        mHasLastError = false;
        return ret;
    }
    
    @Override
    public String getLastError() {
        return mLastError;
    }
    
}
