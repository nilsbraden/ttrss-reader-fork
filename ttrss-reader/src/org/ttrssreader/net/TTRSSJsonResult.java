/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
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

package org.ttrssreader.net;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TTRSSJsonResult {
    
    private JSONArray mNames;
    private JSONArray mValues;
    
    // public TTRSSJsonResult(InputStream is) throws JSONException {
    // this(Utils.convertStreamToString(is));
    // }
    
    public TTRSSJsonResult(String input) throws JSONException {
        
        JSONObject object = new JSONObject(input);
        
        mNames = object.names();
        mValues = object.toJSONArray(mNames);
        
    }
    
    public JSONArray getNames() {
        return mNames;
    }
    
    public JSONArray getValues() {
        return mValues;
    }
    
}
