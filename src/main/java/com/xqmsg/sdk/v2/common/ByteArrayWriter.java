package com.xqmsg.sdk.v2.common;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Jan Abt
 * @date Jan 19, 2021
 */

public class ByteArrayWriter {
  private byte[] bytes;
  private final DataOutputStream o;

  private ByteArrayWriter(DataOutputStream o) {
    this.o = o;
    this.bytes = new byte[]{};
  }

  public static ByteArrayWriter with(DataOutputStream o) {
    return new ByteArrayWriter(o);
  }

  public ByteArrayWriter addInt(int value) {
    bytes = ByteBuffer.allocate(4).putInt(value).array();
    return this;
  }

  public ByteArrayWriter addString(String s) {
    bytes = ArrayUtils.append(bytes, s.getBytes(StandardCharsets.UTF_8));
    return this;
  }
  
  public ByteArrayWriter addBytes(byte[] b) {
    bytes = ArrayUtils.append(bytes, b);
    return this;
  }

  public ByteArrayWriter reverse() {
    bytes = ArrayUtils.reverse(bytes);
    return this;
  }

  public ByteArrayWriter writeOut() throws IOException {
    o.write(this.bytes);
    return this;
  }

  public ByteArrayWriter writeOut(byte[] bytes) throws IOException {
    o.write(this.bytes);
    return this;
  }

  public byte[] getBytes() {
    return bytes;
  }

}
