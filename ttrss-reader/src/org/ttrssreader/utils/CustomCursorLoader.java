/*
 * Copyright (C) 2008 OpenIntents.org
 * Modified 2010 by Nils Braden
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ttrssreader.utils;

import org.ttrssreader.controllers.DBHelper;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;

public class CustomCursorLoader extends CursorLoader {
    
    public CustomCursorLoader(Context context, String query, String[] queryArgs) {
        super(context);
        this.query = query;
        this.queryArgs = queryArgs;
    }
    
    String query;
    String[] queryArgs;
    
    private final ForceLoadContentObserver mObserver = new ForceLoadContentObserver();
    
    @Override
    public Cursor loadInBackground() {
        Cursor cursor = DBHelper.getInstance().query(query, queryArgs);
        
        if (cursor != null) {
            cursor.getCount();
            cursor.registerContentObserver(mObserver);
        }
        
        return cursor;
    }
    
};
