package com.xqmsg.sdk.v2.utils;

import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;


public abstract class DateTimeFormats {

  public static final String ISO_8601_DATE_TIME = "YYYY-MM-DD'T'kk:mm:ss";


  public static String render(LocalDateTime jodaLocalDateTime, String format) {
    if (jodaLocalDateTime == null) { return null; }
    org.joda.time.format.DateTimeFormatter fmt = DateTimeFormat.forPattern(format);
    return jodaLocalDateTime.toString(fmt);
  }

}
