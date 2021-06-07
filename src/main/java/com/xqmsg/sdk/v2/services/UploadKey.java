package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.CallMethod;
import com.xqmsg.sdk.v2.ServerResponse;
import com.xqmsg.sdk.v2.XQModule;
import com.xqmsg.sdk.v2.XQSDK;
import com.xqmsg.sdk.v2.utils.Destination;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;



public class UploadKey extends XQModule {

  private static final Logger logger = Logger(UploadKey.class);

  public static final String KEY = "key";
  public static final String RECIPIENTS = "recipients";
  public static final String MESSAGE_EXPIRATION_HOURS = "expires";
  public static final String DELETE_ON_RECEIPT = "dor";
  public static final String TYPE = "type";

  private static final String SERVICE_NAME = "packet";

  private UploadKey(XQSDK sdk) {
    assert sdk != null : "An instance of the XQSDK is required";
    super.sdk = sdk;
    super.cache = sdk.getCache();
  }

  /**
   * @param sdk App Settings
   * @returns this
   */
  public static UploadKey with(XQSDK sdk) {
    return new UploadKey(sdk);
  }

  @Override
  public List<String> requiredFields() {
    return List.of(KEY, RECIPIENTS, MESSAGE_EXPIRATION_HOURS);
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   *                  <pre>parameter details:<br>
   *                                   String key! - The secret key that the user wants to protect.<br>
   *                                   Long expires! - The number of hours that this key will remain valid for. After this time, it will no longer be accessible.<br>
   *                                   List<String> recipients! - List of emails of those recipients who are allowed to access the key.<br>
   *                                   Boolean dor? [false] - Should the content be deleted after opening.<br>
   *                                   </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:String}>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

    return validate.andThen((result)->
              authorize.andThen(
                      (authorizationToken) -> {
                        ServerResponse uploadResponse = sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
                                Optional.of(SERVICE_NAME),
                                CallMethod.Post,
                                Optional.of(Map.of("Authorization", String.format("Bearer %s", authorizationToken))),
                                Optional.of(Destination.XQ),
                                maybeArgs);

                        switch (uploadResponse.status) {
                          case Ok: {
                            final String packet = (String) uploadResponse.payload.get(ServerResponse.DATA);
                            return ValidatePacket
                                    .with(sdk)
                                    .supplyAsync(Optional.of(Map.of(ValidatePacket.PACKET, packet)));
                          }
                          default: {
                            return CompletableFuture.completedFuture(uploadResponse);
                          }
                        }
                      }).apply(Optional.of(Destination.XQ),result)

    ).apply(maybeArgs);


  }

  @Override
  public String moduleName() {
    return "UploadKey";
  }

}
