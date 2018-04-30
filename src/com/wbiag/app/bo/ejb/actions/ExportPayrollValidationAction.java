package com.wbiag.app.bo.ejb.actions;

import java.util.*;

import com.workbrain.app.bo.ejb.actions.*;
import com.workbrain.app.workflow.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

import com.wbiag.app.export.payroll.PayrollExporter;

/*
 * Export Payroll Validation Action
 *  
 */
public class ExportPayrollValidationAction extends AbstractActionProcess {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger
            .getLogger(ExportPayrollValidationAction.class);

    private static final String CYCLE_NEW = "New Employee";
    private static final String CYCLE_TERM = "Terminated Employee";
    private static final String MODE = PayrollExporter.MODE_REGULAR;
    private static final String PARAM_PAYGRP_ID = "paygrp_ids";
    private static final String PARAM_EMP_ID = "emp_ids";
    private static final String PARAM_EMP_NAMES = "emp_names";
    private static final String PARAM_ALL_READY = "all_ready_paygrps";
    private static final String PARAM_ALL_READY_WHERE = "all_ready_where"; // TT49367
    private static final String PARAM_CHECK_READY = "check_pgs_ready";
    private static final String PARAM_CYCLE = "cycle";
    private static final String PARAM_USE_PP = "use_pay_period";
    private static final String PARAM_START_DATE = "start_date";
    private static final String PARAM_END_DATE = "end_date";
    private static final String PARAM_ACTION_CKUNAUTH = "CHK_UNAUTH";
    private static final String PARAM_ACTION_LOOK_BACK = "LOOK_BACK_DAYS";

    private PayrollExporter payExp = null;
    private int[] paygrpIdsA = null;
    private int[] empIdsA = null;
    private String cycle;
    private boolean allReadyPaygrps = false;
    private String allReadyWhere = null;
    private boolean usesPayPeriod;
    private boolean chkUnauth;
    private boolean checkReady = true;

    private Date dStartDate = null;
    private Date dEndDate = null;

    public ActionResponse processObject(Action data, WBObject object,
            Branch[] outputs, ActionResponse previous)
            throws WorkflowEngineException {

        if (logger.isInfoEnabled()) {
            logger.info("ExportPayrollValidationAction.processObject");
        }
        DBConnection conn = null;

        try {
            conn = this.getConnection();
            getParams(object, data);

            if (((paygrpIdsA != null) && (paygrpIdsA.length != 0))
                    || ((empIdsA != null) && (empIdsA.length != 0))) {

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
                    } else {
                        payExp.setEmpIds(empIdsA);
                    }

                } else {
                    if (cycle.equalsIgnoreCase(CYCLE_NEW)) {

                        int LBDays = -14;

                        if (logger.isInfoEnabled()) {
                            logger.info("New employee cycle");
                        }
                        if (data.propertyExists(PARAM_ACTION_LOOK_BACK)) {
                            String LBDaysString = WorkflowUtil
                                    .getDataPropertyAsString(data,
                                            PARAM_ACTION_LOOK_BACK);
                            if (LBDaysString.indexOf("default=") != -1) {
                                String SubS = LBDaysString
                                        .substring(LBDaysString
                                                .indexOf("default=") + 8);
                                if (SubS.indexOf('~') != -1) {
                                    LBDaysString = SubS.substring(0, SubS
                                            .indexOf('~'));
                                } else {
                                    LBDaysString = SubS;
                                }
                            }
                            LBDays = Integer.valueOf(LBDaysString).intValue();
                        }
                        payExp = new PayrollExporter(conn, MODE);
                        if (empIdsA == null) {
                            payExp.setPaygrpIds(paygrpIdsA);
                        } else {
                            payExp.setEmpIds(empIdsA);
                        }
                        payExp.setLookBackDays(LBDays);
                        payExp.setCycle(PayrollExporter.CYCLE_ON_BOARD);
                    } else if (cycle.equalsIgnoreCase(CYCLE_TERM)) {
                        if (logger.isInfoEnabled()) {
                            logger.info("Terminated employee cycle");
                        }

                        payExp = new PayrollExporter(conn, MODE);
                        if (empIdsA == null) {
                            payExp.setPaygrpIds(paygrpIdsA);
                        } else {
                            payExp.setEmpIds(empIdsA);
                        }
                        payExp.setCycle(PayrollExporter.CYCLE_TERM);
                    } else {
                        if (logger.isInfoEnabled()) {
                            logger.info("Regular cycle");
                        }

                        payExp = new PayrollExporter(conn, MODE);
                        payExp.setCycle(PayrollExporter.CYCLE_REGULAR);
                        if (empIdsA == null) {
                            payExp.setAllReadyWhere(allReadyWhere);
                            payExp.setPaygrpIds(paygrpIdsA);
                        } else {
                            payExp.setEmpIds(empIdsA);
                        }
                        if (dStartDate == null || dEndDate == null) {
                            payExp.useDefaultDates();
                        } else {
                            payExp.setDates(dStartDate, dEndDate);
                        }
                        
                        if (checkReady && !payExp.checkPaygrpsReady()) {
                            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                                logger
                                        .error("Error exporting records, Some records not ready");
                            }
                            String comment = "Error exporting records, Some pay groups not ready. Check Readiness Report";
                            throw new WorkflowEngineException(comment);
                        }
                    }
                }

                

