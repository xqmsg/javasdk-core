package com.xqmsg.sdk.v2.common;

/**
 * Created by ikechie on 2/7/20.
 */
/*
 * D-Bus Java Implementation Copyright (c) 2005-2006 Matthew Johnson This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of either the GNU Lesser General Public License Version 2 or the
 * Academic Free Licence Version 2.1. Full licence texts are included in the
 * COPYING file with this program.
 */
//package org.freedesktop.dbus;

/**
 * Class to represent unsigned 32-bit numbers.
 */
@SuppressWarnings("serial")
public class UInt32 extends Number implements Comparable<UInt32> {
    /** Maximum allowed value */
    public static final long MAX_VALUE = 4294967295L;
    /** Minimum allowed value */
    public static final long MIN_VALUE = 0;
    private long             value;

    /**
     * Create a UInt32 from a long.
     *
     * @param value
     *          Must be a valid integer within MIN_VALUE&ndash;MAX_VALUE
     * @throws NumberFormatException
     *           if value is not between MIN_VALUE and MAX_VALUE
     */
    public UInt32(long value) {
        this.value = value;
    }

    /**
     * Create a UInt32 from a String.
     *
     * @param value
     *          Must parse to a valid integer within MIN_VALUE&ndash;MAX_VALUE
     * @throws NumberFormatException
     *           if value is not an integer between MIN_VALUE and MAX_VALUE
     */
    public UInt32(String value) {
        this(Long.parseLong(value));
    }

    /** The value of this as a byte. */
    @Override
    public byte byteValue() {
        return (byte) value;
    }

    /**
     * Compare two UInt32s.
     *
     * @return 0 if equal, -ve or +ve if they are different.
     */
    public int compareTo(UInt32 other) {
        return (int) (value - other.value);
    }

    /** The value of this as a double. */
    @Override
    public double doubleValue() {
        return value;
    }

    /** Test two UInt32s for equality. */
    @Override
    public boolean equals(Object o) {
        return (o instanceof UInt32) && (((UInt32) o).value == value);
    }

    /** The value of this as a float. */
    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return (int) value;
    }

    /** The value of this as a int. */
    @Override
    public int intValue() {
        return (int) value;
    }

    /** The value of this as a long. */
    @Override
    public long longValue() {
        return /* (long) */value;
    }

    /** The value of this as a short. */
    @Override
    public short shortValue() {
        return (short) value;
    }

    /** The value of this as a string */
    @Override
    public String toString() {
        return "" + value;
    }
}
