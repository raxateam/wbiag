package com.wbiag.app.bo.ejb.actions;

import java.util.*;
import java.sql.*;
import java.util.Date;

import com.workbrain.util.*;
import com.workbrain.sql.*;
import com.workbrain.app.ta.db.PayGroupAccess;
import com.workbrain.app.ta.model.PayGroupData;

public class OffCycleHelper {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(OffCycleHelper.class);

    private static final String ON_BOARD_FIELD = "EMP_VAL1";

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd hh:mm:ss";

    public static Date getMinHireDate(DBConnection conn, int[] pgIds,
                                      Date startDate, Date endDate) {

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

        if (startDate == null || endDate == null) {
            startDate = getPayPeriodStartDate(conn, pgIds);
            endDate = getPayPeriodEndDate(conn, pgIds);
        }

        if (startDate == null || endDate == null) {
            return null;
        }

        //Find the Min hire date
        PreparedStatement ps = null;
        ResultSet rs = null;

        String startDateString = DateHelper.convertDateString(startDate,
            TIMESTAMP_FORMAT);
        String endDateString = DateHelper.convertDateString(endDate,
            TIMESTAMP_FORMAT);

        try {
            StringBuffer minHireDateSql = new StringBuffer();
            minHireDateSql.append(
                "Select min(EMP_HIRE_DATE) from EMPLOYEE where PAYGRP_ID in (");
            for (int i = 0; i < pgIds.length; i++) {
                minHireDateSql.append(i > 0 ? ",?" : "?");
            }
            minHireDateSql.append(") and ");
            if (conn.getDBType().getName().equals("Oracle")) {
                minHireDateSql.append(oBDateOracle.toString());
            }
            else if (conn.getDBType().getName().equals("DB2")) {
                minHireDateSql.append(oBDateDB2.toString());
            }
            else {
                minHireDateSql.append(oBDateMSSQL.toString());
            }
            minHireDateSql.append(" between ? and ? ");
            ps = conn.prepareStatement(minHireDateSql.toString());
            int fieldCount = 1;
            for (int i = 0; i < pgIds.length; i++) {
                ps.setInt(fieldCount++, pgIds[i]);
            }
            ps.setTimestamp(fieldCount++, Timestamp.valueOf(startDateString));
            ps.setTimestamp(fieldCount++, Timestamp.valueOf(endDateString));
            rs = ps.executeQuery();
            if (rs.next()) {

                return rs.getDate(1);
            }
        }
        catch (Throwable t) {

            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger.error(
                    "com.workbrain.app.bo.ejb.actions.ReadinessPayrollAction.class",
                    t);
            }

        }finally{
            SQLHelper.cleanUp(ps);
            SQLHelper.cleanUp(rs);
        }
        //If no dates found
        return null;
    }

    public static Date getLastPayPeriodEndDate(DBConnection conn, int[] pgIds) {

        PayGroupAccess pga = new PayGroupAccess(conn);
        Date lastPayPeriod = null;
        for (int i = 0; i < pgIds.length; i++) {
            PayGroupData pgd = pga.load(pgIds[i]);
            int diff = DateHelper.dateDifferenceInDays(pgd.getPaygrpStartDate(),
                pgd.getPaygrpEndDate());
            diff--;
            if (lastPayPeriod == null) {
                lastPayPeriod = DateHelper.addDays(pgd.getPaygrpEndDate(), diff);
            }
            else {
                //All pay period end dates must be the same.
                if (!lastPayPeriod.equals(DateHelper.addDays(pgd.
                    getPaygrpStartDate(), -1))) {
                    return null;
                }
            }

        }
        return lastPayPeriod;
    }

    public static Date getPayPeriodStartDate(DBConnection conn, int[] pgIds) {

        PayGroupAccess pga = new PayGroupAccess(conn);
        Date PayPeriodStart = null;
        Date PayPeriodEnd = null;
        for (int i = 0; i < pgIds.length; i++) {
            PayGroupData pgd = pga.load(pgIds[i]);
            if (PayPeriodStart == null) {
                PayPeriodStart = pgd.getPaygrpStartDate();
                PayPeriodEnd = pgd.getPaygrpEndDate();
            }
            else {
                //All pay period start and end dates must be the same.
                if (!PayPeriodStart.equals(pgd.getPaygrpStartDate())) {
                    return null;
                }
                if (!PayPeriodEnd.equals(pgd.getPaygrpEndDate())) {
                    return null;
                }
            }

        }
        return PayPeriodStart;
    }

    public static Date getPayPeriodEndDate(DBConnection conn, int[] pgIds) {

        PayGroupAccess pga = new PayGroupAccess(conn);
        Date PayPeriodStart = null;
        Date PayPeriodEnd = null;
        for (int i = 0; i < pgIds.length; i++) {
            PayGroupData pgd = pga.load(pgIds[i]);
            if (PayPeriodStart == null) {
                PayPeriodStart = pgd.getPaygrpStartDate();
                PayPeriodEnd = pgd.getPaygrpEndDate();
            }
            else {
                //All pay period start and end dates must be the same.
                if (!PayPeriodStart.equals(pgd.getPaygrpStartDate())) {
                    return null;
                }
                if (!PayPeriodEnd.equals(pgd.getPaygrpEndDate())) {
                    return null;
                }
            }

        }
        return PayPeriodEnd;
    }

    public static Date getTermDate(DBConnection conn, int[] pgIds,
                                   Date startDate, Date endDate) {

        //Find the Max term date
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (startDate == null || endDate == null) {
                startDate = getPayPeriodStartDate(conn, pgIds);
                endDate = getPayPeriodEndDate(conn, pgIds);
            }

            String startDateString = DateHelper.convertDateString(startDate,
                TIMESTAMP_FORMAT);
            String endDateString = DateHelper.convertDateString(endDate,
                TIMESTAMP_FORMAT);

            StringBuffer maxTermDateSql = new StringBuffer();
            maxTermDateSql.append(
                "Select max(EMP_TERMINATION_DATE-1) from EMPLOYEE where PAYGRP_ID in (");
            for (int i = 0; i < pgIds.length; i++) {
                maxTermDateSql.append(i > 0 ? ",?" : "?");
            }
            maxTermDateSql.append(") and EMP_TERMINATION_DATE between ? and ?");
            ps = conn.prepareStatement(maxTermDateSql.toString());
            int fieldCount = 1;
            for (int i = 0; i < pgIds.length; i++) {
                ps.setInt(fieldCount++, pgIds[i]);
            }
            ps.setTimestamp(fieldCount++, Timestamp.valueOf(startDateString));
            ps.setTimestamp(fieldCount++, Timestamp.valueOf(endDateString));

            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDate(1);
            }
        }
        catch (Throwable t) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger.error(
                    "com.workbrain.app.bo.ejb.actions.ReadinessPayrollAction.class",
                    t);
            }
        }
        finally {
            SQLHelper.cleanUp(ps);
            SQLHelper.cleanUp(rs);
        }
        //If no dates found
        return null;
    }

    public static Date getRetroStartDate(DBConnection conn, int[] pgIds,
                                         Date startDate, Date endDate) {

        return null;
    }

    public static Date getRetroEndDate(DBConnection conn, int[] pgIds,
                                       Date startDate, Date endDate) {

        return null;
    }

}