                if (data.propertyExists(PARAM_ACTION_CKUNAUTH)) {

                    String chkUnauthString = WorkflowUtil
                            .getDataPropertyAsString(data,
                                    PARAM_ACTION_CKUNAUTH);
                    if (chkUnauthString.equalsIgnoreCase("false")) {
                        chkUnauth = false;
                    } else {
                        chkUnauth = true;
                    }
                } else {
                    chkUnauth = true;
                }

                if (chkUnauth) {
                    // Check Readiness again and Run Task
                    ArrayList badPG = payExp.checkUnauth();
                    if (badPG != null) {
                        if (!badPG.isEmpty()) {
                            String comment = "Error exporting records, some records not authorized. Check Authorization Report \n";
                            comment += "The following pay groups failed "
                                    + badPG.toString();
                            throw new WorkflowEngineException(comment);
                        }
                    }
                }
            }
        } catch (Exception t) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger
                        .error(
                                "com.workbrain.app.bo.ejb.actions.ExportPayrollAction.class",
                                t);
            }
            throw new WorkflowEngineException(t);
        } finally {
            this.close(conn);
        }
        String comment = "Export has completed successfully";
        return WorkflowUtil.createActionResponse(outputs, "Success", comment);
    }

    private void getParams(WBObject object, Action data)
            throws WorkflowEngineException, Exception {
        if (WorkflowUtil.fieldExists(object, PARAM_ALL_READY)) {
            allReadyPaygrps = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_ALL_READY);
        }
        
        if (WorkflowUtil.fieldExists(object, PARAM_CHECK_READY)) {
            checkReady = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_CHECK_READY);
        }

        if (allReadyPaygrps) {
            paygrpIdsA = new int[1];
            paygrpIdsA[0] = -99;
            if (WorkflowUtil.fieldExists(object, PARAM_ALL_READY_WHERE)) {
                allReadyWhere = WorkflowUtil.getFieldValueAsString(object,
                        PARAM_ALL_READY_WHERE);
                if (logger.isDebugEnabled()) {
                    logger.debug("Setting allReadyWhere to " + allReadyWhere);
                }
            }
        } else {
            if (WorkflowUtil.fieldExists(object, PARAM_PAYGRP_ID)) {
                paygrpIdsA = detokenizeStringAsIntArray(WorkflowUtil
                        .getFieldValueId(object, PARAM_PAYGRP_ID), ",");
            } else if (WorkflowUtil.fieldExists(object, PARAM_EMP_ID)) {
                empIdsA = detokenizeStringAsIntArray(WorkflowUtil
                        .getFieldValueId(object, PARAM_EMP_ID), ",");
            } else if (WorkflowUtil.fieldExists(object, PARAM_EMP_NAMES)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Feild emp_names exits");
                    logger.debug(new StringBuffer().append("Values of ")
                            .append(PARAM_EMP_NAMES).append(" :").append(
                                    WorkflowUtil.getFieldValueId(object,
                                            PARAM_EMP_NAMES)));
                }
                empIdsA = detokenizeStringEmpNameAsIntEmpIdArray(WorkflowUtil
                        .getFieldValueId(object, PARAM_EMP_NAMES), ",");
            } else {
                throw new WorkflowEngineException(
                        "paygrp_ids, emp_ids or emp_names must be provided on form");
            }
        }

        if (WorkflowUtil.fieldExists(object, PARAM_CYCLE)) {
            cycle = WorkflowUtil.getFieldValueAsString(object, PARAM_CYCLE);
        } else {
            cycle = PayrollExporter.CYCLE_REGULAR;
        }
        if (WorkflowUtil.fieldExists(object, PARAM_USE_PP)) {
            usesPayPeriod = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_USE_PP);
        } else {
            usesPayPeriod = false;
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

    private int[] detokenizeStringAsIntArray(String input, String separator) {
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
        if (logger.isDebugEnabled()) {
            logger.debug("Checking String" + input);
        }
        String[] st = StringHelper.detokenizeString(input, separator);
        EmployeeAccess ea = new EmployeeAccess(this.getConnection(),
                (CodeMapper) getConnection().getCodeMapper());
        if (st == null || st.length == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("No employee found in input string");
            }
            return null;
        }
        int[] stArray = new int[st.length];
        for (int i = 0; i < st.length; i++) {
            if (logger.isDebugEnabled()) {
                logger.debug("Looking up ID for employee name " + st[i]);
            }
            EmployeeData ed = ea.loadByName(st[i], DateHelper.getCurrentDate());
            int empId;
            if (ed != null) {
                empId = ed.getEmpId();
                if (logger.isDebugEnabled()) {
                    logger.debug("Found ID of " + empId);
                }
            } else {
                logger.error("The employee was not found");
                throw new Exception("Could not get employee ID");
            }
            stArray[i] = empId;
        }
        return stArray;
    }

}