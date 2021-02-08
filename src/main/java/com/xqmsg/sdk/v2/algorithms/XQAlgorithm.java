package com.xqmsg.sdk.v2.algorithms;

import com.xqmsg.sdk.v2.ServerResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Created by ikechie on 2/3/20.
 */
public interface XQAlgorithm {

  String ENCODED_STRING = "encoded";
  String EXPANDED_KEY = "expandedKey";

  /// Returns the full name of the algorithm for displaying in lists.
  String name();

  /// Identifying Prefix
  ///
  /// All algorithms other than AES will prefix their key with a value (e.g. "X" for OTP). That prefix should be stated here.
  String prefix();

  /// Encrypts the text fragment using the provided key.
  ///
  /// - Parameters:
  ///   - text: The text that needs to be encrypted.
  ///   - key: The encryption passphrase. This will be stored on a remote key server.
  ///   - completion: The block that will be called upon completion.
  ///   - encodedData: The encrypted data. This should be nil if the process failed.
  ///   - resultingKey: The key that was ultimately used (the original key could be modified by the algorithm if inadequate).
  CompletableFuture<ServerResponse> encrypt(String text, String key);


  /// Encrypts the text fragment using the provided key.
  ///
  /// - Parameters:
  ///   - text: The text that needs to be decrypted.
  ///   - key: The encryption passphrase.
  ///   - completion: The block that will be called upon completion.
  ///   - decodedData: The decrypted data. This should be nil if the process failed.
  ///   - resultingKey: The key that was ultimately used (the original key could be modified by the algorithm if inadequate).
  CompletableFuture<ServerResponse> decrypt(String text, String key);


  /// Encrypts the referenced file using the provided key.
  ///
  /// - Parameters:
  ///   - file: The text that needs to be encrypted.
  ///   - to: The URL where the resulting file should be stored.
  ///   - key: The encryption passphrase. This will be stored on a remote key server.
  ///   - token: The token that will be used to retrieve the key. This will be embedded in the file
  ///   - completion: The block that will be called upon completion.
  ///   - success: The encryption status. Will be false if anything went wrong.
  ///   - resultingKey: The key that was ultimately used (the original key could be modified by the algorithm if inadequate).
  CompletableFuture<ServerResponse> encrypt(Path sourceFilePath, Path targetFilePath, String key, String publicToken);

  /// Decrypts the referenced file using the provided key.
  ///
  /// - Parameters:
  ///   - file: The text that needs to be encrypted.
  ///   - to: The URL where the resulting file should be stored.
  ///   - key: The encryption passphrase. This will be stored on a remote key server.
  ///   - completion: The block that will be called upon completion.
  ///   - success: The encryption status. Will be false if anything went wrong.
  CompletableFuture<ServerResponse> decrypt(Path sourceFilePath, Path targetFilePath, Function<String, CompletableFuture<String>> retrieveKeyFunction);

   static <T> Logger Logger(Class<T> clazz){
    try {
      InputStream configurationStream = clazz.getClassLoader().getResourceAsStream("test-logging.properties");
      if(configurationStream!=null) {
        LogManager.getLogManager().readConfiguration(configurationStream);
      }
      return Logger.getLogger(clazz.getName());
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }


/**
 * Shuffles the content using the fisher-yates shuffle algorithm.
 * @return {String}
 */

   default String shuffle(String s) {
         var a = s.split("\\s");
        final int n = a.length;
    for (int i = n - 1; i > 0; i--) {
            int j = (int)Math.floor(Math.random() * (i + 1));
            String tmp = a[i];
      a[i] = a[j];
      a[j] = tmp;
    }
     return String.join(" ", a);
  }

  /**
   * Expand a key length to that of the text that needs encryption.
   * @param k The original key ( cannot be empty
   * @param extendTo The key length that we require
   * @returns Expanded Key
   */
  default String expandKey  (String k, int extendTo) {
    //String key = k.replace("\n$","");
    String key = k.trim();
    if (key.length() > extendTo) {
      return shuffle(key.substring(0, extendTo));
    }
    String g = key;
    while (g.length() < extendTo) {
      g += this.shuffle(key);
    }
    return g;
  }


}