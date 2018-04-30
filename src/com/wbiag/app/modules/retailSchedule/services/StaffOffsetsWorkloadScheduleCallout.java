/*---------------------------------------------------------------------------
   (C) Copyright Workbrain Inc. 2005
 --------------------------------------------------------------------------*/
package com.wbiag.app.modules.retailSchedule.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntityStaffRequirement;
import com.workbrain.app.modules.retailSchedule.model.IntervalRequirement;
import com.workbrain.app.modules.retailSchedule.model.IntervalRequirementsManager;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.services.EmployeeAssignmentOptions;
import com.workbrain.app.modules.retailSchedule.services.PreProcessorUtils;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCallout;
import com.workbrain.app.modules.retailSchedule.services.ShiftPattern;
import com.workbrain.app.modules.retailSchedule.services.model.CalloutHelper;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;
import com.workbrain.app.modules.retailSchedule.services.model.LocalData.MutableBoolean;
import com.workbrain.app.modules.retailSchedule.services.model.LocalData.MutableInt;
import com.workbrain.app.modules.retailSchedule.utils.SODate;
import com.workbrain.app.modules.retailSchedule.utils.SODateTime;
import com.workbrain.app.modules.retailSchedule.utils.SOTime;
import com.workbrain.util.DateHelper;

/**
 * This class is for implementing the staff offsets feature to enable client
 * to schedule minimum required staffs during the staff offsets hours
 * Multiple callouts are utilize to implement this feature:
 *              ScheduleCallout.generateVariableShiftStartTimePostAction
 *              ScheduleCallout.includeShiftChoicePreAction
 *              ScheduleCallout.includeShiftChoicePostAction
 *              ScheduleCallout.addIRToMatrixPreAction
 * 
 * @author Daniel Teng, Jun Ma
 */
public class StaffOffsetsWorkloadScheduleCallout {
    private static Logger logger = Logger.getLogger(StaffOffsetsWorkloadScheduleCallout.class);
    
    private static String SO_CONTEXT_SHFTPATTERN        = ScheduleCallout.GENERATE_VARIABLE_SHIFT_START_TIME+CalloutHelper.TYPE_SHIFTPATTERN2;
    private static String SO_CONTEXT_START_TIMES_VECTOR = ScheduleCallout.GENERATE_VARIABLE_SHIFT_START_TIME+CalloutHelper.TYPE_LIST1;
    private static String SO_CONTEXT_EMPLOYEE_OPTION    = ScheduleCallout.GENERATE_VARIABLE_SHIFT_START_TIME+CalloutHelper.TYPE_EMP_ASSIGN_OPTION;
    private static String SO_CONTEXT_IS_FIXED           = ScheduleCallout.INCLUDE_SHIFT_CHOICE+CalloutHelper.TYPE_BOOLEEAN;
    
    private static String SO_CONTEXT_SHFTPATTERN_SHIFT_CHOICE = ScheduleCallout.INCLUDE_SHIFT_CHOICE+CalloutHelper.TYPE_SHIFTPATTERN;
    private static String SO_CONTEXT_SHIFT_START_TIME         = ScheduleCallout.INCLUDE_SHIFT_CHOICE+CalloutHelper.TYPE_INT;

    private static String SO_CONTEXT_STAFF_OFFSET_ARRAY    = ScheduleCallout.GENERATE_VARIABLE_SHIFT_START_TIME + "LBIShiftChoicesCallout:staffOffsets";
    private static String SO_CONTEXT_DUP_START_TIMES       = ScheduleCallout.INCLUDE_SHIFT_CHOICE + "LBIShiftChoicesCallout:dupStartTimeStaffOffsetMap";
    private static String SO_CONTEXT_ORIGINAL_SHIFT_LENGTH = ScheduleCallout.INCLUDE_SHIFT_CHOICE + "LBIShiftChoicesCallout:shiftLength";
    
