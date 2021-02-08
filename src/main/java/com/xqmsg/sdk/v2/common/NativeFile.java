package com.xqmsg.sdk.v2.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class NativeFile {


    public static String filename(String path) {
        java.io.File f = new java.io.File(path);
        if (f.exists())  return f.getName();
        return path;
    }

    public static boolean exists(String path) {
        java.io.File f = new java.io.File(path);
        return f.exists();
    }


    public static boolean delete(String path) {
        java.io.File f = new java.io.File(path);
        if (f.exists())  return f.delete();
        return false;
    }

    public static byte[] readAllBytes(String path) throws FileNotFoundException {
        java.io.File file = new java.io.File(path);
        if (!file.exists()) throw new FileNotFoundException();

        InputStream in;

        try {

            in = new FileInputStream(file);

            final byte[] bytes  = new byte[(int)file.length()];

            if ( bytes.length == 0 ) return bytes;

            int offset = 0;
            while ( offset < bytes.length ) {
                int result = in.read(bytes, offset, bytes.length - offset);
                if (result == -1) {
                    break;
                }
                offset += result;
            }
            in.close();

            return bytes;

        } catch (Exception e) {
            return new byte[0];
        }
    }
}
