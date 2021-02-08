package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.AlgorithmEnum;
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

import static java.util.Arrays.asList;

public class Decrypt extends XQModule {

  private final Logger logger = Logger.getLogger(getClass().getName(), null);

  public static final String LOCATOR_TOKEN = "locatorToken";
  public static final String ENCRYPTED_TEXT = "encryptedText";

  private final XQSDK sdk;
  private final String accessToken;
  private final AlgorithmEnum algorithm;

  private Decrypt(XQSDK sdk, AlgorithmEnum algorithm, String accessToken) {
    this.sdk = sdk;
    this.algorithm = algorithm;
    this.accessToken = accessToken;
  }

  /**
   * @param sdk App Settings
   * @param algorithm the {@link AlgorithmEnum} used to encrypt the data.
   * @param accessToken Access Token retrieved by {@link ExchangeForAccessToken}
   * @returns this
   */
  public static Decrypt with(XQSDK sdk, AlgorithmEnum algorithm, String accessToken) {
    return new Decrypt(sdk, algorithm, accessToken);
  }

  @Override
  public List<String> requiredFields() {
    return asList(LOCATOR_TOKEN, ENCRYPTED_TEXT);
  }


  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   * <pre>parameter details:<br>
   * String locatorToken! - The locator token needed to fetch the encryption key from the server.<br>
   * String encryptedText!  - The text to decrypt.<br>
   * </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:String}>>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

    return validateInput(maybeArgs)
            .thenCompose((validatedArgs) -> {
              Map<String, Object> args = validatedArgs.get();

              String locatorToken = (String) args.get(LOCATOR_TOKEN);
              String encryptedText = (String) args.get(ENCRYPTED_TEXT);

              return RetrieveKey.with(sdk, accessToken)
                      .supplyAsync(Optional.of(Map.of(RetrieveKey.LOCATOR_TOKEN, locatorToken)))
                      .thenCompose(
                              (ServerResponse keyRetrievalResponse) -> {
                                switch (keyRetrievalResponse.status) {
                                  case Ok: {
                                    final String encryptionKey = (String) keyRetrievalResponse.payload.get(ServerResponse.DATA);
                                    logger.info(String.format("retrieveKeyFromServer=>encryptionKey: %s", encryptionKey));
                                    return sdk.getAlgorithm(algorithm).decrypt(encryptedText, encryptionKey);
                                  }
                                  case Error: {
                                    return CompletableFuture.completedFuture(keyRetrievalResponse);
                                  }
                                  default:
                                    throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", keyRetrievalResponse.status));

                                }
                              });
            })
            .exceptionally(e -> new ServerResponse(CallStatus.Error, Reasons.MissingParameters, e.getMessage()));
  }

  @Override
  public String moduleName() {
    return "Decrypt";
  }


}
