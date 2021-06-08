



## javasdk-core

A Java Implementation of XQ Message SDK, V.2



## Table of contents

- [Installation](#installation)
- [Generating API Keys](#api-keys)
- [Basic Usage](#basic-usage)
- [Initializing the XQ java SDK](#initializing-the-sdk)
- [Encrypt](#encrypting-a-message)
- [Decrypt](#decrypting-a-message)
- [FileEncrypt](#encrypting-a-file)
- [FileDecrypt](#decrypting-a-file)
- [Authorize](#authorization)
- [CodeValidator](#code-validator)
- [RevokeKeyAccess](#revoking-key-access)
- [GrantUserAccess](#granting-and-revoking-user-access)
- [AuthorizeAlias](#connect-to-an-alias-account)
- [Dashboard Mangement](#dashboard-management)
- [Manage Keys Yourself](#manual-key-management)
---


## Installation


#### API Keys

In order to utilize the XQ SDK and interact with XQ servers you will need both the **`General`** and **`Dashboard`** API keys. To generate these keys, follow these steps:

1. Go to your [XQ management portal](https://manage.xqmsg.com/applications).
2. Select or create an application.
3. Create a **`General`** key for the XQ framework API.
4. Create a **`Dashboard`** key for the XQ dashboard API.

---

  _Once a key has been obtained from XQ Message it must be inserted it into these files:_<br>
  [config.properties](./src/main/resources/config.properties)<br>
  [dev-config.properties](./src/main/resources/dev-config.properties)<br>
  [test-config.properties](./src/test/resources/test-config.properties)<br>

  _The config properties for the API keys are called_

 1. com.xq-msg.sdk.v2.xq-api-key
 2. com.xq-msg.sdk.v2.dashboard-api-key

#### JUnit Tests

   _Debug/RunConfig:_

   Test Kind:   `Class`
     
   Class: `com.xqmsg.sdk.v2.XQSDKTests`
    
   VM Options:

   * `-Dmode=test` (loads: test/resources/test-config.properties)

   * `-Dclear-cache=false|true` (re-use access tokens from previous run or create new ones each time) <br>
   * `-Dxqsdk-user.email=username@domain-name.com` (validation pins will be sent to this email address)<br>
   * `-Dxqsdk-user2.email=username@domain-name.com`(an additional email for tests involving `merge tokens`)<br>
   * `-Dxqsdk-recipients.email=username@domain-name.com` (an additional email, needed for tests involving `recipients`)<br>

---

## Basic Usage


**_Note: You only need to generate one SDK instance for use across your application._**

#### Encrypting a message ([Encrypt.java](./src/main/java/com/xqmsg/sdk/v2/services/Encrypt.java))
The text to be encrypted should be submitted along with the email addresses of the intended recipients, as well as the amount of time that the message should be available.

```java

    String userEmail = "me@email.com";

    List recipients = List.of("jane@email.com", "jack@email.com");

    final String messageToEncrypt =
            "The first stanza of Pushkin's Bronze Horseman (Russian):\n" +
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


    Encrypt   
       .with(sdk, AlgorithmEnum.OTPv2) // Either "OTPv2" or "AES"
       .supplyAsync(Optional.of(Map.of(Encrypt.USER, userEmail,
               Encrypt.TEXT, messageToEncrypt,
               Encrypt.RECIPIENTS, recipients,
               Encrypt.MESSAGE_EXPIRATION_HOURS, 5)))
       .thenApply(
           (ServerResponse response) -> {
               switch (response.status) {
                   case Ok: {
                       String locatorToken = (String) response.payload.get(Encrypt.LOCATOR_KEY);
                       String encryptedText = (String) response.payload.get(Encrypt.ENCRYPTED_TEXT);
                       // Store the locator key and encryptted text somehwere
                       return response;
                   }
                   default: {
                        //Something went wrong
                       logger.severe(String.format("failed , reason: %s", response.moreInfo()));
                       return response;
                   }
               }

           }
       ).get();

```

#### Decrypting a message ([Decrypt.java](./src/main/java/com/xqmsg/sdk/v2/services/Decrypt.java))

To decrypt a message, the encrypted payload must be provided, along with the locator token received from XQ during encryption. The authenticated user must be one of the recipients that the message was originally sent to ( or the sender themselves).

```java

      String locatorToken = "";// obtained from a preceding Encrypt call
      String encryptedText = "";// obtained from a preceding Encrypt call

      ServerResponse decryptResponse = Decrypt
              .with(sdk, AlgorithmEnum.OTPv2)
              .supplyAsync(Optional.of(Map.of(Decrypt.LOCATOR_TOKEN, locatorToken, Decrypt.ENCRYPTED_TEXT, encryptedText)))
              .thenApply(
                      (ServerResponse response) -> {
                          switch (response.status) {
                              case Ok: {
                                  String decryptedText = (String) response.payload.get(ServerResponse.DATA);
                                  //Do something with the decrypted text 
                                  return response;
                              }
                              default: {
                                  //Something went wrong
                                  logger.severe(String.format("failed , reason: %s", response.moreInfo()));
                                  return response;
                              }
                          }

                      }).get();
     
```

#### Encrypting a file ([FileEncrypt.java](./src/main/java/com/xqmsg/sdk/v2/services/FileEncrypt.java))

Here, a `File` object containing the data for encryption must be provided. Like message encryption, a list of recipients who will be able to decrypt the file, as well as the amount of time before expiration must also be provided.

```java
    
   final Path sourceSpec = Paths.get("path/to/original/file/.txt", "my-original-file"));
      final Path targetSpec = Paths.get(String.format("path/to/encrypted/file/.txt.xqf", "my-encrypted-file"));

      String userEmail = "me@email.com";
      List recipients = List.of("jane@email.com", "jack@email.com");
      Integer expiration = 5;

      Path encryptedFilePath = FileEncrypt.with(sdk, AlgorithmEnum.OTPv2)
              .supplyAsync(Optional.of(Map.of(FileEncrypt.USER, userEmail,
                      FileEncrypt.RECIPIENTS, recipients,
                      FileEncrypt.MESSAGE_EXPIRATION_HOURS, expiration,
                      FileEncrypt.SOURCE_FILE_PATH, sourceSpec,
                      FileEncrypt.TARGET_FILE_PATH, targetSpec)))
              .thenApply((response) -> {
                  switch (response.status) {
                      case Ok: {
                          var encryptFilePath = (Path) response.payload.get(ServerResponse.DATA);
                          // Do something with the encrypted file
                          return encryptFilePath;
                      }
                      default: {
                          // Something went wrong
                          logger.warning(String.format("failed , reason: %s", response.moreInfo()));
                          return null;
                      }

                  }

              }).get();
  }
```

#### Decrypting a file ([FileDecrypt.java](./src/main/java/com/xqmsg/sdk/v2/services/FileDecrypt.java))

To decrypt a file, the URI to the XQ encrypted file must be provided. The user decrypting the file must be one of the recipients original specified ( or the sender ).

```java

 final Path sourceSpec = Paths.get(String.format("path/to/encrypted/file/.txt.xqf", "my-encrypted-file"));
 final Path targetSpec = Paths.get("path/to/decrypted/file/.txt", "my-decrypted-file"));

Map<String, Object> payload = Map.of(FileDecrypt.SOURCE_FILE_PATH, sourceSpec, FileDecrypt.TARGET_FILE_PATH, targetSpec);
     
      FileDecrypt.with(sdk, AlgorithmEnum.OTPv2)
              .supplyAsync(Optional.of(payload))
              .thenApply((response) -> {
                  switch (response.status) {
                      case Ok: {
                          var decryptFilePath = (Path) response.payload.get(ServerResponse.DATA);
                          logger.info("Decrypt Filepath: " + decryptFilePath);
                          return decryptFilePath;
                      }
                      default: {
                          logger.warning(String.format("failed , reason: %s", response.moreInfo()));
                          return null;
                      }
                  }
              }).get();
```

#### Authorization ([Authorize.java](./src/main/java/com/xqmsg/sdk/v2/services/Authorize.java))

Request a temporary access token (which is cached in the active user profile) for a particular email address.
If successful, the user will receive an email containing a PIN number and a validation link.


```java
   Map<String, Object> payload =
              Map.of( Authorize.USER, "me@email.com",
                      Authorize.FIRST_NAME, "John",
                      Authorize.LAST_NAME, "Doe");

      String accessToken = Authorize
              .with(sdk)
              .supplyAsync(Optional.of(payload))
              .thenApply(
                      (ServerResponse response) -> {
                          switch (response.status) {
                              case Ok: {
                                    // Success. A pre-authorization token is automatically be cached
                                    // If you like, you can also retrieve it directly
                                  return (String) response.payload.get(ServerResponse.DATA);
                              }
                              default: {
                                  logger.warning(String.format("failed , reason: %s", response.moreInfo()));
                                  return null;
                              }
                          }
                      }).get();

```

#### Code Validator ([CodeValidator.java](./src/main/java/com/xqmsg/sdk/v2/services/CodeValidator.java))

This service validates that the PIN received in the email is mapped to  same temporary access token that was previously cached.
If so, the temporary code will be exchanged for a permanent access token which can be used for subsequent activities. This access token is  stored in the user's active profile .

```java

       Map<String, Object> payload = 
           Map.of(CodeValidator.PIN, "the-pin-that-was-mailed-to-you");

      CodeValidator
              .with(sdk)
              .supplyAsync(Optional.of(payload))
              .thenApply(
                      (ServerResponse response) -> {
                          switch (response.status) {
                              case Ok: {
                                  // Success. The access token is  stored in the user's active profile
                                  // If you like, you can also retrieve it directly
                                  return (String) response.payload.get(ServerResponse.DATA);
                              }
                              default: {
                                  //something went wrong
                                 logger.warning(String.format("failed , reason: %s", response.moreInfo()));
                                  return null;
                              }
                          }

                      }).get();
```

Alternatively, if the user clicks on the link in the email, they can simply exchange their pre-authorization token for a valid access token by using <b>[ExchangeForAccessToken.java](./src/main/java/com/xqmsg/sdk/v2/services/ExchangeForAccessToken.java)</b> directly. <br>Note that the active user in the `sdk` should be the same as the one used to make the authorization call:

```java
  ExchangeForAccessToken
         .with(sdk)
         .supplyAsync(Optional.empty())
         .thenApply(
            (ServerResponse response) -> {
                switch (response.status) {
                    case Ok: {
                       // Success. A new access token is already stored in the user's active profile
                       // If you like, you can also retrieve it directly
                       return (String) response.payload.get(ServerResponse.DATA);
                    }
                    default: {
                      //something went wrong
                      return null;
                    }
                }

            }).get();
```

#### Revoking Key Access ([RevokeKeyAccess.java](./src/main/java/com/xqmsg/sdk/v2/services/RevokeKeyAccess.java))

Revokes a key using its token. Only the user who sent the message will be able to revoke it. If successful a 204 status is returned.

**Warning: This action is not reversible.**

```java
 RevokeKeyAccess
              .with(sdk)
              .supplyAsync(Optional.of(Map.of(Decrypt.LOCATOR_TOKEN, "message_locator_token")))
              .thenApply(
                (ServerResponse response) -> {
                    switch (response.status) {
                        case Ok: {
                            // Success. Key was revoked successfully.
                            String noContent = (String) response.payload.get(ServerResponse.DATA);
                            return noContent;
                        }
                        default: {
                            // Something went wrong...
                            return null;
                        }
                    }
                }).get();

```

#### Granting and Revoking User Access ([GrantUserAccess.java](./src/main/java/com/xqmsg/sdk/v2/services/GrantUserAccess.java))

There may be cases where additional users need to be granted access to a previously sent message, or access needs to be revoked. This can be achieved via **GrantUserAccess** and **RevokeUserAccess** respectively:

```java

GrantUserAccess.with(sdk)
         .supplyAsync(Optional.of(Map.of(
                 GrantUserAccess.RECIPIENTS, "john@email.com",
                 GrantUserAccess.LOCATOR_TOKEN, "message_locator_token"
                 )))
         .thenApply(
            (ServerResponse response) -> {
                switch (response.status) {
                    case Ok: {
                        // Success. John will now be able to read that message.
                        break;
                    }
                    default: {
                        // Something went wrong...
                        break;
                    }
                }

                return response;
            }).get();
```

####  Revoke access from particular users ([RevokeUserAccess.java](./src/main/java/com/xqmsg/sdk/v2/services/RevokeUserAccess.java))
```java

// Revoke access from particular users.
RevokeUserAccess.with(sdk)
      .supplyAsync(Optional.of(Map.of(
              RevokeUserAccess.RECIPIENTS, "jack@email.com",
              RevokeUserAccess.LOCATOR_TOKEN, "message_locator_token"
      )))
      .thenApply(
              (ServerResponse response) -> {
                  switch (response.status) {
                      case Ok: {
                          // Success - Jack will no longer be able to read that message.
                          break;
                      }
                      default: {
                          // Something went wrong...
                          break;
                      }
                  }

                  return response;
              }).get();
```

#### Connect to an alias account ([AuthorizeAlias.java](./src/main/java/com/xqmsg/sdk/v2/services/AuthorizeAlias.java))


After creation, a user can connect to an Alias account by using the **`AuthorizeAlias`** endpoint:

```java
 Map<String, Object> payload = Map.of(Authorize.USER, "an-alias-id");

      AuthorizeAlias
         .with(sdk)
         .supplyAsync(Optional.of(payload))
         .thenApply(
           (ServerResponse response) -> {
               switch (response.status) {
                   case Ok: {
                       // Success - The alias user was authorized. 
                       // The alias token is automatically stored as the active profile.
                       // If you like, you can also retrieve it directly
                       return (String) response.payload.get(ServerResponse.DATA);
                   }
                   default: {
                       return null;
                   }
               }
           }).get();

```

---
## Manual key management

A user has the option of only using XQ for its key management services alone. The necessary steps to do this are detailed below:


#### 1. Get quantum entropy ( Optional ) ([FetchQuantumEntropy.java](./src/main/java/com/xqmsg/sdk/v2/quantum/FetchQuantumEntropy.java))

XQ provides a quantum source that can be used to generate entropy for seeding their encryption key:

```java

 FetchQuantumEntropy.with(sdk)
        .supplyAsync(Optional.empty())
        .thenApply(
           (ServerResponse response) -> {
               switch (response.status) {
                   case Ok: {
                       String key = (String) response.payload.get(ServerResponse.DATA);
                        // Do something with the key
                       return key;
                   }
                   default: {
                       //Something went wrong
                       logger.warning(String.format("failed , reason: %s", response.moreInfo()));
                       return null;
                   }
               }
          }).get();

```

#### 2. Upload The Key ([UploadKey.java](./src/main/java/com/xqmsg/sdk/v2/services/UploadKey.java))

 The encryption key, authorized recipients, as well as any additional metadata is sent to the XQ subscription server. If successful, a signed and encrypted key packet, a.k.a locator token  returned to the user.


```java

Map<String, Object> payload = Map.of(
              UploadKey.KEY, "THE_ENCRYPTION_KEY",
              UploadKey.RECIPIENTS, List.of("jane@email.com, jack@email.com"),
              UploadKey.MESSAGE_EXPIRATION_HOURS, 24,
              UploadKey.DELETE_ON_RECEIPT, false
      );

      UploadKey.with(sdk)
              .supplyAsync(Optional.of(payload))
              .thenApply((ServerResponse response) -> {
                  switch (response.status) {
                      case Ok: {
                          String locatorToken = (String) response.payload.get(ServerResponse.DATA);
                          // The packet, a.k.a locator token, is used later on to retrieve the key.
                          break;
                      }
                      default: {
                          logger.warning(String.format("failed , reason: %s", response.moreInfo()));
                          break;
                      }
                  }
                  return response;
              }).get();
```

#### 3. Retrieve a key from server ([FetchKey.java](./src/main/java/com/xqmsg/sdk/v2/services/FetchKey.java))

Use the locator token associated with the respective message to retrieve the encryption key. 
Even with the locator key only users that were previously specified as  recipients can fetch this key.

```java
     Map<String, Object> payload = Map.of(FetchKey.LOCATOR_TOKEN, "KEY_LOCATOR_TOKEN");

      FetchKey.with(this.sdk)
        .supplyAsync(Optional.of(payload))
        .thenApply((ServerResponse response) -> {
            switch (response.status) {
                case Ok: {
                    String encryptionKey = (String) response.payload.get(ServerResponse.DATA);
                    // The received key can now be used to decrypt the original message.
                    return encryptionKey;
                }
                default: {
                    logger.warning(String.format("failed , reason: %s", response.moreInfo()));
                    return null;
                }
            }
        }).get();
```

---
## Dashboard Management
The SDK provides limited functionality for dashboard administration. In order to use any of the services listed in this section
a user must be signed into XQ with an authorized email account associated with the management portal.

- [DashboardLogin](#connecting-to-the-dashboard)
- [AddUserGroup](#managing-a-user-group)
- [AddContact](#using-an-external-id-based-contact-for-tracking)

#### Connecting to the Dashboard ([DashboardLogin.java](./src/main/java/com/xqmsg/sdk/v2/services/dashboard/DashboardLogin.java))

```java

   DashboardLogin.with(sdk)
        .supplyAsync(Optional.empty())
        .thenApply((ServerResponse response) -> {
            switch (response.status) {
                case Ok: {
                    // Success. New dashboard access token will be stored
                    // for the current profile.
                    String dashboardAccessToken = (String) response.payload.get(ServerResponse.DATA);
                    return dashboardAccessToken;
                }
                default: {
                    logger.warning(String.format("failed , reason: %s", response.moreInfo()));
                    return null;
                }
            }
        }).get();

```

#### Managing a user group ([AddUserGroup.java](./src/main/java/com/xqmsg/sdk/v2/services/dashboard/AddUserGroup.java))

Users may group a number of emails accounts under a single alias. Doing this makes it possible to add all of the associated email accounts to an outgoing message by adding that alias as a message recipient instead. Note that changing the group members does not affect the access rights of messages that have previously been sent.

```java

 AddUserGroup.with(sdk)
        .supplyAsync(Optional.of(payload))
        .thenApply((ServerResponse response) -> {
            switch (response.status) {
                case Ok: {
                    // Success. The new user group was created.
                    String groupId = (String) response.payload.get(AddUserGroup.ID);

                    // The new group email format is {groupId}@group.local
                    return groupId;
                }
                default: {
                    logger.warning(String.format("failed , reason: %s", response.moreInfo()));
                    return null;
                }
            }

        });
      
```

#### Using an external ID-based contact for tracking ([AddContact.java](./src/main/java/com/xqmsg/sdk/v2/services/dashboard/AddContact.java))

In situations where a user may want to associate an external account with an XQ account for the purposes of encryption and tracking , they can choose to create an account with an **Alias** role.

These type of accounts will allow user authorization using only an account ID. However, these accounts have similar restrictions to anonymous accounts: They will be incapable of account management, and also have no access to the dashboard. However - unlike basic anonymous accounts - they can be fully tracked in a dashboard portal.

```java
  Map<String, Object> payload = Map.of(AddContact.EMAIL, "john@email.com",
                                      AddContact.NOTIFICATIONS, Notifications.NONE,
                                      AddContact.ROLE, Roles.Alias.ordinal(),
                                      AddContact.TITLE, "Mr.",
                                      AddContact.FIRST_NAME, "John",
                                      AddContact.LAST_NAME, "Doe");
      AddContact.with(sdk)
        .supplyAsync(Optional.of(payload))
        .thenApply(
                (ServerResponse serverResponse) -> {
                    switch (serverResponse.status) {
                        case Ok: {
                            var contactId = serverResponse.payload.get(AddContact.ID);
                            return contactId;
                        }
                        default: {
                            logger.info(String.format("failed , reason: %s", serverResponse.moreInfo()));
                            return null;
                        }
                    }
                }
        ).get();

```
  ## _Cache_

  A basic disk backed cache implementation utilizing <a href="https://mapdb.org/">MapDB</a> which is used to store access tokens by email address

#####  [SimpleXQCache.java](./src/main/java/com/xqmsg/sdk/v2/caching/SimpleXQCache.java) 