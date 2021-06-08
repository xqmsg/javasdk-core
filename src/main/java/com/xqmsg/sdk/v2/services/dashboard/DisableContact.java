package com.xqmsg.sdk.v2.services.dashboard;

import com.xqmsg.sdk.v2.*;
import com.xqmsg.sdk.v2.utils.Destination;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * A service to edit an existing user group within the dashboard
 *
 */
public class DisableContact extends XQModule {

  private final Logger logger = Logger.getLogger(getClass().getName(), null);


  public static String  ID= "id";

  private static final String SERVICE_NAME = "contact";

  private DisableContact(XQSDK sdk) {
    assert sdk != null : "An instance of the XQSDK is required";
     super.sdk = sdk;
     super.cache = sdk.getCache();
  }

  /**
   * @param sdk App Settings
   * @returns RemoveUserGroup
   */
  public static DisableContact with(XQSDK sdk) {
    return new DisableContact(sdk);
  }

  @Override
  public List<String> requiredFields() {
    return List.of(ID);
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   *                  <pre>parameter details:<br>
   *                                   String user! - Email of the user to be authorized.<br>
   *                                   String firstName?  - First name of the user.<br>
   *                                   String lastName? - Last name of the user.<br>
   *                                   Boolean newsLetter? [false] - Should the user receive a newsletter.<br>
   *                                   NotificationEnum notifications? [0] - Enum Value to specify Notification Settings.<br>
   *                                   </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:String}>>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

    return CompletableFuture.completedFuture(
            validate
                    .andThen((validatedArgs) ->
                            authorize
                                    .andThen((dashboardAccessToken) -> {
                                      Map<String, String> headerProperties = Map.of("Authorization", String.format("Bearer %s", dashboardAccessToken));
                                        final String DYNAMIC_SERVICE_NAME = String.format("%s/%s", SERVICE_NAME, validatedArgs.get().get(ID));
                                        return sdk.call(sdk.DASHBOARD_SERVER_URL,
                                              Optional.of(DYNAMIC_SERVICE_NAME),
                                              CallMethod.Delete,
                                              Optional.of(headerProperties),
                                              Optional.of(Destination.DASHBOARD),
                                              Optional.empty());
                                    })
                                    .apply(Optional.of(Destination.DASHBOARD), validatedArgs)
                    )
                    .apply(maybeArgs))
            .exceptionally(e -> new ServerResponse(CallStatus.Error, Reasons.LocalException, e.getMessage()));

  }

  @Override
  public String moduleName() {
    return "DisableContact";
  }

}
