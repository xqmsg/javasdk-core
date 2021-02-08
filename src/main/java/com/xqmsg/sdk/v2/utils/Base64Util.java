package com.xqmsg.sdk.v2.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public  class Base64Util {

  public final String publicKey;
  public final String secretKey;
  private static final int MIME_LINE_MAX = Integer.MAX_VALUE;
  private static final byte[] CRLF = new byte[]{'\r', '\n'};

  public Base64Util(String aPublicKey, String aSecretKey) {
    publicKey = aPublicKey;
    secretKey = aSecretKey;
  }

  public static String encodeToString(byte[] bytes) {
    return Base64.getMimeEncoder(MIME_LINE_MAX, CRLF).encodeToString(bytes);
  }

  public static String decodeToString(String string) {
    return new String(Base64.getMimeDecoder().decode(string.getBytes(StandardCharsets.UTF_8)));
  }

  public static String encodeToString(String string) {
    return Base64.getMimeEncoder(MIME_LINE_MAX, CRLF).encodeToString(string.getBytes(StandardCharsets.UTF_8));
  }

  public static String decodeToString(byte[] bytes) {
    return new String(Base64.getMimeDecoder().decode(bytes));
  }

  public static byte[] encode(String string) {
    return Base64.getMimeEncoder(MIME_LINE_MAX, CRLF).encode(string.getBytes(StandardCharsets.UTF_8));
  }

  public static byte[] encode(byte[] bytes) {
    return Base64.getMimeEncoder(MIME_LINE_MAX, CRLF).encode(bytes);
  }

  public static String decode(byte[] bytes) {
    return new String(Base64.getMimeDecoder().decode(bytes));
  }

  public static String decode(String string) {
    return new String(Base64.getMimeDecoder().decode(string.getBytes(StandardCharsets.UTF_8)));
  }

  public static byte[] decodeToBytes(byte[] bytes) {
    return Base64.getMimeDecoder().decode(bytes);
  }

  public static byte[] decodeToBytes(String string) {
    return Base64.getMimeDecoder().decode(string.getBytes(StandardCharsets.UTF_8));
  }


}