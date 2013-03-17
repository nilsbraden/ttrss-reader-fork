/*
 * Copyright 2013 two forty four a.m. LLC <http://www.twofortyfouram.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <http://www.apache.org/licenses/LICENSE-2.0>
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.ttrssreader.imageCache;

import org.ttrssreader.imageCache.bundle.BundleScrubber;
import org.ttrssreader.imageCache.bundle.PluginBundleManager;
import org.ttrssreader.utils.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in setting.
 * 
 * @see com.twofortyfouram.locale.Intent#ACTION_FIRE_SETTING
 * @see com.twofortyfouram.locale.Intent#EXTRA_BUNDLE
 */
public final class PluginReceiver extends BroadcastReceiver {
    
    public static final String ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING";
    public static final String EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE";
    
    /**
     * @param context
     *            {@inheritDoc}.
     * @param intent
     *            the incoming {@link com.twofortyfouram.locale.Intent#ACTION_FIRE_SETTING} Intent. This
     *            should contain the {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} that was saved by
     *            {@link EditActivity} and later broadcast by Locale.
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {
        /*
         * Always be strict on input parameters! A malicious third-party app could send a malformed Intent.
         */
        
        if (!ACTION_FIRE_SETTING.equals(intent.getAction())) {
            Log.e(Utils.TAG, "Received unexpected Intent action " + intent.getAction());
            return;
        }
        
        BundleScrubber.scrub(intent);
        
        final Bundle bundle = intent.getBundleExtra(EXTRA_BUNDLE);
        BundleScrubber.scrub(bundle);
        
        if (!PluginBundleManager.isBundleValid(bundle)) {
            Log.e(Utils.TAG, "Received invalid Bundle for action " + intent.getAction());
            return;
        }
        
        final boolean images = bundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_IMAGES);
        final boolean notification = bundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_NOTIFICATION);
        Toast.makeText(context, "This doesn't work yet, sry.", Toast.LENGTH_LONG).show();
        // Toast.makeText(context, "Called. Images: " + images + " Notification: " + notification, Toast.LENGTH_LONG)
        // .show();
    }
    
}
