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
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DataController;
import org.ttrssreader.gui.IRefreshEndListener;
import org.ttrssreader.gui.IUpdateEndListener;
import org.ttrssreader.model.ReadStateUpdater;
import org.ttrssreader.model.Refresher;
import org.ttrssreader.model.Updater;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.article.ArticleItemAdapter;
import org.ttrssreader.utils.Utils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.GestureDetector.OnGestureListener;
import android.webkit.WebView;
import android.widget.TextView;

public class ArticleActivity extends Activity implements IRefreshEndListener, IUpdateEndListener {
	
	public static final String ARTICLE_ID = "ARTICLE_ID";
	public static final String FEED_ID = "FEED_ID";
	public static final String ARTICLE_LIST = "ARTICLE_LIST";
	
	public static final long SHORT_VIBRATE = 50;
	
	private static final int MENU_MARK_READ = Menu.FIRST;
	private static final int MENU_MARK_UNREAD = Menu.FIRST + 1;
	private static final int MENU_OPEN_LINK = Menu.FIRST + 2;
	private static final int MENU_OPEN_COMMENT_LINK = Menu.FIRST + 3;
	private static final int MENU_SHARE_LINK = Menu.FIRST + 6;
	
	private String mArticleId;
	private String mFeedId;
	private ArrayList<String> mArticleIds;
	
	private ArticleItem mArticleItem = null;
	
	private ArticleItemAdapter mAdapter = null;
	
