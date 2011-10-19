/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
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

package org.ttrssreader.gui.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class FeedHeadlineListFragment extends ItemListFragment {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setListAdapter(ArrayAdapter.createFromResource(getActivity().getApplicationContext(), R.array.tut_titles,
        // R.layout.list_item));
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // String[] links = getResources().getStringArray(R.array.tut_links);
        
        // String content = links[position];
        // Intent showContent = new Intent(getActivity().getApplicationContext(), TutViewerActivity.class);
        // showContent.setData(Uri.parse(content));
        // startActivity(showContent);
    }
}
