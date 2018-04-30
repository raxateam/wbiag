package com.wbiag.app.bo.ejb.actions;

import java.util.Date;
import java.sql.*;

import com.wbiag.app.export.payroll.HighVolumePayrollExportThread;
import com.wbiag.app.export.payroll.PayrollExporter;
import com.workbrain.app.bo.ejb.actions.WorkflowUtil;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.EmployeeAccess;
import com.workbrain.app.workflow.AbstractActionProcess;
import com.workbrain.app.workflow.Action;
import com.workbrain.app.workflow.ActionResponse;
import com.workbrain.app.workflow.Branch;
import com.workbrain.app.workflow.WBObject;
import com.workbrain.app.workflow.WorkflowEngineException;
import com.workbrain.util.DateHelper;
import com.workbrain.util.Datetime;
import com.workbrain.util.StringHelper;
import org.apache.log4j.*;
import com.workbrain.sql.*;

/**
 * Title: HighVolumePayrollExportAction.java <br>
 * Description: TT49356 - High Volume Payroll Export; Thread class. See
 * Technical documentation off WBIAG website for more informaiton. <br>
 * 
 * Created: May 17, 2005
 * 
 * @author cleigh
 *         <p>
 *         Revision History <br>
 *         May 17, 2005 - File Created according to Technical documentation <br>
 *         <p>
 */
public class HighVolumePayrollExportAction extends AbstractActionProcess {

    private static Logger logger = Logger
            .getLogger(HighVolumePayrollExportAction.class);

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd hh:mm:ss";

    public static final String PARAM_PAYSYS_ID = "paysys_id";
    public static final String PARAM_PAYGRP_ID = "paygrp_ids";
    public static final String PARAM_EMP_ID = "emp_ids";
    public static final String PARAM_ALL_READY = "all_ready_paygrps";
    public static final String PARAM_ALL_READY_WHERE = "all_ready_where"; //TT49367
    public static final String PARAM_RESET = "reset";
    public static final String PARAM_CYCLE = "cycle";
    public static final String PARAM_USE_PP = "use_pay_period";
    public static final String PARAM_ADJ_DATE = "adjust_dates";
    public static final String PARAM_WRITE_FILE = "write_to_file";
    public static final String PARAM_MERGE_FILE = "merge_files";
    public static final String PARAM_WRITE_TABLE = "write_to_table";
    public static final String PARAM_START_DATE = "start_date";
    public static final String PARAM_END_DATE = "end_date";
    public static final String PARAM_CKUNAUTH = "chk_unauth";
    public static final String PARAM_LOOK_BACK = "look_back_days";
    public static final String PARAM_CONT_ON_UNAUTH = "continue_on_unauth";
    public static final String PARAM_TERM_ADD_DAYS = "term_add_days";
    public static final String PARAM_SUBMITTED_BY = "submitted_by";

    private DBConnection conn = null;

    //Queue Variables
    protected int lock;
    protected int queueId;
    protected String status = null;
    protected String error = null;
    protected String submittedBy = null;
    protected Date submitDate = null;
    protected Date queueStartTime = null;
    protected Date queueEndTime = null;

    //Export Variables
    private int[] payGrpIds = null;
    private String empIds = null;
    protected int paysysId;
    protected int lookBackDays = -14;
    protected int termAddDays = 7;
    protected boolean usePayPeriod = true;
    protected boolean allReadyPaygrps = false;
    protected boolean adjustDates = false;
    protected boolean writeToFile = false;
    protected boolean mergeFiles = false;
    protected boolean writeToTable = false;
    protected boolean chkUnauth = false;
    protected boolean contOnUnauth = true;
    protected String allReadyWhere = null;
    protected Date startDate = null;
    protected Date endDate = null;
    protected String cycle = PayrollExporter.CYCLE_REGULAR;

    //Extra feilds
    protected String flag1;
    protected String flag2;
    protected String flag3;
    protected String flag4;
    protected String flag5;
    protected String udf1 = null;
    protected String udf2 = null;
    protected String udf3 = null;
    protected String udf4 = null;
    protected String udf5 = null;
    protected String extraField = null;

