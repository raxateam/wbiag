/*---------------------------------------------------------------------------
   (C) Copyright Workbrain Inc. 2005
 --------------------------------------------------------------------------*/
package com.wbiag.app.modules.retailSchedule.services;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCallout;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;

public class StaffOffsetsWorkloadAllScheduleCallout extends ScheduleCallout {
    private static Logger logger = Logger.getLogger(StaffOffsetsWorkloadAllScheduleCallout.class);
     
    /**
     * This callout creates a duplicate shift choices by duplicating the start time. 
     * If staff offsets exists, any shift choices with start/end time that is equivalent to
     * hours of operation start/end time will be duplicated and the offset length is calculated
     * and stored in the HashMap and placed in the context to be applied later.
     *   
     * @param soContext - SOData which has all the preprocessor data
     * @throws RetailException - if any exception occurs
     */
    public synchronized void generateVariableShiftStartTimePostAction(SOData soContext) throws CalloutException
    {
        try {
            StaffOffsetsWorkloadScheduleCallout.generateVariableShiftStartTimePostAction(soContext);
        } catch(RetailException re) {
            logger.error(re.getMessage(), re);
            throw new CalloutException(re);
        }
    }
    
    /**
     * Intercept all duplicated shifts from the previous callout and apply the offsets
     * that is calculated from the callout earlier. It temporary adjust the shift 
     * length in ShiftPattern so that the offset is applied properly. The staff offsets
     * is also removed from the EmployeeAssignmentOptions to prevent it from creating
     * the offsets in core code. 
     * 
     * @param soContext - SOData which has all the preprocessor data
     */
    public boolean includeShiftChoicePreAction(SOData soContext) throws CalloutException
    {
        StaffOffsetsWorkloadScheduleCallout.includeShiftChoicePreAction(soContext);
        return true;
    }
    
    /**
     * Restore the ShiftPattern original shift length and the staff offsets in EmployeeAssignmentOption.
     * The duplicated shift choices is also removed from the HashMap
     * 
     * @param soContext - SOData which has all the preprocessor data
     */
    public void includeShiftChoicePostAction(SOData soContext) throws CalloutException
    {
        StaffOffsetsWorkloadScheduleCallout.includeShiftChoicePostAction(soContext);
    }
    
    /**
     * This callout modifies the interval requirement such that the requirement
     * for the staff offsets interval has the minimum staff requirement.  This
     * changes will be reflected in the Schedule IR and the IR temp file that is
     * send to dash engine.  The changes here will ensure that enough staffs are
     * schedule to cover the staff offsets time.
     * 
     * @param SOData - preprocessor data
     * @exception CalloutException - if error occurs
     */
    public boolean addIRToMatrixPreAction(SOData soContext) throws CalloutException {
        StaffOffsetsWorkloadScheduleCallout.addIRToMatrixPreAction(soContext);
        return true;
    }
}