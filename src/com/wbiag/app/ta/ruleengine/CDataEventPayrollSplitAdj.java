package com.wbiag.app.ta.ruleengine;

import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import java.lang.reflect.*;
/**
 * Custom event for CDataEventPayrollSplitAdj
 * Recreates adjustments for days that have records that span split time which
 * is retrieved from calcgrp_udf1
 * @deprecated    As of 5.0.2.0, use core classes
 */
public class CDataEventPayrollSplitAdj extends DataEvent {

    private static Logger logger = org.apache.log4j.Logger.getLogger(CDataEventPayrollSplitAdj.class);

    private final String SPLIT_TIME_FORMAT = "yyyyMMdd HHmmss";

    /**
     * checkAdjustmentsOnSplit
     * @param data
     * @param od
     * @param c
     */
    public void afterSave(WBData data, DBConnection c) {
        try {
            checkAdjustmentsOnSplit(data);
        }
        catch (Exception ex) {
            throw new NestedRuntimeException("Error in checkAdjustmentsOnSplit", ex);
        }
    }

    protected void checkAdjustmentsOnSplit(WBData data)  throws Exception{
        Date workDate = data.getWrksWorkDate();
        Date workDateYest = DateHelper.addDays(workDate, -1);
        Date workDateTom = DateHelper.addDays(workDate, 1);

        PayGroupData pg =  data.getCodeMapper().getPayGroupById(data.getPaygrpId());
        Date splitTime = getSplitTime(data , pg);
        if (logger.isDebugEnabled()) logger.debug("Split time for checkAdjustmentsOnSplit:" + splitTime);
        if (splitTime == null) {
            return;
        }

        if (logger.isDebugEnabled()) logger.debug("workDate :" + workDate + ", pg Start :" + pg.getPaygrpStartDate() + ", pgEnd :" + pg.getPaygrpEndDate() + ", pgAdj :" + pg.getPaygrpAdjustDate() );
        /**
         * A day is eligible if
         *   - it has loaded/final records that span split time
         *   - and
         *     - (is either a first day pay group)
         *     - (day before first day pay group which is an adjust date)
         */
        boolean hasRecsSpanningSplit =
            data.getRuleData().getWorkDetails().getWorkDetail(splitTime, null, true, null , true) != null
            ||
            data.getRuleData().getLoadedWorkDetails().getWorkDetail(splitTime, null, true, null , true) != null;
        boolean isFirstDay = DateHelper.equals(workDate , pg.getPaygrpStartDate() )
            && workDateYest.compareTo(pg.getPaygrpAdjustDate()) <= 0;
        boolean isLastDay = DateHelper.equals(workDateTom , pg.getPaygrpStartDate() )
            && DateHelper.equals(workDate , DateHelper.addDays(pg.getPaygrpStartDate() , -1 ))
            && workDate.compareTo(pg.getPaygrpAdjustDate()) <= 0;
        if (logger.isDebugEnabled()) logger.debug("hasRecsSpanningSplit=" + hasRecsSpanningSplit + " , isFirstDay=" + isFirstDay + " , isLastDay=" + isLastDay);
        boolean isDayEligible = (isFirstDay || isLastDay) && hasRecsSpanningSplit;
        if (isDayEligible) {
            if (logger.isDebugEnabled()) logger.debug("Day is eligible, checking adjustments");
            checkAdjustments(data , splitTime);
        }
        else {
            if (logger.isDebugEnabled()) logger.debug("Day is NOT eligible");
        }

    }

