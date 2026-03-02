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

public class ParseDRM31
extends AParseDRM {
    protected static final List<String> COLUMNS = Arrays.asList("\u30ec\u30b3\u30fc\u30c9ID", "\u30ce\u30fc\u30c9\u756a\u53f7", "\u6b63\u898f\u5316x\u5ea7\u6a19", "\u6b63\u898f\u5316y\u5ea7\u6a19", "\u30ce\u30fc\u30c9\u7a2e\u5225\u30b3\u30fc\u30c9", "\u96a3\u63a5\uff12\u6b21\u30e1\u30c3\u30b7\u30e5\u30b3\u30fc\u30c9", "\u96a3\u63a5\uff12\u6b21\u30e1\u30c3\u30b7\u30e5\u63a5\u5408\u30ce\u30fc\u30c9\u756a\u53f7", "\u63a5\u7d9a\u30ea\u30f3\u30af\u672c\u6570", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(1)", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(2)", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(3)", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(4)", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(5)", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(6)", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(7)", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(8)", "\u4ea4\u5dee\u70b9\u6587\u5b57\u6570", "\u4ea4\u5dee\u70b9\u540d\u79f0\u6f22\u5b57", "\u30ab\u30ca\u6587\u5b57\u6570", "\u4ea4\u5dee\u70b9\u540d\u79f0\u30ab\u30ca", "\u4e88\u5099");

    @Override
    protected List<String> listColumns() {
        return COLUMNS;
    }

    @Override
    protected void init() {
        super.init();
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 6));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.K, 20));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 40));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.X, 102));
    }

    @Override
    protected boolean accept(byte[] buf) {
        byte[] b = new byte[]{buf[0], buf[1]};
        return StringUtils.equals((String)this.parseByte(b), (String)"31");
    }

    @Override
    protected boolean accept(File infile) {
        return infile.getName().endsWith(".mt");
    }
}

