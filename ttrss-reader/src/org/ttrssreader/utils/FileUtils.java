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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Comparator;
import android.util.Log;

/**
 * 
 * @author Nils Braden
 * 
 */
public class FileUtils {
    
    /**
     * Supported extensions of imagefiles, see http://developer.android.com/guide/appendix/media-formats.html
     */
    public static final String[] IMAGE_EXTENSIONS = { "jpeg", "jpg", "gif", "png", "bmp", "webp" };
    public static final String IMAGE_MIME = "image/*";
    
    /**
     * Supported extensions of audiofiles, see http://developer.android.com/guide/appendix/media-formats.html
     * I removed the extensions from this list which are also used for video files. It is easier to open these in the
     * videoplayer and blaming the source instead of trying to figure out mime-types by hand.
     */
    public static final String[] AUDIO_EXTENSIONS = { "mp3", "mid", "midi", "xmf", "mxmf", "ogg", "wav" };
    public static final String AUDIO_MIME = "audio/*";
    
    /**
     * Supported extensions of videofiles, see http://developer.android.com/guide/appendix/media-formats.html
     */
    public static final String[] VIDEO_EXTENSIONS = { "3gp", "mp4", "m4a", "aac", "ts", "webm", "mkv", "mpg", "mpeg",
            "avi", "flv" };
    public static final String VIDEO_MIME = "video/*";
    
    /**
     * Path on sdcard to store files (DB, Certificates, ...)
     */
    public static final String SDCARD_PATH_FILES = "/Android/data/org.ttrssreader/files/";
    
    /**
     * Path on sdcard to store cache
     */
    public static final String SDCARD_PATH_CACHE = "/Android/data/org.ttrssreader/cache/";
    
    /**
     * Downloads a given URL directly to a file, when maxSize bytes are reached the download is stopped and the file is
     * deleted.
     * 
     * @param downloadUrl
     *            the URL of the file
     * @param file
     *            the destination file
     * @param maxSize
     *            the size in bytes after which to abort the download
     * @return length of the downloaded file
     */
    public static long downloadToFile(String downloadUrl, File file, long maxSize) {
        FileOutputStream fos = null;
        int byteWritten = 0;
        
        try {
            if (file.exists())
                file.delete();
            
            URL url = new URL(downloadUrl);
            URLConnection connection = url.openConnection();
            
            // Check filesize if available from header
            if (connection.getHeaderField("Content-Length") != null) {
                long length = Long.parseLong(connection.getHeaderField("Content-Length"));
                if (length > maxSize) {
                    Log.i(Utils.TAG, String.format(
                            "Not starting download of %s, the size (%s MB) exceeds the maximum filesize of %s MB.",
                            downloadUrl, length / 1048576, maxSize / 1048576));
                    return -1;
                }
            }
            
            file.createNewFile();
            fos = new FileOutputStream(file);
            InputStream is = connection.getInputStream();
            
            int size = 1024 * 8;
            byte[] buf = new byte[size];
            int byteRead;
            
            while (((byteRead = is.read(buf)) != -1)) {
                fos.write(buf, 0, byteRead);
                byteWritten += byteRead;
                
                if (byteWritten > maxSize) {
                    Log.w(Utils.TAG, String.format(
                            "Download interrupted, the size of %s bytes exceeds maximum filesize.", byteWritten));
                    file.delete();
                    byteWritten = 0; // Set to 0 so the article will not be scheduled for download again.
                    break;
                }
            }
        } catch (Exception e) {
            Log.i(Utils.TAG, "Download not finished properly. Exception: " + e.getMessage());
            byteWritten = -1;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
        return byteWritten;
    }
    
    /**
     * Sums up the size of a folder including all files and subfolders.
     * 
     * @param folder
     *            the folder
     * @return the size of the folder
     */
    public static long getFolderSize(File folder) {
        long size = 0;
        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                size += getFolderSize(f);
            } else {
                size += f.length();
            }
        }
        return size;
    }
    
    /**
     * At the moment this method just returns a generic mime-type for audio, video or image-files, a more specific way
     * of probing for the type (MIME-Sniffing or exact checks on the extension) are yet to be implemented.
     * 
     * Implementation-Hint: See
     * https://code.google.com/p/openintents/source/browse/trunk/filemanager/FileManager/src/org
     * /openintents/filemanager/FileManagerActivity.java
     * 
     * @param fileName
     * @return
     */
    public static String getMimeType(String fileName) {
        String ret = "";
        if (fileName == null || fileName.isEmpty())
            return ret;
        
        for (String ext : IMAGE_EXTENSIONS) {
            if (fileName.endsWith(ext))
                return IMAGE_MIME;
        }
        for (String ext : AUDIO_EXTENSIONS) {
            if (fileName.endsWith(ext))
                return AUDIO_MIME;
        }
        for (String ext : VIDEO_EXTENSIONS) {
            if (fileName.endsWith(ext))
                return VIDEO_MIME;
        }
        
        return ret;
    }
    
    public class FileDateComparator implements Comparator<File> {
        
        @Override
        public int compare(File f1, File f2) {
            
            // Hopefully avoids crashes due to IllegalArgumentExceptions
            if (f1.equals(f2))
                return 0;
            
            long size1 = f1.lastModified();
            long size2 = f2.lastModified();
            
            if (size1 < size2) {
                return -1;
            } else if (size1 > size2) {
                return 1;
            }
            
            return 0; // equal
        }
        
    }
    
}
