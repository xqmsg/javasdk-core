package com.xqmsg.sdk.v2.algorithms;

import com.xqmsg.sdk.v2.CallStatus;
import com.xqmsg.sdk.v2.Reasons;
import com.xqmsg.sdk.v2.ServerResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Logger;

import static com.xqmsg.sdk.v2.algorithms.XQAlgorithm.Logger;

/**
 * Created by ikechie on 2/3/20.
 */
public class AESEncryption implements XQAlgorithm {

  private static final Logger logger = Logger(AESEncryption.class);

  public static final String prefix = ".A";
  public static final String name = "Advanced Encryption Standard";

  @Override
  public String name() {
    return name;
  }

  @Override
  public String prefix() {
    return prefix;
  }

  private byte[] evpKDF(byte[] password, int keySize, int ivSize, byte[] salt, byte[] resultKey, byte[] resultIv) throws NoSuchAlgorithmException {
    return evpKDF(password, keySize, ivSize, salt, 1, "MD5", resultKey, resultIv);
  }

  private byte[] evpKDF(byte[] password, int keySize, int ivSize, byte[] salt, int iterations, String hashAlgorithm, byte[] resultKey, byte[] resultIv) throws NoSuchAlgorithmException {
    keySize = keySize / 32;
    ivSize = ivSize / 32;
    int targetKeySize = keySize + ivSize;
    byte[] derivedBytes = new byte[targetKeySize * 4];
    int numberOfDerivedWords = 0;
    byte[] block = null;
    MessageDigest hasher = MessageDigest.getInstance(hashAlgorithm);
    while (numberOfDerivedWords < targetKeySize) {
      if (block != null) {
        hasher.update(block);
      }
      hasher.update(password);
      block = hasher.digest(salt);
      hasher.reset();

      // Iterations
      for (int i = 1; i < iterations; i++) {
        block = hasher.digest(block);
        hasher.reset();
      }

      System.arraycopy(block, 0, derivedBytes, numberOfDerivedWords * 4,
              Math.min(block.length, (targetKeySize - numberOfDerivedWords) * 4));

      numberOfDerivedWords += block.length / 4;
    }

    System.arraycopy(derivedBytes, 0, resultKey, 0, keySize * 4);
    System.arraycopy(derivedBytes, keySize * 4, resultIv, 0, ivSize * 4);

    return derivedBytes; // key + iv
  }

  private byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
              + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  private final int keySize = 256;
  private final int ivSize = 128;

