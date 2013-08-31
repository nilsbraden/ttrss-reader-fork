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
import org.ttrssreader.gui.MenuActivity;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.gui.interfaces.IUpdateEndListener;
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

public class MainListFragment extends ListFragment implements IUpdateEndListener {
    
    protected static final TYPE THIS_TYPE = TYPE.CATEGORY;
    
    protected static final String SELECTED_INDEX = "selectedIndex";
    protected static final int SELECTED_INDEX_DEFAULT = -1;
    
    protected int selectedIndex = SELECTED_INDEX_DEFAULT;
    protected int selectedIndexOld = SELECTED_INDEX_DEFAULT;
    
    protected MainAdapter adapter = null;
    
    private ListView listView;
    private int scrollPosition;
    
    private GestureDetector gestureDetector;
    private View.OnTouchListener gestureListener;
    
    public static MainListFragment newInstance() {
        // Create a new fragment instance
        MainListFragment detail = new MainListFragment();
        detail.setHasOptionsMenu(true);
        detail.setRetainInstance(true);
        return detail;
    }
    
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
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_INDEX, selectedIndex);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onStop() {
        super.onStop();
        getListView().setVisibility(View.GONE);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.refreshQuery();
            setTitleAfterUpdate();
        }
        getListView().setVisibility(View.VISIBLE);
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
        
        Activity activity = getActivity();
        if (activity instanceof IItemSelectedListener)
            ((IItemSelectedListener) activity).itemSelected(THIS_TYPE, selectedIndex, selectedIndexOld,
                    adapter.getId(selectedIndex));
    }
    
    @Override
    public void onUpdateEnd() {
        adapter.refreshQuery();
        setTitleAfterUpdate();
    }
    
    private void setTitleAfterUpdate() {
        MenuActivity activity = (MenuActivity) getActivity();
        if (adapter != null && activity != null) {
            if (adapter.title != null)
                activity.setTitle(adapter.title);
            if (adapter.unreadCount >= 0)
                activity.setUnread(adapter.unreadCount);
        }
    }
    
}
