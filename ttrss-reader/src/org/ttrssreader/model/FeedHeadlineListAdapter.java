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

import java.text.DateFormat;
import java.util.Date;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.ArticleItem;
import org.ttrssreader.model.pojos.FeedItem;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedHeadlineListAdapter extends MainAdapter {
    
    private boolean selectArticlesForCategory;
    
    public FeedHeadlineListAdapter(Context context, int feedId) {
        this(context, feedId, -1, false);
    }
    
    public FeedHeadlineListAdapter(Context context, int feedId, int categoryId, boolean selectArticlesForCategory) {
        super(context);
        this.selectArticlesForCategory = selectArticlesForCategory;
        this.feedId = feedId;
        this.categoryId = categoryId;
        makeQuery();
    }
    
    @Override
    public Object getItem(int position) {
        if (cursor.isClosed()) {
            return null;
        }
        
        if (cursor.getCount() >= position) {
            if (cursor.moveToPosition(position)) {
                ArticleItem ret = new ArticleItem();
                ret.id = cursor.getInt(0);
                ret.feedId = cursor.getInt(1);
                ret.title = cursor.getString(2);
                ret.isUnread = cursor.getInt(3) != 0;
                ret.updated = new Date(cursor.getLong(4));
                ret.isStarred = cursor.getInt(5) != 0;
                ret.isPublished = cursor.getInt(6) != 0;
                return ret;
            }
        }
        return null;
    }
    
    private void getImage(ImageView icon, ArticleItem a) {
        if (a.isUnread) {
            icon.setBackgroundResource(R.drawable.articleunread48);
        } else {
            icon.setBackgroundResource(R.drawable.articleread48);
        }
        
        if (a.isStarred && a.isPublished) {
            icon.setImageResource(R.drawable.published_and_starred48);
        } else if (a.isStarred) {
            icon.setImageResource(R.drawable.star_yellow48);
        } else if (a.isPublished) {
            icon.setImageResource(R.drawable.published_blue48);
        } else {
            icon.setBackgroundDrawable(null);
            if (a.isUnread) {
                icon.setImageResource(R.drawable.articleunread48);
            } else {
                icon.setImageResource(R.drawable.articleread48);
            }
        }
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= getCount() || position < 0)
            return new View(context);
        
        ArticleItem a = (ArticleItem) getItem(position);
        
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout layout = null;
        if (convertView == null) {
            layout = (LinearLayout) inflater.inflate(R.layout.feedheadlineitem, null);
        } else {
            if (convertView instanceof LinearLayout) {
                layout = (LinearLayout) convertView;
            }
        }
        
        ImageView icon = (ImageView) layout.findViewById(R.id.icon);
        getImage(icon, a);
        
        TextView title = (TextView) layout.findViewById(R.id.title);
        title.setText(a.title);
        if (a.isUnread) {
            title.setTypeface(Typeface.DEFAULT_BOLD, 1);
        } else {
            title.setTypeface(Typeface.DEFAULT, 0);
        }
        
        TextView updateDate = (TextView) layout.findViewById(R.id.updateDate);
        String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(a.updated);
        updateDate.setText(date);
        
        TextView dataSource = (TextView) layout.findViewById(R.id.dataSource);
        // Display Feed-Title in Virtual-Categories or when displaying all Articles in a Category
        if ((feedId < 0 && feedId >= -4) || (selectArticlesForCategory)) {
            FeedItem f = DBHelper.getInstance().getFeed(a.feedId);
            if (f != null) {
                dataSource.setText(f.title);
            }
        }
        
        return layout;
    }
    
    protected String buildQuery() {
        StringBuilder query = new StringBuilder();
        
        query.append("SELECT a.id,a.feedId,a.title,a.isUnread,a.updateDate,a.isStarred,a.isPublished,b.title AS feedTitle FROM ");
        query.append(DBHelper.TABLE_ARTICLES);
        query.append(" a, ");
        query.append(DBHelper.TABLE_FEEDS);
        query.append(" b WHERE a.feedId=b.id");
        
        switch (feedId) {
            case -1:
                query.append(" AND a.isStarred=1");
                break;
            
            case -2:
                query.append(" AND a.isPublished=1");
                break;
            
            case -3:
                query.append(" AND a.updateDate>");
                query.append(Controller.getInstance().getFreshArticleMaxAge());
                query.append(" AND a.isUnread>0");
                break;
            
            case -4:
                if (displayOnlyUnread)
                    query.append(" AND a.isUnread>0");
                break;
            
            default:
                if (selectArticlesForCategory) {
                    // User selected to display all articles of a category directly
                    query.append(" AND b.categoryId=");
                    query.append(categoryId);
                } else {
                    query.append(" AND a.feedId=");
                    query.append(feedId);
                }
                if (displayOnlyUnread)
                    query.append(" AND a.isUnread>0");
        }
        
        query.append(" ORDER BY a.updateDate ");
        
        if (invertSortArticles) {
            query.append("ASC");
        } else {
            query.append("DESC");
        }
        
        return query.toString();
    }
    
    @Override
    public void update() {
        if (selectArticlesForCategory) {
            unreadCount = DBHelper.getInstance().getUnreadCount(categoryId, true);
            for (FeedItem f : DBHelper.getInstance().getFeeds(categoryId)) {
                Data.getInstance().updateArticles(f.id, Controller.getInstance().displayOnlyUnread());
            }
        } else {
            unreadCount = DBHelper.getInstance().getUnreadCount(feedId, false);
            Data.getInstance().updateArticles(feedId, Controller.getInstance().displayOnlyUnread());
        }
    }
    
}
