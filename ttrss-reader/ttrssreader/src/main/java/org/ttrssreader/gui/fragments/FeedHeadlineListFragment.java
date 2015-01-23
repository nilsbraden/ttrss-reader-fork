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
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.gui.FeedHeadlineActivity;
import org.ttrssreader.gui.TextInputAlert;
import org.ttrssreader.gui.dialogs.YesNoUpdaterDialog;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.gui.interfaces.TextInputAlertCallback;
import org.ttrssreader.gui.view.MyGestureDetector;
import org.ttrssreader.model.FeedAdapter;
import org.ttrssreader.model.FeedHeadlineAdapter;
import org.ttrssreader.model.ListContentProvider;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.updaters.ArticleReadStateUpdater;
import org.ttrssreader.model.updaters.PublishedStateUpdater;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.StarredStateUpdater;
import org.ttrssreader.model.updaters.UnsubscribeUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.Utils;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import java.util.ArrayList;
import java.util.List;

public class FeedHeadlineListFragment extends MainListFragment implements TextInputAlertCallback {

    @SuppressWarnings("unused")
    private static final String TAG = FeedHeadlineListFragment.class.getSimpleName();

    private static final TYPE THIS_TYPE = TYPE.FEEDHEADLINE;
    public static final String FRAGMENT = "FEEDHEADLINE_FRAGMENT";

    public static final String FEED_CAT_ID = "FEED_CAT_ID";
    public static final String FEED_ID = "ARTICLE_FEED_ID";
    public static final String ARTICLE_ID = "ARTICLE_ID";
    public static final String FEED_SELECT_ARTICLES = "FEED_SELECT_ARTICLES";

    private static final int MARK_GROUP = 200;
    private static final int MARK_READ = MARK_GROUP + 1;
    private static final int MARK_STAR = MARK_GROUP + 2;
    private static final int MARK_PUBLISH = MARK_GROUP + 3;
    private static final int MARK_PUBLISH_NOTE = MARK_GROUP + 4;
    private static final int MARK_ABOVE_READ = MARK_GROUP + 5;
    private static final int SHARE = MARK_GROUP + 6;

    private int categoryId = Integer.MIN_VALUE;
    private int feedId = Integer.MIN_VALUE;
    private int articleId = Integer.MIN_VALUE;
    private boolean selectArticlesForCategory = false;

    private FeedAdapter parentAdapter;
    private List<Integer> parentIds = null;
    private int[] parentIdsBeforeAndAfter = new int[2];

    private Uri headlineUri;
    private Uri feedUri;

    public static FeedHeadlineListFragment newInstance(int id, int categoryId, boolean selectArticles, int articleId) {
        FeedHeadlineListFragment detail = new FeedHeadlineListFragment();
        detail.categoryId = categoryId;
        detail.feedId = id;
        detail.selectArticlesForCategory = selectArticles;
        detail.articleId = articleId;
        detail.setRetainInstance(true);
        return detail;
    }

    @Override
    public void onCreate(Bundle instance) {
        if (instance != null) {
            categoryId = instance.getInt(FEED_CAT_ID);
            feedId = instance.getInt(FEED_ID);
            selectArticlesForCategory = instance.getBoolean(FEED_SELECT_ARTICLES);
            articleId = instance.getInt(ARTICLE_ID);
        }

        if (feedId > 0)
            Controller.getInstance().lastOpenedFeeds.add(feedId);
        Controller.getInstance().lastOpenedArticles.clear();
        setHasOptionsMenu(true);
        super.onCreate(instance);
    }

