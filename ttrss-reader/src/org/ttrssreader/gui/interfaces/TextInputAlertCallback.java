package org.ttrssreader.gui.interfaces;

import org.ttrssreader.model.pojos.Article;

public interface TextInputAlertCallback {
    public void onPublishNoteResult(Article a, String note);
}
