package com.wbiag.app.bo.ejb.actions;

import java.util.*;


import com.workbrain.app.bo.ejb.actions.*;
import com.workbrain.app.workflow.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

import com.wbiag.app.export.payroll.PayrollExporter;

/*
 * Export Payroll Action
 *
 */
public class ExportPayrollAction
    extends AbstractActionProcess {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(ExportPayrollAction.class);

    private static final String CYCLE_NEW = "New Employee";
    private static final String CYCLE_TERM = "Terminated Employee";

    private static final String MODE = PayrollExporter.MODE_REGULAR;

    private static final String PARAM_PAYSYS_ID = "paysys_id";
    private static final String PARAM_PAYGRP_ID = "paygrp_ids";
    private static final String PARAM_EMP_ID    = "emp_ids";
    private static final String PARAM_EMP_NAMES = "emp_names";
    private static final String PARAM_ALL_READY = "all_ready_paygrps";
    private static final String PARAM_ALL_READY_WHERE = "all_ready_where"; //TT49367
    private static final String PARAM_CHECK_READY = "check_pgs_ready";
    private static final String PARAM_RESET     = "reset";
    private static final String PARAM_CYCLE		= "cycle";
    private static final String PARAM_USE_PP	= "use_pay_period";
    private static final String PARAM_ADJ_DATE	= "adjust_dates";
    private static final String PARAM_WRITE_FILE = "write_to_file";
    private static final String PARAM_MERGE_FILE = "merge_files";
    private static final String PARAM_WRITE_TABLE = "write_to_table";
    private static final String PARAM_START_DATE = "start_date";
    private static final String PARAM_END_DATE = "end_date";
    
    
    private static final String PARAM_ACTION_CKUNAUTH = "CHK_UNAUTH";
    private static final String PARAM_ACTION_LOOK_BACK = "LOOK_BACK_DAYS";
    private static final String PARAM_ACTION_CONT_ON_UNAUTH = "CONTINUE_ON_UNAUTH";
    private static final String PARAM_ACTION_TERM_ADD_DAYS = "TERM_ADD_DAYS";
    
    private PayrollExporter payExp = null;
    private int[] paygrpIdsA = null;
    private int[] empIdsA = null;
    private int petID;
    private String cycle;
    private boolean allReadyPaygrps = false;
    private boolean resetReadiness = false;
    private String allReadyWhere = null;
    private boolean checkReady = true;
    private boolean usesPayPeriod;
    private boolean adjustDates;
    private boolean writeToFile;
    private boolean mergeFiles;
    private boolean writeToTable;
    private boolean chkUnauth;
    private boolean continueOnUnauth;
    

    private Date dStartDate = null;
    private Date dEndDate = null;
    
    public ActionResponse processObject(Action data, WBObject object,
                                        Branch[] outputs,
                                        ActionResponse previous) throws
        WorkflowEngineException {
        if (logger.isInfoEnabled()) {
            logger.info("ExportPayrollAction.processObject");
        }

        DBConnection conn = null;

        String results = null;
        

        try {
            conn = this.getConnection();
            getParams(object, data);
          

           if (( (paygrpIdsA != null) && (paygrpIdsA.length != 0))||( (empIdsA != null) && (empIdsA.length != 0))) {
     
                if (MODE.equalsIgnoreCase(PayrollExporter.MODE_BASIC)) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Operating in basic export mode");
                    }

                    payExp = new PayrollExporter(conn, MODE);
                    payExp.setCycle(PayrollExporter.CYCLE_REGULAR);
                    payExp.setDates(dStartDate, dEndDate);
                    if (empIdsA == null) {
                        payExp.setAllReadyWhere(allReadyWhere);
                        payExp.setPaygrpIds(paygrpIdsA);
                    }
                    else {
                        payExp.setEmpIds(empIdsA);
                    }

                }
                else {
                    if (cycle.equalsIgnoreCase(CYCLE_NEW)) {

                        int LBDays = -14;

                        if (logger.isInfoEnabled()) {
                            logger.info("New employee cycle");
                        }
                        if (data.propertyExists(PARAM_ACTION_LOOK_BACK)) {
                            String LBDaysString = WorkflowUtil.
                                getDataPropertyAsString(
                                data, PARAM_ACTION_LOOK_BACK);
                            if (LBDaysString.indexOf("default=") != -1) {
                                String subS = LBDaysString.substring(
                                    LBDaysString.
                                    indexOf("default=") + 8);
                                if (!(subS.indexOf('~') == -1)) {
                                    LBDaysString = subS.substring(0,
                                        subS.indexOf('~'));
                                }
                                else {
                                    LBDaysString = subS;
                                }
                            }
                            LBDays = Integer.valueOf(LBDaysString).intValue();
                        }
                        payExp = new PayrollExporter(conn, MODE);
                        if (empIdsA == null) {
                            payExp.setPaygrpIds(paygrpIdsA);
                        }
                        else {
                            payExp.setEmpIds(empIdsA);
                        }
                        payExp.setLookBackDays(LBDays);
                        payExp.setCycle(PayrollExporter.CYCLE_ON_BOARD);
                    }
                    else if (cycle.equalsIgnoreCase(CYCLE_TERM)) {
                        
                        int termAdd = 7;
                        
                        if (logger.isInfoEnabled()) {
                            logger.info("Terminated employee cycle");
                        }
                        
                        if (data.propertyExists(PARAM_ACTION_TERM_ADD_DAYS)) {
                            String TADaysString = WorkflowUtil.
                                getDataPropertyAsString(
                                data, PARAM_ACTION_TERM_ADD_DAYS);
                            if (TADaysString.indexOf("default=") != -1) {
                                String subS = TADaysString.substring(
                                        TADaysString.
                                    indexOf("default=") + 8);
                                if (!(subS.indexOf('~') == -1)) {
                                    TADaysString = subS.substring(0,
                                        subS.indexOf('~'));
                                }
                                else {
                                    TADaysString = subS;
                                }
                            }
                            termAdd = Integer.valueOf(TADaysString).intValue();
                        }

                        payExp = new PayrollExporter(conn, MODE);
                        if (empIdsA == null) {
                            payExp.setPaygrpIds(paygrpIdsA);
                        }
                        else {
                            payExp.setEmpIds(empIdsA);
                        }
                        payExp.setTermAddDays(termAdd);
                        payExp.setCycle(PayrollExporter.CYCLE_TERM);
                    }
                    else {
                        if (logger.isInfoEnabled()) {
                            logger.info("Regular cycle");
                        }

                        payExp = new PayrollExporter(conn, MODE);
                        payExp.setCycle(PayrollExporter.CYCLE_REGULAR);
                        if (empIdsA == null) {
                            payExp.setAllReadyWhere(allReadyWhere);
                            payExp.setPaygrpIds(paygrpIdsA);
                        }
                        else {

                            payExp.setEmpIds(empIdsA);
                        }
                        if (dStartDate == null || dEndDate == null) {
                            payExp.useDefaultDates();
                        }
                        else {
                            payExp.setDates(dStartDate, dEndDate);
                        }
                        
                        if (checkReady && !payExp.checkPaygrpsReady()) {
                            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                                logger.error(
                                    "Error exporting records, Some records not ready");
                            }
                            String comment =
                                "Error exporting records, Some pay groups not ready. Check Readiness Report";
                            return WorkflowUtil.createActionResponse(outputs,
                                "Failure", comment);
                        }
                    }
                }

                if (data.propertyExists(PARAM_ACTION_CKUNAUTH)) {
                    String chkUnauthString = WorkflowUtil.
                        getDataPropertyAsString(
                        data, PARAM_ACTION_CKUNAUTH);
                    if (chkUnauthString.equalsIgnoreCase("false")) {
                        chkUnauth = false;
                    }
                    else {
                        chkUnauth = true;
                    }

                }
                else {
                    chkUnauth = true;
                }
                if (data.propertyExists(PARAM_ACTION_CONT_ON_UNAUTH)) {
                    String chkUnauthString = WorkflowUtil.
                        getDataPropertyAsString(
                        data, PARAM_ACTION_CONT_ON_UNAUTH);
                    if (chkUnauthString.equalsIgnoreCase("true")) {
                        continueOnUnauth = true;
                    }
                    else {
                        continueOnUnauth = false;
                    }

                }
                else {
                    continueOnUnauth = false;
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
                    //Check Readiness and Run Task
                    if (empIdsA != null){
                        badPG = payExp.checkUnauth(continueOnUnauth);
                    }
                    else {
                        if (logger.isDebugEnabled()){
                            logger.debug("Export Selected by Employee. Continue on Unauth is set to false");
                        }
                        badPG = payExp.checkUnauth(false);
                    }
                    if (badPG != null){
                        if (!badPG.isEmpty()) {
                            StringBuffer errMsg = new StringBuffer();
                            errMsg.append("Some records not authorized.\n")
                                    .append(badPG.size()).append(" of ").append(paygrpNum)
                                    .append(" groups failed: ");
                            for (int i=0; i <badPG.size(); i++){
                                errMsg.append(badPG.get(i)).append(" ");
                            }
                            errMsg.append("Continue On unauth is " + continueOnUnauth);
                            if (logger.isDebugEnabled()){
                                logger.debug(errMsg.toString());
                            }
                    		if ( (empIdsA != null) ||
                    				(badPG.size() == paygrpNum) ||
									(!continueOnUnauth)) {
                    			return WorkflowUtil.createActionResponse(outputs,
                    					"Failure", errMsg.toString());
                    		}
                    	}
                    }
                }
                payExp.setPayExpTsk(petID);
                payExp.setWriteToFile(writeToFile);
                payExp.setWriteToTable(writeToTable);
                payExp.setAdjustDates(adjustDates);
                payExp.setMergeFiles(mergeFiles);
                payExp.process();
                this.commit(conn);
                results = payExp.getMessage();
            }

            if (resetReadiness) {
                if (payExp == null) {
                    payExp = new PayrollExporter(conn, MODE);
                    payExp.setCycle(PayrollExporter.CYCLE_RESET);
                }
                payExp.resetReadiness();
                results += payExp.getMessage();
            }

        }
        catch (Exception t) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger.error(
                    "com.workbrain.app.bo.ejb.actions.ExportPayrollAction.class",
                    t);
            }
            this.rollback(conn);
            throw new WorkflowEngineException(t);
        }
        finally {
            this.close(conn);
        }
        
        return WorkflowUtil.createActionResponse(outputs, "Success", results);
    }

    private void getParams(WBObject object, Action data) throws WorkflowEngineException, Exception{
        if (WorkflowUtil.fieldExists(object, PARAM_ALL_READY)) {
            allReadyPaygrps = WorkflowUtil.isCheckBoxSelected(
                object,
                PARAM_ALL_READY);
        }
        if (WorkflowUtil.fieldExists(object, PARAM_RESET)) {
            resetReadiness = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_RESET);
        }

        if (WorkflowUtil.fieldExists(object, PARAM_CHECK_READY)) {
            checkReady = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_CHECK_READY);
        }
        
        if (allReadyPaygrps) {
            paygrpIdsA = new int[1];
            paygrpIdsA[0] = -99;
            if (WorkflowUtil.fieldExists(object, PARAM_ALL_READY_WHERE)){
                allReadyWhere = WorkflowUtil.getFieldValueAsString(object, PARAM_ALL_READY_WHERE);
                if (logger.isDebugEnabled()){
                    logger.debug("Setting allReadyWhere to "+ allReadyWhere);
                }
            }
            
        }
        else {
            if (WorkflowUtil.fieldExists(object, PARAM_PAYGRP_ID)) {
                paygrpIdsA = detokenizeStringAsIntArray(
                    WorkflowUtil.getFieldValueId(object, PARAM_PAYGRP_ID),
                    ",");
            }
            else if (WorkflowUtil.fieldExists(object, PARAM_EMP_ID)) {
                empIdsA = detokenizeStringAsIntArray(
                    WorkflowUtil.getFieldValueId(object, PARAM_EMP_ID),
                    ",");
            }
            else if (WorkflowUtil.fieldExists(object, PARAM_EMP_NAMES)) {
                empIdsA = detokenizeStringEmpNameAsIntEmpIdArray(
                    WorkflowUtil.getFieldValueId(object, PARAM_EMP_NAMES),
                    ",");
            }

            else {
                throw new WorkflowEngineException(
                    "paygrp_ids, emp_ids or emp_names must be provided on form");
            }
        }
        
        //More
        if (!WorkflowUtil.fieldExists(object, PARAM_PAYSYS_ID)) {
            throw new WorkflowEngineException(
                "paysys_id missing from form");
        }
        if (WorkflowUtil.getFieldValueId(object,
                PARAM_PAYSYS_ID) == null) {
            petID = WorkflowUtil.getFieldValueAsInt(object,
                    PARAM_PAYSYS_ID);
        }
        else {
            petID = Integer.valueOf(WorkflowUtil.getFieldValueId(object,
                    PARAM_PAYSYS_ID)).intValue();
        }
        if (WorkflowUtil.fieldExists(object, PARAM_CYCLE)) {
            cycle = WorkflowUtil.getFieldValueAsString(object, PARAM_CYCLE);
        }
        else {
            cycle = PayrollExporter.CYCLE_REGULAR;
        }
        if (WorkflowUtil.fieldExists(object, PARAM_USE_PP)) {
            usesPayPeriod = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_USE_PP);
        }
        else {
            usesPayPeriod = false;
        }
        if (WorkflowUtil.fieldExists(object, PARAM_ADJ_DATE)) {
            adjustDates = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_ADJ_DATE);
        }
        else {
            adjustDates = false;
        }
        if (WorkflowUtil.fieldExists(object, PARAM_WRITE_FILE)) {
            writeToFile = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_WRITE_FILE);
        }
        else {
            writeToFile = false;
        }
        if (WorkflowUtil.fieldExists(object, PARAM_MERGE_FILE)) {
            mergeFiles = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_MERGE_FILE);
        }
        else {
            mergeFiles = false;
        }
        if (WorkflowUtil.fieldExists(object, PARAM_WRITE_TABLE)) {
            writeToTable = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_WRITE_TABLE);
        }
        else {
            writeToTable = false;
        }

        if (!usesPayPeriod) {
            if (WorkflowUtil.fieldExists(object, PARAM_START_DATE)) {
                dStartDate = WorkflowUtil.getFieldValueAsDate(object,
                        PARAM_START_DATE);
            }
            if (WorkflowUtil.fieldExists(object, PARAM_END_DATE)) {
                dEndDate = WorkflowUtil.getFieldValueAsDate(object,
                        PARAM_END_DATE);
            }
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

    private int[] detokenizeStringEmpNameAsIntEmpIdArray(String input,
        String separator) throws Exception {
        String[] st = StringHelper.detokenizeString(input, separator);
        EmployeeAccess ea = new EmployeeAccess(this.getConnection(),
                                               (CodeMapper) getConnection().
                                               getCodeMapper());
        if (st == null || st.length == 0) {
            return null;
        }
        int[] stArray = new int[st.length];
        for (int i = 0; i < st.length; i++) {
            int empId = ea.loadByName(st[i], DateHelper.getCurrentDate()).
                getEmpId();
            stArray[i] = empId;
        }
        return stArray;
    }

}
