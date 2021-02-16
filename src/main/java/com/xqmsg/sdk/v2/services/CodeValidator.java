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
 * Authenticate the PIN which resulted from the preceding {@link Authorize} service call.<br>
 * If successful this service returns a server response containing the access token.
 *
 */
public class CodeValidator extends XQModule {

  private final Logger logger = Logger.getLogger(getClass().getName(), null);

  public static final String PIN = "pin";

  private static final String SERVICE_NAME = "codevalidation";

  private CodeValidator(XQSDK sdk) {
    assert sdk != null : "An instance of the XQSDK is required";
    super.sdk = sdk;
    super.cache = sdk.getCache();
  }

  @Override
  public List<String> requiredFields() {
    return List.of(PIN);
  }

  /**
   * @param sdk App Settings
   * @returns this
   */
  public static CodeValidator with(XQSDK sdk) {
    return new CodeValidator(sdk);
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   *                  <pre>parameter details:<br>
   *                                                    String pin! - Pin to validate the access request.<br>
   *                                                    </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:String}}>>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

    return
            validate.andThen(
                    preAuthorize.andThen(
                            (temporaryAccessToken) -> {

                                Map<String, String> headerProperties = Map.of("Authorization", String.format("Bearer %s", temporaryAccessToken));
                                ServerResponse validationResponse = sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
                                        Optional.of(SERVICE_NAME),
                                        CallMethod.Get,
                                        Optional.of(headerProperties),
                                        maybeArgs);
                                switch (validationResponse.status) {
                                  case Ok: {
                                    return ExchangeForAccessToken
                                            .with(sdk)
                                            .supplyAsync(Optional.empty());
                                  }
                                  default: {
                                    return CompletableFuture.completedFuture(validationResponse);
                                  }
                                }


                            }))

                    .apply(maybeArgs)
                    .exceptionally(e -> new ServerResponse(CallStatus.Error, Reasons.MissingParameters, e.getMessage()));


  }

  @Override
  public String moduleName() {
    return "CodeValidator";
  }

}
