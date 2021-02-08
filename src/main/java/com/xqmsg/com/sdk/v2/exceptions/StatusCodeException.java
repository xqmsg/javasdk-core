package com.xqmsg.com.sdk.v2.exceptions;



public class StatusCodeException extends Exception {

  protected int statusCode;
  protected String statusMessage;

  public StatusCodeException(int statusCode) {
    this(statusCode, HttpStatusCodes.messages.get(statusCode));
  }

  public StatusCodeException(int statusCode, String statusMessage) {
    super();
    this.statusCode=statusCode;
    this.statusMessage= statusMessage;

  }

  public static StatusCodeException notImplemented() {
    return new StatusCodeException(HttpStatusCodes.HTTP_NOT_IMPLEMENTED);
  }

  public int statusCode(){
    return statusCode;
  }

  public String statusMessage(){
    return statusMessage;
  }


}





