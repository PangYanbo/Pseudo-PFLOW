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

public class ParseDRM22
extends AParseDRM {
    protected static final List<String> COLUMNS = Arrays.asList("\u30ec\u30b3\u30fc\u30c9ID", "\u30ce\u30fc\u30c9\uff11\u756a\u53f7", "\u30ce\u30fc\u30c9\uff12\u756a\u53f7", "\u30a2\u30a4\u30c6\u30e0\u5185\u30ec\u30b3\u30fc\u30c9\u756a\u53f7", "\u4e3b\u8def\u7dda\uff1a\u7ba1\u7406\u8005\u30b3\u30fc\u30c9", "\u4e3b\u8def\u7dda\uff1a\u9053\u8def\u7a2e\u5225\u30b3\u30fc\u30c9", "\u4e3b\u8def\u7dda\uff1a\u8def\u7dda\u756a\u53f7", "\u4e3b\u8def\u7dda\uff1a\u4e3b\u30fb\u5f93\u9053\u8def\u533a\u5206\u30b3\u30fc\u30c9", "\u91cd\u8981\u8def\u7dda\u7dcf\u6570", "\u91cd\u8981\u8def\u7dda\uff11\uff1a\u9053\u8def\u7a2e\u5225\u30b3\u30fc\u30c9", "\u91cd\u8981\u8def\u7dda\uff11\uff1a\u8def\u7dda\u756a\u53f7", "\u91cd\u8981\u8def\u7dda\uff11\uff1a\u4e3b\u30fb\u5f93\u9053\u8def\u533a\u5206\u30b3\u30fc\u30c9", "\u91cd\u8981\u8def\u7dda\uff12\uff1a\u9053\u8def\u7a2e\u5225\u30b3\u30fc\u30c9", "\u91cd\u8981\u8def\u7dda\uff12\uff1a\u8def\u7dda\u756a\u53f7", "\u91cd\u8981\u8def\u7dda\uff12\uff1a\u4e3b\u30fb\u5f93\u9053\u8def\u533a\u5206\u30b3\u30fc\u30c9", "\u91cd\u8981\u8def\u7dda\uff13\uff1a\u9053\u8def\u7a2e\u5225\u30b3\u30fc\u30c9", "\u91cd\u8981\u8def\u7dda\uff13\uff1a\u8def\u7dda\u756a\u53f7", "\u91cd\u8981\u8def\u7dda\uff13\uff1a\u4e3b\u30fb\u5f93\u9053\u8def\u533a\u5206\u30b3\u30fc\u30c9", "\u884c\u653f\u533a\u57df\u30b3\u30fc\u30c9", "\u30ea\u30f3\u30af\u9577\uff08\u8a08\u7b97\u5024\uff09", "\u30ea\u30f3\u30af\u7a2e\u5225\u30b3\u30fc\u30c9", "\u30d3\u30fc\u30b3\u30f3\u6709\u7121\u30b3\u30fc\u30c9", "\u81ea\u52d5\u8eca\u5c02\u7528\u9053\u8def\u30b3\u30fc\u30c9", "\u6709\u6599\u9053\u8def\u30b3\u30fc\u30c9", "\u30ea\u30f3\u30af\u901a\u884c\u53ef\u30fb\u4e0d\u53ef\u30b3\u30fc\u30c9", "\u7570\u5e38\u6c17\u8c61\u6642\u901a\u884c\u898f\u5236\u533a\u9593\u7a2e\u5225\u30b3\u30fc\u30c9", "\u8eca\u4e21\u91cd\u91cf\u5236\u9650\u6709\u7121\u30b3\u30fc\u30c9", "\u8eca\u4e21\u9ad8\u3055\u5236\u9650\u6709\u7121\u30b3\u30fc\u30c9", "\u8eca\u4e21\u5e45\u5236\u9650\u6709\u7121\u30b3\u30fc\u30c9", "\u9053\u8def\u5e45\u54e1\u533a\u5206\u30b3\u30fc\u30c9", "\u8eca\u7dda\u6570\u30b3\u30fc\u30c9", "\u8eca\u9053\u5e45\u54e1", "\u6700\u5c0f\u8eca\u9053\u90e8\u5fa9\u54e1", "\u4e2d\u592e\u5e2f\u5fa9\u54e1", "\u4e2d\u592e\u5e2f\u8a2d\u7f6e\u5ef6\u9577", "\uff11\uff12\u6642\u9593\u4ea4\u901a\u91cf", "\u65c5\u884c\u6642\u9593\uff08\u30d4\u30fc\u30af\u6642\uff09", "\u4ea4\u901a\u898f\u5236\u7a2e\u5225\u30b3\u30fc\u30c9", "\u4ea4\u901a\u898f\u5236\u6761\u4ef6\u7a2e\u5225\u30b3\u30fc\u30c9", "\u51ac\u5b63\u901a\u884c\u4e0d\u53ef\u30b3\u30fc\u30c9", "\u898f\u5236\u901f\u5ea6\u30b3\u30fc\u30c9", "\u7279\u8eca\u901a\u884c\u30b7\u30b9\u30c6\u30e0\u5bfe\u8c61\u30b3\u30fc\u30c9", "\u30ea\u30f3\u30af\u5185\u5c5e\u6027\u6709\u7121\u30b3\u30fc\u30c9", "\u88dc\u9593\u70b9\u7dcf\u6570", "X\u5ea7\u6a19\uff08\uff11\uff09", "Y\u5ea7\u6a19\uff08\uff11\uff09", "X\u5ea7\u6a19\uff08\uff12\uff09", "Y\u5ea7\u6a19\uff08\uff12\uff09", "X\u5ea7\u6a19\uff08\uff13\uff09", "Y\u5ea7\u6a19\uff08\uff13\uff09", "X\u5ea7\u6a19\uff08\uff14\uff09", "Y\u5ea7\u6a19\uff08\uff14\uff09", "X\u5ea7\u6a19\uff08\uff15\uff09", "Y\u5ea7\u6a19\uff08\uff15\uff09", "X\u5ea7\u6a19\uff08\uff16\uff09", "Y\u5ea7\u6a19\uff08\uff16\uff09", "X\u5ea7\u6a19\uff08\uff17\uff09", "Y\u5ea7\u6a19\uff08\uff17\uff09", "X\u5ea7\u6a19\uff08\uff18\uff09", "Y\u5ea7\u6a19\uff08\uff18\uff09", "X\u5ea7\u6a19\uff08\uff19\uff09", "Y\u5ea7\u6a19\uff08\uff19\uff09", "X\u5ea7\u6a19\uff08\uff11\uff10\uff09", "Y\u5ea7\u6a19\uff08\uff11\uff10\uff09", "X\u5ea7\u6a19\uff08\uff11\uff11\uff09", "Y\u5ea7\u6a19\uff08\uff11\uff11\uff09", "X\u5ea7\u6a19\uff08\uff11\uff12\uff09", "Y\u5ea7\u6a19\uff08\uff11\uff12\uff09", "X\u5ea7\u6a19\uff08\uff11\uff13\uff09", "Y\u5ea7\u6a19\uff08\uff11\uff13\uff09", "X\u5ea7\u6a19\uff08\uff11\uff14\uff09", "Y\u5ea7\u6a19\uff08\uff11\uff14\uff09", "X\u5ea7\u6a19\uff08\uff11\uff15\uff09", "Y\u5ea7\u6a19\uff08\uff11\uff15\uff09", "X\u5ea7\u6a19\uff08\uff11\uff16\uff09", "Y\u5ea7\u6a19\uff08\uff11\uff16\uff09", "\u7570\u5e38\u6c17\u8c61\u6642\u901a\u884c\u898f\u5236\u533a\u9593\u7a2e\u5225\u30b3\u30fc\u30c9\u304c\uff12\u306e\u6642\u306e\u6c17\u8c61\u7b49\u57fa\u6e96\u5024", "\u5f62\u72b6\u30c7\u30fc\u30bf\u53d6\u5f97\u8cc7\u6599\u30b3\u30fc\u30c9", "\u4e00\u822c\u56fd\u9053\u30fb\u6307\u5b9a\u533a\u9593\u8a72\u5f53\u30b3\u30fc\u30c9", "\u7d99\u7d9a\u30d5\u30e9\u30b0");

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
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 4));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 4));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 4));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 4));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 3));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 3));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 3));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 4));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 4));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 3));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 5));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 2));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
        this.addFormatContent(new FormatContent(AParseDRM.DataType.N, 1));
    }

    @Override
    protected boolean accept(byte[] buf) {
        byte[] b = new byte[]{buf[0], buf[1]};
        return StringUtils.equals((String)this.parseByte(b), (String)"22");
    }

    @Override
    protected boolean accept(File infile) {
        return infile.getName().endsWith(".mt");
    }
}

