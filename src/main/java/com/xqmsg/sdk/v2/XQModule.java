package com.xqmsg.sdk.v2;


import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static java.util.Arrays.asList;

/**
 * Created by ikechie on 2/3/20.
 */

public abstract class XQModule {

    public abstract List<String> requiredFields();

    public abstract String moduleName();

    public abstract CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> args);

    protected CompletableFuture<Optional<Map<String, Object>>> validateInput(Optional<Map<String, Object>> maybeArgs) {
        return CompletableFuture.supplyAsync(() -> {
            if(requiredFields().size()==0){
                return maybeArgs;
            }
            if (  maybeArgs.isEmpty()) {
                throw new RuntimeException("Required: " + requiredFields().toString());
            }
            HashSet<String> missing = new HashSet<>(requiredFields());
            HashSet<String> input = new HashSet<>(maybeArgs.get().keySet());

            missing.removeAll(asList(input.toArray()));

            if (missing.size() > 0) {
                throw new RuntimeException("missing " + missing + "!");
            }
            return maybeArgs;
        });
    }

    protected  static <T> Logger Logger(Class<T> clazz){
        try {
            LogManager.getLogManager().readConfiguration(clazz.getClassLoader().getResourceAsStream("test-logging.properties"));
            return Logger.getLogger(clazz.getName());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}
