



## javasdk-core

A Java Implementation of XQ Message SDK, V.2

## _API Key_ 

##### An API key is required for interaction with the XQ Message services.

  _Once a key has been obtained from XQ Message it must be inserted it into these files:_<br>
  [config.properties](./src/main/resources/config.properties)<br>
  [dev-config.properties](./src/main/resources/dev-config.properties)<br>
  [test-config.properties](./src/test/resources/test-config.properties)<br>
  _The config property is called_ `com.xq-msg.sdk.v2.api-key`

## _JUnit Tests_ 

   ##### Debug/RunConfig:

   Test Kind:   `Class`
     
   Class: `com.xqmsg.sdk.v2.XQSDKTests`
    
   VM Options:

   * `-Dmode=test` 

    loads: test/resources/test-config.properties

   * `-Dclear-cache=false|true`<r>
     _re-use access tokens from previous run or create new ones each time
   * `-Dxqsdk-user.email=username@domain-name.com` <br>
     _validation pins will be sent to this email address_<br>
   * `-Dxqsdk-user2.email=username2@domain-name.com`<br>
     _and additional email, needed for tests with `recipients`<br>
     Note: to mimic pins of different accounts while using the same email account it is recommended to add +... <br>
            after the email's username part, for example: john.doe+user2@example.com_<br>
   * `-Dxqsdk-recipients.email=<recipient-email>` <br>

    validation pins will all be sent to john.doe@example.com ignoring the `+user2` portion

------
## _Services_

​                                                                                             Pre-requisite: API Key 
​                                                                            (_see above for more API Key informaion_)

An Access Token is required for all secure interactions with the XQ Message services.
To aquire one depends on interactions with three services.

   #####  GetAccess Token:  [Authorize](./src/main/java/com/xqmsg/sdk/v2/services/Authorize.java) 1/2

    Request an access token for a particular email address.
    If successful, the user will receive a PIN via email.

| Initialization Argument Name | Type   | Value                   |
| :-------------------------: | ------ | ------------------------ | 
|         xqsdk         | XQSDK | |


| Request  Argument Name | Type    | Value              | Required | Description                                                  |
| ---------------------- | ------- | ------------------ | -------- | ------------------------------------------------------------ |
| user                   | String  | \<user-email>      | √        | email to be validated                                        |
| firstName              | String  | \<user-first-name> | x        | first name of the user.                                      |
| lastName               | String  | \<user-last-name>  | x        | last name of the user                                        |
| newsLetter             | boolean | true\|false        | x        | should the user receive a newsletter                         |
| notifications          | int     | [0...3]            | x        | 0: No Notifications <br/>1: Receive Usage Reports<br/>2: Receive Tutorials<br />3: Receive Both |

| Response Name | Type     | Value                          |
| :-----------: | -------- | ------------------------------ |
|       -       | *String* | *Validation PIN  (via e-mail)* |
|     data      | String   | <temporary-access-token>       |


   #####   GetAccess Token:   [CodeValidator](./src/main/java/com/xqmsg/sdk/v2/services/CodeValidator.java) 2/2

    Usig the PIN that was sent to a users email validate a temporary access token and
    exchange it for a real token used in all secured XQ Message interactions.

| Initialization Argument Name | Type   | Value                   | 
| :-------------------------: | ------ | ------------------------ |
|         xqsdk         | XQSDK | |


| Request Argument Name | Type | Value     | Required | Description                                                  |
| --------------------- | ---- | --------- | -------- | ------------------------------------------------------------ |
| pin                   | int  | \[0-9]{6} | √        | the access token validation pin <br/>number received via email |


| Response Name | Type | Value |
| ------------- | ---- | ----- |
| -             | -    | -     |



------
_ENCRYPT TEXT_

   #####  [Encrypt](./src/main/java/com/xqmsg/sdk/v2/services/Encrypt.java)

   For encryption supply a piece of textual data  along with  the author's email, one or more emails of  intended<br>   recipients and the  intended life-span of the message.


| Initialization Argument Name | Type          | Value          | 
| :--------------------------: | ------------- | -------------- | 
|         xqsdk         | XQSDK | |
|          algorithm           | AlgorithmEnum | <optv2\|aes>   | 



| Request  Argument Name | Type    | Value                  | Required | Description                                                  |
| ---------------------- | ------- | ---------------------- | -------- | ------------------------------------------------------------ | 
| user                   | String  | \<user-email>          | √        | The author's email                                           | 
| recipients             | List    | \<recipient-emails>    | √        | A list of recipients who are allowed to access the key.      | 
| expires                | int     | \<expiration-duration> | √        | The number of hours that this key will remain valid for. After this time, it will no longer be accessible. |     
| dor                    | boolean | true\|false            | x        | Delete on Read If this is set to true a recipient will only be able to read a message once. Defaults to false. |      