    private static int NUM_START_OFFSET = 2;
    private static int START_STAFF_OFFSET = 0;
    private static int END_STAFF_OFFSET = 1;
    
    /**
     * This callout create duplicate shift choices by duplicating the start time. 
     * If staff offsets exist, any shift choices with start/end time that is equivalent to
     * hours of operation start/end time will be duplicated and the offset length is calculated
     * and stored in the HashMap and placed in the context to be applied later.
     *   
     * @param soContext - SOData which has all the preprocessor data
     * @throws RetailException - if any exception occurs
     */
    public static void generateVariableShiftStartTimePostAction(SOData soContext) throws RetailException {        
        // determine and save the original offsets
        int[] staffOffsets = (int[]) soContext.getLocalVariable(SO_CONTEXT_STAFF_OFFSET_ARRAY);
        if (staffOffsets == null) {    
            staffOffsets = new int[NUM_START_OFFSET];
            soContext.addLocalVariable(SO_CONTEXT_STAFF_OFFSET_ARRAY, staffOffsets);
            
            EmployeeAssignmentOptions empOption = (EmployeeAssignmentOptions) soContext.getLocalVariable(SO_CONTEXT_EMPLOYEE_OPTION);
            staffOffsets[START_STAFF_OFFSET] = empOption.getStaffStartOffset();           
            staffOffsets[END_STAFF_OFFSET] = empOption.getStaffEndOffset();         
        }

        if (staffOffsets[START_STAFF_OFFSET] != 0 || staffOffsets[END_STAFF_OFFSET] != 0) {
            duplicateShiftChoicesForStaffOffset(soContext);
        }
    }
    
    /**
     * Intercept all duplicated shifts from the previous callout and apply the offsets
     * that are calculated from the callout earlier. It temporary adjusts the shift 
     * length in ShiftPattern so that the offset is applied properly. The staff offset
     * is also removed from the EmployeeAssignmentOptions to prevent it from creating
     * the offsets in core code. 
     * 
     * @param soContext - SOData which has all the preprocessor data
     */
    public static void includeShiftChoicePreAction(SOData soContext) {
        
        HashMap startTimeStaffOffset = (HashMap) soContext.getLocalVariable(SO_CONTEXT_DUP_START_TIMES);
        MutableBoolean isFixed = (MutableBoolean) soContext.getLocalVariable(SO_CONTEXT_IS_FIXED);
        if (startTimeStaffOffset == null || isFixed.getBoolean()) {
            return;
        }
        
        MutableInt mutInt = (MutableInt) soContext.getLocalVariable(SO_CONTEXT_SHIFT_START_TIME);
        Double startTime = new Double(mutInt.getInt());
        ShiftPattern shift = (ShiftPattern) soContext.getLocalVariable(SO_CONTEXT_SHFTPATTERN_SHIFT_CHOICE);

        Integer offset = (Integer) startTimeStaffOffset.get(startTime);
        if (offset != null) {
            // apply the offset by modifying the shiftpattern shift
            shift.setShiftLength(offset.doubleValue() + shift.getShiftLength());
        }
        
        // remove the offset from employee option
        EmployeeAssignmentOptions empOption = (EmployeeAssignmentOptions) soContext.getLocalVariable(SO_CONTEXT_EMPLOYEE_OPTION);
        empOption.setStaffEndOffset(0);
        empOption.setStaffStartOffset(0);
    }
    
