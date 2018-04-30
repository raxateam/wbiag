/*
 * Created on Dec 8, 2004
 *
 */
package com.wbiag.app.wbinterface.pos;

import com.workbrain.test.*;
import com.workbrain.sql.*;
//import com.workbrain.server.sql.ConnectionManager;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.util.*;

import java.sql.*;
import java.util.*;

import junit.framework.*;

/** Title:         POSImportTransactionTest
 * Description:    Junit test for POSImportTransaction
 * Copyright:      Copyright (c) 2003
 * Company:        Workbrain Inc
 * @author         Kevin Tsoi
 * @version 1.0
 */
public class POSImportTransactionTest extends TestCaseHW
{

    private String skdgrpNameTest1 = "JUNIT TEST LOCATION 1";
    private String skdgrpNameTest2 = "JUNIT TEST LOCATION 2";

	public POSImportTransactionTest(String arg0)
	{
		super(arg0);
	}

    protected void setUp() throws Exception {
        super.setUp();
        // *** set up test schedule group data
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("INSERT INTO so_schedule_group (skdgrp_id, skdgrp_name, skdgrp_parent_id, clnttyp_id, wbt_id, skdgrp_clientkey)");
            sb.append("VALUES (?,?,?,?,?,?)");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setInt(1  , getConnection().getDBSequence("seq_skdgrp_id").getNextValue() );
            ps.setString(2, skdgrpNameTest1);
            ps.setInt(3, 1);
            ps.setInt(4, 1);
            ps.setInt(5, 0);
            ps.setString(6, "X");
            ps.addBatch();

            ps.setInt(1  , getConnection().getDBSequence("seq_skdgrp_id").getNextValue() );
            ps.setString(2, skdgrpNameTest2);
            ps.setInt(3, 1);
            ps.setInt(4, 1);
            ps.setInt(5, 1);
            ps.setString(6, "Y");
            ps.addBatch();

            ps.executeBatch();
        }
        catch (SQLException e) {

        }
        finally {
            if (ps != null) ps.close();
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // *** remove test schedule group data
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM so_schedule_group WHERE skdgrp_name in (?, ?)");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setString(1  , skdgrpNameTest1);
            ps.setString(2  , skdgrpNameTest2);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        getCodeMapper().invalidateTable("SO_SCHEDULE_GROUP");


        getConnection().commit();
    }

	public static TestSuite suite()
	{
		TestSuite result = new TestSuite();
		result.addTestSuite(POSImportTransactionTest.class);
		return result;
	}

