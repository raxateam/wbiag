/*
 * Created on Dec 8, 2004
 *
 */
package com.wbiag.app.wbinterface.hbm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestSuite;

import com.workbrain.app.wbinterface.db.ImportData;
import com.workbrain.sql.DBConnection;
import com.workbrain.test.TestCaseHW;
import com.workbrain.util.DateHelper;

/** Title:         POSImportTransactionTest
 * Description:    Junit test for ImportTransaction
 * Copyright:      Copyright (c) 2006
 * Company:        Workbrain Inc
 * @author         Philip Liew
 * @version 1.0
 */
public class HBMImportTransactionTest extends TestCaseHW
{
    private String skdgrpStore = "JUNIT TEST STORE";
    private String skdgrpDept = "JUNIT TEST DEPARTMENT";

    private int skdgrpStoreId;
    private int skdgrpDeptId;
    private Map jobIdMap = new HashMap();
    private Map actIdMap = new HashMap();
    
	public HBMImportTransactionTest(String arg0)
	{
		super(arg0);
	}

	public static TestSuite suite()
	{
		TestSuite result = new TestSuite();
		result.addTestSuite(HBMImportTransactionTest.class);
		return result;
	}
	
   protected void setUp() throws Exception {
        super.setUp();
        // *** set up test schedule group data
        PreparedStatement ps = null;
        try {
        	// Insert Jobs
            StringBuffer sb = new StringBuffer(200);
            sb.append("INSERT INTO JOB(JOB_ID, JOB_NAME, JOB_START_DATE, JOB_END_DATE, JOB_UNAUTH, LMS_ID) ");
            sb.append("VALUES (?,?,?,?,?,?)");
            ps = getConnection().prepareStatement(sb.toString());

            for (int i=1; i <= 4; i++) {
            	int jobId = getConnection().getDBSequence("seq_job_id").getNextValue();
            	jobIdMap.put("JOB"+i, new Integer(jobId));
                ps.setInt(1, jobId);
                ps.setString(2, "JOB"+i);
                ps.setDate(3, new java.sql.Date(1900,01,01) );
                ps.setDate(4, new java.sql.Date(3000,01,01) );
                ps.setString(5, "N");
                ps.setInt(6, 1);
                ps.addBatch();            	
            }
            ps.executeBatch();

            // Insert Teams for schedules
            sb = new StringBuffer(200);
            sb.append("INSERT INTO WORKBRAIN_TEAM(WBT_ID, WBT_NAME, WBTT_ID, WBT_PARENT_ID)");
            sb.append("VALUES (?,?,?,?)");
            ps = getConnection().prepareStatement(sb.toString());
            
            int wbtId1 = getConnection().getDBSequence("seq_wbt_id").getNextValue();
            int wbtId2 = getConnection().getDBSequence("seq_wbt_id").getNextValue();
            
            ps.setInt(1, wbtId1);
            ps.setString(2, "TEAM1");
            ps.setInt(3, 2);
            ps.setInt(4, 0);
            ps.addBatch();     
            
            ps.setInt(1, wbtId2);
            ps.setString(2, "TEAM2");
            ps.setInt(3, 2);
            ps.setInt(4, 0);
            ps.addBatch();            	
            
            ps.executeBatch();
            
            // Insert schedule locations
            sb = new StringBuffer(200);
            sb.append("INSERT INTO so_schedule_group (skdgrp_id, skdgrp_name, skdgrp_parent_id, clnttyp_id, wbt_id, skdgrp_clientkey)");
            sb.append("VALUES (?,?,?,?,?,?)");
            ps = getConnection().prepareStatement(sb.toString());
            skdgrpStoreId = getConnection().getDBSequence("seq_skdgrp_id").getNextValue(); 
            ps.setInt(1, skdgrpStoreId);
            ps.setString(2, skdgrpStore);
            ps.setInt(3, 1);
            ps.setInt(4, 1);
            ps.setInt(5, wbtId1);
            ps.setString(6, "X");
            ps.addBatch();

            skdgrpDeptId = getConnection().getDBSequence("seq_skdgrp_id").getNextValue(); 
            ps.setInt(1, skdgrpDeptId );
            ps.setString(2, skdgrpDept);
            ps.setInt(3, skdgrpStoreId);
            ps.setInt(4, 1);
            ps.setInt(5, wbtId2);
            ps.setString(6, "Y");
            ps.addBatch();

            ps.executeBatch();

            // Insert Activities
            sb = new StringBuffer(200);
            sb.append("INSERT INTO SO_ACTIVITY(ACT_ID,ACT_NAME, SKDGRP_ID, ACT_PAID, COLR_ID, TCODE_ID, ACT_WORKING, ACT_CONT_TO_COST, ACT_SERVICE) ");
            sb.append("VALUES (?,?,?,?,?,?,?,?,?)");
            ps = getConnection().prepareStatement(sb.toString());
            for ( int i=1; i <= 4; i++) {
            	int actId =  getConnection().getDBSequence("seq_act_id").getNextValue();
            	actIdMap.put("ACTIVITY"+i, new Integer(actId));
            	
                ps.setInt(1, actId);
                ps.setString(2, "ACTIVITY"+i);
                ps.setInt(3, skdgrpDeptId);
                ps.setInt(4, 1);
                ps.setInt(5, 1);
                ps.setInt(6, 1);
                ps.setInt(7, 1);
                ps.setInt(8, 0);
                ps.setString(9, "Y");
                ps.addBatch();            	            	
            }
            ps.executeBatch();

        }
        catch (SQLException e) {
        	System.out.println(e.getMessage());
        }
        finally {
            if (ps != null) ps.close();
        }
    }

