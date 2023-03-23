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

package org.ttrssreader.net;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;

import org.json.JSONException;
import org.json.JSONObject;
import org.ttrssreader.MyApplication;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.pojos.Label;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.SSLUtils;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;

import java.io.IOException;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLPeerUnverifiedException;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class JSONConnector {

	private static final String TAG = JSONConnector.class.getSimpleName();

	private static String lastError = "";
	private static boolean hasLastError = false;

	private static final String PARAM_OP = "op";
	private static final String PARAM_USER = "user";
	private static final String PARAM_PW = "password";
	private static final String PARAM_CAT_ID = "cat_id";
	private static final String PARAM_CATEGORY_ID = "category_id";
	private static final String PARAM_FEED_ID = "feed_id";
	private static final String PARAM_FEED_URL = "feed_url";
	private static final String PARAM_ARTICLE_IDS = "article_ids";
	private static final String PARAM_LIMIT = "limit";
	private static final int PARAM_LIMIT_API_5 = 60;
	private static final String PARAM_VIEWMODE = "view_mode";
	private static final String PARAM_SHOW_CONTENT = "show_content";
	// include_attachments available since 1.5.3 but is ignored on older versions
	private static final String PARAM_INC_ATTACHMENTS = "include_attachments";
	private static final String PARAM_SINCE_ID = "since_id";
	private static final String PARAM_SEARCH = "search";
	private static final String PARAM_SKIP = "skip";
	private static final String PARAM_MODE = "mode";
	// 0-starred, 1-published, 2-unread, 3-article note (since api level 1)
	private static final String PARAM_FIELD = "field";
	// optional data parameter when setting note field
	private static final String PARAM_DATA = "data";
	private static final String PARAM_IS_CAT = "is_cat";
	private static final String PARAM_PREF = "pref_name";

	private static final String VALUE_LOGIN = "login";
	//private static final String VALUE_IS_LOGGEDIN = "isLoggedIn";
	private static final String VALUE_GET_CATEGORIES = "getCategories";
	private static final String VALUE_GET_FEEDS = "getFeeds";
	private static final String VALUE_GET_HEADLINES = "getHeadlines";
	private static final String VALUE_UPDATE_ARTICLE = "updateArticle";
	private static final String VALUE_CATCHUP = "catchupFeed";
	private static final String VALUE_UPDATE_FEED = "updateFeed";
	private static final String VALUE_GET_PREF = "getPref";
	private static final String VALUE_SET_LABELS = "setArticleLabel";
	private static final String VALUE_SHARE_TO_PUBLISHED = "shareToPublished";
	private static final String VALUE_FEED_SUBSCRIBE = "subscribeToFeed";
	private static final String VALUE_FEED_UNSUBSCRIBE = "unsubscribeFeed";

	private static final String VALUE_LABEL_ID = "label_id";
	private static final String VALUE_ASSIGN = "assign";

	private static final String ERROR = "error";
	private static final String LOGIN_ERROR = "LOGIN_ERROR";
	private static final String NOT_LOGGED_IN = "NOT_LOGGED_IN";
	private static final String UNKNOWN_METHOD = "UNKNOWN_METHOD";
	private static final String API_DISABLED = "API_DISABLED";
	private static final String INCORRECT_USAGE = "INCORRECT_USAGE";

	private static final String STATUS = "status";
	private static final String API_LEVEL = "api_level";

	// session id as an OUT parameter
	private static final String SESSION_ID = "session_id";
	private static final String ID = "id";

	private static final String TITLE = "title";
	private static final String UNREAD = "unread";

	private static final String CAT_ID = "cat_id";

	private static final String CONTENT = "content";

	private static final String URL_SHARE = "url";
	private static final String FEED_URL = "feed_url";

	private static final String CONTENT_URL = "content_url";

	private static final String VALUE = "value";

	private static final int MAX_ID_LIST_LENGTH = 100;
	private static final MediaType MEDIATYPE_JSON = MediaType.parse("application/json; charset=utf-8");

	// session id as an IN parameter
	private static final String SID = "sid";

	private String sessionId = null;

	private final Object loginLock = new Object();
	private int apiLevel = -1;
	private String currentSettings = "";

	public static final int PARAM_LIMIT_MAX_VALUE = 200;

	private OkHttpClient client;

	public JSONConnector() {
		initHttpClient();
	}

	/**
	 * TODO: Probably it would be better to listen for change events on preferences directly but this was the quick and easy way.
	 * <p>
	 * The following preferences are checked for changes:
	 * - useProxy
	 * - proxyHost
	 * - proxyPort
	 * - lazyServer
	 * <p>
	 * If any of these change we need to re-initialize the OkHttpClient, otherwise we can re-use the existing instance, shaving off some ms on every API request.
	 */
	private void initHttpClient() {
		// Just build a string out of all settings involved here, then check against the old string to see if anything changed in between:
		String newSettings = Controller.getInstance().useProxy() + Controller.getInstance().proxyHost() + Controller.getInstance().proxyPort() + Controller.getInstance().lazyServer();

		if (!currentSettings.equals(newSettings)) {
			// Set longer timeouts for lazy loading servers
			TimeUnit timeoutUnit = Controller.getInstance().lazyServer() ? TimeUnit.MINUTES : TimeUnit.SECONDS;

			// Build Client-Object:
			OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
			if (SSLUtils.factory != null) {
				clientBuilder.sslSocketFactory(SSLUtils.factory);
			}
			clientBuilder.proxy(getProxy());
			clientBuilder.readTimeout(10, timeoutUnit);
			this.client = clientBuilder.build();

			// Store new settings
			this.currentSettings = newSettings;
		}
	}

	private Reader doRequest(Map<String, String> params) {
		try {
			if (sessionId != null)
				params.put(SID, sessionId);

			JSONObject json = new JSONObject(params);
			logRequest(json);

			// Build Request-Object:
			Request.Builder reqBuilder = new Request.Builder();
			reqBuilder.url(Controller.getInstance().hostname());
			reqBuilder.post(RequestBody.create(MEDIATYPE_JSON, json.toString()));

			// HTTP-Basic Authentication
			if (Controller.getInstance().useHttpAuth()) {
				String user = Controller.getInstance().httpUsername();
				String pw = Controller.getInstance().httpPassword();
				reqBuilder.addHeader("Authorization", Credentials.basic(user, pw));
			}

			Request request = reqBuilder.build();

			// Call Server:
			Response response = this.client.newCall(request).execute();

			// Check for HTTP Status codes:
			int code = response.code();
			if (!response.isSuccessful() || code >= 400 && code < 600) {
				hasLastError = true;
				lastError = "Server returned status: " + code + " (Message: " + response.message() + ")";
				return null;
			}

			// Read Response as stream:
			ResponseBody body = response.body();
			return body != null ? body.charStream() : null;

		} catch (JsonSyntaxException e) {
			hasLastError = true;
			lastError = "JsonSyntaxException (Invalid JSON Data) in doRequest(): " + formatException(e);
		} catch (SSLPeerUnverifiedException e) {
			hasLastError = true;
			lastError = "SSLPeerUnverifiedException in doRequest(): " + formatException(e);
		} catch (Exception e) {
			hasLastError = true;
			lastError = "Exception in doRequest(): " + formatException(e);
		}
		return null;
	}

	private Proxy getProxy() {
		if (Controller.getInstance().useProxy()) {
			String host = Controller.getInstance().proxyHost();
			if (!Constants.EMPTY.equals(host)) {
				int port = Controller.getInstance().proxyPort();
				return new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));
			}
		}
		return Proxy.NO_PROXY;
	}

	public void init() {
		// Empty
	}

	private void logRequest(final JSONObject json) throws JSONException {
		// Filter password and session-id
		Object paramPw = json.remove(PARAM_PW);
		Object paramSID = json.remove(SID);
		Log.i(TAG, json.toString());
		json.put(PARAM_PW, paramPw);
		json.put(SID, paramSID);
	}

	private String readResult(Map<String, String> params) throws IOException {
		return readResult(params, false, true);
	}

	private String readResult(Map<String, String> params, boolean login, boolean retry) throws IOException {

		/*
		Response response = doRequest(params);
		if (response == null)
			return null;

		ResponseBody responseBody = response.body();
		if (responseBody == null)
			return null;

		try {
			String body = responseBody.string();
			JsonElement result = JsonParser.parseString(body);
			JsonObject resultObj = result.getAsJsonObject();

			// TODO...

		} finally {
			responseBody.close();
		}
		*/

		String ret = "";
		Reader reader = doRequest(params);
		if (reader == null)
			return null;

		// Check if content contains array or object, array indicates login-response or error, object is content
		try (JsonReader json = new JsonReader(reader)) {
			json.beginObject();

			while (json.hasNext()) {
				String name = json.nextName();
				if (!name.equals("content")) {
					json.skipValue();
					continue;
				}

				JsonToken t = json.peek();
				if (!t.equals(JsonToken.BEGIN_OBJECT))
					continue;

				JsonObject object = new JsonObject();
				json.beginObject();
				while (json.hasNext()) {
					String currentName = json.nextName();
					if (currentName.equals("config"))
						readConfig(json);
					else
						object.addProperty(currentName, json.nextString());
				}
				json.endObject();

				if (object.get(SESSION_ID) != null) {
					ret = object.get(SESSION_ID).getAsString();
				}
				if (object.get(STATUS) != null) {
					ret = object.get(STATUS).getAsString();
				}
				if (this.apiLevel == -1 && object.get(API_LEVEL) != null) {
					this.apiLevel = object.get(API_LEVEL).getAsInt();
				}
				if (object.get(VALUE) != null) {
					ret = object.get(VALUE).getAsString();
				}
				if (object.get(ERROR) != null) {
					String message = object.get(ERROR).getAsString();
					Context ctx = MyApplication.context();

					switch (message) {
						case API_DISABLED:
							lastError = ctx.getString(R.string.Error_ApiDisabled, Controller.getInstance().username());
							break;
						case NOT_LOGGED_IN:
						case LOGIN_ERROR:
							if (!login && retry && internalLogin())
								return readResult(params, false, false); // Just do the same request again
							else
								lastError = ctx.getString(R.string.Error_LoginFailed);
							break;
						case INCORRECT_USAGE:
							lastError = ctx.getString(R.string.Error_ApiIncorrectUsage);
							break;
						case UNKNOWN_METHOD:
							lastError = ctx.getString(R.string.Error_ApiUnknownMethod);
							break;
						default:
							lastError = ctx.getString(R.string.Error_ApiUnknownError);
							break;
					}

					hasLastError = true;
					Log.e(TAG, message);
					return null;
				}

			}
		}

		if (ret.startsWith("\""))
			ret = ret.substring(1);
		if (ret.endsWith("\""))
			ret = ret.substring(0, ret.length() - 1);

		return ret;
	}

	private void readConfig(JsonReader json) throws IOException {
		while (json.hasNext()) {
			if (json.peek() == JsonToken.BEGIN_OBJECT)
				json.beginObject();

			switch (json.nextName()) {
				case "icons_dir":
					Controller.serverConfigIconsDir = json.nextString();
					break;
				case "icons_url":
					Controller.serverConfigIconsUrl = json.nextString();
					break;
				case "daemon_is_running":
					Controller.serverConfigDaemonIsRunning = json.nextBoolean();
					break;
				case "custom_sort_types":
					//Controller.serverConfigCustomSortTypes = json.next...
					//Too lazy, don't need this anyway...
					json.skipValue();
					break;
				case "num_feeds":
					Controller.serverConfigNumFeeds = json.nextInt();
					break;
				default:
					json.skipValue();
			}

			if (json.peek() == JsonToken.END_OBJECT) {
				json.endObject();
				break; // End of config object
			}
		}
	}

	private JsonReader prepareReader(Map<String, String> params) throws IOException {
		return prepareReader(params, true);
	}

	private JsonReader prepareReader(Map<String, String> params, boolean firstCall) throws IOException {
		Reader in = doRequest(params);
		if (in == null)
			return null;

		// Check if content contains array or object, array indicates login-response or error, object is content
		JsonReader reader = new JsonReader(in);
		reader.beginObject();

		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("content")) {
				JsonToken t = reader.peek();

				if (t.equals(JsonToken.BEGIN_ARRAY)) {
					return reader;
				} else if (t.equals(JsonToken.BEGIN_OBJECT)) {

					JsonObject object = new JsonObject();
					reader.beginObject();

					String nextName = reader.nextName();
					// We have a BEGIN_OBJECT here but its just the response to call "subscribeToFeed" or "isLoggedIn"
					if ("status".equals(nextName))
						return reader;

					// Handle error
					while (reader.hasNext()) {
						if (nextName != null) {
							object.addProperty(nextName, reader.nextString());
							nextName = null;
						} else {
							object.addProperty(reader.nextName(), reader.nextString());
						}
					}
					reader.endObject();

					if (object.get(ERROR) != null) {
						String message = object.get(ERROR).toString();

						if (message.contains(NOT_LOGGED_IN)) {
							lastError = NOT_LOGGED_IN;
							if (firstCall && internalLogin() && !hasLastError)
								return prepareReader(params, false); // Just do the same request again
							else
								return null;
						}

						if (message.contains(API_DISABLED)) {
							hasLastError = true;
							lastError = MyApplication.context().getString(R.string.Error_ApiDisabled, Controller.getInstance().username());
							return null;
						}

						// Any other error
						hasLastError = true;
						lastError = message;
					}
				}

			} else {
				reader.skipValue();
			}
		}
		return null;
	}

	private boolean sessionNotAlive() {
		// Make sure we are logged in
		if (sessionId == null || lastError.equals(NOT_LOGGED_IN))
			if (!internalLogin())
				return true;
		return hasLastError;
	}

	/**
	 * Does an API-Call and ignores the result.
	 *
	 * @return true if the call was successful.
	 */
	private boolean doRequestNoAnswer(Map<String, String> params) {
		if (sessionNotAlive())
			return false;

		try {
			String result = readResult(params);

			// Reset error, this is only for an api-bug which returns an empty result for updateFeed
			if (result == null)
				pullLastError();

			return "OK".equals(result);
		} catch (MalformedJsonException mje) {
			// Reset error, this is only for an api-bug which returns an empty result for updateFeed
			pullLastError();
		} catch (IOException e) {
			e.printStackTrace();
			if (!hasLastError) {
				hasLastError = true;
				lastError = formatException(e);
			}
		}

		return false;
	}

	/**
	 * At this time, we can only offer a best guess: if http authentication is on, and
	 * the user has no tt-rss username and password, then it's likely.
	 * In the future, it may be a better option to have an explicit setting for this.
	 * The problem with that is that many people may not know what single-user mode is,
	 * as it can only be set in server config files.
	 *
	 * @return true if the configured tt-rss instance is configured for single user
	 * (ie. no username, no password, no user management)
	 */
	private boolean isSingleUser() {
		return Controller.getInstance().useHttpAuth() && StringSupport.isEmpty(Controller.getInstance().username()) && StringSupport.isEmpty(Controller.getInstance().password());
	}

	/**
	 * Tries to login to the ttrss-server with the password.
	 *
	 * @return true on success, false otherwise
	 */
	private boolean internalLogin() {
		long time = System.currentTimeMillis();

		// Just login once, check if already logged in after acquiring the lock on mSessionId
		if (sessionId != null && !lastError.equals(NOT_LOGGED_IN))
			return true;

		synchronized (loginLock) {
			if (sessionId != null && !lastError.equals(NOT_LOGGED_IN))
				return true; // Login done while we were waiting for the lock

			Map<String, String> params = new HashMap<>();
			params.put(PARAM_OP, VALUE_LOGIN);

			if (!isSingleUser()) {
				params.put(PARAM_USER, Controller.getInstance().username());
				params.put(PARAM_PW, Controller.getInstance().password());
			}

			try {
				sessionId = readResult(params, true, false);
				if (sessionId != null) {
					Log.d(TAG, "internalLogin: " + (System.currentTimeMillis() - time) + "ms");
					return true;
				}
			} catch (IOException e) {
				if (!hasLastError) {
					hasLastError = true;
					lastError = formatException(e);
				}
			}

			if (!hasLastError) {
				// Login didnt succeed, write message
				hasLastError = true;
				lastError = MyApplication.context().getString(R.string.Error_NotLoggedIn);
			}
			return false;
		}
	}

	// ***************** Helper-Methods **************************************************

	private Set<String> parseAttachments(JsonReader reader) throws IOException {
		Set<String> ret = new HashSet<>();
		reader.beginArray();
		while (reader.hasNext()) {

			String attId = null;
			String attUrl = null;

			reader.beginObject();
			while (reader.hasNext()) {

				try {
					switch (reader.nextName()) {
						case CONTENT_URL:
							attUrl = reader.nextString();
							// Some URLs may start with // to indicate that both, http and https can be used
							if (attUrl.startsWith("//"))
								attUrl = "https:" + attUrl;
							break;
						case ID:
							attId = reader.nextString();
							break;
						default:
							reader.skipValue();
							break;
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					reader.skipValue();
				}

			}
			reader.endObject();

			if (attId != null && attUrl != null)
				ret.add(attUrl);
		}
		reader.endArray();
		return ret;
	}

	/**
	 * parse articles from JSON-reader
	 *
	 * @param articles container, where parsed articles will be stored
	 * @param reader   JSON-reader, containing articles (received from server)
	 * @param filter   filter for articles, defining which articles should be omitted while parsing (may be {@code
	 *                 null})
	 * @return amount of processed articles
	 */
	private int parseArticleArray(final Set<Article> articles, JsonReader reader, IArticleOmitter filter) {
		int count = 0;

		try {
			reader.beginArray();
			while (reader.hasNext()) {
				Article article = new Article();

				reader.beginObject();
				boolean skipObject = parseArticle(article, reader, filter);
				reader.endObject();

				if (!skipObject && article.id != -1 && article.title != null)
					articles.add(article);

				count++;
			}
			reader.endArray();
		} catch (OutOfMemoryError e) {
			Controller.getInstance().lowMemory(true); // Low memory detected
		} catch (Exception e) {
			Log.e(TAG, "Input data could not be read: " + e.getMessage() + " (" + e.getCause() + ")", e);
		}

		return count;
	}

	private boolean parseArticle(final Article a, final JsonReader reader, final IArticleOmitter filter) throws IOException {

		boolean skipObject = false;
		while (reader.hasNext() && reader.peek().equals(JsonToken.NAME)) {
			if (skipObject) {
				// field name
				reader.skipValue();
				// field value
				reader.skipValue();
				continue;
			}

			String name = reader.nextName();

			try {
				Article.ArticleField field = Article.ArticleField.valueOf(name);

				switch (field) {
					case id:
						a.id = reader.nextInt();
						break;
					case guid:
						a.guid = reader.nextString();
						break;
					case title:
						a.title = reader.nextString();
						break;
					case unread:
						a.isUnread = reader.nextBoolean();
						break;
					case updated:
						a.updated = new Date(reader.nextLong() * 1000);
						break;
					case feed_id:
						if (reader.peek() == JsonToken.NULL)
							reader.nextNull();
						else
							a.feedId = reader.nextInt();
						break;
					case content:
						a.content = reader.nextString().replaceAll("(<(?:img|video)[^>]+?src=[\"'])//([^\"']*)", "$1https://$2");
						break;
					case link:
						a.url = reader.nextString();
						// Some URLs may start with // to indicate that both, http and https can be used
						if (a.url.startsWith("//"))
							a.url = "https:".concat(a.url);
						break;
					case comments:
						a.commentUrl = reader.nextString();
						// Some URLs may start with // to indicate that both, http and https can be used
						if (a.commentUrl.startsWith("//"))
							a.commentUrl = "https:".concat(a.commentUrl);
						break;
					case attachments:
						a.attachments = parseAttachments(reader);
						break;
					case marked:
						a.isStarred = reader.nextBoolean();
						break;
					case published:
						a.isPublished = reader.nextBoolean();
						break;
					case labels:
						a.labels = parseLabels(reader);
						break;
					case author:
						a.author = reader.nextString();
						break;
					case note:
						if (reader.peek() == JsonToken.NULL)
							reader.nextNull();
						else
							a.note = reader.nextString();
						break;
					case score:
						a.score = reader.nextInt();
						break;
					default:
						//Log.d(TAG, "Skipped field " + field);
						reader.skipValue();
						continue;
				}

				if (filter != null)
					skipObject = filter.omitArticle(field, a);

			} catch (IllegalArgumentException | IOException e) {
				Log.w(TAG, "Result contained illegal value for entry \"" + name + "\".");
				reader.skipValue();
			}
		}
		return skipObject;
	}

	private Set<Label> parseLabels(final JsonReader reader) throws IOException {
		Set<Label> ret = new HashSet<>();

		if (reader.peek().equals(JsonToken.BEGIN_ARRAY)) {
			reader.beginArray();
		} else {
			reader.skipValue();
			return ret;
		}

		try {
			while (reader.hasNext()) {

				Label label = new Label();
				reader.beginArray();
				try {
					label.id = Integer.parseInt(reader.nextString());
					label.caption = reader.nextString();
					label.foregroundColor = reader.nextString();
					label.backgroundColor = reader.nextString();
					label.checked = true;
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					reader.skipValue();
					continue;
				}
				ret.add(label);
				reader.endArray();
			}
			reader.endArray();
		} catch (Exception e) {
			// Ignore exceptions here
			try {
				if (reader.peek().equals(JsonToken.END_ARRAY))
					reader.endArray();
			} catch (Exception ee) {
				// Empty!
			}
		}

		return ret;
	}

	// ***************** Retrieve-Data-Methods **************************************************

	/**
	 * Retrieves all categories.
	 *
	 * @return a list of categories.
	 */
	public Set<Category> getCategories() {
		long time = System.currentTimeMillis();
		Set<Category> ret = new LinkedHashSet<>();
		if (sessionNotAlive())
			return ret;

		Map<String, String> params = new HashMap<>();
		params.put(PARAM_OP, VALUE_GET_CATEGORIES);

		try (JsonReader reader = prepareReader(params)) {
			if (reader == null)
				return ret;

			reader.beginArray();
			while (reader.hasNext()) {

				int id = -1;
				String title = null;
				int unread = 0;

				reader.beginObject();
				while (reader.hasNext()) {

					try {
						switch (reader.nextName()) {
							case ID:
								id = reader.nextInt();
								break;
							case TITLE:
								title = reader.nextString();
								break;
							case UNREAD:
								unread = reader.nextInt();
								break;
							default:
								reader.skipValue();
								break;
						}
					} catch (IllegalArgumentException | IllegalStateException | IOException e) {
						e.printStackTrace();
						reader.skipValue();
						// Not sure when this actually happens, maybe we should also break; out here...
					}
				}
				reader.endObject();

				// Don't handle categories with an id below 1, we already have them in the DB from
				// Data.updateVirtualCategories()
				if (id > 0 && title != null)
					ret.add(new Category(id, title, unread));
			}
			reader.endArray();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.d(TAG, "getCategories: " + (System.currentTimeMillis() - time) + "ms");
		return ret;
	}

	/**
	 * get current feeds from server
	 *
	 * @param tolerateWrongUnreadInformation if set to {@code false}, then
	 *                                       lazy server will be updated before
	 * @return set of actual feeds on server
	 */
	private Set<Feed> getFeeds(boolean tolerateWrongUnreadInformation) {
		long time = System.currentTimeMillis();
		Set<Feed> ret = new LinkedHashSet<>();
		if (sessionNotAlive())
			return ret;

		if (!tolerateWrongUnreadInformation) {
			makeLazyServerWork();
		}

		Map<String, String> params = new HashMap<>();
		params.put(PARAM_OP, VALUE_GET_FEEDS);
		params.put(PARAM_CAT_ID, Data.VCAT_ALL + ""); // Hardcoded -4 fetches all feeds. See
		// http://tt-rss.org/redmine/wiki/tt-rss/JsonApiReference#getFeeds

		try (JsonReader reader = prepareReader(params)) {
			if (reader == null)
				return ret;

			reader.beginArray();
			while (reader.hasNext()) {

				int categoryId = -1;
				int id = 0;
				String title = null;
				String feedUrl = null;
				int unread = 0;

				reader.beginObject();
				while (reader.hasNext()) {

					try {
						switch (reader.nextName()) {
							case ID:
								id = reader.nextInt();
								break;
							case CAT_ID:
								categoryId = reader.nextInt();
								break;
							case TITLE:
								title = reader.nextString();
								break;
							case FEED_URL:
								feedUrl = reader.nextString();
								break;
							case UNREAD:
								unread = reader.nextInt();
								break;
							default:
								reader.skipValue();
								break;
						}
					} catch (IllegalArgumentException e) {
						Log.e(TAG, (e.getMessage() != null ? e.getMessage() : "no exception message available"));
						reader.skipValue();
					}

				}
				reader.endObject();

				if (id != -1 || categoryId == -2) { // normal feed (>0) or label (-2)
					if (title != null) {
						Feed f = new Feed();
						f.id = id;
						f.categoryId = categoryId;
						f.title = title;
						f.url = feedUrl;
						f.unread = unread;
						ret.add(f);
					}
				}

			}
			reader.endArray();
		} catch (IOException e) {
			Log.e(TAG, (e.getMessage() != null ? e.getMessage() : "no exception message available"));
		}

		Log.d(TAG, "getFeeds: " + (System.currentTimeMillis() - time) + "ms");
		return ret;
	}

	/**
	 * Retrieves all feeds from server.
	 *
	 * @return a set of all feeds on server.
	 */
	public Set<Feed> getFeeds() {
		return getFeeds(false);
	}

	private void makeLazyServerWork(Integer feedId) {
		if (Controller.getInstance().lazyServer()) {
			Map<String, String> taskParams = new HashMap<>();
			taskParams.put(PARAM_OP, VALUE_UPDATE_FEED);
			taskParams.put(PARAM_FEED_ID, String.valueOf(feedId));
			doRequestNoAnswer(taskParams);
		}
	}

	private long noTaskUntil = 0;
	final static private long minTaskIntervall = 10 * Utils.MINUTE;

	private void makeLazyServerWork() {
		final long time = System.currentTimeMillis();
		if (Controller.getInstance().lazyServer() && (noTaskUntil < time)) {
			noTaskUntil = time + minTaskIntervall;
			for (Feed feed : getFeeds(true)) {
				makeLazyServerWork(feed.id);
			}
		}
	}

	/**
	 * Retrieves the specified articles.
	 *
	 * @param articles   container for retrieved articles
	 * @param id         the id of the feed/category
	 * @param limit      the maximum number of articles to be fetched
	 * @param viewMode   indicates wether only unread articles should be included (Possible values: all_articles,
	 *                   unread,
	 *                   adaptive, marked, updated)
	 * @param isCategory indicates if we are dealing with a category or a feed
	 * @param sinceId    the first ArticleId which is to be retrieved.
	 * @param search     search query
	 * @param filter     filter for articles, defining which articles should be omitted while parsing (may be
	 *                   {@code
	 *                   null})
	 */
	public void getHeadlines(final Set<Article> articles, Integer id, int limit, String viewMode, boolean isCategory, Integer sinceId, String search, IArticleOmitter filter) {
		long time = System.currentTimeMillis();
		int offset = 0;
		int count;
		int maxSize = articles.size() + limit;

		if (sessionNotAlive())
			return;

		int limitParam = Math.min((apiLevel < 6) ? PARAM_LIMIT_API_5 : PARAM_LIMIT_MAX_VALUE, limit);

		makeLazyServerWork(id);

		while (articles.size() < maxSize) {

			Map<String, String> params = new HashMap<>();
			params.put(PARAM_OP, VALUE_GET_HEADLINES);
			params.put(PARAM_FEED_ID, id + "");
			params.put(PARAM_LIMIT, limitParam + "");
			params.put(PARAM_SKIP, offset + "");
			params.put(PARAM_VIEWMODE, viewMode);
			params.put(PARAM_IS_CAT, (isCategory ? "1" : "0"));
			params.put(PARAM_SHOW_CONTENT, "1");
			params.put(PARAM_INC_ATTACHMENTS, "1");
			if (sinceId > 0)
				params.put(PARAM_SINCE_ID, sinceId + "");
			if (search != null)
				params.put(PARAM_SEARCH, search);

			try (JsonReader reader = prepareReader(params)) {

				if (hasLastError)
					return;
				if (reader == null)
					continue;

				count = parseArticleArray(articles, reader, filter);

				if (count < limitParam)
					break;
				else
					offset += count;

			} catch (IOException e) {
				e.printStackTrace();
			}
			// Empty!
		}

		Log.d(TAG, "getHeadlines: " + (System.currentTimeMillis() - time) + "ms");
	}

	/**
	 * Marks the given list of article-Ids as read/unread depending on int articleState.
	 *
	 * @param articlesIds  a list of article-ids.
	 * @param articleState the new state of the article (0 -> mark as read; 1 -> mark as unread).
	 */
	public boolean setArticleRead(Set<Integer> articlesIds, int articleState) {
		boolean ret = true;
		if (articlesIds.isEmpty())
			return true;

		for (String idList : StringSupport.convertListToString(articlesIds, MAX_ID_LIST_LENGTH)) {
			Map<String, String> params = new HashMap<>();
			params.put(PARAM_OP, VALUE_UPDATE_ARTICLE);
			params.put(PARAM_ARTICLE_IDS, idList);
			params.put(PARAM_MODE, articleState + "");
			params.put(PARAM_FIELD, "2");
			ret = ret && doRequestNoAnswer(params);
		}
		return ret;
	}

	/**
	 * Marks the given Article as "starred"/"not starred" depending on int articleState.
	 *
	 * @param ids          a list of article-ids.
	 * @param articleState the new state of the article (0 -> not starred; 1 -> starred; 2 -> toggle).
	 * @return true if the operation succeeded.
	 */
	public boolean setArticleStarred(Set<Integer> ids, int articleState) {
		boolean ret = true;
		if (ids.size() == 0)
			return true;

		for (String idList : StringSupport.convertListToString(ids, MAX_ID_LIST_LENGTH)) {
			Map<String, String> params = new HashMap<>();
			params.put(PARAM_OP, VALUE_UPDATE_ARTICLE);
			params.put(PARAM_ARTICLE_IDS, idList);
			params.put(PARAM_MODE, articleState + "");
			params.put(PARAM_FIELD, "0");
			ret = ret && doRequestNoAnswer(params);
		}
		return ret;
	}

	/**
	 * Marks a feed or a category with all its feeds as read.
	 *
	 * @param id         the feed-id/category-id.
	 * @param isCategory indicates whether id refers to a feed or a category.
	 * @return true if the operation succeeded.
	 */
	public boolean setRead(int id, boolean isCategory) {
		Map<String, String> params = new HashMap<>();
		params.put(PARAM_OP, VALUE_CATCHUP);
		params.put(PARAM_FEED_ID, id + "");
		params.put(PARAM_IS_CAT, (isCategory ? "1" : "0"));
		params.put(PARAM_MODE, "all"); // indicates range of articles to mark as read - "all", "1day", "1week", "2week".
		return doRequestNoAnswer(params);
	}

	/**
	 * Marks the given Articles as "published"/"not published" depending on articleState.
	 *
	 * @param ids          a list of article-ids.
	 * @param articleState the new state of the articles (0 -> not published; 1 -> published; 2 -> toggle).
	 * @return true if the operation succeeded.
	 */
	public boolean setArticlePublished(Set<Integer> ids, int articleState) {
		if (ids.size() == 0)
			return true;
		boolean ret = true;
		for (String idList : StringSupport.convertListToString(ids, MAX_ID_LIST_LENGTH)) {
			Map<String, String> params = new HashMap<>();
			params.put(PARAM_OP, VALUE_UPDATE_ARTICLE);
			params.put(PARAM_ARTICLE_IDS, idList);
			params.put(PARAM_MODE, articleState + "");
			params.put(PARAM_FIELD, "1");
			ret = ret && doRequestNoAnswer(params);
		}
		return ret;
	}

	/**
	 * Adds a note to the given articles
	 *
	 * @param ids a list of article-ids with corresponding notes (may be null).
	 * @return true if the operation succeeded.
	 */
	public boolean setArticleNote(Map<Integer, String> ids) {
		if (ids.size() == 0)
			return true;
		boolean ret = true;
		for (Integer id : ids.keySet()) {
			String note = ids.get(id);
			if (note == null)
				continue;

			Map<String, String> params = new HashMap<>();
			params.put(PARAM_OP, VALUE_UPDATE_ARTICLE);
			params.put(PARAM_ARTICLE_IDS, id + "");
			params.put(PARAM_FIELD, "3"); // Field 3 is the "Add note" field
			params.put(PARAM_DATA, note);
			ret = ret && doRequestNoAnswer(params);
		}
		return ret;
	}

	public boolean feedUnsubscribe(int feed_id) {
		Map<String, String> params = new HashMap<>();
		params.put(PARAM_OP, VALUE_FEED_UNSUBSCRIBE);
		params.put(PARAM_FEED_ID, feed_id + "");
		return doRequestNoAnswer(params);
	}

	/**
	 * Returns the value for the given preference-name as a string.
	 *
	 * @param pref the preferences name
	 * @return the value of the preference or null if it ist not set or unknown
	 */
	public String getPref(String pref) {
		if (sessionNotAlive())
			return null;

		Map<String, String> params = new HashMap<>();
		params.put(PARAM_OP, VALUE_GET_PREF);
		params.put(PARAM_PREF, pref);

		try {
			return readResult(params);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public boolean setArticleLabel(Set<Integer> articleIds, int labelId, boolean assign) {
		boolean ret = true;
		if (articleIds.size() == 0)
			return true;

		for (String idList : StringSupport.convertListToString(articleIds, MAX_ID_LIST_LENGTH)) {
			Map<String, String> params = new HashMap<>();
			params.put(PARAM_OP, VALUE_SET_LABELS);
			params.put(PARAM_ARTICLE_IDS, idList);
			params.put(VALUE_LABEL_ID, labelId + "");
			params.put(VALUE_ASSIGN, (assign ? "1" : "0"));
			ret = ret && doRequestNoAnswer(params);
		}

		return ret;
	}

	public boolean shareToPublished(String title, String url, String content) {
		Map<String, String> params = new HashMap<>();
		params.put(PARAM_OP, VALUE_SHARE_TO_PUBLISHED);
		params.put(TITLE, title);
		params.put(URL_SHARE, url);
		params.put(CONTENT, content);
		return doRequestNoAnswer(params);
	}

	public static class SubscriptionResponse {
		public int code = -1;
		public String message = null;
	}

	public SubscriptionResponse feedSubscribe(String feed_url, int category_id) {
		SubscriptionResponse ret = new SubscriptionResponse();
		if (sessionNotAlive())
			return ret;

		Map<String, String> params = new HashMap<>();
		params.put(PARAM_OP, VALUE_FEED_SUBSCRIBE);
		params.put(PARAM_FEED_URL, feed_url);
		params.put(PARAM_CATEGORY_ID, category_id + "");

		String code = "";
		String message = null;

		try (JsonReader reader = prepareReader(params)) {
			if (reader == null)
				return ret;

			reader.beginObject();
			while (reader.hasNext()) {
				switch (reader.nextName()) {
					case "code":
						code = reader.nextString();
						break;
					case "message":
						message = reader.nextString();
						break;
					default:
						reader.skipValue();
						break;
				}
			}

			if (!code.contains(UNKNOWN_METHOD)) {
				ret.code = Integer.parseInt(code);
				ret.message = message;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	/**
	 * Returns true if there was an error.
	 *
	 * @return true if there was an error.
	 */
	public boolean hasLastError() {
		return hasLastError;
	}

	/**
	 * Returns the last error-message and resets the error-state of the connector.
	 *
	 * @return a string with the last error-message.
	 */
	public String pullLastError() {
		String ret = lastError;
		lastError = "";
		hasLastError = false;
		return ret;
	}

	/**
	 * Formats an exception message and cause, both only if available in the given exception
	 *
	 * @param e the exception
	 * @return a string representing message and cause of the exception
	 */
	private static String formatException(Exception e) {
		if (e == null)
			return "";
		String msg = e.getMessage() != null ? "Exception-Message: " + e.getMessage() + " " : "No Exception-Message available. ";
		String cause = e.getCause() != null ? "Exception-Cause: " + e.getCause() : "No Exception-Cause available.";
		return msg + cause;
	}

}
