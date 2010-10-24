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

package org.ttrssreader.controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.utils.Utils;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class DBHelper {
    
    private static DBHelper mInstance = null;
    private boolean mIsDBInitialized = false;
    
    private static final String DATABASE_NAME = "ttrss.db";
    private static final int DATABASE_VERSION = 27;
    
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_FEEDS = "feeds";
    private static final String TABLE_ARTICLES = "articles";
    
    // @formatter:off
    private static final String INSERT_CATEGORY = 
        "REPLACE INTO "
        + TABLE_CATEGORIES
        + " (id, title, unread)"
        + " VALUES (?, ?, ?)";
    
    private static final String INSERT_FEEDS = 
        "REPLACE INTO "
        + TABLE_FEEDS
        + " (id, categoryId, title, url, unread)"
        + " VALUES (?, ?, ?, ?, ?)";
    
    private static final String INSERT_ARTICLES = 
        "REPLACE INTO "
        + TABLE_ARTICLES
        + " (id, feedId, title, isUnread, articleUrl, articleCommentUrl, updateDate, content, attachments)" 
        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String UPDATE_ARTICLES =
        "UPDATE "
        + TABLE_ARTICLES
        + " SET title=?,"
        + "  articleUrl=?,"
        + "  articleCommentUrl=?,"
        + "  updateDate=?"
        + " WHERE id=?"
        + " AND feedId=?";
    // @formatter:on
    
    private SQLiteDatabase db;
    
    private SQLiteStatement insertCategorie;
    private SQLiteStatement insertFeed;
    private SQLiteStatement insertArticle;
    private SQLiteStatement updateArticle;
    
    boolean externalDBState;
    private Context context;
    
    public DBHelper() {
        context = null;
        
        db = null;
        
        insertCategorie = null;
        insertFeed = null;
        insertArticle = null;
        updateArticle = null;
    }
    
    private synchronized boolean initializeController() {
        
        if (context == null) {
            Log.e(Utils.TAG, "Can't handle internal DB without Context-Object.");
            return false;
        }
        
        handleDBUpdate();
        
        OpenHelper openHelper = new OpenHelper(context);
        db = openHelper.getWritableDatabase();
        db.setLockingEnabled(false);
        
        insertCategorie = db.compileStatement(INSERT_CATEGORY);
        insertFeed = db.compileStatement(INSERT_FEEDS);
        insertArticle = db.compileStatement(INSERT_ARTICLES);
        updateArticle = db.compileStatement(UPDATE_ARTICLES);
        return true;
    }
    
    public synchronized void checkAndInitializeDB(Context context) {
        this.context = context;
        
        if (!mIsDBInitialized) {
            mIsDBInitialized = initializeController();
        } else if (db == null || !db.isOpen()) {
            mIsDBInitialized = initializeController();
        }
    }
    
    public static DBHelper getInstance() {
        if (mInstance == null) {
            mInstance = new DBHelper();
        }
        return mInstance;
    }
    
    public void destroy() {
        db.close();
        mInstance = null;
        mIsDBInitialized = false;
    }
    
    private void handleDBUpdate() {
        if (DATABASE_VERSION > Controller.getInstance().getDatabaseVersion()) {
            Log.i(Utils.TAG, "Database-Version: " + Controller.getInstance().getDatabaseVersion() + " (Internal: "
                    + DATABASE_VERSION + ")");
            
            OpenHelper openHelper = new OpenHelper(context);
            db = openHelper.getWritableDatabase();
            
            dropDB();
            
            db.close();
        }
        
        Controller.getInstance().setDatabaseVersion(DATABASE_VERSION);
    }
    
    private void dropDB() {
        if (context.getDatabasePath(DATABASE_NAME).delete()) {
            Log.d(Utils.TAG, "dropDB(): database deleted.");
        } else {
            Log.d(Utils.TAG, "dropDB(): database NOT deleted.");
        }
    }
    
    private boolean isDBAvailable() {
        if (db != null && db.isOpen()) {
            return true;
        } else {
            Log.w(Utils.TAG, "Controller not initialized, trying to do that now...");
            mIsDBInitialized = initializeController();
            return mIsDBInitialized;
        }
    }
    
    private static class OpenHelper extends SQLiteOpenHelper {
        
        OpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            // @formatter:off
            db.execSQL(
                    "CREATE TABLE "
                    + TABLE_CATEGORIES 
                    + " (id INTEGER PRIMARY KEY," 
                    + " title TEXT," 
                    + " unread INTEGER)");
            
            db.execSQL(
                    "CREATE TABLE "
                    + TABLE_FEEDS
                    + " (id INTEGER PRIMARY KEY," 
                    + " categoryId INTEGER," 
                    + " title TEXT," 
                    + " url TEXT," 
                    + " unread INTEGER)");
            
            db.execSQL(
                    "CREATE TABLE "
                    + TABLE_ARTICLES
                    + " (id INTEGER PRIMARY KEY," 
                    + " feedId INTEGER," 
                    + " title TEXT," 
                    + " isUnread INTEGER," 
                    + " articleUrl TEXT," 
                    + " articleCommentUrl TEXT," 
                    + " updateDate INTEGER, "
                    + " content TEXT, "
                    + " attachments TEXT)");
            // @formatter:on
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w("Example", "Upgrading database, this will drop tables and recreate.");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FEEDS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ARTICLES);
            onCreate(db);
        }
    }
    
    // *******| INSERT |*******************************************************************
    
    private void insertCategory(int id, String title, int unread) {
        if (!isDBAvailable())
            return;
        
        if (title == null)
            title = "";
        
        synchronized (TABLE_CATEGORIES) {
            insertCategorie.bindLong(1, id);
            insertCategorie.bindString(2, title);
            insertCategorie.bindLong(3, unread);
            insertCategorie.execute();
        }
    }
    
    public void insertCategory(CategoryItem c) {
        if (c == null)
            return;
        
        insertCategory(c.getId(), c.getTitle(), c.getUnread());
    }
    
    public void insertCategories(Set<CategoryItem> set) {
        if (set == null)
            return;
        
        for (CategoryItem c : set) {
            insertCategory(c.getId(), c.getTitle(), c.getUnread());
        }
    }
    
    private void insertFeed(int feedId, int categoryId, String title, String url, int unread) {
        if (!isDBAvailable())
            return;
        
        if (title == null)
            title = "";
        if (url == null)
            url = "";
        
        synchronized (TABLE_FEEDS) {
            insertFeed.bindLong(1, new Integer(feedId).longValue());
            insertFeed.bindLong(2, new Integer(categoryId).longValue());
            insertFeed.bindString(3, title);
            insertFeed.bindString(4, url);
            insertFeed.bindLong(5, unread);
            insertFeed.execute();
        }
    }
    
    public void insertFeed(FeedItem f) {
        if (f == null)
            return;
        
        insertFeed(f.getId(), f.getCategoryId(), f.getTitle(), f.getUrl(), f.getUnread());
    }
    
    public void insertFeeds(Set<FeedItem> set) {
        if (set == null)
            return;
        
        for (FeedItem f : set) {
            insertFeed(f);
        }
    }
    
    private void insertArticle(int articleId, int feedId, String title, boolean isUnread, String content, String articleUrl, String articleCommentUrl, Date updateDate, Set<String> attachments) {
        
        if (!isDBAvailable())
            return;
        
        if (title == null)
            title = "";
        if (content == null)
            content = "";
        if (articleUrl == null)
            articleUrl = "";
        if (articleCommentUrl == null)
            articleCommentUrl = "";
        if (updateDate == null)
            updateDate = new Date(System.currentTimeMillis());
        if (attachments == null)
            attachments = new LinkedHashSet<String>();
        
        String att = parseAttachmentSet(attachments);
        
        synchronized (TABLE_ARTICLES) {
            insertArticle.bindLong(1, articleId);
            insertArticle.bindLong(2, feedId);
            insertArticle.bindString(3, title);
            insertArticle.bindLong(4, (isUnread ? 1 : 0));
            insertArticle.bindString(5, articleUrl);
            insertArticle.bindString(6, articleCommentUrl);
            insertArticle.bindLong(7, updateDate.getTime());
            insertArticle.bindString(8, content);
            insertArticle.bindString(9, att);
            insertArticle.executeInsert();
        }
        
    }
    
    public void insertArticle(ArticleItem a, int number) {
        if (a == null)
            return;
        
        insertArticleInternal(a);
        purgeArticlesNumber(number);
    }
    
    private void insertArticleInternal(ArticleItem a) {
        insertArticle(a.getId(), a.getFeedId(), a.getTitle(), a.isUnread(), a.getContent(), a.getArticleUrl(),
                a.getArticleCommentUrl(), a.getUpdateDate(), a.getAttachments());
    }
    
    public void insertArticles(Set<ArticleItem> list, int number) {
        if (list == null)
            return;
        
        insertArticlesInternal(list);
        purgeArticlesNumber(number);
    }
    
    private void insertArticlesInternal(Set<ArticleItem> set) {
        if (!isDBAvailable())
            return;
        if (set == null)
            return;
        
        synchronized (TABLE_ARTICLES) {
            db.beginTransaction();
            try {
                for (ArticleItem a : set) {
                    insertArticleInternal(a);
                }
                db.setTransactionSuccessful();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        }
    }
    
    // *******| UPDATE |*******************************************************************
    
    public void markCategoryRead(CategoryItem c, boolean recursive) {
        updateCategoryUnreadCount(c.getId(), 0);
        
        if (recursive) {
            for (FeedItem f : getFeeds(c.getId())) {
                markFeedRead(f, recursive);
            }
        }
    }
    
    public void markFeedRead(FeedItem f, boolean recursive) {
        if (!isDBAvailable())
            return;
        
        updateFeedUnreadCount(f.getId(), f.getCategoryId(), 0);
        
        if (recursive) {
            ContentValues cv = new ContentValues();
            cv.put("isUnread", 0);
            
            synchronized (TABLE_ARTICLES) {
                db.update(TABLE_ARTICLES, cv, "feedId=?", new String[] { f.getId() + "" });
            }
        }
    }
    
    public void markArticlesRead(Set<Integer> iDlist, int articleState) {
        if (!isDBAvailable())
            return;
        
        for (Integer id : iDlist) {
            ContentValues cv = new ContentValues();
            cv.put("isUnread", articleState);
            
            synchronized (TABLE_ARTICLES) {
                db.update(TABLE_ARTICLES, cv, "id=?", new String[] { id + "" });
            }
        }
    }
    
    public void updateCategoryUnreadCount(int id, int count) {
        if (!isDBAvailable())
            return;
        
        ContentValues cv = new ContentValues();
        cv.put("unread", count);
        
        synchronized (TABLE_CATEGORIES) {
            db.update(TABLE_CATEGORIES, cv, "id=?", new String[] { id + "" });
        }
    }
    
    public void updateCategoryDeltaUnreadCount(int id, int delta) {
        CategoryItem c = getCategory(id);
        int count = c.getUnread();
        count += delta;
        
        updateCategoryUnreadCount(id, count);
    }
    
    public void updateFeedUnreadCount(int id, int categoryId, int count) {
        if (!isDBAvailable())
            return;
        
        ContentValues cv = new ContentValues();
        cv.put("unread", count);
        
        synchronized (TABLE_FEEDS) {
            db.update(TABLE_FEEDS, cv, "id=? and categoryId=?", new String[] { id + "", categoryId + "" });
        }
    }
    
    public void updateFeedDeltaUnreadCount(int id, int categoryId, int delta) {
        FeedItem f = getFeed(id);
        int count = f.getUnread();
        count += delta;
        
        updateFeedUnreadCount(id, categoryId, count);
    }
    
    public void updateArticleUnread(int id, int feedId, boolean isUnread) {
        if (!isDBAvailable())
            return;
        
        ContentValues cv = new ContentValues();
        cv.put("isUnread", isUnread);
        
        synchronized (TABLE_ARTICLES) {
            db.update(TABLE_ARTICLES, cv, "id=? and feedId=?", new String[] { id + "", feedId + "" });
        }
    }
    
    public void updateArticleContent(ArticleItem a) {
        if (!isDBAvailable())
            return;
        
        int id = a.getId();
        int feedId = a.getFeedId();
        String content = a.getContent();
        String title = a.getTitle();
        String articleUrl = a.getArticleUrl();
        String articleCommentUrl = a.getArticleCommentUrl();
        Date updateDate = a.getUpdateDate();
        
        if (content == null)
            content = "";
        if (title == null)
            title = "";
        if (articleUrl == null)
            articleUrl = "";
        if (articleCommentUrl == null)
            articleCommentUrl = "";
        if (updateDate == null)
            updateDate = new Date();
        
        String att = parseAttachmentSet(a.getAttachments());
        
        synchronized (TABLE_ARTICLES) {
            updateArticle.bindString(1, title);
            updateArticle.bindString(2, articleUrl);
            updateArticle.bindString(3, articleCommentUrl);
            updateArticle.bindLong(4, updateDate.getTime());
            updateArticle.bindLong(5, new Long(id));
            updateArticle.bindLong(6, new Long(feedId));
            updateArticle.bindString(7, content);
            updateArticle.bindString(8, att);
            updateArticle.execute();
        }
    }
    
    public void deleteCategory(int id) {
        if (!isDBAvailable())
            return;
        
        String[] args = new String[] { id + "" };
        
        synchronized (TABLE_CATEGORIES) {
            db.delete(TABLE_CATEGORIES, "id=?", args);
        }
    }
    
    public void deleteFeed(int id) {
        if (!isDBAvailable())
            return;
        
        String[] args = new String[] { id + "" };
        
        synchronized (TABLE_FEEDS) {
            db.delete(TABLE_FEEDS, "id=?", args);
        }
    }
    
    public void deleteArticle(int id) {
        if (!isDBAvailable())
            return;
        
        String[] args = new String[] { id + "" };
        
        synchronized (TABLE_ARTICLES) {
            db.delete(TABLE_ARTICLES, "id=?", args);
        }
    }
    
    public void deleteCategories(boolean withVirtualCategories) {
        if (!isDBAvailable())
            return;
        
        String wherePart = "";
        if (!withVirtualCategories) {
            wherePart = "id > 0";
        }
        
        synchronized (TABLE_CATEGORIES) {
            db.delete(TABLE_CATEGORIES, wherePart, null);
        }
    }
    
    public void deleteFeeds() {
        if (!isDBAvailable())
            return;
        
        synchronized (TABLE_FEEDS) {
            db.delete(TABLE_FEEDS, null, null);
        }
    }
    
    public void deleteArticles() {
        if (!isDBAvailable())
            return;
        
        synchronized (TABLE_ARTICLES) {
            db.delete(TABLE_ARTICLES, null, null);
        }
    }
    
    public void purgeArticlesDays(Date olderThenThis) {
        if (!isDBAvailable())
            return;
        
        String[] args = new String[] { olderThenThis.getTime() + "" };
        
        synchronized (TABLE_ARTICLES) {
            db.delete(TABLE_ARTICLES, "updateDate<?", args);
        }
    }
    
    public void purgeArticlesNumber(int number) {
        if (!isDBAvailable())
            return;
        
        String[] args = new String[] { "select id from " + TABLE_ARTICLES
                + " ORDER BY updateDate DESC LIMIT -1 OFFSET " + number };
        
        synchronized (TABLE_ARTICLES) {
            db.delete(TABLE_ARTICLES, "id in(?)", args);
        }
    }
    
    // *******| SELECT |*******************************************************************
    
    public ArticleItem getArticle(int id) {
        ArticleItem ret = null;
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db.query(TABLE_ARTICLES, null, "id=" + id, null, null, null, null, null);
            
            while (!c.isAfterLast()) {
                ret = handleArticleCursor(c);
                
                c.move(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        
        return ret;
    }
    
    public FeedItem getFeed(int id) {
        FeedItem ret = new FeedItem();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db.query(TABLE_FEEDS, null, "id=" + id, null, null, null, null, null);
            
            while (!c.isAfterLast()) {
                ret = handleFeedCursor(c);
                
                c.move(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        
        return ret;
    }
    
    public CategoryItem getCategory(int id) {
        CategoryItem ret = new CategoryItem();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db.query(TABLE_CATEGORIES, null, "id=" + id, null, null, null, null, null);
            
            while (!c.isAfterLast()) {
                ret = handleCategoryCursor(c);
                
                c.move(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        
        return ret;
    }
    
    public Set<ArticleItem> getArticles(int feedId, boolean withContent) {
        
        Set<ArticleItem> ret = new LinkedHashSet<ArticleItem>();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db.query(TABLE_ARTICLES, null, "feedId=" + feedId, null, null, null, "updateDate DESC");
            
            while (!c.isAfterLast()) {
                ret.add(handleArticleCursor(c));
                
                c.move(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        
        return ret;
    }
    
    public Set<FeedItem> getFeeds(int categoryId) {
        Set<FeedItem> ret = new LinkedHashSet<FeedItem>();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db.query(TABLE_FEEDS, null, "categoryId=" + categoryId, null, null, null, "title ASC");
            
            while (!c.isAfterLast()) {
                ret.add(handleFeedCursor(c));
                
                c.move(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        
        return ret;
    }
    
    public Map<Integer, Set<ArticleItem>> getArticles(boolean withContent) {
        Map<Integer, Set<ArticleItem>> ret = new HashMap<Integer, Set<ArticleItem>>();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db.query(TABLE_ARTICLES, null, null, null, null, null, "updateDate DESC");
            
            while (!c.isAfterLast()) {
                ArticleItem a = handleArticleCursor(c);
                int feedId = a.getFeedId();
                
                Set<ArticleItem> set;
                if (ret.get(feedId) != null) {
                    set = ret.get(feedId);
                } else {
                    set = new LinkedHashSet<ArticleItem>();
                }
                
                set.add(a);
                ret.put(feedId, set);
                
                c.move(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        
        return ret;
    }
    
    public Map<Integer, Set<FeedItem>> getFeeds() {
        Map<Integer, Set<FeedItem>> ret = new HashMap<Integer, Set<FeedItem>>();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db.query(TABLE_FEEDS, null, null, null, null, null, "title ASC");
            
            while (!c.isAfterLast()) {
                FeedItem fi = handleFeedCursor(c);
                int catId = c.getInt(1);
                
                Set<FeedItem> set;
                if (ret.get(catId) != null) {
                    set = ret.get(catId);
                } else {
                    set = new LinkedHashSet<FeedItem>();
                }
                
                set.add(fi);
                ret.put(catId, set);
                
                c.move(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        
        return ret;
    }
    
    public Set<CategoryItem> getVirtualCategories() {
        Set<CategoryItem> ret = new LinkedHashSet<CategoryItem>();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db.query(TABLE_CATEGORIES, null, "id < 1", null, null, null, "id ASC");
            
            while (!c.isAfterLast()) {
                CategoryItem ci = handleCategoryCursor(c);
                
                ret.add(ci);
                c.move(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        
        return ret;
    }
    
    public Set<CategoryItem> getCategories() {
        Set<CategoryItem> ret = new LinkedHashSet<CategoryItem>();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db.query(TABLE_CATEGORIES, null, "id > 0", null, null, null, "title DESC");
            
            while (!c.isAfterLast()) {
                CategoryItem ci = handleCategoryCursor(c);
                
                ret.add(ci);
                c.move(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        
        return ret;
    }
    
    public Set<CategoryItem> getCategoryCounters() {
        Set<CategoryItem> ret = new LinkedHashSet<CategoryItem>();
        ret.addAll(getVirtualCategories());
        ret.addAll(getCategories());
        return ret;
    }
    
    public Set<FeedItem> getFeedCounters() {
        Set<FeedItem> ret = new LinkedHashSet<FeedItem>();
        
        for (CategoryItem c : getCategories()) {
            ret.addAll(getFeeds(c.getId()));
        }
        
        return ret;
    }
    
    public void setCounters(Map<CategoryItem, Set<FeedItem>> map) {
        for (CategoryItem c : map.keySet()) {
            updateCategoryUnreadCount(c.getId(), c.getUnread());
            
            Set<FeedItem> set = map.get(c);
            if (set == null)
                continue;
            
            for (FeedItem f : set) {
                updateFeedUnreadCount(f.getId(), c.getId(), f.getUnread());
            }
        }
    }
    
    // *******************************************
    
    private ArticleItem handleArticleCursor(Cursor c) {
        ArticleItem ret = null;
        
        if (c.isBeforeFirst()) {
            if (!c.moveToFirst()) {
                return ret;
            }
        }
        
        if (isDBAvailable()) {
            // @formatter:off
            ret = new ArticleItem(
                    c.getInt(0),                        // feedId
                    c.getInt(1),                        // id
                    c.getString(2),                     // title
                    (c.getInt(3) != 0 ? true : false),  // isUnread
                    c.getString(4),                     // updateDate
                    c.getString(5),                     // content
                    new Date(c.getLong(6)),             // articleUrl
                    c.getString(7),                     // articleCommentUrl
                    parseAttachments(c.getString(8))    // attachments
            );
            // @formatter:on
        }
        
        return ret;
    }
    
    private FeedItem handleFeedCursor(Cursor c) {
        FeedItem ret = null;
        
        if (c.isBeforeFirst()) {
            if (!c.moveToFirst()) {
                return ret;
            }
        }
        
        // @formatter:off
        ret = new FeedItem(c.getInt(1), // categoryId
                c.getInt(0),            // id
                c.getString(2),         // title
                c.getString(3),         // url
                c.getInt(4));           // unread
        // @formatter:on
        
        return ret;
    }
    
    private CategoryItem handleCategoryCursor(Cursor c) {
        CategoryItem ret = null;
        
        if (c.isBeforeFirst()) {
            if (!c.moveToFirst()) {
                return ret;
            }
        }
        
        // @formatter:off
        ret = new CategoryItem(c.getInt(0), // id
                c.getString(1),             // title
                c.getInt(2));               // unread
        // @formatter:on
        
        return ret;
    }
    
    private Set<String> parseAttachments(String att) {
        Set<String> ret = new LinkedHashSet<String>();
        if (att == null)
            return ret;
        
        for (String s : att.split(";")) {
            ret.add(s);
        }
        
        return ret;
    }
    
    private String parseAttachmentSet(Set<String> att) {
        if (att == null)
            return "";
        StringBuilder ret = new StringBuilder();
        
        for (String s : att) {
            ret.append(s + ";");
        }
        if (att.size() > 0) {
            ret.deleteCharAt(ret.length() - 1);
        }
        
        return DatabaseUtils.sqlEscapeString(ret.toString());
    }
    
}