	private WebView webview;
	private TextView webviewSwipeText;
	private GestureDetector mGestureDetector;
	private boolean useSwipe;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.articleitem);
		
		setProgressBarIndeterminateVisibility(false);		

		webview = (WebView) findViewById(R.id.webview);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.getSettings().setBuiltInZoomControls(true);
		mGestureDetector = new GestureDetector(onGestureListener);
		findViewById(R.layout.articleitem);
		
		webviewSwipeText = (TextView) findViewById(R.id.webview_swipe_text);
		webviewSwipeText.setVisibility(TextView.INVISIBLE);
		useSwipe = Controller.getInstance().isUseSwipe();

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mArticleId = extras.getString(ARTICLE_ID);
			mFeedId = extras.getString(FEED_ID);
			mArticleIds = extras.getStringArrayList(ARTICLE_LIST);
		} else if (savedInstanceState != null) {
			mArticleId = savedInstanceState.getString(ARTICLE_ID);
			mFeedId = savedInstanceState.getString(FEED_ID);
			mArticleIds = savedInstanceState.getStringArrayList(ARTICLE_LIST);
		} else {
			mArticleId = "-1";
			mFeedId = "-1";
			mArticleIds = new ArrayList<String>();
		}
	}
	
	@Override
	protected void onResume() {
		doRefresh();
		super.onResume();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuItem item;
		
		item = menu.add(0, MENU_MARK_READ, 0, R.string.ArticleActivity_MarkRead);
		
		item = menu.add(0, MENU_MARK_UNREAD, 0, R.string.ArticleActivity_MarkUnread);
		
		item = menu.add(0, MENU_OPEN_LINK, 0, R.string.ArticleActivity_OpenLink);
		item.setIcon(R.drawable.link32);
		
		item = menu.add(0, MENU_OPEN_COMMENT_LINK, 0, R.string.ArticleActivity_OpenCommentLink);
		item.setIcon(R.drawable.commentlink32);
		
		item = menu.add(0, MENU_SHARE_LINK, 0, R.string.ArticleActivity_ShareLink);
		
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
			case MENU_MARK_READ:
				markRead();
				return true;
			case MENU_OPEN_LINK:
				openLink();
				return true;
			case MENU_OPEN_COMMENT_LINK:
				openCommentLink();
				return true;
			case MENU_SHARE_LINK:
				shareLink();
				return true;
		}
		
		return super.onMenuItemSelected(featureId, item);
	}
	
	private void doRefresh() {
		setProgressBarIndeterminateVisibility(true);
		
		mAdapter = new ArticleItemAdapter(mArticleId);
		new Refresher(this, mAdapter).execute();
	}
	
	private void markRead() {
		setProgressBarIndeterminateVisibility(true);
		new Updater(this, new ReadStateUpdater(mArticleItem, mFeedId, 0)).execute();
	}
	
	private void openUrl(String url) {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);
	}
	
	private void openLink() {
		if (mArticleItem != null) {
			String url = mArticleItem.getArticleUrl();
			if ((url != null) && (url.length() > 0)) {
				openUrl(url);
			}
		}
	}
	
	private void openCommentLink() {
		if (mArticleItem != null) {
			String url = mArticleItem.getArticleCommentUrl();
			if ((url != null) && (url.length() > 0)) {
				openUrl(url);
			}
		}
	}
	
	private void shareLink() {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_SUBJECT, "Link from ttrss-reader");
		i.putExtra(Intent.EXTRA_TEXT, mArticleItem.getArticleUrl());
		this.startActivity(Intent.createChooser(i, "Send link..."));
	}
	
	private void openNextNewerArticle() {
		Intent i = new Intent(this, ArticleActivity.class);
		
		int index = mArticleIds.indexOf(mArticleId) + 1;

		// No more articles in this direction
		if (index < 0 || index >= mArticleIds.size()) {
			if (Controller.getInstance().isVibrateOnLastArticle()) {
				Log.i(Utils.TAG, "No more articles, vibrate..");
				Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(SHORT_VIBRATE);
			}
			return;
		}
		
		i.putExtra(ArticleActivity.ARTICLE_ID, mArticleIds.get(index));
		i.putExtra(ArticleActivity.FEED_ID, mFeedId);
		i.putExtra(ArticleActivity.ARTICLE_LIST, mArticleIds);
		
		Log.i(Utils.TAG, "openPreviousArticle() FeedID: " + mFeedId + ", ArticleID: " + mArticleIds.get(index));
		
		startActivityForResult(i, 0);
		finish();
	}
	
	private void openNextOlderArticle() {
		Intent i = new Intent(this, ArticleActivity.class);
		
		int index = mArticleIds.indexOf(mArticleId) - 1;
		
		// No more articles in this direction
		if (index < 0 || index >= mArticleIds.size()) {
			if (Controller.getInstance().isVibrateOnLastArticle()) {
				Log.i(Utils.TAG, "No more articles, vibrate..");
				Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(SHORT_VIBRATE);
			}
			return;
		}
		
		i.putExtra(ArticleActivity.ARTICLE_ID, mArticleIds.get(index));
		i.putExtra(ArticleActivity.FEED_ID, mFeedId);
		i.putExtra(ArticleActivity.ARTICLE_LIST, mArticleIds);
		
		Log.i(Utils.TAG, "openNextArticle() FeedID: " + mFeedId + ", ArticleID: " + mArticleIds.get(index));
		
		startActivityForResult(i, 0);
		finish();
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent e){
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
			
			// Define SWIPE_AREA to be in the bottom and have a height of 80px
			int SWIPE_HEIGHT = 80;
			int SWIPE_BOTTOM = webview.getHeight() - SWIPE_HEIGHT;
			
			int dx = (int) (e2.getX() - e1.getX());
			int dy = (int) (e2.getY() - e1.getY());
			
			if (Math.abs(dy) > 60) {
				// Too much Y-Movement or
				return false;
			} else if (e1.getY() < SWIPE_BOTTOM || e2.getY() < SWIPE_BOTTOM) {
				
				// Only accept swipe in SWIPE_AREA so we can use scrolling as usual
				if (Math.abs(dx) > 80 && Math.abs(velocityX) > Math.abs(velocityY)) {
					
					// Display text for swipe-area
					webviewSwipeText.setVisibility(TextView.VISIBLE);
					new Handler().postDelayed(timerTask, 1000);
				}
				return false;
			}
			
			
			// don't accept the fling if it's too short as it may conflict with a button push
			if (Math.abs(dx) > 80 && Math.abs(velocityX) > Math.abs(velocityY)) {
				
				Log.d(Utils.TAG, "Fling: (" + e1.getX() + " " + e1.getY() + ")(" + e2.getX() + " " + e2.getY() + 
						") dx: " + dx + " dy: " + dy + " (Direction: " + ((velocityX > 0) ? "right" : "left"));
				
				if (velocityX > 0) {
					Log.d(Utils.TAG, "Fling right");
					openNextOlderArticle();
				} else {
					Log.d(Utils.TAG, "Fling left");
					openNextNewerArticle();
				}
				return true;
			}
			return false;
		}
		
		// I need this to set the text invisible after some time 
		private Runnable timerTask = new Runnable() {
			public void run() {
				webviewSwipeText.setVisibility(TextView.INVISIBLE);
			}
		}; 

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { return false; }
		@Override
		public boolean onSingleTapUp(MotionEvent e) { return false; }
		@Override
		public boolean onDown(MotionEvent e) { return false; }
		@Override
		public void onLongPress(MotionEvent e) { }
		@Override
		public void onShowPress(MotionEvent e) { }
	};
	
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Intercept the volume-key-events if preference is set
		if (Controller.getInstance().isUseVolumeKeys()) {
		    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
		    	openNextNewerArticle();
		        return true;
		    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
		    	openNextOlderArticle();
		        return true;
		    }
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// Intercept the volume-key-events if preference is set
		if (Controller.getInstance().isUseVolumeKeys()) {
		    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
		        return true;
		    }
		    else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
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
			mArticleItem = mAdapter.getArticle();
			
			if (mArticleItem != null) {
				// Use if loadDataWithBaseURL, 'cause loadData is buggy (encoding error & don't support "%" in html).
				webview.loadDataWithBaseURL(null, mArticleItem.getContent(), "text/html", "utf-8", "about:blank");
				
				if (mArticleItem.getTitle() != null) {
					this.setTitle(this.getResources().getString(R.string.ApplicationName) + " - "
							+ mArticleItem.getTitle());
				} else {
					this.setTitle(this.getResources().getString(R.string.ApplicationName));
				}
				
				// TODO: FIXTHIS
				if ((mArticleItem.isUnread()) && (Controller.getInstance().isAutomaticMarkRead())) {
					setProgressBarIndeterminateVisibility(true);
					new Updater(this, new ReadStateUpdater(mAdapter.getArticle(), mFeedId, 0)).execute();
					mArticleItem.setUnread(false);
				}
				
				if (mArticleItem.getContent().length() < 3) {
					if (Controller.getInstance().isOpenUrlEmptyArticle()) {
						Log.i(Utils.TAG, "Article-Content is empty, opening URL in browser");
						openLink();
					} else {
						Log.i(Utils.TAG, "Article-Content is empty");
					}
				}
			}
		} else {
			openConnectionErrorDialog(Controller.getInstance().getTTRSSConnector().getLastError());
		}

		setProgressBarIndeterminateVisibility(false);
	}
	
	@Override
	public void onSubRefreshEnd() {
		setProgressBarIndeterminateVisibility(false);
		DataController.getInstance().disableForceFullRefresh();
	}
	
	@Override
	public void onUpdateEnd() {
		if (Controller.getInstance().getTTRSSConnector().hasLastError()) {
			openConnectionErrorDialog(Controller.getInstance().getTTRSSConnector().getLastError());
		}

		setProgressBarIndeterminateVisibility(false);
	}
	
}
