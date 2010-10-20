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

import java.io.File;
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
import android.os.Environment;
import android.util.Log;

public class DBHelper {
    
    // TODO: Check if article/feed/category is already in DB instead of just using UPDATE.
    
    private static DBHelper mInstance = null;
    private boolean mIsDBInitialized = false;
    
    private static final String DATABASE_NAME = "ttrss.db";
    private static final int DATABASE_VERSION = 21;
    
    private static final String TABLE_CAT = "categories";
    private static final String TABLE_FEEDS = "feeds";
    private static final String TABLE_ARTICLES = "articles";
    
    // @formatter:off
    private static final String INSERT_CAT = 
        "REPLACE INTO "
        + TABLE_CAT
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
        + " (id, feedId, title, isUnread, articleUrl, articleCommentUrl, updateDate)" 
        + " VALUES (?, ?, ?, ?, ?, ?, ?)";
    
    private static final String INSERT_ARTICLES_EXTERN =
        "REPLACE INTO "
        + TABLE_ARTICLES 
        + " (id, feedId, content, isUnread, updateDate, attachments)"
        + " VALUES (?, ?, ?, ?, ?, ?)";
    
    private static final String UPDATE_ARTICLES =
        "UPDATE "
        + TABLE_ARTICLES
        + " SET title=?,"
        + "  articleUrl=?,"
        + "  articleCommentUrl=?,"
        + "  updateDate=?"
        + " WHERE id=?"
        + " AND feedId=?";
    
    private static final String UPDATE_ARTICLES_EXTERN = 
        "UPDATE "
        + TABLE_ARTICLES
        + " SET content=?,"
        + "  updateDate=?,"
        + "  attachments=?"
        + " WHERE id=?"
        + " AND feedId=?";
    // @formatter:on
    
    private SQLiteDatabase db_intern;
    private SQLiteDatabase db_extern;
    
