package com.xqmsg.sdk.v2.utils;

import java.util.Base64;

public class SimpleKeyPair {

  public final String publicKey;
  public final String secretKey;
  private static final int MIME_LINE_MAX = Integer.MAX_VALUE;
  private static final byte[] CRLF = new byte[]{'\r', '\n'};

  public SimpleKeyPair(String aPublicKey, String aSecretKey) {
    publicKey = aPublicKey;
    secretKey = aSecretKey;
  }

  public SimpleKeyPair(byte[] publicKey, byte[] secretKey) {
    this(
            Base64.getMimeEncoder(MIME_LINE_MAX, CRLF).encodeToString(publicKey),
            Base64.getMimeEncoder(MIME_LINE_MAX, CRLF).encodeToString(secretKey)
    );
  }


}