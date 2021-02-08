package com.xqmsg.sdk.v2.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Jan Abt
 * @date Jan 19, 2021
 */

 public class ByteArrayReader{
  private byte[] bytes ;
  private int intValue;
  private final DataInputStream i;
  private ByteArrayReader(DataInputStream i){
    this.bytes= new byte[]{};
    this.i=i;

  }
  public static ByteArrayReader with(DataInputStream i) throws IOException {
   return new ByteArrayReader(i);
  }
  public ByteArrayReader addInt (int length) throws IOException {
    bytes = ArrayUtils.append(bytes, i.readNBytes(length));
    return this;
  }
  public ByteArrayReader addBytes (byte [] b) throws IOException {
    byte [] c = new byte[b.length];
    i.read(b);
    bytes = ArrayUtils.append(bytes, c);
    return this;
  }

  public ByteArrayReader reverse ()  {
    bytes = ArrayUtils.reverse(bytes);
    return this;
  }

 public int intValue()  {
    return ByteBuffer.wrap(bytes).getInt();
  }
  public byte[] getBytes()  {
    return bytes;
  }

 }
