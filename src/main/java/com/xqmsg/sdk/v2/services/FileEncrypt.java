package com.xqmsg.sdk.v2.services;

import com.xqmsg.com.sdk.v2.quantum.FetchQuantumEntropy;
import com.xqmsg.sdk.v2.AlgorithmEnum;
import com.xqmsg.sdk.v2.CallStatus;
import com.xqmsg.sdk.v2.Reasons;
import com.xqmsg.sdk.v2.ServerResponse;
import com.xqmsg.sdk.v2.XQModule;
import com.xqmsg.sdk.v2.XQSDK;
import com.xqmsg.sdk.v2.algorithms.XQAlgorithm;

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
  public static final String LOCATOR_KEY = "locatorKey";
  public static final String ENCRYPTED_TEXT = "encryptedText";
  public static final String SOURCE_FILE_PATH = "sourceFilePath";
  public static final String TARGET_FILE_PATH = "targetFilePath";
  public static final String USER = "user";
  public static final String RECIPIENTS = "recipients";
  public static final String DELETE_ON_RECEIPT = "dor";
  public static final String MESSAGE_EXPIRATION_HOURS = "expires";
  private final XQSDK sdk;
  private final String accessToken;
  private final AlgorithmEnum algorithm;

  private FileEncrypt(XQSDK sdk, AlgorithmEnum algorithm, String accessToken) {
    this.sdk = sdk;
    this.algorithm = algorithm;
    this.accessToken = accessToken;
  }

  @Override
  public List<String> requiredFields() {
    return asList(USER, SOURCE_FILE_PATH, TARGET_FILE_PATH, RECIPIENTS, MESSAGE_EXPIRATION_HOURS);
  }

  /**
   * @param sdk App Settings
   * @param algorithm the {@link AlgorithmEnum} used to encrypt the data.
   * @param accessToken Access Token retrieved by {@link ExchangeForAccessToken}
   * @returns this
   */
  public static FileEncrypt with(XQSDK sdk, AlgorithmEnum algorithm, String accessToken) {
    return new FileEncrypt(sdk, algorithm, accessToken);
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

    return validateInput(maybeArgs)
            .thenCompose((validatedArgs) -> {

              Map<String, Object> args = validatedArgs.get();
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

                              return AddNewKeyPacket.with(sdk, accessToken)
                                      .supplyAsync(Optional.of(Map.of(
                                              KEY, algorithm.prefix() + expandedKey,
                                              RECIPIENTS, recipients.stream().collect(Collectors.joining(",")),
                                              MESSAGE_EXPIRATION_HOURS, expiration,
                                              DELETE_ON_RECEIPT, deleteOnReceipt)))
                                      .thenCompose((uploadResponse) -> {
                                        switch (uploadResponse.status) {
                                          case Ok: {
                                            final String packet = (String) uploadResponse.payload.get(ServerResponse.DATA);
                                            logger.info(String.format("uploadQuantumKeyAndRecipientsStep=>packet: %s", packet));
                                            return ValidateNewKeyPacket.with(sdk, accessToken)
                                                    .supplyAsync(Optional.of(Map.of(ValidateNewKeyPacket.PACKET, packet)))
                                                    .thenCompose((validateResponse) -> {
                                                      switch (validateResponse.status) {
                                                        case Ok: {
                                                          final String locatorToken = (String) validateResponse.payload.get(ServerResponse.DATA);
                                                          logger.info(String.format("validateUploadStep=>locator: %s", locatorToken));
                                                          return algorithm
                                                                  .encrypt(sourceFilePath, targetFilePath, expandedKey, locatorToken);
                                                        }
                                                        case Error: {
                                                          return CompletableFuture.completedFuture(validateResponse);
                                                        }
                                                        default:
                                                          throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", validateResponse.status));
                                                      }

                                                    });
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
            .exceptionally(e -> new ServerResponse(CallStatus.Error, Reasons.MissingParameters, e.getMessage()));
  }

  @Override
  public String moduleName() {
    return "Encrypt";
  }



}
