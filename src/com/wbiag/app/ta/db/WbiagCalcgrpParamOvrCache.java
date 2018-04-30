package com.wbiag.app.ta.db;

import java.sql.*;
import java.util.*;
import java.util.Date;

import org.apache.log4j.*;
import com.wbiag.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.server.cache.*;
import com.workbrain.server.registry.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 *  Title: WbiagCalcgrpParamOvrCache
 *  Description: Subclass of WorkbrainCache used for long-term caching of WBIAG_CALCGRP_PARAM_OVR table
 *  Copyright: Copyright (c) 2005, 2006, 200nc.
 *
 * @author gtam@workbrain.com
*/
public class WbiagCalcgrpParamOvrCache extends WorkbrainCache {

    private static final Logger logger = Logger.getLogger(WbiagCalcgrpParamOvrCache.class);

    public static final String CACHE_NAME = "WbiagCalcgrpParamOvrCache";
    public static final String WBIAG_CALCGRP_PARAM_OVR_TABLE = "WBIAG_CALCGRP_PARAM_OVR";
    public static final int ACCESS_INTERVAL = 10000;

    private static final WbiagCalcgrpParamOvrCache instance = new WbiagCalcgrpParamOvrCache(CACHE_NAME);

    public static final WbiagCalcgrpParamOvrCache getInstance() {
        instance.setAccessInterval(ACCESS_INTERVAL);
        return instance;
    }

    private WbiagCalcgrpParamOvrCache(String name) {
        super(name);
    }

    public void updateCacheContents(Connection conn) {
        if(logger.isDebugEnabled()) { logger.debug("Reloading WbiagCalcgrpParamOvrCache."); }

        put(WBIAG_CALCGRP_PARAM_OVR_TABLE, new HashMap());
    }

    /**
     * Returns WbiagCalcgrpParamOvrData for given Calculation Group and Effective Date. Throughput cache.
     * @param DBConnection conn
     * @param int calcGrpId
     * @param Date effDate
     * @return WbiagCalcgrpParamOvrData
     */
    public WbiagCalcgrpParamOvrData getCalcgrpParamOvrByCalcgrpDateEff(DBConnection conn, 
            int calcGrpId, Date effDate) {
        WbiagCalcgrpParamOvrData ret = null;
        Integer calcGrpKey = new Integer(calcGrpId);
        HashMap calcgrps = (HashMap)get(WBIAG_CALCGRP_PARAM_OVR_TABLE , conn);
        if (!calcgrps.containsKey(calcGrpKey)) {
            if(logger.isDebugEnabled()) { logger.debug("Record not found, reloading WbiagCalcgrpParamOvrCache."); }
            RecordAccess ra = new RecordAccess(conn);
            List recs = ra.loadRecordDataOrderBy(new WbiagCalcgrpParamOvrData(),
                              WBIAG_CALCGRP_PARAM_OVR_TABLE,
                              "calcgrp_id", calcGrpId,
                              "wcpo_start_date");
            calcgrps.put(calcGrpKey, recs);
        }
        if (calcgrps.containsKey(calcGrpKey)) {
            List items = (List)calcgrps.get(calcGrpKey);
            if (items != null) {
                for ( int i = 0; i < items.size(); i++ ) {
                    WbiagCalcgrpParamOvrData calcgrpParam = (WbiagCalcgrpParamOvrData)items.get( i );
                    // *** find the first record whose start/end date contain the effectiveDate
                    if ( DateHelper.compare( calcgrpParam.getWcpoStartDate(), effDate ) <= 0 &&
                         DateHelper.compare( calcgrpParam.getWcpoEndDate(), effDate )  >= 0) {
                        try {
                            ret = (WbiagCalcgrpParamOvrData) calcgrpParam.clone();
                            if(logger.isDebugEnabled()) { logger.debug("Record found: " + ret); }
                            break;
                        }
                        catch (CloneNotSupportedException ex) {
                            throw new NestedRuntimeException(ex);
                        }
                    }
                }
            }
        }
        return ret;
    }
}