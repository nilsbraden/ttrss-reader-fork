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

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Activity;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.interfaces.IDataChangedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.gui.view.MyGestureListener;
import org.ttrssreader.model.MainAdapter;
import org.ttrssreader.utils.AsyncTask;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

public abstract class MainListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	@SuppressWarnings("unused")
	private static final String TAG = MainListFragment.class.getSimpleName();

	protected static final int TYPE_CAT_ID = 1;
	protected static final int TYPE_FEED_ID = 2;
	protected static final int TYPE_HEADLINE_ID = 3;

	private static final String SELECTED_ID = "selectedId";
	private static final int SELECTED_ID_DEFAULT = Integer.MIN_VALUE;

	protected int selectedId = SELECTED_ID_DEFAULT;
	private int scrollPosition;

	protected MainAdapter adapter = null;
	protected GestureDetector gestureDetector;
	protected View.OnTouchListener gestureListener;

	protected String title;
	protected int unreadCount;
	protected byte[] icon;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Async update of title und unread data:
		updateTitleAndUnread();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.item_list, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		int[] attrs = new int[]{android.R.attr.windowBackground};
		Activity activity = getActivity();
		if (activity != null) {
			TypedArray ta = activity.obtainStyledAttributes(attrs);
			Drawable drawableFromTheme = ta.getDrawable(0);
			ta.recycle();
			view.setBackground(drawableFromTheme);
		}

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

		registerForContextMenu(getListView());

		Activity activity = getActivity();
		if (activity != null) {
			ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

			MyGestureListener gestureListener = new MyGestureListener(actionBar, Controller.getInstance().hideActionbar(), getActivity());
			gestureDetector = new GestureDetector(getActivity(), gestureListener, null);
			this.gestureListener = (v, event) -> gestureDetector.onTouchEvent(event) || v.performClick();
			getListView().setOnTouchListener(this.gestureListener);
		}

		// Read the selected list item after orientation changes and similar
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
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
	public void onListItemClick(@NonNull ListView listview, @NonNull View view, int position, long id) {
		selectedId = adapter.getId(position);

		setChecked(selectedId);

		Activity activity = getActivity();
		if (activity instanceof IItemSelectedListener) {
			((IItemSelectedListener) activity).itemSelected(this, selectedId);
		}
	}

	private void setChecked(int id) {
		// Return if data hasn't been retrieved or content view has not been created yet
		if (adapter == null || getView() == null)
			return;

		int pos = -1;
		for (int item : adapter.getIds()) {
			pos++;
			if (item == id) {
				getListView().setItemChecked(pos, true);
				getListView().smoothScrollToPosition(pos);
				return;
			}
		}
		// Nothing found, uncheck everything:
		getListView().setItemChecked(getListView().getCheckedItemPosition(), false);
	}

	public void doRefresh() {
		if (adapter != null)
			adapter.notifyDataSetChanged();
	}

	public String getTitle() {
		return title != null ? title : "";
	}

	public int getUnread() {
		return Math.max(unreadCount, 0);
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
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
		adapter.notifyDataSetChanged();
		updateTitleAndUnread();
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
			}.execute();
		}
	}

	protected abstract void fetchOtherData();

	@Override
	public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
		if (Controller.sFragmentAnimationDirection != 0 && Controller.getInstance().animations()) {
			Animator a;
			if (Controller.sFragmentAnimationDirection > 0)
				a = AnimatorInflater.loadAnimator(getActivity(), R.animator.slide_out_left);
			else
				a = AnimatorInflater.loadAnimator(getActivity(), R.animator.slide_out_right);

			// Reset:
			Controller.sFragmentAnimationDirection = 0;
			return a;
		}
		return super.onCreateAnimator(transit, enter, nextAnim);
	}

	/**
	 * Needed to determine if we need to read the title from this fragment or use the generic title from the
	 * CategoryFragment instead.
	 */
	public boolean isEmptyPlaceholder() {
		return false;
	}

}
