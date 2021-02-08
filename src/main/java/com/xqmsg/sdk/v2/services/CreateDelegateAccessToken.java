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
 * This service allows a user to create a very short-lived version of their access token in order to access certain <br>
 * services such as file encryption/decryption on the XQ websie without having to transmit their main access token.
 */
public class CreateDelegateAccessToken extends XQModule {

  public static final String SERVICE_NAME = "delegate";
  private final Logger logger = Logger.getLogger(getClass().getName(), null);

  private final XQSDK sdk;
  private final String authorizationToken;

  private CreateDelegateAccessToken(XQSDK aSDK, String authorizationToken) {
    sdk = aSDK;
    this.authorizationToken = authorizationToken;
  }

  /**
   * @param sdk App Settings
   * @param authorizationToken Access Token retrieved by {@link ExchangeForAccessToken}
   * @returns this
   */
  public static CreateDelegateAccessToken with(XQSDK sdk, String authorizationToken) {
    return new CreateDelegateAccessToken(sdk, authorizationToken);
  }

  @Override
  public List<String> requiredFields() {
    return List.of();
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   * <pre>parameter details:none</pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:String}}>>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

    return validateInput(maybeArgs)
            .thenApply((validatedArgs) -> {
              Map<String, String> headerProperties = Map.of("Authorization", String.format("Bearer %s", authorizationToken));
              return sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
                      Optional.of(SERVICE_NAME),
                      CallMethod.Get,
                      Optional.of(headerProperties),
                      validatedArgs);
            })
            .exceptionally(e -> new ServerResponse(CallStatus.Error, Reasons.MissingParameters, e.getMessage()));
  }

  @Override
  public String moduleName() {
    return "CreateDelegateAccessToken";
  }


}
