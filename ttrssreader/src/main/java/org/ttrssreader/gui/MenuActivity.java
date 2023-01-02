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

package org.ttrssreader.gui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.ProgressBarManager;
import org.ttrssreader.controllers.UpdateController;
import org.ttrssreader.gui.dialogs.ErrorDialog;
import org.ttrssreader.gui.dialogs.YesNoUpdaterDialog;
import org.ttrssreader.gui.interfaces.ICacheEndListener;
import org.ttrssreader.gui.interfaces.IDataChangedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.gui.interfaces.IUpdateEndListener;
import org.ttrssreader.imageCache.ForegroundService;
import org.ttrssreader.model.updaters.IUpdatable;
import org.ttrssreader.model.updaters.StateSynchronisationUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.preferences.PreferencesActivity;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.PostMortemReportExceptionHandler;
import org.ttrssreader.utils.Utils;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import static org.ttrssreader.controllers.Controller.isTablet;

/**
 * This class provides common functionality for Activities.
 */
public abstract class MenuActivity extends MenuFlavorActivity implements IUpdateEndListener, ICacheEndListener, IItemSelectedListener, IDataChangedListener {

	private static final String TAG = MenuActivity.class.getSimpleName();

	private PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);

	protected MenuActivity activity;
	protected volatile boolean mOnSaveInstanceStateCalled = false;

	private Updater updater;
	private boolean isVertical;
	private static int minSize;
	private static int maxSize;
	private int dividerSize;
	private int displaySize;

	private View frameMain = null;
	private View divider = null;
	private View frameSub = null;
	//	private TextView header_title;
	private TextView header_unread;
	private Toolbar m_Toolbar;

	private ProgressBar progressbar;
	private ProgressBar progressspinner;

	@Override
	protected void onCreate(Bundle instance) {
		setTheme(Controller.getInstance().getThemeResource());
		Controller.getInstance().initializeThemeMode();

		super.onCreate(instance);
		mDamageReport.initialize();
		activity = this;

		Controller.getInstance().setHeadless(false);
		refreshDisplayMetrics();
		setContentView(getLayoutResource());
		initToolbar();
		initTabletLayout();
	}

	protected abstract int getLayoutResource();

	protected boolean useTabletLayout() {
		if (!Controller.getInstance().allowTabletLayout())
			return false;

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
			return Controller.displayVirtualHeight >= 800;
		else
			return Controller.displayVirtualWidth >= 1280;
	}

	protected void initTabletLayout() {
		frameMain = findViewById(R.id.frame_main);
		divider = findViewById(R.id.list_divider);
		frameSub = findViewById(R.id.frame_sub);

		if (frameMain == null || frameSub == null || divider == null)
			return; // Do nothing, the views do not exist...

		isVertical = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
		displaySize = Controller.displayWidth;
		if (isVertical) {
			TypedValue tv = new TypedValue();
			getApplicationContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
			int actionBarHeight = getResources().getDimensionPixelSize(tv.resourceId);
			displaySize = Controller.displayHeight - actionBarHeight;
		}

		minSize = (int) (displaySize * 0.05);
		maxSize = displaySize - (int) (displaySize * 0.05);

		// use tablet layout?
		isTablet = (Controller.getInstance().allowTabletLayout() && divider != null);
		Log.i(TAG, String.format("Tablet layout: %s, minSize: %s, maxSize: %s, displaySize: %s, isVertical: %s", isTablet, minSize, maxSize, displaySize, isVertical));

		// Hide divider if necessary
		if (!isTablet && divider != null) {
			divider.setVisibility(View.GONE);
			getWindow().getDecorView().getRootView().invalidate();
		}

		// Set proper frame sizes
		if (isTablet) {
			// Resize frames and do it only if stored size is within our bounds:
			int mainFrameSize = Controller.getInstance().getMainFrameSize(this, isVertical, minSize, maxSize);
			int subFrameSize = displaySize - mainFrameSize;

			RelativeLayout.LayoutParams lpMain = (RelativeLayout.LayoutParams) frameMain.getLayoutParams();
			RelativeLayout.LayoutParams lpSub = (RelativeLayout.LayoutParams) frameSub.getLayoutParams();

			if (isVertical) {
				// calculate height of divider
				int padding = divider.getPaddingTop() + divider.getPaddingBottom();
				dividerSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, padding, getApplicationContext().getResources().getDisplayMetrics()));

				// Create LayoutParams for all three views
				lpMain.height = mainFrameSize;
				lpSub.height = subFrameSize - dividerSize;
			} else {
				// calculate width of divider
				int padding = divider.getPaddingLeft() + divider.getPaddingRight();
				dividerSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, padding, getApplicationContext().getResources().getDisplayMetrics()));

				// Create LayoutParams for all three views
				lpMain.width = mainFrameSize;
				lpSub.width = subFrameSize - dividerSize;
			}

			// Set all params and visibility
			frameMain.setLayoutParams(lpMain);
			frameSub.setLayoutParams(lpSub);
			divider.setVisibility(View.VISIBLE);
			getWindow().getDecorView().getRootView().invalidate();
		}
	}

	private void refreshDisplayMetrics() {
		// Initialize values for layout changes:
		WindowManager wm = ((WindowManager) getSystemService(Context.WINDOW_SERVICE));
		if (wm != null)
			Controller.refreshDisplayMetrics(wm.getDefaultDisplay());
	}

	private void handleResize() {
		if (frameMain == null || frameSub == null)
			return;

		int mainFrameSize;
		if (isVertical) {
			mainFrameSize = calculateSize(frameMain.getHeight() + mDeltaY);
		} else {
			mainFrameSize = calculateSize(frameMain.getWidth() + mDeltaX);
		}

		int subFrameSize = displaySize - dividerSize - mainFrameSize;
		RelativeLayout.LayoutParams lpMain = (RelativeLayout.LayoutParams) frameMain.getLayoutParams();
		RelativeLayout.LayoutParams lpSub = (RelativeLayout.LayoutParams) frameSub.getLayoutParams();

		if (isVertical) {
			lpMain.height = mainFrameSize;
			lpSub.height = subFrameSize;
		} else {
			lpMain.width = mainFrameSize;
			lpSub.width = subFrameSize;
		}

		frameMain.setLayoutParams(lpMain);
		frameSub.setLayoutParams(lpSub);

		// Add all the null-checks to this line:
		//getWindow().getDecorView().getRootView().invalidate();
		// Safe version:
		Window window = getWindow();
		if (window != null) {
			View rv = window.getDecorView().getRootView();
			if (rv != null)
				rv.invalidate();
		}
	}

	private void initToolbar() {
		m_Toolbar = findViewById(R.id.toolbar);
		if (m_Toolbar != null) {
			setSupportActionBar(m_Toolbar);
			m_Toolbar.setTitle(R.string.ApplicationName);
			m_Toolbar.setVisibility(View.VISIBLE);
			header_unread = findViewById(R.id.head_unread);
			progressbar = findViewById(R.id.progressbar);
			progressspinner = findViewById(R.id.progressspinner);
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		m_Toolbar.setTitle(title);
		super.setTitle(title);
	}

	public void setUnread(int unread) {
		header_unread.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
		header_unread.setText(String.valueOf(unread));
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (Controller.getInstance().isScheduledRestart()) {
			Controller.getInstance().setScheduledRestart(false);
			Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
			if (intent != null) {
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		} else {
			UpdateController.getInstance().registerActivity(this);
			DBHelper.getInstance().initialize(this);
		}
		refreshAndUpdate();
	}

	@Override
	protected void onStop() {
		super.onStop();
		UpdateController.getInstance().unregisterActivity(this);
	}

	@Override
	protected void onDestroy() {
		mDamageReport.restoreOriginalHandler();
		mDamageReport = null;
		super.onDestroy();
		if (updater != null) {
			updater.cancel(true);
			updater = null;
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		mOnSaveInstanceStateCalled = true;
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
			case ErrorActivity.ACTIVITY_SHOW_ERROR:
			case Constants.ACTIVITY_SHOW_PREFERENCES:
				refreshAndUpdate();
				break;
			case ErrorActivity.ACTIVITY_EXIT:
				finish();
				break;
			case PreferencesActivity.ACTIVITY_RELOAD:
				finish();
				startActivity(getIntent());
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();

		// Reset the boolean flag back to false for next time.
		mOnSaveInstanceStateCalled = false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.generic, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		MenuItem offline = menu.findItem(R.id.Menu_WorkOffline);
		MenuItem refresh = menu.findItem(R.id.Menu_Refresh);
		if (offline != null) {
			if (Controller.getInstance().workOffline()) {
				offline.setTitle(getString(R.string.UsageOnlineTitle));
				offline.setIcon(R.drawable.ic_menu_play_clip);
				if (refresh != null)
					menu.findItem(R.id.Menu_Refresh).setVisible(false);
			} else {
				offline.setTitle(getString(R.string.UsageOfflineTitle));
				offline.setIcon(R.drawable.ic_menu_stop);
				if (refresh != null)
					menu.findItem(R.id.Menu_Refresh).setVisible(true);
			}
		}

		MenuItem displayUnread = menu.findItem(R.id.Menu_DisplayOnlyUnread);
		if (displayUnread != null) {
			if (Controller.getInstance().onlyUnread()) {
				displayUnread.setTitle(getString(R.string.Commons_DisplayAll));
			} else {
				displayUnread.setTitle(getString(R.string.Commons_DisplayOnlyUnread));
			}
		}

		MenuItem displayOnlyCachedImages = menu.findItem(R.id.Menu_DisplayOnlyCachedImages);
		if (displayOnlyCachedImages != null) {
			if (Controller.getInstance().onlyDisplayCachedImages()) {
				displayOnlyCachedImages.setTitle(getString(R.string.Commons_DisplayAll));
			} else {
				displayOnlyCachedImages.setTitle(getString(R.string.Commons_DisplayOnlyCachedImages));
			}
		}

		if (!(this instanceof FeedHeadlineActivity)) {
			menu.removeItem(R.id.Menu_FeedUnsubscribe);
		}

		if (Controller.getInstance().hideFeedReadButtons()) {
			menu.removeItem(R.id.Menu_MarkFeedRead);
			menu.removeItem(R.id.Menu_MarkFeedsRead);
			menu.removeItem(R.id.Menu_MarkAllRead);
		}

		MenuItem cache = menu.findItem(R.id.Category_Menu_ImageCache);
		if (cache != null) {
			if (isCacherRunning()) {
				cache.setTitle(getString(R.string.Main_ImageCacheCancel));
			} else {
				cache.setTitle(getString(R.string.Main_ImageCache));
			}
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
		if (super.onOptionsItemSelected(item))
			return true;

		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {// Go to the CategoryActivity and clean the return-stack
			Intent intent = new Intent(this, CategoryActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		} else if (itemId == R.id.Menu_DisplayOnlyUnread) {
			Controller.getInstance().setDisplayOnlyUnread(!Controller.getInstance().onlyUnread());
			doRefresh();
			return true;
		} else if (itemId == R.id.Menu_DisplayOnlyCachedImages) {
			Controller.getInstance().setDisplayCachedImages(!Controller.getInstance().onlyDisplayCachedImages());
			doRefresh();
			return true;
		} else if (itemId == R.id.Menu_InvertSort) {
			if (this instanceof FeedHeadlineActivity) {
				Controller.getInstance().setInvertSortArticleList(!Controller.getInstance().invertSortArticlelist());
			} else {
				Controller.getInstance().setInvertSortFeedsCats(!Controller.getInstance().invertSortFeedscats());
			}
			doRefresh();
			return true;
		} else if (itemId == R.id.Menu_WorkOffline) {
			Controller.getInstance().setWorkOffline(!Controller.getInstance().workOffline());
			if (!Controller.getInstance().workOffline()) {
				// Synchronize status of articles with server
				new Updater(this, new StateSynchronisationUpdater()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
			doRefresh();
			return true;
		} else if (itemId == R.id.Menu_ShowPreferences) {
			startActivityForResult(new Intent(this, PreferencesActivity.class), Constants.ACTIVITY_SHOW_PREFERENCES);
			return true;
		} else if (itemId == R.id.Menu_About) {
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		} else if (itemId == R.id.Category_Menu_ImageCache) {
			if (isCacherRunning())
				doStopImageCache();
			else
				doStartImageCache();
			return true;
		} else if (itemId == R.id.Menu_FeedSubscribe) {
			startActivity(new Intent(this, SubscribeActivity.class));
			return true;
		}
		return false;
	}

	@Override
	public void onUpdateEnd(boolean goBackAfterUpdate) {
		updater = null;
		doRefresh();
		if (goBackAfterUpdate && !isFinishing())
			onBackPressed();
	}

	protected void doStopImageCache() {
		ForegroundService.cancel();
		invalidateOptionsMenu();
	}

	protected void doStartImageCache() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		final int title = R.string.Main_ImageCache_YesNoTitle;
		final FragmentManager fm = getSupportFragmentManager();

		switch (Utils.getNetworkType(cm)) {
			case Utils.NETWORK_MOBILE:
				YesNoUpdaterDialog.getInstance(new ImageCacheUpdater(Utils.NETWORK_MOBILE), title, R.string.Main_ImageCache_NetworkMobile).show(fm, "imagecache");
				break;
			case Utils.NETWORK_METERED:
				YesNoUpdaterDialog.getInstance(new ImageCacheUpdater(Utils.NETWORK_METERED), title, R.string.Main_ImageCache_NetworkMetered).show(fm, "imagecache");
				break;
			case Utils.NETWORK_WIFI:
				doCache(Utils.NETWORK_WIFI);
				break;
		}
	}

	private class ImageCacheUpdater implements IUpdatable {
		final int networkState;

		public ImageCacheUpdater(int networkState) {
			this.networkState = networkState;
		}

		@Override
		public void update() {
			doCache(networkState);
		}
	}

	/* ############# BEGIN: Cache */
	private void doCache(final int networkState) {
		// Register for progress-updates
		ForegroundService.registerCallback(this);

		if (isCacherRunning())
			return;

		invalidateOptionsMenu();

		// Start new cacher
		Intent intent = new Intent(ForegroundService.ACTION_LOAD_IMAGES);
		intent.putExtra(ForegroundService.PARAM_NETWORK, networkState);
		intent.setClass(this.getApplicationContext(), ForegroundService.class);

		this.startService(intent);
	}

	@Override
	public void onCacheStart() {
		ProgressBarManager.getInstance().addProgress(this);
	}

	@Override
	public void onCacheEnd() {
		ProgressBarManager.getInstance().removeProgress(this);
	}

	@Override
	public void onCacheInterrupted() {
		ProgressBarManager.getInstance().removeProgress(this);
		Toast.makeText(this, R.string.Main_ImageCache_ConnectivityLost, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCacheProgress(int taskCount, int progress) {
		if (taskCount == 0)
			setMyProgress(0);
		else
			setMyProgress((10000 / taskCount) * progress);
	}

	protected boolean isCacherRunning() {
		return ForegroundService.isInstanceCreated();
	}

	/* ############# END: Cache */

	protected void openConnectionErrorDialog(String errorMessage) {
		if (updater != null) {
			updater.cancel(true);
			updater = null;
		}
		ProgressBarManager.getInstance().resetProgress(this);
		Intent i = new Intent(this, ErrorActivity.class);
		i.putExtra(ErrorActivity.ERROR_MESSAGE, errorMessage);
		startActivityForResult(i, ErrorActivity.ACTIVITY_SHOW_ERROR);
	}

	protected void showErrorDialog(String message) {
		ErrorDialog.getInstance(message).show(getSupportFragmentManager(), "error");
	}

	private void refreshAndUpdate() {
		refreshDisplayMetrics();
		initTabletLayout();
		if (!Utils.checkIsConfigInvalid()) {
			doUpdate(false);
			doRefresh();
		}
	}

	@Override
	public final void dataChanged() {
		doRefresh();
	}

	@Override
	public void dataLoadingFinished() {
		// Empty!
	}

	protected void doRefresh() {
		invalidateOptionsMenu();
		ProgressBarManager.getInstance().setIndeterminateVisibility(this);
		if (Controller.getInstance().getConnector().hasLastError())
			openConnectionErrorDialog(Controller.getInstance().getConnector().pullLastError());
	}

	protected abstract void doUpdate(boolean forceUpdate);

	/**
	 * Can be used in child activities to update their data and get a UI refresh afterwards.
	 */
	abstract class ActivityUpdater extends AsyncTask<Void, Integer, Void> {
		protected int taskCount = 0;
		protected final boolean forceUpdate;

		ActivityUpdater(boolean forceUpdate) {
			this.forceUpdate = forceUpdate;
			ProgressBarManager.getInstance().addProgress(activity);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (values[0] == Integer.MAX_VALUE) {
				if (!isCacherRunning())
					ProgressBarManager.getInstance().removeProgress(activity);
				return;
			}
			// Add 500 to make sure we are still within 10000 but never show an empty progressbar at 0
			setMyProgress((10000 / (taskCount + 1)) * values[0] + 500);
		}
	}

	// The "active pointer" is the one currently moving our object.
	private static final int INVALID_POINTER_ID = -1;
	private int mActivePointerId = INVALID_POINTER_ID;

	private float mLastTouchX = 0;
	private float mLastTouchY = 0;
	private int mDeltaX = 0;
	private int mDeltaY = 0;

	private boolean resizing = false;

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (ev == null)
			return false;
		if (!isTablet)
			return false;

		// Only handle events when the list-divider is selected or we are already resizing:
		View view = findViewAtPosition(getWindow().getDecorView().getRootView(), (int) ev.getRawX(), (int) ev.getRawY());
		if (view == null && !resizing)
			return false;

		switch (ev.getActionMasked()) {
			case MotionEvent.ACTION_DOWN: {
				divider.setSelected(true);
				resizing = true;
				final int pointerIndex = ev.getActionIndex();

				// Remember where we started (for dragging)
				mLastTouchX = ev.getX(pointerIndex);
				mLastTouchY = ev.getY(pointerIndex);
				mDeltaX = 0;
				mDeltaY = 0;

				// Save the ID of this pointer (for dragging)
				mActivePointerId = ev.getPointerId(0);
				break;
			}

			case MotionEvent.ACTION_MOVE: {
				// Find the index of the active pointer and fetch its position
				final int pointerIndex = ev.findPointerIndex(mActivePointerId);

				if (pointerIndex < 0)
					break;

				final float x = ev.getX(pointerIndex);
				final float y = ev.getY(pointerIndex);

				// Calculate the distance moved
				mDeltaX = (int) (x - mLastTouchX);
				mDeltaY = (int) (y - mLastTouchY);

				// Store location for next difference
				mLastTouchX = x;
				mLastTouchY = y;

				handleResize();
				break;
			}

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
			default:
				mActivePointerId = INVALID_POINTER_ID;
				handleResize();
				storeSize();
				divider.setSelected(false);
				resizing = false;
				break;

		}
		return true;
	}

	private int calculateSize(final int size) {
		int ret = size;
		if (ret < minSize)
			ret = minSize;
		if (ret > maxSize)
			ret = maxSize;
		return ret;
	}

	private void storeSize() {
		int size = isVertical ? frameMain.getHeight() : frameMain.getWidth();
		Controller.getInstance().setViewSize(this, isVertical, size);
	}

	private static View findViewAtPosition(View parent, int x, int y) {
		if (parent instanceof ViewGroup) {
			ViewGroup viewGroup = (ViewGroup) parent;
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				View child = viewGroup.getChildAt(i);
				View viewAtPosition = findViewAtPosition(child, x, y);
				if (viewAtPosition != null) {
					return viewAtPosition;
				}
			}
			return null;
		} else {
			Rect rect = new Rect();
			parent.getGlobalVisibleRect(rect);
			if (rect.contains(x, y)) {
				return parent;
			} else {
				return null;
			}
		}
	}

	public void setMyProgress(int progress) {
		setMyProgressBarVisibility(progress > 1 && progress < 9999);
		progressbar.setProgress(progress);
		//		super.setProgress(progress);
	}

	public void setMyProgressBarVisibility(boolean visible) {
		progressbar.setVisibility(visible ? View.VISIBLE : View.GONE);
		//		super.setProgressBarVisibility(visible);
	}

	public void setMyProgressBarIndeterminateVisibility(boolean visible) {
		progressspinner.setVisibility(visible ? View.VISIBLE : View.GONE);
		//		super.setProgressBarIndeterminateVisibility(visible);
	}

}
