/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.pojos.Label;
import org.ttrssreader.utils.Base64;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;
import android.util.Log;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;

public abstract class JSONConnector {
    
    private static final String TAG = JSONConnector.class.getSimpleName();
    
    protected static String lastError = "";
    protected static boolean hasLastError = false;
    
    private static final String PARAM_OP = "op";
    private static final String PARAM_USER = "user";
    private static final String PARAM_PW = "password";
    private static final String PARAM_CAT_ID = "cat_id";
    private static final String PARAM_CATEGORY_ID = "category_id";
    private static final String PARAM_FEED_ID = "feed_id";
    private static final String PARAM_FEED_URL = "feed_url";
    private static final String PARAM_ARTICLE_IDS = "article_ids";
    private static final String PARAM_LIMIT = "limit";
    private static final int PARAM_LIMIT_API_5 = 60;
    private static final String PARAM_VIEWMODE = "view_mode";
    private static final String PARAM_SHOW_CONTENT = "show_content";
    private static final String PARAM_INC_ATTACHMENTS = "include_attachments"; // include_attachments available since
                                                                               // 1.5.3 but is ignored on older
                                                                               // versions
    private static final String PARAM_SINCE_ID = "since_id";
    private static final String PARAM_SEARCH = "search";
    private static final String PARAM_SKIP = "skip";
    private static final String PARAM_MODE = "mode";
    private static final String PARAM_FIELD = "field"; // 0-starred, 1-published, 2-unread, 3-article note (since api
                                                       // level 1)
    private static final String PARAM_DATA = "data"; // optional data parameter when setting note field
    private static final String PARAM_IS_CAT = "is_cat";
    private static final String PARAM_PREF = "pref_name";
    
    private static final String VALUE_LOGIN = "login";
    private static final String VALUE_GET_CATEGORIES = "getCategories";
    private static final String VALUE_GET_FEEDS = "getFeeds";
    private static final String VALUE_GET_HEADLINES = "getHeadlines";
    private static final String VALUE_UPDATE_ARTICLE = "updateArticle";
    private static final String VALUE_CATCHUP = "catchupFeed";
    private static final String VALUE_UPDATE_FEED = "updateFeed";
    private static final String VALUE_GET_PREF = "getPref";
    private static final String VALUE_GET_VERSION = "getVersion";
    private static final String VALUE_SET_LABELS = "setArticleLabel";
    private static final String VALUE_SHARE_TO_PUBLISHED = "shareToPublished";
    private static final String VALUE_FEED_SUBSCRIBE = "subscribeToFeed";
    private static final String VALUE_FEED_UNSUBSCRIBE = "unsubscribeFeed";
    
    private static final String VALUE_LABEL_ID = "label_id";
    private static final String VALUE_ASSIGN = "assign";
    
    private static final String ERROR = "error";
    private static final String NOT_LOGGED_IN = "NOT_LOGGED_IN";
    private static final String UNKNOWN_METHOD = "UNKNOWN_METHOD";
    private static final String NOT_LOGGED_IN_MESSAGE = "Couldn't login to your account, please check your credentials.";
    private static final String API_DISABLED = "API_DISABLED";
    private static final String API_DISABLED_MESSAGE = "Please enable API for the user \"%s\" in the preferences of this user on the Server.";
    private static final String STATUS = "status";
    private static final String API_LEVEL = "api_level";
    
    private static final String SESSION_ID = "session_id"; // session id as an OUT parameter
    private static final String ID = "id";
    
    private static final String TITLE = "title";
    private static final String UNREAD = "unread";
    
    private static final String CAT_ID = "cat_id";
    
    private static final String CONTENT = "content";
    
    private static final String URL_SHARE = "url";
    private static final String FEED_URL = "feed_url";
    
    private static final String CONTENT_URL = "content_url";
    
    private static final String VALUE = "value";
    private static final String VERSION = "version";
    
    private static final int MAX_ID_LIST_LENGTH = 100;
    
    protected static final String SID = "sid"; // session id as an IN parameter
    
    protected boolean httpAuth = false;
    protected String httpUsername;
    protected String httpPassword;
    
    protected String sessionId = null;
    
    private final Object lock = new Object();
    private int apiLevel = -1;
    
    public static final int PARAM_LIMIT_MAX_VALUE = 200;
    
