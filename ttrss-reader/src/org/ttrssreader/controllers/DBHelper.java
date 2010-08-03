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

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.utils.Base64;
import org.ttrssreader.utils.Utils;

public class DBHelper {
	
	private static DBHelper mInstance = null;
	private boolean mIsControllerInitialized = false;
//	private boolean mDBLocationSDCard = false;

//	private static final String DATABASE_PATH = "ttrss-reader-fork";
	private static final String DATABASE_NAME = "ttrss.db";
	private static final int DATABASE_VERSION = 1;
	
	private static final String TABLE_CAT = "categories";
	private static final String TABLE_FEEDS = "feeds";
	private static final String TABLE_ARTICLES = "articles";
	
	private static final String INSERT_CAT = "REPLACE INTO " + TABLE_CAT + "(id, title, unread) VALUES (?, ?, ?)";
	private static final String INSERT_FEEDS = "REPLACE INTO " + TABLE_FEEDS
			+ "(id, categoryId, title, url, unread) VALUES (?, ?, ?, ?, ?)";
	private static final String INSERT_ARTICLES = "REPLACE INTO "
			+ TABLE_ARTICLES
			+ "(id, feedId, title, isUnread, content, articleUrl, articleCommentUrl, updateDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String UPDATE_ARTICLES = "UPDATE " + TABLE_ARTICLES
			+ " SET content=?, title=?, articleUrl=?, articleCommentUrl=?, updateDate=? WHERE id=? AND feedId=?";
	
	private Context context;
	private SQLiteDatabase db;
	private SQLiteStatement insertCat;
	private SQLiteStatement insertFeed;
	private SQLiteStatement insertArticle;
	private SQLiteStatement updateArticle;
	
	public DBHelper() {
		
		context = null;
		db = null;
		insertCat = null;
		insertFeed = null;
		insertArticle = null;
		updateArticle = null;
	}

	public void initializeController(Context c) {
		context = c;

		OpenHelper openHelper = new OpenHelper(context);
		db = openHelper.getWritableDatabase();		

		insertCat = db.compileStatement(INSERT_CAT);
		insertFeed = db.compileStatement(INSERT_FEEDS);
		insertArticle = db.compileStatement(INSERT_ARTICLES);
		updateArticle = db.compileStatement(UPDATE_ARTICLES);
	}
	
	public synchronized void checkAndInitializeController(Context context) {
		if (!mIsControllerInitialized) {
			
			initializeController(context);
			
			mIsControllerInitialized = true;
		}
	}
	
//	public void switchDB(Context c) {
//		if (mDBLocationSDCard) {
//			OpenHelper openHelper = new OpenHelper(context);
//			db = openHelper.getWritableDatabase();
//			
//			insertCat = db.compileStatement(INSERT_CAT);
//			insertFeed = db.compileStatement(INSERT_FEEDS);
//			insertArticle = null;
//			updateArticle = null;
//		
//			mDBLocationSDCard = false;
//		} else {
//			openDatabase();
//			
//			insertCat = db.compileStatement(INSERT_CAT);
//			insertFeed = db.compileStatement(INSERT_FEEDS);
//			insertArticle = db.compileStatement(INSERT_ARTICLES);
//			updateArticle = db.compileStatement(UPDATE_ARTICLES);
//			
//			mDBLocationSDCard = true;
//		}
//	}
	
	public static DBHelper getInstance() {
		if (mInstance == null) {
			mInstance = new DBHelper();
		}
		return mInstance;
	}
	
	public SQLiteDatabase getDb() {
		return this.db;
	}
	
	public void deleteAll() {
		db.execSQL("DELETE FROM " + TABLE_CAT);
		db.execSQL("DELETE FROM " + TABLE_FEEDS);
		db.execSQL("DELETE FROM " + TABLE_ARTICLES);
	}
	
