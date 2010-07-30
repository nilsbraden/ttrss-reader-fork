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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.utils.Utils;
import android.util.Log;

public class TTRSSJsonConnector implements ITTRSSConnector {

	private static final String OP_LOGIN = "?op=login&user=%s&password=%s";
	private static final String OP_GET_UNREAD = "?op=getUnread&sid=%s";
	private static final String OP_GET_CATEGORIES = "?op=getCategories&sid=%s";
	private static final String OP_GET_FEEDS = "?op=getFeeds&sid=%s";
	private static final String OP_GET_FEEDHEADLINES = "?op=getHeadlines&sid=%s&feed_id=%s&limit=%s&view_mode=%s";
	private static final String OP_GET_ARTICLES_WITH_CONTENT = "?op=getArticles&sid=%s&feed_id=%s";
	private static final String OP_GET_ARTICLE = "?op=getArticle&sid=%s&article_id=%s";
	private static final String OP_UPDATE_ARTICLE = "?op=updateArticle&sid=%s&article_ids=%s&mode=%s&field=%s";
	private static final String OP_CATCHUP = "?op=catchupFeed&sid=%s&feed_id=%s&is_cat=%s";
	
	private static final String ERROR_NAME = "{\"error\":";
	private static final String SESSION_ID = "session_id";
	private static final String ID_NAME = "id";
	private static final String TITLE_NAME = "title";
	private static final String UNREAD_NAME = "unread";
	private static final String CAT_ID_NAME = "cat_id";
	private static final String FEED_URL_NAME = "feed_url";
	private static final String FEED_ID_NAME = "feed_id";
	private static final String UPDATED_NAME = "updated";
	private static final String CONTENT_NAME = "content";
	private static final String URL_NAME = "articleUrl";
	private static final String COMMENT_URL_NAME = "articleCommentUrl";
	private static final String ATTACHMENTS_NAME = "attachments";
	
	private String mServerUrl;
	private String mUserName;
	private String mPassword;
	
	private String mSessionId;
	
	private String mLastError = "";
	private boolean mHasLastError = false;
	
	public TTRSSJsonConnector(String serverUrl, String userName, String password) {
		mServerUrl = serverUrl;
		mUserName = userName;
		mPassword = password;
		mSessionId = null;
	}
	
	private String doRequest(String url) {
		String strResponse = null;
		
		HttpClient httpclient = new DefaultHttpClient();		 				
		HttpPost httpPost = new HttpPost(url);
 
        // Execute the request
        HttpResponse response;
        
        try {
        	
            response = httpclient.execute(httpPost);
            
            HttpEntity entity = response.getEntity();
            
            if (entity != null) {
            	InputStream instream = entity.getContent();
            	
            	strResponse = Utils.convertStreamToString(instream);
            	
            	if (strResponse.startsWith(ERROR_NAME)) {
            		mHasLastError = true;
            		mLastError = strResponse;
            	}
            }
        } catch (ClientProtocolException e) {
        	mHasLastError = true;
    		mLastError = e.getMessage();
        } catch (IOException e) {
        	mHasLastError = true;
    		mLastError = e.getMessage();
        }
        
        return strResponse;
	}
	
	private JSONArray getJSONResponseAsArray(String url) {
		mHasLastError = false;
		mLastError = "";
		
		JSONArray result = null;
		
		String strResponse = doRequest(url);
		
		try {
			result = new JSONArray(strResponse);
		} catch (JSONException e) {
			mHasLastError = true;
    		mLastError = e.getMessage();
		}
		
		return result;
	}
	
	private TTRSSJsonResult getJSONResponse(String url) {
		
		mHasLastError = false;
		mLastError = "";
		
		TTRSSJsonResult result = null;
		
		String strResponse = doRequest(url);
		
		if (!mHasLastError) {
			try {

				result = new TTRSSJsonResult(strResponse);

			} catch (JSONException e) {
				mHasLastError = true;
				mLastError = e.getMessage();
			}
		}
        
        return result;
	}
	
