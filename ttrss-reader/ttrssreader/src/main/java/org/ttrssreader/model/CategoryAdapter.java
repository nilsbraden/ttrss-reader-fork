/*
 * Copyright (c) 2015, Nils Braden
 *
 * This file is part of ttrss-reader-fork. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; If
 * not, see http://www.gnu.org/licenses/.
 */

package org.ttrssreader.model;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends MainAdapter {

    @SuppressWarnings("unused")
    private static final String TAG = CategoryAdapter.class.getSimpleName();
    
    public CategoryAdapter(Context context) {
        super(context);
    }
    
    @Override
    public Object getItem(int position) {
        Category ret = new Category();
        Cursor cur = getCursor();
        if (cur == null)
            return ret;
        
        if (cur.getCount() >= position) {
            if (cur.moveToPosition(position)) {
                ret.id = cur.getInt(0);
                ret.title = cur.getString(1);
                ret.unread = cur.getInt(2);
            }
        }
        return ret;
    }
    
    public List<Category> getCategories() {
        List<Category> ret = new ArrayList<Category>();
        Cursor cur = getCursor();
        if (cur == null)
            return ret;
        
        if (cur.moveToFirst()) {
            while (!cur.isAfterLast()) {
                Category c = new Category();
                c.id = cur.getInt(0);
                c.title = cur.getString(1);
                c.unread = cur.getInt(2);
                ret.add(c);
                cur.move(1);
            }
        }
        return ret;
    }
    
    private int getImage(int id, boolean unread) {
        if (id == Data.VCAT_STAR) {
            return R.drawable.star48;
        } else if (id == Data.VCAT_PUB) {
            return R.drawable.published48;
        } else if (id == Data.VCAT_FRESH) {
            return R.drawable.fresh48;
        } else if (id == Data.VCAT_ALL) {
            return R.drawable.all48;
        } else if (id < -10) {
            if (unread) {
                return R.drawable.label;
            } else {
                return R.drawable.label_read;
            }
        } else {
            if (unread) {
                return R.drawable.categoryunread48;
            } else {
                return R.drawable.categoryread48;
            }
        }
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = parent.getContext();
        if (position >= getCount() || position < 0)
            return new View(context);
        
        Category c = (Category) getItem(position);
        
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout layout = null;
        if (convertView == null) {
            layout = (LinearLayout) inflater.inflate(R.layout.item_category, parent, false);
        } else {
            if (convertView instanceof LinearLayout) {
                layout = (LinearLayout) convertView;
            }
        }
        
        if (layout == null)
            return new View(context);
        
        ImageView icon = (ImageView) layout.findViewById(R.id.icon);
        icon.setImageResource(getImage(c.id, c.unread > 0));
        
        TextView title = (TextView) layout.findViewById(R.id.title);
        title.setText(formatItemTitle(c.title, c.unread));
        if (c.unread > 0)
            title.setTypeface(Typeface.DEFAULT_BOLD);
        
        return layout;
    }
    
}
