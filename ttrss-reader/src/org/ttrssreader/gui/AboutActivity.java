/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
 * Copyright (C) 2009-2010 J. Devauchelle.
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

import java.util.Date;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.utils.PostMortemReportExceptionHandler;
import org.ttrssreader.utils.Utils;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class AboutActivity extends Activity {
    
    protected static final String TAG = AboutActivity.class.getSimpleName();
    
    protected PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Controller.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        mDamageReport.initialize();
        
        Window w = getWindow();
        w.requestFeature(Window.FEATURE_LEFT_ICON);
        
        setContentView(R.layout.about);
        
        w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_info);
        
        TextView versionText = (TextView) this.findViewById(R.id.AboutActivity_VersionText);
        versionText.setText(this.getString(R.string.AboutActivity_VersionText) + " " + Utils.getAppVersionName(this));
        TextView versionCodeText = (TextView) this.findViewById(R.id.AboutActivity_VersionCodeText);
        versionCodeText.setText(this.getString(R.string.AboutActivity_VersionCodeText) + " "
                + Utils.getAppVersionCode(this));
        
        TextView licenseText = (TextView) this.findViewById(R.id.AboutActivity_LicenseText);
        licenseText.setText(this.getString(R.string.AboutActivity_LicenseText) + " "
                + this.getString(R.string.AboutActivity_LicenseTextValue));
        
        TextView urlText = (TextView) this.findViewById(R.id.AboutActivity_UrlText);
        urlText.setText(this.getString(R.string.AboutActivity_UrlTextValue));
        
        TextView lastSyncText = (TextView) this.findViewById(R.id.AboutActivity_LastSyncText);
        lastSyncText.setText(this.getString(R.string.AboutActivity_LastSyncText) + " "
                + new Date(Controller.getInstance().getLastSync()));
        
        TextView thanksText = (TextView) this.findViewById(R.id.AboutActivity_ThanksText);
        thanksText.setText(this.getString(R.string.AboutActivity_ThanksTextValue));
        
        Button closeBtn = (Button) this.findViewById(R.id.AboutActivity_CloseBtn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            
            public void onClick(View view) {
                closeButtonPressed();
            }
        });
        
        Button donateBtn = (Button) this.findViewById(R.id.AboutActivity_DonateBtn);
        donateBtn.setOnClickListener(new View.OnClickListener() {
            
            public void onClick(View view) {
                donateButtonPressed();
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        mDamageReport.restoreOriginalHandler();
        mDamageReport = null;
        super.onDestroy();
    }
    
    private void closeButtonPressed() {
        this.finish();
    }
    
    private void donateButtonPressed() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.DonateUrl))));
        this.finish();
    }
}
