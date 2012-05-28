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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.utils.FileDateComparator;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class DBHelper {
    
    private static DBHelper instance = null;
    private volatile boolean initialized = false;
    private boolean vacuumDone = false;
    
    /*
     * We use two locks here to avoid too much locking for reading and be able to completely lock everything up when we
     * need to close the DB. In this case we lock both locks with write-access to avoid any other Threads holding the
     * locks while we use only read-locking on the "dbReadLock" for normal accesss. The "dbWriteLock" is always locked
     * on one Thread since concurrent write-access is not possible.
     */
    // private static final ReentrantReadWriteLock dbReadLock = new ReentrantReadWriteLock();
    // private static final ReentrantLock dbWriteLock = new ReentrantLock();
    
    public static final String DATABASE_NAME = "ttrss.db";
    public static final String DATABASE_BACKUP_NAME = "_backup_";
    public static final int DATABASE_VERSION = 51;
    
    public static final String TABLE_CATEGORIES = "categories";
    public static final String TABLE_FEEDS = "feeds";
    public static final String TABLE_ARTICLES = "articles";
    public static final String TABLE_ARTICLES2LABELS = "articles2labels";
    public static final String TABLE_LABELS = "labels";
    public static final String TABLE_MARK = "marked";
    
    public static final String MARK_READ = "isUnread";
    public static final String MARK_STAR = "isStarred";
    public static final String MARK_PUBLISH = "isPublished";
    public static final String MARK_NOTE = "note";
    
    // @formatter:off
    private static final String INSERT_CATEGORY = 
        "REPLACE INTO "
        + TABLE_CATEGORIES
        + " (id, title, unread)"
        + " VALUES (?, ?, ?)";
    
    private static final String INSERT_FEED = 
        "REPLACE INTO "
        + TABLE_FEEDS
        + " (id, categoryId, title, url, unread)"
        + " VALUES (?, ?, ?, ?, ?)";
    
    private static final String INSERT_ARTICLE = 
        "INSERT OR REPLACE INTO "
        + TABLE_ARTICLES
        + " (id, feedId, title, isUnread, articleUrl, articleCommentUrl, updateDate, content, attachments, isStarred, isPublished, cachedImages)" 
        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, coalesce((SELECT cachedImages FROM " + TABLE_ARTICLES + " WHERE id=?), 0))";
    // This should insert new values or replace existing values but should always keep an already inserted value for "cachedImages".
    // When inserting it is set to the default value which is 0 (not "NULL").
    
    private static final String INSERT_LABEL = 
        "REPLACE INTO "
        + TABLE_ARTICLES2LABELS
        + " (articleId, labelId)"
        + " VALUES (?, ?)";
    // @formatter:on
    
    private Context context;
    public SQLiteDatabase db;
    
    private SQLiteStatement insertCategory;
    private SQLiteStatement insertFeed;
    private SQLiteStatement insertArticle;
    private SQLiteStatement insertLabel;
    
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
        
        // Check if deleteDB is scheduled or if DeleteOnStartup is set
        if (Controller.getInstance().isDeleteDBScheduled()) {
            if (deleteDB()) {
                initializeDBHelper();
                Controller.getInstance().resetDeleteDBScheduled();
                return; // Don't need to check if DB is corrupted, it is NEW!
            }
        }
        
        // Initialize DB
        if (!initialized) {
            initializeDBHelper();
        } else if (db == null || !db.isOpen()) {
            initializeDBHelper();
        } else {
            return; // DB was already initialized, no need to check anything.
        }
        
        // Test if DB is accessible, backup and delete if not, else do the vacuuming
        if (initialized) {
            
            Cursor c = null;
            try {
                // Try to access the DB
                c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CATEGORIES, null);
                c.getCount();
                if (c.moveToFirst())
                    c.getInt(0);
                
            } catch (Exception e) {
                Log.e(Utils.TAG, "Database was corrupted, creating a new one...");
                
                closeDB();
                backupAndRemoveDB();
                
                // Initialize again...
                initializeDBHelper();
            } finally {
                if (c != null)
                    c.close();
            }
            
            // Do VACUUM if necessary and hasn't been done yet
            if (Controller.getInstance().isVacuumDBScheduled() && !vacuumDone) {
                Log.i(Utils.TAG, "Doing VACUUM, this can take a while...");
                
                // Reset scheduling-data
                Controller.getInstance().setVacuumDBScheduled(false);
                Controller.getInstance().setLastVacuumDate();
                
                // call vacuum
                vacuum();
            }
            
        }
    }
    
    private synchronized void backupAndRemoveDB() {
        // Move DB-File to backup
        File f = context.getDatabasePath(DATABASE_NAME);
        f.renameTo(new File(f.getAbsolutePath() + DATABASE_BACKUP_NAME + System.currentTimeMillis()));
        
        // Find setReadble method in old api
        try {
            Class<?> cls = SharedPreferences.Editor.class;
            Method m = cls.getMethod("setReadble");
            m.invoke(f, true, false);
        } catch (Exception e1) {
        }
        
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
    }
    
    private synchronized boolean initializeDBHelper() {
        
        if (context == null) {
            Log.e(Utils.TAG, "Can't handle internal DB without Context-Object.");
            return false;
        }
        
        if (db != null)
            closeDB();
        
        OpenHelper openHelper = new OpenHelper(context);
        db = openHelper.getWritableDatabase();
        db.setLockingEnabled(false);
        
        insertCategory = db.compileStatement(INSERT_CATEGORY);
        insertFeed = db.compileStatement(INSERT_FEED);
        insertArticle = db.compileStatement(INSERT_ARTICLE);
        insertLabel = db.compileStatement(INSERT_LABEL);
        
        initialized = true;
        return true;
    }
    
    private boolean deleteDB() {
        if (context == null)
            return false;
        
        if (db != null) {
            closeDB();
        }
        
        Log.i(Utils.TAG, "Deleting Database as requested by preferences.");
        File f = context.getDatabasePath(DATABASE_NAME);
        if (f.exists())
            return f.delete();
        
        return false;
    }
    
    private void closeDB() {
        // Close DB, acquire write-lock to make sure no other threads start accessing the DB from now on
        // dbWriteLock.lock();
        // dbReadLock.writeLock().lock();
        db.close();
        db = null;
        // dbReadLock.writeLock().unlock();
        // dbWriteLock.unlock();
    }
    
    private boolean isDBAvailable() {
        boolean ret = false;
        
        if (db != null && db.isOpen()) {
            ret = true;
        } else if (db != null) {
            
            synchronized (this) {
                if (db != null) {
                    OpenHelper openHelper = new OpenHelper(context);
                    db = openHelper.getWritableDatabase();
                    initialized = db.isOpen();
                    ret = initialized;
                }
            }
            
        } else {
            Log.i(Utils.TAG, "Controller not initialized, trying to do that now...");
            initializeDBHelper();
            ret = true;
        }
        
        return ret;
    }
    
    private void acquireLock() {
        // acquireLock(false);
    }
    
    private void acquireLock(boolean write) {
        // if (write) {
        // dbWriteLock.lock();
        // } else {
        // dbReadLock.readLock().lock();
        // }
    }
    
    private void releaseLock() {
        // releaseLock(false);
    }
    
    private void releaseLock(boolean write) {
        // if (write) {
        // dbWriteLock.unlock();
        // } else {
        // dbReadLock.readLock().unlock();
        // }
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
            
            db.execSQL(
                    "CREATE TABLE " 
                    + TABLE_ARTICLES2LABELS
                    + " (articleId INTEGER," 
                    + " labelId INTEGER, PRIMARY KEY(articleId, labelId))");
            
            db.execSQL("CREATE TABLE "
                    + TABLE_MARK
                    + " (id INTEGER,"
                    + " type INTEGER,"
                    + " " + MARK_READ + " INTEGER,"
                    + " " + MARK_STAR + " INTEGER,"
                    + " " + MARK_PUBLISH + " INTEGER,"
                    + " " + MARK_NOTE + " TEXT,"
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
                
                Log.i(Utils.TAG, String.format("Upgrading database from %s to 40.", oldVersion));
                Log.i(Utils.TAG, String.format(" (Executing: %s", sql));
                
                db.execSQL(sql);
                didUpgrade = true;
            }
            
            if (oldVersion < 42) {
                String sql = "ALTER TABLE " + TABLE_ARTICLES + " ADD COLUMN isPublished INTEGER";
                
                Log.i(Utils.TAG, String.format("Upgrading database from %s to 42.", oldVersion));
                Log.i(Utils.TAG, String.format(" (Executing: %s", sql));
                
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
                
                Log.i(Utils.TAG, String.format("Upgrading database from %s to 45.", oldVersion));
                Log.i(Utils.TAG, String.format(" (Executing: %s", sql));
                
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
                
                Log.i(Utils.TAG, String.format("Upgrading database from %s to 46.", oldVersion));
                Log.i(Utils.TAG, String.format(" (Executing: %s", sql));
                Log.i(Utils.TAG, String.format(" (Executing: %s", sql2));
                
                db.execSQL(sql);
                db.execSQL(sql2);
                didUpgrade = true;
            }
            
            if (oldVersion < 47) {
                String sql = "ALTER TABLE " + TABLE_ARTICLES + " ADD COLUMN cachedImages INTEGER DEFAULT 0";
                
                Log.i(Utils.TAG, String.format("Upgrading database from %s to 47.", oldVersion));
                Log.i(Utils.TAG, String.format(" (Executing: %s", sql));
                
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
                
                Log.i(Utils.TAG, String.format("Upgrading database from %s to 48.", oldVersion));
                Log.i(Utils.TAG, String.format(" (Executing: %s", sql));
                
                db.execSQL(sql);
                didUpgrade = true;
            }
            
            if (oldVersion < 49) {
                // @formatter:off
                String sql = "CREATE TABLE " 
                        + TABLE_ARTICLES2LABELS
                        + " (articleId INTEGER," 
                        + " labelId INTEGER, PRIMARY KEY(articleId, labelId))";
                // @formatter:on
                
                Log.i(Utils.TAG, String.format("Upgrading database from %s to 49.", oldVersion));
                Log.i(Utils.TAG, String.format(" (Executing: %s", sql));
                
                db.execSQL(sql);
                didUpgrade = true;
            }
            
            if (oldVersion < 50) {
                Log.i(Utils.TAG, String.format("Upgrading database from %s to 50.", oldVersion));
                ContentValues cv = new ContentValues();
                cv.put("cachedImages", 0);
                db.update(TABLE_ARTICLES, cv, "cachedImages IS null", null);
                didUpgrade = true;
            }
            
            if (oldVersion < 51) {
                // @formatter:off
                String sql = "DROP TABLE IF EXISTS "
                    + TABLE_MARK;
                String sql2 = "CREATE TABLE "
                    + TABLE_MARK
                    + " (id INTEGER,"
                    + " type INTEGER,"
                    + " " + MARK_READ + " INTEGER,"
                    + " " + MARK_STAR + " INTEGER,"
                    + " " + MARK_PUBLISH + " INTEGER,"
                    + " " + MARK_NOTE + " TEXT,"
                    + " PRIMARY KEY(id, type))";
                // @formatter:on
                
                Log.i(Utils.TAG, String.format("Upgrading database from %s to 51.", oldVersion));
                Log.i(Utils.TAG, String.format(" (Executing: %s", sql));
                Log.i(Utils.TAG, String.format(" (Executing: %s", sql2));
                
                db.execSQL(sql);
                db.execSQL(sql2);
                didUpgrade = true;
            }
            
            if (didUpgrade == false) {
                Log.i(Utils.TAG, "Upgrading database, this will drop tables and recreate.");
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
        acquireLock();
        Cursor cursor = db.rawQuery(sql, selectionArgs);
        releaseLock();
        return cursor;
    }
    
    public Cursor queryArticlesForImageCache(boolean onlyUnreadImages) {
        acquireLock();
        // Add where-clause for only unread articles
        String where = "cachedImages=0";
        if (onlyUnreadImages)
            where += " AND isUnread>0";
        
        Cursor cursor = db.query(TABLE_ARTICLES, new String[] { "id", "content", "attachments" }, where, null, null,
                null, null);
        releaseLock();
        return cursor;
    }
    
    // *******| INSERT |*******************************************************************
    
    private void insertCategory(int id, String title, int unread) {
        if (title == null)
            title = "";
        
        synchronized (insertCategory) {
            insertCategory.bindLong(1, id);
            insertCategory.bindString(2, title);
            insertCategory.bindLong(3, unread);
            insertCategory.execute();
        }
    }
    
    public void insertCategories(Set<Category> set) {
        if (!isDBAvailable())
            return;
        if (set == null)
            return;
        
        acquireLock(true);
        
        db.beginTransaction();
        try {
            for (Category c : set) {
                insertCategory(c.id, c.title, c.unread);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            releaseLock(true);
        }
    }
    
    private void insertFeed(int id, int categoryId, String title, String url, int unread) {
        if (title == null)
            title = "";
        if (url == null)
            url = "";
        
        synchronized (insertFeed) {
            insertFeed.bindLong(1, new Integer(id).longValue());
            insertFeed.bindLong(2, new Integer(categoryId).longValue());
            insertFeed.bindString(3, title);
            insertFeed.bindString(4, url);
            insertFeed.bindLong(5, unread);
            insertFeed.execute();
        }
    }
    
    public void insertFeeds(Set<Feed> set) {
        if (!isDBAvailable())
            return;
        if (set == null)
            return;
        
        acquireLock(true);
        db.beginTransaction();
        try {
            for (Feed f : set) {
                insertFeed(f.id, f.categoryId, f.title, f.url, f.unread);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            releaseLock(true);
        }
    }
    
    private void insertArticleIntern(ArticleContainer a) {
        if (a.title == null)
            a.title = "";
        if (a.content == null)
            a.content = "";
        if (a.articleUrl == null)
            a.articleUrl = "";
        if (a.articleCommentUrl == null)
            a.articleCommentUrl = "";
        if (a.updateDate == null)
            a.updateDate = new Date();
        if (a.attachments == null)
            a.attachments = new LinkedHashSet<String>();
        
        long retId = -1;
        synchronized (insertArticle) {
            insertArticle.bindLong(1, a.id);
            insertArticle.bindLong(2, a.feedId);
            insertArticle.bindString(3, a.title);
            insertArticle.bindLong(4, (a.isUnread ? 1 : 0));
            insertArticle.bindString(5, a.articleUrl);
            insertArticle.bindString(6, a.articleCommentUrl);
            insertArticle.bindLong(7, a.updateDate.getTime());
            insertArticle.bindString(8, a.content);
            insertArticle.bindString(9, parseAttachmentSet(a.attachments));
            insertArticle.bindLong(10, (a.isStarred ? 1 : 0));
            insertArticle.bindLong(11, (a.isPublished ? 1 : 0));
            insertArticle.bindLong(12, a.id); // ID again for the where-clause
            retId = insertArticle.executeInsert();
        }
        
        if (retId > 0)
            insertLabel(a.id, a.label);
    }
    
    public void insertArticle(ArticleContainer a) {
        if (!isDBAvailable())
            return;
        
        acquireLock(true);
        insertArticleIntern(a);
        releaseLock(true);
    }
    
    public void insertArticle(List<ArticleContainer> articles) {
        if (!isDBAvailable())
            return;
        if (articles == null || articles.isEmpty())
            return;
        
        acquireLock(true);
        db.beginTransaction();
        try {
            for (ArticleContainer a : articles) {
                insertArticleIntern(a);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            releaseLock(true);
        }
    }
    
    public static Object[] prepareArticleArray(int id, int feedId, String title, boolean isUnread, String articleUrl, String articleCommentUrl, Date updateDate, String content, Set<String> attachments, boolean isStarred, boolean isPublished, int label) {
        Object[] ret = new Object[11];
        
        ret[0] = id;
        ret[1] = feedId;
        ret[2] = (title == null ? "" : title);
        ret[3] = (isUnread ? 1 : 0);
        ret[4] = (articleUrl == null ? "" : articleUrl);
        ret[5] = (articleCommentUrl == null ? "" : articleCommentUrl);
        ret[6] = updateDate.getTime();
        ret[7] = (content == null ? "" : content);
        ret[8] = parseAttachmentSet(attachments);
        ret[9] = (isStarred ? 1 : 0);
        ret[10] = (isPublished ? 1 : 0);
        
        return ret;
    }
    
    /**
     * New method of inserting many articles into the DB at once. Doesn't seem to run faster then the old way so i'll
     * just leave this code here for future reference and ignore it until it proves to be useful.
     * 
     * @param input
     *            Object-Array with the fields of an article-object.
     */
    public void bulkInsertArticles(List<Object[]> input) {
        
        if (!isDBAvailable())
            return;
        if (input == null || input.isEmpty())
            return;
        
        StringBuilder stmt = new StringBuilder();
        stmt.append("INSERT OR REPLACE INTO " + TABLE_ARTICLES);
        
        Object[] entry = input.get(0);
        stmt.append(" SELECT ");
        
        stmt.append(entry[0] + " AS id, ");
        stmt.append(entry[1] + " AS feedId, ");
        stmt.append(DatabaseUtils.sqlEscapeString(entry[2] + "") + " AS title, ");
        stmt.append(entry[3] + " AS isUnread, ");
        stmt.append("'" + entry[4] + "' AS articleUrl, ");
        stmt.append("'" + entry[5] + "' AS articleCommentUrl, ");
        stmt.append(entry[6] + " AS updateDate, ");
        stmt.append(DatabaseUtils.sqlEscapeString(entry[7] + "") + " AS content, ");
        stmt.append("'" + entry[8] + "' AS attachments, ");
        stmt.append(entry[9] + " AS isStarred, ");
        stmt.append(entry[10] + " AS isPublished, ");
        stmt.append("coalesce((SELECT cachedImages FROM articles WHERE id=" + entry[0] + "), 0) AS cachedImages UNION");
        
        for (int i = 1; i < input.size(); i++) {
            entry = input.get(i);
            
            stmt.append(" SELECT ");
            for (int j = 0; j < entry.length; j++) {
                
                if (j == 2 || j == 7) {
                    // Escape and enquote Content and Title, they can contain quotes
                    stmt.append(DatabaseUtils.sqlEscapeString(entry[j] + ""));
                } else if (j == 4 || j == 5 || j == 8) {
                    // Just enquote Text-Fields
                    stmt.append("'" + entry[j] + "'");
                } else {
                    // Leave numbers..
                    stmt.append(entry[j]);
                }
                
                if (j < (entry.length - 1))
                    stmt.append(", ");
                if (j == (entry.length - 1))
                    stmt.append(", coalesce((SELECT cachedImages FROM articles WHERE id=" + entry[0] + "), 0)");
            }
            if (i < input.size() - 1)
                stmt.append(" UNION ");
            
        }
        
        acquireLock(true);
        db.execSQL(stmt.toString());
        releaseLock(true);
    }
    
    private void insertLabel(int articleId, int label) {
        if (label < -10) {
            synchronized (insertLabel) {
                insertLabel.bindLong(1, articleId);
                insertLabel.bindLong(2, label);
                insertLabel.executeInsert();
            }
        }
    }
    
    // *******| UPDATE |*******************************************************************
    
    public void markCategoryRead(int categoryId) {
        if (isDBAvailable()) {
            acquireLock(true);
            updateCategoryUnreadCount(categoryId, 0);
            for (Feed f : getFeeds(categoryId)) {
                markFeedRead(f.id);
            }
            releaseLock(true);
        }
    }
    
    public void markFeedRead(int feedId) {
        if (isDBAvailable()) {
            acquireLock(true);
            markFeedReadIntern(feedId);
            releaseLock(true);
        }
    }
    
    private void markFeedReadIntern(int feedId) {
        String[] cols = new String[] { "isStarred", "isPublished", "updateDate" };
        Cursor c = db.query(TABLE_ARTICLES, cols, "isUnread>0 AND feedId=" + feedId, null, null, null, null);
        
        int countStar = 0;
        int countPub = 0;
        int countFresh = 0;
        int countAll = 0;
        long ms = System.currentTimeMillis() - Controller.getInstance().getFreshArticleMaxAge();
        Date maxAge = new Date(ms);
        
        if (c.moveToFirst()) {
            while (true) {
                countAll++;
                
                if (c.getInt(0) > 0)
                    countStar++;
                
                if (c.getInt(1) > 0)
                    countPub++;
                
                Date d = new Date(c.getLong(2));
                if (d.after(maxAge))
                    countFresh++;
                
                if (!c.move(1))
                    break;
            }
        }
        c.close();
        
        updateCategoryDeltaUnreadCount(Data.VCAT_STAR, countStar * -1);
        updateCategoryDeltaUnreadCount(Data.VCAT_PUB, countPub * -1);
        updateCategoryDeltaUnreadCount(Data.VCAT_FRESH, countFresh * -1);
        updateCategoryDeltaUnreadCount(Data.VCAT_ALL, countAll * -1);
        
        updateFeedUnreadCount(feedId, 0);
        
        if (feedId < -10) {
            markLabelRead(feedId);
        } else {
            ContentValues cv = new ContentValues();
            cv.put("isUnread", 0);
            db.update(TABLE_ARTICLES, cv, "isUnread>0 AND feedId=" + feedId, null);
        }
    }
    
    public void markLabelRead(int labelId) {
        if (isDBAvailable()) {
            
            ContentValues cv = new ContentValues();
            cv.put("isUnread", 0);
            String idList = "SELECT id FROM " + TABLE_ARTICLES + " AS a, " + TABLE_ARTICLES2LABELS
                    + " as l WHERE a.id=l.articleId AND l.labelId=" + labelId;
            
            acquireLock(true);
            db.update(TABLE_ARTICLES, cv, "isUnread>0 AND id IN(" + idList + ")", null);
            releaseLock(true);
        }
    }
    
    // Marks only the articles as read so the JSONConnector can retrieve new articles and overwrite the old articles
    public void markFeedOnlyArticlesRead(int feedId, boolean isCat) {
        
        if (!isCat && feedId < -10) {
            markLabelRead(feedId);
            return;
        }
        
        if (isDBAvailable()) {
            ContentValues cv = new ContentValues();
            cv.put("isUnread", 0);
            
            // Mark all articles from feed or category as read, depending on isCat. Just use idList with only one feedId
            // if it is just a feed, else create a list of feedIds.
            String idList = "";
            if (isCat)
                idList = "SELECT id FROM " + TABLE_FEEDS + " WHERE categoryId=" + feedId;
            else
                idList = feedId + "";
            
            acquireLock(true);
            db.update(TABLE_ARTICLES, cv, "isUnread>0 AND feedId IN(" + idList + ")", null);
            releaseLock(true);
        }
    }
    
    public void markArticles(Set<Integer> iDlist, String mark, int state) {
        if (isDBAvailable()) {
            acquireLock(true);
            
            db.beginTransaction();
            try {
                for (Integer id : iDlist) {
                    markArticle(id, mark, state);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
                releaseLock(true);
            }
        }
    }
    
    public void markArticle(int id, String mark, int state) {
        if (isDBAvailable()) {
            String sql = String.format("UPDATE %s SET %s=%s WHERE id=%s", TABLE_ARTICLES, mark, state, id);
            acquireLock(true);
            db.execSQL(sql);
            releaseLock(true);
        }
    }
    
    public void markUnsynchronizedStatesCategory(int categoryId) {
        Set<Integer> ids = new HashSet<Integer>();
        for (Feed f : getFeeds(categoryId)) {
            if (f.unread > 0) {
                for (Article a : getUnreadArticles(f.id)) {
                    ids.add(a.id);
                }
            }
        }
        markUnsynchronizedStates(ids, MARK_READ, 0);
    }
    
    public void markUnsynchronizedStatesFeed(int feedId) {
        Feed f = getFeed(feedId);
        if (f != null && f.unread > 0) {
            Set<Integer> ids = new HashSet<Integer>();
            for (Article a : getUnreadArticles(f.id)) {
                ids.add(a.id);
            }
            markUnsynchronizedStates(ids, MARK_READ, 0);
        }
    }
    
    public void markUnsynchronizedStates(Set<Integer> ids, String mark, int state) {
        if (!isDBAvailable())
            return;
        
        // Disabled until further testing and proper SQL has been built. Tries to do the UPDATE and INSERT without
        // looping over the ids but instead with a list of ids:
        // Set<String> idList = StringSupport.convertListToString(ids);
        // for (String s : idList) {
        // db.execSQL(String.format("UPDATE %s SET %s=%s WHERE id in %s", TABLE_MARK, mark, state, s));
        // db.execSQL(String.format("INSERT OR IGNORE INTO %s (id, %s) VALUES (%s, %s)", TABLE_MARK, mark, id, state));
        // <- WRONG!
        // }
        
        acquireLock(true);
        db.beginTransaction();
        try {
            for (Integer id : ids) {
                // First update, then insert. If row exists it gets updated and second call ignores it, else the second
                // call inserts it.
                db.execSQL(String.format("UPDATE %s SET %s=%s WHERE id=%s", TABLE_MARK, mark, state, id));
                db.execSQL(String.format("INSERT OR IGNORE INTO %s (id, %s) VALUES (%s, %s)", TABLE_MARK, mark, id,
                        state));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            releaseLock(true);
        }
    }
    
    // Special treatment for notes since the method markUnsynchronizedStates(...) doesn't support inserting any
    // additional data.
    public void markUnsynchronizedNotes(Map<Integer, String> ids, String markPublish) {
        if (!isDBAvailable())
            return;
        
        acquireLock(true);
        
        db.beginTransaction();
        try {
            for (Integer id : ids.keySet()) {
                String note = ids.get(id);
                if (note == null || note.equals(""))
                    continue;
                
                ContentValues cv = new ContentValues();
                cv.put(MARK_NOTE, note);
                db.update(TABLE_MARK, cv, "id=" + id, null);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            releaseLock(true);
        }
    }
    
    public void updateCategoryUnreadCount(int id, int count) {
        if (isDBAvailable()) {
            if (count >= 0) {
                ContentValues cv = new ContentValues();
                cv.put("unread", count);
                
                acquireLock(true);
                db.update(TABLE_CATEGORIES, cv, "id=?", new String[] { id + "" });
                releaseLock(true);
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
        if (isDBAvailable()) {
            if (count >= 0) {
                ContentValues cv = new ContentValues();
                cv.put("unread", count);
                
                acquireLock(true);
                db.update(TABLE_FEEDS, cv, "id=?", new String[] { id + "" });
                releaseLock(true);
            }
        }
    }
    
    public void updateFeedDeltaUnreadCount(int id, int delta) {
        Feed f = getFeed(id);
        int count = f.unread;
        count += delta;
        updateFeedUnreadCount(id, count);
    }
    
    public void updateAllArticlesCachedImages(boolean isCachedImages) {
        if (isDBAvailable()) {
            ContentValues cv = new ContentValues();
            cv.put("cachedImages", isCachedImages);
            
            acquireLock(true);
            db.update(TABLE_ARTICLES, cv, "cachedImages=0", null); // Only apply if not yet applied
            releaseLock(true);
        }
    }
    
    public void updateArticleCachedImages(int id, boolean isCachedImages) {
        if (isDBAvailable()) {
            ContentValues cv = new ContentValues();
            cv.put("cachedImages", isCachedImages);
            
            acquireLock(true);
            db.update(TABLE_ARTICLES, cv, "cachedImages=0 & id=" + id, null); // Only apply if not yet applied and ID
            releaseLock(true);
        }
    }
    
    public void deleteCategories(boolean withVirtualCategories) {
        if (isDBAvailable()) {
            String wherePart = "";
            if (!withVirtualCategories)
                wherePart = "id > 0";
            
            acquireLock(true);
            db.delete(TABLE_CATEGORIES, wherePart, null);
            releaseLock(true);
        }
    }
    
    public void deleteFeeds() {
        if (isDBAvailable()) {
            acquireLock(true);
            db.delete(TABLE_FEEDS, null, null);
            releaseLock(true);
        }
    }
    
    /**
     * Deletes articles until the configured number of articles is matched. Published and Starred articles are ignored
     * so the configured limit is not an exact upper limit to the numbe rof articles in the database.
     */
    public void purgeArticlesNumber() {
        if (isDBAvailable()) {
            int number = Controller.getInstance().getArticleLimit();
            String idList = "SELECT id FROM " + TABLE_ARTICLES
                    + " WHERE isPublished=0 AND isStarred=0 ORDER BY updateDate DESC LIMIT -1 OFFSET " + number;
            
            acquireLock(true);
            db.delete(TABLE_ARTICLES, "id in(" + idList + ")", null);
            purgeLabels();
            releaseLock(true);
        }
    }
    
    public void purgePublishedArticles() {
        if (isDBAvailable()) {
            acquireLock(true);
            db.delete(TABLE_ARTICLES, "isPublished>0", null);
            purgeLabels();
            releaseLock(true);
        }
    }
    
    public void purgeStarredArticles() {
        if (isDBAvailable()) {
            acquireLock(true);
            db.delete(TABLE_ARTICLES, "isStarred>0", null);
            purgeLabels();
            releaseLock(true);
        }
    }
    
    private void purgeLabels() {
        // @formatter:off
        String idsArticles = "SELECT a2l.articleId FROM "
            + TABLE_ARTICLES2LABELS + " AS a2l LEFT OUTER JOIN "
            + TABLE_ARTICLES + " AS a"
            + " ON a2l.articleId = a.id WHERE a.id IS null";

        String idsFeeds = "SELECT a2l.labelId FROM "
            + TABLE_ARTICLES2LABELS + " AS a2l LEFT OUTER JOIN "
            + TABLE_FEEDS + " AS f"
            + " ON a2l.labelId = f.id WHERE f.id IS null";
        // @formatter:on
        db.delete(TABLE_ARTICLES2LABELS, "articleId IN(" + idsArticles + ")", null);
        db.delete(TABLE_ARTICLES2LABELS, "labelId IN(" + idsFeeds + ")", null);
    }
    
    public void vacuum() {
        if (vacuumDone)
            return;
        
        try {
            long time = System.currentTimeMillis();
            db.execSQL("VACUUM");
            vacuumDone = true;
            Log.i(Utils.TAG, "SQLite VACUUM took " + (System.currentTimeMillis() - time) + " ms.");
        } catch (SQLException e) {
            Log.e(Utils.TAG, "SQLite VACUUM failed: " + e.getMessage() + " " + e.getCause());
        }
    }
    
    // *******| SELECT |*******************************************************************
    
    public int getSinceId() {
        int ret = 0;
        if (!isDBAvailable())
            return ret;
        
        acquireLock();
        
        Cursor c = null;
        try {
            c = db.query(TABLE_ARTICLES, new String[] { "id" }, null, null, null, null, "id DESC", "1");
            
            if (!c.isAfterLast()) {
                
                if (c.isBeforeFirst() && !c.moveToFirst())
                    return 0;
                ret = c.getInt(0);
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
            
            releaseLock();
        }
        
        return ret;
    }
    
    // Takes about 2 to 6 ms on Motorola Milestone
    public Article getArticle(int id) {
        Article ret = null;
        if (!isDBAvailable())
            return ret;
        
        acquireLock();
        
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
            
            releaseLock();
        }
        
        return ret;
    }
    
    public Feed getFeed(int id) {
        Feed ret = new Feed();
        if (!isDBAvailable())
            return ret;
        
        acquireLock();
        
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
            
            releaseLock();
        }
        
        return ret;
    }
    
    public Category getCategory(int id) {
        Category ret = new Category();
        if (!isDBAvailable())
            return ret;
        
        acquireLock();
        
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
            
            releaseLock();
        }
        
        return ret;
    }
    
    public Set<Article> getUnreadArticles(int feedId) {
        Set<Article> ret = new LinkedHashSet<Article>();
        if (!isDBAvailable())
            return ret;
        
        acquireLock();
        
        Cursor c = null;
        try {
            c = db.query(TABLE_ARTICLES, null, "feedId=? AND isUnread>0", new String[] { feedId + "" }, null, null,
                    null, null);
            
            while (!c.isAfterLast()) {
                ret.add(handleArticleCursor(c));
                c.move(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
            
            releaseLock();
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
        
        acquireLock();
        
        Cursor c = null;
        try {
            String where = "categoryId=" + categoryId;
            if (categoryId < 0 && categoryId != -2) {
                where = null;
            }
            
            c = db.query(TABLE_FEEDS, null, where, null, null, null, "UPPER(title) ASC");
            
            while (!c.isAfterLast()) {
                ret.add(handleFeedCursor(c));
                c.move(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
            
            releaseLock();
        }
        
        return ret;
    }
    
    public Set<Category> getVirtualCategories() {
        Set<Category> ret = new LinkedHashSet<Category>();
        if (!isDBAvailable())
            return ret;
        
        acquireLock();
        
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
            
            releaseLock();
        }
        
        return ret;
    }
    
    public Set<Category> getCategoriesIncludingUncategorized() {
        Set<Category> ret = new LinkedHashSet<Category>();
        if (!isDBAvailable())
            return ret;
        
        acquireLock();
        
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
            
            releaseLock();
        }
        
        return ret;
    }
    
    public int getUnreadCount(int id, boolean isCat) {
        int ret = 0;
        if (!isDBAvailable())
            return ret;
        
        if (isCat && id >= 0) { // Only do this for real categories for now
        
            for (Feed f : getFeeds(id)) {
                // Recurse into all feeds of this category and add the unread-count
                ret += getUnreadCount(f.id, false);
            }
            
        } else {
            // Read count for given feed
            acquireLock();
            Cursor c = null;
            try {
                c = db.query(isCat ? TABLE_CATEGORIES : TABLE_FEEDS, new String[] { "unread" }, "id=" + id, null, null,
                        null, null, null);
                
                if (c.moveToFirst())
                    ret = c.getInt(0);
                
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (c != null)
                    c.close();
                
                releaseLock();
            }
        }
        
        return ret;
    }
    
    public Map<Integer, String> getMarked(String mark, int status) {
        Map<Integer, String> ret = new HashMap<Integer, String>();
        if (!isDBAvailable())
            return ret;
        
        acquireLock();
        
        Cursor c = null;
        try {
            c = db.query(TABLE_MARK, new String[] { "id", MARK_NOTE }, mark + "=" + status, null, null, null, null,
                    null);
            
            if (!c.moveToFirst())
                return ret;
            
            while (!c.isAfterLast()) {
                ret.put(c.getInt(0), c.getString(1));
                c.move(1);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
            
            releaseLock();
        }
        
        return ret;
    }
    
    public void setMarked(Map<Integer, String> ids, String mark) {
        if (!isDBAvailable())
            return;
        
        acquireLock();
        
        db.beginTransaction();
        try {
            for (String idList : StringSupport.convertListToString(ids.keySet(), 100)) {
                ContentValues cv = new ContentValues();
                cv.putNull(mark);
                db.update(TABLE_MARK, cv, "id IN(" + idList + ")", null);
                db.delete(TABLE_MARK, "isUnread IS null AND isStarred IS null AND isPublished IS null", null);
            }
            
            // Insert notes afterwards and only if given note is not null
            for (Integer id : ids.keySet()) {
                String note = ids.get(id);
                if (note == null || note.equals(""))
                    continue;
                
                ContentValues cv = new ContentValues();
                cv.put(MARK_NOTE, note);
                db.update(TABLE_MARK, cv, "id=" + id, null);
            }
            
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            releaseLock();
        }
    }
    
    // *******************************************
    
    private static Article handleArticleCursor(Cursor c) {
        if (c.isBeforeFirst() && !c.moveToFirst())
            return null;
        
        // @formatter:off
        Article ret = new Article(
                c.getInt(0),                        // id
                c.getInt(1),                        // feedId
                c.getString(2),                     // title
                (c.getInt(3) != 0),                 // isUnread
                c.getString(4),                     // articleUrl
                c.getString(5),                     // articleCommentUrl
                new Date(c.getLong(6)),             // updateDate
                c.getString(7),                     // content
                parseAttachments(c.getString(8)),   // attachments
                (c.getInt(9) != 0),                 // isStarred
                (c.getInt(10) != 0)                 // isPublished
        );
        ret.cachedImages = (c.getInt(11) != 0);
        // @formatter:on
        return ret;
    }
    
    private static Feed handleFeedCursor(Cursor c) {
        if (c.isBeforeFirst() && !c.moveToFirst())
            return null;
        
        // @formatter:off
        Feed ret = new Feed(
                c.getInt(0),            // id
                c.getInt(1),            // categoryId
                c.getString(2),         // title
                c.getString(3),         // url
                c.getInt(4));           // unread
        // @formatter:on
        return ret;
    }
    
    private static Category handleCategoryCursor(Cursor c) {
        if (c.isBeforeFirst() && !c.moveToFirst())
            return null;
        
        // @formatter:off
        Category ret = new Category(
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
