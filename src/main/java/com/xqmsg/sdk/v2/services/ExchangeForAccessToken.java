package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.*;
import com.xqmsg.sdk.v2.exceptions.StatusCodeException;
import com.xqmsg.sdk.v2.utils.Destination;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Exchange the temporary access token with a real access token used in all secured XQ Message interactions
 */
public class ExchangeForAccessToken extends XQModule {

    private final Logger logger = Logger.getLogger(getClass().getName(), null);

    private static final String SERVICE_NAME = "exchange";

    private ExchangeForAccessToken(XQSDK sdk) {
        assert sdk != null : "An instance of the XQSDK is required";
        super.sdk = sdk;
        super.cache = sdk.getCache();

    }

    /**
     * @param sdk App Settings
     * @returns ExchangeForAccessToken
     */
    public static ExchangeForAccessToken with(XQSDK sdk) {
        return new ExchangeForAccessToken(sdk);
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
            return CompletableFuture.completedFuture(validate.andThen((maybeValidated) -> {
                try {
                    return preAuthorize.andThen(
                            (preauthAccessToken) -> {
                                Map<String, String> headerProperties = Map.of("Authorization", String.format("Bearer %s", preauthAccessToken));

                                ServerResponse exchangeResponse = sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
                                        Optional.of(SERVICE_NAME),
                                        CallMethod.Get,
                                        Optional.of(headerProperties),
                                        Optional.of(Destination.XQ),
                                        maybeArgs);

                                switch (exchangeResponse.status) {
                                    case Ok: {
                                        String accessToken = (String) exchangeResponse.payload.get(ServerResponse.DATA);
                                        try {
                                            String activeProfile = cache.getActiveProfile(true);
                                            cache.putXQAccess(activeProfile, accessToken);
                                            cache.removeXQPreAuthToken(activeProfile);
                                        } catch (StatusCodeException e) {
                                            logger.severe(e.getMessage());
                                            return null;
                                        }
                                    }
                                    default: {
                                        return exchangeResponse;
                                    }
                                }
                            }).apply(maybeValidated);
                } catch (RuntimeException e) {
                    return unwrapException(e, CallStatus.Error, Reasons.Unauthorized);
                }
            }).apply(maybeArgs));
        } catch (RuntimeException e) {
            return CompletableFuture.completedFuture(unwrapException(e, CallStatus.Error, Reasons.InvalidPayload));
        }
    }

    @Override
    public String moduleName() {
        return "ExchangeForAccessToken";
    }


}
