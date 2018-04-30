package com.wbiag.app.clockInterface.processing;

import java.util.*;

import com.wbiag.app.ta.db.*;
import com.wbiag.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.modules.entitlements.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import com.workbrain.test.*;
import junit.framework.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.math.BigDecimal;
/**
 * Test for WBClockProcessTaskExtTest.
 */
public class WBClockProcessTaskExtTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WBClockProcessTaskExtTest.class);

    public WBClockProcessTaskExtTest(String testName) throws Exception {
        super(testName);
    }
    
    final String rdrgrpName = "VIRTUAL READER GROUP";
    final String rdrName = "VIRTUAL READER";

    final String tmpRdrGrpName = "DUMMY GROUP";
    final String tmpRdrName = "DUMMY READER";
    
    final int numDays = 10;
    final int numEmployees = 50;
    final int numReaders = 10;

    private String[] arrTempRdrGrpName = {"DUMMY GROUP 1", "DUMMY GROUP 2", "DUMMY GROUP 3"};
    private String[] arrTempRdrName = {"DUMMY READER 1", "DUMMY READER 2", "DUMMY READER 3"};
    private int[] arrEmpId = null;
    
    final Date startDate = DateHelper.nextDay(DateHelper.getCurrentDate(), "Mon");
    final Date endDate = DateHelper.addDays(startDate, numDays - 1);

    private Set rdrGrpNameSet = null;
    private Set rdrNameSet = null;
    
    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WBClockProcessTaskExtTest.class);
        return result;
    }

    protected void setUp() throws Exception {
        super.setUp();
        
        // initialize reader groups, readers, employees
        arrEmpId = new int[numEmployees]; 
        for (int i = 0; i < numEmployees; i ++) {
        	arrEmpId[i] = i + 10;
        }        
        arrTempRdrGrpName = new String[numReaders]; 
        arrTempRdrName = new String[numReaders]; 
        for (int i = 0; i < numReaders; i ++) {
        	arrTempRdrGrpName[i] = tmpRdrGrpName + " " + (i + 1);
        	arrTempRdrName[i] = tmpRdrName + " " + (i + 1);
        }
        
        PreparedStatement ps = null;
        int rdrGrpId = 0;

        for (int i = 0; i < arrTempRdrGrpName.length; i ++) {
            try {
            	
                StringBuffer sb = new StringBuffer(200);
                sb.append("INSERT INTO reader_group(rdrgrp_id, rdrgrp_name, rdrsvr_id) VALUES (?,?,?)");
                ps = getConnection().prepareStatement(sb.toString());

                rdrGrpId = getConnection().getDBSequence("seq_rdrgrp_id").getNextValue();
                ps.clearParameters();
                ps.setInt(1  ,  rdrGrpId );
                ps.setString(2, arrTempRdrGrpName[i]);
                ps.setInt(3, 55);
                ps.execute();
            	
            }
            catch(Exception e) {
                logger.error(e);
            }
            finally {
                if (ps != null) ps.close();
            }
            ps = null;
            
            try {
                StringBuffer sb = new StringBuffer(200);
                sb.append("INSERT INTO reader (rdr_id, rdr_name, rdr_desc, rdrgrp_id, rdr_ip_address, rdrstat_id, rdr_zone, tz_id)");
                sb.append("VALUES (?,?,?,?,?,?, ?, ?)");
                ps = getConnection().prepareStatement(sb.toString());
                
            	int rdrId = getConnection().getDBSequence("seq_rdr_id").getNextValue();
            	
                ps.clearParameters();
                ps.setInt(1, rdrId);
                ps.setString(2, arrTempRdrName[i]);
                ps.setString(3, arrTempRdrName[i]);
                ps.setInt(4, rdrGrpId) ;
                ps.setString(5, String.valueOf(rdrId)) ;
                ps.setInt(6, 1) ;
                ps.setInt(7, 0) ;
                ps.setInt(8, 0) ;
                int upd = ps.executeUpdate();
            }
            finally {
                if (ps != null) ps.close();
            }
            ps = null;
            
        }

        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM clock_tran_pend_j");
            ps = getConnection().prepareStatement(sb.toString());
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        getConnection().commit();
    }


    protected void tearDown() throws Exception {
        super.tearDown();
        
        PreparedStatement ps = null;

        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM CLOCK_TRAN_PROCESSED WHERE RDR_ID IN (SELECT RDR_ID FROM READER WHERE RDR_NAME LIKE '" + tmpRdrName + "%')");
            ps = getConnection().prepareStatement(sb.toString());
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM reader WHERE rdr_name IN (?)");
            ps = getConnection().prepareStatement(sb.toString());

            for (int i = 0; i < arrTempRdrName.length; i ++) {
                ps.clearParameters();
                ps.setString(1  , arrTempRdrName[i]);
                int upd = ps.executeUpdate();
            }

        } finally {
            if (ps != null) ps.close();
        }

        ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM reader_group WHERE rdrgrp_name IN (?)");
            ps = getConnection().prepareStatement(sb.toString());

            for (int i = 0; i < arrTempRdrGrpName.length; i ++) {
                ps.clearParameters();
                ps.setString(1  , arrTempRdrGrpName[i]);
                int upd = ps.executeUpdate();
            }

        }
        finally {
            if (ps != null) ps.close();
        }

        ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM clock_tran_pend_j");
            ps = getConnection().prepareStatement(sb.toString());
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("UPDATE work_summary SET wrks_orig_clocks = null WHERE emp_id = ? AND wrks_work_date between ? and ?");
            ps = getConnection().prepareStatement(sb.toString());
            
            for (int i = 0; i < arrEmpId.length; i ++) {
            	ps.clearParameters();
                ps.setInt(1 , arrEmpId[i]);
                ps.setTimestamp(2 , new java.sql.Timestamp(startDate.getTime()));
                ps.setTimestamp(3 , new java.sql.Timestamp(endDate.getTime()));
                int upd = ps.executeUpdate();
            }
            
        }
        finally {
            if (ps != null) ps.close();
        }

        ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM clock_tran_processed WHERE emp_id = ?");
            ps = getConnection().prepareStatement(sb.toString());

            for (int i = 0; i < arrEmpId.length; i ++) {
            	ps.clearParameters();
                ps.setInt(1 , arrEmpId[i]);
                int upd = ps.executeUpdate();
            }
        }
        finally {
            if (ps != null) ps.close();
        }

        getConnection().commit();
    }


    public void testClks() throws Exception {

    	rdrGrpNameSet = new HashSet();
    	rdrGrpNameSet.add(rdrgrpName);
    	rdrGrpNameSet.add(arrTempRdrGrpName[3]);
    	rdrGrpNameSet.add(arrTempRdrGrpName[6]);
    	rdrGrpNameSet.add(arrTempRdrGrpName[7]);

    	rdrNameSet = new HashSet();
    	rdrNameSet.add(rdrName);
    	rdrNameSet.add(arrTempRdrName[3]);
    	rdrNameSet.add(arrTempRdrName[6]);
    	rdrNameSet.add(arrTempRdrName[7]);

        ClockTranAccess access = new ClockTranAccess(getConnection());

        for (int i = 0; i < arrEmpId.length; i ++) {
        	
        	Date date = startDate;
        	while (date.getTime() <= endDate.getTime()) {
            	Date clkOn = DateHelper.addMinutes(date, 550);
                Date clkOff = DateHelper.addMinutes(date, 1020);
                
                ClockTranPendJData data1 = new ClockTranPendJData();

                data1.setCtpjIdentifier(Integer.toString(arrEmpId[i]));
                data1.setCtpjIdentType("I");
                data1.setCtpjTime(DateHelper.convertDateString(clkOn, Clock.CLOCKDATEFORMAT_STRING));
                data1.setCtpjRdrName(rdrName);
                data1.setCtpjType(Integer.toString(Clock.TYPE_ON));
                access.insert(data1);

                ClockTranPendJData data2 = new ClockTranPendJData();

                data2.setCtpjIdentifier(Integer.toString(arrEmpId[i]));
                data2.setCtpjIdentType("I");
                data2.setCtpjTime(DateHelper.convertDateString(clkOff, Clock.CLOCKDATEFORMAT_STRING));
                data2.setCtpjRdrName(arrTempRdrName[i % arrTempRdrName.length]);
                data2.setCtpjType(Integer.toString(Clock.TYPE_ON));
                access.insert(data2);
        		
        		date = DateHelper.addDays(date, 1);
        	}
        	
        }

        getConnection().commit();

        WBClockProcessTaskExt ctask = new WBClockProcessTaskExt();
        ctask.setShouldCommit(false);
        ctask.setCheckForInterrupt(false);
        Map params = new HashMap();
        
        StringBuffer rdrGrpNames = new StringBuffer();
        StringBuffer rdrGrpNamesQuotes = new StringBuffer();
        Iterator iter = rdrGrpNameSet.iterator();
        while (iter.hasNext()) {
        	String cur = (String)iter.next();
        	rdrGrpNames.append((rdrGrpNames.length() == 0 ? "" : ",") + cur);
        	rdrGrpNamesQuotes.append((rdrGrpNamesQuotes.length() == 0 ? "'" : ", '") + cur + "'");
        }
        
        params.put(WBClockProcessTaskExt.PARAM_RDRGRP_NAMES, rdrGrpNames.toString());
        params.put(WBClockProcessTaskExt.PARAM_RDRGRP_NAMES_INCL, "Y");
        ctask.run(-1, params);
    	
        List listAfter2 = access.loadRecordData(new ClockTranPendJData(), "CLOCK_TRAN_PEND_J", "1=1") ;

        //assertTrue(listAfter2.size() == arrEmpId.length * numDays);
        String sql = "SELECT COUNT(*) FROM CLOCK_TRAN_PEND_J A, READER B, READER_GROUP C"
        	+ " WHERE A.CTPJ_RDR_NAME = B.RDR_NAME"
        	+ " AND B.RDRGRP_ID = C.RDRGRP_ID"
        	+ " AND C.RDRGRP_NAME IN (" + rdrGrpNamesQuotes + ")";
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
        	assertEquals(0, rs.getInt(1));
        }
        
        for (int i = 0; i < listAfter2.size(); i ++) {
            ClockTranPendJData dataAfter = (ClockTranPendJData)listAfter2.get(i);
            assertTrue(!rdrNameSet.contains(dataAfter.getCtpjRdrName()));
        }
        
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
