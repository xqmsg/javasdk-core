package com.xqmsg.sdk.v2;


import com.xqmsg.sdk.v2.caching.XQCache;
import com.xqmsg.sdk.v2.exceptions.HttpStatusCodes;
import com.xqmsg.sdk.v2.exceptions.StatusCodeException;
import com.xqmsg.sdk.v2.utils.Destination;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static java.util.Arrays.asList;

/**
 * Created by ikechie on 2/3/20.
 */

public abstract class XQModule {

    protected XQSDK sdk;
    protected XQCache cache;

    public abstract List<String> requiredFields();

    public abstract String moduleName();

    public abstract CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> args);

    protected ServerResponse unwrapException(RuntimeException runtimeException, CallStatus status, Reasons reason){
        String message = null;
        if(runtimeException.getCause() instanceof StatusCodeException){
            message = ((StatusCodeException)runtimeException.getCause()).statusMessage();
        }else{
            message = runtimeException.getMessage();
        }
        return new ServerResponse(status,reason,message);
    }

    protected static <T> Logger Logger(Class<T> clazz) {
        try {
            LogManager.getLogManager().readConfiguration(clazz.getClassLoader().getResourceAsStream("test-logging.properties"));
            return Logger.getLogger(clazz.getName());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected Function<Optional<Map<String, Object>>, Optional<Map<String, Object>>> validate =
            (Optional<Map<String, Object>> maybeArgs) -> {

                if (requiredFields().size() == 0) {
                    return maybeArgs;
                }

                if (maybeArgs.isEmpty()) {
                    throw new RuntimeException("Required: " + requiredFields().toString());
                }
                HashSet<String> missing = new HashSet<>(requiredFields());
                HashSet<String> input = new HashSet<>(maybeArgs.get().keySet());

                missing.removeAll(asList(input.toArray()));

                if (missing.size() > 0) {
                    throw new RuntimeException("", new StatusCodeException(HttpStatusCodes.HTTP_UNAUTHORIZED, "missing " + missing + "!"));
                }
                return maybeArgs;

            };

    protected Function<Optional<Map<String, Object>>, String> preAuthorize =
            (Optional<Map<String, Object>> maybeArgs) -> {

                // Ensure that there is an active profile.
                String activeProfile = null;
                try {
                    activeProfile = cache.getActiveProfile(true);
                } catch (StatusCodeException e) {
                    throw new RuntimeException("", e);
                }

                String temporaryAccessToken = cache.getXQPreAuthToken(activeProfile);

                if (temporaryAccessToken == null) {
                    throw new RuntimeException("", new StatusCodeException(HttpStatusCodes.HTTP_UNAUTHORIZED));
                }
                return temporaryAccessToken;

            };


    protected BiFunction<Optional<Destination>, Optional<Map<String, Object>>, String> authorize =
            (Optional<Destination> maybeDestination, Optional<Map<String, Object>> maybeArgs) -> {

                // Ensure that there is an active profile.
                String activeProfile = null;
                String authorizationToken = null;
                try {
                    activeProfile = cache.getActiveProfile(true);

                    Destination destination = maybeDestination.orElse(Destination.XQ);
                    switch (destination) {
                        case XQ: {
                            authorizationToken = cache.getXQAccess(activeProfile, true);
                            break;
                        }
                        case DASHBOARD: {
                            authorizationToken = cache.getDashboardAccess(activeProfile, true);
                            break;
                        }
                    }
                } catch (StatusCodeException e) {
                    e.printStackTrace();
                    throw new RuntimeException("", e);
                }

                if (authorizationToken == null) {
                    throw new RuntimeException("", new StatusCodeException(HttpStatusCodes.HTTP_UNAUTHORIZED));
                }
                return authorizationToken;


            };

}
