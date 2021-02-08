package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.AlgorithmEnum;
import com.xqmsg.sdk.v2.CallStatus;
import com.xqmsg.sdk.v2.Reasons;
import com.xqmsg.sdk.v2.ServerResponse;
import com.xqmsg.sdk.v2.XQModule;
import com.xqmsg.sdk.v2.XQSDK;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static java.util.Arrays.asList;


/**
 * Decrypts data stored in a file using the {@link AlgorithmEnum} provided.
 */
public class FileDecrypt extends XQModule {

  private final Logger logger = Logger.getLogger(getClass().getName(), null);

  public static final String LOCATOR_TOKEN = "locatorToken";
  public static final String SOURCE_FILE_PATH = "sourceFilePath";
  public static final String TARGET_FILE_PATH = "targetFilePath";

  private final XQSDK sdk;
  private final String accessToken;
  private final AlgorithmEnum algorithm;

  private FileDecrypt(XQSDK sdk, AlgorithmEnum algorithm, String accessToken) {
    this.sdk = sdk;
    this.algorithm = algorithm;
    this.accessToken = accessToken;
  }

  @Override
  public List<String> requiredFields() {
    return asList(SOURCE_FILE_PATH, TARGET_FILE_PATH);
  }

  /**
   * @param sdk App Settings
   * @param algorithm the {@link AlgorithmEnum} used to encrypt the data.
   * @param accessToken Access Token retrieved by {@link ExchangeForAccessToken}
   * @returns this
   */
  public static FileDecrypt with(XQSDK sdk, AlgorithmEnum algorithm, String accessToken) {
    return new FileDecrypt(sdk, algorithm, accessToken);
  }
  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   * <pre>parameter details:<br>
   * Path sourceFilePath! - Path to the file to be decrypted.<br>
   * Path targetFilePath! - Path to the encrypted file.<br>
   * </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:File}>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

    return validateInput(maybeArgs)
            .thenCompose((validatedArgs) -> {
              Map<String, Object> args = validatedArgs.get();

              Path sourceFilePath = (Path) args.get(SOURCE_FILE_PATH);
              Path targetFilePath = (Path) args.get(TARGET_FILE_PATH);

              return sdk.getAlgorithm(algorithm)
                      .decrypt(sourceFilePath, targetFilePath,
                              (aLocatorToken) -> {
                       return RetrieveKey.with(sdk, accessToken)
                                        .supplyAsync(Optional.of(Map.of(RetrieveKey.LOCATOR_TOKEN, aLocatorToken)))
                                        .thenApply(rsp -> {
                                          String key = (String) rsp.payload.get(ServerResponse.DATA);
                                          return key;
                                        });
                              }
                      );

            })
            .exceptionally(e -> new ServerResponse(CallStatus.Error, Reasons.MissingParameters, e.getMessage()));
  }

  @Override
  public String moduleName() {
    return "Decrypt";
  }


}
