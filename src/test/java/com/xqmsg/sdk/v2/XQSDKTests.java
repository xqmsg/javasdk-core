package com.xqmsg.sdk.v2;


import com.xqmsg.sdk.v2.services.CheckKeyExpiration;
import com.xqmsg.sdk.v2.services.CreateDelegateAccessToken;
import com.xqmsg.sdk.v2.services.Decrypt;
import com.xqmsg.sdk.v2.services.DeleteAccessCredentials;
import com.xqmsg.sdk.v2.services.DeleteUser;
import com.xqmsg.sdk.v2.services.Encrypt;
import com.xqmsg.sdk.v2.services.ExchangeForAccessToken;
import com.xqmsg.sdk.v2.services.FileDecrypt;
import com.xqmsg.sdk.v2.services.FileEncrypt;
import com.xqmsg.sdk.v2.services.GetUserInfo;
import com.xqmsg.sdk.v2.services.GetUserSettings;
import com.xqmsg.sdk.v2.services.MergeTokens;
import com.xqmsg.sdk.v2.services.RequestAccess;
import com.xqmsg.sdk.v2.services.RetrieveKey;
import com.xqmsg.sdk.v2.services.RevokeKeyAccess;
import com.xqmsg.sdk.v2.services.UpdateUserSettings;
import com.xqmsg.sdk.v2.services.ValidateAccessRequest;
import com.xqmsg.sdk.v2.utils.DateTimeFormats;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static com.xqmsg.sdk.v2.CallStatus.Ok;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class XQSDKTests {

  private static final Logger logger = getLogger(XQSDKTests.class);

  @TempDir
  static Path sharedTempDir;
  static private final boolean TEMPORARY = true;
  static private final boolean PERMANENT = false;

  static XQSDK sdk ;

  @BeforeAll
  static void init() throws MalformedURLException {

    sdk = new XQSDK();
    if(Boolean.parseBoolean(System.getProperty("clear-cache", "true"))) {
      clearCache();
    }else{
      String email = System.getProperty("xqsdk-user.email");
      String accessToken = cached(email, "accessToken", PERMANENT);
      if (accessToken != null) {
        sdk.addAccessToken(email, accessToken);
      }else{
        fail(String.format(" %s not in cache. Try clearing cache, i.e. -Dclear-cache=true", email));
      }
    }

  }


  /**
   * This test  requires user input. <br> You may have to configure IntelliJ to do so. <br>
   * Please see {@link #readOneLineFromTerminalInput} for more information.<br>
   *
   */
  @Test
  @Order(1)
  void testRequestAccess() throws Exception {

    String email = System.getProperty("xqsdk-user.email");

    if (sdk.getAccessToken(email) == null) {

      Map<String, Object> payload =
              Map.of(RequestAccess.USER, email,
                      RequestAccess.FIRST_NAME, "User",
                      RequestAccess.LAST_NAME, "XQMessage",
                      RequestAccess.NEWSLETTER, true,
                      RequestAccess.NOTIFICATIONS,
                      Notifications.TUTORIALS.ordinal());

      String accessToken = requestAccess(sdk, payload).get();
      assertNotEquals("", accessToken);

      cache(email, "accessToken", accessToken, PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }

  }

  @Test
  @Order(2)
  void testGetUserInfo() throws Exception {

    String email = System.getProperty("xqsdk-user.email");
     final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }

    GetUserInfo.with(sdk, accessToken)
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
  @Order(3)
  void testGetUserSettings() throws Exception {

    String email = System.getProperty("xqsdk-user.email");
     final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }

    GetUserSettings.with(sdk, accessToken)
            .supplyAsync(Optional.empty())
            .thenAccept((serverResponse) -> {

              switch (serverResponse.status) {
                case Ok: {

                  boolean newsletter = (Boolean) serverResponse.payload.get(GetUserSettings.NEWSLETTER);
                  Long notifications = (Long) serverResponse.payload.get(GetUserSettings.NOTIFICATIONS);

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
  @Order(4)
  void testUpdateUserSettings() throws Exception {

    String email = System.getProperty("xqsdk-user.email");
     final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }

    Map<String, Object> payload = Map.of(GetUserSettings.NEWSLETTER, false, GetUserSettings.NOTIFICATIONS, 2);

    UpdateUserSettings.with(sdk, accessToken)
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
  @Order(5)
  void testEncrypt() throws Exception {

    String email = System.getProperty("xqsdk-user.email");
     final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }

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

    cache(email, "originalText", text, PERMANENT);

    String encryptionResult = Encrypt
            .with(sdk, AlgorithmEnum.OTPv2, accessToken)
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

                          cache(email, "locatorToken", locatorToken, PERMANENT);
                          cache(email, "encryptedText", encryptedText, PERMANENT);
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
  @Order(6)
  void testDecrypt() throws ExecutionException, InterruptedException {

    String email = System.getProperty("xqsdk-user.email");
     final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }

    String locatorToken = cached(email, Decrypt.LOCATOR_TOKEN, PERMANENT);
    String encryptedText = cached(email, Decrypt.ENCRYPTED_TEXT, PERMANENT);

    assert locatorToken != null : "locator token cannot be null";
    assert encryptedText != null : "encrypted text cannot be null";

    String decryptionResult = null;

      decryptionResult = Decrypt
              .with(sdk, AlgorithmEnum.OTPv2, accessToken)
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

    assertEquals(cached(email, "originalText", PERMANENT), decryptionResult);


  }

  @Test
  @Order(7)
  void testKeyRetrieval() throws Exception {

    String email = System.getProperty("xqsdk-user.email");
     final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }

    String locatorToken = cached(email, Decrypt.LOCATOR_TOKEN, PERMANENT);

    assert locatorToken != null : " locatorToken is null";

    String key = RetrieveKey
            .with(sdk, accessToken)
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
  @Order(8)
  void testMergeTokens() throws Exception {

    String email1 = System.getProperty("xqsdk-user.email");
    String accessToken1 = sdk.getAccessToken(email1);
    if (accessToken1 == null) {
      accessToken1 = cached(email1, "accessToken", PERMANENT);
      sdk.addAccessToken(email1, accessToken1);
    }

    String email2 = System.getProperty("xqsdk-user2.email");
    String accessToken2 = sdk.getAccessToken(email2);
    if (accessToken2 == null) {
      accessToken2 = cached(email2, "accessToken", PERMANENT);
      if (accessToken2 == null) {
        Map<String, Object> payload2 =
                Map.of(RequestAccess.USER, email2,
                        RequestAccess.FIRST_NAME, "User2",
                        RequestAccess.LAST_NAME, "XQMessage",
                        RequestAccess.NEWSLETTER, false,
                        RequestAccess.NOTIFICATIONS,
                        Notifications.NONE.ordinal());
        accessToken2 = requestAccess(sdk, payload2).get();
        cache(email2, "accessToken", accessToken2, PERMANENT);
      }
      sdk.addAccessToken(email2, accessToken2);
    }

    String combinedAccessToken = MergeTokens
            .with(sdk, accessToken1)
            .supplyAsync(Optional.of(Map.of(MergeTokens.TOKENS, List.of(accessToken2))))
            .thenApply(
                    (ServerResponse serverResponse) -> {
                      switch (serverResponse.status) {
                        case Ok: {
                          String combined = (String) serverResponse.payload.get(MergeTokens.MERGED_TOKEN);
                          Long mergeCount = (Long) serverResponse.payload.get(MergeTokens.MERGE_COUNT);
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
  @Order(9)
  void testCheckKeyExpiration() throws Exception {

    String email = System.getProperty("xqsdk-user.email");

     final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }

    String locatorToken = cached(email, Decrypt.LOCATOR_TOKEN, PERMANENT);

    assert locatorToken != null : " locatorToken is null";

    CheckKeyExpiration
            .with(sdk, accessToken)
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
  @Order(10)
  void testCreateDelegateAccessToken() throws Exception {

    String email = System.getProperty("xqsdk-user.email");

     final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }

    String delegateAccessToken =
            CreateDelegateAccessToken
                    .with(sdk, accessToken)
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
   * If annotated with {@link Disabled}, <br>
   * see {@link #testDeleteUser()} for more information <br>
   */
  @Test
  @Order(11)
  @Disabled
  void testDeleteAccessCredentials() throws Exception {

    String email = System.getProperty("xqsdk-user.email");

     final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }

    String noContent = DeleteAccessCredentials
            .with(sdk, accessToken)
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
  @Order(12)
  void testRevokeKeyAccess() throws Exception {

    String email = System.getProperty("xqsdk-user.email");
     final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }

    String locatorToken = cached(email, Decrypt.LOCATOR_TOKEN, PERMANENT);

    assert locatorToken != null : " locatorToken is null";

    String noContent = RevokeKeyAccess
            .with(sdk, accessToken)
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
   * {@link #testDeleteAccessCredentials()} <br>
   * is annotated with {@link Disabled}
   */
  @Test
  @Order(13)
  @Disabled
  void testDeleteUser() throws Exception {

    String email = System.getProperty("xqsdk-user.email");
     final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }

    DeleteUser.with(sdk, accessToken)
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
  @Order(14)
  @Disabled
  void testAESFileEncryption() throws Exception{

    String email = System.getProperty("xqsdk-user.email");
    final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }

    final Path sourceSpec = Paths.get(String.format("src/test/resources/%s.txt", "utf-8-sampler"));
    final Path targetSpec = Paths.get(String.format("src/test/resources/%s.txt.xqf", "utf-8-sampler"));
    final String user = email;
    final String recipients = email;
    final Integer expiration = 5;

    Path encryptedFilePath = FileEncrypt.with(sdk, AlgorithmEnum.AES, accessToken)
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

      assertTrue(encryptedFilePath!=null);


  }

  @Test
  @Order(15)
  @Disabled
  void testAESFileDecryption() throws Exception{

    String email = System.getProperty("xqsdk-user.email");
    final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }
    final Path originalSpec = Paths.get(String.format("src/test/resources/%s.ctrl", "utf-8-sampler"));
    final Path encryptedSpec = Paths.get(String.format("src/test/resources/%s.txt", "utf-8-sampler"));
    final Path decryptedSpec = Paths.get(String.format("src/test/resources/%s.txt.xqf", "utf-8-sampler"));

    Path resultSpec = FileDecrypt.with(sdk, AlgorithmEnum.AES, accessToken)
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
  @Order(16)
  void testOTPv2FileEncryption() throws Exception{

    String email = System.getProperty("xqsdk-user.email");
    final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }

    final Path sourceSpec = Paths.get(String.format("src/test/resources/%s.txt", "utf-8-sampler"));
    final Path targetSpec = Paths.get(String.format("src/test/resources/%s.txt.xqf", "utf-8-sampler"));

    final String user = email;
    final List recipients = List.of(email);
    final Integer expiration = 5;

    Path encryptedFilePath = FileEncrypt.with(sdk, AlgorithmEnum.OTPv2, accessToken)
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

    assertTrue(encryptedFilePath!=null);


  }

  @Test
  @Order(17)
  void testOTPv2FileDecryption() throws Exception{

    String email = System.getProperty("xqsdk-user.email");
    final String accessToken;
    if ( sdk.getAccessToken(email) == null) {
      accessToken = cached(email, "accessToken", PERMANENT);
      sdk.addAccessToken(email, accessToken);
    }else {
      accessToken = sdk.getAccessToken(email);
    }

    final Path originalSpec = Paths.get(String.format("src/test/resources/%s.ctrl", "utf-8-sampler"));
    final Path sourceSpec = Paths.get(String.format("src/test/resources/%s.txt.xqf", "utf-8-sampler"));
    final Path targetSpec = Paths.get(String.format("src/test/resources/%s.txt", "utf-8-sampler"));

    Path resultSpec = FileDecrypt.with(sdk, AlgorithmEnum.OTPv2, accessToken)
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
  @Order(18)
  @Disabled
  void testQuantumKeysAreUnique() throws MalformedURLException {

    XQSDK sdk = new XQSDK();

    List<String> keys = new ArrayList<>();

    Optional<String> serviceName = Optional.empty();
    Map<String, Object> queryParams = Map.of("ks", "256");

    IntStream.range(0, 25)
            .forEach(count -> {
              ServerResponse serverResponse = sdk.call(sdk.KEY_SERVER_URL, serviceName, CallMethod.Get, Optional.empty(), Optional.of(queryParams));
              String key = (String) serverResponse.payload.get(ServerResponse.DATA);
              keys.add(key);
            });

    assertEquals(keys.size(), keys.stream().distinct().count());

  }


  @Test
  @Order(19)
  @Disabled
  @DisplayName("XQSDK should load test properties specified in the config file under /test/resources")
  void testLoadPropertiesFromFile() {

    assertEquals("a1a03794-33e0-4e01-8030-36052e1a55ed-4eb8e457-0f15-42f4-b398-4ce038687cfb", sdk.APPLICATION_KEY);

    assertEquals("https://stg-subscription.xqmsg.net/v2", sdk.SUBSCRIPTION_SERVER_URL.toString());
    assertEquals("https://stg-validation.xqmsg.net/v2", sdk.VALIDATION_SERVER_URL.toString());
    assertEquals("https://stg-quantum.xqmsg.net", sdk.KEY_SERVER_URL.toString());

  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //////////                             UTILITY METHODS                                  //////////
  //////////////////////////////////////////////////////////////////////////////////////////////////

  private String getPinFromTerminalInput() {
    return readOneLineFromTerminalInput("Code", "Please enter the pin number");
  }

  private String getRecipientsFromTerminalInput() {
    String recipients = readOneLineFromTerminalInput("Recipients", "Please enter one of more email address");
    return Arrays.toString(recipients.trim().split("[\\s,]+")).replaceAll("[\\[\\]]", "");
  }

  private String getMessageFromTerminalInput() {
    return readOneLineFromTerminalInput("Message", "Please enter the message you want to encrypt");
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


  private static void clearCache() {
    try {
      Path dirPath = Paths.get(String.format("/Users/%s/Documents/%s", System.getenv().get("USER"), "xqsdk-tests"));
      if (Files.exists(dirPath)) {
        Files.list(dirPath).forEach(p -> {
          try {
            Files.delete(p);
          } catch (IOException i) {
          }
        });
        Files.delete(dirPath);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * writes conrtent to a file. utility to store user ids, access tokens or locator tokens and son on, between tests.
   *
   * @param email           part 1 of the cache key (file name)
   * @param cacheKey        part2 of the cache key (file name)
   * @param content         the data to be cached
   * @param isSharedTempDir if set to {@link #TEMPORARY} the item will be destroyed after the test suite ran
   *                        if set to {@link #PERMANENT} the item will remain indefinitely under
   *                        /Users/<your-user-id>/Documents/xqsdk-tests/
   */
  private static void cache(String email, String cacheKey, String content, boolean isSharedTempDir) {
    try {
      String cacheName = String.format(".%s_%s", cacheKey, email.replaceAll("[@.]", "-"));
      if (isSharedTempDir) {
        Files.write(sharedTempDir.resolve(cacheName), content.getBytes(StandardCharsets.UTF_8));
      } else {

        Path dirPath = Paths.get(String.format("/Users/%s/Documents/%s", System.getenv().get("USER"), "xqsdk-tests"));
        Path filePath = dirPath.resolve(cacheName);
        boolean dirExists = Files.exists(dirPath);
        if (dirExists) {
          //file
          Files.deleteIfExists(filePath);
        } else {
          Files.createDirectory(dirPath);
        }
        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }



  private static String cached(String email, String cacheKey, boolean isPermantent) {
    try {
      String cacheName = String.format(".%s_%s", cacheKey, email.replaceAll("[@.]", "-"));
      if (isPermantent) {
        return Files.readString(sharedTempDir.resolve(cacheName));
      } else {
        Path dirPath = Paths.get(String.format("/Users/%s/Documents/%s", System.getenv().get("USER"), "xqsdk-tests"));
        Path filePath = dirPath.resolve(cacheName);
        if (Files.exists(filePath)) {
          return Files.readString(filePath);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    return null;
  }

  private CompletableFuture<String> requestAccess(XQSDK sdk, Map<String, Object> payload) {
    return RequestAccess
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
                          return validateAccessRequest(sdk, pin, tempToken);
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
            ).thenCompose(
                    (ServerResponse validatedResponse) -> {
                      switch (validatedResponse.status) {
                        case Ok: {
                          String validatedTempToken = (String) validatedResponse.payload.get(ServerResponse.DATA);
                          return exchangeForAccessToken(sdk, validatedTempToken);
                        }
                        case Error: {
                          logger.warning(String.format("`testUserAccessRequest` failed at validation stage, reason: %s", validatedResponse.moreInfo()));
                          fail();
                          return CompletableFuture.completedFuture("");
                        }
                        default:
                          throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", validatedResponse.status));
                      }
                    }
            );
  }

  private CompletableFuture<ServerResponse> validateAccessRequest(XQSDK sdk, String pin, String tempToken) {
    return ValidateAccessRequest
            .with(sdk, tempToken)
            .supplyAsync(Optional.of(Map.of(ValidateAccessRequest.PIN, pin)))
            .thenCompose(
                    (ServerResponse pinValidationResponse) -> {
                      switch (pinValidationResponse.status) {
                        case Ok: {
                          String data = (String) pinValidationResponse.payload.get(ServerResponse.DATA);
                          logger.info("Validation Response: " + data);
                          assertEquals("No Content", data);
                          return CompletableFuture.completedFuture(new ServerResponse(Ok, Map.of(ServerResponse.DATA, tempToken)));
                        }
                        case Error: {
                          logger.warning(String.format("`testEncryption` failed at pin validation stage, reason: %s", pinValidationResponse.moreInfo()));
                          fail();
                          return CompletableFuture.completedFuture(new ServerResponse(CallStatus.Error, Reasons.EncryptionFailed, pinValidationResponse.moreInfo()));
                        }
                        default:
                          throw new RuntimeException(String.format("switch logic for case: `%s` does not exist", pinValidationResponse.status));

                      }
                    }

            );
  }

  private CompletableFuture<String> exchangeForAccessToken(XQSDK sdk, String tempToken) {
    return ExchangeForAccessToken
            .with(sdk, tempToken)
            .supplyAsync(Optional.empty())
            .thenApply(
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


