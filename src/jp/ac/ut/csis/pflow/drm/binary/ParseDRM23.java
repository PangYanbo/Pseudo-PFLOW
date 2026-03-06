/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 */
package jp.ac.ut.csis.pflow.drm.binary;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import jp.ac.ut.csis.pflow.drm.binary.AParseDRM;
import org.apache.commons.lang3.StringUtils;

public class ParseDRM23
extends AParseDRM {
    protected static final List<String> COLUMNS = Arrays.asList("\u30ec\u30b3\u30fc\u30c9ID", "\u30ce\u30fc\u30c9\uff11\u756a\u53f7", "\u30ce\u30fc\u30c9\uff12\u756a\u53f7", "\u30d5\u30a3\u30e9\u30fc", "\u30a2\u30a4\u30c6\u30e0\u5185\u30ec\u30b3\u30fc\u30c9\u756a\u53f7", "\u30ea\u30f3\u30af\u5185\u5c5e\u6027\u7dcf\u6570", "\u5c5e\u6027\u7a2e\u5225\u30b3\u30fc\u30c9", "\u8868\u793a\u30ec\u30d9\u30eb\u53c2\u8003\u30b3\u30fc\u30c9", "\u59cb\u70b9\u88dc\u9593\u70b9\u756a\u53f7", "\u59cb\u70b9\u5074\u63a5\u7d9a\u6709\u7121\u30b3\u30fc\u30c9", "\u7d42\u70b9\u88dc\u9593\u70b9\u756a\u53f7", "\u7d42\u70b9\u5074\u63a5\u7d9a\u6709\u7121\u30b3\u30fc\u30c9", "\u5c5e\u6027\u5ef6\u9577", "\u5c5e\u6027\u540d\u79f0\u8868\u793a\u53c2\u8003\u4f4d\u7f6e\u6b63\u898f\u5316\u5ea7\u6a19\uff08X\uff09", "\u5c5e\u6027\u540d\u79f0\u8868\u793a\u53c2\u8003\u4f4d\u7f6e\u6b63\u898f\u5316\u5ea7\u6a19\uff08Y\uff09", "\u6f22\u5b57\u6587\u5b57\u6570", "\u6f22\u5b57\u540d\u79f0", "\u30ab\u30ca\u6587\u5b57\u6570", "\u30ab\u30ca\u540d\u79f0", "\u5c5e\u6027\u7a2e\u5225\u30b3\u30fc\u30c9", "\u8868\u793a\u30ec\u30d9\u30eb\u53c2\u8003\u30b3\u30fc\u30c9", "\u59cb\u70b9\u88dc\u9593\u70b9\u756a\u53f7", "\u59cb\u70b9\u5074\u63a5\u7d9a\u6709\u7121\u30b3\u30fc\u30c9", "\u7d42\u70b9\u88dc\u9593\u70b9\u756a\u53f7", "\u7d42\u70b9\u5074\u63a5\u7d9a\u6709\u7121\u30b3\u30fc\u30c9", "\u5c5e\u6027\u5ef6\u9577", "\u5c5e\u6027\u540d\u79f0\u8868\u793a\u53c2\u8003\u4f4d\u7f6e\u6b63\u898f\u5316\u5ea7\u6a19\uff08X\uff09", "\u5c5e\u6027\u540d\u79f0\u8868\u793a\u53c2\u8003\u4f4d\u7f6e\u6b63\u898f\u5316\u5ea7\u6a19\uff08Y\uff09", "\u6f22\u5b57\u6587\u5b57\u6570", "\u6f22\u5b57\u540d\u79f0", "\u30ab\u30ca\u6587\u5b57\u6570", "\u30ab\u30ca\u540d\u79f0", "\u5c5e\u6027\u7a2e\u5225\u30b3\u30fc\u30c9", "\u8868\u793a\u30ec\u30d9\u30eb\u53c2\u8003\u30b3\u30fc\u30c9", "\u59cb\u70b9\u88dc\u9593\u70b9\u756a\u53f7", "\u59cb\u70b9\u5074\u63a5\u7d9a\u6709\u7121\u30b3\u30fc\u30c9", "\u7d42\u70b9\u88dc\u9593\u70b9\u756a\u53f7", "\u7d42\u70b9\u5074\u63a5\u7d9a\u6709\u7121\u30b3\u30fc\u30c9", "\u5c5e\u6027\u5ef6\u9577", "\u5c5e\u6027\u540d\u79f0\u8868\u793a\u53c2\u8003\u4f4d\u7f6e\u6b63\u898f\u5316\u5ea7\u6a19\uff08X\uff09", "\u5c5e\u6027\u540d\u79f0\u8868\u793a\u53c2\u8003\u4f4d\u7f6e\u6b63\u898f\u5316\u5ea7\u6a19\uff08Y\uff09", "\u6f22\u5b57\u6587\u5b57\u6570", "\u6f22\u5b57\u540d\u79f0", "\u30ab\u30ca\u6587\u5b57\u6570", "\u30ab\u30ca\u540d\u79f0", "\u8eca\u4e21\u901a\u884c\u898f\u5236\u30b3\u30fc\u30c9", "\u65bd\u8a2d\u7ba1\u7406\u30b3\u30fc\u30c9", "\u8eca\u4e21\u901a\u884c\u898f\u5236\u30b3\u30fc\u30c9", "\u65bd\u8a2d\u7ba1\u7406\u30b3\u30fc\u30c9", "\u8eca\u4e21\u901a\u884c\u898f\u5236\u30b3\u30fc\u30c9", "\u65bd\u8a2d\u7ba1\u7406\u30b3\u30fc\u30c9", "\u30d5\u30a3\u30e9\u30fc", "\u7d99\u7d9a\u30d5\u30e9\u30b0");

    @Override
    protected List<String> listColumns() {
        return COLUMNS;
    }

    @Override
    protected void init() {
        super.init();
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 4));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 4));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 3));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 3));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.K, 10));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 20));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 3));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 3));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.K, 10));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 20));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 3));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 3));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.K, 10));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 20));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 4));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 4));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 4));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 14));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 1));
    }

    @Override
    protected boolean accept(byte[] buf) {
        byte[] b = new byte[]{buf[0], buf[1]};
        return StringUtils.equals((String)this.parseByte(b), (String)"23");
    }

    @Override
    protected boolean accept(File infile) {
        return infile.getName().endsWith(".23");
    }
}

