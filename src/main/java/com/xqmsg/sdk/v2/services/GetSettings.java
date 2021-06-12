package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.*;
import com.xqmsg.sdk.v2.utils.Destination;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;


/**
 * Gets the notification and newsletter settings for the current user.
 */
public class GetSettings extends XQModule {

    private static final Logger logger = Logger(GetSettings.class);

    public static final String NOTIFICATIONS = "notifications";
    public static final String NEWSLETTER = "newsletter";

    private static final String SERVICE_NAME = "settings";

    private GetSettings(XQSDK sdk) {
        assert sdk != null : "An instance of the XQSDK is required";
        super.sdk = sdk;
        super.cache = sdk.getCache();

    }

    @Override
    public List<String> requiredFields() {
        return List.of();
    }

    /**
     * @param sdk App Settings
     * @returns this
     */
    public static GetSettings with(XQSDK sdk) {
        return new GetSettings(sdk);
    }

    /**
     * @param maybeArgs Map of request parameters supplied to this method.
     *                  <pre>parameter details:none</pre>
     * @returns CompletableFuture&lt;ServerResponse#payload:{notifications:Notifications, newsLetter:boolean}>
     * @apiNote !=required ?=optional [...]=default {...} map
     */
    @Override
    public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

        try {
            return CompletableFuture.completedFuture(validate
                    .andThen((maybeValid) -> {
                        try {
                            return authorize
                                    .andThen((authorizationToken) -> {
                                        Map<String, String> headerProperties = Map.of("Authorization", String.format("Bearer %s", authorizationToken));
                                        return sdk.call(sdk.SUBSCRIPTION_SERVER_URL,
                                                Optional.of(SERVICE_NAME),
                                                CallMethod.Get,
                                                Optional.of(headerProperties),
                                                Optional.of(Destination.XQ),
                                                maybeArgs);

                                    }).apply(Optional.of(Destination.XQ), maybeValid);

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
        return "GetUserInfo";
    }

}
