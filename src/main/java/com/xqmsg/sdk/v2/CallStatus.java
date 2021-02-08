package com.xqmsg.sdk.v2;

/**
 * Created by ikechie on 2/4/20.
 */
public enum CallStatus {
    /// The call succeeded.
    Ok,
    /// The call failed. When this happens, the *reason* element should have more details on the failure.
    Error
}