    protected Date getSplitTime(WBData data, PayGroupData pg) {
        Date split = null;
        CalcGroupData cgd = data.getCodeMapper().getCalcGroupById(data.getCalcgrpId());
        // *** split time
        String sSplitTime = cgd.getCalcgrpUdf1();
        if (StringHelper.isEmpty(sSplitTime)) {
            if (logger.isDebugEnabled()) logger.debug("Split time not defined for calc group:" + cgd.getCalcgrpName());
            return null;
        }
        Date splitTime = null;
        try {
            splitTime = DateHelper.parseDate(sSplitTime, SPLIT_TIME_FORMAT);
        }
        catch (Exception ex) {
            throw new RuntimeException ("Couldn't parse :" + sSplitTime + " for checkAdjustmentsOnSplit");
        }
        split = DateHelper.setTimeValues(pg.getPaygrpStartDate() ,  splitTime);
        return split;
    }

    protected void checkAdjustments(WBData data , Date split) throws Exception{

        int delCnt = deleteAdjustments(data);
        if (logger.isDebugEnabled()) logger.debug("Deleted :" + delCnt + " adjustments");

        WorkDetailList newDetails = data.getRuleData().getWorkDetails().duplicate();
        WorkDetailList oldDetails = data.getRuleData().getLoadedWorkDetails().duplicate() ;
        WorkSummaryData workSummary = data.getRuleData().getWorkSummary();
        WorkSummaryData oldWorkSummary = data.getRuleData().getLoadedWorkSummary();
        boolean doAdjustmentsForOldWrkSummary = true;
        boolean doAdjustments = true;
        List adjustments = new ArrayList();
        //CalcDataCache calcDataCache = data.getRuleData().getCalcDataCache();

        newDetails.removeWorkDetails(split, DateHelper.DATE_3000 );
        oldDetails.removeWorkDetails(split, DateHelper.DATE_3000 );
        // remove similars
        int currNewNdx = 0;
        while (currNewNdx < newDetails.size()) {
            boolean removedOne = false;
            int currOldNdx = 0;
            while (currOldNdx < oldDetails.size()) {
                WorkDetailData currNewDetail = newDetails.getWorkDetail(currNewNdx);
                if (currNewDetail.equalWithoutIds(oldDetails.getWorkDetail(currOldNdx), false)) {
                    // don't need to update anything
                    newDetails.remove(currNewNdx);
                    oldDetails.remove(currOldNdx);
                    removedOne = true;
                    break;
                }
                currOldNdx++;
            }
            if (!removedOne) {
                currNewNdx++;
            }
        }

        for (int i = 0; i < newDetails.size(); i++) {
            WorkDetailData wd = newDetails.getWorkDetail(i);
            if (oldDetails.size() > 0) {
                WorkDetailData oldWd = oldDetails.getWorkDetail(0);
                wd.setWrkdId(oldWd.getWrkdId());
                //calcDataCache.addBatchUpdate(wd);
                if (doAdjustmentsForOldWrkSummary) {
                    //Donot do adjustments for old wrks summary
                    //if the paygrp period of the old wrk summary is not in the adjustment period
                    //SINCE paygroup ids of the 2 work summaries can be different
                    WorkDetailAdjustData adjustment;
                    oldWd.setWrkdMinutes(-oldWd.getWrkdMinutes());
                    adjustment = WorkDetailAdjustData.create(oldWd, oldWorkSummary);
                    //calcDataCache.addBatchInsert( adjustment );
                    adjustments.add(adjustment);
                }
                if (doAdjustments) {
                    WorkDetailAdjustData adjustment;
                    adjustment = WorkDetailAdjustData.create(wd, workSummary);
                    //calcDataCache.addBatchInsert( adjustment );
                    adjustments.add(adjustment);
                }
                oldDetails.remove(0);
            } else {
                wd.setWrkdId( -1 );
                //calcDataCache.addBatchInsert(wd);
                if (doAdjustments) {
                    WorkDetailAdjustData adjustment =
                            WorkDetailAdjustData.create(wd, workSummary);
                    //calcDataCache.addBatchInsert( adjustment );
                    adjustments.add(adjustment);
                }
            }
        }
        for (int i = 0; i < oldDetails.size(); i++) {
            WorkDetailData wd = oldDetails.getWorkDetail(i);
            //calcDataCache.addBatchDelete(wd);
            if (doAdjustmentsForOldWrkSummary) {
                WorkDetailData negWd = wd.duplicate();
                negWd.setWrkdMinutes(-wd.getWrkdMinutes());
                WorkDetailAdjustData adjustment =
                        WorkDetailAdjustData.create(negWd, oldWorkSummary);
                //calcDataCache.addBatchInsert( adjustment );
                adjustments.add(adjustment);
            }
        }

        if (logger.isDebugEnabled()) logger.debug("Added adjustments for split: \n" + adjustments);
        if (adjustments.size() > 0) {
            insertDetailAdjustment(data.getDBconnection() , adjustments);
        }
    }

