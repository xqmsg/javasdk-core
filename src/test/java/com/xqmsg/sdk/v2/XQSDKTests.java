package com.xqmsg.sdk.v2;


import com.xqmsg.sdk.v2.exceptions.StatusCodeException;
import com.xqmsg.sdk.v2.quantum.FetchQuantumEntropy;
import com.xqmsg.sdk.v2.services.*;
import com.xqmsg.sdk.v2.services.dashboard.*;
import com.xqmsg.sdk.v2.utils.DateTimeFormats;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.xqmsg.sdk.v2.CallStatus.Ok;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class XQSDKTests {

    private static final Logger logger = getLogger(XQSDKTests.class);

    static XQSDK sdk;

    @BeforeAll
    static void init() {
        sdk = new XQSDK();
        if (Boolean.parseBoolean(System.getProperty("clear-cache", "true"))) {
            sdk.getCache().clearAllProfiles();
        }
    }

    @AfterAll
    static void cleanup() {
//    try {
//      final Path generated = Paths.get(String.format("src/test/resources/%s.txt.xqf", "utf-8-sampler"));
//      Files.deleteIfExists(generated);
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
    }


    /**
     * Tests xq user login. If successful an xq access token will be returned from the server.
     * This test requires manual console input for pin code validation. <br>
     * If you are using IntelliJ the console may not be set to accept user input. <br>
     * You may have to change console settings. <br>
     * Please see {@link #readOneLineFromTerminalInput} for more information.<br>
     */
    @Test
    @Order(10)
    void testXQAuthorize() throws Exception {

        String email = System.getProperty("xqsdk-user.email");

        try {
            String xqAccessToken = sdk.getCache().getXQAccess(email, true);
            logger.info(String.format("The user had already been authorized for xq usage.\nThe xq access token is: %s", xqAccessToken));

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


    /**
     * Tests dashboard user login. If successful a dashboard access token will be returned from the server.
     * This test relies on an existing authenticated xq user.
     * XQ user authorization happens in {@link #testXQAuthorize()}
     */
    @Test
    @Order(11)
    //@Disabled
    void testDashboardLogin() throws Exception {

        String email = System.getProperty("xqsdk-user.email");
        try {
            String dashboardAccessToken = sdk.getCache().getDashboardAccess(email, true);
            logger.info(String.format("The user had already been authorized for dashboard usage.\nThe dashboard authorization token is: %s", dashboardAccessToken));
            assertTrue(true);
        } catch (StatusCodeException e) {

            logger.info(String.format("%s", e.statusMessage()));

            String result = DashboardLogin.with(sdk)
                    .supplyAsync(Optional.empty())
                    .thenApply(
                            (ServerResponse serverResponse) -> {
                                switch (serverResponse.status) {
                                    case Ok: {
                                        String accessToken = (String) serverResponse.payload.get(ServerResponse.DATA);

                                        logger.info(String.format("Dashboard Access Token: %s", accessToken));
                                        return "success";
                                    }
                                    default: {
                                        logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                                        return "error";
                                    }
                                }
                            }
                    ).get();

            assertEquals("success", result);

        }
    }


    /**
     *
     **/
    @Test
    @Order(12)
    //@Disabled
    void testDashboardGetApplications() throws Exception {

        List apps = GetApplications.with(sdk)
                .supplyAsync(Optional.empty())
                .thenApply(
                        (ServerResponse serverResponse) -> {
                            switch (serverResponse.status) {
                                case Ok: {
                                    List<Map<String, Object>> applications = (List<Map<String, Object>>) serverResponse.payload.get(GetApplications.APPS);
                                    applications.forEach(app -> {
                                        logger.info(String.format("Name: %s, ID: %s", app.get("name"), app.get("id")));
                                    });
                                    return applications;
                                }
                                default: {
                                    logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                                    return List.of();
                                }
                            }
                        }
                ).get();

        assertNotEquals(0, apps.size());

    }

    /**
     *
     **/
    @Test
    @Order(13)
    //@Disabled
    void testDashboardAddUserGroup() throws Exception {

        String result = AddUserGroup.with(sdk)
                .supplyAsync(Optional.of(Map.of(AddUserGroup.NAME, "New Test Generated User Group",
                        AddUserGroup.MEMBERS, makeUsers(2).get("ALL"))))
                .thenApply(
                        (ServerResponse serverResponse) -> {
                            switch (serverResponse.status) {
                                case Ok: {
                                    var id = serverResponse.payload.get(AddUserGroup.ID);
                                    logger.info(String.format("Group created, Id: %s", id));
                                    return "success";
                                }
                                default: {
                                    logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                                    return "error";
                                }
                                    
                            }
                        }
                ).get();

        assertEquals("success", result);

    }

    /**
     *
     **/
    @Test
    @Order(14)
    //@Disabled
    void testDashboardUpdateUserGroup() throws Exception {


        ServerResponse userGroupsServerResponse = FindUserGroups.with(sdk)
                .supplyAsync(Optional.of(Map.of(FindUserGroups.ID, "[0-9]+"))).get();

        List<Map<String, Object>> userGroups = (List<Map<String, Object>>) userGroupsServerResponse.payload.get(FindUserGroups.GROUPS);

        Optional<Map<String, Object>> found = userGroups.stream().filter((userGroup) -> {
            return "New Test Generated User Group".equals(userGroup.get("name"));
        }).findFirst();

        Map<String, Object> payload = Map.of(
                UpdateUserGroup.ID, found.get().get(FindUserGroups.ID),
                UpdateUserGroup.NAME, "Updated Test Generated User Group",
                UpdateUserGroup.MEMBERS, makeUsers(3).get("ALL"));

        String result = UpdateUserGroup.with(sdk)
                .supplyAsync(Optional.of(payload))
                .thenApply(
                        (ServerResponse updateResponse) -> {
                            switch (updateResponse.status) {
                                case Ok: {
                                    logger.info(String.format("Response Status Code:: %s", updateResponse.status));
                                    return "success";
                                }
                                default: {
                                    logger.severe(String.format("failed , reason: %s", updateResponse.moreInfo()));
                                    return "error";
                                }
                            }
                        }
                ).get();

        assertEquals("success", result);

    }

    /**
     *
     **/
    @Test
    @Order(15)
    //@Disabled
    void testDashboardRemoveUserGroup() throws Exception {

        ServerResponse userGroupsServerResponse = FindUserGroups.with(sdk)
                .supplyAsync(Optional.of(Map.of(FindUserGroups.ID, "[0-9]+"))).get();

        List<Map<String, Object>> userGroups = (List<Map<String, Object>>) userGroupsServerResponse.payload.get(FindUserGroups.GROUPS);

        List<Map<String, Object>> groups = userGroups.stream().filter((userGroup) -> {
            return "Updated Test Generated User Group".equals(userGroup.get("name"));
        }).collect(Collectors.toList());

        groups.forEach(group -> {
            Map<String, Object> payload = Map.of(UpdateUserGroup.ID, group.get(FindUserGroups.ID));
            String result = null;
            try {
                result = RemoveUserGroup.with(sdk)
                        .supplyAsync(Optional.of(payload))
                        .thenApply(
                                (ServerResponse removeResponse) -> {
                                    switch (removeResponse.status) {
                                        case Ok: {
                                            logger.info(String.format("Response Status Code:: %s", removeResponse.status));
                                            return "success";
                                        }
                                        default: {
                                            logger.severe(String.format("failed , reason: %s", removeResponse.moreInfo()));
                                            return "error";
                                        }

                                    }
                                }
                        ).get();
            } catch (Exception e) {
            }
            assertEquals("success", result);
        });

    }

    /**
     *
     **/
    @Test
    @Order(16)
    //@Disabled
    void testDashboardAddContact() throws Exception {

        String result = AddContact.with(sdk)
                .supplyAsync(Optional.of(Map.of(AddContact.EMAIL, makeUsers(1).get("FIRST"),
                        AddContact.NOTIFICATIONS, Notifications.NONE,
                        AddContact.ROLE, Roles.Alias.ordinal(),
                        AddContact.TITLE, "Mr.",
                        AddContact.FIRST_NAME, "John",
                        AddContact.LAST_NAME, "Doe")))
                .thenApply(
                        (ServerResponse serverResponse) -> {
                            switch (serverResponse.status) {
                                case Ok: {
                                    var id = serverResponse.payload.get(AddContact.ID);
                                    logger.info(String.format("Contact created, Id: %s", id));
                                    return "success";
                                }
                                default: {
                                    logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                                    return "error";
                                }

                            }
                        }
                ).get();

        assertEquals("success", result);

    }


    /**
     *
     **/
    @Test
    @Order(15)
    //@Disabled
    void testDashboardDisableContact() throws Exception {

        ServerResponse contactsServerResponse = FindContacts.with(sdk)
                .supplyAsync(Optional.of(Map.of(FindContacts.FILTER, "%"))).get();

        List<Map<String, Object>> contacts = (List<Map<String, Object>>) contactsServerResponse.payload.get(FindContacts.CONTACTS);

        Optional<Map<String, Object>> found = contacts.stream().filter((contact) -> {
            return "John".equals(contact.get("fn")) && "Doe".equals(contact.get("ln"));
        }).findFirst();

        Map<String, Object> payload = Map.of(FindContacts.ID, found.get().get(FindContacts.ID));

        String result = DisableContact.with(sdk)
                .supplyAsync(Optional.of(payload))
                .thenApply(
                        (ServerResponse removeResponse) -> {
                            switch (removeResponse.status) {
                                case Ok: {
                                    logger.info(String.format("Response Status Code:: %s", removeResponse.status));
                                    return "success";
                                }
                                default: {
                                    logger.severe(String.format("failed , reason: %s", removeResponse.moreInfo()));
                                    return "error";
                                }
                            }
                        }
                ).get();

        assertEquals("success", result);

    }

    /**
     *
     **/
    @Test
    @Order(16)
    //@Disabled
    void testDashboardRemoveContact() throws Exception {

        ServerResponse contactsServerResponse = FindContacts.with(sdk)
                .supplyAsync(Optional.of(Map.of(FindContacts.FILTER, "%"))).get();

        List<Map<String, Object>> contacts = (List<Map<String, Object>>) contactsServerResponse.payload.get(FindContacts.CONTACTS);

        List<Map<String, Object>> candidates = contacts.stream().filter((contact) -> {
            return "John".equals(contact.get("fn")) && "Doe".equals(contact.get("ln"));
        }).collect(Collectors.toList());

        candidates.forEach(candidate -> {

            String result = null;
            try {
                Map<String, Object> payload = Map.of(FindContacts.ID, candidate.get(FindContacts.ID));

                result = RemoveContact.with(sdk)
                        .supplyAsync(Optional.of(payload))
                        .thenApply(
                                (ServerResponse removeResponse) -> {
                                    switch (removeResponse.status) {
                                        case Ok: {
                                            logger.info(String.format("Response Status Code:: %s", removeResponse.status));
                                            return "success";
                                        }
                                        default: {
                                            logger.severe(String.format("failed , reason: %s", removeResponse.moreInfo()));
                                            return "error";
                                        }
                                        
                                    }
                                }
                        ).get();
            } catch (Exception e) {
            }
            assertEquals("success", result);

        });


    }

    @Test
    @Order(20)
    //@Disabled
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
                    }

                }).get();

    }

    @Test
    @Order(30)
    //@Disabled
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
                            
                    }

                }).get();

    }

    @Test
    @Order(40)
    //@Disabled
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

                    }

                }).get();


    }

    @Test
    @Order(45)
    //@Disabled
    void testEncryptDecryptMessageWithAliasUserAndAliasRecipient() throws Exception {

        final String ALIAS_USER_ID = "123";
        final String ALIAS_RECIPIENT_ONE_ID = "456";
        final String ALIAS_RECIPIENT_TWO_ID = "789";

        //alias user doing the encryption
        authorizeliasAccess(ALIAS_USER_ID);
        sdk.getCache().putActiveProfile(ALIAS_USER_ID);

        //a specific alias recipient
        List aliasRecipients = List.of(ALIAS_RECIPIENT_ONE_ID+"@alias.local");

        final String originalText =
                "\n\nSehr geehrter Mr. Krings,\n" +
                        "wenn Oechtringen irgendwo mit einem Akzent auf dem O geschrieben wurde, dann kann das nur ein Fehldruck sein. \n"+
                        "Die offizielle Schreibweise lautet jedenfalls „Oechtringen“. \n" +
                        "Mit freundlichen Grüssen \n" +
                        "Der Samtgemeindebürgermeister \n" +
                        "i.A. Lothar Jessel \n" +
                        "From Karl Pentzlin (Kochel am See, Bavaria, Germany): \n"+
                        "This German phrase is suited for display by a Fraktur (broken letter) font. \n" +
                        "It contains: all common three-letter ligatures: ffi ffl fft and all two-letter ligatures required by the Duden for Fraktur typesetting: \n"+
                        "ch ck ff fi fl ft ll ſch ſi ſſ ſt tz (all in a manner such they are not part of a three-letter ligature), one example of f-l where German typesetting \n"+
                        "rules prohibit ligating (marked by a ZWNJ), and all German letters a...z, ä,ö,ü,ß, ſ [long s] \n"+
                        "(all in a manner such that they are not part of a two-letter Fraktur ligature). \n" +
                        "Otto Stolz notes that \" 'Schloß' \" is now spelled 'Schloss', in contrast to 'größer' (example 4) which has kept its 'ß'. \n"+
                        "Fraktur has been banned from general use, in 1942, and long-s (ſ) has ceased to be used with Antiqua (Roman) even earlier \n" +
                        "(the latest Antiqua-ſ I have seen is from 1913, but then I am no expert, so there may well be a later instance.\" Later Otto confirms the latter theory, \n"+
                        "\"Now I've run across a book “Deutsche Rechtschreibung” (edited by Lutz Mackensen) from 1954 (my reprint is from 1956) that has kept the Antiqua-ſ in \n"+
                        "its dictionary part (but neither in the preface nor in the appendix).\" \n" +
                        "Diaeresis is not used in Iberian Portuguese. Also this pangram is missing a-tilde (ã) so it's a pænpangram. \n" +
                        "From Yurio Miyazawa: \"This poetry contains all the sounds in the Japanese language and used to be the first thing for children to learn in their Japanese class. \n"+
                        "The Hiragana version is particularly neat because it covers every character in the phonetic Hiragana character set.\" Yurio also sent the Kanji version: \n" +
                        "色は匂へど 散りぬるを \n" +
                        "我が世誰ぞ 常ならむ \n" +
                        "有為の奥山 今日越えて \n" +
                        "浅き夢見じ 酔ひもせず";


        ServerResponse encryptResponse = Encrypt
                .with(sdk, AlgorithmEnum.OTPv2)
                .supplyAsync(Optional.of(Map.of(Encrypt.USER, ALIAS_USER_ID,
                        Encrypt.TEXT, originalText,
                        Encrypt.RECIPIENTS, aliasRecipients,
                        Encrypt.MESSAGE_EXPIRATION_HOURS, 1))).get();

        String locatorToken = (String) encryptResponse.payload.get(Encrypt.LOCATOR_KEY);
        String encryptedText = (String) encryptResponse.payload.get(Encrypt.ENCRYPTED_TEXT);

        ///---------
        //intended recipient, should be able to decrypt
        authorizeliasAccess(ALIAS_RECIPIENT_ONE_ID);
        sdk.getCache().putActiveProfile(ALIAS_RECIPIENT_ONE_ID);
        ServerResponse decryptResponse = Decrypt
                                           .with(sdk, AlgorithmEnum.OTPv2)
                                           .supplyAsync(Optional.of(Map.of(Decrypt.LOCATOR_TOKEN, locatorToken, Decrypt.ENCRYPTED_TEXT, encryptedText))).get();

        String decryptedText = (String) decryptResponse.payload.get(ServerResponse.DATA);
        logger.info("Decrypted Text:  " + decryptedText);
        assertEquals(originalText, decryptedText);

        ///---------
        //not a valid recipient, should not be able to decrypt
        authorizeliasAccess(ALIAS_RECIPIENT_TWO_ID);
        sdk.getCache().putActiveProfile(ALIAS_RECIPIENT_TWO_ID);
        ServerResponse decryptResponse1 = Decrypt
                                             .with(sdk, AlgorithmEnum.OTPv2)
                                             .supplyAsync(Optional.of(Map.of(Decrypt.LOCATOR_TOKEN, locatorToken, Decrypt.ENCRYPTED_TEXT, encryptedText))).get();

        //reset the active profile back to the test user
        String email = System.getProperty("xqsdk-user.email");
        sdk.getCache().putActiveProfile(email);

        //remove alias account from cache
        sdk.getCache().removeProfile(ALIAS_USER_ID);
        sdk.getCache().removeProfile(ALIAS_RECIPIENT_ONE_ID);
        sdk.getCache().removeProfile(ALIAS_RECIPIENT_TWO_ID);

        assertEquals(CallStatus.Error, decryptResponse1.status.Error);
    }

    @Test
    @Order(46)
    //@Disabled
    void testEncryptDecryptMessageWithAliasUserAndXQPublic() throws Exception {

        final String ALIAS_USER_ID = "123";
        final String ALIAS_RECIPIENT_ONE_ID = "456";
        final String ALIAS_RECIPIENT_TWO_ID = "789";

        //encryptor
        authorizeliasAccess(ALIAS_USER_ID);
        sdk.getCache().putActiveProfile(ALIAS_USER_ID);

        List aliasRecipients = List.of(AuthorizeAlias.ANY_AUTHORIZED);

        final String originalText =
                "\n\nSehr geehrter Mr. Krings,\n" +
                        "wenn Oechtringen irgendwo mit einem Akzent auf dem O geschrieben wurde, dann kann das nur ein Fehldruck sein. \n" +
                        "Die offizielle Schreibweise lautet jedenfalls „Oechtringen“. \n" +
                        "Mit freundlichen Grüssen \n" +
                        "Der Samtgemeindebürgermeister \n" +
                        "i.A. Lothar Jessel \n" +
                        "From Karl Pentzlin (Kochel am See, Bavaria, Germany): \n" +
                        "This German phrase is suited for display by a Fraktur (broken letter) font. \n" +
                        "It contains: all common three-letter ligatures: ffi ffl fft and all two-letter ligatures required by the Duden for Fraktur typesetting: \n" +
                        "ch ck ff fi fl ft ll ſch ſi ſſ ſt tz (all in a manner such they are not part of a three-letter ligature), one example of f-l where German typesetting \n" +
                        "rules prohibit ligating (marked by a ZWNJ), and all German letters a...z, ä,ö,ü,ß, ſ [long s] \n" +
                        "(all in a manner such that they are not part of a two-letter Fraktur ligature). \n" +
                        "Otto Stolz notes that \" 'Schloß' \" is now spelled 'Schloss', in contrast to 'größer' (example 4) which has kept its 'ß'. \n" +
                        "Fraktur has been banned from general use, in 1942, and long-s (ſ) has ceased to be used with Antiqua (Roman) even earlier \n" +
                        "(the latest Antiqua-ſ I have seen is from 1913, but then I am no expert, so there may well be a later instance.\" Later Otto confirms the latter theory, \n" +
                        "\"Now I've run across a book “Deutsche Rechtschreibung” (edited by Lutz Mackensen) from 1954 (my reprint is from 1956) that has kept the Antiqua-ſ in \n" +
                        "its dictionary part (but neither in the preface nor in the appendix).\" \n" +
                        "Diaeresis is not used in Iberian Portuguese. Also this pangram is missing a-tilde (ã) so it's a pænpangram. \n" +
                        "From Yurio Miyazawa: \"This poetry contains all the sounds in the Japanese language and used to be the first thing for children to learn in their Japanese class. \n" +
                        "The Hiragana version is particularly neat because it covers every character in the phonetic Hiragana character set.\" Yurio also sent the Kanji version: \n" +
                        "色は匂へど 散りぬるを \n" +
                        "我が世誰ぞ 常ならむ \n" +
                        "有為の奥山 今日越えて \n" +
                        "浅き夢見じ 酔ひもせず";


        ServerResponse encryptResponse = Encrypt
                .with(sdk, AlgorithmEnum.OTPv2)
                .supplyAsync(Optional.of(Map.of(Encrypt.USER, ALIAS_USER_ID,
                        Encrypt.TEXT, originalText,
                        Encrypt.RECIPIENTS, aliasRecipients,
                        Encrypt.MESSAGE_EXPIRATION_HOURS, 1))).get();

        String locatorToken = (String) encryptResponse.payload.get(Encrypt.LOCATOR_KEY);
        String encryptedText = (String) encryptResponse.payload.get(Encrypt.ENCRYPTED_TEXT);

        ///---------
        //should be able to decrypt
        authorizeliasAccess(ALIAS_RECIPIENT_ONE_ID);
        sdk.getCache().putActiveProfile(ALIAS_RECIPIENT_ONE_ID);
        ServerResponse decryptResponse = Decrypt
                .with(sdk, AlgorithmEnum.OTPv2)
                .supplyAsync(Optional.of(Map.of(Decrypt.LOCATOR_TOKEN, locatorToken, Decrypt.ENCRYPTED_TEXT, encryptedText))).get();

        String decryptedText = (String) decryptResponse.payload.get(ServerResponse.DATA);
        logger.info("Decrypted Text:  " + decryptedText);
        assertEquals(originalText, decryptedText);

        ///---------
        //should also be able to decrypt
        authorizeliasAccess(ALIAS_RECIPIENT_TWO_ID);
        sdk.getCache().putActiveProfile(ALIAS_RECIPIENT_TWO_ID);
        ServerResponse decryptResponse1 = Decrypt
                .with(sdk, AlgorithmEnum.OTPv2)
                .supplyAsync(Optional.of(Map.of(Decrypt.LOCATOR_TOKEN, locatorToken, Decrypt.ENCRYPTED_TEXT, encryptedText))).get();

        String decryptedText1 = (String) decryptResponse1.payload.get(ServerResponse.DATA);
        logger.info("Decrypted Text:  " + decryptedText1);
        assertEquals(originalText, decryptedText1);

        //reset the active profile back to the test user
        String email = System.getProperty("xqsdk-user.email");
        sdk.getCache().putActiveProfile(email);


        //remove alias account from cache
        sdk.getCache().removeProfile(ALIAS_USER_ID);
        sdk.getCache().removeProfile(ALIAS_RECIPIENT_ONE_ID);
        sdk.getCache().removeProfile(ALIAS_RECIPIENT_TWO_ID);

    }

    @Test
    @Order(50)
    //@Disabled
    void testEncryptDecryptKeyRetrievalRevokeKeyAccessGrantKeyAccessRevokeUserAccess() throws Exception {

        String email = System.getProperty("xqsdk-user.email");

        List recipients = List.of(System.getProperty("xqsdk-recipients.email"));

        final String originalText =
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


        ServerResponse encryptResponse = Encrypt
                .with(sdk, AlgorithmEnum.OTPv2)
                .supplyAsync(Optional.of(Map.of(Encrypt.USER, email,
                        Encrypt.TEXT, originalText,
                        Encrypt.RECIPIENTS, recipients,
                        Encrypt.MESSAGE_EXPIRATION_HOURS, 1))).get();

        String locatorToken = (String) encryptResponse.payload.get(Encrypt.LOCATOR_KEY);
        String encryptedText = (String) encryptResponse.payload.get(Encrypt.ENCRYPTED_TEXT);

        ServerResponse decryptResponse = Decrypt
                .with(sdk, AlgorithmEnum.OTPv2)
                .supplyAsync(Optional.of(Map.of(Decrypt.LOCATOR_TOKEN, locatorToken, Decrypt.ENCRYPTED_TEXT, encryptedText))).get();

        String decryptedText = (String) decryptResponse.payload.get(ServerResponse.DATA);
        logger.info("Decrypted Text:  " + decryptedText);

        assertEquals(originalText, decryptedText);

        ServerResponse keyRetrievalResponse = FetchKey
                .with(sdk)
                .supplyAsync(Optional.of(Map.of(Decrypt.LOCATOR_TOKEN, locatorToken)))
                .get();

        logger.info("Key from Server:  " + keyRetrievalResponse.payload.get(ServerResponse.DATA));

        ServerResponse keyExpirationResponse = CheckKeyExpiration
                .with(sdk)
                .supplyAsync(Optional.of(Map.of(Decrypt.LOCATOR_TOKEN, locatorToken))).get();

        Long expiresIn = (Long) keyExpirationResponse.payload.get(CheckKeyExpiration.EXPIRES_IN);
        LocalDateTime now = new LocalDateTime();
        LocalDateTime expiresOn = now.plus(new Period().withSeconds(expiresIn.intValue()));
        String expiresOnString = DateTimeFormats.render(expiresOn, DateTimeFormats.ISO_8601_DATE_TIME);
        logger.info(String.format("Key Expires On %s", expiresOnString));

        ServerResponse revokeKeyAccessResponse = RevokeKeyAccess
                .with(sdk)
                .supplyAsync(Optional.of(Map.of(Decrypt.LOCATOR_TOKEN, locatorToken)))
                .get();
        String noContent = (String) revokeKeyAccessResponse.payload.get(ServerResponse.DATA);

        assertEquals("No Content", noContent);

        ServerResponse grantUserAccessResponse = GrantUserAccess
                .with(sdk)
                .supplyAsync(Optional.of(Map.of(
                        GrantUserAccess.LOCATOR_TOKEN, locatorToken,
                        GrantUserAccess.RECIPIENTS, recipients
                )))
                .get();

        assertEquals("No Content", grantUserAccessResponse.payload.get(ServerResponse.DATA));

        ServerResponse revokeUserAccessResponse = RevokeUserAccess
                .with(sdk)
                .supplyAsync(Optional.of(Map.of(
                        RevokeUserAccess.LOCATOR_TOKEN, locatorToken,
                        RevokeUserAccess.RECIPIENTS, recipients
                )))
                .get();

        assertEquals("No Content", revokeUserAccessResponse.payload.get(ServerResponse.DATA));

    }

    /**
     * make sure to have your vm args set up for this on. check you run/debug configs.
     * should be something like this:
     * <p>
     * -Dmode=test
     * -Dxqsdk-user.email=john.doe@example.com
     * -Dxqsdk-user2.email=john.doe+2@example.com
     * <p>
     */
    @Test
    @Order(60)
    //@Disabled
    void testMergeTokens() throws Exception {

        String email1 = System.getProperty("xqsdk-user.email");

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
                                default: {
                                    logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                                    return "";
                                }

                            }
                        }
                ).get();

        logger.info("Combined Access Token from Server:  " + combinedAccessToken);

        assertNotEquals("", combinedAccessToken);

    }


    @Test
    @Order(70)
    //@Disabled
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
                                        default: {
                                            logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                                            fail();
                                            return null;
                                        }
                                    }
                                }
                        ).get();

        assertNotNull(delegateAccessToken);

    }


    @Test
    @Order(80)
    //@Disabled
    void testAESFileEncryption() throws Exception {

        String email = System.getProperty("xqsdk-user.email");

        final Path sourceSpec = Paths.get(String.format("src/test/resources/%s.txt", "utf-8-sampler"));
        final Path targetSpec = Paths.get(String.format("src/test/resources/%s.txt.aes.xqf", "utf-8-sampler"));
        final String user = email;
        final List recipients = List.of(email);
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
                        default: {
                            logger.warning(String.format("failed , reason: %s", serverResponse.moreInfo()));
                            fail();
                            return null;
                        }

                    }

                }).get();

        assertTrue(encryptedFilePath != null);


    }

    @Test
    @Order(90)
    //@Disabled
    void testAESFileDecryption() throws Exception {

        final Path originalSpec = Paths.get(String.format("src/test/resources/%s.txt", "utf-8-sampler"));
        final Path encryptedSpec = Paths.get(String.format("src/test/resources/%s.txt.aes.xqf", "utf-8-sampler"));
        final Path decryptedSpec = Paths.get(String.format("src/test/resources/%s.aes.out.txt", "utf-8-sampler"));

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
                        default: {
                            logger.warning(String.format("failed , reason: %s", serverResponse.moreInfo()));
                            fail();
                            return null;
                        }
                    }

                }).get();


        String originalFileContent = Files.readString(originalSpec);
        String decryptedFileContent = Files.readString(resultSpec);

        logger.severe(String.format("  Original File Content: %s", originalFileContent.replaceAll("\n", " ")));
        logger.severe(String.format(" Decrypted File Content: %s", decryptedFileContent.replaceAll("\n", " ")));

        assertEquals(originalFileContent, decryptedFileContent);

        logger.severe(String.format("Cleaning up generated resources [%s, %s]",encryptedSpec, decryptedSpec));

        Files.deleteIfExists(encryptedSpec);
        Files.deleteIfExists(decryptedSpec);

    }

    @Test
    @Order(100)
    //@Disabled
    void testOTPv2FileEncryption() throws Exception {

        String email = System.getProperty("xqsdk-user.email");

        final Path sourceSpec = Paths.get(String.format("src/test/resources/%s.txt", "utf-8-sampler"));
        final Path targetSpec = Paths.get(String.format("src/test/resources/%s.txt.opt.xqf", "utf-8-sampler"));

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
                        default: {
                            logger.warning(String.format("failed , reason: %s", serverResponse.moreInfo()));
                            fail();
                            return null;
                        }

                    }

                }).get();

        assertTrue(encryptedFilePath != null);

    }


    @Test
    @Order(110)
    //@Disabled
    void testOTPv2FileDecryption() throws Exception {

        final Path originalSpec = Paths.get(String.format("src/test/resources/%s.txt", "utf-8-sampler"));
        final Path encryptedSpec = Paths.get(String.format("src/test/resources/%s.txt.opt.xqf", "utf-8-sampler"));
        final Path decryptedSpec = Paths.get(String.format("src/test/resources/%s.opt.out.txt", "utf-8-sampler"));

        Path resultSpec = FileDecrypt.with(sdk, AlgorithmEnum.OTPv2)
                .supplyAsync(Optional.of(Map.of(FileDecrypt.SOURCE_FILE_PATH, encryptedSpec, FileDecrypt.TARGET_FILE_PATH, decryptedSpec)))
                .thenApply((serverResponse) -> {
                    switch (serverResponse.status) {
                        case Ok: {
                            var decryptFilePath = (Path) serverResponse.payload.get(ServerResponse.DATA);
                            logger.info("Decrypt Filepath: " + decryptFilePath);
                            return decryptFilePath;
                        }
                        default: {
                            logger.warning(String.format("failed , reason: %s", serverResponse.moreInfo()));
                            return null;
                        }
                    }

                }).get();


        assertNotEquals(resultSpec, null);

        String originalFileContent = Files.readString(originalSpec);
        String decryptedFileContent = Files.readString(resultSpec);

        logger.severe(String.format("  Original File Content: %s", originalFileContent.replaceAll("\n", " ")));
        logger.severe(String.format(" Decrypted File Content: %s", decryptedFileContent.replaceAll("\n", " ")));

        assertEquals(originalFileContent, decryptedFileContent);

        logger.severe(String.format("Cleaning up generated resources [%s, %s]",encryptedSpec, decryptedSpec));

        Files.deleteIfExists(encryptedSpec);
        Files.deleteIfExists(decryptedSpec);

    }


    @Test
    @Order(120)
    //@Disabled
    void testAuthorizeAlias() throws Exception {

        String alias = System.getProperty("xqsdk-alias-user");

        Map<String, Object> payload =
                Map.of(Authorize.USER, alias,
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

        //change the active profile back to the previously authorized account
        String email = System.getProperty("xqsdk-user.email");

        sdk.getCache().putActiveProfile(email);

        assertTrue(accessToken != null && !"".equals(accessToken.trim()));
        assertNotEquals("", accessToken);

    }

    @Test
    @Order(130)
    //@Disabled
    void testCheckApiKey() throws Exception {

        List<String> scopes = CheckApiKey.with(sdk)
                .supplyAsync(Optional.of((Map.of(CheckApiKey.API_KEY, sdk.XQ_APPLICATION_KEY))))
                .thenApply(
                        (ServerResponse apiKeyCheckResponse) -> {
                            switch (apiKeyCheckResponse.status) {
                                case Ok: {
                                    var payload = apiKeyCheckResponse.payload;
                                    return (List<String>) payload.get(CheckApiKey.SCOPES);
                                }
                                default: {
                                    logger.warning(String.format("`testCheckApiKey` failed , reason: %s", apiKeyCheckResponse.moreInfo()));
                                    return null;
                                }
                            }
                        }).get();

        assertTrue(scopes != null && scopes.size() > 0);

    }


    /**
     * Can only be tested this if <br>
     * {@link #testDeleteAuthorization()} )} <br>
     * is annotated with {@link Disabled}
     */
    @Test
    @Order(140)
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
                        default: {
                            logger.warning(String.format("failed , reason: %s", serverResponse.moreInfo()));
                            fail();
                            break;
                        }
                    }

                }).get();


    }

    /**
     * Can only be tested this if <br>
     * {@link #testDeleteSubscriber()}  <br>
     * is annotated with {@link Disabled}
     */
    @Test
    @Order(150)
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
                                default: {
                                    logger.severe(String.format("failed , reason: %s", serverResponse.moreInfo()));
                                    return "";
                                }

                            }
                        }
                ).get();

        assertEquals("No Content", noContent);

    }


    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////                             UTILITY METHODS                                  //////////
    //////////////////////////////////////////////////////////////////////////////////////////////////


    private String getPinFromTerminalInput() {
        return readOneLineFromTerminalInput("Code", "Please enter the pin number");
    }

    /**
     * Utility method for reading user input needed to enter for example the user validation pin sent by email.<br>
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
                                default: {
                                    logger.warning(String.format("`testUserAccessRequest` failed at authorization stage, reason: %s", authorizationResponse.moreInfo()));
                                    fail();
                                    return CompletableFuture.completedFuture(new ServerResponse(CallStatus.Error, Reasons.EncryptionFailed, authorizationResponse.moreInfo()));
                                }
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
                                default: {
                                    logger.warning(String.format("`testEncryption` failed at access code exchange stage, reason: %s", exchangeResponse.moreInfo()));
                                    fail();
                                    return null;
                                }

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

    static Map<String, Object> makeUsers(int limit) {
        List users = new ArrayList();
        for (int i = 0; i < limit; i++) {
            users.add(String.format("test-user-%s@xqmsg.com", Double.valueOf(Math.random() * (1000 - 1) + 1).intValue()));
        }
        return Map.of("FIRST", users.get(0), "ALL", users);
    }

    static String authorizeliasAccess(String id){
        try {
            return AuthorizeAlias
                    .with(sdk)
                    .supplyAsync(Optional.of(Map.of(AuthorizeAlias.USER, id)))
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
        }catch (Exception e){return null;}
    }

}


