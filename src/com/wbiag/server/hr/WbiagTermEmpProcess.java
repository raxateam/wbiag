package com.wbiag.server.hr;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.scheduler.enterprise.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.tool.security.*;
import com.workbrain.util.*;
/**
 * Process class to process terminated employee records.
 */
public class WbiagTermEmpProcess {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(WbiagTermEmpProcess.class);

    private static final String OVR_WBU_NAME = "EMP_TERMINATION_TASK";

    public WbiagTermEmpProcess() {
    }

    /**
     * Process terminated employee data based on given context
     * @param context
     * @throws Exception
     */
    public WbiagTermEmpProcessResult process(WbiagTermEmpProcessContext context) throws Exception{
        if (context.conn == null) {
            throw new RuntimeException ("Connection must be supplied");
        }
        if (context.lastRun == null) {
            throw new RuntimeException ("Last run must be supplied");
        }
        if (logger.isDebugEnabled()) logger.debug("Started Terminated Employee Process with context :\n(" + context + ")");

        EmployeeIdAndDateList empDates = findTerminatedEmployeeDates(context) ;
        WbiagTermEmpProcessResult res = new WbiagTermEmpProcessResult();
        res.empTermDatesCount = empDates.size();

        if (empDates == null || empDates.size() == 0) {
            if (logger.isDebugEnabled()) logger.debug("No terminated employees found");
            return res;
        }
        if (logger.isDebugEnabled()) logger.debug("Found :" + empDates.size() + " terminated employee dates");

        Iterator iter = empDates.iterator();
        while (iter.hasNext()) {
            EmployeeIdAndDate item = (EmployeeIdAndDate)iter.next();
            processEmpJobs(context, item.getEmpId(), item.getDate() );
            processEntPolicies(context, item.getEmpId(), item.getDate() );
            processOverrides(context, item.getEmpId(), item.getDate() );
            processEmpBalReset(context, item.getEmpId(), item.getDate() );
            processWorkbrainUser(context, item.getEmpId(), item.getDate() );
            processTeamHierarchy(context, item.getEmpId(), item.getDate() );
            processEmpSkds(context, item.getEmpId(), item.getDate());
            processPayrollData(context, item.getEmpId(), item.getDate() );
            processEmpStatus(context, item.getEmpId(), item.getDate() );
            processReaderGroups(context, item.getEmpId());

            if (context.shouldCommit) {
                context.conn.commit();
            }
            if (isInterrupted(context)) {
                break;
            }
        }
        return res;
    }

    protected static final String UPDATE_EMPJOB_SQL = "UPDATE employee_job " +
            " SET empjob_end_date = ? WHERE emp_id = ? AND empjob_end_date >= ?";
    protected static final String DELETE_EMPJOB_SQL = "DELETE FROM employee_job " +
            " WHERE emp_id = ? AND empjob_start_date >= ?";

    /**
     * Updates or purges empjobs overlapping term date
     * @param context
     * @param empId
     * @param termDate
     * @throws SQLException
     */
    protected boolean processEmpJobs(WbiagTermEmpProcessContext context,
                                  int empId, Date termDate) throws SQLException{
        if (!context.processEmployeeJobs) {
            return false;
        }
        int upd = deleteAfterDate(context.conn, DELETE_EMPJOB_SQL, empId, termDate);
        if (logger.isDebugEnabled()) logger.debug("Deleted :" + upd + " employee jobs for empId : " + empId);

        upd = updateEndDate(context.conn, UPDATE_EMPJOB_SQL, empId, termDate);
        if (logger.isDebugEnabled()) logger.debug("Updated :" + upd + " employee jobs for empId : " + empId);
        return true;
    }

    protected static final String UPDATE_EMPENTPOL_SQL = "UPDATE ent_emp_policy " +
            " SET entemppol_end_date = ? WHERE emp_id = ? AND entemppol_end_date >= ?";
    protected static final String DELETE_EMPENTPOL_SQL = "DELETE FROM ent_emp_policy " +
            " WHERE emp_id = ? AND entemppol_start_date >= ?";

    /**
     * Updates or purges emp entitlement policies overlapping term date
     * @param context
     * @param empId
     * @param termDate
     * @throws SQLException
     */
    protected boolean processEntPolicies(WbiagTermEmpProcessContext context,
                                      int empId, Date termDate) throws SQLException {
        if (!context.processEntEmpPols) {
            return false;
        }

        int upd = deleteAfterDate(context.conn, DELETE_EMPENTPOL_SQL, empId, termDate);
        if (logger.isDebugEnabled())  logger.debug("Deleted :" + upd + " employee entitlement policies for empId : " + empId);

        upd = updateEndDate(context.conn, UPDATE_EMPENTPOL_SQL, empId, termDate);
        if (logger.isDebugEnabled())  logger.debug("Updated :" + upd + " employee entitlement policies for empId : " + empId);

        return true;
    }

