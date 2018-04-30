package com.wbiag.app.wbinterface.schedulein;

import java.util.*;
import java.sql.SQLException;
import java.text.ParseException;
import javax.sql.*;
import javax.naming.*;
import java.util.Date.*;
import java.text.*;

import com.workbrain.util.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.schedulein.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.server.WorkbrainParametersRetriever;
import com.workbrain.sql.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.ta.ruleengine.*;

import com.wbiag.tool.overrides.WBIAGInsertWorkSummaryOverride;

/**
 * Transaction type for Tomax Schedule data processing.
 *
 * <p>Copyright: Copyright (c) 2002 Workbrain Inc.</p>
 *
 **/
public class WorkSummaryTransaction
    extends WorkTransaction
    implements TransactionType {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(WorkSummaryTransaction.class);
    public final static int EMP_NAME = 0;
    public final static int OVRTYP_ID = 1;
    public final static int WORK_DATE = 2;
    public final static int WRKS_CLOCKS = 3;
    public final static int CALCGRP_NAME = 4;
    public final static int PAYGRP_NAME = 5;
    public final static int WRKS_AUTHORIZED = 6;
    public final static int WRKS_FLAG_BRK = 7;
    public final static int WRKS_FLAG_RECALL = 8;
    public final static int WRKS_IN_CODE = 9;
    public final static int WRKS_OUT_CODE = 10;
    public final static int WRKS_FULL_DAY_CODE = 11;
    public final static int WRKS_FULL_DAY_MINUTES = 12;
    public final static int OVR_WBU_NAME = 13;
    public final static int OVR_COMMENTS = 14;
    public final static int WRKS_FLAGS1_5 = 15;
    public final static int WRKS_UDF1 = 16;
    public final static int WRKS_UDF2 = 17;
    public final static int WRKS_UDF3 = 18;
    public final static int WRKS_UDF4 = 19;
    public final static int WRKS_UDF5 = 20;
    public final static int WRKS_UDF6 = 21;
    public final static int WRKS_UDF7 = 22;
    public final static int WRKS_UDF8 = 23;
    public final static int WRKS_UDF9 = 24;
    public final static int WRKS_UDF10 = 25;

    public static final String DATE_FMT = "yyyyMMdd";
    public static final String TIME_FMT = "yyyyMMdd HHmm";

    private int empId;
    private int ovrtypId;
    private int wrksFullDayMinutes = Integer.MIN_VALUE;

    private Date workDate = null;
    private String wrksClocks = null;
    private int calcgrpId = Integer.MIN_VALUE;

    private String paygrpName = null;
    private String wrksAuthorized = null;
    private String wrksFlagBrk = null;
    private String wrksFlagRecall = null;
    private String wrksInCode = null;
    private String wrksOutCode = null;

    private String wrksFullDayCode = null;
    private String ovrWbuName = null;

    private String ovrComments = null;
    private String wrksFlags = null;
    private String wrksUdf1 = null;
    private String wrksUdf2 = null;
    private String wrksUdf3 = null;
    private String wrksUdf4 = null;
    private String wrksUdf5 = null;
    private String wrksUdf6 = null;
    private String wrksUdf7 = null;
    private String wrksUdf8 = null;
    private String wrksUdf9 = null;
    private String wrksUdf10 = null;

    protected ImportData data;
    protected OverrideBuilder ovrBuilder;
    protected String status;
    protected String message;
    private long allStart;

    /**
     * Processes a given row from WBINT_IMPORT.
     *
     * @param data ImportData
     * @param conn DBConnection
     * @throws WBInterfaceException
     * @throws SQLException
     */
    public void process(ImportData data, DBConnection conn) throws
        WBInterfaceException, SQLException {

        if (logger.isInfoEnabled()) {
            logger.info("WorkSummaryTransaction.process");
        }
        this.data = data;
        try {
            preProcess(data, conn);

            validateAndSetFeildValues();
            WBIAGInsertWorkSummaryOverride iwso = createOverride();

            insertOverride(iwso);
            calculateOverrides();

            if (isBatchCalculation()) {
                addToEmpIdAndDates(empId, workDate);
            }

            postProcess(data, data, conn);
        }
        catch (Throwable t) {
            status = ImportData.STATUS_ERROR;
            message = t.getMessage();
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger.error(t);
            }
            WBInterfaceException.throwWBInterfaceException(t);
        }

    }

    /**
     * This should be set to an appropriate status flag after an import of a row
     *
     * @return String
     */
    public String getStatus() {
        log("ImportTransaction.getStatus() returns " + status);
        return status;
    }

    /**
     * Optional status message with regards to row
     *
     * @return String
     */
    public String getMessage() {
        log("ImportTransaction.getMessage");
        return message;
    }

    /**
     * Transaction types are re-used to minimize excessive object creation. This
     * method will be called to tell the class to reset itself back to a pristine
     * state in preparation for the next import row
     */
    public void reset() {
        log("ImportTransaction.reset");
        message = "";
        status = ImportData.STATUS_APPLIED;
    }

    /**
     *
     * @return String
     */
    public String getTaskUI() {
        return null;
    }

    /**
     * This method will be called each time a transaction is initialized
     *
     * @param param HashMap
     */
    public void setTransactionParameters(HashMap param) {
    }

    /**
     * This method will be called each time a transaction is initialized
     *
     * @param conn DBConnection
     * @throws Exception
     */
    public void initializeTransaction(DBConnection conn) throws Exception {
        super.conn = conn;
        allStart = System.currentTimeMillis();
        ovrBuilder = new OverrideBuilder(conn);
    }

    /**
     * This method will be called each time a transaction is finalized
     *
     * @param conn DBConnection
     * @throws Exception
     */
    public void finalizeTransaction(DBConnection conn) throws Exception {
        if (isBatchCalculation()) {
            calculateEmpIdAndDates(false);
        }
        meterTime("All transaction", allStart);
    }

    protected void calculateOverrides() throws WBInterfaceException {
        try {
            ovrBuilder.execute(false);
            ovrBuilder.clear();
        }
        catch (Throwable t) {
            throw new WBInterfaceException(
                "Unable to create override" + t);

        }

    }

    protected WBIAGInsertWorkSummaryOverride createOverride() {
        WBIAGInsertWorkSummaryOverride iwso = new
            WBIAGInsertWorkSummaryOverride(conn);

        if (logger.isInfoEnabled()) {
            logger.info("WorkSummaryTransaction.createOverride");
            logger.info("EMP_ID: " + empId + " WORK_DATE: " + workDate);
        }
        iwso.setEmpId(empId);
        iwso.setOvrType(ovrtypId);
        iwso.setStartDate(workDate);
        iwso.setEndDate(workDate);
        iwso.setWrksClocks(wrksClocks);
        if (! (calcgrpId == Integer.MIN_VALUE)) {
            iwso.setCalcgrpId(calcgrpId);
        }
        if (!StringHelper.isEmpty(paygrpName)) {
            iwso.setWrksPaygrpName(paygrpName);
        }
        iwso.setWrksAuthorized(wrksAuthorized);
        iwso.setWrksFlagBrk(wrksFlagBrk);
        iwso.setWrksFlagRecall(wrksFlagRecall);
        iwso.setWrksInCode(wrksInCode);
        iwso.setWrksOutCode(wrksOutCode);
        iwso.setWrksFullDayCode(wrksFullDayCode);
        iwso.setWbuName(ovrWbuName);
        iwso.setOvrComment(ovrComments);
        if (!StringHelper.isEmpty(wrksFlags)) {
            if (wrksFlags.length() >= 1 &&
                !wrksFlags.substring(0, 1).equalsIgnoreCase(" ")) {
                iwso.setWrksFlag1(wrksFlags.substring(0, 1));
            }
            if (wrksFlags.length() >= 2 &&
                !wrksFlags.substring(1, 2).equalsIgnoreCase(" ")) {
                iwso.setWrksFlag2(wrksFlags.substring(1, 2));
            }
            if (wrksFlags.length() >= 3 &&
                !wrksFlags.substring(2, 3).equalsIgnoreCase(" ")) {
                iwso.setWrksFlag3(wrksFlags.substring(2, 3));
            }
            if (wrksFlags.length() >= 4 &&
                !wrksFlags.substring(3, 4).equalsIgnoreCase(" ")) {
                iwso.setWrksFlag4(wrksFlags.substring(3, 4));
            }
            if (wrksFlags.length() >= 5 &&
                !wrksFlags.substring(4, 5).equalsIgnoreCase(" ")) {
                iwso.setWrksFlag5(wrksFlags.substring(4, 5));
            }
        }
        if (wrksFullDayMinutes != Integer.MIN_VALUE){
            iwso.setWrksFullDayMinutes(new Integer(wrksFullDayMinutes));
        }
        iwso.setWrksUdf1(wrksUdf1);
        iwso.setWrksUdf2(wrksUdf2);
        iwso.setWrksUdf3(wrksUdf3);
        iwso.setWrksUdf4(wrksUdf4);
        iwso.setWrksUdf5(wrksUdf5);
        iwso.setWrksUdf6(wrksUdf6);
        iwso.setWrksUdf7(wrksUdf7);
        iwso.setWrksUdf8(wrksUdf8);
        iwso.setWrksUdf9(wrksUdf9);
        iwso.setWrksUdf10(wrksUdf10);

        return iwso;
    }

    protected void insertOverride(AbstractOverrideOperation ins) throws
        WBInterfaceException {
        ovrBuilder.add(ins);
    }

    protected void validateAndSetFeildValues() throws java.text.ParseException,
        WBInterfaceException, SQLException {

        WBInterfaceUtil.checkForNull(data.getField(EMP_NAME),
                                     "EMP_NAME cannot be null");
        this.empId = WBInterfaceUtil.getEmpIdFromName(data.getField(EMP_NAME),
            conn);

        /**
         * Override type must be between 300 and 399 for Work summary overrides.
         */
        String ovrTypIdStr = data.getField(OVRTYP_ID);
        WBInterfaceUtil.checkForNull(ovrTypIdStr, "OVRTYP_ID must be supplied");
        this.ovrtypId = Integer.parseInt(ovrTypIdStr);
        if ( (ovrtypId >= OverrideData.WORK_SUMMARY_TYPE_START) &&
            (ovrtypId <= OverrideData.WORK_DETAIL_TYPE_END)) {
            throw new WBInterfaceException(
                "This override type id " + ovrtypId + " is not supported");
        }
        WBInterfaceUtil.checkForNull(data.getField(WORK_DATE),
                                     "WORK_DATE cannot be null");
        this.workDate = WBInterfaceUtil.parseDateTime(data.getField(WORK_DATE),
            DATE_FMT, "Error parsing WORK_DATE");

        if (!StringHelper.isEmpty(data.getField(CALCGRP_NAME))) {
            CalcGroupData cgd = getCodeMapper().getCalcGroupByName(data.
                getField(CALCGRP_NAME));
            if (cgd == null) {
                throw new WBInterfaceException(
                    "CALCGRP_NAME not found: " +
                    data.getField(CALCGRP_NAME));
            }
            this.calcgrpId = cgd.getCalcgrpId();
        }

        if (!StringHelper.isEmpty(data.getField(PAYGRP_NAME))) {
            PayGroupData pgd = getCodeMapper().getPayGroupByName(data.getField(
                PAYGRP_NAME));
            if (pgd == null) {
                throw new WBInterfaceException(
                    "PAYGRP_NAME not found: " +
                    data.getField(PAYGRP_NAME));
            }
            this.paygrpName = pgd.getPaygrpName();
        }

        if (!StringHelper.isEmpty(data.getField(WRKS_IN_CODE))) {
            TimeCodeData tgd = getCodeMapper().getTimeCodeByName(data.
                getField(WRKS_IN_CODE));
            if (tgd == null) {
                throw new WBInterfaceException(
                    "WRKS_IN_CODE not found: " +
                    data.getField(WRKS_IN_CODE));
            }
            this.wrksInCode = tgd.getTcodeName();
        }

        if (!StringHelper.isEmpty(data.getField(WRKS_OUT_CODE))) {
            TimeCodeData tgd = getCodeMapper().getTimeCodeByName(data.
                getField(WRKS_OUT_CODE));
            if (tgd == null) {
                throw new WBInterfaceException(
                    "WRKS_OUT_CODE not found: " +
                    data.getField(WRKS_OUT_CODE));
            }
            this.wrksOutCode = tgd.getTcodeName();
        }

        if (!StringHelper.isEmpty(data.getField(WRKS_FULL_DAY_CODE))) {
            TimeCodeData tgd = getCodeMapper().getTimeCodeByName(data.
                getField(WRKS_FULL_DAY_CODE));
            if (tgd == null) {
                throw new WBInterfaceException(
                    "WRKS_FULL_DAY_CODE not found: " +
                    data.getField(WRKS_FULL_DAY_CODE));
            }
            this.wrksFullDayCode = data.getField(WRKS_FULL_DAY_CODE);
        }

        if (!StringHelper.isEmpty(data.getField(WRKS_FULL_DAY_MINUTES))) {
            this.wrksFullDayMinutes = WBInterfaceUtil.getInt(data.getField(
                WRKS_FULL_DAY_MINUTES), "Error parsing WRKS_FULL_DAY_MINUTES");
        }

        if (StringHelper.isEmpty(data.getField(OVR_WBU_NAME))) {
            ovrWbuName = "Work Summary Interface";
        }
        else {
            this.ovrWbuName = data.getField(OVR_WBU_NAME);
        }

        this.wrksClocks = data.getField(WRKS_CLOCKS);
        this.wrksAuthorized = data.getField(WRKS_AUTHORIZED);
        this.wrksFlagBrk = data.getField(WRKS_FLAG_BRK);
        this.wrksFlagRecall = data.getField(WRKS_FLAG_RECALL);
        this.ovrComments = data.getField(OVR_COMMENTS);
        this.wrksFlags = data.getField(WRKS_FLAGS1_5);
        this.wrksUdf1 = data.getField(WRKS_UDF1);
        this.wrksUdf2 = data.getField(WRKS_UDF2);
        this.wrksUdf3 = data.getField(WRKS_UDF3);
        this.wrksUdf4 = data.getField(WRKS_UDF4);
        this.wrksUdf5 = data.getField(WRKS_UDF5);
        this.wrksUdf6 = data.getField(WRKS_UDF6);
        this.wrksUdf7 = data.getField(WRKS_UDF7);
        this.wrksUdf8 = data.getField(WRKS_UDF8);
        this.wrksUdf9 = data.getField(WRKS_UDF9);
        this.wrksUdf10 = data.getField(WRKS_UDF10);

    }
}
