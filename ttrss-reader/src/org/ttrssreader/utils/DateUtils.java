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

package org.ttrssreader.utils;

import java.util.Date;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import android.content.Context;

/**
 * Provides functionality to automatically format date and time values (or both) depending on settings of the app and
 * the systems configuration.
 * 
 * @author Nils Braden
 */
public class DateUtils {
    
    /**
     * Returns the formatted date and time in the format specified by Controller.dateString() and
     * Controller.timeString() or if settings indicate the systems configuration should be used it returns the date and
     * time formatted as specified by the system.
     * 
     * @param context
     *            the application context
     * @param date
     *            the date to be formatted
     * @return a formatted representation of the date and time
     */
    public static String getDateTime(Context context, Date date) {
        if (Controller.getInstance().dateTimeSystem()) {
            
            java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
            java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
            return dateFormat.format(date) + " " + timeFormat.format(date);
            
        } else {
            
            try {
                
                // Only display delimiter if both formats are available, if the user did set one to an empty string he
                // doesn't want to see this information and we can hide the delimiter too.
                String dateStr = Controller.getInstance().dateString();
                String timeStr = Controller.getInstance().timeString();
                String delimiter = (dateStr.length() > 0 && timeStr.length() > 0) ? " " : "";
                String formatted = dateStr + delimiter + timeStr;
                return android.text.format.DateFormat.format(formatted, date).toString();
                
            } catch (Exception e) {
                
                // Retreat to default date-time-format
                java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
                java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
                return dateFormat.format(date) + " " + timeFormat.format(date);
                
            }
            
        }
    }
    
    /**
     * Returns the formatted date in the format specified by Controller.dateString() or if settings indicate the systems
     * configuration should be used it returns the date formatted as specified by the system.
     * 
     * @param context
     *            the application context
     * @param date
     *            the date to be formatted
     * @return a formatted representation of the date
     */
    public static String getDate(Context context, Date date) {
        if (Controller.getInstance().dateTimeSystem()) {
            
            java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
            return dateFormat.format(date);
            
        } else {
            
            try {
                String format = Controller.getInstance().dateString();
                return android.text.format.DateFormat.format(format, date).toString();
                
            } catch (Exception e) {
                // Retreat to default date-format
                String format = context.getResources().getString(R.string.DisplayDateFormatDefault);
                return android.text.format.DateFormat.format(format, date).toString();
            }
        }
    }
    
    /**
     * Returns the formatted time in the format specified by Controller.timeString() or if settings indicate the systems
     * configuration should be used it returns the time formatted as specified by the system.
     * 
     * @param context
     *            the application context
     * @param date
     *            the date to be formatted
     * @return a formatted representation of the time
     */
    public static String getTime(Context context, Date time) {
        if (Controller.getInstance().dateTimeSystem()) {
            
            java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
            return timeFormat.format(time);
            
        } else {
            
            try {
                String format = Controller.getInstance().timeString();
                return android.text.format.DateFormat.format(format, time).toString();
                
            } catch (Exception e) {
                // Retreat to default time-format
                String format = context.getResources().getString(R.string.DisplayTimeFormatDefault);
                return android.text.format.DateFormat.format(format, time).toString();
            }
        }
    }
    
}