	public void dropDB() {
		if (context.deleteDatabase(DATABASE_NAME)) {
			Log.d(Utils.TAG, "deleteDatabase(): database deleted.");
		} else {
			Log.d(Utils.TAG, "deleteDatabase(): database NOT deleted.");
		}
	}
	
//	public boolean isSDCardAvailable() {
//		return mDBLocationSDCard;
//	}
//
//	public void setSDCardAvailable(boolean sDCardAvailable) {
//		this.mDBLocationSDCard = sDCardAvailable;
//	}
//	
//	/**
//	 * Opens the SDcard database. If it cannot be opened, it
//	 * creates a new instance. If a new instance cannot be created, it throws
//	 * an exception and logs the failure.
//	 * 
//	 * @return true if successful
//	 * @throws SQLException
//	 *             if the database is unable to be opened or created
//	 */
//	public synchronized boolean openDatabase() throws SQLException {
//		if (db != null && db.isOpen()) {
//			return true;
//		} else if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
//			Log.w(Utils.TAG, "SDcard not mounted, cant access article-database.");
//			return false;
//		} else {
//			StringBuilder builder = new StringBuilder();
//			
//			// open or create a new directory
//			builder.setLength(0);
//			builder.append(Environment.getExternalStorageDirectory()).append(File.separator).append(DATABASE_PATH);
//			
//			File dir = new File(builder.toString());
//			dir.mkdirs();
//			File file = new File(dir, DATABASE_NAME);
//			
//			try {
//				Log.d(Utils.TAG, "Opening database: " + file.getAbsolutePath());
//				db = SQLiteDatabase.openOrCreateDatabase(file.getAbsolutePath(), null);
//				
//				// Create tables if they dont exist
//				createTables();
//			} catch (SQLException e) {
//				Log.e(Utils.TAG, "failed to open" + e);
//				throw e;
//			}
//		}
//		return true;
//	}
	
