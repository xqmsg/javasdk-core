package com.xqmsg.sdk.v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.xqmsg.sdk.v2.algorithms.AESEncryption;
import com.xqmsg.sdk.v2.algorithms.OTPv2Encryption;
import com.xqmsg.sdk.v2.algorithms.XQAlgorithm;
import com.xqmsg.sdk.v2.caching.SimpleXQCache;
import com.xqmsg.sdk.v2.caching.XQCache;
import com.xqmsg.sdk.v2.utils.Destination;
import com.xqmsg.sdk.v2.utils.XQMsgJSONTypeAdapter;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class XQSDK {

  private final Logger logger = Logger.getLogger(getClass().getName(), null);

  public static final String API_KEY = "api-key";
  public static final String CONTENT_TYPE = "content-type";
  public static final String APPLICATION_JSON = "application/json";
  public static final String TEXT_PLAIN_UTF_8 = "text/plain;charset=UTF-8";

  public final String XQ_APPLICATION_KEY;
  public final String DASHBOARD_APPLICATION_KEY;

  public URL DASHBOARD_SERVER_URL;
  public URL SUBSCRIPTION_SERVER_URL;
  public URL VALIDATION_SERVER_URL;
  public URL KEY_SERVER_URL;

  private final HashMap<AlgorithmEnum, XQAlgorithm> ALGORITHMS;

  private final XQCache cache;

  public XQSDK() {

    Properties  applicationProperties = getApplicationProperties();

    String cachingMechanism = applicationProperties.getProperty("com.xq-msg.sdk.v2.cache-key");

    try {

      DASHBOARD_SERVER_URL = new URL(applicationProperties.getProperty("com.xq-msg.sdk.v2.dashboard-server-url"));
      SUBSCRIPTION_SERVER_URL = new URL(applicationProperties.getProperty("com.xq-msg.sdk.v2.subscription-server-url"));
      VALIDATION_SERVER_URL = new URL(applicationProperties.getProperty("com.xq-msg.sdk.v2.validation-server-url"));
      KEY_SERVER_URL = new URL(applicationProperties.getProperty("com.xq-msg.sdk.v2.key-server-url"));

      cache = ((Constructor<SimpleXQCache>)
               Class.forName(cachingMechanism)
                    .getDeclaredConstructor())
                    .newInstance();

    } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException | MalformedURLException e) {
      e.printStackTrace();
      throw new RuntimeException(String.format("Fatal Configuration Exception %s ", e.getMessage()), e);
    }

    XQ_APPLICATION_KEY = applicationProperties.getProperty("com.xq-msg.sdk.v2.xq-api-key");
    DASHBOARD_APPLICATION_KEY = applicationProperties.getProperty("com.xq-msg.sdk.v2.dashboard-api-key");

    ALGORITHMS = new HashMap<>();
    ALGORITHMS.put(AlgorithmEnum.AES, new AESEncryption());
    ALGORITHMS.put(AlgorithmEnum.OTPv2, new OTPv2Encryption());

  }

  public ServerResponse call(URL baseUrl,
                             Optional<String> mayBeService,
                             CallMethod callMethod,
                             Optional<Map<String, String>> maybeHeaderProperties,
                             Optional<Destination> maybeDestination,
                             Optional<Map<String, Object>> maybePayload) {

    assert baseUrl != null : "baseUrl cannot be null";
    assert callMethod != null : "method cannot be null";

    if (maybePayload.isPresent() && List.of(CallMethod.Post,  CallMethod.Patch, CallMethod.Options).contains(callMethod)) {
      try {
        return makeBodyRequest(baseUrl, callMethod, mayBeService, maybeDestination, maybeHeaderProperties, maybePayload);
      } catch (IOException e) {
        return new ServerResponse(CallStatus.Error, Reasons.LocalException, e.getLocalizedMessage());
      }
    } else {
      try {
        return makeParamRequest(baseUrl, callMethod, mayBeService, maybeDestination, maybeHeaderProperties, maybePayload);
      } catch (IOException e) {
        return new ServerResponse(CallStatus.Error, Reasons.LocalException, e.getLocalizedMessage());
      }
    }
  }


  private ServerResponse makeBodyRequest(URL baseUrl, CallMethod callMethod, Optional<String> maybeService, Optional<Destination> maybeDestination, Optional<Map<String, String>> maybeHeaderProperties, Optional<Map<String, Object>> maybePayload) throws IOException {

    final String spec = String.format("%s%s", baseUrl, maybeService.map(service -> "/" + service).orElse(""));
    final URL url = new URL(spec);

    final HttpsURLConnection httpsConnection = (HttpsURLConnection) url.openConnection();

    Destination destination = maybeDestination.orElse(Destination.XQ);
      switch (destination) {
        case XQ: {
          httpsConnection.setRequestProperty(XQSDK.API_KEY, XQ_APPLICATION_KEY);
          break;
        }
        case DASHBOARD: {
          httpsConnection.setRequestProperty(XQSDK.API_KEY, DASHBOARD_APPLICATION_KEY);
          break;
        }
      }

    switch (callMethod) {
      case Patch:
      case Post: {
        httpsConnection.setRequestMethod(CallMethod.Post.name().toUpperCase());
        break;
      }
      default: {
        httpsConnection.setRequestMethod(callMethod.name().toUpperCase());
        break;
      }
    }


    httpsConnection.setDoOutput(true);

    try {

      maybeHeaderProperties.ifPresentOrElse(
              (headerProperties) -> {
                headerProperties.forEach(httpsConnection::setRequestProperty);
                if (!headerProperties.containsKey(XQSDK.CONTENT_TYPE)) {
                  httpsConnection.setRequestProperty(XQSDK.CONTENT_TYPE, APPLICATION_JSON);
                }
              },
              () -> {
                httpsConnection.setRequestProperty(XQSDK.CONTENT_TYPE, APPLICATION_JSON);
              }
      );

      writeToOutputStream(maybePayload.orElseGet(Collections::emptyMap), httpsConnection);

      Map<String, Object> response = receiveData(httpsConnection);

      return convertToServerResponse(response);

    } catch (Exception e) {
      String errorMessage = e.getMessage();
      logger.warning(errorMessage);
      return new ServerResponse(CallStatus.Error, Reasons.LocalException, errorMessage);
    } finally {
      httpsConnection.disconnect();
    }
  }


  private ServerResponse makeParamRequest(URL baseUrl, CallMethod callMethod, Optional<String> maybeService, Optional<Destination> maybeDestination, Optional<Map<String, String>> maybeHeaderProperties, Optional<Map<String, Object>> maybePayload) throws IOException {

    String spec = String.format("%s%s%s",
            baseUrl, maybeService.map(service -> "/" + service).orElse(""),
            maybePayload.map(payload -> "?" + buildQeryParams(payload)).orElse(""));

    URL url = new URL(spec);

    HttpsURLConnection httpsConnection = (HttpsURLConnection) url.openConnection();

    try {

      Destination destination = maybeDestination.orElse(Destination.XQ);
      switch (destination) {
        case XQ: {
          httpsConnection.setRequestProperty(XQSDK.API_KEY, XQ_APPLICATION_KEY);
          break;
        }
        case DASHBOARD: {
          httpsConnection.setRequestProperty(XQSDK.API_KEY, DASHBOARD_APPLICATION_KEY);
          break;
        }
      }
      httpsConnection.setRequestMethod(callMethod.name().toUpperCase());

      maybeHeaderProperties.ifPresentOrElse(
              (headerProperties) -> {
                headerProperties.forEach(httpsConnection::setRequestProperty);
                if (!headerProperties.containsKey(XQSDK.CONTENT_TYPE)) {
                  httpsConnection.setRequestProperty(XQSDK.CONTENT_TYPE, APPLICATION_JSON);
                }
              },
              () -> {
                httpsConnection.setRequestProperty(XQSDK.CONTENT_TYPE, APPLICATION_JSON);
              }
      );

      Map<String, Object> response = receiveData(httpsConnection);

      logger.info(String.format("Server Response: %s ", response));
      return convertToServerResponse(response);


    } catch (Exception e) {
      String errorMessage = e.getMessage();
      logger.warning(errorMessage);
      return new ServerResponse(CallStatus.Error, Reasons.LocalException, errorMessage);
    } finally {
      httpsConnection.disconnect();
    }
  }


  public XQAlgorithm getAlgorithm(AlgorithmEnum algorithm) {
    if (!ALGORITHMS.containsKey(algorithm)) return null;
    return ALGORITHMS.get(algorithm);
  }

  /**
   * Loads the application properties for this application as specified in a file named `config.properties`.
   * If a `mode` vm argument was specified it will be prepended to the filename followed by a hyphen.
   * Example: `-Dmode=dev` will cause the app to read properties from `dev-config.properties`.
   *
   * @return
   */
  private Properties getApplicationProperties() {

    Properties applicationProperties = new Properties();

    Properties vmArgs = new Properties();
    ManagementFactory.getRuntimeMXBean()
            .getInputArguments()
            .forEach(vmArg -> {
              if (vmArg.contains("=")) {
                try {
                  if (vmArg.startsWith("-D")) {
                    vmArg = vmArg.substring(2);
                  } else if (vmArg.startsWith("-")) {
                    vmArg = vmArg.substring(1);
                  }
                  vmArgs.load(new StringReader(vmArg));
                } catch (IOException ex) {
                  ex.printStackTrace();
                }
              }
            });

    String configFilename = String.format("%sconfig.properties", vmArgs.getProperty("mode") != null ? vmArgs.getProperty("mode") + "-" : "");

    try (InputStream input = XQSDK.class.getClassLoader().getResourceAsStream(configFilename)) {
      if (input == null) {
        throw new FileNotFoundException("Sorry, unable to find " + configFilename + " in the `resources` folder");
      }
      applicationProperties.load(input);
    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return applicationProperties;
  }

  public static String shuffle(String array) {

    byte[] shuffled = array.getBytes();

    for (int i = 0; i < shuffled.length; i++) {
      int r = i + (int) (Math.random() * (shuffled.length - i));
      byte temp = shuffled[r];
      shuffled[r] = shuffled[i];
      shuffled[i] = temp;
    }
    return new String(shuffled);
  }

  private String buildQeryParams(Map<String, Object> params) {
    return params.entrySet().stream()
            .map(p -> p.getKey() + "=" + encode((String)p.getValue()))
            .reduce((p1, p2) -> p1 + "&" + p2)
            .orElse("");
  }

  private void writeToOutputStream(Map payload, HttpsURLConnection httpsConnection) throws IOException {
    String dataString = null;
    if (APPLICATION_JSON.equals(httpsConnection.getRequestProperty(XQSDK.CONTENT_TYPE))) {
      dataString = new Gson().toJson(payload);
    } else {
      dataString = (String) payload.get(ServerResponse.DATA);
    }
    try (OutputStream os = httpsConnection.getOutputStream()) {
      byte[] input = dataString.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }
  }

  private Map<String, Object> receiveData(HttpsURLConnection httpConnection) throws IOException {

    BufferedInputStream is = null;

    if (httpConnection.getResponseCode() < 200 | httpConnection.getResponseCode() > 299) {
      is = new BufferedInputStream(httpConnection.getErrorStream());
    } else {
      is = new BufferedInputStream(httpConnection.getInputStream());
    }
    List<String> prefixes = ALGORITHMS.values().stream().map(a -> a.prefix()).collect(Collectors.toList());
    StringBuilder responseBuilder = null;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      responseBuilder = new StringBuilder();
      String responseLine = null;
      int l = 0;
      boolean appendNewLine = false;
      while ((responseLine = br.readLine()) != null) {
        responseBuilder.append(responseLine.trim());
        if (l == 0 && prefixes.contains(responseLine.substring(0, 2))) {
          appendNewLine = true;
        }
        if (appendNewLine) {
          responseBuilder.append("\n");
        }
        ++l;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    String responseString = responseBuilder.toString().trim();

    if (httpConnection.getResponseCode() < 200 | httpConnection.getResponseCode() > 299) {
      throw new IOException(responseString);
    } else {
      switch (responseString) {
        case "":
          responseString = "{status:\"OK\", data:\"No Content\"}";
          break;
        default: {
          if (!responseString.contains("status")) {
            responseString = String.format("{status:\"OK\", data:\"%s\"}", responseString);
          }
        }
        break;
      }
    }

    GsonBuilder gsonBuilder = new GsonBuilder();

    gsonBuilder.registerTypeAdapter(new TypeToken<Map<String, Object>>() {
    }.getType(), new XQMsgJSONTypeAdapter());

    Gson gson = gsonBuilder.create();

    return gson.fromJson(responseString, new TypeToken<Map<String, Object>>() {
    }.getType());

  }

  ServerResponse convertToServerResponse(Map<String, Object> response) {

    try {
      if (response == null) {
        return new ServerResponse(CallStatus.Error, Reasons.InvalidPayload);
      }
      if (response.containsKey("status")) {

        boolean success = response.get("status").equals("OK");

        if (success) {
          response.remove("status");
          return new ServerResponse(CallStatus.Ok, response);
        } else {
          String reason = (response.containsKey("reason")) ? (String) response.get("reason") : "";
          return new ServerResponse(CallStatus.Error, Reasons.InvalidPayload, reason);
        }
      } else {
        return new ServerResponse(CallStatus.Error, Reasons.InvalidPayload, "Error: " + response);
      }

    } catch (Exception e) {
      String errorMessage = e.getMessage();
      logger.warning(errorMessage);
      return new ServerResponse(CallStatus.Error, Reasons.LocalException, errorMessage);
    }
  }

  /**
   * @return XQCache
   */
  public XQCache getCache() {
    return cache;
  }

  private String encode(String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }

}
