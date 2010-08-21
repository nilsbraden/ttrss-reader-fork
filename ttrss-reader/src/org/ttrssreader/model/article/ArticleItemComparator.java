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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.model.article;

import java.util.Comparator;

public class ArticleItemComparator implements Comparator<ArticleItem> {
	
	@Override
	public int compare(ArticleItem object1, ArticleItem object2) {
		return object2.getUpdateDate().compareTo(object1.getUpdateDate());
	}
	
}
