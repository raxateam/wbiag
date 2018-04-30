package com.csl.app.modules.retailSchedule;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.util.callouts.scheduling.DefaultScheduleUICallout;
import com.workbrain.util.callouts.scheduling.ScheduleUIContext;

/**
 * Hub to execute all Schedule UI Callout.  
 * 
 * This class needs to be registered in wb_registry callouts_ScheduleUICallout, replacing the
 * default value from com.workbrain.util.callouts.scheduling.DefaultScheduleUICallout
 * to com.csl.app.modules.retailSchedule.ScheduleUICallout
 *
 * @author qvuong
 * 
 * <p>Copyright: Copyright (c) 2009 Infor Global Solutions.</p> 
 *
 */

public class ScheduleUICallout extends DefaultScheduleUICallout 
{
    public static Logger logger = Logger.getLogger(ScheduleUICallout.class);    
    
    /**
     * @see com.workbrain.util.callouts.scheduling.ScheduleUICallout#generateCustomJSData(com.workbrain.util.callouts.scheduling.ScheduleUIContext)
     */
    public void generateCustomJSData(ScheduleUIContext context) throws CalloutException {
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {  
            logger.debug( "generateCustomJSData() Start" );
        }        

        com.csl.app.modules.retailSchedule.ScheduleHoursSummary.generateSummary(context);

        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {  
            logger.debug( "generateCustomJSData() End" );
        }        
}

}

