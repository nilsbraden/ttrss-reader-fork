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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import java.io.IOException;
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
	
	private static final String DATABASE_NAME = "ttrss.db";
	private static final int DATABASE_VERSION = 1;
	
	private static final String TABLE_CAT = "categories";
	private static final String TABLE_FEEDS = "feeds";
	private static final String TABLE_ARTICLES = "articles";
	
	private static final String INSERT_CAT = "replace into " + TABLE_CAT + "(id, title, unread) values (?, ?, ?)";
	private static final String INSERT_FEEDS = "replace into " + TABLE_FEEDS
			+ "(id, categoryId, title, url, unread) values (?, ?, ?, ?, ?)";
	private static final String INSERT_ARTICLES = "replace into "
			+ TABLE_ARTICLES
			+ "(id, feedId, title, isUnread, content, articleUrl, articleCommentUrl, updateDate) values (?, ?, ?, ?, ?, ?, ?, ?)";
	
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
		
		// deleteAll();
	}
	
	public void checkAndInitializeController(Context context) {
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
	
	public SQLiteDatabase getDb() {
		return this.db;
	}
	
	public void deleteAll() {
		db.execSQL("DELETE FROM " + TABLE_CAT);
		db.execSQL("DELETE FROM " + TABLE_FEEDS);
		db.execSQL("DELETE FROM " + TABLE_ARTICLES);
	}
	
	public void DropDB() {
		if (context.deleteDatabase(DATABASE_NAME)) {
			Log.d(Utils.TAG, "deleteDatabase(): database deleted.");
		} else {
			Log.d(Utils.TAG, "deleteDatabase(): database NOT deleted.");
		}
	}
	
	private static class OpenHelper extends SQLiteOpenHelper {
		
		OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_CAT + " (" + "id INTEGER PRIMARY KEY, " + "title TEXT, "
					+ "unread INTEGER)");
			
			db.execSQL("CREATE TABLE " + TABLE_FEEDS + " (" + "id INTEGER, " + "categoryId INTEGER, " + "title TEXT, "
					+ "url TEXT, " + "unread INTEGER, " + "PRIMARY KEY( id, categoryId ))");
			
			db.execSQL("CREATE TABLE " + TABLE_ARTICLES + " (" + "id INTEGER, " + "feedId INTEGER, " + "title TEXT, "
					+ "isUnread INTEGER, " + "content BLOB, " + "articleUrl TEXT, " + "articleCommentUrl TEXT, "
					+ "updateDate INTEGER, " + "PRIMARY KEY( id , feedId ))");
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
		
		insertCat.bindString(1, id);
		insertCat.bindString(2, title);
		insertCat.bindLong(3, unread);
		
		return insertCat.executeInsert();
	}
	
	public long insertCategory(CategoryItem c) {
		return insertCategory(c.getId(), c.getTitle(), c.getUnreadCount());
	}
	
	public void insertCategories(List<CategoryItem> list) {
		if (list != null && list.size() > 0) {
			db.execSQL("DELETE FROM " + TABLE_CAT);
		}
		
		for (CategoryItem c : list) {
			insertCategory(c.getId(), c.getTitle(), c.getUnreadCount());
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
		
		insertFeed.bindLong(1, new Integer(feedId).longValue());
		insertFeed.bindLong(2, new Integer(categoryId).longValue());
		insertFeed.bindString(3, title);
		insertFeed.bindString(4, url);
		insertFeed.bindLong(5, unread);
		
		return insertFeed.executeInsert();
	}
	
	public long insertFeed(FeedItem f) {
		if (f.getCategoryId().startsWith("-")) {
			return 0;
		}
		
		return insertFeed(f.getId(), f.getCategoryId(), f.getTitle(), f.getUrl(), f.getUnreadCount());
	}
	
	public void insertFeeds(List<FeedItem> list) {
		if (list != null && list.size() > 0) {
			String categoryId = list.get(0).getCategoryId();
			db.execSQL("DELETE FROM " + TABLE_FEEDS + " WHERE categoryId='" + categoryId + "'");
		}
		
		for (FeedItem f : list) {
			if (f.getCategoryId().startsWith("-")) {
				continue;
			}
			
			insertFeed(f.getId(), f.getCategoryId(), f.getTitle(), f.getUrl(), f.getUnreadCount());
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
		
		
		insertArticle.bindLong(1, new Integer(articleId).longValue());
		insertArticle.bindLong(2, new Integer(feedId).longValue());
		insertArticle.bindString(3, title);
		insertArticle.bindLong(4, (isUnread ? 1 : 0));
		try {
			byte[] contentBytes = Base64.encodeBytesToBytes(content.getBytes(), 0, content.getBytes().length, Base64.GZIP);
			insertArticle.bindBlob(5, contentBytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			insertArticle.bindBlob(5, "".getBytes());
		}
		insertArticle.bindString(6, articleUrl);
		insertArticle.bindString(7, articleCommentUrl);
		insertArticle.bindLong(8, updateDate.getTime());
		
		return insertArticle.executeInsert();
	}
	
	public long insertArticle(ArticleItem a, int number) {
		long ret = insertArticle(a.getId(), a.getFeedId(), a.getTitle(), a.isUnread(), a.getContent(), a
				.getArticleUrl(), a.getArticleCommentUrl(), a.getUpdateDate());
		
		purgeArticlesNumber(number);
		
		return ret;
	}
	
	public void insertArticles(List<ArticleItem> list, int number) {
		
		if (list != null && list.size() > 0) {
			String feedId = list.get(0).getFeedId();
			db.execSQL("DELETE FROM " + TABLE_ARTICLES + " WHERE feedId='" + feedId + "'");
		}
		
		for (ArticleItem a : list) {
			insertArticle(a.getId(), a.getFeedId(), a.getTitle(), a.isUnread(), a.getContent(), a.getArticleUrl(), a
					.getArticleCommentUrl(), a.getUpdateDate());
		}
		
		purgeArticlesNumber(number);
	}
	
	// *******| UPDATE |*******************************************************************
	
	public void markCategoryRead(CategoryItem c) {
		// TODO Implement more efficient method here. We need to iterate over the feeds here first so we can get the
		// feedIDs and mark all articles with these IDs as read. Perhaps its easier to leave this to the DataController
		// who is handling it right now.
	}
	
	public void markFeedRead(FeedItem f) {
		String feedId = f.getId();
		String categoryId = f.getCategoryId();
		
		updateFeedUnreadCount(feedId, categoryId, 0);
		
		db.execSQL("UPDATE " + TABLE_ARTICLES + " SET isUnread='0' " + "WHERE feedId='" + feedId + "' AND categoryId='"
				+ categoryId + "'");
	}
	
	public void updateCategoryUnreadCount(String id, int count) {
		if (id == null) return;
		
		db.execSQL("UPDATE " + TABLE_CAT + " SET unread='" + count + "' " + "WHERE id='" + id + "'");
	}
	
	public void updateFeedUnreadCount(String id, String categoryId, int count) {
		if (id == null || categoryId == null) return;
		
		db.execSQL("UPDATE " + TABLE_FEEDS + " SET unread='" + count + "' " + "WHERE id='" + id + "' and categoryId='"
				+ categoryId + "'");
	}
	
	public void updateArticleUnread(String id, String feedId, boolean isUnread) {
		if (id == null || feedId == null) return;
		
		db.execSQL("UPDATE " + TABLE_ARTICLES + " SET isUnread='" + isUnread + "' " + "WHERE id='" + id
				+ "' and feedId='" + feedId + "'");
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
		
		try {
			byte[] contentBytes = Base64.encodeBytesToBytes(content.getBytes(), 0, content.getBytes().length, Base64.GZIP);
			updateArticle.bindBlob(1, contentBytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			updateArticle.bindBlob(1, "".getBytes());
		}
		
		updateArticle.bindString(2, title);
		updateArticle.bindString(3, articleUrl);
		updateArticle.bindString(4, articleCommentUrl);
		updateArticle.bindLong(5, updateDate.getTime());
		updateArticle.bindLong(6, new Long(id));
		updateArticle.bindLong(7, new Long(feedId));
		updateArticle.executeInsert();
	}
	
	public void purgeArticlesDays(Date olderThenThis) {
		db.execSQL("DELETE FROM " + TABLE_ARTICLES + " WHERE isUnread=0 AND updateDate<" + olderThenThis.getTime());
	}
	
	public void purgeArticlesNumber(int number) {
		db.execSQL("DELETE FROM " + TABLE_ARTICLES + " WHERE id in( " + " select id from " + TABLE_ARTICLES
				+ " WHERE isUnread=0" + " ORDER BY updateDate DESC LIMIT -1 OFFSET " + number + ")");
	}
	
	// *******| SELECT |*******************************************************************
	
	public Map<String, List<ArticleItem>> getArticles(int maxArticles) {
		Map<String, List<ArticleItem>> ret = new HashMap<String, List<ArticleItem>>();
		
		Cursor c = db.query(TABLE_ARTICLES, null, null, null, null, null, "updateDate DESC", String
				.valueOf(maxArticles));
		if (c.isBeforeFirst()) {
			if (!c.moveToFirst()) {
				return ret;
			}
		}
		
		while (!c.isAfterLast()) {
			String key = c.getString(1);
			byte[] content = c.getBlob(4);
			String contentStr = "";
			
			try {
				if (content.length > 0) {
					contentStr = new String(Base64.decode(content, 0, content.length, Base64.GZIP));
				}
			} catch (Exception e) {
				Log.w(Utils.TAG, "Could not decode content or title. length: " + content.length);
			}
			
			ArticleItem fi = new ArticleItem(key, // feedId
					c.getString(0), // id
					c.getString(2), // title
					(c.getInt(3) != 0 ? true : false), // isUnread
					new Date(c.getLong(7)), // updateDate
					contentStr, // content
					c.getString(5), // articleUrl
					c.getString(6) // articleCommentUrl
			);
			
			List<ArticleItem> list;
			if (ret.get(key) != null) {
				list = ret.get(key);
			} else {
				list = new ArrayList<ArticleItem>();
			}
			
			list.add(fi);
			ret.put(key, list);
			
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	public Map<String, List<FeedItem>> getFeeds() {
		Map<String, List<FeedItem>> ret = new HashMap<String, List<FeedItem>>();
		
		Cursor c = db.query(TABLE_FEEDS, null, null, null, null, null, null);
		if (c.isBeforeFirst()) {
			if (!c.moveToFirst()) {
				return ret;
			}
		}
		
		while (!c.isAfterLast()) {
			String key = c.getString(1);
			FeedItem fi = new FeedItem(key, // categoryId
					c.getString(0), // id
					c.getString(2), // title
					c.getString(3), // url
					c.getInt(4)); // unread
			
			List<FeedItem> list;
			if (ret.get(key) != null) {
				list = ret.get(key);
			} else {
				list = new ArrayList<FeedItem>();
			}
			
			list.add(fi);
			ret.put(key, list);
			
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	public List<CategoryItem> getVirtualCategories() {
		List<CategoryItem> ret = new ArrayList<CategoryItem>();
		
		Cursor c = db.query(TABLE_CAT, null, null, null, null, null, null);
		if (c.isBeforeFirst()) {
			if (!c.moveToFirst()) {
				return ret;
			}
		}
		
		while (!c.isAfterLast()) {
			CategoryItem ci = new CategoryItem(c.getString(0), // id
					c.getString(1), // title
					(int) c.getLong(2)); // unread
			if (ci.getId().startsWith("-")) {
				ret.add(ci);
			}
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
	public List<CategoryItem> getCategories() {
		List<CategoryItem> ret = new ArrayList<CategoryItem>();
		
		Cursor c = db.query(TABLE_CAT, null, null, null, null, null, null);
		if (c.isBeforeFirst()) {
			if (!c.moveToFirst()) {
				return ret;
			}
		}
		
		while (!c.isAfterLast()) {
			CategoryItem ci = new CategoryItem(c.getString(0), // id
					c.getString(1), // title
					(int) c.getLong(2)); // unread
			if (!ci.getId().startsWith("-")) {
				ret.add(ci);
			}
			c.move(1);
		}
		c.close();
		
		return ret;
	}
	
}
