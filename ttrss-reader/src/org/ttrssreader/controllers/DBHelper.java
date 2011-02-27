/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.ttrssreader.model.pojos.ArticleItem;
import org.ttrssreader.model.pojos.CategoryItem;
import org.ttrssreader.model.pojos.FeedItem;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class DBHelper {
    
    private static final Long mutex = new Long(1);
    private static DBHelper instance = null;
    private boolean initialized = false;
    
    private static final String DATABASE_NAME = "ttrss.db";
    private static final int DATABASE_VERSION = 47;
    
    public static final String TABLE_CATEGORIES = "categories";
    public static final String TABLE_FEEDS = "feeds";
    public static final String TABLE_ARTICLES = "articles";
    public static final String TABLE_MARK = "marked";
    
    public static final String MARK_READ = "isUnread";
    public static final String MARK_STAR = "isStarred";
    public static final String MARK_PUBLISH = "isPublished";
    
    // private static final int TYPE_CATEGORY = 1;
    // private static final int TYPE_FEED = 2;
    // private static final int TYPE_ARTICLE = 3;
    
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
        + " (id, feedId, title, isUnread, articleUrl, articleCommentUrl, updateDate, content, attachments, isStarred, isPublished, cachedImages)" 
        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    // @formatter:on
    
    private Context context;
    public SQLiteDatabase db;
    
    private SQLiteStatement insertCategorie;
    private SQLiteStatement insertFeed;
    private SQLiteStatement insertArticle;
    
    public DBHelper() {
        context = null;
        
        db = null;
        
        insertCategorie = null;
        insertFeed = null;
        insertArticle = null;
    }
    
    private synchronized boolean initializeController() {
        
        if (context == null) {
            Log.e(Utils.TAG, "Can't handle internal DB without Context-Object.");
            return false;
        }
        
        OpenHelper openHelper = new OpenHelper(context);
        db = openHelper.getWritableDatabase();
        db.setLockingEnabled(false);
        
        insertCategorie = db.compileStatement(INSERT_CATEGORY);
        insertFeed = db.compileStatement(INSERT_FEEDS);
        insertArticle = db.compileStatement(INSERT_ARTICLES);
        return true;
    }
    
    public synchronized void checkAndInitializeDB(Context context) {
        this.context = context;
        
        if (!initialized) {
            initialized = initializeController();
        } else if (db == null || !db.isOpen()) {
            initialized = initializeController();
        }
    }
    
    public static DBHelper getInstance() {
        if (instance == null) {
            synchronized (mutex) {
                if (instance == null) {
                    instance = new DBHelper();
                }
            }
        }
        return instance;
    }
    
    private boolean isDBAvailable() {
        if (db != null && db.isOpen()) {
            return true;
        } else if (db != null) {
            OpenHelper openHelper = new OpenHelper(context);
            db = openHelper.getWritableDatabase();
            initialized = db.isOpen();
            return initialized;
        } else {
            Log.w(Utils.TAG, "Controller not initialized, trying to do that now...");
            initialized = initializeController();
            return initialized;
        }
    }
    
    private static class OpenHelper extends SQLiteOpenHelper {
        
        OpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        
        /**
         * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
         */
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
                    + " updateDate INTEGER,"
                    + " content TEXT,"
                    + " attachments TEXT,"
                    + " isStarred INTEGER,"
                    + " isPublished INTEGER,"
                    + " cachedImages INTEGER)");
            // @formatter:on
        }
        
        /**
         * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            boolean didUpgrade = false;
            
            if (oldVersion < 40) {
                String sql = "ALTER TABLE " + TABLE_ARTICLES + " ADD COLUMN isStarred INTEGER";
                
                Log.w(Utils.TAG, String.format("Upgrading database from %s to 40.", oldVersion));
                Log.w(Utils.TAG, String.format(" (Executing: %s", sql));
                
                db.execSQL(sql);
                didUpgrade = true;
            }
            
            if (oldVersion < 42) {
                String sql = "ALTER TABLE " + TABLE_ARTICLES + " ADD COLUMN isPublished INTEGER";
                
                Log.w(Utils.TAG, String.format("Upgrading database from %s to 42.", oldVersion));
                Log.w(Utils.TAG, String.format(" (Executing: %s", sql));
                
                db.execSQL(sql);
                didUpgrade = true;
            }
            
            if (oldVersion < 45) {
                // @formatter:off
                String sql = "CREATE TABLE "
                    + TABLE_MARK
                    + " (id INTEGER,"
                    + " type INTEGER,"
                    + " " + MARK_READ + " INTEGER,"
                    + " " + MARK_STAR + " INTEGER,"
                    + " " + MARK_PUBLISH + " INTEGER,"
                    + " PRIMARY KEY(id, type))";
                // @formatter:on
                
                Log.w(Utils.TAG, String.format("Upgrading database from %s to 45.", oldVersion));
                Log.w(Utils.TAG, String.format(" (Executing: %s", sql));
                
                db.execSQL(sql);
                didUpgrade = true;
            }
            
            if (oldVersion < 46) {
                
                // @formatter:off
                String sql = "DROP TABLE "
                    + TABLE_MARK;
                String sql2 = "CREATE TABLE "
                    + TABLE_MARK
                    + " (id INTEGER PRIMARY KEY,"
                    + " " + MARK_READ + " INTEGER,"
                    + " " + MARK_STAR + " INTEGER,"
                    + " " + MARK_PUBLISH + " INTEGER)";
                // @formatter:on
                
                Log.w(Utils.TAG, String.format("Upgrading database from %s to 46.", oldVersion));
                Log.w(Utils.TAG, String.format(" (Executing: %s", sql));
                Log.w(Utils.TAG, String.format(" (Executing: %s", sql2));
                
                db.execSQL(sql);
                db.execSQL(sql2);
                didUpgrade = true;
            }
            
            if (oldVersion < 47) {
                String sql = "ALTER TABLE " + TABLE_ARTICLES + " ADD COLUMN cachedImages INTEGER DEFAULT 0";
                
                Log.w(Utils.TAG, String.format("Upgrading database from %s to 45.", oldVersion));
                Log.w(Utils.TAG, String.format(" (Executing: %s", sql));
                
                db.execSQL(sql);
                didUpgrade = true;
            }
            
            if (didUpgrade == false) {
                Log.w(Utils.TAG, "Upgrading database, this will drop tables and recreate.");
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_FEEDS);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_ARTICLES);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_MARK);
                onCreate(db);
            }
            
        }
    }
    
    /**
     * @see android.database.sqlite.SQLiteDatabase#rawQuery(String, String[])
     */
    public Cursor query(String sql, String[] selectionArgs) {
        return db.rawQuery(sql, selectionArgs);
    }
    
    public Cursor queryArticlesForImageCache(boolean onlyUnreadImages) {
        // Add where-clause for only unread articles
        String where = "cachedImages=0";
        if (onlyUnreadImages) {
            where += " AND isUnread>0";
        }
        
        return db.query(DBHelper.TABLE_ARTICLES, new String[] { "content", "attachments" }, where, null, null, null,
                null);
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
    
    public void insertCategories(Set<CategoryItem> set) {
        if (set == null)
            return;
        
        for (CategoryItem c : set) {
            insertCategory(c.id, c.title, c.unread);
        }
    }
    
    private void insertFeed(int id, int categoryId, String title, String url, int unread) {
        if (!isDBAvailable())
            return;
        if (title == null)
            title = "";
        if (url == null)
            url = "";
        
        synchronized (TABLE_FEEDS) {
            insertFeed.bindLong(1, new Integer(id).longValue());
            insertFeed.bindLong(2, new Integer(categoryId).longValue());
            insertFeed.bindString(3, title);
            insertFeed.bindString(4, url);
            insertFeed.bindLong(5, unread);
            insertFeed.execute();
        }
    }
    
    private void insertFeed(FeedItem f) {
        if (f == null)
            return;
        
        insertFeed(f.id, f.categoryId, f.title, f.url, f.unread);
    }
    
    public void insertFeeds(Set<FeedItem> set) {
        if (set == null)
            return;
        
        for (FeedItem f : set) {
            insertFeed(f);
        }
    }
    
    public void insertArticle(int id, int feedId, String title, boolean isUnread, String articleUrl, String articleCommentUrl, Date updateDate, String content, Set<String> attachments, boolean isStarred, boolean isPublished) {
        
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
        
        boolean cachedImages = false;
        ArticleItem a = getArticle(id);
        if (a != null) {
            cachedImages = a.cachedImages;
        }
        
        synchronized (TABLE_ARTICLES) {
            insertArticle.bindLong(1, id);
            insertArticle.bindLong(2, feedId);
            insertArticle.bindString(3, title);
            insertArticle.bindLong(4, (isUnread ? 1 : 0));
            insertArticle.bindString(5, articleUrl);
            insertArticle.bindString(6, articleCommentUrl);
            insertArticle.bindLong(7, updateDate.getTime());
            insertArticle.bindString(8, content);
            insertArticle.bindString(9, parseAttachmentSet(attachments));
            insertArticle.bindLong(10, (isStarred ? 1 : 0));
            insertArticle.bindLong(11, (isPublished ? 1 : 0));
            insertArticle.bindLong(12, (cachedImages ? 1 : 0));
            insertArticle.executeInsert();
        }
        
    }
    
    // *******| UPDATE |*******************************************************************
    
    public void markCategoryRead(CategoryItem c, boolean recursive) {
        if (isDBAvailable()) {
            updateCategoryUnreadCount(c.id, 0);
            
            if (recursive) {
                for (FeedItem f : getFeeds(c.id)) {
                    markFeedRead(f, recursive);
                }
            }
        }
    }
    
    public void markFeedRead(FeedItem f, boolean recursive) {
        if (isDBAvailable()) {
            updateFeedUnreadCount(f.id, 0);
            
            if (recursive) {
                ContentValues cv = new ContentValues();
                cv.put("isUnread", 0);
                synchronized (TABLE_ARTICLES) {
                    db.update(TABLE_ARTICLES, cv, "feedId=" + f.id, null);
                }
            }
        }
    }
    
    public void markArticlesRead(Set<Integer> iDlist, int articleState) {
        if (isDBAvailable()) {
            for (Integer id : iDlist) {
                boolean isUnread = articleState == 1 ? true : false;
                updateArticleUnread(id, isUnread);
            }
        }
    }
    
    private void markArticle(int id, String mark, int isMarked) {
        if (isDBAvailable()) {
            synchronized (TABLE_MARK) {
                /*
                 * First update, then insert. If row is already there it gets updated and second
                 * call ignores it, if it is not there yet the second call inserts it.
                 * TODO: Is there a better way to achieve this?
                 */
                String sql;
                sql = String.format("UPDATE %s SET %s=%s WHERE id=%s", TABLE_MARK, mark, isMarked, id);
                db.execSQL(sql);
                
                sql = String
                        .format("INSERT OR IGNORE INTO %s (id, %s) VALUES (%s, %s)", TABLE_MARK, mark, id, isMarked);
                db.execSQL(sql);
            }
        }
    }
    
    public void markArticles(Set<Integer> iDlist, String mark, int state) {
        if (isDBAvailable()) {
            for (Integer id : iDlist) {
                markArticle(id, mark, state);
            }
        }
    }
    
    public void markArticlesReadCategory(int id) {
        for (FeedItem f : getFeeds(id)) {
            markArticlesReadFeed(f.id);
        }
    }
    
    public void markArticlesReadFeed(int id) {
        if (!isDBAvailable())
            return;
        
        Set<Integer> set = new HashSet<Integer>();
        Cursor c = null;
        try {
            c = db.query(TABLE_ARTICLES, new String[] { "id" }, "feedId=" + id + " AND isUnread=1", null, null, null,
                    null, null);
            
            if (c.isBeforeFirst()) {
                if (!c.moveToFirst()) {
                    return;
                }
            }
            
            while (!c.isAfterLast()) {
                set.add(c.getInt(0));
                c.move(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        
        markArticles(set, MARK_READ, 0);
    }
    
    public void updateCategoryUnreadCount(int id, int count) {
        if (isDBAvailable() && count >= 0) {
            ContentValues cv = new ContentValues();
            cv.put("unread", count);
            
            synchronized (TABLE_CATEGORIES) {
                db.update(TABLE_CATEGORIES, cv, "id=" + id, null);
            }
        }
    }
    
    public void updateCategoryDeltaUnreadCount(int id, int delta) {
        CategoryItem c = getCategory(id);
        int count = c.unread;
        count += delta;
        updateCategoryUnreadCount(id, count);
    }
    
    public void updateFeedUnreadCount(int id, int count) {
        if (isDBAvailable() && count >= 0) {
            ContentValues cv = new ContentValues();
            cv.put("unread", count);
            
            synchronized (TABLE_FEEDS) {
                db.update(TABLE_FEEDS, cv, "id=" + id, null);
            }
        }
    }
    
    public void updateFeedDeltaUnreadCount(int id, int delta) {
        FeedItem f = getFeed(id);
        int count = f.unread;
        count += delta;
        updateFeedUnreadCount(id, count);
    }
    
    private void updateArticleUnread(int id, boolean isUnread) {
        if (isDBAvailable()) {
            ContentValues cv = new ContentValues();
            
            cv.put("isUnread", isUnread);
            synchronized (TABLE_ARTICLES) {
                db.update(TABLE_ARTICLES, cv, "id=" + id, null);
            }
        }
    }
    
    public void updateArticleStarred(int id, boolean isStarred) {
        if (isDBAvailable()) {
            ContentValues cv = new ContentValues();
            
            cv.put("isStarred", isStarred);
            synchronized (TABLE_ARTICLES) {
                db.update(TABLE_ARTICLES, cv, "id=" + id, null);
            }
        }
    }
    
    public void updateArticlePublished(int id, boolean isPublished) {
        if (isDBAvailable()) {
            ContentValues cv = new ContentValues();
            
            cv.put("isPublished", isPublished);
            synchronized (TABLE_ARTICLES) {
                db.update(TABLE_ARTICLES, cv, "id=" + id, null);
            }
        }
    }
    
    public void updateAllArticlesCachedImages(boolean isCachedImages) {
        if (isDBAvailable()) {
            ContentValues cv = new ContentValues();
            
            cv.put("cachedImages", true);
            synchronized (TABLE_ARTICLES) {
                db.update(TABLE_ARTICLES, cv, "cachedImages=0", null); // Only apply if not yet applied
            }
        }
    }
    
    public void deleteCategories(boolean withVirtualCategories) {
        if (isDBAvailable()) {
            String wherePart = "";
            if (!withVirtualCategories) {
                wherePart = "id > 0";
            }
            synchronized (TABLE_CATEGORIES) {
                db.delete(TABLE_CATEGORIES, wherePart, null);
            }
        }
    }
    
    public void deleteFeeds() {
        if (isDBAvailable()) {
            synchronized (TABLE_FEEDS) {
                db.delete(TABLE_FEEDS, null, null);
            }
        }
    }
    
    public void purgeArticlesNumber(int number) {
        if (isDBAvailable()) {
            String idList = "select id from " + TABLE_ARTICLES + " ORDER BY updateDate DESC LIMIT -1 OFFSET " + number;
            synchronized (TABLE_ARTICLES) {
                db.delete(TABLE_ARTICLES, "id in(" + idList + ")", null);
            }
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
    
    /**
     * 0 - Uncategorized
     * -1 - Special (e.g. Starred, Published, Archived, etc.) <- these are categories here o.O
     * -2 - Labels
     * -3 - All feeds, excluding virtual feeds (e.g. Labels and such)
     * -4 - All feeds, including virtual feeds
     * 
     * @param categoryId
     * @return
     */
    public Set<FeedItem> getFeeds(int categoryId) {
        Set<FeedItem> ret = new LinkedHashSet<FeedItem>();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            String where = "categoryId=" + categoryId;
            if (categoryId < 0) {
                where = null;
            }
            
            c = db.query(TABLE_FEEDS, null, where, null, null, null, "upper(title) ASC");
            
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
    
    public Set<CategoryItem> getVirtualCategories() {
        Set<CategoryItem> ret = new LinkedHashSet<CategoryItem>();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = db.query(TABLE_CATEGORIES, null, "id<1", null, null, null, "id ASC");
        try {
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
    
    public int getUnreadCount(int id, boolean isCat) {
        int ret = 0;
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db.query(isCat ? TABLE_CATEGORIES : TABLE_FEEDS, new String[] { "unread" }, "id=" + id, null, null,
                    null, null, null);
            
            if (c.moveToFirst()) {
                ret = c.getInt(0);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        
        return ret;
    }
    
    public Set<Integer> getMarked(String mark, int status) {
        Set<Integer> ret = new HashSet<Integer>();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db.query(TABLE_MARK, new String[] { "id" }, mark + "=" + status, null, null, null, null, null);
            
            if (!c.moveToFirst())
                return ret;
            
            while (!c.isAfterLast()) {
                ret.add(c.getInt(0));
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
    
    public void setMarked(Set<Integer> ids, String mark) {
        if (!isDBAvailable())
            return;
        
        try {
            for (String idList : StringSupport.convertListToString(ids)) {
                ContentValues cv = new ContentValues();
                cv.putNull(mark);
                db.update(TABLE_MARK, cv, "id in (" + idList + ")", null);
                db.delete(TABLE_MARK, "isUnread is null AND isStarred is null AND isPublished is null", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // *******************************************
    
    private static ArticleItem handleArticleCursor(Cursor c) {
        ArticleItem ret = null;
        
        if (c.isBeforeFirst()) {
            if (!c.moveToFirst()) {
                return ret;
            }
        }
        
        // @formatter:off
        ret = new ArticleItem(
                c.getInt(0),                        // id
                c.getInt(1),                        // feedId
                c.getString(2),                     // title
                (c.getInt(3) != 0),                 // isUnread
                c.getString(4),                     // updateDate
                c.getString(5),                     // content
                new Date(c.getLong(6)),             // articleUrl
                c.getString(7),                     // articleCommentUrl
                parseAttachments(c.getString(8)),   // attachments
                (c.getInt(9) != 0),                 // isStarred
                (c.getInt(10) != 0)                 // isPublished
        );
        ret.cachedImages = (c.getInt(11) != 0);
        // @formatter:on
        
        return ret;
    }
    
    private static FeedItem handleFeedCursor(Cursor c) {
        FeedItem ret = null;
        
        if (c.isBeforeFirst()) {
            if (!c.moveToFirst()) {
                return ret;
            }
        }
        
        // @formatter:off
        ret = new FeedItem(
                c.getInt(0),            // id
                c.getInt(1),            // categoryId
                c.getString(2),         // title
                c.getString(3),         // url
                c.getInt(4));           // unread
        // @formatter:on
        
        return ret;
    }
    
    private static CategoryItem handleCategoryCursor(Cursor c) {
        CategoryItem ret = null;
        
        if (c.isBeforeFirst()) {
            if (!c.moveToFirst()) {
                return ret;
            }
        }
        
        // @formatter:off
        ret = new CategoryItem(
                c.getInt(0),                // id
                c.getString(1),             // title
                c.getInt(2));               // unread
        // @formatter:on
        
        return ret;
    }
    
    private static Set<String> parseAttachments(String att) {
        Set<String> ret = new LinkedHashSet<String>();
        if (att == null)
            return ret;
        
        for (String s : att.split(";")) {
            ret.add(s);
        }
        
        return ret;
    }
    
    private static String parseAttachmentSet(Set<String> att) {
        if (att == null)
            return "";
        
        StringBuilder ret = new StringBuilder();
        for (String s : att) {
            ret.append(s + ";");
        }
        if (att.size() > 0)
            ret.deleteCharAt(ret.length() - 1);
        
        return ret.toString();
    }
    
}
