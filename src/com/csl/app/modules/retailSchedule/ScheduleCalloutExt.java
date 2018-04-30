package com.csl.app.modules.retailSchedule;

import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCallout;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;

import com.workbrain.server.registry.Registry;

/**
 * Hub to execute all Schedule Callout.  
 * 
 * This class needs to be registered in wb_registry SO_CUST_CALLOUT_CLASS, replacing the
 * default value from com.workbrain.app.modules.retailSchedule.services.ScheduleCallout 
 * to com.csl.app.modules.retailSchedule.ScheduleCalloutExt
 *
 * @author mahmed
 * Modified by Quoc Vuong and Rakesh Moddi to Incorporate into Common Solution Library March 2009
 * <p>Copyright: Copyright (c) 2009 Infor Global Solutions.</p> 
 *
 */
public class ScheduleCalloutExt extends ScheduleCallout {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ScheduleCalloutExt.class);

    private static String REGKEY_SO_USE_MEAL_BREAK_COVERAGE = "/system/modules/scheduleOptimization/SO_USE_MEAL_BREAK_COVERAGE";

	public void populateSchedulePostLoop(SOData soContext) throws CalloutException  {
        
    	BreakCoverageCallout breakCov = new BreakCoverageCallout();
        
		// Meal break Coverage
		String coverage = Registry.getVarString(REGKEY_SO_USE_MEAL_BREAK_COVERAGE, "FALSE").toUpperCase();     
		if( "TRUE".equals(coverage) || "T".equals(coverage) || "Y".equals(coverage) ) {
			breakCov.populateSchedulePostLoop(soContext);
		}
		else {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {  
                logger.debug( "No meal break coverage code executed" );
            }
		}
    }
    
    
}
