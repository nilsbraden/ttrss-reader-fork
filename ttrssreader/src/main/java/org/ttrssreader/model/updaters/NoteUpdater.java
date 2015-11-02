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
import org.ttrssreader.model.pojos.Article;

public class NoteUpdater implements IUpdatable {

	@SuppressWarnings("unused")
	private static final String TAG = NoteUpdater.class.getSimpleName();

	private Article article;
	private String note;

	/**
	 * Adds the given note to the article.
	 */
	public NoteUpdater(Article article, String note) {
		this.article = article;
		this.note = note;
	}

	@Override
	public void update() {
		if (note != null) {
			DBHelper.getInstance().addArticleNote(article.id, note);
			Data.getInstance().setArticleNote(article.id, note);
		}
	}

}
