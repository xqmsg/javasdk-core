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



public class AddNewKeyPacket extends XQModule {

  private static final Logger logger = Logger(AddNewKeyPacket.class);

  public static final String SERVICE_NAME = "packet";
  public static final String KEY = "key";
  public static final String RECIPIENTS = "recipients";
  public static final String MESSAGE_EXPIRATION_HOURS = "expires";
  public static final String DELETE_ON_RECEIPT = "dor";
  public static final String TYPE = "type";
  private final XQSDK sdk;
  private final String authorizationToken;

  private AddNewKeyPacket(XQSDK aSDK, String authorizationToken) {
    sdk = aSDK;
    this.authorizationToken = authorizationToken;
  }

  /**
   * @param sdk App Settings
   * @param authorizationToken Access Token retrieved by {@link ExchangeForAccessToken}
   * @returns this
   */
  public static AddNewKeyPacket with(XQSDK sdk, String authorizationToken) {
    return new AddNewKeyPacket(sdk, authorizationToken);
  }

  @Override
  public List<String> requiredFields() { return  List.of(KEY, RECIPIENTS, MESSAGE_EXPIRATION_HOURS);}

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   * <pre>parameter details:<br>
   * String key! - The secret key that the user wants to protect.<br>
   * Long expires! - The number of hours that this key will remain valid for. After this time, it will no longer be accessible.<br>
   * List<String> recipients! - List of emails of those recipients who are allowed to access the key.<br>
   * Boolean dor? [false] - Should the content be deleted after opening.<br>
   * </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:String}>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

     return validateInput(maybeArgs)
     .thenApply((validatedArgs)->{
       return sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
               Optional.of(SERVICE_NAME),
               CallMethod.Post,
               Optional.of(Map.of("Authorization", String.format("Bearer %s", authorizationToken))),
               validatedArgs);
     })
     .exceptionally(e->new ServerResponse(CallStatus.Error, Reasons.MissingParameters,e.getMessage()));

  }

  @Override
  public String moduleName() {
    return "AddNewKeyPacket";
  }

}
