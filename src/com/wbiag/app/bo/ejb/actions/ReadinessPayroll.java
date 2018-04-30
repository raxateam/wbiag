package com.wbiag.app.bo.ejb.actions;

import java.sql.*;
import java.lang.String;
import java.util.Date;
import java.util.*;

import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.model.PayGroupData;
import com.workbrain.app.ta.db.PayGroupAccess;

import com.wbiag.app.bo.ejb.actions.OffCycleHelper;

public class ReadinessPayroll {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(ReadinessPayroll.class);

    private static final String ON_BOARD_FIELD = "EMP_VAL1";

    private static final String MARK_INPROGRESS = "I";

    private static final String PAYGRP_FLAG = "PAYGRP_FLAG1";
    private static final String PAYGRP_IN_PROGRESS_FLAG = "PAYGRP_FLAG1";

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd hh:mm:ss";

    public static boolean checkUnauth(DBConnection conn, int pgId,
                                      java.sql.Timestamp startDate,
                                      java.sql.Timestamp endDate,
                                      java.sql.Timestamp oBStartDate,
                                      java.sql.Timestamp oBEndDate) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        logger.info("Checking for unauthorized records");
        StringBuffer oBDateOracle = new StringBuffer();
        StringBuffer oBDateDB2 = new StringBuffer();
        StringBuffer oBDateMSSQL = new StringBuffer();
        //Oracle On Board Date
        oBDateOracle.append(" to_date(");
        oBDateOracle.append(ON_BOARD_FIELD);
        oBDateOracle.append(", 'MM/dd/yyyy') ");

        //DB2 On Board Date
        oBDateDB2.append(" Timestamp(");
        oBDateDB2.append(ON_BOARD_FIELD);
        oBDateDB2.append(") ");

        //MS SQL On Board Date
        oBDateMSSQL.append(" CAST(");
        oBDateMSSQL.append(ON_BOARD_FIELD);
        oBDateMSSQL.append(" as date) ");

