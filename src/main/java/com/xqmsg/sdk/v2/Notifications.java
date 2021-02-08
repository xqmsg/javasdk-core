package com.xqmsg.sdk.v2;

/**
 * @author Jan Abt
 * @date Jan 05, 2021
 */

public enum Notifications {
  NONE,
  USAGE_REPORTS,
  TUTORIALS,
  BOTH ;

  public static String name(long o){

    int ordinal = (int) o;

    if( ordinal < values().length){
      return values()[ordinal].toString();
    }
    throw new RuntimeException(String.format("Invalid Ordinal for enum: %s", ordinal));
  }
}
