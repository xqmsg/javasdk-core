package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.AlgorithmEnum;
import com.xqmsg.sdk.v2.ServerResponse;
import com.xqmsg.sdk.v2.XQModule;
import com.xqmsg.sdk.v2.XQSDK;
import com.xqmsg.sdk.v2.algorithms.XQAlgorithm;
import com.xqmsg.sdk.v2.quantum.FetchQuantumEntropy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
/**
 * Encrypts data stored in a file using the {@link AlgorithmEnum} provided.
 */
public class FileEncrypt extends XQModule {

  private final Logger logger = Logger.getLogger(getClass().getName(), null);

  public static final String KEY = "key";
  public static final String SOURCE_FILE_PATH = "sourceFilePath";
  public static final String TARGET_FILE_PATH = "targetFilePath";
  public static final String USER = "user";
  public static final String RECIPIENTS = "recipients";
  public static final String DELETE_ON_RECEIPT = "dor";
  public static final String MESSAGE_EXPIRATION_HOURS = "expires";

  private final AlgorithmEnum algorithm;

  private FileEncrypt(XQSDK sdk, AlgorithmEnum algorithm) {
    assert sdk != null : "An instance of the XQSDK is required";
    super.sdk = sdk;
    super.cache = sdk.getCache();
    this.algorithm = algorithm;
  }

  @Override
  public List<String> requiredFields() {
    return asList(USER, SOURCE_FILE_PATH, TARGET_FILE_PATH, RECIPIENTS, MESSAGE_EXPIRATION_HOURS);
  }

  /**
   * @param sdk App Settings
   * @param algorithm the {@link AlgorithmEnum} used to encrypt the data.
   * @returns this
   */
  public static FileEncrypt with(XQSDK sdk, AlgorithmEnum algorithm) {
    return new FileEncrypt(sdk, algorithm);
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   * <pre>parameter details:<br>
   * String user! - Email of the validated user and author of the message.<br>
   * Path sourceFilePath! - Path to the file to be encrypted.<br>
   * Path targetFilePath! - Path to the decrypted file.<br>
   * List<String> recipients! - List of emails of those recipients who are allowed to access the key.<br>
   * Long expires! - The number of hours that this key will remain valid for. After this time, it will no longer be accessible.<br>
   * Boolean dor? [false] - Should the content be deleted after opening.<br>
   * </pre>
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
                              final Path sourceFilePath = (Path) args.get(SOURCE_FILE_PATH);
                              final Path targetFilePath = (Path) args.get(TARGET_FILE_PATH);
                              final List<String> recipients = (List<String>) args.get(RECIPIENTS);
                              final Integer expiration = (Integer) args.get(MESSAGE_EXPIRATION_HOURS);
                              final boolean deleteOnReceipt = args.get(DELETE_ON_RECEIPT) != null && (boolean) args.getOrDefault(DELETE_ON_RECEIPT, false);
                              final XQAlgorithm algorithm = sdk.getAlgorithm(this.algorithm);

                              return FetchQuantumEntropy
                                      .with(sdk)
                                      .supplyAsync(Optional.empty())
                                      .thenCompose((keyResponse) -> {
                                        switch (keyResponse.status) {
                                          case Ok: {
                                            final String initialKey = (String) keyResponse.payload.get(ServerResponse.DATA);
                                            try {
                                              String expandedKey = algorithm.expandKey(initialKey, (int) (Files.size(sourceFilePath) > 4096 ? 4096: Math.max(2048, Files.size(sourceFilePath) )));
                                              logger.info(String.format("expanded key:  %s", expandedKey));
                                              return UploadKey.with(sdk)
                                                      .supplyAsync(Optional.of(Map.of(
                                                              KEY, algorithm.prefix() + expandedKey,
                                                              RECIPIENTS, recipients.stream().collect(Collectors.joining(",")),
                                                              MESSAGE_EXPIRATION_HOURS, expiration,
                                                              DELETE_ON_RECEIPT, deleteOnReceipt)))
                                                      .thenCompose((uploadResponse) -> {
                                                        switch (uploadResponse.status) {
                                                          case Ok: {
                                                            final String locatorToken = (String) uploadResponse.payload.get(ServerResponse.DATA);
                                                            logger.info(String.format("validateUploadStep=>locator: %s", locatorToken));
                                                            return algorithm
                                                                    .encrypt(sourceFilePath, targetFilePath, expandedKey, locatorToken);
                                                          }
                                                          case Error: {
                                                            return CompletableFuture.completedFuture(uploadResponse);
                                                          }
                                                          default:
                                                            throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", uploadResponse.status));
                                                        }
                                                      });
                                            } catch (IOException e) {
                                              e.printStackTrace();
                                            }
                                          }
                                          case Error: {
                                            return CompletableFuture.completedFuture(keyResponse);
                                          }
                                          default:
                                            throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", keyResponse.status));
                                        }
                                      });
                            })
            ).apply(maybeArgs);




  }

  @Override
  public String moduleName() {
    return "Encrypt";
  }



}
