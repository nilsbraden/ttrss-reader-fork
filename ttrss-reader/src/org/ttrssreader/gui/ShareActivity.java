package org.ttrssreader.gui;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.utils.AsyncTask;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.actionbarsherlock.view.Menu;

public class ShareActivity extends MenuActivity {
    
    private static final String PARAM_TITLE = "title";
    private static final String PARAM_URL = "url";
    private static final String PARAM_CONTENT = "content";
    
    private Button shareButton;
    private EditText title;
    private EditText url;
    private EditText content;
    
    private ProgressDialog progress;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sharetopublished);
        
        String titleValue = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        String urlValue = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        String contentValue = "";
        
        if (savedInstanceState != null) {
            titleValue = savedInstanceState.getString(PARAM_TITLE);
            urlValue = savedInstanceState.getString(PARAM_URL);
            contentValue = savedInstanceState.getString(PARAM_CONTENT);
        }
        
        title = (EditText) findViewById(R.id.title);
        url = (EditText) findViewById(R.id.url);
        content = (EditText) findViewById(R.id.content);
        
        title.setText(titleValue);
        url.setText(urlValue);
        content.setText(contentValue);
        
        shareButton = (Button) findViewById(R.id.share_ok_button);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progress = ProgressDialog.show(context, null, "Sending...");
                new MyPublisherTask().execute();
            }
        });
    }
    
    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        
        EditText url = (EditText) findViewById(R.id.url);
        EditText title = (EditText) findViewById(R.id.title);
        EditText content = (EditText) findViewById(R.id.content);
        out.putString(PARAM_TITLE, title.getText().toString());
        out.putString(PARAM_URL, url.getText().toString());
        out.putString(PARAM_CONTENT, content.getText().toString());
    }
    
    public class MyPublisherTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            
            String titleValue = title.getText().toString();
            String urlValue = url.getText().toString();
            String contentValue = content.getText().toString();
            
            try {
                boolean ret = Data.getInstance().shareToPublished(titleValue, urlValue, contentValue);
                progress.dismiss();
                
                if (ret)
                    finishCompat();
                else if (Controller.getInstance().getConnector().hasLastError())
                    showErrorDialog(Controller.getInstance().getConnector().pullLastError());
                else if (Controller.getInstance().workOffline())
                    showErrorDialog("Working offline, synchronisation of published articles is not implemented yet.");
                else
                    showErrorDialog("An unknown error occurred.");
                
            } catch (RuntimeException r) {
                showErrorDialog(r.getMessage());
            }
            
            return null;
        }
        
        private void finishCompat() {
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
                finishAffinity();
            else
                finish();
        }
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (super.onCreateOptionsMenu(menu)) {
            menu.removeGroup(R.id.group_normal_primary);
            menu.removeGroup(R.id.group_normal_secondary);
        }
        return true;
    }
    
    // @formatter:off // Not needed here:
    @Override public void itemSelected(TYPE type, int selectedIndex, int oldIndex, int selectedId) { }
    @Override protected void doRefresh() { }
    @Override protected void doUpdate(boolean forceUpdate) { }
    @Override protected void onDataChanged() { }
    //@formatter:on
    
}
