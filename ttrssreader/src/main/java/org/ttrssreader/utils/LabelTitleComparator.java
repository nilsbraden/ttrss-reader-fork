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

package org.ttrssreader.utils;

import org.ttrssreader.model.pojos.Label;

import java.io.Serializable;
import java.util.Comparator;

@SuppressWarnings("serial")
public class LabelTitleComparator implements Comparator<Label>, Serializable {

	public static final Comparator<Label> LABELTITLE_COMPARATOR = new LabelTitleComparator();

	public int compare(Label obj1, Label obj2) {
		if (obj1 == null || obj2 == null)
			throw new NullPointerException();
		return obj1.caption.compareTo(obj2.caption);
	}

}