	public void testInsertValidData()
		throws Exception
	{
		String csdEffStartDate = "01/01/2000";
		String csdEffEndDate = "01/01/3000";
		
		ResultSet rs = null;
		ResultSet rs2 = null;
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;

		ArrayList al = new ArrayList();

		// Entry with date
		ImportData id1 = new ImportData();
		id1.setField(0, skdgrpStore);
		id1.setField(1, skdgrpDept);
		id1.setField(2, "Description1");
		id1.setField(3, "JOB1");
		id1.setField(4, "ACTIVITY1");
		id1.setField(5, "TESTVOLUME1");
		id1.setField(6, "0.25");
		id1.setField(7, "Fixed");
		id1.setField(8, csdEffStartDate);
		id1.setField(9, csdEffEndDate);
		al.add(id1);

		// Entry without date
		ImportData id2 = new ImportData();
		id2.setField(0, skdgrpStore);
		id2.setField(1, skdgrpDept);
		id2.setField(2, "Description2");
		id2.setField(3, "JOB2");
		id2.setField(4, "ACTIVITY2");
		id2.setField(5, "TESTVOLUME2");
		id2.setField(6, "0.5");
		id2.setField(7, "Fixed");
		al.add(id2);

		HBMImportTransaction hbmTran = new HBMImportTransaction();
		DBConnection conn = getConnection();

		try
		{
			int actId1 = ((Integer)actIdMap.get("ACTIVITY1")).intValue(); 
			int actId2 = ((Integer)actIdMap.get("ACTIVITY2")).intValue();
			int jobId1 = ((Integer)jobIdMap.get("JOB1")).intValue();
			int jobId2 = ((Integer)jobIdMap.get("JOB2")).intValue();

			// Run test
			hbmTran.processBatch(conn, al);
			
			// Check if client staffing requirement is entered properly
			ps = conn.prepareStatement("select csd_id from so_client_stfdef where csd_id in ((select csd_id from so_client_stfdef where skdgrp_id=? and job_id=? and act_id=?),(select csd_id from so_client_stfdef where skdgrp_id=? and job_id=? and act_id=?))");
			ps.setInt(1, skdgrpDeptId);
			ps.setInt(2, jobId1);
			ps.setInt(3, actId1);
			ps.setInt(4, skdgrpDeptId);
			ps.setInt(5, jobId2);
			ps.setInt(6, actId2);

			rs = ps.executeQuery();

			ps2 = conn.prepareStatement("select wrkld_id from so_volume_workload where csd_id in (?,?)");
			
			int i=0;
			while(rs.next())
			{
				i++;
				ps2.setInt(i,rs.getInt(1));
			}

			if(i!=2)
			{
				assertTrue(false);
			}
			
			rs2 = ps2.executeQuery();
			i=0;
			while (rs2.next())
			{
				i++;
			}
			if ( i!=2) {
				assertTrue(false);
			}
			
			// Update data and test if it updated properly
			Date startDate = DateHelper.convertStringToDate("01/01/1900", "MM/dd/yyyy");
			Date endDate = DateHelper.convertStringToDate("01/01/1999", "MM/dd/yyyy");
			id1.setField(6, "4");
			id1.setField(8, "01/01/1900");
			id1.setField(9, "01/01/1999");
			
			hbmTran.processBatch(conn, al);
			
			// Check if client staffing requirement is entered properly
			ps = conn.prepareStatement("select csd_id, csd_eff_start_date, csd_eff_end_date from so_client_stfdef where csd_id in ((select csd_id from so_client_stfdef where skdgrp_id=? and job_id=? and act_id=?))");
			ps.setInt(1, skdgrpDeptId);
			ps.setInt(2, jobId1);
			ps.setInt(3, actId1);
	
			rs = ps.executeQuery();
	
			ps2 = conn.prepareStatement("select wrkld_stdvol_hour from so_volume_workload where csd_id = ?");
			
			int j=0;
			Date dbStartDate = null;
			Date dbEndDate = null;
			while(rs.next())
			{
				j++;
				ps2.setInt(j,rs.getInt(1));
				dbStartDate = rs.getDate(2);
				dbEndDate = rs.getDate(3);
			}
	
			if(j!=1 || startDate.getTime() != dbStartDate.getTime() || endDate.getTime() != dbEndDate.getTime())
			{
				assertTrue(false);
			}
			
			rs2 = ps2.executeQuery();
			j=0;
			double vol = 0;
			while (rs2.next())
			{
				j++;
				vol = rs2.getDouble(1);
			}
			if ( j!=1 || vol != 0.25) {
				assertTrue(false);
			}

		}
		catch (Exception e) 
		{
			System.out.println(e.getMessage());
			assertTrue(false);
		}
		finally
		{
			if(rs != null)
			{
				rs.close();
			}
			if(ps != null)
			{
				ps.close();
			}
		}
		assertTrue(true);		
	}

