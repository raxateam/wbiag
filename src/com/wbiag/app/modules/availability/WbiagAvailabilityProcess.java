package com.wbiag.app.modules.availability;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.modules.availability.*;
import com.workbrain.app.modules.availability.model.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.scheduler.*;
import com.workbrain.app.scheduler.enterprise.ScheduledJob;
/**
 * Process class to synch WBIAG_AVAL tables based on given context.
 * @deprecated As of 5.0, use core Make Availability Reportable Task.
 */
public class WbiagAvailabilityProcess {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(WbiagAvailabilityProcess.class);

    private static final int MAX_RANGE_DAYS = 31;
    private static final int EMP_CHUNK_SIZE = 100;

    public WbiagAvailabilityProcess(){
    }


    /**
     * Creates summary/details based on context
     * @param context
     * @throws Exception
     */
    public WbiagAvailabilityProcessResult process(WbiagAvailabilityProcessContext context) throws Exception{
        if (context.conn == null || context.startDate == null
            || context.endDate == null) {
            throw new RuntimeException ("Connection, start and end date date must be supplied");
        }
        if (!context.recreatesSummaryDetail && context.lastRun == null) {
            throw new RuntimeException ("Last run must be supplied when recreatesSummaryDetail is true");
        }
        if (DateHelper.dateDifferenceInDays(context.endDate , context.startDate ) > MAX_RANGE_DAYS) {
            throw new RuntimeException ("Date range cannot exceed " + MAX_RANGE_DAYS + " days");
        }
        if (context.endDate.before(context.startDate)) {
            throw new RuntimeException ("End date cannot precede start date");
        }
        if (logger.isDebugEnabled()) logger.debug("Started Availability Process with context :\n(" + context + ")");

        IntegerList emps = findEligibleEmployees(context) ;
        WbiagAvailabilityProcessResult res = new WbiagAvailabilityProcessResult();
        res.eligibleEmpCount = emps.size();

        if (emps == null || emps.size() == 0) {
            if (logger.isDebugEnabled()) logger.debug("No employees found matching critera");
            return res;
        }
        if (logger.isDebugEnabled()) logger.debug("Found :" + emps.size() + " eligible employees");

        if (context.recreatesSummaryDetail) {
            addProcessTransactionWithPurge(emps.toIntArray(), context, res);
        }
        else{
            // *** find changed emps and purge
            EmployeeIdAndDateList changedEmpDates = new EmployeeIdAndDateList(false);
            if (context.runsForChangedEmps) {
                changedEmpDates = findChangedEmployeeDates(emps, context);
                if (logger.isDebugEnabled()) logger.debug("Found :" + changedEmpDates.size() + " changed empdates");
                if (changedEmpDates != null && changedEmpDates.size() > 0) {
                    int[] empIdsChanged = getEmpIdsFromEmpDateList(
                        changedEmpDates);
                    addProcessTransactionWithPurge(empIdsChanged, context, res);
                }
            }
            if (context.runsForMissingSummaries) {
                // *** find missing emps, no need to purge existing summary/detail here
                EmployeeIdAndDateList missingEmpDates =
                    findMissingEmployeeDates(emps, context);
                // *** remove previously processed changed empdates, if any
                missingEmpDates.removeAll(changedEmpDates);
                if (logger.isDebugEnabled()) logger.debug("Found :" + missingEmpDates.size() +  " missing empdates");
                if (missingEmpDates != null && missingEmpDates.size() > 0) {
                    addProcessTransaction(missingEmpDates, context, res);
                }
            }

        }

        return res;
    }

