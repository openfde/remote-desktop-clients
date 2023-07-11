/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright (C) 2011-2019 Brian P. Hinz
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */

//
// rdr::OutStream marshalls data into a buffer stored in RDR (RFB Data
// Representation).
//

package com.iiordanov.bVNC;

abstract public class OutStream {

    // check() ensures there is buffer space for at least one item of size
    // itemSize bytes.  Returns the number of items which fit (up to a maximum
    // of nItems).

    public final int check(int itemSize, int nItems) throws Exception {
        int nAvail;

        if (itemSize > (end - ptr)) {
            return overrun(itemSize, nItems);
        }

        nAvail = (end - ptr) / itemSize;
        return Math.min(nAvail, nItems);
    }

    public final void check(int itemSize) throws Exception {
        if (ptr + itemSize > end)
            overrun(itemSize, 1);
    }

    public final byte[] getbuf() { return b; }
    public final int getptr() { return ptr; }
    public final int getend() { return end; }
    public final void setptr(int p) { ptr = p; }
    // writeU/SN() methods write unsigned and signed N-bit integers.

    public void writeU8(int u) throws Exception {
        check(1);
        b[ptr++] = (byte) u;
        flush();
    }

    public void writeU16(int u) throws Exception {
        check(2);
        b[ptr++] = (byte) (u >> 8);
        b[ptr++] = (byte) u;
        flush();
    }

    public void writeU32(int u) throws Exception {
        check(4);
        b[ptr++] = (byte) (u >> 24);
        b[ptr++] = (byte) (u >> 16);
        b[ptr++] = (byte) (u >> 8);
        b[ptr++] = (byte) u;
        flush();
    }

    public final void pad(int bytes) throws Exception {
        while (bytes-- > 0) writeU8(0);
    }

    // writeBytes() writes an exact number of bytes from an array at an offset.

    public void writeBytes(byte[] data, int dataPtr, int length) throws Exception {
        int dataEnd = dataPtr + length;
        while (dataPtr < dataEnd) {
            int n = check(1, dataEnd - dataPtr);
            System.arraycopy(data, dataPtr, b, ptr, n);
            ptr += n;
            dataPtr += n;
        }
        flush();
    }

    // length() returns the length of the stream.

    abstract public int length();

    // flush() requests that the stream be flushed.

    public void flush() throws Exception {
    }

    abstract protected int overrun(int itemSize, int nItems) throws Exception;

    protected OutStream() {
    }

    static final int maxMessageSize = 8192;
    protected byte[] b = new byte[maxMessageSize];
    protected int ptr;
    protected int end;

    public void write(byte b[]) throws Exception {
        this.writeBytes(b, 0, b.length);
    }

    public void write(byte b[], int off, int len) throws Exception {
        this.writeBytes(b, off, len);
    }

    public void write(int i) throws Exception {
        this.writeU32(i);
    }

    public final void writeS8(int s) throws Exception { writeU8(s); }
    public final void writeS16(int s) throws Exception { writeU16(s); }
    public final void writeS32(int s) throws Exception { writeU32(s); }
}
