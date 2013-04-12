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

package org.ttrssreader.gui.view;

import java.util.Date;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.updaters.StarredStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.DateUtils;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * Copied and modified for my purpose, originally developed for K-9 Mail. Source:
 * https://github.com/k9mail/k-9/blob/master/src/com/fsck/k9/view/MessageHeader.java
 * 
 * @author https://code.google.com/p/k9mail/
 * 
 */
public class HeaderFragment extends Fragment {
    
    private TextView feedView;
    private TextView dateView;
    private TextView timeView;
    private TextView titleView;
    
    private CheckBox starred;
    
    private Article article;
    
    private Activity activity;
    
    public static HeaderFragment getInstance() {
        HeaderFragment fragment = new HeaderFragment();
        return fragment;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.articleheader, container, false);
    }
    
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = getActivity();
    }
    
    private void initializeLayout() {
        getView().setBackgroundColor(Color.WHITE);
        
        feedView = (TextView) getActivity().findViewById(R.id.feed);
        feedView.setTextColor(Color.BLACK);
        titleView = (TextView) getActivity().findViewById(R.id.title);
        titleView.setTextColor(Color.BLACK);
        titleView.setTextSize(Controller.getInstance().headlineSize()); // Read Text-Size for the title from prefs.
        dateView = (TextView) getActivity().findViewById(R.id.date);
        dateView.setTextColor(Color.BLACK);
        timeView = (TextView) getActivity().findViewById(R.id.time);
        timeView.setTextColor(Color.BLACK);
        
        starred = (CheckBox) getActivity().findViewById(R.id.starred);
        starred.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new Updater(null, new StarredStateUpdater(article, article.isStarred ? 0 : 1)).exec();
            }
        });
    }
    
    public void populate(Article article) {
        this.article = article;
        initializeLayout();
        if (article == null)
            return;
        
        Feed feed = DBHelper.getInstance().getFeed(article.feedId);
        if (feed != null)
            feedView.setText(feed.title);
        
        titleView.setText(article.title);
        
        Date updated = article.updated;
        dateView.setText(DateUtils.getDate(activity, updated));
        timeView.setText(DateUtils.getTime(activity, updated));
        starred.setChecked(article.isStarred);
    }
    
}
