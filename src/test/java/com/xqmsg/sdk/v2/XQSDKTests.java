package com.xqmsg.sdk.v2;


import com.xqmsg.com.sdk.v2.exceptions.StatusCodeException;
import com.xqmsg.sdk.v2.services.Authorize;
import com.xqmsg.sdk.v2.services.AuthorizeAlias;
import com.xqmsg.sdk.v2.services.AuthorizeDelegate;
import com.xqmsg.sdk.v2.services.CheckApiKey;
import com.xqmsg.sdk.v2.services.CheckKeyExpiration;
import com.xqmsg.sdk.v2.services.CodeValidator;
import com.xqmsg.sdk.v2.services.CombineAuthorizations;
import com.xqmsg.sdk.v2.services.Decrypt;
import com.xqmsg.sdk.v2.services.DeleteAuthorization;
import com.xqmsg.sdk.v2.services.DeleteSubscriber;
import com.xqmsg.sdk.v2.services.Encrypt;
import com.xqmsg.sdk.v2.services.FetchKey;
import com.xqmsg.sdk.v2.services.FileDecrypt;
import com.xqmsg.sdk.v2.services.FileEncrypt;
import com.xqmsg.sdk.v2.services.GetSettings;
import com.xqmsg.sdk.v2.services.GetUserInfo;
import com.xqmsg.sdk.v2.services.GrantKeyAccess;
import com.xqmsg.sdk.v2.services.RevokeKeyAccess;
import com.xqmsg.sdk.v2.services.RevokeUserAccess;
import com.xqmsg.sdk.v2.services.UpdateSettings;
import com.xqmsg.sdk.v2.utils.DateTimeFormats;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static com.xqmsg.sdk.v2.CallStatus.Ok;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class XQSDKTests {

  private static final Logger logger = getLogger(XQSDKTests.class);

  static Map<String, String> map = null;

  static XQSDK sdk;

  @BeforeAll
  static void init() {
    sdk = new XQSDK();
    if (Boolean.parseBoolean(System.getProperty("clear-cache", "true"))) {
      sdk.getCache().clearAllProfiles();
    }
    map = new HashMap<>();
  }


  /**
   * This test  requires user input. <br> You may have to configure IntelliJ to do so. <br>
   * Please see {@link #readOneLineFromTerminalInput} for more information.<br>
   */
  @Test
  @Order(10)
  void testAuthorize() throws Exception {

    String email = System.getProperty("xqsdk-user.email");

    try {
      sdk.getCache().getXQAccess(email, true);
    } catch (StatusCodeException e) {

      logger.info(String.format("%s", e.statusMessage()));

      Map<String, Object> payload =
              Map.of(Authorize.USER, email,
                      Authorize.FIRST_NAME, "User",
                      Authorize.LAST_NAME, "XQMessage",
                      Authorize.NEWSLETTER, true,
                      Authorize.NOTIFICATIONS,
                      Notifications.TUTORIALS.ordinal());

      String newAccessToken = authorize(sdk, payload).get();
      assertNotEquals("", newAccessToken);

    }

  }

  @Test
  @Order(20)
  void testGetUserInfo() throws Exception {

    GetUserInfo.with(sdk)
            .supplyAsync(Optional.empty())
            .thenAccept((serverResponse) -> {

              switch (serverResponse.status) {
                case Ok: {

                  long id = (Long) serverResponse.payload.get(GetUserInfo.ID);
                  String usr = (String) serverResponse.payload.get(GetUserInfo.USER);
                  String firstName = (String) serverResponse.payload.get(GetUserInfo.FIRST_NAME);
                  String lastName = (String) serverResponse.payload.get(GetUserInfo.LAST_NAME);
                  String subscriptionStatus = (String) serverResponse.payload.get(GetUserInfo.SUBSCRIPTION_STATUS);
                  Long starts = (Long) serverResponse.payload.get(GetUserInfo.STARTS);
                  Long ends = (Long) serverResponse.payload.get(GetUserInfo.ENDS);

                  logger.info("Id: " + id);
                  logger.info("User: " + usr);
                  logger.info("First Name: " + firstName);
                  logger.info("Last Name: " + lastName);
                  logger.info("Subscription Status: " + subscriptionStatus);

                  LocalDateTime startDateTime = new LocalDateTime(starts);
                  logger.info("Subscription Start: " + DateTimeFormats.render(startDateTime, DateTimeFormats.ISO_8601_DATE_TIME));

                  LocalDateTime endDateTime = new LocalDateTime(ends);
                  logger.info("Subscription Ends: " + DateTimeFormats.render(endDateTime, DateTimeFormats.ISO_8601_DATE_TIME));
                  break;
                }
                case Error: {
                  logger.warning(String.format("failed , reason: %s", serverResponse.moreInfo()));
                  fail();
                  break;
                }
                default:
                  throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
              }

            }).get();

  }

  @Test
  @Order(30)
  void testGetUserSettings() throws Exception {

    GetSettings.with(sdk)
            .supplyAsync(Optional.empty())
            .thenAccept((serverResponse) -> {

              switch (serverResponse.status) {
                case Ok: {

                  boolean newsletter = (Boolean) serverResponse.payload.get(GetSettings.NEWSLETTER);
                  Long notifications = (Long) serverResponse.payload.get(GetSettings.NOTIFICATIONS);

                  logger.info("Receives Newsletters: " + newsletter);
                  logger.info("Notifications: " + Notifications.name(Long.valueOf(notifications).intValue()));
                  break;
                }
                case Error: {
                  fail();
                  logger.warning(String.format("failed , reason: %s", serverResponse.moreInfo()));
                  break;
                }
                default:
                  fail();
                  throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
              }

            }).get();

  }

  @Test
  @Order(40)
  void testUpdateUserSettings() throws Exception {


    Map<String, Object> payload = Map.of(GetSettings.NEWSLETTER, false, GetSettings.NOTIFICATIONS, 2);

    UpdateSettings.with(sdk)
            .supplyAsync(Optional.of(payload))
            .thenAccept((serverResponse) -> {

              switch (serverResponse.status) {
                case Ok: {
                  String noContent = (String) serverResponse.payload.get(ServerResponse.DATA);
                  logger.info("Status: " + Ok.name());
                  logger.info("Data: " + noContent);
                  break;
                }
                case Error: {
                  logger.warning(String.format("failed , reason: %s", serverResponse.moreInfo()));
                  fail();
                  break;
                }
                default:
                  fail();
                  throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
              }

            }).get();


  }

  /**
   * This test  requires user input. <br> You may have to configure IntelliJ to do so. <br>
   * Please see {@link #readOneLineFromTerminalInput} for more information.<br>
   */
  @Test
  @Order(50)
  void testEncrypt() throws Exception {

    String email = System.getProperty("xqsdk-user.email");

    List recipients = List.of(System.getProperty("xqsdk-recipients.email"));

    //final String text = getMessageFromTerminalInput();
    final String text =
            "\n\nThe first stanza of Pushkin's Bronze Horseman (Russian):\n" +
                    "На берегу пустынных волн\n" +
                    "Стоял он, дум великих полн,\n" +
                    "И вдаль глядел. Пред ним широко\n" +
                    "Река неслася; бедный чёлн\n" +
                    "По ней стремился одиноко.\n" +
                    "По мшистым, топким берегам\n" +
                    "Чернели избы здесь и там,\n" +
                    "Приют убогого чухонца;\n" +
                    "И лес, неведомый лучам\n" +
                    "В тумане спрятанного солнца,\n" +
                    "Кругом шумел.\n";

    map.put("originalText", text);

    String encryptionResult = Encrypt
            .with(sdk, AlgorithmEnum.OTPv2)
            .supplyAsync(Optional.of(Map.of(Encrypt.USER, email,
                    Encrypt.TEXT, text,
                    Encrypt.RECIPIENTS, recipients,
                    Encrypt.MESSAGE_EXPIRATION_HOURS, 1)))
            .thenApply(
                    (ServerResponse encryptResponse) -> {

                      switch (encryptResponse.status) {
                        case Ok: {
                          String locatorToken = (String) encryptResponse.payload.get(Encrypt.LOCATOR_KEY);
                          String encryptedText = (String) encryptResponse.payload.get(Encrypt.ENCRYPTED_TEXT);

                          logger.info("Locator Token: " + locatorToken);
                          logger.info("Encrypted Text: " + encryptedText);

                          map.put("locatorToken", locatorToken);
                          map.put("encryptedText", encryptedText);
                          return encryptedText;

                        }
                        case Error: {
                          logger.warning(String.format("`testEncryption` failed at encryption stage, reason: %s", encryptResponse.moreInfo()));
                          return "";
                        }
                        default:
                          throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", encryptResponse.status));
                      }

                    }
            ).get();

    assertNotEquals("", encryptionResult);
    assertNotEquals(text, encryptionResult);

  }

  @Test
  @Order(60)
  void testDecrypt() throws Exception {

    String locatorToken = map.get(Decrypt.LOCATOR_TOKEN);
    String encryptedText = map.get(Decrypt.ENCRYPTED_TEXT);

    assert locatorToken != null : "locator token cannot be null";
    assert encryptedText != null : "encrypted text cannot be null";

    String decryptionResult = null;

    decryptionResult = Decrypt
            .with(sdk, AlgorithmEnum.OTPv2)
            .supplyAsync(Optional.of(Map.of(Decrypt.LOCATOR_TOKEN, locatorToken, Decrypt.ENCRYPTED_TEXT, encryptedText)))
            .thenApply(
                    (ServerResponse decryptResponse) -> {
                      switch (decryptResponse.status) {
                        case Ok: {
                          return (String) decryptResponse.payload.get(ServerResponse.DATA);
                        }
                        case Error: {
                          return String.format("`testDecryption` failed at decryption stage, reason: %s", decryptResponse.moreInfo());
                        }
                        default: {
                          throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", decryptResponse.status));
                        }

                      }
                    }
            ).get();


    logger.info("Decrypted Text:  " + decryptionResult);

    assertEquals(map.get("originalText"), decryptionResult);


  }

  @Test
  @Order(70)
  void testKeyRetrieval() throws Exception {


    String locatorToken = map.get(Decrypt.LOCATOR_TOKEN);

    assert locatorToken != null : " locatorToken is null";

    String key = FetchKey
            .with(sdk)
            .supplyAsync(Optional.of(Map.of(Decrypt.LOCATOR_TOKEN, locatorToken)))
            .thenApply(
                    (ServerResponse serverResponse) -> {
                      switch (serverResponse.status) {
                        case Ok: {
                          return (String) serverResponse.payload.get(ServerResponse.DATA);
                        }
                        case Error: {
                          logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                          return "";
                        }
                        default:
                          throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
                      }
                    }
            ).get();

    logger.info("Key from Server:  " + key);

    assertNotEquals("", key);

  }

  /**
   * make sure to have your vm args set up for this on. check you run/debug configs.
   * should be something like this:
   * <p>
   * -Dmode=test
   * -Dxqsdk-user.email=john.doe@example.com
   * -Dxqsdk-user2.email=john.doe+2@example.com
   * <p>
   * -Dxqsdk-recipients.john.doe@example.com
   */
  @Test
  @Order(80)
  void testMergeTokens() throws Exception {

    String email1 = System.getProperty("xqsdk-user.email");
    String accessToken1 = sdk.getCache().getXQAccess(email1, true);

    String email2 = null;
    String accessToken2 = null;
    try {
      email2 = System.getProperty("xqsdk-user2.email");
      accessToken2 = sdk.getCache().getXQAccess(email2, true);
    } catch (StatusCodeException e) {
      logger.info(String.format("%s", e.statusMessage()));
      Map<String, Object> payload2 =
              Map.of(Authorize.USER, email2,
                      Authorize.FIRST_NAME, "User2",
                      Authorize.LAST_NAME, "XQMessage2",
                      Authorize.NEWSLETTER, false,
                      Authorize.NOTIFICATIONS,
                      Notifications.NONE.ordinal());

      accessToken2 = authorize(sdk, payload2).get();

       sdk.getCache().putActiveProfile(email1);
    }

    String combinedAccessToken = CombineAuthorizations
            .with(sdk)
            .supplyAsync(Optional.of(Map.of(CombineAuthorizations.TOKENS, List.of(accessToken2))))
            .thenApply(
                    (ServerResponse serverResponse) -> {
                      switch (serverResponse.status) {
                        case Ok: {
                          String combined = (String) serverResponse.payload.get(CombineAuthorizations.MERGED_TOKEN);
                          Long mergeCount = (Long) serverResponse.payload.get(CombineAuthorizations.MERGE_COUNT);
                          logger.info(String.format("Number of tokens combined: %s", mergeCount));
                          return combined;
                        }
                        case Error: {
                          logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                          return "";
                        }
                        default:
                          throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
                      }
                    }
            ).get();

    logger.info("Combined Access Token from Server:  " + combinedAccessToken);

    assertNotEquals("", combinedAccessToken);

  }

  @Test
  @Order(90)
  void testCheckKeyExpiration() throws Exception {

    String locatorToken = map.get(Decrypt.LOCATOR_TOKEN);

    assert locatorToken != null : " locatorToken is null";

    CheckKeyExpiration
            .with(sdk)
            .supplyAsync(Optional.of(Map.of(Decrypt.LOCATOR_TOKEN, locatorToken)))
            .thenAccept(
                    (ServerResponse serverResponse) -> {
                      switch (serverResponse.status) {
                        case Ok: {
                          logger.info("Status: " + Ok.name());
                          Long expiresIn = (Long) serverResponse.payload.get(CheckKeyExpiration.EXPIRES_IN);
                          LocalDateTime now = new LocalDateTime();
                          LocalDateTime expiresOn = now.plus(new Period().withSeconds(expiresIn.intValue()));
                          String expiresOnString = DateTimeFormats.render(expiresOn, DateTimeFormats.ISO_8601_DATE_TIME);
                          logger.info(String.format("Key Expires On %s", expiresOnString));

                          break;
                        }
                        case Error: {
                          logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                          fail();
                          break;
                        }
                        default:
                          throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
                      }
                    }
            ).get();


  }


  @Test
  @Order(100)
  void testAuthorizeDelegate() throws Exception {

    String delegateAccessToken =
            AuthorizeDelegate
                    .with(sdk)
                    .supplyAsync(Optional.empty())
                    .thenApply(
                            (ServerResponse serverResponse) -> {
                              switch (serverResponse.status) {
                                case Ok: {
                                  logger.info("Status: " + Ok.name());
                                  String token = (String) serverResponse.payload.get(ServerResponse.DATA);
                                  logger.info("Delegate Access Token: " + token);
                                  return token;
                                }
                                case Error: {
                                  logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                                  fail();
                                  return null;
                                }
                                default:
                                  throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
                              }
                            }
                    ).get();

    assertNotNull(delegateAccessToken);

  }

  /**
   * Can only be tested this if <br>
   * {@link #testDeleteSubscriber()}  <br>
   * is annotated with {@link Disabled}
   */
  @Test
  @Order(110)
  @Disabled
  void testDeleteAuthorization() throws Exception {

    String noContent = DeleteAuthorization
            .with(sdk)
            .supplyAsync(Optional.empty())
            .thenApply(
                    (ServerResponse serverResponse) -> {
                      switch (serverResponse.status) {
                        case Ok: {
                          String data = (String) serverResponse.payload.get(ServerResponse.DATA);
                          logger.info("Status: " + Ok.name());
                          logger.info("Data: " + data);
                          return data;
                        }
                        case Error: {
                          logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                          return "";
                        }
                        default:
                          throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
                      }
                    }
            ).get();

    assertEquals("No Content", noContent);

  }

  @Test
  @Order(120)
  @Disabled
  void testRevokeKeyAccess() throws Exception {

    String locatorToken = map.get(Decrypt.LOCATOR_TOKEN);

    assert locatorToken != null : " locatorToken is null";

    String noContent = RevokeKeyAccess
            .with(sdk)
            .supplyAsync(Optional.of(Map.of(Decrypt.LOCATOR_TOKEN, locatorToken)))
            .thenApply(
                    (ServerResponse serverResponse) -> {
                      switch (serverResponse.status) {
                        case Ok: {
                          String data = (String) serverResponse.payload.get(ServerResponse.DATA);
                          logger.info("Status: " + Ok.name());
                          logger.info("Data: " + data);
                          return data;
                        }
                        case Error: {
                          logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                          return "";
                        }
                        default:
                          throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
                      }
                    }
            ).get();

    assertEquals("No Content", noContent);

  }

  /**
   * Can only be tested this if <br>
   * {@link #testDeleteAuthorization()} )} <br>
   * is annotated with {@link Disabled}
   */
  @Test
  @Order(130)
  @Disabled
  void testDeleteSubscriber() throws Exception {

    DeleteSubscriber.with(sdk)
            .supplyAsync(Optional.empty())
            .thenAccept((serverResponse) -> {

              switch (serverResponse.status) {
                case Ok: {
                  String noContent = (String) serverResponse.payload.get(ServerResponse.DATA);
                  logger.info("Status: " + Ok.name());
                  logger.info("Data: " + noContent);
                  break;
                }
                case Error: {
                  logger.warning(String.format("failed , reason: %s", serverResponse.moreInfo()));
                  fail();
                  break;
                }
                default:
                  fail();
                  throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
              }

            }).get();


  }

  @Test
  @Order(140)
  @Disabled
  void testAESFileEncryption() throws Exception {

    String email = System.getProperty("xqsdk-user.email");

    final Path sourceSpec = Paths.get(String.format("src/test/resources/%s.txt", "utf-8-sampler"));
    final Path targetSpec = Paths.get(String.format("src/test/resources/%s.txt.xqf", "utf-8-sampler"));
    final String user = email;
    final String recipients = email;
    final Integer expiration = 5;

    Path encryptedFilePath = FileEncrypt.with(sdk, AlgorithmEnum.AES)
            .supplyAsync(Optional.of(Map.of(FileEncrypt.USER, user,
                    FileEncrypt.RECIPIENTS, recipients,
                    FileEncrypt.MESSAGE_EXPIRATION_HOURS, expiration,
                    FileEncrypt.SOURCE_FILE_PATH, sourceSpec,
                    FileEncrypt.TARGET_FILE_PATH, targetSpec)))
            .thenApply((serverResponse) -> {

              switch (serverResponse.status) {
                case Ok: {
                  var encryptFilePathResult = (Path) serverResponse.payload.get(ServerResponse.DATA);
                  return encryptFilePathResult;
                }
                case Error: {
                  logger.warning(String.format("failed , reason: %s", serverResponse.moreInfo()));
                  fail();
                  return null;
                }
                default:
                  fail();
                  throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
              }

            }).get();

    assertTrue(encryptedFilePath != null);


  }

  @Test
  @Order(150)
  @Disabled
  void testAESFileDecryption() throws Exception {

    final Path originalSpec = Paths.get(String.format("src/test/resources/%s.ctrl", "utf-8-sampler"));
    final Path encryptedSpec = Paths.get(String.format("src/test/resources/%s.txt", "utf-8-sampler"));
    final Path decryptedSpec = Paths.get(String.format("src/test/resources/%s.txt.xqf", "utf-8-sampler"));

    Path resultSpec = FileDecrypt.with(sdk, AlgorithmEnum.AES)
            .supplyAsync(Optional.of(Map.of(FileDecrypt.SOURCE_FILE_PATH, encryptedSpec,
                    FileDecrypt.TARGET_FILE_PATH, decryptedSpec)))
            .thenApply((serverResponse) -> {
              switch (serverResponse.status) {
                case Ok: {
                  var decryptFilePath = (Path) serverResponse.payload.get(ServerResponse.DATA);
                  logger.info("Encrypt Filepath: " + decryptFilePath);
                  return decryptFilePath;
                }
                case Error: {
                  logger.warning(String.format("failed , reason: %s", serverResponse.moreInfo()));
                  fail();
                  return null;
                }
                default:
                  fail();
                  throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
              }

            }).get();


    String originalFileContent = Files.readString(originalSpec);
    String decryptedFileContent = Files.readString(resultSpec);

    logger.severe(String.format("  Original File Content: %s", originalFileContent.replaceAll("\n", " ")));
    logger.severe(String.format(" Decrypted File Content: %s", decryptedFileContent.replaceAll("\n", " ")));

    assertEquals(originalFileContent, decryptedFileContent);

    Files.deleteIfExists(encryptedSpec);
    Files.deleteIfExists(decryptedSpec);

  }

  @Test
  @Order(160)
  void testOTPv2FileEncryption() throws Exception {

    String email = System.getProperty("xqsdk-user.email");

    final Path sourceSpec = Paths.get(String.format("src/test/resources/%s.txt", "utf-8-sampler"));
    final Path targetSpec = Paths.get(String.format("src/test/resources/%s.txt.xqf", "utf-8-sampler"));

    final String user = email;
    final List recipients = List.of(email);
    final Integer expiration = 5;

    Path encryptedFilePath = FileEncrypt.with(sdk, AlgorithmEnum.OTPv2)
            .supplyAsync(Optional.of(Map.of(FileEncrypt.USER, user,
                    FileEncrypt.RECIPIENTS, recipients,
                    FileEncrypt.MESSAGE_EXPIRATION_HOURS, expiration,
                    FileEncrypt.SOURCE_FILE_PATH, sourceSpec,
                    FileEncrypt.TARGET_FILE_PATH, targetSpec)))
            .thenApply((serverResponse) -> {

              switch (serverResponse.status) {
                case Ok: {
                  var encryptFilePathResult = (Path) serverResponse.payload.get(ServerResponse.DATA);
                  return encryptFilePathResult;
                }
                case Error: {
                  logger.warning(String.format("failed , reason: %s", serverResponse.moreInfo()));
                  fail();
                  return null;
                }
                default:
                  fail();
                  throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
              }

            }).get();

    assertTrue(encryptedFilePath != null);

  }

  @Test
  @Order(170)
  void testOTPv2FileDecryption() throws Exception {

    final Path originalSpec = Paths.get(String.format("src/test/resources/%s.ctrl", "utf-8-sampler"));
    final Path sourceSpec = Paths.get(String.format("src/test/resources/%s.txt.xqf", "utf-8-sampler"));
    final Path targetSpec = Paths.get(String.format("src/test/resources/%s.txt", "utf-8-sampler"));

    Path resultSpec = FileDecrypt.with(sdk, AlgorithmEnum.OTPv2)
            .supplyAsync(Optional.of(Map.of(FileDecrypt.SOURCE_FILE_PATH, sourceSpec, FileDecrypt.TARGET_FILE_PATH, targetSpec)))
            .thenApply((serverResponse) -> {
              switch (serverResponse.status) {
                case Ok: {
                  var decryptFilePath = (Path) serverResponse.payload.get(ServerResponse.DATA);
                  logger.info("Decrypt Filepath: " + decryptFilePath);
                  return decryptFilePath;
                }
                case Error: {
                  logger.warning(String.format("failed , reason: %s", serverResponse.moreInfo()));
                  fail();
                  return null;
                }
                default:
                  fail();
                  throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
              }

            }).get();

    String originalFileContent = Files.readString(originalSpec);
    String decryptedFileContent = Files.readString(resultSpec);

    logger.severe(String.format("  Original File Content: %s", originalFileContent.replaceAll("\n", " ")));
    logger.severe(String.format(" Decrypted File Content: %s", decryptedFileContent.replaceAll("\n", " ")));

    assertEquals(originalFileContent, decryptedFileContent);

    //Files.deleteIfExists(sourceSpec);
    // Files.deleteIfExists(targetSpec);

  }

  @Test
  @Order(180)
  void testRevokeUserAccess() throws Exception {

    List recipients = List.of(System.getProperty("xqsdk-recipients.email"));

    String locatorToken = map.get(Decrypt.LOCATOR_TOKEN);

    assert locatorToken != null : " locatorToken is null";

    String noContent = RevokeUserAccess
            .with(sdk)
            .supplyAsync(Optional.of(Map.of(
                    RevokeUserAccess.LOCATOR_TOKEN, locatorToken,
                    RevokeUserAccess.RECIPIENTS, recipients
            )))
            .thenApply(
                    (ServerResponse serverResponse) -> {
                      switch (serverResponse.status) {
                        case Ok: {
                          String data = (String) serverResponse.payload.get(ServerResponse.DATA);
                          logger.info("Status: " + Ok.name());
                          logger.info("Data: " + data);
                          return data;
                        }
                        case Error: {
                          logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                          return "";
                        }
                        default:
                          throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
                      }
                    }
            ).get();

    assertEquals("No Content", noContent);

  }

  @Test
  @Order(190)
  void testGrantUserAccess() throws Exception {

    List recipients = List.of(System.getProperty("xqsdk-recipients.email"));

    String locatorToken = map.get(Decrypt.LOCATOR_TOKEN);

    assert locatorToken != null : " locatorToken is null";

    String noContent = GrantKeyAccess
            .with(sdk)
            .supplyAsync(Optional.of(Map.of(
                    GrantKeyAccess.LOCATOR_TOKEN, locatorToken,
                    GrantKeyAccess.RECIPIENTS, recipients
            )))
            .thenApply(
                    (ServerResponse serverResponse) -> {
                      switch (serverResponse.status) {
                        case Ok: {
                          String data = (String) serverResponse.payload.get(ServerResponse.DATA);
                          logger.info("Status: " + Ok.name());
                          logger.info("Data: " + data);
                          return data;
                        }
                        case Error: {
                          logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                          return "";
                        }
                        default:
                          throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", serverResponse.status));
                      }
                    }
            ).get();

    assertEquals("No Content", noContent);

  }

  @Test
  @Order(200)
  void testAuthorizeAlias() throws Exception {

    String email = System.getProperty("xqsdk-user.email");

    Map<String, Object> payload =
            Map.of(Authorize.USER, email,
                    Authorize.FIRST_NAME, "User",
                    Authorize.LAST_NAME, "XQMessage");

    String accessToken = AuthorizeAlias
            .with(sdk)
            .supplyAsync(Optional.of(payload))
            .thenApply(
                    (ServerResponse authorizationResponse) -> {
                      switch (authorizationResponse.status) {
                        case Ok: {
                          return (String) authorizationResponse.payload.get(ServerResponse.DATA);
                        }
                        default: {
                          logger.warning(String.format("`testAuthorizeAlias` failed , reason: %s", authorizationResponse.moreInfo()));
                          return null;
                        }
                      }
                    }).get();

    assertTrue(accessToken != null && !"".equals(accessToken.trim()));
    assertNotEquals("", accessToken);

  }

  @Test
  @Order(210)
  void testCheckApiKey() throws Exception {

    List<String> scopes = CheckApiKey.with(sdk)
            .supplyAsync(Optional.of((Map.of(CheckApiKey.API_KEY, sdk.APPLICATION_KEY))))
            .thenApply(
                    (ServerResponse apiKeyCheckResponse) -> {
                      switch (apiKeyCheckResponse.status) {
                        case Ok: {
                          var payload = apiKeyCheckResponse.payload;
                          return (List<String>) payload.get(CheckApiKey.SCOPES);
                        }
                        default: {
                          logger.warning(String.format("`testAuthorizeAlias` failed , reason: %s", apiKeyCheckResponse.moreInfo()));
                          return null;
                        }
                      }
                    }).get();

    assertTrue(scopes != null && scopes.size() > 0);

  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  //////////                             UTILITY METHODS                                  //////////
  //////////////////////////////////////////////////////////////////////////////////////////////////

  private String getPinFromTerminalInput() {
    return readOneLineFromTerminalInput("Code", "Please enter the pin number");
  }

  /**
   * In order to accept user input from the IntelliJ Console window during junit tests <br>
   * please go to the toolbar and select `Help/Edit Custom VM Options`.<br>
   * The `idea.vmoptions` file will be opened.<br>
   * Here add the line: -Deditable.java.test.console=true<br>
   *
   * @return String
   */
  private String readOneLineFromTerminalInput(String label, String instruction) {

    Scanner in = new Scanner(System.in);
    logger.info(String.format("%s then press [Enter]\n\n", instruction));
    String text = in.nextLine();
    logger.info(String.format("%s: %s", label, text));

    return text;

  }

  private CompletableFuture<String> authorize(XQSDK sdk, Map<String, Object> payload) {
    return Authorize
            .with(sdk)
            .supplyAsync(Optional.of(payload))
            .thenCompose(
                    (ServerResponse authorizationResponse) -> {
                      switch (authorizationResponse.status) {
                        case Ok: {
                          String tempToken = (String) authorizationResponse.payload.get(ServerResponse.DATA);
                          final String pin = getPinFromTerminalInput();
                          logger.info("Temporary Token: " + tempToken);
                          assertTrue(tempToken != null && !"".equals(tempToken.trim()));
                          return CodeValidator
                                  .with(sdk)
                                  .supplyAsync(Optional.of(Map.of(CodeValidator.PIN, pin)));
                        }
                        case Error: {
                          logger.warning(String.format("`testUserAccessRequest` failed at authorization stage, reason: %s", authorizationResponse.moreInfo()));
                          fail();
                          return CompletableFuture.completedFuture(new ServerResponse(CallStatus.Error, Reasons.EncryptionFailed, authorizationResponse.moreInfo()));
                        }
                        default:
                          throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", authorizationResponse.status));
                      }
                    }
            ).thenApply(
                    (ServerResponse exchangeResponse) -> {
                      switch (exchangeResponse.status) {
                        case Ok: {
                          String accessToken = (String) exchangeResponse.payload.get(ServerResponse.DATA);
                          logger.info("Access Token: " + accessToken);
                          assertTrue(accessToken != null && !"".equals(accessToken.trim()));
                          return accessToken;
                        }
                        case Error: {
                          logger.warning(String.format("`testEncryption` failed at access code exchange stage, reason: %s", exchangeResponse.moreInfo()));
                          fail();
                          return null;
                        }
                        default:
                          throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", exchangeResponse.status));

                      }
                    }
            );
  }

  static <T> Logger getLogger(Class<T> clazz) {
    try {
      LogManager.getLogManager().readConfiguration(clazz.getClassLoader().getResourceAsStream("test-logging.properties"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Logger.getLogger(clazz.getName());
  }

}


