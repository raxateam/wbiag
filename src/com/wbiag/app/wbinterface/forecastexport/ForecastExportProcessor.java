package com.wbiag.app.wbinterface.forecastexport;

import java.util.HashMap;
import java.util.Map;

import com.workbrain.app.export.process.ExportTransactionType;
import com.workbrain.app.export.process.RowSourceExportProcessor;
import com.workbrain.app.wbinterface.db.TransactionAccess;
import com.workbrain.app.wbinterface.db.TransactionData;
import com.workbrain.server.data.RowSource;
import com.workbrain.sql.DBConnection;

/** 
 * Title:			Forecast Export Processor
 * Description:		Transaction class for the forecast export
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Mar 21, 2006
 * @author         	Kevin Tsoi
 */
public class ForecastExportProcessor implements ExportTransactionType
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ForecastExportProcessor.class);
    public static final String PARAMS_PAGE_LOCATION = "/jobs/wbinterface/forecastExportParams.jsp";
    private HashMap exportParam = null;
    
    public RowSource getRowSource(Map param, DBConnection conn) 
    {
        exportParam = (HashMap) param;

        try 
        {
            return new ForecastExportDataSource(conn, exportParam);
        }
        catch (Exception e) 
        {
            return null;
        }
    }
    
    public String getTaskUI() 
    {
        return PARAMS_PAGE_LOCATION;
    }
    
    public void initializeTransaction(DBConnection conn) 
		throws Exception 
	{
	}

	public void finalizeTransaction(DBConnection conn) 
		throws Exception 
	{
    	try 
    	{
    		int wbitranId = ((Integer) this.exportParam.get(RowSourceExportProcessor.PARAM_TRANSACTION_ID)).intValue();
    		// Empty Transmitter. Does not do anything just to complete the requirement of export processor
    		TransactionAccess ta = new TransactionAccess(conn);
    		TransactionData td = ta.load(wbitranId);
    		td.setWbitranStatus(TransactionData.STATUS_PENDING);
    		ta.save(td);
    		conn.commit();
    	} 
    	catch (Exception e)
    	{    		
    		logger.error(e);    		
    		throw e;
    	}
	}
}
