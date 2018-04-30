package com.wbiag.app.export.payroll;

import java.util.*;
import java.util.Date;

import com.workbrain.util.*;
import com.workbrain.sql.*;
import com.workbrain.server.sql.*;
import com.workbrain.app.scheduler.enterprise.*;
import com.workbrain.security.SecurityService;

public class PayrollExporterTask
    extends AbstractScheduledJob {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(PayrollExporterTask.class);

    private DBConnection conn = null;
    private StringBuffer taskLogMessage = new StringBuffer("Scheduled OK.");

    public static final String UDF_DATE_FORMAT = "yyyyMMdd HHmmss";

    public static final String PARAM_CLIENT_ID = "CLIENT_ID";
    public static final String PARAM_PAYGRP_ID = "PAYGRP_ID";
    public static final String PARAM_PET_ID = "PET_ID";
    public static final String PARAM_CYCLE = "CYCLE";
    public static final String PARAM_LOOK_BACK_DAYS = "LOOK_BACK_DAYS";
    public static final String PARAM_CHECK_UNAUTH = "CHK_UNAUTH";
    public static final String PARAM_CONTINUE_ON_UNAUTH = "CONTINUE_ON_UNAUTH";
    public static final String PARAM_ALL_READY_PAYGRPS = "ALL_READY_PAYGRPS";
    public static final String PARAM_ALL_READY_WHERE = "ALL_READY_WHERE";
    public static final String PARAM_RESET_READINESS = "RESET_READINESS";
    public static final String PARAM_USE_PAY_PERIOD = "USE_PAY_PERIOD";
    public static final String PARAM_ADJUST_DATES = "ADJUST_DATES";
    public static final String PARAM_WRITE_TO_FILE = "WRITE_TO_FILE";
    public static final String PARAM_MERGE_FILES = "MERGE_FILES";
    public static final String PARAM_WRITE_TO_TABLE = "WRITE_TO_STAGING_TABLE";
    public static final String PARAM_START_DATE = "START_DATE";
    public static final String PARAM_END_DATE = "END_DATE";
    public static final String PARAM_TERM_ADD_DAYS = "TERM_ADD_DAYS";
    

    private static final String CYCLE_NEW = "New Employee";
    private static final String CYCLE_TERM = "Terminated Employee";
    

    public ScheduledJob.Status run(int taskID, Map param) throws
        Exception {
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
            logger.debug("PayrollExporterTask.run(" + taskID + ", " + param + ")");
        }
        String currentClientId = null;
        try {
            // *** store current client id
            currentClientId = SecurityService.getCurrentClientId();

            conn = new DBConnection(ConnectionManager.getConnection());
            conn.turnTraceOff();
            conn.setAutoCommit(false);

            if (StringHelper.isEmpty( (String) param.get(PARAM_CLIENT_ID))) {
                throw new RuntimeException("Client_id must be specified");
            }
            String clientId = (String) param.get(PARAM_CLIENT_ID);

            SecurityService.setCurrentClientId(clientId);
            execute(conn, param);

            conn.commit();
        }
        catch (Exception e) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger.error(
                    "com.wbiag.app.export.payroll.PayrollExporterTask", e);
            }
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        }
        finally {
            SecurityService.setCurrentClientId(currentClientId);
            if (conn != null) {
                conn.close();
            }
        }
        return new ScheduledJob.Status(false, taskLogMessage.toString());

    }

    public String getTaskUI() {
        return "/jobs/payrollExporterTaskParams.jsp";
    }

    public void execute(DBConnection c, Map param) throws Exception {
        this.conn = c;

        PayrollExporter payExp = null;
        int[] paygrpIdsA = null;
        int petID;
        int LBDays;
        int termDays;
        String cycle;
        String allReadyWhere = null;
        String comment = " ";
        boolean allReadyPaygrps;
        boolean resetReadiness;
        boolean usePayPeriod;
        boolean adjustDates;
        boolean writeToFile;
        boolean mergeFiles;
        boolean writeToTable;
        boolean chkUnauth = true;
        boolean continueOnUnauth = true;
        Date dStartDate = null;
        Date dEndDate = null;

        allReadyPaygrps = Boolean.valueOf( (String) param.get(
            PARAM_ALL_READY_PAYGRPS)).booleanValue();

        if (allReadyPaygrps) {
            paygrpIdsA = new int[1];
            paygrpIdsA[0] = -99;
            allReadyWhere = (String) param.get(PARAM_ALL_READY_WHERE);
        }
        else {
            paygrpIdsA = detokenizeStringAsIntArray( (String) param.get(
                PARAM_PAYGRP_ID), ",");
        }
        resetReadiness = Boolean.valueOf( (String) param.get(
            PARAM_RESET_READINESS)).booleanValue();

        if ( (paygrpIdsA != null) && (paygrpIdsA.length != 0)) {
            petID = Integer.parseInt( (String) param.get(PARAM_PET_ID));
            cycle = (String) param.get(PARAM_CYCLE);
            if (!StringHelper.isEmpty(param.get(PARAM_LOOK_BACK_DAYS))){
                LBDays = Integer.parseInt( (String) param.get(PARAM_LOOK_BACK_DAYS));
            } else {
                LBDays = 0;
            }
            if (!StringHelper.isEmpty(param.get(PARAM_TERM_ADD_DAYS))){
                termDays = Integer.parseInt( (String) param.get(PARAM_TERM_ADD_DAYS));
            } else {
                termDays = 0;
            }
            usePayPeriod = Boolean.valueOf( (String) param.get(
                PARAM_USE_PAY_PERIOD)).booleanValue();
            adjustDates = Boolean.valueOf( (String) param.get(
                PARAM_ADJUST_DATES)).booleanValue();
            writeToFile = Boolean.valueOf( (String) param.get(
                PARAM_WRITE_TO_FILE)).booleanValue();
            mergeFiles = Boolean.valueOf( (String) param.get(
                PARAM_MERGE_FILES)).booleanValue();
            writeToTable = Boolean.valueOf( (String) param.get(
                PARAM_WRITE_TO_TABLE)).booleanValue();

            chkUnauth = Boolean.valueOf( (String) param.get(
                PARAM_CHECK_UNAUTH)).booleanValue();
            continueOnUnauth = Boolean.valueOf( (String) param.get(
                PARAM_CONTINUE_ON_UNAUTH)).booleanValue();

            if (!usePayPeriod) {
                dStartDate = DateHelper.parseDate( (String) param.get(
                    PARAM_START_DATE), UDF_DATE_FORMAT);
                dEndDate = DateHelper.parseDate( (String) param.get(
                    PARAM_END_DATE), UDF_DATE_FORMAT);
            }

            if (cycle.equalsIgnoreCase(CYCLE_NEW)) {
                payExp = new PayrollExporter(conn, PayrollExporter.MODE_REGULAR);
                payExp.setLookBackDays(LBDays);
                payExp.setCycle(PayrollExporter.CYCLE_ON_BOARD);
                payExp.setAllReadyWhere(allReadyWhere);
                payExp.setPaygrpIds(paygrpIdsA);
            }
            else if (cycle.equalsIgnoreCase(CYCLE_TERM)) {
                payExp = new PayrollExporter(conn, PayrollExporter.MODE_REGULAR);
                payExp.setPaygrpIds(paygrpIdsA);
                payExp.setCycle(PayrollExporter.CYCLE_TERM);
                payExp.setTermAddDays(termDays);
            }
            else {
                payExp = new PayrollExporter(conn, PayrollExporter.MODE_REGULAR);
                payExp.setCycle(PayrollExporter.CYCLE_REGULAR);
                payExp.setAllReadyWhere(allReadyWhere);
                payExp.setPaygrpIds(paygrpIdsA);
                if (dStartDate == null || dEndDate == null) {
                    payExp.useDefaultDates();
                }
                else {
                    payExp.setDates(dStartDate, dEndDate);
                }
                
                if (!payExp.checkPaygrpsReady()) {
                    if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                        logger.error(
                            "Error exporting records, Some records not ready");
                    }
                    taskLogMessage.append(
                        " Some pay groups not ready. ");
                    //throw new RuntimeException(comment);
                }
            }

            

            ArrayList badPG = null;
            if (chkUnauth) {
                int[] exportPaygrp = payExp.getPayGroups();
                int paygrpNum = exportPaygrp.length;
                if (logger.isDebugEnabled()){
                    StringBuffer debugMsg = new StringBuffer();
                    debugMsg.append("Checking pay group authorization ")
                        .append(paygrpNum).append(" pay groups to check ");
                    logger.debug(debugMsg.toString());
                }
                //Check Readiness again and Run Task
                badPG = payExp.checkUnauth(continueOnUnauth);
                if (!badPG.isEmpty()) {
                    StringBuffer msg = new StringBuffer();
                    msg.append("Some records not authorized.\n")
                            .append(badPG.size()).append(" of ").append(paygrpNum)
                            .append(" pay groups failed: ");
                    for (int i=0; i <badPG.size(); i++){
                        msg.append(badPG.get(i)).append(" ");
                    }
                    msg.append("Continue On unauth is " + continueOnUnauth);
                    if (logger.isDebugEnabled()){
                        logger.debug(msg.toString());
                    }
                    taskLogMessage.append(msg.toString());
                    if ( (badPG.size() == paygrpNum) ||
                        (!continueOnUnauth)) {
                        StringBuffer errMsg = new StringBuffer();
                        errMsg.append("Error exporting records, ")
                                .append("some records not authorized.")
                                .append(" Check Authorization Report \n")
                                .append(" No records will be exported ")
                                .append(badPG.size()).append(" of ").append(paygrpNum)
                                .append(" pay groups failed: ");
                        for (int i=0; i <badPG.size(); i++){
                            errMsg.append(badPG.get(i)).append(" ");
                        }
                        throw new RuntimeException(errMsg.toString());
                    }
                }
            }

            payExp.setPayExpTsk(petID);
            payExp.setWriteToFile(writeToFile);
            payExp.setWriteToTable(writeToTable);
            payExp.setAdjustDates(adjustDates);
            payExp.setMergeFiles(mergeFiles);
            payExp.process();
            taskLogMessage.append(payExp.getMessage());

        }

        if (resetReadiness) {
            if (payExp == null) {
                payExp = new PayrollExporter(conn, "Reset");
            }
            payExp.resetReadiness();
            taskLogMessage.append(payExp.getMessage());
        }

    }

    private int[] detokenizeStringAsIntArray(String input,
                                             String separator) {
        String[] st = StringHelper.detokenizeString(input, separator);
        if (st == null || st.length == 0) {
            return null;
        }
        int[] stArray = new int[st.length];
        for (int i = 0; i < st.length; i++) {
            stArray[i] = Integer.parseInt(st[i]);
        }
        return stArray;
    }

}
