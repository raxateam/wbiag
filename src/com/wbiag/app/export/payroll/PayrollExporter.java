package com.wbiag.app.export.payroll;

import java.sql.*;
import java.lang.String;
import java.util.Date;
import java.util.*;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.model.PayGroupData;
import com.workbrain.app.ta.db.PayGroupAccess;
import com.workbrain.tool.overrides.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.export.payroll.*;

import com.wbiag.tool.overrides.WBIAGInsertWorkSummaryBulkOverride;
import com.wbiag.app.export.payroll.PayrollReadiness;
import com.wbiag.app.ta.db.CustomerRegistryAccess;
import com.wbiag.app.ta.model.CustomerRegistryData;

/**
 * Title: PayrollExporter.java <br>
 * Description: <br>
 * 
 * Created: Aug 23, 2005
 * @author cleigh
 * <p>
 * Revision History <br>
 * Aug 23, 2005 -  <br>
 * <p>
 * */
/**
 * Title: PayrollExporter.java <br>
 * Description: <br>
 * 
 * Created: Aug 23, 2005
 * 
 * @author cleigh
 *         <p>
 *         Revision History <br>
 *         Aug 23, 2005 - <br>
 *         <p>
 */
public class PayrollExporter {
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger
            .getLogger(PayrollExporter.class);

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd 00:00:00";
    private static final String TIMESTAMP_FORMAT_FULL = "yyyy-MM-dd hh24:mi:ss";
    private static final String ORACLE_OBD_FORMAT = "'MM/dd/yyyy'";
    private static final String ERR_MSG_PG_DATES = " [ERROR:] All pay groups must have the same start and end dates";
    private static final String ERR_MSG_PG_IDS = "Pay group ids must be set";

    private String ON_BOARD_FIELD = "EMP_VAL1";
    private String ON_BOARD_EX_FIELD = "EMP_VAL2";
    private String PAID_WRKS_FLAG = "WRKS_FLAG1";
    private String PAID_WRKSA_FLAG = "WRKSA_FLAG1";
    private String PAID_WRKDA_FLAG = "wrkda_flag1";
    private String PAID_WRKD_FLAG = "wrkd_flag1";
    private String PAYGRP_STATUS_FLAG = "PAYGRP_FLAG1";
    private String EMP_ON_BOARD_FLAG = "EMP_FLAG1";
    private String ENABLE_ON_BOARD = "TRUE";
    private String HANDS_OFF_UDF = "PAYGRP_UDF2";

    private static final String REG_PATH = "system/customer/";
    private static final String REG_ON_BOARD_FIELD = "ON_BOARD_FIELD";
    private static final String REG_ON_BOARD_EX_FIELD = "ON_BOARD_EX_FIELD";
    private static final String REG_PAID_WRKS_FLAG = "PAID_WRKS_FLAG";
    private static final String REG_PAID_WRKSA_FLAG = "PAID_WRKSA_FLAG";
    private static final String REG_PAID_WRKDA_FLAG = "PAID_WRKDA_FLAG";
    private static final String REG_PAID_WRKD_FLAG = "PAID_WRKD_FLAG";
    private static final String REG_PAYGRP_STATUS_FLAG = "PAYGRP_STATUS_FLAG";
    private static final String REG_EMP_ON_BOARD_FLAG = "EMP_ON_BOARD_FLAG";
    private static final String REG_ENABLE_ON_BOARD = "ENABLE_ON_BOARD";
    private static final String REG_HANDS_OFF_UDF = "HANDS_OFF_UDF";

    public static final String MARK_IN_PROGRESS = "I";
    public static final String MARK_COMPLETE = "C";
    public static final String MARK_ERROR = "E";
    public static final String MARK_READY = "Y";
    public static final String MARK_NOT_READY = "N";
    public static final String MARK_PAID = "Y";
    public static final String MARK_UNPAID = "N";

    public static final String MODE_BASIC = "Basic";
    public static final String MODE_REGULAR = "Regular";

    public static final String CYCLE_REGULAR = "Regular";
    public static final String CYCLE_ON_BOARD = "On Board";
    public static final String CYCLE_TERM = "Terminated";
    public static final String CYCLE_RETRO = "Retro";
    public static final String CYCLE_RESET = "Reset";
    public static final String CYCLE_UNPAY = "Unpay";

    private static final int REGULAR = 0;
    private static final int ON_BOARD = 1;
    private static final int TERM = 2;
    private static final int RETRO = 3;
    private static final int RESET = 4;
    private static final int UNPAY = 5;

    private static final String WORKBRAIN = "WORKBRAIN";

    private int cycleId;
    private int[] payGrpIds;
    private int[] empIds;
    private int petId;
    private int lookBackDays = -14;
    private int termAddDays = 7;
    private boolean writeToFile;
    private boolean writeToTable;
    private boolean mergeFiles;
    private boolean adjustDates;
    private boolean basicMode;
    private boolean doExport = true;
    private boolean enableOnBoard = true;
    private boolean unlockRecords = false;

    private String whereCurrent;
    private String whereAdjust;
    private String allReadyWhere = null;
    private String whereMinHire = null;
    private String whereTerm = null;
    private String message = null;
    private String numRecExp = null;
    private Date startDate;
    private Date endDate;
    private Date pgStartDate;
    private Date pgEndDate;
    private DBConnection conn;
    private PayrollReadiness pr = null;

    // String Literals, used for increased efficiency
    private static final String AND_EMP_ID = " and emp_id in (";
    private static final String NOT_Y = " <> 'Y' ";
    private static final String QUOTE_N = "'N'";
    private static final String DATE_FUN = " date(";
    private static final String SANDS = " AND ";
    private static final String TO_DATE_FUN = " to_date( ";
    private static final String BETWEEN = " between ";

    /**
     * Get the Exporter message
     * 
     * @return String
     */
    final public String getMessage() {
        return message;
    }

    /**
     * Gets the export Start Date
     * 
     * @return Date
     */
    final public Date getStartDate() {
        return startDate;
    }

    /**
     * Get the export End Date
     * 
     * @return Date
     */
    final public Date getEndDate() {
        return endDate;
    }

    final public int[] getPayGroups() {
        return payGrpIds;
    }

    /**
     * Returns True if all Pay Groups are marked ready, if a pay group is not
     * ready then it is removed from the list of pay groups
     * 
     * 
     * @return boolean
     */
    public boolean checkPaygrpsReady() throws Exception {
        boolean result = true;
        if ((pr == null)) {
            pr = new PayrollReadiness(conn);
        }
        switch (cycleId) {
        case REGULAR: {
            result = pr.checkPaygrpsReady(payGrpIds);
            if ((!result)) {
                message += " Nothing will be exported for non ready pay groups. ";
            }
            break;
        }
        default: {
            throw new Exception("checkPaygrpsReady not supported for cycle Id "
                    + cycleId);
        }
        }
        return result;
    }

    /**
     * Set the look back days for employee on boarding. Look Back Days will
     * always be < 0
     * 
     * @param days
     *            int
     */
    public void setLookBackDays(int days) {
        if ((days > 0)) {
            lookBackDays = -days;
        } else {
            lookBackDays = days;
        }
    }

    /**
     * Set the Term Add Days back days for employee on teminination Export. Term
     * add days will always be > 0.
     * 
     * @param days
     *            int
     */

    /**
     * Determins if the hands off date needs to be changed after export.
     * 
     * @param days
     * 
     */
    public void setTermAddDays(int days) {
        if ((days < 0)) {
            termAddDays = -days;
        } else {
            termAddDays = days;
        }
    }

    /**
     * 
     * @param b
     * 
     */
    public void setUnlockRecords(boolean b) {
        unlockRecords = b;
    }

