package com.wbiag.app.wbalert.source;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.workbrain.app.jsp.util.Timesheet;
import com.workbrain.server.data.AbstractRowCursor;
import com.workbrain.server.data.AbstractRowSource;
import com.workbrain.server.data.AccessException;
import com.workbrain.server.data.BasicRow;
import com.workbrain.server.data.Row;
import com.workbrain.server.data.RowCursor;
import com.workbrain.server.data.RowDefinition;
import com.workbrain.server.data.RowStructure;
import com.workbrain.server.data.type.DatetimeType;
import com.workbrain.server.data.type.StringType;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.DateHelper;
import com.workbrain.util.NestedRuntimeException;
import com.workbrain.util.StringHelper;

/** 
 * Title:			UnauthorizedTimesheetAlertSource
 * Description:		Row Source for UnauthorizedTimesheetAlert
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Dec 22, 2005
 * @author         	Kevin Tsoi
 */
public class UnauthorizedTimesheetAlertSource extends AbstractRowSource
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(UnauthorizedTimesheetAlertSource.class);    
    
    private RowDefinition rowDefinition;
    private DBConnection conn;
    private String employees;
    private String calcgroups;
    private String paygroups;
    private String teams;

    private java.util.List rows = new ArrayList();    
    
    public static final String PARAM_EMPLOYEES = "Employees";
    public static final String PARAM_CALCGROUPS = "Calcgroups";
    public static final String PARAM_PAYGROUPS = "Paygroups";
    public static final String PARAM_TEAMS = "Teams";    
    
    public static final String COL_EMP_ID = "EMP_ID";
    public static final String COL_EMP_NAME = "EMP_NAME";    
    public static final String COL_WRKS_WORK_DATE = "WRKS_WORK_DATE";
    public static final String COL_TIMESHEET_LINK = "timesheet";

    public static final String LIST_DELIMTER = ",";
    
    {
        RowStructure rs = new RowStructure(20);
        rs.add(COL_EMP_ID, StringType.get());
        rs.add(COL_EMP_NAME, StringType.get());        
        rs.add(COL_WRKS_WORK_DATE, StringType.get());
        rs.add(COL_TIMESHEET_LINK, StringType.get());    
        rowDefinition = new RowDefinition(-1,rs);
    }
    
    public UnauthorizedTimesheetAlertSource(DBConnection c , HashMap alertParams) 
    	throws AccessException 
    {
        employees = (String)alertParams.get(PARAM_EMPLOYEES);
        calcgroups = (String)alertParams.get(PARAM_CALCGROUPS);
        paygroups = (String)alertParams.get(PARAM_PAYGROUPS);
        teams = (String)alertParams.get(PARAM_TEAMS);                        
        
        try 
        {
            conn = c;
            loadRows();
        }
        catch (Exception e) 
        {
            throw new NestedRuntimeException (e);
        }
    }
    
    private void loadRows() 
    	throws Exception
    {
        int empId = 0;
        String empName = null;
        Date wrksDate = null;
        String wrksDateStr = null;
        StringBuffer tsLink = null;        
        String[] empArray = null;
        String[] calcGroupArray = null;
        String[] payGroupArray = null;
        String[] teamsArray = null;
        String params = "&DATE_SELECT=7&AUTH_SELECT=0&VIEW_SELECT=0&SUBMIT_PARAMS=T&ORDER_SELECT=0";
        
        //parse parameters into array
        empArray = StringHelper.detokenizeString(employees, LIST_DELIMTER);
        calcGroupArray = StringHelper.detokenizeString(calcgroups, LIST_DELIMTER);
        payGroupArray = StringHelper.detokenizeString(paygroups, LIST_DELIMTER);
        teamsArray = StringHelper.detokenizeString(teams, LIST_DELIMTER);
        
        rows.clear();        
        StringBuffer sb = new StringBuffer();
        
        sb.append(" SELECT ");
        sb.append(" EH.EMP_ID, ");
        sb.append(" EH.EMP_NAME, ");
        sb.append(" WS.WRKS_WORK_DATE ");

        sb.append(" FROM ");
        sb.append(" WORK_SUMMARY WS, ");
        sb.append(" EMPLOYEE_HISTORY EH, ");
        sb.append(" EMPLOYEE_TEAM ET ");

        sb.append(" WHERE ");
        sb.append(" EH.EMP_ID=WS.EMP_ID ");
        sb.append(" AND ");
        sb.append(" EH.EMP_ID=ET.EMP_ID ");
        sb.append(" AND ");
        sb.append(" WS.WRKS_WORK_DATE ");
        sb.append(" BETWEEN ");
        sb.append(" EH.EMPHIST_START_DATE ");
        sb.append(" AND ");
        sb.append(" EH.EMPHIST_END_DATE ");
        sb.append(" AND ");
        sb.append(" WS.WRKS_WORK_DATE ");
        sb.append(" BETWEEN ");
        sb.append(" ET.EMPT_START_DATE ");
        sb.append(" AND ");
        sb.append(" ET.EMPT_END_DATE ");
        sb.append(" AND ");
        sb.append(" ET.EMPT_HOME_TEAM='Y' ");
        sb.append(" AND ");
        sb.append(" (WS.WRKS_AUTHORIZED = 'N' ");
        sb.append(" OR ");
        sb.append(" WS.WRKS_AUTHORIZED IS NULL) ");
        
        if(empArray != null && empArray.length > 0)
        {
            sb.append(" AND ");
            sb.append(" EH.EMP_ID IN (");                        
            for (int i = 0; i < empArray.length; i++)
        	{
                sb.append(i > 0 ? ",?" : "?");
        	}
            sb.append(")");            
        }
        
        if(calcGroupArray != null && calcGroupArray.length > 0)
        {
            sb.append(" AND ");
            sb.append(" EH.CALCGRP_ID IN (");
            for (int i = 0; i < calcGroupArray.length; i++)
        	{
                sb.append(i > 0 ? ",?" : "?");
        	}
            sb.append(")");
        }
        
        if(payGroupArray != null && payGroupArray.length > 0)
        {
            sb.append(" AND ");
            sb.append(" EH.PAYGRP_ID IN (");
            for (int i = 0; i < payGroupArray.length; i++)
        	{
                sb.append(i > 0 ? ",?" : "?");
        	}
            sb.append(")");
        }
        
        if(teamsArray != null && teamsArray.length > 0)
        {
            sb.append(" AND ");
            sb.append(" ET.WBT_ID IN (");
            for (int i = 0; i < teamsArray.length; i++)
        	{
                sb.append(i > 0 ? ",?" : "?");
        	}
            sb.append(")");
        }
        
        sb.append(" ORDER BY EH.EMP_NAME ");

        PreparedStatement ps = null;
        ResultSet rs = null;
        int paramIndex = 1;
        try 
        {
            ps = conn.prepareStatement(sb.toString());       
            
            //bind parameters
            if(empArray != null && empArray.length > 0)
            {                    
                for (int i = 0; i < empArray.length; i++)
            	{
                    ps.setInt(paramIndex++, Integer.parseInt(empArray[i]));
            	}           
            }
            
            if(calcGroupArray != null && calcGroupArray.length > 0)
            {
                for (int i = 0; i < calcGroupArray.length; i++)
            	{
                    ps.setInt(paramIndex++, Integer.parseInt(calcGroupArray[i]));
            	}
            }
            
            if(payGroupArray != null && payGroupArray.length > 0)
            {
                for (int i = 0; i < payGroupArray.length; i++)
            	{
                    ps.setInt(paramIndex++, Integer.parseInt(payGroupArray[i]));
            	}
            }
            
            if(teamsArray != null && teamsArray.length > 0)
            {
                for (int i = 0; i < teamsArray.length; i++)
            	{
                    ps.setInt(paramIndex++, Integer.parseInt(teamsArray[i]));
            	}
            }            
            
            rs = ps.executeQuery();
                       
            while (rs.next()) 
            {
                empId = rs.getInt(1);
                empName = rs.getString(2);
                wrksDate = rs.getTimestamp(3);
                wrksDateStr = DatetimeType.FORMAT.format(wrksDate);
                
                //create timesheet link
                tsLink = new StringBuffer();
                tsLink.append("<a href=# onclick=\"");
                tsLink.append(Timesheet.getTimesheetLink(conn, empId, false, wrksDateStr, wrksDateStr, params,""));
                tsLink.append("\">");
                tsLink.append("link to timesheet");
                tsLink.append("</a>");
                                
                Row r = new BasicRow(getRowDefinition());
                r.setValue(COL_EMP_ID, String.valueOf(empId));
                r.setValue(COL_EMP_NAME, empName);
                r.setValue(COL_WRKS_WORK_DATE, DateHelper.convertDateString(wrksDate, "dd/MM/yyyy"));       
                r.setValue(COL_TIMESHEET_LINK, tsLink.toString());
                rows.add(r);                                
            }
        }
        finally 
        {
            SQLHelper.cleanUp(ps, rs);
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
        return new AbstractRowCursor(getRowDefinition())
        {
            private int counter = -1;
            protected Row getCurrentRowInternal()
            {
                return counter >= 0 && counter < rows.size() ? (BasicRow)rows.get(counter) : null;
            }
            protected boolean fetchRowInternal() 
            	throws AccessException
            {
                return ++counter < rows.size();
            }
            public void close(){}
        };
    }

    public boolean isReadOnly()
    {
       return true;
    }

    public int count() 
    {
        return rows.size();
    }

    public int count(String where) 
    {
        return rows.size();
    }    
}

