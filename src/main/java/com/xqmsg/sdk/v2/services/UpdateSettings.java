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



public class UpdateSettings extends XQModule {

  private static final Logger logger = Logger(UpdateSettings.class);

  public static final String NOTIFICATIONS="notifications";
  public static final String NEWSLETTER="newsletter";

  private static final String SERVICE_NAME = "settings";

  private UpdateSettings(XQSDK sdk) {
    assert sdk != null : "An instance of the XQSDK is required";
    super.sdk = sdk;
    super.cache = sdk.getCache();
  }

  @Override
  public List<String> requiredFields() { return  List.of();}

  /**
   * @param sdk App Settings
   * @returns this
   */
  public static UpdateSettings with(XQSDK sdk) {
    return new UpdateSettings(sdk);
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   * <pre>parameter details:<br>
   * Boolean newsLetter! - Should this user receive newsletters or not? <br>
   *                       This is only valid for new users, and is ignored if the user already exists.
   *  Notifications notifications! - Specifies the notifications that the user should receive  <br>.
   * </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:{}}>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {


    return CompletableFuture.completedFuture(
            validate.andThen((result)->
                    authorize.andThen(
                            (authorizationToken) -> {
                              Map<String, String> headerProperties = Map.of("Authorization", String.format("Bearer %s", authorizationToken));
                              return sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
                                      Optional.of(SERVICE_NAME),
                                      CallMethod.Options,
                                      Optional.of(headerProperties),
                                      Optional.of(Destination.XQ),
                                      maybeArgs);
                            }).apply(Optional.of(Destination.XQ),result)
            )
                    .apply(maybeArgs));






  }

  @Override
  public String moduleName() {
    return "UpdateUSerSettings";
  }

}