	public void testInvalidSkdGrpData()
		throws Exception
	{
		String csdEffStartDate = "01/01/2000";
		String csdEffEndDate = "01/01/3000";
		
		ResultSet rs = null;
		PreparedStatement ps = null;
	
		ArrayList al = new ArrayList();
	
		// Empty Store Field
		ImportData id1 = new ImportData();
		//id1.setField(0, skdgrpStore);
		id1.setField(1, skdgrpDept);
		id1.setField(2, "No Store");
		id1.setField(3, "JOB1");
		id1.setField(4, "ACTIVITY1");
		id1.setField(5, "TESTVOLUME1");
		id1.setField(6, "0.25");
		id1.setField(7, "Fixed");
		id1.setField(8, csdEffStartDate);
		id1.setField(9, csdEffEndDate);
		al.add(id1);
	
		// Entry with non-existent store
		ImportData id2 = new ImportData();
		id2.setField(0, "FAKESTORE");
		id2.setField(1, skdgrpDept);
		id2.setField(2, "Wrong Store");
		id2.setField(3, "JOB2");
		id2.setField(4, "ACTIVITY2");
		id2.setField(5, "TESTVOLUME2");
		id2.setField(6, "0.5");
		id2.setField(7, "Fixed");
		id2.setField(8, csdEffStartDate);
		id2.setField(9, csdEffEndDate);
		al.add(id2);

		// Empty Department field
		ImportData id3 = new ImportData();
		id3.setField(0, skdgrpStore);
//		id3.setField(1, skdgrpDept);
		id3.setField(2, "Wrong Dept");
		id3.setField(3, "JOB3");
		id3.setField(4, "ACTIVITY3");
		id3.setField(5, "TESTVOLUME3");
		id3.setField(6, "0.2");
		id3.setField(7, "Fixed");
		id3.setField(8, csdEffStartDate);
		id3.setField(9, csdEffEndDate);
		al.add(id3);

		// Non-existent Department field
		ImportData id4 = new ImportData();
		id4.setField(0, skdgrpStore);
		id4.setField(1, "FAKEDEPT");
		id4.setField(2, "Wrong Dept");
		id4.setField(3, "JOB4");
		id4.setField(4, "ACTIVITY4");
		id4.setField(5, "TESTVOLUME4");
		id4.setField(6, "0.2");
		id4.setField(7, "Fixed");
		id4.setField(8, csdEffStartDate);
		id4.setField(9, csdEffEndDate);
		al.add(id4);

		HBMImportTransaction hbmTran = new HBMImportTransaction();
		DBConnection conn = getConnection();
	
		try
		{
			int actId1 = ((Integer)actIdMap.get("ACTIVITY1")).intValue(); 
			int actId2 = ((Integer)actIdMap.get("ACTIVITY2")).intValue();
			int actId3 = ((Integer)actIdMap.get("ACTIVITY3")).intValue();
			int actId4 = ((Integer)actIdMap.get("ACTIVITY4")).intValue();
			int jobId1 = ((Integer)jobIdMap.get("JOB1")).intValue();
			int jobId2 = ((Integer)jobIdMap.get("JOB2")).intValue();
			int jobId3 = ((Integer)jobIdMap.get("JOB3")).intValue();
			int jobId4 = ((Integer)jobIdMap.get("JOB4")).intValue();

			// Run test
			hbmTran.processBatch(conn, al);
			
			// Check if client staffing requirement is entered properly
			StringBuffer sb = new StringBuffer("select csd_id from so_client_stfdef where csd_id in (");
			sb.append("(select csd_id from so_client_stfdef where skdgrp_id=? and job_id=? and act_id=?)");
			sb.append(",");
			sb.append("(select csd_id from so_client_stfdef where skdgrp_id=? and job_id=? and act_id=?)");
			sb.append(",");
			sb.append("(select csd_id from so_client_stfdef where skdgrp_id=? and job_id=? and act_id=?)");
			sb.append(",");
			sb.append("(select csd_id from so_client_stfdef where skdgrp_id=? and job_id=? and act_id=?)");
			sb.append(")");
			
			ps = conn.prepareStatement(sb.toString());
			ps.setInt(1, skdgrpDeptId);
			ps.setInt(2, jobId1);
			ps.setInt(3, actId1);
			ps.setInt(4, skdgrpDeptId);
			ps.setInt(5, jobId2);
			ps.setInt(6, actId2);
			ps.setInt(7, skdgrpDeptId);
			ps.setInt(8, jobId3);
			ps.setInt(9, actId3);
			ps.setInt(10, skdgrpDeptId);
			ps.setInt(11, jobId4);
			ps.setInt(12, actId4);

			rs = ps.executeQuery();
			
			int i=0;
			while(rs.next())
			{
				i++;
			}
			if(i!=0)
			{
				assertTrue(false);
			}			
		}
		catch (Exception e) 
		{
			System.out.println(e.getMessage());
			assertTrue(false);
		}
		finally
		{
			if(rs != null)
			{
				rs.close();
			}
			if(ps != null)
			{
				ps.close();
			}
		}
		assertTrue(true);		
	}

