package org.ttrssreader.utils;

import org.ttrssreader.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

public class ArticleActionBar extends RelativeLayout {
    
    public ArticleActionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.articleactionbar, this);
    }
    
}
