package com.wbiag.app.export.payroll;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.ruleengine.*;

import java.sql.*;

/**
 * Title: HighVolumePayrollExportThreadTest.java <br>
 * Description: JUnit test case for the HighVolumePayrollExport Thread <br>
 * 
 * Created: May 18, 2005
 * 
 * @author cleigh
 *         <p>
 *         Revision History <br>
 *         May 18, 2005 -<br>
 *         <p>
 */
public class HighVolumePayrollExportThreadTest extends RuleTestCase {

    public static String SUBMITTED_BY = "'WORKBRAIN'";
    public static String PAYGRP_ID = "2";
    public static String EMP_IDS = "''";
    public static String PAYSYS_ID = "1";
    public static String START_DATE = "''";
    public static String END_DATE = "''";
    public static String USE_PAYPERIOD = "'Y'";
    public static String ALL_READY_WHERE = "''";
    public static String ALL_READY_PAYGRPS = "''";
    public static String CYCLE = "'"+PayrollExporter.CYCLE_REGULAR+"'";
    public static String ADJUST_DATES = "'N'";
    public static String WRITE_TO_FILE = "'Y'";
    public static String MERGE_FILES = "'Y'";
    public static String WRITE_TO_TABLE = "'N'";
    public static String TERM_ADD_DAYS = "7";
    public static String LOOK_BACK_DAYS = "-14";
    public static String CHK_UNAUTH = "'N'";
    public static String UDF1 = "''";
    public static String UDF2 = "''";
    public static String UDF3 = "''";
    public static String UDF4 = "''";
    public static String UDF5 = "''";
    public static String FLAG1 = "''";
    public static String FLAG2 = "''";
    public static String FLAG3 = "''";
    public static String FLAG4 = "''";
    public static String FLAG5 = "''";
    public static String EXTRA = "''";

    private DBConnection conn;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(HighVolumePayrollExportThreadTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Constructor for HighVolumePayrollExportThreadTest.
     * 
     * @param arg0
     */
    public HighVolumePayrollExportThreadTest(String arg0) throws Exception {
        super(arg0);
    }
    
    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(HighVolumePayrollExportThreadTest.class);
        return result;
    }

    public void testRun() throws SQLException {
        conn = this.getConnection();
        createExportQueue();
        HighVolumePayrollExportThread peThread = new HighVolumePayrollExportThread( "1" );
        peThread.setSlaveThreadCount(1);

        peThread.execute();
        
        int queueId = peThread.getQueueId();
        
        assertEquals(HighVolumePayrollExportThread.QUEUE_STATUS_COMPLETE, getQueueStatus(queueId));
        

    }
    
    private String getQueueStatus(int queueId) throws SQLException {
        String result = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer getStatusSql = new StringBuffer();
            getStatusSql.append("SELECT ").append(HighVolumePayrollExportThread.QUEUE_STATUS)
            .append(" FROM ").append(HighVolumePayrollExportThread.QUEUE_TABLE).append(" WHERE ")
            .append(HighVolumePayrollExportThread.QUEUE_ID).append(" = ? ");
            
            ps = conn.prepareStatement(getStatusSql.toString());
            ps.setInt(1, queueId);
            rs = ps.executeQuery();
            while (rs.next()){
                result = rs.getString(1);
            }
        } catch (SQLException e){
            throw e;
        } finally {
            if (ps != null){
                ps.close();
            }
            if (rs != null){
                rs.close();
            }
        }
        return result;
    }

    public void createExportQueue() throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer expQueueSql = new StringBuffer();
            expQueueSql.append("INSERT INTO ").append(
                    HighVolumePayrollExportThread.QUEUE_TABLE).append(" (")
                    .append(HighVolumePayrollExportThread.QUEUE_ID).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_STATUS).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_SUBMITTED_BY).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_SUBMIT_DATE).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_PAYGRP_IDS).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_EMP_IDS).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_PAYSYS_ID).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_START_DATE).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_END_DATE).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_USE_PAYPERIOD).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_ALL_READY_PAYGRPS).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_ALL_READY_WHERE).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_CYCLE).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_ADJUST_DATES).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_WRITE_TO_FILE).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_MERGE_FILES).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_WRITE_TO_TABLE).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_TERM_ADD_DAYS).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_LOOK_BACK_DAYS).append(", ")
                    .append(HighVolumePayrollExportThread.QUEUE_CHK_UNAUTH)
                    .append(") VALUES (SEQ_WIEQ_ID.nextval, '")
                    .append(HighVolumePayrollExportThread.QUEUE_STATUS_READY).append("', ")
                    .append(SUBMITTED_BY).append(", ")
                    .append(" to_date('")
                    .append(DateHelper.convertDateString(new java.util.Date(), "yyyy-MM-dd  hh:mm:ss")).append("', 'yyyy-MM-dd hh:mm:ss'), ")
                    .append(PAYGRP_ID).append(", ")
                    .append(EMP_IDS).append(", ")
                    .append(PAYSYS_ID).append(", ")
                    .append(START_DATE).append(", ")
                    .append(END_DATE).append(", ")
                    .append(USE_PAYPERIOD).append(", ")
                    .append(ALL_READY_PAYGRPS).append(", " )
                    .append(ALL_READY_WHERE).append(", " )
                    .append(CYCLE).append(", ")
                    .append(ADJUST_DATES).append(", ")
                    .append(WRITE_TO_FILE).append(", ")
                    .append(MERGE_FILES).append(", ")
                    .append(WRITE_TO_TABLE).append(", ")
                    .append(TERM_ADD_DAYS).append(", ")
                    .append(LOOK_BACK_DAYS).append(", ")
                    .append(CHK_UNAUTH).append(" ) ");

            ps = conn.prepareStatement(expQueueSql.toString());
            ps.execute();
            conn.commit();
            
            
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
        
            if (ps != null) {
                ps.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
    }
}