        try {
            if (startDate == null || endDate == null) {
                //Check for Current pay period dates
                String wrkSumNAuthSql = "SELECT * from WORK_SUMMARY x , EMPLOYEE y where x.EMP_ID = y.EMP_ID and x.WRKS_AUTHORIZED = 'N' and x.PAYGRP_ID = ? and x.WRKS_WORK_DATE between (select paygrp_start_date from PAY_GROUP where paygrp_id = ? ) and (select paygrp_end_date from PAY_GROUP where paygrp_id = ? ) and x.WRKS_WORK_DATE < y.EMP_TERMINATION_DATE";
                ps = conn.prepareStatement(wrkSumNAuthSql);
                ps.setInt(1, pgId);
                ps.setInt(2, pgId);
                ps.setInt(3, pgId);
                rs = ps.executeQuery();
                if (rs.next()) {
                    return false;
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            }
            else {
                //Check that all records are authorized for given dates
                String wrkSumNAuthSql = "SELECT * from WORK_SUMMARY x , EMPLOYEE y where x.EMP_ID = y.EMP_ID and x.WRKS_AUTHORIZED = 'N' and x.PAYGRP_ID = ? and WRKS_WORK_DATE between ? and ? and WRKS_WORK_DATE < EMP_TERMINATION_DATE";
                ps = conn.prepareStatement(wrkSumNAuthSql);
                ps.setInt(1, pgId);
                ps.setTimestamp(2, startDate);
                ps.setTimestamp(3, endDate);
                rs = ps.executeQuery();

                if (rs.next()) {
                    return false;
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            }
            if (! (oBStartDate == null || endDate == null)) {
                //Check On Boarded employee records are authorized
                StringBuffer onBoardSql = new StringBuffer();
                onBoardSql.append("SELECT * from WORK_SUMMARY x, EMPLOYEE y where x.EMP_ID = y.EMP_ID and x.WRKS_AUTHORIZED = 'N' and x.PAYGRP_ID = ? and x.WRKS_WORK_DATE between ? and ? and x.WRKS_WORK_DATE < y.EMP_TERMINATION_DATE and x.EMP_ID in (select EMP_ID from EMPLOYEE where ");
                if (conn.getDBType().getName().equals("Oracle")) {
                    onBoardSql.append(oBDateOracle.toString());
                }
                else if (conn.getDBType().getName().equals("DB2")) {
                    onBoardSql.append(oBDateDB2.toString());
                }
                else {
                    onBoardSql.append(oBDateMSSQL.toString());
                }
                onBoardSql.append(" between ? and ? )");
                ps = conn.prepareStatement(onBoardSql.toString());
                ps.setInt(1, pgId);
                ps.setTimestamp(2, oBStartDate);
                ps.setTimestamp(3, oBEndDate);
                ps.setTimestamp(4, startDate);
                ps.setTimestamp(5, endDate);
                rs = ps.executeQuery();
                if (rs.next()) {
                    logger.info("There are on boarded un authorized records");
                    return false;
                }

                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            }

            return true;

        }
        catch (Throwable t) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger.error(
                    "com.workbrain.app.bo.ejb.actions.ExportPayrollAction.class",
                    t);
            }
            return false;
        }
    }

    public static boolean checkUnauth(DBConnection conn, int[] pgIds,
                                      Date startDate, Date endDate) {
        java.sql.Timestamp SqlStartDate = null;
        java.sql.Timestamp SqlEndDate = null;
        java.sql.Timestamp oBSqlStartDate = null;
        java.sql.Timestamp oBSqlEndDate = null;
        String oBStartDateString = null;
        String oBEndDateString = null;

        if (startDate != null && endDate != null) {
            String startDateString = DateHelper.convertDateString(startDate,
                TIMESTAMP_FORMAT);
            String endDateString = DateHelper.convertDateString(endDate,
                TIMESTAMP_FORMAT);
            SqlStartDate = java.sql.Timestamp.valueOf(startDateString);
            SqlEndDate = java.sql.Timestamp.valueOf(endDateString);
        }
        Date oBStartDate = OffCycleHelper.getMinHireDate(conn, pgIds, startDate,
            endDate);
        Date oBEndDate = OffCycleHelper.getLastPayPeriodEndDate(conn, pgIds);

        if (! (oBStartDate == null || oBEndDate == null)) {
            oBStartDateString = DateHelper.convertDateString(oBStartDate,
                TIMESTAMP_FORMAT);
            oBEndDateString = DateHelper.convertDateString(oBEndDate,
                TIMESTAMP_FORMAT);
            oBSqlStartDate = java.sql.Timestamp.valueOf(oBStartDateString);
            oBSqlEndDate = java.sql.Timestamp.valueOf(oBEndDateString);
        }

        for (int i = 0; i < pgIds.length; i++) {
            if (!checkUnauth(conn, pgIds[i], SqlStartDate, SqlEndDate,
                             oBSqlStartDate,
                             oBSqlEndDate)) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkUnauth(DBConnection conn, int pgId,
                                      Date startDate, Date endDate) {
        int[] pgIds = new int[1];
        pgIds[0] = pgId;
        return checkUnauth(conn, pgIds, startDate, endDate);
    }

    public static boolean checkPaygrpsReady(DBConnection conn, int[] payGrps) {

        //Check that paygrps are ready for export
        PayGroupAccess pga = new PayGroupAccess(conn);
        PayGroupData pgd = null;
        for (int i = 0; i < payGrps.length; i++) {

            pgd = pga.load(payGrps[i]);
            String status = pgd.getField(PAYGRP_FLAG).toString();
            String state = null;
            if (pgd.getField(PAYGRP_IN_PROGRESS_FLAG) != null) {
                state = pgd.getField(PAYGRP_IN_PROGRESS_FLAG).toString();
                if ( (status.equalsIgnoreCase("N")) ||
                    state.equalsIgnoreCase(MARK_INPROGRESS)) {
                    return false;
                }
            }
            else if (status.equalsIgnoreCase("N")) {
                return false;
            }
        }

        return true;
    }

    public static int[] getReadyPaygrps(DBConnection conn) {
        PayGroupAccess pga = new PayGroupAccess(conn);
        List allPaygrps = pga.loadAll();
        PayGroupData pgd = null;

        ArrayList pgs = new ArrayList();

        Iterator pGroups = allPaygrps.iterator();
        while (pGroups.hasNext()) {
            pgd = (PayGroupData) pGroups.next();
            String status = pgd.getField(PAYGRP_FLAG).toString();
            if (status.equals("Y") || status.equals("E")) {
                pgs.add(pgd.getField("PAYGRP_ID"));
            }
        }
        Object[] pgArray = pgs.toArray();
        int[] payGrpsInt = new int[pgArray.length];
        for (int i = 0; i < pgArray.length; i++) {
            payGrpsInt[i] = Integer.parseInt(pgArray[i].toString());
        }

        return payGrpsInt;
    }

    public static boolean resetReadiness(DBConnection conn) {
        PreparedStatement ps = null;
        try {
            StringBuffer resetSQL = new StringBuffer();
            resetSQL.append("UPDATE PAY_GROUP SET ").append(PAYGRP_FLAG).append(
                " = ? ");
            ps = conn.prepareStatement(resetSQL.toString());
            ps.setString(1, "N");
            ps.executeUpdate();
            conn.commit();
        }
        catch (Throwable t) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger.error(
                    "com.workbrain.app.bo.ejb.actions.ReadinessPayrollAction.class",
                    t);
            }
            return false;
        }
        return true;

    }

}
