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
 * This service validates an API key and returns the scopes associated with it.
 */
public class CheckApiKey extends XQModule {

  private final Logger logger = Logger.getLogger(getClass().getName(), null);

  public static String API_KEY = "api-key";
  public static String SCOPES = "scopes";

  private static final String SERVICE_NAME = "apikey";

  private CheckApiKey(XQSDK sdk) {
    assert sdk != null : "An instance of the XQSDK is required";
    super.sdk = sdk;
    super.cache = sdk.getCache();
  }

  /**
   * @param sdk App Settings
   * @returns Authorize
   */
  public static CheckApiKey with(XQSDK sdk) {
    return new CheckApiKey(sdk);
  }

  @Override
  public List<String> requiredFields() {
    return List.of(API_KEY);
  }


  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   *                  <pre>parameter details:<br>
   *                  String api-key! - The API key whose scopes are to be checked.<br>
   *                  </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{scopes:List<String>}>>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {


    return CompletableFuture.completedFuture(
            validate.andThen(
                    authorize.andThen(
                            (authorizationToken) -> {
                             return sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
                                      Optional.of(SERVICE_NAME),
                                      CallMethod.Get,
                                      Optional.of(Map.of("Authorization", String.format("Bearer %s", authorizationToken))),
                                      maybeArgs);
                            })
            )
                    .apply(maybeArgs))
            .exceptionally(e -> new ServerResponse(CallStatus.Error, Reasons.MissingParameters, e.getMessage()));


  }

  @Override
  public String moduleName() {
    return "AuthorizeAlias";
  }

}
