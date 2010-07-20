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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.model.feed;

import java.util.Comparator;

public class FeedItemComparator implements Comparator<FeedItem> {

	@Override
	public int compare(FeedItem object1, FeedItem object2) {
		return object1.getTitle().compareToIgnoreCase(object2.getTitle());
	}

}
