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
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedAdapter extends MainAdapter {
    
    public FeedAdapter(Context context, int categoryId) {
        super(context, categoryId);
    }
    
    @Override
    public Object getItem(int position) {
        Feed ret = new Feed();
        Object[] o = content.get(position);
        ret.id = (Integer) o[POS_ID];
        ret.title = (String) o[POS_TITLE];
        ret.unread = (Integer) o[POS_UNREAD];
        return ret;
    }
    
    private int getImage(boolean unread) {
        if (unread) {
            return R.drawable.feedheadlinesunread48;
        } else {
            return R.drawable.feedheadlinesread48;
        }
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= getCount() || position < 0)
            return new View(context);
        
        Feed f = (Feed) getItem(position);
        
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout layout = null;
        if (convertView == null) {
            layout = (LinearLayout) inflater.inflate(R.layout.feeditem, null);
        } else {
            if (convertView instanceof LinearLayout) {
                layout = (LinearLayout) convertView;
            }
        }
        
        if (layout == null)
            return new View(context);
        
        ImageView icon = (ImageView) layout.findViewById(R.id.feed_icon);
        icon.setImageResource(getImage(f.unread > 0));
        
        TextView title = (TextView) layout.findViewById(R.id.feed_title);
        title.setText(formatEntryTitle(f.title, f.unread));
        if (f.unread > 0) {
            title.setTypeface(Typeface.DEFAULT_BOLD, 1);
        } else {
            title.setTypeface(Typeface.DEFAULT, 0);
        }
        
        return layout;
    }
    
    protected ArrayList<Object[]> executeQuery(boolean overrideDisplayUnread, boolean buildSafeQuery) {
        
        StringBuilder query = new StringBuilder();
        
        String lastOpenedFeedsList = Utils.separateItems(Controller.getInstance().lastOpenedFeeds, ",");
        
        boolean displayUnread = Controller.getInstance().onlyUnread();
        boolean invertSortFeedCats = Controller.getInstance().invertSortFeedscats();
        
        if (overrideDisplayUnread)
            displayUnread = false;
        
        if (lastOpenedFeedsList.length() > 0 && !buildSafeQuery) {
            query.append("SELECT id,title,unread FROM (");
        }
        
        query.append("SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_FEEDS);
        query.append(" WHERE categoryId=");
        query.append(categoryId);
        query.append(displayUnread ? " AND unread>0" : "");
        
        if (lastOpenedFeedsList.length() > 0 && !buildSafeQuery) {
            query.append(" UNION SELECT id,title,unread");
            query.append(" FROM feeds WHERE id IN (");
            query.append(lastOpenedFeedsList);
            query.append(" ))");
        }
        
        query.append(" ORDER BY UPPER(title) ");
        query.append(invertSortFeedCats ? "DESC" : "ASC");
        query.append(buildSafeQuery ? " LIMIT 200" : " LIMIT 1000");
        
        return DBHelper.getInstance().queryFeeds(query.toString());
    }
    
}
