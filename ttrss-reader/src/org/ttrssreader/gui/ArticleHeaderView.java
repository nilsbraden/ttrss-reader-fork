/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
 * Copyright (C) The Developers from K-9 Mail (https://code.google.com/p/k9mail/)
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

package org.ttrssreader.gui;

import java.text.DateFormat;
import java.util.Date;
import org.ttrssreader.R;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.updaters.StarredStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Copied and modified for my purpose, originally developed for K-9 Mail. Source:
 * https://github.com/k9mail/k-9/blob/master/src/com/fsck/k9/view/MessageHeader.java
 * 
 * @author https://code.google.com/p/k9mail/
 * 
 */
public class ArticleHeaderView extends LinearLayout {
    private TextView feedView;
    private TextView dateView;
    private TextView timeView;
    private TextView titleView;
    private DateFormat dateFormat;
    private DateFormat timeFormat;
    
    private CheckBox starred;
    
    private Article article;
    
    public ArticleHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.timeFormat = android.text.format.DateFormat.getTimeFormat(context); // 12/24 date format
        this.dateFormat = android.text.format.DateFormat.getDateFormat(context);
        this.setBackgroundColor(Color.WHITE);
    }
    
    private void initializeLayout() {
        feedView = (TextView) findViewById(R.id.feed);
        feedView.setTextColor(Color.BLACK);
        titleView = (TextView) findViewById(R.id.title);
        titleView.setTextColor(Color.BLACK);
        dateView = (TextView) findViewById(R.id.date);
        dateView.setTextColor(Color.BLACK);
        timeView = (TextView) findViewById(R.id.time);
        timeView.setTextColor(Color.BLACK);
        starred = (CheckBox) findViewById(R.id.starred);
        
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                
                return;
            }
        });
        
        feedView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                
            }
        });
        
        starred.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new Updater(null, new StarredStateUpdater(article, article.isStarred ? 0 : 1)).execute();
            }
        });
    }
    
    public void populate(Article article) {
        this.article = article;
        this.initializeLayout();
        
        Feed feed = DBHelper.getInstance().getFeed(article.feedId);
        if (feed != null) {
            feedView.setText(feed.title);
        }
        
        titleView.setText(article.title);
        
        Date updated = article.updated;
        dateView.setText(dateFormat.format(updated));
        timeView.setText(timeFormat.format(updated));
        starred.setChecked(article.isStarred);
    }
    
}
