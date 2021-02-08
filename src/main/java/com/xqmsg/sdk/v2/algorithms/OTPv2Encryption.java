package com.xqmsg.sdk.v2.algorithms;

import com.xqmsg.sdk.v2.CallStatus;
import com.xqmsg.sdk.v2.Reasons;
import com.xqmsg.sdk.v2.ServerResponse;
import com.xqmsg.sdk.v2.common.ByteArrayReader;
import com.xqmsg.sdk.v2.common.ByteArrayWriter;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.logging.Logger;

import static com.xqmsg.sdk.v2.algorithms.XQAlgorithm.Logger;

/**
 * Created by ikechie on 2/3/20.
 */
public class OTPv2Encryption implements XQAlgorithm {



  private static final Logger logger = Logger(OTPv2Encryption.class);

  public static final String prefix = ".X";
  private static final String name = "Extended One-Time Pad";

  //  @Override
  public String name() {
    return name;
  }

  //  @Override
  public String prefix() {
    return prefix;
  }

  public static final Function<String, Object> doubleFunction = new Function<String, Object>() {
    @Override
    public Object apply(String input) {
      return new Byte[]{};
    }
  };

  @Override
  public CompletableFuture<ServerResponse> encrypt(String text, String k) {

    return CompletableFuture.supplyAsync(() -> {

      String key = k;

      key =  this.shuffle(key);

      if (key.isEmpty()) {
        logger.warning("OTPv2 Source Key cannot be empty.");
        return null;
      }

      byte[] encoded = text.getBytes(StandardCharsets.UTF_8);

      byte[] expandedKeyData = expandKey(key, encoded.length)
                                   .getBytes(StandardCharsets.UTF_8);

      if (expandedKeyData == null) {
        logger.warning("Key could not be UTF8 encoded.");
        return null;
      }

      for (int x = 0; x < encoded.length; ++x) {
        encoded[x] ^= expandedKeyData[x % expandedKeyData.length];
      }
      String expandedKey = "" + new String(expandedKeyData);
      logger.info(String.format("Expanded Key: %s", expandedKey));

      return new ServerResponse(CallStatus.Ok, Map.of(ServerResponse.DATA, Base64.getEncoder().encode(encoded)));

    }).exceptionally((e) -> {
      e.printStackTrace();
      throw new CompletionException(e);
    });

  }

  @Override
  public CompletableFuture<ServerResponse> decrypt(String encryptedText, String k) {

    return CompletableFuture.supplyAsync(() -> {

      String key =  k;

      byte[] byteData;

      try {

        byteData = Base64.getDecoder().decode(encryptedText.getBytes());

      } catch (Exception e) {
        logger.warning("Encrypted source is not base64 encoded.");
        return null;
      }

      if (byteData == null) {
        logger.warning("Encrypted source is not base64 encoded.");
        return null;
      }

      byte[] keyData = key.getBytes();

      if (byteData.length > keyData.length) {
        logger.warning("Key is not long enough to decode data.");
        return null;
      }

      for (int x = 0; x < byteData.length; ++x) {
        byteData[x] ^= keyData[x % keyData.length];
      }

      return new ServerResponse(CallStatus.Ok, Map.of(ServerResponse.DATA,  new String(byteData, StandardCharsets.UTF_8)));

    }).exceptionally((e) -> {
      e.printStackTrace();
      throw new CompletionException(e);
    });
  }

