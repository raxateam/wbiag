
package com.wbiag.app.export.payroll;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.workbrain.app.scheduler.enterprise.ScheduledJob;
import com.workbrain.sql.DBConnection;
import com.workbrain.test.TestCaseHW;
import com.workbrain.util.DateHelper;

/**
 * Title: HighVolumeExportTaskTest.java <br>
 * Description: <br>
 * 
 * Created: May 20, 2005
 * @author cleigh
 * <p>
 * Revision History <br>
 * May 20, 2005 -  <br>
 * <p>
 * */
public class HighVolumeExportTaskTest extends TestCaseHW {

    public static String SUBMITTED_BY = "'WORKBRAIN'";
    public static String PAYGRP_ID = "2";
    public static String EMP_IDS = "''";
    public static String PAYSYS_ID = "'1'";
    public static String START_DATE = "''";
    public static String END_DATE = "''";
    public static String USE_PAYPERIOD = "'Y'";
    public static String ALL_READY_WHERE = "'N'";
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
        junit.textui.TestRunner.run(HighVolumeExportTaskTest.class);
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
     * Constructor for HighVolumeExportTaskTest.
     * @param arg0
     */
    public HighVolumeExportTaskTest(String arg0) {
        super(arg0);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(HighVolumeExportTaskTest.class);
        return result;
    }
    
    /*
     * Class under test for Status run(int, Map)
     */
    public void testRunintMap() throws Exception {
        
        conn = this.getConnection();
        createExportQueue();
        
        HighVolumeExportTask task = new HighVolumeExportTask();

        Map params = new HashMap();
        params.put(HighVolumeExportTask.PARAM_CLIENT_ID, "1");
        params.put(HighVolumeExportTask.PARAM_MAX_IN_PROGRESS, "2");
        
        ScheduledJob.Status result = null;
        result = task.run(1, params);

        assertNotNull(result);
        assertFalse( result.isJobFailed());
        
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
                    .append(DateHelper.convertDateString(DateHelper.getCurrentDate(), "yyyy-MM-dd")).append("', 'yyyy-MM-dd'), ")
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