    private SQLiteStatement insertCat;
    private SQLiteStatement insertFeed;
    private SQLiteStatement insertArticle;
    private SQLiteStatement insertArticle_extern;
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
            insertArticle_extern = db_extern.compileStatement(INSERT_ARTICLES_EXTERN);
            updateArticle_extern = db_extern.compileStatement(UPDATE_ARTICLES_EXTERN);
        }
        return true;
        
    }
    
    public synchronized void checkAndInitializeDB(Context context) {
        this.context = context;
        
        if (!mIsDBInitialized) {
            mIsDBInitialized = initializeController();
        } else if(db_intern == null || !db_intern.isOpen()) {
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
        closeDB();
        mInstance = null;
        mIsDBInitialized = false;
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
            Log.w(Utils.TAG, "Controller not initialized, trying to do that now...");
            mIsDBInitialized = initializeController();
            return mIsDBInitialized;
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
                // @formatter:off
                db_extern.execSQL(
                        "CREATE TABLE IF NOT EXISTS "
                        + TABLE_ARTICLES
                        + " (id INTEGER PRIMARY KEY," 
                        + " feedId INTEGER," 
                        + " content TEXT," 
                        + " isUnread INTEGER," 
                        + " updateDate INTEGER," 
                        + " attachments TEXT)");
                // @formatter:on
                
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
            // @formatter:off
            db.execSQL(
                    "CREATE TABLE "
                    + TABLE_CAT 
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
                    + " updateDate INTEGER)");
            // @formatter:on
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
        
        synchronized (db_intern) {
            insertCat.bindLong(1, id);
            insertCat.bindString(2, title);
            insertCat.bindLong(3, unread);
            insertCat.execute();
        }
    }
    
    public void insertCategory(CategoryItem c) {
        if (c == null)
            return;
        
        insertCategory(
                c.getId(),
                c.getTitle(), 
                c.getUnread());
    }
    
    public void insertCategories(Set<CategoryItem> set) {
        if (set == null)
            return;
        
        for (CategoryItem c : set) {
            insertCategory(
                    c.getId(), 
                    c.getTitle(),
                    c.getUnread());
        }
    }
    
    private void insertFeed(int feedId, int categoryId, String title, String url, int unread) {
        if (!isInternalDBAvailable())
            return;
        
        if (title == null)
            title = "";
        if (url == null)
            url = "";
        
        synchronized (db_intern) {
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
        
        insertFeed(
                f.getId(),
                f.getCategoryId(),
                f.getTitle(), 
                f.getUrl(), 
                f.getUnread());
    }
    
    public void insertFeeds(Set<FeedItem> set) {
        if (set == null)
            return;
        
        for (FeedItem f : set) {
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
            attachments = new LinkedHashSet<String>();
        
        String att = parseAttachmentSet(attachments);
        
        synchronized (db_intern) {
            insertArticle.bindLong(1, articleId);
            insertArticle.bindLong(2, feedId);
            insertArticle.bindString(3, title);
            insertArticle.bindLong(4, (isUnread ? 1 : 0));
            insertArticle.bindString(5, articleUrl);
            insertArticle.bindString(6, articleCommentUrl);
            insertArticle.bindLong(7, updateDate.getTime());
            insertArticle.executeInsert();
        }
        
        if (isExternalDBAvailable() && !content.equals("")) {
            content = DatabaseUtils.sqlEscapeString(content);
            synchronized (db_extern) {
                insertArticle_extern.bindLong(1, articleId);
                insertArticle_extern.bindLong(2, feedId);
                insertArticle_extern.bindString(3, content);
                insertArticle_extern.bindLong(4, (isUnread ? 1 : 0));
                insertArticle_extern.bindLong(5, updateDate.getTime());
                insertArticle_extern.bindString(6, att);
                insertArticle_extern.executeInsert();
            }
        }
    }
    
    public void insertArticle(ArticleItem a, int number) {
        if (a == null)
            return;
        
        insertArticleInternal(a);
        purgeArticlesNumber(number);
    }
    
    private void insertArticleInternal(ArticleItem a) {
        insertArticle(
                a.getId(),
                a.getFeedId(),
                a.getTitle(),
                a.isUnread(), 
                a.getContent(),
                a.getArticleUrl(),
                a.getArticleCommentUrl(),
                a.getUpdateDate(), 
                a.getAttachments());
    }
    
    public void insertArticles(Set<ArticleItem> list, int number) {
        if (list == null)
            return;
        
        insertArticlesInternal(list);
        purgeArticlesNumber(number);
    }
    
    private void insertArticlesInternal(Set<ArticleItem> set) {
        if (!isInternalDBAvailable())
            return;
        if (set == null)
            return;

        synchronized (db_intern) {
            db_intern.beginTransaction();
            try {
                for (ArticleItem a : set) {
                    insertArticleInternal(a);
                }
                db_intern.setTransactionSuccessful();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                db_intern.endTransaction();
            }
        }
        
        if (isExternalDBAvailable()) {
            synchronized (db_extern) {
                db_extern.beginTransaction();
                try {
                    for (ArticleItem a : set) {
                        insertArticleInternal(a);
                    }
                    db_extern.setTransactionSuccessful();
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    db_extern.endTransaction();
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
            db_intern.update(TABLE_ARTICLES, cv, "feedId=?", new String[] { f.getId() + "" });
            
            if (isExternalDBAvailable()) {
                db_extern.update(TABLE_ARTICLES, cv, "feedId=?", new String[] { f.getId() + "" });
            }
        }
    }
    
    public void markArticlesRead(Set<Integer> iDlist, int articleState) {
        if (!isInternalDBAvailable())
            return;
        
        // boolean isUnread = (articleState == 0 ? false : true);
        for (Integer id : iDlist) {
            ContentValues cv = new ContentValues();
            cv.put("isUnread", articleState);
            db_intern.update(TABLE_ARTICLES, cv, "id=?", new String[] { id + "" });
            
            if (isExternalDBAvailable()) {
                db_extern.update(TABLE_ARTICLES, cv, "id=?", new String[] { id + "" });
            }
        }
    }
    
    public void updateCategoryUnreadCount(int id, int count) {
        if (!isInternalDBAvailable())
            return;
        
        ContentValues cv = new ContentValues();
        cv.put("unread", count);
        db_intern.update(TABLE_CAT, cv, "id=?", new String[] { id + "" });
    }
    
    public void updateCategoryDeltaUnreadCount(int id, int delta) {
        CategoryItem c = getCategory(id);
        int count = c.getUnread();
        count += delta;
        
        updateCategoryUnreadCount(id, count);
    }
    
    public void updateFeedUnreadCount(int id, int categoryId, int count) {
        if (!isInternalDBAvailable())
            return;
        
        ContentValues cv = new ContentValues();
        cv.put("unread", count);
        db_intern.update(TABLE_FEEDS, cv, "id=? and categoryId=?", new String[] { id + "", categoryId + "" });
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
        db_intern.update(TABLE_ARTICLES, cv, "id=? and feedId=?", new String[] { id + "", feedId + "" });
        
        if (isExternalDBAvailable()) {
            db_extern.update(TABLE_ARTICLES, cv, "id=? and feedId=?", new String[] { id + "", feedId + "" });
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
        
        synchronized (db_intern) {
            updateArticle.bindString(1, title);
            updateArticle.bindString(2, articleUrl);
            updateArticle.bindString(3, articleCommentUrl);
            updateArticle.bindLong(4, updateDate.getTime());
            updateArticle.bindLong(5, new Long(id));
            updateArticle.bindLong(6, new Long(feedId));
            updateArticle.execute();
        }
        
        if (isExternalDBAvailable() && !content.equals("")) {
            synchronized (db_extern) {
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
        
        String[] args = new String[] { id + "" };
        db_intern.delete(TABLE_CAT, "id=?", args);
    }
    
    public void deleteFeed(int id) {
        if (!isInternalDBAvailable())
            return;
        
        String[] args = new String[] { id + "" };
        db_intern.delete(TABLE_FEEDS, "id=?", args);
    }
    
    public void deleteArticle(int id) {
        if (!isInternalDBAvailable())
            return;
        
        String[] args = new String[] { id + "" };
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
        
        String[] args = new String[] { olderThenThis.getTime() + "" };
        
        db_intern.delete(TABLE_ARTICLES, "updateDate<?", args);
        
        if (isExternalDBAvailable()) {
            db_extern.delete(TABLE_ARTICLES, "isUnread=0 AND updateDate<?", args);
        }
    }
    
    public void purgeArticlesNumber(int number) {
        if (!isInternalDBAvailable())
            return;
        
        String[] args = new String[] { "select id from " + TABLE_ARTICLES
                + " ORDER BY updateDate DESC LIMIT -1 OFFSET " + number };
        
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
    
    public Set<ArticleItem> getArticles(FeedItem fi, boolean withContent) {
        Set<ArticleItem> ret = new LinkedHashSet<ArticleItem>();
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
    
    public Set<FeedItem> getFeeds(CategoryItem ci) {
        Set<FeedItem> ret = new LinkedHashSet<FeedItem>();
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
    public Map<Integer, Set<ArticleItem>> getArticles(int maxArticles, boolean withContent) {
        Map<Integer, Set<ArticleItem>> ret = new HashMap<Integer, Set<ArticleItem>>();
        if (!isInternalDBAvailable())
            return ret;
        
        String limit = (maxArticles > 0 ? String.valueOf(maxArticles) : null);
        
        Cursor c = null;
        try {
            c = db_intern.query(TABLE_ARTICLES, null, null, null, null, null, "updateDate DESC", limit);
            
            while (!c.isAfterLast()) {
                ArticleItem a = handleArticleCursor(c, withContent);
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
        if (!isInternalDBAvailable())
            return ret;
        
        Cursor c = null;
        try {
            c = db_intern.query(TABLE_FEEDS, null, null, null, null, null, null);
            
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
    
    public Set<CategoryItem> getCategories(boolean withVirtualCategories) {
        Set<CategoryItem> ret = new LinkedHashSet<CategoryItem>();
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
    public Map<CategoryItem, Set<FeedItem>> getCounters() {
        Map<CategoryItem, Set<FeedItem>> ret = new HashMap<CategoryItem, Set<FeedItem>>();
        
        for (CategoryItem c : getCategories(true)) {
            ret.put(c, getFeeds(c));
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
        
        // @formatter:off
        ret = new ArticleItem(c.getInt(1),          // feedId
                id,                                 // id
                c.getString(2),                     // title
                (c.getInt(3) != 0 ? true : false),  // isUnread
                new Date(c.getLong(6)),             // updateDate
                content,                            // content
                c.getString(4),                     // articleUrl
                c.getString(5),                     // articleCommentUrl
                parseAttachments(c.getString(6))
        );
        // @formatter:on
        
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
            ret.deleteCharAt(ret.length()-1);
        }
        
        return DatabaseUtils.sqlEscapeString(ret.toString());
    }
    
}
