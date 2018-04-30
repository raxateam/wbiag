package com.wbiag.app.ta.db;

import com.wbiag.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.test.*;
import java.sql.*;
import org.apache.log4j.*;
import org.apache.log4j.BasicConfigurator;

import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;

/**
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class WbiagStateCacheTest extends TestCaseHW{

    private static final Logger logger = Logger.getLogger(WbiagStateCacheTest.class);

    public WbiagStateCacheTest(String arg0) {
        super(arg0);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WbiagStateCacheTest.class);

        // TODO - Remove this.
        BasicConfigurator.configure();

        return result;
    }

    /**
     * Tests cache methods.
     * @throws Exception
     */
    public void xGet() throws Exception {

    	final String STATE_NAME = "BV";

        WbiagStateCache cache = WbiagStateCache.getInstance();

        // Insert the state data
        insertStateData(STATE_NAME);

        WbiagStateData st = cache.getStateData(getConnection(), STATE_NAME);
        assertNotNull(st);

        Timestamp d1 = DateHelper.getCurrentDate();
        try {
            insertMinWageData(st.getWistId(),
                              new double[] {5}
                              ,
                              new Timestamp[] {d1});
        }
        catch (Exception ex) {
            logger.error("Error in insertion, ignore", ex);
        }
        cache.updateCacheContents(getConnection()); // clears the cache.

        WbiagStateMinwgeData sMin = cache.getMinWageByStateEffDate(getConnection() ,
            STATE_NAME,
            d1);
        assertNotNull(sMin);

        // *** test employee val driven state
        TestUtil.getInstance().setVarTemp("/" + WbiagStateCache.REG_EMPLOYEE_STATE_COLUMN ,"EMP_VAL1");
        final int empId = 15;
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);
        InsertEmployeeOverride insEmp = new InsertEmployeeOverride(getConnection());
        insEmp.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insEmp.setEmpId(empId);
        insEmp.setStartDate(DateHelper.DATE_1900);      insEmp.setEndDate(DateHelper.DATE_3000);
        insEmp.setEmpVal1(STATE_NAME);
        ovrBuilder.add(insEmp);
        ovrBuilder.execute(false , false); ovrBuilder.clear();

        EmployeeData empData = new EmployeeAccess(getConnection(),
                                                  getCodeMapper()).load(empId, DateHelper.getCurrentDate());

        WbiagStateMinwgeData sEmpMin = cache.getMinWageByEmpEffDate(getConnection() ,
            empData,
            d1);

        assertEquals(sMin.getWistmMinWage() , sEmpMin.getWistmMinWage() , 0);

    }

    public void testEffectiveDate() throws Exception {

    	final String STATE_NAME = "BV";

        // Load the state data from the cache.
        WbiagStateCache cache = WbiagStateCache.getInstance();
        cache.updateCacheContents(getConnection()); // clears the cache.

        // Insert the state data
        try {
            insertStateData(STATE_NAME);
        }
        catch (Exception ex) {
            logger.error("Error in insertion, ignore", ex);
        }

        WbiagStateData st = cache.getStateData(getConnection(), STATE_NAME);
        assertNotNull(st);

        Timestamp d1 = DateHelper.parseDate("01/01/1984", "dd/MM/yyyy");
        Timestamp d2 = DateHelper.parseDate("01/01/1985", "dd/MM/yyyy");


        // Insert two date effective entries.
        try {
            insertMinWageData(st.getWistId(),
                              new double[] {5, 10}
                              ,
                              new Timestamp[] {d1, d2});
        }
        catch (Exception ex1) {
            logger.error("Error in insertion, ignore", ex1);
        }
        cache.updateCacheContents(getConnection()); // clears the cache.

        WbiagStateMinwgeData sMin = null;

        // Check a date between d1 and d2.
        sMin = cache.getMinWageByStateEffDate(getConnection() ,
									            STATE_NAME,
									            DateHelper.addDays(d1, 1));
        assertNotNull(sMin);
        assertEquals(5, sMin.getWistmMinWage(), 0);

        // Check a date after d2.
        sMin = cache.getMinWageByStateEffDate(getConnection() ,
									            STATE_NAME,
									            DateHelper.addDays(d2, 1));
        assertNotNull(sMin);
        assertEquals(10, sMin.getWistmMinWage(), 0);

    }

    private void insertStateData(String stateName) throws Exception{

    	final int STATE_ID = 8888;

    	Connection conn = getConnection();
    	PreparedStatement pstmt = conn.prepareStatement("insert into wbiag_state "
    													+ "(wist_id, wist_name, wist_desc) "
														+ "values (?,?,?)");

    	pstmt.setInt(1, STATE_ID);
    	pstmt.setString(2, stateName);
    	pstmt.setString(3, stateName);

    	pstmt.executeUpdate();
    	pstmt.close();

    }

    private void insertMinWageData(int stateId, double minWage[], Timestamp effectiveDate[]) throws Exception {

    	DBConnection conn = getConnection();
    	PreparedStatement pstmt = null;

        try {
            pstmt = conn.prepareStatement("insert into wbiag_state_minwge "
                                  +
                "(wistm_id, wist_id, wistm_eff_date, wistm_min_wage) "
                                  + "values (?,?,?,?)");

            //int minWageId = 9999;

            for (int i = 0; i < minWage.length; i++) {
                int wistm_id = getConnection().getDBSequence("seq_wistm_id").getNextValue();
                pstmt.setInt(1, wistm_id);
                pstmt.setInt(2, stateId);
                pstmt.setTimestamp(3, effectiveDate[i]);
                pstmt.setDouble(4, minWage[i]);

                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
        finally {
            if (pstmt != null) pstmt.close();
        }

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
}

}
