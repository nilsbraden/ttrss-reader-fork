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

package org.ttrssreader.model.pojos;


public class Feed implements Comparable<Feed> {

	public int id;
	public int categoryId;
	public String title;
	public String url;
	public int unread;
	public byte[] icon;

	@Override
	public int compareTo(Feed fi) {
		return title.compareToIgnoreCase(fi.title);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Feed) {
			Feed other = (Feed) o;
			return (id == other.id);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return id + "".hashCode();
	}

}
