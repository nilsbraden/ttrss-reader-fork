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

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.MenuActivity;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.gui.view.MyGestureDetector;
import org.ttrssreader.model.MainAdapter;
import org.ttrssreader.utils.Utils;
import android.app.Activity;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public abstract class MainListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    
    protected static final int TYPE_CAT_ID = 1;
    protected static final int TYPE_FEED_ID = 2;
    protected static final int TYPE_HEADLINE_ID = 3;
    
    protected static final String SELECTED_INDEX = "selectedIndex";
    protected static final int SELECTED_INDEX_DEFAULT = Integer.MIN_VALUE;
    protected static final String SELECTED_ID = "selectedId";
    protected static final int SELECTED_ID_DEFAULT = Integer.MIN_VALUE;
    
    private int selectedId = SELECTED_ID_DEFAULT;
    
    protected MainAdapter adapter = null;
    
    private ListView listView;
    private int scrollPosition;
    
    protected GestureDetector gestureDetector;
    protected View.OnTouchListener gestureListener;
    
    protected String title;
    protected int unreadCount;
    
    @Override
    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);
        
        listView = getListView();
        listView.setSelector(R.drawable.list_item_background);
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
            int selectedIndex = instance.getInt(SELECTED_INDEX, SELECTED_INDEX_DEFAULT);
            selectedId = adapter.getId(selectedIndex);
            
            setChecked(selectedId);
        }
        
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                setChecked(selectedId);
                super.onChanged();
            }
        });
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_ID, selectedId);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onStop() {
        super.onStop();
        listView.setVisibility(View.GONE);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void onResume() {
        super.onResume();
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
        int selectedIndex = position; // Set selected item
        selectedId = adapter.getId(selectedIndex);
        
        setChecked(selectedId);
        
        Activity activity = getActivity();
        if (activity instanceof IItemSelectedListener) {
            ((IItemSelectedListener) activity).itemSelected(this, selectedIndex, selectedId);
        }
    }
    
    private void setChecked(int id) {
        int pos = -1;
        for (int item : adapter.getIds()) {
            pos++;
            if (item == id) {
                listView.setItemChecked(pos, true);
                listView.smoothScrollToPosition(pos);
                return;
            }
        }
        // Nothing found, uncheck everything:
        listView.setItemChecked(listView.getCheckedItemPosition(), false);
    }
    
    public void doRefresh() {
        // if (adapter != null)
        // adapter.refreshQuery();
        Log.d(Utils.TAG, "doRefresh...");
        adapter.notifyDataSetChanged();
    }
    
    public String getTitle() {
        if (title != null)
            return title;
        return "";
    }
    
    public int getUnread() {
        if (unreadCount > 0)
            return unreadCount;
        return 0;
    }
    
    public abstract TYPE getType();
    
    public static Fragment recreateFragment(FragmentManager fm, Fragment f) {
        try {
            Fragment.SavedState savedState = fm.saveFragmentInstanceState(f);
            Fragment newInstance = f.getClass().newInstance();
            newInstance.setInitialSavedState(savedState);
            return newInstance;
        } catch (Exception e) {
            Log.e(Utils.TAG, "Error while recreating Fragment-Instance...", e);
            return null;
        }
    }
    
    public void setSelectedId(int selectedId) {
        this.selectedId = selectedId;
        setChecked(selectedId);
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        fetchOtherData();
        if (getActivity() instanceof MenuActivity)
            ((MenuActivity) getActivity()).dataChanged();
    }
    
    protected abstract void fetchOtherData();
    
}
