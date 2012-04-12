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

package org.ttrssreader.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.ttrssreader.gui.interfaces.IDataChangedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class UpdateController {
    
    public static final int LISTEN_ALL = Integer.MIN_VALUE;
    public static final int ID_EMPTY = Integer.MIN_VALUE + 1;
    
    public static final int TYPE_CATEGORY = 1;
    public static final int TYPE_FEED = 2;
    public static final int TYPE_ARTICLE = 3;
    public static final int TYPE_COUNTERS = 4;
    // See Data.java -> String VCAT_* for constants of virtual categories
    
    private static String DATA_ID = "data_id";
    private static String DATA_SUPER_ID = "data_super_id"; // ID of the super-type (Feed->Category)
    private static String DATA_TYPE = "data_type";
    
    private static UpdateController instance = null;
    
    ConcurrentHashMap<Integer, List<IDataChangedListener>> categoryListeners = new ConcurrentHashMap<Integer, List<IDataChangedListener>>();
    ConcurrentHashMap<Integer, List<IDataChangedListener>> feedListeners = new ConcurrentHashMap<Integer, List<IDataChangedListener>>();
    ConcurrentHashMap<Integer, List<IDataChangedListener>> articleListeners = new ConcurrentHashMap<Integer, List<IDataChangedListener>>();
    ConcurrentHashMap<Integer, List<IDataChangedListener>> counterListeners = new ConcurrentHashMap<Integer, List<IDataChangedListener>>();
    
    private Handler handler = new Handler() {
        
        @Override
        public void handleMessage(Message msg) {
            final int id = (Integer) msg.getData().get(DATA_ID);
            final int type = (Integer) msg.getData().get(DATA_TYPE);
            
            final int superId = (Integer) msg.getData().get(DATA_SUPER_ID);
            final int superType = type - 1;
            
            Map<Integer, List<IDataChangedListener>> map = null;
            Map<Integer, List<IDataChangedListener>> superMap = null;
            Map<IDataChangedListener, List<Integer>> notifyMap = new HashMap<IDataChangedListener, List<Integer>>();
            
            switch (type) {
                case TYPE_CATEGORY:
                    map = categoryListeners;
                    break;
                case TYPE_FEED:
                    map = feedListeners;
                    superMap = categoryListeners;
                    break;
                case TYPE_ARTICLE:
                    map = articleListeners;
                    superMap = feedListeners;
                    break;
                case TYPE_COUNTERS:
                    map = counterListeners;
                    break;
            }
            
            if (map == null)
                return;
            
            // Call all listeners in the map with the given id
            insertIntoMap(map, notifyMap, id, type);
            
            // Add all listeners on counters, they probably have changed so we notify them
            insertIntoMap(counterListeners, notifyMap, id, LISTEN_ALL);
            
            if (superMap != null) {
                // Check for super-listeners (listeners which listen on all events for one type
                insertIntoMap(superMap, notifyMap, superId, LISTEN_ALL);
                
                // Also notify listeners
                if (superId != ID_EMPTY)
                    insertIntoMap(superMap, notifyMap, superId, superType);
            }
            
            // For all items in notifyMap:
            for (IDataChangedListener listener : notifyMap.keySet()) {
                listener.dataChanged(type);
            }
        }
    };
    
    private static void insertIntoMap(final Map<Integer, List<IDataChangedListener>> source, final Map<IDataChangedListener, List<Integer>> target, int id, int type) {
        if (source == null || target == null)
            return; // No source or target, nothing to do
            
        if (source.get(id) == null)
            return; // No listeners for this id
            
        // Read listener for this id from source, add listener with type to target, type is inserted in the list of
        // types
        for (IDataChangedListener listener : source.get(id)) {
            List<Integer> typeList = target.get(listener);
            if (typeList == null) {
                typeList = new ArrayList<Integer>();
                target.put(listener, typeList);
            }
            
            if (!typeList.contains(type))
                typeList.add(type);
        }
    }
    
    // Singleton
    private UpdateController() {
    }
    
    public static UpdateController getInstance() {
        if (instance == null) {
            synchronized (UpdateController.class) {
                if (instance == null) {
                    instance = new UpdateController();
                }
            }
        }
        return instance;
    }
    
    /**
     * Registers an activity for callback-notifications about change-events on the given type of object and the specific
     * ID.
     * 
     * @param listener
     *            the interface to be called in case of an event for this data-type and ID.
     * @param type
     *            the type of the referenced object
     * @param id
     *            the ID of the referenced object
     */
    public void registerActivity(IDataChangedListener listener, int type, Integer id) {
        switch (type) {
            case TYPE_CATEGORY:
                putListener(listener, id, categoryListeners);
                break;
            case TYPE_FEED:
                putListener(listener, id, feedListeners);
                break;
            case TYPE_ARTICLE:
                putListener(listener, id, articleListeners);
                break;
            case TYPE_COUNTERS:
                putListener(listener, id, counterListeners);
                break;
        }
    }
    
    /**
     * Adds the listener to the list of listeners, mapped by id. If the list does not yet exist it is created and added
     * to the map.
     * 
     * @param listener
     *            the listener to be added
     * @param id
     *            the id of the object to be watched
     * @param map
     *            the map of the type to which the object belongs
     */
    private static void putListener(final IDataChangedListener listener, final Integer id, final ConcurrentHashMap<Integer, List<IDataChangedListener>> map) {
        if (id == null)
            return;
        
        // Atomically checks if the list is present, else inserts it and returns the list in both cases
        List<IDataChangedListener> list = new ArrayList<IDataChangedListener>();
        map.putIfAbsent(id, list);
        list.add(listener);
    }
    
    /**
     * Removes an Activity from the list of listeners.
     * 
     * @param listener
     *            the listener to be removed
     * @param type
     *            the type of the referenced object
     * @param id
     *            the ID of the referenced object
     */
    public void unregisterActivity(IDataChangedListener listener, int type, int id) {
        switch (type) {
            case TYPE_CATEGORY:
                removeListener(listener, id, categoryListeners);
            case TYPE_FEED:
                removeListener(listener, id, feedListeners);
                break;
            case TYPE_ARTICLE:
                removeListener(listener, id, articleListeners);
                break;
            case TYPE_COUNTERS:
                removeListener(listener, id, counterListeners);
                break;
        }
    }
    
    private static void removeListener(final IDataChangedListener listener, final int id, final ConcurrentHashMap<Integer, List<IDataChangedListener>> map) {
        if (map != null)
            if (map.contains(id))
                map.get(id).remove(listener);
    }
    
    public void notifyListeners(int type, int id, int superId) {
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putInt(DATA_ID, id);
        bundle.putInt(DATA_SUPER_ID, superId);
        bundle.putInt(DATA_TYPE, type);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }
    
}
