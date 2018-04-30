/*---------------------------------------------------------------------------
   (C) Copyright Workbrain Inc. 2005
 --------------------------------------------------------------------------*/
package com.wbiag.util.callouts;

import org.apache.log4j.Logger;

import com.wbiag.app.modules.retailSchedule.services.CompressibleTaskScheduleCallout;
import com.wbiag.util.callouts.scheduling.CompressibleTaskLocationSetupCallout;
import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCallout;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;
import com.workbrain.util.callouts.scheduling.LocationSetupCallout;
import com.workbrain.util.callouts.scheduling.LocationSetupContext;

/**
 * Plug-in implementation for handling all callouts to be used for testing
 * the compressible task feature.
 *
 * @author James Tam
 */
public class CompressibleTaskAllCallout extends ScheduleCallout implements LocationSetupCallout {
    private static Logger logger = Logger.getLogger(CompressibleTaskAllCallout.class);

/* ------------------------Location Setup Callouts-------------------------- */
    /**
     * Deletes all compression factors related to a location, when the location
     * itself is being deleted.
     *
     * @param context Context object containing objects related to location
     * setup callouts
     * @return Whether or not to execute the standard deletion action in
     * <code>LocationNavigationAction.Detele()</code>
     * @throws CalloutException If an error occurs while executing this
     * callout
     */
    public boolean deleteLocationSetupPreAction (LocationSetupContext context) throws CalloutException {
        CompressibleTaskLocationSetupCallout compressibleTask = new CompressibleTaskLocationSetupCallout();
        try {
            compressibleTask.deleteAllLocCompressionFactors(context);
        } catch (RetailException re) {
            logger.error("Error occured while deleting Compression Factors: " + re.getMessage(), re);
            throw new CalloutException(re);
        }
        return true;
    }

    /**
     * Superficial implementation to add post-actions to the location setup
     * deletion action.
     *
     * @param context Context object containing objects related to location
     * setup callouts
     * @throws CalloutException if an error occurs while performing the
     * actions
     */
    public void deleteLocationSetupPostAction (LocationSetupContext context) throws CalloutException {
    }

    /**
     * Updates all compression factors related to a location, when the location
     * itself is being saved.
     *
     * @param context Context object containing objects related to location
     * setup callouts
     * @return Whether or not to execute the standard save action in
     * <code>LocationNavigationAction.FSubmitForm()</code>
     * @throws CalloutException If an error occurs while executing this
     * callout
     */
    public boolean saveLocationSetupPreAction (LocationSetupContext context) throws CalloutException {
        CompressibleTaskLocationSetupCallout compressibleTask = new CompressibleTaskLocationSetupCallout();
        try {
            compressibleTask.updateLocCompressionFactors(context);
        } catch (RetailException re) {
            logger.error("Error occured while updating Compression Factors: " + re.getMessage(), re);
            throw new CalloutException(re);
        }
        return true;
    }

    /**
     * Superficial implementation to add post-actions to the location setup
     * save action.
     *
     * @param context Context object containing objects related to location
     * setup callouts
     * @throws CalloutException if an error occurs while performing the
     * actions
    */
    public void saveLocationSetupPostAction (LocationSetupContext context) throws CalloutException {
    }

/* -----------------------------Schedule Callouts---------------------------- */
    /**
     * Enables the <i>Compressible Task</i> staff rule.
     *
     * @param soContext Context object containing objects related to schedule
     * callouts
     * @return Whether or not to execute the loop for adding IRs to the matrix
     * in the <code>PreProcessor.buildIntervalRequirementsData()</code> method
     * @throws CalloutException If an error occurs while excuting the
     * callout
     */
    public boolean addIRToMatrixPreLoop(SOData soContext) throws CalloutException {
        return true;
    }
    
    public void addIRToMatrixPostLoop(SOData soContext) throws CalloutException {
        CompressibleTaskScheduleCallout compressibleTask = new CompressibleTaskScheduleCallout();
        try {
            compressibleTask.activateCompressibleTaskRule(soContext);
        } catch (RetailException re) {
            logger.error("Error occured while enabling the Compressible Task rule: " + re.getMessage(), re);
            throw new CalloutException(re);
        }    	
    }

    /**
     * Retrieves the current SA staffing requirement being processed and adds it
     * to a list, which is stored in the context for the <i>Compressible Task</i>
     * rule.
     *
     * @param soContext Context object containing objects related to schedule
     * callouts
     * @return Whether or not to execute the code to perform the schedule area
     * workload calculation and distribution in the
     * <code>Scheduler.distributeScheduleAreaWorkload()</code> method
     * @throws CalloutException If an error occurs while executing the
     * callout
     */
    /*public boolean distributeScheduleAreaWorkloadPreAction(SOData soContext) throws CalloutException {
        CompressibleTaskScheduleCallout compressibleTask = new CompressibleTaskScheduleCallout();
        compressibleTask.storeSAStaffRequirement(soContext);
        return true;
    }*/

}