	private boolean login() {
		boolean result = true;
		mSessionId = null;
		
		String url = mServerUrl + String.format(OP_LOGIN, mUserName, mPassword);
		
		TTRSSJsonResult jsonResult = getJSONResponse(url);
		
		if (!mHasLastError) {
			
			int i = 0;
			boolean stop = false;
			
			try {
				while ((i < jsonResult.getNames().length()) &&
						(!stop)) {

					if (jsonResult.getNames().getString(i).equals(SESSION_ID)) {
						stop = true;
						mSessionId = jsonResult.getValues().getString(i);
					} else {
						i++;
					}

				}
			} catch (JSONException e) {
				result = false;
				mHasLastError = true;
	    		mLastError = e.getMessage();
			}
			
		} else {
			result = false;
		}
		
		return result;
	}

	@Override
	public List<CategoryItem> getCategories() {
		Log.w(Utils.TAG, "-- Called: getCategories()");
		
		List<CategoryItem> finalResult = new ArrayList<CategoryItem>();
		
		if (mSessionId == null) {
			login();
			
			if (mHasLastError) {
				return null;
			}
		}
		
		String url = mServerUrl + String.format(OP_GET_CATEGORIES, mSessionId);
		
		JSONArray jsonResult = getJSONResponseAsArray(url);
			
		JSONObject object;
		
		CategoryItem categoryItem;
		
		String id = null;
		String title = null;
		int unread = 0;
		
		
		try {
			for (int i = 0; i < jsonResult.length(); i++) {
				object = jsonResult.getJSONObject(i);

				JSONArray names = object.names();
				JSONArray values = object.toJSONArray(names);
				
				for (int j = 0; j < names.length(); j++) {
		
					if (names.getString(j).equals(ID_NAME)) {
						id = values.getString(j);
					} else if (names.getString(j).equals(TITLE_NAME)) {
						title = values.getString(j);
					} else  if (names.getString(j).equals(UNREAD_NAME)) {
						unread = values.getInt(j);
					}
			
				}
				categoryItem = new CategoryItem(id,
						title,
						unread);
				
				finalResult.add(categoryItem);
			}
		} catch (JSONException e) {
			mHasLastError = true;
			mLastError = e.getMessage();
		}
		
		Log.w(Utils.TAG, "-- END Called: getCategories()");
		return finalResult;
	}

	@Override
	public List<ArticleItem> getFeedHeadlines(int feedId, int limit, int filter, String viewMode) {
		Log.w(Utils.TAG, "-- Called: getFeedHeadlines(feedId " + feedId + ", limit " + limit + ", filter " + filter + ")");
		
		ArrayList<ArticleItem> finalResult = new ArrayList<ArticleItem>();
		
		if (mSessionId == null) {
			login();
			
			if (mHasLastError) {
				return null;
			}
		}
		
		String url = mServerUrl + String.format(OP_GET_FEEDHEADLINES, mSessionId, feedId, limit, viewMode);
		
		JSONArray jsonResult = getJSONResponseAsArray(url);
		
		JSONObject object;
		ArticleItem articleItem;
		
		try {
			for (int i = 0; i < jsonResult.length(); i++) {
				object = jsonResult.getJSONObject(i);
				
				JSONArray names = object.names();
				JSONArray values = object.toJSONArray(names);
				
				articleItem = parseDataForArticle(names, values);
				
				finalResult.add(articleItem);
			}
		} catch (JSONException e) {
			mHasLastError = true;
			mLastError = e.getMessage();
		}
		
		Log.w(Utils.TAG, "-- END Called: getFeedHeadlines(feedId " + feedId + ", limit " + limit + ", filter " + filter + ")");
		return finalResult;
	}
	
	@Override
	public List<ArticleItem> getFeedArticles(int feedId, int limit, int filter) {
		Log.w(Utils.TAG, "-- Called: getFeedArticles(feedId " + feedId + ", limit " + limit + ", filter " + filter + ")");
		
		ArrayList<ArticleItem> finalResult = new ArrayList<ArticleItem>();
		
		if (mSessionId == null) {
			login();
			
			if (mHasLastError) {
				return null;
			}
		}
		
		String url = mServerUrl + String.format(OP_GET_ARTICLES_WITH_CONTENT, mSessionId, feedId);
		
		JSONArray jsonResult = getJSONResponseAsArray(url);
		
		JSONObject object;

		try {
			for (int i = 0; i < jsonResult.length(); i++) {
				object = jsonResult.getJSONObject(i);
				
				JSONArray names = object.names();
				JSONArray values = object.toJSONArray(names);
				
				finalResult.add(parseDataForArticle(names, values));
			}
		} catch (JSONException e) {
			mHasLastError = true;
			mLastError = e.getMessage();
		}
		
		Log.w(Utils.TAG, "-- END Called: getFeedArticles(feedId " + feedId + ", limit " + limit + ", filter " + filter + ")");
		return finalResult;
	}
	
