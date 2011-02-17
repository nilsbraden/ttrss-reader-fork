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
import java.util.ArrayList;
import java.util.Date;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.ArticleItem;
import org.ttrssreader.model.pojos.FeedItem;
import org.ttrssreader.model.updaters.IUpdatable;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedHeadlineListAdapter extends BaseAdapter implements IUpdatable {
    
    private Context context;
    
    private int feedId;
    public Cursor cursor;
    private boolean displayOnlyUnread;
    private int unreadCount = 0;
    
    public FeedHeadlineListAdapter(Context context, int feedId) {
        displayOnlyUnread = Controller.getInstance().displayOnlyUnread();
        this.context = context;
        this.feedId = feedId;
        makeQuery();
    }
    
    @Override
    public int getCount() {
        if (cursor.isClosed()) {
            return 1;
        }
        
        return cursor.getCount();
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
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    public int getFeedItemId(int position) {
        if (cursor.isClosed()) {
            return 1;
        }
        
        if (cursor.getCount() >= position) {
            if (cursor.moveToPosition(position)) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }
    
    public ArrayList<Integer> getFeedItemIds() {
        if (cursor.isClosed()) {
            return null;
        }
        
        ArrayList<Integer> result = new ArrayList<Integer>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            result.add(cursor.getInt(0));
            cursor.move(1);
        }
        return result;
    }
    
    public int getUnread() {
        return unreadCount;
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
        
        // TODO: Find a way to overlay more than 2 images
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
        if (feedId < 0 && feedId >= -4) {
            FeedItem f = DBHelper.getInstance().getFeed(a.feedId);
            if (f != null) {
                dataSource.setText(f.title);
            }
        }
        
        return layout;
    }
    
    private void closeCursor() {
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }
    
    public synchronized void makeQuery() {
        if (displayOnlyUnread != Controller.getInstance().displayOnlyUnread()) {
            displayOnlyUnread = Controller.getInstance().displayOnlyUnread();
            closeCursor();
        } else if (cursor != null && !cursor.isClosed()) {
            cursor.requery();
        }
        
        StringBuffer query = new StringBuffer();
        
        query.append("SELECT a.id,a.feedId,a.title,a.isUnread,a.updateDate,a.isStarred,a.isPublished,b.title AS feedTitle FROM ");
        query.append(DBHelper.TABLE_ARTICLES);
        query.append(" a, ");
        query.append(DBHelper.TABLE_FEEDS);
        query.append(" b WHERE a.feedId=b.id");
        
        if (feedId == -1) {
            query.append(" AND a.isStarred=1");
        } else if (feedId == -2) {
            query.append(" AND a.isPublished=1");
        } else if (feedId == -3) {
            long updateDate = Controller.getInstance().getFreshArticleMaxAge();
            query.append(" AND a.updateDate>");
            query.append(updateDate);
        } else if (feedId == -4) {
            if (displayOnlyUnread) {
                query.append(" AND a.isUnread>0");
            }
        } else {
            query.append(" AND a.feedId=");
            query.append(feedId);
            if (displayOnlyUnread) {
                query.append(" AND a.isUnread>0");
            }
        }
        
        query.append(" ORDER BY a.updateDate DESC");
        
        // Log.v(Utils.TAG, query.toString());
        if (cursor != null)
            cursor.close();
        cursor = DBHelper.getInstance().query(query.toString(), null);
    }
    
    @Override
    public void update() {
        Data.getInstance().updateArticles(feedId, Controller.getInstance().displayOnlyUnread());
        unreadCount = DBHelper.getInstance().getUnreadCount(feedId, false);
    }
    
}
