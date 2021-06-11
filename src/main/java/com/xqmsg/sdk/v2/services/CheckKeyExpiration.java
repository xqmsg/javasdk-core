package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.CallMethod;
import com.xqmsg.sdk.v2.CallStatus;
import com.xqmsg.sdk.v2.Reasons;
import com.xqmsg.sdk.v2.ServerResponse;
import com.xqmsg.sdk.v2.XQModule;
import com.xqmsg.sdk.v2.XQSDK;
import com.xqmsg.sdk.v2.utils.Destination;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;


/**
 * Check whether a particular key is expired or not without actually fetching it.
 */
public class CheckKeyExpiration extends XQModule {

    private static final Logger logger = Logger(CheckKeyExpiration.class);

    public static final String LOCATOR_TOKEN = "locatorToken";
    public static final String EXPIRES_IN = "expiresOn";

    private static final String SERVICE_NAME = "expiration";

    private CheckKeyExpiration(XQSDK sdk) {
        assert sdk != null : "An instance of the XQSDK is required";
        super.sdk = sdk;
        super.cache = sdk.getCache();
    }

    /**
     * @param sdk App Settings
     * @returns this
     */
    public static CheckKeyExpiration with(XQSDK sdk) {
        return new CheckKeyExpiration(sdk);
    }

    @Override
    public List<String> requiredFields() {
        return List.of(LOCATOR_TOKEN);
    }

    /**
     * @param maybeArgs Map of request parameters supplied to this method.
     *                  <pre>parameter details:<br>
     *                                                                     String locatorToken! - A URL encoded version of the key locator token to fetch the key from the server.
     *                                                                     </pre>
     * @returns CompletableFuture&lt;ServerResponse#payload:{expiresOn:long}}>>
     * @apiNote !=required ?=optional [...]=default {...} map
     */
    @Override
    public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {
        try {
            return
                    validate
                            .andThen((maybeValidated) -> {
                                try {
                                    return authorize
                                            .andThen(
                                                    (authorizationToken) -> {
                                                        Map<String, Object> args = maybeValidated.get();
                                                        String locatorToken = (String) args.get(LOCATOR_TOKEN);

                                                        final String DYNAMIC_SERVICE_NAME = String.format("%s/%s", SERVICE_NAME, encode(locatorToken));

                                                        return CompletableFuture.completedFuture(sdk.call(sdk.VALIDATION_SERVER_URL,
                                                                Optional.of(DYNAMIC_SERVICE_NAME),
                                                                CallMethod.Get,
                                                                Optional.of(Map.of("Authorization", String.format("Bearer %s", authorizationToken))),
                                                                Optional.of(Destination.XQ),
                                                                Optional.of(Map.of())));

                                                    })
                                            .apply(Optional.of(Destination.XQ), maybeValidated);
                                } catch (RuntimeException e) {
                                    return CompletableFuture.completedFuture(unwrapException(e, CallStatus.Error, Reasons.Unauthorized));
                                }
                            }).apply(maybeArgs);
        } catch (RuntimeException e) {
            return CompletableFuture.completedFuture(unwrapException(e, CallStatus.Error, Reasons.InvalidPayload));
        }

    }

    @Override
    public String moduleName() {
        return "CheckKeyExpiration";
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

}