    private int deleteAdjustments(WBData data) throws Exception {
        List coreAdjs = findCoreAdjs(data);
        if (logger.isDebugEnabled()) logger.debug("Found : " + coreAdjs.size() + " core adjustment(s) to delete");
        // *** check adjustments created for this employee/date from CalcDataCache
        // *** since wrkdaIds are not stored in calcdataCache, we are checking by createDate
        if (coreAdjs.size() == 0) {
            return 0;
        }

        int upd = 0;
        java.sql.PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM work_detail_adjust WHERE emp_id = ? ");
            sb.append("AND wrkda_work_date = ? AND wrkda_adjust_date = ?");
            //vlo - TT2837 - only delete details, not premiums(wrkda_type = 'P')
            sb.append(" AND wrkda_type = 'D' ");
            ps = data.getDBconnection().prepareStatement(sb.toString());
            Iterator iter = coreAdjs.iterator();
            while (iter.hasNext()) {
                WorkDetailAdjustData item = (WorkDetailAdjustData) iter.next();
                ps.setInt(1, item.getEmpId());
                ps.setTimestamp(2,
                                new java.sql.Timestamp(item.getWrkdaWorkDate().getTime()));
                Date adjDate = item.getWrkdaAdjustDate();
                // oracle stores in seconds. Change this if >= 9i and DB is configured to do so
                if (data.getDBconnection().getDBServer().isOracle()) {
                    adjDate = DateHelper.truncateToSeconds(adjDate);
                }
                ps.setTimestamp(3, new java.sql.Timestamp(adjDate.getTime()));
                ps.addBatch();
            }
            int[] updA = ps.executeBatch();
            upd = updA.length;

        }
        finally {
            if (ps != null) ps.close();
        }
        return upd;
    }

    private List findCoreAdjs(WBData data) throws Exception{
        List ret = new ArrayList();
        Field fld = data.getRuleData().getCalcDataCache().getClass()
            .getDeclaredField("workDetAdjsToInsert");
        fld.setAccessible(true);
        List adjsCore = (List)fld.get(data.getRuleData().getCalcDataCache());
        Iterator iter = adjsCore.iterator();
        while (iter.hasNext()) {
            WorkDetailAdjustData item = (WorkDetailAdjustData)iter.next();
            if (item.getEmpId() == data.getEmpId()
                && DateHelper.equals(item.getWrksaWorkDate(), data.getWrksWorkDate())) {
                ret.add(item) ;
            }
        }

        return ret;
    }

    private void insertDetailAdjustment( DBConnection conn, List wdas ) throws Exception {
        Iterator iter = wdas.iterator();
        while (iter.hasNext()) {
            WorkDetailAdjustData wda = (WorkDetailAdjustData) iter.next();
            wda.setGeneratesPrimaryKeyValue(true);
            // always set it to 1 sec later to be safe
            Date oneSecLater = new java.util.Date(DateHelper.truncateToSeconds(new java.util.Date()).getTime() + 1000L) ;
            wda.setWrkdaAdjustDate( oneSecLater );
        }
        new RecordAccess(conn).insertRecordData(wdas, WorkDetailAdjustAccess.WORK_DETAIL_ADJUST_TABLE);
    }

}

