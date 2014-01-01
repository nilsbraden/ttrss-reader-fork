package org.ttrssreader.gui;

import org.ttrssreader.R;
import org.ttrssreader.gui.interfaces.TextInputAlertCallback;
import org.ttrssreader.model.pojos.Article;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
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
        
        alert.setTitle(context.getString(R.string.Commons_MarkPublishNote));
        
        final EditText input = new EditText(context);
        input.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        input.setMinLines(3);
        input.setMaxLines(10);
        alert.setView(input);
        
        alert.setPositiveButton(context.getString(R.string.Utils_OkayAction), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                callback.onPublishNoteResult(article, value);
            }
        });
        
        alert.setNegativeButton(context.getString(R.string.Utils_CancelAction), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        
        alert.show();
    }
}
