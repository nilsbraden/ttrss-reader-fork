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

package org.ttrssreader.model.updaters;

import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;

import java.util.Collection;
import java.util.HashSet;

public class ReadStateUpdater implements IUpdatable {

	//	private static final String TAG = ReadStateUpdater.class.getSimpleName();

	public enum TYPE {
		ALL_CATEGORIES, ALL_FEEDS, CATEGORY, FEED, ARTICLE
	}

	private final TYPE type;
	private final int id;

	private Collection<Category> categories = null;
	private Collection<Feed> feeds = null;

	public ReadStateUpdater(TYPE type) {
		this(type, -1);
	}

	private ReadStateUpdater(TYPE type, int id) {
		this.type = type;
		this.id = id;
	}

	public ReadStateUpdater(int categoryId) {
		type = TYPE.CATEGORY;
		id = categoryId;
	}

	public ReadStateUpdater(int feedId, int dummy) {
		if (feedId > 0 || feedId < -4) {
			type = TYPE.FEED;
		} else { // Virtual Category
			type = TYPE.CATEGORY;
		}
		id = feedId;
	}

	@Override
	public void update() {
		// Read appropriate data from the DB
		switch (type) {
			case ALL_CATEGORIES:
				categories = DBHelper.getInstance().getAllCategories();
				break;
			case CATEGORY:
				categories = new HashSet<>();
				Category c = DBHelper.getInstance().getCategory(id);
				if (c != null)
					categories.add(c);
				break;
			case ALL_FEEDS:
				feeds = DBHelper.getInstance().getFeeds(id);
				break;
			case FEED:
				feeds = new HashSet<>();
				Feed f = DBHelper.getInstance().getFeed(id);
				if (f != null)
					feeds.add(f);
				break;
			default:
				break;
		}

		if (categories != null) {
			for (Category ci : categories) {
				// VirtualCats are actually Feeds (the server handles them as such) so we have to set isCat to false
				Data.getInstance().setRead(ci.id, ci.id >= 0);
			}
		} else if (feeds != null) {
			for (Feed fi : feeds) {
				Data.getInstance().setRead(fi.id, false);
			}
		}

		Data.getInstance().calculateCounters();
		Data.getInstance().notifyListeners();
	}

}
