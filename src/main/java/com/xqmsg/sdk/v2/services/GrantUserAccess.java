package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.CallMethod;
import com.xqmsg.sdk.v2.ServerResponse;
import com.xqmsg.sdk.v2.XQModule;
import com.xqmsg.sdk.v2.XQSDK;
import com.xqmsg.sdk.v2.utils.Destination;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This services grants access for a particular user to a specified key. The
 * person granting access must be the one who owns the key.
 */
public class GrantUserAccess extends XQModule {

  private static final Logger logger = Logger(GrantUserAccess.class);

  public static final String RECIPIENTS = "recipients";
  public static final String LOCATOR_TOKEN = "locatorToken";

  private static final String SERVICE_NAME = "grant";

  private GrantUserAccess(XQSDK sdk) {
    assert sdk != null : "An instance of the XQSDK is required";
    super.sdk = sdk;
    super.cache = sdk.getCache();

  }

  @Override
  public List<String> requiredFields() {
    return List.of(LOCATOR_TOKEN, RECIPIENTS);
  }

  /**
   * @param sdk App Settings
   * @returns this
   */
  public static GrantUserAccess with(XQSDK sdk) {
    return new GrantUserAccess(sdk);
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   *                  <pre>parameter details:<br>
   *                  List<String> recipients! - List of emails of users intended to have read access to the encrypted content.<br>
   *                  String locatorToken! - The locator token used as a URL to discover the key on the server.<br>
   *                                         The URL encoding part is handled internally in the service itself.
   *                  </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:{}}>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {


    return CompletableFuture.completedFuture(
            validate.andThen( (result)->
                    authorize.andThen(
                            (authorizationToken) -> {

                              Map<String, Object> args = maybeArgs.get();

                              String locatorToken = (String) args.get(LOCATOR_TOKEN);
                              final List<String> recipients = (List<String>) args.get(RECIPIENTS);

                              final String DYNAMIC_SERVICE_NAME = String.format("%s/%s", SERVICE_NAME, encode(locatorToken));

                              return sdk.call(sdk.VALIDATION_SERVER_URL,
                                      Optional.of(DYNAMIC_SERVICE_NAME),
                                      CallMethod.Options,
                                      Optional.of(Map.of("Authorization", String.format("Bearer %s", authorizationToken))),
                                      Optional.of(Destination.XQ),
                                      Optional.of(Map.of(
                                              RECIPIENTS, recipients.stream().collect(Collectors.joining(","))
                                      )));
                            }).apply(Optional.of(Destination.XQ),result)
            )
                    .apply(maybeArgs));


  }

  @Override
  public String moduleName() {
    return "GrantUserAccess";
  }

  private String encode(String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }

}
