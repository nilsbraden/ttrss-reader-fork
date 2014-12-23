/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
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

package org.ttrssreader.gui.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;

public class MyDialogFragment extends DialogFragment {
    
    @Override
    public void onCreate(Bundle instance) {
        super.onCreate(instance);
        setRetainInstance(true);
    }
    
    @Override
    public void onDestroyView() {
        // See http://stackoverflow.com/a/15444485
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }
    
}