  @Override
  public CompletableFuture<ServerResponse> encrypt(Path sourceFilePath, Path targetFilePath, String expandedKey, String locatorToken) {

    return CompletableFuture.supplyAsync(() -> {

      if (sourceFilePath.toFile().exists()) {
        if (targetFilePath.toFile().exists()) {
          targetFilePath.toFile().delete();
        }
      } else {
        String message = "Source file does not exist.";
        logger.warning(message);
        return new ServerResponse(CallStatus.Error, Reasons.SourceFileNotFound, message);
      }
      String key =  expandedKey;

      byte[] keyData = key.getBytes(StandardCharsets.UTF_8);
      if (keyData.length < 64) {
        String message = "OTP Source Key must be at least 2048 bytes.";
        logger.warning(message);
        return new ServerResponse(CallStatus.Error, Reasons.OTPKeyLengthIncorrect, message);
      }

      try (FileInputStream instream = new FileInputStream(sourceFilePath.toFile());
           ByteArrayOutputStream out = new ByteArrayOutputStream();
           DataOutputStream dao = new DataOutputStream(out)) {

        //Write the locator into the file
        ByteArrayWriter.with(dao).addInt(locatorToken.length()).reverse().writeOut();
        ByteArrayWriter.with(dao).addString(locatorToken).writeOut();

        //write filename into the file
        ByteArrayWriter.with(dao).addInt(sourceFilePath.getFileName().toString().length()).reverse().writeOut();
        //dont write it out just yet, first wncrypt it ...
        byte[] fileNameByteArray = ByteArrayWriter.with(dao)
                                                  .addString(sourceFilePath.getFileName().toString())
                                                  .getBytes();

        logger.info("File size (bytes): " + instream.available());
        logger.info("Original filename: "+ new String(fileNameByteArray, StandardCharsets.UTF_8));
        int keyIndex = 0;
        byte[] encryptedFileNameByteArray=fileNameByteArray;
        for (int x = 0; x < encryptedFileNameByteArray.length; ++x) {
          encryptedFileNameByteArray[x] ^= keyData[keyIndex];
          if (++keyIndex >= keyData.length) keyIndex = 0;
        }

        logger.info("Encrypted filename: "+ new String(encryptedFileNameByteArray, StandardCharsets.UTF_8));
        ByteArrayWriter.with(dao).addBytes(encryptedFileNameByteArray).writeOut();

        File encryptedFile = targetFilePath.toFile();

        if (encryptedFile.createNewFile()) {

          try (FileOutputStream outstream = new FileOutputStream(encryptedFile)) {

            // read data in ch
            final int READ_CHUNK = 1028;
            byte[] inputBuffer = new byte[READ_CHUNK]; // We will stream
            long readAmount = Math.min(instream.available(), READ_CHUNK);
            keyIndex = 0;
            //encrypt the file content using the key
            while (readAmount > 0) {
              int dataRead = instream.read(inputBuffer);
              for (int x = 0; x < dataRead; ++x) {
                inputBuffer[x] ^= keyData[keyIndex];
                if (++keyIndex >= keyData.length) keyIndex = 0;
              }
              dao.write(inputBuffer, 0, dataRead);
              readAmount = Math.min(instream.available(), READ_CHUNK);
            }
            outstream.write(out.toByteArray());
          }
          return new ServerResponse(CallStatus.Ok, Map.of(ServerResponse.DATA, encryptedFile.toPath()));

        } else {
          String message = "Failed to create output file.";
          logger.warning(message);
          return new ServerResponse(CallStatus.Error, Reasons.OutputFileCreationFailed, message);
        }

      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return new ServerResponse(CallStatus.Error, Reasons.FileNotFound, e.getMessage());
      } catch (IOException e) {
        e.printStackTrace();
        return new ServerResponse(CallStatus.Error, Reasons.IOException, e.getMessage());
      }
    });

  }

