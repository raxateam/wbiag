package com.wbiag.app.ta.db;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.*;
import com.workbrain.util.*;
import com.workbrain.sql.*;
import com.wbiag.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;

/**
 * Provides database access for the CALC_EMPLOYEE_DATE table.
 *
 */
public class CalcEmployeeDateAccess {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CalcEmployeeDateAccess.class);
    public static final String CALC_EMPLOYEE_DATE_TABLE = "calc_employee_date";
    public static final String CALC_EMPLOYEE_DATE_PRI_KEY = "ced_id";
    public static final String CALC_EMPLOYEE_DATE_SEQ = "seq_ced_id";

    public static final String DELETE_CALC_EMPLOYEE_DATE_SQL
        = "DELETE FROM calc_employee_date WHERE ced_id = ?";

    public static final String SELECT_CALC_EMPLOYEE_DATE_SQL
        = "SELECT * FROM calc_employee_date";

    protected CalcEmployeeDateAccess() {
    }

    /**
     * Adds empId, date, msg to CALC_EMPLOYEE_DATE_TABLE
     * @param conn
     * @param empId
     * @param date
     * @param msg
     * @throws SQLException
     */
    public static void addCalcEmployeeDate( DBConnection conn,
                                           int empId ,
                                           Date date, String msg) throws SQLException {
        CalcEmployeeDateData ced = new  CalcEmployeeDateData();
        ced.setEmpId(empId);
        ced.setCedWorkDate(date);
        ced.setCedMessage(msg);
        List ceds = new ArrayList();
        ceds.add(ced);
        addCalcEmployeeDate(conn , ceds, msg);
    }

    /**
     * Adds empId, start/end date, msg to CALC_EMPLOYEE_DATE_TABLE
     * @param conn
     * @param empId
     * @param startDate
     * @param endDate
     * @param msg
     * @throws SQLException
     */
    public static void addCalcEmployeeDate( DBConnection conn,
                                           int empId ,
                                           Date startDate,
                                           Date endDate,
                                           String msg) throws SQLException {
        List ceds = new ArrayList();
        for (Date date = startDate; date.compareTo(endDate) <= 0;
             date = DateHelper.addDays(date, 1)) {
            CalcEmployeeDateData ced = new  CalcEmployeeDateData();
            ced.setEmpId(empId);
            ced.setCedWorkDate(date);
            ced.setCedMessage(msg);
            ceds.add(ced);
        }
        addCalcEmployeeDate(conn , ceds, msg);
    }

    /**
     * Adds a list of <code>CalcEmployeeDateData</code> to CALC_EMPLOYEE_DATE_TABLE
     * @param conn
     * @param empDates
     * @param msg
     * @throws SQLException
     */
    public static void addCalcEmployeeDate( DBConnection conn,
                                        List empDates,
                                        String msg) throws SQLException {
        if (empDates == null || empDates.size() == 0) {
            return;
        }
        List calcEmpDates = new ArrayList();
        Iterator iter = empDates.iterator();
        while (iter.hasNext()) {
            CalcEmployeeDateData item = (CalcEmployeeDateData)iter.next();
            if (item == null || item.getCedWorkDate() == null) {
                throw new RuntimeException ("CalcEmployeeDateData/Date cannot be null");
            }
            item.setCedId(conn.getDBSequence(CALC_EMPLOYEE_DATE_SEQ).getNextValue() );
            calcEmpDates.add(item);
        }
        RecordAccess ra = new RecordAccess(conn);
        ra.insertRecordData(calcEmpDates , CALC_EMPLOYEE_DATE_TABLE) ;
    }

    /**
     * Loads all records in CALC_EMPLOYEE_DATE_TABLE
     * @param conn
     * @return
     * @throws SQLException
     */
    public static List loadCalcEmployeeDate(DBConnection conn) throws SQLException {
        RecordAccess ra = new RecordAccess(conn);
        return ra.loadRecordData(new CalcEmployeeDateData(), CALC_EMPLOYEE_DATE_TABLE, "1=1");
    }

    /**
     * Deletes given list of <code>CalcEmployeeDateData</code>
     * @param conn
     * @param list
     * @return
     * @throws SQLException
     */
    public static int deleteCalcEmployeeDate(DBConnection conn ,
                                              List list) throws SQLException {
        int ret = 0;
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(DELETE_CALC_EMPLOYEE_DATE_SQL);
            Iterator iter = list.iterator();
            while (iter.hasNext()) {
                CalcEmployeeDateData item = (CalcEmployeeDateData)iter.next();
                ps.setInt(1 , item.getCedId());
                ps.addBatch();
            }
            int upd[] = ps.executeBatch();
            ret = upd.length;
        }
        finally {
            if (ps != null) ps.close();
        }
        return ret;
    }

}