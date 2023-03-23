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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.util.Log;

import org.ttrssreader.R;
import org.ttrssreader.imageCache.ImageCache;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.pojos.Label;
import org.ttrssreader.net.IArticleOmitter;
import org.ttrssreader.net.IdUnreadArticleOmitter;
import org.ttrssreader.net.IdUpdatedArticleOmitter;
import org.ttrssreader.net.JSONConnector;
import org.ttrssreader.utils.Utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@SuppressLint("UseSparseArrays")
public class Data {

	private static final String TAG = Data.class.getSimpleName();

	private static final int VCAT_UNCAT = 0;
	public static final int VCAT_STAR = -1;
	public static final int VCAT_PUB = -2;
	public static final int VCAT_FRESH = -3;
	public static final int VCAT_ALL = -4;

	private static final String VIEW_ALL = "all_articles";
	private static final String VIEW_UNREAD = "unread";

	private static final int FETCH_ARTICLES_LIMIT = 1000;

	private long time;
	private long articlesCached;
	private Map<Integer, Long> articlesChanged;

	/**
	 * map of category id to last changed time
	 */
	private Map<Integer, Long> feedsChanged;
	private long virtCategoriesChanged;
	private long categoriesChanged;

	private ConnectivityManager cm;

	// Singleton (see http://stackoverflow.com/a/11165926)
	private Data() {
		initTimers();
	}

	public void initTimers() {
		time = 0;
		articlesCached = 0;
		articlesChanged = new HashMap<>();
		feedsChanged = new HashMap<>();
		virtCategoriesChanged = 0;
		categoriesChanged = 0;
	}

	private static class InstanceHolder {
		private static final Data instance = new Data();
	}

	public static Data getInstance() {
		return InstanceHolder.instance;
	}