    @Override
    public void onActivityCreated(Bundle instance) {
        adapter = new FeedHeadlineAdapter(getActivity(), feedId, selectArticlesForCategory);
        getLoaderManager().restartLoader(TYPE_HEADLINE_ID, null, this);

        super.onActivityCreated(instance);

        // Detect touch gestures like swipe and scroll down:
        ActionBar actionBar = getActivity().getActionBar();
        gestureDetector = new GestureDetector(getActivity(), new HeadlineGestureDetector(actionBar, Controller
                .getInstance().hideActionbar()));
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event) || v.performClick();
            }
        };
        getView().setOnTouchListener(gestureListener);

        parentAdapter = new FeedAdapter(getActivity());
        getLoaderManager().restartLoader(TYPE_FEED_ID, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(FEED_CAT_ID, categoryId);
        outState.putInt(FEED_ID, feedId);
        outState.putBoolean(FEED_SELECT_ARTICLES, selectArticlesForCategory);
        outState.putInt(ARTICLE_ID, articleId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Get selected Article
        AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Article a = (Article) adapter.getItem(info.position);

        menu.add(MARK_GROUP, MARK_ABOVE_READ, Menu.NONE, R.string.Commons_MarkAboveRead);

        if (a.isUnread) {
            menu.add(MARK_GROUP, MARK_READ, Menu.NONE, R.string.Commons_MarkRead);
        } else {
            menu.add(MARK_GROUP, MARK_READ, Menu.NONE, R.string.Commons_MarkUnread);
        }

        if (a.isStarred) {
            menu.add(MARK_GROUP, MARK_STAR, Menu.NONE, R.string.Commons_MarkUnstar);
        } else {
            menu.add(MARK_GROUP, MARK_STAR, Menu.NONE, R.string.Commons_MarkStar);
        }

        if (a.isPublished) {
            menu.add(MARK_GROUP, MARK_PUBLISH, Menu.NONE, R.string.Commons_MarkUnpublish);
        } else {
            menu.add(MARK_GROUP, MARK_PUBLISH, Menu.NONE, R.string.Commons_MarkPublish);
            menu.add(MARK_GROUP, MARK_PUBLISH_NOTE, Menu.NONE, R.string.Commons_MarkPublishNote);
        }

        menu.add(MARK_GROUP, SHARE, Menu.NONE, R.string.ArticleActivity_ShareLink);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        if (cmi == null)
            return false;

        Article a = (Article) adapter.getItem(cmi.position);
        if (a == null)
            return false;

        switch (item.getItemId()) {
            case MARK_READ:
                new Updater(getActivity(), new ArticleReadStateUpdater(a, feedId, a.isUnread ? 0 : 1))
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            case MARK_STAR:
                new Updater(getActivity(), new StarredStateUpdater(a, a.isStarred ? 0 : 1))
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            case MARK_PUBLISH:
                new Updater(getActivity(), new PublishedStateUpdater(a, a.isPublished ? 0 : 1))
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            case MARK_PUBLISH_NOTE:
                new TextInputAlert(this, a).show(getActivity());
                break;
            case MARK_ABOVE_READ:
                new Updater(getActivity(), new ArticleReadStateUpdater(getUnreadAbove(cmi.position), feedId, 0))
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            case SHARE:
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, a.url);
                i.putExtra(Intent.EXTRA_SUBJECT, a.title);
                startActivity(Intent.createChooser(i, (String) getText(R.string.ArticleActivity_ShareTitle)));
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (super.onOptionsItemSelected(item))
            return true;

        switch (item.getItemId()) {
            case R.id.Menu_MarkFeedRead: {
                boolean backAfterUpdate = Controller.getInstance().goBackAfterMarkAllRead();
                if (selectArticlesForCategory) {
                    YesNoUpdaterDialog dialog = YesNoUpdaterDialog
                            .getInstance(new ReadStateUpdater(categoryId), R.string.Dialog_Title,
                                    R.string.Dialog_MarkAllRead, backAfterUpdate);
                    dialog.show(getFragmentManager(), YesNoUpdaterDialog.DIALOG);
                } else if (feedId >= -4 && feedId < 0) { // Virtual Category
                    YesNoUpdaterDialog dialog = YesNoUpdaterDialog
                            .getInstance(new ReadStateUpdater(feedId, 42), R.string.Dialog_Title,
                                    R.string.Dialog_MarkAllRead, backAfterUpdate);
                    dialog.show(getFragmentManager(), YesNoUpdaterDialog.DIALOG);
                } else {
                    new Updater(getActivity(), new ReadStateUpdater(feedId, 42), backAfterUpdate)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                return true;
            }
            case R.id.Menu_FeedUnsubscribe: {
                YesNoUpdaterDialog dialog = YesNoUpdaterDialog.getInstance(new UnsubscribeUpdater(feedId),
                        R.string.Dialog_unsubscribeTitle, R.string.Dialog_unsubscribeText);
                dialog.show(getFragmentManager(), YesNoUpdaterDialog.DIALOG);
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * Creates a list of articles which are above the given index in the currently displayed list of items.
     *
     * @param index the selected index, will be excluded in returned list
     * @return a list of items above the selected item
     */
    private List<Article> getUnreadAbove(int index) {
        List<Article> ret = new ArrayList<Article>();
        for (int i = 0; i < index; i++) {
            Article a = (Article) adapter.getItem(i);
            if (a != null && a.isUnread)
                ret.add(a);
        }
        return ret;
    }

    public void onPublishNoteResult(Article a, String note) {
        new Updater(getActivity(), new PublishedStateUpdater(a, a.isPublished ? 0 : 1, note))
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public TYPE getType() {
        return THIS_TYPE;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public int getFeedId() {
        return feedId;
    }

    public int getArticleId() {
        return articleId;
    }

    public boolean getSelectArticlesForCategory() {
        return selectArticlesForCategory;
    }

    private class HeadlineGestureDetector extends MyGestureDetector {
        private HeadlineGestureDetector(ActionBar actionBar, boolean hideActionbar) {
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
                    FeedHeadlineActivity activity = (FeedHeadlineActivity) getActivity();
                    activity.openNextFeed(1);
                    return true;

                } else if (e2.getX() - e1.getX() > Controller.relSwipeMinDistance
                        && Math.abs(velocityX) > Controller.relSwipteThresholdVelocity) {

                    // left to right swipe
                    FeedHeadlineActivity activity = (FeedHeadlineActivity) getActivity();
                    activity.openNextFeed(-1);
                    return true;

                }
            } catch (Exception e) {
            }
            return false;
        }
    }

    ;

    private void fillParentInformation() {
        if (parentIds == null) {
            parentIds = new ArrayList<Integer>(parentAdapter.getCount() + 2);

            parentIds.add(Integer.MIN_VALUE);
            parentIds.addAll(parentAdapter.getIds());
            parentIds.add(Integer.MIN_VALUE);

            parentAdapter.notifyDataSetInvalidated(); // Not needed anymore
        }

        // Added dummy-elements at top and bottom of list for easier access, index == 0 cannot happen.
        int index = -1;
        int i = 0;
        for (Integer id : parentIds) {
            if (id.intValue() == feedId) {
                index = i;
                break;
            }
            i++;
        }
        if (index > 0) {
            parentIdsBeforeAndAfter[0] = parentIds.get(index - 1); // Previous
            parentIdsBeforeAndAfter[1] = parentIds.get(index + 1); // Next
        } else {
            parentIdsBeforeAndAfter[0] = Integer.MIN_VALUE;
            parentIdsBeforeAndAfter[1] = Integer.MIN_VALUE;
        }
    }

    public int openNextFeed(int direction) {
        if (feedId < 0)
            return feedId;

        int id = direction < 0 ? parentIdsBeforeAndAfter[0] : parentIdsBeforeAndAfter[1];
        if (id == Integer.MIN_VALUE) {
            Utils.alert(getActivity(), true);
            return feedId;
        }

        feedId = id;
        adapter = new FeedHeadlineAdapter(getActivity(), feedId, selectArticlesForCategory);
        setListAdapter(adapter);
        getLoaderManager().restartLoader(TYPE_HEADLINE_ID, null, this);

        fillParentInformation();

        // Find next id in this direction and see if there is another next article or not
        id = direction < 0 ? parentIdsBeforeAndAfter[0] : parentIdsBeforeAndAfter[1];
        if (id == Integer.MIN_VALUE)
            Utils.alert(getActivity());

        if (feedId > 0)
            Controller.getInstance().lastOpenedFeeds.add(feedId);
        Controller.getInstance().lastOpenedArticles.clear();

        getActivity().invalidateOptionsMenu(); // Force redraw of menu items in actionbar
        return feedId;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case TYPE_HEADLINE_ID: {
                Builder builder = ListContentProvider.CONTENT_URI_HEAD.buildUpon();
                builder.appendQueryParameter(ListContentProvider.PARAM_CAT_ID, categoryId + "");
                builder.appendQueryParameter(ListContentProvider.PARAM_FEED_ID, feedId + "");
                builder.appendQueryParameter(ListContentProvider.PARAM_SELECT_FOR_CAT, (selectArticlesForCategory ? "1"
                        : "0"));
                headlineUri = builder.build();
                return new CursorLoader(getActivity(), headlineUri, null, null, null, null);
            }
            case TYPE_FEED_ID: {
                Builder builder = ListContentProvider.CONTENT_URI_FEED.buildUpon();
                builder.appendQueryParameter(ListContentProvider.PARAM_CAT_ID, categoryId + "");
                feedUri = builder.build();
                return new CursorLoader(getActivity(), feedUri, null, null, null, null);
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case TYPE_HEADLINE_ID:
                adapter.changeCursor(data);
                break;
            case TYPE_FEED_ID:
                parentAdapter.changeCursor(data);
                fillParentInformation();
                break;
        }
        super.onLoadFinished(loader, data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case TYPE_HEADLINE_ID:
                adapter.changeCursor(null);
                break;
            case TYPE_FEED_ID:
                parentAdapter.changeCursor(null);
                break;
        }
    }

    @Override
    protected void fetchOtherData() {
        if (selectArticlesForCategory) {
            Category category = DBHelper.getInstance().getCategory(categoryId);
            if (category != null)
                title = category.title;
        } else if (feedId >= -4 && feedId < 0) { // Virtual Category
            Category category = DBHelper.getInstance().getCategory(feedId);
            if (category != null)
                title = category.title;
        } else {
            Feed feed = DBHelper.getInstance().getFeed(feedId);
            if (feed != null)
                title = feed.title;
        }
        unreadCount = DBHelper.getInstance().getUnreadCount(selectArticlesForCategory ? categoryId : feedId,
                selectArticlesForCategory);
    }

    @Override
    public void doRefresh() {
        // getLoaderManager().restartLoader(TYPE_HEADLINE_ID, null, this);
        Activity activity = getActivity();
        if (activity != null && headlineUri != null)
            activity.getContentResolver().notifyChange(headlineUri, null);
        super.doRefresh();
    }

}
