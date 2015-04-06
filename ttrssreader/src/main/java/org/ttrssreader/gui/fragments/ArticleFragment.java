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

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.HtmlNode;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagNodeVisitor;
import org.stringtemplate.v4.ST;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.ProgressBarManager;
import org.ttrssreader.gui.ErrorActivity;
import org.ttrssreader.gui.FeedHeadlineActivity;
import org.ttrssreader.gui.TextInputAlert;
import org.ttrssreader.gui.dialogs.ArticleLabelDialog;
import org.ttrssreader.gui.dialogs.ImageCaptionDialog;
import org.ttrssreader.gui.interfaces.TextInputAlertCallback;
import org.ttrssreader.gui.view.ArticleWebViewClient;
import org.ttrssreader.gui.view.MyGestureDetector;
import org.ttrssreader.gui.view.MyWebView;
import org.ttrssreader.imageCache.ImageCache;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.pojos.Label;
import org.ttrssreader.model.pojos.RemoteFile;
import org.ttrssreader.model.updaters.ArticleReadStateUpdater;
import org.ttrssreader.model.updaters.PublishedStateUpdater;
import org.ttrssreader.model.updaters.StarredStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.DateUtils;
import org.ttrssreader.utils.FileUtils;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ArticleFragment extends Fragment implements TextInputAlertCallback {

	private static final String TAG = ArticleFragment.class.getSimpleName();

	public static final String FRAGMENT = "ARTICLE_FRAGMENT";

	private static final String ARTICLE_ID = "ARTICLE_ID";
	private static final String ARTICLE_FEED_ID = "ARTICLE_FEED_ID";

	private static final String ARTICLE_MOVE = "ARTICLE_MOVE";
	private static final int CONTEXT_MENU_SHARE_URL = 1000;
	private static final int CONTEXT_MENU_SHARE_ARTICLE = 1001;
	private static final int CONTEXT_MENU_DISPLAY_CAPTION = 1002;
	private static final int CONTEXT_MENU_COPY_URL = 1003;
	private static final int CONTEXT_MENU_COPY_CONTENT = 1004;

	private static final char TEMPLATE_DELIMITER_START = '$';
	private static final char TEMPLATE_DELIMITER_END = '$';
	private static final String LABEL_COLOR_STRING = "<span style=\"color: %s; background-color: %s\">%s</span>";

	private static final String TEMPLATE_ARTICLE_VAR = "article";
	private static final String TEMPLATE_FEED_VAR = "feed";
	private static final String MARKER_CACHED_IMAGES = "CACHED_IMAGES";
	private static final String MARKER_UPDATED = "UPDATED";
	private static final String MARKER_LABELS = "LABELS";
	private static final String MARKER_CONTENT = "CONTENT";
	private static final String MARKER_ATTACHMENTS = "ATTACHMENTS";

	// Extras
	private int articleId = -1;
	private int feedId = -1;
	private int categoryId = Integer.MIN_VALUE;
	private boolean selectArticlesForCategory = false;
	private int lastMove = 0;

	private Article article = null;
	private Feed feed = null;
	private String content;
	private boolean linkAutoOpened;
	private boolean markedRead = false;
	private String cachedImages = null;

	private FrameLayout webContainer = null;
	private MyWebView webView;
	private boolean webviewInitialized = false;
	private Button buttonNext;
	private Button buttonPrev;

	private String mSelectedExtra;
	private String mSelectedAltText;

	private ArticleJSInterface articleJSInterface;

	private GestureDetector gestureDetector = null;
	private View.OnTouchListener gestureListener = null;

	public static ArticleFragment newInstance(int id, int feedId, int categoryId, boolean selectArticles,
			int lastMove) {
		// Create a new fragment instance
		ArticleFragment detail = new ArticleFragment();
		detail.articleId = id;
		detail.feedId = feedId;
		detail.categoryId = categoryId;
		detail.selectArticlesForCategory = selectArticles;
		detail.lastMove = lastMove;
		detail.setRetainInstance(true);
		return detail;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.articleitem, container, false);
	}

	@Override
	public void onCreate(Bundle instance) {
		if (instance != null) {
			articleId = instance.getInt(ARTICLE_ID);
			feedId = instance.getInt(ARTICLE_FEED_ID);
			categoryId = instance.getInt(FeedHeadlineListFragment.FEED_CAT_ID);
			selectArticlesForCategory = instance.getBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES);
			lastMove = instance.getInt(ARTICLE_MOVE);
			if (webView != null) webView.restoreState(instance);
		}
		super.onCreate(instance);
	}

	@Override
	public void onActivityCreated(Bundle instance) {
		super.onActivityCreated(instance);
		articleJSInterface = new ArticleJSInterface(getActivity());

		initData();
		initUI();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// Remove the WebView from the old placeholder
		if (webView != null) webContainer.removeView(webView);

		super.onConfigurationChanged(newConfig);

		initUI();
		doRefresh();
	}

	@Override
	public void onSaveInstanceState(Bundle instance) {
		instance.putInt(ARTICLE_ID, articleId);
		instance.putInt(ARTICLE_FEED_ID, feedId);
		instance.putInt(FeedHeadlineListFragment.FEED_CAT_ID, categoryId);
		instance.putBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES, selectArticlesForCategory);
		instance.putInt(ARTICLE_MOVE, lastMove);
		if (webView != null) webView.saveState(instance);
		super.onSaveInstanceState(instance);
	}

	@SuppressLint("ClickableViewAccessibility")
	private void initUI() {
		// Wrap webview inside another FrameLayout to avoid memory leaks as described here:
		// http://stackoverflow.com/questions/3130654/memory-leak-in-webview
		webContainer = (FrameLayout) getActivity().findViewById(R.id.article_webView_Container);
		buttonPrev = (Button) getActivity().findViewById(R.id.article_buttonPrev);
		buttonNext = (Button) getActivity().findViewById(R.id.article_buttonNext);
		buttonPrev.setOnClickListener(onButtonPressedListener);
		buttonNext.setOnClickListener(onButtonPressedListener);

		// Initialize the WebView if necessary
		if (webView == null) {
			webView = new MyWebView(getActivity());
			webView.setWebViewClient(new ArticleWebViewClient());
			webView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

			boolean supportZoom = Controller.getInstance().supportZoomControls();
			webView.getSettings().setSupportZoom(supportZoom);
			webView.getSettings().setBuiltInZoomControls(supportZoom);
			webView.getSettings().setDisplayZoomControls(false);
			webView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
			webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
			webView.setScrollbarFadingEnabled(true);
			webView.setOnKeyListener(keyListener);
			webView.getSettings().setTextZoom(Controller.getInstance().textZoom());
			webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

			if (gestureDetector == null || gestureListener == null) {
				ActionBar actionBar = getActivity().getActionBar();

				// Detect touch gestures like swipe and scroll down:
				gestureDetector = new GestureDetector(getActivity(),
						new ArticleGestureDetector(actionBar, Controller.getInstance().hideActionbar()));

				gestureListener = new View.OnTouchListener() {
					public boolean onTouch(View v, MotionEvent event) {
						gestureDetector.onTouchEvent(event);
						// Call webView.onTouchEvent(event) everytime, seems to fix issues with webview not beeing
						// refreshed after swiping:
						return webView.onTouchEvent(event) || v.performClick();
					}
				};
			}
			// TODO: Lint-Error
			// "Custom view org/ttrssreader/gui/view/MyWebView has setOnTouchListener called on it but does not
			// override performClick"
			webView.setOnTouchListener(gestureListener);
		}

		// TODO: Is this still necessary?
		int backgroundColor = Controller.getInstance().getThemeBackground();
		int fontColor = Controller.getInstance().getThemeFont();
		webView.setBackgroundColor(backgroundColor);
		if (getActivity().findViewById(R.id.article_view) instanceof ViewGroup)
			setBackground((ViewGroup) getActivity().findViewById(R.id.article_view), backgroundColor, fontColor);

		registerForContextMenu(webView);
		// Attach the WebView to its placeholder
		if (webView.getParent() != null && webView.getParent() instanceof FrameLayout)
			((FrameLayout) webView.getParent()).removeAllViews();
		webContainer.addView(webView);

		getActivity().findViewById(R.id.article_button_view).setVisibility(
				Controller.getInstance().showButtonsMode() == Constants.SHOW_BUTTONS_MODE_ALLWAYS ? View.VISIBLE
																								  : View.GONE);

		setHasOptionsMenu(true);
	}

	private void initData() {
		if (feedId > 0) Controller.getInstance().lastOpenedFeeds.add(feedId);
		Controller.getInstance().lastOpenedArticles.add(articleId);

		/* Move database access to background:
		 */
		new AsyncTask<Void, Void, Void>() {
			protected Void doInBackground(Void... params) {
				// Get article from DB
				article = DBHelper.getInstance().getArticle(articleId);
				if (article == null) {
					getActivity().finish();
					return null;
				}
				feed = DBHelper.getInstance().getFeed(article.feedId);

				// Mark as read if necessary, do it here because in doRefresh() it will be done several times even if
				// you set it to "unread" in the meantime.
				if (article.isUnread) {
					article.isUnread = false;
					markedRead = true;
					new Updater(null, new ArticleReadStateUpdater(article, 0))
							.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}

				cachedImages = getCachedImagesJS(article.id);

				getActivity().invalidateOptionsMenu(); // Force redraw of menu items in actionbar

				// Reload content on next doRefresh()
				webviewInitialized = false;
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				super.onPostExecute(aVoid);
				// Has to be called from UI thread
				doRefresh();
			}
		}.execute();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (getView() != null) getView().setVisibility(View.VISIBLE);
	}

	@Override
	public void onStop() {
		// Check again to make sure it didnt get updated and marked as unread again in the background
		if (!markedRead) {
			if (article != null && article.isUnread) new Updater(null, new ArticleReadStateUpdater(article, 0))
					.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		super.onStop();
		if (getView() != null) getView().setVisibility(View.GONE);
	}

	@Override
	public void onDestroy() {
		// Check again to make sure it didnt get updated and marked as unread again in the background
		if (!markedRead) {
			if (article != null && article.isUnread) new Updater(null, new ArticleReadStateUpdater(article, 0))
					.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		super.onDestroy();
		if (webContainer != null) webContainer.removeAllViews();
		if (webView != null) webView.destroy();
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void doRefresh() {
		if (webView == null) return;

		try {
			ProgressBarManager.getInstance().addProgress(getActivity());

			if (Controller.getInstance().workOffline() || !Controller.getInstance().loadMedia()) {
				webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
			} else {
				webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
			}
			if (!Controller.getInstance().loadMedia()) webView.getSettings().setMediaPlaybackRequiresUserGesture
					(false);

			// No need to reload everything
			if (webviewInitialized) return;

			// Check for errors
			if (Controller.getInstance().getConnector().hasLastError()) {
				Intent i = new Intent(getActivity(), ErrorActivity.class);
				i.putExtra(ErrorActivity.ERROR_MESSAGE, Controller.getInstance().getConnector().pullLastError());
				startActivityForResult(i, ErrorActivity.ACTIVITY_SHOW_ERROR);
				return;
			}

			if (article == null || article.content == null) return;

			StringBuilder labels = new StringBuilder();
			for (Label label : article.labels) {
				if (label.checked) {
					if (labels.length() > 0) labels.append(", ");

					String labelString = label.caption;
					if (label.foregroundColor != null && label.backgroundColor != null) labelString = String
							.format(LABEL_COLOR_STRING, label.foregroundColor, label.backgroundColor, label.caption);
					labels.append(labelString);
				}
			}

			// Load html from Controller and insert content
			ST contentTemplate = new ST(Controller.htmlTemplate, TEMPLATE_DELIMITER_START, TEMPLATE_DELIMITER_END);

			contentTemplate.add(TEMPLATE_ARTICLE_VAR, article);
			contentTemplate.add(TEMPLATE_FEED_VAR, feed);
			contentTemplate.add(MARKER_CACHED_IMAGES, cachedImages);
			contentTemplate.add(MARKER_LABELS, labels.toString());
			contentTemplate.add(MARKER_UPDATED, DateUtils.getDateTimeCustom(getActivity(), article.updated));
			contentTemplate.add(MARKER_CONTENT, article.content);
			// Inject the specific code for attachments, <img> for images, http-link for Videos
			contentTemplate.add(MARKER_ATTACHMENTS, getAttachmentsMarkup(getActivity(), article.attachments));

			webView.getSettings().setJavaScriptEnabled(true);
			// TODO: Do we need to do this?
			//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
			webView.addJavascriptInterface(articleJSInterface, "articleController");
			content = contentTemplate.render();
			webView.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "utf-8", null);

			if (!linkAutoOpened && article.content.length() < 3) {
				if (Controller.getInstance().openUrlEmptyArticle()) {
					Log.i(TAG, "Article-Content is empty, opening URL in browser");
					linkAutoOpened = true;
					openLink();
				}
			}

			// Everything did load, we dont have to do this again.
			webviewInitialized = true;
		} catch (Exception e) {
			Log.w(TAG, e.getClass().getSimpleName() + " in doRefresh(): " + e.getMessage() + " (" + e.getCause() + ")",
					e);
		} finally {
			ProgressBarManager.getInstance().removeProgress(getActivity());
		}
	}

	/**
	 * Starts a new activity with the url of the current article. This should open a webbrowser in most cases. If the
	 * url contains spaces or newline-characters it is first trim()'ed.
	 */
	private void openLink() {
		if (article.url == null || article.url.length() == 0) return;

		String url = article.url;
		if (article.url.contains(" ") || article.url.contains("\n")) url = url.trim();

		try {
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(url));
			startActivity(i);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "Couldn't find a suitable activity for the uri: " + url);
		}
	}

	/**
	 * Recursively walks all viewGroups and their Views inside the given ViewGroup and sets the background to black
	 * and,
	 * in case a TextView is found, the Text-Color to white.
	 *
	 * @param v the ViewGroup to walk through
	 */
	private void setBackground(ViewGroup v, int background, int font) {
		v.setBackgroundColor(getResources().getColor(background));

		for (int i = 0; i < v.getChildCount(); i++) { // View at index 0 seems to be this view itself.
			View vChild = v.getChildAt(i);
			if (vChild == null) continue;
			if (vChild instanceof TextView) ((TextView) vChild).setTextColor(font);
			if (vChild instanceof ViewGroup) setBackground(((ViewGroup) vChild), background, font);
		}
	}

	/**
	 * generate HTML code for attachments to be shown inside article
	 *
	 * @param context     current context
	 * @param attachments collection of attachment URLs
	 */
	private static String getAttachmentsMarkup(Context context, Set<String> attachments) {
		StringBuilder content = new StringBuilder();
		Map<String, Collection<String>> attachmentsByMimeType = FileUtils.groupFilesByMimeType(attachments);

		if (attachmentsByMimeType.isEmpty()) return "";

		for (String mimeType : attachmentsByMimeType.keySet()) {
			Collection<String> mimeTypeUrls = attachmentsByMimeType.get(mimeType);
			if (mimeTypeUrls.isEmpty()) return "";

			if (mimeType.equals(FileUtils.IMAGE_MIME)) {
				ST st = new ST(context.getResources().getString(R.string.ATTACHMENT_IMAGES_TEMPLATE));
				st.add("items", mimeTypeUrls);
				content.append(st.render());
			} else {
				ST st = new ST(context.getResources().getString(R.string.ATTACHMENT_MEDIA_TEMPLATE));
				st.add("items", mimeTypeUrls);
				CharSequence linkText = mimeType.equals(FileUtils.AUDIO_MIME) || mimeType.equals(FileUtils.VIDEO_MIME)
										? context.getText(R.string.ArticleActivity_MediaPlay)
										: context.getText(R.string.ArticleActivity_MediaDisplayLink);
				st.add("linkText", linkText);
				content.append(st.render());
			}
		}

		return content.toString();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		HitTestResult result = ((WebView) v).getHitTestResult();
		menu.setHeaderTitle(getResources().getString(R.string.ArticleActivity_ShareLink));
		mSelectedExtra = null;
		mSelectedAltText = null;

		int type = result.getType();
		boolean image = (type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE || type == HitTestResult.IMAGE_TYPE);
		boolean anchor = (type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE || type == HitTestResult.SRC_ANCHOR_TYPE);

		// Anchors get a context-menu with "Share URL" and "Copy URL"
		if (anchor) {
			mSelectedExtra = result.getExtra();
			menu.add(ContextMenu.NONE, CONTEXT_MENU_SHARE_URL, 2,
					getResources().getString(R.string.ArticleActivity_ShareURL));
			menu.add(ContextMenu.NONE, CONTEXT_MENU_COPY_URL, 3,
					getResources().getString(R.string.ArticleActivity_CopyURL));
		}

		// Images get a context-menu with "Show caption" which displays the content of the title- or alt-attribute
		if (image) {
			mSelectedAltText = getAltTextForImageUrl(result.getExtra());
			if (mSelectedAltText != null) menu.add(ContextMenu.NONE, CONTEXT_MENU_DISPLAY_CAPTION, 1,
					getResources().getString(R.string.ArticleActivity_ShowCaption));
		}
		menu.add(ContextMenu.NONE, CONTEXT_MENU_SHARE_ARTICLE, 10,
				getResources().getString(R.string.ArticleActivity_ShareArticle));
		menu.add(ContextMenu.NONE, CONTEXT_MENU_COPY_CONTENT, 4,
				getResources().getString(R.string.ArticleActivity_CopyContent));
	}

	public void onPrepareOptionsMenu(Menu menu) {
		if (article != null) {
			MenuItem read = menu.findItem(R.id.Article_Menu_MarkRead);
			if (read != null) {
				if (article.isUnread) {
					read.setTitle(getString(R.string.Commons_MarkRead));
					read.setIcon(R.drawable.ic_menu_mark);
				} else {
					read.setTitle(getString(R.string.Commons_MarkUnread));
					read.setIcon(R.drawable.ic_menu_clear_playlist);
				}
			}

			MenuItem publish = menu.findItem(R.id.Article_Menu_MarkPublish);
			if (publish != null) {
				if (article.isPublished) {
					publish.setTitle(getString(R.string.Commons_MarkUnpublish));
					publish.setIcon(R.drawable.menu_published);
				} else {
					publish.setTitle(getString(R.string.Commons_MarkPublish));
					publish.setIcon(R.drawable.menu_publish);
				}
			}

			MenuItem star = menu.findItem(R.id.Article_Menu_MarkStar);
			if (star != null) {
				if (article.isStarred) {
					star.setTitle(getString(R.string.Commons_MarkUnstar));
					star.setIcon(R.drawable.menu_starred);
				} else {
					star.setTitle(getString(R.string.Commons_MarkStar));
					star.setIcon(R.drawable.ic_menu_star);
				}
			}
		}
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.Article_Menu_MarkRead: {
				if (article != null)
					new Updater(getActivity(), new ArticleReadStateUpdater(article, article.isUnread ? 0 : 1))
							.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				return true;
			}
			case R.id.Article_Menu_MarkStar: {
				if (article != null)
					new Updater(getActivity(), new StarredStateUpdater(article, article.isStarred ? 0 : 1))
							.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				return true;
			}
			case R.id.Article_Menu_MarkPublish: {
				if (article != null)
					new Updater(getActivity(), new PublishedStateUpdater(article, article.isPublished ? 0 : 1))
							.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				return true;
			}
			case R.id.Article_Menu_MarkPublishNote: {
				new TextInputAlert(this, article).show(getActivity());
				return true;
			}
			case R.id.Article_Menu_AddArticleLabel: {
				if (article != null) {
					DialogFragment dialog = ArticleLabelDialog.newInstance(article.id);
					dialog.show(getFragmentManager(), "Edit Labels");
				}
				return true;
			}
			case R.id.Article_Menu_ShareLink: {
				if (article != null) {
					Intent i = new Intent(Intent.ACTION_SEND);
					i.setType("text/plain");
					i.putExtra(Intent.EXTRA_TEXT, article.url);
					i.putExtra(Intent.EXTRA_SUBJECT, article.title);
					startActivity(Intent.createChooser(i, getText(R.string.ArticleActivity_ShareTitle)));
				}
				return true;
			}
			default:
				return false;
		}
	}

	/**
	 * Using a small html parser with a visitor which goes through the html I extract the alt-attribute from the
	 * content. If nothing is found it is left as null and the menu should'nt contain the item to display the caption.
	 *
	 * @param extra the
	 * @return the alt-text or null if none was found.
	 */
	private String getAltTextForImageUrl(String extra) {
		if (content == null || !content.contains(extra)) return null;

		HtmlCleaner cleaner = new HtmlCleaner();
		TagNode node = cleaner.clean(content);

		MyTagNodeVisitor tnv = new MyTagNodeVisitor(extra);
		node.traverse(tnv);

		return tnv.alt;
	}

	/**
	 * Create javascript associative array with article cached image url as key and image hash as value. Only
	 * RemoteFiles which are "cached" are added to this array so if an image is not available locally it is left as it
	 * is.
	 *
	 * @param articleId article ID
	 * @return javascript associative array content as text
	 */
	private String getCachedImagesJS(int articleId) {
		StringBuilder hashes = new StringBuilder("");
		Collection<RemoteFile> rfs = DBHelper.getInstance().getRemoteFiles(articleId);

		if (rfs != null && !rfs.isEmpty()) {
			for (RemoteFile rf : rfs) {

				if (rf.cached) {
					if (hashes.length() > 0) hashes.append(",\n");

					hashes.append("'");
					hashes.append(rf.url);
					hashes.append("': '");
					hashes.append(ImageCache.getHashForKey(rf.url));
					hashes.append("'");
				}

			}
		}

		return hashes.toString();
	}

	/**
	 * This is necessary to iterate over all HTML-Nodes and scan for images with ALT-Attributes.
	 */
	private class MyTagNodeVisitor implements TagNodeVisitor {
		private String alt = null;
		private String extra;

		private MyTagNodeVisitor(String extra) {
			this.extra = extra;
		}

		public boolean visit(TagNode tagNode, HtmlNode htmlNode) {
			if (htmlNode instanceof TagNode) {
				TagNode tag = (TagNode) htmlNode;
				String tagName = tag.getName();
				// Only if the image-url is the same as the url of the image the long-press was on:
				if ("img".equals(tagName) && extra.equals(tag.getAttributeByName("src"))) {
					// Prefer title-attribute over alt since this is the html default
					alt = tag.getAttributeByName("title");
					if (alt != null) return false;
					alt = tag.getAttributeByName("alt");
					if (alt != null) return false;
				}
			}
			return true;
		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		Intent shareIntent;
		switch (item.getItemId()) {
			case CONTEXT_MENU_SHARE_URL:
				if (mSelectedExtra != null) {
					shareIntent = getUrlShareIntent(mSelectedExtra);
					startActivity(Intent.createChooser(shareIntent, "Share URL"));
				}
				break;
			case CONTEXT_MENU_COPY_URL:
				if (mSelectedExtra != null) {
					ClipboardManager clipboard = (ClipboardManager) getActivity()
							.getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("URL from TTRSS", mSelectedExtra);
					clipboard.setPrimaryClip(clip);
				}
				break;
			case CONTEXT_MENU_DISPLAY_CAPTION:
				ImageCaptionDialog fragment = ImageCaptionDialog.getInstance(mSelectedAltText);
				fragment.show(getFragmentManager(), ImageCaptionDialog.DIALOG_CAPTION);
				return true;
			case CONTEXT_MENU_SHARE_ARTICLE:
				// default behavior is to share the article URL
				shareIntent = getUrlShareIntent(article.url);
				startActivity(Intent.createChooser(shareIntent, "Share URL"));
				break;
			case CONTEXT_MENU_COPY_CONTENT:
				articleJSInterface.javaCallCopyToClipoard();
		}
		return super.onContextItemSelected(item);
	}

	private Intent getUrlShareIntent(String url) {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL");
		i.putExtra(Intent.EXTRA_TEXT, url);
		return i;
	}

	private OnClickListener onButtonPressedListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (v.equals(buttonNext)) {
				FeedHeadlineActivity activity = (FeedHeadlineActivity) getActivity();
				activity.openNextArticle(-1);
			} else if (v.equals(buttonPrev)) {
				FeedHeadlineActivity activity = (FeedHeadlineActivity) getActivity();
				activity.openNextArticle(1);
			}
		}
	};

	private OnKeyListener keyListener = new OnKeyListener() {
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (Controller.getInstance().useVolumeKeys()) {
				if (keyCode == KeyEvent.KEYCODE_N) {
					FeedHeadlineActivity activity = (FeedHeadlineActivity) getActivity();
					activity.openNextArticle(-1);
					return true;
				} else if (keyCode == KeyEvent.KEYCODE_B) {
					FeedHeadlineActivity activity = (FeedHeadlineActivity) getActivity();
					activity.openNextArticle(1);
					return true;
				}
			}
			return false;
		}
	};

	/**
	 * this class represents an object, which methods can be called from article's {@code WebView} javascript to
	 * manipulate the article activity
	 */
	private class ArticleJSInterface {

		/**
		 * current article activity
		 */
		private Activity activity;

		/**
		 * public constructor, which saves calling activity as member variable
		 *
		 * @param aa current article activity
		 */
		private ArticleJSInterface(Activity aa) {
			activity = aa;
		}

		/**
		 * go to previous article
		 */
		@JavascriptInterface
		public void prev() {
			// Add this to avoid android.view.windowmanager$badtokenexception unable to add window
			if (activity.isFinishing()) return;

			// loadurl on UI main thread
			activity.runOnUiThread(new Runnable() {
				public void run() {
					FeedHeadlineActivity activity = (FeedHeadlineActivity) getActivity();
					activity.openNextArticle(-1);
				}
			});
		}

		/**
		 * go to next article
		 */
		@JavascriptInterface
		public void next() {
			// Add this to avoid android.view.windowmanager$badtokenexception unable to add window
			if (activity.isFinishing()) return;

			activity.runOnUiThread(new Runnable() {
				public void run() {
					FeedHeadlineActivity activity = (FeedHeadlineActivity) getActivity();
					activity.openNextArticle(1);
				}
			});
		}

		@JavascriptInterface
		public void copyContentToClipboard(String aContent) {
			final String contentPlain = aContent;
			// Add this to avoid android.view.windowmanager$badtokenexception unable to add window
			if (activity.isFinishing()) return;

			activity.runOnUiThread(new Runnable() {
				public void run() {
					ClipboardManager clipboard = (ClipboardManager) activity
							.getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData
							.newHtmlText("HTML Text", contentPlain, prepareHTMLContentForClipboard(content));
					//ClipData clip = ClipData.newPlainText("HTML Text", contentPlain);
					clipboard.setPrimaryClip(clip);
				}
			});
		}

		/**
		 * This function handles call from Java to JavaScript
		 */
		public void javaCallCopyToClipoard() {
			final String webUrl =
					"javascript:window.articleController.copyContentToClipboard(document.getElementsByTagName"
							+ "('body')[0].innerText);";

			// Add this to avoid android.view.windowmanager$badtokenexception unable to add window
			if (activity.isFinishing()) return;

			// loadurl on UI main thread
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					webView.loadUrl(webUrl);
				}
			});
		}

	}

	/**
	 * Cut out the head-Tag from the html-content.
	 */
	private String prepareHTMLContentForClipboard(String html) {
		int start = html.indexOf("<head");
		int end = html.indexOf("</head>", start);
		if (0 < start && start < end && end < html.length())
			return html.substring(0, start) + html.substring(end, html.length());
		return html;
	}

	private class ArticleGestureDetector extends MyGestureDetector {
		private ArticleGestureDetector(ActionBar actionBar, boolean hideActionbar) {
			super(actionBar, hideActionbar);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			// Refresh metrics-data in Controller
			Controller.refreshDisplayMetrics(
					((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay());

			if (Math.abs(e1.getY() - e2.getY()) > Controller.relSwipeMaxOffPath) return false;

			float distX = Math.abs(e1.getX() - e2.getX());
			float distY = Math.abs(e1.getY() - e2.getY());
			// Only accept this movement as a swipe if the horizontal distance was greater then the vertical distance
			if (distX < 1.3 * distY) return false;

			if (e1.getX() - e2.getX() > Controller.relSwipeMinDistance
					&& Math.abs(velocityX) > Controller.relSwipeThresholdVelocity) {

				// right to left swipe
				FeedHeadlineActivity activity = (FeedHeadlineActivity) getActivity();
				activity.openNextArticle(1);
				return true;

			} else if (e2.getX() - e1.getX() > Controller.relSwipeMinDistance
					&& Math.abs(velocityX) > Controller.relSwipeThresholdVelocity) {

				// left to right swipe
				FeedHeadlineActivity activity = (FeedHeadlineActivity) getActivity();
				activity.openNextArticle(-1);
				return true;

			}
			return false;
		}

	}

	@Override
	public void onPublishNoteResult(Article a, String note) {
		new Updater(getActivity(), new PublishedStateUpdater(a, a.isPublished ? 0 : 1, note))
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
		if (Controller.sFragmentAnimationDirection != 0 && Controller.getInstance().animations()) {
			Animator a;
			if (Controller.sFragmentAnimationDirection > 0)
				a = AnimatorInflater.loadAnimator(getActivity(), R.animator.slide_out_left);
			else a = AnimatorInflater.loadAnimator(getActivity(), R.animator.slide_out_right);

			// Reset:
			Controller.sFragmentAnimationDirection = 0;
			return a;
		}
		return super.onCreateAnimator(transit, enter, nextAnim);
	}

}
