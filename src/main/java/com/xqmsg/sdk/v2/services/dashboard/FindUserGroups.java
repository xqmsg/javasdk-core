package com.xqmsg.sdk.v2.services.dashboard;

import com.xqmsg.sdk.v2.*;
import com.xqmsg.sdk.v2.utils.Destination;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 *
 */
public class FindUserGroups extends XQModule {

    private final Logger logger = Logger.getLogger(getClass().getName(), null);

    public static final String GROUPS = "groups";
    public static final String ID = "id";

    private static final String SERVICE_NAME = "usergroup";

    private FindUserGroups(XQSDK sdk) {
        assert sdk != null : "An instance of the XQSDK is required";
        super.sdk = sdk;
        super.cache = sdk.getCache();
    }

    /**
     * @param sdk App Settings
     * @returns FindUserGroups
     */
    public static FindUserGroups with(XQSDK sdk) {
        return new FindUserGroups(sdk);
    }

    @Override
    public List<String> requiredFields() {
        return List.of();
    }

    /**
     * @param maybeArgs Map of request parameters supplied to this method.
     *                  <pre>parameter details:<br>
     *                                                    String user! - Email of the user to be authorized.<br>
     *                                                    String firstName?  - First name of the user.<br>
     *                                                    String lastName? - Last name of the user.<br>
     *                                                    Boolean newsLetter? [false] - Should the user receive a newsletter.<br>
     *                                                    NotificationEnum notifications? [0] - Enum Value to specify Notification Settings.<br>
     *                                                    </pre>
     * @returns CompletableFuture&lt;ServerResponse#payload:{data:String}>>
     * @apiNote !=required ?=optional [...]=default {...} map
     */
    @Override
    public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

        try {
            return CompletableFuture.completedFuture(validate
                    .andThen((maybeValid) -> {
                        try {
                            return authorize
                                    .andThen((dashboardAccessToken) -> {
                                                Map<String, String> headerProperties = Map.of("Authorization", String.format("Bearer %s", dashboardAccessToken));
                                                return sdk.call(sdk.DASHBOARD_SERVER_URL,
                                                        Optional.of(SERVICE_NAME),
                                                        CallMethod.Get,
                                                        Optional.of(headerProperties),
                                                        Optional.of(Destination.DASHBOARD),
                                                        Optional.empty());
                                            }
                                    ).apply(Optional.of(Destination.DASHBOARD), maybeValid);

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
        return "FindUserGroups";
    }

}