	public synchronized void createTables() {
		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CAT + " (" + 
				"id INTEGER PRIMARY KEY, " + 
				"title TEXT, " + 
				"unread INTEGER)");
		
		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FEEDS + " (" + 
				"id INTEGER, " + 
				"categoryId INTEGER, " + 
				"title TEXT, " +
				"url TEXT, " + 
				"unread INTEGER, " + 
				"PRIMARY KEY( id, categoryId ))");
		
		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ARTICLES + " (" + 
				"id INTEGER, " + 
				"feedId INTEGER, " + 
				"title TEXT, " + 
				"isUnread INTEGER, " + 
				"content BLOB, " + 
				"articleUrl TEXT, " + 
				"articleCommentUrl TEXT, " + 
				"updateDate INTEGER, " + 
				"PRIMARY KEY( id , feedId ))");
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
					"title TEXT, "
					+ "url TEXT, " + 
					"unread INTEGER, " + 
					"PRIMARY KEY( id, categoryId ))");
			
			db.execSQL("CREATE TABLE " + TABLE_ARTICLES + " (" + 
					"id INTEGER, " + 
					"feedId INTEGER, " + 
					"title TEXT, " + 
					"isUnread INTEGER, " + 
					"content BLOB, " + 
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
	
	private long insertCategory(String id, String title, int unread) {

		if (id == null)
			return 0;
		if (title == null)
			title = "";

		long ret = 0;
		synchronized(this) {
			insertCat.bindString(1, id);
			insertCat.bindString(2, title);
			insertCat.bindLong(3, unread);
			
			ret = insertCat.executeInsert();
		}
		return ret;
	}
	
	public void insertCategory(CategoryItem c) {
		insertCategory(c.getId(), c.getTitle(), c.getUnread());
	}
	
	public void insertCategories(List<CategoryItem> list) {
		for (CategoryItem c : list) {
			insertCategory(
					c.getId(),
					c.getTitle(),
					c.getUnread());
		}
	}
	
	private long insertFeed(String feedId, String categoryId, String title, String url, int unread) {

		if (feedId == null)
			return 0;
		if (categoryId == null)
			categoryId = "";
		if (title == null)
			title = "";
		if (url == null)
			url = "";
		if (title == null)
			title = "";

		long ret = 0;
		synchronized(this) {
			insertFeed.bindLong(1, new Integer(feedId).longValue());
			insertFeed.bindLong(2, new Integer(categoryId).longValue());
			insertFeed.bindString(3, title);
			insertFeed.bindString(4, url);
			insertFeed.bindLong(5, unread);
			
			ret = insertFeed.executeInsert();
		}
		return ret;
	}
	
	public void insertFeed(FeedItem f) {
		if (f.getCategoryId().startsWith("-")) {
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
		for (FeedItem f : list) {
			if (f.getCategoryId().startsWith("-")) {
				continue;
			}
			
			insertFeed(
					f.getId(),
					f.getCategoryId(),
					f.getTitle(),
					f.getUrl(),
					f.getUnread());
		}
	}
	
	private long insertArticle(String articleId, String feedId, String title, boolean isUnread, String content,
			String articleUrl, String articleCommentUrl, Date updateDate) {
		
		if (articleId == null)
			return 0;
		if (feedId == null)
			feedId = "";
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

		long ret = 0;
		synchronized(this) {
			insertArticle.bindLong(1, Long.parseLong(articleId));
			insertArticle.bindLong(2, Long.parseLong(feedId));
			insertArticle.bindString(3, title);
			insertArticle.bindLong(4, (isUnread ? 1 : 0));
			byte[] contentBytes = Base64.encodeBytesToBytes(content.getBytes());
			insertArticle.bindBlob(5, contentBytes);
			insertArticle.bindString(6, articleUrl);
			insertArticle.bindString(7, articleCommentUrl);
			insertArticle.bindLong(8, updateDate.getTime());
			
			ret = insertArticle.executeInsert();
		}
		return ret;
	}
	
	public void insertArticle(ArticleItem a, int number) {
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
		insertArticlesInternal(list);
		purgeArticlesNumber(number);
	}
	
	public void insertArticlesInternal(List<ArticleItem> list) {
		for (ArticleItem a : list) {
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
			db.execSQL("UPDATE " + TABLE_ARTICLES +
					" SET isUnread='0' WHERE feedId='" + f.getId() + "'");
		}
	}
	
	public void markArticlesRead(List<String> list, int articleState) {
		boolean isUnread = articleState == 0 ? false : true;
		for (String id : list) {
			db.execSQL("UPDATE " + TABLE_ARTICLES + 
					" SET isUnread='" + isUnread + "' " + "WHERE id='" + id + "'");
		}
	}
	
	public void updateCategoryUnreadCount(String id, int count) {
		if (id == null) return;
		
		db.execSQL("UPDATE " + TABLE_CAT +
				" SET unread='" + count + "' " + "WHERE id='" + id + "'");
	}
	
	public void updateFeedUnreadCount(String id, String categoryId, int count) {
		if (id == null || categoryId == null) return;
		
		db.execSQL("UPDATE " + TABLE_FEEDS +
				" SET unread='" + count + "' " + "WHERE id='" + id + "' and categoryId='" + categoryId + "'");
	}
	
	public void updateArticleUnread(String id, String feedId, boolean isUnread) {
		if (id == null || feedId == null) return;
		
		db.execSQL("UPDATE " + TABLE_ARTICLES + 
				" SET isUnread='" + isUnread + "' " + "WHERE id='" + id + "' and feedId='" + feedId + "'");
	}
	
	public void updateArticleContent(ArticleItem result) {
		
		String id = result.getId();
		String feedId = result.getFeedId();
		String content = result.getContent();
		String title = result.getTitle();
		String articleUrl = result.getArticleUrl();
		String articleCommentUrl = result.getArticleCommentUrl();
		Date updateDate = result.getUpdateDate();
		
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

		synchronized(this) {
			byte[] contentBytes = Base64.encodeBytesToBytes(content.getBytes());
			updateArticle.bindBlob(1, contentBytes);
			updateArticle.bindString(2, title);
			updateArticle.bindString(3, articleUrl);
			updateArticle.bindString(4, articleCommentUrl);
			updateArticle.bindLong(5, updateDate.getTime());
			updateArticle.bindLong(6, new Long(id));
			updateArticle.bindLong(7, new Long(feedId));
			updateArticle.executeInsert();
		}
	}
	
	public void purgeArticlesDays(Date olderThenThis) {
		db.execSQL("DELETE FROM " + TABLE_ARTICLES + " WHERE isUnread=0 AND updateDate<" + olderThenThis.getTime());
	}
	
	public void purgeArticlesNumber(int number) {
		db.execSQL("DELETE FROM " + TABLE_ARTICLES +
				" WHERE id in( select id from " + TABLE_ARTICLES +
				" WHERE isUnread=0" +
				" ORDER BY updateDate DESC" +
				" LIMIT -1 OFFSET " + number + ")");
	}
	
	// *******| SELECT |*******************************************************************
	
	public ArticleItem getArticle(String id) {
		ArticleItem ret = null;
		
		Cursor c = db.query(TABLE_ARTICLES, null, "id=" + id, null, null, null, null, null);
		
		while (!c.isAfterLast()) {
			ret = handleArticleCursor(c);
			
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	public FeedItem getFeed(String id) {
		FeedItem ret = new FeedItem();
		
		Cursor c = db.query(TABLE_FEEDS, null, "id=" + id, null, null, null, null, null);
		
		while (!c.isAfterLast()) {
			ret = handleFeedCursor(c);
			
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	public CategoryItem getCategory(String id) {
		CategoryItem ret = new CategoryItem();
		
		Cursor c = db.query(TABLE_CAT, null, "id=" + id, null, null, null, null, null);
		
		while (!c.isAfterLast()) {
			ret = handleCategoryCursor(c);
			
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	
	public List<ArticleItem> getArticles(FeedItem fi) {
		List<ArticleItem> ret = new ArrayList<ArticleItem>();
		
		Cursor c = db.query(TABLE_ARTICLES, null, "feedId=" + fi.getId(), null, null, null, null, null);
		
		while (!c.isAfterLast()) {
			ret.add(handleArticleCursor(c));
			
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	public List<FeedItem> getFeeds(CategoryItem ci) {
		List<FeedItem> ret = new ArrayList<FeedItem>();
		
		Cursor c = db.query(TABLE_FEEDS, null, "categoryId=" + ci.getId(), null, null, null, null, null);
		
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
	public Map<String, List<ArticleItem>> getArticles(int maxArticles) {
		Map<String, List<ArticleItem>> ret = new HashMap<String, List<ArticleItem>>();
		
		String limit = (maxArticles > 0 ? String.valueOf(maxArticles) : null);
		
		Cursor c = db.query(TABLE_ARTICLES, null, null, null, null, null, "updateDate DESC", limit);
		
		while (!c.isAfterLast()) {
			ArticleItem a = handleArticleCursor(c);
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
		
		Cursor c = db.query(TABLE_FEEDS, null, null, null, null, null, null);
		
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
		
		Cursor c = db.query(TABLE_CAT, null, "id like '-%' OR id=0", null, null, null, null);

		while (!c.isAfterLast()) {
			CategoryItem ci = handleCategoryCursor(c);
			
			ret.add(ci);
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	public List<CategoryItem> getCategories() {
		List<CategoryItem> ret = new ArrayList<CategoryItem>();
		
		Cursor c = db.query(TABLE_CAT, null, "id not like '-%' AND id!=0", null, null, null, null);
		
		while (!c.isAfterLast()) {
			CategoryItem ci = handleCategoryCursor(c);
			
			ret.add(ci);
			c.move(1);
		}
		c.close();
		
		return ret;
	}

	
	// *******************************************
	
	private ArticleItem handleArticleCursor(Cursor c) {
		ArticleItem ret = null;
		
		if (c.isBeforeFirst()) {
			if (!c.moveToFirst()) {
				return ret;
			}
		}
		
		String key = c.getString(1);
		byte[] content = c.getBlob(4);
		String contentStr = "";
		
		try {
			if (content.length > 0) {
				contentStr = new String(Base64.decode(content));
			}
		} catch (Exception e) {
			Log.w(Utils.TAG, "Could not decode content or title. length: " + content.length);
		}
		
		ret = new ArticleItem(key,					// feedId
				c.getString(0),						// id
				c.getString(2),						// title
				(c.getInt(3) != 0 ? true : false),	// isUnread
				new Date(c.getLong(7)),				// updateDate
				contentStr,							// content
				c.getString(5),						// articleUrl
				c.getString(6)						// articleCommentUrl
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
				c.getString(1),			// categoryId
				c.getString(0), 		// id
				c.getString(2),			// title
				c.getString(3),			// url
				c.getInt(4));			// unread
		
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
				c.getString(0),			// id
				c.getString(1),			// title
				(int) c.getLong(2));	// unread
		
		return ret;
	}
}
