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

public class ParseDRM32
extends AParseDRM {
    protected static final List<String> COLUMNS = Arrays.asList("\u30ec\u30b3\u30fc\u30c9ID", "\u30ce\u30fc\u30c91\u756a\u53f7", "\u30ce\u30fc\u30c92\u756a\u53f7", "\u30a2\u30a4\u30c6\u30e0\u5185\u30ec\u30b3\u30fc\u30c9\u756a\u53f7", "\u7ba1\u7406\u8005\u30b3\u30fc\u30c9 ", "\u9053\u8def\u7a2e\u5225\u30b3\u30fc\u30c9 ", "\u884c\u653f\u533a\u57df\u30b3\u30fc\u30c9", "\u30ea\u30f3\u30af\u9577\uff08\u8a08\u7b97\u5024\uff09", "\u9053\u8def\u5e45\u54e1\u533a\u5206\u30b3\u30fc\u30c9", "\u8eca\u7dda\u6570\u30b3\u30fc\u30c9", "\u4ea4\u901a\u898f\u5236\u7a2e\u5225\u30b3\u30fc\u30c9", "\u4ea4\u901a\u898f\u5236\u6761\u4ef6\u7a2e\u5225\u30b3\u30fc\u30c9", "\u5bfe\u5fdc\u57fa\u672c\u9053\u8def\u30ea\u30f3\u30af\u756a\u53f7(\u30ce\u30fc\u30c91\u756a\u53f7)", "\u5bfe\u5fdc\u57fa\u672c\u9053\u8def\u30ea\u30f3\u30af\u756a\u53f7(\u30ce\u30fc\u30c92\u756a\u53f7)", "\u88dc\u9593\u70b9\u7dcf\u6570", "\u88dc\u9593\u70b9(1) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(1) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(2) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(2) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(3) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(3) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(4) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(4) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(5) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(5) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(6) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(6) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(7) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(7) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(8) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(8) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(9) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(9) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(10) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(10) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(11) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(11) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(12) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(12) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(13) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(13) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(14) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(14) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(15) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(15) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(16) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(16) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(17) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(17) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(18) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(18) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(19) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(19) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(20) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(20) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u88dc\u9593\u70b9(21) \u6b63\u898f\u5316x\u5ea7\u6a19", "\u88dc\u9593\u70b9(21) \u6b63\u898f\u5316y\u5ea7\u6a19", "\u30ea\u30f3\u30af\u5185\u5c5e\u6027\u6709\u7121\u30b3\u30fc\u30c9", "\u30ea\u30f3\u30af\u901a\u884c\u53ef\u30fb\u4e0d\u53ef\u30b3\u30fc\u30c9", "\u30d5\u30a3\u30e9\u30fc", "\u7d99\u7d9a\u30d5\u30e9\u30b0");

    @Override
    protected List<String> listColumns() {
        return COLUMNS;
    }

    @Override
    protected void init() {
        super.init();
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.X, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.X, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.X, 4));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 3));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.X, 2));
        this.addFormatContent(new AParseDRM.FormatContent(AParseDRM.DataType.N, 1));
    }

    @Override
    protected boolean accept(byte[] buf) {
        byte[] b = new byte[]{buf[0], buf[1]};
        return StringUtils.equals((String)this.parseByte(b), (String)"32");
    }

    @Override
    protected boolean accept(File infile) {
        return infile.getName().endsWith(".mt");
    }
}

