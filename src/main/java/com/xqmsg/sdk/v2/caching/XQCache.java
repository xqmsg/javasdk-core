package com.xqmsg.sdk.v2.caching;

import com.xqmsg.sdk.v2.exceptions.StatusCodeException;

import java.util.List;

public interface XQCache {

  String XQ_PREFIX = "xq";
  String DASHBOARD_PREFIX = "dsb";
  String EXCHANGE_PREFIX = "exchange";
  String PROFILE_LIST_KEY = "available-profiles";
  String ACTIVE_PROFILE_KEY = "active-profile";

  /**
   * @param user
   * @param preAuthToken
   */
  void putXQPreAuthToken(String user, String preAuthToken);

  /**
   * @param user
   * @return
   */
  String getXQPreAuthToken(String user);

  /**
   * @param user
   */
  boolean removeXQPreAuthToken(String user);

  /**
   * @param user
   * @param accessToken
   */
  void putXQAccess(String user, String accessToken);

 /**
   * @param user
   * @param accessToken
   */
  void putDashboardAccess(String user, String accessToken);

  /**
   * @param user
   * @param required
   * @return
   * @throws StatusCodeException
   */
  String getXQAccess(String user, boolean required) throws StatusCodeException;

  /**
   * @param user
   * @param required
   * @return
   * @throws StatusCodeException
   */
  String getDashboardAccess(String user, boolean required) throws StatusCodeException;

  /**
   * @param user
   */
  boolean removeXQAccess(String user) ;

 /**
   * @param user
   */
  boolean removeDashoardAccess(String user) ;

  /**
   * @param user
   * @return
   */
  boolean hasProfile(String user);

  /**
   * @param user
   */
  void putActiveProfile(String user);

  /**
   * @param user
   */
  void putProfile(String user);

  /**
   * @param required
   * @return
   * @throws StatusCodeException
   */
  String getActiveProfile(boolean required) throws StatusCodeException;

  /**
   * @param user
   */
  void removeProfile(String user);

  void clearAllProfiles();

  /**
   * @return
   */
  List<String> listProfiles();

}