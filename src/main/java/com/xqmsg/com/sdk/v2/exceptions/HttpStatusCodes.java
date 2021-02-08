package com.xqmsg.com.sdk.v2.exceptions;

import java.util.HashMap;

public class HttpStatusCodes {

  // [Informational 1xx]
  public static final int HTTP_CONTINUE = 100;
  public static final int HTTP_SWITCHING_PROTOCOLS = 101;

  // [Successful 2xx]
  public static final int HTTP_OK = 200;
  public static final int HTTP_CREATED = 201;
  public static final int HTTP_ACCEPTED = 202;
  public static final int HTTP_NONAUTHORITATIVE_INFORMATION = 203;
  public static final int HTTP_NO_CONTENT = 204;
  public static final int HTTP_RESET_CONTENT = 205;
  public static final int HTTP_PARTIAL_CONTENT = 206;

  // [Redirection 3xx]
  public static final int HTTP_MULTIPLE_CHOICES = 300;
  public static final int HTTP_MOVED_PERMANENTLY = 301;
  public static final int HTTP_FOUND = 302;
  public static final int HTTP_SEE_OTHER = 303;
  public static final int HTTP_NOT_MODIFIED = 304;
  public static final int HTTP_USE_PROXY = 305;
  public static final int HTTP_UNUSED = 306;
  public static final int HTTP_TEMPORARY_REDIRECT = 307;

  // [Client Error 4xx]
  public static final int interrorCodesBeginAt = 400;
  public static final int HTTP_BAD_REQUEST = 400;
  public static final int HTTP_UNAUTHORIZED = 401;
  public static final int HTTP_PAYMENT_REQUIRED = 402;
  public static final int HTTP_FORBIDDEN = 403;
  public static final int HTTP_NOT_FOUND = 404;
  public static final int HTTP_METHOD_NOT_ALLOWED = 405;
  public static final int HTTP_NOT_ACCEPTABLE = 406;
  public static final int HTTP_PROXY_AUTHENTICATION_REQUIRED = 407;
  public static final int HTTP_REQUEST_TIMEOUT = 408;
  public static final int HTTP_CONFLICT = 409;
  public static final int HTTP_GONE = 410;
  public static final int HTTP_LENGTH_REQUIRED = 411;
  public static final int HTTP_PRECONDITION_FAILED = 412;
  public static final int HTTP_REQUEST_ENTITY_TOO_LARGE = 413;
  public static final int HTTP_REQUEST_URI_TOO_LONG = 414;
  public static final int HTTP_UNSUPPORTED_MEDIA_TYPE = 415;
  public static final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
  public static final int HTTP_EXPECTATION_FAILED = 417;

  // [Server Error 5xx]
  public static final int HTTP_INTERNAL_SERVER_ERROR = 500;
  public static final int HTTP_NOT_IMPLEMENTED = 501;
  public static final int HTTP_BAD_GATEWAY = 502;
  public static final int HTTP_SERVICE_UNAVAILABLE = 503;
  public static final int HTTP_GATEWAY_TIMEOUT = 504;
  public static final int HTTP_VERSION_NOT_SUPPORTED = 505;

  static HashMap<Integer, String> messages = new HashMap();

  static {
    messages.put(0, "Application Exception");
    messages.put(100, "100 Continue");
    messages.put(101, "101 Switching Protocols");

    messages.put(200, "200 OK");
    messages.put(201, "201 Created");
    messages.put(202, "202 Accepted");
    messages.put(203, "203 Non-Authoritative Information");
    messages.put(204, "204 No Content");
    messages.put(205, "205 Reset Content");
    messages.put(206, "206 Partial Content");

    messages.put(300, "300 Multiple Choices");
    messages.put(301, "301 Moved Permanently");
    messages.put(302, "302 Found");
    messages.put(303, "303 See Other");
    messages.put(304, "304 Not Modified");
    messages.put(305, "305 Use Proxy");
    messages.put(306, "306 (Unused)");
    messages.put(307, "307 Temporary Redirect");

    messages.put(400, "400 Bad Request");
    messages.put(401, "401 Unauthorized");
    messages.put(402, "402 Payment Required");
    messages.put(403, "403 Forbidden");
    messages.put(404, "404 Not Found");
    messages.put(405, "405 Method Not Allowed");
    messages.put(406, "406 Not Acceptable");
    messages.put(407, "407 Proxy Authentication Required");
    messages.put(408, "408 Request Timeout");
    messages.put(409, "409 Conflict");
    messages.put(410, "410 Gone");
    messages.put(411, "411 Length Required");
    messages.put(412, "412 Precondition Failed");
    messages.put(413, "413 Request Entity Too Large");
    messages.put(414, "414 Request-URI Too Long");
    messages.put(415, "415 Unsupported Media Type");
    messages.put(416, "416 Requested Range Not Satisfiable");
    messages.put(417, "417 Expectation Failed");

    messages.put(500, "500 Internal Server Error");
    messages.put(501, "501 Not Implemented");
    messages.put(502, "502 Bad Gateway");
    messages.put(503, "503 Service Unavailable");
    messages.put(504, "504 Gateway Timeout");
    messages.put(505, "HTTP Version Not Supported");
  }

  public static String httpHeaderFor(int code) {
    return "HTTP/1.1 " + messages.get(code);
  }

  public static String getMessageForCode(int code) {
    return messages.get(code);
  }

  public static boolean isError(int code) {
    return code >= HttpStatusCodes.HTTP_BAD_REQUEST;
  }

  public static boolean canHaveBody(int code) {

    return
            // True if not in 100s
            (code < HttpStatusCodes.HTTP_CONTINUE || code >= HttpStatusCodes.HTTP_OK)
                    && // and not 204 NO CONTENT
                    code != HttpStatusCodes.HTTP_NO_CONTENT
                    && // and not 304 NOT MODIFIED
                    code != HttpStatusCodes.HTTP_NOT_MODIFIED;
  }


}
