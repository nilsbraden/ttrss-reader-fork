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
import java.util.Date;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.utils.DateUtils;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedHeadlineAdapter extends MainAdapter {
    public static final int POS_FEEDID = 3;
    public static final int POS_UPDATED = 4;
    public static final int POS_STARRED = 5;
    public static final int POS_PUBLISHED = 6;
    
    public FeedHeadlineAdapter(Context context, int feedId, int categoryId, boolean selectArticlesForCategory) {
        super(context, feedId, categoryId, selectArticlesForCategory);
    }
    
    @Override
    public Object getItem(int position) {
        Article ret = new Article();
        Object[] o = content.get(position);
        ret.id = (Integer) o[POS_ID];
        ret.title = (String) o[POS_TITLE];
        ret.isUnread = (Boolean) o[POS_UNREAD];
        ret.feedId = (Integer) o[POS_FEEDID];
        ret.updated = ((Date) o[POS_UPDATED]);
        ret.isStarred = (Boolean) o[POS_STARRED];
        ret.isPublished = (Boolean) o[POS_PUBLISHED];
        return ret;
    }
    
    @SuppressWarnings("deprecation")
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                icon.setBackgroundDrawable(null);
            } else {
                icon.setBackground(null);
            }
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
        
        if (layout == null)
            return new View(context);
        
        ImageView icon = (ImageView) layout.findViewById(R.id.fh_icon);
        getImage(icon, a);
        
        TextView title = (TextView) layout.findViewById(R.id.fh_title);
        title.setText(a.title);
        if (a.isUnread) {
            title.setTypeface(Typeface.DEFAULT_BOLD, 1);
        } else {
            title.setTypeface(Typeface.DEFAULT, 0);
        }
        
        TextView updateDate = (TextView) layout.findViewById(R.id.fh_updateDate);
        String date = DateUtils.getDateTime(context, a.updated);
        updateDate.setText(date.length() > 0 ? "(" + date + ")" : "");
        
        TextView dataSource = (TextView) layout.findViewById(R.id.fh_dataSource);
        // Display Feed-Title in Virtual-Categories or when displaying all Articles in a Category
        if ((feedId < 0 && feedId >= -4) || (selectArticlesForCategory)) {
            Feed f = DBHelper.getInstance().getFeed(a.feedId);
            if (f != null) {
                dataSource.setText(f.title);
            }
        }
        
        return layout;
    }
    
    protected ArrayList<Object[]> executeQuery(boolean overrideDisplayUnread, boolean buildSafeQuery) {
        
        String query;
        if (feedId > -10)
            query = buildFeedQuery(overrideDisplayUnread, buildSafeQuery);
        else
            query = buildLabelQuery(overrideDisplayUnread, buildSafeQuery);
        
        return DBHelper.getInstance().queryArticles(query);
    }
    
    private String buildFeedQuery(boolean overrideDisplayUnread, boolean buildSafeQuery) {
        String lastOpenedArticlesList = Utils.separateItems(Controller.getInstance().lastOpenedArticles, ",");
        
        boolean displayUnread = Controller.getInstance().onlyUnread();
        boolean invertSortArticles = Controller.getInstance().invertSortArticlelist();
        
        if (overrideDisplayUnread)
            displayUnread = false;
        
        StringBuilder query = new StringBuilder();
        query.append("SELECT a.id,a.feedId,a.title,a.isUnread AS unread,a.updateDate,a.isStarred,a.isPublished FROM ");
        query.append(DBHelper.TABLE_ARTICLES);
        query.append(" a, ");
        query.append(DBHelper.TABLE_FEEDS);
        query.append(" b WHERE a.feedId=b.id");
        
        switch (feedId) {
            case Data.VCAT_STAR:
                query.append(" AND a.isStarred=1");
                break;
            
            case Data.VCAT_PUB:
                query.append(" AND a.isPublished=1");
                break;
            
            case Data.VCAT_FRESH:
                query.append(" AND a.updateDate>");
                query.append(Controller.getInstance().getFreshArticleMaxAge());
                query.append(" AND a.isUnread>0");
                break;
            
            case Data.VCAT_ALL:
                query.append(displayUnread ? " AND a.isUnread>0" : "");
                break;
            
            default:
                // User selected to display all articles of a category directly
                query.append(selectArticlesForCategory ? (" AND b.categoryId=" + categoryId)
                        : (" AND a.feedId=" + feedId));
                query.append(displayUnread ? " AND a.isUnread>0" : "");
        }
        
        if (lastOpenedArticlesList.length() > 0 && !buildSafeQuery) {
            query.append(" UNION SELECT c.id,c.feedId,c.title,c.isUnread AS unread,c.updateDate,c.isStarred,c.isPublished FROM ");
            query.append(DBHelper.TABLE_ARTICLES);
            query.append(" c, ");
            query.append(DBHelper.TABLE_FEEDS);
            query.append(" d WHERE c.feedId=d.id AND c.id IN (");
            query.append(lastOpenedArticlesList);
            query.append(" )");
        }
        
        query.append(" ORDER BY a.updateDate ");
        query.append(invertSortArticles ? "ASC" : "DESC");
        return query.toString();
    }
    
    private String buildLabelQuery(boolean overrideDisplayUnread, boolean buildSafeQuery) {
        String lastOpenedArticlesList = Utils.separateItems(Controller.getInstance().lastOpenedArticles, ",");
        
        boolean displayUnread = Controller.getInstance().onlyUnread();
        boolean invertSortArticles = Controller.getInstance().invertSortArticlelist();
        
        if (overrideDisplayUnread)
            displayUnread = false;
        
        StringBuilder query = new StringBuilder();
        query.append("SELECT a.id,feedId,a.title,isUnread AS unread,updateDate,isStarred,isPublished FROM ");
        query.append(DBHelper.TABLE_ARTICLES);
        query.append(" a, ");
        query.append(DBHelper.TABLE_ARTICLES2LABELS);
        query.append(" a2l, ");
        query.append(DBHelper.TABLE_FEEDS);
        query.append(" l WHERE a.id=a2l.articleId AND a2l.labelId=l.id");
        query.append(" AND a2l.labelId=" + feedId);
        query.append(displayUnread ? " AND isUnread>0" : "");
        
        if (lastOpenedArticlesList.length() > 0 && !buildSafeQuery) {
            query.append(" UNION SELECT b.id,feedId,b.title,isUnread AS unread,updateDate,isStarred,isPublished FROM ");
            query.append(DBHelper.TABLE_ARTICLES);
            query.append(" b, ");
            query.append(DBHelper.TABLE_ARTICLES2LABELS);
            query.append(" b2m, ");
            query.append(DBHelper.TABLE_FEEDS);
            query.append(" m WHERE b2m.labelId=m.id AND b2m.articleId=b.id");
            query.append(" AND b.id IN (");
            query.append(lastOpenedArticlesList);
            query.append(" )");
        }
        
        query.append(" ORDER BY updateDate ");
        query.append(invertSortArticles ? "ASC" : "DESC");
        return query.toString();
    }
    
}