    /**
     * Checks all records to be exported for unauthorized records and returns an
     * array of the failed paygroup/employees. If removeFailed is true then all
     * failing pay groups are removed.
     * 
     * @param removeFailed
     * @return
     * 
     */
    public ArrayList checkUnauth(boolean removeFailed) throws Exception {
        ArrayList failedPaygrps = new ArrayList();

        if (logger.isDebugEnabled()) {
            logger.debug("PayrollExporter.checkUnauth begin");
        }
        if ((!(payGrpIds.length == 0))) {
            if (logger.isDebugEnabled()) {
                logger.debug("Checking pay groups for unauthorized records.");
            }
            boolean pgFailed;
            for (int i = 0; i < payGrpIds.length; i++) {
                pgFailed = false;
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuffer().append(
                            "Now checking pay group ").append(payGrpIds[i])
                            .toString()); // $IGN_Avoid_object_instantiation_in_loops$
                }
                if ((basicMode || (cycleId == REGULAR))) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Regular export mode");
                    }
                    if ((!pr.checkGivenDatesUnauth(payGrpIds[i],
                            this.startDate, this.endDate))) {
                        pgFailed = true;
                    }
                } else if ((cycleId == ON_BOARD)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("On board export mode");
                    }
                    if ((this.empIds != null)) {
                        failedPaygrps = checkOnBoardUnAuthByEmployee();
                        break;
                    } else {
                        if ((!pr.checkNewEmpUnauth(payGrpIds[i],
                                this.lookBackDays))) {
                            pgFailed = true;
                        }
                    }
                } else if ((cycleId == TERM)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Term export mode");
                    }
                    if ((this.empIds != null)) {
                        failedPaygrps = checkTermUnAuthByEmployee();
                        break;
                    } else {
                        if ((!pr.checkTermUnauth(payGrpIds[i]))) {
                            pgFailed = true;
                        }
                    }
                } else if ((cycleId == RETRO)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Retro export mode");
                    }
                    if ((!pr.checkRetroUnauth(payGrpIds[i], this.empIds,
                            pgStartDate, pgEndDate))) {
                        pgFailed = true;
                    }
                } else {
                    if (logger.isEnabledFor(Level.ERROR)) {
                        logger
                                .error(new StringBuffer()
                                        .append(
                                                "Check unauthorization not supported for cycle id")
                                        .append(cycleId).toString()); // $IGN_Avoid_object_instantiation_in_loops$
                    }
                    throw new Exception("Check unauth not supported for cycle "); // $IGN_Avoid_object_instantiation_in_loops$
                }
                if (pgFailed) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(new StringBuffer().append(
                                "Found failing pay group ")
                                .append(payGrpIds[i]).toString()); // $IGN_Avoid_object_instantiation_in_loops$
                    }
                    failedPaygrps.add(Integer.toString(payGrpIds[i]));
                }
            }
        }
        if (removeFailed) {
            if ((empIds != null)) {
                if (logger.isEnabledFor(Level.ERROR)) {
                    logger
                            .error("Remove failed not supported for export by employee");
                }
                throw new Exception(
                        "Remove failed not supported for export by employee");
            }
            removePaygroups(failedPaygrps);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("PayrollExporter.checkUnauth end");
        }
        return failedPaygrps;
    }

    /**
     * Returns an array of all the failed paygroups.
     * 
     * 
     * @return boolean
     */
    public ArrayList checkUnauth() throws Exception {

        final ArrayList badPaygrps = new ArrayList();

        if ((payGrpIds.length == 0)) {
            return badPaygrps;
        }

        if ((pr == null)) {
            pr = new PayrollReadiness(conn);
        }

        if (basicMode) {

            if ((((pgStartDate == null)) || ((pgEndDate == null)))) {
                message += " [ERROR:] Dates are invalid or not set";
                throw new Exception("Dates are invalid or not set");
            }
            for (int i = 0; i < payGrpIds.length; i++) {
                if ((!pr.checkGivenDatesUnauth(payGrpIds[i], this.startDate,
                        this.endDate))) {
                    badPaygrps.add(Integer.toString(payGrpIds[i]));
                }
            }
            return badPaygrps;
        } else {
            switch (cycleId) {
            case REGULAR: {
                if ((((pgStartDate == null)) || ((pgEndDate == null)))) {
                    message += " [ERROR:] Dates are invalid or not set";
                    throw new Exception("Dates are invalid or not set");
                }
                for (int i = 0; i < payGrpIds.length; i++) {
                    if ((!pr.checkGivenDatesUnauth(payGrpIds[i],
                            this.startDate, this.endDate))) {
                        badPaygrps.add(Integer.toString(payGrpIds[i]));
                    }
                }
                return badPaygrps;
            }
            case ON_BOARD: {
                if ((this.empIds != null)) {
                    return checkOnBoardUnAuthByEmployee();
                } else {
                    for (int i = 0; i < payGrpIds.length; i++) {
                        if ((!pr.checkNewEmpUnauth(payGrpIds[i],
                                this.lookBackDays))) {
                            badPaygrps.add(Integer.toString(payGrpIds[i]));
                        }
                    }
                    return badPaygrps;
                }
            }
            case TERM: {
                if ((this.empIds != null)) {
                    return checkTermUnAuthByEmployee();
                } else {
                    for (int i = 0; i < payGrpIds.length; i++) {
                        if ((!pr.checkTermUnauth(payGrpIds[i]))) {
                            badPaygrps.add(Integer.toString(payGrpIds[i]));
                        }
                    }
                    return badPaygrps;
                }
            }
            case RETRO: {
                for (int i = 0; i < payGrpIds.length; i++) {
                    if ((!pr.checkRetroUnauth(payGrpIds[i], this.empIds,
                            pgStartDate, pgEndDate))) {
                        badPaygrps.add(Integer.toString(payGrpIds[i]));
                    }
                }
                return badPaygrps;
            }
            default: {
                throw new Exception("Check unauth not supported for cycle ");
            }
            }
        }

    }

    /**
     * Sets the Payroll Export Task Id
     * 
     * @param id
     *            int
     */
    public void setPayExpTsk(int id) {
        petId = id;
    }

    /**
     * Adds the where condition when selecting all ready pay groups. This must
     * be set before selecting all pay groups w/ pg[0] == -99
     * 
     * @param v
     * 
     */
    public void setAllReadyWhere(String v) {
        if (logger.isDebugEnabled()) {
            logger.debug("Setting allReadyWhere : " + v);
        }
        allReadyWhere = v;
    }

    /**
     * Sets the pay groups ID for export. Not be be called if exporting by
     * employee ID. If pgIds[0] = -99 then all ready pay groups are used.
     * 
     * @param pgIds
     *            int[]
     * @throws Exception
     */
    public void setPaygrpIds(int[] pgIds) throws Exception {
        if ((empIds == null)) {
            if ((pgIds.length == 0)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No Pay groups for export");
                }
                return;
            }
            if ((pgIds[0] == -99)) {

                if ((pr == null)) {
                    pr = new PayrollReadiness(conn);
                }
                if (cycleId == REGULAR){
                    this.payGrpIds = pr.getReadyPaygrps(allReadyWhere);
                } else if (cycleId == ON_BOARD){
                    this.payGrpIds = pr.getReadyPaygrpsWhere(allReadyWhere);
                    this.useDefaultDates();
                } else {
                    throw new Exception(
                    "All ready pay groups not supported by this cycle. ");
                }
            } else {
                this.payGrpIds = pgIds;
            }
            if ((pgIds.length == 0)) {
                if (logger.isDebugEnabled()) {
                    logger.debug(" No ready pay groups selected for export. ");
                }
            } else if (logger.isDebugEnabled()) {
                final StringBuffer list = new StringBuffer();
                for (int i = 0; i < payGrpIds.length; i++) {
                    (list.append(payGrpIds[i])).append(" ");
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Set Pay group IDs to " + list);
                }
            }
        } else {
            throw new Exception(
                    "Can not set pay group ids when emp ids are used for export");
        }
    }

    /**
     * Setting the employee Ids for export
     * 
     * @param eIds
     *            int[]
     */
    public void setEmpIds(int[] eIds) throws SQLException, Exception {
        this.empIds = eIds;
        if (logger.isDebugEnabled()) {
            final StringBuffer list = new StringBuffer();
            for (int i = 0; i < empIds.length; i++) {
                (list.append(empIds[i])).append(" ");
            }
            logger.debug("Set Emp IDs to " + list);
        }
        this.payGrpIds = this.getPayGroupIdsFromEmpIds(eIds, this.startDate);
        if (logger.isDebugEnabled()) {
            final StringBuffer list = new StringBuffer();
            for (int i = 0; i < payGrpIds.length; i++) {
                (list.append(payGrpIds[i])).append(" ");
            }
            logger.debug("Set Pay group IDs to " + list);
        }
    }

    /**
     * Sets WriteToFile
     * 
     * @param wtf
     *            boolean
     */
    public void setWriteToFile(boolean wtf) {
        writeToFile = wtf;
    }

    /**
     * Sets WriteToTable
     * 
     * @param wtt
     *            boolean
     */
    public void setWriteToTable(boolean wtt) {
        writeToTable = wtt;
    }

    /**
     * Sets MergeFiles
     * 
     * @param mf
     *            boolean
     */
    public void setMergeFiles(boolean mf) {
        mergeFiles = mf;
    }

    /**
     * Sets AdjustDates
     * 
     * @param ad
     *            boolean
     */
    public void setAdjustDates(boolean ad) {
        adjustDates = ad;
    }

    /**
     * Resets Paygroup Readiness feild
     */
    public void resetReadiness() throws java.sql.SQLException {
        if ((pr == null)) {
            pr = new PayrollReadiness(conn);
        }
        pr.resetReadiness();
        message += " Pay Group Readiness Reset ";
        if (logger.isInfoEnabled()) {
            logger.info(message);
        }
    }

    /**
     * Processes the Payroll Export. For regular or Basic cycle records the pay
     * group is marked as inprogress and the pay group is exported. For On Board
     * exports the employee is marked as paid on completion. For Term Exports
     * and Retro exports the records are marked as paid.
     * 
     * If there is an error the pay groups is marked error.
     * 
     * @throws Exception
     * @return boolean
     */
    public boolean process() throws Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("PayrollExportProcessor.process begin");
        }
        if ((this.exporterReady() && ((cycleId != RESET)))) {
            if ((basicMode || ((cycleId == REGULAR)))) {
                this.markPayGroups(MARK_IN_PROGRESS);
            }
            try {
                if (this.doExport) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Running Payroll Export");
                    }
                    this.runPayroll();
                }

                if ((basicMode || ((cycleId == REGULAR)))) {
                    this.markPayGroups(MARK_COMPLETE);
                    message = " Export has completed successfully Records exported (before grouping): "
                            + numRecExp;
                    if (logger.isInfoEnabled()) {
                        logger.info(message);
                    }
                } else if ((cycleId == ON_BOARD)) {
                    this.markEmployees(MARK_PAID);
                    message = "On Board Export has completed successfully Records exported (before grouping): "
                            + numRecExp;
                    if (logger.isInfoEnabled()) {
                        logger.info(message);
                    }
                } else if ((cycleId == TERM)) {
                    this.markRecords(MARK_PAID);
                    message = "Term Export has completed successfully Records exported (before grouping): "
                            + numRecExp;
                    if (logger.isInfoEnabled()) {
                        logger.info(message);
                    }
                } else {
                    this.markRecords(MARK_PAID);
                    message = "Off Cycle Export has completed successfully Records exported (before grouping): "
                            + numRecExp;
                    if (logger.isInfoEnabled()) {
                        logger.info(message);
                    }
                }
            }

            catch (Exception e) {
                if ((cycleId == REGULAR)) {
                    this.markPayGroups(MARK_ERROR);
                }
                logger.error("There was an error exporting records", e);
                message += "There was an error exporting records: "
                        + e.toString();
                throw e;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(message);
            logger.debug("PayrollExportProcessor.process end");
        }
        return true;
    }

    /**
     * Allows you to use all the features of PayrollExporter with out actully
     * processing the export file. DoExport default is true.
     * 
     * @param relitiveDate
     *            Date
     * @throws Exception
     */
    public void setDoExport(boolean exp) {
        this.doExport = exp;
    }

    /**
     * Sets the export dates based on the pay period containing relitiveDate
     * 
     * @param relitiveDate
     *            Date
     * @throws Exception
     */
    public void setDatesRelitive(Date relitiveDate) throws Exception {
        if (basicMode) {
            startDate = getPayPeriodStartDate();
            endDate = getPayPeriodEndDate();
            if (logger.isInfoEnabled()) {
                logger.info(" basic mode setDatesRelitive :"
                        + endDate.toString());
            }
        } else {
            final Date sd = GetRelativePayPeriodStartDate(relitiveDate);
            final Date ed = GetRelativePayPeriodEndDate(relitiveDate);
            if (logger.isInfoEnabled()) {
                logger.info(" setDatesRelitive :" + ed.toString());
            }
            setDates(sd, ed);
        }
    }

    /**
     * Sets the export Dates
     * 
     * @param sd
     *            Date
     * @param ed
     *            Date
     * @throws Exception
     */
    public void setDates(Date sd, Date ed) throws Exception {
        if (basicMode) {
            this.startDate = sd;
            this.endDate = ed;
        } else {
            switch (cycleId) {
            case REGULAR: {
                startDate = sd;
                endDate = ed;
                pgStartDate = startDate;
                pgEndDate = endDate;
                break;
            }
            case RETRO: {
                // All Retros for given pay groups accross given dates
                startDate = sd;
                endDate = ed;
                pgStartDate = startDate;
                pgEndDate = endDate;
                break;
            }
            case UNPAY: {
                startDate = sd;
                endDate = ed;
                pgStartDate = startDate;
                pgEndDate = endDate;
                break;
            }
            default: {
                throw new Exception("Set dates is not supported for this cycle");
            }
            }
            this.setWhere();
        }
    }

    /**
     * Sets the export dates using default values (ie Current Pay period,
     * lookback days ect.)
     * 
     * @throws Exception
     */
    public void useDefaultDates() throws Exception {

        if ((payGrpIds == null)) {
            throw new Exception(ERR_MSG_PG_IDS);
        } else if (((this.payGrpIds.length == 0) && (cycleId != RESET))) {
            if (logger.isDebugEnabled()) {
                logger
                        .debug(" Can not get export dates, no pay groups selected. ");
            }
        } else {
            if (basicMode) {
                switch (cycleId) {
                case REGULAR: {
                    startDate = getPayPeriodStartDate();
                    endDate = getPayPeriodEndDate();
                    if (logger.isDebugEnabled()) {
                        logger.debug("regular =" + endDate.toString());
                    }
                    this.setWhere();
                }
                case RESET: {
                    break;
                }
                default: {
                    throw new Exception(
                            "Cycle not set / not supported in Basic mode");
                }
                }
            } else {
                // set default Dates
                switch (cycleId) {
                case REGULAR: {

                    startDate = getPayPeriodStartDate();
                    endDate = getPayPeriodEndDate();
                    if (logger.isDebugEnabled()) {
                        logger
                                .debug("else regular ="
                                        + this.endDate.toString());
                    }
                    this.setWhere();
                    pgStartDate = startDate;
                    pgEndDate = endDate;

                    break;
                }
                case ON_BOARD: {

                    pgStartDate = DateHelper.addDays(getPayPeriodStartDate(),
                            lookBackDays);
                    pgEndDate = getPayPeriodEndDate();
                    this.setWhere();
                    this.startDate = getMinHireDate();
                    this.endDate = getLastPayPeriodEndDate();
                    if (logger.isDebugEnabled()) {
                        logger.debug("on board this.endDate ="
                                + this.endDate.toString());
                    }
                    break;
                }
                case TERM: {

                    pgStartDate = getPayPeriodStartDate();
                    pgEndDate = DateHelper.addDays(getPayPeriodEndDate(),
                            termAddDays);
                    this.setWhere();
                    this.startDate = pgStartDate;
                    this.endDate = getTermDate();
                    if (logger.isDebugEnabled()) {
                        logger.debug("term =" + this.endDate.toString());
                    }
                    break;
                }
                case RETRO: {
                    // All Retro for that pay group for current pay period

                    startDate = getPayPeriodStartDate();
                    endDate = getPayPeriodEndDate();
                    if (logger.isDebugEnabled()) {
                        logger.debug("retro =" + this.endDate.toString());
                    }
                    this.setWhere();
                    pgStartDate = startDate;
                    pgEndDate = endDate;
                    break;
                }
                case RESET: {
                    // Nothing to do.
                    break;
                }
                case UNPAY: {
                    startDate = getPayPeriodStartDate();
                    endDate = getPayPeriodEndDate();
                    this.setWhere();
                    pgStartDate = startDate;
                    pgEndDate = endDate;

                    break;
                }
                }
            }
        }
    }

    /**
     * Sets the cycle type
     * 
     * @param v
     *            String
     */
    public void setCycle(String v) throws Exception {
        if (v.equalsIgnoreCase(CYCLE_REGULAR)) {
            cycleId = REGULAR;
        } else if (v.equalsIgnoreCase(CYCLE_ON_BOARD)) {
            if ((!enableOnBoard)) {
                throw new Exception(
                        "Invalid Cycle, On boarding not enabled. Check Registry settings");
            }
            cycleId = ON_BOARD;
            if ((payGrpIds != null)) {
                this.useDefaultDates();
            }
        } else if (v.equalsIgnoreCase(CYCLE_TERM)) {
            cycleId = TERM;
            this.useDefaultDates();
        } else if (v.equalsIgnoreCase(CYCLE_RETRO)) {
            cycleId = RETRO;
        } else if (v.equalsIgnoreCase(CYCLE_RESET)) {
            cycleId = RESET;
        } else if (CYCLE_UNPAY.equalsIgnoreCase(v)) {
            cycleId = UNPAY;
        }

    }

    public void unpayRecords() throws Exception {

        if ((cycleId == UNPAY)) {
            markRecords(MARK_UNPAID);
        } else {
            throw new Exception("Function not supported for this cycle");
        }
    }

    /**
     * Constructor for PayrollExported, mode is ether basic or regular. A null
     * mode is assumed regular.
     * 
     * Currently only regular mode is supported.
     * 
     * @param c
     *            DBConnection
     * @param cycle
     *            String
     */
    public PayrollExporter(DBConnection c, String mode) throws Exception {
        conn = c;
        initRegistryVariables();
        pr = new PayrollReadiness(conn);

        if (StringHelper.isEmpty(mode)) {
            this.setMode(PayrollExporter.MODE_REGULAR);
        } else {
            this.setMode(mode);
        }

    }

    private void initRegistryVariables() {
        final CustomerRegistryAccess cbra = new CustomerRegistryAccess(conn);
        CustomerRegistryData cbd = null;

        if (logger.isDebugEnabled()) {
            logger.debug("Getting Registry Settings");
        }

        cbd = cbra.loadParameterByName(REG_ENABLE_ON_BOARD);
        if ((cbd != null)) {
            if ((cbd.getWbregValue() != null)) {
                ENABLE_ON_BOARD = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(((((new StringBuffer()).append("Setting "))
                            .append(REG_ENABLE_ON_BOARD)).append(" to "))
                            .append(ENABLE_ON_BOARD));
                }
            }
        }

        if (("false").equalsIgnoreCase(ENABLE_ON_BOARD)) {
            enableOnBoard = false;
        }
        if (enableOnBoard) {
            if (logger.isDebugEnabled()) {
                logger
                        .debug("OnBoarding Enabled, getting on board registry settings");
            }

            cbd = cbra.loadParameterByName(REG_ON_BOARD_FIELD);
            if ((cbd != null)) {
                if ((cbd.getWbregValue() != null)) {
                    ON_BOARD_FIELD = cbd.getWbregValue();
                    if (logger.isDebugEnabled()) {
                        logger
                                .debug(((((new StringBuffer())
                                        .append("Setting "))
                                        .append(REG_ON_BOARD_FIELD))
                                        .append(" to ")).append(ON_BOARD_FIELD));
                    }
                }
            }

            cbd = cbra.loadParameterByName(REG_ON_BOARD_EX_FIELD);
            if ((cbd != null)) {
                if ((cbd.getWbregValue() != null)) {
                    ON_BOARD_EX_FIELD = cbd.getWbregValue();
                    if (logger.isDebugEnabled()) {
                        logger
                                .debug(((((new StringBuffer())
                                        .append("Setting "))
                                        .append(REG_ON_BOARD_EX_FIELD))
                                        .append(" to "))
                                        .append(ON_BOARD_EX_FIELD));
                    }
                }
            }

            cbd = cbra.loadParameterByName(REG_EMP_ON_BOARD_FLAG);
            if ((cbd != null)) {
                if ((cbd.getWbregValue() != null)) {
                    EMP_ON_BOARD_FLAG = cbd.getWbregValue();
                    if (logger.isDebugEnabled()) {
                        logger
                                .debug(((((new StringBuffer())
                                        .append("Setting "))
                                        .append(REG_EMP_ON_BOARD_FLAG))
                                        .append(" to "))
                                        .append(EMP_ON_BOARD_FLAG));
                    }
                }
            }
        }
        cbd = cbra.loadParameterByName(REG_PAID_WRKS_FLAG);
        if ((cbd != null)) {
            if ((cbd.getWbregValue() != null)) {
                PAID_WRKS_FLAG = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(((((new StringBuffer()).append("Setting "))
                            .append(REG_PAID_WRKS_FLAG)).append(" to "))
                            .append(PAID_WRKS_FLAG));
                }
            }
        }

        cbd = cbra.loadParameterByName(REG_PAID_WRKDA_FLAG);
        if ((cbd != null)) {
            if ((cbd.getWbregValue() != null)) {
                PAID_WRKDA_FLAG = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(((((new StringBuffer()).append("Setting "))
                            .append(REG_PAID_WRKDA_FLAG)).append(" to "))
                            .append(PAID_WRKDA_FLAG));
                }
            }
        }

        cbd = cbra.loadParameterByName(REG_PAID_WRKD_FLAG);
        if ((cbd != null)) {
            if ((cbd.getWbregValue() != null)) {
                PAID_WRKD_FLAG = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(((((new StringBuffer()).append("Setting "))
                            .append(REG_PAID_WRKD_FLAG)).append(" to "))
                            .append(PAID_WRKD_FLAG));
                }
            }
        }

        cbd = cbra.loadParameterByName(REG_PAYGRP_STATUS_FLAG);
        if ((cbd != null)) {
            if ((cbd.getWbregValue() != null)) {
                PAYGRP_STATUS_FLAG = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(((((new StringBuffer()).append("Setting "))
                            .append(REG_PAYGRP_STATUS_FLAG)).append(" to "))
                            .append(PAYGRP_STATUS_FLAG));
                }
            }
        }

        cbd = cbra.loadParameterByName(REG_PAID_WRKSA_FLAG);
        if ((cbd != null)) {
            if ((cbd.getWbregValue() != null)) {
                PAID_WRKSA_FLAG = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(((((new StringBuffer()).append("Setting "))
                            .append(REG_PAID_WRKSA_FLAG)).append(" to "))
                            .append(PAID_WRKSA_FLAG));
                }
            }
        }

        cbd = cbra.loadParameterByName(REG_HANDS_OFF_UDF);
        if ((cbd != null)) {
            if ((cbd.getWbregValue() != null)) {
                HANDS_OFF_UDF = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(((((new StringBuffer()).append("Setting "))
                            .append(REG_HANDS_OFF_UDF)).append(" to "))
                            .append(HANDS_OFF_UDF));
                }
            }
        }

    }

    private void removePaygroups(ArrayList remPg) throws Exception {
        final int paygrpNum = payGrpIds.length;
        int index = 0;
        final int size = (paygrpNum - remPg.size());
        final int[] goodPG = new int[size];

        if (logger.isDebugEnabled()) {
            final StringBuffer debugMsg = new StringBuffer();
            ((((debugMsg.append("Removing pay groups from export.\n"))
                    .append(size)).append(" of ")).append(paygrpNum))
                    .append(" pay groups remain");
            logger.debug(debugMsg.toString());
        }

        for (int i = 0; i < paygrpNum; i++) {
            if ((!remPg.contains(Integer.toString(payGrpIds[i])))) {
                goodPG[index] = payGrpIds[i]; // $IGN_Use_System_arrayCopy$
                index++;
            }
        }
        setPaygrpIds(goodPG);
    }

    private int[] getPayGroupIdsFromEmpIds(int[] eIds, Date rDate)
            throws SQLException, Exception {
        final HashSet pg = new HashSet();
        final int[] pgIds;

        if ((((eIds == null)) || ((eIds.length == 0)))) {
            logger.error("No employees supplied, can not get paygroups");
            throw new Exception("No employees supplied, can not get paygroups");
        }

        try {
            final CodeMapper cm = CodeMapper.createCodeMapper(conn);
            final EmployeeAccess ea = new EmployeeAccess(conn, cm);
            EmployeeData ed = null;
            for (int i = 0; i < eIds.length; i++) {
                if ((rDate != null)) {
                    ed = ea.load(eIds[i], rDate);
                } else {
                    ed = ea.load(eIds[i], DateHelper.getCurrentDate());
                }
                if ((!pg.contains(String.valueOf(ed.getPaygrpId())))) {
                    pg.add(String.valueOf(ed.getPaygrpId()));
                    if (logger.isDebugEnabled()) {
                        logger.debug(((((new StringBuffer()).append( // $IGN_Avoid_object_instantiation_in_loops$
                                "Adding paygroup ")).append(ed.getPaygrpId()))
                                .append(" for employee ID ")).append(ed
                                .getEmpId()));
                    }
                }
            }
            pgIds = new int[pg.size()];
            int index = 0;
            final Iterator it = pg.iterator();
            while (it.hasNext()) {
                pgIds[index++] = Integer.parseInt((it.next()).toString());
            }
            return pgIds;
        } catch (java.sql.SQLException e) {
            logger.error("Could not set paygroups from employee list");
            throw e;
        }
    }

    private boolean markEmployees(String mark) throws java.sql.SQLException,
            OverrideException, Exception {

        final StringBuffer markEmpSql = new StringBuffer();
        final DBServer dbs = DBServer.getServer(conn);

        markEmpSql.append(" Select emp_id, emp_hire_date from EMPLOYEE ");
        markEmpSql.append(" WHERE ");

        if (dbs.isOracle()) {
            markEmpSql.append(TO_DATE_FUN);
            markEmpSql.append(ON_BOARD_FIELD);
            markEmpSql.append(", ");
            markEmpSql.append(ORACLE_OBD_FORMAT);
            markEmpSql.append(")");
        } else if (dbs.isDB2()) {
            markEmpSql.append(DATE_FUN);
            markEmpSql.append(ON_BOARD_FIELD);
            markEmpSql.append(") ");
        }
        markEmpSql.append(BETWEEN);
        markEmpSql.append(dbs.encodeDate(pgStartDate));
        markEmpSql.append(SANDS);
        markEmpSql.append(dbs.encodeDate(pgEndDate));
        markEmpSql.append(SANDS);
        markEmpSql.append(" PAYGRP_ID in (");
        for (int i = 0; i < payGrpIds.length; i++) {
            markEmpSql.append(((i > 0) ? ",?" : "?"));
        }
        markEmpSql.append(") ");
        markEmpSql.append(SANDS);
        markEmpSql.append(dbs.encodeNullCheck(EMP_ON_BOARD_FLAG, QUOTE_N));
        markEmpSql.append(NOT_Y);
        if ((empIds != null)) {
            markEmpSql.append(AND_EMP_ID);
            for (int i = 0; i < empIds.length; i++) {
                markEmpSql.append(String.valueOf(empIds[i]));
                if ((i != (empIds.length - 1))) {
                    markEmpSql.append(", ");
                }
            }
            markEmpSql.append(") ");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Marking employees found by: " + markEmpSql);
        }

        final Time t = new Time(System.currentTimeMillis());
        final String dateString = (((new StringBuffer()).append(((DateHelper
                .getCurrentDate()).toString()).substring(0, 11))).append(t
                .toString())).toString();

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(markEmpSql.toString());
            int fieldCount = 1;
            // ps.setString(fieldCount++, mark);
            for (int i = 0; i < payGrpIds.length; i++) {
                ps.setInt(fieldCount++, payGrpIds[i]);
            }
            rs = ps.executeQuery();
            final OverrideBuilder ovrBuilder = new OverrideBuilder(conn);
            while (rs.next()) {
                final InsertEmployeeOverride ieo = new InsertEmployeeOverride(
                        conn); // $IGN_Avoid_object_instantiation_in_loops$
                ieo.setEmpId(rs.getInt(1));
                setEmpOvrFlagField(ieo, EMP_ON_BOARD_FLAG, mark);
                setEmpOvrValField(ieo, ON_BOARD_EX_FIELD, dateString);
                ieo.setOvrComment("Employee on boarded");
                ieo.setOvrType(OverrideData.EMPLOYEE_OVERRIDE_TYPE);
                ieo.setStartDate(rs.getDate(2));
                ieo.setEndDate(DateHelper.DATE_3000);
                ieo.setWbuNameBoth(WORKBRAIN, WORKBRAIN);
                ovrBuilder.add(ieo);
            }
            conn.commit();
            ovrBuilder.execute(false, true);

        } catch (OverrideException e) {
            conn.rollback();
            logger
                    .error("Can not mark employee on board SQL: " + markEmpSql,
                            e);
            throw e;
        } finally {
            if ((ps != null)) {
                ps.close();
            }
            if ((rs != null)) {
                rs.close();
            }
        }

        return true;

    }

    private void markRecords(String mark) throws java.sql.SQLException,
            com.workbrain.tool.overrides.OverrideException, Exception {

        final StringBuffer markWDSql = new StringBuffer();
        final StringBuffer markWDASql = new StringBuffer();

        final String startDateString = DateHelper.convertDateString(startDate,
                TIMESTAMP_FORMAT);
        final String endDateString = DateHelper.convertDateString(endDate,
                TIMESTAMP_FORMAT);

        if ((((((cycleId == TERM)) || ((cycleId == RETRO)))) || ((cycleId == UNPAY)))) {

            if ((((cycleId == TERM)) || ((cycleId == UNPAY)))) {
                if ((this.whereCurrent != null)) {
                    markWDSql
                            .append("select WRKS_ID, EMP_ID, WRKS_WORK_DATE from WORK_SUMMARY where PAYGRP_ID in (");
                    for (int i = 0; i < payGrpIds.length; i++) {
                        markWDSql.append(((i > 0) ? ",?" : "?"));
                    }
                    markWDSql.append(") AND WRKS_WORK_DATE between ? AND ? ");
                    markWDSql.append(SANDS);
                    markWDSql.append(this.whereCurrent);

                    if (logger.isDebugEnabled()) {
                        logger
                                .debug("Marking Work summary records found with: "
                                        + markWDSql);
                    }
                    // Mark WORK_DETAIL records
                    PreparedStatement ps = null;
                    ResultSet rs = null;
                    try {
                        ps = conn.prepareStatement(markWDSql.toString());
                        int fieldCount = 1;
                        for (int i = 0; i < payGrpIds.length; i++) {
                            ps.setInt(fieldCount++, payGrpIds[i]);
                        }
                        ps.setTimestamp(fieldCount++, Timestamp
                                .valueOf(startDateString));
                        ps.setTimestamp(fieldCount++, Timestamp
                                .valueOf(endDateString));

                        rs = ps.executeQuery();
                        final OverrideBuilder ovrBuilder = new OverrideBuilder(
                                conn);

                        while (rs.next()) {
                            final WBIAGInsertWorkSummaryBulkOverride bulkStatment = new WBIAGInsertWorkSummaryBulkOverride(
                                    conn); // $IGN_Avoid_object_instantiation_in_loops$
                            bulkStatment.setStartDate(rs
                                    .getDate("WRKS_WORK_DATE"));
                            bulkStatment.setEndDate(rs
                                    .getDate("WRKS_WORK_DATE"));
                            bulkStatment.setWbuNameBoth(WORKBRAIN, WORKBRAIN);
                            bulkStatment.setEmpId(rs.getInt("EMP_ID"));
                            setWrksOvrFlagFieldBulk(bulkStatment,
                                    PAID_WRKS_FLAG, mark);
                            ovrBuilder.add(bulkStatment);
                        }
                        ovrBuilder.execute(true, true);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Current period records marked");
                        }
                        conn.commit();

                    } catch (com.workbrain.tool.overrides.OverrideException e) {
                        logger.error("Could not mark work summary records", e);
                        throw e;
                    } catch (java.sql.SQLException e) {
                        logger
                                .error("Could not get list of records to mark",
                                        e);
                        throw e;
                    } finally {
                        if ((ps != null)) {
                            ps.close();
                        }
                        if ((rs != null)) {
                            rs.close();
                        }
                    }
                }
            }
            if ((this.whereAdjust != null)) {
                // WORK DETAIL ADJUST RECORDS
                markWDASql.append("update WORK_DETAIL_ADJUST set ");
                markWDASql.append(PAID_WRKDA_FLAG);
                markWDASql.append(" = ? where PAYGRP_ID in (");
                for (int i = 0; i < payGrpIds.length; i++) {
                    markWDASql.append(((i > 0) ? ",?" : "?"));
                }
                markWDASql.append(") AND ");
                markWDASql.append(" WRKDA_ADJUST_DATE BETWEEN ");
                markWDASql.append(" ? AND ? AND ");

                String whereStatment = StringHelper.searchReplace(whereAdjust,
                        PAID_WRKS_FLAG, PAID_WRKSA_FLAG);
                whereStatment = StringHelper.searchReplace(whereStatment,
                        PAID_WRKD_FLAG, PAID_WRKDA_FLAG);
                markWDASql.append(whereStatment);

                if (logger.isDebugEnabled()) {
                    logger.debug("Marking Work detail adjust records with: \n"
                            + markWDASql);
                }

                PreparedStatement ps = null;
                final ResultSet rs = null;
                try {
                    ps = conn.prepareStatement(markWDASql.toString());
                    int fieldCount = 1;
                    ps.setString(fieldCount++, mark);
                    for (int i = 0; i < payGrpIds.length; i++) {
                        ps.setInt(fieldCount++, payGrpIds[i]);
                    }

                    ps.setTimestamp(fieldCount++, Timestamp
                            .valueOf(startDateString));
                    ps.setTimestamp(fieldCount++, DateHelper.addDays(Timestamp
                            .valueOf(endDateString), 1));

                    ps.executeUpdate();
                    conn.commit();
                } catch (java.sql.SQLException e) {
                    conn.rollback();
                    logger
                            .error("Could not mark work detail adjust records",
                                    e);
                    throw e;
                } finally {
                    if ((ps != null)) {
                        ps.close();
                    }
                    if ((rs != null)) {
                        rs.close();
                    }

                }
            }
        }
    }

    private boolean markPayGroups(String mark) throws java.sql.SQLException {
        PreparedStatement ps = null;
        final DBServer dbs = DBServer.getServer(conn);
        final String encodedHODDate = dbs.encodeNullCheck(HANDS_OFF_UDF,
                (("'" + DateHelper.convertDateString(DateHelper.DATE_3000,
                        TIMESTAMP_FORMAT)) + "'"));
        try {
            final StringBuffer markSQL = new StringBuffer();
            ((markSQL.append("UPDATE PAY_GROUP SET "))
                    .append(PAYGRP_STATUS_FLAG)).append(" = ? ");
            if (unlockRecords) {
                markSQL.append(", PAYGRP_HANDS_OFF_DATE = ");
                if (dbs.isOracle()) {
                    markSQL.append(TO_DATE_FUN);
                    markSQL.append(encodedHODDate);
                    markSQL.append(", ");
                    markSQL.append(TIMESTAMP_FORMAT);
                    markSQL.append(")");
                } else if (dbs.isDB2()) {
                    markSQL.append(DATE_FUN);
                    markSQL.append(encodedHODDate);
                    markSQL.append(") ");
                }
            }
            markSQL.append("WHERE PAYGRP_ID = ?");
            for (int i = 0; i < payGrpIds.length; i++) {
                ps = conn.prepareStatement(markSQL.toString());
                ps.setString(1, mark);
                ps.setInt(2, payGrpIds[i]);
                ps.executeUpdate();
            }
        } catch (java.sql.SQLException e) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger
                        .error(
                                "com.workbrain.app.bo.ejb.actions.ExportPayrollAction.class",
                                e);
            }
        } finally {
            if ((ps != null)) {
                ps.close();
            }
        }

        return true;
    }

    protected void runPayroll() throws Exception {

        final String filePath;
        final String fileName;
        final String fileExt;
        final String petName;

        final PayrollExportProcessor processor = new PayrollExportProcessor();

        processor.setPetId(petId);
        processor.setPayGrpIds(payGrpIds);
        if ((startDate != null)) {
            processor.setFromDate(startDate);
        }
        if ((endDate != null)) {
            processor.setToDate(endDate);
        }
        processor.setWriteToTable(writeToTable);
        processor.setWriteToFile(writeToFile);
        processor.setMergeFiles(mergeFiles);
        processor.setAdjustDates(adjustDates);
        // petXML, filename and directory
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            final String petSql = "SELECT * FROM payroll_export_tsk WHERE pet_id = ?";
            ps = conn.prepareStatement(petSql);
            ps.setInt(1, petId);
            rs = ps.executeQuery();
            if (rs.next()) {
                final Clob clob = rs.getClob("pet_xml");
                final String xml = clob.getSubString(1L, ((int) clob.length()));
                processor.setPetXml(xml);
                filePath = rs.getString("PET_OUT_FILE_PATH");
                // filePath = "\\\\Toriag\\IAGWebsite\\build";
                fileName = rs.getString("PET_OUT_FILE_MASK");
                fileExt = rs.getString("PET_OUT_FILE_EXT");
                petName = rs.getString("PET_NAME");
                processor.setPetName(petName);
                processor.setPetPath(filePath);
                processor.setPetMask(fileName);
                processor.setPetExt(fileExt);
            } else {
                throw new RuntimeException(
                        "Payroll export task id not found : " + petId);
            }
        } finally {
            if ((ps != null)) {
                ps.close();
            }
            if ((rs != null)) {
                rs.close();
            }
        }
        processor.setWhereCurrent(this.whereCurrent);
        processor.setWhereAdjustment(this.whereAdjust);
        if (logger.isInfoEnabled()) {
            logger.info("Exporting");
            logger.info("Commiting Records");
        }
        conn.commit();
        processor.process(conn, null);
        numRecExp = ((processor.getResults()).values()).toString();
        if (logger.isInfoEnabled()) {
            logger.info("Export finished successfully");
            logger.info("Records exported (before grouping):  " + numRecExp);
        }

    }

    private Date getPayPeriodEndDate() throws Exception {

        final PayGroupAccess pga = new PayGroupAccess(conn);
        Date PayPeriodStart = null;
        Date PayPeriodEnd = null;
        for (int i = 0; i < payGrpIds.length; i++) {
            final PayGroupData pgd = pga.load(payGrpIds[i]);
            if ((PayPeriodStart == null)) {
                PayPeriodStart = pgd.getPaygrpStartDate();
                PayPeriodEnd = pgd.getPaygrpEndDate();
                if (logger.isDebugEnabled()) {
                    logger.debug(((((new StringBuffer()).append( // $IGN_Avoid_object_instantiation_in_loops$
                            "Pay group End date for pay group: ")).append(pgd
                            .getPaygrpId())).append(" : ")).append(pgd
                            .getPaygrpEndDate()));
                }
            } else {
                // All pay period start and end dates must be the same.
                if ((!PayPeriodStart.equals(pgd.getPaygrpStartDate()))) {
                    if (logger.isDebugEnabled()) {
                        logger
                                .debug(((((((new StringBuffer())
                                        .append( // $IGN_Avoid_object_instantiation_in_loops$
                                        "Pay group Start dates different, expecting: "))
                                        .append(PayPeriodStart)).append(" != "))
                                        .append(pgd.getPaygrpStartDate()))
                                        .append(" For pay group ID "))
                                        .append(pgd.getPaygrpId()));
                    }
                    logger
                            .error("Different Pay Periods. Check Pay period Start Dates");
                    throw new Exception( // $IGN_Avoid_object_instantiation_in_loops$
                            "Can not export paygroups with different Pay Periods");
                }
                if ((!PayPeriodEnd.equals(pgd.getPaygrpEndDate()))) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(((((((new StringBuffer()).append( // $IGN_Avoid_object_instantiation_in_loops$
                                "Pay group End dates different, expecting: "))
                                .append(PayPeriodStart)).append(" != "))
                                .append(pgd.getPaygrpEndDate()))
                                .append(" For pay group ID ")).append(pgd
                                .getPaygrpId()));
                    }
                    logger
                            .error("Different Pay Periods. Check Pay period End Dates");
                    throw new Exception( // $IGN_Avoid_object_instantiation_in_loops$
                            "Can not export paygroups with different Pay Periods");
                }
            }

        }
        return PayPeriodEnd;
    }

    private Date getPayPeriodStartDate() throws Exception {

        final PayGroupAccess pga = new PayGroupAccess(conn);
        Date PayPeriodStart = null;
        Date PayPeriodEnd = null;
        for (int i = 0; i < payGrpIds.length; i++) {
            final PayGroupData pgd = pga.load(payGrpIds[i]);
            if ((PayPeriodStart == null)) {
                PayPeriodStart = pgd.getPaygrpStartDate();
                PayPeriodEnd = pgd.getPaygrpEndDate();
                if (logger.isDebugEnabled()) {
                    logger.debug(((((new StringBuffer()).append( // $IGN_Avoid_object_instantiation_in_loops$
                            "Pay group Start date for pay group: ")).append(pgd
                            .getPaygrpId())).append(" : ")).append(pgd
                            .getPaygrpStartDate()));
                }
            } else {
                // All pay period start and end dates must be the same.
                if ((!PayPeriodStart.equals(pgd.getPaygrpStartDate()))) {
                    if (logger.isDebugEnabled()) {
                        logger
                                .debug(((((((new StringBuffer())
                                        .append( // $IGN_Avoid_object_instantiation_in_loops$
                                        "Pay group Start dates different, expecting: "))
                                        .append(PayPeriodStart)).append(" != "))
                                        .append(pgd.getPaygrpStartDate()))
                                        .append(" For pay group ID "))
                                        .append(pgd.getPaygrpId()));
                    }
                    logger
                            .error("Different Pay Periods. Check Pay period End Dates");
                    throw new Exception( // $IGN_Avoid_object_instantiation_in_loops$
                            "Can not export paygroups with different Start Dates Periods");
                }
                if ((!PayPeriodEnd.equals(pgd.getPaygrpEndDate()))) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(((((((new StringBuffer()).append( // $IGN_Avoid_object_instantiation_in_loops$
                                "Pay group End dates different, expecting: "))
                                .append(PayPeriodStart)).append(" != "))
                                .append(pgd.getPaygrpEndDate()))
                                .append(" For pay group ID ")).append(pgd
                                .getPaygrpId()));
                    }
                    logger
                            .error("Different Pay Periods. Check Pay period End Dates");
                    throw new Exception( // $IGN_Avoid_object_instantiation_in_loops$
                            "Can not export paygroups with different End Dates");
                }
            }

        }
        return PayPeriodStart;
    }

    private Date getLastPayPeriodEndDate() throws Exception {

        final PayGroupAccess pga = new PayGroupAccess(conn);
        Date lastPayPeriod = null;
        for (int i = 0; i < payGrpIds.length; i++) {
            final PayGroupData pgd = pga.load(payGrpIds[i]);
            if ((lastPayPeriod == null)) {
                lastPayPeriod = DateHelper
                        .addDays(pgd.getPaygrpStartDate(), -1);
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuffer().append(
                            "lastPayPeriod Date =") // $IGN_Avoid_object_instantiation_in_loops$
                            .append(lastPayPeriod.toString()).toString());
                }
            } else {
                // All pay period end dates must be the same.
                if ((!lastPayPeriod.equals(DateHelper.addDays(pgd
                        .getPaygrpStartDate(), -1)))) {
                    if (logger.isInfoEnabled()) {
                        logger.info(ERR_MSG_PG_DATES);
                    }
                    logger
                            .error("Different Pay Periods. Check Pay period End Dates");
                    throw new Exception( // $IGN_Avoid_object_instantiation_in_loops$
                            "Can not export paygroups with different Pay Periods");

                }
            }

        }
        return lastPayPeriod;
    }

    private Date getMinHireDate() throws Exception {

        if ((((pgStartDate == null)) || ((pgEndDate == null)))) {
            throw new Exception("Invalid Dates, can not get min hire date");
        }

        // Find the Min hire date
        PreparedStatement ps = null;
        ResultSet rs = null;
        Date foundDate = null;
        try {
            final StringBuffer minHireDateSql = new StringBuffer();
            minHireDateSql
                    .append("Select min(EMP_HIRE_DATE) from EMPLOYEE where ");
            minHireDateSql.append(this.whereMinHire);

            ps = conn.prepareStatement(minHireDateSql.toString());
            rs = ps.executeQuery();
            if (logger.isInfoEnabled()) {
                logger.info("PayrollExportProcessor.getMinHireDate");
                logger.info("SQL: " + minHireDateSql.toString());
            }
            if (rs.next()) {
                if (logger.isInfoEnabled()) {
                    logger.info("PayrollExportProcessor.getMinHireDate");
                    logger.info("min date found: " + rs.getDate(1));
                }

                foundDate = rs.getDate(1);
            }

        } catch (Exception t) {

            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger
                        .error(
                                "com.workbrain.app.bo.ejb.actions.ReadinessPayrollAction.class",
                                t);
            }

        } finally {
            if ((ps != null)) {
                ps.close();
            }
            if ((rs != null)) {
                rs.close();
            }
        }
        return foundDate;
    }

    private void setWhere() {
        final DBServer dbs = DBServer.getServer(conn);
        final StringBuffer pgWhereCurrent = new StringBuffer();
        final StringBuffer pgWhereAdj = new StringBuffer();
        final String encodedOnboardDate = dbs.encodeNullCheck(ON_BOARD_FIELD,
                (("'" + DateHelper.convertDateString(DateHelper.DATE_3000,
                        TIMESTAMP_FORMAT)) + "'"));

        switch (cycleId) {
        case REGULAR: {

            pgWhereCurrent.append(dbs.encodeNullCheck(PAID_WRKS_FLAG, QUOTE_N));
            pgWhereCurrent.append(NOT_Y);
            if (enableOnBoard) {
                pgWhereCurrent.append(AND_EMP_ID);
                pgWhereCurrent.append(" SELECT EMP_ID FROM EMPLOYEE WHERE ");
                pgWhereCurrent.append(dbs.encodeNullCheck(EMP_ON_BOARD_FLAG,
                        QUOTE_N));
                pgWhereCurrent.append(" = 'Y' ) ");
            }

            if ((empIds != null)) {
                pgWhereCurrent.append(AND_EMP_ID);
                for (int i = 0; i < empIds.length; i++) {
                    pgWhereCurrent.append(String.valueOf(empIds[i]));
                    if ((i != (empIds.length - 1))) {
                        pgWhereCurrent.append(", ");
                    }
                }
                pgWhereCurrent.append(") ");
            }
            this.whereCurrent = pgWhereCurrent.toString();

            if (enableOnBoard) {
                final String encodedOnboardExDate = dbs.encodeNullCheck(
                        ON_BOARD_EX_FIELD, (("'" + DateHelper
                                .convertDateString(DateHelper.DATE_3000,
                                        TIMESTAMP_FORMAT)) + "'"));

                pgWhereAdj.append(" wrkda_adjust_date > ");
                if (dbs.isOracle()) {
                    pgWhereAdj.append(TO_DATE_FUN);
                    pgWhereAdj.append(encodedOnboardExDate);
                    pgWhereAdj.append(", '");
                    pgWhereAdj.append(TIMESTAMP_FORMAT_FULL);
                    pgWhereAdj.append("')");
                } else if (dbs.isDB2()) {
                    pgWhereAdj.append("Timestamp(");
                    pgWhereAdj.append(encodedOnboardExDate);
                    pgWhereAdj.append(") ");
                }
                pgWhereAdj.append(SANDS);
            }

            pgWhereAdj.append(" ");
            pgWhereAdj.append(dbs.encodeNullCheck(PAID_WRKD_FLAG, QUOTE_N));
            pgWhereAdj.append(NOT_Y);
            if (enableOnBoard) {
                pgWhereAdj.append(AND_EMP_ID);
                pgWhereAdj.append(" SELECT EMP_ID FROM EMPLOYEE WHERE ");
                pgWhereAdj.append(dbs.encodeNullCheck(EMP_ON_BOARD_FLAG,
                        QUOTE_N));
                pgWhereAdj.append(" = 'Y' ) ");
            }

            if ((empIds != null)) {
                pgWhereAdj.append(AND_EMP_ID);
                for (int i = 0; i < empIds.length; i++) {
                    pgWhereAdj.append(String.valueOf(empIds[i]));
                    if ((i != (empIds.length - 1))) {
                        pgWhereAdj.append(", ");
                    }
                }
                pgWhereAdj.append(") ");
            }
            this.whereAdjust = pgWhereAdj.toString();
            break;
        }
        case ON_BOARD: {
            final StringBuffer minHire = new StringBuffer();
            pgWhereCurrent
                    .append(" EMP_ID in (select EMP_ID from EMPLOYEE where ");
            // DB Speciffic
            if (dbs.isOracle()) {
                pgWhereCurrent.append(TO_DATE_FUN);
                pgWhereCurrent.append(ON_BOARD_FIELD);
                pgWhereCurrent.append(", ");
                pgWhereCurrent.append(ORACLE_OBD_FORMAT);
                pgWhereCurrent.append(")");
            } else if (dbs.isDB2()) {
                pgWhereCurrent.append(DATE_FUN);
                pgWhereCurrent.append(encodedOnboardDate);
                pgWhereCurrent.append(") ");
            }
            // ----------
            pgWhereCurrent.append(BETWEEN);
            pgWhereCurrent.append(dbs.encodeDate(pgStartDate));
            pgWhereCurrent.append(SANDS);
            pgWhereCurrent.append(dbs.encodeDate(pgEndDate));
            pgWhereCurrent.append(SANDS);
            pgWhereCurrent.append(dbs.encodeNullCheck(EMP_ON_BOARD_FLAG,
                    QUOTE_N));
            pgWhereCurrent.append(" <> 'Y' ) ");
            pgWhereCurrent.append(SANDS);
            pgWhereCurrent.append(dbs.encodeNullCheck(PAID_WRKS_FLAG, QUOTE_N));
            pgWhereCurrent.append(NOT_Y);

            if ((empIds != null)) {
                pgWhereCurrent.append(AND_EMP_ID);
                for (int i = 0; i < empIds.length; i++) {
                    pgWhereCurrent.append(String.valueOf(empIds[i]));
                    if ((i != (empIds.length - 1))) {
                        pgWhereCurrent.append(", ");
                    }
                }
                pgWhereCurrent.append(") ");
            }
            // Set Where Min Hire Date

            if (dbs.isOracle()) {
                minHire.append(TO_DATE_FUN);
                minHire.append(ON_BOARD_FIELD);
                minHire.append(", ");
                minHire.append(ORACLE_OBD_FORMAT);
                minHire.append(")");
            } else if (dbs.isDB2()) {
                minHire.append(DATE_FUN);
                minHire.append(encodedOnboardDate);
                minHire.append(") ");
            }
            // ----------
            minHire.append(BETWEEN);
            minHire.append(dbs.encodeDate(pgStartDate));
            minHire.append(SANDS);
            minHire.append(dbs.encodeDate(pgEndDate));
            minHire.append(SANDS);
            minHire.append(dbs.encodeNullCheck(EMP_ON_BOARD_FLAG, QUOTE_N));
            minHire.append(NOT_Y);

            if ((empIds != null)) {
                minHire.append(" AND employee.EMP_ID in (");
                for (int i = 0; i < empIds.length; i++) {
                    minHire.append(String.valueOf(empIds[i]));
                    if ((i != (empIds.length - 1))) {
                        minHire.append(", ");
                    }
                }
                minHire.append(") ");
            }

            if ((payGrpIds != null)) {
                minHire.append(" AND employee.paygrp_id in (");
                for (int i = 0; i < payGrpIds.length; i++) {
                    minHire.append(String.valueOf(payGrpIds[i]));
                    if ((i != (payGrpIds.length - 1))) {
                        minHire.append(", ");
                    }
                }
                minHire.append(") ");
            }

            this.whereMinHire = minHire.toString();
            this.whereCurrent = pgWhereCurrent.toString();
            this.whereAdjust = " emp_id = -99999 ";
            break;
        }
        case TERM: {
            final StringBuffer term = new StringBuffer();
            pgWhereCurrent
                    .append(" EMP_ID in (select EMP_ID from EMPLOYEE where ");
            // DB Speciffic
            pgWhereCurrent.append(" EMP_TERMINATION_DATE ");
            // ----------
            pgWhereCurrent.append(BETWEEN);
            pgWhereCurrent.append(dbs.encodeTimestamp(pgStartDate));
            pgWhereCurrent.append(SANDS);
            pgWhereCurrent.append(dbs.encodeTimestamp(pgEndDate));
            if (enableOnBoard) {
                pgWhereCurrent.append(SANDS);
                pgWhereCurrent.append(dbs.encodeNullCheck(EMP_ON_BOARD_FLAG,
                        QUOTE_N));
                pgWhereCurrent.append(" = 'Y'");
            }
            pgWhereCurrent.append(" ) ");
            pgWhereCurrent.append(SANDS);
            pgWhereCurrent.append(dbs.encodeNullCheck(PAID_WRKS_FLAG, QUOTE_N));
            pgWhereCurrent.append(NOT_Y);

            if ((empIds != null)) {
                pgWhereCurrent.append(AND_EMP_ID);
                for (int i = 0; i < empIds.length; i++) {
                    pgWhereCurrent.append(String.valueOf(empIds[i]));
                    if ((i != (empIds.length - 1))) {
                        pgWhereCurrent.append(", ");
                    }
                }
                pgWhereCurrent.append(") ");
            }
            // Set WhereTerm
            term.append(" wrks_work_date between ");
            term.append(" emp_hire_date AND emp_termination_date");
            term.append(" AND wrks_work_date between ");
            term.append(dbs.encodeTimestamp(pgStartDate));
            term.append(SANDS);
            term.append(dbs.encodeTimestamp(pgEndDate));
            term.append(SANDS);

            term.append(" EMP_TERMINATION_DATE ");
            // ----------
            term.append(BETWEEN);
            term.append(dbs.encodeTimestamp(pgStartDate));
            term.append(SANDS);
            term.append(dbs.encodeTimestamp(pgEndDate));
            if (enableOnBoard) {
                term.append(SANDS);
                term.append(dbs.encodeNullCheck(EMP_ON_BOARD_FLAG, QUOTE_N));
                term.append(" = 'Y' ");
            }
            term.append(SANDS);
            term.append(dbs.encodeNullCheck(PAID_WRKS_FLAG, QUOTE_N));
            term.append(NOT_Y);

            if ((empIds != null)) {
                term.append(" AND employee.EMP_ID in (");
                for (int i = 0; i < empIds.length; i++) {
                    term.append(String.valueOf(empIds[i]));
                    if ((i != (empIds.length - 1))) {
                        term.append(", ");
                    }
                }
                term.append(") ");
            }

            this.whereTerm = term.toString();
            this.whereCurrent = pgWhereCurrent.toString();
            this.whereAdjust = StringHelper.searchReplace(pgWhereCurrent
                    .toString(), PAID_WRKS_FLAG, PAID_WRKD_FLAG);
            break;
        }
        case RETRO: {

            pgWhereAdj.append("  ");
            pgWhereAdj.append(dbs.encodeNullCheck(PAID_WRKD_FLAG, QUOTE_N));
            pgWhereAdj.append(NOT_Y);
            if (enableOnBoard) {
                pgWhereAdj.append(AND_EMP_ID);
                pgWhereAdj.append(" SELECT EMP_ID FROM EMPLOYEE WHERE ");
                pgWhereAdj.append(dbs.encodeNullCheck(EMP_ON_BOARD_FLAG,
                        QUOTE_N));
                pgWhereAdj.append(" = 'Y' ) ");
            }
            if ((empIds != null)) {
                pgWhereAdj.append(AND_EMP_ID);
                for (int i = 0; i < empIds.length; i++) {
                    pgWhereAdj.append(String.valueOf(empIds[i]));
                    if ((i != (empIds.length - 1))) {
                        pgWhereAdj.append(", ");
                    }
                }
                pgWhereAdj.append(") ");
            }

            this.whereCurrent = " emp_id = -99999 "; // pgWhereAdj.toString();
            this.whereAdjust = pgWhereAdj.toString();
            break;
        }
        case RESET: {
            this.whereCurrent = null;
            this.whereAdjust = null;
            break;
        }
        case UNPAY: {
            pgWhereAdj.append("  ");
            pgWhereCurrent.append(dbs.encodeNullCheck(PAID_WRKS_FLAG, QUOTE_N));
            pgWhereCurrent.append(" = 'Y' ");
            if ((empIds != null)) {
                pgWhereCurrent.append(AND_EMP_ID);
                for (int i = 0; i < empIds.length; i++) {
                    pgWhereCurrent.append(String.valueOf(empIds[i]));
                    if ((i != (empIds.length - 1))) {
                        pgWhereCurrent.append(", ");
                    }
                }
                pgWhereCurrent.append(") ");
            }

            this.whereCurrent = pgWhereCurrent.toString();
            this.whereAdjust = StringHelper.searchReplace(pgWhereCurrent
                    .toString(), PAID_WRKS_FLAG, PAID_WRKD_FLAG);

        }
        }
    }

    /**
     * Returns the max term date of all employees terminated
     * 
     * @return Date
     */
    private Date getTermDate() throws java.sql.SQLException {

        // Find the Max term date
        PreparedStatement ps = null;
        ResultSet rs = null;
        Date termDate = null;
        try {
            if ((((startDate == null)) || ((endDate == null)))) {
                startDate = getPayPeriodStartDate();
                endDate = DateHelper
                        .addDays(getPayPeriodEndDate(), termAddDays);
            }

            final String startDateString = DateHelper.convertDateString(
                    startDate, TIMESTAMP_FORMAT);
            final String endDateString = DateHelper.convertDateString(endDate,
                    TIMESTAMP_FORMAT);

            final StringBuffer maxTermDateSql = new StringBuffer();
            maxTermDateSql
                    .append("Select max(EMP_TERMINATION_DATE) from EMPLOYEE where PAYGRP_ID in (");
            for (int i = 0; i < payGrpIds.length; i++) {
                maxTermDateSql.append(((i > 0) ? ",?" : "?"));
            }
            maxTermDateSql.append(") AND EMP_TERMINATION_DATE between ? AND ?");
            ps = conn.prepareStatement(maxTermDateSql.toString());
            int fieldCount = 1;
            for (int i = 0; i < payGrpIds.length; i++) {
                ps.setInt(fieldCount++, payGrpIds[i]);
            }
            ps.setTimestamp(fieldCount++, Timestamp.valueOf(startDateString));
            ps.setTimestamp(fieldCount++, Timestamp.valueOf(endDateString));

            rs = ps.executeQuery();
            if (rs.next()) {
                termDate = DateHelper.addDays(rs.getDate(1), -1);
            }

        } catch (Throwable t) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger
                        .error(
                                "com.workbrain.app.bo.ejb.actions.ReadinessPayrollAction.class",
                                t);
            }
        } finally {
            if ((ps != null)) {
                ps.close();
            }
            if ((rs != null)) {
                rs.close();
            }
        }
        // If no dates found
        return termDate;
    }

    private boolean exporterReady() {
        boolean result = true;
        if ((payGrpIds.length == 0)) {
            message = " No ready pay groups selected. ";
            if (logger.isDebugEnabled()) {
                logger.debug("No Pay groups selected");
            }
            result = false;
        } else if (((((startDate == null)) || ((endDate == null))) && (cycleId != REGULAR))) {
            message = " No records to export. ";
            if (logger.isDebugEnabled()) {
                logger.debug("No records to export");
            }
            result = false;
        } else if (((((startDate == null)) || ((endDate == null))) && (cycleId == REGULAR))) {
            message = " Invalid start and end dates";
            if (logger.isDebugEnabled()) {
                logger.debug("Invalid start and end dates");
            }
            result = false;
        }
        return result;

    }

    private void setMode(String mode) {
        if (StringHelper.isEmpty(mode)) {
            basicMode = false;
        } else if (mode.equalsIgnoreCase(PayrollExporter.MODE_BASIC)) {
            basicMode = true;
        } else {
            basicMode = false;
        }
    }

    public static java.util.Date GetRelativePayPeriodStartDate(
            java.util.Date datRequested, PayGroupData pgd) {

        // Find out the difference in pay periods from the requested date to the
        // pay period start
        final double dblDaysDiff = DateHelper.dateDifferenceInDays(
                datRequested, pgd.getPaygrpStartDate());
        final double dblPayDuration = DateHelper.dateDifferenceInDays(pgd
                .getPaygrpEndDate(), pgd.getPaygrpStartDate());

        // request date is within current pay period
        if ((((dblDaysDiff >= 0)) && ((dblDaysDiff <= dblPayDuration)))) {
            return pgd.getPaygrpStartDate();
        }

        // Get number of pay periods which separate request date and current pay
        // period
        int intNumPayperiods = (int) (dblDaysDiff / dblPayDuration);

        if ((dblDaysDiff < 0)) {
            intNumPayperiods--;
        }
        // Date requested is in prior pay period
        return DateHelper.addDays(pgd.getPaygrpStartDate(),
                (int) (intNumPayperiods * dblPayDuration));
    }

    private java.util.Date GetRelativePayPeriodEndDate(
            java.util.Date datRequested, PayGroupData pgd) {
        // Date requested is in prior pay period

        final Date datStartDate = GetRelativePayPeriodStartDate(datRequested,
                pgd);

        final int intPayDuration = DateHelper.dateDifferenceInDays(pgd
                .getPaygrpEndDate(), pgd.getPaygrpStartDate());

        return DateHelper.addDays(datStartDate, intPayDuration);

    }

    private java.util.Date GetRelativePayPeriodStartDate(
            java.util.Date dateRequested) {
        Date sd = null;
        final PayGroupAccess pga = new PayGroupAccess(conn);
        for (int i = 0; i < payGrpIds.length; i++) {
            final PayGroupData pgd = pga.load(payGrpIds[i]);
            if ((sd == null)) {
                sd = GetRelativePayPeriodStartDate(dateRequested, pgd);
            } else if ((sd != GetRelativePayPeriodStartDate(dateRequested, pgd))) {
                return null;
            }
        }
        return sd;
    }

    private java.util.Date GetRelativePayPeriodEndDate(
            java.util.Date dateRequested) {
        Date ed = null;
        final PayGroupAccess pga = new PayGroupAccess(conn);
        for (int i = 0; i < payGrpIds.length; i++) {
            final PayGroupData pgd = pga.load(payGrpIds[i]);
            if ((ed == null)) {

                ed = GetRelativePayPeriodEndDate(dateRequested, pgd);
            } else if ((ed != GetRelativePayPeriodEndDate(dateRequested, pgd))) {
                return null;
            }
        }
        return ed;
    }

    private ArrayList checkOnBoardUnAuthByEmployee() throws Exception {
        if ((this.empIds == null)) {
            throw new Exception("Employee IDs not set");
        }
        PreparedStatement ps = null;
        ResultSet rs = null;
        ArrayList badEmp = null;
        try {
            final StringBuffer readyByEmpSql = new StringBuffer();

            readyByEmpSql.append("select employee.emp_id ");
            readyByEmpSql.append(" from employee, work_summary ");
            readyByEmpSql
                    .append(" where work_summary.emp_id = employee.emp_id ");
            readyByEmpSql.append(" AND work_summary.wrks_authorized <> 'Y' ");
            readyByEmpSql.append(SANDS);
            readyByEmpSql.append(this.whereMinHire);
            readyByEmpSql.append("group by employee.emp_id");

            ps = conn.prepareStatement(readyByEmpSql.toString());
            rs = ps.executeQuery();
            badEmp = new ArrayList();
            while (rs.next()) {
                badEmp.add(rs.getObject(1));
            }
            if (logger.isInfoEnabled()) {
                logger.info("The following employess were not ready "
                        + badEmp.toString());
            }
        } catch (java.sql.SQLException e) {
            logger.error("Can not check Unauth", e);
        } finally {
            if ((ps != null)) {
                ps.close();
            }
            if ((rs != null)) {
                rs.close();
            }
        }
        return badEmp;
    }

    private ArrayList checkTermUnAuthByEmployee() throws Exception {
        if ((this.empIds == null)) {
            throw new Exception("Employee IDs not set");
        }
        PreparedStatement ps = null;
        ResultSet rs = null;
        ArrayList badEmp = null;
        try {
            final StringBuffer readyByEmpSql = new StringBuffer();

            readyByEmpSql.append("select employee.emp_id ");
            readyByEmpSql.append(" from employee, work_summary ");
            readyByEmpSql
                    .append(" where work_summary.emp_id = employee.emp_id ");
            readyByEmpSql.append(" AND work_summary.wrks_authorized <> 'Y' ");
            readyByEmpSql.append(SANDS);
            readyByEmpSql.append(this.whereTerm);
            readyByEmpSql.append("group by employee.emp_id");

            ps = conn.prepareStatement(readyByEmpSql.toString());
            rs = ps.executeQuery();
            badEmp = new ArrayList();
            while (rs.next()) {
                badEmp.add(rs.getObject(1));
            }
            if (logger.isInfoEnabled()) {
                logger.info("The following employess were not ready "
                        + badEmp.toString());
            }
        } catch (java.sql.SQLException e) {
            logger.error("Can't check term unauth", e);
            throw e;
        } finally {
            if ((ps != null)) {
                ps.close();
            }
            if ((rs != null)) {
                rs.close();
            }
        }

        return badEmp;
    }

    /**
     * Method used to set a work summary flag field within the
     * InsertWorkDetailOverride object based on the parameter.
     * 
     * @param wrksOvr
     * @param wrksFieldName
     * @throws Exception
     */
    public void setWrksOvrFlagFieldBulk(
            WBIAGInsertWorkSummaryBulkOverride wrksOvr, String wrksFieldName,
            String mark) throws Exception {
        if (wrksFieldName.equalsIgnoreCase("WRKS_FLAG1")) {
            wrksOvr.setWrksFlag1(mark);
        } else if (wrksFieldName.equalsIgnoreCase("WRKS_FLAG2")) {
            wrksOvr.setWrksFlag2(mark);
        } else if (wrksFieldName.equalsIgnoreCase("WRKS_FLAG3")) {
            wrksOvr.setWrksFlag3(mark);
        } else if (wrksFieldName.equalsIgnoreCase("WRKS_FLAG4")) {
            wrksOvr.setWrksFlag4(mark);
        } else if (wrksFieldName.equalsIgnoreCase("WRKS_FLAG5")) {
            wrksOvr.setWrksFlag5(mark);
        } else {
            throw new Exception((((((new StringBuffer())
                    .append("Value in registry for ")).append(REG_PATH))
                    .append(REG_PAID_WRKS_FLAG)).append(" is invalid."))
                    .toString());
        }
    }

    /**
     * Method used to set an employee flag field within the
     * InsertEmployeeOverride object based on the parameter.
     * 
     * @param empOvr
     * @param empFieldName
     * @throws Exception
     */
    private void setEmpOvrFlagField(InsertEmployeeOverride empOvr,
            String empFieldName, String mark) throws Exception {
        if (empFieldName.equalsIgnoreCase("EMP_FLAG1")) {
            empOvr.setEmpFlag1(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG2")) {
            empOvr.setEmpFlag2(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG3")) {
            empOvr.setEmpFlag3(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG4")) {
            empOvr.setEmpFlag4(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG5")) {
            empOvr.setEmpFlag5(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG6")) {
            empOvr.setEmpFlag6(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG7")) {
            empOvr.setEmpFlag7(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG8")) {
            empOvr.setEmpFlag8(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG9")) {
            empOvr.setEmpFlag9(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG10")) {
            empOvr.setEmpFlag10(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG11")) {
            empOvr.setEmpFlag11(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG12")) {
            empOvr.setEmpFlag12(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG13")) {
            empOvr.setEmpFlag13(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG14")) {
            empOvr.setEmpFlag14(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG15")) {
            empOvr.setEmpFlag15(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG16")) {
            empOvr.setEmpFlag16(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG17")) {
            empOvr.setEmpFlag17(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG18")) {
            empOvr.setEmpFlag18(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG19")) {
            empOvr.setEmpFlag19(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_FLAG20")) {
            empOvr.setEmpFlag20(mark);
        } else {
            throw new Exception((((((new StringBuffer())
                    .append("Value in registry for ")).append(REG_PATH))
                    .append(REG_EMP_ON_BOARD_FLAG)).append(" is invalid."))
                    .toString());
        }
    }

    /**
     * Method used to set an employee flag field within the
     * InsertEmployeeOverride object based on the parameter.
     * 
     * @param empOvr
     * @param empFieldName
     * @throws Exception
     */
    private void setEmpOvrValField(InsertEmployeeOverride empOvr,
            String empFieldName, String mark) throws Exception {
        if (empFieldName.equalsIgnoreCase("EMP_VAL1")) {
            empOvr.setEmpVal1(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL2")) {
            empOvr.setEmpVal2(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL3")) {
            empOvr.setEmpVal3(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL4")) {
            empOvr.setEmpVal4(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL5")) {
            empOvr.setEmpVal5(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL6")) {
            empOvr.setEmpVal6(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL7")) {
            empOvr.setEmpVal7(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL8")) {
            empOvr.setEmpVal8(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL9")) {
            empOvr.setEmpVal9(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL10")) {
            empOvr.setEmpVal10(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL11")) {
            empOvr.setEmpVal11(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL12")) {
            empOvr.setEmpVal12(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL13")) {
            empOvr.setEmpVal13(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL14")) {
            empOvr.setEmpVal14(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL15")) {
            empOvr.setEmpVal15(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL16")) {
            empOvr.setEmpVal16(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL17")) {
            empOvr.setEmpVal17(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL18")) {
            empOvr.setEmpVal18(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL19")) {
            empOvr.setEmpVal19(mark);
        } else if (empFieldName.equalsIgnoreCase("EMP_VAL20")) {
            empOvr.setEmpVal20(mark);
        } else {
            throw new Exception((((((new StringBuffer())
                    .append("Value in registry for ")).append(REG_PATH))
                    .append(REG_ON_BOARD_EX_FIELD)).append(" is invalid."))
                    .toString());
        }
    }

}
