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
 * Exchange the temporary access token with a real access token used in all secured XQ Message interactions
 */
public class ExchangeForAccessToken extends XQModule {

  public static final String SERVICE_NAME = "exchange";
  private final Logger logger = Logger.getLogger(getClass().getName(), null);

  private final XQSDK sdk;
  private final String tempToken;

  private ExchangeForAccessToken(XQSDK aSDK, String tempToken) {
    sdk = aSDK;
    this.tempToken = tempToken;
  }

  /**
   * @param sdk App Settings
   * @param tempToken Temporary token retrieved by {@link RequestAccess}
   *
   * @returns ExchangeForAccessToken
   */
  public static ExchangeForAccessToken with(XQSDK sdk, String tempToken) {
    return new ExchangeForAccessToken(sdk, tempToken);
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
    return "ExchangeForAccessToken";
  }


}