    /**
     * Processes each transaction and commits. Recoverable if it fails since the task always
     * finds missing records
     * @param context
     * @param transactions
     * @throws SQLException
     */
    private void processTransaction(WbiagAvailabilityProcessContext context,
                                     SummaryDetailTransaction transaction,
                                     WbiagAvailabilityProcessResult res) throws Exception{
        if (transaction == null) {
            if (logger.isDebugEnabled()) logger.debug("No transaction to process");
            return;
        }
        if (logger.isDebugEnabled()) logger.debug("Starting executing transactions");
        WbiagAvailabilityAccess acc =  new WbiagAvailabilityAccess (context.conn);
        if (transaction.purgeEmpIds != null) {
            acc.deleteSummaries(transaction.purgeEmpIds, transaction.purgeStartDate,
                                transaction.purgeEndDate);
            if (logger.isDebugEnabled()) logger.debug("Deleted summaries for : " + transaction.purgeEmpIds.length + " employees");
        }
        acc.insertRecordData(transaction.insertSummaries,
                             WbiagAvailabilityAccess.WBIAG_AVAL_SUMMARY_TABLE);
        if (logger.isDebugEnabled()) logger.debug("Inserted " + transaction.insertSummaries.size() +   " summaries");
        res.summariesInsertedCount += transaction.insertSummaries.size();

        acc.insertRecordData(transaction.insertDetails,
                             WbiagAvailabilityAccess.WBIAG_AVAL_DETAIL_TABLE);
        if (logger.isDebugEnabled()) logger.debug("Inserted " + transaction.insertDetails.size() +" details");
        res.detailsInsertedCount += transaction.insertDetails.size();

        if (context.shouldCommit) {
            context.conn.commit();
        }

        if (logger.isDebugEnabled()) logger.debug("TOTAL Inserted - Summaries :" + res.summariesInsertedCount + ", Details :" + res.detailsInsertedCount);
    }

    /**
     * Add employees in chunks for transaction consistency. These employee dates
     * will be purged from summary/detail before insertion.
     * @param empIds
     * @param context
     * @param transactions
     * @throws Exception
     */
    private void addProcessTransactionWithPurge(int[] empIds,
                                   WbiagAvailabilityProcessContext context,
                                   WbiagAvailabilityProcessResult res) throws Exception{


        IntArrayIterator iter = new IntArrayIterator(empIds, EMP_CHUNK_SIZE);
        while (iter.hasNext()) {
            int[] empIdsChunk = iter.next();
            SummaryDetailTransaction sdtran = new SummaryDetailTransaction();
            sdtran.purgeEmpIds = empIdsChunk;
            sdtran.purgeStartDate = context.startDate;
            sdtran.purgeEndDate = context.endDate;
            createSummaryDetails(context.conn , empIdsChunk, context.startDate, context.endDate, sdtran, null);
            processTransaction(context, sdtran, res);
            if (isInterrupted(context)) {
                return;
            }
        }
    }

