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
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Feed;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedHeadlineAdapter extends MainAdapter {
    
    public FeedHeadlineAdapter(Context context, int feedId) {
        super(context, feedId, -1, false);
    }
    
    public FeedHeadlineAdapter(Context context, int feedId, int categoryId, boolean selectArticlesForCategory) {
        super(context, feedId, categoryId, selectArticlesForCategory);
    }
    
    @Override
    public Object getItem(int position) {
        if (cursor.isClosed()) {
            makeQuery();
        }
        
        Article ret = null;
        if (cursor.getCount() >= position) {
            if (cursor.moveToPosition(position)) {
                ret = new Article();
                ret.id = cursor.getInt(0);
                ret.feedId = cursor.getInt(1);
                ret.title = cursor.getString(2);
                ret.isUnread = cursor.getInt(3) != 0;
                ret.updated = new Date(cursor.getLong(4));
                ret.isStarred = cursor.getInt(5) != 0;
                ret.isPublished = cursor.getInt(6) != 0;
            }
        }
        return ret;
    }
    
    private void getImage(ImageView icon, Article a) {
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
        
        Article a = (Article) getItem(position);
        
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
            Feed f = DBHelper.getInstance().getFeed(a.feedId);
            if (f != null) {
                dataSource.setText(f.title);
            }
        }
        
        return layout;
    }
    
    protected String buildQuery(boolean overrideDisplayUnread, boolean buildSafeQuery) {
        StringBuilder query = new StringBuilder();
        
        Integer lastOpenedArticle = Controller.getInstance().lastOpenedArticle;
        boolean displayUnread = displayOnlyUnread;
        if (overrideDisplayUnread)
            displayUnread = false;
        
//        if (lastOpenedArticle != null) {
//            query.append("SELECT id,feedId,title,unread,updateDate,isStarred,isPublished FROM (");
//        }
        
        query.append("SELECT a.id,feedId,a.title,isUnread AS unread,updateDate,isStarred,isPublished FROM ");
        query.append(DBHelper.TABLE_ARTICLES);
        query.append(" a, ");
        query.append(DBHelper.TABLE_FEEDS);
        query.append(" b WHERE feedId=b.id");
        
        switch (feedId) {
            case -1:
                query.append(" AND isStarred=1");
                break;
            
            case -2:
                query.append(" AND isPublished=1");
                break;
            
            case -3:
                query.append(" AND updateDate>");
                query.append(Controller.getInstance().getFreshArticleMaxAge());
                query.append(" AND isUnread>0");
                break;
            
            case -4:
                query.append(displayUnread ? " AND isUnread>0" : "");
                break;
            
            default:
                // User selected to display all articles of a category directly
                query.append(selectArticlesForCategory ? (" AND categoryId=" + categoryId)
                        : (" AND feedId=" + feedId));
                query.append(displayUnread ? " AND isUnread>0" : "");
        }
        
        if (lastOpenedArticle != null && !buildSafeQuery) {
            query.append(" UNION SELECT c.id,feedId,c.title,isUnread AS unread,updateDate,isStarred,isPublished");
            query.append(" FROM articles c, feeds d WHERE feedId=d.id AND c.id=");
            query.append(lastOpenedArticle);
//            query.append(")");
        }
        
        query.append(" ORDER BY updateDate ");
        query.append(invertSortArticles ? "ASC" : "DESC");
        query.append(buildSafeQuery ? " LIMIT 100" : " LIMIT 1000"); // TODO: Does a hard limit make sense here?
        
        return query.toString();
    }
    
}