    /**
     * Restore the ShiftPattern original shift length and the staff offsets in EmployeeAssignmentOption.
     * The duplicated shift choices are also removed from the HashMap
     * 
     * @param soContext - SOData which has all the preprocessor data
     */
    public static void includeShiftChoicePostAction(SOData soContext) {
        HashMap startTimeStaffOffset = (HashMap) soContext.getLocalVariable(SO_CONTEXT_DUP_START_TIMES);
        MutableBoolean isFixed = (MutableBoolean) soContext.getLocalVariable(SO_CONTEXT_IS_FIXED);
        if (startTimeStaffOffset == null || isFixed.getBoolean()) {
            return;
        }
        MutableInt mutInt = (MutableInt) soContext.getLocalVariable(SO_CONTEXT_SHIFT_START_TIME);
        Double startTime = new Double(mutInt.getInt());
        Integer offset = (Integer) startTimeStaffOffset.get(startTime);
        if (offset != null) {
            // remove the offset from hash
            startTimeStaffOffset.remove(startTime);
            // revert the shift length back to the original
            Double orgShiftLength = (Double) soContext.getCustomData(SO_CONTEXT_ORIGINAL_SHIFT_LENGTH);
            ShiftPattern shift = (ShiftPattern) soContext.getLocalVariable(SO_CONTEXT_SHFTPATTERN_SHIFT_CHOICE);
            shift.setShiftLength(orgShiftLength.doubleValue());
        }
        
        // revert the employee option back to the original
        EmployeeAssignmentOptions empOption = (EmployeeAssignmentOptions) soContext.getLocalVariable(SO_CONTEXT_EMPLOYEE_OPTION);
        int[] staffOffsets = (int[]) soContext.getLocalVariable(SO_CONTEXT_STAFF_OFFSET_ARRAY);
        empOption.setStaffStartOffset(staffOffsets[START_STAFF_OFFSET]);
        empOption.setStaffEndOffset(staffOffsets[END_STAFF_OFFSET]);
    }
    
    /**
     * This method modifies the interval requirement such that the requirement
     * for the staff offsets interval has the minimum staff requirement.  This
     * changes will be reflected in the Schedule IR and the IR temp file that is
     * send to dash engine.  The changes here will ensure that enough staffs are
     * schedule to cover the staff offsets time.
     * 
     * @param SOData - preprocessor data
     * @exception CalloutException - if error occurs
     */
    public static void addIRToMatrixPreAction(SOData soContext)
                                       throws CalloutException {
        ScheduleGroupData skdgrpData = null;
        IntervalRequirement intReq = CalloutHelper.getIntervalRequirement(
                soContext, ScheduleCallout.ADD_IR_TO_MATRIX);
        int skdgrpId = intReq.getSkdgrpId();

        try {
            skdgrpData = ScheduleGroupData.getScheduleGroupData(skdgrpId);
            CorporateEntity ce = skdgrpData.getCorporateEntity();

            // obtain the offset hours
            double startOffset = skdgrpData.getSkdgrpStfstrOfs();
            double endOffset = skdgrpData.getSkdgrpStfendOfs();

            // don't need to do anything if offsets not used
            if (startOffset == 0 && endOffset == 0)
                return;

            // obtain the hours of operation
            CorporateEntity corpEnt = skdgrpData.getCorporateEntity();

            // check which day of the week the change will be applied to
            Date intReqDate = intReq.getIntreqDate();

            // find the schedule start and end times for that day of the week
            // and convert to Date format
            SOTime openTime = corpEnt.getOpenTime(new SODate(intReqDate));
            SOTime closeTime = corpEnt.getCloseTime(new SODate(intReqDate));
            
            // No hours of op defined - treat the day as if the store is closed
            if ((openTime == null) || (closeTime == null)){
            	if (logger.isDebugEnabled()) logger.debug("Store is closed on :" + intReqDate + ", exiting");
            	return;
            }
            
            Date hoursOpen = convertToDate(openTime);
            Date hoursClose = convertToDate(closeTime);

            // check if the interval is within the start or end offset interval
            // and set to minimum requirement
            Date staffStartOff = DateHelper.addMinutes(hoursOpen, ((int) Math
                    .round(startOffset * 60)));
            Date staffEndOff = DateHelper.addMinutes(hoursClose, ((int) Math
                    .round(endOffset * 60)));
            Date curInterval = intReq.getIntreqTime();
            if ((curInterval.compareTo(staffStartOff) >= 0 && curInterval
                    .compareTo(hoursOpen) < 0)
                    || (curInterval.compareTo(hoursClose) >= 0 && curInterval
                            .compareTo(staffEndOff) < 0)) {
                // obtains min requirements from corporate staff requirements
                // list
                int minRequirement = getMinRequirement(ce.getStaffReqList(),
                        intReq);
                intReq.setIntreqTotalReq(minRequirement);
                intReq.setIntreqMintotlReq(minRequirement);
                IntervalRequirementsManager irm = CalloutHelper
                        .getIntervalRequirementsManager(soContext,
                                ScheduleCallout.ADD_IR_TO_MATRIX);
                irm.update(irm.getIRTemporaryID(), minRequirement);
                // check to ensure the requirement is written to the temp file
                boolean[] m_aAllowedIR = (boolean[]) soContext.getLocalVariable(ScheduleCallout.ADD_IR_TO_MATRIX+CalloutHelper.TYPE_BOOLEAN_ARRAY);
                int period = irm.getCurrentElementIndex();
                m_aAllowedIR[period] = true;               
            }

            // set the interval requirement to the one that has been adjusted
            soContext.updateLocalVariable(ScheduleCallout.ADD_IR_TO_MATRIX
                    + CalloutHelper.TYPE_IR, intReq);
            // CalloutHelper.setIntervalRequirement(soContext,
            // ScheduleCallout.ADD_IR_TO_MATRIX, intReq);
        } catch (RetailException e) {
            throw new CalloutException(e);
        }

        return;
    }