	@Override
	public ArticleItem getArticle(int articleId) {
		Log.w(Utils.TAG, "-- Called: getArticle(articleId " + articleId + ")");
		
		ArticleItem ret = new ArticleItem();
		
		if (mSessionId == null) {
			login();
			
			if (mHasLastError) {
				return null;
			}
		}
		
		String url = mServerUrl + String.format(OP_GET_ARTICLE, mSessionId, articleId);
		
		TTRSSJsonResult jsonResult = getJSONResponse(url);
		
		if (!mHasLastError) {
			ret = parseDataForArticle(jsonResult.getNames(), jsonResult.getValues());
			if (ret.getId() == null || ret.getId().length() < 1) {
				ret.setId(articleId + "");
			}
		}
		
		Log.w(Utils.TAG, "-- END Called: getArticle(articleId " + articleId + ")");
		return ret;
	}

	public ArticleItem parseDataForArticle(JSONArray names, JSONArray values) {
		ArticleItem articleItem = new ArticleItem();
		
		try {
			String realFeedId = null;
			String id = null;
			String title = null;
			boolean isUnread = false;
			String updated = null;
			String content = "";
			String articleUrl = null;
			String articleCommentUrl = null;
			String attachments = "";
			
			for (int i = 0; i < names.length(); i++) {
			
				if (names.getString(i).equals(ID_NAME)) {
					id = values.getString(i);
				} else if (names.getString(i).equals(TITLE_NAME)) {
					title = values.getString(i);
				} else if (names.getString(i).equals(UNREAD_NAME)) {
					isUnread = values.getBoolean(i);
				} else if (names.getString(i).equals(UPDATED_NAME)) {
					updated = values.getString(i);
				} else if (names.getString(i).equals(FEED_ID_NAME)) {
					realFeedId = values.getString(i);
				} else if (names.getString(i).equals(CONTENT_NAME)) {
					content = values.getString(i);
				} else if (names.getString(i).equals(URL_NAME)) {
					articleUrl = values.getString(i);
				} else if (names.getString(i).equals(COMMENT_URL_NAME)) {
					articleCommentUrl = values.getString(i);
				} else if (names.getString(i).equals(ATTACHMENTS_NAME)) {
					Map<String, String> map = handleAttachments((JSONArray) values.get(i));
					if (map.size() > 0) {
						attachments += "<br>\n";
						for (String s : map.keySet()) {
							attachments += "Attachment " + s + ": <br><img src=\"" + map.get(s) + "\" /><br>\n";
						}
					}
				}
			}
			
			content += attachments;
			
			articleItem = new ArticleItem(
					realFeedId,
					id,
					title,
					isUnread,
					new Date(new Long(updated + "000").longValue()),
					content,
					articleUrl,
					articleCommentUrl);
		} catch (JSONException e) {
			mHasLastError = true;
    		mLastError = e.getMessage();
			e.printStackTrace();
		}
		
		return articleItem;
	}
	
	@Override
	public String getLastError() {
		return mLastError;
	}