| Response Name      | Type   | Value            |
| ------------------ | ------ | ---------------- |
| data/ locatorKey   | String | <locator-key>    |
| data/encryptedText | String | <encrypted-text> |

------

_DECRYPT TEXT_

   #####  [Decrypt](./src/main/java/com/xqmsg/sdk/v2/services/Decrypt.java)

   For decryption supply a piece of textual data  along with  the locator key you received when encrypting


| Initialization Argument Name | Type          | Value          |
| :--------------------------: | ------------- | -------------- |
|         xqsdk         | XQSDK | |
|          algorithm           | AlgorithmEnum | <optv2\|aes>   |



| Request  Argument Name | Type   | Value            | Required | Description                                  | 
| ---------------------- | ------ | ---------------- | -------- | -------------------------------------------- | 
| locatorToken           | String | <locator-token>  | √        | The locator token needed to discover the key | 
| encryptedText          | String | <encrypted-text> | √        | the encrypted textual data                   | 


| Response Name | Type          | Value                  |
| ------------- | ------------- | ---------------------- |
| data          | DecryptResult | <decrypted-data-bytes> |


------

_ENCRYPT FILE_

   #####  [FileEncrypt](./src/main/java/com/xqmsg/sdk/v2/services/FileEncrypt.java)

   For file encryption supply the path to the unencrypted  source document as well as a <br>   path to the target document to contain the encrypted data,  along with the author's email, <br>   one or more emails of  intended  recipients and the life-span of the message.


| Initialization Argument Name | Type          | Value          | 
| :--------------------------: | ------------- | -------------- | 
|         xqsdk         | XQSDK | |
|          algorithm           | AlgorithmEnum | <optv2\|aes>   | 



| Request  Argument Name | Type    | Value                       | Required | Description                                                  | 
| ---------------------- | ------- | --------------------------- | -------- | ------------------------------------------------------------ | 
| user                   | String  | \<user-email>               | √        | The author's email                                           | 
| sourceFilePath         | Path    | \<path-to-unencrypted-file> | √        | Path to the document, which is  supposed to be encrypted     | 
| targetFilePath         | Path    | \<path-to-encrypted-file>   | √        | Path to the document, which supposed to be decrypted         | 
| recipients             | List    | \<recipient-emails>         | √        | A list of recipients who are allowed to access the key.      | 
| expires                | int     | \<expiration-duration>      | √        | The number of hours that this key will remain valid for. After this time, it will no longer be accessible. |      
| dor                    | boolean | true\|false                 | x        | Delete on Read If this is set to true a recipient will only be able to read a message once. Defaults to false. |      


| Response Name | Type                           | Value |
| ------------- | ------------------------------ | ----- |
| data          | Path  <path-to-encrypted-file> |       |

------

_DECRYPT FILE_

   #####  [FileDecrypt](./src/main/java/com/xqmsg/sdk/v2/services/FileDecrypt.java)

    For file decryption supply the path to the encrypted source document as well as a <br> path to the target document to contain the decryped data.


| Initialization Argument Name | Type          | Value          | 
| :--------------------------: | ------------- | -------------- | 
|         xqsdk         | XQSDK | |
|          algorithm           | AlgorithmEnum | <optv2\|aes>   | 



| Request  Argument Name | Type | Value                       | Required | Description                                              |
| ---------------------- | ---- | --------------------------- | -------- | -------------------------------------------------------- |
| sourceFilePath         | Path | \<path-to-encrypted-file>   | √        | Path to the document, which supposed to be decrypted     |
| targetFilePath         | Path | \<path-to-unencrypted-file> | √        | Path to the document, which is  supposed to be encrypted |



| Response Name | Type | Value                    |
| ------------- | ---- | ------------------------ |
| data          | Path | <path-to-decrypted-file> |


------

   #####  [CheckKeyExpiration](./src/main/java/com/xqmsg/sdk/v2/services/CheckKeyExpiration.java)

This service is used to check whether a particular key is expired or not without actually fetching it. 


| Initialization Argument Name | Type   | Value          | 
| :--------------------------: | ------ | -------------- | 
|         xqsdk         | XQSDK | |


| Request  Argument Name | Type   | Value                        | Required | Description                                                  |
| ---------------------- | ------ | ---------------------------- | -------- | ------------------------------------------------------------ |
| locatorToken           | String | \<url-encoded-locator token> | √        | A URL encoded version of the key locator token.<br>It is  is needed for key discovery. |


