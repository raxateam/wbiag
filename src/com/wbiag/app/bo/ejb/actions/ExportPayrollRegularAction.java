package com.wbiag.app.bo.ejb.actions;

import java.util.*;

import com.workbrain.app.export.payroll.*;
import com.workbrain.app.bo.ejb.actions.*;
import com.workbrain.app.workflow.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Clob;
/*
 * Export Payroll Regular Action
 *
 */
public class ExportPayrollRegularAction
    extends AbstractActionProcess {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(ExportPayrollRegularAction.class);


    private static final String PARAM_PAYSYS_ID = "paysys_id";
    private static final String PARAM_PAYGRP_ID = "paygrp_ids";
    private static final String PARAM_USE_PP	= "use_pay_period";
    private static final String PARAM_ADJ_DATE	= "adjust_dates";
    private static final String PARAM_WRITE_FILE = "write_to_file";
    private static final String PARAM_MERGE_FILE = "merge_files";
    private static final String PARAM_WRITE_TABLE = "write_to_table";
    private static final String PARAM_START_DATE = "start_date";
    private static final String PARAM_END_DATE = "end_date";
    private static final String PARAM_WBINT_TYPE = "dbWbint";
    private static final String WBITYP_PAYROLL_EXPORT = "PAYROLL EXPORT";

    private int[] paygrpIdsA = null;
    private int petID;
    private boolean usesPayPeriod;
    private boolean adjustDates;
    private boolean writeToFile;
    private boolean mergeFiles;
    private boolean writeToTable;
    private String wbintTypName;

    private Date dStartDate = null;
    private Date dEndDate = null;

    public ActionResponse processObject(Action data, WBObject object,
                                        Branch[] outputs,
                                        ActionResponse previous) throws
        WorkflowEngineException {
        if (logger.isInfoEnabled()) {    logger.info("ExportPayrollRegularAction.processObject");  }

        DBConnection conn = null;
        String results = null;

        try {
            conn = this.getConnection();
            getParams(object, data);


            PayrollExportProcessor payExp = new PayrollExportProcessor();
            if (logger.isDebugEnabled()) logger.debug("Running export for paygrps :" + paygrpIdsA);
            payExp.setFromDate(dStartDate);
            payExp.setToDate(dEndDate);
            payExp.setPayGrpIds(paygrpIdsA);
            payExp.setWbitypeName(wbintTypName);
            payExp.setPetId(petID);
            payExp.setWriteToFile(writeToFile);
            payExp.setWriteToTable(writeToTable);
            payExp.setAdjustDates(adjustDates);
            payExp.setMergeFiles(mergeFiles);
            // petXML, filename and directory
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                final String petSql = "SELECT * FROM payroll_export_tsk WHERE pet_id = ?";
                ps = conn.prepareStatement(petSql);
                ps.setInt(1, petID);
                rs = ps.executeQuery();
                if (rs.next()) {
                    final Clob clob = rs.getClob("pet_xml");
                    final String xml = clob.getSubString(1L, ((int) clob.length()));
                    payExp.setPetXml(xml);
                    String filePath = rs.getString("PET_OUT_FILE_PATH");
                    String fileName = rs.getString("PET_OUT_FILE_MASK");
                    String fileExt = rs.getString("PET_OUT_FILE_EXT");
                    String petName = rs.getString("PET_NAME");
                    payExp.setPetName(petName);
                    payExp.setPetPath(filePath);
                    payExp.setPetMask(fileName);
                    payExp.setPetExt(fileExt);
                } else {
                    throw new RuntimeException("Payroll export task id not found : " + petID);
                }
            } finally {
                if ((ps != null)) {
                    ps.close();
                }
                if ((rs != null)) {
                    rs.close();
                }
            }

            payExp.process(conn, null);
            this.commit(conn);
            results = payExp.getResults().toString();

        }
        catch (Exception t) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("com.workbrain.app.bo.ejb.actions.ExportPayrollRegularAction.class",        t);         }
            this.rollback(conn);
            throw new WorkflowEngineException(t);
        }
        finally {
            this.close(conn);
        }

        return WorkflowUtil.createActionResponse(outputs, "Success", results);
    }

    private void getParams(WBObject object, Action data) throws WorkflowEngineException, Exception{
        paygrpIdsA = StringHelper.detokenizeStringAsIntArray(
                    WorkflowUtil.getFieldValueId(object, PARAM_PAYGRP_ID),
                    ",", true);

        if (paygrpIdsA ==  null || paygrpIdsA.length == 0) {
            throw new WorkflowEngineException("Paygroups can't be null");
        }
        if (WorkflowUtil.getFieldValueId(object,
                PARAM_PAYSYS_ID) == null) {
            petID = WorkflowUtil.getFieldValueAsInt(object, PARAM_PAYSYS_ID);
        }
        else {
            petID = Integer.valueOf(WorkflowUtil.getFieldValueId(object,
                    PARAM_PAYSYS_ID)).intValue();
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
        if (WorkflowUtil.fieldExists(object, PARAM_WBINT_TYPE)) {
            wbintTypName = WorkflowUtil.getFieldValueAsString(object,
                    PARAM_WBINT_TYPE);
        }
        else {
            wbintTypName = "PAYROLL EXPORT";
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


}
