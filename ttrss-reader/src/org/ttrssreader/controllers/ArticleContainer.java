package org.ttrssreader.controllers;

import java.util.Date;
import java.util.Set;

public class ArticleContainer {
    int id;
    int feedId;
    String title;
    boolean isUnread;
    String articleUrl;
    String articleCommentUrl;
    Date updateDate;
    String content;
    Set<String> attachments;
    boolean isStarred;
    boolean isPublished;
    int label;
    
    public ArticleContainer(int id, int feedId, String title, boolean isUnread, String articleUrl,
            String articleCommentUrl, Date updateDate, String content, Set<String> attachments, boolean isStarred,
            boolean isPublished, int label) {
        this.id = id;
        this.feedId = feedId;
        this.title = (title == null ? "" : title);
        this.isUnread = isUnread;
        this.articleUrl = (articleUrl == null ? "" : articleUrl);
        this.articleCommentUrl = (articleCommentUrl == null ? "" : articleCommentUrl);
        this.updateDate = updateDate;
        this.content = (content == null ? "" : content);
        this.attachments = attachments;
        this.isStarred = isStarred;
        this.isPublished = isPublished;
    }
    
}
