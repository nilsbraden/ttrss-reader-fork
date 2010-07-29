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

package org.ttrssreader.model.article;

import java.util.Date;

public class ArticleItem {
	
	private String mId;
	private String mTitle;
	private String mFeedId;
	private boolean mIsUnread;
	private String mContent;
	private String mArticleUrl;
	private String mArticleCommentUrl;
	private Date mUpdateDate;
	private boolean mIsContentLoaded;
	
	public ArticleItem() { }
	
	public ArticleItem(String feedId, String id) {
		this(feedId, id, null, false, null);
	}
	
	public ArticleItem(String feedId, String id, String title, boolean isUnread, Date updateDate) {
		mId = id;
		mTitle = title;
		mFeedId = feedId;
		mIsUnread = isUnread;
		mUpdateDate = updateDate;
		mContent = "";
		mArticleUrl = null;
		mArticleCommentUrl = null;
		setIsContentLoaded(false);
	}
	
	public ArticleItem(String feedId, String id, String title, boolean isUnread, Date updateDate, String content,
			String articleUrl, String articleCommentUrl) {
		mId = id;
		mTitle = title;
		mFeedId = feedId;
		mIsUnread = isUnread;
		mUpdateDate = updateDate;
		mArticleUrl = articleUrl;
		mArticleCommentUrl = articleCommentUrl;
		
		if ( content == null || (content.length() < 1 || content.equals("null")) ) {	
			setIsContentLoaded(false);
			mContent = "";
		} else {
			setIsContentLoaded(true);
			mContent = content;
		}
	}
	
//	public void doLoadContent() {
//		Map<?, ?> result = Controller.getInstance().getTTRSSConnector().getArticle(new Integer(mId).intValue());
//		if (result != null) {
//			setContent(result.get("content").toString());
//			
//			if ((mTitle == null) || (mTitle.length() == 0)) {
//				setTitle(result.get("title").toString());
//			}
//			
//			setArticleUrl(result.get("link").toString());
//			setArticleCommentUrl(result.get("comments").toString());
//			
//			if (mUpdateDate == null) {
//				String updatedDate = result.get("updated").toString();
//				if ((updatedDate != null) && (updatedDate.length() > 0)) {
//					// PHP strtotime gives timestamp in seconds.
//					setUpdateDate(new Date(new Long(result.get("updated").toString() + "000").longValue()));
//				} else {
//					setUpdateDate(null);
//				}
//			}
//			
//			Map<String, String> mAttachments = new HashMap<String, String>();
//			
//			// Find attachments (key = "attachment_[0-9]*"
//			for (Object key : result.keySet()) {
//				
//				String string = (String) key;
//				String value;
//				
//				if (string.startsWith("attachment_")) {
//					
//					string = string.replace("attachment_", "");
//					value = (String) result.get(key);
//					
//					mAttachments.put(string, value);
//					
//				}
//			}
//			
//			// Add Attachments to Content
//			if (mAttachments.size() > 0) {
//				mContent += "<br>\n";
//				for (String s : mAttachments.keySet()) {
//					mContent += "Attachment " + s + ": <br><img src=\"" + mAttachments.get(s) + "\" /><br>\n";
//				}
//			}
//			
//		}
//		mIsContentLoaded = true;
//	}

	public String getId() {
		return mId;
	}

	public void setId(String mId) {
		this.mId = mId;
	}

	public String getTitle() {
		return mTitle;
	}

	public void setTitle(String mTitle) {
		this.mTitle = mTitle;
	}

	public String getFeedId() {
		return mFeedId;
	}

	public void setFeedId(String mFeedId) {
		this.mFeedId = mFeedId;
	}

	public boolean isUnread() {
		return mIsUnread;
	}

	public void setIsUnread(boolean mIsUnread) {
		this.mIsUnread = mIsUnread;
	}

	public String getContent() {
		return mContent;
	}

	public void setContent(String mContent) {
		this.mContent = mContent;
	}

	public String getArticleUrl() {
		return mArticleUrl;
	}

	public void setArticleUrl(String mArticleUrl) {
		this.mArticleUrl = mArticleUrl;
	}

	public String getArticleCommentUrl() {
		return mArticleCommentUrl;
	}

	public void setArticleCommentUrl(String mArticleCommentUrl) {
		this.mArticleCommentUrl = mArticleCommentUrl;
	}

	public Date getUpdateDate() {
		return mUpdateDate;
	}

	public void setUpdateDate(Date mUpdateDate) {
		this.mUpdateDate = mUpdateDate;
	}

	public boolean isContentLoaded() {
		return mIsContentLoaded;
	}

	public void setIsContentLoaded(boolean mIsContentLoaded) {
		this.mIsContentLoaded = mIsContentLoaded;
	}
	
}
