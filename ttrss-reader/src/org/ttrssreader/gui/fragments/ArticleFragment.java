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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
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
import org.ttrssreader.gui.dialogs.ImageCaptionDialog;
import org.ttrssreader.gui.view.ArticleWebViewClient;
import org.ttrssreader.gui.view.MyGestureDetector;
import org.ttrssreader.imageCache.ImageCache;
import org.ttrssreader.model.FeedHeadlineAdapter;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.pojos.Label;
import org.ttrssreader.model.pojos.RemoteFile;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.DateUtils;
import org.ttrssreader.utils.FileUtils;
import org.ttrssreader.utils.Utils;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import android.webkit.WebSettings.TextSize;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;

@SuppressWarnings("deprecation")
public class ArticleFragment extends SherlockFragment {
    
    public static final String FRAGMENT = "ARTICLE_FRAGMENT";
    
    public static final String ARTICLE_ID = "ARTICLE_ID";
    public static final String ARTICLE_FEED_ID = "ARTICLE_FEED_ID";
    
    public static final String ARTICLE_MOVE = "ARTICLE_MOVE";
    public static final int ARTICLE_MOVE_NONE = 0;
    public static final int ARTICLE_MOVE_DEFAULT = ARTICLE_MOVE_NONE;
    private static final int CONTEXT_MENU_SHARE_URL = 1000;
    private static final int CONTEXT_MENU_SHARE_ARTICLE = 1001;
    private static final int CONTEXT_MENU_DISPLAY_CAPTION = 1002;
    
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
    private boolean selectForCategory = false;
    private int lastMove = ARTICLE_MOVE_DEFAULT;
    
    private Article article = null;
    private Feed feed = null;
    private String content;
    private boolean linkAutoOpened;
    private boolean markedRead = false;
    
    private FrameLayout webContainer = null;
    private WebView webView;
    private boolean webviewInitialized = false;
    private Button buttonNext;
    private Button buttonPrev;
    // private GestureDetector gestureDetector;
    
    private FeedHeadlineAdapter parentAdapter = null;
    private int[] parentIDs = new int[2];
    private String mSelectedExtra;
    private String mSelectedAltText;
    
    private ArticleJSInterface articleJSInterface;
    
    private GestureDetector gestureDetector = null;
    private View.OnTouchListener gestureListener = null;
    
