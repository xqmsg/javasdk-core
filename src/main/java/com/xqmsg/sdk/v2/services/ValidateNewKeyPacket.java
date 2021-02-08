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



public class ValidateNewKeyPacket extends XQModule {

  private static final Logger logger = Logger(ValidateNewKeyPacket.class);

  public static final String SERVICE_NAME = "packet";
  public static final String PACKET = "data";
  private final XQSDK sdk;
  private final String authorizationToken;

  private ValidateNewKeyPacket(XQSDK aSDK, String authorizationToken) {
    sdk = aSDK;
    this.authorizationToken = authorizationToken;
  }

  @Override
  public List<String> requiredFields() {
    return List.of(PACKET);
  }

  /**
   * @param sdk App Settings
   * @param accessToken Access Token retrieved by {@link ExchangeForAccessToken}
   * @returns this
   */
  public static ValidateNewKeyPacket with(XQSDK sdk, String accessToken) {
    return new ValidateNewKeyPacket(sdk, accessToken);
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   * <pre>parameter details:none</pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:{}}>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {
    return validateInput(maybeArgs)
            .thenApply((validatedArgs) -> {
              return sdk.call(sdk.VALIDATION_SERVER_URL,
                      Optional.of(SERVICE_NAME),
                      CallMethod.Post,
                      Optional.of(Map.of("Authorization", String.format("Bearer %s", authorizationToken),
                                  XQSDK.CONTENT_TYPE, XQSDK.TEXT_PLAIN_UTF_8)),
                      validatedArgs);
            })
            .exceptionally(e -> new ServerResponse(CallStatus.Error, Reasons.MissingParameters, e.getMessage()));


  }

  @Override
  public String moduleName() {
    return "ValidateNewKeyPacket";
  }

}
