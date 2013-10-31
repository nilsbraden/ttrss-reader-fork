/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
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

package org.ttrssreader.gui;

import org.ttrssreader.R;
import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

public class MediaPlayerActivity extends Activity {
    
    public static final String URL = "media_url";
    
    private String url;
    private MediaPlayer mp;
    
    @Override
    protected void onCreate(Bundle instance) {
        super.onCreate(instance);
        
        setContentView(R.layout.media);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            url = extras.getString(URL);
        } else if (instance != null) {
            url = instance.getString(URL);
        } else {
            url = "";
        }
        
        VideoView videoView = (VideoView) findViewById(R.id.MediaView);
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        Uri video = Uri.parse(url);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(video);
        videoView.start();
    }
    
    @Override
    protected void onResume() {
        if (mp != null)
            mp.start();
        super.onResume();
    }
    
    @Override
    protected void onPause() {
        if (mp != null)
            mp.pause();
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mp != null)
            mp.release();
    }
    
}
