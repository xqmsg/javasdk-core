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
 * This endpoint is useful for merging two or more valid access tokens ( along with the access token used to make the call ) into a single one that can be used for temporary read access.
 * This is useful in situations where a user who has authenticated with multiple accounts wants to get a key for a particular message without needing to know exactly which of their accounts is a valid recipient. As long as one of the accounts in the merged token have access, they will be able to get the ke
 * The merged token has three restrictions:
 * 1. It cannot be used to send messages
 * 2. It can only be created from other valid access tokens.
 * 3. It is only valid for a short amount of time.
 */
public class MergeTokens extends XQModule {

  public static final String SERVICE_NAME = "combined";
  public static final String TOKENS = "tokens";
  public static final String MERGED_TOKEN = "token";
  public static final String MERGE_COUNT = "merged";
  private final Logger logger = Logger.getLogger(getClass().getName(), null);

  private final XQSDK sdk;
  private final String authorizationToken;

  private MergeTokens(XQSDK aSDK, String authorizationToken) {
    sdk = aSDK;
    this.authorizationToken = authorizationToken;
  }

  @Override
  public List<String> requiredFields() {
    return List.of(TOKENS);
  }
  /**
   * @param sdk App Settings
   * @param authorizationToken Access Token retrieved by {@link ExchangeForAccessToken}
   * @returns this
   */
  public static MergeTokens with(XQSDK sdk, String authorizationToken) {
    return new MergeTokens(sdk, authorizationToken);
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   * <pre>parameter details:<br>
   * List<String> tokens! -  The list of tokens to merge.<br>
   * Boolean dor? [false] - Should the content be deleted after opening.<br>
   * </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{token:string, merged:long}>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {
    return validateInput(maybeArgs)
            .thenApply((validatedArgs)->{
              Map<String, String> headerProperties = Map.of("Authorization", String.format("Bearer %s", authorizationToken));
              return sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
                      Optional.of(SERVICE_NAME),
                      CallMethod.Post,
                      Optional.of(headerProperties),
                      validatedArgs);
            })
            .exceptionally(e->new ServerResponse(CallStatus.Error, Reasons.MissingParameters,e.getMessage()));
  }

  @Override
  public String moduleName() {
    return "CreateDelegateAccessToken";
  }


}
