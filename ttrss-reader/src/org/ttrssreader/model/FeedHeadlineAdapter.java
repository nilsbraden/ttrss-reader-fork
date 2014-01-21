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

import java.util.Date;
import org.ttrssreader.R;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.utils.DateUtils;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedHeadlineAdapter extends MainAdapter {
    
    protected static final String TAG = FeedHeadlineAdapter.class.getSimpleName();
    
    private int feedId;
    private boolean selectArticlesForCategory;
    
    public FeedHeadlineAdapter(Context context, int feedId, boolean selectArticlesForCategory) {
        super(context);
        this.feedId = feedId;
        this.selectArticlesForCategory = selectArticlesForCategory;
    }
    
    @Override
    public Object getItem(int position) {
        Article ret = new Article();
        Cursor cur = getCursor();
        if (cur == null)
            return ret;
        
        if (cur.getCount() >= position) {
            if (cur.moveToPosition(position)) {
                ret.id = cur.getInt(0);
                ret.feedId = cur.getInt(1);
                ret.title = cur.getString(2);
                ret.isUnread = cur.getInt(3) != 0;
                ret.updated = new Date(cur.getLong(4));
                ret.isStarred = cur.getInt(5) != 0;
                ret.isPublished = cur.getInt(6) != 0;
            }
        }
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
            layout = (LinearLayout) inflater.inflate(R.layout.item_feedheadline, null);
        } else {
            if (convertView instanceof LinearLayout) {
                layout = (LinearLayout) convertView;
            }
        }
        
        if (layout == null)
            return new View(context);
        
        ImageView icon = (ImageView) layout.findViewById(R.id.icon);
        getImage(icon, a);
        
        TextView title = (TextView) layout.findViewById(R.id.title);
        title.setText(a.title);
        if (a.isUnread)
            title.setTypeface(Typeface.DEFAULT_BOLD);
        
        TextView updateDate = (TextView) layout.findViewById(R.id.updateDate);
        String date = DateUtils.getDateTime(context, a.updated);
        updateDate.setText(date.length() > 0 ? "(" + date + ")" : "");
        
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
    
}
