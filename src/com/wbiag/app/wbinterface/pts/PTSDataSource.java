package com.wbiag.app.wbinterface.pts;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.wbiag.app.modules.pts.PTSHelper;
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
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

/** 
 * Title:			PTSDataSource
 * Description:		Data source for PTS export
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Jun 27, 2005
 * @author         	Kevin Tsoi
 */
public class PTSDataSource extends AbstractRowSource
{    
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PTSDataSource.class);
    public static final String COL_DEPT_SKDGRP_NAME = "SKDGRP_NAME";
    public static final String COL_STORE_SKDGRP_NAME = "PTS_STORE_SKDGRP_NAME";
    public static final String COL_PTS_WORKDATE = "PTS_WORKDATE";
    public static final String COL_PTS_TYPE = "PTS_TYPE";
    public static final String COL_PTS_VALUE = "PTS_VALUE";
    public static final String COL_PTS_COST = "PTS_COST";
    public static final String COL_PTS_SALES = "PTS_SALES";
    public static final int scale = 2;    
        
    protected DBConnection conn = null;
    private RowDefinition rowDefinition = null;
    private java.util.List rows = new ArrayList();
    private int offset = 0;
    private int numberOfDays = 0;
    
    public PTSDataSource(DBConnection conn, HashMap exportParam)
    	throws AccessException
    {
        this.conn = conn;
        
        //get paramters
        String offsetStr = (String)exportParam.get(PTSExportProcessor.PARAM_OFFSET);
        String numberOfDaysStr = (String)exportParam.get(PTSExportProcessor.PARAM_NUM_OF_DAYS);
        if(!StringHelper.isEmpty(offsetStr))
        {
            this.offset = Integer.parseInt(offsetStr);        
        }        
        if(!StringHelper.isEmpty(numberOfDaysStr))
        {    
            this.numberOfDays = Integer.parseInt(numberOfDaysStr);
        }
        
        //creates row structure and definition
        initSourceDefinition();
        try 
        {
            //loads records
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
        RowStructure rs = new RowStructure();
        
        //reserved system fields
        rs.add(ExportData.WBIEXP_STATUS, StringType.get());
        rs.add(ExportData.WBIEXP_MSG, StringType.get());
        
        //fields of the export
        rs.add(COL_DEPT_SKDGRP_NAME , StringType.get());
        rs.add(COL_STORE_SKDGRP_NAME , StringType.get());
        rs.add(COL_PTS_WORKDATE , StringType.get());
        rs.add(COL_PTS_COST , StringType.get());
        rs.add(COL_PTS_SALES , StringType.get());
        rowDefinition = new RowDefinition(-1,rs);
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
    	throws SQLException 
    {
        Row r = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        Date startDate = null;
        String storeName = null;
        String deptName = null;
        String ptsDate = null;
        String ptsType = null;
        String ptsValue = null;
        String ptsCost = null;
        String ptsSales = null;
        String oldStoreName = null;
        String oldDeptName = null;
        String oldPtsDate = null;  
        BigDecimal ptsValueBD = null;        
        
        //sets start and end date to export
        startDate = DateHelper.getCurrentDate();
        startDate = DateHelper.addDays(startDate, offset);        
        Date endDate = DateHelper.addDays(startDate, numberOfDays);
        
        ptsCost = "0";
        ptsSales = "0";
        
        try 
        {            
            //retrieves pts record between specified dates for type ACTUAL
            StringBuffer sql = new StringBuffer();
            sql.append(" SELECT ");
            sql.append(" PTS.PTS_STORE_SKDGRP_NAME, ");
            sql.append(" SG.SKDGRP_NAME, ");
            sql.append(" PTS.PTS_WORKDATE, ");
            sql.append(" PTS.PTS_TYPE, ");
            sql.append(" PTS.PTS_VALUE ");
            sql.append(" FROM ");
            sql.append(" PAYROLL_TO_SALES PTS, ");
            sql.append(" SO_SCHEDULE_GROUP SG ");
            sql.append(" WHERE ");
            sql.append(" PTS.SKDGRP_ID = SG.SKDGRP_ID ");
            sql.append(" AND ");
            sql.append(" PTS.PTS_WORKDATE BETWEEN ? AND ? ");
            sql.append(" AND ");
            sql.append(" PTS.PTS_CATEGORY = ? ");
            sql.append(" ORDER BY ");
            sql.append(" PTS_STORE_SKDGRP_NAME, ");
            sql.append(" SKDGRP_NAME, ");
            sql.append(" PTS_WORKDATE, ");
            sql.append(" PTS_TYPE, ");
            sql.append(" PTS_VALUE ");
            
            ps = conn.prepareStatement(sql.toString());
            ps.setDate(1, new java.sql.Date(startDate.getTime()));
            ps.setDate(2, new java.sql.Date(endDate.getTime()));
            ps.setString(3, PTSHelper.ACTUAL);
            rs = ps.executeQuery();
            while (rs.next()) 
            {
                try 
                {
                    //get values for record
	                storeName = rs.getString(COL_STORE_SKDGRP_NAME);
	                deptName = rs.getString(COL_DEPT_SKDGRP_NAME);
	                ptsDate = DateHelper.convertDateString(rs.getDate(COL_PTS_WORKDATE), PTSHelper.DATE_FORMAT);
	                ptsType = rs.getString(COL_PTS_TYPE);
	                ptsValue = rs.getString(COL_PTS_VALUE);	                
	                ptsValueBD = new BigDecimal(ptsValue);
	                ptsValueBD = ptsValueBD.setScale(scale, BigDecimal.ROUND_HALF_EVEN);
	                ptsValue = ptsValueBD.toString();
	                
	                //create single row to export for cost and sales per department + store + date
	                if(!deptName.equalsIgnoreCase(oldDeptName) ||
	                        !ptsDate.equalsIgnoreCase(oldPtsDate) ||
	                        !storeName.equalsIgnoreCase(oldStoreName))
	                {
	                    if(r != null)
	                    {
	                        rows.add(r);
	                        ptsCost = "0.00";
		                    ptsSales = "0.00";
	                    }	                    
	                    r = new BasicRow(getRowDefinition());	                                      
	                }                	                

	                //sets cost
	                if(PTSHelper.COST.equalsIgnoreCase(ptsType))
	                {
	                    ptsCost = ptsValue;
	                }
	                //sets sales
	                else if(PTSHelper.EARNED.equalsIgnoreCase(ptsType))
	                {
	                    ptsSales = ptsValue;
	                }	                
	                
	                //sets value for row to export
                    r.setValue(COL_STORE_SKDGRP_NAME, storeName);
                    r.setValue(COL_DEPT_SKDGRP_NAME, deptName);
                    r.setValue(COL_PTS_WORKDATE, ptsDate);
                    r.setValue(COL_PTS_COST, ptsCost);
                    r.setValue(COL_PTS_SALES, ptsSales);
                    
                    //keep track of previous record
                    oldStoreName = storeName;
                    oldDeptName = deptName;
                    oldPtsDate = ptsDate;                    
                } 
                catch (Exception e) 
                {
                    r.setValue(ExportData.WBIEXP_STATUS, ExportData.STATUS_ERROR);
                    r.setValue(ExportData.WBIEXP_MSG, e.getMessage());
                    rows.add(r);
                }                               
            }
            //add last row
            if(r != null)
            {
                rows.add(r);
            }
        }
        catch (Exception e) 
        {
            throw new SQLException(e.getMessage());
        }
        finally 
        {
            SQLHelper.cleanUp(ps, rs);
        }
    }
}
