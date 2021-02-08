package com.xqmsg.sdk.v2;

import java.util.Map;

/**
 * Created by ikechie on 2/4/20.
 */

public class ServerResponse {

  public static final String DATA = "data";
  public CallStatus status;
  public Reasons reason;
  public Map payload;
  private String otherReason = "";

  public String moreInfo() {
    if (!"".equals(otherReason)) return otherReason;
    return (String) payload.getOrDefault("reason", reason.toString());
  }

  public ServerResponse(CallStatus status, Reasons reason, Map payload) {
    this.status = status;
    this.reason = reason;
    this.payload = payload;
  }

  public ServerResponse(CallStatus status, Reasons reason, String otherReason) {
    this.status = status;
    this.reason = reason;
    this.payload = Map.of();
    this.otherReason = otherReason;
  }

  public ServerResponse(CallStatus status, Reasons reason) {
    this.status = status;
    this.reason = reason;
    this.payload = Map.of();
  }

  public ServerResponse(CallStatus status, Map payload) {
    this.status = status;
    this.reason = Reasons.NoneProvided;
    this.payload = payload;
  }

}

