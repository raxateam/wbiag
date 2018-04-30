/*---------------------------------------------------------------------------
   (C) Copyright Workbrain Inc. 2005
 --------------------------------------------------------------------------*/
package com.wbiag.app.modules.retailSchedule.services;
import org.apache.log4j.Logger;

import com.wbiag.app.modules.retailSchedule.services.SmallEventsScheduleCallout;
import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCallout;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;

/**
 * Callout class that calls the small event implementation code.
 * First callout invokes a custom temp file creation.
 * Second callout replace standard rules with custom small events rules
 * Third callout write Small Events shift to small events files - any PFS
 *      shifts that has length smaller than minimum shift length
 * Fourth callout remove all small events shift from the schedule and place 
 *      it in context
 * Fifth callout will loop through all small events shifts and blend them with
 *      any overlapping shift.  If no overlap found, small events will be added
 *      back to the schedule
 */

public class SmallEventsAllCallout extends ScheduleCallout {
    private static Logger logger = Logger.getLogger(SmallEventsAllCallout.class);
   
    /**
    * Override of method thereby enabling creation of the custom temp file
    * 
    * @param    soContext 
    */
    public void buildViewRuleFilePostLoop(SOData soContext) throws CalloutException
    {
        try {
            SmallEventsScheduleCallout.createSmallEventsFile(soContext);
        } catch(RetailException re) {
            logger.error(re.getMessage(), re);
            throw new CalloutException(re);
        }
    }
    
    /**
     * Override of method thereby enabling substition of the rule ID with 
     * small events custom rule ID custom rules file. This will activate
     * the corresponsing mosel code in the mosel alpha file.
     * 
     * @param    soContext
     */
    public boolean buildViewRuleFilePreAction(SOData soContext) throws CalloutException {
        try {
        	return SmallEventsScheduleCallout.writeSmallEventsRuleID(soContext);
        } catch(RetailException re) {
            logger.error(re.getMessage(), re);
            throw new CalloutException(re);
        }
    }
    
    /**
     * Override of method thereby enabling writing of all PFSs smaller than a 
     * specified length to a custom temp file.
     * 
     * @param    soContext
     */
    public void writeShiftIntoFilePostAction(SOData soContext) throws CalloutException {
        try {
            SmallEventsScheduleCallout.addSmallPFSToFile(soContext);
        } catch(RetailException re) {
            logger.error(re.getMessage(), re);
            throw new CalloutException(re);
        }
    }
    
    /**
     * Removes shifs details created for small events from schedule.
     * Collects those removed shifts in a list stored in soContext
     * 
     * @param soContext
     * @throws CalloutException
     */
    public void populateSchedulePostAction(SOData soContext) throws CalloutException {
        SmallEventsScheduleCallout.populateSchedulePostAction(soContext);
    }
    
    /** 
     * Loops through shift details (small events) removed in {@link #populateSchedulePostAction(SOData)}
     * and blends them with overlapping shifts.<br> 
     * If there are no overlapping shifts then the removed  shift detail is restored.<br>
     * If overlapping shift detail start or end time changed after merging, new shift is created
     * for such shift detail
     * 
     * @param soContext
     * @throws CalloutException
     */
    public void populateSchedulePostLoop(SOData soContext) throws CalloutException {
        SmallEventsScheduleCallout.populateSchedulePostLoop(soContext);
    }
    
}