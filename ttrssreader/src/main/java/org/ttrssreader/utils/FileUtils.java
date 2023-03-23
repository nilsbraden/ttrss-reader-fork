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

package org.ttrssreader.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nils Braden
 */
public class FileUtils {

	//	private static final String TAG = FileUtils.class.getSimpleName();

	/**
	 * Supported extensions of imagefiles, see http://developer.android.com/guide/appendix/media-formats.html
	 */
	public static final String[] IMAGE_EXTENSIONS = {"jpeg", "jpg", "gif", "png", "bmp", "webp"};
	public static final String IMAGE_MIME = "image/*";

	/**
	 * Supported extensions of audiofiles, see http://developer.android.com/guide/appendix/media-formats.html
	 * I removed the extensions from this list which are also used for video files. It is easier to open these in the
	 * videoplayer and blaming the source instead of trying to figure out mime-types by hand.
	 */
	public static final String[] AUDIO_EXTENSIONS = {"mp3", "mid", "midi", "xmf", "mxmf", "ogg", "wav"};
	public static final String AUDIO_MIME = "audio/*";

	/**
	 * Supported extensions of videofiles, see http://developer.android.com/guide/appendix/media-formats.html
	 */
	public static final String[] VIDEO_EXTENSIONS = {"3gp", "mp4", "m4a", "aac", "ts", "webm", "mkv", "mpg", "mpeg", "avi", "flv"};
	public static final String VIDEO_MIME = "video/*";

	/**
	 * At the moment this method just returns a generic mime-type for audio, video or image-files, a more specific way
	 * of probing for the type (MIME-Sniffing or exact checks on the extension) are yet to be implemented.
	 * <p/>
	 * Implementation-Hint: See
	 * https://code.google.com/p/openintents/source/browse/trunk/filemanager/FileManager/src/org
	 * /openintents/filemanager/FileManagerActivity.java
	 */
	public static String getMimeType(String fileName) {
		String ret = "";
		if (fileName == null || fileName.length() == 0)
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

	/**
	 * group given files (URLs) into hash by mime-type
	 *
	 * @param attachments collection of file names (URLs)
	 * @return map, which keys are found mime-types and values are file collections of this mime-type
	 */
	public static Map<String, Collection<String>> groupFilesByMimeType(Collection<String> attachments) {
		Map<String, Collection<String>> attachmentsByMimeType = new HashMap<>();
		for (String url : attachments) {
			String mimeType = getMimeType(url);

			if (mimeType.length() > 0) {
				Collection<String> mimeTypeList = attachmentsByMimeType.get(mimeType);

				if (mimeTypeList == null) {
					mimeTypeList = new ArrayList<>();
					attachmentsByMimeType.put(mimeType, mimeTypeList);
				}

				mimeTypeList.add(url);
			}
		}

		return attachmentsByMimeType;
	}

	/**
	 * From http://stackoverflow.com/a/4943771
	 *
	 * @return false if delete() failed on any of the files
	 */
	public static boolean deleteFolderRecursive(final File dir) {
		boolean ret = true;
		if (dir.isDirectory()) {

			String[] children = dir.list();
			if (children != null) {
				for (String aChildren : children) {
					ret &= new File(dir, aChildren).delete();
				}
			}
		}
		return ret;
	}

}
