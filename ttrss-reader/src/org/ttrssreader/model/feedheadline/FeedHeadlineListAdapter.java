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

package org.ttrssreader.model.feedheadline;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.IRefreshable;
import org.ttrssreader.model.IUpdatable;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedHeadlineListAdapter extends BaseAdapter implements IRefreshable, IUpdatable {
    
    private Context context;
    
    private int feedId;
    private Cursor cursor;
    private boolean displayOnlyUnread;
    
    public FeedHeadlineListAdapter(Context context, int feedId_) {
        displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnread();
        this.context = context;
        this.feedId = feedId_;
        this.cursor = makeQuery(feedId, displayOnlyUnread);
    }
    
    @Override
    public int getCount() {
        return cursor.getCount();
    }
    
    @Override
    public Object getItem(int position) {
        ArticleItem ret = null;
        if (cursor.getCount() >= position) {
            if (cursor.moveToPosition(position)) {
                
                ret = new ArticleItem();
                ret.setFeedId(cursor.getInt(0));
                ret.setId(cursor.getInt(1));
                ret.setTitle(cursor.getString(2));
                ret.setUnread(cursor.getInt(3) != 0);
                ret.setStarred(cursor.getInt(4) != 0);
                ret.setPublished(cursor.getInt(5) != 0);
                ret.setUpdateDate(new Date(cursor.getLong(6)));
            }
        }
        return ret;
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    public int getFeedItemId(int position) {
        int ret = 0;
        if (cursor.getCount() >= position) {
            if (cursor.moveToPosition(position)) {
                ret = cursor.getInt(0);
            }
        }
        return ret;
    }
    
    public ArrayList<Integer> getFeedItemIds() {
        ArrayList<Integer> result = new ArrayList<Integer>();
        
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            result.add(cursor.getInt(0));
            cursor.move(1);
        }
        
        return result;
    }
    
    public int getUnreadCount() {
        int result = 0;
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            if (cursor.getInt(3) != 0) {
                result++;
            }
            cursor.move(1);
        }
        return result;
    }
    
    private void getImage(ImageView icon, ArticleItem a) {
        if (a.isUnread()) {
            icon.setBackgroundResource(R.drawable.articleunread48);
        } else {
            icon.setBackgroundResource(R.drawable.articleread48);
        }
        
        if (a.isStarred() && a.isPublished()) {
            icon.setImageResource(R.drawable.published_and_starred48);
        } else if (a.isStarred()) {
            icon.setImageResource(R.drawable.star_yellow48);
        } else if (a.isPublished()) {
            icon.setImageResource(R.drawable.published_blue48);
        } else {
            icon.setBackgroundDrawable(null);
            if (a.isUnread()) {
                icon.setImageResource(R.drawable.articleunread48);
            } else {
                icon.setImageResource(R.drawable.articleread48);
            }
        }
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= cursor.getCount())
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
        
        // TODO: Find a way to overlay more than 2 images
        ImageView icon = (ImageView) layout.findViewById(R.id.icon);
        getImage(icon, a);
        
        TextView title = (TextView) layout.findViewById(R.id.title);
        title.setText(a.getTitle());
        if (a.isUnread()) {
            title.setTypeface(Typeface.DEFAULT_BOLD, 1);
        } else {
            title.setTypeface(Typeface.DEFAULT, 0);
        }
        
        TextView updateDate = (TextView) layout.findViewById(R.id.updateDate);
        String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(a.getUpdateDate());
        updateDate.setText(date);
        
        TextView dataSource = (TextView) layout.findViewById(R.id.dataSource);
        if (feedId < 0 && feedId >= -4) {
            FeedItem f = Data.getInstance().getFeed(a.getFeedId());
            if (f != null) {
                dataSource.setText(f.getTitle());
            }
        }
        
        return layout;
    }
    
    public void closeCursor() {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }
    
    private static Cursor makeQuery(int feedId, boolean displayOnlyUnread) {
        StringBuffer query = new StringBuffer();
        
        query.append("SELECT a.feedId,a.id,a.title,a.isUnread,a.isStarred,a.isPublished,a.updateDate,b.title AS feedTitle FROM ");
        query.append(DBHelper.TABLE_ARTICLES);
        query.append(" a,");
        query.append(DBHelper.TABLE_FEEDS);
        query.append(" b WHERE a.feedId=b.id");
        
        if (displayOnlyUnread) {
            query.append(" AND a.isUnread>0");
        }
        
        if (feedId == -1) {
            query.append(" AND a.isStarred=1");
        } else if (feedId == -2) {
            query.append(" AND a.isPublished=1");
        } else if (feedId == -3) {
            long updateDate = Controller.getInstance().getFreshArticleMaxAge();
            query.append(" AND a.updateDate>");
            query.append(updateDate);
        } else if (feedId == -4) {
            
        } else {
            query.append(" AND a.feedId=");
            query.append(feedId);
        }
        
        query.append(" ORDER BY a.updateDate DESC");
        
        Log.d(Utils.TAG, query.toString());
        return DBHelper.getInstance().query(query.toString(), null);
    }
    
    @Override
    public Set<?> refreshData() {
        
        // Only create new query when request changed, close cursor before
        if (displayOnlyUnread != Controller.getInstance().isDisplayOnlyUnread()) {
            closeCursor();
            displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnread();
            cursor = makeQuery(feedId, displayOnlyUnread);
        } else if (cursor.isClosed()) {
            cursor = makeQuery(feedId, displayOnlyUnread);
        } else {
            cursor.requery();
        }
        return null;
    }
    
    @Override
    public void update() {
        if (!Controller.getInstance().isWorkOffline()) {
            Log.i(Utils.TAG, "updateArticles(feedId: " + feedId + ")");
            Data.getInstance().updateArticles(feedId, Controller.getInstance().isDisplayOnlyUnread());
        }
    }
    
}