    public JSONConnector() {
        refreshHTTPAuth();
    }
    
    protected abstract InputStream doRequest(Map<String, String> params);
    
    protected void refreshHTTPAuth() {
        httpAuth = Controller.getInstance().useHttpAuth();
        if (!httpAuth)
            return;
        
        if (httpUsername != null && httpUsername.equals(Controller.getInstance().httpUsername()))
            return;
        if (httpPassword != null && httpPassword.equals(Controller.getInstance().httpPassword()))
            return;
        
        // Refresh data
        httpUsername = Controller.getInstance().httpUsername();
        httpPassword = Controller.getInstance().httpPassword();
    }
    
    protected void logRequest(final JSONObject json) throws JSONException {
        // Filter password and session-id
        Object paramPw = json.remove(PARAM_PW);
        Object paramSID = json.remove(SID);
        Log.i(TAG, json.toString());
        json.put(PARAM_PW, paramPw);
        json.put(SID, paramSID);
    }
    
    private String readResult(Map<String, String> params, boolean login) throws IOException {
        return readResult(params, login, true);
    }
    
    private String readResult(Map<String, String> params, boolean login, boolean retry) throws IOException {
        InputStream in = doRequest(params);
        if (in == null)
            return null;
        
        JsonReader reader = null;
        String ret = "";
        try {
            reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
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
                        }
                        if (object.get(STATUS) != null) {
                            ret = object.get(STATUS).getAsString();
                        }
                        if (object.get(API_LEVEL) != null) {
                            this.apiLevel = object.get(API_LEVEL).getAsInt();
                        }
                        if (object.get(VALUE) != null) {
                            ret = object.get(VALUE).getAsString();
                        }
                        if (object.get(ERROR) != null) {
                            String message = object.get(ERROR).getAsString();
                            
                            if (message.contains(NOT_LOGGED_IN)) {
                                if (!login && retry && login()) {
                                    return readResult(params, false, false); // Just do the same request again
                                } else {
                                    hasLastError = true;
                                    lastError = message;
                                    return null;
                                }
                            }
                            
                            if (message.contains(API_DISABLED)) {
                                hasLastError = true;
                                lastError = String.format(API_DISABLED_MESSAGE, Controller.getInstance().username());
                                return null;
                            }
                            
                            // Any other error
                            hasLastError = true;
                            lastError = message;
                            return null;
                        }
                    }
                    
                } else {
                    reader.skipValue();
                }
            }
        } finally {
            if (reader != null)
                reader.close();
        }
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
        InputStream in = doRequest(params);
        if (in == null)
            return null;
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        
        // Check if content contains array or object, array indicates login-response or error, object is content
        try {
            reader.beginObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("content")) {
                JsonToken t = reader.peek();
                
                if (t.equals(JsonToken.BEGIN_ARRAY)) {
                    return reader;
                } else if (t.equals(JsonToken.BEGIN_OBJECT)) {
                    
                    JsonObject object = new JsonObject();
                    reader.beginObject();
                    
                    String nextName = reader.nextName();
                    // We have a BEGIN_OBJECT here but its just the response to call "subscribeToFeed"
                    if ("status".equals(nextName))
                        return reader;
                    
                    // Handle error
                    while (reader.hasNext()) {
                        if (nextName != null) {
                            object.addProperty(nextName, reader.nextString());
                            nextName = null;
                        } else {
                            object.addProperty(reader.nextName(), reader.nextString());
                        }
                    }
                    reader.endObject();
                    
                    if (object.get(ERROR) != null) {
                        String message = object.get(ERROR).toString();
                        
                        if (message.contains(NOT_LOGGED_IN)) {
                            lastError = NOT_LOGGED_IN;
                            if (firstCall && login() && !hasLastError)
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
                        lastError = message;
                    }
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
            String result = readResult(params, false);
            
            // Reset error, this is only for an api-bug which returns an empty result for updateFeed
            if (result == null)
                pullLastError();
            
            if ("OK".equals(result))
                return true;
            else
                return false;
        } catch (MalformedJsonException mje) {
            // Reset error, this is only for an api-bug which returns an empty result for updateFeed
            pullLastError();
        } catch (IOException e) {
            e.printStackTrace();
            if (!hasLastError) {
                hasLastError = true;
                lastError = formatException(e);
            }
        }
        
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
        if (sessionId != null && !lastError.equals(NOT_LOGGED_IN))
            return true;
        
        synchronized (lock) {
            if (sessionId != null && !lastError.equals(NOT_LOGGED_IN))
                return true; // Login done while we were waiting for the lock
                
            Map<String, String> params = new HashMap<String, String>();
            params.put(PARAM_OP, VALUE_LOGIN);
            params.put(PARAM_USER, Controller.getInstance().username());
            params.put(PARAM_PW, Base64.encodeBytes(Controller.getInstance().password().getBytes()));
            
            try {
                sessionId = readResult(params, true, false);
                if (sessionId != null) {
                    Log.d(TAG, "login: " + (System.currentTimeMillis() - time) + "ms");
                    return true;
                }
            } catch (IOException e) {
                if (!hasLastError) {
                    hasLastError = true;
                    lastError = formatException(e);
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
    
    /**
     * parse articles from JSON-reader
     * 
     * @param articles
     *            container, where parsed articles will be stored
     * @param reader
     *            JSON-reader, containing articles (received from server)
     * @param labelId
     *            ID of label to be added to each parsed article
     * @param skipNames
     *            set of names (article properties), which should not be processed (may be {@code null})
     * @param filter
     *            filter for articles, defining which articles should be omitted while parsing (may be {@code null})
     * @return amount of processed articles
     */
    private int parseArticleArray(final Set<Article> articles, JsonReader reader, long labelId, Set<Article.ArticleField> skipNames, IArticleOmitter filter) {
        long time = System.currentTimeMillis();
        int count = 0;
        
        try {
            reader.beginArray();
            while (reader.hasNext()) {
                Article a = new Article();
                
                boolean skipObject = false;
                
                reader.beginObject();
                while (reader.hasNext() && reader.peek().equals(JsonToken.NAME)) {
                    if (skipObject) {
                        // field name
                        reader.skipValue();
                        // field value
                        reader.skipValue();
                        continue;
                    }
                    
                    String name = reader.nextName();
                    
                    try {
                        Article.ArticleField field = Article.ArticleField.valueOf(name);
                        
                        if (skipNames != null && skipNames.contains(field)) {
                            reader.skipValue();
                            continue;
                        }
                        
                        switch (field) {
                            case id:
                                a.id = reader.nextInt();
                                break;
                            case title:
                                a.title = reader.nextString();
                                break;
                            case unread:
                                a.isUnread = reader.nextBoolean();
                                break;
                            case updated:
                                a.updated = new Date(reader.nextLong() * 1000);
                                break;
                            case feed_id:
                                if (reader.peek() == JsonToken.NULL)
                                    reader.nextNull();
                                else
                                    a.feedId = reader.nextInt();
                                break;
                            case content:
                                a.content = reader.nextString();
                                break;
                            case link:
                                a.url = reader.nextString();
                                break;
                            case comments:
                                a.commentUrl = reader.nextString();
                                break;
                            case attachments:
                                a.attachments = parseAttachments(reader);
                                break;
                            case marked:
                                a.isStarred = reader.nextBoolean();
                                break;
                            case published:
                                a.isPublished = reader.nextBoolean();
                                break;
                            case labels:
                                a.labels = parseLabels(reader);
                                break;
                            case author:
                                a.author = reader.nextString();
                                break;
                            // valid, but currently unused
                            // TODO: incorporate into Article object?
                            case is_updated:
                            case tags:
                            case feed_title:
                            case comments_count:
                            case comments_link:
                            case always_display_attachments:
                            case score:
                            case note:
                            case lang:
                                reader.skipValue();
                                continue;
                        }
                        
                        if (filter != null)
                            skipObject = filter.omitArticle(field, a);
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Result contained illegal value for entry \"" + name + "\".");
                        reader.skipValue();
                        continue;
                    }
                    
                }
                reader.endObject();
                
                if (!skipObject && a.id != -1 && a.title != null) {
                    articles.add(a);
                }
                
                count++;
            }
            reader.endArray();
        } catch (StopJsonParsingException e) {
            Log.i(TAG, "Parsing of aricle array was broken after " + count + " articles");
        } catch (OutOfMemoryError e) {
            Controller.getInstance().lowMemory(true); // Low memory detected
        } catch (Exception e) {
            Log.e(TAG, "Input data could not be read: " + e.getMessage() + " (" + e.getCause() + ")", e);
        }
        
        Log.d(TAG, "parseArticleArray: parsing " + count + " articles took " + (System.currentTimeMillis() - time)
                + "ms");
        return count;
    }
    
    private Set<Label> parseLabels(final JsonReader reader) throws IOException {
        Set<Label> ret = new HashSet<Label>();
        
        if (reader.peek().equals(JsonToken.BEGIN_ARRAY)) {
            reader.beginArray();
        } else {
            reader.skipValue();
            return ret;
        }
        
        try {
            while (reader.hasNext()) {
                
                Label label = new Label();
                reader.beginArray();
                try {
                    label.id = Integer.parseInt(reader.nextString());
                    label.caption = reader.nextString();
                    label.foregroundColor = reader.nextString();
                    label.backgroundColor = reader.nextString();
                    label.checked = true;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    reader.skipValue();
                    continue;
                }
                ret.add(label);
                reader.endArray();
            }
            reader.endArray();
        } catch (Exception e) {
            // Ignore exceptions here
            try {
                if (reader.peek().equals(JsonToken.END_ARRAY))
                    reader.endArray();
            } catch (Exception ee) {
            }
        }
        
        return ret;
    }
    
    // ***************** Retrieve-Data-Methods **************************************************
    
    /**
     * Retrieves all categories.
     * 
     * @return a list of categories.
     */
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
                
                // Don't handle categories with an id below 1, we already have them in the DB from
                // Data.updateVirtualCategories()
                if (id > 0 && title != null)
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
        
        Log.d(TAG, "getCategories: " + (System.currentTimeMillis() - time) + "ms");
        return ret;
    }
    
    /**
     * get current feeds from server
     * 
     * @param tolerateWrongUnreadInformation
     *            if set to {@code false}, then
     *            lazy server will be updated before
     * 
     * @return set of actual feeds on server
     */
    private Set<Feed> getFeeds(boolean tolerateWrongUnreadInformation) {
        long time = System.currentTimeMillis();
        Set<Feed> ret = new LinkedHashSet<Feed>();
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
        
        Log.d(TAG, "getFeeds: " + (System.currentTimeMillis() - time) + "ms");
        return ret;
    }
    
    /**
     * Retrieves all feeds from server.
     * 
     * @return a set of all feeds on server.
     */
    public Set<Feed> getFeeds() {
        return getFeeds(false);
    }
    
    private boolean makeLazyServerWork(Integer feedId) {
        if (Controller.getInstance().lazyServer()) {
            Map<String, String> taskParams = new HashMap<String, String>();
            taskParams.put(PARAM_OP, VALUE_UPDATE_FEED);
            taskParams.put(PARAM_FEED_ID, String.valueOf(feedId));
            return doRequestNoAnswer(taskParams);
        }
        return true;
    }
    
    private long noTaskUntil = 0;
    final static private long minTaskIntervall = 10 * Utils.MINUTE;
    
    private boolean makeLazyServerWork() {
        boolean ret = true;
        final long time = System.currentTimeMillis();
        if (Controller.getInstance().lazyServer() && (noTaskUntil < time)) {
            noTaskUntil = time + minTaskIntervall;
            Set<Feed> feedset = getFeeds(true);
            Iterator<Feed> feeds = feedset.iterator();
            while (feeds.hasNext()) {
                final Feed f = feeds.next();
                ret = ret && makeLazyServerWork(f.id);
            }
        }
        return ret;
    }
    
    /**
     * Retrieves the specified articles.
     * 
     * @param articles
     *            container for retrieved articles
     * @param id
     *            the id of the feed/category
     * @param limit
     *            the maximum number of articles to be fetched
     * @param viewMode
     *            indicates wether only unread articles should be included (Possible values: all_articles, unread,
     *            adaptive, marked, updated)
     * @param isCategory
     *            indicates if we are dealing with a category or a feed
     * @param sinceId
     *            the first ArticleId which is to be retrieved.
     * @param search
     *            search query
     * @param skipProperties
     *            set of article fields, which should not be parsed (may be {@code null})
     * @param filter
     *            filter for articles, defining which articles should be omitted while parsing (may be {@code null})
     */
    public void getHeadlines(final Set<Article> articles, Integer id, int limit, String viewMode, boolean isCategory, Integer sinceId, String search, Set<Article.ArticleField> skipProperties, IArticleOmitter filter) {
        long time = System.currentTimeMillis();
        int offset = 0;
        int count = 0;
        int maxSize = articles.size() + limit;
        
        if (!sessionAlive())
            return;
        
        int limitParam = Math.min((apiLevel < 6) ? PARAM_LIMIT_API_5 : PARAM_LIMIT_MAX_VALUE, limit);
        
        makeLazyServerWork(id);
        
        while (articles.size() < maxSize) {
            
            Map<String, String> params = new HashMap<String, String>();
            params.put(PARAM_OP, VALUE_GET_HEADLINES);
            params.put(PARAM_FEED_ID, id + "");
            params.put(PARAM_LIMIT, limitParam + "");
            params.put(PARAM_SKIP, offset + "");
            params.put(PARAM_VIEWMODE, viewMode);
            // params.put(PARAM_ORDERBY, "feed_dates");
            
            if (skipProperties == null || !skipProperties.contains(Article.ArticleField.content))
                params.put(PARAM_SHOW_CONTENT, "1");
            
            if (skipProperties == null || !skipProperties.contains(Article.ArticleField.attachments))
                params.put(PARAM_INC_ATTACHMENTS, "1");
            
            params.put(PARAM_IS_CAT, (isCategory ? "1" : "0"));
            
            if (sinceId > 0)
                params.put(PARAM_SINCE_ID, sinceId + "");
            
            if (search != null)
                params.put(PARAM_SEARCH, search);
            
            JsonReader reader = null;
            try {
                reader = prepareReader(params);
                
                if (hasLastError)
                    return;
                
                if (reader == null)
                    continue;
                
                count = parseArticleArray(articles, reader, (!isCategory && id < -10 ? id : -1), skipProperties, filter);
                
                if (count < limitParam)
                    break;
                else
                    offset += count;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                    }
                }
            }
        }
        
        Log.d(TAG, "getHeadlines: " + (System.currentTimeMillis() - time) + "ms");
    }
    
    /**
     * Marks the given list of article-Ids as read/unread depending on int articleState.
     * 
     * @param articlesIds
     *            a list of article-ids.
     * @param articleState
     *            the new state of the article (0 -> mark as read; 1 -> mark as unread).
     */
    public boolean setArticleRead(Set<Integer> articlesIds, int articleState) {
        boolean ret = true;
        if (articlesIds.isEmpty())
            return ret;
        
        for (String idList : StringSupport.convertListToString(articlesIds, MAX_ID_LIST_LENGTH)) {
            Map<String, String> params = new HashMap<String, String>();
            params.put(PARAM_OP, VALUE_UPDATE_ARTICLE);
            params.put(PARAM_ARTICLE_IDS, idList);
            params.put(PARAM_MODE, articleState + "");
            params.put(PARAM_FIELD, "2");
            ret = ret && doRequestNoAnswer(params);
        }
        return ret;
    }
    
    /**
     * Marks the given Article as "starred"/"not starred" depending on int articleState.
     * 
     * @param ids
     *            a list of article-ids.
     * @param articleState
     *            the new state of the article (0 -> not starred; 1 -> starred; 2 -> toggle).
     * @return true if the operation succeeded.
     */
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
    
    /**
     * Marks the given Articles as "published"/"not published" depending on articleState.
     * 
     * @param ids
     *            a list of article-ids with corresponding notes (may be null).
     * @param articleState
     *            the new state of the articles (0 -> not published; 1 -> published; 2 -> toggle).
     * @return true if the operation succeeded.
     */
    public boolean setArticlePublished(Map<Integer, String> ids, int articleState) {
        boolean ret = true;
        if (ids.size() == 0)
            return ret;
        
        for (String idList : StringSupport.convertListToString(ids.keySet(), MAX_ID_LIST_LENGTH)) {
            Map<String, String> params = new HashMap<String, String>();
            params.put(PARAM_OP, VALUE_UPDATE_ARTICLE);
            params.put(PARAM_ARTICLE_IDS, idList);
            params.put(PARAM_MODE, articleState + "");
            params.put(PARAM_FIELD, "1");
            ret = ret && doRequestNoAnswer(params);
            
            // Add a note to the article(s)
            
            for (Integer id : ids.keySet()) {
                String note = ids.get(id);
                if (note == null || note.equals(""))
                    continue;
                
                params.put(PARAM_FIELD, "3"); // Field 3 is the "Add note" field
                params.put(PARAM_DATA, note);
                ret = ret && doRequestNoAnswer(params);
            }
        }
        
        return ret;
    }
    
    /**
     * Marks a feed or a category with all its feeds as read.
     * 
     * @param id
     *            the feed-id/category-id.
     * @param isCategory
     *            indicates whether id refers to a feed or a category.
     * @return true if the operation succeeded.
     */
    public boolean setRead(int id, boolean isCategory) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_CATCHUP);
        params.put(PARAM_FEED_ID, id + "");
        params.put(PARAM_IS_CAT, (isCategory ? "1" : "0"));
        return doRequestNoAnswer(params);
    }
    
    public boolean feedUnsubscribe(int feed_id) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_FEED_UNSUBSCRIBE);
        params.put(PARAM_FEED_ID, feed_id + "");
        return doRequestNoAnswer(params);
    }
    
    /**
     * Returns the value for the given preference-name as a string.
     * 
     * @param pref
     *            the preferences name
     * @return the value of the preference or null if it ist not set or unknown
     */
    public String getPref(String pref) {
        if (!sessionAlive())
            return null;
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_GET_PREF);
        params.put(PARAM_PREF, pref);
        
        try {
            String ret = readResult(params, false);
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Returns the version of the server-installation as integer (version-string without dots)
     * 
     * @return the version
     */
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
    
    public boolean setArticleLabel(Set<Integer> articleIds, int labelId, boolean assign) {
        boolean ret = true;
        if (articleIds.size() == 0)
            return ret;
        
        for (String idList : StringSupport.convertListToString(articleIds, MAX_ID_LIST_LENGTH)) {
            Map<String, String> params = new HashMap<String, String>();
            params.put(PARAM_OP, VALUE_SET_LABELS);
            params.put(PARAM_ARTICLE_IDS, idList);
            params.put(VALUE_LABEL_ID, labelId + "");
            params.put(VALUE_ASSIGN, (assign ? "1" : "0"));
            ret = ret && doRequestNoAnswer(params);
        }
        
        return ret;
    }
    
    public boolean shareToPublished(String title, String url, String content) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_SHARE_TO_PUBLISHED);
        params.put(TITLE, title);
        params.put(URL_SHARE, url);
        params.put(CONTENT, content);
        return doRequestNoAnswer(params);
    }
    
    public class SubscriptionResponse {
        public int code = -1;
        public String message = null;
    }
    
    public SubscriptionResponse feedSubscribe(String feed_url, int category_id) {
        SubscriptionResponse ret = new SubscriptionResponse();
        if (!sessionAlive())
            return ret;
        
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_OP, VALUE_FEED_SUBSCRIBE);
        params.put(PARAM_FEED_URL, feed_url);
        params.put(PARAM_CATEGORY_ID, category_id + "");
        
        String code = "";
        String message = null;
        JsonReader reader = null;
        try {
            reader = prepareReader(params);
            if (reader == null)
                return ret;
            
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                
                if (name.equals("code")) {
                    code = reader.nextString();
                } else if (name.equals("message")) {
                    message = reader.nextString();
                } else {
                    reader.skipValue();
                }
            }
            
            if (!code.contains(UNKNOWN_METHOD)) {
                ret.code = Integer.parseInt(code);
                ret.message = message;
            }
            
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
    
    /**
     * Returns true if there was an error.
     * 
     * @return true if there was an error.
     */
    public boolean hasLastError() {
        return hasLastError;
    }
    
    /**
     * Returns the last error-message and resets the error-state of the connector.
     * 
     * @return a string with the last error-message.
     */
    public String pullLastError() {
        String ret = new String(lastError);
        lastError = "";
        hasLastError = false;
        return ret;
    }
    
    protected static String formatException(Exception e) {
        return e.getMessage() + (e.getCause() != null ? "(" + e.getCause() + ")" : "");
    }
    
}
