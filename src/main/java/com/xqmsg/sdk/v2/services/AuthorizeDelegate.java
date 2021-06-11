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
 * This service allows a user to create a very short-lived version of their access token in order to access certain <br>
 * services such as file encryption/decryption on the XQ websie without having to transmit their main access token.
 */
public class AuthorizeDelegate extends XQModule {

    private final Logger logger = Logger.getLogger(getClass().getName(), null);

    private static final String SERVICE_NAME = "delegate";

    private AuthorizeDelegate(XQSDK sdk) {
        assert sdk != null : "An instance of the XQSDK is required";
        super.sdk = sdk;
        super.cache = sdk.getCache();
    }

    /**
     * @param sdk App Settings
     * @returns this
     */
    public static AuthorizeDelegate with(XQSDK sdk) {
        return new AuthorizeDelegate(sdk);
    }

    @Override
    public List<String> requiredFields() {
        return List.of();
    }

    /**
     * @param maybeArgs Map of request parameters supplied to this method.
     *                  <pre>parameter details:none</pre>
     * @returns CompletableFuture&lt;ServerResponse#payload:{data:String}}>>
     * @apiNote !=required ?=optional [...]=default {...} map
     */
    @Override
    public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {
        try {
            return validate
                    .andThen((validated) -> {
                        try {
                            return authorize
                                    .andThen((authorizationToken) -> {
                                        Map<String, String> headerProperties = Map.of("Authorization", String.format("Bearer %s", authorizationToken));
                                        return CompletableFuture.completedFuture(
                                                sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
                                                        Optional.of(SERVICE_NAME),
                                                        CallMethod.Get,
                                                        Optional.of(headerProperties),
                                                        Optional.of(Destination.XQ),
                                                        maybeArgs)
                                        );
                                    })
                                    .apply(Optional.of(Destination.XQ), validated);

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
        return "AuthorizeDelegate";
    }


}
