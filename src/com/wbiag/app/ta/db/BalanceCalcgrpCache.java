package com.wbiag.app.ta.db;

import java.sql.*;
import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.wbiag.app.ta.model.*;
import com.workbrain.security.*;
import com.workbrain.server.cache.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Subclass of WorkbrainCache used for long-term caching of IAG_BALANCE_CALCGRP and IAG_TIME_CODE_BAL tables
 */

public class BalanceCalcgrpCache extends WorkbrainCache {

    private static final Logger logger = Logger.getLogger(BalanceCalcgrpCache.class);

    public static final String CACHE_NAME = "BalanceCalcgrpCache";
    public static final String IAG_BALANCE_CALCGRP_TABLE = "IAG_BALANCE_CALCGRP";
    public static final String IAG_TIME_CODE_BAL_TABLE = "IAG_TIME_CODE_BAL";
    public static final int ACCESS_INTERVAL = 10000;

    private static final BalanceCalcgrpCache instance = new BalanceCalcgrpCache(CACHE_NAME);

    public static final BalanceCalcgrpCache getInstance() {
        instance.setAccessInterval(ACCESS_INTERVAL);
        return instance;
    }

    private BalanceCalcgrpCache(String name) {
        super(name);
    }

    public void updateCacheContents(Connection conn) {
        logger.debug("Reloading data");

        put(IAG_BALANCE_CALCGRP_TABLE, new HashMap());
        put(IAG_TIME_CODE_BAL_TABLE, new HashMap());
    }

    public IagBalanceCalcgrpData getIagBalanceCalcgrpData(DBConnection conn,
        int calcgrpId,
        int balId) {
        IagBalanceCalcgrpData ret = null;

        HashMap balCalcgrp = (HashMap)get(IAG_BALANCE_CALCGRP_TABLE , conn);
        String key = calcgrpId + "-" + balId;
        if (!balCalcgrp.containsKey(key)) {
            RecordAccess ra = new RecordAccess(conn);
            List recs = ra.loadRecordData(new IagBalanceCalcgrpData(),
                              IAG_BALANCE_CALCGRP_TABLE,
                              "calcgrp_id", calcgrpId , "bal_id", balId);
            if (recs.size() > 0) {
                balCalcgrp.put(key , recs.get(0));
            }
            else {
                balCalcgrp.put(key , null);
            }
        }
        if (balCalcgrp.containsKey(key)) {
            IagBalanceCalcgrpData item = (IagBalanceCalcgrpData)balCalcgrp.get(key);
            if (item != null) {
                try {
                    ret = (IagBalanceCalcgrpData) item.clone();
                }
                catch (CloneNotSupportedException ex) {
                    throw new NestedRuntimeException(ex);
                }
            }
        }
        return ret;
    }

    public List getIagTimeCodeBalData(DBConnection conn,
        int tcodeId,
        int calcgrpId) {
        List ret = new ArrayList() ;

        HashMap tcodeCalcgrp = (HashMap)get(IAG_TIME_CODE_BAL_TABLE , conn);
        String key = tcodeId + "-" + calcgrpId;
        if (!tcodeCalcgrp.containsKey(key)) {
            RecordAccess ra = new RecordAccess(conn);
            List recs = ra.loadRecordData(new IagTimeCodeBalData(),
                              IAG_TIME_CODE_BAL_TABLE,
                              "tcode_id", tcodeId , "calcgrp_id", calcgrpId);
            tcodeCalcgrp.put(key , recs);
        }
        if (tcodeCalcgrp.containsKey(key)) {
            List items = (List)tcodeCalcgrp.get(key);
            if (items != null) {
                ret = RecordData.cloneList(items);
            }
        }
        return ret;
    }


}
