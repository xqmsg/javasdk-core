package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.*;
import com.xqmsg.sdk.v2.utils.Destination;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;


public class ValidatePacket extends XQModule {

    private static final Logger logger = Logger(ValidatePacket.class);

    public static final String PACKET = "data";

    private static final String SERVICE_NAME = "packet";

    private ValidatePacket(XQSDK sdk) {
        assert sdk != null : "An instance of the XQSDK is required";
        super.sdk = sdk;
        super.cache = sdk.getCache();
    }


    @Override
    public List<String> requiredFields() {
        return List.of(PACKET);
    }

    /**
     * @param sdk App Settings
     * @returns this
     */
    public static ValidatePacket with(XQSDK sdk) {
        return new ValidatePacket(sdk);
    }

    /**
     * @param maybeArgs Map of request parameters supplied to this method.
     *                  <pre>parameter details:none</pre>
     * @returns CompletableFuture&lt;ServerResponse#payload:{data:{}}>
     * @apiNote !=required ?=optional [...]=default {...} map
     */
    @Override
    public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

        try {
            return CompletableFuture.completedFuture(validate
                    .andThen((maybeValid) -> {
                        try {
                            return authorize
                                    .andThen((authorizationToken) ->
                                            sdk.call(sdk.VALIDATION_SERVER_URL,
                                                    Optional.of(SERVICE_NAME),
                                                    CallMethod.Post,
                                                    Optional.of(Map.of("Authorization", String.format("Bearer %s", authorizationToken),
                                                            XQSDK.CONTENT_TYPE, XQSDK.TEXT_PLAIN_UTF_8)),
                                                    Optional.of(Destination.XQ),
                                                    maybeArgs)
                                    ).apply(Optional.of(Destination.XQ), maybeValid);

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
        return "ValidatePacket";
    }

}
