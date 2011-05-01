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

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.utils.FileDateComparator;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.widget.Toast;

public class DBHelper {
    
    private static DBHelper instance = null;
    private boolean initialized = false;
    
    public static final String DATABASE_NAME = "ttrss.db";
    public static final String DATABASE_BACKUP_NAME = "_backup_";
    public static final int DATABASE_VERSION = 48;
    
    public static final String TABLE_CATEGORIES = "categories";
    public static final String TABLE_FEEDS = "feeds";
    public static final String TABLE_ARTICLES = "articles";
    public static final String TABLE_MARK = "marked";
    
    public static final String MARK_READ = "isUnread";
    public static final String MARK_STAR = "isStarred";
    public static final String MARK_PUBLISH = "isPublished";
    
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
    
    // Singleton
    private DBHelper() {
    }
    
    public static DBHelper getInstance() {
        if (instance == null) {
            synchronized (DBHelper.class) {
                if (instance == null) {
                    instance = new DBHelper();
                }
            }
        }
        return instance;
    }
    
    public synchronized void checkAndInitializeDB(final Context context) {
        this.context = context;
        
        if (!initialized) {
            initialized = initializeController();
        } else if (db == null || !db.isOpen()) {
            initialized = initializeController();
        }
        
        if (initialized) {
            Cursor c = null;
            try {
                // Try to access the DB
                c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_FEEDS, null);
                c.getCount();
                if (c.moveToFirst())
                    c.getInt(0);
                
            } catch (Exception e) {
                Toast.makeText(context, "Database was corrupted, creating a new one...", Toast.LENGTH_LONG);
                
                // Close DB
                db.close();
                db = null;
                
                // Delete DB-File
                File f = context.getDatabasePath(DATABASE_NAME);
                f.renameTo(new File(f.getAbsolutePath() + DATABASE_BACKUP_NAME + System.currentTimeMillis()));
                f.setReadable(true, false);
                
                // Check if there are too many old backups
                FilenameFilter fnf = new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        if (filename.contains(DATABASE_BACKUP_NAME))
                            return true;
                        return false;
                    }
                };
                File[] backups = f.getParentFile().listFiles(fnf);
                if (backups != null && backups.length > 3) {
                    // Sort list of files by last access date
                    List<File> list = Arrays.asList(backups);
                    Collections.sort(list, new FileDateComparator());
                    
                    for (int i = list.size(); i > 2; i++) {
                        // Delete all except the 2 newest backups
                        list.get(i).delete();
                    }
                }
                
