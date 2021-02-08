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
 * Check whether a particular key is expired or not without actually fetching it.
 */
public class CheckKeyExpiration extends XQModule {

  private static final Logger logger = Logger(CheckKeyExpiration.class);

  public static final String SERVICE_NAME = "expiration";

  public static final String LOCATOR_TOKEN = "locatorToken";
  public static final String EXPIRES_IN = "expiresOn";
  private final XQSDK sdk;
  private final String authorizationToken;

  private CheckKeyExpiration(XQSDK aSDK, String authorizationToken) {
    sdk = aSDK;
    this.authorizationToken = authorizationToken;
  }

  /** @param sdk App Settings
   * @param authorizationToken Access Token retrieved by {@link ExchangeForAccessToken}
   * @returns this
   */
  public static CheckKeyExpiration with(XQSDK sdk, String authorizationToken) {
    return new CheckKeyExpiration(sdk, authorizationToken);
  }

  @Override
  public List<String> requiredFields() {
    return List.of(LOCATOR_TOKEN);
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   * <pre>parameter details:<br>
   * String locatorToken! - A URL encoded version of the key locator token to fetch the key from the server.
   * </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{expiresOn:long}}>>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {
    return validateInput(maybeArgs)
            .thenApply((validatedArgs) -> {
              Map<String, Object> args = validatedArgs.get();
              String locatorToken = (String) args.get(LOCATOR_TOKEN);

              final String DYNAMIC_SERVICE_NAME = String.format("%s/%s", SERVICE_NAME, encode(locatorToken));

              return sdk.call(sdk.VALIDATION_SERVER_URL,
                      Optional.of(DYNAMIC_SERVICE_NAME),
                      CallMethod.Get,
                      Optional.of(Map.of("Authorization", String.format("Bearer %s", authorizationToken))),
                      Optional.of(Map.of()));
            })
            .exceptionally(e -> new ServerResponse(CallStatus.Error, Reasons.MissingParameters, e.getMessage()));


  }

  @Override
  public String moduleName() {
    return "CheckKeyExpiration";
  }

  private String encode(String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }

}
