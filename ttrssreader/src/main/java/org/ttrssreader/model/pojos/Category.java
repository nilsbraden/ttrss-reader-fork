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

import org.jetbrains.annotations.NotNull;

public class Category implements Comparable<Category> {

	public int id;
	public String title;
	public int unread;

	public Category() {
	}

	public Category(int id, String title, int unread) {
		this.id = id;
		this.title = title;
		this.unread = unread;
	}

	@Override
	public int compareTo(@NotNull Category ci) {
		// Sort by Id if Id is 0 or smaller, else sort by Title
		if (id <= 0 || ci.id <= 0) {
			Integer thisInt = id;
			Integer thatInt = ci.id;
			return thisInt.compareTo(thatInt);
		}
		return title.compareToIgnoreCase(ci.title);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Category) {
			Category other = (Category) o;
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
