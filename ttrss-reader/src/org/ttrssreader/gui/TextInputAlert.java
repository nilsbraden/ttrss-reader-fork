package org.ttrssreader.gui;

import org.ttrssreader.gui.interfaces.TextInputAlertCallback;
import org.ttrssreader.model.pojos.Article;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

public class TextInputAlert {
    
    private Article article;
    private TextInputAlertCallback callback;
    
    public TextInputAlert(TextInputAlertCallback callback, Article article) {
        this.callback = callback;
        this.article = article;
    }
    
    public void show(Context context) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        
        alert.setTitle("Publish with a note");
        alert.setMessage("");
        
        final EditText input = new EditText(context);
        alert.setView(input);
        
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                callback.onPublishNoteResult(article, value);
            }
        });
        
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        
        alert.show();
    }
}
