package com.xqmsg.sdk.v2.common;

import java.lang.reflect.Array;

public class ArrayUtils<T> {

  /**
   * return a new array of the first 'n' elements of T[] aArr
   *
   * @param aArr
   * @param n
   * @return
   */
  public static <T> T[] take(int n, T[] aArr) {
    return slice(0, n, aArr);
  }


  /**
   * return a new array, having dropped the first 'n' elements of T[] aArr
   *
   * @param aArr
   * @param n
   * @return
   */
  public static <T> T[] drop(int n, T[] aArr) {
    return slice(n, aArr.length, aArr);

  }

/**
   * return a new array, having dropped the 'n'th element of T[] aArr
   *
   * @param aArr
   * @param n
   * @return
   */
  public static <T> T[] remove(int n, T e, T[] aArr) {
    T[] left =  slice(0, n, aArr);
    T[] right = slice(n+1,aArr.length,aArr);
    return append(left,right);

  }

  /**
   * return the first item of T[] aArr
   *
   * @param aArr
   * @return
   */
  public static <T> T first(T[] aArr) {
    T[] first = take(1, aArr);
    return first.length != 0 ? first[0] : null;
  }

  /**
   * return the last item of  aArr
   *
   * @param aArr
   * @return
   */
  public static <T> T last(T[] aArr) {
    T[] last = drop(aArr.length - 1, aArr);
    return last.length != 0 ? last[0] : null;
  }

  /**
   * Selects an interval of elements. The new array returned is made up
   * of all elements `e` of T[] aArr which satisfy: from <= indexOf(e) < until
   *
   * @param from  the lowest index to include from aArr.
   * @param until the lowest index to EXCLUDE from aArr.
   */
  @SuppressWarnings("unchecked")
  private static <T> T[] slice(int from, int until, T[] aArr) {
    int lo = Math.max(from, 0);
    int hi = Math.min(Math.max(until, 0), aArr.length);
    int elems = Math.max(hi - lo, 0);
    if (aArr == null || aArr.length == 0) {
      return null;
    }
    T[] array = (T[]) Array.newInstance(aArr[0].getClass(), elems);
    int index = 0;
    int i = lo;
    while (i < hi) {
      array[index++] = aArr[i++];
    }
    return array;
  }

  /**
   * Appends n items to an array and returns a new array of the result.
   *
   * @param aArr
   * @param aItems
   * @param <T>
   * @return
   */
  @SuppressWarnings("unchecked")
  public static <T> T[] append(T[] aArr, T... aItems) {

    if (aArr == null || aArr.length == 0) {
      return aItems;
    }
    if (aItems == null || aItems.length == 0) {
      return aArr;
    }
    T[] array = (T[]) Array.newInstance(aArr[0].getClass(), aArr.length + aItems.length);
    int index = 0;
    int i = 0;
    while (i < aArr.length) {
      array[index++] = aArr[i++];
    }
    if (!aArr[0].getClass().equals(aItems[0].getClass())) {
      throw new ArrayStoreException(String.format("%s and %s must be of the same type", aArr, aItems));
    }
    int j = 0;
    while (j < aItems.length) {
      array[index++] = aItems[j++];
    }
    return array;
  }

  public static Object[] appendAny(Object[] aArr, Object... aItems) {

    if (aArr == null || aArr.length == 0) {
      return aItems;
    }
    if (aItems == null || aItems.length == 0) {
      return aArr;
    }
    Object[] array = new Object[aArr.length + aItems.length];
    int index = 0;
    int i = 0;
    while (i < aArr.length) {
      array[index++] = aArr[i++];
    }
    int j = 0;
    while (j < aItems.length) {
      array[index++] = aItems[j++];
    }
    return array;
  }


  public static <T> T[] reverse(T[] aArr) {

    if (aArr == null || aArr.length == 0) {
      return aArr;
    }
    T[] array = (T[]) Array.newInstance(aArr[0].getClass(), aArr.length);
    int index = 0;
    int i = aArr.length - 1;
    while (i >= 0) {
      array[index++] = aArr[i--];
    }

    return array;
  }

///////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////
//////////////////////////   int   ////////////////////////////////
///////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////  

  public static int[] take(int n, int[] aArr) {
    return slice(0, n, aArr);
  }

  public static int[] drop(int n, int[] aArr) {
    return slice(n, aArr.length, aArr);
  }

  public static int[] remove(int n, int e, int[] aArr) {
    int[] left =  slice(0, n, aArr);
    int[] right = slice(n+1,aArr.length,aArr);
    return append(left,right);
  }

  public static int first(int[] aArr) {
    int[] first = take(1, aArr);
    return first.length != 0 ? first[0] : null;
  }

  public static int last(int[] aArr) {
    int[] last = drop(aArr.length - 1, aArr);
    return last.length != 0 ? last[0] : null;
  }

