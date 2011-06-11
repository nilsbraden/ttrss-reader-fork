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

public class NotInitializedException extends Exception {

    private static final long serialVersionUID = 1L;
    
    public NotInitializedException(String msg) {
        super(msg);
    }

    public NotInitializedException(String format, Object... args) {
        super(String.format(format, args));
    }

    public NotInitializedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NotInitializedException(final Throwable cause) {
        super(cause);
    }

    public NotInitializedException(final Throwable cause, final String format, final Object... args) {
        super(String.format(format, args), cause);
    }
    
}
