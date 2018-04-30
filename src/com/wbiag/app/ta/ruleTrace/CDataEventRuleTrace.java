package com.wbiag.app.ta.ruleTrace;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.wbiag.app.ta.ruleTrace.db.*;
import com.wbiag.app.ta.ruleTrace.engine.*;
import com.wbiag.app.ta.ruleTrace.model.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 *  @deprecated Core as of 5.0.3.0
 */
public class CDataEventRuleTrace extends DataEvent
{
    private static final String RULE_TRACE_ENTITY_CACHE = "RULE_TRACE";
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CDataEventRuleTrace.class);

    private static HashMap getRuleTraceMap(WBData data) {
        Hashtable entity = data.getRuleData().getCalcDataCache().getEntityCache();

        HashMap trcMap = (HashMap)entity.get(RULE_TRACE_ENTITY_CACHE);
        if (trcMap == null) {
            trcMap = new HashMap();
            entity.put(RULE_TRACE_ENTITY_CACHE, trcMap);
        }
        return trcMap;
    }

    /**
     * Returns RuleTrace map for given empId/date
     * @param data
     * @return
     */
    public static RuleTraceList getRuleTraceList(WBData data) {

        HashMap trcMap = getRuleTraceMap(data);
        EmployeeIdAndDate key = new EmployeeIdAndDate (data.getEmpId() , data.getWrksWorkDate());
        RuleTraceList trcList = (RuleTraceList)trcMap.get(key);
        if (trcList == null) {
            trcList = new RuleTraceList();
            trcMap.put(key , trcList);
        }
        return trcList;
    }

    public void afterSave(WBData data, DBConnection c)   {
        try {
            saveRuleTrace(data);
        }
        catch (Exception ex) {
            throw new NestedRuntimeException ("Error in saving Rule Trace data", ex);
        }
    }

    /**
     * Save rule trace data here
     * @param data WBData
     * @throws Exception
     */
    private void saveRuleTrace(WBData data) throws Exception{
        RuleTraceConfig cfg = RuleTraceConfig.getRuleTraceConfig(data.getDBconnection());
        boolean isEnabled = cfg.isEnabled
            (data.getCodeMapper().getCalcGroupById(data.getCalcgrpId()).getCalcgrpName()) ;
        if (!isEnabled) {
            return;
        }

        Map ruleTraces = getRuleTraceMap(data);
        EmployeeIdAndDate key = new EmployeeIdAndDate (data.getEmpId() , data.getWrksWorkDate()) ;
        RuleTraceList trcList = (RuleTraceList)ruleTraces.get(key) ;
        // *** i.e no rules have been executed
        if (trcList == null) {
            trcList = new RuleTraceList();
        }
        String trcXml = trcList.toXML(cfg);
        int wrksId = data.getRuleData().getWorkSummary().getWrksId();
        if (logger.isDebugEnabled()) logger.debug("Saved Rule Trace for empId:" + key.getEmpId() + ", date:" + key.getDate() + ", wrksId=" + wrksId);
        //System.out.println(trcXml + "\n\n");
        /** @todo to be batched later on */
        RuleTraceAccess acc = new RuleTraceAccess (data.getDBconnection());
        acc.save(wrksId , trcXml);
        ruleTraces.remove(key);
    }

    /**
     * Chancge Calcgroup to CalcGroup here at the beginning of calc engine
     * @param data
     * @param ol
     * @param c
     */
    public void afterWorkSummaryOverrides(WBData data, OverrideList ol, DBConnection c) {
        try {
            RuleTraceConfig cfg = RuleTraceConfig.getRuleTraceConfig(data.getDBconnection());
            boolean isEnabled = cfg.isEnabled
                (data.getCodeMapper().getCalcGroupById(data.getCalcgrpId()).getCalcgrpName()) ;
            if (!isEnabled) {
                //if (logger.isDebugEnabled()) logger.debug("Rule Trace not enabled for empId :" + data.getEmpId() + ", work date:" + data.getWrksWorkDate());
                return;
            }
            if (logger.isDebugEnabled()) logger.debug("Rule Trace enabled for empId :" + data.getEmpId() + ", work date:" + data.getWrksWorkDate());
            populateCalcGroupExt(data);
        }
        catch (Exception ex) {
            throw new NestedRuntimeException("Error in beforeOverrides" , ex);
        }
    }

    /**
     * This is a backdoor hack to populate tempCalculationGroups to fake RuleEngine to use CalculationGroupExt
     * @param data
     * @throws Exception
     */
    private void populateCalcGroupExt(WBData data) throws Exception{
        CalcDataCache cdc = data.getRuleData().getCalcDataCache();

        java.lang.reflect.Field fldTemp = cdc.getClass().getDeclaredField("tempCalculationGroups");
        fldTemp.setAccessible(true);
        Map tempCGs = (Map) fldTemp.get(cdc);

        java.lang.reflect.Field fldCache = cdc.getClass().getDeclaredField("calcGroupHistoryCache");
        fldCache.setAccessible(true);
        Hashtable calcGroupHistoryCache = (Hashtable) fldCache.get(cdc);

        CalculationGroup calcGroup = null;
        Date date = data.getWrksWorkDate();
        int calcGroupId = data.getRuleData().getWorkSummary().getCalcgrpId();
        Integer cgIdKey = new Integer(calcGroupId);
        try {
            com.workbrain.app.ta.db.CodeMapper codeMapper = data.getCodeMapper() ;
            if (calcGroupHistoryCache.containsKey(cgIdKey)) {
                Iterator histories = ( (List) calcGroupHistoryCache.get(cgIdKey)).
                    iterator();
                while (histories.hasNext()) {
                    CalcGroupHistoryData history = (CalcGroupHistoryData)
                        histories.
                        next();
                    if (DateHelper.isBetween(date, history.getCghStartDate(),
                                             history.getCghEndDate())) {
                        calcGroup = codeMapper.getCalcGroup(history.getCgvId());
                        break;
                    }
                }
            }

            if (calcGroup == null) {
                List historyList = new ArrayList();

                Iterator histories = new com.workbrain.app.ta.db.CalcGroupAccess(data.getDBconnection()).
                    loadCalcGroupHistories(
                    new int[] {calcGroupId}
                    , date, date).iterator();
                if (histories.hasNext()) {
                    CalcGroupHistoryData history = (CalcGroupHistoryData)
                        histories.
                        next();
                    historyList.add(history);
                    calcGroup = codeMapper.getCalcGroup(history.getCgvId());
                }

                calcGroupHistoryCache.put(cgIdKey, historyList);
            }
        } catch( SQLException se ) {
            throw new NestedRuntimeException( se );
        }

        if (calcGroup == null) {
            throw new NestedRuntimeException(
                "Calc Group History or Calc Group Version Not Found for CALCGRP_ID = "
                + calcGroupId + " on " + date);
        }
        CalculationGroupExt ret = new CalculationGroupExt(calcGroup);
        tempCGs.put(cgIdKey, ret);
    }

 }