    /**
     * Iterate through all possible starttimes and calculate the start/end offsets
     * if offsets apply.  The offsets are store in a HashMap and place in a context
     * that will be used in another callout.  Note that shift choices are duplicated
     * by duplicating the start times.
     * 
     * @param soContext - SOData which has all the preprocessor data
     * @throws RetailException - if any exception occurs
     */
    private static void duplicateShiftChoicesForStaffOffset(SOData soContext)
                                                            throws RetailException {
        ShiftPattern shift = (ShiftPattern) soContext.getLocalVariable(SO_CONTEXT_SHFTPATTERN);
        Vector startTimes = (Vector) soContext.getLocalVariable(SO_CONTEXT_START_TIMES_VECTOR);        
        
        int intervalLengthInMinutes = soContext.getIntervalType().getLengthInMinutes();
        EmployeeAssignmentOptions empOption = (EmployeeAssignmentOptions) soContext.getLocalVariable(SO_CONTEXT_EMPLOYEE_OPTION);
        int staffStartOffset = empOption.calculateNumberStaffOffsetIntervals(0, intervalLengthInMinutes);           
        int staffEndOffset = Math.abs(empOption.calculateNumberStaffOffsetIntervals(1, intervalLengthInMinutes));
    
        HashMap startTimeStaffOffset = new HashMap();
        List dupStartTimeList = new ArrayList();
        
        SODate skdStartDate = new SODate(soContext.getSchedule().getSkdFromDate());
        Iterator itStartTime = startTimes.iterator();
        while(itStartTime.hasNext()) {
            Double startTime = (Double)itStartTime.next();
            
            SODateTime startDateTime = periodToTime(startTime, skdStartDate, 
                                                    intervalLengthInMinutes);
            
            SOTime shiftStartTime = startDateTime.toSOTime();
            SODate shiftDate = startDateTime.toSODate();
            Double newStartTime = null;
            int totalOffset = 0;
            CorporateEntity oCE = (CorporateEntity) soContext.getScheduleCE();
            if(staffStartOffset < 0) {
                SOTime openTime = oCE.getOpenTime(shiftDate);
                if (shiftStartTime.equals(openTime)) {
                    totalOffset += (staffStartOffset * -1);
                    newStartTime = new Double(startTime.doubleValue() - Math.abs(staffStartOffset));
                    
                }
            }

            if (staffEndOffset != 0) {
                SOTime shiftEndTime = shiftStartTime;
                shiftEndTime.add(Calendar.MINUTE, (int)(shift.getShiftLength() * intervalLengthInMinutes));
                SOTime closeTime = oCE.getCloseTime(shiftDate);                
                // apply offset for end time if end time match closing time
                if (shiftEndTime.equals(closeTime)) {
                    totalOffset += staffEndOffset;
                    if (newStartTime == null) {
                        newStartTime = new Double(startTime.doubleValue());
                    }
                }
            }
            
            if (newStartTime != null) {
                // store the calculated offset and the new start time
                startTimeStaffOffset.put(newStartTime, new Integer(totalOffset));
                dupStartTimeList.add(newStartTime);
            }
        }
        // add all the new startTime
        startTimes.addAll(dupStartTimeList);
        soContext.setLocalVariable(SO_CONTEXT_DUP_START_TIMES, startTimeStaffOffset);
        // store original shiftlength in context
        soContext.addCustomData(SO_CONTEXT_ORIGINAL_SHIFT_LENGTH, new Double(shift.getShiftLength()));
    }
    
