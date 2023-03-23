/*
 * Copyright (c) 2015, Nils Braden
 *
 * This file is part of ttrss-reader-fork. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; If
 * not, see http://www.gnu.org/licenses/.
 */

package org.ttrssreader.controllers;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import org.ttrssreader.MyApplication;
import org.ttrssreader.gui.dialogs.ErrorDialog;
import org.ttrssreader.imageCache.ImageCache;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.pojos.Label;
import org.ttrssreader.model.pojos.RemoteFile;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.FileUtils;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DBHelper {

	private static final String TAG = DBHelper.class.getSimpleName();

	private static final String DATABASE_NAME = "ttrss.db";
	private static final int DATABASE_VERSION = 67;

	public static final String[] CATEGORIES_COLUMNS = new String[]{"_id", "title", "unread"};

	public static final String TABLE_CATEGORIES = "categories";
	public static final String TABLE_FEEDS = "feeds";
	public static final String TABLE_ARTICLES = "articles";
	public static final String TABLE_ARTICLES2LABELS = "articles2labels";
	private static final String TABLE_MARK = "marked";
	private static final String TABLE_NOTES = "notes";
	public static final String TABLE_REMOTEFILES = "remotefiles";
	public static final String TABLE_REMOTEFILE2ARTICLE = "remotefile2article";

	static final String MARK_READ = "isUnread";
	static final String MARK_STAR = "isStarred";
	static final String MARK_PUBLISH = "isPublished";
	static final String COL_NOTE = "note";
	static final String COL_UNREAD = "unread";

	// @formatter:off
	private static final String CREATE_TABLE_CATEGORIES =
			"CREATE TABLE "
					+ TABLE_CATEGORIES
					+ " (_id INTEGER PRIMARY KEY,"
					+ " title TEXT,"
					+ " unread INTEGER)";

	private static final String CREATE_TABLE_FEEDS =
			"CREATE TABLE "
					+ TABLE_FEEDS
					+ " (_id INTEGER PRIMARY KEY,"
					+ " categoryId INTEGER,"
					+ " title TEXT,"
					+ " url TEXT,"
					+ " unread INTEGER,"
					+ " icon BLOB)";

	private static final String CREATE_TABLE_ARTICLES =
			"CREATE TABLE "
					+ TABLE_ARTICLES
					+ " (_id INTEGER PRIMARY KEY,"
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
					+ " cachedImages INTEGER DEFAULT 0,"
					+ " articleLabels TEXT,"
					+ " author TEXT,"
					+ " note TEXT,"
					+ " score INTEGER DEFAULT 0)";

	private static final String CREATE_TABLE_ARTICLES2LABELS =
			"CREATE TABLE "
					+ TABLE_ARTICLES2LABELS
					+ " (articleId INTEGER,"
					+ " labelId INTEGER, PRIMARY KEY(articleId, labelId))";

	private static final String CREATE_TABLE_MARK =
			"CREATE TABLE "
					+ TABLE_MARK
					+ " (id INTEGER PRIMARY KEY,"
					+ " " + MARK_READ + " INTEGER,"
					+ " " + MARK_STAR + " INTEGER,"
					+ " " + MARK_PUBLISH + " INTEGER)";

	private static final String CREATE_TABLE_NOTES =
			"CREATE TABLE "
					+ TABLE_NOTES
					+ " (_id INTEGER PRIMARY KEY,"
					+ " " + COL_NOTE + " TEXT)";

	private static final String INSERT_CATEGORY =
			"REPLACE INTO "
					+ TABLE_CATEGORIES
					+ " (_id, title, unread)"
					+ " VALUES (?, ?, ?)";

	private static final String INSERT_FEED =
			"REPLACE INTO "
					+ TABLE_FEEDS
					+ " (_id, categoryId, title, url, unread, icon)"
					+ " VALUES (?, ?, ?, ?, ?, ?)";

	private static final String INSERT_ARTICLE =
			"INSERT OR REPLACE INTO "
					+ TABLE_ARTICLES
					+ " (_id, feedId, title, isUnread, articleUrl, articleCommentUrl, updateDate, content,"
					+ " attachments, isStarred, isPublished, cachedImages, articleLabels, author, note, score)"
					+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, coalesce((SELECT cachedImages FROM " + TABLE_ARTICLES
					+ " WHERE _id=?), NULL), ?, ?, ?, ?)";
	// This should insert new values or replace existing values but should always keep an already inserted value for
	// "cachedImages". When inserting it is set to the default value which is 0 (not "NULL").

	private static final String INSERT_LABEL =
			"REPLACE INTO "
					+ TABLE_ARTICLES2LABELS
					+ " (articleId, labelId)"
					+ " VALUES (?, ?)";

	private static final String INSERT_REMOTEFILE =
			"INSERT OR FAIL INTO "
					+ TABLE_REMOTEFILES
					+ " (url, ext)"
					+ " VALUES (?, ?)";

	private static final String INSERT_REMOTEFILE2ARTICLE =
			"INSERT OR IGNORE INTO "
					+ TABLE_REMOTEFILE2ARTICLE
					+ " (remotefileId, articleId)"
					+ " VALUES (?, ?)";
	// @formatter:on

	private volatile boolean initialized = false;

	private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	private final Lock read = rwl.readLock();
	private final Lock write = rwl.writeLock();

	private OpenHelper openHelper;

	public synchronized OpenHelper getOpenHelper() {
		return openHelper;
	}

	private final Object insertCategoryLock = new Object();
	private final Object insertFeedLock = new Object();
	private final Object insertArticleLock = new Object();
	private final Object insertLabelLock = new Object();
	private final Object insertRemoteFileLock = new Object();
	private final Object insertRemoteFile2ArticleLock = new Object();

	private SQLiteStatement insertCategory;
	private SQLiteStatement insertFeed;
	private SQLiteStatement insertArticle;
	private SQLiteStatement insertLabel;
	private SQLiteStatement insertRemoteFile;
	private SQLiteStatement insertRemoteFile2Article;

	private static boolean specialUpgradeSuccessful = false;

	// Singleton (see http://stackoverflow.com/a/11165926)
	private DBHelper() {
	}

	private static class InstanceHolder {
		private static final DBHelper instance = new DBHelper();
	}

	public static DBHelper getInstance() {
		return InstanceHolder.instance;
	}

	public synchronized void initialize(final Context context) {
		new AsyncTask<Void, Void, Void>() {
			protected Void doInBackground(Void... params) {

				// Check if deleteDB is scheduled or if DeleteOnStartup is set
				if (Controller.getInstance().isDeleteDBScheduled()) {
					final File dbFile = context.getDatabasePath(DATABASE_NAME);
					if (getOpenHelper() != null)
						closeDB();
					if (deleteDB(dbFile)) {
						Controller.getInstance().setDeleteDBScheduled(false);
						initializeDBHelper();
						return null; // Don't need to check if DB is corrupted, it is NEW!
					}
				}

				// Initialize DB
				if (!initialized) {
					initializeDBHelper();
				} else if (getOpenHelper() == null) {
					initializeDBHelper();
				} else {
					return null; // DB was already initialized, no need to check anything.
				}

				// Test if DB is accessible, backup and delete if not
				if (initialized) {
					Cursor c = null;
					read.lock();
					try {
						// Try to access the DB
						c = getOpenHelper().getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE_CATEGORIES, null);
						c.getCount();
						if (c.moveToFirst())
							c.getInt(0);

					} catch (Exception e) {
						Log.e(TAG, "Database was corrupted, creating a new one...", e);
						closeDB();
						final File dbFile = context.getDatabasePath(DATABASE_NAME);
						if (dbFile.delete())
							initializeDBHelper();
						ErrorDialog.getInstance("The Database was corrupted and had to be recreated. If this happened more " + "than once to you please let me know under what circumstances this " + "happened.");
					} finally {
						close(c);
						read.unlock();
					}
				}
				return null;
			}
		}.execute();
	}

	private synchronized void initializeDBHelper() {
		final Context context = MyApplication.context();

		if (getOpenHelper() != null)
			closeDB();

		openHelper = new OpenHelper(context);
		SQLiteDatabase db = openHelper.getWritableDatabase();

		if (specialUpgradeSuccessful) {
			// Re-open DB for final usage:
			closeDB();
			openHelper = new OpenHelper(context);
			db = openHelper.getWritableDatabase();

			Toast.makeText(context, "ImageCache is beeing cleaned...", Toast.LENGTH_LONG).show();
			new AsyncTask<Void, Void, Void>() {
				protected Void doInBackground(Void... params) {
					// Clear ImageCache since no files are in REMOTE_FILES anymore and we dont want to leave them
					// there forever:
					ImageCache imageCache = Controller.getInstance().getImageCache();
					if (imageCache != null) {
						imageCache.fillMemoryCacheFromDisk();
						File cacheFolder = new File(imageCache.getDiskCacheDirectory());
						FileUtils.deleteFolderRecursive(cacheFolder);
					}
					return null;
				}

				protected void onPostExecute(Void result) {
					Toast.makeText(context, "ImageCache has been cleaned up...", Toast.LENGTH_LONG).show();
				}
			}.execute();
		}

		insertCategory = db.compileStatement(INSERT_CATEGORY);
		insertFeed = db.compileStatement(INSERT_FEED);
		insertArticle = db.compileStatement(INSERT_ARTICLE);
		insertLabel = db.compileStatement(INSERT_LABEL);
		insertRemoteFile = db.compileStatement(INSERT_REMOTEFILE);
		insertRemoteFile2Article = db.compileStatement(INSERT_REMOTEFILE2ARTICLE);

		db.acquireReference();
		initialized = true;
	}

	private synchronized boolean deleteDB(final File dbFile) {
		if (dbFile == null)
			return false;

		Log.i(TAG, "Deleting Database as requested by preferences.");
		if (dbFile.exists()) {
			if (getOpenHelper() != null) {
				closeDB();
			}
			return dbFile.delete();
		}

		return false;
	}

	private synchronized void closeDB() {
		write.lock();
		try {
			getOpenHelper().close();
			openHelper = null;
		} finally {
			write.unlock();
		}
	}

	private synchronized boolean isDBAvailable() {
		return getOpenHelper() != null;
	}

	public static class OpenHelper extends SQLiteOpenHelper {

		public OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/**
		 * set wished DB modes on DB
		 *
		 * @param db DB to be used
		 */
		@Override
		public void onOpen(SQLiteDatabase db) {
			super.onOpen(db);
			if (!db.isReadOnly()) {
				// Enable foreign key constraints
				db.execSQL("PRAGMA foreign_keys=ON;");
			}
		}

		/**
		 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TABLE_CATEGORIES);
			db.execSQL(CREATE_TABLE_FEEDS);
			db.execSQL(CREATE_TABLE_ARTICLES);
			db.execSQL(CREATE_TABLE_ARTICLES2LABELS);
			db.execSQL(CREATE_TABLE_MARK);
			db.execSQL(CREATE_TABLE_NOTES);
			createRemoteFilesSupportDBObjects(db);
		}

		@Override
		public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(TAG, "Downgrading database from " + oldVersion + " to version " + newVersion);
			dropAllTables(db);
			onCreate(db);
		}

		private void dropAllTables(SQLiteDatabase db) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_FEEDS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_ARTICLES);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_ARTICLES2LABELS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_MARK);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMOTEFILES);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMOTEFILE2ARTICLE);
		}

		/**
		 * upgrade the DB
		 *
		 * @param db         The database.
		 * @param oldVersion The old database version.
		 * @param newVersion The new database version.
		 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			boolean didUpgrade = false;

			if (oldVersion < 50) {
				Log.i(TAG, String.format("Upgrading database from %s to 50.", oldVersion));
				ContentValues cv = new ContentValues(1);
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
						+ " note TEXT,"
						+ " PRIMARY KEY(id, type))";
				// @formatter:on

				Log.i(TAG, String.format("Upgrading database from %s to 51.", oldVersion));
				Log.i(TAG, String.format(" (Executing: %s)", sql));
				Log.i(TAG, String.format(" (Executing: %s)", sql2));

				db.execSQL(sql);
				db.execSQL(sql2);
				didUpgrade = true;
			}

			if (oldVersion < 52) {
				// @formatter:off
				String sql = "ALTER TABLE " + TABLE_ARTICLES + " ADD COLUMN articleLabels TEXT";
				// @formatter:on

				Log.i(TAG, String.format("Upgrading database from %s to 52.", oldVersion));
				Log.i(TAG, String.format(" (Executing: %s)", sql));

				db.execSQL(sql);
				didUpgrade = true;
			}

			if (oldVersion < 53) {
				Log.i(TAG, String.format("Upgrading database from %s to 53.", oldVersion));
				didUpgrade = createRemoteFilesSupportDBObjects(db);
				if (didUpgrade) {
					ContentValues cv = new ContentValues(1);
					cv.putNull("cachedImages");
					db.update(TABLE_ARTICLES, cv, null, null);
					ImageCache ic = Controller.getInstance().getImageCache();
					if (ic != null)
						ic.clear();
				}
			}

			if (oldVersion < 58) {
				Log.i(TAG, String.format("Upgrading database from %s to 58.", oldVersion));

				// Rename columns "id" to "_id" by modifying the table structure:
				try {
					db.beginTransaction();
					db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMOTEFILES);
					db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMOTEFILE2ARTICLE);

					db.execSQL("PRAGMA writable_schema=1;");
					String sql = "UPDATE SQLITE_MASTER SET SQL = '%s' WHERE NAME = '%s';";
					db.execSQL(String.format(sql, CREATE_TABLE_CATEGORIES, TABLE_CATEGORIES));
					db.execSQL(String.format(sql, CREATE_TABLE_FEEDS, TABLE_FEEDS));
					db.execSQL(String.format(sql, CREATE_TABLE_ARTICLES, TABLE_ARTICLES));
					db.execSQL("PRAGMA writable_schema=0;");

					if (createRemoteFilesSupportDBObjects(db)) {
						db.setTransactionSuccessful();
						didUpgrade = true;
					}
				} finally {
					db.execSQL("PRAGMA foreign_keys=ON;");
					db.endTransaction();
					specialUpgradeSuccessful = true;
				}
			}

			if (oldVersion < 59) {
				// @formatter:off
				String sql = "ALTER TABLE " + TABLE_ARTICLES + " ADD COLUMN author TEXT";
				// @formatter:on

				Log.i(TAG, String.format("Upgrading database from %s to 59.", oldVersion));
				Log.i(TAG, String.format(" (Executing: %s)", sql));

				db.execSQL(sql);
				didUpgrade = true;
			}

			if (oldVersion < 60) {
				Log.i(TAG, String.format("Upgrading database from %s to 60.", oldVersion));
				Log.i(TAG, " (Re-Creating View: remotefiles_sequence )");

				createRemotefilesView(db);
				didUpgrade = true;
			}

			if (oldVersion < 61) {
				// @formatter:off
				String sql = "ALTER TABLE " + TABLE_ARTICLES + " ADD COLUMN note TEXT";
				// @formatter:on

				Log.i(TAG, String.format("Upgrading database from %s to 61.", oldVersion));
				Log.i(TAG, String.format(" (Executing: %s)", sql));

				db.execSQL(sql);
				didUpgrade = true;
			}

			if (oldVersion < 64) {
				Log.i(TAG, String.format("Upgrading database from %s to 64.", oldVersion));

				try {
					db.beginTransaction();
					db.execSQL("PRAGMA writable_schema=1;");
					String sql = "UPDATE SQLITE_MASTER SET SQL = '%s' WHERE NAME = '%s';";
					db.execSQL(String.format(sql, CREATE_TABLE_MARK, TABLE_MARK));
					db.execSQL("PRAGMA writable_schema=0;");
					db.execSQL(CREATE_TABLE_NOTES);
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}

				didUpgrade = true;
			}

			if (oldVersion < 65) {
				String sql = "ALTER TABLE " + TABLE_FEEDS + " ADD COLUMN icon BLOB";

				Log.i(TAG, String.format("Upgrading database from %s to 65.", oldVersion));
				Log.i(TAG, String.format(" (Executing: %s", sql));

				db.execSQL(sql);
				didUpgrade = true;
			}

			if (oldVersion < 66) {
				String sql = "ALTER TABLE " + TABLE_ARTICLES + " ADD COLUMN score INTEGER";

				Log.i(TAG, String.format("Upgrading database from %s to 66.", oldVersion));
				Log.i(TAG, String.format(" (Executing: %s", sql));

				db.execSQL(sql);
				didUpgrade = true;
			}

			if (oldVersion < 67) {

				boolean exists = false;
				Cursor res = null;
				try {
					// Check if column "score" exists...
					res = db.rawQuery("PRAGMA table_info(" + TABLE_ARTICLES + ")", null);
					res.moveToFirst();
					do {
						String currentColumn = res.getString(1);
						if (currentColumn.equals("score")) {
							exists = true;
						}
					} while (res.moveToNext());
				} finally {
					if (res != null && !res.isClosed())
						res.close();
				}
				if (!exists) {
					// Run upgrade from 66 again:
					String sql = "ALTER TABLE " + TABLE_ARTICLES + " ADD COLUMN score INTEGER";

					Log.i(TAG, String.format("Upgrading database from %s to 67.", oldVersion));
					Log.i(TAG, String.format(" (Executing: %s", sql));

					db.execSQL(sql);
					didUpgrade = true;
				}

			}

			if (!didUpgrade) {
				Log.i(TAG, "Upgrading database, this will drop tables and recreate.");
				dropAllTables(db);
				onCreate(db);
			}

		}

		/**
		 * create DB objects (tables, triggers, views) which
		 * are necessary for file cache support
		 *
		 * @param db current database
		 */
		private boolean createRemoteFilesSupportDBObjects(SQLiteDatabase db) {
			boolean success = false;
			try {
				createRemotefiles(db);
				createRemotefiles2Articles(db);
				createRemotefilesView(db);
				success = true;
			} catch (SQLException e) {
				Log.e(TAG, "Creation of remote file support DB objects failed.\n" + e);
			}

			return success;
		}

		private void createRemotefiles(SQLiteDatabase db) {
			// @formatter:off
			// remote files (images, attachments, etc) belonging to articles,
			// which are locally stored (cached)
			db.execSQL("CREATE TABLE "
					+ TABLE_REMOTEFILES
					+ " (id INTEGER PRIMARY KEY AUTOINCREMENT,"
					// remote file URL
					+ " url TEXT UNIQUE NOT NULL,"
					// file size
					+ " length INTEGER DEFAULT 0,"
					// extension - some kind of additional info
					// (i.e. file extension)
					+ " ext TEXT NOT NULL,"
					// unix timestamp of last change
					// (set automatically by triggers)
					+ " updateDate INTEGER,"
					// boolean flag determining if the file is locally stored
					+ " cached INTEGER DEFAULT 0)");

			// index for quiicker search by by URL
			db.execSQL("DROP INDEX IF EXISTS idx_remotefiles_by_url");
			db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_remotefiles_by_url"
					+ " ON " + TABLE_REMOTEFILES
					+ " (url)");

			// sets last change unix timestamp after row creation
			db.execSQL("DROP TRIGGER IF EXISTS insert_remotefiles");
			db.execSQL("CREATE TRIGGER IF NOT EXISTS insert_remotefiles AFTER INSERT"
					+ " ON " + TABLE_REMOTEFILES
					+ "   BEGIN"
					+ "	 UPDATE " + TABLE_REMOTEFILES
					+ "	   SET updateDate = strftime('%s', 'now')"
					+ "	 WHERE id = new.id;"
					+ "   END");

			// sets last change unix timestamp after row update
			db.execSQL("DROP TRIGGER IF EXISTS update_remotefiles_lastchanged");
			db.execSQL("CREATE TRIGGER IF NOT EXISTS update_remotefiles_lastchanged AFTER UPDATE"
					+ " ON " + TABLE_REMOTEFILES
					+ "   BEGIN"
					+ "	 UPDATE " + TABLE_REMOTEFILES
					+ "	   SET updateDate = strftime('%s', 'now')"
					+ "	 WHERE id = new.id;"
					+ "   END");

			// @formatter:on
		}

		private void createRemotefiles2Articles(SQLiteDatabase db) {
			// @formatter:off
			// m to n relations between articles and remote files
			db.execSQL("CREATE TABLE "
					+ TABLE_REMOTEFILE2ARTICLE
					// ID of remote file
					+ "(remotefileId INTEGER"
					+ "   REFERENCES " + TABLE_REMOTEFILES + "(id)"
					+ "	 ON DELETE CASCADE,"
					// ID of article
					+ " articleId INTEGER"
					+ "   REFERENCES " + TABLE_ARTICLES + "(_id)"
					+ "	 ON UPDATE CASCADE"
					+ "	 ON DELETE NO ACTION,"
					// if both IDs are known, then the row should be found faster
					+ " PRIMARY KEY(remotefileId, articleId))");

			// update count of cached images for article on change of "cached"
			// field of remotefiles
			db.execSQL("DROP TRIGGER IF EXISTS update_remotefiles_articlefiles");
			db.execSQL("CREATE TRIGGER IF NOT EXISTS update_remotefiles_articlefiles AFTER UPDATE"
					+ " OF cached"
					+ " ON " + TABLE_REMOTEFILES
					+ "   BEGIN"
					+ "	 UPDATE " + TABLE_ARTICLES + ""
					+ "	   SET"
					+ "		 cachedImages = ("
					+ "		   SELECT"
					+ "			 COUNT(r.id)"
					+ "		   FROM " + TABLE_REMOTEFILES + " r,"
					+ TABLE_REMOTEFILE2ARTICLE + " m"
					+ "		   WHERE"
					+ "			 m.remotefileId=r.id"
					+ "			 AND m.articleId=" + TABLE_ARTICLES + "._id"
					+ "			 AND r.cached=1)"
					+ "	   WHERE _id IN ("
					+ "		 SELECT"
					+ "		   a._id"
					+ "		 FROM " + TABLE_REMOTEFILE2ARTICLE + " m,"
					+ TABLE_ARTICLES + " a"
					+ "		 WHERE"
					+ "		   m.remotefileId=new.id AND m.articleId=a._id);"
					+ "   END");
			// @formatter:on
		}

		private void createRemotefilesView(SQLiteDatabase db) {
			// @formatter:off
			// represents importance of cached files
			// the sequence is defined by
			// 1. the article to which the remote file belongs to is not read
			// 2. update date of the article to which the remote file belongs to
			// 3. the file length
			db.execSQL("DROP VIEW IF EXISTS remotefile_sequence");
			db.execSQL("CREATE VIEW IF NOT EXISTS remotefile_sequence AS"
					+ " SELECT r.*, MAX(a.isUnread) AS isUnread,"
					+ "   MAX(a.updateDate) AS articleUpdateDate,"
					+ "   MAX(a.isUnread)||MAX(a.updateDate)||(100000000000-r.length)"
					+ "	 AS ord"
					+ " FROM " + TABLE_REMOTEFILES + " r,"
					+ TABLE_REMOTEFILE2ARTICLE + " m,"
					+ TABLE_ARTICLES + " a"
					+ " WHERE m.remotefileId=r.id AND m.articleId=a._id"
					+ " GROUP BY r.id");
			// @formatter:on
		}

	}

	// *******| INSERT |*******************************************************************

	void insertCategories(Set<Category> set) {
		if (!isDBAvailable() || set == null || set.isEmpty()) {
			return;
		}

		List<Category> list = new ArrayList<>(set);
		Collections.sort(list);

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.beginTransaction();
			for (int i = 0; i < list.size(); i++) {
				Category c = list.get(i);
				insertCategory.bindLong(1, c.id);
				insertCategory.bindString(2, c.title == null ? "" : c.title);
				insertCategory.bindLong(3, c.unread);
				if (isDBAvailable()) {
					insertCategory.execute();
				}
			}
			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}
	}

	int insertFeedIcon(int id, byte[] icon) {
		int ret = -1;
		if (!isDBAvailable() || icon == null || icon.length == 0) {
			return ret;
		}

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		ContentValues cv = new ContentValues(1);
		write.lock();
		try {
			db.beginTransaction();
			cv.put("icon", icon);
			ret = db.update(TABLE_FEEDS, cv, "_id = " + id, null);
			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}
		return ret;
	}

	private void insertFeed(int id, int categoryId, String title, String url, int unread, byte[] icon) {
		if (title == null)
			title = "";
		if (url == null)
			url = "";

		synchronized (insertFeedLock) {
			insertFeed.bindLong(1, Integer.valueOf(id).longValue());
			insertFeed.bindLong(2, Integer.valueOf(categoryId).longValue());
			insertFeed.bindString(3, title);
			insertFeed.bindString(4, url);
			insertFeed.bindLong(5, unread);
			if (icon != null && icon.length > 0) {
				insertFeed.bindBlob(6, icon);
			} else {
				insertFeed.bindNull(6);
			}

			if (!isDBAvailable())
				return;
			insertFeed.execute();
		}
	}

	void insertFeeds(Set<Feed> set) {
		if (!isDBAvailable() || set == null || set.isEmpty())
			return;

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.beginTransaction();
			for (Feed f : set) {
				insertFeed(f.id, f.categoryId, f.title, f.url, f.unread, f.icon);
			}
			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}
	}

	private void insertArticleIntern(Article a) {
		if (a.title == null)
			a.title = "";
		if (a.content == null)
			a.content = "";
		if (a.url == null)
			a.url = "";
		if (a.commentUrl == null)
			a.commentUrl = "";
		if (a.updated == null)
			a.updated = new Date();
		if (a.attachments == null)
			a.attachments = new LinkedHashSet<>();
		if (a.labels == null)
			a.labels = new LinkedHashSet<>();
		if (a.author == null)
			a.author = "";
		if (a.note == null)
			a.note = "";

		// articleLabels
		long retId;
		synchronized (insertArticleLock) {
			insertArticle.bindLong(1, a.id);
			insertArticle.bindLong(2, a.feedId);

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
				insertArticle.bindString(3, Html.fromHtml(a.title).toString());
			else
				insertArticle.bindString(3, Html.fromHtml(a.title, Html.FROM_HTML_MODE_COMPACT).toString());

			insertArticle.bindLong(4, (a.isUnread ? 1 : 0));
			insertArticle.bindString(5, a.url);
			insertArticle.bindString(6, a.commentUrl);
			insertArticle.bindLong(7, a.updated.getTime());
			insertArticle.bindString(8, a.content);
			insertArticle.bindString(9, Utils.separateItems(a.attachments, ";"));
			insertArticle.bindLong(10, (a.isStarred ? 1 : 0));
			insertArticle.bindLong(11, (a.isPublished ? 1 : 0));
			insertArticle.bindLong(12, a.id); // ID again for the where-clause
			insertArticle.bindString(13, Utils.separateItems(a.labels, "---"));
			insertArticle.bindString(14, a.author);
			insertArticle.bindString(15, a.note);
			insertArticle.bindLong(16, a.score);

			if (!isDBAvailable())
				return;
			retId = insertArticle.executeInsert();
		}

		if (retId != -1)
			insertLabels(a.id, a.labels);
	}

	void insertArticles(Collection<Article> articles) {
		if (!isDBAvailable() || articles == null || articles.isEmpty())
			return;

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.beginTransaction();
			for (Article a : articles) {
				insertArticleIntern(a);
			}
			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}
	}

	private void insertLabels(int articleId, Set<Label> labels) {
		for (Label label : labels) {
			insertLabel(articleId, label);
		}
	}

	private void insertLabel(int articleId, Label label) {
		if (!isDBAvailable())
			return;

		if (label.id < -10) {
			synchronized (insertLabelLock) {
				insertLabel.bindLong(1, articleId);
				insertLabel.bindLong(2, label.id);
				insertLabel.executeInsert();
			}
		}
	}

	private void removeLabel(int articleId, Label label) {
		if (!isDBAvailable())
			return;

		if (label.id < -10) {
			String[] args = new String[]{articleId + "", label.id + ""};

			SQLiteDatabase db = getOpenHelper().getWritableDatabase();
			write.lock();
			try {
				db.delete(TABLE_ARTICLES2LABELS, "articleId=? AND labelId=?", args);
			} finally {
				write.unlock();
			}
		}
	}

	void insertLabels(Set<Integer> articleIds, Label label, boolean assign) {
		if (!isDBAvailable())
			return;

		for (Integer articleId : articleIds) {
			if (assign)
				insertLabel(articleId, label);
			else
				removeLabel(articleId, label);
		}
	}

	/**
	 * insert given remote file into DB
	 *
	 * @param url remote file URL
	 * @return remote file id, which was inserted or already exist in DB
	 */
	private long insertRemoteFile(String url) {
		long ret = 0;

		try {
			synchronized (insertRemoteFileLock) {
				insertRemoteFile.bindString(1, url);
				// extension (reserved for future)
				insertRemoteFile.bindString(2, "");

				if (isDBAvailable())
					ret = insertRemoteFile.executeInsert();
			}
		} catch (SQLException e) {
			// if this remote file already in DB, get its ID
			RemoteFile rf = getRemoteFile(url);
			if (rf != null)
				ret = rf.id;
		}

		return ret;
	}

	/**
	 * insert given relation (remotefileId <-> articleId) into DB
	 *
	 * @param rfId remote file ID
	 * @param aId  article ID
	 */
	private void insertRemoteFile2Article(long rfId, long aId) {
		synchronized (insertRemoteFile2ArticleLock) {
			insertRemoteFile2Article.bindLong(1, rfId);
			// extension (reserved for future)
			insertRemoteFile2Article.bindLong(2, aId);

			if (isDBAvailable()) {
				try {
					insertRemoteFile2Article.executeInsert();
				} catch (SQLiteConstraintException e) {
					Log.w(TAG, "Article with id " + aId + " was removed before we added the corresponding remote-files. This warning can safely be" + " ignored.");
				}
			}
		}
	}

	// *******| UPDATE |*******************************************************************

	/**
	 * set read status in DB for given category/feed
	 *
	 * @param id         category/feed ID
	 * @param isCategory if set to {@code true}, then given id is category
	 *                   ID, otherwise - feed ID
	 * @return collection of article IDs, which was marked as read or {@code null} if nothing was changed
	 */
	Collection<Integer> markRead(int id, boolean isCategory) {
		Set<Integer> ret = null;
		if (!isDBAvailable())
			return null;

		StringBuilder where = new StringBuilder();
		StringBuilder feedIds = new StringBuilder();
		switch (id) {
			case Data.VCAT_ALL:
				where.append(" 1 "); // Select everything...
				break;
			case Data.VCAT_FRESH:
				long time = System.currentTimeMillis() - Controller.getInstance().getFreshArticleMaxAge();
				where.append(" updateDate > ").append(time);
				break;
			case Data.VCAT_PUB:
				where.append(" isPublished > 0 ");
				break;
			case Data.VCAT_STAR:
				where.append(" isStarred > 0 ");
				break;
			default:
				if (isCategory) {
					feedIds.append("SELECT _id FROM ").append(TABLE_FEEDS).append(" WHERE categoryId=").append(id);
				} else {
					feedIds.append(id);
				}
				where.append(" feedId IN (").append(feedIds).append(") ");
				break;
		}

		where.append(" and isUnread>0 ");

		Cursor c = null;
		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		read.lock();
		try {
			// select id from articles where categoryId in (...)
			c = db.query(TABLE_ARTICLES, new String[]{"_id"}, where.toString(), null, null, null, null);

			int count = c.getCount();
			if (count > 0) {
				ret = new HashSet<>(count);
				while (c.moveToNext()) {
					ret.add(c.getInt(0));
				}
			}

		} finally {
			close(c);
			read.unlock();
		}

		if (ret != null && !ret.isEmpty()) {
			markArticles(ret, "isUnread", 0);
		}

		return ret;
	}

	/**
	 * mark given property of given articles with given state
	 *
	 * @param idList set of article IDs, which should be processed
	 * @param mark   mark to be set
	 * @param state  value for the mark
	 */
	public void markArticles(Set<Integer> idList, String mark, int state) {
		if (!isDBAvailable())
			return;

		if (idList != null && !idList.isEmpty()) {
			SQLiteDatabase db = getOpenHelper().getWritableDatabase();
			write.lock();
			try {
				db.beginTransaction();
				for (String ids : StringSupport.convertListToString(idList, 400)) {
					markArticles(ids, mark, "" + state);
				}
				db.setTransactionSuccessful();
			} finally {
				try {
					db.endTransaction();
				} finally {
					write.unlock();
				}
			}
		}
	}

	/**
	 * mark given property of given article with given state
	 *
	 * @param id    set of article IDs, which should be processed
	 * @param mark  mark to be set
	 * @param state value for the mark
	 */
	public void markArticle(int id, String mark, int state) {
		if (!isDBAvailable())
			return;

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.beginTransaction();
			markArticles("" + id, mark, "" + state);
			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}
	}

	/**
	 * mark given property of given article with given state
	 *
	 * @param id   set of article IDs, which should be processed
	 * @param note the note to be set
	 */
	public void addArticleNote(int id, String note) {
		if (!isDBAvailable())
			return;

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.beginTransaction();
			markArticles("" + id, "note", note);
			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}
	}

	/**
	 * mark given property of given articles with given state
	 *
	 * @param idList set of article IDs, which should be processed
	 * @param mark   mark to be set
	 * @param state  value for the mark
	 * @return the number of rows affected
	 */
	private int markArticles(String idList, String mark, String state) {
		int ret = 0;
		if (!isDBAvailable())
			return ret;

		ContentValues cv = new ContentValues(1);
		cv.put(mark, state);

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.beginTransaction();
			ret = db.update(TABLE_ARTICLES, cv, "_id IN (" + idList + ") AND ? != ?", new String[]{mark, String.valueOf(state)});
			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}

		return ret;
	}

	void markUnsynchronizedStates(Collection<Integer> ids, String mark, int state) {
		if (!isDBAvailable())
			return;

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.beginTransaction();
			for (Integer id : ids) {
				// First update, then insert. If row exists it gets updated and second call ignores it, else the second
				// call inserts it.
				db.execSQL(String.format("UPDATE %s SET %s=%s WHERE id=%s", TABLE_MARK, mark, state, id));
				db.execSQL(String.format("INSERT OR IGNORE INTO %s (id, %s) VALUES (%s, %s)", TABLE_MARK, mark, id, state));
			}
			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}
	}

	// Special treatment for notes since the method markUnsynchronizedStates(...) doesn't support inserting any
	// additional data.
	void markUnsynchronizedNotes(Map<Integer, String> ids) {
		if (!isDBAvailable())
			return;

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.beginTransaction();
			for (Map.Entry<Integer, String> entry : ids.entrySet()) {
				if (entry.getValue() == null)
					continue;
				ContentValues cv = new ContentValues(2);
				cv.put("_id", entry.getKey());
				cv.put(COL_NOTE, entry.getValue());
				db.insert(TABLE_NOTES, null, cv);
			}
			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}
	}

	/**
	 * Set unread counters for feeds and categories according to real amount of unread articles. Split up the tasks in
	 * single transactions so we can do other work in between.
	 */
	void calculateCounters() {
		if (!isDBAvailable())
			return;

		long time = System.currentTimeMillis();
		int total = 0;

		write.lock();
		try {
			countResetFeedsAndCategories();
			total += countFeedsWithUnread();
			countCategoriesWithUnread();
			countSpecialCategories(total);
		} finally {
			write.unlock();
		}

		Log.i(TAG, String.format("Recalculated counters, total unread: %s (took %sms)", total, (System.currentTimeMillis() - time)));
	}

	/**
	 * First of all, reset all feeds and all categories to unread=0.
	 */
	private void countResetFeedsAndCategories() {
		final SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		try {
			db.beginTransaction();
			final ContentValues cv = new ContentValues(1);
			cv.put(COL_UNREAD, 0);
			db.update(TABLE_FEEDS, cv, null, null);
			db.update(TABLE_CATEGORIES, cv, null, null);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * Count all feeds where unread articles exist.
	 *
	 * @return the number of overall unread articles
	 */
	private int countFeedsWithUnread() {
		int total = 0;
		Cursor c = null;
		final SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		try {
			db.beginTransaction();
			final ContentValues cv = new ContentValues(1);
			c = db.query(TABLE_ARTICLES, new String[]{"feedId", "count(*)"}, "isUnread>0", null, "feedId", null, null, null);

			// update feeds
			while (c.moveToNext()) {
				int feedId = c.getInt(0);
				int unreadCount = c.getInt(1);
				total += unreadCount;

				cv.put(COL_UNREAD, unreadCount);
				db.update(TABLE_FEEDS, cv, "_id=" + feedId, null);
			}
			db.setTransactionSuccessful();
		} finally {
			if (c != null && !c.isClosed())
				c.close();
			db.endTransaction();
		}
		return total;
	}

	/**
	 * Count all categories where feeds with unread articles exist.
	 */
	private void countCategoriesWithUnread() {
		Cursor c = null;
		final SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		try {
			db.beginTransaction();
			final ContentValues cv = new ContentValues(1);
			c = db.query(TABLE_FEEDS, new String[]{"categoryId", "sum(unread)"}, "categoryId>=0", null, "categoryId", null, null, null);

			// update real categories
			while (c.moveToNext()) {
				int categoryId = c.getInt(0);
				int unreadCount = c.getInt(1);

				cv.put(COL_UNREAD, unreadCount);
				db.update(TABLE_CATEGORIES, cv, "_id=" + categoryId, null);
			}
			db.setTransactionSuccessful();
		} finally {
			close(c);
			db.endTransaction();
		}
	}

	/**
	 * Count special categories.
	 */
	private void countSpecialCategories(final int total) {
		final SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		try {
			db.beginTransaction();
			final ContentValues cv = new ContentValues(1);
			cv.put(COL_UNREAD, total);
			db.update(TABLE_CATEGORIES, cv, "_id=" + Data.VCAT_ALL, null);

			cv.put(COL_UNREAD, getUnreadCount(Data.VCAT_FRESH, true));
			db.update(TABLE_CATEGORIES, cv, "_id=" + Data.VCAT_FRESH, null);

			cv.put(COL_UNREAD, getUnreadCount(Data.VCAT_PUB, true));
			db.update(TABLE_CATEGORIES, cv, "_id=" + Data.VCAT_PUB, null);

			cv.put(COL_UNREAD, getUnreadCount(Data.VCAT_STAR, true));
			db.update(TABLE_CATEGORIES, cv, "_id=" + Data.VCAT_STAR, null);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * update amount of remote file references for article.
	 * normally should only be used with {@code null} ("unknown") and {@code 0} (no references)
	 *
	 * @param id         ID of article, which should be updated
	 * @param filesCount new value for remote file references (may be {@code null})
	 */
	public void updateArticleCachedImages(int id, Integer filesCount) {
		if (!isDBAvailable())
			return;

		ContentValues cv = new ContentValues(1);
		if (filesCount == null)
			cv.putNull("cachedImages");
		else
			cv.put("cachedImages", filesCount);

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.update(TABLE_ARTICLES, cv, "_id=?", new String[]{String.valueOf(id)});
		} finally {
			write.unlock();
		}
	}

	void deleteCategories(boolean withVirtualCategories) {
		if (!isDBAvailable())
			return;

		String wherePart = "";
		if (!withVirtualCategories)
			wherePart = "_id > 0";

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.delete(TABLE_CATEGORIES, wherePart, null);
		} finally {
			write.unlock();
		}
	}

	/**
	 * delete all rows from feeds table
	 */
	void deleteFeeds() {
		if (!isDBAvailable())
			return;

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.delete(TABLE_FEEDS, null, null);
		} finally {
			write.unlock();
		}
	}

	/**
	 * delete articles and all its resources (e.g. remote files, labels etc.)
	 *
	 * @param whereClause the optional WHERE clause to apply when deleting.
	 *                    Passing null will delete all rows.
	 * @param whereArgs   You may include ?s in the where clause, which
	 *                    will be replaced by the values from whereArgs. The values
	 *                    will be bound as Strings.
	 * @return the number of rows affected if a whereClause is passed in, 0
	 * otherwise. To remove all rows and get a count pass "1" as the
	 * whereClause.
	 */
	private int safelyDeleteArticles(String whereClause, String[] whereArgs) {
		int deletedCount;

		Collection<RemoteFile> rfs = getRemoteFilesForArticles(whereClause, whereArgs, true);
		if (rfs != null && !rfs.isEmpty()) {
			Set<Integer> rfIds = new HashSet<>(rfs.size());
			for (RemoteFile rf : rfs) {
				rfIds.add(rf.id);
				ImageCache imageCache = Controller.getInstance().getImageCache();
				if (imageCache != null) {
					File file = imageCache.getCacheFile(rf.url);
					boolean deleted = file.delete();
					if (!deleted)
						Log.e(TAG, "Couldn't delete file: " + file.getAbsolutePath());
				}
			}
			deleteRemoteFiles(rfIds);
		}

		// @formatter:off
		StringBuilder query = new StringBuilder();
		query.append(
				" articleId IN (").append(
				"	 SELECT _id").append(
				"	   FROM ").append(
				TABLE_ARTICLES).append(
				"	   WHERE ").append(
				whereClause).append(
				" )");
		// @formatter:on

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.beginTransaction();
			// first, delete article referencies from linking table to preserve foreign key constraint on the next step
			db.delete(TABLE_REMOTEFILE2ARTICLE, query.toString(), whereArgs);

			deletedCount = db.delete(TABLE_ARTICLES, whereClause, whereArgs);
			purgeLabels();
			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}

		return deletedCount;
	}

	/**
	 * Delete given amount of last updated articles from DB. Published and Starred articles are ignored
	 * so the configured limit is not an exact upper limit to the number of articles in the database.
	 *
	 * @param amountToPurge amount of articles to be purged
	 */
	void purgeLastArticles(int amountToPurge) {
		if (!isDBAvailable())
			return;

		long time = System.currentTimeMillis();
		String query = "_id IN ( SELECT _id FROM " + TABLE_ARTICLES + " WHERE isPublished=0 AND isStarred=0 ORDER BY updateDate DESC LIMIT -1 OFFSET " + (Utils.ARTICLE_LIMIT - amountToPurge + ")");

		safelyDeleteArticles(query, null);
		Log.d(TAG, "purgeLastArticles took " + (System.currentTimeMillis() - time) + "ms");
	}

	/**
	 * delete articles, which belongs to non-existent feeds
	 */
	void purgeOrphanedArticles() {
		if (!isDBAvailable())
			return;

		long time = System.currentTimeMillis();
		safelyDeleteArticles("feedId NOT IN (SELECT _id FROM " + TABLE_FEEDS + ")", null);
		Log.d(TAG, "purgeOrphanedArticles took " + (System.currentTimeMillis() - time) + "ms");
	}

	private void purgeLabels() {
		if (!isDBAvailable())
			return;

		// @formatter:off
		String idsArticles = "SELECT a2l.articleId FROM "
				+ TABLE_ARTICLES2LABELS + " AS a2l LEFT OUTER JOIN "
				+ TABLE_ARTICLES + " AS a"
				+ " ON a2l.articleId = a._id WHERE a._id IS null";

		String idsFeeds = "SELECT a2l.labelId FROM "
				+ TABLE_ARTICLES2LABELS + " AS a2l LEFT OUTER JOIN "
				+ TABLE_FEEDS + " AS f"
				+ " ON a2l.labelId = f._id WHERE f._id IS null";
		// @formatter:on

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.delete(TABLE_ARTICLES2LABELS, "articleId IN(" + idsArticles + ")", null);
			db.delete(TABLE_ARTICLES2LABELS, "labelId IN(" + idsFeeds + ")", null);
		} finally {
			write.unlock();
		}
	}

	void handlePurgeMarked(String idList, int minId, String vcat) {
		if (!isDBAvailable())
			return;

		long time = System.currentTimeMillis();
		ContentValues cv = new ContentValues(1);
		cv.put(vcat, 0);

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			int count = db.update(TABLE_ARTICLES, cv, vcat + ">0 AND _id>" + minId + " AND _id NOT IN (" + idList + ")", null);
			long timeDiff = (System.currentTimeMillis() - time);
			Log.d(TAG, String.format("Marked %s articles %s=0 (%s ms)", count, vcat, timeDiff));
		} finally {
			write.unlock();
		}
	}

	// *******| SELECT |*******************************************************************

	public Article getArticle(int id) {
		Article ret = null;
		if (!isDBAvailable())
			return null;

		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			c = db.query(TABLE_ARTICLES, null, "_id=?", new String[]{id + ""}, null, null, null, null);
			if (c.moveToFirst())
				ret = handleArticleCursor(c);
		} finally {
			close(c);
			read.unlock();
		}

		return ret;
	}

	Set<Label> getLabelsForArticle(int articleId) {
		if (!isDBAvailable())
			return new HashSet<>();

		// @formatter:off
		String sql = "SELECT f._id, f.title, 0 checked FROM " + TABLE_FEEDS + " f "
				+ "	 WHERE f._id <= -11 AND"
				+ "	 NOT EXISTS (SELECT * FROM " + TABLE_ARTICLES2LABELS
				+ " a2l where f._id = a2l.labelId AND a2l.articleId = " + articleId + ")"
				+ " UNION"
				+ " SELECT f._id, f.title, 1 checked FROM " + TABLE_FEEDS + " f, " + TABLE_ARTICLES2LABELS + " a2l "
				+ "	 WHERE f._id <= -11 AND f._id = a2l.labelId AND a2l.articleId = " + articleId;
		// @formatter:on

		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			c = db.rawQuery(sql, null);
			Set<Label> ret = new HashSet<>(c.getCount());
			while (c.moveToNext()) {
				Label label = new Label();
				label.id = c.getInt(0);
				label.caption = c.getString(1);
				label.checked = c.getInt(2) == 1;
				ret.add(label);
			}
			return ret;

		} finally {
			close(c);
			read.unlock();
		}
	}

	public Feed getFeed(int id) {
		Feed ret = new Feed();
		if (!isDBAvailable())
			return ret;

		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			c = db.query(TABLE_FEEDS, null, "_id=?", new String[]{id + ""}, null, null, null, null);
			if (c.moveToFirst())
				ret = handleFeedCursor(c);
		} finally {
			close(c);
			read.unlock();
		}

		return ret;
	}

	public Category getCategory(int id) {
		Category ret = new Category();
		if (!isDBAvailable())
			return ret;

		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			c = db.query(TABLE_CATEGORIES, null, "_id=?", new String[]{id + ""}, null, null, null, null);
			if (c.moveToFirst())
				ret = handleCategoryCursor(c);
		} finally {
			close(c);
			read.unlock();
		}

		return ret;
	}

	/**
	 * Retrieves all rows in the categories table
	 *
	 * @param includeVirtual include virtual categories (eg. starred, published, ...)
	 * @param includeRead    include categories that have 0 unread articles
	 * @return a List with all categories in the database, never {@code null}
	 */
	public List<Category> getCategories(boolean includeVirtual, boolean includeRead) {
		if (!isDBAvailable()) {
			return Collections.emptyList();
		}
		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			String selection;
			if (includeVirtual) {
				selection = includeRead ? null : " _id < 0 or unread > 0 ";
			} else {
				selection = includeRead ? " _id > -1 " : " _id > -1 and unread > 0 ";
			}

			c = db.query(TABLE_CATEGORIES, CATEGORIES_COLUMNS, selection, null, null, null, null, null);

			List<Category> categories = new ArrayList<>(c.getCount());
			while (c.moveToNext()) {
				categories.add(handleCategoryCursor(c));
			}
			return categories;
		} catch (SQLException e) {
			Log.e(TAG, "getCategories()", e);
			return Collections.emptyList();
		} finally {
			close(c);
			read.unlock();
		}
	}

	/**
	 * Retrieves all labels as Category instance
	 *
	 * @return a List with all labels in the database, never {@code null}
	 */
	public List<Category> getLabelsAsCategories(boolean includeRead) {
		if (!isDBAvailable()) {
			return Collections.emptyList();
		}
		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			if (includeRead) {
				c = db.query(TABLE_FEEDS, CATEGORIES_COLUMNS, " _id < -10 ", null, null, null, null, null);
			} else {
				c = db.query(TABLE_FEEDS, CATEGORIES_COLUMNS, " _id < -10 and unread > 0", null, null, null, null, null);
			}
			List<Category> categories = new ArrayList<>(c.getCount());
			while (c.moveToNext()) {
				categories.add(handleCategoryCursor(c));
			}
			return categories;
		} catch (SQLException e) {
			Log.e(TAG, "getCategories()", e);
			return Collections.emptyList();
		} finally {
			close(c);
			read.unlock();
		}
	}

	/**
	 * get the map of article IDs to its update date from DB
	 *
	 * @param selection A filter declaring which articles should be considered, formatted as an SQL WHERE clause
	 *                  (excluding the WHERE itself). Passing null will return all rows.
	 * @return map of unread article IDs to its update date (may be {@code null})
	 */
	@SuppressLint("UseSparseArrays")
	public Map<Integer, Long> getArticleIdUpdatedMap(String selection) {
		Map<Integer, Long> ret;
		if (!isDBAvailable())
			return null;

		Cursor c = null;
		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		read.lock();
		try {
			c = db.query(TABLE_ARTICLES, new String[]{"_id", "updateDate"}, selection, null, null, null, null);
			ret = new HashMap<>(c.getCount());
			while (c.moveToNext()) {
				ret.put(c.getInt(0), c.getLong(1));
			}
		} finally {
			close(c);
			read.unlock();
		}
		return ret;
	}

	/**
	 * 0 - Uncategorized
	 * -1 - Special (e.g. Starred, Published, Archived, etc.) <- these are categories here o.O
	 * -2 - Labels
	 * -3 - All feeds, excluding virtual feeds (e.g. Labels and such)
	 * -4 - All feeds, including virtual feeds
	 */
	public Set<Feed> getFeeds(int categoryId) {
		if (!isDBAvailable())
			return new LinkedHashSet<>();

		String where = null; // categoryId = 0
		if (categoryId >= 0)
			where = "categoryId=" + categoryId;
		switch (categoryId) {
			case -1:
				where = "_id IN (0, -2, -3)";
				break;
			case -2:
				where = "_id < -10";
				break;
			case -3:
				where = "categoryId >= 0";
				break;
			case -4:
				where = null;
				break;
		}

		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			c = db.query(TABLE_FEEDS, null, where, null, null, null, "UPPER(title) ASC");
			Set<Feed> ret = new LinkedHashSet<>(c.getCount());
			while (c.moveToNext()) {
				ret.add(handleFeedCursor(c));
			}
			return ret;
		} finally {
			close(c);
			read.unlock();
		}
	}

	public Set<Category> getAllCategories() {
		if (!isDBAvailable())
			return new LinkedHashSet<>();

		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			c = db.query(TABLE_CATEGORIES, null, "_id>=0", null, null, null, "title ASC");
			Set<Category> ret = new LinkedHashSet<>(c.getCount());
			while (c.moveToNext()) {
				ret.add(handleCategoryCursor(c));
			}
			return ret;
		} finally {
			close(c);
			read.unlock();
		}
	}

	public int getUnreadCount(int id, boolean isCat) {
		if (!isDBAvailable())
			return 0;

		StringBuilder selection = new StringBuilder("isUnread>0");
		String[] selectionArgs = new String[]{String.valueOf(id)};

		if (isCat && id >= 0) {
			// real categories
			selection.append(" and feedId in (select _id from feeds where categoryId=?)");
		} else {
			if (id < 0) {
				// virtual categories
				switch (id) {
					// All Articles
					case Data.VCAT_ALL:
						selectionArgs = null;
						break;

					// Fresh Articles
					case Data.VCAT_FRESH:
						selection.append(" and updateDate>?");
						selectionArgs = new String[]{String.valueOf(new Date().getTime() - Controller.getInstance().getFreshArticleMaxAge())};
						break;

					// Published Articles
					case Data.VCAT_PUB:
						selection.append(" and isPublished>0");
						selectionArgs = null;
						break;

					// Starred Articles
					case Data.VCAT_STAR:
						selection.append(" and isStarred>0");
						selectionArgs = null;
						break;

					default:
						// Probably a label...
						selection.append(" and feedId=?");
				}
			} else {
				// feeds
				selection.append(" and feedId=?");
			}
		}

		// Read count for given feed
		int ret = 0;
		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			c = db.query(TABLE_ARTICLES, new String[]{"count(*)"}, selection.toString(), selectionArgs, null, null, null, null);

			if (c.moveToFirst())
				ret = c.getInt(0);
		} finally {
			close(c);
			read.unlock();
		}

		return ret;
	}

	@SuppressLint("UseSparseArrays")
	Set<Integer> getMarked(String mark, int status) {
		if (!isDBAvailable())
			return new LinkedHashSet<>();

		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			c = db.query(TABLE_MARK, new String[]{"id"}, mark + "=" + status, null, null, null, null, null);
			Set<Integer> ret = new LinkedHashSet<>(c.getCount());
			while (c.moveToNext()) {
				ret.add(c.getInt(0));
			}
			return ret;
		} finally {
			close(c);
			read.unlock();
		}
	}

	Map<Integer, String> getMarkedNotes() {
		if (!isDBAvailable())
			return new HashMap<>();

		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			c = db.query(TABLE_NOTES, new String[]{"_id", COL_NOTE}, null, null, null, null, null, null);
			Map<Integer, String> ret = new HashMap<>(c.getCount());
			while (c.moveToNext()) {
				ret.put(c.getInt(0), c.getString(1));
			}
			return ret;
		} finally {
			close(c);
			read.unlock();
		}
	}

	void setMarked(Set<Integer> ids, String mark) {
		if (!isDBAvailable())
			return;

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.beginTransaction();
			ContentValues cv = new ContentValues(1);
			for (String idList : StringSupport.convertListToString(ids, 1000)) {
				cv.putNull(mark);
				db.update(TABLE_MARK, cv, "id IN(" + idList + ")", null);
			}
			db.delete(TABLE_MARK, "isUnread IS null AND isStarred IS null AND isPublished IS null", null);
			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}
	}

	/**
	 * remove specified mark in the temporary mark table for specified
	 * articles and then cleanup this table
	 *
	 * @param ids article IDs of which the notes should be reset
	 */
	void setMarkedNotes(Map<Integer, String> ids) {
		if (!isDBAvailable())
			return;

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.beginTransaction();
			ContentValues cv = new ContentValues(1);
			for (String idList : StringSupport.convertListToString(ids.keySet(), 1000)) {
				cv.putNull(COL_NOTE);
				db.update(TABLE_NOTES, cv, "_id IN(" + idList + ")", null);
			}
			db.delete(TABLE_NOTES, COL_NOTE + " IS null", null);
			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}
	}

	// *******************************************

	private static Article handleArticleCursor(Cursor c) {
		Article a = new Article();
		a.id = c.getInt(0);
		a.feedId = c.getInt(1);
		a.title = c.getString(2);
		a.isUnread = (c.getInt(3) != 0);
		a.url = c.getString(4);
		a.commentUrl = c.getString(5);
		a.updated = new Date(c.getLong(6));
		a.content = c.getString(7);
		a.attachments = parseAttachments(c.getString(8));
		a.isStarred = (c.getInt(9) != 0);
		a.isPublished = (c.getInt(10) != 0);
		a.labels = parseArticleLabels(c.getString(12));
		a.author = c.getString(13);
		a.note = c.getString(14);
		return a;
	}

	private static Feed handleFeedCursor(Cursor c) {
		Feed f = new Feed();
		f.id = c.getInt(0);
		f.categoryId = c.getInt(1);
		f.title = c.getString(2);
		f.url = c.getString(3);
		f.unread = c.getInt(4);
		f.icon = c.getBlob(5);
		return f;
	}

	private static Category handleCategoryCursor(Cursor c) {
		Category cat = new Category();
		cat.id = c.getInt(0);
		cat.title = c.getString(1);
		cat.unread = c.getInt(2);
		return cat;
	}

	private static RemoteFile handleRemoteFileCursor(Cursor c) {
		RemoteFile rf = new RemoteFile();
		rf.id = c.getInt(0);
		rf.url = c.getString(1);
		rf.length = c.getInt(2);
		rf.updated = new Date(c.getLong(4));
		rf.cached = c.getInt(5) != 0;
		return rf;
	}

	private static Set<String> parseAttachments(String att) {
		Set<String> ret = new LinkedHashSet<>();
		if (att == null)
			return ret;

		ret.addAll(Arrays.asList(att.split(";")));
		return ret;
	}

	/*
	 * Parse labels from string of the form "label;;label;;...;;label" where each label is of the following format:
	 * "caption;forground;background"
	 */
	private static Set<Label> parseArticleLabels(String labelStr) {
		Set<Label> ret = new LinkedHashSet<>();
		if (labelStr == null)
			return ret;

		int i = 0;
		for (String s : labelStr.split("---")) {
			String[] l = s.split(";");
			if (l.length > 0) {
				i++;
				Label label = new Label();
				label.id = i;
				label.checked = true;
				label.caption = l[0];
				if (l.length > 1 && l[1].startsWith("#"))
					label.foregroundColor = l[1];
				if (l.length > 2 && l[1].startsWith("#"))
					label.backgroundColor = l[2];
				ret.add(label);
			}
		}

		return ret;
	}

	public ArrayList<Article> queryArticlesForImagecache() {
		if (!isDBAvailable())
			return null;

		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			c = db.query(TABLE_ARTICLES, new String[]{"_id", "content", "attachments"}, "cachedImages IS NULL AND isUnread>0", null, null, null, null, "1000");

			ArrayList<Article> ret = new ArrayList<>(c.getCount());
			while (c.moveToNext()) {
				Article a = new Article();
				a.id = c.getInt(0);
				a.content = c.getString(1);
				a.attachments = parseAttachments(c.getString(2));
				ret.add(a);
			}
			return ret;
		} finally {
			close(c);
			read.unlock();
		}
	}

	private void insertArticleFiles(int articleId, List<String> fileUrls) {
		if (!isDBAvailable())
			return;

		for (String url : fileUrls) {
			long remotefileId = insertRemoteFile(url);
			if (remotefileId != 0)
				insertRemoteFile2Article(remotefileId, articleId);
		}
	}

	/**
	 * insert given remote files into DB and link them with given article
	 *
	 * @param map A map of arrays of remote file URLs mapped to ids of "parent" articles
	 */
	public void insertArticleFiles(Map<Integer, List<String>> map) {
		if (!isDBAvailable())
			return;

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.beginTransaction();
			for (Map.Entry<Integer, List<String>> entry : map.entrySet()) {
				insertArticleFiles(entry.getKey(), entry.getValue());
			}
			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}
	}

	/**
	 * get the DB object representing remote file by its URL
	 *
	 * @param url remote file URL
	 * @return remote file object from DB
	 */
	private RemoteFile getRemoteFile(String url) {
		if (!isDBAvailable())
			return null;

		RemoteFile rf = null;
		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			c = db.query(TABLE_REMOTEFILES, null, "url=?", new String[]{url}, null, null, null, null);
			if (c.moveToFirst())
				rf = handleRemoteFileCursor(c);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(c);
			read.unlock();
		}
		return rf;
	}

	/**
	 * get remote files for given article
	 *
	 * @param articleId article, which remote files should be found
	 * @return collection of remote file objects from DB or {@code null}
	 */
	public Collection<RemoteFile> getRemoteFiles(int articleId) {
		if (!isDBAvailable())
			return null;

		ArrayList<RemoteFile> rfs = null;
		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			// @formatter:off
			c = db.rawQuery(" SELECT r.*"
							+ " FROM "
							+ TABLE_REMOTEFILES + " r,"
							+ TABLE_REMOTEFILE2ARTICLE + " m, "
							+ TABLE_ARTICLES + " a"
							+ " WHERE m.remotefileId=r.id"
							+ "   AND m.articleId=a._id"
							+ "   AND a._id=?",
					new String[]{String.valueOf(articleId)});
			// @formatter:on

			rfs = new ArrayList<>(c.getCount());

			while (c.moveToNext()) {
				rfs.add(handleRemoteFileCursor(c));
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(c);
			read.unlock();
		}
		return rfs;
	}

	/**
	 * get remote files for given articles
	 *
	 * @param whereClause the WHERE clause to apply when selecting.
	 * @param whereArgs   You may include ?s in the where clause, which
	 *                    will be replaced by the values from whereArgs. The values
	 *                    will be bound as Strings.
	 * @param uniqOnly    if set to {@code true}, then only remote files, which are referenced by given articles only
	 *                    will be
	 *                    returned, otherwise all remote files referenced by given articles will be found (even those,
	 *                    which are
	 *                    referenced also by some other articles)
	 * @return collection of remote file objects from DB or {@code null}
	 */
	private Collection<RemoteFile> getRemoteFilesForArticles(String whereClause, String[] whereArgs, boolean uniqOnly) {
		if (!isDBAvailable())
			return null;

		ArrayList<RemoteFile> rfs = null;
		StringBuilder uniqRestriction = new StringBuilder();
		String[] queryArgs = whereArgs;

		if (uniqOnly) {
			// @formatter:off
			uniqRestriction.append(
					" AND m.remotefileId NOT IN (").append(
					"   SELECT remotefileId").append(
					"	 FROM ").append(
					TABLE_REMOTEFILE2ARTICLE).append(
					"		   WHERE remotefileId IN (").append(
					"	   SELECT remotefileId").append(
					"		 FROM ").append(
					TABLE_REMOTEFILE2ARTICLE).append(
					"		 WHERE articleId IN (").append(
					"		   SELECT _id").append(
					"			 FROM ").append(
					TABLE_ARTICLES).append(
					"			 WHERE ").append(
					whereClause).append(
					"		   )").append(
					"		 GROUP BY remotefileId)").append(
					"	   AND articleId NOT IN (").append(
					"		 SELECT _id").append(
					"		   FROM ").append(
					TABLE_ARTICLES).append(
					"		   WHERE ").append(
					whereClause).append(
					"	   )").append(
					"   GROUP by remotefileId)");
			// @formatter:on

			// because we are using whereClause twice in uniqRestriction, then we should also extend queryArgs,
			// which will be used in query
			if (whereArgs != null) {
				int initialLength = whereArgs.length;
				queryArgs = new String[initialLength * 3];
				for (int i = 0; i < 3; i++) {
					System.arraycopy(whereArgs, 0, queryArgs, i * initialLength, initialLength);
				}
			}
		}

		StringBuilder query = new StringBuilder();
		// @formatter:off
		query.append(
				" SELECT r.*").append(
				"   FROM ").append(
				TABLE_REMOTEFILES + " r,").append(
				TABLE_REMOTEFILE2ARTICLE + " m, ").append(
				TABLE_ARTICLES + " a").append(
				"   WHERE m.remotefileId=r.id").append(
				"	 AND m.articleId=a._id").append(
				"	 AND a._id IN (").append(
				"	   SELECT _id FROM ").append(
				TABLE_ARTICLES).append(
				"	   WHERE ").append(
				whereClause).append(
				"	 )").append(
				uniqRestriction).append(
				"   GROUP BY r.id");
		// @formatter:on

		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			long time = System.currentTimeMillis();
			c = db.rawQuery(query.toString(), queryArgs);

			rfs = new ArrayList<>();

			while (c.moveToNext()) {
				rfs.add(handleRemoteFileCursor(c));
			}
			Log.d(TAG, "Query in getRemoteFilesForArticles took " + (System.currentTimeMillis() - time) + "ms... (remotefiles: " + rfs.size() + ")");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(c);
			read.unlock();
		}
		return rfs;
	}

	/**
	 * mark given remote file as cached/uncached and optionally specify it's file size
	 *
	 * @param remoteFiles A map of file sizes mapped to their remote file URL
	 */
	public void markRemoteFilesCached(Map<String, Long> remoteFiles) {
		if (!isDBAvailable())
			return;

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.beginTransaction();

			for (String url : remoteFiles.keySet()) {
				ContentValues cv = new ContentValues(2);
				Long size = remoteFiles.get(url);
				if (size == null) {
					cv.put("cached", false);
					cv.put("length", 0);
				} else if (size <= 0) {
					cv.put("cached", false);
					cv.put("length", -size);
				} else {
					cv.put("cached", true);
					cv.put("length", size);
				}
				db.update(TABLE_REMOTEFILES, cv, "url=?", new String[]{url});
			}

			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}
	}

	/**
	 * mark remote files with given IDs as non cached (cached=0)
	 *
	 * @param rfIds IDs of remote files to be marked as non-cached
	 */
	public void markRemoteFilesNonCached(Collection<Integer> rfIds) {
		if (!isDBAvailable())
			return;

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.beginTransaction();
			ContentValues cv = new ContentValues(1);
			cv.put("cached", 0);
			for (String ids : StringSupport.convertListToString(rfIds, 1000)) {
				db.update(TABLE_REMOTEFILES, cv, "id in (" + ids + ")", null);
			}
			db.setTransactionSuccessful();
		} finally {
			try {
				db.endTransaction();
			} finally {
				write.unlock();
			}
		}
	}

	/**
	 * get summary length of remote files, which are cached
	 *
	 * @return summary length of remote files
	 */
	public long getCachedFilesSize() {
		if (!isDBAvailable())
			return 0;

		long ret = 0;
		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			c = db.query(TABLE_REMOTEFILES, new String[]{"SUM(length)"}, "cached=1", null, null, null, null);
			if (c.moveToFirst())
				ret = c.getLong(0);
		} finally {
			close(c);
			read.unlock();
		}
		return ret;
	}

	/**
	 * get remote files which should be deleted to free given amount of space
	 *
	 * @param spaceToBeFreed amount of space (summary file size) to be freed
	 * @return collection of remote files, which can be deleted
	 * to free given amount of space
	 */
	public Collection<RemoteFile> getUncacheFiles(long spaceToBeFreed) {
		if (!isDBAvailable())
			return null;

		ArrayList<RemoteFile> rfs = new ArrayList<>();
		SQLiteDatabase db = getOpenHelper().getReadableDatabase();
		Cursor c = null;
		read.lock();
		try {
			c = db.query("remotefile_sequence", null, "cached = 1", null, null, null, "ord");

			long spaceToFree = spaceToBeFreed;
			while (spaceToFree > 0 && c.moveToNext()) {
				RemoteFile rf = handleRemoteFileCursor(c);
				spaceToFree -= rf.length;
				rfs.add(rf);
			}
		} finally {
			close(c);
			read.unlock();
		}
		return rfs;
	}

	/**
	 * delete remote files with given IDs
	 *
	 * @param idList set of remote file IDs, which should be deleted
	 * @return the number of deleted rows
	 */
	private int deleteRemoteFiles(Set<Integer> idList) {
		if (!isDBAvailable())
			return 0;

		int deletedCount = 0;
		if (idList != null && !idList.isEmpty()) {
			SQLiteDatabase db = getOpenHelper().getWritableDatabase();
			write.lock();
			try {
				for (String ids : StringSupport.convertListToString(idList, 400)) {
					deletedCount += db.delete(TABLE_REMOTEFILES, "id IN (" + ids + ")", null);
				}
			} finally {
				write.unlock();
			}
		}
		return deletedCount;
	}

	/**
	 * delete all remote files
	 *
	 * @return the number of deleted rows
	 */
	public int deleteAllRemoteFiles() {
		if (!isDBAvailable())
			return 0;

		SQLiteDatabase db = getOpenHelper().getWritableDatabase();
		write.lock();
		try {
			db.delete(TABLE_REMOTEFILE2ARTICLE, null, null);
			int count = db.delete(TABLE_REMOTEFILES, null, null);

			ContentValues cv = new ContentValues();
			cv.putNull("cachedImages");
			db.update(TABLE_ARTICLES, cv, "cachedImages IS NOT NULL", null);

			return count;
		} finally {
			write.unlock();
		}
	}

	/**
	 * Closes cursor quietly, logging any exceptions
	 *
	 * @param cursor a cursor, not {@code null}
	 */
	public static void close(Cursor cursor) {
		try {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		} catch (Exception e) {
			Log.w(TAG, "close(cursor)", e);
		}
	}

}