	public void testAggregateAndRound () throws Exception
	{

		String dateStr = "01/01/3000";
		String timeStr = dateStr+"00:00:00";
		Datetime resdetDate = DateHelper.parseDate(dateStr, "MM/dd/yyyy");
		Datetime resdetTime = DateHelper.parseDate(timeStr, "MM/dd/yyyyHH:mm:ss");

		int volume = 0;

		ResultSet rs = null;
		PreparedStatement ps = null;

		ArrayList al = new ArrayList();

		//daily interval
		ImportData id1 = new ImportData();
		id1.setField(0, skdgrpNameTest1);
		id1.setField(1, "1");
		id1.setField(2, dateStr);
		id1.setField(3, "14:59:00");
		id1.setField(4, "1");
		id1.setField(5, "0");
		id1.setField(6, "");
		al.add(id1);

		//60 min interval
		ImportData id2 = new ImportData();
		id2.setField(0, skdgrpNameTest1);
		id2.setField(1, "2");
		id2.setField(2, dateStr);
		id2.setField(3, "00:59:00");
		id2.setField(4, "2");
		id2.setField(5, "0");
		id2.setField(6, "");
		al.add(id2);

		//30 min interval
		ImportData id3 = new ImportData();
		id3.setField(0, skdgrpNameTest1);
		id3.setField(1, "3");
		id3.setField(2, dateStr);
		id3.setField(3, "00:29:00");
		id3.setField(4, "3");
		id3.setField(5, "0");
		id3.setField(6, "");
		al.add(id3);

		//15 min interval
		ImportData id4 = new ImportData();
		id4.setField(0, skdgrpNameTest1);
		id4.setField(1, "4");
		id4.setField(2, dateStr);
		id4.setField(3, "00:14:00");
		id4.setField(4, "4");
		id4.setField(5, "0");
		id4.setField(6, "");
		al.add(id4);

		POSImportTransaction postran = new POSImportTransaction();
		DBConnection conn = getConnection();

		try
		{
			postran.processAggregate(conn, al);
			ps = conn.prepareStatement("select RD.resdet_id, RD.resdet_volume from so_results_detail RD, so_schedule_group SG where RD.skdgrp_id=SG.skdgrp_id and SG.skdgrp_name=? and RD.resdet_date=? and RD.resdet_time=?");
			ps.setString(1, skdgrpNameTest1);
			ps.setTimestamp(2, resdetDate);
			ps.setTimestamp(3, resdetTime);

			rs = ps.executeQuery();

			int i=0;
			int resdetId = 0;
			while(rs.next())
			{
				i++;
				resdetId = rs.getInt(1);
				volume = rs.getInt(2);
			}

			if(i==1)
			{
				//clean up db
				ps = conn.prepareStatement("delete from so_results_detail where resdet_id=?");
				ps.setInt(1, resdetId);
				ps.execute();
				conn.commit();
			}
			else
			{
				assertTrue(false);
			}
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
		assertTrue(volume == 10);

	}

	public void testUpdate() throws Exception
	{
		String dateStr = "02/01/3000";
		String timeStr = dateStr+"00:00:00";
		Datetime resdetDate = DateHelper.parseDate(dateStr, "MM/dd/yyyy");
		Datetime resdetTime = DateHelper.parseDate(timeStr, "MM/dd/yyyyHH:mm:ss");

		int volume = 0;

		ResultSet rs = null;
		PreparedStatement ps = null;

		ArrayList al1 = new ArrayList();
		ArrayList al2 = new ArrayList();

		//old record
		ImportData id1 = new ImportData();
		id1.setField(0, skdgrpNameTest1);
		id1.setField(1, "1");
		id1.setField(2, dateStr);
		id1.setField(3, "14:59:00");
		id1.setField(4, "1");
		id1.setField(5, "0");
		id1.setField(6, "");
		al1.add(id1);

		//new record
		ImportData id2 = new ImportData();
		id2.setField(0, skdgrpNameTest1);
		id2.setField(1, "1");
		id2.setField(2, dateStr);
		id2.setField(3, "14:59:00");
		id2.setField(4, "2");
		id2.setField(5, "0");
		id2.setField(6, "");
		al2.add(id2);

		POSImportTransaction postran = new POSImportTransaction();
		DBConnection conn = getConnection();

		try
		{
			postran.processAggregate(conn, al1);
			postran.processAggregate(conn, al2);

			ps = conn.prepareStatement("select RD.resdet_id, RD.resdet_volume from so_results_detail RD, so_schedule_group SG where RD.skdgrp_id=SG.skdgrp_id and SG.skdgrp_name=? and RD.resdet_date=? and RD.resdet_time=?");

			ps.setString(1, skdgrpNameTest1);
			ps.setTimestamp(2, resdetDate);
			ps.setTimestamp(3, resdetTime);
			rs = ps.executeQuery();

			//new record should exist
			int i=0;
			int resdetId = 0;
			while(rs.next())
			{
				i++;
				resdetId = rs.getInt(1);
				volume = rs.getInt(2);
			}

			if(i==1)
			{
				//clean up db
				ps = conn.prepareStatement("delete from so_results_detail where resdet_id=?");
				ps.setInt(1, resdetId);
				ps.execute();
				conn.commit();
			}
			else
			{
				assertTrue(false);
			}
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
		assertTrue(volume == 3);

	}

	public void testHistoricUpdate() throws Exception
	{
		String oldSkdgrpName = skdgrpNameTest1;
		String newSkdgrpName = skdgrpNameTest2;
		String dateStr = "03/01/3000";
		String timeStr = dateStr+"00:00:00";
		Datetime resdetDate = DateHelper.parseDate(dateStr, "MM/dd/yyyy");
		Datetime resdetTime = DateHelper.parseDate(timeStr, "MM/dd/yyyyHH:mm:ss");

		ResultSet rs = null;
		PreparedStatement ps = null;

		ArrayList al1 = new ArrayList();
		ArrayList al2 = new ArrayList();

		//old records
		ImportData id1 = new ImportData();
		id1.setField(0, oldSkdgrpName);
		id1.setField(1, "1");
		id1.setField(2, dateStr);
		id1.setField(3, "14:59:00");
		id1.setField(4, "1");
		id1.setField(5, "0");
		id1.setField(6, "");
		al1.add(id1);

		ImportData id2 = new ImportData();
		id2.setField(0, newSkdgrpName);
		id2.setField(1, "1");
		id2.setField(2, dateStr);
		id2.setField(3, "14:59:00");
		id2.setField(4, "5");
		id2.setField(5, "0");
		id2.setField(6, "");
		al1.add(id2);

		//historic update
		ImportData id3 = new ImportData();
		id3.setField(0, newSkdgrpName);
		id3.setField(1, "1");
		id3.setField(2, dateStr);
		id3.setField(3, "14:59:00");
		id3.setField(4, "1");
		id3.setField(5, "0");
		id3.setField(6, oldSkdgrpName);
		al2.add(id3);

		POSImportTransaction postran = new POSImportTransaction();
		DBConnection conn = getConnection();

		try
		{
			postran.processAggregate(conn, al1);
			postran.processAggregate(conn, al2);

			ps = conn.prepareStatement("select RD.resdet_id, RD.resdet_volume from so_results_detail RD, so_schedule_group SG where RD.skdgrp_id=SG.skdgrp_id and SG.skdgrp_name=? and RD.resdet_date=? and RD.resdet_time=?");

			ps.setString(1, oldSkdgrpName);
			ps.setTimestamp(2, resdetDate);
			ps.setTimestamp(3, resdetTime);
			rs = ps.executeQuery();

			//old record should no longer exist
			if(!rs.next())
			{
				assertTrue(true);
			}
			else
			{
				assertTrue(false);
			}

			ps.setString(1, newSkdgrpName);
			ps.setTimestamp(2, resdetDate);
			ps.setTimestamp(3, resdetTime);
			rs = ps.executeQuery();

			//new record should exist
			int i=0;
			int resdetId = 0;
			while(rs.next())
			{
				i++;
				resdetId = rs.getInt(1);
				assertTrue(1 == rs.getInt(2));
			}

			if(i==1)
			{
				//clean up db
				ps = conn.prepareStatement("delete from so_results_detail where resdet_id=?");
				ps.setInt(1, resdetId);
				ps.execute();
				conn.commit();
			}
			else
			{
				assertTrue(false);
			}
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

	}

	public void testInvalidHistoricUpdate() throws Exception
	{
		String oldSkdgrpName = "NONEXISTENT GROUP";
		String newSkdgrpName = skdgrpNameTest2;
		String dateStr = "03/01/3000";
		String timeStr = dateStr+"00:00:00";
		Datetime resdetDate = DateHelper.parseDate(dateStr, "MM/dd/yyyy");
		Datetime resdetTime = DateHelper.parseDate(timeStr, "MM/dd/yyyyHH:mm:ss");

		ResultSet rs = null;
		PreparedStatement ps = null;

		ArrayList al = new ArrayList();

		//invalid historic update
		ImportData id = new ImportData();
		id.setField(0, newSkdgrpName);
		id.setField(1, "1");
		id.setField(2, dateStr);
		id.setField(3, "14:59:00");
		id.setField(4, "2");
		id.setField(5, "0");
		id.setField(6, oldSkdgrpName);
		al.add(id);

		POSImportTransaction postran = new POSImportTransaction();
		DBConnection conn = getConnection();

		try
		{
			postran.processAggregate(conn, al);
		}
		catch (Exception e)
		{
			assertTrue(e.toString().indexOf(POSImportTransaction.HISTORIC_UPDATE_ERROR_MESSAGE) != -1);
		}
	}

	public void testInvalidData() throws Exception
	{
		String skdgrpName = "INVALID SKDGRP_NAME";
		String dateStr = "03/01/3000";
		String timeStr = dateStr+"00:00:00";
		Datetime resdetDate = DateHelper.parseDate(dateStr, "MM/dd/yyyy");
		Datetime resdetTime = DateHelper.parseDate(timeStr, "MM/dd/yyyyHH:mm:ss");

		ResultSet rs = null;
		PreparedStatement ps = null;

		ArrayList al = new ArrayList();

		//invalid skpgrp_name
		ImportData id = new ImportData();
		id.setField(0, skdgrpName);
		id.setField(1, "1");
		id.setField(2, dateStr);
		id.setField(3, "14:59:00");
		id.setField(4, "2");
		id.setField(5, "0");
		id.setField(6, "");
		al.add(id);

		POSImportTransaction postran = new POSImportTransaction();
		DBConnection conn = getConnection();

		try
		{
			postran.processAggregate(conn, al);
		}
		catch (Exception e)
		{
			assertTrue(e.toString().indexOf(POSImportTransaction.INVALID_DATA_ERROR_MESSAGE) != -1);
		}
	}

	public void testNoAggregate () throws Exception
	{
		String dateStr = "01/01/3000";
		String timeStr = dateStr+"00:00:00";
		Datetime resdetDate = DateHelper.parseDate(dateStr, "MM/dd/yyyy");
		Datetime resdetTime = DateHelper.parseDate(timeStr, "MM/dd/yyyyHH:mm:ss");

		int volume = 0;

		ResultSet rs = null;
		PreparedStatement ps = null;

		ArrayList al = new ArrayList();

		//no round
		ImportData id1 = new ImportData();
		id1.setField(0, skdgrpNameTest1);
		id1.setField(1, "1");
		id1.setField(2, dateStr);
		id1.setField(3, "00:00:00");
		id1.setField(4, "1");
		id1.setField(5, "1");
		id1.setField(6, "");
		al.add(id1);

		POSImportTransaction postran = new POSImportTransaction();
		DBConnection conn = getConnection();

		try
		{
			postran.processNoAggregate(conn, al);
			ps = conn.prepareStatement("select RD.resdet_id, RD.resdet_volume from so_results_detail RD, so_schedule_group SG where RD.skdgrp_id=SG.skdgrp_id and SG.skdgrp_name=? and RD.resdet_date=? and RD.resdet_time=?");
			ps.setString(1, skdgrpNameTest1);
			ps.setTimestamp(2, resdetDate);
			ps.setTimestamp(3, resdetTime);

			rs = ps.executeQuery();

			int i=0;
			int resdetId = 0;
			while(rs.next())
			{
				i++;
				resdetId = rs.getInt(1);
				volume = rs.getInt(2);
			}

			if(i==1)
			{
				//clean up db
				ps = conn.prepareStatement("delete from so_results_detail where resdet_id=?");
				ps.setInt(1, resdetId);
				ps.execute();
				conn.commit();
			}
			else
			{
				assertTrue(false);
			}
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
		assertTrue(volume == 1);

	}


    public void testNoAggregateVoltyp () throws Exception
    {
        String dateStr = "01/01/3000";
        String timeStr = dateStr+"00:00:00";
        Datetime resdetDate = DateHelper.parseDate(dateStr, "MM/dd/yyyy");
        Datetime resdetTime = DateHelper.parseDate(timeStr, "MM/dd/yyyyHH:mm:ss");
        final int UNIT_VOLTYPID = 1000; final String UNIT_VOLTYPNAME = "UNIT";
        int volume = 0;
        int volTypId = 0;

        ResultSet rs = null;
        PreparedStatement ps = null;

        ArrayList al = new ArrayList();

        //no round
        ImportData id1 = new ImportData();
        id1.setField(0, skdgrpNameTest1);
        id1.setField(1, "1");
        id1.setField(2, dateStr);
        id1.setField(3, "00:00:00");
        id1.setField(4, "1");
        id1.setField(5, "1");
        id1.setField(6, "");
        id1.setField(POSImportTransaction.VOLTYP_COL , UNIT_VOLTYPNAME);
        al.add(id1);

        POSImportTransaction postran = new POSImportTransaction();
        postran.initializeTransaction(getConnection() );
        DBConnection conn = getConnection();

        try
        {
            postran.processNoAggregate(conn, al);
            ps = conn.prepareStatement("select RD.resdet_id, RD.resdet_volume, RD.voltyp_id from so_results_detail RD, so_schedule_group SG where RD.skdgrp_id=SG.skdgrp_id and SG.skdgrp_name=? and RD.resdet_date=? and RD.resdet_time=?");
            ps.setString(1, skdgrpNameTest1);
            ps.setTimestamp(2, resdetDate);
            ps.setTimestamp(3, resdetTime);

            rs = ps.executeQuery();

            int i=0;
            int resdetId = 0;
            while(rs.next())
            {
                i++;
                resdetId = rs.getInt(1);
                volume = rs.getInt(2);
                volTypId = rs.getInt(3);
            }

            if(i==1)
            {
                //clean up db
                ps = conn.prepareStatement("delete from so_results_detail where resdet_id=?");
                ps.setInt(1, resdetId);
                ps.execute();
                conn.commit();
            }
            else
            {
                assertTrue(false);
            }
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
        assertTrue(volume == 1);
        assertEquals(UNIT_VOLTYPID,  volTypId);

    }

	public void testNoAggHistUpdateVoltyp() throws Exception
	{
		String oldSkdgrpName = skdgrpNameTest1;
		String newSkdgrpName = skdgrpNameTest2;
		String dateStr = "03/01/3000";
		String timeStr = dateStr+"00:00:00";
		Datetime resdetDate = DateHelper.parseDate(dateStr, "MM/dd/yyyy");
		Datetime resdetTime = DateHelper.parseDate(timeStr, "MM/dd/yyyyHH:mm:ss");
		final int UNIT_VOLTYPID = 1000; 
		final String UNIT_VOLTYPNAME = "UNIT";

		ResultSet rs = null;
		PreparedStatement ps = null;

		ArrayList al1 = new ArrayList();
		ArrayList al2 = new ArrayList();

		//old records
		ImportData id1 = new ImportData();
		id1.setField(0, oldSkdgrpName);
		id1.setField(1, "1");
		id1.setField(2, dateStr);
		id1.setField(3, "00:00:00");
		id1.setField(4, "1");
		id1.setField(5, "0");
		id1.setField(6, "");
		id1.setField(POSImportTransaction.VOLTYP_COL , UNIT_VOLTYPNAME);
		al1.add(id1);

		//historic update
		ImportData id3 = new ImportData();
		id3.setField(0, newSkdgrpName);
		id3.setField(1, "1");
		id3.setField(2, dateStr);
		id3.setField(3, "00:00:00");
		id3.setField(4, "1");
		id3.setField(5, "0");
		id3.setField(6, oldSkdgrpName);
		id3.setField(POSImportTransaction.VOLTYP_COL , UNIT_VOLTYPNAME);
		al2.add(id3);

		POSImportTransaction postran = new POSImportTransaction();
		DBConnection conn = getConnection();

		try
		{
		    postran.initializeTransaction(conn);
			postran.processNoAggregate(conn, al1);
			postran.processNoAggregate(conn, al2);

			ps = conn.prepareStatement("select RD.resdet_id, RD.resdet_volume from so_results_detail RD, so_schedule_group SG where RD.skdgrp_id=SG.skdgrp_id and SG.skdgrp_name=? and RD.resdet_date=? and RD.resdet_time=? and RD.voltyp_id=?");

			ps.setString(1, oldSkdgrpName);
			ps.setTimestamp(2, resdetDate);
			ps.setTimestamp(3, resdetTime);
			ps.setInt(4, UNIT_VOLTYPID);
			rs = ps.executeQuery();

			//old record should no longer exist
			if(!rs.next())
			{
				assertTrue(true);
			}
			else
			{
				assertTrue(false);
			}

			ps.setString(1, newSkdgrpName);
			ps.setTimestamp(2, resdetDate);
			ps.setTimestamp(3, resdetTime);
			rs = ps.executeQuery();

			//new record should exist
			int i=0;
			int resdetId = 0;
			while(rs.next())
			{
				i++;
				resdetId = rs.getInt(1);
				assertTrue(1 == rs.getInt(2));
			}

			if(i==1)
			{
				//clean up db
				ps = conn.prepareStatement("delete from so_results_detail where resdet_id=?");
				ps.setInt(1, resdetId);
				ps.execute();
				conn.commit();
			}
			else
			{
				assertTrue(false);
			}
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

	}

	public void testHistUpdateVoltyp() throws Exception
	{
		String oldSkdgrpName = skdgrpNameTest1;
		String newSkdgrpName = skdgrpNameTest2;
		String dateStr = "03/01/3000";
		String timeStr = dateStr+"00:00:00";
		Datetime resdetDate = DateHelper.parseDate(dateStr, "MM/dd/yyyy");
		Datetime resdetTime = DateHelper.parseDate(timeStr, "MM/dd/yyyyHH:mm:ss");
		final int UNIT_VOLTYPID = 1000; 
		final String UNIT_VOLTYPNAME = "UNIT";

		ResultSet rs = null;
		PreparedStatement ps = null;

		ArrayList al1 = new ArrayList();
		ArrayList al2 = new ArrayList();

		//old records
		ImportData id1 = new ImportData();
		id1.setField(0, oldSkdgrpName);
		id1.setField(1, "1");
		id1.setField(2, dateStr);
		id1.setField(3, "14:59:00");
		id1.setField(4, "1");
		id1.setField(5, "0");
		id1.setField(6, "");
		id1.setField(POSImportTransaction.VOLTYP_COL , UNIT_VOLTYPNAME);
		al1.add(id1);

		ImportData id2 = new ImportData();
		id2.setField(0, newSkdgrpName);
		id2.setField(1, "1");
		id2.setField(2, dateStr);
		id2.setField(3, "14:59:00");
		id2.setField(4, "5");
		id2.setField(5, "0");
		id2.setField(6, "");
		id2.setField(POSImportTransaction.VOLTYP_COL , UNIT_VOLTYPNAME);
		al1.add(id2);

		//historic update
		ImportData id3 = new ImportData();
		id3.setField(0, newSkdgrpName);
		id3.setField(1, "1");
		id3.setField(2, dateStr);
		id3.setField(3, "14:59:00");
		id3.setField(4, "1");
		id3.setField(5, "0");
		id3.setField(6, oldSkdgrpName);
		id3.setField(POSImportTransaction.VOLTYP_COL , UNIT_VOLTYPNAME);
		al2.add(id3);

		POSImportTransaction postran = new POSImportTransaction();
		DBConnection conn = getConnection();

		try
		{
		    postran.initializeTransaction(conn);
			postran.processAggregate(conn, al1);
			postran.processAggregate(conn, al2);

			ps = conn.prepareStatement("select RD.resdet_id, RD.resdet_volume from so_results_detail RD, so_schedule_group SG where RD.skdgrp_id=SG.skdgrp_id and SG.skdgrp_name=? and RD.resdet_date=? and RD.resdet_time=? and RD.voltyp_id=?");

			ps.setString(1, oldSkdgrpName);
			ps.setTimestamp(2, resdetDate);
			ps.setTimestamp(3, resdetTime);
			ps.setInt(4, UNIT_VOLTYPID);
			rs = ps.executeQuery();

			//old record should no longer exist
			if(!rs.next())
			{
				assertTrue(true);
			}
			else
			{
				assertTrue(false);
			}

			ps.setString(1, newSkdgrpName);
			ps.setTimestamp(2, resdetDate);
			ps.setTimestamp(3, resdetTime);
			ps.setInt(4, UNIT_VOLTYPID);
			rs = ps.executeQuery();

			//new record should exist
			int i=0;
			int resdetId = 0;
			while(rs.next())
			{
				i++;
				resdetId = rs.getInt(1);
				assertTrue(1 == rs.getInt(2));
			}

			if(i==1)
			{
				//clean up db
				ps = conn.prepareStatement("delete from so_results_detail where resdet_id=?");
				ps.setInt(1, resdetId);
				ps.execute();
				conn.commit();
			}
			else
			{
				assertTrue(false);
			}
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

	}
	
	public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
	}

}
