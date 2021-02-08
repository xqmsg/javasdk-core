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
 * Revokes a key using its token. <br>
 * Only the user who sent the message will be able to revoke it.
 */
public class DeleteAccessCredentials extends XQModule {

  private static final Logger logger = Logger(DeleteAccessCredentials.class);

  public static final String SERVICE_NAME = "authorization";

  private final XQSDK sdk;
  private final String authorizationToken;

  private DeleteAccessCredentials(XQSDK aSDK, String authorizationToken) {
    sdk = aSDK;
    this.authorizationToken = authorizationToken;
  }

  /**
   * @param sdk App Settings
   * @param authorizationToken Access Token retrieved by {@link ExchangeForAccessToken}
   * @returns this
   */
  public static DeleteAccessCredentials with(XQSDK sdk, String authorizationToken) {
    return new DeleteAccessCredentials(sdk, authorizationToken);
  }

  @Override
  public List<String> requiredFields() { return  List.of();}

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   * <pre>parameter details:none</pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:{}}>>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {
    return validateInput(maybeArgs)
            .thenApply((validatedArgs)->{
              Map<String, String> headerProperties = Map.of("Authorization", String.format("Bearer %s", authorizationToken));
              return sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
                      Optional.of(SERVICE_NAME),
                      CallMethod.Delete,
                      Optional.of(headerProperties),
                      validatedArgs);

            })
            .exceptionally(e->new ServerResponse(CallStatus.Error, Reasons.MissingParameters,e.getMessage()));

  }

  @Override
  public String moduleName() {
    return "DeleteAccessCredentials";
  }

}
