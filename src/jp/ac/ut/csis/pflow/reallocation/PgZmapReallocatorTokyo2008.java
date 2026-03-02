/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.reallocation;

import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.reallocation.PgZmapReallocator;
import jp.ac.ut.csis.pflow.reallocation.ZmapPOI;

public class PgZmapReallocatorTokyo2008
extends PgZmapReallocator {
    public static void main(String[] args) {
        try (PgLoader pgLoader = new PgLoader("localhost", "postgres", "your_password", "your_DB_name");){
            PgZmapReallocatorTokyo2008 realloc = new PgZmapReallocatorTokyo2008(pgLoader, "pt08tky.zmap_reallocationtable");
            ZmapPOI poi = realloc.reallocate("00100", 2);
            System.out.println(poi);
        }
    }

    public PgZmapReallocatorTokyo2008(PgLoader dbLoader, String tablename) {
        super(dbLoader, tablename);
    }

    @Override
    protected String getColumnName(int purpose) {
        switch (purpose) {
            case 2: 
            case 4: 
            case 5: 
            case 6: 
            case 7: 
            case 8: 
            case 9: 
            case 10: 
            case 11: 
            case 12: 
            case 13: 
            case 14: {
                return PgZmapReallocator.ColumnName.B_RATIO.toString();
            }
            case 1: {
                return PgZmapReallocator.ColumnName.S_RATIO.toString();
            }
            case 3: 
            case 97: 
            case 99: {
                return PgZmapReallocator.ColumnName.H_RATIO.toString();
            }
        }
        return null;
    }
}

