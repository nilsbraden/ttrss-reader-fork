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

package org.ttrssreader.model.updaters;

import org.ttrssreader.controllers.Data;

public class UnsubscribeUpdater implements IUpdatable {
    
    private int feed_id;
    
    public UnsubscribeUpdater(int feed_id) {
        this.feed_id = feed_id;
    }
    
    @Override
    public void update(Updater parent) {
        Data.getInstance().feedUnsubscribe(feed_id);
    }
    
}
