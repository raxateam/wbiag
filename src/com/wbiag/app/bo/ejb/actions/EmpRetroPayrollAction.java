package com.wbiag.app.bo.ejb.actions;

import java.util.*;

import com.workbrain.app.bo.ejb.actions.*;
import com.workbrain.app.workflow.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

import com.wbiag.app.export.payroll.PayrollExporter;

/*
 * Export Payroll Action
 *
 */
public class EmpRetroPayrollAction
    extends AbstractActionProcess {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(EmpRetroPayrollAction.class);

    private static final String MODE = PayrollExporter.MODE_REGULAR;

    private static final String PARAM_PAYSYS_ID = "paysys_id";
    private static final String PARAM_EMP_ID    = "emp_ids";
    private static final String PARAM_USE_PP	= "use_pay_period";
    private static final String PARAM_WRITE_FILE = "write_to_file";
    private static final String PARAM_MERGE_FILE = "merge_files";
    private static final String PARAM_WRITE_TABLE = "write_to_table";
    private static final String PARAM_START_DATE = "start_date";
    
    private static final String PARAM_ACTION_CKUNAUTH = "CHK_UNAUTH";
    
    public ActionResponse processObject( Action data, WBObject object,
                                        Branch[] outputs,
                                        ActionResponse previous) throws
        WorkflowEngineException {

        if (logger.isInfoEnabled()) {
            logger.info("EmpRetroPayrollAction.processObject");
        }
        DBConnection conn = null;
        PayrollExporter payExp = null;
        int[] empIdsA = null;

        try {
            conn = this.getConnection();
            empIdsA = detokenizeStringAsIntArray(
                WorkflowUtil.getFieldValueId(object, PARAM_EMP_ID),
                ",");

            if ( (empIdsA != null) && (empIdsA.length != 0)) {

                int petID;
                boolean usesPayPeriod;
                boolean writeToFile;
                boolean mergeFiles;
                boolean writeToTable;
                boolean chkUnauth;

                Date dDate = null;

                if (WorkflowUtil.getFieldValueId(object,
                		PARAM_PAYSYS_ID) == null) {
                    petID = WorkflowUtil.getFieldValueAsInt(object,
                    		PARAM_PAYSYS_ID);
                }
                else {
                    petID = Integer.valueOf(WorkflowUtil.getFieldValueId(object,
                    		PARAM_PAYSYS_ID)).intValue();
                }
                usesPayPeriod = WorkflowUtil.isCheckBoxSelected(object,
                		PARAM_USE_PP);
                writeToFile = WorkflowUtil.isCheckBoxSelected(object,
                		PARAM_WRITE_FILE);
                mergeFiles = WorkflowUtil.isCheckBoxSelected(object,
                		PARAM_MERGE_FILE);
                writeToTable = WorkflowUtil.isCheckBoxSelected(object,
                		PARAM_WRITE_TABLE);

                if (!usesPayPeriod) {
                    dDate = WorkflowUtil.getFieldValueAsDate(object,
                    		PARAM_START_DATE);
                }

                payExp = new PayrollExporter(conn, MODE);
                payExp.setEmpIds(empIdsA);
                payExp.setCycle(PayrollExporter.CYCLE_RETRO);
                if (dDate == null){
                    payExp.useDefaultDates();
                }
                else {
                    payExp.setDatesRelitive(dDate);
                }

                if (data.propertyExists(PARAM_ACTION_CKUNAUTH)) {
                    final String chkUnauthString = WorkflowUtil.
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

                ArrayList badPG = null;
                if (chkUnauth) {
                    //Check Readiness again and Run Task
                    badPG = payExp.checkUnauth();
                    if (!badPG.isEmpty()) {
                        String comment =
                            "Error exporting records, some records not authorized. Check Authorization Report \n";
                        comment += "The following pay groups failed " +
                            badPG.toString();
                        return WorkflowUtil.createActionResponse(outputs,
                            "Failure", comment);
                    }
                }

                payExp.setPayExpTsk(petID);
                payExp.setWriteToFile(writeToFile);
                payExp.setWriteToTable(writeToTable);
                payExp.setMergeFiles(mergeFiles);
                payExp.process();
                this.commit(conn);
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
        String comment = "Export has completed successfully";
        return WorkflowUtil.createActionResponse(outputs, "Success", comment);
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
