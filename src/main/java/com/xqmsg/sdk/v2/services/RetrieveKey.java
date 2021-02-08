package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.CallMethod;
import com.xqmsg.sdk.v2.CallStatus;
import com.xqmsg.sdk.v2.Reasons;
import com.xqmsg.sdk.v2.ServerResponse;
import com.xqmsg.sdk.v2.XQModule;
import com.xqmsg.sdk.v2.XQSDK;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;


/**
 * The key will only be returned if the following hold true:
 * The access token of the requesting user is valid and unexpired.
 * The expiration time specified for the key has not elapsed.
 * The person requesting the key was listed as a valid recipient by the sender.
 * The key is either not geofenced, or is being accessed from an authorized location.
 * If any of these is not true, an error will be returned instead.
 */
public class RetrieveKey extends XQModule {

  private static final Logger logger = Logger(RetrieveKey.class);

  public static final String SERVICE_NAME = "key";
  public static final String LOCATOR_TOKEN = "locatorToken";
  private final XQSDK sdk;
  private final String authorizationToken;

  private RetrieveKey(XQSDK aSDK, String authorizationToken) {
    sdk = aSDK;
    this.authorizationToken = authorizationToken;
  }

  @Override
  public List<String> requiredFields() {
    return List.of(LOCATOR_TOKEN);
  }

  /**
   * @param sdk App Settings
   * @param authorizationToken Access Token retrieved by {@link ExchangeForAccessToken}
   * @returns this
   */
  public static RetrieveKey with(XQSDK sdk, String authorizationToken) {
    return new RetrieveKey(sdk, authorizationToken);
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   * <pre>parameter details:<br>
   * String locatorToken! - The locator token used as a URL to discover the key on the server.<br>
   *                        The URL encoding part is handled internally in the service itself.
   * </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:String}>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {
    return validateInput(maybeArgs)
            .thenApply((validatedArgs) -> {
              Map<String, Object> args = validatedArgs.get();

              String locatorToken = (String) args.get(LOCATOR_TOKEN);

              final String DYNAMIC_SERVICE_NAME = String.format("%s/%s", SERVICE_NAME, encode(locatorToken));

              ServerResponse serverResponse = sdk.call(sdk.VALIDATION_SERVER_URL,
                      Optional.of(DYNAMIC_SERVICE_NAME),
                      CallMethod.Get,
                      Optional.of(Map.of("Authorization", String.format("Bearer %s", authorizationToken))),
                      Optional.of(Map.of()));

              switch (serverResponse.status) {
                case Ok: {
                  String key = (String) serverResponse.payload.get(ServerResponse.DATA);
                  key = key.substring(2);
                  return new ServerResponse(CallStatus.Ok, Map.of(ServerResponse.DATA, key));
                }
                case Error:
                default: {
                  return serverResponse;
                }
              }

            })
            .exceptionally(e -> new ServerResponse(CallStatus.Error, Reasons.MissingParameters, e.getMessage()));

  }

  @Override
  public String moduleName() {
    return "RetrieveKey";
  }

  private String encode(String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }

}
