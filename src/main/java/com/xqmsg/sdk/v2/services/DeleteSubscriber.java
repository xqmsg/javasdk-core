package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.CallMethod;
import com.xqmsg.sdk.v2.ServerResponse;
import com.xqmsg.sdk.v2.XQModule;
import com.xqmsg.sdk.v2.XQSDK;
import com.xqmsg.sdk.v2.exceptions.StatusCodeException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;


/**
 * Deletes the user specified by the access token.
 * After an account is deleted, the subscriber will be sent an email notifying them of its deletion.
 */
public class DeleteSubscriber extends XQModule {

  private static final Logger logger = Logger(DeleteSubscriber.class);

  private static final String SERVICE_NAME = "subscriber";

  private DeleteSubscriber(XQSDK sdk) {
    assert sdk != null : "An instance of the XQSDK is required";
    super.sdk = sdk;
    super.cache = sdk.getCache();
  }


  /**
   * @param sdk App Settings
   * @returns this
   */
  public static DeleteSubscriber with(XQSDK sdk) {
    return new DeleteSubscriber(sdk);
  }

  @Override
  public List<String> requiredFields() {
    return List.of();
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   *                  <pre>parameter details:none</pre>
   * @returns CompletableFuture&lt;ServerResponse#payload{data:{}}>>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

    return CompletableFuture.completedFuture(
            validate.andThen(
                    authorize.andThen(
                            (authorizationToken) -> {
                              Map<String, String> headerProperties = Map.of("Authorization", String.format("Bearer %s", authorizationToken));
                              ServerResponse deleteResponse = sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
                                      Optional.of(SERVICE_NAME),
                                      CallMethod.Delete,
                                      Optional.of(headerProperties),
                                      maybeArgs);
                              switch (deleteResponse.status) {
                                case Ok: {
                                  try {
                                    String activeProfile = cache.getActiveProfile(true);
                                    cache.removeProfile(activeProfile);
                                  } catch (StatusCodeException e) {
                                    logger.severe(e.getMessage());
                                  }
                                }
                                default: {
                                  return deleteResponse;
                                }
                              }


                            })
            )
                    .apply(maybeArgs));


  }

  @Override
  public String moduleName() {
    return "DeleteSubscriber";
  }

}
