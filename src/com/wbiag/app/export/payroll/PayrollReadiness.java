package com.wbiag.app.export.payroll;

import java.sql.*;
import java.lang.String;
import java.util.Date;
import java.util.*;

import org.apache.log4j.Level;

import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.model.PayGroupData;
import com.wbiag.app.ta.model.CustomerRegistryData;
import com.workbrain.app.ta.db.PayGroupAccess;
import com.wbiag.app.ta.db.CustomerRegistryAccess;

/**
 * 
 * <p>
 * Title: Payroll Readiness
 * </p>
 * <p>
 * Description: Tools used to check paygroups are read, set and reset readiness.
 * </p>
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * <p>
 * Company: Workbrain
 * </p>
 * 
 * @cleigh
 * @version 1.0
 */
public class PayrollReadiness {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger
            .getLogger(PayrollReadiness.class);

    private static final String MARK_INPROGRESS = "I";
    private static final String MARK_READY = "Y";

    private String ON_BOARD_FIELD = "EMP_VAL1";
    private String PAID_WRKS_FLAG = "WRKS_FLAG1";
    private String PAID_WRKDA_FLAG = "wrkda_flag1";
    private String PAID_WRKD_FLAG = "wrkd_flag1";
    private String PAYGRP_STATUS_FLAG = "PAYGRP_FLAG1";
    private String EMP_ON_BOARD_FLAG = "EMP_FLAG1";
    private String PAYGRP_STATUS_UDF = "PAYGRP_UDF1";
    private String HANDS_OFF_UDF = "PAYGRP_UDF2";
    private String ENABLE_ON_BOARD = "TRUE";

    private static final String REG_PATH = "system/customer/export/";
    private static final String REG_ON_BOARD_FIELD = "ON_BOARD_FIELD";
    private static final String REG_PAID_WRKS_FLAG = "PAID_WRKS_FLAG";
    private static final String REG_PAID_WRKDA_FLAG = "PAID_WRKDA_FLAG";
    private static final String REG_PAID_WRKD_FLAG = "PAID_WRKD_FLAG";
    private static final String REG_PAYGRP_STATUS_FLAG = "PAYGRP_STATUS_FLAG";
    private static final String REG_PAYGRP_STATUS_UDF = "PAYGRP_STATUS_UDF";
    private static final String REG_EMP_ON_BOARD_FLAG = "EMP_ON_BOARD_FLAG";
    private static final String REG_HANDS_OFF_UDF = "HANDS_OFF_UDF";
    private static final String REG_ENABLE_ON_BOARD = "ENABLE_ON_BOARD";
    
    
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd hh:mm:ss";
    private static final String TIMESTAMP_FORMAT_NO_TIME = "yyyy-MM-dd 00:00:00";
    
    private static final String READY_PAYGROUPS_SQL = "select paygrp_id from pay_group where ";

    private static final int REGULAR = 0;

    private static final int ON_BOARD = 1;

    private static final int TERM = 2;

    private static final int RETRO = 3;

    private static final int RESET = 4;

    //String Literals, used for increased efficiency
    private static final String EMP_NAME = " emp_name ";
    private static final String EMP_ID = " emp_id ";

    private static final String AND_EMP_ID = " and emp_id in (";

    private static final String NOT_Y = " <> 'Y' ";

    private static final String QUOTE_N = "'N'";

    private DBConnection conn;
    private boolean lockRecords = false;
    private boolean enableOnBoard = true;
    

    private static final String DATE_FUN = " date(";
    private static final String SANDS = " AND ";
    private static final String TO_DATE_FUN = " to_date( ";
    private static final String BETWEEN = " between ";
    private static final String ORACLE_OBD_FORMAT = "'MM/dd/yyyy'";

    /**
     * Method to set lock records option. If Lock records is
     * set to true then the hands off date will be moved to the
     * end of the pay period when setFlag is called.
     * 
     * @param b
     * 
     */
    public void setLockRecords(boolean b){
        lockRecords = b;
    }
    
    /**
     * constructor
     */
    public PayrollReadiness(DBConnection c) {
        if (logger.isDebugEnabled()) {
            logger.debug("Payroll Readiness Constructor");
        }
        this.conn = c;
        initRegistryVariables();
    }

