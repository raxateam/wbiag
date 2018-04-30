package com.wbiag.app.modules.retailSchedule.services;

import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCallout;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;

public class TransitionByStoreScheduleCallout extends ScheduleCallout{
	
	public boolean isTransition(SOData soContext) throws CalloutException {
	    boolean ret = false;
	    TransitionByStoreProcess process = new TransitionByStoreProcess (); 
	    try {
	        ret = process.isTransition(soContext);
	    }
	    catch (RetailException e) {
	        if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) logger.error("Error in isTransition" , e);
	        throw new CalloutException("Error in isTransition" , e);
	    }
	    return ret;
   	}

	
	public boolean buildTransitionShiftPreAction(SOData soContext) throws CalloutException 
	{
        boolean ret = false;
        TransitionByStoreProcess process = new TransitionByStoreProcess (); 
        try {
            ret = process.buildTransitionShiftPreAction(soContext);
        }
        catch (RetailException e) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) logger.error("Error in buildTransitionShiftPreAction" , e);
            throw new CalloutException("Error in buildTransitionShiftPreAction" , e);
        }
        return ret;
	}
	
}

