package com.wbiag.server.hr;


import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.scheduler.enterprise.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.security.*;
import com.workbrain.server.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * WbiagTermEmpTask to process terminated employees
 */
public class WbiagTermEmpTask extends AbstractScheduledJob {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WbiagTermEmpTask.class);
    public static final String PARAM_PROCESS_EMPJOBS = "PROCESS_EMPJOBS";
    public static final String PARAM_PROCESS_ENTEMPPOLS = "PROCESS_ENTEMPPOLS";
    public static final String PARAM_PROCESS_OVRS = "PROCESS_OVRS";
    public static final String PARAM_EMP_BALS_RESET = "EMP_BALS_RESET";
    public static final String PARAM_PROCESS_WBUSER = "PROCESS_WBUSER";
    public static final String PARAM_PROCESS_TEAM_HIERARCHY = "PROCESS_TEAM_HIERARCHY";
    public static final String PARAM_CLIENT_ID = "CLIENT_ID";
    public static final String PARAM_PROCESS_EMPSKDS = "PROCESS_EMPSKDS_PARAM";
    public static final String PARAM_PROCESS_PAYROLL_DATA = "PROCESS_PAYROLL_DATA_PARAM";
    public static final String PARAM_PROCESS_EMPSTATUS = "PROCESS_EMPSTATUS";
    public static final String PARAM_PROCESS_READERGROUPS = "PROCESS_READERGROUPS";
    
    public static final String REG_ROLLOUT_DATE = "ROLLOUT_DATE";
    public static final String WBINTTYPE_WBIAG_TERM_EMP_TASK = "WBIAG_TERM_EMP_TASK";
    public static final String UDF_DATE_FORMAT = "yyyyMMdd HHmmss";
    public static final String ABSOLUTE_DATE_FORMAT = "yyyyMMdd HHmmss";
    public static final String ROLLOUT_DATE_FORMAT = "MM/dd/yyyy";

    public WbiagTermEmpTask() {
    }

    public Status run(int taskID, Map param) throws Exception {
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) logger.debug("WbiagTermEmpTask.run(" + taskID + ", " + param + ")");

        String currentClientId = null;

        StringBuffer sb = new StringBuffer(200);
        sb.append("Scheduled OK <br>");
        try {
            // *** store current client id
            currentClientId = SecurityService.getCurrentClientId();

            getConnection().turnTraceOff();
            getConnection().setAutoCommit(false);

            if (StringHelper.isEmpty((String)param.get( PARAM_CLIENT_ID ))) {
                throw new RuntimeException ("Client_id must be specified");
            }
            String clientId = (String) param.get( PARAM_CLIENT_ID );

            SecurityService.setCurrentClientId(clientId);
            String msg = execute(getConnection() , param);
            sb.append(msg);

            if (!isInterrupted()) {
                getConnection().commit();
                return jobOk( sb.toString() );
            } else {
                getConnection().rollback();
                return jobInterrupted("WbiagTermEmpTask task has been interrupted.");
            }

        } catch (Exception e) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in running WbiagAvailability Task", e);}
            if( getConnection() != null ) getConnection().rollback();
            throw e;
        } finally {
            SecurityService.setCurrentClientId(currentClientId);
            releaseConnection();
        }
    }


    public String execute(DBConnection conn, Map param) throws Exception {

        WbiagTermEmpProcess.WbiagTermEmpProcessContext context =
            new WbiagTermEmpProcess.WbiagTermEmpProcessContext();

        Date taskDatetime = new Date();
        context.conn = conn;
        context.shouldCommit = true;
        context.intCheck = this;

        String sPrJobs = (String)param.get(PARAM_PROCESS_EMPJOBS);
        context.processEmployeeJobs = StringHelper.isEmpty(sPrJobs)
            ? false : "Y".equals(sPrJobs) ? true : false;

        String sEntPols = (String)param.get(PARAM_PROCESS_ENTEMPPOLS);
        context.processEntEmpPols = StringHelper.isEmpty(sEntPols)
            ? false : "Y".equals(sEntPols) ? true : false;

        String sProcessOvrs = (String)param.get(PARAM_PROCESS_OVRS);
        context.processOverrideTypes = StringHelper.detokenizeStringAsIntArray(
            sProcessOvrs , ",", true);

        String sEmpBalsReset = (String)param.get(PARAM_EMP_BALS_RESET);
        context.empBalsReset = StringHelper.detokenizeStringAsIntArray(
            sEmpBalsReset , ",", true);

        String sWbu = (String)param.get(PARAM_PROCESS_WBUSER);
        context.processWorkbrainUser = StringHelper.isEmpty(sWbu)
            ? false : "Y".equals(sWbu) ? true : false;

        String sTeam = (String)param.get(PARAM_PROCESS_TEAM_HIERARCHY);
        context.processTeamHierarchy = StringHelper.isEmpty(sTeam)
            ? false : "Y".equals(sTeam) ? true : false;

        String sEmpSkd = (String)param.get(PARAM_PROCESS_EMPSKDS);
        context.processEmpSkds = StringHelper.isEmpty(sEmpSkd)
            ? false : "Y".equals(sEmpSkd) ? true : false;

        String sPayroll = (String)param.get(PARAM_PROCESS_PAYROLL_DATA);
        context.processPayrollData = StringHelper.isEmpty(sPayroll)
            ? false : "Y".equals(sPayroll) ? true : false;

        String sEmpStat = (String)param.get(PARAM_PROCESS_EMPSTATUS);
        context.processEmpStatus = StringHelper.isEmpty(sEmpStat)
            ? false : "Y".equals(sEmpStat) ? true : false;

        String sReaderGroups = (String)param.get(PARAM_PROCESS_READERGROUPS);
        context.processReaderGroups = StringHelper.isEmpty(sReaderGroups)
            ? false : "Y".equals(sReaderGroups) ? true : false;
        
        context.lastRun = getLastRunDate(conn);

        WbiagTermEmpProcess pr = new WbiagTermEmpProcess();
        WbiagTermEmpProcess.WbiagTermEmpProcessResult res = pr.process(context);

        updateLastRecalcDate(conn , taskDatetime);
        StringBuffer sb = new StringBuffer(200);
        sb.append("Found : ").append(res.empTermDatesCount).append(" terminated employee date(s) <br>");

        return sb.toString();
    }

    public String getTaskUI() {
        return "/jobs/wbiag/WbiagTermEmpParams.jsp";
    }

    /**
     * Returns last run date from wbint_type of registry=ROLLOUT_DATE.
     * If wbint_type is not there, it will create it
     * @param conn
     * @return
     * @throws Exception
     */
    protected Date getLastRunDate(DBConnection conn) throws Exception {
        Date ret = null;

        String rolloutDate = WorkbrainParametersRetriever.getString(REG_ROLLOUT_DATE , null);
        if (StringHelper.isEmpty(rolloutDate)) {
            throw new RuntimeException("Rollout date needs to be defined in MM/dd/yyyy format in Registry");
        }

        boolean typeExists = false;
        String wbitypUdf1 = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT wbityp_udf1 FROM wbint_type WHERE wbityp_name = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1 , WBINTTYPE_WBIAG_TERM_EMP_TASK);
            rs = ps.executeQuery();
            if (rs.next()) {
                typeExists = true;
                wbitypUdf1 = rs.getString(1);
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }

        if (!typeExists ) {
            PreparedStatement ps1 = null;
            try {
                String sql = "INSERT INTO wbint_type (wbityp_id, wbityp_name, wbityp_type, wbityp_del_older_than) VALUES (?,?,?,?)";
                ps1 = conn.prepareStatement(sql);
                ps1.setInt(1 , conn.getDBSequence(TypeAccess.WBINTTYPE_PRI_KEY_SEQ).getNextValue());
                ps1.setString(2 , WBINTTYPE_WBIAG_TERM_EMP_TASK);
                ps1.setString(3 , "IMPORT");
                ps1.setInt(4, Integer.MAX_VALUE);
                int upd = ps1.executeUpdate();
            }
            finally {
                if (ps1 != null) ps1.close();
            }
            ret = DateHelper.parseDate(rolloutDate, ROLLOUT_DATE_FORMAT);
        }
        else {
            if (!StringHelper.isEmpty(wbitypUdf1)) {
                ret = DateHelper.parseDate(wbitypUdf1,  UDF_DATE_FORMAT);
            }
            else {
                ret = DateHelper.parseDate(rolloutDate, ROLLOUT_DATE_FORMAT);
            }
        }
        return ret;
    }

    protected void updateLastRecalcDate(DBConnection conn, Date taskDateTime) throws Exception {
        PreparedStatement ps = null;
        try {
            String sql = "UPDATE wbint_type SET wbityp_udf1=? WHERE wbityp_name = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1  , DateHelper.convertDateString(taskDateTime, UDF_DATE_FORMAT));
            ps.setString(2 , WBINTTYPE_WBIAG_TERM_EMP_TASK);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
    }
}