    private void initRegistryVariables() {
        CustomerRegistryAccess cbra = new CustomerRegistryAccess(conn);
        CustomerRegistryData cbd = null;

        if (logger.isDebugEnabled()) {
            logger.debug("Getting Registry Settings");
        }
        
        cbd = cbra.loadParameterByName(REG_ENABLE_ON_BOARD);
        if (cbd != null) {
            if (cbd.getWbregValue() != null) {
                ENABLE_ON_BOARD = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuffer().append("Setting ").append(
                            REG_ENABLE_ON_BOARD).append(" to ").append(
                            ENABLE_ON_BOARD));
                }
            }
        }

        if ("false".equalsIgnoreCase(ENABLE_ON_BOARD)) {
            enableOnBoard = false;
        } 
        else if (logger.isDebugEnabled()){
           logger.debug("On Boarding Enabled"); 
        }

        cbd = cbra.loadParameterByName( REG_ON_BOARD_FIELD);
        if (cbd != null) {
            if (cbd.getWbregValue() != null) {
                ON_BOARD_FIELD = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuffer().append("Setting ").append(
                        REG_ON_BOARD_FIELD).append(" to ").append(
                        ON_BOARD_FIELD));
                }
            }
        }

        cbd = cbra.loadParameterByName(REG_PAID_WRKS_FLAG);
        if (cbd != null) {
            if (cbd.getWbregValue() != null) {
                PAID_WRKS_FLAG = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuffer().append("Setting ").append(
                        REG_PAID_WRKS_FLAG).append(" to ").append(
                        PAID_WRKS_FLAG));
                }
            }
        }

        cbd = cbra.loadParameterByName( REG_PAID_WRKDA_FLAG);
        if (cbd != null) {
            if (cbd.getWbregValue() != null) {
                PAID_WRKDA_FLAG = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuffer().append("Setting ").append(
                        REG_PAID_WRKDA_FLAG).append(" to ").append(
                        PAID_WRKDA_FLAG));
                }
            }
        }
        
        cbd = cbra.loadParameterByName( REG_PAID_WRKD_FLAG);
        if (cbd != null) {
            if (cbd.getWbregValue() != null) {
                PAID_WRKD_FLAG = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuffer().append("Setting ").append(
                        REG_PAID_WRKD_FLAG).append(" to ").append(
                        PAID_WRKD_FLAG));
                }
            }
        }

        cbd = cbra.loadParameterByName( REG_PAYGRP_STATUS_FLAG);
        if (cbd != null) {
            if (cbd.getWbregValue() != null) {
                PAYGRP_STATUS_FLAG = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuffer().append("Setting ").append(
                        REG_PAYGRP_STATUS_FLAG).append(" to ").append(
                        PAYGRP_STATUS_FLAG));
                }
            }
        }
            

        cbd = cbra.loadParameterByName(REG_PAYGRP_STATUS_UDF);
        if (cbd != null) {
            if (cbd.getWbregValue() != null) {
                PAYGRP_STATUS_UDF = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuffer().append("Setting ").append(
                        REG_PAYGRP_STATUS_UDF).append(" to ").append(
                        PAYGRP_STATUS_UDF));
                }
            }
        }

        cbd = cbra.loadParameterByName(REG_EMP_ON_BOARD_FLAG);
        if (cbd != null) {
            if (cbd.getWbregValue() != null) {
                EMP_ON_BOARD_FLAG = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuffer().append("Setting ").append(
                        REG_EMP_ON_BOARD_FLAG).append(" to ").append(
                        EMP_ON_BOARD_FLAG));
                }
            }
        }
        
        cbd = cbra.loadParameterByName(REG_EMP_ON_BOARD_FLAG);
        if (cbd != null) {
            if (cbd.getWbregValue() != null) {
                EMP_ON_BOARD_FLAG = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuffer().append("Setting ").append(
                        REG_EMP_ON_BOARD_FLAG).append(" to ").append(
                        EMP_ON_BOARD_FLAG));
                }
            }
        }
        
        cbd = cbra.loadParameterByName(REG_HANDS_OFF_UDF);
        if (cbd != null) {
            if (cbd.getWbregValue() != null) {
                HANDS_OFF_UDF = cbd.getWbregValue();
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuffer().append("Setting ").append(
                            REG_HANDS_OFF_UDF).append(" to ").append(
                                    HANDS_OFF_UDF));
                }
            }
        }
    }

    /**
     * If all records for the given pay period over the current pay period dates
     * are authorized then the value true is returned.
     * 
     * @param conn
     *            DBConnection
     * @param pgId
     *            int
     * @throws Exception
     * @return boolean
     */
    public boolean checkCurrentPayPeriodUnauth(int pgId) throws Exception {

        if (logger.isInfoEnabled()){
            logger.info("Checking Current Pay Period Unauth");
        }
        PayGroupAccess pa = new PayGroupAccess(conn);
        PayGroupData pd = pa.load(pgId);

        String startDateString = DateHelper.convertDateString(pd
                .getPaygrpStartDate(), TIMESTAMP_FORMAT_NO_TIME);
        String endDateString = DateHelper.convertDateString(pd
                .getPaygrpStartDate(), TIMESTAMP_FORMAT_NO_TIME);

        java.sql.Timestamp pgStartDate = java.sql.Timestamp
                .valueOf(startDateString);
        java.sql.Timestamp pgEndDate = java.sql.Timestamp
                .valueOf(endDateString);

        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs1 = null;
        ResultSet rs2 = null;

        String SQL1 = querySetup1(EMP_NAME, REGULAR, null);
        String SQL2 = querySetup2(EMP_ID, REGULAR, null);
        
        if (logger.isDebugEnabled()){
            logger.debug(new StringBuffer().append("Checking for unauth records with SQL1: ").append(SQL1)
                    .append(" \n SQL2:").append(SQL2));
        }
        
        boolean result = true;
        try {
            ps1 = conn.prepareStatement(SQL1);
            ps1.setInt(1, pgId);
            ps1.setTimestamp(2, pgStartDate);
            ps1.setTimestamp(3, pgEndDate);
            rs1 = ps1.executeQuery();

            ps2 = conn.prepareStatement(SQL2);
            ps2.setInt(1, pgId);
            ps2.setTimestamp(2, pgStartDate);
            ps2.setTimestamp(3, pgEndDate);
            rs2 = ps2.executeQuery();

            //	If Any records are returnded then return false
            if (rs1.next() || rs2.next()) {
                if (logger.isDebugEnabled()){
                    logger.debug("Unauthorized records found");
                }
                result = false;
            }
        } finally {
            if (ps1 != null) {
                ps1.close();
            }
            if (rs1 != null) {
                rs1.close();
            }
            if (ps2 != null) {
                ps2.close();
            }
            if (rs2 != null) {
                rs2.close();
            }
        }

        return result;

    }

    /**
     * If all records for the given pay group across the given dates are
     * authorized then the value true it returned.
     * 
     * @param conn
     *            DBConnection
     * @param pgId
     *            int
     * @param startDate
     *            Date
     * @param endDate
     *            Date
     * @throws Exception
     * @return boolean
     */
    public boolean checkGivenDatesUnauth(int pgId, Date startDate, Date endDate)
            throws Exception {
        
        if (startDate == null || endDate == null) {
            if (logger.isEnabledFor(Level.ERROR)){
                logger.error("Dates are invalid or not set");
            }
            throw new Exception("Dates are invalid or not set");
        }
        String startDateString = DateHelper.convertDateString(startDate,
                TIMESTAMP_FORMAT_NO_TIME);
        String endDateString = DateHelper.convertDateString(endDate,
                TIMESTAMP_FORMAT_NO_TIME);
        return checkGivenDatesUnauth(pgId, java.sql.Timestamp
                .valueOf(startDateString), java.sql.Timestamp
                .valueOf(endDateString));
    }

    private boolean checkGivenDatesUnauth(int pgId,
            java.sql.Timestamp startDate, java.sql.Timestamp endDate)
            throws Exception {
        if (logger.isInfoEnabled()){
            logger.info(new StringBuffer().append("Checking Unauth given dates ").append(startDate)
                    .append(" - ").append(endDate));
        }
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs1 = null;
        ResultSet rs2 = null;

        String SQL1 = querySetup1(EMP_NAME, REGULAR, null);
        String SQL2 = querySetup2(EMP_ID, REGULAR, null);
        if (logger.isDebugEnabled()){
            logger.debug(new StringBuffer().append("Checking for unauth records with SQL1: ").append(SQL1)
                    .append(" \n SQL2:").append(SQL2));
        }
        boolean result = true;
        try {
            ps1 = conn.prepareStatement(SQL1);
            ps1.setInt(1, pgId);
            ps1.setTimestamp(2, startDate);
            ps1.setTimestamp(3, endDate);
            rs1 = ps1.executeQuery();

            ps2 = conn.prepareStatement(SQL2);
            ps2.setInt(1, pgId);
            ps2.setTimestamp(2, startDate);
            ps2.setTimestamp(3, endDate);
            rs2 = ps2.executeQuery();

            //If Any records are returnded then return false
            if (rs1.next() || rs2.next()) {
                if (logger.isDebugEnabled()){
                    logger.debug("Unauthorized records found");
                }
                result = false;
            }
        } finally {
            if (ps1 != null) {
                ps1.close();
            }
            if (rs1 != null) {
                rs1.close();
            }
            if (ps2 != null) {
                ps2.close();
            }
            if (rs2 != null) {
                rs2.close();
            }
        }

        return result;

    }

    /**
     * If all retro adjustment records for given pay group, dates and employees
     * are unpaid (PAID_WRKD_FLAG != 'Y') and the employee has been on boarded
     * and are authorized then true is returned.
     * 
     * @param conn
     *            DBConnection
     * @param pgId
     *            int
     * @param empIds
     *            int[]
     * @param startDate
     *            Date
     * @param endDate
     *            Date
     * @throws Exception
     * @return boolean
     */
    public boolean checkRetroUnauth(int pgId, int[] empIds, Date startDate,
            Date endDate) throws Exception {
        String startDateString = DateHelper.convertDateString(startDate,
                TIMESTAMP_FORMAT_NO_TIME);
        String endDateString = DateHelper.convertDateString(endDate,
                TIMESTAMP_FORMAT_NO_TIME);
        return checkRetroUnauth(pgId, empIds, java.sql.Timestamp
                .valueOf(startDateString), java.sql.Timestamp
                .valueOf(endDateString));
    }

    public boolean checkRetroUnauth(int pgId, int[] empIds,
            java.sql.Timestamp startDate, java.sql.Timestamp endDate)
            throws Exception {
        if (logger.isInfoEnabled()){
            logger.info(new StringBuffer().append("Checking for Retro Unauth records ")
                    .append(startDate).append(" - ").append(endDate));
        }
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs1 = null;
        ResultSet rs2 = null;

        String SQL1 = querySetup1(EMP_NAME, RETRO, empIds);
        String SQL2 = querySetup2(EMP_ID, RETRO, empIds);
        if (logger.isDebugEnabled()){
            logger.debug(new StringBuffer().append("Checking for unauth records with SQL1: ").append(SQL1)
                    .append(" \n SQL2:").append(SQL2));
        }
        boolean result = true;
        try {
            ps1 = conn.prepareStatement(SQL1);
            ps1.setInt(1, pgId);
            ps1.setTimestamp(2, startDate);
            ps1.setTimestamp(3, endDate);
            rs1 = ps1.executeQuery();

            ps2 = conn.prepareStatement(SQL2);
            ps2.setInt(1, pgId);
            ps2.setTimestamp(2, startDate);
            ps2.setTimestamp(3, endDate);
            rs2 = ps2.executeQuery();

            //	If Any records are returnded then return false
            if (rs1.next() || rs2.next()) {
                if (logger.isDebugEnabled()){
                    logger.debug("Unauthorized records found");
                }
                result = false;
            }
        } finally {
            if (ps1 != null) {
                ps1.close();
            }
            if (rs1 != null) {
                rs1.close();
            }
            if (ps2 != null) {
                ps2.close();
            }
            if (rs2 != null) {
                rs2.close();
            }
        }

        return result;

    }

    /**
     * If all records for employees who were terminated during the current pay
     * period are authorized then the value true it returned.
     * 
     * @param conn
     *            DBConnection
     * @param pgId
     *            int
     * @throws Exception
     * @return boolean
     */
    public boolean checkTermUnauth(int pgId) throws Exception {

        if(logger.isInfoEnabled()){
            logger.info("Checking for Term unauth records ");
        }
        
        PayGroupAccess pa = new PayGroupAccess(conn);
        PayGroupData pd = pa.load(pgId);

        String startDateString = DateHelper.convertDateString(pd
                .getPaygrpStartDate(), TIMESTAMP_FORMAT_NO_TIME);
        String endDateString = DateHelper.convertDateString(pd
                .getPaygrpStartDate(), TIMESTAMP_FORMAT_NO_TIME);

        java.sql.Timestamp pgStartDate = java.sql.Timestamp
                .valueOf(startDateString);
        java.sql.Timestamp pgEndDate = java.sql.Timestamp
                .valueOf(endDateString);

        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs1 = null;
        ResultSet rs2 = null;

        String SQL1 = querySetup1(EMP_NAME, TERM, null);
        String SQL2 = querySetup2(EMP_ID, TERM, null);
        if (logger.isDebugEnabled()){
            logger.debug(new StringBuffer().append("Checking for unauth records with SQL1: ").append(SQL1)
                    .append(" \n SQL2:").append(SQL2));
        }
        boolean result = true;
        try {
            ps1 = conn.prepareStatement(SQL1);
            ps1.setInt(1, pgId);
            ps1.setTimestamp(2, pgStartDate);
            ps1.setTimestamp(3, pgEndDate);
            ps1.setTimestamp(4, pgStartDate);
            ps1.setTimestamp(5, pgEndDate);
            rs1 = ps1.executeQuery();

            ps2 = conn.prepareStatement(SQL2);
            ps2.setInt(1, pgId);
            ps2.setTimestamp(2, pgStartDate);
            ps2.setTimestamp(3, pgEndDate);
            ps2.setTimestamp(4, pgStartDate);
            ps2.setTimestamp(5, pgEndDate);
            rs2 = ps2.executeQuery();

            //If Any records are returnded then return false
            if (rs1.next() || rs2.next()) {
                if (logger.isDebugEnabled()){
                    logger.debug("Unauthorized records found");
                }
                result = false;
            }
        } finally {
            if (ps1 != null) {
                ps1.close();
            }
            if (rs1 != null) {
                rs1.close();
            }
            if (ps2 != null) {
                ps2.close();
            }
            if (rs2 != null) {
                rs2.close();
            }
        }

        return result;

    }

    /**
     * If all records for New employees from the date of hire to the end of the
     * current pay period are authorized then the value true it returned.
     * 
     * An Employee is New if EMP_ON_BOARD_FLAG = 'N' and ON_BOARD_FIELD is
     * between the pay period start date + lookbackdays and pay period end date.
     * 
     * note: lookbackdays should be a negitive value.
     * 
     * @param conn
     *            DBConnection
     * @param pgId
     *            int
     * @param lookBackDays
     *            int
     * @throws Exception
     * @return boolean
     */

    public boolean checkNewEmpUnauth(int pgId, int lookBackDays)
            throws Exception {
        
        if (!enableOnBoard){
            throw new Exception("Can not check unauth, Employee on boarding not enabled.");
        }
        Date obStartDate;
        Date obEndDate;
        Date exportStart;
        Date exportEnd;

        PayGroupAccess pa = new PayGroupAccess(conn);
        PayGroupData pd = pa.load(pgId);
        
        if (lookBackDays > 0){
            lookBackDays = lookBackDays*-1;
        }
        obStartDate = DateHelper.addDays(pd.getPaygrpStartDate(), lookBackDays);
        obEndDate = pd.getPaygrpEndDate();

        exportStart = getMinHireDate(pgId, obStartDate, obEndDate);

        exportEnd = DateHelper.addDays(pd.getPaygrpStartDate(), -1);

        if (exportStart != null && exportStart.before(exportEnd)) {
            return checkNewEmpUnauth(pgId, exportStart, exportEnd, obStartDate,
                    obEndDate);
        } else {
            if (logger.isDebugEnabled()){
                logger.debug("No new employee records to check");
            }
            return true;
        }

    }

    public boolean checkNewEmpUnauth(int pgId, Date startDate, Date endDate,
            Date oBStartDate, Date oBEndDate) throws

    Exception {
        
        if (!enableOnBoard){
            throw new Exception("Can not check unauth, Employee on boarding not enabled.");
        }
        
        String startDateString = DateHelper.convertDateString(startDate,
                TIMESTAMP_FORMAT_NO_TIME);
        String endDateString = DateHelper.convertDateString(endDate,
                TIMESTAMP_FORMAT_NO_TIME);
        String oBStartDateString = DateHelper.convertDateString(oBStartDate,
                TIMESTAMP_FORMAT_NO_TIME);
        String oBEndDateString = DateHelper.convertDateString(oBEndDate,
                TIMESTAMP_FORMAT_NO_TIME);

        return checkNewEmpUnauth(pgId, java.sql.Timestamp
                .valueOf(startDateString), java.sql.Timestamp
                .valueOf(endDateString), java.sql.Timestamp
                .valueOf(oBStartDateString), java.sql.Timestamp
                .valueOf(oBEndDateString));
    }

    public boolean checkNewEmpUnauth(int pgId, java.sql.Timestamp startDate,
            java.sql.Timestamp endDate, java.sql.Timestamp oBStartDate,
            java.sql.Timestamp oBEndDate) throws Exception {
        
        if (!enableOnBoard){
            throw new Exception("Can not check unauth, Employee on boarding not enabled.");
        }
        
        if (logger.isInfoEnabled()){
            logger.info(new StringBuffer().append("Checking for New employee unauth records")
                    .append(startDate).append(" - ").append(endDate).append(" OB Date ")
                    .append(oBStartDate).append(" - ").append(oBEndDate));
        }
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs1 = null;
        ResultSet rs2 = null;

        String SQL1 = querySetup1(EMP_NAME, ON_BOARD, null);
        String SQL2 = querySetup2(EMP_ID, ON_BOARD, null);
        if (logger.isDebugEnabled()){
            logger.debug(new StringBuffer().append("Checking for unauth records with SQL1: ").append(SQL1)
                    .append(" \n SQL2:").append(SQL2));
        }
        boolean result = true;
        try {
            ps1 = conn.prepareStatement(SQL1);
            ps1.setInt(1, pgId);
            ps1.setTimestamp(2, startDate);
            ps1.setTimestamp(3, endDate);
            ps1.setTimestamp(4, oBStartDate);
            ps1.setTimestamp(5, oBEndDate);
            rs1 = ps1.executeQuery();

            ps2 = conn.prepareStatement(SQL2);
            ps2.setInt(1, pgId);
            ps2.setTimestamp(2, startDate);
            ps2.setTimestamp(3, endDate);
            ps2.setTimestamp(4, oBStartDate);
            ps2.setTimestamp(5, oBEndDate);
            rs2 = ps2.executeQuery();

            //If Any records are returnded then return false
            if (rs1.next() || rs2.next()) {
                if (logger.isDebugEnabled()){
                    logger.debug("Unauthorized records found");
                }
                result = false;
            }
        } finally {
            if (ps1 != null) {
                ps1.close();
            }
            if (rs1 != null) {
                rs1.close();
            }
            if (ps2 != null) {
                ps2.close();
            }
            if (rs2 != null) {
                rs2.close();
            }
        }

        return result;

    }

    /**
     * Will call checkNewEmpUnauth and checkCurrentPayPeriodUnauth for
     * unauthorized records.
     * 
     * @param conn
     *            DBConnection
     * @param pgIds
     *            int[]
     * @param lookBackDays
     *            int
     * @throws Exception
     * @return boolean
     */
    public boolean checkAllUnauth(int[] pgIds, int lookBackDays)
            throws Exception {

        for (int i = 0; i < pgIds.length; i++) {
            //Check Regular paygroups
            if (!checkCurrentPayPeriodUnauth(pgIds[i])) {
                return false;
            }
            if (enableOnBoard){
	            if (!checkNewEmpUnauth(pgIds[i], lookBackDays)) {
	                return false;
	            }
            }
        }
        return true;

    }

    /**
     * Will call checkGivenDatesUnauth and checkNewEmpUnauth to check for
     * unauthorized records.
     * 
     * @param conn
     *            DBConnection
     * @param pgIds
     *            int[]
     * @param startDate
     *            Date
     * @param endDate
     *            Date
     * @param lookBackDays
     *            int
     * @throws Exception
     * @return boolean
     */
    public boolean checkAllUnauth(int[] pgIds, Date startDate, Date endDate,
            int lookBackDays) throws Exception {

        for (int i = 0; i < pgIds.length; i++) {
            //Check Regular paygroups
            if (!checkGivenDatesUnauth(pgIds[i], startDate, endDate)) {
                return false;
            }
            if (enableOnBoard){
	            if (!checkNewEmpUnauth(pgIds[i], lookBackDays)) {
	                return false;
	            }
            }
        }
        return true;
    }

    /**
     * Returns true if PAYGRP_STATUS_FLAG != N or I
     * 
     * @param conn
     *            DBConnection
     * @param payGrps
     *            int[]
     * @return boolean
     */
    public boolean checkPaygrpsReady(int[] payGrps) {

        boolean result = true;
        
        if (logger.isInfoEnabled()){
            logger.info("Checking Pay groups Ready");
        }
        ArrayList readyGroups = new ArrayList();
        
        //Check that paygrps are ready for export
        PayGroupAccess pga = new PayGroupAccess(conn);
        PayGroupData pgd = null;
        for (int i = 0; i < payGrps.length; i++) {

            pgd = pga.load(payGrps[i]);
            String status;
            if (pgd.getField(PAYGRP_STATUS_FLAG) != null) {
                status = pgd.getField(PAYGRP_STATUS_FLAG).toString();
                if (!status.equalsIgnoreCase(MARK_READY)) {
                    if (logger.isDebugEnabled()){
                        logger.debug("Unready pay groups found: " + pgd.getPaygrpId());
                    }
                    result = false;
                } else {
                    readyGroups.add(new Integer(payGrps[i]));
                }
            } else {
                if (logger.isDebugEnabled()){
                    logger.debug("unready pay groups found");
                }
                result = false;
            }
        }
        
        if (!result){
            payGrps = new int[readyGroups.size()];
            for (int i = 0; i < readyGroups.size(); i++) {
                    payGrps[i] = ((Integer) readyGroups.get(i)).intValue();
            }
        }

        return result;
    }
    
    

    /**
     * Return an int array of paygroups with PAYGRP_STATUS_FLAG = Y or E
     * 
     * @param conn
     *            DBConnection
     * @return int[]
     */
    public int[] getReadyPaygrps() throws SQLException {
        return getReadyPaygrps(null);
    }
    
    /**
     * Return an int array of paygroups with 
     * PAYGRP_STATUS_FLAG = Y or E and condition where
     * 
     * @param conn
     *            DBConnection
     * @return int[]
     */
    public int[] getReadyPaygrps(String where) throws SQLException {
        StringBuffer readyWhereSql = new StringBuffer();
        
        DBServer dbs = DBServer.getServer(conn);
        readyWhereSql.append(dbs.encodeNullCheck(PAYGRP_STATUS_FLAG, QUOTE_N));
        readyWhereSql.append(" = 'Y' ");
        if (where != null){
            readyWhereSql.append(where);
        }
        
        return getReadyPaygrpsWhere(readyWhereSql.toString());
    }
    
    /**
     * Return an int array of paygroups where must not be null.
     * 
     * @param conn
     *            DBConnection
     * @return int[]
     */
    public int[] getReadyPaygrpsWhere(String where) throws SQLException {
        if (logger.isDebugEnabled()){
            logger.debug("Payrollreadiness: getReadyPaygrps Begin");
            logger.debug("Setting condition where to "+ where);
        }
        StringBuffer readySql = new StringBuffer();
        
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        ArrayList pgs = new ArrayList();
        
        try{
            
            readySql.append(READY_PAYGROUPS_SQL);
            if (where != null){
                readySql.append(where);
            } else{
                throw new SQLException("Ready where statement must not be null");
            }
            if (logger.isDebugEnabled()){
                logger.debug("Getting Ready Pay Groups using SQL: " + readySql.toString());
            }
            ps = conn.prepareStatement(readySql.toString());
            rs = ps.executeQuery();
            
            while (rs.next()){
                pgs.add(rs.getObject(1));
            }
            
        }catch (SQLException e){
            logger.error("Error could not get list of ready Pay groups: " + readySql.toString());
            throw e;
        }
        finally {
            if (ps != null){
                ps.close();
            }
            if (rs != null){
                rs.close();
            }
                
        }
        
        int[] payGrpsInt = new int[pgs.size()];
        for (int i =0; i < pgs.size(); i++){
            payGrpsInt[i] = Integer.parseInt(pgs.get(i).toString());
        }
        if (logger.isDebugEnabled()){
            logger.debug("Number of ready paygroups found :"+ pgs.size());
            logger.debug("Payrollreadiness: getReadyPaygrps End");
        }
        return payGrpsInt;
    }

    /**
     * Sets all Paygroups PAYGRP_STATUS_FLAG = 'N'
     * 
     * @param conn
     *            DBConnection
     * @return boolean
     */
    public boolean resetReadiness() throws SQLException {
        PreparedStatement ps = null;
        try {
            StringBuffer resetSQL = new StringBuffer();
            resetSQL.append("UPDATE PAY_GROUP SET ").append(PAYGRP_STATUS_FLAG)
                    .append(" = ? ");
            ps = conn.prepareStatement(resetSQL.toString());
            ps.setString(1, "N");
            ps.executeUpdate();
        } catch (java.sql.SQLException t) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger
                        .error(
                                "com.workbrain.app.bo.ejb.actions.ReadinessPayrollAction.class",
                                t);
            }
            throw t;
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
        return true;

    }

    private String querySetup1(String s, int cycleId, int[] empIds)  throws Exception  {
        String whereCurrentThis = getWhere(cycleId, empIds, true);
        return "SELECT"
                + s
                + "\nFROM view_payexp_current"
                + "\n WHERE paygrp_id=? "
                + "\n   AND wrks_authorized = "
                + QUOTE_N
                + "\n   AND wrks_work_date between ? and ? "
                + (!StringHelper.isEmpty(whereCurrentThis) ? "\n   AND "
                        + whereCurrentThis : "")
                + "\n ORDER BY emp_id, wrks_work_date ";
    }

    private String querySetup2(String s, int cycleId, int[] empIds)  throws Exception  {
        String whereAdjustmentThis = getWhere(cycleId, empIds, false);
        return "SELECT"
                + s
                + "\nFROM work_summary, work_detail, view_retro_adjustments"
                + "\n WHERE work_summary.wrks_id = view_retro_adjustments.wrks_id"
                + "\n AND work_summary.wrks_id = work_detail.WRKS_ID"
                + "\n AND paygrp_id=? "
                + "\n   AND wrks_authorized = "
                + QUOTE_N
                + "\n   AND wrkda_adjust_date between ? and ? "
                + (!StringHelper.isEmpty(whereAdjustmentThis) ? "\n   AND "
                        + whereAdjustmentThis : "")
                + "\n ORDER BY emp_id, wrkd_work_date ";
    }

    private String getWhere(int cycleId, int[] empIds, boolean current) throws Exception  {
        DBServer dbs = DBServer.getServer(conn);
        StringBuffer pgWhereCurrent = new StringBuffer();
        StringBuffer pgWhereAdj = new StringBuffer();
        String encodedOnboardDate = dbs.encodeNullCheck(ON_BOARD_FIELD,
                "'" +
                DateHelper.
                convertDateString(DateHelper.
                                  DATE_3000, TIMESTAMP_FORMAT) + "'");

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
            if (empIds != null) {
                pgWhereCurrent.append(AND_EMP_ID);
                for (int i = 0; i < empIds.length; i++) {
                    pgWhereCurrent.append(String.valueOf(empIds[i]));
                    if (i != empIds.length - 1) {
                        pgWhereCurrent.append(", ");
                    }
                }
                pgWhereCurrent.append(") ");
            }
            if (current) {
                return (pgWhereCurrent.toString());
            } else {
                
                pgWhereAdj.append(dbs.encodeNullCheck(PAID_WRKD_FLAG, QUOTE_N));
                pgWhereAdj.append(NOT_Y);
                if (enableOnBoard) {
	                pgWhereAdj.append(AND_EMP_ID);
	                pgWhereAdj.append(" SELECT EMP_ID FROM EMPLOYEE WHERE ");
	                pgWhereAdj.append(dbs.encodeNullCheck(EMP_ON_BOARD_FLAG,
	                    QUOTE_N));
	                pgWhereAdj.append(" = 'Y' ) ");
                }

                if (empIds != null) {
                    pgWhereAdj.append(AND_EMP_ID);
                    for (int i = 0; i < empIds.length; i++) {
                        pgWhereAdj.append(String.valueOf(empIds[i]));
                        if (i != empIds.length - 1) {
                            pgWhereAdj.append(", ");
                        }
                    }
                    pgWhereAdj.append(") ");
                }
                return pgWhereAdj.toString();
            }
        }
        case ON_BOARD: {
            if (!enableOnBoard){
                throw new Exception("Invalid cycle, Employee on boarding not enabled.");
            }
            if (current) {
                pgWhereCurrent
                    	.append(" EMP_ID in (select EMP_ID from EMPLOYEE where ");
            //	DB Speciffic
                if (dbs.isOracle()) {
                    pgWhereCurrent.append(" to_date( ");
                    pgWhereCurrent.append(ON_BOARD_FIELD);
                    pgWhereCurrent.append(", 'MM/dd/yyyy') ");
                } else if (dbs.isDB2()) {
                    pgWhereCurrent.append(" date(");
                    pgWhereCurrent.append(ON_BOARD_FIELD);
                    pgWhereCurrent.append(") ");
                }
            // 	----------
                pgWhereCurrent.append(" between ");
                pgWhereCurrent.append(" ? and ? ");
                pgWhereCurrent.append(" and ");
                pgWhereCurrent.append(dbs.encodeNullCheck(EMP_ON_BOARD_FLAG,
                    QUOTE_N));
                pgWhereCurrent.append(" <> 'Y' ) AND ");
                pgWhereCurrent.append(dbs.encodeNullCheck(PAID_WRKS_FLAG, QUOTE_N));
                pgWhereCurrent.append(NOT_Y);

                if (empIds != null) {
                    pgWhereCurrent.append(AND_EMP_ID);
                    for (int i = 0; i < empIds.length; i++) {
                        pgWhereCurrent.append(String.valueOf(empIds[i]));
                        if (i != empIds.length - 1) {
                            pgWhereCurrent.append(", ");
                        }
                    }
                    pgWhereCurrent.append(") ");
                }
                
                return pgWhereCurrent.toString();
            } else {
                pgWhereAdj.append(
                " EMP_ID in (select EMP_ID from EMPLOYEE where ");
//DB Speciffic
            if (dbs.isOracle()) {
                pgWhereAdj.append(TO_DATE_FUN);
                pgWhereAdj.append(ON_BOARD_FIELD);
                pgWhereAdj.append(", ");
                pgWhereAdj.append(ORACLE_OBD_FORMAT);
                pgWhereAdj.append(")");
            }
            else if (dbs.isDB2()) {
                pgWhereAdj.append(DATE_FUN);
                pgWhereAdj.append(encodedOnboardDate);
                pgWhereAdj.append(") ");
            }
//----------
            pgWhereAdj.append(BETWEEN);
            pgWhereAdj.append(" ? and ? ");
            pgWhereAdj.append(SANDS);
            pgWhereAdj.append(dbs.encodeNullCheck(EMP_ON_BOARD_FLAG,
                QUOTE_N));
            pgWhereAdj.append(" <> 'Y' ) ");
            pgWhereAdj.append(SANDS);
            pgWhereAdj.append(dbs.encodeNullCheck(PAID_WRKD_FLAG, QUOTE_N));
            pgWhereAdj.append(NOT_Y);

            if (empIds != null) {
                pgWhereAdj.append(AND_EMP_ID);
                for (int i = 0; i < empIds.length; i++) {
                    pgWhereAdj.append(String.valueOf(empIds[i]));
                    if (i != empIds.length - 1) {
                        pgWhereAdj.append(", ");
                    }
                }
                pgWhereAdj.append(") ");
            }
                
                return pgWhereAdj.toString();
            }
        }
        case TERM: {
            pgWhereCurrent
                    .append(" EMP_ID in (select EMP_ID from EMPLOYEE where ");
            //DB Speciffic
            pgWhereCurrent.append(" EMP_TERMINATION_DATE ");
            // ----------
            pgWhereCurrent.append(" between ");
            pgWhereCurrent.append(" ? and ? ");
            
            if (enableOnBoard) {
                pgWhereCurrent.append(SANDS);
	            pgWhereCurrent.append(dbs.encodeNullCheck(EMP_ON_BOARD_FLAG,
	                    QUOTE_N));
	            pgWhereCurrent.append(" = 'Y' ");
            }
            pgWhereCurrent.append(" ) \n AND ");
            pgWhereCurrent.append(dbs.encodeNullCheck(PAID_WRKS_FLAG, QUOTE_N));
            pgWhereCurrent.append(NOT_Y);

            if (empIds != null) {
                pgWhereCurrent.append(AND_EMP_ID);
                for (int i = 0; i < empIds.length; i++) {
                    pgWhereCurrent.append(String.valueOf(empIds[i]));
                    if (i != empIds.length - 1) {
                        pgWhereCurrent.append(", ");
                    }
                }
            }

            if (current) {
                return pgWhereCurrent.toString();
            } else {
                return pgWhereCurrent.toString();
            }
        }
        case RETRO: {

            pgWhereAdj.append("  ");
            pgWhereAdj.append(dbs.encodeNullCheck(PAID_WRKD_FLAG, QUOTE_N));
            pgWhereAdj.append(NOT_Y);
            if (enableOnBoard) {
	            pgWhereCurrent.append(AND_EMP_ID);
	            pgWhereCurrent.append(" SELECT EMP_ID FROM EMPLOYEE WHERE ");
	            pgWhereCurrent.append(dbs.encodeNullCheck(EMP_ON_BOARD_FLAG,
	                    QUOTE_N));
	            pgWhereCurrent.append(" = 'Y' ) ");
            }

            if (empIds != null) {
                pgWhereAdj.append(AND_EMP_ID);
                for (int i = 0; i < empIds.length; i++) {
                    pgWhereAdj.append(String.valueOf(empIds[i]));
                    if (i != empIds.length - 1) {
                        pgWhereAdj.append(", ");
                    }
                }
                pgWhereAdj.append(") ");
            }
            if (current) {
                return " emp_Id = -99999 ";
            } else {
                return pgWhereAdj.toString();
            }
        }
        case RESET: {
            return null;
        }
        default: {
            return null;
        }
        }

    }

    private Date getMinHireDate(int pdId, Date startDate, Date endDate)
            throws Exception {

        if (logger.isInfoEnabled()){
            logger.info("Getting the min hire date");
        }
        DBServer dbs = DBServer.getServer(conn);

        java.sql.Timestamp tsStartDate = java.sql.Timestamp.valueOf(DateHelper
                .convertDateString(startDate, TIMESTAMP_FORMAT_NO_TIME));
        java.sql.Timestamp tsEndDate = java.sql.Timestamp.valueOf(DateHelper
                .convertDateString(endDate, TIMESTAMP_FORMAT_NO_TIME));

        StringBuffer minHireSql = new StringBuffer();
        minHireSql.append("select min(EMP_HIRE_DATE) ");
        minHireSql.append(" from EMPLOYEE ");
        minHireSql.append(" where PAYGRP_ID = ? ");
        minHireSql.append(" and ");
        minHireSql.append(dbs.encodeNullCheck(EMP_ON_BOARD_FLAG, QUOTE_N));
        minHireSql.append(" <> 'Y'  and ");

        if (dbs.isDB2()) {
            minHireSql.append(" Timestamp(");
            StringBuffer tmpEncodeDate = new StringBuffer();
            tmpEncodeDate.append("'");
            tmpEncodeDate.append(DateHelper.convertDateString(
                    DateHelper.DATE_3000, TIMESTAMP_FORMAT_NO_TIME));
            tmpEncodeDate.append("'");
            minHireSql.append(dbs.encodeNullCheck(ON_BOARD_FIELD, tmpEncodeDate
                    .toString()));
            minHireSql.append(") ");
        } else if (dbs.isOracle()) {
            minHireSql.append(" to_date(");
            minHireSql.append(ON_BOARD_FIELD);
            minHireSql.append(", 'MM/dd/yyyy') ");
        } else if (dbs.isMSSQL()) {
            minHireSql.append(" CAST(");
            minHireSql.append(ON_BOARD_FIELD);
            minHireSql.append(" as date) ");
        }
        minHireSql.append(" between ? and ? ");

        //Find the Min hire date
        PreparedStatement ps = null;
        ResultSet rs = null;
        Date result = null;
        try {
            ps = conn.prepareStatement(minHireSql.toString());
            ps.setInt(1, pdId);
            ps.setTimestamp(2, tsStartDate);
            ps.setTimestamp(3, tsEndDate);
            if (logger.isInfoEnabled()) {
                logger.info("SQL " + minHireSql.toString());
                logger.info("VALUES " + pdId + ", " + tsStartDate + ", "
                        + tsEndDate);
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getDate(1) != null) {
                    if (!rs.getDate(1).equals(DateHelper.DATE_3000)) {
                        result = rs.getDate(1);
                        if (logger.isDebugEnabled()){
                            logger.debug("Found a min hire date of " + result);
                        }
                    }
                }
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
        
        return result;
    }

    public boolean setFlag(int payGrp) throws SQLException {
        PreparedStatement ps = null;
        boolean result = true;
        try {
            Time t = new Time(System.currentTimeMillis());
            String dateString = new StringBuffer().append(
                    DateHelper.getCurrentDate().toString().substring(0, 11))
                    .append(t.toString()).toString();
            Datetime readyTime = DateHelper.parseDate(dateString,
                    TIMESTAMP_FORMAT);

            StringBuffer setPayGrpFlagSB = new StringBuffer();
            setPayGrpFlagSB.append("update PAY_GROUP Set ").append(
                    PAYGRP_STATUS_FLAG).append(" = 'Y', ").append(
                    PAYGRP_STATUS_UDF).append(" = '").append(
                    readyTime.toString()).append("' ");
            if (lockRecords){
                setPayGrpFlagSB.append(", ").append(HANDS_OFF_UDF).append(" = PAYGRP_HANDS_OFF_DATE, ")
                	.append(" PAYGRP_HANDS_OFF_DATE = PAYGRP_END_DATE ");
            }
            setPayGrpFlagSB.append(" where PAYGRP_ID = ?");
            ps = conn.prepareStatement(setPayGrpFlagSB.toString());
            ps.setInt(1, payGrp);
            ps.executeUpdate();
            conn.commit();
        } catch (Throwable t1) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger
                        .error(
                                "com.workbrain.app.bo.ejb.actions.ReadinessPayrollAction.class",
                                t1);
            }
            result = false;
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
        return result;

    }

}