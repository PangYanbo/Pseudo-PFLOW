/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.ArrayUtils
 */
package jp.ac.ut.csis.pflow.drm.binary;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jp.ac.ut.csis.pflow.drm.binary.EBCDIC;
import org.apache.commons.lang.ArrayUtils;

public abstract class AParseDRM {
    public static final int RECORD_SIZE = 256;
    public static final String CHAR_ENCODING = "IBM1047";
    public static final String JP_ENCODING = "ISO2022JP";
    private List<FormatContent> _list;
    private int _size;

    protected AParseDRM() {
        this.init();
    }

    protected void addFormatContent(FormatContent content) {
        this._list.add(content);
        this._size += content.getTotalByteSize();
    }

    protected int getRecordSize() {
        return this._size;
    }

    protected String parseByte(byte[] input) {
        StringBuffer buf = new StringBuffer();
        byte[] byArray = input;
        int n = input.length;
        int n2 = 0;
        while (n2 < n) {
            byte b = byArray[n2];
            char c = EBCDIC.convert(b);
            buf.append(String.format("%c", Character.valueOf(c == '\u0000' ? (char)' ' : (char)c)));
            ++n2;
        }
        return buf.toString();
    }

    protected String parseMultiByte(byte[] input) throws IOException {
        int len = input.length;
        byte[] bytes = new byte[len * 2 + 3];
        bytes[0] = 27;
        bytes[1] = 36;
        bytes[2] = 66;
        int idx = 3;
        int i = 0;
        while (i < len) {
            bytes[idx++] = input[i];
            ++i;
        }
        return new String(bytes, JP_ENCODING);
    }

    protected String parseData(DataType type, byte[] input) throws IOException {
        switch (type) {
            case N: {
                return this.parseByte(input);
            }
            case X: {
                return this.parseByte(input);
            }
            case K: {
                return this.parseMultiByte(input);
            }
        }
        return null;
    }

    protected List<String> parseRecord(byte[] buf) throws IOException {
        if (!this.accept(buf)) {
            return null;
        }
        ArrayList<String> list = new ArrayList<String>();
        int idx0 = 0;
        int idx1 = 0;
        for (FormatContent content : this._list) {
            list.add(this.parseData(content.getType(), ArrayUtils.subarray((byte[])buf, (int)idx0, (int)(idx1 += content.getTotalByteSize()))));
            idx0 = idx1;
        }
        return list;
    }

    protected void init() {
        this._list = new ArrayList<FormatContent>();
        this._size = 0;
    }

    protected abstract List<String> listColumns();

    protected abstract boolean accept(byte[] var1);

    protected abstract boolean accept(File var1);

    protected static enum DataType {
        N(1),
        X(1),
        K(2);

        private int _byteSize;

        private DataType(int byteSize) {
            this._byteSize = byteSize;
        }

        protected int getByteSize() {
            return this._byteSize;
        }
    }

    protected class FormatContent {
        private DataType _type;
        private int _len;

        protected FormatContent(DataType type, int length) {
            this._type = type;
            this._len = length;
        }

        protected DataType getType() {
            return this._type;
        }

        protected int getLength() {
            return this._len;
        }

        protected int getTotalByteSize() {
            return this._type.getByteSize() * this._len;
        }
    }
}

