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

public class Feed implements Comparable<Feed> {

    public int id;
    public int categoryId;
    public String title;
    public String url;
    public int unread;

    public Feed() {
        this.id = 0;
        this.categoryId = 0;
        this.title = "";
        this.url = "";
        this.unread = 0;
    }

    public Feed(int id, int categoryId, String title, String url, int unread) {
        this.id = id;
        this.categoryId = categoryId;
        this.title = title;
        this.url = url;
        this.unread = unread;
    }

    @Override
    public int compareTo(@NotNull Feed fi) {
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
