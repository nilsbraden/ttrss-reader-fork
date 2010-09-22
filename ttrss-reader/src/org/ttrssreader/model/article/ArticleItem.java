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
import org.ttrssreader.utils.Utils;
import android.util.Log;

public class ArticleItem {
    
    private int mId;
    private String mTitle;
    private String mFeedId;
    private boolean mIsUnread;
    private String mContent;
    private String mArticleUrl;
    private String mArticleCommentUrl;
    private Date mUpdateDate;
    
    public ArticleItem() {
    }
    
    public ArticleItem(String feedId, int id, String title, boolean isUnread, Date updateDate, String content,
            String articleUrl, String articleCommentUrl) {
        
        setId(id);
        
        mTitle = title;
        mFeedId = feedId;
        mIsUnread = isUnread;
        mUpdateDate = updateDate;
        mArticleUrl = articleUrl;
        mArticleCommentUrl = articleCommentUrl;
        
        if (content == null || content.equals("") || content.equals("null")) {
            mContent = null;
        } else {
            mContent = content;
        }
    }
    
    /*
     * Article-ID given as String, will be parsed in setId(String mId) or set to 0 if value is invalid.
     */
    public ArticleItem(String feedId, String id, String title, boolean isUnread, Date updateDate, String content,
            String articleUrl, String articleCommentUrl) {
        
        setId(id);
        
        mTitle = title;
        mFeedId = feedId;
        mIsUnread = isUnread;
        mUpdateDate = updateDate;
        mArticleUrl = articleUrl;
        mArticleCommentUrl = articleCommentUrl;
        
        if (content == null || content.equals("") || content.equals("null")) {
            mContent = null;
        } else {
            mContent = content;
        }
    }
    
    public int getId() {
        return mId;
    }
    
    public void setId(String mId) {
        // Check if mId is a number, else set to 0
        try {
            if (mId == null) {
                this.mId = 0;
                mId = "null"; // Set to (String) "null" for log-output..
            } else if (!mId.matches("[0-9]*")) {
                this.mId = 0;
            } else {
                this.mId = Integer.parseInt(mId);
            }
        } catch (NumberFormatException e) {
            Log.d(Utils.TAG, "Article-ID has to be an integer-value but was " + mId);
        }
    }
    
    public void setId(int mId) {
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
    
    public void setUnread(boolean mIsUnread) {
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
}
