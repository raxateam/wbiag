package com.wbiag.app.wbinterface.forecastexport;

import java.util.Map;

import com.workbrain.app.wbinterface.WBInterfaceException;
import com.workbrain.app.wbinterface.mapping_rowsource.Transmitter;
import com.workbrain.sql.DBConnection;

/** 
 * Title:			Forecast Export Transmitter
 * Description:		Empty transmitter for Forecast Export
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Mar 24, 2006
 * @author         	Kevin Tsoi
 */
public class ForecastExportTransmitter implements Transmitter
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ForecastExportTransmitter.class);
    String nameFields = null;

    public void execute( DBConnection conn, Map runtimeParam )
    	throws WBInterfaceException 
    {
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) 
        {
            logger.debug( "ForecastExportTransmitter.execute(" + runtimeParam + ")" );
        }
        // Empty Transmitter. Does not do anything just to complete the requirement of export processor
       
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) 
        { 
            logger.debug("ForecastExportTransmitter completed processing sucessfully!");
        }
     }
    
     /**
      * Returns transmitterUI jsp path.
      *
      * @return path
      */
     public String getTransmitterUI() 
     {
        return null;
     }
}