	public void testInvalidActivityData()
		throws Exception
	{
		String csdEffStartDate = "01/01/2000";
		String csdEffEndDate = "01/01/3000";
		
		ResultSet rs = null;
		PreparedStatement ps = null;
	
		ArrayList al = new ArrayList();
	
		// Empty Activity Field
		ImportData id1 = new ImportData();
		id1.setField(0, skdgrpStore);
		id1.setField(1, skdgrpDept);
		id1.setField(2, "No Store");
		id1.setField(3, "JOB1");
//		id1.setField(4, "ACTIVITY1");
		id1.setField(5, "TESTVOLUME1");
		id1.setField(6, "0.25");
		id1.setField(7, "Fixed");
		id1.setField(8, csdEffStartDate);
		id1.setField(9, csdEffEndDate);
		al.add(id1);
	
		// Entry non-existent Activity
		ImportData id2 = new ImportData();
		id2.setField(0, skdgrpStore);
		id2.setField(1, skdgrpDept);
		id2.setField(2, "Wrong Activity");
		id2.setField(3, "JOB2");
		id2.setField(4, "FAKEACTIVITY");
		id2.setField(5, "TESTVOLUME2");
		id2.setField(6, "0.5");
		id2.setField(7, "Fixed");
		id2.setField(8, csdEffStartDate);
		id2.setField(9, csdEffEndDate);
		al.add(id2);
	
		HBMImportTransaction hbmTran = new HBMImportTransaction();
		DBConnection conn = getConnection();
	
		try
		{
			int actId1 = ((Integer)actIdMap.get("ACTIVITY1")).intValue(); 
			int actId2 = ((Integer)actIdMap.get("ACTIVITY2")).intValue();
			int jobId1 = ((Integer)jobIdMap.get("JOB1")).intValue();
			int jobId2 = ((Integer)jobIdMap.get("JOB2")).intValue();
	
			// Run test
			hbmTran.processBatch(conn, al);
			
			// Check if client staffing requirement is entered properly
			ps = conn.prepareStatement("select csd_id from so_client_stfdef where csd_id in ((select csd_id from so_client_stfdef where skdgrp_id=? and job_id=? and act_id=?),(select csd_id from so_client_stfdef where skdgrp_id=? and job_id=? and act_id=?))");
			ps.setInt(1, skdgrpDeptId);
			ps.setInt(2, jobId1);
			ps.setInt(3, actId1);
			ps.setInt(4, skdgrpDeptId);
			ps.setInt(5, jobId2);
			ps.setInt(6, actId2);
	
			rs = ps.executeQuery();
			
			int i=0;
			while(rs.next())
			{
				i++;
			}
			if(i!=0)
			{
				assertTrue(false);
			}			
		}
		catch (Exception e) 
		{
			System.out.println(e.getMessage());
			assertTrue(false);
		}
		finally
		{
			if(rs != null)
			{
				rs.close();
			}
			if(ps != null)
			{
				ps.close();
			}
		}
		assertTrue(true);		
	}
	