  @Override
  public CompletableFuture<ServerResponse> decrypt(Path sourceFilePath, Path targetFilePath, Function<String, CompletableFuture<String>> retrieveKeyFunction) {
    return CompletableFuture.supplyAsync(() -> {
      if (sourceFilePath.toFile().exists()) {
        if (targetFilePath.toFile().exists()) {
          targetFilePath.toFile().delete();
        }
      } else {
        String message = "Source file does not exist.";
        logger.warning(message);
        return new ServerResponse(CallStatus.Error, Reasons.SourceFileNotFound, message);
      }

      try (FileInputStream instream = new FileInputStream(sourceFilePath.toFile());
           DataInputStream dai = new DataInputStream(instream)) {

        // Read the token
        final int tokenSize = ByteArrayReader.with(dai).addInt(4).reverse().intValue();
        byte[] locatorTokenBytes = ByteArrayReader.with(dai).addInt(tokenSize).getBytes();

        // Read the filename
        final int filenameSize = ByteArrayReader.with(dai).addInt(4).reverse().intValue();
        byte[] filenameBytes = ByteArrayReader.with(dai).addInt(filenameSize).getBytes();

        return retrieveKeyFunction
                .apply(new String((locatorTokenBytes)))
                .thenApply((k) -> {

                  String key = k;

                  if (key == null) {
                    String message = "Unable to retrieve a valid key.";
                    return new ServerResponse(CallStatus.Error, Reasons.InvalidQuantumKey, message);
                  }

                  byte[] keyData = key.getBytes(StandardCharsets.UTF_8);

                  if (keyData.length < 64) {
                    String message = "OTP Source Key must be at least 64 bytes.";
                    return new ServerResponse(CallStatus.Error, Reasons.OTPKeyLengthIncorrect, message);
                  }
                  logger.info("Filename : " + new String(filenameBytes, StandardCharsets.UTF_8));
                  // Decrypt the filename using the key.
                  int keyIndex = 0;
                  byte[] decoded = filenameBytes;
                  for (int x = 0; x < filenameBytes.length; ++x) {
                    decoded[x] ^= keyData[keyIndex];
                    if (++keyIndex >= keyData.length) keyIndex = 0;
                  }

                  logger.info("Decrypted Filename: " + new String(decoded, StandardCharsets.UTF_8));


                  // Confirm writeability.
                  File decryptedFile = targetFilePath.toFile();
                  try {
                    /// Decrypt the message content.
                    if (!decryptedFile.createNewFile()) {
                      String message = "Failed to create new file.";
                      logger.warning(message);
                      return new ServerResponse(CallStatus.Error, Reasons.FileCreateFailed, message);
                    }

                  } catch (IOException e) { }

                  try (FileOutputStream outstream = new FileOutputStream(decryptedFile)) {


                    final int READ_CHUNK = 1028;
                    byte[] inputBuffer = new byte[READ_CHUNK]; // We will stream
                    int available = instream.available();
                    long readAmount = Math.min(available, READ_CHUNK);
                    keyIndex = 0;
                    while (readAmount > 0) {
                      int dataRead = instream.read(inputBuffer);
                      for (int x = 0; x < dataRead; ++x) {
                        inputBuffer[x] ^= keyData[keyIndex];
                        if (++keyIndex >= keyData.length) keyIndex = 0;
                      }
                      outstream.write(inputBuffer, 0, dataRead);
                      logger.info("Decrypted File Data: " + new String(inputBuffer, StandardCharsets.UTF_8));
                      readAmount = Math.min(instream.available(), READ_CHUNK);
                    }
                  } catch (FileNotFoundException e) {
                    e.printStackTrace();
                  } catch (IOException e) {
                    e.printStackTrace();
                  }

                  return new ServerResponse(CallStatus.Ok, Map.of(ServerResponse.DATA, decryptedFile.toPath()));
                }).get();

      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return new ServerResponse(CallStatus.Error, Reasons.InternalException, e.getMessage());
      } catch (IOException | InterruptedException | ExecutionException e) {
        e.printStackTrace();
        return new ServerResponse(CallStatus.Error, Reasons.InternalException, e.getMessage());

      }
    });
  }

  public static final long readUint(DataInputStream is) {
    try {
      return is.readInt() & 0xFFFFFFFFL; // Mask with 32 one-bits
    } catch (IOException e) {
      e.printStackTrace();
      return 0;
    }
  }

}
