package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.CallMethod;
import com.xqmsg.sdk.v2.CallStatus;
import com.xqmsg.sdk.v2.Reasons;
import com.xqmsg.sdk.v2.ServerResponse;
import com.xqmsg.sdk.v2.XQModule;
import com.xqmsg.sdk.v2.XQSDK;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;


/**
 * Authenticate the PIN which resulted from the preceding {@link RequestAccess} service call.<br>
 * If successful this service returns a  status of 204, No Content .
 */
public class ValidateAccessRequest extends XQModule {

  public static final String PIN = "pin";
  public static final String SERVICE_NAME = "codevalidation";

  private final Logger logger = Logger.getLogger(getClass().getName(), null);

  private final XQSDK sdk;
  private final String tempToken;

  private ValidateAccessRequest(XQSDK aSDK, String tempToken) {
    sdk = aSDK;
    this.tempToken = tempToken;
  }

  @Override
  public List<String> requiredFields() {
    return List.of(PIN);
  }

  /**
   * @param sdk App Settings
   * @param tempToken - a temporary token retrieved by {@link RequestAccess}
   * @returns this
   */
  public static ValidateAccessRequest with(XQSDK sdk, String tempToken) {
    return new ValidateAccessRequest(sdk, tempToken);
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   * <pre>parameter details:<br>
   * String pin! - Pin to validate the access request.<br>
   * </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:{}}>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {
    return validateInput(maybeArgs)
            .thenApply((validatedArgs)->{
              Map<String, String> headerProperties = Map.of("Authorization", String.format("Bearer %s", tempToken));
              return sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
                      Optional.of(SERVICE_NAME),
                      CallMethod.Get,
                      Optional.of(headerProperties),
                      validatedArgs);

            })
            .exceptionally(e->new ServerResponse(CallStatus.Error, Reasons.MissingParameters,e.getMessage()));


  }

  @Override
  public String moduleName() {
    return "ValidateAccessRequest";
  }

}
