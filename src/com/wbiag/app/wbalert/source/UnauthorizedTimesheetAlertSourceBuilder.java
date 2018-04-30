package com.wbiag.app.wbalert.source;

import java.util.HashMap;

import com.workbrain.app.wbalert.AbstractWBAlertSourceBuilder;
import com.workbrain.server.data.RowSource;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.NestedRuntimeException;

/** 
 * Title:			Unauthorized Timesheet Alert Source Builder
 * Description:		Source Builder for UnauthorizedTimesheetAlertSource
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Dec 22, 2005
 * @author         	Kevin Tsoi
 */
public class UnauthorizedTimesheetAlertSourceBuilder extends AbstractWBAlertSourceBuilder
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(UnauthorizedTimesheetAlertSourceBuilder.class);            
           
    public UnauthorizedTimesheetAlertSourceBuilder() 
    {
    }
    
    public RowSource getRowSource(java.io.Serializable param, 
            DBConnection conn) 
    {
        HashMap exportParam = (HashMap)param;

        try 
        {
            UnauthorizedTimesheetAlertSource alertSource = new UnauthorizedTimesheetAlertSource(conn, exportParam);            
            return alertSource;
        } 
        catch (Exception e) 
        {
            logger.error(e.getMessage() , e);
            throw new NestedRuntimeException(e);
        }
    }
    
    public String getTaskParametersUI() 
    {
        return "/jobs/wbalert/unauthorizedTimesheetAlertParams.jsp";
    }    
}