  private static int[] slice(int from, int until, int[] aArr) {
    int lo = Math.max(from, 0);
    int hi = Math.min(Math.max(until, 0), aArr.length);
    int elems = Math.max(hi - lo, 0);
    if (aArr == null || aArr.length == 0) {
      return null;
    }
    int[] array = (int[]) Array.newInstance(int.class, elems);
    int index = 0;
    int i = lo;
    while (i < hi) {
      array[index++] = aArr[i++];
    }
    return array;
  }

  public static int[] append(int[] aArr, int... aItems) {

    if (aArr == null || aArr.length == 0) {
      return aItems;
    }
    if (aItems == null || aItems.length == 0) {
      return aArr;
    }
    int[] array = (int[]) Array.newInstance(int.class, aArr.length + aItems.length);
    int index = 0;
    int i = 0;
    while (i < aArr.length) {
      array[index++] = aArr[i++];
    }
    int j = 0;
    while (j < aItems.length) {
      array[index++] = aItems[j++];
    }
    return array;
  }

  public static int[] reverse(int[] aArr) {
    if (aArr == null || aArr.length == 0) {
      return aArr;
    }
    int[] array = (int[]) Array.newInstance(int.class, aArr.length);
    int index = 0;
    int i = aArr.length - 1;
    while (i >= 0) {
      array[index++] = aArr[i--];
    }
    return array;
  }


////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////
///////////////////////////  byte //////////////////////////////////
////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////


  public static byte[] take(int n, byte[] aArr) {
    return slice(0, n, aArr);
  }

  public static byte[] drop(int n, byte[] aArr) {
    return slice(n, aArr.length, aArr);
  }

  public static byte[] remove(int n, byte e, byte[] aArr) {
    byte[] left =  slice(0, n, aArr);
    byte[] right = slice(n+1,aArr.length,aArr);
    return append(left,right);
  }

  public static byte first(byte[] aArr) {
    byte[] first = take(1, aArr);
    return first.length != 0 ? first[0] : null;
  }

  public static byte last(byte[] aArr) {
    byte[] last = drop(aArr.length - 1, aArr);
    return last.length != 0 ? last[0] : null;
  }

  private static byte[] slice(int from, int until, byte[] aArr) {
    int lo = Math.max(from, 0);
    int hi = Math.min(Math.max(until, 0), aArr.length);
    int elems = Math.max(hi - lo, 0);
    if (aArr == null || aArr.length == 0) {
      return null;
    }
    byte[] array = (byte[]) Array.newInstance(byte.class, elems);
    int index = 0;
    int i = lo;
    while (i < hi) {
      array[index++] = aArr[i++];
    }
    return array;
  }

  public static byte[] append(byte[] aArr, byte... aItems) {

    if (aArr == null || aArr.length == 0) {
      return aItems;
    }
    if (aItems == null || aItems.length == 0) {
      return aArr;
    }
    byte[] array = (byte[]) Array.newInstance(byte.class, aArr.length + aItems.length);
    int index = 0;
    int i = 0;
    while (i < aArr.length) {
      array[index++] = aArr[i++];
    }
    int j = 0;
    while (j < aItems.length) {
      array[index++] = aItems[j++];
    }
    return array;
  }


  public static byte[] reverse(byte[] aArr) {
    if (aArr == null || aArr.length == 0) {
      return aArr;
    }
    byte[] array = (byte[]) Array.newInstance(byte.class, aArr.length);
    int index = 0;
    int i = aArr.length - 1;
    while (i >= 0) {
      array[index++] = aArr[i--];
    }
    return array;
  }


////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////
///////////////////////////  short //////////////////////////////////
////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////


  public static short[] take(int n, short[] aArr) {
    return slice(0, n, aArr);
  }

  public static short[] drop(int n, short[] aArr) {
    return slice(n, aArr.length, aArr);
  }

  public static short[] remove(int n, short e, short[] aArr) {
    short[] left =  slice(0, n, aArr);
    short[] right = slice(n+1,aArr.length,aArr);
    return append(left,right);
  }

  public static short first(short[] aArr) {
    short[] first = take(1, aArr);
    return first.length != 0 ? first[0] : null;
  }

  public static short last(short[] aArr) {
    short[] last = drop(aArr.length - 1, aArr);
    return last.length != 0 ? last[0] : null;
  }

  private static short[] slice(int from, int until, short[] aArr) {
    int lo = Math.max(from, 0);
    int hi = Math.min(Math.max(until, 0), aArr.length);
    int elems = Math.max(hi - lo, 0);
    if (aArr == null || aArr.length == 0) {
      return null;
    }
    short[] array = (short[]) Array.newInstance(short.class, elems);
    int index = 0;
    int i = lo;
    while (i < hi) {
      array[index++] = aArr[i++];
    }
    return array;
  }

