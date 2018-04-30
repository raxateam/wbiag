package com.wbiag.app.modules.pts;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

import junit.framework.TestSuite;

import com.wbiag.app.ta.model.PTSData;
import com.workbrain.app.modules.retailSchedule.SOTestCase;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.db.ScheduleGroupAccess;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.model.ScheduleProfile;
import com.workbrain.app.ta.ruleengine.RuleEngine;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.test.TestUtil;
import com.workbrain.util.DateHelper;
import com.workbrain.util.Datetime;
import com.workbrain.util.TimeZoneUtil;

/** 
 * Title:			PTS Processor Test
 * Description:		JUnit test for PTS Processor
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Apr 25, 2005
 * @author         	Kevin Tsoi
 */
public class PTSProcessorTest extends SOTestCase
{
	public PTSProcessorTest(String arg0) 
	{
		super(arg0);
	}
	
	public static TestSuite suite() 
	{
		TestSuite result = new TestSuite();
		result.addTestSuite(PTSProcessorTest.class);
		return result;
	}	
	
/*	public void testGetScheduleGroupDataList()
		throws Exception
	{
	    List scheduleGroupDataList = null;
	    PTSProcessor ptsProcessor = null;
	    ScheduleGroupData scheduleGroupData = null;
	    DBConnection conn = null;
	    Iterator it = null;
	    int skdGrpId = 0;
	    int skdGrpIntrnlType = 0;
	    
	    conn = getConnection();
	    skdGrpId = 10013;
	    
	    ptsProcessor = new PTSProcessor(conn);
	    scheduleGroupDataList = ptsProcessor.getScheduleGroupDataList(skdGrpId);
	    
	    it = scheduleGroupDataList.iterator();
	    while(it.hasNext())
	    {
	        scheduleGroupData = ((ScheduleGroupData)it.next());
	        skdGrpId = scheduleGroupData.getSkdgrpId().intValue();
	        skdGrpIntrnlType = scheduleGroupData.getSkdgrpIntrnlType();
	        System.out.println("skdGrpId: "+skdGrpId);
	        System.out.println("skdGrpIntrnlType: "+skdGrpIntrnlType);
	    }	    
	}	*/
		
	public void testCalculateCost()
		throws Exception
	{
	    List ptsDataList = null;	    
	    PTSProcessor ptsProcessor = null;
	    DBConnection conn = null;
	    Date startDate = null;
	    Date endDate = null;	    
	    String timeCodes = null;
	    String hourTypes = null;
	    String deptSkdGrpName = null;
	    String storeSkdGrpName = null;
	    boolean timeCodesInclusive = false;
	    boolean hourTypesInclusive = false;
	    int deptSkdGrpId = 0;	    
	    double cost = 0;	
	    
	    deptSkdGrpName = "PARENT";
	    deptSkdGrpId = 1;
	    storeSkdGrpName = "PARENT";
	    
	    timeCodes = "WRK;UAT";
	    hourTypes = "REG;UNPAID";
	    timeCodesInclusive = true;
	    hourTypesInclusive = true;
	    
	    startDate = DateHelper.convertStringToDate("05/04/2005", "MM/dd/yyyy");
	    endDate = DateHelper.addDays(startDate, 3);
	    
	    conn = getConnection();
	    ptsProcessor = new PTSProcessor(conn);	
	    
	    ptsDataList = ptsProcessor.calculateCostWeek(new HashMap(), deptSkdGrpId, deptSkdGrpName, storeSkdGrpName, timeCodes, timeCodesInclusive, hourTypes, hourTypesInclusive, startDate, endDate, false);
	 //   ptsDataList = ptsProcessor.calculateCostDay(deptSkdGrpId, deptSkdGrpName, storeSkdGrpName, timeCodes, timeCodesInclusive, hourTypes, hourTypesInclusive, startDate, false);
	    if(ptsDataList.size() > 0)
	    {
	        cost = ((PTSData)ptsDataList.get(0)).getPtsValue();
	    }
	    
	    assertTrue(cost >= 0);
	}	
		