  public byte[] decryptImmediate(String text, String secretKey) {

    try {

      final byte[] ctBytes = Base64.getMimeDecoder().decode(text.getBytes(StandardCharsets.UTF_8));
      final byte[] saltBytes = Arrays.copyOfRange(ctBytes, 8, 16);
      final byte[] ciphertextBytes = Arrays.copyOfRange(ctBytes, 16, ctBytes.length);

      byte[] key = new byte[keySize / 8];
      byte[] iv = new byte[ivSize / 8];
      evpKDF(secretKey.getBytes(StandardCharsets.UTF_8), keySize, ivSize, saltBytes, key, iv);

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", new BouncyCastleProvider());
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
      return cipher.doFinal(ciphertextBytes);

    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public CompletableFuture<ServerResponse> encrypt(String text, String key) {

    return CompletableFuture.supplyAsync(() -> {
      return new ServerResponse(CallStatus.Ok, Map.of(ServerResponse.DATA,  new String(encryptImmediate(text, key), StandardCharsets.UTF_8)));

    }).exceptionally((e) -> {
      e.printStackTrace();
      return null;
    });
  }

  public byte[] encryptImmediate(String text, String secretKey) {

    try {

      byte[] saltBytes = UUID.randomUUID().toString().substring(0, 8).getBytes();
      byte[] saltTextBytes = new byte[]{0x53, 0x61, 0x6c, 0x74, 0x65, 0x64, 0x5f, 0x5f};//"Salted__".getBytes();
      byte[] key = new byte[keySize / 8];
      byte[] iv = new byte[ivSize / 8];
      evpKDF(secretKey.getBytes(StandardCharsets.UTF_8), keySize, ivSize, saltBytes, key, iv);

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", new BouncyCastleProvider());
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
      byte[] encryptedBytes = cipher.doFinal(text.getBytes());

      byte[] payload = new byte[encryptedBytes.length + saltTextBytes.length + saltBytes.length];
      System.arraycopy(saltTextBytes, 0, payload, 0, saltTextBytes.length);
      System.arraycopy(saltBytes, 0, payload, saltTextBytes.length, saltBytes.length);
      System.arraycopy(encryptedBytes, 0, payload, saltTextBytes.length + saltBytes.length, encryptedBytes.length);

      final int NO_WRAP = 2;
      // return Base64.getEncoder().encode(payload);
      return Base64.getMimeEncoder(text.length(), ByteBuffer.allocate(4).putInt(NO_WRAP).array()).encode(payload);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public CompletableFuture<ServerResponse> decrypt(String text, String key) {
    return CompletableFuture.supplyAsync(() -> {
      return new ServerResponse(CallStatus.Ok, Map.of(ServerResponse.DATA,  new String(decryptImmediate(text, key), StandardCharsets.UTF_8)));


    });
  }

  @Override
  public CompletableFuture<ServerResponse> encrypt(Path sourceFilePath, Path targetFilePath, String key, String locatorToken) {
    return CompletableFuture.supplyAsync(() -> {

      if (sourceFilePath.toFile().exists()) {
        if (targetFilePath.toFile().exists()) {
          targetFilePath.toFile().delete();
        }
      } else {
        String message = "Source file does not exist.";
        logger.severe(message);
        return new ServerResponse(CallStatus.Error, Reasons.SourceFileNotFound, message);
      }

      try (FileInputStream instream = new FileInputStream(sourceFilePath.toFile())) {

        byte[] locatorTokenLengthBytes = ByteBuffer.allocate(4).putInt(locatorToken.length()).array();
        byte[] locatorTokenBytes = locatorToken.getBytes();
        byte[] saltTextBytes = new byte[]{0x53, 0x61, 0x6c, 0x74, 0x65, 0x64, 0x5f, 0x5f};//"Salted__".getBytes();
        byte[] saltBytes = UUID.randomUUID().toString().substring(0, 8).getBytes();
        byte[] k = new byte[keySize / 8];
        byte[] iv = new byte[ivSize / 8];

        evpKDF(key.getBytes(StandardCharsets.UTF_8), keySize, ivSize, saltBytes, k, iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", new BouncyCastleProvider());
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, "AES"), new IvParameterSpec(iv));

        byte[] buffer = new byte[(int) sourceFilePath.toFile().length()];
        instream.read(buffer);

        byte[] encryptedBytes = cipher.doFinal(buffer);


        byte[] payload = new byte[locatorTokenBytes.length + locatorTokenLengthBytes.length + saltTextBytes.length + saltBytes.length + encryptedBytes.length];
        System.arraycopy(locatorTokenLengthBytes, 0, payload, 0, locatorTokenLengthBytes.length);
        System.arraycopy(locatorTokenBytes, 0, payload, locatorTokenLengthBytes.length, locatorTokenBytes.length);
        System.arraycopy(saltTextBytes, 0, payload, locatorTokenLengthBytes.length+locatorTokenBytes.length, saltTextBytes.length);
        System.arraycopy(saltBytes, 0, payload, locatorTokenLengthBytes.length+locatorTokenBytes.length+saltTextBytes.length, saltBytes.length);
        System.arraycopy(encryptedBytes, 0, payload, locatorTokenLengthBytes.length+locatorTokenBytes.length+saltTextBytes.length+saltBytes.length, encryptedBytes.length);

        //file format: locatorTokenLengthBytes + locatorTokenBytes + saltTextBytes + saltBytes + encryptedBytes
        File encryptedFile = targetFilePath.toFile();

        if (encryptedFile.createNewFile()) {

          try (FileOutputStream outstream = new FileOutputStream(encryptedFile)) {
            outstream.write(payload);
            return new ServerResponse(CallStatus.Ok, Map.of(ServerResponse.DATA, encryptedFile.toPath()));
          }

        } else {
          String message = "Failed to create output file.";
          logger.warning(message);
          return new ServerResponse(CallStatus.Error, Reasons.OutputFileCreationFailed, message);
        }

      } catch (Exception e) {
        e.printStackTrace();
        return new ServerResponse(CallStatus.Error, Reasons.OutputFileCreationFailed, e.getMessage());
      }

    });
  }

  @Override
  public CompletableFuture<ServerResponse> decrypt(Path sourceFilePath, Path targetFilePath, Function<String, CompletableFuture<String>> retrieveKeyFunction) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        if (sourceFilePath.toFile().exists()) {
          if (targetFilePath.toFile().exists()) {
            targetFilePath.toFile().delete();
          }
        } else {
          String message = "Source file does not exist.";
          logger.warning(message);
          return new ServerResponse(CallStatus.Error, Reasons.SourceFileNotFound, message);
        }

        try (FileInputStream instream = new FileInputStream(sourceFilePath.toFile())) {

          byte[] buffer = new byte[(int) sourceFilePath.toFile().length()];

          instream.read(buffer);

          final byte[] locatorTokenLengthBytes = Arrays.copyOfRange(buffer, 0, 4);
          int locatorTokenLength = ByteBuffer.wrap(locatorTokenLengthBytes).getInt();

          final byte[] locatorTokenBytes = Arrays.copyOfRange(buffer, locatorTokenLengthBytes.length, locatorTokenLengthBytes.length+locatorTokenLength);
          final byte[] saltTextBytes = Arrays.copyOfRange(buffer, locatorTokenLengthBytes.length+locatorTokenLength, locatorTokenLengthBytes.length+locatorTokenLength+8);
          final byte[] saltBytes = Arrays.copyOfRange(buffer, locatorTokenLengthBytes.length+locatorTokenLength+saltTextBytes.length, locatorTokenLengthBytes.length+locatorTokenLength+saltTextBytes.length+8);
          final byte[] ciphertextBytes = Arrays.copyOfRange(buffer, locatorTokenLengthBytes.length+locatorTokenLength+saltTextBytes.length+saltBytes.length, buffer.length);

          return retrieveKeyFunction.apply(new String((locatorTokenBytes)))
                  .thenApply((key) -> {
                    try {
                      byte[] k = new byte[keySize / 8];
                      byte[] iv = new byte[ivSize / 8];

                      evpKDF(key.getBytes(StandardCharsets.UTF_8), keySize, ivSize, saltBytes, k, iv);

                      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", new BouncyCastleProvider());
                      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(k, "AES"), new IvParameterSpec(iv));
                      byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);

                      File decryptedFile = targetFilePath.toFile();

                      if (decryptedFile.createNewFile()) {
                        try (FileOutputStream os = new FileOutputStream(decryptedFile)) {
                          os.write(decryptedBytes);
                          return new ServerResponse(CallStatus.Ok, Map.of(ServerResponse.DATA, decryptedFile.toPath()));
                        }
                      } else {
                        String message = "Failed to create output file.";
                        logger.warning(message);
                        return new ServerResponse(CallStatus.Error, Reasons.OutputFileCreationFailed, message);
                      }
                    } catch (Exception e) {
                      return new ServerResponse(CallStatus.Error, Reasons.OutputFileCreationFailed, e.getMessage());

                    }


                  }).get();

        } catch (Exception e) {
          String message = e.getMessage();
          logger.warning(message);
          return new ServerResponse(CallStatus.Error, Reasons.OutputFileCreationFailed, message);
        }


      } catch (Exception e) {
        String message = e.getMessage();
        logger.warning(message);
        return new ServerResponse(CallStatus.Error, Reasons.OutputFileCreationFailed, message);

      }


    });
  }
}
