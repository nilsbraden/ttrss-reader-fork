/*
 * Tiny Tiny RSS Reader for Android
 * 
 * Copyright (C) 2009 J. Devauchelle and contributors.
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

package org.ttrssreader.gui.activities;

import java.util.ArrayList;
import java.util.List;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.DataController;
import org.ttrssreader.gui.IRefreshEndListener;
import org.ttrssreader.gui.IUpdateEndListener;
import org.ttrssreader.model.ReadStateUpdater;
import org.ttrssreader.model.Refresher;
import org.ttrssreader.model.Updater;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.feedheadline.FeedHeadlineListAdapter;
import org.ttrssreader.utils.Utils;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.GestureDetector.OnGestureListener;
import android.widget.ListView;

public class FeedHeadlineListActivity extends ListActivity implements IRefreshEndListener, IUpdateEndListener {
	
	private static final int ACTIVITY_SHOW_FEED_ITEM = 0;
	
	private static final int MENU_REFRESH = Menu.FIRST;
	private static final int MENU_MARK_ALL_READ = Menu.FIRST + 1;
	private static final int MENU_MARK_ALL_UNREAD = Menu.FIRST + 2;
	private static final int MENU_DISPLAY_ONLY_UNREAD = Menu.FIRST + 3;
	
	public static final String FEED_ID = "FEED_ID";
	public static final String FEED_TITLE = "FEED_TITLE";
	public static final String FEED_LIST = "FEED_LIST";
	public static final String FEED_LIST_NAMES = "FEED_LIST_NAMES";
	
	private String mFeedId;
	private String mFeedTitle;
	private ArrayList<String> mFeedIds;
	private ArrayList<String> mFeedNames;
	private boolean useSwipe;
	private GestureDetector mGestureDetector;
	
	private ListView mFeedHeadlineListView;
	private FeedHeadlineListAdapter mAdapter = null;
	private Refresher refresher;
	private Updater updater;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.feedheadlinelist);

        Controller.getInstance().checkAndInitializeController(this);
        DBHelper.getInstance().checkAndInitializeController(this);
        DataController.getInstance().checkAndInitializeController(this);
        
		setProgressBarIndeterminateVisibility(false);
		mFeedHeadlineListView = getListView();
		
		useSwipe = Controller.getInstance().isUseSwipe();
		mGestureDetector = new GestureDetector(onGestureListener);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mFeedId = extras.getString(FEED_ID);
			mFeedTitle = extras.getString(FEED_TITLE);
			mFeedIds = extras.getStringArrayList(FEED_LIST);
			mFeedNames = extras.getStringArrayList(FEED_LIST_NAMES);
		} else if (savedInstanceState != null) {
			mFeedId = savedInstanceState.getString(FEED_ID);
			mFeedTitle = savedInstanceState.getString(FEED_TITLE);
			mFeedIds = savedInstanceState.getStringArrayList(FEED_LIST);
			mFeedNames = savedInstanceState.getStringArrayList(FEED_LIST_NAMES);
		} else {
			mFeedId = "-1";
			mFeedTitle = null;
			mFeedIds = null;
			mFeedNames = null;
		}
		
		mAdapter = new FeedHeadlineListAdapter(this, mFeedId);
		mFeedHeadlineListView.setAdapter(mAdapter);
		updater = new Updater(this, mAdapter);

		new Handler().postDelayed(new Runnable() {
			public void run() {
				setProgressBarIndeterminateVisibility(true);
				updater.execute();
			}
		}, Utils.WAIT);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		doRefresh();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (refresher != null) refresher.cancel(true);
		if (updater != null) updater.cancel(true);
	}
	
	private void doRefresh() {
		setProgressBarIndeterminateVisibility(true);
		
		if (mAdapter == null) {
			mAdapter = new FeedHeadlineListAdapter(this, mFeedId);
			mFeedHeadlineListView.setAdapter(mAdapter);
		}
		
		refresher = new Refresher(this, mAdapter);
		refresher.execute();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		Intent i = new Intent(this, ArticleActivity.class);
		i.putExtra(ArticleActivity.ARTICLE_ID, mAdapter.getFeedItemId(position));
		i.putExtra(ArticleActivity.FEED_ID, mFeedId);
		i.putStringArrayListExtra(ArticleActivity.ARTICLE_LIST, mAdapter.getFeedItemIds());
		
		startActivityForResult(i, ACTIVITY_SHOW_FEED_ITEM);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuItem item;
		
		item = menu.add(0, MENU_REFRESH, 0, R.string.Main_RefreshMenu);
		item.setIcon(R.drawable.refresh32);
		
		item = menu.add(0, MENU_MARK_ALL_READ, 0, R.string.FeedHeadlinesListActivity_MarkAllRead);
		
		item = menu.add(0, MENU_MARK_ALL_UNREAD, 0, R.string.FeedHeadlinesListActivity_MarkAllUnread);
		
		item = menu.add(0, MENU_DISPLAY_ONLY_UNREAD, 0, R.string.Commons_DisplayOnlyUnread);
		
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
			case MENU_REFRESH:
				doForceRefresh();
				return true;
			case MENU_MARK_ALL_READ:
				setReadState();
				return true;
			case MENU_MARK_ALL_UNREAD:
				setUnreadState();
				return true;
			case MENU_DISPLAY_ONLY_UNREAD:
				displayOnlyUnreadSwitch();
				return true;
		}
		
		return super.onMenuItemSelected(featureId, item);
	}
	
	private void doForceRefresh() {
		if (!Controller.getInstance().isWorkOffline()) {
			DataController.getInstance().forceFullRefresh();
		}
		doRefresh();
	}
	
	private void setReadState() {
		setProgressBarIndeterminateVisibility(true);
		new Updater(this, new ReadStateUpdater(mAdapter.getArticles(), mFeedId, 0)).execute();
	}
	
	private void setUnreadState() {
		setProgressBarIndeterminateVisibility(true);
		new Updater(this, new ReadStateUpdater(mAdapter.getArticles(), mFeedId, 1)).execute();
	}
	
	private void displayOnlyUnreadSwitch() {
		boolean displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnread();
		Controller.getInstance().setDisplayOnlyUnread(!displayOnlyUnread);
		doRefresh();
	}
	
	private void openNextFeed() {
		int index = mFeedIds.indexOf(mFeedId) + 1;
		
		// No more feeds in this direction
		if (index < 0 || index >= mFeedIds.size()) {
			if (Controller.getInstance().isVibrateOnLastArticle()) {
				Log.i(Utils.TAG, "No more feeds, vibrate..");
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(Utils.SHORT_VIBRATE);
			}
			return;
		}
		
		Intent i = new Intent(this, FeedHeadlineListActivity.class);
		i.putExtra(FeedHeadlineListActivity.FEED_ID, mFeedIds.get(index));
		i.putExtra(FeedHeadlineListActivity.FEED_TITLE, mFeedNames.get(index));
		i.putStringArrayListExtra(FeedHeadlineListActivity.FEED_LIST, mFeedIds);
		i.putStringArrayListExtra(FeedHeadlineListActivity.FEED_LIST_NAMES, mFeedNames);
		
		startActivityForResult(i, 0);
		super.finish();
		this.finish();
	}
	
	private void openPreviousFeed() {
		int index = mFeedIds.indexOf(mFeedId) - 1;
		
		// No more feeds in this direction
		if (index < 0 || index >= mFeedIds.size()) {
			if (Controller.getInstance().isVibrateOnLastArticle()) {
				Log.i(Utils.TAG, "No more feeds, vibrate..");
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(Utils.SHORT_VIBRATE);
			}
			return;
		}
		
		Intent i = new Intent(this, FeedHeadlineListActivity.class);
		i.putExtra(FeedHeadlineListActivity.FEED_ID, mFeedIds.get(index));
		i.putExtra(FeedHeadlineListActivity.FEED_TITLE, mFeedNames.get(index));
		i.putStringArrayListExtra(FeedHeadlineListActivity.FEED_LIST, mFeedIds);
		i.putStringArrayListExtra(FeedHeadlineListActivity.FEED_LIST_NAMES, mFeedNames);
		
		startActivityForResult(i, 0);
		super.finish();
		this.finish();
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent e) {
		super.dispatchTouchEvent(e);
		return mGestureDetector.onTouchEvent(e);
	}
	
	private OnGestureListener onGestureListener = new OnGestureListener() {
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			
			if (!useSwipe) {
				// Swiping is disabled in preferences
				return false;
			}
			
			int dx = (int) (e2.getX() - e1.getX());
			int dy = (int) (e2.getY() - e1.getY());
			
			if (Math.abs(dy) > 60) {
				// Too much Y-Movement
				return false;
			}
			
			// don't accept the fling if it's too short as it may conflict with a button push
			if (Math.abs(dx) > 80 && Math.abs(velocityX) > Math.abs(velocityY)) {
				
				Log.d(Utils.TAG, "Fling: (" + e1.getX() + " " + e1.getY() + ")(" + e2.getX() + " " + e2.getY() +
						") dx: " + dx + " dy: " + dy + " (Direction: " + ((velocityX > 0) ? "right" : "left"));
				
				if (velocityX > 0) {
					openPreviousFeed();
				} else {
					openNextFeed();
				}
				return true;
			}
			return false;
		}
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			return false;
		}
		
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}
		
		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}
		
		@Override
		public void onLongPress(MotionEvent e) {
		}
		
		@Override
		public void onShowPress(MotionEvent e) {
		}
	};
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (Controller.getInstance().isUseVolumeKeys()) {
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
				openNextFeed();
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
				openPreviousFeed();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (Controller.getInstance().isUseVolumeKeys()) {
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}
	
	private void openConnectionErrorDialog(String errorMessage) {
		Intent i = new Intent(this, ConnectionErrorActivity.class);
		i.putExtra(ConnectionErrorActivity.ERROR_MESSAGE, errorMessage);
		startActivity(i);
	}
	
	@Override
	public void onRefreshEnd() {
		if (!Controller.getInstance().getTTRSSConnector().hasLastError()) {
			
			try {
				List<ArticleItem> list = new ArrayList<ArticleItem>();
				for (Object f : refresher.get()) {
					list.add((ArticleItem)f);
				}
				mAdapter.setArticles(list);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			mAdapter.notifyDataSetChanged();
			
			if (mFeedTitle != null) {
			    this.setTitle(mFeedTitle + " (" + mAdapter.getUnreadCount() + ")");
			} else {
				this.setTitle(this.getResources().getString(R.string.ApplicationName) + " ("
						+ mAdapter.getUnreadCount() + ")");
			}
		} else {
			openConnectionErrorDialog(Controller.getInstance().getTTRSSConnector().getLastError());
		}
		
		if (updater != null) {
            if (updater.getStatus().equals(Status.FINISHED)) {
                setProgressBarIndeterminateVisibility(false);
            }
        } else {
            setProgressBarIndeterminateVisibility(false);
        }
	}
	
	@Override
	public void onUpdateEnd() {
		if (!Controller.getInstance().getTTRSSConnector().hasLastError()) {
			mAdapter.notifyDataSetChanged();
		} else {
			openConnectionErrorDialog(Controller.getInstance().getTTRSSConnector().getLastError());
		}
		
		doRefresh();
	}
	
}
