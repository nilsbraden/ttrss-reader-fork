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

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.gui.view.MyGestureDetector;
import org.ttrssreader.model.MainAdapter;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public abstract class MainListFragment extends ListFragment {
    
    protected static final String SELECTED_INDEX = "selectedIndex";
    protected static final int SELECTED_INDEX_DEFAULT = -1;
    
    private int selectedIndex = SELECTED_INDEX_DEFAULT;
    private int selectedIndexOld = SELECTED_INDEX_DEFAULT;
    
    protected MainAdapter adapter = null;
    
    private ListView listView;
    private int scrollPosition;
    
    protected GestureDetector gestureDetector;
    protected View.OnTouchListener gestureListener;
    
    @Override
    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);
        
        listView = getListView();
        registerForContextMenu(listView);
        
        ActionBar actionBar = ((SherlockFragmentActivity) getActivity()).getSupportActionBar();
        
        gestureDetector = new GestureDetector(getActivity(), new MyGestureDetector(actionBar, Controller.getInstance()
                .hideActionbar()), null);
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
        listView.setOnTouchListener(gestureListener);
        
        // Read the selected list item after orientation changes and similar
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        if (instance != null) {
            selectedIndex = instance.getInt(SELECTED_INDEX, SELECTED_INDEX_DEFAULT);;
            listView.setItemChecked(selectedIndex, true);
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_INDEX, selectedIndex);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onStop() {
        super.onStop();
        listView.setVisibility(View.GONE);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null)
            adapter.refreshQuery();
        listView.setVisibility(View.VISIBLE);
        listView.setSelectionFromTop(scrollPosition, 0);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        scrollPosition = listView.getFirstVisiblePosition();
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        selectedIndexOld = selectedIndex;
        selectedIndex = position; // Set selected item
        listView.setItemChecked(selectedIndex, true);
        
        Activity activity = getActivity();
        if (activity instanceof IItemSelectedListener) {
            ((IItemSelectedListener) activity).itemSelected(this, selectedIndex, selectedIndexOld,
                    adapter.getId(selectedIndex));
        }
    }
    
    public void doRefresh() {
        adapter.refreshQuery();
    }
    
    public String getTitle() {
        if (adapter != null && adapter.title != null)
            return adapter.title;
        return "";
    }
    
    public int getUnread() {
        if (adapter != null && adapter.unreadCount > 0)
            return adapter.unreadCount;
        return 0;
    }
    
    public abstract TYPE getType();
    
}