	public void testInvalidJobData()
		throws Exception
	{
		String csdEffStartDate = "01/01/2000";
		String csdEffEndDate = "01/01/3000";
		
		ResultSet rs = null;
		PreparedStatement ps = null;
	
		ArrayList al = new ArrayList();
	
		// Empty Job Field
		ImportData id1 = new ImportData();
		id1.setField(0, skdgrpStore);
		id1.setField(1, skdgrpDept);
		id1.setField(2, "No Store");
//		id1.setField(3, "JOB1");
		id1.setField(4, "ACTIVITY1");
		id1.setField(5, "TESTVOLUME1");
		id1.setField(6, "0.25");
		id1.setField(7, "Fixed");
		id1.setField(8, csdEffStartDate);
		id1.setField(9, csdEffEndDate);
		al.add(id1);
	
		// Entry non-existent Job
		ImportData id2 = new ImportData();
		id2.setField(0, skdgrpStore);
		id2.setField(1, skdgrpDept);
		id2.setField(2, "Wrong Job");
		id2.setField(3, "FAKEJOB2");
		id2.setField(4, "ACTIVITY2");
		id2.setField(5, "TESTVOLUME2");
		id2.setField(6, "0.5");
		id2.setField(7, "Fixed");
		id2.setField(8, csdEffStartDate);
		id2.setField(9, csdEffEndDate);
		al.add(id2);
	
		HBMImportTransaction hbmTran = new HBMImportTransaction();
		DBConnection conn = getConnection();
	
		try
		{
			int actId1 = ((Integer)actIdMap.get("ACTIVITY1")).intValue(); 
			int actId2 = ((Integer)actIdMap.get("ACTIVITY2")).intValue();
			int jobId1 = ((Integer)jobIdMap.get("JOB1")).intValue();
			int jobId2 = ((Integer)jobIdMap.get("JOB2")).intValue();
	
			// Run test
			hbmTran.processBatch(conn, al);
			
			// Check if client staffing requirement is entered properly
			ps = conn.prepareStatement("select csd_id from so_client_stfdef where csd_id in ((select csd_id from so_client_stfdef where skdgrp_id=? and job_id=? and act_id=?),(select csd_id from so_client_stfdef where skdgrp_id=? and job_id=? and act_id=?))");
			ps.setInt(1, skdgrpDeptId);
			ps.setInt(2, jobId1);
			ps.setInt(3, actId1);
			ps.setInt(4, skdgrpDeptId);
			ps.setInt(5, jobId2);
			ps.setInt(6, actId2);
	
			rs = ps.executeQuery();
			
			int i=0;
			while(rs.next())
			{
				i++;
			}
			if(i!=0)
			{
				assertTrue(false);
			}			
		}
		catch (Exception e) 
		{
			System.out.println(e.getMessage());
			assertTrue(false);
		}
		finally
		{
			if(rs != null)
			{
				rs.close();
			}
			if(ps != null)
			{
				ps.close();
			}
		}
		assertTrue(true);		
	}
	
//	public void testUpdateValidData()
//		throws Exception
//	{
//		String csdEffStartDate = "01/01/2000";
//		String csdEffEndDate = "01/01/3000";
//		
//		ResultSet rs = null;
//		ResultSet rs2 = null;
//		PreparedStatement ps = null;
//		PreparedStatement ps2 = null;
//	
//		ArrayList al = new ArrayList();
//	
//		// Entry with date
//		ImportData id1 = new ImportData();
//		id1.setField(0, skdgrpStore);
//		id1.setField(1, skdgrpDept);
//		id1.setField(2, "Description1");
//		id1.setField(3, "JOB1");
//		id1.setField(4, "ACTIVITY1");
//		id1.setField(5, "TESTVOLUME1");
//		id1.setField(6, "0.25");
//		id1.setField(7, "Fixed");
//		id1.setField(8, csdEffStartDate);
//		id1.setField(9, csdEffEndDate);
//		al.add(id1);
//	
//		// Entry without date
//		ImportData id2 = new ImportData();
//		id2.setField(0, skdgrpStore);
//		id2.setField(1, skdgrpDept);
//		id2.setField(2, "Description2");
//		id2.setField(3, "JOB2");
//		id2.setField(4, "ACTIVITY2");
//		id2.setField(5, "TESTVOLUME2");
//		id2.setField(6, "0.5");
//		id2.setField(7, "Fixed");
//		al.add(id2);
//	
//		HBMImportTransaction hbmTran = new HBMImportTransaction();
//		DBConnection conn = getConnection();
//	
//		try
//		{
//			int actId1 = ((Integer)actIdMap.get("ACTIVITY1")).intValue(); 
//			int jobId1 = ((Integer)jobIdMap.get("JOB1")).intValue();
//			// Run Processing
//			hbmTran.processBatch(conn, al);
//			
//			// Update data
//			Date startDate = DateHelper.convertStringToDate("01/01/1900", "MM/dd/yyyy");
//			Date endDate = DateHelper.convertStringToDate("01/01/1999", "MM/dd/yyyy");
//			id1.setField(6, "4");
//			id1.setField(8, "01/01/1900");
//			id1.setField(9, "01/01/1999");
//			
//			hbmTran.processBatch(conn, al);
//			
//			// Check if client staffing requirement is entered properly
//			ps = conn.prepareStatement("select csd_id, csd_eff_start_date, csd_eff_end_date from so_client_stfdef where csd_id in ((select csd_id from so_client_stfdef where skdgrp_id=? and job_id=? and act_id=?))");
//			ps.setInt(1, skdgrpDeptId);
//			ps.setInt(2, jobId1);
//			ps.setInt(3, actId1);
//	
//			rs = ps.executeQuery();
//	
//			ps2 = conn.prepareStatement("select wrkld_stdvol_hour from so_volume_workload where csd_id = ?");
//			
//			int i=0;
//			Date dbStartDate = null;
//			Date dbEndDate = null;
//			while(rs.next())
//			{
//				i++;
//				ps2.setInt(i,rs.getInt(1));
//				dbStartDate = rs.getDate(2);
//				dbEndDate = rs.getDate(3);
//			}
//	
//			if(i!=1 || startDate.getTime() != dbStartDate.getTime() || endDate.getTime() != dbEndDate.getTime())
//			{
//				assertTrue(false);
//			}
//			
//			rs2 = ps2.executeQuery();
//			i=0;
//			double vol = 0;
//			while (rs2.next())
//			{
//				i++;
//				vol = rs2.getDouble(1);
//			}
//			if ( i!=1 || vol != 0.25) {
//				assertTrue(false);
//			}
//		}
//		catch (Exception e) 
//		{
//			System.out.println(e.getMessage());
//			assertTrue(false);
//		}
//		finally
//		{
//			if(rs != null)
//			{
//				rs.close();
//			}
//			if(ps != null)
//			{
//				ps.close();
//			}
//		}
//		assertTrue(true);		
//	}

