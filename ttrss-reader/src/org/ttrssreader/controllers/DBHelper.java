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
import java.util.List;
import java.util.Map;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.utils.Utils;
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
	
	private static final String DATABASE_PATH = "/Android/data/org.ttrssreader/files/";
	private static final String DATABASE_NAME = "ttrss.db";
	private static final int DATABASE_VERSION = 5;
	
	private static final String TABLE_CAT = "categories";
	private static final String TABLE_FEEDS = "feeds";
	private static final String TABLE_ARTICLES = "articles";
	
	private static final String INSERT_CAT = "REPLACE INTO " + TABLE_CAT
			+ "(id, title, unread) VALUES (?, ?, ?)";
	
	private static final String INSERT_FEEDS = "REPLACE INTO " + TABLE_FEEDS
			+ "(id, categoryId, title, url, unread) VALUES (?, ?, ?, ?, ?)";
	
	// private static final String INSERT_ARTICLES = "REPLACE INTO " + TABLE_ARTICLES
	// + "(id, feedId, title, isUnread, articleUrl, articleCommentUrl, updateDate) VALUES (?, ?, ?, ?, ?, ?, ?)";
	//	
	// private static final String INSERT_ARTICLES_EXTERN = "REPLACE INTO " + TABLE_ARTICLES
	// + "(id, feedId, content, isUnread, updateDate) VALUES (?, ?, ?, ?, ?)";
	
	private static final String UPDATE_ARTICLES = "UPDATE " + TABLE_ARTICLES
			+ " SET title=?, articleUrl=?, articleCommentUrl=?, updateDate=? WHERE id=? AND feedId=?";
	
	private static final String UPDATE_ARTICLES_EXTERN = "UPDATE " + TABLE_ARTICLES
			+ " SET content=?, updateDate=? WHERE id=? AND feedId=?";
	
	private Context context;
	
	private SQLiteDatabase db_intern;
	private SQLiteDatabase db_extern;
	
	private SQLiteStatement insertCat;
	private SQLiteStatement insertFeed;
	// private SQLiteStatement insertArticle;
	// private SQLiteStatement insertArticle_extern;
	private SQLiteStatement updateArticle;
	private SQLiteStatement updateArticle_extern;
	
	boolean externalDBState;
	
	public DBHelper() {
		context = null;
		
		db_intern = null;
		db_extern = null;
		
		insertCat = null;
		insertFeed = null;
		// insertArticle = null;
		// insertArticle_extern = null;
		updateArticle = null;
		updateArticle_extern = null;
		
		externalDBState = false;
	}
	
	public synchronized void initializeController(Context c) {
		context = c;
		
		handleDBUpdate();
		
		OpenHelper openHelper = new OpenHelper(context);
		db_intern = openHelper.getWritableDatabase();
		db_extern = openDatabase();
		
		db_intern.setLockingEnabled(false);
		db_extern.setLockingEnabled(false);
		
		insertCat = db_intern.compileStatement(INSERT_CAT);
		insertFeed = db_intern.compileStatement(INSERT_FEEDS);
		// insertArticle = db_intern.compileStatement(INSERT_ARTICLES);
		// insertArticle_extern = db_extern.compileStatement(INSERT_ARTICLES_EXTERN);
		updateArticle = db_intern.compileStatement(UPDATE_ARTICLES);
		updateArticle_extern = db_extern.compileStatement(UPDATE_ARTICLES_EXTERN);
		
	}
	
	public synchronized void checkAndInitializeController(Context context) {
		if (!mIsControllerInitialized) {
			initializeController(context);
			mIsControllerInitialized = true;
		}
	}
	
	public static DBHelper getInstance() {
		if (mInstance == null) {
			mInstance = new DBHelper();
		}
		return mInstance;
	}
	
	public SQLiteDatabase getInternalDb() {
		return this.db_intern;
	}
	
	public SQLiteDatabase getExternalDb() {
		return this.db_extern;
	}
	
	private void handleDBUpdate() {
		if (DATABASE_VERSION > Controller.getInstance().getDatabaseVersion()) {
			Log.i(Utils.TAG, "Database-Version: " + Controller.getInstance().getDatabaseVersion() + "(Internal: "
					+ DATABASE_VERSION + ")");
			
			OpenHelper openHelper = new OpenHelper(context);
			db_intern = openHelper.getWritableDatabase();
			db_extern = openDatabase();
			
			dropInternalDB();
			dropExternalDB();
			
			db_intern.close();
			db_extern.close();
		}
		
		Controller.getInstance().setDatabaseVersion(DATABASE_VERSION);
	}
	
	public void deleteAll() {
		db_intern.execSQL("DELETE FROM " + TABLE_CAT);
		db_intern.execSQL("DELETE FROM " + TABLE_FEEDS);
		db_intern.execSQL("DELETE FROM " + TABLE_ARTICLES);
		db_extern.execSQL("DELETE FROM " + TABLE_ARTICLES);
	}
	
	public void dropInternalDB() {
		if (context.deleteFile(DATABASE_NAME)) {
			Log.d(Utils.TAG, "dropInternalDB(): database deleted.");
		} else {
			Log.d(Utils.TAG, "dropInternalDB(): database NOT deleted.");
		}
	}
	
	public void dropExternalDB() {
		StringBuilder builder = new StringBuilder();
		builder.append(Environment.getExternalStorageDirectory()).append(File.separator).append(DATABASE_PATH).append(
				File.separator).append(DATABASE_NAME);
		
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
			db_extern.close();
			externalDBState = false;
		}
	}
	
	public boolean isExternalDBAvailable() {
		return externalDBState;
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
	public synchronized SQLiteDatabase openDatabase() throws SQLException {
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			StringBuilder builder = new StringBuilder();
			
			// open or create a new directory
			builder.setLength(0);
			builder.append(Environment.getExternalStorageDirectory()).append(File.separator).append(DATABASE_PATH);
			
			File dir = new File(builder.toString());
			dir.mkdirs();
			File file = new File(dir, DATABASE_NAME);
			
			try {
				Log.d(Utils.TAG, "Opening database: " + file.getAbsolutePath());
				db_extern = SQLiteDatabase.openOrCreateDatabase(file.getAbsolutePath(), null);
				
				// Create tables if they dont exist
				db_extern.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ARTICLES + " (" +
						"id INTEGER, " +
						"feedId INTEGER, " +
						"content TEXT, " +
						"isUnread INTEGER, " +
						"updateDate INTEGER, " +
						"PRIMARY KEY( id , feedId ))");
				
			} catch (SQLException e) {
				Log.e(Utils.TAG, "failed to open" + e);
				throw e;
			}
		}
		
		externalDBState = db_extern.isOpen();
		
		return db_extern;
	}
	
	private static class OpenHelper extends SQLiteOpenHelper {
		
		OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_CAT + " (" +
					"id INTEGER PRIMARY KEY, " +
					"title TEXT, " +
					"unread INTEGER)");
			
			db.execSQL("CREATE TABLE " + TABLE_FEEDS + " (" +
					"id INTEGER, " +
					"categoryId INTEGER, " +
					"title TEXT, " +
					"url TEXT, " +
					"unread INTEGER, " +
					"PRIMARY KEY( id, categoryId ))");
			
			db.execSQL("CREATE TABLE " + TABLE_ARTICLES + " (" +
					"id INTEGER, " +
					"feedId INTEGER, " +
					"title TEXT, " +
					"isUnread INTEGER, " +
					"articleUrl TEXT, " +
					"articleCommentUrl TEXT, " +
					"updateDate INTEGER, " +
					"PRIMARY KEY( id , feedId ))");
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
	
	private void insertCategory(String id, String title, int unread) {
		if (id == null) return;
		if (title == null) title = "";
		
		synchronized (insertCat) {
			insertCat.bindString(1, id);
			insertCat.bindString(2, DatabaseUtils.sqlEscapeString(title));
			insertCat.bindLong(3, unread);
			insertCat.execute();
		}
	}
	
	public void insertCategory(CategoryItem c) {
		if (c == null) return;
		
		insertCategory(c.getId(), c.getTitle(), c.getUnreadCount());
	}
	
	public void insertCategories(List<CategoryItem> list) {
		if (list == null) return;
		
		for (CategoryItem c : list) {
			insertCategory(
					c.getId(),
					c.getTitle(),
					c.getUnreadCount());
		}
	}
	
	private void insertFeed(String feedId, String categoryId, String title, String url, int unread) {
		if (feedId == null) return;
		if (categoryId == null) return;
		if (title == null) title = "";
		if (url == null) url = "";
		
		synchronized (insertFeed) {
			insertFeed.bindLong(1, new Integer(feedId).longValue());
			insertFeed.bindLong(2, new Integer(categoryId).longValue());
			insertFeed.bindString(3, DatabaseUtils.sqlEscapeString(title));
			insertFeed.bindString(4, DatabaseUtils.sqlEscapeString(url));
			insertFeed.bindLong(5, unread);
			insertFeed.execute();
		}
	}
	
	public void insertFeed(FeedItem f) {
		if (f == null || f.getCategoryId().startsWith("-")) {
			return;
		}
		
		insertFeed(
				f.getId(),
				f.getCategoryId(),
				f.getTitle(),
				f.getUrl(),
				f.getUnread());
	}
	
	public void insertFeeds(List<FeedItem> list) {
		if (list == null) return;
		
		for (FeedItem f : list) {
			insertFeed(f);
		}
	}
	
	private void insertArticle(String articleId, String feedId, String title, boolean isUnread, String content,
			String articleUrl, String articleCommentUrl, Date updateDate) {
		
		if (articleId == null) return;
		if (feedId == null) return;
		if (title == null) title = "";
		if (content == null) content = "";
		if (articleUrl == null) articleUrl = "";
		if (articleCommentUrl == null) articleCommentUrl = "";
		if (updateDate == null) updateDate = new Date(System.currentTimeMillis());
		
		title = DatabaseUtils.sqlEscapeString(title);
		articleUrl = DatabaseUtils.sqlEscapeString(articleUrl);
		articleCommentUrl = DatabaseUtils.sqlEscapeString(articleCommentUrl);
		
		db_intern.execSQL("REPLACE INTO " + TABLE_ARTICLES +
				" (id, feedId, title, isUnread, articleUrl, articleCommentUrl, updateDate) VALUES" +
				" ('" + articleId + "','" + feedId + "'," + title + ",'" + isUnread + "'," + articleUrl + ","
				+ articleCommentUrl + ",'" + updateDate.getTime() + "')");
		
		if (isExternalDBAvailable() && !content.equals("")) {
			content = DatabaseUtils.sqlEscapeString(content);
			db_extern.execSQL("REPLACE INTO " + TABLE_ARTICLES +
					" (id, feedId, content, isUnread, updateDate) VALUES" +
					" ('" + articleId + "','" + feedId + "'," + content + ",'" + isUnread + "','"
					+ updateDate.getTime() + "')");
		}
	}
	
	public void insertArticle(ArticleItem a, int number) {
		if (a == null) return;
		
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
				a.getUpdateDate());
	}
	
	public void insertArticles(List<ArticleItem> list, int number) {
		if (list == null) return;
		
		insertArticlesInternal(list);
		purgeArticlesNumber(number);
	}
	
	private synchronized void insertArticlesInternal(List<ArticleItem> list) {
		if (list == null) return;

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
		updateFeedUnreadCount(f.getId(), f.getCategoryId(), 0);
		
		if (recursive) {
			db_intern.execSQL("UPDATE " + TABLE_ARTICLES +
					" SET isUnread='0' WHERE feedId='" + f.getId() + "'");
			
			if (isExternalDBAvailable()) {
				db_extern.execSQL("UPDATE " + TABLE_ARTICLES +
						" SET isUnread='0' WHERE feedId='" + f.getId() + "'");
			}
		}
	}
	
	public void markArticlesRead(List<String> list, int articleState) {
		boolean isUnread = articleState == 0 ? false : true;
		for (String id : list) {
			db_intern.execSQL("UPDATE " + TABLE_ARTICLES +
					" SET isUnread='" + isUnread + "' " + "WHERE id='" + id + "'");
			
			if (isExternalDBAvailable()) {
				db_extern.execSQL("UPDATE " + TABLE_ARTICLES +
						" SET isUnread='" + isUnread + "' " + "WHERE id='" + id + "'");
			}
		}
	}
	
	public void updateCategoryUnreadCount(String id, int count) {
		if (id == null) return;
		
		db_intern.execSQL("UPDATE " + TABLE_CAT +
				" SET unread='" + count + "' " + "WHERE id='" + id + "'");
	}
	
	public void updateCategoryDeltaUnreadCount(String id, int delta) {
		if (id == null) return;
		
		CategoryItem c = getCategory(id);
		int count = c.getUnreadCount();
		count += delta;
		
		updateCategoryUnreadCount(id, count);
	}
	
	public void updateFeedUnreadCount(String id, String categoryId, int count) {
		if (id == null || categoryId == null) return;
		
		db_intern.execSQL("UPDATE " + TABLE_FEEDS +
				" SET unread='" + count + "' " + "WHERE id='" + id + "' and categoryId='" + categoryId + "'");
	}
	
	public void updateFeedDeltaUnreadCount(String id, String categoryId, int delta) {
		if (id == null || categoryId == null) return;
		
		FeedItem f = getFeed(id);
		int count = f.getUnread();
		count += delta;
		
		updateFeedUnreadCount(id, categoryId, count);
	}
	
	public void updateArticleUnread(String id, String feedId, boolean isUnread) {
		if (id == null || feedId == null) return;
		
		db_intern.execSQL("UPDATE " + TABLE_ARTICLES +
				" SET isUnread='" + isUnread + "' " + "WHERE id='" + id + "' and feedId='" + feedId + "'");
		
		if (isExternalDBAvailable()) {
			db_extern.execSQL("UPDATE " + TABLE_ARTICLES +
					" SET isUnread='" + isUnread + "' " + "WHERE id='" + id + "' and feedId='" + feedId + "'");
		}
	}
	
	public void updateArticleContent(ArticleItem a) {
		
		String id = a.getId();
		String feedId = a.getFeedId();
		String content = a.getContent();
		String title = a.getTitle();
		String articleUrl = a.getArticleUrl();
		String articleCommentUrl = a.getArticleCommentUrl();
		Date updateDate = a.getUpdateDate();
		
		title = DatabaseUtils.sqlEscapeString(title);
		articleUrl = DatabaseUtils.sqlEscapeString(articleUrl);
		articleCommentUrl = DatabaseUtils.sqlEscapeString(articleCommentUrl);
		
		if (content == null) content = "";
		if (title == null) title = "";
		if (articleUrl == null) articleUrl = "";
		if (articleCommentUrl == null) articleCommentUrl = "";
		if (updateDate == null) updateDate = new Date();
		
		synchronized (updateArticle) {
			updateArticle.bindString(1, title);
			updateArticle.bindString(2, articleUrl);
			updateArticle.bindString(3, articleCommentUrl);
			updateArticle.bindLong(4, updateDate.getTime());
			updateArticle.bindLong(5, new Long(id));
			updateArticle.bindLong(6, new Long(feedId));
			updateArticle.execute();
		}
		
		if (isExternalDBAvailable() && !content.equals("")) {
			synchronized (updateArticle_extern) {
				content = DatabaseUtils.sqlEscapeString(content);
				updateArticle_extern.bindString(1, DatabaseUtils.sqlEscapeString(content));
				updateArticle_extern.bindLong(2, updateDate.getTime());
				updateArticle_extern.bindLong(3, new Long(id));
				updateArticle_extern.bindLong(4, new Long(feedId));
				updateArticle_extern.execute();
			}
		}
	}
	
	public void deleteCategory(String id) {
		db_intern.execSQL("DELETE FROM " + TABLE_CAT + " WHERE id=" + id);
	}
	
	public void deleteFeed(String id) {
		db_intern.execSQL("DELETE FROM " + TABLE_FEEDS + " WHERE id=" + id);
	}
	
	public void deleteArticle(String id) {
		db_intern.execSQL("DELETE FROM " + TABLE_ARTICLES + " WHERE id=" + id);
		
		if (isExternalDBAvailable()) {
			db_extern.execSQL("DELETE FROM " + TABLE_ARTICLES + " WHERE id=" + id);
		}
	}
	
	public void deleteCategories(boolean withVirtualCategories) {
		String wherePart = "";
		if (!withVirtualCategories) {
			wherePart = " WHERE id not like '-%' OR id!=0";
		}
		db_intern.execSQL("DELETE FROM " + TABLE_CAT + wherePart);
	}
	
	public void deleteFeeds() {
		db_intern.execSQL("DELETE FROM " + TABLE_FEEDS);
	}
	
	public void deleteArticles() {
		db_intern.execSQL("DELETE FROM " + TABLE_ARTICLES);
		
		if (isExternalDBAvailable()) {
			db_extern.execSQL("DELETE FROM " + TABLE_ARTICLES);
		}
	}
	
	public void purgeArticlesDays(Date olderThenThis) {
		db_intern.execSQL("DELETE FROM " + TABLE_ARTICLES + " WHERE isUnread=0 AND updateDate<"
				+ olderThenThis.getTime());
		
		if (isExternalDBAvailable()) {
			// Dont rely on isUnread here, article could have been marked as read while sdcard wasnt available
			db_extern.execSQL("DELETE FROM " + TABLE_ARTICLES + " WHERE updateDate<" + olderThenThis.getTime());
		}
	}
	
	public void purgeArticlesNumber(int number) {
		db_intern.execSQL("DELETE FROM " + TABLE_ARTICLES +
				" WHERE id in( select id from " + TABLE_ARTICLES +
				" WHERE isUnread=0" +
				" ORDER BY updateDate DESC" +
				" LIMIT -1 OFFSET " + number + ")");
		
		if (isExternalDBAvailable()) {
			// Dont rely on isUnread here, article could have been marked as read while sdcard wasnt available
			db_extern.execSQL("DELETE FROM " + TABLE_ARTICLES +
					" WHERE id in( select id from " + TABLE_ARTICLES +
					" ORDER BY updateDate DESC" +
					" LIMIT -1 OFFSET " + number + ")");
		}
	}
	
	// *******| SELECT |*******************************************************************
	
	public ArticleItem getArticle(String id) {
		ArticleItem ret = null;
		
		Cursor c = db_intern.query(TABLE_ARTICLES, null, "id=" + id, null, null, null, null, null);
		
		while (!c.isAfterLast()) {
			ret = handleArticleCursor(c, true);
			
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	public FeedItem getFeed(String id) {
		FeedItem ret = new FeedItem();
		
		Cursor c = db_intern.query(TABLE_FEEDS, null, "id=" + id, null, null, null, null, null);
		
		while (!c.isAfterLast()) {
			ret = handleFeedCursor(c);
			
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	public CategoryItem getCategory(String id) {
		CategoryItem ret = new CategoryItem();
		
		Cursor c = db_intern.query(TABLE_CAT, null, "id=" + id, null, null, null, null, null);
		
		while (!c.isAfterLast()) {
			ret = handleCategoryCursor(c);
			
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	public List<ArticleItem> getArticles(FeedItem fi, boolean withContent) {
		List<ArticleItem> ret = new ArrayList<ArticleItem>();
		
		Cursor c = db_intern.query(TABLE_ARTICLES, null, "feedId=" + fi.getId(), null, null, null, null, null);
		
		while (!c.isAfterLast()) {
			ret.add(handleArticleCursor(c, withContent));
			
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	public List<FeedItem> getFeeds(CategoryItem ci) {
		List<FeedItem> ret = new ArrayList<FeedItem>();
		
		Cursor c = db_intern.query(TABLE_FEEDS, null, "categoryId=" + ci.getId(), null, null, null, null, null);
		
		while (!c.isAfterLast()) {
			ret.add(handleFeedCursor(c));
			
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	/**
	 * Returns the maxArticles newest articles, mapped in lists to their feed-id.
	 * Returns all articles if maxArticles is 0 or lower.
	 */
	public Map<String, List<ArticleItem>> getArticles(int maxArticles, boolean withContent) {
		Map<String, List<ArticleItem>> ret = new HashMap<String, List<ArticleItem>>();
		
		String limit = (maxArticles > 0 ? String.valueOf(maxArticles) : null);
		
		Cursor c = db_intern.query(TABLE_ARTICLES, null, null, null, null, null, "updateDate DESC", limit);
		
		while (!c.isAfterLast()) {
			ArticleItem a = handleArticleCursor(c, withContent);
			String feedId = a.getFeedId();
			
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
		c.close();
		
		return ret;
	}
	
	public Map<String, List<FeedItem>> getFeeds() {
		Map<String, List<FeedItem>> ret = new HashMap<String, List<FeedItem>>();
		
		Cursor c = db_intern.query(TABLE_FEEDS, null, null, null, null, null, null);
		
		while (!c.isAfterLast()) {
			FeedItem fi = handleFeedCursor(c);
			String catId = c.getString(1);
			
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
		c.close();
		
		return ret;
	}
	
	public List<CategoryItem> getVirtualCategories() {
		List<CategoryItem> ret = new ArrayList<CategoryItem>();
		
		Cursor c = db_intern.query(TABLE_CAT, null, "id like '-%' OR id=0", null, null, null, null);
		
		while (!c.isAfterLast()) {
			CategoryItem ci = handleCategoryCursor(c);
			
			ret.add(ci);
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	public List<CategoryItem> getCategories(boolean withVirtualCategories) {
		List<CategoryItem> ret = new ArrayList<CategoryItem>();
		
		String wherePart = "id not like '-%' AND id!=0";
		if (withVirtualCategories) {
			wherePart = null;
		}
		
		Cursor c = db_intern.query(TABLE_CAT, null, wherePart, null, null, null, null);
		
		while (!c.isAfterLast()) {
			CategoryItem ci = handleCategoryCursor(c);
			
			ret.add(ci);
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	/*
	 * Equal to the API-Call to getCounters
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
			if (list == null) continue;
			
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
		
		String id = c.getString(0);
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
		
		ret = new ArticleItem(
				c.getString(1), // feedId
				id, // id
				c.getString(2), // title
				(c.getInt(3) != 0 ? true : false), // isUnread
				new Date(c.getLong(6)), // updateDate
				content, // content
				c.getString(4), // articleUrl
				c.getString(5) // articleCommentUrl
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
		
		ret = new FeedItem(
				c.getString(1), // categoryId
				c.getString(0), // id
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
		
		ret = new CategoryItem(
				c.getString(0), // id
				c.getString(1), // title
				c.getInt(2)); // unread
		
		return ret;
	}
}
