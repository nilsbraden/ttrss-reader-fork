/*
 * ttrss-reader-fork for Android
 *
 * Copyright (C) 2013 I. Lubimov.
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

/**
 * this exception should be thrown if parsing of JSON object should be broken
 *
 * @author igor
 */
public class StopJsonParsingException extends Exception {


  /**
   * constructor with detail message specification
   *
   * @param detailMessage   detailed description of exception cause
   */
  public StopJsonParsingException (String detailMessage)
  {
    super (detailMessage);
  }

}