	public void testInvalidDate()
			throws Exception
		{
			// Invalid date
			String csdEffStartDate = "2000-01-01";
			// Flawed date doing dd/MM/yyyy (proper format is MM/dd/yyyy
			String csdEffEndDate = "26/01/3000";
			
			ResultSet rs = null;
			PreparedStatement ps = null;
		
			ArrayList al = new ArrayList();
		
			// Empty Job Field
			ImportData id1 = new ImportData();
			id1.setField(0, skdgrpStore);
			id1.setField(1, skdgrpDept);
			id1.setField(2, "No Store");
	//		id1.setField(3, "JOB1");
			id1.setField(4, "ACTIVITY1");
			id1.setField(5, "TESTVOLUME1");
			id1.setField(6, "0.25");
			id1.setField(7, "Fixed");
			id1.setField(8, csdEffStartDate);
			id1.setField(9, "01/01/3000");
			al.add(id1);
		
			// Entry non-existent Job
			ImportData id2 = new ImportData();
			id2.setField(0, skdgrpStore);
			id2.setField(1, skdgrpDept);
			id2.setField(2, "Wrong Job");
			id2.setField(3, "FAKEJOB2");
			id2.setField(4, "ACTIVITY2");
			id2.setField(5, "TESTVOLUME2");
			id2.setField(6, "0.5");
			id2.setField(7, "Fixed");
			id2.setField(8, "01/01/2000");
			id2.setField(9, csdEffEndDate);
			al.add(id2);
		
			HBMImportTransaction hbmTran = new HBMImportTransaction();
			DBConnection conn = getConnection();
		
			try
			{
				int actId1 = ((Integer)actIdMap.get("ACTIVITY1")).intValue(); 
				int actId2 = ((Integer)actIdMap.get("ACTIVITY2")).intValue();
				int jobId1 = ((Integer)jobIdMap.get("JOB1")).intValue();
				int jobId2 = ((Integer)jobIdMap.get("JOB2")).intValue();
		
				// Run test
				hbmTran.processBatch(conn, al);
				
				// Check if client staffing requirement is entered properly
				ps = conn.prepareStatement("select csd_id from so_client_stfdef where csd_id in ((select csd_id from so_client_stfdef where skdgrp_id=? and job_id=? and act_id=?),(select csd_id from so_client_stfdef where skdgrp_id=? and job_id=? and act_id=?))");
				ps.setInt(1, skdgrpDeptId);
				ps.setInt(2, jobId1);
				ps.setInt(3, actId1);
				ps.setInt(4, skdgrpDeptId);
				ps.setInt(5, jobId2);
				ps.setInt(6, actId2);
		
				rs = ps.executeQuery();
				
				int i=0;
				while(rs.next())
				{
					i++;
				}
				if(i!=0)
				{
					assertTrue(false);
				}			
			}
			catch (Exception e) 
			{
				System.out.println(e.getMessage());
				assertTrue(false);
			}
			finally
			{
				if(rs != null)
				{
					rs.close();
				}
				if(ps != null)
				{
					ps.close();
				}
			}
			assertTrue(true);		
		}

	public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
	}

}