	public void testLoadActualEarnings()
		throws Exception
	{
	    List actualEarnings = null;
	    PTSProcessor ptsProcessor = null;	    
	    DBConnection conn = null;
	    PreparedStatement ps = null;	    	   
	    String storeSkdGrpName = null;	    	    
	    Datetime resDate = null;
	    Datetime resTime = null;	    
	    int skdGrpId = 0;
	    int parentSkdGrpId = 0;
	    int volume = 0;
	    double resultVolume = 0;
	    
	    conn = getConnection();	    
	    PTSHelper.setRegistryValue(PTSHelper.REG_PTS_VOLUME_TYPE, "REVENUE");	    
	    
	    storeSkdGrpName = "PARENT";
	    volume = 0;
	    skdGrpId = 1;
	    parentSkdGrpId = 1;
	    resDate = DateHelper.toDatetime(DateHelper.convertStringToDate("06/05/2004", "MM/dd/yyyy"));
	    resTime = (Datetime)resDate.clone();	        
	    
	    //insert test data
	    ps = conn.prepareStatement("insert into so_results_detail(resdet_id, skdgrp_id, resdet_date, resdet_time, resdet_volume, invtyp_id) values(?, ?, ?, ?, ?, ?)");
	    ps.setInt(1, conn.getDBSequence("SEQ_RESDET_ID").getNextValue());
	    ps.setInt(2, skdGrpId);
	    ps.setTimestamp(3, resDate);
	    ps.setTimestamp(4, resTime);
	    ps.setInt(5, volume);
	    ps.setInt(6, 1);	    
	    ps.execute();	    	    	    	    	    
	    
	    ptsProcessor = new PTSProcessor(conn);	   
	    ptsProcessor.getVolumeType();
	    actualEarnings = ptsProcessor.loadActualEarnings(1, storeSkdGrpName, resDate, resDate);
	    if(actualEarnings.size() > 0)
	    {
	        resultVolume = ((PTSData)actualEarnings.get(0)).getPtsValue();
	    }	    
	    assertTrue(resultVolume>=volume);	        
	}
	
/*	public void testLoadForecastEarnings()
		throws Exception
	{
	    List forecastEarnings = null;
	    PTSProcessor ptsProcessor = null;
	    DBConnection conn = null;
	    Date startDate = null;
	    Date endDate = null;	    
	    int deptSkdGrpId = 0;
	    String storeSkdGrpName = null;
	    
	    deptSkdGrpId = 10001;
	    storeSkdGrpName = "bla";
	    
	    conn = getConnection();
	    ptsProcessor = new PTSProcessor(conn);
	    
	    startDate = DateHelper.convertStringToDate("05/20/2005", "MM/dd/yyyy");
	    //endDate = DateHelper.addDays(startDate, 1);
	    endDate = startDate;
	    
	    forecastEarnings = ptsProcessor.loadForecastEarnings(deptSkdGrpId, storeSkdGrpName, startDate, endDate);
	    
	    assertTrue(true);
	}*/
	
