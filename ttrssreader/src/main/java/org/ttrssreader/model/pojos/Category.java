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


import java.util.Comparator;

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

	/**
	 * @return true if this is a virtual category (eg. starred, fresh, published)
	 */
	public boolean isVirtualCategory() {
		return id < 1 && id > -11;
	}

	@Override
	public int compareTo(Category ci) {
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

	public static class CategoryComparator implements Comparator<Category> {

		private final boolean inverted;

		public CategoryComparator(boolean inverted) {
			this.inverted = inverted;
		}

		@Override
		public int compare(Category left, Category right) {
			// special categories are sorted before all others
			// sort for these is by id
			if (left.isVirtualCategory()) {
				if (right.isVirtualCategory()) {
					return Integer.compare(left.id, right.id);
				}
				return -1;
			}
			if (right.isVirtualCategory()) {
				return 1;
			}
			// neither are special category -> sort by title
			if (inverted) {
				return right.title.compareToIgnoreCase(left.title);
			} else {
				return left.title.compareToIgnoreCase(right.title);
			}
		}

	}

}
