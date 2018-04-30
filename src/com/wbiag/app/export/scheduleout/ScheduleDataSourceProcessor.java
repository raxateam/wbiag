package com.wbiag.app.export.scheduleout;


import java.util.*;

import com.workbrain.sql.*;
import com.workbrain.server.data.*;
import com.workbrain.util.NestedRuntimeException;
import com.workbrain.app.export.process.*;
import com.wbiag.app.export.scheduleout.ScheduleDataSource;


public class ScheduleDataSourceProcessor implements ExportTransactionType 
{
	public ScheduleDataSourceProcessor(){}
	
    /**
     *  Returns a new SampleExportRowSource
     *
     *
     *@param  param  Parameters of the export
     *@param  conn   Connection
     *@return        The RowSource value
     */
    public RowSource getRowSource(Map param, DBConnection conn) {
        try {
            return new ScheduleDataSource(conn, (HashMap) param);
        }
        catch (Exception ex) {
           throw new NestedRuntimeException (ex);
        }
    }
    public String getTaskUI() {
        return "/jobs/ent/jobs/scheduleExportParams.jsp";
    }
    
    public void initializeTransaction(DBConnection conn) throws Exception {
    }

    public void finalizeTransaction(DBConnection conn) throws Exception {
    }
}