  public static short[] append(short[] aArr, short... aItems) {

    if (aArr == null || aArr.length == 0) {
      return aItems;
    }
    if (aItems == null || aItems.length == 0) {
      return aArr;
    }
    short[] array = (short[]) Array.newInstance(short.class, aArr.length + aItems.length);
    int index = 0;
    int i = 0;
    while (i < aArr.length) {
      array[index++] = aArr[i++];
    }
    int j = 0;
    while (j < aItems.length) {
      array[index++] = aItems[j++];
    }
    return array;
  }


  public static short[] reverse(short[] aArr) {
    if (aArr == null || aArr.length == 0) {
      return aArr;
    }
    short[] array = (short[]) Array.newInstance(short.class, aArr.length);
    int index = 0;
    int i = aArr.length - 1;
    while (i >= 0) {
      array[index++] = aArr[i--];
    }
    return array;
  }


////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////
///////////////////////////  long //////////////////////////////////
////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////


  public static long[] take(int n, long[] aArr) {
    return slice(0, n, aArr);
  }

  public static long[] drop(int n, long[] aArr) {
    return slice(n, aArr.length, aArr);
  }

  public static long[] remove(int n, long e, long[] aArr) {
    long[] left =  slice(0, n, aArr);
    long[] right = slice(n+1,aArr.length,aArr);
    return append(left,right);
  }

  public static long first(long[] aArr) {
    long[] first = take(1, aArr);
    return first.length != 0 ? first[0] : null;
  }

  public static long last(long[] aArr) {
    long[] last = drop(aArr.length - 1, aArr);
    return last.length != 0 ? last[0] : null;
  }

  private static long[] slice(int from, int until, long[] aArr) {
    int lo = Math.max(from, 0);
    int hi = Math.min(Math.max(until, 0), aArr.length);
    int elems = Math.max(hi - lo, 0);
    if (aArr == null || aArr.length == 0) {
      return null;
    }
    long[] array = (long[]) Array.newInstance(long.class, elems);
    int index = 0;
    int i = lo;
    while (i < hi) {
      array[index++] = aArr[i++];
    }
    return array;
  }

  public static long[] append(long[] aArr, long... aItems) {

    if (aArr == null || aArr.length == 0) {
      return aItems;
    }
    if (aItems == null || aItems.length == 0) {
      return aArr;
    }
    long[] array = (long[]) Array.newInstance(long.class, aArr.length + aItems.length);
    int index = 0;
    int i = 0;
    while (i < aArr.length) {
      array[index++] = aArr[i++];
    }
    int j = 0;
    while (j < aItems.length) {
      array[index++] = aItems[j++];
    }
    return array;
  }


  public static long[] reverse(long[] aArr) {
    if (aArr == null || aArr.length == 0) {
      return aArr;
    }
    long[] array = (long[]) Array.newInstance(long.class, aArr.length);
    int index = 0;
    int i = aArr.length - 1;
    while (i >= 0) {
      array[index++] = aArr[i--];
    }
    return array;
  }


////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////
///////////////////////////  double //////////////////////////////////
////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////


  public static double[] take(int n, double[] aArr) {
    return slice(0, n, aArr);
  }

  public static double[] drop(int n, double[] aArr) {
    return slice(n, aArr.length, aArr);
  }

  public static double[] remove(int n, double e, double[] aArr) {
    double[] left =  slice(0, n, aArr);
    double[] right = slice(n+1,aArr.length,aArr);
    return append(left,right);
  }
  public static double first(double[] aArr) {
    double[] first = take(1, aArr);
    return first.length != 0 ? first[0] : null;
  }

  public static double last(double[] aArr) {
    double[] last = drop(aArr.length - 1, aArr);
    return last.length != 0 ? last[0] : null;
  }

  private static double[] slice(int from, int until, double[] aArr) {
    int lo = Math.max(from, 0);
    int hi = Math.min(Math.max(until, 0), aArr.length);
    int elems = Math.max(hi - lo, 0);
    if (aArr == null || aArr.length == 0) {
      return null;
    }
    double[] array = (double[]) Array.newInstance(double.class, elems);
    int index = 0;
    int i = lo;
    while (i < hi) {
      array[index++] = aArr[i++];
    }
    return array;
  }

  public static double[] append(double[] aArr, double... aItems) {

    if (aArr == null || aArr.length == 0) {
      return aItems;
    }
    if (aItems == null || aItems.length == 0) {
      return aArr;
    }
    double[] array = (double[]) Array.newInstance(double.class, aArr.length + aItems.length);
    int index = 0;
    int i = 0;
    while (i < aArr.length) {
      array[index++] = aArr[i++];
    }
    int j = 0;
    while (j < aItems.length) {
      array[index++] = aItems[j++];
    }
    return array;
  }


  public static double[] reverse(double[] aArr) {
    if (aArr == null || aArr.length == 0) {
      return aArr;
    }
    double[] array = (double[]) Array.newInstance(double.class, aArr.length);
    int index = 0;
    int i = aArr.length - 1;
    while (i >= 0) {
      array[index++] = aArr[i--];
    }
    return array;
  }


}