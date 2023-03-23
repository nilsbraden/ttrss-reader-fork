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


import java.util.Date;

/**
 * this class represents remote file (image, attachment, etc)
 * belonging to article(s), which may be locally stored (cached)
 *
 * @author igor
 */
public class RemoteFile implements Comparable<RemoteFile> {
	/**
	 * numerical ID
	 */
	public int id;

	/**
	 * remote file URL
	 */
	public String url;

	/**
	 * file size
	 */
	public int length;

	/**
	 * last change date
	 */
	public Date updated;

	/**
	 * boolean flag determining if the file is locally stored
	 */
	public volatile boolean cached;

	@Override
	public int compareTo(RemoteFile rf) {
		return rf.updated.compareTo(this.updated);
	}

	@Override
	public boolean equals(Object o) {
		boolean isEqual = false;
		if (o instanceof RemoteFile) {
			RemoteFile rf = (RemoteFile) o;
			isEqual = (id == rf.id);
		}
		return isEqual;
	}

	@Override
	public int hashCode() {
		return id;
	}

}