| Response  Name | Response  Type | Response  Value | Description                                                  |
| -------------- | -------------- | --------------- | ------------------------------------------------------------ |
| expiresOn      | long           | \>=0            | The number of seconds before this token expires.<br> If the token is already expired, this will be zero |


------

   #####  [AuthorizeDelegate](./src/main/java/com/xqmsg/sdk/v2/services/AuthorizeDelegate.java)

This service allows a user to create a very short-lived version of their access token in order to access certain services ( such as file encryption/decryption on the XQ website) without having to transmit their main access token.


| Initialization Argument Name | Type   | Value          | 
| :--------------------------: | ------ | -------------- | 
|         xqsdk         | XQSDK | |


| Request  Argument Name | Type | Value | Required | Description |
| ---------------------- | ---- | ----- | -------- | ----------- |
| -                      | -    | -     | -        | -           |


| Response  Name | Response  Type | Response  Value         | Description |
| -------------- | -------------- | ----------------------- | ----------- |
| data           | String         | <delegate-access-token> |             |


------

   #####  [RevokeKeyAccess](./src/main/java/com/xqmsg/sdk/v2/services/RevokeKeyAccess.java)

Revokes a key using its token. Only the user who sent the message will be able to revoke it.


| Initialization Argument Name | Type   | Value          | 
| :--------------------------: | ------ | -------------- | 
|         xqsdk         | XQSDK | |


| Request  Argument Name | Type | Value | Required | Description |
| ---------------------- | ---- | ----- | -------- | ----------- |
| -                      | -    | -     | -        | -           |


| Response  Name | Response  Type | Response  Value | Description |
| -------------- | -------------- | --------------- | ----------- |
| -              | -              | -               | -           |


------

   #####  [DeleteSubscriber](./src/main/java/com/xqmsg/sdk/v2/services/DeleteSubscriber.java)

Deletes the subscribed user specified by the access token.
After an account is deleted, the subscriber will be sent an email notifying them of its deletion.


| Initialization Argument Name | Type   | Value          | 
| :--------------------------: | ------ | -------------- | 
|         xqsdk         | XQSDK | |


| Request  Argument Name | Type | Value | Required | Description |
| ---------------------- | ---- | ----- | -------- | ----------- |
| -                      | -    | -     | -        | -           |


| Response  Name | Response  Type | Response  Value | Description |
| -------------- | -------------- | --------------- | ----------- |
| -              | -              | -               | -           |


------

   #####  [GetUserInfo](./src/main/java/com/xqmsg/sdk/v2/services/GetUserInfo.java)

Deletes the user specified by the access token.
After an account is deleted, the subscriber will be sent an email notifying them of its deletion.


| Initialization Argument Name | Type   | Value          |
| :--------------------------: | ------ | -------------- |
|         xqsdk         | XQSDK | |


| Request  Argument Name | Type | Value | Required | Description |
| ---------------------- | ---- | ----- | -------- | ----------- |
| -                      | -    | -     | -        | -           |


| Response  Name     | Type   | Value     | Description                                                  |
| ------------------ | ------ | --------- | ------------------------------------------------------------ |
| id                 | long   | <user-id> | The user ID.                                                 |
| usr                | String | <user-id> | The users' email address                                     |
| firstName          | String | <user-id> | The users first name                                         |
| lastName           | String | <user-id> | The users last name                                          |
| subscriptionStatus | long   | <user-id> | The user's subscription status                               |
| starts             | long   | <user-id> | The datetime ( in milliseconds ) when the subscription was activated. |
| ends               | long   | <user-id> | The datetime ( in milliseconds ) when the subscription will end. |




------

   #####  [GetSettings](./src/main/java/com/xqmsg/sdk/v2/services/GetSettings.java)

Gets the notification and newsletter settings for the current user.


| Initialization Argument Name | Type   | Value          |
| :--------------------------: | ------ | -------------- |
|         xqsdk         | XQSDK | |   


| Request  Argument Name | Type | Value | Required | Description |
| ---------------------- | ---- | ----- | -------- | ----------- |
| -                      | -    | -     | -        | -           |

| Response  Name | Type    | Value       | Description                                                  |
| -------------- | ------- | ----------- | ------------------------------------------------------------ |
| newsLetter     | boolean | true\|false | Should this user receive newsletters or not? <br>This is only valid for new users, and is ignored if the user already exists. |
| notifications  | Long    | [ 0 .. 3 ]  | Specifies the notifications that the user should receive  <br> 0 = No Notifications, <br> 1 = Receive Usage Reports, <br> 2 = Receive Tutorials, <br> 3 = Receive Both |

