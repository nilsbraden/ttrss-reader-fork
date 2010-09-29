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

package org.ttrssreader.model;

import java.util.ArrayList;
import java.util.List;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.DataController;
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
        for (CategoryItem c : list) {
            mList.addAll(DataController.getInstance().retrieveHeadlines(c.getId(), false, false));
        }
        mArticleState = articleState;
    }
    
    public ReadStateUpdater(CategoryItem c, int pid, int articleState) {
        mList = new ArrayList<ArticleItem>();
        mList.addAll(DataController.getInstance().retrieveHeadlines(c.getId(), false, false));
        mPid = pid;
        mArticleState = articleState;
    }
    
    public ReadStateUpdater(List<FeedItem> list, int pid, int articleState, boolean isFeedList) {
        mList = new ArrayList<ArticleItem>();
        for (FeedItem f : list) {
            mList.addAll(DataController.getInstance().retrieveHeadlines(f.getId(), false, false));
        }
        mPid = pid;
        mArticleState = articleState;
    }
    
    public ReadStateUpdater(FeedItem f, int pid, int articleState) {
        mList = new ArrayList<ArticleItem>();
        mList.addAll(DataController.getInstance().retrieveHeadlines(f.getId(), false, false));
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
        
        List<Integer> idList = new ArrayList<Integer>();
        mList = filterList();
        
        for (ArticleItem article : mList) {
            // Build a list of article ids to update.
            idList.add(article.getId());
            article.setUnread(false);
            
            int feedId = article.getFeedId();
            int articleId = article.getId();
            FeedItem mFeed = DataController.getInstance().getFeed(feedId, false);
            
            if (mFeed == null) {
                continue;
            }
            
            int categoryId = mFeed.getCategoryId();
            
            ArticleItem articleTemp = DataController.getInstance().getArticleHeadline(feedId, articleId);
            if (articleTemp != null)
                articleTemp.setUnread(boolState);
            
            FeedItem feed = DataController.getInstance().getFeed(feedId, false);
            if (feed != null) {
                feed.setDeltaUnreadCount(intState);
                DBHelper.getInstance().updateFeedDeltaUnreadCount(feedId, categoryId, intState);
            }
            
            CategoryItem category = DataController.getInstance().getCategory(categoryId, true);
            if (category != null) {
                category.setDeltaUnreadCount(intState);
                DBHelper.getInstance().updateCategoryDeltaUnreadCount(categoryId, intState);
            }
            
            // If on a virtual feeds, also update article state in it.
            if (mPid < 0 && mPid >= -4) {
                
                ArticleItem a = DataController.getInstance().getArticleHeadline(mPid, articleId);
                if (a != null)
                    a.setUnread(boolState);
                
                CategoryItem c = DataController.getInstance().getVirtualCategory(mPid);
                if (c != null)
                    c.setDeltaUnreadCount(intState);
            }
        }
        
        if (idList.size() > 0) {
            Controller.getInstance().getTTRSSConnector().setArticleRead(idList, mArticleState);
            DBHelper.getInstance().markArticlesRead(idList, mArticleState);
            
            int deltaUnread = mArticleState == 1 ? mList.size() : -mList.size();
            DataController.getInstance().getVirtualCategory(-4).setDeltaUnreadCount(deltaUnread);
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
