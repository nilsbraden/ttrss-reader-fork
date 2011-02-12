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
    
    public int mId;
    public String mTitle;
    public int mFeedId;
    public volatile boolean mIsUnread;
    public String mArticleUrl;
    public String mArticleCommentUrl;
    public Date mUpdateDate;
    public String mContent;
    public Set<String> mAttachments;
    public boolean mIsStarred;
    public boolean mIsPublished;
    
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
        if (content == null || content.equals("null")) {
            this.mContent = null;
        } else {
            this.mContent = content;
        }
        mAttachments = attachments;
        mIsStarred = isStarred;
        mIsPublished = isPublished;
    }
    
    @Override
    public int compareTo(ArticleItem ai) {
        return ai.mUpdateDate.compareTo(this.mUpdateDate);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof ArticleItem) {
            ArticleItem other = (ArticleItem) o;
            return (this.mId == other.mId);
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return this.mId + "".hashCode();
    }
    
}
