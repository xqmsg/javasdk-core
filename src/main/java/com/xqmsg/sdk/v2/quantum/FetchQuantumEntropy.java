package com.xqmsg.sdk.v2.quantum;

import com.xqmsg.sdk.v2.CallMethod;
import com.xqmsg.sdk.v2.ServerResponse;
import com.xqmsg.sdk.v2.XQModule;
import com.xqmsg.sdk.v2.XQSDK;
import com.xqmsg.sdk.v2.utils.Destination;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Fetches quantum entropy from the server. When the user makes the request, <br>
 * they must include the number of entropy bits to fetch.
 * In order to reduce the amount of data the quantum server emits, <br>
 * the entropy is sent as a base64-encoded hex string. <br>
 * While the hex string itself can be used as entropy, to retrieve the actual bits ( if required ),<br>
 * the string should be decoded from base-64, and each hex character in the sequence converted to its <br>
 * 4-bit binary representation.
 */
public class FetchQuantumEntropy extends XQModule {

  private static final Logger logger = Logger(FetchQuantumEntropy.class);

  public static String KS = "ks";

  public static String _256 = "256";

  private final XQSDK sdk;


  private FetchQuantumEntropy(XQSDK aSDK) {
    sdk = aSDK;
  }

  /**
   * @param sdk App Settings
   * @returns this
   */
  public static FetchQuantumEntropy with(XQSDK sdk) {
    return new FetchQuantumEntropy(sdk);
  }

  @Override
  public List<String> requiredFields() { return  List.of();}

  /**
   * @param maybeArgs Map of request parameters supplied to this method.
   * <pre>parameter details:none</pre>
   * @returns CompletableFuture&lt;ServerResponse#payload:{data:String}>>
   * @apiNote !=required ?=optional [...]=default {...} map
   */
  @Override
  public CompletableFuture<ServerResponse> supplyAsync(Optional<Map<String, Object>> maybeArgs) {

    return CompletableFuture.supplyAsync (() -> {

      return sdk.call(sdk.KEY_SERVER_URL,
              Optional.empty(),
              CallMethod.Get,
              Optional.empty(),
              Optional.of(Destination.XQ),
              Optional.of(maybeArgs.orElse(Map.of(KS, _256))));
    });
  }

  @Override
  public String moduleName() {
    return "FetchQuantumEntropy";
  }

}
