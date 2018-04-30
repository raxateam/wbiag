package com.wbiag.app.ta.ruleengine;

import java.util.Date;

import com.wbiag.app.ta.db.WbiagCalcgrpParamOvrCache;
import com.wbiag.app.ta.model.WbiagCalcgrpParamOvrData;
import com.workbrain.app.ta.model.RuleData;
import com.workbrain.app.ta.ruleengine.DataEvent;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.StringHelper;

/**
 *  Title: DataEventCalcgrpShiftBreakParam
 *  Description: Data Event for overriding global registry parameters
 *               PARAM_NO_SWIPE_FOR_BREAKS and ALLOW_PARTIAL_BREAKS
 *  Copyright: Copyright (c) 2005, 2006, 200nc.
 *
 * @author gtam@workbrain.com
*/
public class DataEventCalcgrpShiftBreakParam extends DataEvent {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DataEventCalcgrpShiftBreakParam.class);
    private ThreadLocal localConn = null;
    
    /**
     * Sets the class level connection object.
     * @param WBData data
     * @param DBConnection c
     */ 
    public void beforeApplyClocks(WBData data, DBConnection c)   {
        
        if(localConn == null) {
            localConn = new ThreadLocal();
        }
        localConn.set(c);
    }

    /**
     * Unsets the class level connection object.
     * @param WBData data
     * @param DBConnection c
     */ 
    public void afterApplyClocks(WBData data, DBConnection c)   {
        localConn = null;
    }
    
    /**
     * Gets the calc group specific NO_SWIPE_FOR_BREAKS parameter if specified.
     * Otherwise, return the supplied global registry setting.
     * @param boolean noSwipeForBreaks //The supplied global registry setting.
     * @param RuleData ruleData
     * @return boolean flag
     */
    public boolean getNoSwipeForBreaks(boolean noSwipeForBreaks, RuleData ruleData){
        // Set global Workbrain no swipe for breaks parameter
        boolean flag = noSwipeForBreaks;
        if(logger.isDebugEnabled()) { logger.debug("Global NoSwipeForBreaks parameter: " + flag); }
        DBConnection conn = (DBConnection)localConn.get();
        
        WbiagCalcgrpParamOvrCache wbiagCalcgrpParamOvrCache = WbiagCalcgrpParamOvrCache.getInstance();
        WbiagCalcgrpParamOvrData wbiagCalcgrpParamOvrData = null;
        
        // Get entry for employee’s calc group, and processing date from WbiagCalcgrpParamOvr Cache
        Date effDate = ruleData.getWrksWorkDate();
        int calcgrpId = ruleData.getEmployeeData().getCalcgrpId();
        wbiagCalcgrpParamOvrData = wbiagCalcgrpParamOvrCache.getCalcgrpParamOvrByCalcgrpDateEff(conn, calcgrpId, effDate);

        // Return calc group value if it exists, otherwise return global setting.
        if (wbiagCalcgrpParamOvrData != null) {
            if (wbiagCalcgrpParamOvrData.getWcpoNoSwipeForBreaks() != null) {
                flag = StringHelper.equalsIgnoreCase(wbiagCalcgrpParamOvrData.getWcpoNoSwipeForBreaks(),"True") ||
                        StringHelper.equalsIgnoreCase(wbiagCalcgrpParamOvrData.getWcpoNoSwipeForBreaks(),"Yes");
                if(logger.isDebugEnabled()) { logger.debug("Calcgrp level NoSwipeForBreaks parameter found: " + flag); }
            }
        }
        return flag;
    }   

    /**
     * Gets the calc group specific ALLOW_PARTIAL_BREAKS parameter if specified.
     * Otherwise, return the supplied global registry setting.
     * @param boolean allowPartialBreaks //The supplied global registry setting.
     * @param RuleData ruleData
     * @return boolean flag
     */
    public boolean getAllowPartialBreaks(boolean allowPartialBreaks, RuleData ruleData){
        // Set global Workbrain no swipe for breaks parameter
        boolean flag = allowPartialBreaks;
        if(logger.isDebugEnabled()) { logger.debug("Global NoSwipeForBreaks parameter: " + flag); }
        DBConnection conn = (DBConnection)localConn.get();
        
        WbiagCalcgrpParamOvrCache wbiagCalcgrpParamOvrCache = WbiagCalcgrpParamOvrCache.getInstance();
        WbiagCalcgrpParamOvrData wbiagCalcgrpParamOvrData = null;
        
        // Get entry for employee’s calc group, and processing date from WbiagCalcgrpParamOvr Cache
        Date effDate = ruleData.getWrksWorkDate();
        int calcgrpId = ruleData.getEmployeeData().getCalcgrpId();
        wbiagCalcgrpParamOvrData = wbiagCalcgrpParamOvrCache.getCalcgrpParamOvrByCalcgrpDateEff(conn, calcgrpId, effDate);

        // Return calc group value if it exists, otherwise return global setting.
        if (wbiagCalcgrpParamOvrData != null) {
            if (wbiagCalcgrpParamOvrData.getWcpoAllowPartialBreaks() != null) {
                flag = StringHelper.equalsIgnoreCase(wbiagCalcgrpParamOvrData.getWcpoAllowPartialBreaks(),"True") ||
                    StringHelper.equalsIgnoreCase(wbiagCalcgrpParamOvrData.getWcpoAllowPartialBreaks(),"Yes");
                if(logger.isDebugEnabled()) { logger.debug("Calcgrp level AllowPartialBreaks parameter found: " + flag); }
            }
        }
        return flag;
   }
    
}
