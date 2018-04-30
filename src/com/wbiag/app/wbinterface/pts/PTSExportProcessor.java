package com.wbiag.app.wbinterface.pts;

import java.util.HashMap;
import java.util.Map;

import com.workbrain.app.export.process.ExportTransactionType;
import com.workbrain.server.data.RowSource;
import com.workbrain.sql.DBConnection;

/** 
 * Title:			PTSExportProcessor
 * Description:		Exports the PTS data
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Jun 27, 2005
 * @author         	Kevin Tsoi
 */
public class PTSExportProcessor implements ExportTransactionType
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PTSExportProcessor.class);
    public static final String PARAMS_PAGE_LOCATION = "/jobs/pts/PTSExportTaskParams.jsp";
    public static final String PARAM_OFFSET = "offset";
    public static final String PARAM_NUM_OF_DAYS = "numberOfDays";
    
    public PTSExportProcessor()
    {        
    }
    
    public String getTaskUI() 
    {
        return PARAMS_PAGE_LOCATION;
    }
    
    public RowSource getRowSource(Map param, DBConnection conn) 
    {
        HashMap exportParam = (HashMap) param;

        try 
        {
            return new PTSDataSource(conn, exportParam);
        }
        catch (Exception e) 
        {
            return null;
        }
    }
    
    public void initializeTransaction(DBConnection conn) 
    	throws Exception 
    {
    }

    public void finalizeTransaction(DBConnection conn) 
    	throws Exception 
    {
    }
}