------

   #####  [CombineAuthorizations](./src/main/java/com/xqmsg/sdk/v2/services/CombineAuthorizations.java)

This endpoint is useful for merging two or more valid access tokens ( along with the access token used to make the call ) into a single one that can be used for temporary read access.

This is useful in situations where a user who has authenticated with multiple accounts wants to get a key for a particular message without needing to know exactly which of their accounts is a valid recipient. As long as one of the accounts in the merged token have access, they will be able to get the key

The merged token has three restrictions:

1. It cannot be used to send messages
2. It can only be created from other valid access tokens.
3. It is only valid for a short amount of time.


| Initialization Argument Name | Type   | Value          | 
| :--------------------------: | ------ | -------------- | 
|         xqsdk         | XQSDK | |


| Request  Argument Name | Type | Value                 | Required | Description                  |
| ---------------------- | ---- | --------------------- | -------- | ---------------------------- |
| tokens                 | List | (<token-string>,...)+ | √        | The list of tokens to merge. |

| Response  Name | Type   | Value          | Description                                                  |
| -------------- | ------ | -------------- | ------------------------------------------------------------ |
| token          | String | <merged-token> | The merged token.                                            |
| merged         | Long   | [ 0 -9 ]+      | The number of tokens that were successfully merged into the token. |


------

   #####  [FetchKey](./src/main/java/com/xqmsg/sdk/v2/services/FetchKey.java)

This endpoint fetches the encryption key associated with the token provided.
The key will only be returned if the following hold true:

 * The access token of the requesting user is valid and unexpired.

 * The expiration time specified for the key has not elapsed.

 * The person requesting the key was listed as a valid recipient by the sender.

 * The key is either not geofenced, or is being accessed from an authorized location.

If any of these is not true, an error will be returned instead.


| Initialization Argument Name | Type   | Value          | 
| :--------------------------: | ------ | -------------- | 
|         xqsdk         | XQSDK | |


| Request  Argument Name | Type   | Value           | Required | Description                                                  |
| ---------------------- | ------ | --------------- | -------- | ------------------------------------------------------------ |
| locatorToken           | String | <locator-token> | √        | Thr key locator token ( the token received after adding a key packet). It is used as a URL to discover the key on  the server.<br />The URL encoding part is handled internally in the service itself |

| Response  Name | Type   | Value | Description                                 |
| -------------- | ------ | ----- | ------------------------------------------- |
| data           | String | <key> | The Encryption Key obtained from the server |

------

   #####  [RevokeKeyAccess](./src/main/java/com/xqmsg/sdk/v2/services/RevokeKeyAccess.java)

Revokes a key using its token. 

Only the user who sent the message will be able to revoke it.


| Initialization Argument Name | Type   | Value          |
| :--------------------------: | ------ | -------------- |
|         xqsdk         | XQSDK | |


| Request  Argument Name | Type   | Value           | Required | Description                                                  |
| ---------------------- | ------ | --------------- | -------- | ------------------------------------------------------------ |
| locatorToken           | String | <locator-token> | √        | Thr key locator token ( the token received after adding a key packet). It is used as a URL to discover the key on  the server.<br />The URL encoding part is handled internally in the service itself |

| Response  Name | Type | Value | Description |
| -------------- | ---- | ----- | ----------- |
| -              | -    | -     | -           |

------

   #####  [UpdateSettings](./src/main/java/com/xqmsg/sdk/v2/services/UpdateSettings.java)

Revokes a key using its token. 

Only the user who sent the message will be able to revoke it.


| Initialization Argument Name | Type   | Value          |
| :--------------------------: | ------ | -------------- |
|         xqsdk         | XQSDK | |


| Request  Argument Name | Type    | Value       | Description                                                  |
| ---------------------- | ------- | ----------- | ------------------------------------------------------------ |
| newsLetter             | boolean | true\|false | Should this user receive newsletters or not? <br>This is only valid for new users, and is ignored if the user already exists. |
| notifications          | Long    | [ 0 .. 3 ]  | Specifies the notifications that the user should receive  <br> 0 = No Notifications, <br> 1 = Receive Usage Reports, <br> 2 = Receive Tutorials, <br> 3 = Receive Both |

| Response  Name | Type | Value | Description |
| -------------- | ---- | ----- | ----------- |
| -              | -    | -     | -           |

------

  
  ## _Cache_
  
  A basic disk backed cache implementation utilizing <a href="https://mapdb.org/">MapDB</a> which is used to store access tokens by email address
  
#####  [SimpleXQCache](./src/main/java/com/xqmsg/sdk/v2/caching/SimpleXQCache.java) 
