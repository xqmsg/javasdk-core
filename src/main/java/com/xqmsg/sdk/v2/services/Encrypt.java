package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.AlgorithmEnum;
import com.xqmsg.sdk.v2.CallStatus;
import com.xqmsg.sdk.v2.Reasons;
import com.xqmsg.sdk.v2.ServerResponse;
import com.xqmsg.sdk.v2.XQModule;
import com.xqmsg.sdk.v2.XQSDK;
import com.xqmsg.sdk.v2.algorithms.XQAlgorithm;
import com.xqmsg.sdk.v2.quantum.FetchQuantumEntropy;
import com.xqmsg.sdk.v2.utils.Destination;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
/**
 * @class
 * Encrypts textual data using the {@link AlgorithmEnum} provided.
 */
public class Encrypt extends XQModule {

  private final Logger logger = Logger.getLogger(getClass().getName(), null);

  public static final String KEY = "key";
  public static final String LOCATOR_KEY = "locatorKey";
  public static final String ENCRYPTED_TEXT = "encryptedText";
  public static final String TEXT = "text";
  public static final String USER = "user";
  public static final String RECIPIENTS = "recipients";
  public static final String DELETE_ON_RECEIPT = "dor";
  public static final String MESSAGE_EXPIRATION_HOURS = "expires";

  private final AlgorithmEnum algorithm;

  private Encrypt(XQSDK sdk, AlgorithmEnum algorithm) {
    assert sdk != null : "An instance of the XQSDK is required";
    super.sdk = sdk;
    super.cache = sdk.getCache();
    this.algorithm = algorithm;
  }

  @Override
  public List<String> requiredFields() {
    return asList(USER, TEXT, RECIPIENTS, MESSAGE_EXPIRATION_HOURS);
  }

  /**
   * @param sdk App Settings
   * @param algorithm the {@link AlgorithmEnum} used to encrypt the data.
   * @returns this
   */
  public static Encrypt with(XQSDK sdk, AlgorithmEnum algorithm) {
    return new Encrypt(sdk, algorithm);
  }

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   * <pre>parameter details:<br>
   * String user! - Email of the validated user and author of the message.<br>
   * List<String> recipients! - List of emails of users intended to have read access to the encrypted content.<br>
   * String text! - Text to be encrypted.<br>
   * Long expires! - The number of hours that this key will remain valid for. After this time, it will no longer be accessible.<br>
   * Boolean dor? [false] - Should the content be deleted after opening.<br>
   * </pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{locatorKey:string, encryptedText:string}}>>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

    return
            validate.andThen((result)->
                    authorize.andThen(
                            (authorizationToken) -> {
                              Map<String, Object> args = maybeArgs.get();
                              final String message = (String) args.get(TEXT);
                              final List<String> recipients = (List<String>) args.get(RECIPIENTS);
                              final Integer expiration = (Integer) args.get(MESSAGE_EXPIRATION_HOURS);
                              final boolean deleteOnReceipt = args.get(DELETE_ON_RECEIPT) != null && (boolean) args.getOrDefault(DELETE_ON_RECEIPT, false);
                              final XQAlgorithm algorithm = sdk.getAlgorithm(this.algorithm);

                              return FetchQuantumEntropy
                                      .with(sdk)
                                      .supplyAsync(Optional.empty())
                                      .thenCompose((ServerResponse keyResponse) -> {
                                        switch (keyResponse.status) {
                                          case Ok: {
                                            final String initialKey = (String) keyResponse.payload.get(ServerResponse.DATA);
                                            try {
                                              String expandedKey = algorithm.expandKey(initialKey, message.length() > 4096 ? 4096 : Math.max(2048, message.length()));
                                              return algorithm
                                                      .encrypt(message, expandedKey)
                                                      .thenCompose((encryptServerResponse) -> {
                                                        final byte[] encryptedBytes = (byte[]) encryptServerResponse.payload.get(ServerResponse.DATA);
                                                        String encryptedText = new String(encryptedBytes, StandardCharsets.UTF_8);
                                                        return UploadKey
                                                                .with(sdk)
                                                                .supplyAsync(Optional.of(Map.of(
                                                                        KEY, algorithm.prefix() + expandedKey,
                                                                        RECIPIENTS, recipients.stream().collect(Collectors.joining(",")),
                                                                        MESSAGE_EXPIRATION_HOURS, expiration,
                                                                        DELETE_ON_RECEIPT, deleteOnReceipt)))
                                                                .thenApply(
                                                                        (validateResponse) -> {
                                                                          switch (validateResponse.status) {
                                                                            case Ok: {
                                                                              final String locator = (String) validateResponse.payload.get(ServerResponse.DATA);
                                                                              return new ServerResponse(CallStatus.Ok, Map.of(Encrypt.LOCATOR_KEY, locator, Encrypt.ENCRYPTED_TEXT, encryptedText));
                                                                            }
                                                                            default: {
                                                                              return validateResponse;
                                                                            }

                                                                          }
                                                                        });
                                                      });
                                            } catch (Exception e) {
                                              String errorMessage = e.getMessage();
                                              logger.warning(errorMessage);
                                              return CompletableFuture.completedFuture(new ServerResponse(CallStatus.Error, Reasons.LocalException, errorMessage));
                                            }
                                          }
                                          case Error: {
                                            return CompletableFuture.completedFuture(keyResponse);
                                          }
                                          default:
                                            throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", keyResponse.status));
                                        }
                                      });

                    }).apply(Optional.of(Destination.XQ), result)
            )
            .apply(maybeArgs) ;



}

  @Override
  public String moduleName() {
    return "Encrypt";
  }



}
