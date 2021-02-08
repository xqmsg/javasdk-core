package com.xqmsg.sdk.v2;

/**
 * Created by ikechie on 2/4/20.
 */
/// Specifies what protocol  will be used for the call.
public enum CallMethod {
    /// Use POST for content transmission.
     Post,
    /// Use GET for content transmission. Data will be url encoded.
    Get,
  Options,
  Delete,
    Patch,
    Ping
}
