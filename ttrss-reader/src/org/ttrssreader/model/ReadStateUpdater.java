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

package org.ttrssreader.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.utils.Utils;
import android.util.Log;

public class ReadStateUpdater implements IUpdatable {
    
    List<ArticleItem> mList = null;
    private int mPid = 0;
    private int mArticleState;
    
    public ReadStateUpdater(List<CategoryItem> list, int articleState) {
        mList = new ArrayList<ArticleItem>();
        for (CategoryItem ci : list) {
            for (FeedItem fi : Data.getInstance().getFeeds(ci.getId())) {
                mList.addAll(Data.getInstance().getArticles(fi.getId()));
            }
        }
        mArticleState = articleState;
    }
    
    public ReadStateUpdater(CategoryItem c, int pid, int articleState) {
        mList = new ArrayList<ArticleItem>();
        for (FeedItem fi : Data.getInstance().getFeeds(c.getId())) {
            mList.addAll(Data.getInstance().getArticles(fi.getId()));
        }
        mPid = pid;
        mArticleState = articleState;
    }
    
    public ReadStateUpdater(List<FeedItem> list, int pid, int articleState, boolean isFeedList) {
        mList = new ArrayList<ArticleItem>();
        for (FeedItem fi : list) {
            mList.addAll(Data.getInstance().getArticles(fi.getId()));
        }
        mPid = pid;
        mArticleState = articleState;
    }
    
    public ReadStateUpdater(FeedItem f, int pid, int articleState) {
        mList = new ArrayList<ArticleItem>();
        mList.addAll(Data.getInstance().getArticles(f.getId()));
        mPid = pid;
        mArticleState = articleState;
    }
    
    public ReadStateUpdater(List<ArticleItem> list, int pid, int articleState) {
        mList = list;
        mPid = pid;
        mArticleState = articleState;
    }
    
    public ReadStateUpdater(ArticleItem article, int pid, int articleState) {
        mList = new ArrayList<ArticleItem>();
        mList.add(article);
        mPid = pid;
        mArticleState = articleState;
    }
    
    @Override
    public void update() {
        
        Log.i(Utils.TAG, "Updating Article-Read-Status...");
        
        boolean boolState = mArticleState == 1 ? true : false;
        int intState = mArticleState == 1 ? 1 : -1;
        
        Set<Integer> ids = new HashSet<Integer>();
        mList = filterList();
        
        for (ArticleItem article : mList) {
            // Build a list of article ids to update.
            ids.add(article.getId());
            article.setUnread(false);
            
            int feedId = article.getFeedId();
            int articleId = article.getId();
            FeedItem mFeed = Data.getInstance().getFeed(feedId);
            
            if (mFeed == null) {
                continue;
            }
            
            int categoryId = mFeed.getCategoryId();
            
            ArticleItem articleTemp = Data.getInstance().getArticle(articleId);
            if (articleTemp != null)
                articleTemp.setUnread(boolState);
            
            FeedItem feed = Data.getInstance().getFeed(feedId);
            if (feed != null) {
                feed.setDeltaUnreadCount(intState);
                DBHelper.getInstance().updateFeedDeltaUnreadCount(feedId, categoryId, intState);
            }
            
            CategoryItem category = Data.getInstance().getCategory(categoryId);
            if (category != null) {
                category.setDeltaUnreadCount(intState);
                DBHelper.getInstance().updateCategoryDeltaUnreadCount(categoryId, intState);
            }
            
            // If on a virtual feeds, also update article state in it.
            if (mPid < 0 && mPid >= -4) {
                
                ArticleItem a = Data.getInstance().getArticle(articleId);
                if (a != null)
                    a.setUnread(boolState);
                
                CategoryItem c = Data.getInstance().getCategory(mPid);
                if (c != null)
                    c.setDeltaUnreadCount(intState);
            }
        }
        
        if (ids.size() > 0) {
            Controller.getInstance().getConnector().setArticleRead(ids, mArticleState);
            DBHelper.getInstance().markArticlesRead(ids, mArticleState);
            
            int deltaUnread = mArticleState == 1 ? mList.size() : -mList.size();
            Data.getInstance().getCategory(-4).setDeltaUnreadCount(deltaUnread);
        }
    }
    
    public List<ArticleItem> filterList() {
        List<ArticleItem> ret = new ArrayList<ArticleItem>();
        
        for (ArticleItem a : mList) {
            if (mArticleState == 0 && a.isUnread()) {
                ret.add(a);
            } else if (mArticleState != 0 && !a.isUnread()) {
                ret.add(a);
            }
        }
        
        return ret;
    }
    
}