    /**
     * Cancels overrides after and/on term date excluding termination one
     * @param context
     * @param empId
     * @param termDate
     * @throws SQLException
     */
    protected boolean processOverrides(WbiagTermEmpProcessContext context,
                                  int empId, Date termDate) throws Exception{
        if (context.processOverrideTypes == null
            || context.processOverrideTypes.length == 0) {
            return false;
        }
        // *** get override ids type by type
        IntegerList ovrIds = getOverridesAfterDate(context.conn , empId ,
                                       termDate,
                                       context.processOverrideTypes );

        if (logger.isDebugEnabled()) logger.debug("Found : " + ovrIds.size() + " overrides after termination date");
        if (ovrIds.size() == 0) {
            return false;
        }
        OverrideBuilder ob = new OverrideBuilder(context.conn);

        int[] ovrIdsA = ovrIds.toIntArray();
        for (int i = 0; i < ovrIdsA.length; i++) {
            DeleteOverride dov = new DeleteOverride();
            dov.setWbuNameBoth(OVR_WBU_NAME , OVR_WBU_NAME);
            dov.setOverrideId(ovrIdsA[i]);
            ob.add(dov);
        }
        ob.execute(true , false);

        return true;
    }

    protected IntegerList getOverridesAfterDate(DBConnection conn,
                                       int empId, Date termDate,
                                       int[] ovrTypes) throws SQLException{

        IntegerList ret = new IntegerList();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT ovr_id, ovrtyp_id ");
            sb.append(" FROM override ");
            sb.append(" WHERE emp_id = ? ");
            sb.append(" AND ovr_start_date >= ? ");
            sb.append(" AND ovr_status = ? ");
            sb.append(" AND ovr_new_value NOT LIKE '%").append(EmployeeData.EMP_TERMINATION_DATE).append("%' ");

            ps = conn.prepareStatement(sb.toString());
            ps.setInt(1  , empId);
            ps.setTimestamp(2  , new Timestamp(termDate.getTime()));
            ps.setString(3, OverrideData.APPLIED);
            rs = ps.executeQuery();
            while (rs.next()) {
                int ovrId = rs.getInt(1); int ovrTypId = rs.getInt(2);
                // *** do ovrType check here, OR in sql is expensive
                for (int i = 0; i < ovrTypes.length; i++) {
                    if (ovrTypId >= ovrTypes[i]
                        && ovrTypId <= (ovrTypes[i] + 99)) {
                        ret.add(ovrId);
                        break;
                    }
                }
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return ret;
    }

    /**
     * Set selected employee balances to 0
     * @param context
     * @param empId
     * @param termDate
     * @throws SQLException
     */
    protected boolean processEmpBalReset(WbiagTermEmpProcessContext context,
                                  int empId, Date termDate) throws Exception{
        if (context.empBalsReset == null
            || context.empBalsReset.length == 0) {
            return false;
        }

        IntegerList eligibleBals = new IntegerList(context.empBalsReset);

        EmployeeBalanceAccess eba = new EmployeeBalanceAccess(context.conn);
        List empBalVals = eba.loadByEmployeeAsOfDate(empId , termDate);

        OverrideBuilder ob = new OverrideBuilder(context.conn);

        Iterator iter = empBalVals.iterator();
        while (iter.hasNext()) {
            EmployeeBalanceData item = (EmployeeBalanceData)iter.next();
            if (eligibleBals.contains(item.getBalId())
                && item.getEmpbalValue() != 0) {

                InsertEmployeeBalanceOverride ov = new InsertEmployeeBalanceOverride(context.conn);
                ov.setStartDate(DateHelper.addDays(termDate , -1));
                ov.setEndDate(DateHelper.addDays(termDate , -1));
                ov.setWbuNameBoth(OVR_WBU_NAME , OVR_WBU_NAME);
                ov.setEmpbalActionSET();
                ov.setEmpId(empId );
                ov.setBalId(item.getBalId());
                ov.setEmpbalValue(0);
                ob.add(ov);
            }
        }

        ob.execute(true , false);
        if (logger.isDebugEnabled()) logger.debug("Created " + ob.getUpdateCount() + " balance reset override(s) for empId : " + empId);
        return true;
    }

    protected static final String UPDATE_WBUSER_SQL = "UPDATE workbrain_user " +
            " SET wbu_active = ? WHERE emp_id = ?";

    /**
     * Sets workbrain user inactive
     * @param context
     * @param empId
     * @param termDate
     * @throws SQLException
     */
    protected boolean processWorkbrainUser(WbiagTermEmpProcessContext context,
                                  int empId, Date termDate) throws SQLException{
        if (!context.processWorkbrainUser) {
            return false;
        }

        PreparedStatement ps = null;
        try {
            ps = context.conn.prepareStatement(UPDATE_WBUSER_SQL);
            ps.setString(1  , "N");
            ps.setInt(2  , empId);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        return true;
    }

    protected static final String UPDATE_WBUSERTEAM_SQL = "UPDATE workbrain_user_team " +
            " SET wbut_end_date = ? WHERE emp_id = ? AND wbut_end_date >= ?";
    protected static final String DELETE_WBUSERTEAM_SQL = "DELETE FROM workbrain_user_team " +
            " WHERE emp_id = ? AND wbut_start_date >= ?";

    /**
     * Updates employee team and workbrain user team records.
     * @param context
     * @param empId
     * @param termDate
     * @throws SQLException
     */
    protected boolean processTeamHierarchy(WbiagTermEmpProcessContext context,
                                        int empId, Date termDate) throws Exception{
        if (!context.processTeamHierarchy) {
            return false;
        }
        // *** emp team to be terminated
        SecurityEmployee.setHomeTeamToTerminated(context.conn, empId,
                                                 new java.sql.Date(termDate.getTime()) ,
                                                 SQLHelper.getMaxDate());
        // *** user team assignments
        int upd = deleteAfterDate(context.conn, DELETE_WBUSERTEAM_SQL, empId, termDate);
        if (logger.isDebugEnabled())  logger.debug("Deleted :" + upd + " user team assignments for empId : " + empId);

        upd = updateEndDate(context.conn, UPDATE_WBUSERTEAM_SQL, empId, termDate);
        if (logger.isDebugEnabled())  logger.debug("Updated :" + upd + " user team assignments for empId : " + empId);

        return true;
    }

    protected static final String DELETE_EMPSKD_SQL = "DELETE FROM employee_schedule " +
            " WHERE emp_id = ? AND work_date >= ?";
    protected static final String DELETE_EMPSKDDTL_SQL = "DELETE FROM employee_sched_dtl " +
            " WHERE emp_id = ? AND eschd_work_date >= ?";

    /**
     * Purges employee schedule records
     * @param context
     * @param empId
     * @param termDate
     * @throws SQLException
     */
    protected boolean processEmpSkds(WbiagTermEmpProcessContext context,
                                  int empId, Date termDate) throws SQLException{
        if (!context.processEmpSkds) {
            return false;
        }
        int del = deleteAfterDate(context.conn , DELETE_EMPSKD_SQL, empId, termDate);
        if (logger.isDebugEnabled())  logger.debug("Deleted :" + del + " employee schedules for empId : " + empId);

        del = deleteAfterDate(context.conn , DELETE_EMPSKDDTL_SQL, empId, termDate);
        if (logger.isDebugEnabled())  logger.debug("Deleted :" + del + " employee schedule details for empId : " + empId);

        return true;
    }

    protected static final String DELETE_CLOCKTRAN_SQL = "DELETE FROM clock_tran_processed " +
            " WHERE wrks_id IN " +
            " (SELECT wrks_id FROM work_summary WHERE emp_id = ? AND wrks_work_date >= ?)";
    protected static final String DELETE_EMPBALLOG_SQL = "DELETE FROM employee_balance_log " +
            " WHERE wrks_id IN " +
            " (SELECT wrks_id FROM work_summary WHERE emp_id = ? AND wrks_work_date >= ?)";
    protected static final String DELETE_WRKD_SQL = "DELETE FROM work_detail " +
            " WHERE wrks_id IN " +
            " (SELECT wrks_id FROM work_summary WHERE emp_id = ? AND wrks_work_date >= ?)";
    protected static final String DELETE_WRKS_SQL = "DELETE FROM work_summary " +
            " WHERE emp_id = ? AND wrks_work_date >= ?";

    /**
     * Purges payroll related data
     * @param context
     * @param empId
     * @param termDate
     * @throws SQLException
     */
    protected boolean processPayrollData(WbiagTermEmpProcessContext context,
                                  int empId, Date termDate) throws SQLException{
        if (!context.processPayrollData) {
            return false;
        }
        // *** first calc all existing summaries and then purge
        WorkSummaryAccess wsa = new WorkSummaryAccess(context.conn);
        List wrksDates = wsa.loadByEmpIdAndDateRange(empId,
            termDate, DateHelper.DATE_3000);
        IntegerList wrksIds = new IntegerList (wrksDates.size());
        Iterator iter = wrksDates.iterator();
        while (iter.hasNext()) {
            WorkSummaryData item = (WorkSummaryData) iter.next();
            wrksIds.add(item.getWrksId() );
        }
        RuleEngine.runCalcGroup(context.conn, wrksIds.toIntArray() , false, 100, false);
        if (logger.isDebugEnabled()) logger.debug("Recalculated " + wrksIds.size() + " work summaries for payroll data");

        int upd = deleteAfterDate(context.conn, DELETE_CLOCKTRAN_SQL, empId, termDate);
        if (logger.isDebugEnabled()) logger.debug("Deleted :" + upd + " processed clocks for empId : " + empId);

        upd = deleteAfterDate(context.conn, DELETE_EMPBALLOG_SQL, empId, termDate);
        if (logger.isDebugEnabled()) logger.debug("Deleted :" + upd + " employee balance logs for empId : " + empId);

        upd = deleteAfterDate(context.conn, DELETE_WRKD_SQL, empId, termDate);
        if (logger.isDebugEnabled()) logger.debug("Deleted :" + upd + " work details for empId : " + empId);

        upd = deleteAfterDate(context.conn, DELETE_WRKS_SQL, empId, termDate);
        if (logger.isDebugEnabled()) logger.debug("Deleted :" + upd + " work summaries for empId : " + empId);

        return true;
    }

    /**
     * Set selected employee balances to 0
     * @param context
     * @param empId
     * @param termDate
     * @throws SQLException
     */
    protected boolean processEmpStatus(WbiagTermEmpProcessContext context,
                                  int empId, Date termDate) throws Exception{
        if (!context.processEmpStatus) {
            return false;
        }

        OverrideBuilder ob = new OverrideBuilder(context.conn);

        InsertEmployeeOverride ov = new InsertEmployeeOverride(context.conn);
        ov.setStartDate(termDate);
        ov.setEndDate(DateHelper.DATE_3000);
        ov.setWbuNameBoth(OVR_WBU_NAME, OVR_WBU_NAME);
        ov.setEmpStatus("I");
        ov.setEmpId(empId);
        ob.add(ov);

        ob.execute(true , false);
        if (logger.isDebugEnabled()) logger.debug("Set employee status to INACTIVE empId : " + empId);
        return true;
    }
    
    /**
     * Remove employee from the reader groups.
     * @param context
     * @param empId
     * @param termDate
     * @return
     * @throws Exception
     */
    protected boolean processReaderGroups(WbiagTermEmpProcessContext context, int empId) throws Exception {
        if (!context.processReaderGroups) {
            return false;
        }
        
        int delRdrGrp = deleteRecords(context.conn, DELETE_EMP_RDRGRP_SQL, empId);
        if (logger.isDebugEnabled()) logger.debug("Deleted :" + delRdrGrp + " employee reader groups for empId : " + empId);
    	return true;
    }
    
    protected static final String DELETE_EMP_RDRGRP_SQL = "DELETE FROM EMPLOYEE_READER_GROUP WHERE emp_id = ?";
    
    /**
     * Finds terminated emps since last run.
     * IF there are more than one override, the most recent created one is valid
     * @param context
     * @return
     * @throws SQLException
     */
    protected EmployeeIdAndDateList findTerminatedEmployeeDates(WbiagTermEmpProcessContext context)
        throws SQLException{
        EmployeeIdAndDateList ret = new EmployeeIdAndDateList();

        Map empDates = new HashMap();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT emp_id, ovr_new_value FROM override ");
            sb.append(" WHERE ovr_create_date > ? ");
            sb.append(" AND ovrtyp_id BETWEEN ? and ? ");
            sb.append(" AND ovr_status = ? ");
            sb.append(" AND ovr_new_value LIKE '%").append(EmployeeData.EMP_TERMINATION_DATE).append("%' ");
            sb.append(" ORDER BY ovr_create_date, ovr_id");
            ps = context.conn.prepareStatement(sb.toString());
            ps.setTimestamp(1  , new Timestamp(context.lastRun.getTime()));
            ps.setInt(2  , OverrideData.EMPLOYEE_TYPE_START);
            ps.setInt(3  , OverrideData.EMPLOYEE_TYPE_END);
            ps.setString(4, OverrideData.APPLIED);
            rs = ps.executeQuery();
            while (rs.next()) {
                String ovrVal = rs.getString(2);
                Integer empId  = new Integer(rs.getInt(1));
                Date termDate = null;
                if (!StringHelper.isEmpty(ovrVal)) {
                    OverrideData od = new OverrideData();
                    od.setOvrNewValue(ovrVal);
                    OverrideData.OverrideToken token = od.getNewOverrideByName(
                        EmployeeData.EMP_TERMINATION_DATE);
                    termDate = token.getValueDate();
                    // skip rehires
                    if (termDate.before(DateHelper.DATE_3000)) {
                        empDates.put(empId, termDate);
                    }
                    else {
                        if (logger.isDebugEnabled())
                            logger.debug("Skipped a rehire: empId=" + empId + ", termDate=" + termDate);
                        empDates.remove(empId);
                    }

                }
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        Iterator iter = empDates.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            int empId = ((Integer)entry.getKey()).intValue();
            Date date = (Date)entry.getValue();
            ret.add(empId , date);
        }
        return ret;
    }

    /**
     * Updates end date with term date - 1
     * @param conn
     * @param sql
     * @param empId
     * @param termDate
     * @return
     * @throws SQLException
     */
    private int updateEndDate(DBConnection conn, String sql,
                               int empId, Date termDate) throws SQLException {

        int upd = 0;
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);

            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, new Timestamp(DateHelper.addDays(termDate , -1).getTime()));
            ps.setInt(2, empId);
            ps.setTimestamp(3, new Timestamp(termDate.getTime()));
            upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        return upd;
    }