                // Initialize again...
                initialized = initializeController();
            } finally {
                if (c != null)
                    c.close();
            }
        }
    }
    
    private synchronized boolean initializeController() {
        
        if (context == null) {
            Log.e(Utils.TAG, "Can't handle internal DB without Context-Object.");
            return false;
        }
        
        if (db != null)
            db.close();
        
        OpenHelper openHelper = new OpenHelper(context);
        db = openHelper.getWritableDatabase();
        db.setLockingEnabled(false);
        
        if (insertCategorie == null) {
            insertCategorie = db.compileStatement(INSERT_CATEGORY);
            insertFeed = db.compileStatement(INSERT_FEEDS);
            insertArticle = db.compileStatement(INSERT_ARTICLES);
        }
        
        return true;
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
                    + " cachedImages INTEGER DEFAULT 0)");
            
            db.execSQL("CREATE TABLE "
                    + TABLE_MARK
                    + " (id INTEGER,"
                    + " type INTEGER,"
                    + " " + MARK_READ + " INTEGER,"
                    + " " + MARK_STAR + " INTEGER,"
                    + " " + MARK_PUBLISH + " INTEGER,"
                    + " PRIMARY KEY(id, type))");
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
                String sql = "CREATE TABLE IF NOT EXISTS "
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
                String sql = "DROP TABLE IF EXISTS "
                    + TABLE_MARK;
                String sql2 = "CREATE TABLE IF NOT EXISTS "
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
                
                Log.w(Utils.TAG, String.format("Upgrading database from %s to 47.", oldVersion));
                Log.w(Utils.TAG, String.format(" (Executing: %s", sql));
                
                db.execSQL(sql);
                didUpgrade = true;
            }
            
            if (oldVersion < 48) {
                // @formatter:off
                String sql = "CREATE TABLE IF NOT EXISTS "
                        + TABLE_MARK
                        + " (id INTEGER,"
                        + " type INTEGER,"
                        + " " + MARK_READ + " INTEGER,"
                        + " " + MARK_STAR + " INTEGER,"
                        + " " + MARK_PUBLISH + " INTEGER,"
                        + " PRIMARY KEY(id, type))";
                // @formatter:on
                
                Log.w(Utils.TAG, String.format("Upgrading database from %s to 48.", oldVersion));
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
     * Used by MainAdapter to directly access the DB and get a cursor for the ListViews.
     * 
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
    
    public void insertCategories(Set<Category> set) {
        if (set == null)
            return;
        
        for (Category c : set) {
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
    
    private void insertFeed(Feed f) {
        if (f == null)
            return;
        
        insertFeed(f.id, f.categoryId, f.title, f.url, f.unread);
    }
    
    public void insertFeeds(Set<Feed> set) {
        if (set == null)
            return;
        
        for (Feed f : set) {
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
        Article a = getArticle(id);
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
    
    public void markCategoryRead(Category c, boolean recursive) {
        if (isDBAvailable()) {
            updateCategoryUnreadCount(c.id, 0);
            
            if (recursive) {
                for (Feed f : getFeeds(c.id)) {
                    markFeedRead(f, recursive);
                }
            }
        }
    }
    
    public void markFeedRead(Feed f, boolean recursive) {
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
                 * First update, then insert. If row exists it gets updated and second
                 * call ignores it, else the second call inserts it.
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
        for (Feed f : getFeeds(id)) {
            markArticlesReadFeed(f.id);
        }
    }
    
    public void markArticlesReadFeed(int id) {
        if (!isDBAvailable())
            return;
        
        Set<Integer> set = new HashSet<Integer>();
        Cursor c = null;
        try {
            c = db.query(TABLE_ARTICLES, new String[] { "id" }, "feedId=? AND isUnread=1", new String[] { id + "" },
                    null, null, null, null);
            
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
                db.update(TABLE_CATEGORIES, cv, "id=?", new String[] { id + "" });
            }
        }
    }
    
    public void updateCategoryDeltaUnreadCount(int id, int delta) {
        Category c = getCategory(id);
        int count = c.unread;
        count += delta;
        updateCategoryUnreadCount(id, count);
    }
    
    public void updateFeedUnreadCount(int id, int count) {
        if (isDBAvailable() && count >= 0) {
            ContentValues cv = new ContentValues();
            cv.put("unread", count);
            
            synchronized (TABLE_FEEDS) {
                db.update(TABLE_FEEDS, cv, "id=?", new String[] { id + "" });
            }
        }
    }
    
    public void updateFeedDeltaUnreadCount(int id, int delta) {
        Feed f = getFeed(id);
        int count = f.unread;
        count += delta;
        updateFeedUnreadCount(id, count);
    }
    
    private void updateArticleUnread(int id, boolean isUnread) {
        if (isDBAvailable()) {
            ContentValues cv = new ContentValues();
            
            cv.put("isUnread", isUnread);
            synchronized (TABLE_ARTICLES) {
                db.update(TABLE_ARTICLES, cv, "id=?", new String[] { id + "" });
            }
        }
    }
    
    public void updateArticleStarred(int id, boolean isStarred) {
        if (isDBAvailable()) {
            ContentValues cv = new ContentValues();
            
            cv.put("isStarred", isStarred);
            synchronized (TABLE_ARTICLES) {
                db.update(TABLE_ARTICLES, cv, "id=?", new String[] { id + "" });
            }
        }
    }
    
    public void updateArticlePublished(int id, boolean isPublished) {
        if (isDBAvailable()) {
            ContentValues cv = new ContentValues();
            
            cv.put("isPublished", isPublished);
            synchronized (TABLE_ARTICLES) {
                db.update(TABLE_ARTICLES, cv, "id=?", new String[] { id + "" });
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
    
    public void purgePublishedArticles() {
        if (isDBAvailable()) {
            synchronized (TABLE_ARTICLES) {
                db.delete(TABLE_ARTICLES, "isPublished> 0", null);
            }
        }
    }
    
    public void purgeStarredArticles() {
        if (isDBAvailable()) {
            synchronized (TABLE_ARTICLES) {
                db.delete(TABLE_ARTICLES, "isStarred  > 0", null);
            }
        }
    }
    
    // *******| SELECT |*******************************************************************
    
    // Takes about 2 to 6 ms on Motorola Milestone
    public Article getArticle(int id) {
        Article ret = null;
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db.query(TABLE_ARTICLES, null, "id=?", new String[] { id + "" }, null, null, null, null);
            
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
    
    public Feed getFeed(int id) {
        Feed ret = new Feed();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db.query(TABLE_FEEDS, null, "id=?", new String[] { id + "" }, null, null, null, null);
            
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
    
    public Category getCategory(int id) {
        Category ret = new Category();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db.query(TABLE_CATEGORIES, null, "id=?", new String[] { id + "" }, null, null, null, null);
            
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
    public Set<Feed> getFeeds(int categoryId) {
        Set<Feed> ret = new LinkedHashSet<Feed>();
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
    
    public Set<Category> getVirtualCategories() {
        Set<Category> ret = new LinkedHashSet<Category>();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = db.query(TABLE_CATEGORIES, null, "id<1", null, null, null, "id ASC");
        try {
            while (!c.isAfterLast()) {
                Category ci = handleCategoryCursor(c);
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
    
    public Set<Category> getCategoriesIncludingUncategorized() {
        Set<Category> ret = new LinkedHashSet<Category>();
        if (!isDBAvailable())
            return ret;
        
        Cursor c = db.query(TABLE_CATEGORIES, null, "id>=0", null, null, null, "title ASC");
        try {
            while (!c.isAfterLast()) {
                Category ci = handleCategoryCursor(c);
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
            c = db.query(isCat ? TABLE_CATEGORIES : TABLE_FEEDS, new String[] { "unread" }, "id=?", new String[] { id
                    + "" }, null, null, null, null);
            
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
                db.update(TABLE_MARK, cv, "id IN (" + idList + ")", null);
                db.delete(TABLE_MARK, "isUnread is null AND isStarred is null AND isPublished is null", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // *******************************************
    
    private static Article handleArticleCursor(Cursor c) {
        Article ret = null;
        
        if (c.isBeforeFirst()) {
            if (!c.moveToFirst()) {
                return ret;
            }
        }
        
        // @formatter:off
        ret = new Article(
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
    
    private static Feed handleFeedCursor(Cursor c) {
        Feed ret = null;
        
        if (c.isBeforeFirst()) {
            if (!c.moveToFirst()) {
                return ret;
            }
        }
        
        // @formatter:off
        ret = new Feed(
                c.getInt(0),            // id
                c.getInt(1),            // categoryId
                c.getString(2),         // title
                c.getString(3),         // url
                c.getInt(4));           // unread
        // @formatter:on
        
        return ret;
    }
    
    private static Category handleCategoryCursor(Cursor c) {
        Category ret = null;
        
        if (c.isBeforeFirst()) {
            if (!c.moveToFirst()) {
                return ret;
            }
        }
        
        // @formatter:off
        ret = new Category(
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
