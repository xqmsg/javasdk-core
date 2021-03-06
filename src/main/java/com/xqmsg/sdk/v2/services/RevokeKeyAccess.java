package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.*;
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
 * Revokes a key using its token.
 * Only the user who sent the message will be able to revoke it.
 */
public class RevokeKeyAccess extends XQModule {

    private static final Logger logger = Logger(RevokeKeyAccess.class);

    public static final String LOCATOR_TOKEN = "locatorToken";

    private static final String SERVICE_NAME = "key";

    private RevokeKeyAccess(XQSDK sdk) {
        assert sdk != null : "An instance of the XQSDK is required";
        super.sdk = sdk;
        super.cache = sdk.getCache();

    }

    @Override
    public List<String> requiredFields() {
        return List.of(LOCATOR_TOKEN);
    }

    /**
     * @param sdk App Settings
     * @returns this
     */
    public static RevokeKeyAccess with(XQSDK sdk) {
        return new RevokeKeyAccess(sdk);
    }

    /**
     * @param maybeArgs Map of request parameters supplied to this method.
     *                  <pre>parameter details:<br>
     *                  String locatorToken! - The locator token used as a URL to discover the key on the server.<br>
     *                                         The URL encoding part is handled internally in the service itself.
     *                  </pre>
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
                                    .andThen((authorizationToken) -> {

                                        Map<String, Object> args = maybeArgs.get();

                                        String locatorToken = (String) args.get(LOCATOR_TOKEN);

                                        final String DYNAMIC_SERVICE_NAME = String.format("%s/%s", SERVICE_NAME, encode(locatorToken));

                                        return sdk.call(sdk.VALIDATION_SERVER_URL,
                                                Optional.of(DYNAMIC_SERVICE_NAME),
                                                CallMethod.Delete,
                                                Optional.of(Map.of("Authorization", String.format("Bearer %s", authorizationToken))),
                                                Optional.of(Destination.XQ),
                                                Optional.of(Map.of()));
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
        return "RevokeKeyAccess";
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

}
