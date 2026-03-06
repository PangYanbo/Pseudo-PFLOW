/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.StringUtils
 */
package jp.ac.ut.csis.pflow.drm.binary;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import jp.ac.ut.csis.pflow.drm.binary.AParseDRM;
import org.apache.commons.lang.StringUtils;

public class ParseDRM21
extends AParseDRM {
    protected static final List<String> COLUMNS = Arrays.asList("\u30ec\u30b3\u30fc\u30c9ID", "\u30ce\u30fc\u30c9\u756a\u53f7", "\u30a2\u30a4\u30c6\u30e0\u5185\u30ec\u30b3\u30fc\u30c9\u756a\u53f7", "\u6b63\u898f\u5316x\u5ea7\u6a19", "\u6b63\u898f\u5316y\u5ea7\u6a19", "\u6a19\u9ad8", "\u30ce\u30fc\u30c9\u7a2e\u5225\u30b3\u30fc\u30c9", "\u96a3\u63a5\uff12\u6b21\u30e1\u30c3\u30b7\u30e5\u30b3\u30fc\u30c9", "\u96a3\u63a5\uff12\u6b21\u30e1\u30c3\u30b7\u30e5\u63a5\u5408\u30ce\u30fc\u30c9\u756a\u53f7", "\u63a5\u7d9a\u30ea\u30f3\u30af\u672c\u6570", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(1)", "\u4ea4\u5dee\u70b9\u901a\u884c\u30b3\u30fc\u30c9(1)", "\u63a5\u7d9a\u89d2\u5ea6(1)", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(2)", "\u4ea4\u5dee\u70b9\u901a\u884c\u30b3\u30fc\u30c9(2)", "\u63a5\u7d9a\u89d2\u5ea6(2)", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(3)", "\u4ea4\u5dee\u70b9\u901a\u884c\u30b3\u30fc\u30c9(3)", "\u63a5\u7d9a\u89d2\u5ea6(3)", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(4)", "\u4ea4\u5dee\u70b9\u901a\u884c\u30b3\u30fc\u30c9(4)", "\u63a5\u7d9a\u89d2\u5ea6(4)", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(5)", "\u4ea4\u5dee\u70b9\u901a\u884c\u30b3\u30fc\u30c9(5)", "\u63a5\u7d9a\u89d2\u5ea6(5)", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(6)", "\u4ea4\u5dee\u70b9\u901a\u884c\u30b3\u30fc\u30c9(6)", "\u63a5\u7d9a\u89d2\u5ea6(6)", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(7)", "\u4ea4\u5dee\u70b9\u901a\u884c\u30b3\u30fc\u30c9(7)", "\u63a5\u7d9a\u89d2\u5ea6(7)", "\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(8)", "\u4ea4\u5dee\u70b9\u901a\u884c\u30b3\u30fc\u30c9(8)", "\u63a5\u7d9a\u89d2\u5ea6(8)", "\u4ea4\u5dee\u70b9\u540d\u79f0\uff08\u6f22\u5b57\u6587\u5b57\u6570\uff09", "\u4ea4\u5dee\u70b9\u540d\u79f0\uff08\u6f22\u5b57\uff09", "\u4ea4\u5dee\u70b9\u540d\u79f0\uff08\u30ab\u30ca\u6587\u5b57\u6570\uff09", "\u4ea4\u5dee\u70b9\u540d\u79f0\uff08\u30ab\u30ca\uff09", "\u30d5\u30a7\u30ea\u30fc\u63a5\u7d9a\u822a\u8def\u7dcf\u6570", "\u822a\u8def\u63a5\u7d9a2\u6b21\u30e1\u30c3\u30b7\u30e5\u30b3\u30fc\u30c9(1)", "\u822a\u8def\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(1)", "\u822a\u8def\u63a5\u7d9a2\u6b21\u30e1\u30c3\u30b7\u30e5\u30b3\u30fc\u30c9(2)", "\u822a\u8def\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(2)", "\u822a\u8def\u63a5\u7d9a2\u6b21\u30e1\u30c3\u30b7\u30e5\u30b3\u30fc\u30c9(3)", "\u822a\u8def\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(3)", "\u822a\u8def\u63a5\u7d9a2\u6b21\u30e1\u30c3\u30b7\u30e5\u30b3\u30fc\u30c9(4)", "\u822a\u8def\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(4)", "\u822a\u8def\u63a5\u7d9a2\u6b21\u30e1\u30c3\u30b7\u30e5\u30b3\u30fc\u30c9(5)", "\u822a\u8def\u63a5\u7d9a\u30ce\u30fc\u30c9\u756a\u53f7(5)", "\u30d5\u30a3\u30e9\u30fc", "\u7d99\u7d9a\u30d5\u30e9\u30b0");

    @Override
    protected List<String> listColumns() {
        return COLUMNS;
    }

    @Override
    protected void init() {
        super.init();
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 2));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 2));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 3));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 1));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 6));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 1));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 8));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 3));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 8));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 3));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 8));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 3));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 8));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 3));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 8));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 3));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 8));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 3));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 8));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 3));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 8));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 3));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 2));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.K, 10));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 2));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 20));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 2));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 6));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 6));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 6));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 6));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 6));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.X, 6));
        this.addFormatContent(new AParseDRM.FormatContent(this, AParseDRM.DataType.N, 1));
    }

    @Override
    protected boolean accept(byte[] buf) {
        byte[] b = new byte[]{buf[0], buf[1]};
        return StringUtils.equals((String)this.parseByte(b), (String)"21");
    }

    @Override
    protected boolean accept(File infile) {
        return infile.getName().endsWith(".mt");
    }
}