    /**
     * Add employee dates in emp chunks for transaction consistency.
     * Creates summary detail transaction for the maximum range of the chunk.
     * @param empDates
     * @param context
     * @param transactions
     * @throws Exception
     */
    private void addProcessTransaction(EmployeeIdAndDateList empDates,
                                   WbiagAvailabilityProcessContext context,
                                   WbiagAvailabilityProcessResult res) throws Exception{
        Map empDatesMap = empDates.getMapOfEmployeeDates();
        Iterator iter = empDatesMap.entrySet().iterator();
        int cnt = 1;
        EmployeeIdAndDateList subList = new EmployeeIdAndDateList ();
        IntegerList subEmps = new IntegerList ();
        // *** want to find out the optimal range for employees being processed
        Date maxEndDate = context.startDate; Date minStartDate = context.endDate;
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            int empId = Integer.parseInt((( String)entry.getKey())) ;
            subEmps.add(empId);
            List dates = (List)entry.getValue();
            Iterator iterD = dates.iterator();
            while (iterD.hasNext()) {
                Date item = (Date)iterD.next();
                if (item.before(minStartDate)) {
                    minStartDate = item;
                }
                if (item.after(maxEndDate)) {
                    maxEndDate = item;
                }
                subList.add(empId , item);
            }
            if (cnt++ % EMP_CHUNK_SIZE == 0) {
                SummaryDetailTransaction sdtran = new SummaryDetailTransaction();
                createSummaryDetails(context.conn,
                                     subEmps.toIntArray() ,
                                     minStartDate, maxEndDate, sdtran, subList);
                processTransaction(context, sdtran, res);
                subList.clear(); subEmps.clear();
                maxEndDate = context.startDate; minStartDate = context.endDate;
            }
            if (isInterrupted(context)) {
                return;
            }
        }
        if (subEmps.size() > 0) {
            SummaryDetailTransaction sdtran = new SummaryDetailTransaction();
            createSummaryDetails(context.conn,
                                 subEmps.toIntArray() ,
                                 minStartDate, maxEndDate, sdtran, subList);
            processTransaction(context, sdtran, res);
            subList.clear(); subEmps.clear();
        }
    }

    private class SummaryDetailTransaction {
        int[] purgeEmpIds;
        Date purgeStartDate;
        Date purgeEndDate;
        List insertSummaries;
        List insertDetails;
    }

    /**
     * Creates summary details for given empIds and date range.
     * empDatesToCheck is used for none-uniform dates and a summary/detail
     * is created iff it exists in empDatesToCheck if supplied. <p>
     * This is required because there is no availability API to do none-uniform dates,
     * it is always a range.
     *
     * @param empIds
     * @param startDate
     * @param endDate
     * @param transaction
     * @param empDatesToCheck
     * @throws Exception
     */
    private void createSummaryDetails(DBConnection conn,
                                      int[] empIds, Date startDate, Date endDate,
                                      SummaryDetailTransaction transaction,
                                      EmployeeIdAndDateList empDatesToCheck) throws Exception{

        if (logger.isDebugEnabled()) logger.debug("Creating summaries from :" + startDate + " to: " + endDate + " for " + empIds.length + " employees");
        if (transaction.insertSummaries == null) {
            transaction.insertSummaries = new ArrayList();
        }
        if (transaction.insertDetails == null) {
            transaction.insertDetails = new ArrayList();
        }
        IntArrayIterator iter = new IntArrayIterator(empIds, EMP_CHUNK_SIZE);
        while (iter.hasNext()) {
            Set empIdsSet = new HashSet(new IntegerList(iter.next()));
            Map empAvalFinal = AvailabilityHelper.getAvailabilityInfo(
                 empIdsSet, startDate , endDate, conn);
            Iterator iterMap = empAvalFinal.entrySet().iterator();
            while (iterMap.hasNext()) {
                Map.Entry entry = (Map.Entry) iterMap.next();
                int empId = ((Integer)entry.getKey()).intValue();
                AvailabilityInfo info = (AvailabilityInfo)entry.getValue();
                Integer[] wbtIds = info.getTeamAvailability() ;
                List[] avals = info.getAvailabilities();
                for (int i = 0; i < avals.length; i++) {
                    List avalTimes = (List) avals[i];
                    if (avalTimes == null || avalTimes.size() == 0) {
                        continue;
                    }
                    Date date = DateHelper.addDays(info.getFirstDate() , i);
                    boolean createsSummary = empDatesToCheck == null
                        || (empDatesToCheck != null && empDatesToCheck.contains(empId , date ));
                    if (!createsSummary) {
                        continue;
                    }
                    WbiagAvalSummaryData summ = new WbiagAvalSummaryData();
                    int wavsId = conn.getDBSequence(WbiagAvailabilityAccess.WBIAG_AVAL_SUMMARY_SEQ).getNextValue();
                    summ.setWavsId(wavsId);
                    summ.setEmpId(empId);
                    summ.setWavsDate(date);
                    summ.setWbtId(wbtIds[i]);
                    int totalDayMins = 0;
                    Iterator iterTimes = avalTimes.iterator();
                    while (iterTimes.hasNext()) {
                        TimePeriod item = (TimePeriod)iterTimes.next();
                        WbiagAvalDetailData det = new WbiagAvalDetailData();
                        det.setGeneratesPrimaryKeyValue(true);
                        det.setWavsId(wavsId );
                        det.setWavdActStTime(item.retrieveStartDate());
                        det.setWavdActEndTime(item.retrieveEndDate());
                        int mins = (int)DateHelper.getMinutesBetween(item.retrieveEndDate() , item.retrieveStartDate());
                        det.setWavdActMinutes(mins);
                        transaction.insertDetails.add(det);
                        totalDayMins += mins;
                    }
                    summ.setWavsActMinutes(totalDayMins);
                    transaction.insertSummaries.add(summ);
                }

            }
        }
    }

    private EmployeeIdAndDateList findMissingEmployeeDates(IntegerList emps,
        WbiagAvailabilityProcessContext context) throws Exception{

        EmployeeIdAndDateList ret = new EmployeeIdAndDateList(false);
        IntArrayIterator iter = new IntArrayIterator(emps.toIntArray(), EMP_CHUNK_SIZE);
         while (iter.hasNext()) {
             IntegerList empsChunk = new IntegerList(iter.next());
             EmployeeIdAndDateList allEmpDates =
                 createEmployeeDates(empsChunk, context);
             EmployeeIdAndDateList existingEmpDates =
                 findExistingSummaries(empsChunk, context);
             // *** remove existing records
             Iterator iter2 = existingEmpDates.iterator();
             while (iter2.hasNext()) {
                 EmployeeIdAndDate item = (EmployeeIdAndDate)iter2.next();
                 allEmpDates.remove(item.getEmpId() , item.getDate());
             }
             ret.addAll(allEmpDates);
             if (logger.isDebugEnabled()) logger.debug("Found :" + allEmpDates.size() + " missing summaries for the chunk" );

             if (isInterrupted(context)) {
                 break;
             }
         }
        return ret;
    }

    /**
     * Finds existing summary records for given context
     * @param emps
     * @param context
     * @return
     * @throws SQLException
     */
    private EmployeeIdAndDateList findExistingSummaries(IntegerList empsIn,
        WbiagAvailabilityProcessContext context) throws Exception{


        EmployeeIdAndDateList ret = new EmployeeIdAndDateList();
        if (empsIn == null || empsIn.size() == 0) {
            return ret;
        }
        int[] emps = empsIn.toIntArray();

        // *** remove existing records
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringBuffer sb = new StringBuffer(200);
        sb.append("SELECT emp_id, wavs_date FROM wbiag_aval_summary  ");
        sb.append(" WHERE wavs_date BETWEEN ? and ? ");
        sb.append(" AND emp_id IN (");
        for (int i = 0; i < emps.length; i++) {
            sb.append(i > 0 ? ",?" : "?");
        }
        sb.append(" )");

        try {
            ps = context.conn.prepareStatement(sb.toString());
            int cnt = 1;
            ps.setTimestamp(cnt++ , new Timestamp(context.startDate.getTime()));
            ps.setTimestamp(cnt++ , new Timestamp(context.endDate.getTime()));
            for (int i = 0; i < emps.length; i++) {
                ps.setInt(cnt++, emps[i]);
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                ret.add(rs.getInt(1) , rs.getDate(2));
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }

        return ret;
    }

    /**
     * Finds changed employees since last run from AMX_OVERRIDE and CHANGE_HISTORY
     * @param emps
     * @param context
     * @return
     * @throws SQLException
     */
    private EmployeeIdAndDateList findChangedEmployeeDates(IntegerList emps,
        WbiagAvailabilityProcessContext context) throws SQLException{

        EmployeeIdAndDateList changedEmpDates = new EmployeeIdAndDateList(false);
        // *** find changed emps for only eligible emps
        IntegerList changedEmps = findChangedEmployees(context , emps);
        if (logger.isDebugEnabled()) logger.debug("Found :" + changedEmps.size() + " changed employees");
        changedEmpDates.addAll(createEmployeeDates(changedEmps , context));

        return changedEmpDates;
    }

    private static final String CHANGE_HIST_SQL = "SELECT chnghist_record_id FROM change_history WHERE chnghist_table_name = ? AND chnghist_change_date >= ?";
    private static final String AMX_OVERRIDE_SQL = "SELECT emp_id FROM amx_override WHERE amxov_crt_dt > ?";

    private IntegerList findChangedEmployees(WbiagAvailabilityProcessContext context,
                                             IntegerList empsInclusive) throws SQLException{
        IntegerList ret = new IntegerList ();
        // *** change history for pattern updates/inserts/deletes

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = context.conn.prepareStatement(CHANGE_HIST_SQL);
            ps.setString(1, AvailabilityData.EMPLOYEE_AVAILABILITY_TABLE.toUpperCase());
            ps.setTimestamp(2, new Timestamp(context.lastRun.getTime()));
            rs = ps.executeQuery();
            while (rs.next()) {
                int empId = rs.getInt(1);
                if (!ret.contains(empId) && empsInclusive.contains(empId) ) {
                    ret.add(empId);
                }
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        int chHistCount = ret.size();
        if (logger.isDebugEnabled()) logger.debug("Found :" + ret.size() + " employees with default pattern updates");

        ps = null; rs = null;
        try {
            ps = context.conn.prepareStatement(AMX_OVERRIDE_SQL);
            ps.setTimestamp(1, new Timestamp(context.lastRun.getTime()));
            rs = ps.executeQuery();
            while (rs.next()) {
                int empId = rs.getInt(1);
                if (!ret.contains(empId) && empsInclusive.contains(empId) ) {
                    ret.add(empId);
                }

            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        if (logger.isDebugEnabled()) logger.debug("Found :" + (ret.size() - chHistCount) + " employees with overrides");

        return ret;
    }

    private EmployeeIdAndDateList createEmployeeDates(IntegerList emps,
        WbiagAvailabilityProcessContext context) {

        EmployeeIdAndDateList ret = new EmployeeIdAndDateList(false);

        int[] empIds = emps.toIntArray();
        if (empIds == null || empIds.length == 0) {
            return ret;
        }
        for (int i = 0; i < empIds.length; i++) {
            for (Date date = context.startDate; date.compareTo(context.endDate) <= 0;
                 date = DateHelper.addDays(date, 1)) {
                ret.add(empIds[i], date);
            }
        }
        return ret;
    }

    private IntegerList findEligibleEmployees(WbiagAvailabilityProcessContext context) throws SQLException{
        IntegerList ret = new IntegerList ();

        StringBuffer sb = new StringBuffer(400);
        sb.append(" SELECT employee.emp_id ");
        sb.append(" FROM employee ");
        if (context.teamIds != null && context.teamIds.length > 0) {
            sb.append(" , employee_team WHERE 1=1 ");
        }
        else {
            sb.append(" WHERE 1=1 ");
        }
        if (context.empIds != null && context.empIds.length > 0) {
            sb.append ( " AND employee.emp_id IN ( ");
            for (int i = 0; i < context.empIds.length; i++) {
                sb.append(i > 0 ? ",?" : "?");
            }
            sb.append (" ) ");
        }
        if (context.teamIds != null && context.teamIds.length > 0) {
            sb.append(" AND wbt_id IN ( ");
            if (!context.subteams) {
                for (int i = 0; i < context.teamIds.length; i++) {
                    sb.append(i > 0 ? ",?" : "?");
                }
                sb.append(" ) ");
            }
            else {
                sb.append("   SELECT child_wbt_id FROM sec_wb_team_child_parent ");
                sb.append("   WHERE parent_wbt_id IN ( ");
                for (int i = 0; i < context.teamIds.length; i++) {
                    sb.append(i > 0 ? ",?" : "?");
                }
                sb.append("   ) ");
                sb.append(" ) ");
            }
            sb.append(" AND empt_home_team = 'Y' ");
            sb.append(" AND employee_team.emp_id = employee.emp_id ");
            sb.append(" AND ? BETWEEN empt_start_date AND empt_end_date ");
            //sb.append(" ) ");
        }
        if (context.calcgrpIds != null && context.calcgrpIds.length > 0) {
            sb.append ( " AND calcgrp_id IN ( ");
            for (int i = 0; i < context.calcgrpIds.length; i++) {
                sb.append(i > 0 ? ",?" : "?");
            }
            sb.append (" ) ");
        }
        if (context.paygrpIds != null && context.paygrpIds.length > 0) {
            sb.append ( " AND paygrp_id IN ( ");
            for (int i = 0; i < context.paygrpIds.length; i++) {
                sb.append(i > 0 ? ",?" : "?");
            }
            sb.append (" ) ");
        }
        // *** we only care about employees who have default availability
        sb.append (" AND EXISTS (SELECT emp_id FROM amx_availability ");
        sb.append (" WHERE amx_availability.emp_id = employee.emp_id) ");


        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (logger.isDebugEnabled()) logger.debug("Eligible Employees Sql : " + sb.toString());
            ps = context.conn.prepareStatement(sb.toString());
            int cnt = 1;
            if (context.empIds != null && context.empIds.length > 0) {
                for (int i = 0; i < context.empIds.length; i++) {
                    ps.setInt(cnt++ , context.empIds[i]);
                }
            }
            if (context.teamIds != null && context.teamIds.length > 0) {
                for (int i = 0; i < context.teamIds.length; i++) {
                    ps.setInt(cnt++ , context.teamIds[i]);
                }
                ps.setTimestamp(cnt++ , new Timestamp(DateHelper.getCurrentDate().getTime()));
            }
            if (context.calcgrpIds != null && context.calcgrpIds.length > 0) {
                for (int i = 0; i < context.calcgrpIds.length; i++) {
                    ps.setInt(cnt++ , context.calcgrpIds[i]);
                }
            }
            if (context.paygrpIds != null && context.paygrpIds.length > 0) {
                for (int i = 0; i < context.paygrpIds.length; i++) {
                    ps.setInt(cnt++ , context.paygrpIds[i]);
                }
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                ret.add(rs.getInt(1));
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }

        return ret;
    }

    private int[] getEmpIdsFromEmpDateList(EmployeeIdAndDateList empDates) {
        IntegerList list = new IntegerList();
        if (empDates == null || empDates.size() == 0) {
            return list.toIntArray();
        }
        // *** contains is too expensive, keep a temp HashSet
        Set internalList = new HashSet();
        Iterator iter = empDates.iterator();
        while (iter.hasNext()) {
            EmployeeIdAndDate item = (EmployeeIdAndDate)iter.next();
            Integer empId = new Integer(item.getEmpId());
            if (!internalList.contains(empId)) {
                list.add(item.getEmpId());
                internalList.add(empId);
            }
        }
        return list.toIntArray();
    }

    private boolean isInterrupted(WbiagAvailabilityProcessContext context) throws Exception{
        if( context.intCheck != null ) {
            if( context.intCheck.isInterrupted() ) {
                logger.info("Interrupting WbiagAvailability Process");
                return true;
            }
        }
        return false;
    }

    public static class WbiagAvailabilityProcessContext {
        public DBConnection conn;
        public int[] empIds;
        public int[] teamIds;
        public int[] calcgrpIds;
        public int[] paygrpIds;
        public boolean subteams;
        public Date startDate;
        public Date endDate;
        public boolean recreatesSummaryDetail;
        public Date lastRun;
        public boolean shouldCommit;
        public ScheduledJob intCheck;
        public boolean runsForChangedEmps;
        public boolean runsForMissingSummaries;

        public boolean isAllEmployees() {
            return (empIds == null || empIds.length == 0)
                && (teamIds == null || teamIds.length == 0)
                && (calcgrpIds == null || calcgrpIds.length == 0)
                && (paygrpIds == null || paygrpIds.length == 0);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer(200);
            sb.append("empIds : ").append(StringHelper.createCSVForNumber(new IntegerList(empIds))).append("\n");
            sb.append("teamIds : ").append(StringHelper.createCSVForNumber(new IntegerList(teamIds))).append("\n");
            sb.append("calcgrpIds : ").append(StringHelper.createCSVForNumber(new IntegerList(calcgrpIds))).append("\n");
            sb.append("paygrpIds : ").append(StringHelper.createCSVForNumber(new IntegerList(paygrpIds))).append("\n");
            sb.append("subteams : ").append(subteams).append("\n");
            sb.append("startDate : ").append(startDate).append("\n");
            sb.append("endDate : ").append(endDate).append("\n");
            sb.append("recreatesSummaryDetail : ").append(recreatesSummaryDetail).append("\n");
            sb.append("lastRun : ").append(lastRun).append("\n");
            sb.append("shouldCommit : ").append(shouldCommit).append("\n");
            sb.append("runsForChangedEmps : ").append(runsForChangedEmps).append("\n");
            sb.append("runsForMissingSummaries : ").append(runsForMissingSummaries).append("\n");
            return sb.toString();
        }
    }

    public static class WbiagAvailabilityProcessResult {
        public int summariesInsertedCount;
        public int detailsInsertedCount;
        public int eligibleEmpCount;
    }

}
