/*
 * Copyright (c) 2015, Nils Braden
 *
 * This file is part of ttrss-reader-fork. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; If
 * not, see http://www.gnu.org/licenses/.
 */

package org.ttrssreader.gui.fragments;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.interfaces.IDataChangedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.gui.view.MyGestureDetector;
import org.ttrssreader.model.MainAdapter;
import org.ttrssreader.utils.AsyncTask;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

public abstract class MainListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    @SuppressWarnings("unused")
    private static final String TAG = MainListFragment.class.getSimpleName();

    protected static final int TYPE_CAT_ID = 1;
    protected static final int TYPE_FEED_ID = 2;
    protected static final int TYPE_HEADLINE_ID = 3;

    private static final String SELECTED_INDEX = "selectedIndex";
    private static final int SELECTED_INDEX_DEFAULT = Integer.MIN_VALUE;
    private static final String SELECTED_ID = "selectedId";
    private static final int SELECTED_ID_DEFAULT = Integer.MIN_VALUE;

    protected int selectedId = SELECTED_ID_DEFAULT;
    private int scrollPosition;

    protected MainAdapter adapter = null;
    protected GestureDetector gestureDetector;
    protected View.OnTouchListener gestureListener;

    protected String title;
    protected int unreadCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Async update of title und unread data:
        updateTitleAndUnread();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        int[] attrs = new int[]{android.R.attr.windowBackground};
        TypedArray ta = getActivity().obtainStyledAttributes(attrs);
        Drawable drawableFromTheme = ta.getDrawable(0);
        ta.recycle();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            view.setBackgroundDrawable(drawableFromTheme);
        else
            view.setBackground(drawableFromTheme);

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);

        setListAdapter(adapter);

        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                setChecked(selectedId);
                super.onChanged();
            }
        });

        getListView().setSelector(R.drawable.list_item_background);
        registerForContextMenu(getListView());

        ActionBar actionBar = getActivity().getActionBar();

        gestureDetector = new GestureDetector(getActivity(), new MyGestureDetector(actionBar, Controller.getInstance()
                .hideActionbar()), null);
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event) || v.performClick();
            }
        };
        getListView().setOnTouchListener(gestureListener);

        // Read the selected list item after orientation changes and similar
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        if (instance != null) {
            int selectedIndex = instance.getInt(SELECTED_INDEX, SELECTED_INDEX_DEFAULT);
            selectedId = adapter.getId(selectedIndex);
            setChecked(selectedId);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_ID, selectedId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        super.onStop();
        getListView().setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        getListView().setVisibility(View.VISIBLE);
        getListView().setSelectionFromTop(scrollPosition, 0);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        scrollPosition = getListView().getFirstVisiblePosition();
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
        if (adapter != null) {
            for (int item : adapter.getIds()) {
                pos++;
                if (item == id) {
                    getListView().setItemChecked(pos, true);
                    getListView().smoothScrollToPosition(pos);
                    return;
                }
            }
        }
        if (getListView() != null) {
            // Nothing found, uncheck everything:
            getListView().setItemChecked(getListView().getCheckedItemPosition(), false);
        }
    }

    public void doRefresh() {
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

    public void setSelectedId(int selectedId) {
        this.selectedId = selectedId;
        setChecked(selectedId);
    }

    /**
     * Updates in here are started asynchronously since the DB is accessed. When the children are done with the updates
     * we call {@link IDataChangedListener#dataLoadingFinished()} in the UI-thread again.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        updateTitleAndUnread();
        adapter.notifyDataSetChanged();
    }

    private volatile Boolean updateTitleAndUnreadRunning = false;

    private void updateTitleAndUnread() {
        if (!updateTitleAndUnreadRunning) {
            updateTitleAndUnreadRunning = true;
            new AsyncTask<Void, Void, Void>() {
                protected Void doInBackground(Void... params) {
                    fetchOtherData();
                    return null;
                }

                protected void onPostExecute(Void result) {
                    if (getActivity() instanceof IDataChangedListener)
                        ((IDataChangedListener) getActivity()).dataLoadingFinished();
                    updateTitleAndUnreadRunning = false;
                }

                ;
            }.execute();
        }
    }

    protected abstract void fetchOtherData();

}
