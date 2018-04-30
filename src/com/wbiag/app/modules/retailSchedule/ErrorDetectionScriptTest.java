package com.wbiag.app.modules.retailSchedule;

import com.workbrain.server.sql.ConnectionManager;
import com.workbrain.sql.DBConnection;
import java.sql.*;
import com.workbrain.test.TestCaseHW;
import com.workbrain.test.ContextFactoryMock;

public class ErrorDetectionScriptTest extends TestCaseHW {
	private static DBConnection conn;
	
	public ErrorDetectionScriptTest(String arg0) {
		super(arg0);
	}
	
	public static void main(String[] args) throws Exception {
		junit.textui.TestRunner.run(ErrorDetectionScriptTest.class);
	}
	
	protected void setUp() throws Exception {
		//System.out.println("Setup starts");
		//super.setUp();
		System.setProperty(ContextFactoryMock.SYS_TEST_FILE_NAME, "C:/Documents and Settings/tyoung/My Documents/Projects/test.properties");
		try {
			conn = new DBConnection(ConnectionManager.getConnection());
		}
		catch(SQLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	protected void tearDown() throws Exception {
		//super.tearDown();
		//System.out.println("tear down starts");
		if(!conn.isClosed())
			conn.close();
	}

	/*
	 * Test method for 'com.wbiag.app.modules.retailSchedule.ErrorDetectionScript.action4_1_4()'
	 */
	public void testAction4_1_4() {
//		create some duplicate records
		try {
			String qs = "insert into SO_EMPLOYEE (SEMP_ID, EMP_ID, SKDGRP_ID, EMPGRP_ID, SEMP_EFF_DATE, SEMP_SKDMAX_HRS, SEMP_SKDMIN_HRS, SEMP_DAYMAX_HRS, SEMP_SHFTMIN_HRS, SEMP_ONFIXED_SKD, SEMP_MAX2NT_RULE, SEMP_IS_SALARY, SEMP_MAXSHFTDAY, SEMP_SEASONAL_REG, SEMP_EXEMPT_STAT, SEMP_IS_MINOR, SEMP_X_IN_DATE, SEMP_X_OUT_DATE, SEMP_ISKEYHOLDER) values (-10000, 3, 1, 1, TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss') ,1, 1, 1, 1, 1, 0, 0, 2, 'R', 'H', 0, TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss'), TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss'), 0)";

			PreparedStatement ps = conn.prepareStatement(qs);
			ResultSet rs = ps.executeQuery();
			
			ps.close();
			rs.close();
			
			qs = "insert into SO_EMPLOYEE (SEMP_ID, EMP_ID, SKDGRP_ID, EMPGRP_ID, SEMP_EFF_DATE, SEMP_SKDMAX_HRS, SEMP_SKDMIN_HRS, SEMP_DAYMAX_HRS, SEMP_SHFTMIN_HRS, SEMP_ONFIXED_SKD, SEMP_MAX2NT_RULE, SEMP_IS_SALARY, SEMP_MAXSHFTDAY, SEMP_SEASONAL_REG, SEMP_EXEMPT_STAT, SEMP_IS_MINOR, SEMP_X_IN_DATE, SEMP_X_OUT_DATE, SEMP_ISKEYHOLDER) values (-10001, 3, 1, 1, TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss') ,1, 1, 1, 1, 1, 0, 0, 2, 'R', 'H', 0, TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss'), TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss'), 0)";
			
			ps = conn.prepareStatement(qs);
			rs = ps.executeQuery();
			
			ps.close();
			rs.close();
			
			qs = "commit";
			
			ps = conn.prepareStatement(qs);
			rs = ps.executeQuery();
			
			ps.close();
			rs.close();
		}
		catch(SQLException e) {
			//System.out.println(e.getMessage());
			fail(e.getMessage());
		}
		
		//run test
		ErrorDetectionScript eds = new ErrorDetectionScript("1", "1");
		ErrorDetectionScriptResult result = eds.action4_1_4();
		String message = result.getMessage();
		
		//System.out.println(message);
		
		//get rid of the extra records
		try {
			String qs = "DELETE FROM SO_EMPLOYEE ";
			qs += "WHERE SEMP_ID =-10000 OR SEMP_ID =-10001";
			
			PreparedStatement ps = conn.prepareStatement(qs);
			ResultSet rs = ps.executeQuery();
			
			ps.close();
			rs.close();
		}
		catch(SQLException e) {
			//System.out.println(e.getMessage());
			fail(e.getMessage());
		}
		
		assertTrue(message.indexOf("SO_EMPLOYEE records for Workbrain Support") >= 0);
	}

	/*
	 * Test method for 'com.wbiag.app.modules.retailSchedule.ErrorDetectionScript.action4_2_10()'
	 
	public void testAction4_2_10() {
//		create some duplicate records
		String qs = "insert into WORKBRAIN.SO_EMPLOYEE (SEMP_ID, EMP_ID, SKDGRP_ID, EMPGRP_ID, SEMP_EFF_DATE, SEMP_SKDMAX_HRS, SEMP_SKDMIN_HRS, SEMP_DAYMAX_HRS, SEMP_SHFTMIN_HRS, SEMP_ONFIXED_SKD, SEMP_MAX2NT_RULE, SEMP_IS_SALARY, SEMP_MAXSHFTDAY, SEMP_SEASONAL_REG, SEMP_EXEMPT_STAT, SEMP_IS_MINOR, SEMP_X_IN_DATE, SEMP_X_OUT_DATE, SEMP_ISKEYHOLDER) ";
		qs += "values (-10000, 3, 1, 1, TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss') ,1, 1, 1, 1, 1, 0, 0, 2, 'R', 'H', 0, TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss'), TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss'), 0);";
		qs += "insert into WORKBRAIN.SO_EMPLOYEE (SEMP_ID, EMP_ID, SKDGRP_ID, EMPGRP_ID, SEMP_EFF_DATE, SEMP_SKDMAX_HRS, SEMP_SKDMIN_HRS, SEMP_DAYMAX_HRS, SEMP_SHFTMIN_HRS, SEMP_ONFIXED_SKD, SEMP_MAX2NT_RULE, SEMP_IS_SALARY, SEMP_MAXSHFTDAY, SEMP_SEASONAL_REG, SEMP_EXEMPT_STAT, SEMP_IS_MINOR, SEMP_X_IN_DATE, SEMP_X_OUT_DATE, SEMP_ISKEYHOLDER) ";
		qs += "values (-10001, 3, 1, 1, TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss') ,1, 1, 1, 1, 1, 0, 0, 2, 'R', 'H', 0, TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss'), TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss'), 0);";

		try {
			PreparedStatement ps = conn.prepareStatement(qs);
			ResultSet rs = ps.executeQuery();
			
			ps.close();
			rs.close();
		}
		catch(SQLException e) {
			fail(e.getMessage());
		}
		//run test
		ErrorDetectionScriptResult result = eds.action4_1_4();
		String message = result.getMessage();
		
		assertTrue(message.indexOf("SO_EMPLOYEE records for Workbrain Support") > 0);
		
		//get rid of the extra records
		qs = "DELETE FROM SO_EMPLOYEE ";
		qs += "WHERE SEMP_ID =-10000;";
		qs += "DELETE FROM SO_EMPLOYEE ";
		qs += "WHERE SEMP_ID =-10000;";
		
		try {
			PreparedStatement ps = conn.prepareStatement(qs);
			ResultSet rs = ps.executeQuery();
			
			ps.close();
			rs.close();
		}
		catch(SQLException e) {
			fail(e.getMessage());
		}
	}*/

	/*
	 * Test method for 'com.wbiag.app.modules.retailSchedule.ErrorDetectionScript.action4_3_7()'
	 
	public void testAction4_3_7() {
//		create some duplicate records
		String qs = "insert into WORKBRAIN.SO_EMPLOYEE (SEMP_ID, EMP_ID, SKDGRP_ID, EMPGRP_ID, SEMP_EFF_DATE, SEMP_SKDMAX_HRS, SEMP_SKDMIN_HRS, SEMP_DAYMAX_HRS, SEMP_SHFTMIN_HRS, SEMP_ONFIXED_SKD, SEMP_MAX2NT_RULE, SEMP_IS_SALARY, SEMP_MAXSHFTDAY, SEMP_SEASONAL_REG, SEMP_EXEMPT_STAT, SEMP_IS_MINOR, SEMP_X_IN_DATE, SEMP_X_OUT_DATE, SEMP_ISKEYHOLDER) ";
		qs += "values (-10000, 3, 1, 1, TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss') ,1, 1, 1, 1, 1, 0, 0, 2, 'R', 'H', 0, TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss'), TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss'), 0);";
		qs += "insert into WORKBRAIN.SO_EMPLOYEE (SEMP_ID, EMP_ID, SKDGRP_ID, EMPGRP_ID, SEMP_EFF_DATE, SEMP_SKDMAX_HRS, SEMP_SKDMIN_HRS, SEMP_DAYMAX_HRS, SEMP_SHFTMIN_HRS, SEMP_ONFIXED_SKD, SEMP_MAX2NT_RULE, SEMP_IS_SALARY, SEMP_MAXSHFTDAY, SEMP_SEASONAL_REG, SEMP_EXEMPT_STAT, SEMP_IS_MINOR, SEMP_X_IN_DATE, SEMP_X_OUT_DATE, SEMP_ISKEYHOLDER) ";
		qs += "values (-10001, 3, 1, 1, TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss') ,1, 1, 1, 1, 1, 0, 0, 2, 'R', 'H', 0, TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss'), TO_DATE('1900-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss'), 0);";

		try {
			PreparedStatement ps = conn.prepareStatement(qs);
			ResultSet rs = ps.executeQuery();
			
			ps.close();
			rs.close();
		}
		catch(SQLException e) {
			fail(e.getMessage());
		}
		//run test
		ErrorDetectionScriptResult result = eds.action4_1_4();
		String message = result.getMessage();
		
		assertTrue(message.indexOf("SO_EMPLOYEE records for Workbrain Support") > 0);
		
		//get rid of the extra records
		qs = "DELETE FROM SO_EMPLOYEE ";
		qs += "WHERE SEMP_ID =-10000;";
		qs += "DELETE FROM SO_EMPLOYEE ";
		qs += "WHERE SEMP_ID =-10000;";
		
		try {
			PreparedStatement ps = conn.prepareStatement(qs);
			ResultSet rs = ps.executeQuery();
			
			ps.close();
			rs.close();
		}
		catch(SQLException e) {
			fail(e.getMessage());
		}
	}*/

	/*
	 * Test method for 'com.wbiag.app.modules.retailSchedule.ErrorDetectionScript.action4_3_13()'
	 */
	public void testAction4_3_13() {
//		create some duplicate records
		try {
			String qs = "insert into SO_EMPLOYEE_GROUP (EMPGRP_ID, EMPGRP_NAME, EMPGRP_DESC, SKDGRP_ID, EMPGRP_SHRINK_PCT) ";
			qs += "values (-99999, 'test_empty_skdgrp', 'test_empty_skdgrp', (select skdgrp_id ";
			qs += "from so_schedule_group ";
			qs += "where skdgrp_intrnl_type = 12 ";
			qs += "and ROWNUM < 2), 0)";
			
			PreparedStatement ps = conn.prepareStatement(qs);
			ResultSet rs = ps.executeQuery();
			
			ps.close();
			rs.close();
			
			qs = "commit";
			
			ps = conn.prepareStatement(qs);
			rs = ps.executeQuery();
			
			ps.close();
			rs.close();
		}
		catch(SQLException e) {
			fail(e.getMessage());
		}
		//run test
		ErrorDetectionScript eds = new ErrorDetectionScript("1", "1");
		ErrorDetectionScriptResult result = eds.action4_3_13();
		String message = result.getMessage();
		
		System.out.println(message);
		
		//get rid of the extra records
		try {
			String qs = "delete from so_employee_group ";
			qs += "where empgrp_id = -99999";
			
			PreparedStatement ps = conn.prepareStatement(qs);
			ResultSet rs = ps.executeQuery();
			
			ps.close();
			rs.close();
		}
		catch(SQLException e) {
			fail(e.getMessage());
		}
		
		assertTrue(message.indexOf("test_empty_skdgrp") >= 0);
	}
}