	public void testGetForecastStartDate()
		throws Exception
	{
	    ScheduleGroupAccess scheduleGroupAccess = null;
	    ScheduleGroupData scheduleGroupData = null;
	    PTSProcessor ptsProcessor = null;
	    DBConnection conn = null;
	    Date startDate = null;
	    Date inputDate = null;
	    int skdGrpId = 0;
	    
	    conn = getConnection();
	    skdGrpId = 1;
	    inputDate = DateHelper.convertStringToDate("05/20/2005", "MM/dd/yyyy");
	    
	    scheduleGroupAccess = new ScheduleGroupAccess(conn);
	    scheduleGroupData = (ScheduleGroupData)scheduleGroupAccess.loadFromIds(new ScheduleGroupData(), new int[]{skdGrpId}).get(0);
	    	   	    
	    startDate = PTSHelper.getForecastStartDate(scheduleGroupData, inputDate, 0);	    
	    
	    assertTrue(1 == DateHelper.dayOfWeek(startDate));
	}
	
/*	public void testPopulate()
		throws Exception
	{	    	    
	    List PTSDataList = null;
	    PTSProcessor ptsProcessor = null;
	    PTSData ptsData = null;
	    DBConnection conn = null;
	    
	    int ptsId = 0;
	    int deptSkdGrpId = 0;
	    int storeSkdGrpId = 0;
	    Date testDate = null;
	    String type = null;	    
	    String category = null;
	    double value = 0;
	    
	    conn = getConnection();
	    
	    ptsProcessor = new PTSProcessor(conn);
	    ptsProcessor.cachePTS(testDate, testDate);
	    
	    List ptsDataList = new ArrayList();
	    
	    ptsId = 1;
	    deptSkdGrpId = 10002;
	    storeSkdGrpId = 10005;
	    testDate = DateHelper.truncateToDays(new Date());
	    type = PTSHelper.COST;
	    category = PTSHelper.BUDGET;	    	    	    
	    value = 100;	    
	    
	    ptsProcessor.cachePTS(testDate, testDate);
	    
	    ptsData = new PTSData();	   	   	    
	    ptsData.setPtsId(ptsId);
	    ptsData.setDeptSkdgrpId(deptSkdGrpId);
	    ptsData.setStoreSkdgrpId(storeSkdGrpId);
	    ptsData.setWorkdate(testDate);	    
	    ptsData.setType(type);
	    ptsData.setCategory(category);
	    ptsData.setValue(value);
	    	    
	    ptsDataList.add(ptsData);
	    
	    ptsProcessor.populate(category, type, ptsDataList);
	    
	    ptsDataList.clear();
	    
	    category = PTSHelper.ACTUAL;
	    value = 500;
	    ptsId = 2;
	    
	    ptsData = new PTSData();	   	    
		ptsData.setPtsId(ptsId);
		ptsData.setDeptSkdgrpId(deptSkdGrpId);
		ptsData.setStoreSkdgrpId(storeSkdGrpId);
		ptsData.setWorkdate(testDate);	    
		ptsData.setType(type);
		ptsData.setCategory(category);
		ptsData.setValue(value);
	    
		ptsDataList.add(ptsData);
		
		ptsProcessor.populate(category, type, ptsDataList);
		
		conn.commit();
		
		assertTrue(true);	   
	}*/	
	
/*	public void testUpdatePTS()
		throws Exception
	{
	    PTSProcessor ptsProcessor = null;
	    DBConnection conn = null;	    
	    
	    int scheduleGroupId = 10001; 
	    int offset = 6;
	    int dailyOffset = 0;
        boolean dayOrWeek = false;
        boolean actualCost = false; 
        boolean budgetCost = false;
        boolean actualEarnings = false;
        boolean budgetEarnings = false;
        String timeCodes = "E-WRK";
        boolean timeCodesInclusive = false;
        String hourTypes = "REG";
        boolean hourTypesInclusive = true;
	    	    
        StringTokenizer test = new StringTokenizer("", ";");
        test.hasMoreTokens();
        
	    conn = getConnection();
	    ptsProcessor = new PTSProcessor(conn);
	    ptsProcessor.updatePTS(scheduleGroupId, offset, dailyOffset, dayOrWeek, actualCost,
	            actualEarnings, budgetCost, budgetEarnings, timeCodes, timeCodesInclusive, 
	            hourTypes, hourTypesInclusive);
	    
	 //   conn.commit();
	    assertTrue(true);	    
	}	*/	
		
	public static void main(String[] args) 
		throws Exception 
	{
		junit.textui.TestRunner.run(suite());
	}    
}
