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
 * Subclass of WorkbrainCache used for long-term caching of WBIAG_STATE and WBIAG_STATE_MINWGE tables
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class WbiagStateCache extends WorkbrainCache {

    private static final Logger logger = Logger.getLogger(WbiagStateCache.class);

    public static final String CACHE_NAME = "WbiagStateCache";
    public static final String WBIAG_STATE_TABLE = "WBIAG_STATE";
    public static final String WBIAG_STATE_MINWGE_TABLE = "WBIAG_STATE_MINWGE";
    public static final String REG_EMPLOYEE_STATE_COLUMN = "system/wbiag/EMPLOYEE_STATE_COLUMN";
    public static final int ACCESS_INTERVAL = 10000;

    private static final WbiagStateCache instance = new WbiagStateCache(CACHE_NAME);

    public static final WbiagStateCache getInstance() {
        instance.setAccessInterval(ACCESS_INTERVAL);
        return instance;
    }

    private WbiagStateCache(String name) {
        super(name);
    }

    public void updateCacheContents(Connection conn) {
        logger.debug("Reloading data");

        put(WBIAG_STATE_TABLE, new HashMap());
        put(WBIAG_STATE_MINWGE_TABLE, new HashMap());
    }

    /**
     * Returns WbiagStateData for given State Name. Loads full cache if empty
     * @param conn
     * @param stateName state abbreviation i.e ON
     * @return
     */
    public WbiagStateData getStateData(DBConnection conn,
        String stateName) {
        WbiagStateData ret = null;

        HashMap states = (HashMap)get(WBIAG_STATE_TABLE , conn);
        if (states.size() == 0) {
            RecordAccess ra = new RecordAccess(conn);
            List recs = ra.loadRecordData(new WbiagStateData(),
                              WBIAG_STATE_TABLE,
                              "");
            Iterator iter = recs.iterator();
            while (iter.hasNext()) {
                WbiagStateData item = (WbiagStateData)iter.next();
                states.put(item.getWistName() , item);
            }
        }
        if (states.containsKey(stateName)) {
            WbiagStateData item = (WbiagStateData)states.get(stateName);
            if (item != null) {
                try {
                    ret = (WbiagStateData) item.clone();
                }
                catch (CloneNotSupportedException ex) {
                    throw new NestedRuntimeException(ex);
                }
            }
        }
        return ret;
    }

    /**
     * Returns min wage by state and eff date. Throughput cache
     * @param conn
     * @param stateName state abbreviation i.e ON
     * @param effDate
     * @return
     */
    public WbiagStateMinwgeData getMinWageByStateEffDate(DBConnection conn,
        String stateName, Date effDate) {
        WbiagStateMinwgeData ret = null;

        WbiagStateData state = getStateData(conn , stateName);
        if (state == null) {
            throw new RuntimeException ("State not found :" +  stateName);
        }
        HashMap minwages = (HashMap)get(WBIAG_STATE_MINWGE_TABLE , conn);
        if (!minwages.containsKey(stateName)) {
            RecordAccess ra = new RecordAccess(conn);
            List recs = ra.loadRecordDataOrderBy(new WbiagStateMinwgeData(),
                              WBIAG_STATE_MINWGE_TABLE,
                              "wist_id", state.getWistId()  ,
                              "wistm_eff_date desc");
            minwages.put(stateName , recs);
        }
        if (minwages.containsKey(stateName)) {
            List items = (List)minwages.get(stateName);
            if (items != null) {
                for ( int i = 0; i < items.size(); i++ ) {
                    WbiagStateMinwgeData minwg = (WbiagStateMinwgeData)items.get( i );
                    // *** find the first rate prior to effectiveDate
                    if ( DateHelper.compare( minwg.getWistmEffDate(), effDate ) <= 0 ) {
                        try {
                            ret = (WbiagStateMinwgeData) minwg.clone();
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

    /**
     * Returns min wage by employee and eff date. Throughput cache.
     * Uses system/wbiag/EMPLOYEE_STATE_COLUMN to find which emp_val field maps to state name.
     * @param conn
     * @param emp   EmployeeData
     * @param effDate  effDate
     * @return
     */
    public WbiagStateMinwgeData getMinWageByEmpEffDate(DBConnection conn,
        EmployeeData emp, Date effDate) {
        WbiagStateMinwgeData ret = null;

        String empValCol = Registry.getVarString(REG_EMPLOYEE_STATE_COLUMN);
        if (StringHelper.isEmpty(empValCol)) {
            throw new RuntimeException ("EMPLOYEE_STATE_COLUMN not defined");
        }
        String stateName = null;
        try {
            stateName = (String) emp.getField(empValCol);
        }
        catch (Exception ex) {
            throw new NestedRuntimeException ("EMPLOYEE_STATE_COLUMN field not found :" + empValCol, ex);
        }
        if (StringHelper.isEmpty(stateName)) {
            throw new RuntimeException ("Employee does not have state defined in : " + empValCol);
        }
        return getMinWageByStateEffDate(conn , stateName, effDate);
    }

}
