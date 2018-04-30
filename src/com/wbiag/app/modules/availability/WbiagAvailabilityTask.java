package com.wbiag.app.modules.availability;


import java.util.*;
import java.text.*;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import com.workbrain.app.scheduler.enterprise.AbstractScheduledJob;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.util.*;
import com.workbrain.security.SecurityService;
import com.workbrain.sql.*;
import com.workbrain.server.WorkbrainParametersRetriever;
import com.workbrain.server.registry.*;
import com.workbrain.app.ta.ruleengine.RuleEngine;
/**
 * WbiagAvailabilityTask to synch WBIAG_AVAL tables
 * @deprecated As of 5.0, use core Make Availability Reportable Task.
 */
public class WbiagAvailabilityTask extends com.wbiag.util.AbstractLastRunTask {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WbiagAvailabilityTask.class);
    public static final String PARAM_CALCGRPS = "CALCGRPS";
    public static final String PARAM_PAYGRPS = "PAYGRPS";
    public static final String PARAM_WBTEAMS = "WBTEAMS";
    public static final String PARAM_SUBTEAMS = "SUBTEAMS";
    public static final String PARAM_EMPLOYEES = "EMPLOYEES";
    public static final String PARAM_CLIENT_ID = "CLIENT_ID";
    public static final String PARAM_DAYS_BEFORE = "DAYS_BEFORE_PARAM";
    public static final String PARAM_DAYS_AFTER = "DAYS_AFTER_PARAM";
    public static final String PARAM_ABS_START_DATE = "ABS_START_DATE_PARAM";
    public static final String PARAM_ABS_END_DATE = "ABS_END_DATE_PARAM";
    public static final String PARAM_RANGE_TYPE = "RANGE_TYPE";
    public static final String PARAM_VAL_RANGE_TYPE_RELATIVE = "rel";
    public static final String PARAM_VAL_RANGE_TYPE_ABSOLUTE = "abs";
    public static final String PARAM_RECREATES_RECORDS = "RECREATES_RECORDS";

    public static final String REG_ROLLOUT_DATE = "ROLLOUT_DATE";
    public static final String WBINTTYPE_WBIAG_AVAILABILITY_TASK = "WBIAG_AVAILABILITY_TASK";
    public static final String UDF_DATE_FORMAT = "yyyyMMdd HHmmss";
    public static final String ABSOLUTE_DATE_FORMAT = "yyyyMMdd HHmmss";
    public static final String ROLLOUT_DATE_FORMAT = "MM/dd/yyyy";

    public WbiagAvailabilityTask() {
    }

    public Status run(int taskID, Map param) throws Exception {
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) logger.debug("WbiagAvailabilityTask.run(" + taskID + ", " + param + ")");

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
                return jobInterrupted("WbiagAvailabilityTask task has been interrupted.");
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

        WbiagAvailabilityProcess.WbiagAvailabilityProcessContext context =
            new WbiagAvailabilityProcess.WbiagAvailabilityProcessContext();

        Date taskDatetime = new Date();
        context.conn = conn;
        context.shouldCommit = true;
        context.intCheck = this;

        String sRecreatesRecs = (String)param.get(PARAM_RECREATES_RECORDS);
        context.recreatesSummaryDetail = StringHelper.isEmpty(sRecreatesRecs)
            ? false : "Y".equals(sRecreatesRecs) ? true : false;

        // *** haven't paramterized these yet
        context.runsForChangedEmps = true;
        context.runsForMissingSummaries = true;

        String rangeType = (String) param.get(PARAM_RANGE_TYPE);
        java.util.Date today = DateHelper.getCurrentDate();

        if (PARAM_VAL_RANGE_TYPE_ABSOLUTE.equals(rangeType)) {
            SimpleDateFormat df = new SimpleDateFormat(ABSOLUTE_DATE_FORMAT);
            String sStart = (String) param.get(PARAM_ABS_START_DATE);
            String sEnd = (String) param.get(PARAM_ABS_END_DATE);
            if (StringHelper.isEmpty(sStart) || StringHelper.isEmpty(sEnd)) {
                throw new RuntimeException("Both start/end dates must be supplied when absolute option is selected");
            }
            context.startDate = df.parse(sStart);
            context.endDate = df.parse(sEnd);
        }
        else if (PARAM_VAL_RANGE_TYPE_RELATIVE.equals(rangeType)) {
            String sBef = (String) param.get(PARAM_DAYS_BEFORE);
            String sAfter = (String) param.get(PARAM_DAYS_AFTER);
            if (StringHelper.isEmpty(sBef) || StringHelper.isEmpty(sAfter)) {
                throw new RuntimeException("Both before/after days must be supplied when relative option is selected");
            }

            Integer daysBefore = Integer.valueOf( sBef );
            Integer daysAfter = Integer.valueOf( sAfter);

            context.startDate = DateHelper.addDays( today, -(daysBefore.intValue()) );
            context.endDate = DateHelper.addDays( today, daysAfter.intValue() );
        }
        else {
            throw new RuntimeException("Unknown range type, must be absolute or relative : " + rangeType);
        }

        context.calcgrpIds = StringHelper.detokenizeStringAsIntArray(
            (String) param.get(PARAM_CALCGRPS),  "," , true);
        context.empIds = StringHelper.detokenizeStringAsIntArray(
            (String) param.get(PARAM_EMPLOYEES),  "," , true);
        context.paygrpIds = StringHelper.detokenizeStringAsIntArray(
            (String) param.get(PARAM_PAYGRPS),  "," , true);
        context.teamIds = StringHelper.detokenizeStringAsIntArray(
            (String) param.get(PARAM_WBTEAMS),  "," , true);
        String sSubteams = (String)param.get(PARAM_SUBTEAMS);
        context.subteams = StringHelper.isEmpty(sSubteams)
            ? true : "Y".equals(sSubteams) ? true : false;

        context.lastRun = getLastRunDate(conn);

        WbiagAvailabilityProcess pr = new WbiagAvailabilityProcess();
        WbiagAvailabilityProcess.WbiagAvailabilityProcessResult res =
            pr.process(context);

        updateLastRunDate(taskDatetime , WBINTTYPE_WBIAG_AVAILABILITY_TASK);
        StringBuffer sb = new StringBuffer(200);
        sb.append("Found : ").append(res.eligibleEmpCount).append(" eligible employees <br>");
        sb.append("Inserted : ").append(res.summariesInsertedCount).append(" summary records <br>");

        return sb.toString();
    }

    public String getTaskUI() {
        return "/jobs/wbiag/WbiagAvailabilityParams.jsp";
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

        return super.getLastRunDate(WBINTTYPE_WBIAG_AVAILABILITY_TASK,
                                    DateHelper.parseDate(rolloutDate, ROLLOUT_DATE_FORMAT));
    }


}
