package com.xqmsg.sdk.v2.services.dashboard;

import com.xqmsg.sdk.v2.*;
import com.xqmsg.sdk.v2.caching.XQCache;
import com.xqmsg.sdk.v2.services.CodeValidator;
import com.xqmsg.sdk.v2.utils.Destination;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class DashboardLogin extends XQModule {

    private final Logger logger = Logger.getLogger(getClass().getName(), null);
    private static final String SERVICE_NAME = "login/verify";

    private DashboardLogin(XQSDK sdk) {
        assert sdk != null : "An instance of the XQSDK is required";
        super.sdk = sdk;
        super.cache = sdk.getCache();
    }

    /**
     * @param sdk App Settings
     * @returns AddContact
     */
    public static DashboardLogin with(XQSDK sdk) {
        return new DashboardLogin(sdk);
    }

    @Override
    public List<String> requiredFields() {
        return List.of();
    }

    /**
     * @param maybeArgs Map of request parameters supplied to this method.
     *                  <pre>parameter details:<br>
     *                                                                     String user! - Email of the user to be authorized.<br>
     *                                                                     String firstName?  - First name of the user.<br>
     *                                                                     String lastName? - Last name of the user.<br>
     *                                                                     Boolean newsLetter? [false] - Should the user receive a newsletter.<br>
     *                                                                     NotificationEnum notifications? [0] - Enum Value to specify Notification Settings.<br>
     *                                                                     </pre>
     * @returns CompletableFuture&lt;ServerResponse#payload:{data:String}>>
     * @apiNote !=required ?=optional [...]=default {...} map
     */
    @Override
    public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

        return CompletableFuture.completedFuture(
                authorize
                        .andThen((xqAccessToken) -> {
                                    Map<String, String> headerProperties = Map.of("Authorization", String.format("Bearer %s", xqAccessToken));
                                    return sdk.call(sdk.DASHBOARD_SERVER_URL,
                                            Optional.of(SERVICE_NAME),
                                            CallMethod.Get,
                                            Optional.of(headerProperties),
                                            Optional.of(Destination.DASHBOARD),
                                            Optional.of((Map.of("request", "sub"))));
                                }
                        ).apply(Optional.of(Destination.DASHBOARD), maybeArgs)
        )
                .thenApply((ServerResponse serverResponse) -> {
                    switch (serverResponse.status) {
                        case Ok: {
                            String dashboardAccessToken = (String) serverResponse.payload.get(ServerResponse.DATA);
                            try {
                                String activeProfile = cache.getActiveProfile(true);
                                cache.putDashboardAccess(activeProfile, dashboardAccessToken);
                                return serverResponse;
                            } catch (Exception e) {
                                return null;
                            }
                        }
                        default: {
                            return serverResponse;
                        }
                    }
                })

                .exceptionally(e -> new ServerResponse(CallStatus.Error, Reasons.MissingParameters, e.getMessage()));

    }

    @Override
    public String moduleName() {
        return "DashboardLogin";
    }

}