    /**
     * Convert interval start time to SODateTime
     * 
     * @param startTime - interval start time
     * @param skdStartDate - schedule start date
     * @param intervalLengthInMinutes - interval length in minutes
     * @return SODateTime of the interval start time
     * @throws RetailException - if any errors occur during conversion
     */
    private static SODateTime periodToTime(Double startTime, SODate skdStartDate, 
                                           int intervalLengthInMinutes) throws RetailException {
        return PreProcessorUtils.intervalToTime(skdStartDate, startTime.intValue(),
                                                intervalLengthInMinutes);
    }
    
    /**
     * This method coverts SOTime to Date
     * @param soTime - SOTime to be converted
     * @return Date object from the SOTime
     */
    private static Date convertToDate(SOTime soTime) {
        Calendar convertedDateCal = soTime.getOriginalCalendar();
        convertedDateCal.getTime();
        return convertedDateCal.getTime();
    }
    
    /**
     * This method returns the staff minimum requirements per interval
     * 
     * @param staffReqList - list of staff requirement
     * @param intReq - current interval requirement to be changed
     * @return minimun requirement for this location
     * @throws RetailException - if any error occur
     */
    private static int getMinRequirement(Vector staffReqList,
                                         IntervalRequirement intReq) 
                                         throws RetailException {
        int minRequirement = 0;
        boolean flag = false;

        Iterator staffReqIter = staffReqList.iterator();

        while (staffReqIter.hasNext() && flag == false) {
            CorporateEntityStaffRequirement cesr = (CorporateEntityStaffRequirement) staffReqIter
                    .next();
            if (cesr != null) {
                // if the staff requirement is for the same job and same
                // activity, it's the one that is needed
                if (intReq.getJobId() == cesr.getJobId().intValue()
                        && intReq.getActId().intValue() == cesr.getActId()
                                .intValue()) {
                    // for all date effective requiremts, make sure schedule
                    // date is within
                    // reqirements dates
                    if (cesr.getCsdEffStartDate() != null
                            && cesr.getCsdEffEndDate() != null) {
                        if (intReq.getIntreqDate().compareTo(
                                cesr.getCsdEffStartDate()) >= 0
                                && intReq.getIntreqDate().compareTo(
                                        cesr.getCsdEffEndDate()) <= 0) {
                            minRequirement = cesr.getCsdMinReq().intValue();
                            flag = true;
                        }
                    }
                    // if no dates are given, assume that requirements is valid
                    // indefinitely
                    else if (cesr.getCsdEffStartDate() == null
                            || cesr.getCsdEffEndDate() == null) {
                        minRequirement = cesr.getCsdMinReq().intValue();
                        flag = true;
                    }
                }

            }
        }

        return minRequirement;
    }

}