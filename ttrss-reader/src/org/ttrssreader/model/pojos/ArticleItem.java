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

package org.ttrssreader.model.pojos;

import java.util.Date;
import java.util.Set;

public class ArticleItem implements Comparable<ArticleItem> {
    
    private int mId;
    private String mTitle;
    private int mFeedId;
    private volatile boolean mIsUnread;
    private String mArticleUrl;
    private String mArticleCommentUrl;
    private Date mUpdateDate;
    private String mContent;
    private Set<String> mAttachments;
    private boolean mIsStarred;
    private boolean mIsPublished;
    
    public ArticleItem() {
    }
    
    public ArticleItem(int id, int feedId, String title, boolean isUnread, String articleUrl, String articleCommentUrl,
            Date updateDate, String content, Set<String> attachments, boolean isStarred, boolean isPublished) {
        mId = id;
        mTitle = title;
        mFeedId = feedId;
        mIsUnread = isUnread;
        mUpdateDate = updateDate;
        mArticleUrl = articleUrl;
        mArticleCommentUrl = articleCommentUrl;
        setContent(content);
        mAttachments = attachments;
        mIsStarred = isStarred;
        mIsPublished = isPublished;
    }
    
    /*
     * Article-ID and Feed-ID given as String, will be parsed in setId(String mId) or set to 0 if value is invalid.
     */
    public ArticleItem(String id, String feedId, String title, boolean isUnread, String articleUrl,
            String articleCommentUrl, Date updateDate, String content, Set<String> attachments, boolean isStarred,
            boolean isPublished) {
        setId(id);
        mTitle = title;
        setFeedId(feedId);
        mIsUnread = isUnread;
        mUpdateDate = updateDate;
        mArticleUrl = articleUrl;
        mArticleCommentUrl = articleCommentUrl;
        setContent(content);
        mAttachments = attachments;
        mIsStarred = isStarred;
        mIsPublished = isPublished;
    }
    
    public int getId() {
        return mId;
    }
    
    public void setId(int id) {
        this.mId = id;
    }
    
    public void setId(String id) {
        // Check if id is a number, else set to 0
        try {
            if (id == null) {
                this.mId = 0;
            } else if (!id.matches("[0-9]+")) {
                this.mId = 0;
            } else {
                this.mId = Integer.parseInt(id);
            }
        } catch (NumberFormatException e) {
        }
    }
    
    public String getTitle() {
        return mTitle;
    }
    
    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }
    
    public int getFeedId() {
        return mFeedId;
    }
    
    public void setFeedId(int feedId) {
        this.mFeedId = feedId;
    }
    
    public void setFeedId(String feedId) {
        // Check if feedId is a number, else set to 0
        try {
            if (feedId == null) {
                this.mFeedId = 0;
            } else if (!feedId.matches("-*[0-9]+")) {
                this.mFeedId = 0;
            } else {
                this.mFeedId = Integer.parseInt(feedId);
            }
        } catch (NumberFormatException e) {
        }
    }
    
    public boolean isUnread() {
        return mIsUnread;
    }
    
    public void setUnread(boolean mIsUnread) {
        this.mIsUnread = mIsUnread;
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
    
    public String getContent() {
        return mContent;
    }
    
    public void setContent(String mContent) {
        if (mContent == null || mContent.equals("") || mContent.equals("null")) {
            this.mContent = null;
        } else {
            this.mContent = mContent;
        }
    }
    
    public void setUpdateDate(Date mUpdateDate) {
        this.mUpdateDate = mUpdateDate;
    }
    
    public Set<String> getAttachments() {
        return mAttachments;
    }
    
    public void setAttachments(Set<String> attachments) {
        this.mAttachments = attachments;
    }
    
    public boolean isStarred() {
        return mIsStarred;
    }
    
    public void setStarred(boolean isStarred) {
        this.mIsStarred = isStarred;
    }
    
    public boolean isPublished() {
        return mIsPublished;
    }
    
    public void setPublished(boolean isPublished) {
        this.mIsPublished = isPublished;
    }
    
    @Override
    public int compareTo(ArticleItem ai) {
        return ai.getUpdateDate().compareTo(this.getUpdateDate());
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof ArticleItem) {
            ArticleItem other = (ArticleItem) o;
            return (this.getId() == other.getId());
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return this.getId() + "".hashCode();
    }
    
}
