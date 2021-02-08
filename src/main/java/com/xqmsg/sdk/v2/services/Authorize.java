package com.xqmsg.sdk.v2.services;

import com.xqmsg.com.sdk.v2.caching.XQCache;
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
 * Request an access token given an email address.<br>
 * If successful, the service itself will return a pre-authorization token that can be exchanged<br>
 * for a full access token after validation is complete.<br>
 * The user will also receive an email containing:<br>
 * 1. validation PIN<br>
 * 2. validation Link<br>
 * The user can then choose to either click the link to complete the process or use the PIN.<br>
 * The pin servers as the input parameter of {@link CodeValidator}.<br>
 */
public class Authorize extends XQModule {

  private final Logger logger = Logger.getLogger(getClass().getName(), null);

  public static String USER = "user";
  public static String FIRST_NAME = "firstName";
  public static String LAST_NAME = "lastName";
  public static String NEWSLETTER = "newsletter";
  public static String NOTIFICATIONS = "notifications";

  private static final String SERVICE_NAME = "authorize";
  private final XQSDK sdk;
  private final XQCache cache;

  private Authorize(XQSDK sdk) {
    assert sdk != null : "An instance of the XQSDK is required";
    this.sdk = sdk;
    this.cache = sdk.getCache();
  }

  /**
   * @param sdk App Settings
   * @returns Authorize
   */
  public static Authorize with(XQSDK sdk) {
    return new Authorize(sdk);
  }

  @Override
  public List<String> requiredFields() {
    return List.of(USER);
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
            validate.andThen(
                    (args) -> {
                      ServerResponse serverResponse = sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
                              Optional.of(SERVICE_NAME),
                              CallMethod.Post,
                              Optional.empty(),
                              maybeArgs);

                      Map<String, Object> inputArguments = maybeArgs.get();
                      String user = (String) inputArguments.get(USER);

                      switch (serverResponse.status) {
                        case Ok: {
                          String temporaryAccessToken = (String) serverResponse.payload.get(ServerResponse.DATA);
                          cache.putXQPreAuthToken(user, temporaryAccessToken);
                          cache.putActiveProfile(user);
                        }
                        default: {
                          return serverResponse;
                        }
                      }
                    })
                    .apply(maybeArgs))
            .exceptionally(e ->
                    new ServerResponse(CallStatus.Error, Reasons.MissingParameters, e.getMessage()));


  }

  @Override
  public String moduleName() {
    return "Authorize";
  }

}