	public synchronized void initialize(final Context context) {
		if (context != null) {
			cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				initNotificationChannels(context);
			}

			/*
			 * See commented code in Controller.sessionId()
			 * See commented code in JSONConnector.login()
			 *
			new AsyncTask<Void, Void, Void>() {
				protected Void doInBackground(Void... params) {
					// Login in background since otherwise this would lead to NetworkOnMainThreadException
					// We try to use the stored sessionId here to reduce number of login() calls...
					String oldSessionId = Controller.getInstance().sessionId();
					String newSessionId = Controller.getInstance().getConnector().login(oldSessionId);
					if (!oldSessionId.equals(newSessionId)) {
						Log.d(TAG, "Saving new SessionId: " + newSessionId);
						Controller.getInstance().setSessionId(newSessionId);
					}
					return null;
				}
			}.execute();
			*/
		}
	}

	public static final String NOTIFICATION_CHANNEL_ID_TASKER = "org.ttrssreader.tasker";
	public static final String NOTIFICATION_CHANNEL_ID_MEDIADOWNLOAD = "org.ttrssreader.mediadownload";

	private void initNotificationChannels(final Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			if (nm != null) {
				nm.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID_TASKER, "Tasker-Service", NotificationManager.IMPORTANCE_DEFAULT));
				nm.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID_MEDIADOWNLOAD, "Media Download-Service", NotificationManager.IMPORTANCE_DEFAULT));
			}
		}
	}

	// *** ARTICLES *********************************************************************

	/**
	 * cache all articles
	 *
	 * @param overrideOffline do not check connected state
	 * @param overrideDelay   if set to {@code true} enforces the update, otherwise the time from last update will be
	 *                        considered
	 */
	public void cacheArticles(boolean overrideOffline, boolean overrideDelay) {
		int limit = FETCH_ARTICLES_LIMIT;
		if (Controller.getInstance().isLowMemory())
			limit = limit / 2;

		if (!overrideDelay && (time > (System.currentTimeMillis() - Utils.UPDATE_TIME))) {
			return;
		} else if (!Utils.isConnected(cm) && !(overrideOffline && Utils.checkConnected(cm))) {
			return;
		}

		Set<Article> articles = new HashSet<>();
		int sinceId = Controller.getInstance().getSinceId();

		long timeStart = System.currentTimeMillis();
		IArticleOmitter filter = new IdUpdatedArticleOmitter("isUnread>0", 0);

		Controller.getInstance().getConnector().getHeadlines(articles, VCAT_ALL, limit, VIEW_UNREAD, true, 0, null, filter);

		final Article newestCachedArticle = DBHelper.getInstance().getArticle(sinceId);
		IArticleOmitter updatedFilter = null;
		if (newestCachedArticle != null)
			updatedFilter = new IdUnreadArticleOmitter(newestCachedArticle.updated);

		Controller.getInstance().getConnector().getHeadlines(articles, VCAT_ALL, limit, VIEW_ALL, true, sinceId, null, updatedFilter);

		handleInsertArticles(articles, true);

		time = System.currentTimeMillis();
		notifyListeners();

		// Store all category-ids and ids of all feeds for this category in db
		articlesCached = time;
		for (Category c : DBHelper.getInstance().getAllCategories()) {
			feedsChanged.put(c.id, time);
		}

		if (!articles.isEmpty() || !filter.getOmittedArticles().isEmpty()) {
			Set<Integer> articleUnreadIds = new HashSet<>(filter.getOmittedArticles());
			for (Article a : articles) {
				if (a.isUnread)
					articleUnreadIds.add(a.id);
			}

			Log.d(TAG, "Amount of unread articles: " + articleUnreadIds.size());
			DBHelper.getInstance().markRead(VCAT_ALL, false);
			DBHelper.getInstance().markArticles(articleUnreadIds, "isUnread", 1);
		}
		Log.d(TAG, "cacheArticles() Took: " + (System.currentTimeMillis() - timeStart) + "ms");
	}

	/**
	 * Downloads the favicon for the given feed ID and inserts it into the database.
	 *
	 * @param feedId ID of the feed
	 */
	public void updateFeedIcon(int feedId) {
		if (!Controller.getInstance().displayFeedIcons())
			return;
		if (feedId <= VCAT_UNCAT) // Virtual feeds don't have icons...
			return;

		try {
			byte[] icon = downloadFeedIcon(feedId);
			DBHelper.getInstance().insertFeedIcon(feedId, icon);
		} catch (MalformedURLException e) {
			Log.e(TAG, "Error while downloading icon for feed #" + feedId, e);
		}
	}

	private byte[] downloadFeedIcon(int feedId) throws MalformedURLException {
		final URL iconUrl = Controller.getInstance().feedIconUrl(feedId);
		return Utils.download(iconUrl);
	}

	/**
	 * update articles for specified feed/category
	 *
	 * @param feedId            feed/category to be updated
	 * @param displayOnlyUnread flag, that indicates, that only unread articles should be shown
	 * @param isCat             if set to {@code true}, then {@code feedId} is actually the category ID
	 * @param overrideOffline   should the "work offline" state be ignored?
	 * @param overrideDelay     should the last update time be ignored?
	 */
	public void updateArticles(int feedId, boolean displayOnlyUnread, boolean isCat, boolean overrideOffline, boolean overrideDelay) {
		Long time = articlesChanged.get(feedId);
		if (isCat) // Category-Ids are in feedsChanged
			time = feedsChanged.get(feedId);

		if (time == null)
			time = 0L;

		if (articlesCached > time && !(feedId == VCAT_PUB || feedId == VCAT_STAR))
			time = articlesCached;

		if (!overrideDelay && time > System.currentTimeMillis() - Utils.UPDATE_TIME) {
			return;
		} else if (!Utils.isConnected(cm) && !(overrideOffline && Utils.checkConnected(cm))) {
			return;
		}

		boolean isVcat = (feedId == VCAT_PUB || feedId == VCAT_STAR);
		int sinceId = 0;

		long timeStart = System.currentTimeMillis();
		IArticleOmitter filter;
		if (isVcat) {
			displayOnlyUnread = false;
			filter = new IdUpdatedArticleOmitter("(isPublished>0 OR isStarred>0)");
		} else {
			sinceId = Controller.getInstance().getSinceId();
			filter = new IdUpdatedArticleOmitter(sinceId);
		}

		// Calculate an appropriate upper limit for the number of articles
		int limit = calculateLimit(feedId, isCat);
		if (limit < FETCH_ARTICLES_LIMIT)
			limit = FETCH_ARTICLES_LIMIT; // Set higher limit to try to update some more articles, fetching only 2 is just not worth the time
		if (Controller.getInstance().isLowMemory())
			limit = limit / 2;

		Log.d(TAG, "UPDATE limit: " + limit);
		Set<Article> articles = new HashSet<>();

		if (!displayOnlyUnread) {
			// If not displaying only unread articles: Refresh unread articles to get them too.
			Controller.getInstance().getConnector().getHeadlines(articles, feedId, limit, VIEW_UNREAD, isCat, 0, null, null);
		}

		String viewMode = (displayOnlyUnread ? VIEW_UNREAD : VIEW_ALL);
		Controller.getInstance().getConnector().getHeadlines(articles, feedId, limit, viewMode, isCat, sinceId, null, filter);

		if (isVcat)
			handlePurgeMarked(articles, feedId);

		handleInsertArticles(articles, false);

		long currentTime = System.currentTimeMillis();
		// Store requested feed-/category-id and ids of all feeds in db for this category if a category was requested
		articlesChanged.put(feedId, currentTime);
		notifyListeners();

		if (isCat) {
			for (Feed f : DBHelper.getInstance().getFeeds(feedId)) {
				articlesChanged.put(f.id, currentTime);
			}
		}
		Log.d(TAG, "updateArticles() Took: " + (System.currentTimeMillis() - timeStart) + "ms");
	}

	/**
	 * Calculate an appropriate upper limit for the number of articles
	 */
	private int calculateLimit(int feedId, boolean isCat) {
		int limit;
		switch (feedId) {
			case VCAT_STAR:  // Starred
			case VCAT_PUB:   // Published
				limit = JSONConnector.PARAM_LIMIT_MAX_VALUE;
				break;
			case VCAT_FRESH: // Fresh
			case VCAT_ALL:   // All Articles
				limit = DBHelper.getInstance().getUnreadCount(feedId, true);
				break;
			default: // Normal categories
				limit = DBHelper.getInstance().getUnreadCount(feedId, isCat);
		}
		if (feedId < -10 && limit <= 0) // Unread-count in DB is wrong for Labels since we only count articles with
			// feedid = ?
			limit = 50;
		return limit;
	}

	private void handlePurgeMarked(Set<Article> articles, int feedId) {
		// TODO Mark all articles with ID > minId as "not starred" and "not published". But why?

		// Search min and max ids
		int minId = Integer.MAX_VALUE;
		Set<String> idSet = new HashSet<>();
		for (Article article : articles) {
			if (article.id < minId)
				minId = article.id;
			idSet.add(article.id + "");
		}

		String idList = Utils.separateItems(idSet, ",");
		String vcat;
		if (feedId == VCAT_STAR)
			vcat = "isStarred";
		else if (feedId == VCAT_PUB)
			vcat = "isPublished";
		else
			return;

		DBHelper.getInstance().handlePurgeMarked(idList, minId, vcat);
	}

	/**
	 * prepare the DB and store given articles
	 *
	 * @param articles articles to be stored
	 */
	private void handleInsertArticles(final Collection<Article> articles, boolean isCaching) {
		if (!articles.isEmpty()) {
			// Search min and max ids
			int minId = Integer.MAX_VALUE;
			int maxId = Integer.MIN_VALUE;
			for (Article article : articles) {
				if (article.id > maxId)
					maxId = article.id;
				if (article.id < minId)
					minId = article.id;
			}

			DBHelper.getInstance().purgeLastArticles(articles.size());
			DBHelper.getInstance().insertArticles(articles);

			// Only store sinceId when doing a full cache of new articles, else it doesn't work.
			if (isCaching) {
				Controller.getInstance().setSinceId(maxId);
				Controller.getInstance().setLastSync(System.currentTimeMillis());
			}
		}
	}

	// *** FEEDS ************************************************************************

	/**
	 * update DB (delete/insert) with actual feeds information from server
	 *
	 * @param categoryId      id of category, which feeds should be returned
	 * @param overrideOffline do not check connected state
	 * @return actual feeds for given category
	 */
	public Set<Feed> updateFeeds(int categoryId, boolean overrideOffline) {

		Long time = feedsChanged.get(categoryId);
		if (time == null)
			time = 0L;

		if (time > System.currentTimeMillis() - Utils.UPDATE_TIME) {
			return null;
		} else if (Utils.isConnected(cm) || (overrideOffline && Utils.checkConnected(cm))) {
			Set<Feed> ret = new LinkedHashSet<>();
			Set<Feed> feeds = Controller.getInstance().getConnector().getFeeds();

			// Only delete feeds if we got new feeds...
			if (!feeds.isEmpty()) {
				for (Feed f : feeds) {
					if (categoryId == VCAT_ALL || f.categoryId == categoryId)
						ret.add(f);

					feedsChanged.put(f.categoryId, System.currentTimeMillis());
				}
				DBHelper.getInstance().deleteFeeds();
				DBHelper.getInstance().insertFeeds(feeds);

				// Store requested category-id and ids of all received feeds
				feedsChanged.put(categoryId, System.currentTimeMillis());
				notifyListeners();
			}

			return ret;
		}
		return null;
	}

	// *** CATEGORIES *******************************************************************

	public Set<Category> updateVirtualCategories(final Context context) {
		if (virtCategoriesChanged > System.currentTimeMillis() - Utils.UPDATE_TIME)
			return null;

		String vCatAll;
		String vCatFresh;
		String vCatPublished;
		String vCatStarred;
		String uncatFeeds;

		vCatAll = (String) context.getText(R.string.VCategory_AllArticles);
		vCatFresh = (String) context.getText(R.string.VCategory_FreshArticles);
		vCatPublished = (String) context.getText(R.string.VCategory_PublishedArticles);
		vCatStarred = (String) context.getText(R.string.VCategory_StarredArticles);
		uncatFeeds = (String) context.getText(R.string.Feed_UncategorizedFeeds);

		Set<Category> vCats = new LinkedHashSet<>();
		vCats.add(new Category(VCAT_ALL, vCatAll, DBHelper.getInstance().getUnreadCount(VCAT_ALL, true)));
		vCats.add(new Category(VCAT_FRESH, vCatFresh, DBHelper.getInstance().getUnreadCount(VCAT_FRESH, true)));
		vCats.add(new Category(VCAT_PUB, vCatPublished, DBHelper.getInstance().getUnreadCount(VCAT_PUB, true)));
		vCats.add(new Category(VCAT_STAR, vCatStarred, DBHelper.getInstance().getUnreadCount(VCAT_STAR, true)));
		vCats.add(new Category(VCAT_UNCAT, uncatFeeds, DBHelper.getInstance().getUnreadCount(VCAT_UNCAT, true)));

		DBHelper.getInstance().insertCategories(vCats);
		notifyListeners();
		virtCategoriesChanged = System.currentTimeMillis();
		return vCats;
	}

	/**
	 * update DB (delete/insert) with actual categories information from server
	 *
	 * @param overrideOffline do not check connected state
	 * @return actual categories
	 */
	public Set<Category> updateCategories(boolean overrideOffline) {
		if (categoriesChanged > System.currentTimeMillis() - Utils.UPDATE_TIME) {
			return null;
		} else if (Utils.isConnected(cm) || overrideOffline) {
			Set<Category> categories = Controller.getInstance().getConnector().getCategories();

			if (!categories.isEmpty()) {
				DBHelper.getInstance().deleteCategories(false);
				DBHelper.getInstance().insertCategories(categories);

				categoriesChanged = System.currentTimeMillis();
				notifyListeners();
			}

			return categories;
		}
		return null;
	}

	// *** STATUS *******************************************************************

	public void setArticleRead(Set<Integer> ids, int status) {
		boolean erg = false;
		if (Utils.isConnected(cm))
			erg = Controller.getInstance().getConnector().setArticleRead(ids, status);
		if (!erg)
			DBHelper.getInstance().markUnsynchronizedStates(ids, DBHelper.MARK_READ, status);
	}

	public void setArticleStarred(int articleId, int status) {
		boolean erg = false;
		Set<Integer> ids = new HashSet<>();
		ids.add(articleId);

		if (Utils.isConnected(cm))
			erg = Controller.getInstance().getConnector().setArticleStarred(ids, status);
		if (!erg)
			DBHelper.getInstance().markUnsynchronizedStates(ids, DBHelper.MARK_STAR, status);
	}

	public void setArticlePublished(int articleId, int status) {
		boolean erg = false;
		Set<Integer> ids = new HashSet<>();
		ids.add(articleId);

		if (Utils.isConnected(cm))
			erg = Controller.getInstance().getConnector().setArticlePublished(ids, status);
		if (!erg)
			DBHelper.getInstance().markUnsynchronizedStates(ids, DBHelper.MARK_PUBLISH, status);
	}

	public void setArticleNote(int articleId, String note) {
		boolean erg = false;
		Map<Integer, String> ids = new HashMap<>();
		ids.put(articleId, note);

		if (Utils.isConnected(cm))
			erg = Controller.getInstance().getConnector().setArticleNote(ids);
		if (!erg)
			DBHelper.getInstance().markUnsynchronizedNotes(ids);
	}

	/**
	 * mark all articles in given category/feed as read
	 *
	 * @param id         category/feed ID
	 * @param isCategory if set to {@code true}, then given id is category
	 *                   ID, otherwise - feed ID
	 */
	public void setRead(int id, boolean isCategory) {
		Collection<Integer> markedArticleIds = DBHelper.getInstance().markRead(id, isCategory);
		if (markedArticleIds != null) {
			boolean isSync = false;
			if (Utils.isConnected(cm))
				isSync = Controller.getInstance().getConnector().setRead(id, isCategory);
			if (!isSync)
				DBHelper.getInstance().markUnsynchronizedStates(markedArticleIds, DBHelper.MARK_READ, 0);
		}

	}

	public boolean shareToPublished(String title, String url, String content) {
		return Utils.isConnected(cm) && Controller.getInstance().getConnector().shareToPublished(title, url, content);
	}

	public JSONConnector.SubscriptionResponse feedSubscribe(String feed_url, int category_id) {
		if (Utils.isConnected(cm))
			return Controller.getInstance().getConnector().feedSubscribe(feed_url, category_id);
		return null;
	}

	public boolean feedUnsubscribe(int feed_id) {
		return Utils.isConnected(cm) && Controller.getInstance().getConnector().feedUnsubscribe(feed_id);
	}

	String getPref(String pref) {
		if (Utils.isConnected(cm))
			return Controller.getInstance().getConnector().getPref(pref);
		return null;
	}

	public Set<Label> getLabels(int articleId) {
		return DBHelper.getInstance().getLabelsForArticle(articleId);
	}

	public boolean setLabel(Integer articleId, Label label) {
		Set<Integer> set = new HashSet<>();
		set.add(articleId);
		return setLabel(set, label);
	}

	private boolean setLabel(Set<Integer> articleIds, Label label) {

		DBHelper.getInstance().insertLabels(articleIds, label, label.checked);
		notifyListeners();

		boolean erg = false;
		if (Utils.isConnected(cm)) {
			Log.d(TAG, "Calling connector with Label: " + label + ") and ids.size() " + articleIds.size());
			erg = Controller.getInstance().getConnector().setArticleLabel(articleIds, label.id, label.checked);
		}
		return erg;
	}

	/**
	 * syncronize read, starred, published articles and notes with server
	 */
	public void synchronizeStatus() {
		if (!Utils.isConnected(cm))
			return;
		long time = System.currentTimeMillis();

		// Try to send all marked articles to the server, every synced status is removed from the DB afterwards
		String[] marks = new String[]{DBHelper.MARK_READ, DBHelper.MARK_STAR, DBHelper.MARK_PUBLISH};
		for (String mark : marks) {
			Set<Integer> idsMark = DBHelper.getInstance().getMarked(mark, 1);
			Set<Integer> idsUnmark = DBHelper.getInstance().getMarked(mark, 0);

			if (DBHelper.MARK_READ.equals(mark)) {
				if (Controller.getInstance().getConnector().setArticleRead(idsMark, 1))
					DBHelper.getInstance().setMarked(idsMark, mark);

				if (Controller.getInstance().getConnector().setArticleRead(idsUnmark, 0))
					DBHelper.getInstance().setMarked(idsUnmark, mark);
			}
			if (DBHelper.MARK_STAR.equals(mark)) {
				if (Controller.getInstance().getConnector().setArticleStarred(idsMark, 1))
					DBHelper.getInstance().setMarked(idsMark, mark);

				if (Controller.getInstance().getConnector().setArticleStarred(idsUnmark, 0))
					DBHelper.getInstance().setMarked(idsUnmark, mark);
			}
			if (DBHelper.MARK_PUBLISH.equals(mark)) {
				if (Controller.getInstance().getConnector().setArticlePublished(idsMark, 1))
					DBHelper.getInstance().setMarked(idsMark, mark);

				if (Controller.getInstance().getConnector().setArticlePublished(idsUnmark, 0))
					DBHelper.getInstance().setMarked(idsUnmark, mark);
			}
		}

		// Try to send all article notes to the server, on success they are deleted from the DB
		Map<Integer, String> notesMarked = DBHelper.getInstance().getMarkedNotes();
		if (notesMarked.size() > 0) {
			if (Controller.getInstance().getConnector().setArticleNote(notesMarked))
				DBHelper.getInstance().setMarkedNotes(notesMarked);
		}

		Log.d(TAG, String.format("Syncing Status took %sms", (System.currentTimeMillis() - time)));
	}

	public void purgeOrphanedArticles() {
		if (Controller.getInstance().getLastCleanup() > System.currentTimeMillis() - Utils.CLEANUP_TIME)
			return;

		DBHelper.getInstance().purgeOrphanedArticles();
		Controller.getInstance().setLastCleanup(System.currentTimeMillis());
	}

	public void calculateCounters() {
		DBHelper.getInstance().calculateCounters();
	}

	public void notifyListeners() {
		if (!Controller.getInstance().isHeadless())
			UpdateController.getInstance().notifyListeners();
	}

	public boolean isConnected() {
		return Utils.isConnected(cm);
	}

	/**
	 * Deletes all database references on remotefiles and clears the cache folder.
	 */
	public void deleteAllRemoteFiles() {
		int count = DBHelper.getInstance().deleteAllRemoteFiles();
		Log.w(TAG, String.format("Deleted %s Remotefiles from database.", count));

		ImageCache cache = Controller.getInstance().getImageCache();
		if (cache != null && cache.deleteAllCachedFiles())
			Log.d(TAG, "Deleting cached files was successful.");
		else
			Log.e(TAG, "Deleting cached files failed at least partially, there were errors!");
	}

}
