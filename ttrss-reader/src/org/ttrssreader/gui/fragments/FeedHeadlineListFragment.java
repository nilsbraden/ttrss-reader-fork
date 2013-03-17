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

import java.util.ArrayList;
import java.util.List;
import org.ttrssreader.R;
import org.ttrssreader.gui.MenuActivity;
import org.ttrssreader.gui.TextInputAlert;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.gui.interfaces.IUpdateEndListener;
import org.ttrssreader.gui.interfaces.TextInputAlertCallback;
import org.ttrssreader.model.FeedHeadlineAdapter;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.updaters.PublishedStateUpdater;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.StarredStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class FeedHeadlineListFragment extends ListFragment implements IUpdateEndListener, TextInputAlertCallback {
    
    private static final TYPE THIS_TYPE = TYPE.FEEDHEADLINE;
    
    public static final String FEED_CAT_ID = "FEED_CAT_ID";
    public static final String FEED_ID = "ARTICLE_FEED_ID";
    public static final String FEED_TITLE = "FEED_TITLE";
    public static final String FEED_SELECT_ARTICLES = "FEED_SELECT_ARTICLES";
    public static final String FEED_INDEX = "INDEX";
    
    private static final int SELECTED_INDEX_DEFAULT = -1;
    private int selectedIndex = SELECTED_INDEX_DEFAULT;
    private int selectedIndexOld = SELECTED_INDEX_DEFAULT;
    
    private int categoryId = -1000;
    private int feedId = -1000;
    private boolean selectArticlesForCategory = false;
    
    private FeedHeadlineAdapter adapter = null;
    private ListView listView;
    
    public static FeedHeadlineListFragment newInstance(int id, int categoryId, boolean selectArticles) {
        FeedHeadlineListFragment detail = new FeedHeadlineListFragment();
        detail.categoryId = categoryId;
        detail.feedId = id;
        detail.selectArticlesForCategory = selectArticles;
        detail.setHasOptionsMenu(true);
        return detail;
    }
    
    @Override
    public void onStop() {
        super.onStop();
        getListView().setVisibility(View.GONE);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null)
            adapter.makeQuery(true);
        getListView().setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);
        
        listView = getListView();
        registerForContextMenu(listView);
        
        if (instance != null) {
            categoryId = instance.getInt(FEED_CAT_ID);
            feedId = instance.getInt(FEED_ID);
            selectArticlesForCategory = instance.getBoolean(FEED_SELECT_ARTICLES);
        }
        
        adapter = new FeedHeadlineAdapter(getActivity(), feedId, categoryId, selectArticlesForCategory);
        setListAdapter(adapter);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(FEED_CAT_ID, categoryId);
        outState.putInt(FEED_ID, feedId);
        outState.putBoolean(FEED_SELECT_ARTICLES, selectArticlesForCategory);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        selectedIndexOld = selectedIndex;
        selectedIndex = position; // Set selected item
        
        if (getActivity() instanceof IItemSelectedListener)
            ((IItemSelectedListener) getActivity()).itemSelected(THIS_TYPE, selectedIndex, selectedIndexOld,
                    adapter.getId(selectedIndex));
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        // Get selected Article
        AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Article a = (Article) adapter.getItem(info.position);
        menu.removeItem(MenuActivity.MARK_READ); // Remove "Mark read" from super-class
        
        menu.add(MenuActivity.MARK_GROUP, MenuActivity.MARK_ABOVE_READ, Menu.NONE, R.string.Commons_MarkAboveRead);
        
        if (a.isUnread) {
            menu.add(MenuActivity.MARK_GROUP, MenuActivity.MARK_READ, Menu.NONE, R.string.Commons_MarkRead);
        } else {
            menu.add(MenuActivity.MARK_GROUP, MenuActivity.MARK_READ, Menu.NONE, R.string.Commons_MarkUnread);
        }
        
        if (a.isStarred) {
            menu.add(MenuActivity.MARK_GROUP, MenuActivity.MARK_STAR, Menu.NONE, R.string.Commons_MarkUnstar);
        } else {
            menu.add(MenuActivity.MARK_GROUP, MenuActivity.MARK_STAR, Menu.NONE, R.string.Commons_MarkStar);
        }
        
        if (a.isPublished) {
            menu.add(MenuActivity.MARK_GROUP, MenuActivity.MARK_PUBLISH, Menu.NONE, R.string.Commons_MarkUnpublish);
        } else {
            menu.add(MenuActivity.MARK_GROUP, MenuActivity.MARK_PUBLISH, Menu.NONE, R.string.Commons_MarkPublish);
            menu.add(MenuActivity.MARK_GROUP, MenuActivity.MARK_PUBLISH_NOTE, Menu.NONE,
                    R.string.Commons_MarkPublishNote);
        }
        
    }
    
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        Article a = (Article) adapter.getItem(cmi.position);
        
        if (a == null)
            return false;
        
        switch (item.getItemId()) {
            case MenuActivity.MARK_READ:
                new Updater(this, new ReadStateUpdater(a, feedId, a.isUnread ? 0 : 1)).exec();
                break;
            case MenuActivity.MARK_STAR:
                new Updater(this, new StarredStateUpdater(a, a.isStarred ? 0 : 1)).exec();
                break;
            case MenuActivity.MARK_PUBLISH:
                new Updater(this, new PublishedStateUpdater(a, a.isPublished ? 0 : 1)).exec();
                break;
            case MenuActivity.MARK_PUBLISH_NOTE:
                new TextInputAlert(this, a).show(getActivity());
                break;
            case MenuActivity.MARK_ABOVE_READ:
                new Updater(this, new ReadStateUpdater(getUnreadArticlesAbove(cmi.position), feedId, 0)).exec();
                break;
            default:
                return false;
        }
        return true;
    }
    
    /**
     * Creates a list of articles which are above the given index in the currently displayed list of items.
     * 
     * @param index
     *            the selected index, will be excluded in returned list
     * @return a list of items above the selected item
     */
    private List<Article> getUnreadArticlesAbove(int index) {
        List<Article> ret = new ArrayList<Article>();
        for (int i = 0; i < index; i++) {
            ret.add((Article) adapter.getItem(i));
        }
        return ret;
    }
    
    @Override
    public void onUpdateEnd() {
        adapter.refreshQuery();
    }
    
    public void onPublishNoteResult(Article a, String note) {
        new Updater(this, new PublishedStateUpdater(a, a.isPublished ? 0 : 1, note)).exec();
    }
    
}