    /*
     * (non-Javadoc)
     * 
     * @see com.workbrain.app.workflow.ActionProcess#processObject(com.workbrain.app.workflow.Action,
     *      com.workbrain.app.workflow.WBObject,
     *      com.workbrain.app.workflow.Branch[],
     *      com.workbrain.app.workflow.ActionResponse)
     */
    public ActionResponse processObject(Action arg0, WBObject object,
            Branch[] outputs, ActionResponse arg3)
            throws WorkflowEngineException {
        try {
            conn = this.getConnection();

            getParameters(object);

            getAdditionalParameters(object);

            if (validateData()) {
                addExportToQueue();
            } else {
                return WorkflowUtil.createActionResponse(outputs, "Failure",
                        "Export faild validation, not added to queue");
            }
        } catch (Exception t) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger
                        .error(
                                "com.workbrain.app.bo.ejb.actions.HighVolumePayrollExportAction.class",
                                t);
            }
            this.rollback(conn);
            throw new WorkflowEngineException(t);
        } finally {
            this.close(conn);
        }

        return WorkflowUtil.createActionResponse(outputs, "Success",
                "Export succesfuly added to Queue");
    }

    private void getParameters(WBObject object) throws WorkflowEngineException,
            SQLException, WorkflowEngineException {

        //Required Parameters
        if (!WorkflowUtil.fieldExists(object, PARAM_PAYSYS_ID)) {
            throw new WorkflowEngineException("paysys_id missing from form");
        } else if (WorkflowUtil.getFieldValueId(object, PARAM_PAYSYS_ID) == null) {
            paysysId = WorkflowUtil.getFieldValueAsInt(object, PARAM_PAYSYS_ID);
        } else {
            paysysId = Integer.valueOf(
                    WorkflowUtil.getFieldValueId(object, PARAM_PAYSYS_ID))
                    .intValue();
        }
        if (!WorkflowUtil.fieldExists(object, PARAM_SUBMITTED_BY)) {
            throw new WorkflowEngineException("submitted_by missing from form");
        } else {
            submittedBy = WorkflowUtil.getFieldValueAsString(object,
                    PARAM_SUBMITTED_BY);
        }

        //Semi Optional Parameters
        if (WorkflowUtil.fieldExists(object, PARAM_ALL_READY)) {
            allReadyPaygrps = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_ALL_READY);
            if (WorkflowUtil.fieldExists(object, PARAM_ALL_READY_WHERE)) {
                allReadyWhere = WorkflowUtil.getFieldValueAsString(object,
                        PARAM_ALL_READY_WHERE);
                if (logger.isDebugEnabled()) {
                    logger.debug("Setting allReadyWhere to " + allReadyWhere);
                }
            }
        }
        if (!allReadyPaygrps) {
            if (WorkflowUtil.fieldExists(object, PARAM_PAYGRP_ID)) {
                payGrpIds = detokenizeStringAsIntArray(WorkflowUtil
                        .getFieldValueId(object, PARAM_PAYGRP_ID), ",");
            } 
            if (WorkflowUtil.fieldExists(object, PARAM_EMP_ID)) {
                empIds = WorkflowUtil.getFieldValueId(object, PARAM_EMP_ID);
            }
            if (payGrpIds == null && empIds == null) {
                throw new WorkflowEngineException(new StringBuffer().append(
                        PARAM_PAYGRP_ID).append(" or ").append(PARAM_EMP_ID)
                        .append(
                                " must be provided on form if ").append(
                                PARAM_ALL_READY).append(" is false").toString());
            }
        }

        //Optional Params
        if (WorkflowUtil.fieldExists(object, PARAM_USE_PP)) {
            usePayPeriod = WorkflowUtil
                    .isCheckBoxSelected(object, PARAM_USE_PP);
        }
        if (!usePayPeriod) {
            if (WorkflowUtil.fieldExists(object, PARAM_START_DATE)) {
                if (WorkflowUtil.isFieldEmpty(object, PARAM_START_DATE)) {
                    throw new WorkflowEngineException(new StringBuffer()
                            .append("Use pay period dates is false but ")
                            .append(PARAM_END_DATE).append(" is empty. ")
                            .toString());
                }
                startDate = WorkflowUtil.getFieldValueAsDate(object,
                        PARAM_START_DATE);
            } else {
                throw new WorkflowEngineException(new StringBuffer().append(
                        "Use pay period dates is false but ").append(
                        PARAM_START_DATE).append(" not provided on form. ")
                        .toString());
            }
            if (WorkflowUtil.fieldExists(object, PARAM_END_DATE)) {
                if (WorkflowUtil.isFieldEmpty(object, PARAM_END_DATE)) {
                    throw new WorkflowEngineException(new StringBuffer()
                            .append("Use pay period dates is false but ")
                            .append(PARAM_END_DATE).append(" is empty. ")
                            .toString());
                }
                endDate = WorkflowUtil.getFieldValueAsDate(object,
                        PARAM_END_DATE);
            } else {
                throw new WorkflowEngineException(new StringBuffer().append(
                        "Use pay period dates is false but ").append(
                        PARAM_END_DATE).append(" not provided on form. ")
                        .toString());
            }
        }

        if (WorkflowUtil.fieldExists(object, PARAM_CYCLE)) {
            cycle = WorkflowUtil.getFieldValueAsString(object, PARAM_CYCLE);
        }

        if (WorkflowUtil.fieldExists(object, PARAM_ADJ_DATE)) {
            adjustDates = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_ADJ_DATE);
        }

        if (WorkflowUtil.fieldExists(object, PARAM_WRITE_FILE)) {
            writeToFile = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_WRITE_FILE);
        }

        if (WorkflowUtil.fieldExists(object, PARAM_MERGE_FILE)) {
            mergeFiles = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_MERGE_FILE);
        }

        if (WorkflowUtil.fieldExists(object, PARAM_WRITE_TABLE)) {
            writeToTable = WorkflowUtil.isCheckBoxSelected(object,
                    PARAM_WRITE_TABLE);
        }

        if (WorkflowUtil.fieldExists(object, PARAM_CKUNAUTH)) {
            chkUnauth = WorkflowUtil.isCheckBoxSelected(object, PARAM_CKUNAUTH);
        }

        if (WorkflowUtil.fieldExists(object, PARAM_TERM_ADD_DAYS)) {
            termAddDays = WorkflowUtil.getFieldValueAsInt(object,
                    PARAM_TERM_ADD_DAYS);
        }

        if (WorkflowUtil.fieldExists(object, PARAM_LOOK_BACK)) {
            lookBackDays = WorkflowUtil.getFieldValueAsInt(object,
                    PARAM_LOOK_BACK);
        }

    }

    private void addExportToQueue() throws WorkflowEngineException,
            SQLException {
        PreparedStatement ps = null;


        Datetime readyTime = new Datetime(System.currentTimeMillis()); //DateHelper.parseDate(dateString, TIMESTAMP_FORMAT);

        String seqIdNext = null;
        final DBServer dbs = DBServer.getServer(conn);
        
        if (dbs.isDB2()){
            seqIdNext = " next value for SEQ_WIEQ_ID ";
        }
        else if (dbs.isOracle()){
            seqIdNext = " SEQ_WIEQ_ID.nextval ";
        } else {
            throw new WorkflowEngineException("This DB is not yet supported. Contact Solution Center");
        }
        StringBuffer addExpSql = new StringBuffer();
        addExpSql
                .append("INSERT INTO ")
                .append(HighVolumePayrollExportThread.QUEUE_TABLE)
                .append(" (")
                .append(HighVolumePayrollExportThread.QUEUE_ID)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_STATUS)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_SUBMITTED_BY)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_SUBMIT_DATE)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_PAYGRP_IDS)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_EMP_IDS)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_PAYSYS_ID)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_START_DATE)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_END_DATE)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_USE_PAYPERIOD)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_ALL_READY_PAYGRPS)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_ALL_READY_WHERE)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_CYCLE)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_ADJUST_DATES)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_WRITE_TO_FILE)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_MERGE_FILES)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_WRITE_TO_TABLE)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_TERM_ADD_DAYS)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_LOOK_BACK_DAYS)
                .append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_CHK_UNAUTH).append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_UDF1).append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_UDF2).append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_UDF3).append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_UDF4).append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_UDF5).append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_FLAG1).append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_FLAG2).append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_FLAG3).append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_FLAG4).append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_FLAG5).append(", ")
                .append(HighVolumePayrollExportThread.QUEUE_EXTRA)
                .append(") VALUES (")
                .append(
                        "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) ");
        try {
            ps = conn.prepareStatement(addExpSql.toString());
            ps.setInt(1,conn.getDBSequence("SEQ_WIEQ_ID").getNextValue());
            if (allReadyPaygrps) {
                addExportToQueueAllReady(ps, readyTime);
            } else if (payGrpIds != null) {
                addExportToQueueByPayGrp(ps, readyTime);
            } else {
                addExportToQueueByEmpId(ps, readyTime);
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    private void addExportToQueueAllReady(PreparedStatement ps,
            Datetime readyTime) throws WorkflowEngineException, SQLException {
        try {
            int index = 2;

            ps.setString(index++,
                    HighVolumePayrollExportThread.QUEUE_STATUS_READY);
            ps.setString(index++, submittedBy);
            ps.setTimestamp(index++, readyTime);
            ps.setInt(index++, -1);
            ps.setString(index++, null);
            ps.setInt(index++, paysysId);
            ps.setTimestamp(index++, DateHelper.toTimestamp(startDate));
            ps.setTimestamp(index++, DateHelper.toTimestamp(endDate));
            ps.setString(index++, usePayPeriod ? "Y" : "N");
            ps.setString(index++, allReadyPaygrps ? "Y" : "N");
            ps.setString(index++, allReadyWhere);
            ps.setString(index++, cycle);
            ps.setString(index++, adjustDates ? "Y" : "N");
            ps.setString(index++, writeToFile ? "Y" : "N");
            ps.setString(index++, mergeFiles ? "Y" : "N");
            ps.setString(index++, writeToTable ? "Y" : "N");
            ps.setInt(index++, termAddDays);
            ps.setInt(index++, lookBackDays);
            ps.setString(index++, chkUnauth ? "Y" : "N");
            ps.setString(index++, udf1);
            ps.setString(index++, udf2);
            ps.setString(index++, udf3);
            ps.setString(index++, udf4);
            ps.setString(index++, udf5);
            ps.setString(index++, flag1);
            ps.setString(index++, flag2);
            ps.setString(index++, flag3);
            ps.setString(index++, flag4);
            ps.setString(index++, flag5);
            ps.setString(index++, extraField);

            ps.execute();

            conn.commit();

        } catch (SQLException e) {

            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Could not add Export to Queue", e);
            }
            conn.rollback();
            throw new WorkflowEngineException("Could not add Export to Queue",
                    e);

        }

    }

    private void addExportToQueueByPayGrp(PreparedStatement ps,
            Datetime readyTime) throws WorkflowEngineException, SQLException {
        try {
            int index = 2;

            for (int i = 0; i < payGrpIds.length; i++) {

                ps.setString(index++,
                        HighVolumePayrollExportThread.QUEUE_STATUS_READY);
                ps.setString(index++, submittedBy);
                ps.setTimestamp(index++, readyTime);
                ps.setInt(index++, payGrpIds[i]);
                ps.setString(index++, "");
                ps.setInt(index++, paysysId);
                ps.setTimestamp(index++, DateHelper.toTimestamp(startDate));
                ps.setTimestamp(index++, DateHelper.toTimestamp(endDate));
                ps.setString(index++, usePayPeriod ? "Y" : "N");
                ps.setString(index++, allReadyPaygrps ? "Y" : "N");
                ps.setString(index++, allReadyWhere);
                ps.setString(index++, cycle);
                ps.setString(index++, adjustDates ? "Y" : "N");
                ps.setString(index++, writeToFile ? "Y" : "N");
                ps.setString(index++, mergeFiles ? "Y" : "N");
                ps.setString(index++, writeToTable ? "Y" : "N");
                ps.setInt(index++, termAddDays);
                ps.setInt(index++, lookBackDays);
                ps.setString(index++, chkUnauth ? "Y" : "N");
                ps.setString(index++, udf1);
                ps.setString(index++, udf2);
                ps.setString(index++, udf3);
                ps.setString(index++, udf4);
                ps.setString(index++, udf5);
                ps.setString(index++, flag1);
                ps.setString(index++, flag2);
                ps.setString(index++, flag3);
                ps.setString(index++, flag4);
                ps.setString(index++, flag5);
                ps.setString(index++, extraField);

                ps.execute();

            }
            conn.commit();

        } catch (SQLException e) {

            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Could not add Export to Queue", e);
            }
            conn.rollback();
            throw new WorkflowEngineException("Could not add Export to Queue",
                    e);

        }

    }

    private void addExportToQueueByEmpId(PreparedStatement ps,
            Datetime readyTime) throws WorkflowEngineException, SQLException {
        try {
            int index = 2;

            ps.setString(index++,
                    HighVolumePayrollExportThread.QUEUE_STATUS_READY);
            ps.setString(index++, submittedBy);
            ps.setTimestamp(index++, readyTime);
            ps.setInt(index++, -1);
            ps.setString(index++, empIds);
            ps.setInt(index++, paysysId);
            ps.setTimestamp(index++, DateHelper.toTimestamp(startDate));
            ps.setTimestamp(index++, DateHelper.toTimestamp(endDate));
            ps.setString(index++, usePayPeriod ? "Y" : "N");
            ps.setString(index++, allReadyPaygrps ? "Y" : "N");
            ps.setString(index++, allReadyWhere);
            ps.setString(index++, cycle);
            ps.setString(index++, adjustDates ? "Y" : "N");
            ps.setString(index++, writeToFile ? "Y" : "N");
            ps.setString(index++, mergeFiles ? "Y" : "N");
            ps.setString(index++, writeToTable ? "Y" : "N");
            ps.setInt(index++, termAddDays);
            ps.setInt(index++, lookBackDays);
            ps.setString(index++, chkUnauth ? "Y" : "N");
            ps.setString(index++, udf1);
            ps.setString(index++, udf2);
            ps.setString(index++, udf3);
            ps.setString(index++, udf4);
            ps.setString(index++, udf5);
            ps.setString(index++, flag1);
            ps.setString(index++, flag2);
            ps.setString(index++, flag3);
            ps.setString(index++, flag4);
            ps.setString(index++, flag5);
            ps.setString(index++, extraField);

            ps.execute();

            conn.commit();

        } catch (SQLException e) {

            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Could not add Export to Queue", e);
            }
            conn.rollback();
            throw new WorkflowEngineException("Could not add Export to Queue",
                    e);

        }

    }

    protected boolean validateData() throws WorkflowEngineException {
        boolean valid = true;

        return valid;
    }

    /**
     * Method to be overwritten to add additional parameters to the export queue
     * 
     *  
     */
    protected void getAdditionalParameters(WBObject object) {

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
            String separator) throws SQLException, WorkflowEngineException {
        String[] st = StringHelper.detokenizeString(input, separator);
        EmployeeAccess ea = new EmployeeAccess(this.getConnection(),
                (CodeMapper) getConnection().getCodeMapper());
        if (st == null || st.length == 0) {
            return null;
        }
        int[] stArray = new int[st.length];
        for (int i = 0; i < st.length; i++) {
            int empId = ea.loadByName(st[i], DateHelper.getCurrentDate())
                    .getEmpId();
            stArray[i] = empId;
        }
        return stArray;
    }

}
