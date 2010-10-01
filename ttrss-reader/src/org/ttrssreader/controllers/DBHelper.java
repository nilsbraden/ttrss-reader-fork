/*
 * Tiny Tiny RSS Reader for Android
 * 
 * Copyright (C) 2009 J. Devauchelle and contributors.
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import android.os.Environment;
import android.util.Log;

public class DBHelper {
    
    private static DBHelper mInstance = null;
    private boolean mIsControllerInitialized = false;
    
    private static final String DATABASE_NAME = "ttrss.db";
    private static final int DATABASE_VERSION = 14;
    
    private static final String TABLE_CAT = "categories";
    private static final String TABLE_FEEDS = "feeds";
    private static final String TABLE_ARTICLES = "articles";
    
    private static final String INSERT_CAT = "REPLACE INTO " + TABLE_CAT + "(id, title, unread) VALUES (?, ?, ?)";
    
    private static final String INSERT_FEEDS = "REPLACE INTO " + TABLE_FEEDS
            + "(id, categoryId, title, url, unread) VALUES (?, ?, ?, ?, ?)";
    
    private static final String INSERT_ARTICLES = "REPLACE INTO " + TABLE_ARTICLES
            + "(id, feedId, title, isUnread, articleUrl, articleCommentUrl, updateDate, attachments) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String UPDATE_ARTICLES = "UPDATE " + TABLE_ARTICLES
            + " SET title=?, articleUrl=?, articleCommentUrl=?, updateDate=? WHERE id=? AND feedId=? AND attachments=?";
    
    private static final String UPDATE_ARTICLES_EXTERN = "UPDATE " + TABLE_ARTICLES
            + " SET content=?, updateDate=?, attachments=? WHERE id=? AND feedId=?";
    
    private SQLiteDatabase db_intern;
    private SQLiteDatabase db_extern;
    
    private SQLiteStatement insertCat;
    private SQLiteStatement insertFeed;
    private SQLiteStatement insertArticle;
    private SQLiteStatement updateArticle;
    private SQLiteStatement updateArticle_extern;
    
    boolean externalDBState;
    private Context context;
    
    public DBHelper() {
        context = null;
        
        db_intern = null;
        db_extern = null;
        
        insertCat = null;
        insertFeed = null;
        insertArticle = null;
        updateArticle = null;
        updateArticle_extern = null;
        
        externalDBState = false;
    }
    
    private synchronized boolean initializeController() {
        
        if (context == null) {
            Log.e(Utils.TAG, "Can't handle internal DB without Context-Object.");
            return false;
        }
        
        handleDBUpdate();
        
        OpenHelper openHelper = new OpenHelper(context);
        db_intern = openHelper.getWritableDatabase();
        db_extern = openDatabase();
        
        db_intern.setLockingEnabled(false);
        if (db_extern != null) {
            db_extern.setLockingEnabled(false);
        }
        
        insertCat = db_intern.compileStatement(INSERT_CAT);
        insertFeed = db_intern.compileStatement(INSERT_FEEDS);
        insertArticle = db_intern.compileStatement(INSERT_ARTICLES);
        updateArticle = db_intern.compileStatement(UPDATE_ARTICLES);
        if (isExternalDBAvailable()) {
            updateArticle_extern = db_extern.compileStatement(UPDATE_ARTICLES_EXTERN);
        }
        return true;
        
    }
    
    public synchronized void checkAndInitializeController(Context context) {
        if (!mIsControllerInitialized) {
            this.context = context;
            mIsControllerInitialized = initializeController();
        }
    }
    
    public static DBHelper getInstance() {
        if (mInstance == null) {
            mInstance = new DBHelper();
        }
        return mInstance;
    }
    
    public void destroy() {
        closeDB();
        mInstance = null;
        mIsControllerInitialized = false;
    }
    
    private void handleDBUpdate() {
        if (DATABASE_VERSION > Controller.getInstance().getDatabaseVersion()) {
            Log.i(Utils.TAG, "Database-Version: " + Controller.getInstance().getDatabaseVersion() + " (Internal: "
                    + DATABASE_VERSION + ")");
            
            OpenHelper openHelper = new OpenHelper(context);
            db_intern = openHelper.getWritableDatabase();
            db_extern = openDatabase();
            
            dropInternalDB();
            dropExternalDB();
            
            db_intern.close();
            if (isExternalDBAvailable()) {
                db_extern.close();
            }
        }
        
        Controller.getInstance().setDatabaseVersion(DATABASE_VERSION);
    }
    
    // private void deleteAll() {
    // db_intern.execSQL("DELETE FROM " + TABLE_CAT);
    // db_intern.execSQL("DELETE FROM " + TABLE_FEEDS);
    // db_intern.execSQL("DELETE FROM " + TABLE_ARTICLES);
    // if (isExternalDBAvailable()) {
    // db_extern.execSQL("DELETE FROM " + TABLE_ARTICLES);
    // }
    // }
    
    private void dropInternalDB() {
        if (context.getDatabasePath(DATABASE_NAME).delete()) {
            Log.d(Utils.TAG, "dropInternalDB(): database deleted.");
        } else {
            Log.d(Utils.TAG, "dropInternalDB(): database NOT deleted.");
        }
    }
    
    private void dropExternalDB() {
        StringBuilder builder = new StringBuilder();
        builder.append(Environment.getExternalStorageDirectory()).append(File.separator).append(Utils.SDCARD_PATH)
                .append(File.separator).append(DATABASE_NAME);
        
        if (new File(builder.toString()).delete()) {
            Log.d(Utils.TAG, "dropExternalDB(): database deleted.");
        } else {
            Log.d(Utils.TAG, "dropExternalDB(): database NOT deleted.");
        }
    }
    
    public void setExternalDB(boolean state) {
        if (state) {
            openDatabase();
            externalDBState = true;
        } else {
            if (isExternalDBAvailable()) {
                db_extern.close();
                externalDBState = false;
            }
        }
    }
    
    private boolean isInternalDBAvailable() {
        if (db_intern != null && db_intern.isOpen()) {
            return true;
        } else {
            if (mIsControllerInitialized) {
                Log.w(Utils.TAG,
                        "Controller initialized BUT internal DB is null? Trying to initialize Controller again...");
                mIsControllerInitialized = initializeController();
            } else {
                Log.w(Utils.TAG, "Controller not initialized, trying to do that now...");
                mIsControllerInitialized = initializeController();
            }
            return mIsControllerInitialized;
        }
    }
    
    private boolean isExternalDBAvailable() {
        return externalDBState;
    }
    
    public void closeDB() {
        db_intern.close();
        if (isExternalDBAvailable()) {
            db_extern.close();
        }
    }
    
    /**
     * Opens the SDcard database. If it cannot be opened, it
     * creates a new instance. If a new instance cannot be created, it throws
     * an exception and logs the failure.
     * 
     * @return true if successful
     * @throws SQLException
     *             if the database is unable to be opened or created
     */
    private synchronized SQLiteDatabase openDatabase() throws SQLException {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            StringBuilder builder = new StringBuilder();
            
            // open or create a new directory
            builder.setLength(0);
            builder.append(Environment.getExternalStorageDirectory()).append(File.separator).append(Utils.SDCARD_PATH);
            
            File dir = new File(builder.toString());
            dir.mkdirs();
            File file = new File(dir, DATABASE_NAME);
            
            try {
                Log.d(Utils.TAG, "Opening database: " + file.getAbsolutePath());
                db_extern = SQLiteDatabase.openOrCreateDatabase(file.getAbsolutePath(), null);
                
                // Create tables if they dont exist
                db_extern.execSQL("CREATE TABLE IF NOT EXISTS "
                        + TABLE_ARTICLES
                        + " (id INTEGER PRIMARY KEY, feedId INTEGER, content TEXT, isUnread INTEGER, updateDate INTEGER, attachments TEXT)");
                
                externalDBState = db_extern.isOpen();
                
            } catch (SQLException e) {
                Log.e(Utils.TAG, "failed to open" + e);
                throw e;
            }
        }
        
        return db_extern;
    }
    
    private static class OpenHelper extends SQLiteOpenHelper {
        
        OpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_CAT + " (id INTEGER PRIMARY KEY, title TEXT, unread INTEGER)");
            
            db.execSQL("CREATE TABLE "
                    + TABLE_FEEDS
                    + " (id INTEGER PRIMARY KEY, categoryId INTEGER, title TEXT, url TEXT, unread INTEGER)");
            
            db.execSQL("CREATE TABLE "
                    + TABLE_ARTICLES
                    + " (id INTEGER PRIMARY KEY, feedId INTEGER, title TEXT, isUnread INTEGER, articleUrl TEXT, articleCommentUrl TEXT, updateDate INTEGER, attachments TEXT)");
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w("Example", "Upgrading database, this will drop tables and recreate.");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CAT);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FEEDS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ARTICLES);
            onCreate(db);
        }
    }
    
    // *******| INSERT |*******************************************************************
    
    private void insertCategory(int id, String title, int unread) {
        if (!isInternalDBAvailable())
            return;
        
        if (title == null)
            title = "";
        
        synchronized (insertCat) {
            insertCat.bindLong(1, id);
            insertCat.bindString(2, title);
            insertCat.bindLong(3, unread);
            insertCat.execute();
        }
    }
    
    public void insertCategory(CategoryItem c) {
        if (c == null)
            return;
        
        insertCategory(c.getId(), c.getTitle(), c.getUnreadCount());
    }
    
    public void insertCategories(List<CategoryItem> list) {
        if (list == null)
            return;
        
        for (CategoryItem c : list) {
            insertCategory(c.getId(), c.getTitle(), c.getUnreadCount());
        }
    }
    
    private void insertFeed(int feedId, int categoryId, String title, String url, int unread) {
        if (!isInternalDBAvailable())
            return;
        
        if (title == null)
            title = "";
        if (url == null)
            url = "";
        
        synchronized (insertFeed) {
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
    
    public void insertFeeds(List<FeedItem> list) {
        if (list == null)
            return;
        
        for (FeedItem f : list) {
            insertFeed(f);
        }
    }
    
    private void insertArticle(int articleId, int feedId, String title, boolean isUnread, String content, String articleUrl, String articleCommentUrl, Date updateDate, Set<String> attachments) {

        if (!isInternalDBAvailable())
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
            attachments = new HashSet<String>();
        
        String att = parseAttachmentSet(attachments);
        
        synchronized (insertArticle) {
            insertArticle.bindLong(1, articleId);
            insertArticle.bindLong(2, feedId);
            insertArticle.bindString(3, title);
            insertArticle.bindLong(4, (isUnread ? 1 : 0));
            insertArticle.bindString(5, articleUrl);
            insertArticle.bindString(6, articleCommentUrl);
            insertArticle.bindLong(7, updateDate.getTime());
            insertArticle.bindString(8, att);
            insertArticle.executeInsert();
        }
        
        if (isExternalDBAvailable() && !content.equals("")) {
            content = DatabaseUtils.sqlEscapeString(content);
            db_extern.execSQL("REPLACE INTO " + TABLE_ARTICLES + " (id, feedId, content, isUnread, updateDate) VALUES"
                    + " (" + articleId + "," + feedId + "," + content + ",'" + isUnread + "'," + updateDate.getTime()  + "'," + att
                    + ")");
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
    
    public void insertArticles(List<ArticleItem> list, int number) {
        if (list == null)
            return;
        
        insertArticlesInternal(list);
        purgeArticlesNumber(number);
    }
    
    private synchronized void insertArticlesInternal(List<ArticleItem> list) {
        if (!isInternalDBAvailable())
            return;
        if (list == null)
            return;
        
        /*
         * TODO: Find a faster way to insert articles. Transactions like below should speed things up but this code
         * tends to crash once in a while with the following exception:
         * E/AndroidRuntime( 668): Caused by: android.database.sqlite.SQLiteException: cannot commit transaction - SQL
         * statements in progress: COMMIT;
         * E/AndroidRuntime( 668): at android.database.sqlite.SQLiteDatabase.native_execSQL(Native Method)
         * E/AndroidRuntime( 668): at android.database.sqlite.SQLiteDatabase.execSQL(SQLiteDatabase.java:1610)
         * E/AndroidRuntime( 668): at android.database.sqlite.SQLiteDatabase.endTransaction(SQLiteDatabase.java:505)
         * E/AndroidRuntime( 668): at org.ttrssreader.controllers.DBHelper.insertArticlesInternal(DBHelper.java:430)
         */

        if (!isExternalDBAvailable()) {
            // Only lock internal db-object to avoid NPE
            synchronized (db_intern) {
                db_intern.beginTransaction();
                try {
                    for (ArticleItem a : list) {
                        insertArticleInternal(a);
                    }
                    db_intern.setTransactionSuccessful();
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    db_intern.endTransaction();
                }
            }
            
        } else {
            
            synchronized (db_intern) {
                synchronized (db_extern) {
                    db_intern.beginTransaction();
                    db_extern.beginTransaction();
                    try {
                        for (ArticleItem a : list) {
                            insertArticleInternal(a);
                        }
                        db_intern.setTransactionSuccessful();
                        db_extern.setTransactionSuccessful();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } finally {
                        db_intern.endTransaction();
                        db_extern.endTransaction();
                    }
                }
            }
            
        }
    }
    
    // *******| UPDATE |*******************************************************************
    
    public void markCategoryRead(CategoryItem c, boolean recursive) {
        updateCategoryUnreadCount(c.getId(), 0);
        
        if (recursive) {
            for (FeedItem f : getFeeds(c)) {
                markFeedRead(f, recursive);
            }
        }
    }
    
    public void markFeedRead(FeedItem f, boolean recursive) {
        if (!isInternalDBAvailable())
            return;
        
        updateFeedUnreadCount(f.getId(), f.getCategoryId(), 0);
        
        if (recursive) {
            ContentValues cv = new ContentValues();
            cv.put("isUnread", 0);
            db_intern.update(TABLE_ARTICLES, cv, "feedId=?", new String[] {f.getId()+""});
            
            if (isExternalDBAvailable()) {
                db_extern.update(TABLE_ARTICLES, cv, "feedId=?", new String[] {f.getId()+""});
            }
        }
    }
    
    public void markArticlesRead(List<Integer> iDlist, int articleState) {
        if (!isInternalDBAvailable())
            return;
        
        // boolean isUnread = (articleState == 0 ? false : true);
        for (Integer id : iDlist) {
            ContentValues cv = new ContentValues();
            cv.put("isUnread", articleState);
            db_intern.update(TABLE_ARTICLES, cv, "id=?", new String[] {id+""});
            
            if (isExternalDBAvailable()) {
                db_extern.update(TABLE_ARTICLES, cv, "id=?", new String[] {id+""});
            }
        }
    }
    
    public void updateCategoryUnreadCount(int id, int count) {
        if (!isInternalDBAvailable())
            return;
        
        ContentValues cv = new ContentValues();
        cv.put("unread", count);
        db_intern.update(TABLE_CAT, cv, "id=?", new String[] {id+""});
    }
    
    public void updateCategoryDeltaUnreadCount(int id, int delta) {
        CategoryItem c = getCategory(id);
        int count = c.getUnreadCount();
        count += delta;
        
        updateCategoryUnreadCount(id, count);
    }
    
    public void updateFeedUnreadCount(int id, int categoryId, int count) {
        if (!isInternalDBAvailable())
            return;
        
        ContentValues cv = new ContentValues();
        cv.put("unread", count);
        db_intern.update(TABLE_FEEDS, cv, "id=? and categoryId=?", new String[] {id+"", categoryId+""});
    }
    
    public void updateFeedDeltaUnreadCount(int id, int categoryId, int delta) {
        FeedItem f = getFeed(id);
        int count = f.getUnread();
        count += delta;
        
        updateFeedUnreadCount(id, categoryId, count);
    }
    
    public void updateArticleUnread(int id, int feedId, boolean isUnread) {
        if (!isInternalDBAvailable())
            return;
        
        ContentValues cv = new ContentValues();
        cv.put("isUnread", isUnread);
        db_intern.update(TABLE_ARTICLES, cv, "id=? and feedId=?", new String[] {id+"", feedId+""});
        
        if (isExternalDBAvailable()) {
            db_extern.update(TABLE_ARTICLES, cv, "id=? and feedId=?", new String[] {id+"", feedId+""});
        }
    }
    
    public void updateArticleContent(ArticleItem a) {
        if (!isInternalDBAvailable())
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
        
        synchronized (updateArticle) {
            updateArticle.bindString(1, title);
            updateArticle.bindString(2, articleUrl);
            updateArticle.bindString(3, articleCommentUrl);
            updateArticle.bindLong(4, updateDate.getTime());
            updateArticle.bindLong(5, new Long(id));
            updateArticle.bindLong(6, new Long(feedId));
            updateArticle.bindString(7, att);
            updateArticle.execute();
        }
        
        if (isExternalDBAvailable() && !content.equals("")) {
            synchronized (updateArticle_extern) {
                content = DatabaseUtils.sqlEscapeString(content);
                updateArticle_extern.bindString(1, content);
                updateArticle_extern.bindLong(2, updateDate.getTime());
                updateArticle_extern.bindString(3, att);
                updateArticle_extern.bindLong(4, new Long(id));
                updateArticle_extern.bindLong(5, new Long(feedId));
                updateArticle_extern.execute();
            }
        }
    }
    
    public void deleteCategory(int id) {
        if (!isInternalDBAvailable())
            return;
        
        String[] args = new String[] {id+""};
        db_intern.delete(TABLE_CAT, "id=?", args);
    }
    
    public void deleteFeed(int id) {
        if (!isInternalDBAvailable())
            return;
        
        String[] args = new String[] {id+""};
        db_intern.delete(TABLE_FEEDS, "id=?", args);
    }
    
    public void deleteArticle(int id) {
        if (!isInternalDBAvailable())
            return;
        
        String[] args = new String[] {id+""};
        db_intern.delete(TABLE_ARTICLES, "id=?", args);
        
        if (isExternalDBAvailable()) {
            db_extern.delete(TABLE_ARTICLES, "id=?", args);
        }
    }
    
    public void deleteCategories(boolean withVirtualCategories) {
        if (!isInternalDBAvailable())
            return;
        
        String wherePart = "";
        if (!withVirtualCategories) {
            wherePart = "id > 0";
        }
        db_intern.delete(TABLE_CAT, wherePart, null);
    }
    
    public void deleteFeeds() {
        if (!isInternalDBAvailable())
            return;
        
        db_intern.delete(TABLE_FEEDS, null, null);
    }
    
    public void deleteArticles() {
        if (!isInternalDBAvailable())
            return;
        
        db_intern.delete(TABLE_ARTICLES, null, null);
        
        if (isExternalDBAvailable()) {
            db_extern.delete(TABLE_ARTICLES, null, null);
        }
    }
    
    public void purgeArticlesDays(Date olderThenThis) {
        if (!isInternalDBAvailable())
            return;
        
        String[] args = new String[] {olderThenThis.getTime()+""};
        
        db_intern.delete(TABLE_ARTICLES, "updateDate<?", args);
        
        if (isExternalDBAvailable()) {
            db_extern.delete(TABLE_ARTICLES, "isUnread=0 AND updateDate<?", args);
        }
    }
    
    public void purgeArticlesNumber(int number) {
        if (!isInternalDBAvailable())
            return;
        
        String[] args = new String[] {"select id from " + TABLE_ARTICLES + " ORDER BY updateDate DESC LIMIT -1 OFFSET " + number};
        
        db_intern.delete(TABLE_ARTICLES, "id in(?)", args);
        
        if (isExternalDBAvailable()) {
            db_extern.delete(TABLE_ARTICLES, "id in(?)", args);
        }
    }
    
    // *******| SELECT |*******************************************************************
    
    public ArticleItem getArticle(int id) {
        ArticleItem ret = null;
        if (!isInternalDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db_intern.query(TABLE_ARTICLES, null, "id=" + id, null, null, null, null, null);
            
            while (!c.isAfterLast()) {
                ret = handleArticleCursor(c, true);
                
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
        if (!isInternalDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db_intern.query(TABLE_FEEDS, null, "id=" + id, null, null, null, null, null);
            
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
        if (!isInternalDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db_intern.query(TABLE_CAT, null, "id=" + id, null, null, null, null, null);
            
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
    
    public List<ArticleItem> getArticles(FeedItem fi, boolean withContent) {
        List<ArticleItem> ret = new ArrayList<ArticleItem>();
        if (!isInternalDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db_intern.query(TABLE_ARTICLES, null, "feedId=" + fi.getId(), null, null, null, null, null);
            
            while (!c.isAfterLast()) {
                ret.add(handleArticleCursor(c, withContent));
                
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
    
    public List<FeedItem> getFeeds(CategoryItem ci) {
        List<FeedItem> ret = new ArrayList<FeedItem>();
        if (!isInternalDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db_intern.query(TABLE_FEEDS, null, "categoryId=" + ci.getId(), null, null, null, null, null);
            
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
    
    /**
     * Returns the maxArticles newest articles, mapped in lists to their feed-id.
     * Returns all articles if maxArticles is 0 or lower.
     */
    public Map<Integer, List<ArticleItem>> getArticles(int maxArticles, boolean withContent) {
        Map<Integer, List<ArticleItem>> ret = new HashMap<Integer, List<ArticleItem>>();
        if (!isInternalDBAvailable())
            return ret;
        
        String limit = (maxArticles > 0 ? String.valueOf(maxArticles) : null);
        
        Cursor c = null;
        try {
            c = db_intern.query(TABLE_ARTICLES, null, null, null, null, null, "updateDate DESC", limit);
            
            while (!c.isAfterLast()) {
                ArticleItem a = handleArticleCursor(c, withContent);
                int feedId = a.getFeedId();
                
                List<ArticleItem> list;
                if (ret.get(feedId) != null) {
                    list = ret.get(feedId);
                } else {
                    list = new ArrayList<ArticleItem>();
                }
                
                list.add(a);
                ret.put(feedId, list);
                
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
    
    public Map<Integer, List<FeedItem>> getFeeds() {
        Map<Integer, List<FeedItem>> ret = new HashMap<Integer, List<FeedItem>>();
        if (!isInternalDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db_intern.query(TABLE_FEEDS, null, null, null, null, null, null);
            
            while (!c.isAfterLast()) {
                FeedItem fi = handleFeedCursor(c);
                int catId = c.getInt(1);
                
                List<FeedItem> list;
                if (ret.get(catId) != null) {
                    list = ret.get(catId);
                } else {
                    list = new ArrayList<FeedItem>();
                }
                
                list.add(fi);
                ret.put(catId, list);
                
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
    
    public List<CategoryItem> getVirtualCategories() {
        List<CategoryItem> ret = new ArrayList<CategoryItem>();
        if (!isInternalDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db_intern.query(TABLE_CAT, null, "id < 1", null, null, null, null);
            
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
    
    public List<CategoryItem> getCategories(boolean withVirtualCategories) {
        List<CategoryItem> ret = new ArrayList<CategoryItem>();
        if (!isInternalDBAvailable())
            return ret;
        
        String wherePart = "id > 0";
        if (withVirtualCategories) {
            wherePart = null;
        }
        
        Cursor c = null;
        try {
            c = db_intern.query(TABLE_CAT, null, wherePart, null, null, null, null);
            
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
    
    /*
     * Equals the API-Call to getCounters
     */
    public Map<CategoryItem, List<FeedItem>> getCounters() {
        Map<CategoryItem, List<FeedItem>> ret = new HashMap<CategoryItem, List<FeedItem>>();
        
        for (CategoryItem c : getCategories(true)) {
            ret.put(c, getFeeds(c));
        }
        
        return ret;
    }
    
    public void setCounters(Map<CategoryItem, List<FeedItem>> map) {
        for (CategoryItem c : map.keySet()) {
            updateCategoryUnreadCount(c.getId(), c.getUnreadCount());
            
            List<FeedItem> list = map.get(c);
            if (list == null)
                continue;
            
            for (FeedItem f : list) {
                updateFeedUnreadCount(f.getId(), c.getId(), f.getUnread());
            }
        }
    }
    
    // *******************************************
    
    private ArticleItem handleArticleCursor(Cursor c, boolean withContent) {
        ArticleItem ret = null;
        
        if (c.isBeforeFirst()) {
            if (!c.moveToFirst()) {
                return ret;
            }
        }
        
        int id = c.getInt(0);
        String content = "";
        
        if (isExternalDBAvailable() && withContent) {
            
            String[] column = { "content" };
            Cursor content_cursor = db_extern.query(TABLE_ARTICLES, column, "id=" + id, null, null, null, null, null);
            content_cursor.moveToFirst();
            if (content_cursor.getCount() > 0) {
                content = content_cursor.getString(0);
            }
            content_cursor.close();
        }
        
        ret = new ArticleItem(c.getInt(1), // feedId
                id, // id
                c.getString(2), // title
                (c.getInt(3) != 0 ? true : false), // isUnread
                new Date(c.getLong(6)), // updateDate
                content, // content
                c.getString(4), // articleUrl
                c.getString(5), // articleCommentUrl
                parseAttachments(c.getString(6))
        );
        
        return ret;
    }
    
    private FeedItem handleFeedCursor(Cursor c) {
        FeedItem ret = null;
        
        if (c.isBeforeFirst()) {
            if (!c.moveToFirst()) {
                return ret;
            }
        }
        
        ret = new FeedItem(c.getInt(1), // categoryId
                c.getInt(0), // id
                c.getString(2), // title
                c.getString(3), // url
                c.getInt(4)); // unread
        
        return ret;
    }
    
    private CategoryItem handleCategoryCursor(Cursor c) {
        CategoryItem ret = null;
        
        if (c.isBeforeFirst()) {
            if (!c.moveToFirst()) {
                return ret;
            }
        }
        
        ret = new CategoryItem(c.getInt(0), // id
                c.getString(1), // title
                c.getInt(2)); // unread
        
        return ret;
    }
    
    private Set<String> parseAttachments(String att) {
        Set<String> ret = new HashSet<String>();
        
        for (String s : att.split(";")) {
            ret.add(s);
        }
        
        return ret;
    }
    
    private String parseAttachmentSet(Set<String> att) {
        StringBuilder ret = new StringBuilder();
        
        for (String s : att) {
            ret.append(s);
        }
        
        return DatabaseUtils.sqlEscapeString(ret.toString());
    }
    
}
