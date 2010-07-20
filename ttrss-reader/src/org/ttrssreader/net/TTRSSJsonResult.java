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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.net;

import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ttrssreader.utils.Utils;

public class TTRSSJsonResult {
	
	private JSONArray mNames;
	private JSONArray mValues;
	
	public TTRSSJsonResult(InputStream is) throws JSONException {
		this(Utils.convertStreamToString(is));
	}
	
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
