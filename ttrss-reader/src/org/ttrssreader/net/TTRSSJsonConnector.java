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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private static final String OP_GET_UNREAD = "?op=getUnread&sid=%s";
    private static final String OP_GET_CATEGORIES = "?op=getCategories&sid=%s";
    private static final String OP_GET_FEEDS = "?op=getFeeds&sid=%s";
    private static final String OP_GET_FEEDHEADLINES = "?op=getHeadlines&sid=%s&feed_id=%s&limit=%s&show_content=0&view_mode=%s";
    private static final String OP_GET_ARTICLES = "?op=getArticles&sid=%s&id=%s&unread=%s&is_category=%s&limit=%s";
    private static final String OP_GET_NEW_ARTICLES = "?op=getNewArticles&sid=%s&unread=%s&time=%s";
    private static final String OP_GET_ARTICLE = "?op=getArticle&sid=%s&article_id=%s";
    private static final String OP_UPDATE_ARTICLE = "?op=updateArticle&sid=%s&article_ids=%s&mode=%s&field=%s";
    private static final String OP_CATCHUP = "?op=catchupFeed&sid=%s&feed_id=%s&is_cat=%s";
    private static final String OP_GET_COUNTERS = "?op=getCounters&sid=%s";
    
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
    private static final String FEEDS = "feeds";
    private static final String COMMENT_URL = "comments";
    private static final String ATTACHMENTS = "attachments";
    
    private String mServerUrl;
    private String mUserName;
    private String mPassword;
    
    private String mSessionId;
    
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
        String strResponse = null;
        
        HttpPost httpPost = new HttpPost(url);
        
        HttpParams httpParams = httpPost.getParams();
        HttpClient httpclient = HttpClientFactory.createInstance(httpParams);
        
        try {
            HttpResponse response = httpclient.execute(httpPost);
            
            Log.d(Utils.TAG, "Requesting URL: " + url.replace(mPassword, "*") + " (took "
                    + (System.currentTimeMillis() - start) + " ms)");
            
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                strResponse = Utils.convertStreamToString(instream);
                
                if (strResponse.contains(NOT_LOGGED_IN)) {
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
        
        return strResponse;
    }
    
    private JSONArray getJSONResponseAsArray(String url) {
        mHasLastError = false;
        mLastError = "";
        
        JSONArray result = null;
        String strResponse = doRequest(url);
        
        if (!mHasLastError) {
            try {
                result = new JSONArray(strResponse);
            } catch (JSONException e) {
                mHasLastError = true;
                mLastError = e.getMessage() + ", Method: getJSONResponseAsArray(String url)";
                e.printStackTrace();
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
                e.printStackTrace();
            }
        }
        
        return result;
    }
    
    private boolean login() {
        boolean result = true;
        mSessionId = null;
        
        String url = mServerUrl + String.format(OP_LOGIN, mUserName, mPassword);
        TTRSSJsonResult jsonResult = getJSONResponse(url);
        
        if (!mHasLastError || jsonResult != null) {
            
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
        
        if (!result) {
            mHasLastError = false;
            mLastError = "";
            result = loginBase64();
        }
        
        return result;
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
    
    // ***************** Helper-Methods **************************************************
    
    public List<FeedItem> handleFeedCounters(JSONArray array) {
        List<FeedItem> ret = new ArrayList<FeedItem>();
        
        try {
            for (int j = 0; j < array.length(); j++) {
                
                TTRSSJsonResult att = new TTRSSJsonResult(array.getString(j));
                JSONArray names = att.getNames();
                JSONArray values = att.getValues();
                
                FeedItem f = new FeedItem();
                
                // Filter for feed_id and unread-count
                for (int k = 0; k < names.length(); k++) {
                    if (names.getString(k).equals("feed_id")) {
                        f.setId(values.getString(k));
                    } else if (names.getString(k).equals("unread")) {
                        f.setUnread(values.getInt(k));
                    }
                }
                ret.add(f);
                
            }
        } catch (JSONException je) {
            je.printStackTrace();
        }
        
        return ret;
    }
    
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
            Set<String> attachments = new HashSet<String>();
            
            for (int i = 0; i < names.length(); i++) {
                if (names.getString(i).equals(ID))
                    id = values.getString(i);
                else if (names.getString(i).equals(TITLE))
                    title = values.getString(i);
                else if (names.getString(i).equals(UNREAD))
                    isUnread = values.getBoolean(i);
                else if (names.getString(i).equals(UPDATED))
                    updated = values.getString(i);
                else if (names.getString(i).equals(FEED_ID))
                    realFeedId = values.getString(i);
                else if (names.getString(i).equals(CONTENT))
                    content = values.getString(i);
                else if (names.getString(i).equals(URL))
                    articleUrl = values.getString(i);
                else if (names.getString(i).equals(COMMENT_URL))
                    articleCommentUrl = values.getString(i);
                else if (names.getString(i).equals(ATTACHMENTS)) {
                    attachments = parseDataForAttachments((JSONArray) values.get(i));
                }
            }
            
            Date date = new Date(new Long(updated + "000").longValue());
            
            ret = new ArticleItem(realFeedId, id, title, isUnread, date, content, articleUrl, articleCommentUrl,
                    attachments);
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
                if (names.getString(i).equals(CAT_ID)) {
                    categoryId = values.getString(i);
                } else if (names.getString(i).equals(ID)) {
                    id = values.getString(i);
                } else if (names.getString(i).equals(TITLE)) {
                    title = values.getString(i);
                } else if (names.getString(i).equals(FEED_URL)) {
                    url = values.getString(i);
                } else if (names.getString(i).equals(UNREAD)) {
                    unread = values.getInt(i);
                } else if (names.getString(i).equals("articles")) {
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
                if (names.getString(i).equals(ID)) {
                    id = values.getString(i);
                } else if (names.getString(i).equals(TITLE)) {
                    title = values.getString(i);
                } else if (names.getString(i).equals(UNREAD)) {
                    unreadCount = values.getInt(i);
                } else if (names.getString(i).equals("feeds")) {
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
        Set<String> ret = new HashSet<String>();
        
        try {
            for (int j = 0; j < array.length(); j++) {
                
                TTRSSJsonResult att = new TTRSSJsonResult(array.getString(j));
                JSONArray names = att.getNames();
                JSONArray values = att.getValues();
                
                String attId = null;
                String attUrl = null;
                
                // Filter for id and content_url, other fields are not necessary
                for (int k = 0; k < names.length(); k++) {
                    if (names.getString(k).equals("id")) {
                        attId = values.getString(k);
                    } else if (names.getString(k).equals("content_url")) {
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
    public int getTotalUnread() {
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            
            if (mHasLastError) {
                return -1;
            }
        }
        
        String url = mServerUrl + String.format(OP_GET_UNREAD, mSessionId);
        TTRSSJsonResult jsonResult = getJSONResponse(url);
        
        if (jsonResult == null || mHasLastError) {
            return -1;
        }
        
        try {
            for (int i = 0; i < jsonResult.getNames().length(); i++) {
                if (jsonResult.getNames().getString(i).equals(UNREAD)) {
                    return jsonResult.getValues().getInt(i);
                }
            }
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getTotalUnread(), threw JSONException";
            e.printStackTrace();
        }
        
        return -1;
    }
    
    @Override
    public Map<CategoryItem, List<FeedItem>> getCounters() {
        /* Not yet integrated into Tiny Tiny RSS, handle with care so nobody get hurt */
        Map<CategoryItem, List<FeedItem>> ret = new HashMap<CategoryItem, List<FeedItem>>();
        
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError)
                return ret;
        }
        
        String url = mServerUrl + String.format(OP_GET_COUNTERS, mSessionId);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null) {
            return ret;
        } else if (mHasLastError && mLastError.startsWith(ERROR)) {
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
                
                String cat_id = "";
                int unread = 0;
                List<FeedItem> feeds = null;
                
                for (int j = 0; j < names.length(); j++) {
                    if (names.getString(j).equals(CAT_ID)) {
                        cat_id = values.getString(j);
                    } else if (names.getString(j).equals(UNREAD)) {
                        unread = values.getInt(j);
                    } else if (names.getString(j).equals(FEEDS)) {
                        feeds = handleFeedCounters((JSONArray) values.get(j));
                    }
                }
                
                ret.put(new CategoryItem(cat_id, "", unread), feeds);
            }
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getCounters(), threw JSONException";
            e.printStackTrace();
        }
        
        return ret;
    }
    
    @Override
    public List<CategoryItem> getCategories() {
        List<CategoryItem> ret = new ArrayList<CategoryItem>();
        
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError)
                return ret;
        }
        
        String url = mServerUrl + String.format(OP_GET_CATEGORIES, mSessionId);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null)
            return ret;
        
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
    public Map<Integer, List<FeedItem>> getFeeds() {
        Map<Integer, List<FeedItem>> ret = new HashMap<Integer, List<FeedItem>>();;
        
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError)
                return ret;
        }
        
        String url = mServerUrl + String.format(OP_GET_FEEDS, mSessionId);
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
                
                List<FeedItem> feedItemList = ret.get(f.getCategoryId());
                if (feedItemList == null) {
                    feedItemList = new ArrayList<FeedItem>();
                    ret.put(f.getCategoryId(), feedItemList);
                }
                feedItemList.add(f);
            }
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getSubsribedFeeds(), threw JSONException";
            e.printStackTrace();
        }
        
        return ret;
    }
    
    @Override
    public List<ArticleItem> getArticles(int parentId, boolean displayOnlyUnread, boolean isCategory) {
        /* Not yet integrated into Tiny Tiny RSS, handle with care so nobody get hurt */
        ArrayList<ArticleItem> ret = new ArrayList<ArticleItem>();
        
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError)
                return ret;
        }
        
        int unread = displayOnlyUnread ? 1 : 0;
        int cat = isCategory ? 1 : 0;
        
        // TODO: Find a better way of handling the limit 25
        String url = mServerUrl + String.format(OP_GET_ARTICLES, mSessionId, parentId, unread, cat, 25);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null) {
            return ret;
        } else if (mHasLastError && mLastError.startsWith(ERROR)) {
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
                
                ret.add(parseDataForArticle(names, values));
            }
        } catch (JSONException e) {
            mHasLastError = true;
            mLastError = e.getMessage() + ", Method: getFeedArticles(...), threw JSONException";
            e.printStackTrace();
        }
        
        return ret;
    }
    
    @Override
    public ArticleItem getArticle(int articleId) {
        ArticleItem ret = new ArticleItem();
        
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError)
                return ret;
        }
        
        String url = mServerUrl + String.format(OP_GET_ARTICLE, mSessionId, articleId);
        TTRSSJsonResult jsonResult = getJSONResponse(url);
        
        if (jsonResult == null)
            return ret;
        
        if (!mHasLastError) {
            ret = parseDataForArticle(jsonResult.getNames(), jsonResult.getValues());
            if (ret.getId() < 1)
                ret.setId(articleId);
        }
        
        return ret;
    }
    
    @Override
    public List<ArticleItem> getFeedHeadlines(int feedId, int limit, int filter, String viewMode) {
        ArrayList<ArticleItem> ret = new ArrayList<ArticleItem>();
        
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError)
                return ret;
        }
        
        String url = mServerUrl + String.format(OP_GET_FEEDHEADLINES, mSessionId, feedId, limit, viewMode);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null)
            return ret;
        
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
    public Map<CategoryItem, Map<FeedItem, List<ArticleItem>>> getNewArticles(int articleState, long time) {
        Map<CategoryItem, Map<FeedItem, List<ArticleItem>>> ret = new HashMap<CategoryItem, Map<FeedItem, List<ArticleItem>>>();
        
        /* Not yet integrated into Tiny Tiny RSS, handle with care so nobody get hurt */
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError)
                return ret;
        }
        
        String url = mServerUrl + String.format(OP_GET_NEW_ARTICLES, mSessionId, articleState, time);
        JSONArray jsonResult = getJSONResponseAsArray(url);
        
        if (jsonResult == null) {
            return ret;
        } else if (mHasLastError && mLastError.startsWith(ERROR)) {
            // Catch unknown-method error, see comment above
            if (mLastError.contains(UNKNOWN_METHOD)) {
                mLastError = "";
                mHasLastError = false;
            }
        }
        
        try {
            for (int i = 0; i < jsonResult.length(); i++) {
                JSONObject object = jsonResult.getJSONObject(i);
                
                Map<FeedItem, List<ArticleItem>> feedMap = new HashMap<FeedItem, List<ArticleItem>>();
                
                JSONArray names = object.names();
                JSONArray values = object.toJSONArray(names);
                JSONArray feedValues = new JSONArray();
                
                CategoryItem c = parseDataForCategory(names, values, feedValues);
                
                if (feedValues.length() < 1)
                    continue;
                
                TTRSSJsonResult resultFeeds = new TTRSSJsonResult(feedValues.getString(0));
                JSONArray feedNames = resultFeeds.getNames();
                feedValues = resultFeeds.getValues();
                
                for (int j = 0; j < feedNames.length(); j++) {
                    List<ArticleItem> articles = new ArrayList<ArticleItem>();
                    
                    JSONArray articleValues = new JSONArray();
                    FeedItem f = parseDataForFeed(names, values, articleValues);
                    
                    if (articleValues.length() < 1)
                        continue;
                    
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
    public void setArticleRead(List<Integer> articlesIds, int articleState) {
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError)
                return;
        }
        
        StringBuilder sb = new StringBuilder();
        for (Integer s : articlesIds) {
            sb.append(s + ",");
        }
        if (sb.length() > 0)
            sb.deleteCharAt(sb.length() - 1);
        
        String url = mServerUrl + String.format(OP_UPDATE_ARTICLE, mSessionId, sb, articleState, 2);
        doRequest(url);
    }
    
    @Override
    public void setRead(int id, boolean isCategory) {
        if (mSessionId == null || mLastError.equals(NOT_LOGGED_IN)) {
            login();
            if (mHasLastError)
                return;
        }
        
        String url = mServerUrl + String.format(OP_CATCHUP, mSessionId, id, isCategory);
        doRequest(url);
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
