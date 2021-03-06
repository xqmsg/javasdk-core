package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.CallMethod;
import com.xqmsg.sdk.v2.CallStatus;
import com.xqmsg.sdk.v2.Reasons;
import com.xqmsg.sdk.v2.ServerResponse;
import com.xqmsg.sdk.v2.XQModule;
import com.xqmsg.sdk.v2.XQSDK;
import com.xqmsg.sdk.v2.utils.Destination;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * This services adds new users to XQ system.
 * It is a variant of {@link Authorize} which adds the user without validating a given email via PIN.
 * However, its use is limited to basic encryption and decryption.
 */
public class AuthorizeAlias extends XQModule {

    private final Logger logger = Logger.getLogger(getClass().getName(), null);

    public static String USER = "user";
    public static String FIRST_NAME = "firstName";
    public static String LAST_NAME = "lastName";
    public static String ANY_AUTHORIZED = "xq.public";

    private static final String SERVICE_NAME = "authorizealias";

    private AuthorizeAlias(XQSDK sdk) {
        assert sdk != null : "An instance of the XQSDK is required";
        super.sdk = sdk;
        super.cache = sdk.getCache();
    }

    /**
     * @param sdk App Settings
     * @returns Authorize
     */
    public static AuthorizeAlias with(XQSDK sdk) {
        return new AuthorizeAlias(sdk);
    }

    @Override
    public List<String> requiredFields() {
        return List.of(USER);
    }


    /**
     * @param maybeArgs Map of request parameters supplied to this method.
     *                  <pre>parameter details:<br>
     *                                   String user! - Email of the user to be validated.<br>
     *                                   String firstName?  - First name of the user.<br>
     *                                   String lastName? - Last name of the user.<br>
     *                                   </pre>
     * @returns CompletableFuture&lt;ServerResponse#payload:{data:String}>>
     * @apiNote !=required ?=optional [...]=default {...} map
     */
    @Override
    public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {
        try {
            return CompletableFuture.completedFuture(
                    validate.andThen(
                            (validatedArgs) -> {
                                ServerResponse response = sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
                                        Optional.of(SERVICE_NAME),
                                        CallMethod.Post,
                                        Optional.empty(),
                                        Optional.of(Destination.XQ),
                                        validatedArgs);

                                String aliasUser = (String) validatedArgs.get().get(AuthorizeAlias.USER);

                                switch (response.status) {
                                    case Ok: {
                                        String accessToken = (String) response.payload.get("data");

                                        cache.putXQAccess(aliasUser, accessToken);
                                        return response;
                                    }
                                    default: {
                                        logger.warning(String.format("failed , reason: %s", response.moreInfo()));
                                        return response;
                                    }
                                }
                            })
                            .apply(maybeArgs));

        } catch (RuntimeException e) {
            return CompletableFuture.completedFuture(unwrapException(e, CallStatus.Error, Reasons.InvalidPayload));
        }
    }

    @Override
    public String moduleName() {
        return "AuthorizeAlias";
    }

}
