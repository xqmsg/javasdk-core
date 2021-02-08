package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.AlgorithmEnum;
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

  public static final String SOURCE_FILE_PATH = "sourceFilePath";
  public static final String TARGET_FILE_PATH = "targetFilePath";

  private final AlgorithmEnum algorithm;

  private FileDecrypt(XQSDK sdk, AlgorithmEnum algorithm) {
    super.sdk = sdk;
    super.cache = sdk.getCache();
    this.algorithm = algorithm;
  }

  @Override
  public List<String> requiredFields() {
    return asList(SOURCE_FILE_PATH, TARGET_FILE_PATH);
  }

  /**
   * @param sdk       App Settings
   * @param algorithm the {@link AlgorithmEnum} used to encrypt the data.
   * @returns this
   */
  public static FileDecrypt with(XQSDK sdk, AlgorithmEnum algorithm) {
    return new FileDecrypt(sdk, algorithm);
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   *                  <pre>parameter details:<br>
   *                  Path sourceFilePath! - Path to the file to be decrypted.<br>
   *                  Path targetFilePath! - Path to the encrypted file.<br>
   *                  </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:File}>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

    return
            validate.andThen(
                    authorize.andThen(
                            (authorizationToken) -> {
                              Map<String, Object> args = maybeArgs.get();

                              Path sourceFilePath = (Path) args.get(SOURCE_FILE_PATH);
                              Path targetFilePath = (Path) args.get(TARGET_FILE_PATH);

                              return sdk.getAlgorithm(algorithm)
                                      .decrypt(sourceFilePath, targetFilePath,
                                              (aLocatorToken) -> {
                                                return FetchKey.with(sdk)
                                                        .supplyAsync(Optional.of(Map.of(FetchKey.LOCATOR_TOKEN, aLocatorToken)))
                                                        .thenApply(fetchKeyResponse -> {
                                                          switch (fetchKeyResponse.status) {
                                                            case Ok: {
                                                              String key = (String) fetchKeyResponse.payload.get(ServerResponse.DATA);
                                                              return key;
                                                            }
                                                            default: {
                                                              logger.warning(String.format("failed to fetch key, reason: %s", fetchKeyResponse.moreInfo()));
                                                              return null;
                                                            }
                                                          }

                                                        });

                              });

                            })
            ).apply(maybeArgs);

  }

  @Override
  public String moduleName() {
    return "Decrypt";
  }


}
