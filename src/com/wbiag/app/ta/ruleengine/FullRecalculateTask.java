package com.wbiag.app.ta.ruleengine;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

import com.wbiag.app.ta.db.*;
import com.wbiag.app.ta.model.*;
import com.workbrain.app.scheduler.*;
import com.workbrain.app.scheduler.enterprise.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.security.*;
import com.workbrain.server.sql.*;
import com.workbrain.server.registry.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Recalculate task for full period calculation
 * </ul>
 */
public class FullRecalculateTask extends  AbstractScheduledJob{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FullRecalculateTask.class);
    private DBConnection conn = null;
    private StringBuffer taskLogMessage = new StringBuffer("Scheduled OK.");

    public static final String PARAM_DAYS_BEFORE = "DAYS_BEFORE";
    public static final String PARAM_DAYS_AFTER = "DAYS_AFTER";
    public static final String PARAM_ABS_START_DATE = "ABS_START_DATE";
    public static final String PARAM_ABS_END_DATE = "ABS_END_DATE";
    public static final String ABS_DATE_FORMAT = "yyyyMMdd HHmmss";
    public static final String PARAM_TASK_TYPE = "TASK_TYPE";
    public static final String PARAM_TASK_TYPE_RELATIVE = "rel";
    public static final String PARAM_TASK_TYPE_ABSOLUTE = "abs";

    public static final int MAX_BIND_SIZE =  com.workbrain.tool.jdbc.proxy.ProxyPreparedStatement.MAX_PARAMETER;
    public static final int MAX_CALC_BATCH_SIZE = RuleAccess.BATCH_PROCESS_SIZE_DEFAULT;
    public static final String  PARAM_CALCULATION_THREAD_COUNT = "CALCULATION_THREAD_COUNT";
    public static final String  PARAM_CLIENT_ID = "CLIENT_ID";
    public static final String  PARAM_BATCH_SIZE = "BATCH_SIZE";
    public static final String  PARAM_APPLY_TO_CALCGRPS = "APPLY_TO_CALCGRPS";
    public static final String  PARAM_APPLY_TO_PAYGRPS = "APPLY_TO_PAYGRPS";
    public static final String  PARAM_APPLY_TO_TEAMS = "APPLY_TO_TEAMS";
    public static final String  PARAM_APPLY_TO_EMPS = "APPLY_TO_EMPS";
    public static final String  PARAM_APPLY_TO_SUBTEAMS = "APPLY_TO_SUBTEAMS";
    public static final String  PARAM_AUTO_RECALC = "AUTO_RECALC";
    public static final String  PARAM_FUTURE_BALANCE_RECALC = "FUTURE_BALANCE_RECALC";
    public static final String  PARAM_VAL_ALL = "ALL";

    private boolean shouldCommit = true;
    private RecalculateRecords rec = null;;

    public FullRecalculateTask() {
    }

    /**
     * Runs scheduled task
     * @param taskID taskID
     * @param param params
     * @param schedule
     * @return
     * @throws Exception
     */
    public Status run(int taskID, Map param) throws Exception {
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) logger.debug("FullRecalculateTask.run(" + taskID + ", " + param + ")");

        String currentClientId = null;
        try {
            // *** store current client id
            currentClientId = SecurityService.getCurrentClientId();

            conn = new DBConnection(ConnectionManager.getConnection());
            conn.turnTraceOff();
            conn.setAutoCommit(false);

            if (StringHelper.isEmpty((String)param.get( PARAM_CLIENT_ID ))) {
                throw new RuntimeException ("Client_id must be specified");
            }
            String clientId = (String) param.get( PARAM_CLIENT_ID );

            SecurityService.setCurrentClientId(clientId);
            execute(conn , param);

            conn.commit();
        } catch (Exception e) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("com.wbiag.app.ta.ruleengine.FullRecalculateTask.class", e);}
            if( conn != null ) conn.rollback();
            throw e;
        } finally {
            SecurityService.setCurrentClientId(currentClientId);
            if( conn != null ) conn.close();
        }
        return jobOk(taskLogMessage.toString());

    }

    /**
     * TaskUI for parameters
     * @return
     */
    public String getTaskUI() {
        return "/jobs/wbiag/fullRecalculateTaskParams.jsp";
    }


    /**
     * Executes recalculate task based on parameters
     * @param c
     * @param param
     * @throws Exception
     */
    public void execute(DBConnection c , Map param) throws Exception {
        this.conn = c;

        Date taskDateTime = new Date();
        int batchSize = MAX_CALC_BATCH_SIZE ;
        if (param.get(PARAM_BATCH_SIZE) != null) {
            batchSize = Integer.parseInt((String)param.get(PARAM_BATCH_SIZE));
        }
        if (batchSize > MAX_CALC_BATCH_SIZE) {
            throw new RuntimeException("Calculation batch size cannot be more than " + MAX_CALC_BATCH_SIZE);
        }
        int threadCnt = 1;
        if (param.get(PARAM_CALCULATION_THREAD_COUNT) != null) {
            threadCnt = Integer.parseInt((String)param.get(PARAM_CALCULATION_THREAD_COUNT));
        }
        String sCalcgrps = (String) param.get(PARAM_APPLY_TO_CALCGRPS);
        int[] applyCalcgrpIds = StringHelper.detokenizeStringAsIntArray(
            PARAM_VAL_ALL.equals(sCalcgrps) ? null : sCalcgrps,
            "," , true);
        String sPaygrps = (String) param.get(PARAM_APPLY_TO_PAYGRPS);
        int[] applyPaygrpIds = StringHelper.detokenizeStringAsIntArray(
            PARAM_VAL_ALL.equals(sPaygrps) ? null : sPaygrps,
            "," , true);
        String sTeams = (String) param.get(PARAM_APPLY_TO_TEAMS);
        int[] applyTeamIds = StringHelper.detokenizeStringAsIntArray(
            PARAM_VAL_ALL.equals(sTeams) ? null : sTeams,
            "," , true);
        String sEmps = (String) param.get(PARAM_APPLY_TO_EMPS);
        int[] applyEmpIds = StringHelper.detokenizeStringAsIntArray(
            PARAM_VAL_ALL.equals(sEmps) ? null : sEmps,
            "," , true);
        String sSubteams = (String)param.get(PARAM_APPLY_TO_SUBTEAMS);
        boolean subTeams = StringHelper.isEmpty(sSubteams)
            ? false : "Y".equals(sSubteams) ? true : false;
        boolean autoRecalc = false;
        Boolean futureBalanceRecalc = null;
        String sAutoRecalc =  (String)param.get(PARAM_AUTO_RECALC);
        if (!StringHelper.isEmpty(sAutoRecalc)) {
            autoRecalc = Boolean.valueOf(sAutoRecalc).booleanValue();
        }
        String sFutBalRecalc =  (String)param.get(PARAM_FUTURE_BALANCE_RECALC);
        if (!StringHelper.isEmpty(sFutBalRecalc)) {
            futureBalanceRecalc = Boolean.valueOf(sFutBalRecalc);
        }

        java.util.Date today = new java.util.Date();
        java.util.Date startDate;
        java.util.Date endDate;
        String sodType = (String) param.get(PARAM_TASK_TYPE);
        if (PARAM_TASK_TYPE_ABSOLUTE.equals(sodType)) {
            String sStartDate = (String) param.get(PARAM_ABS_START_DATE);
            String sEndDate = (String) param.get(PARAM_ABS_END_DATE);
            if (StringHelper.isEmpty(sStartDate) || StringHelper.isEmpty(sEndDate)) {
                throw new RuntimeException ("Both start and end date must be supplied for absolute");
            }
            startDate = DateHelper.convertStringToDate(sStartDate , ABS_DATE_FORMAT) ;
            endDate = DateHelper.convertStringToDate(sEndDate , ABS_DATE_FORMAT) ;

        }
        else if (PARAM_TASK_TYPE_RELATIVE.equals(sodType)) {
            Integer daysBefore = Integer.valueOf( param.get(PARAM_DAYS_BEFORE).toString() );
            Integer daysAfter = Integer.valueOf( param.get(PARAM_DAYS_AFTER).toString() );

            startDate = DateHelper.addDays( today, -(daysBefore.intValue()) );
            endDate = DateHelper.addDays( today, daysAfter.intValue() );
        }
        else {
             throw new RuntimeException ("Task type not known :" + sodType);
        }


        appendToTaskLogMessage("STARTED FullRecalculateTask at: " + taskDateTime);
        if (logger.isDebugEnabled()) logger.debug("Params: " + param);

        rec = new RecalculateRecords(conn);
        rec.setAuditsCalculationErrorEmpDates(true);
        rec.setCalculationBatchSize(batchSize);
        rec.setCommitsEveryCalculationBatch(shouldCommit);
        rec.setInterruptCheck(this);
        rec.setSlaveThreadCount(threadCnt);
        rec.setProcessLevelAutoRecalc(autoRecalc);
        rec.setProcessLevelFutureBalanceRecalc(futureBalanceRecalc);
        rec.setCreatesDefaultRecords(false);
        rec.addEmployeeDate(applyEmpIds, applyTeamIds, applyCalcgrpIds,
                            applyPaygrpIds, subTeams, startDate, endDate);

        rec.execute();

        if (rec.getCalculationEmployeeDatesErrors().size() > 0) {
            if (logger.isDebugEnabled())
                logger.debug("Errored employees :" +
                             rec.getCalculationEmployeeDatesErrors());
        }
        appendToTaskLogMessage("FINISHED in : " +
                               (System.currentTimeMillis() -
                                taskDateTime.getTime()) + " .ms"
                               + ". Recalculated " + rec.getEmployeeDatesCount() +
                               " employee dates,"
                               + " errored employee count:" +
                               rec.getCalculationEmployeeDatesErrors().size());
    }

    protected String getTaskLogMessage() {
        return taskLogMessage.toString();
    }

    protected void appendToTaskLogMessage(String s) {
        taskLogMessage.append("<br>" + s);
        if (logger.isDebugEnabled()) logger.debug(s);
    }

    /**
     * Used for unit testing. Task always commits from scheduler
     * @param v
     */
    public void setShouldCommit(boolean v) {
        shouldCommit = v;
    }

    public Map getEmployeeDatesCalculated() {
        return rec.getEmployeeDates();
    }


}