	@Override
	public Map<String, List<FeedItem>> getSubsribedFeeds() {
		Log.w(Utils.TAG, "-- Called: getSubscribedFeeds()");
		
		Map<String, List<FeedItem>> finalResult = new HashMap<String, List<FeedItem>>();;
		
		if (mSessionId == null) {
			login();
			
			if (mHasLastError) {
				return null;
			}
		}
		
		String url = mServerUrl + String.format(OP_GET_FEEDS, mSessionId);
		
		JSONArray jsonResult = getJSONResponseAsArray(url);
				
		JSONObject object;
		
		FeedItem feedItem;
		List<FeedItem> feedItemList;
		
		String categoryId = null;		
		String id = null;
		String title = null;
		String feedUrl = null;
		int unread = 0;
		
		try {
			for (int i = 0; i < jsonResult.length(); i++) {
				object = jsonResult.getJSONObject(i);
				
				JSONArray names = object.names();
				JSONArray values = object.toJSONArray(names);
				
				for (int j = 0; j < names.length(); j++) {
					
					if (names.getString(j).equals(CAT_ID_NAME)) {
						categoryId = values.getString(j);
					} else if (names.getString(j).equals(ID_NAME)) {
						id = values.getString(j);
					} else if (names.getString(j).equals(TITLE_NAME)) {
						title = values.getString(j);
					} else if (names.getString(j).equals(FEED_URL_NAME)) {
						feedUrl = values.getString(j);
					} else if (names.getString(j).equals(UNREAD_NAME)) {
						unread = values.getInt(j);
					}
					
				}
				
				feedItem = new FeedItem(categoryId,
						id,
						title,
						feedUrl,
						unread);
				
				feedItemList = finalResult.get(categoryId);
				if (feedItemList == null) {
					feedItemList = new ArrayList<FeedItem>();
					finalResult.put(categoryId, feedItemList);
				}
				
				feedItemList.add(feedItem);
			}
		} catch (JSONException e) {
			mHasLastError = true;
			mLastError = e.getMessage();
		}
		
		Log.w(Utils.TAG, "-- END Called: getSubscribedFeeds()");
		return finalResult;
	}

	@Override
	public int getTotalUnread() {
		if (mSessionId == null) {
			login();
			
			if (mHasLastError) {
				return -1;
			}
		}
		
		String url = mServerUrl + String.format(OP_GET_UNREAD, mSessionId);
		
		TTRSSJsonResult jsonResult = getJSONResponse(url);
		
		if (mHasLastError) {
			return -1;
		}
		
		int result = -1;
		int i = 0;
		boolean stop = false;
		
		try {
			while ((i < jsonResult.getNames().length()) &&
					(!stop)) {
				if (jsonResult.getNames().getString(i).equals(UNREAD_NAME)) {

					stop = true;
					result = jsonResult.getValues().getInt(i);

				} else {
					i++;
				}
			}
		} catch (JSONException e) {
			mHasLastError = true;
    		mLastError = e.getMessage();
		}
		
		return result;
	}

	@Override
	public boolean hasLastError() {		
		return mHasLastError;
	}

	@Override
	public void setArticleRead(String articlesIds, int articleState) {
		if (mSessionId == null) {
			login();
			
			if (mHasLastError) {
				return;
			}
		}
		
		String url = mServerUrl + String.format(OP_UPDATE_ARTICLE, mSessionId, articlesIds, articleState, 2);
	
		doRequest(url);
	}
	
	@Override
	public void setRead(String id, boolean isCategory) {
		if (mSessionId == null) {
			login();
			
			if (mHasLastError) {
				return;
			}
		}
		String url = mServerUrl + String.format(OP_CATCHUP, mSessionId, id, isCategory);
		doRequest(url);
	}
	
	public Map<String, String> handleAttachments(JSONArray array) {
		Map<String, String> ret = new HashMap<String, String>();
		
		try {
			for (int j = 0; j < array.length(); j++) {
				
				TTRSSJsonResult att = new TTRSSJsonResult(array.getString(j));
	
				String attId = "";
				String attUrl = "";
				
				// Filter for id and content_url, other fields are not neccessary
				for (int k = 0; k < att.getNames().length(); k++) {
					if (att.getNames().getString(k).equals("id")) {
						attId = att.getValues().getString(k);
					}
					if (att.getNames().getString(k).equals("content_url")) {
						attUrl = att.getValues().getString(k);
					}
				}
				
				// Add only if both, id and url, are found
				if (attId.length() > 0 && attUrl.length() > 0) {
					ret.put("attachment_" + attId, attUrl);
				}
			}
		} catch (JSONException je) {
			je.printStackTrace();
		}
		
		return ret;
	}

}
