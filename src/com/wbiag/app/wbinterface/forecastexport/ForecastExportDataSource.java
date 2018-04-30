package com.wbiag.app.wbinterface.forecastexport;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.workbrain.app.wbinterface.db.ExportData;
import com.workbrain.server.data.AbstractRowCursor;
import com.workbrain.server.data.AbstractRowSource;
import com.workbrain.server.data.AccessException;
import com.workbrain.server.data.BasicRow;
import com.workbrain.server.data.Row;
import com.workbrain.server.data.RowCursor;
import com.workbrain.server.data.RowDefinition;
import com.workbrain.server.data.RowStructure;
import com.workbrain.server.data.type.StringType;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

/** 
 * Title:			Forecast Export Data Source
 * Description:		Row source for forecast export
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Mar 21, 2006
 * @author         	Kevin Tsoi
 */
public class ForecastExportDataSource extends AbstractRowSource
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ForecastExportDataSource.class);
    
    public final static String STORE_SKDGRP_NAME = "STORE_SKDGRP_NAME";
    public final static String DRIVER_SKDGRP_NAME = "DRIVER_SKDGRP_NAME";
    public final static String VOLTYP_NAME = "VOLTYP_NAME";
    public final static String FCAST_DATE = "FCAST_DATE";
    public final static String FCALLS_ADJVAL = "FCALLS_ADJVAL";
    public final static String DATE_FORMAT = "MM/dd/yyyy";
    public final static String AGG_LEVEL_DAY = "Day";
    public final static String AGG_LEVEL_WEEK = "Week";
    
    protected DBConnection conn = null;
    private RowDefinition rowDefinition = null;
    private java.util.List rows = new ArrayList();
    
    public final static String PARAM_VOLUME_TYPE = "volumeType";
    public final static String PARAM_WEEK_OFFSET = "weekOffset";
    public final static String PARAM_NUM_OF_WEEKS = "numOfWeeks";
    public final static String PARAM_START_OF_WEEK = "startOfWeek";
    public final static String PARAM_AGGREGATE_LEVEL = "aggregateLevel";
    
    public final static String REG_OMIT_TEAM_FIELD = "system/FORECAST_EXPORT/OMIT_TEAM_FIELD";
    
    private String startOfWeek = null;
    private String aggregateLevel = null;
    private int volTypId = 0;
    private int weekOffset = 0;
    private int numOfWeeks = 0;
    private String omitTeamField = null;
    
    public ForecastExportDataSource(DBConnection dbConn, HashMap exportParam) 
    	throws AccessException 
    {
        this.conn = dbConn;
        initSourceDefinition();
        getParameters(exportParam);
        
        try 
        {
            loadInputRecords();
        }
        catch (Exception e) 
        {
            throw new AccessException(e);
        }
    }
    
    public RowDefinition getRowDefinition() 
		throws AccessException 
	{
	    return rowDefinition;
	}

	public RowCursor query(String queryString) 
		throws AccessException 
	{
	    return queryAll();
	}
	
	public RowCursor query(String queryString, String orderByString) 
		throws AccessException 
	{
	    return queryAll();
	}
	
	public RowCursor query(List keys) 
		throws AccessException 
	{
	    return queryAll();
	}
	
	public RowCursor query(String[] fields, Object[] values) 
		throws AccessException 
	{
	    return queryAll();
	}
	
	public RowCursor queryAll() 
		throws AccessException 
	{
	    return
	        new AbstractRowCursor(getRowDefinition()) 
	        {
	            private int counter = -1;
	
	            public void close() 
	            {
	            }
	
	            protected Row getCurrentRowInternal() 
	            {
	                return counter >= 0 && counter < rows.size() ? (BasicRow) rows.get(counter) : null;
	            }
	
	            protected boolean fetchRowInternal() 
	            	throws AccessException 
	            {
	                return ++counter < rows.size();
	            }
	        };
	}
	
	private void initSourceDefinition() 
		throws AccessException 
	{        
	    //initialize
	    volTypId = -9998;
	    weekOffset = 0;
	    numOfWeeks = 1;
	    
	    //get omit team field
	    omitTeamField = getRegistryValue(REG_OMIT_TEAM_FIELD);
	    
        RowStructure rs = new RowStructure();
        
        // *** reserved system fields
        rs.add(ExportData.WBIEXP_STATUS, StringType.get());
        rs.add(ExportData.WBIEXP_MSG, StringType.get());
        
        // *** fields of the export
        rs.add(STORE_SKDGRP_NAME , StringType.get());        
        rs.add(VOLTYP_NAME , StringType.get());
        rs.add(FCAST_DATE, StringType.get());
        rs.add(FCALLS_ADJVAL , StringType.get());
        rs.add(DRIVER_SKDGRP_NAME , StringType.get());
        rowDefinition = new RowDefinition(-1,rs);	    	    
	}
	
	private void getParameters(HashMap exportParams)
	{
	    String volTypIdStr = null;
	    String weekOffsetStr = null;
	    String numOfWeeksStr = null;
	    
	    volTypIdStr = (String)exportParams.get(PARAM_VOLUME_TYPE);
	    weekOffsetStr = (String)exportParams.get(PARAM_WEEK_OFFSET);
	    numOfWeeksStr = (String)exportParams.get(PARAM_NUM_OF_WEEKS);
	    aggregateLevel = (String)exportParams.get(PARAM_AGGREGATE_LEVEL);
	    startOfWeek = (String)exportParams.get(PARAM_START_OF_WEEK);
	    	    
	    if(!StringHelper.isEmpty(volTypIdStr))
	    {
	        volTypId = Integer.parseInt(volTypIdStr);
	    }
	    
	    if(!StringHelper.isEmpty(weekOffsetStr))
	    {
	        weekOffset = Integer.parseInt(weekOffsetStr);
	    }
	    
	    if(!StringHelper.isEmpty(numOfWeeksStr))
	    {
	        numOfWeeks = Integer.parseInt(numOfWeeksStr);
	    }	    
	    
	    //default aggregateLevel to Day
	    if(StringHelper.isEmpty(aggregateLevel))
	    {
	        aggregateLevel = AGG_LEVEL_DAY;
	    }
	    
	    //default start of week to sunday
	    if(StringHelper.isEmpty(startOfWeek))
	    {
	        startOfWeek = "Sunday";
	    }	
	}
	
	public int count() 
	{
	    return rows.size();
	}
	
	public int count(String where) 
	{
	    return rows.size();
	}
	
	private void loadInputRecords() 
		throws Exception 
	{
	    PreparedStatement ps = null;
	    ResultSet rs = null;
	    String sql = null;
	    Row r = null;
	    Date fcastDate = null;
	    Date startDate = null;
	    Date endDate = null;
	    String driverSkdgrpName = null;
	    String storeSkdgrpName = null;
	    String volTypName = null;
	    int fcastCalls = 0;
	    int fcastAdjVal = 0;
	    
	    //determine start and end dates
	    startDate = DateHelper.getCurrentDate();
	    
	    //move start date to start of week
	    startDate = DateHelper.addDays(startDate, -7);
	    startDate = DateHelper.nextDay(startDate, startOfWeek);
	    
	    //move start date according to week offset
	    startDate = DateHelper.addDays(startDate, 7 * weekOffset);
	    
	    //set end date according to number of weeks and start date
	    endDate = DateHelper.addDays(startDate, 7 * numOfWeeks);
	    
	    if(AGG_LEVEL_WEEK.equals(aggregateLevel))
	    {
	        sql = getSQLWeek();
	    }
	    else
	    {
	        sql = getSQLDay();
	    }	    	    
	    
	    try
	    {
	        ps = conn.prepareStatement(sql);
	        ps.setInt(1, volTypId);
	        ps.setDate(2, new java.sql.Date(startDate.getTime()));
	        ps.setDate(3, new java.sql.Date(endDate.getTime()));
	        
	        rs = ps.executeQuery();
	        while(rs.next())
	        {
	            driverSkdgrpName = rs.getString("DRIVER_SKDGRP_NAME");
	            storeSkdgrpName = rs.getString("STORE_SKDGRP_NAME");
	            volTypName = rs.getString("VOLTYP_NAME");
	            fcastDate = rs.getDate("FCAST_DATE");
	            fcastCalls = rs.getInt(4);
	            fcastAdjVal = rs.getInt(5);
	            	            
	            r = new BasicRow(getRowDefinition());
            	try
            	{
            		r.setValue(STORE_SKDGRP_NAME, storeSkdgrpName);
            		r.setValue(VOLTYP_NAME, volTypName);
            		r.setValue(FCAST_DATE, DateHelper.convertDateString(fcastDate, DATE_FORMAT));
            		r.setValue(FCALLS_ADJVAL,String.valueOf(fcastCalls + fcastAdjVal));
            		r.setValue(DRIVER_SKDGRP_NAME, driverSkdgrpName);
            	} 
            	catch (Exception e)
            	{
            		r.setValue(ExportData.WBIEXP_STATUS, ExportData.STATUS_ERROR);
            		r.setValue(ExportData.WBIEXP_MSG, e.getMessage());
            	}
            	rows.add(r);
	        }
	    }
	    finally
	    {
	        SQLHelper.cleanUp(ps, rs);
	    }
	    
	}
	
	public static String getRegistryValue(String registryName)
    {
        String registyValue = "";
        try
        {
            registyValue = (String)Registry.getVar(registryName);
        }
        catch(Exception e)
        {
            logger.debug(e);
        }               
        return registyValue;
    } 
	
	public String getSQLDay()
	{
	    StringBuffer sql = null;
	    
	    sql = new StringBuffer();
	    
	    sql.append(" SELECT ");
	    sql.append(" FD_SKDGRP.SKDGRP_NAME DRIVER_SKDGRP_NAME, ");
	    sql.append(" ST_SKDGRP.SKDGRP_NAME STORE_SKDGRP_NAME, ");
	    sql.append(" FD.FCAST_DATE, ");
	    sql.append(" FD.FCAST_CALLS, ");
	    sql.append(" FD.FCAST_ADJVAL, ");
	    sql.append(" VT.VOLTYP_NAME ");

	    sql.append(" FROM ");
	    sql.append(" SO_SCHEDULE_GROUP FD_SKDGRP, ");
	    sql.append(" SO_SCHEDULE_GROUP ST_SKDGRP, ");
	    sql.append(" WORKBRAIN_TEAM FD_WBT, ");
	    sql.append(" WORKBRAIN_TEAM ST_WBT, ");
	    sql.append(" WORKBRAIN_TEAM_TYPE FD_WBT_TYPE, ");
	    sql.append(" WORKBRAIN_TEAM_TYPE ST_WBT_TYPE, ");
	    sql.append(" SO_VOLUME_TYPE VT, ");
	    sql.append(" SO_FCAST F, ");
	    sql.append(" SO_FCAST_DETAIL FD ");

	    sql.append(" WHERE ");
	    sql.append(" FD_SKDGRP.WBT_ID = FD_WBT.WBT_ID ");
	    sql.append(" AND ");
	    sql.append(" ST_SKDGRP.WBT_ID = ST_WBT.WBT_ID ");
	    sql.append(" AND ");
	    sql.append(" FD_WBT.WBT_LFT BETWEEN ST_WBT.WBT_LFT AND ST_WBT.WBT_RGT ");
	    sql.append(" AND ");
	    sql.append(" FD_WBT.WBTT_ID = FD_WBT_TYPE.WBTT_ID ");
	    sql.append(" AND ");
	    sql.append(" ST_WBT.WBTT_ID = ST_WBT_TYPE.WBTT_ID ");
	    sql.append(" AND ");
	    sql.append(" ST_WBT_TYPE.WBTT_NAME = 'STORE' ");
	    sql.append(" AND ");
	    sql.append(" FD_WBT_TYPE.WBTT_NAME = 'DRIVER' ");
	    sql.append(" AND ");
	    sql.append(" FD_SKDGRP.VOLTYP_ID = VT.VOLTYP_ID ");
	    sql.append(" AND ");
	    sql.append(" F.FCAST_ID = FD.FCAST_ID ");
	    sql.append(" AND ");
	    sql.append(" FD_SKDGRP.SKDGRP_ID = F.SKDGRP_ID ");
	    sql.append(" AND ");
	    sql.append(" FD_SKDGRP.VOLTYP_ID = ? ");
	    sql.append(" AND ");
	    sql.append(" FD.FCAST_DATE BETWEEN ? AND ? ");	    	   
	    sql.append(" AND ");
	    
	    //add omit team condition if field is set in registry
	    if(!StringHelper.isEmpty(omitTeamField))
	    {
	        sql.append(" FD_WBT.");
	        sql.append(omitTeamField);
	        sql.append(" = 'Y' ");
	        sql.append(" AND ");
	    }
	    
	    sql.append(" NOT EXISTS( ");
	    sql.append(" SELECT 0 ");
	    sql.append(" FROM ");
	    sql.append(" WORKBRAIN_TEAM I_WBT ");
	    sql.append(" WHERE ");
	    sql.append(" I_WBT.WBT_LFT ");
	    sql.append(" BETWEEN ST_WBT.WBT_LFT AND ST_WBT.WBT_RGT ");
	    sql.append(" AND ");
	    sql.append(" I_WBT.WBTT_ID = 10001 ");
	    sql.append(" AND ");
	    sql.append(" I_WBT.WBT_ID != ST_WBT.WBT_ID) ");
	    
	    return sql.toString();
	}
	 
	public String getSQLWeek()
	{
	    StringBuffer sql = null;
	    
	    sql = new StringBuffer();
	    
	    sql.append(" SELECT ");
	    sql.append(" FD_SKDGRP.SKDGRP_NAME DRIVER_SKDGRP_NAME, ");
	    sql.append(" ST_SKDGRP.SKDGRP_NAME STORE_SKDGRP_NAME, ");
	    sql.append(" F.FCAST_TO_DATE FCAST_DATE, ");
	    sql.append(" sum(FD.FCAST_CALLS), ");
	    sql.append(" sum(FD.FCAST_ADJVAL), ");
	    sql.append(" VT.VOLTYP_NAME ");

	    sql.append(" FROM ");
	    sql.append(" SO_SCHEDULE_GROUP FD_SKDGRP, ");
	    sql.append(" SO_SCHEDULE_GROUP ST_SKDGRP, ");
	    sql.append(" WORKBRAIN_TEAM FD_WBT, ");
	    sql.append(" WORKBRAIN_TEAM ST_WBT, ");
	    sql.append(" WORKBRAIN_TEAM_TYPE FD_WBT_TYPE, ");
	    sql.append(" WORKBRAIN_TEAM_TYPE ST_WBT_TYPE, ");
	    sql.append(" SO_VOLUME_TYPE VT, ");
	    sql.append(" SO_FCAST F, ");
	    sql.append(" SO_FCAST_DETAIL FD ");

	    sql.append(" WHERE ");
	    sql.append(" FD_SKDGRP.WBT_ID = FD_WBT.WBT_ID ");
	    sql.append(" AND ");
	    sql.append(" ST_SKDGRP.WBT_ID = ST_WBT.WBT_ID ");
	    sql.append(" AND ");
	    sql.append(" FD_WBT.WBT_LFT BETWEEN ST_WBT.WBT_LFT AND ST_WBT.WBT_RGT ");
	    sql.append(" AND ");
	    sql.append(" FD_WBT.WBTT_ID = FD_WBT_TYPE.WBTT_ID ");
	    sql.append(" AND ");
	    sql.append(" ST_WBT.WBTT_ID = ST_WBT_TYPE.WBTT_ID ");
	    sql.append(" AND ");
	    sql.append(" ST_WBT_TYPE.WBTT_NAME = 'STORE' ");
	    sql.append(" AND ");
	    sql.append(" FD_WBT_TYPE.WBTT_NAME = 'DRIVER' ");
	    sql.append(" AND ");
	    sql.append(" FD_SKDGRP.VOLTYP_ID = VT.VOLTYP_ID ");
	    sql.append(" AND ");
	    sql.append(" F.FCAST_ID = FD.FCAST_ID ");
	    sql.append(" AND ");
	    sql.append(" FD_SKDGRP.SKDGRP_ID = F.SKDGRP_ID ");
	    sql.append(" AND ");
	    sql.append(" FD_SKDGRP.VOLTYP_ID = ? ");
	    sql.append(" AND ");
	    sql.append(" FD.FCAST_DATE BETWEEN ? AND ? ");	    	   
	    sql.append(" AND ");
	    
	    //add omit team condition if field is set in registry
	    if(!StringHelper.isEmpty(omitTeamField))
	    {
	        sql.append(" FD_WBT.");
	        sql.append(omitTeamField);
	        sql.append(" = 'Y' ");
	        sql.append(" AND ");
	    }
	    
	    sql.append(" NOT EXISTS( ");
	    sql.append(" SELECT 0 ");
	    sql.append(" FROM ");
	    sql.append(" WORKBRAIN_TEAM I_WBT ");
	    sql.append(" WHERE ");
	    sql.append(" I_WBT.WBT_LFT ");
	    sql.append(" BETWEEN ST_WBT.WBT_LFT AND ST_WBT.WBT_RGT ");
	    sql.append(" AND ");
	    sql.append(" I_WBT.WBTT_ID = 10001 ");
	    sql.append(" AND ");
	    sql.append(" I_WBT.WBT_ID != ST_WBT.WBT_ID) ");
	    
	    sql.append(" GROUP BY ");	    	    
	    sql.append(" FD_SKDGRP.SKDGRP_NAME, ");
	    sql.append(" ST_SKDGRP.SKDGRP_NAME, ");
	    sql.append(" F.FCAST_TO_DATE , ");	    
	    sql.append(" VT.VOLTYP_NAME ");
	    	    
	    return sql.toString();
	}
}
