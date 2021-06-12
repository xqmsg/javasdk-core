package com.xqmsg.sdk.v2.caching;

import com.xqmsg.sdk.v2.exceptions.HttpStatusCodes;
import com.xqmsg.sdk.v2.exceptions.StatusCodeException;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A simple out of the box cache implementation utilizing <a href="https://mapdb.org/">MapDB</a>
 */

public class SimpleXQCache implements XQCache {

  DB db;
  ConcurrentMap map;

  public SimpleXQCache() {
    db = DBMaker.fileDB("src/main/resources/SimpleXQCache.db").checksumHeaderBypass().closeOnJvmShutdown().make();
    map = db.hashMap("map").createOrOpen();

  }

  @Override
  public void putXQPreAuthToken(String user, String preAuthToken) {
    map.put(makeExchangeKey(user), preAuthToken);
}

  @Override
  public String getXQPreAuthToken(String user) {
    return (String) map.get(makeExchangeKey(user));
  }

  @Override
  public boolean removeXQPreAuthToken(String user) {
    String xqPreAuthToken = getXQPreAuthToken(user);
    if (xqPreAuthToken != null) {
      boolean success = map.remove(makeExchangeKey(user), xqPreAuthToken);
      return success;
    }
    return true;
  }

  @Override
  public void putXQAccess(String user, String accessToken) {
    map.put(makeXQAccessKey(user), accessToken);
}

  @Override
  public String getXQAccess(String user, boolean required) throws StatusCodeException {
    String accessToken = (String) map.get(makeXQAccessKey(user));
    if (required && Objects.isNull(accessToken)) {
      throw new StatusCodeException(HttpStatusCodes.HTTP_UNAUTHORIZED, String.format("%s [%s]", HttpStatusCodes.getMessageForCode(HttpStatusCodes.HTTP_UNAUTHORIZED), user));
    }
    return accessToken;
  }

  @Override
  public boolean removeXQAccess(String user) {
    String accessToken = null;
    try {
      accessToken = getXQAccess(user, false);
      if (accessToken != null) {
        boolean success = map.remove(user, accessToken);
        if (success) {
        }
        return success;
      }
    } catch (StatusCodeException e) {
    }
    return true;
  }

  @Override
  public void putDashboardAccess(String user, String accessToken) {
    map.put(makeDashboardAccessKey(user), accessToken);
  }

  @Override
  public String getDashboardAccess(String user, boolean required) throws StatusCodeException {
    String accessToken = (String) map.get(makeDashboardAccessKey(user));
    if (required && Objects.isNull(accessToken)) {
      throw new StatusCodeException(HttpStatusCodes.HTTP_UNAUTHORIZED, String.format("%s [%s]", HttpStatusCodes.getMessageForCode(HttpStatusCodes.HTTP_UNAUTHORIZED), user));
    }
    return accessToken;
  }

  @Override
  public boolean removeDashoardAccess(String user)  {
    String accessToken = null;
    try {
      accessToken = getDashboardAccess(user, false);
      if (accessToken != null) {
        boolean success = map.remove(user, accessToken);
        if (success) {
        }
        return success;
      }
    } catch (StatusCodeException e) { }
    return true;
  }

  @Override
  public boolean hasProfile(String user) {
    List<String> availableProfiles = (List) map.get(PROFILE_LIST_KEY);
    if (availableProfiles == null) {
      return false;
    }
    return availableProfiles.contains(user);
  }

  @Override
  public void putActiveProfile(String user) {
    List<String> availableProfiles = (List) map.get(PROFILE_LIST_KEY);

    if (availableProfiles == null) {
      map.put(PROFILE_LIST_KEY, List.of(user));
    } else {
      if(!availableProfiles.contains(user)) {
        List<String> merged = Stream.concat(availableProfiles.stream(), List.of(user).stream())
                .distinct()
                .collect(Collectors.toList());
        map.put(PROFILE_LIST_KEY, merged);
      }
    }
    map.put(ACTIVE_PROFILE_KEY, user);

  }


  @Override
  public String getActiveProfile(boolean required) throws StatusCodeException {

    String activeProfile = (String) map.get(ACTIVE_PROFILE_KEY);
    if (required && Objects.isNull(activeProfile)) {
      throw new StatusCodeException(HttpStatusCodes.HTTP_UNAUTHORIZED, String.format("%s [active profile missing]", HttpStatusCodes.getMessageForCode(HttpStatusCodes.HTTP_UNAUTHORIZED)));
    }
    return activeProfile;
  }

  @Override
  public void removeProfile(String user) {

    if (map == null) return;

    List<String> availableProfiles = (List) map.get(PROFILE_LIST_KEY);

    availableProfiles
            .stream()
            .dropWhile(profile -> profile.equals(user))
            .findFirst()
            .ifPresent(profile -> {
              map.remove(makeXQAccessKey(user));
              map.remove(makeExchangeKey(user));
          putActiveProfile(profile);
            });


  }

  @Override
  public void clearAllProfiles() {

    if (map == null) return;

    List<String> profiles = (List) map.get(PROFILE_LIST_KEY);
    if (profiles != null) {
      profiles.forEach(user -> {
        map.remove(makeExchangeKey(user));
        map.remove(makeXQAccessKey(user));
        map.remove(makeDashboardAccessKey(user));
      });

      map.remove(ACTIVE_PROFILE_KEY);
      map.remove(PROFILE_LIST_KEY);

  }
  }

  @Override
  public List<String> listProfiles() {
    List<String> availableProfiles = (List) map.get(PROFILE_LIST_KEY);

    if (!Objects.isNull(availableProfiles)) {
      return availableProfiles;
    }

    return Collections.emptyList();
  }

  private String makeExchangeKey(String unvalidatedUser){return String.format("%s-%s-%s", EXCHANGE_PREFIX, XQ_PREFIX, unvalidatedUser);}
  private String makeXQAccessKey(String validatedUser){return String.format("%s-%s", XQ_PREFIX, validatedUser);}
  private String makeDashboardAccessKey(String validatedUser){return String.format("%s-%s", DASHBOARD_PREFIX, validatedUser);}

}
