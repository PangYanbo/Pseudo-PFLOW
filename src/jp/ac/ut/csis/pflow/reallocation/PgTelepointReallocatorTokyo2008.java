/*
 * Decompiled with CFR 0.152.
 */
package jp.ac.ut.csis.pflow.reallocation;

import jp.ac.ut.csis.pflow.dbi.PgLoader;
import jp.ac.ut.csis.pflow.reallocation.PgTelepointReallocator;
import jp.ac.ut.csis.pflow.reallocation.TelepointPOI;

public class PgTelepointReallocatorTokyo2008
extends PgTelepointReallocator {
    public static void main(String[] args) {
        try (PgLoader pgLoader = new PgLoader("localhost", "postgres", "your_password", "your_DB_name");){
            PgTelepointReallocatorTokyo2008 realloc = new PgTelepointReallocatorTokyo2008(pgLoader, "pt08tky.telepoint_reallocationtable");
            TelepointPOI poi = realloc.reallocate("00100", 5);
            System.out.println(poi);
        }
    }

    public PgTelepointReallocatorTokyo2008(PgLoader dbLoader, String tablename) {
        super(dbLoader, tablename);
    }

    @Override
    protected String[] getColumnNames(int purpose) {
        switch (purpose) {
            case 1: 
            case 2: 
            case 3: 
            case 7: 
            case 8: 
            case 10: 
            case 11: 
            case 12: 
            case 13: 
            case 14: {
                return new String[]{String.format("ratio%02d", purpose), "ratioE"};
            }
            case 4: 
            case 5: 
            case 6: 
            case 9: {
                return new String[]{String.format("ratio%02d", purpose), "ratioW", "ratioE"};
            }
            case 97: 
            case 99: {
                return new String[]{String.format("ratio%02d", 3), "ratioE"};
            }
        }
        return new String[]{"ratioE"};
    }
}