    public static ArticleFragment newInstance(int id, int feedId, int categoryId, boolean selectArticles, int lastMove) {
        // Create a new fragment instance
        ArticleFragment detail = new ArticleFragment();
        detail.articleId = id;
        detail.feedId = feedId;
        detail.categoryId = categoryId;
        detail.selectForCategory = selectArticles;
        detail.lastMove = lastMove;
        detail.setHasOptionsMenu(true);
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
            selectForCategory = instance.getBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES);
            lastMove = instance.getInt(ARTICLE_MOVE);
            if (webView != null)
                webView.restoreState(instance);
        }
        super.onCreate(instance);
    }
    
    @Override
    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);
        articleJSInterface = new ArticleJSInterface(getActivity());
        initData();
        initUI();
        doRefresh();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // Remove the WebView from the old placeholder
        if (webView != null)
            webContainer.removeView(webView);
        
        super.onConfigurationChanged(newConfig);
        
        initUI();
        doRefresh();
    }
    
    @Override
    public void onSaveInstanceState(Bundle instance) {
        instance.putInt(ARTICLE_ID, articleId);
        instance.putInt(ARTICLE_FEED_ID, feedId);
        instance.putInt(FeedHeadlineListFragment.FEED_CAT_ID, categoryId);
        instance.putBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES, selectForCategory);
        instance.putInt(ARTICLE_MOVE, lastMove);
        if (webView != null)
            webView.saveState(instance);
        super.onSaveInstanceState(instance);
    }
    
    private void fillParentInformation() {
        int index = parentAdapter.getIds().indexOf(articleId);
        if (index >= 0) {
            parentIDs[0] = parentAdapter.getId(index - 1); // Previous
            parentIDs[1] = parentAdapter.getId(index + 1); // Next
        }
    }
    
    @SuppressLint("InlinedApi")
	private void initUI() {
        // Wrap webview inside another FrameLayout to avoid memory leaks as described here:
        // http://stackoverflow.com/questions/3130654/memory-leak-in-webview
        webContainer = (FrameLayout) getActivity().findViewById(R.id.article_webView_Container);
        buttonPrev = (Button) getActivity().findViewById(R.id.article_buttonPrev);
        buttonNext = (Button) getActivity().findViewById(R.id.article_buttonNext);
        buttonPrev.setOnClickListener(onButtonPressedListener);
        buttonNext.setOnClickListener(onButtonPressedListener);
        
        // Initialize the WebView if necessary
        if (webView == null) { // || webView.getSettings() == null) { // TODO Why can getSettings return null?
            webView = new WebView(getActivity());
            webView.setWebViewClient(new ArticleWebViewClient(getActivity()));
            webView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            
            boolean supportZoom = Controller.getInstance().supportZoomControls();
            webView.getSettings().setSupportZoom(supportZoom);
            webView.getSettings().setBuiltInZoomControls(supportZoom);
            
            webView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
            webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
            webView.setScrollbarFadingEnabled(true);
            webView.setOnKeyListener(keyListener);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                webView.getSettings().setTextZoom(Controller.getInstance().textZoom());
            } else {
                // Use rough estimation of new size for old api levels:
                int size = Controller.getInstance().textZoom();
                TextSize newSize = TextSize.NORMAL;
                if (size < 50)
                    newSize = TextSize.SMALLEST;
                else if (size < 80)
                    newSize = TextSize.SMALLER;
                else if (size > 120)
                    newSize = TextSize.LARGER;
                else if (size > 150)
                    newSize = TextSize.LARGEST;
                webView.getSettings().setTextSize(newSize);
            }
            
            // prevent flicker in ics
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
        }
        
        if (Controller.getInstance().darkBackground()) {
            webView.setBackgroundColor(Color.BLACK);
            if (getActivity().findViewById(R.id.article_view) instanceof ViewGroup)
                setDarkBackground((ViewGroup) getActivity().findViewById(R.id.article_view));
        }
        
        registerForContextMenu(webView);
        // Attach the WebView to its placeholder
        if (webView.getParent() != null && webView.getParent() instanceof FrameLayout)
            ((FrameLayout) webView.getParent()).removeAllViews();
        webContainer.addView(webView);
        
        getActivity().findViewById(R.id.article_button_view).setVisibility(
                Controller.getInstance().showButtonsMode() == Constants.SHOW_BUTTONS_MODE_ALLWAYS ? View.VISIBLE
                        : View.GONE);
        
        if (gestureDetector == null || gestureListener == null) {
            ActionBar actionBar = getSherlockActivity().getSupportActionBar();
            
            // Detect touch gestures like swipe and scroll down:
            gestureDetector = new GestureDetector(getActivity(), new ArticleGestureDetector(actionBar, Controller
                    .getInstance().hideActionbar()));
            
            gestureListener = new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    return (gestureDetector.onTouchEvent(event) || webView.onTouchEvent(event));
                }
            };
        }
        
        webView.setOnTouchListener(gestureListener);
    }
    
    private void initData() {
        if (feedId > 0)
            Controller.getInstance().lastOpenedFeeds.add(feedId);
        Controller.getInstance().lastOpenedArticles.add(articleId);
        
        if (parentAdapter == null)
            parentAdapter = new FeedHeadlineAdapter(getActivity().getApplicationContext(), feedId, categoryId,
                    selectForCategory);
        else
            parentAdapter.makeQuery(true);
        
        fillParentInformation();
        doVibrate(0);
        
        // Get article from DB
        article = DBHelper.getInstance().getArticle(articleId);
        if (article == null) {
            getActivity().finish();
            return;
        }
        feed = DBHelper.getInstance().getFeed(article.feedId);
        
        // Mark as read if necessary, do it here because in doRefresh() it will be done several times even if you set
        // it to "unread" in the meantime.
        if (article.isUnread) {
            article.isUnread = false;
            markedRead = true;
            new Updater(null, new ReadStateUpdater(article, feedId, 0)).exec();
        }
        
        getActivity().invalidateOptionsMenu(); // Force redraw of menu items in actionbar
        
        // Reload content on next doRefresh()
        webviewInitialized = false;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        getView().setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onStop() {
        // Check again to make sure it didnt get updated and marked as unread again in the background
        if (!markedRead) {
            if (article != null && article.isUnread)
                new Updater(null, new ReadStateUpdater(article, feedId, 0)).exec();
        }
        super.onStop();
        getView().setVisibility(View.GONE);
    }
    
    @Override
    public void onDestroy() {
        // Check again to make sure it didnt get updated and marked as unread again in the background
        if (!markedRead) {
            if (article != null && article.isUnread)
                new Updater(null, new ReadStateUpdater(article, feedId, 0)).exec();
        }
        if (parentAdapter != null)
            parentAdapter.close();
        super.onDestroy();
        if (webContainer != null)
            webContainer.removeAllViews();
        if (webView != null)
            webView.destroy();
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void doRefresh() {
        if (webView == null)
            return;
        
        try {
            ProgressBarManager.getInstance().addProgress(getSherlockActivity());
            
            if (Controller.getInstance().workOffline() || !Controller.getInstance().loadImages()) {
                webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
            } else {
                webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            }
            
            // No need to reload everything
            if (webviewInitialized)
                return;
            
            // Check for errors
            if (Controller.getInstance().getConnector().hasLastError()) {
                Intent i = new Intent(getActivity(), ErrorActivity.class);
                i.putExtra(ErrorActivity.ERROR_MESSAGE, Controller.getInstance().getConnector().pullLastError());
                startActivityForResult(i, ErrorActivity.ACTIVITY_SHOW_ERROR);
                return;
            }
            
            if (article.content == null)
                return;
            
            StringBuilder labels = new StringBuilder();
            for (Label label : article.labels) {
                if (label.checked) {
                    if (labels.length() > 0)
                        labels.append(", ");
                    
                    String labelString = label.caption;
                    if (label.foregroundColor != null && label.backgroundColor != null)
                        labelString = String.format(LABEL_COLOR_STRING, label.foregroundColor, label.backgroundColor,
                                label.caption);
                    labels.append(labelString);
                }
            }
            
            // Load html from Controller and insert content
            ST contentTemplate = new ST(Controller.htmlTemplate, TEMPLATE_DELIMITER_START, TEMPLATE_DELIMITER_END);
            
            contentTemplate.add(TEMPLATE_ARTICLE_VAR, article);
            contentTemplate.add(TEMPLATE_FEED_VAR, feed);
            contentTemplate.add(MARKER_CACHED_IMAGES, getCachedImagesJS(article.id));
            contentTemplate.add(MARKER_LABELS, labels.toString());
            contentTemplate.add(MARKER_UPDATED, DateUtils.getDateTimeCustom(getActivity(), article.updated));
            contentTemplate.add(MARKER_CONTENT, article.content);
            // Inject the specific code for attachments, <img> for images, http-link for Videos
            contentTemplate.add(MARKER_ATTACHMENTS, getAttachmentsMarkup(getActivity(), article.attachments));
            
            webView.getSettings().setLightTouchEnabled(true);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(articleJSInterface, "articleController");
            content = contentTemplate.render();
            webView.loadDataWithBaseURL("fake://ForJS", content, "text/html", "utf-8", null);
            
            if (!linkAutoOpened && article.content.length() < 3) {
                if (Controller.getInstance().openUrlEmptyArticle()) {
                    Log.i(Utils.TAG, "Article-Content is empty, opening URL in browser");
                    linkAutoOpened = true;
                    openLink();
                }
            }
            
            // Everything did load, we dont have to do this again.
            webviewInitialized = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.w(Utils.TAG, e.getClass().getSimpleName() + " in doRefresh(): " + e.getMessage() + " (" + e.getCause()
                    + ")");
        } finally {
            ProgressBarManager.getInstance().removeProgress(getSherlockActivity());
        }
    }
    
    /**
     * Starts a new activity with the url of the current article. This should open a webbrowser in most cases. If the
     * url contains spaces or newline-characters it is first trim()'ed.
     */
    public void openLink() {
        if (article.url == null || article.url.length() == 0)
            return;
        
        String url = article.url;
        if (article.url.contains(" ") || article.url.contains("\n"))
            url = url.trim();
        
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Log.e(Utils.TAG, "Couldn't find a suitable activity for the uri: " + url);
        }
    }
    
    public Article getArticle() {
        return article;
    }
    
    public int getArticleId() {
        return articleId;
    }
    
    private boolean doVibrate(int newIndex) {
        if (lastMove == 0)
            return false;
        if (parentAdapter.getIds().indexOf(articleId) == -1)
            return false;
        
        int index = parentAdapter.getIds().indexOf(articleId) + lastMove;
        if (index < 0 || index >= parentAdapter.getIds().size()) {
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(Utils.SHORT_VIBRATE);
            return true;
        }
        return false;
    }
    
    /**
     * Recursively walks all viewGroups and their Views inside the given ViewGroup and sets the background to black and,
     * in case a TextView is found, the Text-Color to white.
     * 
     * @param v
     *            the ViewGroup to walk through
     */
    private void setDarkBackground(ViewGroup v) {
        v.setBackgroundColor(getResources().getColor(R.color.darkBackground));
        
        for (int i = 0; i < v.getChildCount(); i++) { // View at index 0 seems to be this view itself.
            View vChild = v.getChildAt(i);
            
            if (vChild == null)
                continue;
            
            if (vChild instanceof TextView)
                ((TextView) vChild).setTextColor(Color.WHITE);
            
            if (vChild instanceof ViewGroup)
                setDarkBackground(((ViewGroup) vChild));
        }
    }
    
    /**
     * generate HTML code for attachments to be shown inside article
     * 
     * @param context
     *            current context
     * @param attachments
     *            collection of attachment URLs
     */
    private static String getAttachmentsMarkup(Context context, Set<String> attachments) {
        StringBuilder content = new StringBuilder();
        Map<String, Collection<String>> attachmentsByMimeType = FileUtils.groupFilesByMimeType(attachments);
        
        if (!attachmentsByMimeType.isEmpty()) {
            for (String mimeType : attachmentsByMimeType.keySet()) {
                Collection<String> mimeTypeUrls = attachmentsByMimeType.get(mimeType);
                if (!mimeTypeUrls.isEmpty()) {
                    if (mimeType.equals(FileUtils.IMAGE_MIME)) {
                        ST st = new ST(context.getResources().getString(R.string.ATTACHMENT_IMAGES_TEMPLATE));
                        st.add("items", mimeTypeUrls);
                        content.append(st.render());
                    } else {
                        ST st = new ST(context.getResources().getString(R.string.ATTACHMENT_MEDIA_TEMPLATE));
                        st.add("items", mimeTypeUrls);
                        CharSequence linkText = mimeType.equals(FileUtils.AUDIO_MIME)
                                || mimeType.equals(FileUtils.VIDEO_MIME) ? context
                                .getText(R.string.ArticleActivity_MediaPlay) : context
                                .getText(R.string.ArticleActivity_MediaDisplayLink);
                        st.add("linkText", linkText);
                        content.append(st.render());
                    }
                }
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
        
        if (result.getType() == HitTestResult.SRC_ANCHOR_TYPE) {
            mSelectedExtra = result.getExtra();
            menu.add(ContextMenu.NONE, CONTEXT_MENU_SHARE_URL, 2,
                    getResources().getString(R.string.ArticleActivity_ShareURL));
        }
        if (result.getType() == HitTestResult.IMAGE_TYPE) {
            mSelectedAltText = getAltTextForImageUrl(result.getExtra());
            if (mSelectedAltText != null)
                menu.add(ContextMenu.NONE, CONTEXT_MENU_DISPLAY_CAPTION, 1,
                        getResources().getString(R.string.ArticleActivity_ShowCaption));
        }
        menu.add(ContextMenu.NONE, CONTEXT_MENU_SHARE_ARTICLE, 10,
                getResources().getString(R.string.ArticleActivity_ShareArticle));
    }
    
    /**
     * Using a small html parser with a visitor which goes through the html I extract the alt-attribute from the
     * content. If nothing is found it is left as null and the menu should'nt contain the item to display the caption.
     * 
     * @param extra
     *            the
     * @return the alt-text or null if none was found.
     */
    private String getAltTextForImageUrl(String extra) {
        if (content == null || !content.contains(extra))
            return null;
        
        HtmlCleaner cleaner = new HtmlCleaner();
        TagNode node = cleaner.clean(content);
        
        MyTagNodeVisitor tnv = new MyTagNodeVisitor(extra);
        node.traverse(tnv);
        
        return tnv.alt;
    }
    
    /**
     * create javascript associative array with article cached image url as key and image hash as value
     * 
     * @param id
     *            article ID
     * 
     * @return javascript associative array content as text
     */
    private String getCachedImagesJS(int id) {
        StringBuilder hashes = new StringBuilder("");
        Collection<RemoteFile> rfs = DBHelper.getInstance().getRemoteFiles(id);
        
        if (rfs != null && !rfs.isEmpty()) {
            for (RemoteFile rf : rfs) {
                if (hashes.length() > 0) {
                    hashes.append(",\n");
                }
                hashes.append("'").append(rf.url).append("': '").append(ImageCache.getHashForKey(rf.url)).append("'");
            }
        }
        
        return hashes.toString();
    }
    
    class MyTagNodeVisitor implements TagNodeVisitor {
        public String alt = null;
        private String extra;
        
        public MyTagNodeVisitor(String extra) {
            this.extra = extra;
        }
        
        public boolean visit(TagNode tagNode, HtmlNode htmlNode) {
            if (htmlNode instanceof TagNode) {
                TagNode tag = (TagNode) htmlNode;
                String tagName = tag.getName();
                // Only if the image-url is the same as the url of the image the long-press was on:
                if ("img".equals(tagName) && extra.equals(tag.getAttributeByName("src"))) {
                    alt = tag.getAttributeByName("alt");
                    if (alt != null)
                        return false;
                }
            }
            return true;
        }
    };
    
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        Intent shareIntent = null;
        switch (item.getItemId()) {
            case CONTEXT_MENU_SHARE_URL:
                if (mSelectedExtra != null) {
                    shareIntent = getUrlShareIntent(mSelectedExtra);
                    startActivity(Intent.createChooser(shareIntent, "Share URL"));
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
            if (v.equals(buttonNext))
                openNextArticle(-1);
            else if (v.equals(buttonPrev))
                openNextArticle(1);
        }
    };
    
    private OnKeyListener keyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (Controller.getInstance().useVolumeKeys()) {
                if (keyCode == KeyEvent.KEYCODE_N) {
                    openNextArticle(-1);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_B) {
                    openNextArticle(1);
                    return true;
                }
            }
            return false;
        }
    };
    
    public void openNextArticle(int direction) {
        int id = direction < 0 ? parentIDs[0] : parentIDs[1];
        
        if (id == Integer.MIN_VALUE) {
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(Utils.SHORT_VIBRATE);
            return;
        }
        
        this.articleId = id;
        this.lastMove = direction;
        initData();
        doRefresh();
    }
    
    public void openArticle(int articleId, int feedId, int categoryId, boolean selectArticlesForCategory, int lastMove) {
        if (articleId == Integer.MIN_VALUE) {
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(Utils.SHORT_VIBRATE);
            return;
        }
        
        this.articleId = articleId;
        this.feedId = feedId;
        this.categoryId = categoryId;
        this.selectForCategory = selectArticlesForCategory;
        this.lastMove = lastMove;
        initData();
        doRefresh();
    }
    
    /**
     * this class represents an object, which methods can be called from article's {@code WebView} javascript to
     * manipulate the article activity
     */
    public class ArticleJSInterface {
        
        /**
         * current article activity
         */
        Activity activity;
        
        /**
         * public constructor, which saves calling activity as member variable
         * 
         * @param aa
         *            current article activity
         */
        public ArticleJSInterface(Activity aa) {
            activity = aa;
        }
        
        /**
         * go to previous article
         */
        @JavascriptInterface
        public void prev() {
            Log.d(Utils.TAG, "JS: PREV");
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Log.d(Utils.TAG, "JS: PREV");
                    openNextArticle(-1);
                }
            });
        }
        
        /**
         * go to next article
         */
        @JavascriptInterface
        public void next() {
            Log.d(Utils.TAG, "JS: NEXT");
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Log.d(Utils.TAG, "JS: NEXT");
                    openNextArticle(1);
                }
            });
        }
    }
    
    public String getTitle() {
        if (parentAdapter != null) {
            parentAdapter.refreshQuery();
            return "( " + parentAdapter.unreadCount + " )";
        }
        return "";
    }
    
    class ArticleGestureDetector extends MyGestureDetector {
        public ArticleGestureDetector(ActionBar actionBar, boolean hideActionbar) {
            super(actionBar, hideActionbar);
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // Refresh metrics-data in Controller
            Controller.refreshDisplayMetrics(((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay());
            
            try {
                if (Math.abs(e1.getY() - e2.getY()) > Controller.relSwipeMaxOffPath)
                    return false;
                if (e1.getX() - e2.getX() > Controller.relSwipeMinDistance
                        && Math.abs(velocityX) > Controller.relSwipteThresholdVelocity) {
                    
                    // right to left swipe
                    openNextArticle(1);
                    return true;
                    
                } else if (e2.getX() - e1.getX() > Controller.relSwipeMinDistance
                        && Math.abs(velocityX) > Controller.relSwipteThresholdVelocity) {
                    
                    // left to right swipe
                    openNextArticle(-1);
                    return true;
                    
                }
            } catch (Exception e) {
            }
            return false;
        }
    };
    
}
