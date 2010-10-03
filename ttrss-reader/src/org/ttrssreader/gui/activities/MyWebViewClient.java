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

package org.ttrssreader.gui.activities;

import java.io.IOException;
import org.ttrssreader.utils.Utils;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MyWebViewClient extends WebViewClient {
    
    private Context context;
    
    public boolean shouldOverrideUrlLoading(WebView view, final String url) {

        Log.e(Utils.TAG, "Link clicked: " + url);
        context = view.getContext();
        boolean media = false;
        
        for (String s : Utils.MEDIA_EXTENSIONS) {
            if (url.toLowerCase().contains(s)) {
                media = true;
                break;
            }
        }
        
        if (media) {
            
            final CharSequence[] items = {"Display in Mediaplayer", "Download"};

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("What shall we do?");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch (item) {
                        case 0:
                            Log.e(Utils.TAG, "Displaying file in mediaplayer: " + url);
                            displayFile(url);
                            break;
                        case 1:
                            Log.e(Utils.TAG, "Downloading not yet implemented...");
                            downloadFile(url);
                            break;
                        default:
                            Log.e(Utils.TAG, "Doing nothing, but why is that?? Item: " + item);
                            break;
                    }
                }
            });
            AlertDialog alert = builder.create();
            alert.show();

        } else {
            
            Uri uri = Uri.parse(url);
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
            
        }
        
        return true;
    }
    
    private void displayFile(String url) {
        
        // TODO: MediaPlayer in new Intent? is in background atm...
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(url);
            mp.prepare();
            mp.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void downloadFile(String url) {
        // TODO: implement
    }
}