    /**
     * Deletes records that start after and on term date
     * @param conn
     * @param sql
     * @param empId
     * @param termDate
     * @return
     * @throws SQLException
     */
    private int deleteAfterDate(DBConnection conn, String sql,
                                int empId, Date termDate) throws SQLException {
        int upd = 0;
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);

            ps = conn.prepareStatement(sql);
            ps.setInt(1, empId);
            ps.setTimestamp(2, new Timestamp(termDate.getTime()));
            upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        return upd;
    }

    /**
     * Deletes records for the passed employee
     * @param conn
     * @param sql
     * @param empId
     * @param termDate
     * @return
     * @throws SQLException
     */
    private int deleteRecords(DBConnection conn, String sql, int empId) throws SQLException {
        int upd = 0;
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);

            ps = conn.prepareStatement(sql);
            ps.setInt(1, empId);
            upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        return upd;
    }
    
    private boolean isInterrupted(WbiagTermEmpProcessContext context) throws Exception{
        if( context.intCheck != null ) {
            if( context.intCheck.isInterrupted() ) {
                logger.info("Interrupting WbiagTermEmpProcess Process");
                return true;
            }
        }
        return false;
    }
    
    public static class WbiagTermEmpProcessContext {
        public DBConnection conn;
        public boolean processEmployeeJobs;
        public boolean processEntEmpPols;
        public int[] processOverrideTypes;
        public int[] empBalsReset;
        public boolean processWorkbrainUser;
        public boolean processTeamHierarchy;
        public boolean processEmpSkds;
        public boolean processPayrollData;
        public boolean processEmpStatus;
        public boolean processReaderGroups;
        public Date lastRun;
        public boolean shouldCommit;
        public ScheduledJob intCheck;

        public String toString() {
            StringBuffer sb = new StringBuffer(200);
            sb.append("processEmployeeJobs : ").append(processEmployeeJobs).append("\n");
            sb.append("processEntEmpPols : ").append(processEntEmpPols).append("\n");
            sb.append("processOverrideTypes : ").append(StringHelper.createCSVForNumber(new IntegerList(processOverrideTypes))).append("\n");
            sb.append("empBalsReset : ").append(StringHelper.createCSVForNumber(new IntegerList(empBalsReset))).append("\n");
            sb.append("processWorkbrainUser : ").append(processWorkbrainUser).append("\n");
            sb.append("processTeamHierarchy : ").append(processTeamHierarchy).append("\n");
            sb.append("processEmpSkds : ").append(processEmpSkds).append("\n");
            sb.append("processPayrollData : ").append(processPayrollData).append("\n");
            sb.append("processEmpStatus : ").append(processEmpStatus).append("\n");
            sb.append("processReaderGroups : ").append(processReaderGroups).append("\n");
            sb.append("lastRun : ").append(lastRun).append("\n");
            sb.append("shouldCommit : ").append(shouldCommit).append("\n");
            return sb.toString();
        }
    }

    public static class WbiagTermEmpProcessResult {
        public int empTermDatesCount;
    }

}
