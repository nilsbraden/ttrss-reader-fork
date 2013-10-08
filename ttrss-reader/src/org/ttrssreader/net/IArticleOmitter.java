/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2013 I. Lubimov.
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
package org.ttrssreader.net;

import org.ttrssreader.model.pojos.Article;

/**
 * this interface is supposed to be used inside parseArticleArray of JSONConnector. The {@code omitArticle} method will
 * be called for each article field to determine if the article can be already omitted.
 * 
 * @author igor
 */
public interface IArticleOmitter {
    /**
     * this method should return {@code true} if given article should not be processed
     * 
     * @param field
     *            current article field added to article on this iteration
     * @param a
     *            article to test
     * @return {@code true} if given article should be omitted, {@code false} otherwise
     * @throws StopProcessingException
     *             if parsing process should be broken
     */
    public boolean omitArticle(Article.ArticleField field, Article a) throws StopJsonParsingException;
}
