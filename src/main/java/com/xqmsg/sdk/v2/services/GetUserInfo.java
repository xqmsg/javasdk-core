package com.xqmsg.sdk.v2.services;

import com.xqmsg.sdk.v2.*;
import com.xqmsg.sdk.v2.utils.Destination;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/*** Get ser information.
 */
public class GetUserInfo extends XQModule {

    private static final Logger logger = Logger(GetUserInfo.class);

    public static final String ID = "id";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String USER = "user";
    public static final String SUBSCRIPTION_STATUS = "sub";
    public static final String STARTS = "starts";
    public static final String ENDS = "ends";

    private static final String SERVICE_NAME = "subscriber";

    private GetUserInfo(XQSDK sdk) {
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
    public static GetUserInfo with(XQSDK sdk) {
        return new GetUserInfo(sdk);
    }

    /**
     * @param maybeArgs Map of request parameters supplied to this method.
     *                  <pre>parameter details:none</pre>
     * @returns CompletableFuture&lt;ServerResponse#payload:{id:long, usr:string, firstName:string, sub:string, starts:long, ends:Long}>
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
