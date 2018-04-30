package com.wbiag.app.ta.ruleengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.workbrain.app.ta.model.EmployeeData;
import com.workbrain.app.ta.model.EmployeeScheduleData;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.model.OverrideList;
import com.workbrain.app.ta.ruleengine.DataEvent;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;

/**
 * @author bviveiros
 * 
 * Proof of Concept for Woolworths.
 * 
 * If a 50% or more of a shift crosses midnight, the shift should appear on the next
 * day.  Once a shift is moved, all rules and reports should work correctly.
 * 
 * These are data events related to moving a shift, and clocks.
 * 
 * Change History:
 * 19/08/2005 - KTSOI - changed logic to move shifts in beforeEmployeeScheduleOverrides
 *
 */
public class DataEventMoveShift extends DataEvent 
{
	private static org.apache.log4j.Logger logger = 
			org.apache.log4j.Logger.getLogger(DataEventMoveShift.class);
	
	private static String TEMP_OVR_STATUS = "TEMP";
	private static String TEMP_OVR_FLAG1 = "T";
	private static int TEMP_OVR_TYPE_ID = -9999;
	
	// Use a pre-deterined cutoff time.  If a shift has been moved off this day,
	// and a clock occurs after this cutoff time, the clock will also be moved to
	// the next day.
	//public static final Date clockCutoffTime = DateHelper.parseDate("21:59:59", "HH:mm:ss");

	// Flag to indicate that a shift was moved off this day.
	//protected static final String MOVED_SHIFT_FLAG = "wrksFlag1";
	//protected static final String MOVED_SHIFT_FLAG_DB = "WRKS_FLAG1";			
	
	/* (non-Javadoc)
	 * @see com.workbrain.app.ta.ruleengine.DataEvent#beforeScheduleDetailOverrides(com.workbrain.app.ta.ruleengine.WBData, com.workbrain.app.ta.model.OverrideList, com.workbrain.sql.DBConnection)
	 */
	public void beforeEmployeeScheduleOverrides(OverrideList ovrList, 
	        DBConnection conn, 
	        EmployeeData empData, 
	        EmployeeScheduleData empSchedData)  
	{	    
	    OverrideData overrideData = null;
	    List scheduleOvrsList = null;
	    List currentShiftList = null;
	    Iterator itSchOvrsList = null;
	    Date shiftStartTime = null;
	    Date shiftEndTime = null;
	    Date schOvrStartTime = null;
	    Date schOvrEndTime = null;
	    boolean firstOvr = true; 
	    
	    currentShiftList = new ArrayList();
	    
	    //get all schedule detail overrides for the day
	    scheduleOvrsList = getScheduleOvrsList(ovrList);
	    
	    //sort schedule detail overrides according to start time
	    sortByScheduleStartTime(scheduleOvrsList);	    	    
	    
	    //iterate through list of sorted schedule detail overrides
	    itSchOvrsList = scheduleOvrsList.iterator();
	    while(itSchOvrsList.hasNext())
	    {
	        overrideData = (OverrideData)itSchOvrsList.next();
	        schOvrStartTime = overrideData.getOvrStartTime();
	        schOvrEndTime = overrideData.getOvrEndTime();
	        
	        //build shift starting from first element in list
	        if(firstOvr)
	        {
	            currentShiftList.add(overrideData);
	            shiftStartTime = schOvrStartTime;
	            shiftEndTime = schOvrEndTime;	     
	            firstOvr = false;
	        }
	        else
	        {
	            //belongs to current shift
	            if(!schOvrStartTime.after(shiftEndTime))
	            {
	                //sets new shift end time
	                shiftEndTime = DateHelper.max(shiftEndTime, schOvrEndTime);
	                currentShiftList.add(overrideData);
	            }
	            //start of new shift
	            else
	            {
	                //check to see if currentShiftList need to be moved to next day	        		
	        		if(isHalfShiftPastMidnight(shiftStartTime, shiftEndTime, 
	        		        DateHelper.addDays(overrideData.getOvrStartDate(), 1))) 
	        		{
	        		    moveShift(currentShiftList);
	        		}
	        		//start of new shift
	        		currentShiftList = new ArrayList();
	        		currentShiftList.add(overrideData);
	        		shiftStartTime = schOvrStartTime;
		            shiftEndTime = schOvrEndTime;
	            }
	        }
	    }
	    //check to see if last currentShiftList need to be moved to next day
	    if(!firstOvr && isHalfShiftPastMidnight(shiftStartTime, shiftEndTime, 
		        DateHelper.addDays(overrideData.getOvrStartDate(), 1))) 
		{
		    moveShift(currentShiftList);
		}
	}		

	/* (non-Javadoc)
	 * @see com.workbrain.app.ta.ruleengine.DataEvent#beforeOneEmployeeScheduleOverride(com.workbrain.app.ta.model.OverrideData, com.workbrain.sql.DBConnection, com.workbrain.app.ta.model.EmployeeData, com.workbrain.app.ta.model.EmployeeScheduleData)
	 */
	public void beforeOneEmployeeScheduleOverride(OverrideData od, 
	        DBConnection conn, 
	        EmployeeData empData, 
	        EmployeeScheduleData empSchedData)
	{
	    //if override had start date changed
	    if(TEMP_OVR_FLAG1.equals(od.getOvrFlag1()))
	    {	        
	        //Core expects all overrides to be for the current day so it processes these overrides to the employee schedule without
	        //checking the override start date.  Since we changed the override start date, we don't want core to process these overrides
	        //for the current day.  Therefore, we need to temporary change type and status in order to by pass the empSchedule processing by core. 
	        od.setOvrtypId(TEMP_OVR_TYPE_ID);
	        od.setOvrStatus(TEMP_OVR_STATUS);
	    }
	}
	
	/* (non-Javadoc)
	 * @see com.workbrain.app.ta.ruleengine.DataEvent#afterOneEmployeeScheduleOverride(com.workbrain.app.ta.model.OverrideData, com.workbrain.sql.DBConnection, com.workbrain.app.ta.model.EmployeeData, com.workbrain.app.ta.model.EmployeeScheduleData)
	 */
	public void afterOneEmployeeScheduleOverride(OverrideData od, 
	        DBConnection conn, 
	        EmployeeData empData, 
	        EmployeeScheduleData empSchedData)
	{
	    //if override had start date changed
	    if(TEMP_OVR_FLAG1.equals(od.getOvrFlag1()))
	    {
	        //reset override flag1, type, and status to original values
	        od.setOvrFlag1(null);
	        od.setOvrtypId(OverrideData.SCHEDULE_DETAIL_TYPE);
	        od.setOvrStatus(OverrideData.PENDING);
	    }
	}	
	
	/**
	 * Filters on schedule detail type
	 * 
	 * @param originalList
	 * @return
	 */
	public List getScheduleOvrsList(List originalList)
	{
	    OverrideData overrideData = null;
	    List copiedList = null;
	    Iterator itOrgList = null;
	    
	    copiedList = new ArrayList();
	    
	    //iterate through original list
	    itOrgList = originalList.iterator();
	    while(itOrgList.hasNext())
	    {
	        overrideData = (OverrideData)itOrgList.next();
	        
	        //add schedule overrides to copiedList
	        if(overrideData.getOvrtypId() == OverrideData.SCHEDULE_DETAIL_TYPE)
	        {
	            copiedList.add(overrideData);
	        }
	    }	    
	    return copiedList;
	}
	
    /**
     * Sorts list based on override start date
     * 
     * @param sortList
     */
    public void sortByScheduleStartTime(List sortList)
    {
        Collections.sort(sortList, 
            new Comparator()
            {
	            public int compare(Object o1, Object o2)
	            {
	                OverrideData od1 = null;
	                OverrideData od2 = null;
	                Date scheduleStartTime1 = null;
	                Date scheduleStartTime2 = null;
	
	                od1 = (OverrideData)o1;
	                od2 = (OverrideData)o2;
	                scheduleStartTime1 = od1.getOvrStartTime();
	                scheduleStartTime2 = od2.getOvrStartTime();	                
	                
	                int i = DateHelper.compare(scheduleStartTime1, scheduleStartTime2);
	                if(i==0) 
                    {
	                    i = 1;
                    }		                
	                return i;
	            }
            });
    }
	
    /**
     * Moves override start date to the next day
     * 
     * @param shiftList
     */
    public void moveShift(List shiftList)
    {
        OverrideData overrideData = null;
        Iterator itShift = null;
        
        //iterate through overrides for this shift
        itShift = shiftList.iterator();
        while(itShift.hasNext())
        {
            overrideData = (OverrideData)itShift.next();
            //move the start and end date of the schedule overrides to the next day
            overrideData.setOvrStartDate(DateHelper.addDays(overrideData.getOvrStartDate(), 1));
            overrideData.setOvrEndDate(DateHelper.addDays(overrideData.getOvrEndDate(), 1));
            //mark overrides that had their override start date changed
            overrideData.setOvrFlag1(TEMP_OVR_FLAG1);
        }
    }
    
    /**
     * Return true if 50% or more of the shift crosses midnight.
     * 
     * @param shiftStart
     * @param shiftEnd
     * @param midnight
     * @return
     */
    protected boolean isHalfShiftPastMidnight(Date shiftStart, 
            Date shiftEnd, 
            Date midnight) 
    {    	
    	return (midnight.getTime() - shiftStart.getTime() 
    			<= shiftEnd.getTime() - midnight.getTime()
				);
    }		
    
//	/**
//	 * For shifts created by shift patterns, or overrides that were too difficult to 
//	 * process in the beforeOneEmployeeScheduleOverride event.
//	 * 
//	 * Used for moving a shift to the next day if 50% of the shift crosses midnight.
//	 * 
//	 *  (non-Javadoc)
//	 * @see com.workbrain.app.ta.ruleengine.DataEvent#afterLoad(com.workbrain.app.ta.ruleengine.WBData, com.workbrain.sql.DBConnection)
//	 */
//    public void afterLoad(WBData wbData, DBConnection c)   {
//	//public void beforeManpowerOverrides(WBData wbData, OverrideList ol, DBConnection c) {
//    
//    	StartEndTime shiftStartEnd = null;
//    	Date dayToCalculate = null;
//    	OverrideBuilder ovrBuilder = new OverrideBuilder(c);
//
//    	try {
//	    	// Check for any shift that needs to be moved.
//	    	Date endOfDay = DateHelper.addDays(wbData.getWrksWorkDate(), 1);
//	    	int shiftIndexToMove = -1;
//	    	boolean found = false;
//	    	
//	    	// Find a shift where 50% or more crosses midnight.
//	    	for (int i=1; i <= wbData.getScheduledShiftCount() && !found; i++) {
//	    		
//	    		if (isShiftCrossMidnight(wbData.getScheduleEndTime(i), endOfDay)) {
//	    			
//	    			if (isHalfShiftPastMidnight(wbData.getScheduleStartTime(i),
//	    								wbData.getScheduleEndTime(i), 
//										endOfDay)) {
//
//	    				shiftIndexToMove = i;
//	    				
//	    				// Flag the work summary to indicate that a shift has been moved.
//	    				setWorkSummaryFlag(wbData, c, ovrBuilder);
//	    				
//	    				found = true;
//	    				
//	    			} else {
//	    				// In case a shift was moved, but then a new shift was added that
//	    				// does not get moved, clear the flag.
//	    				clearWorkSummaryFlag(wbData);
//	    				
//	    				found = true;
//	    			}
//	    		}
//	    	}
//	    	
//	    	
//	    	// If there is a shift that needs to be moved.
//	    	if (shiftIndexToMove > 0) {
//	    	
//	    		// Clear the shift
//	    		shiftStartEnd = clearShift(wbData, shiftIndexToMove, c, ovrBuilder);
//	    		
//	    		/* TODO - If there are ScheduleDetail Overrides between the start and end time,
//	    		 * is it possible to move the overrides to the next day?  Can we do this
//	    		 * instead of creating a new shift on the next day.
//	    		 */
//	    		
//	    		// Move the shift to the next day
//	    		createShiftNextDay(wbData, shiftStartEnd, c, ovrBuilder);
//	    		
//	    		/* TODO - attempting to create the shift pattern shift labour.
//	    		 * This is throwing a unique constraint exception on inserting an employee schedule.
//	    		int firstDetailIndex = wbData.getMinWorkDetailIndex(shiftStartEnd.getStartTime(), shiftStartEnd.getEndTime(), null, true, true);
//	    		int lastDetailIndex = wbData.getMaxWorkDetailIndex(shiftStartEnd.getStartTime(), shiftStartEnd.getEndTime(), null, true, true);
//	    		
//	    		for (int i = firstDetailIndex; i <= lastDetailIndex && i >= 0; i++) {
//	    			createShiftNextDay(wbData, wbData.getRuleData().getWorkDetail(i), ovrBuilder);
//	    		}
//	    		*/
//	    		
//	    		// Add the next day to the recalculate list.
//	    		flagDayForCalculate(endOfDay);
//	    		
//	    		// Apply the overrides.
//	    		ovrBuilder.execute(false, false);
//	    	}
//    
//    	} catch (Exception e) {
//    		logger.error(e);
//    		throw new RuntimeException(e.toString());
//    	}
//    }
//
//	
//	/**
//	 * For shifts created through overrides.
//	 * 
//	 * Used for moving a shift to the next day if 50% of the shift crosses midnight.
//	 * 
//	 *  (non-Javadoc)
//	 * @see com.workbrain.app.ta.ruleengine.DataEvent#beforeOneEmployeeScheduleOverride(com.workbrain.app.ta.model.OverrideData, com.workbrain.sql.DBConnection, com.workbrain.app.ta.model.EmployeeData, com.workbrain.app.ta.model.EmployeeScheduleData)
//	 */
//    public void beforeScheduleOverrideInsert(OverrideData od, DBConnection c) throws RuleEngineException {
//    
//		/* TODO -- attempt to move a schedule detail override.
//		 * 
//		 * IMPORTANT: This won't work because a shift will be broken up into multiple schedule detail
//		 * overrides; one for each work detail.  We would need to group these to determine
//		 * the shift start/end times.
//		 */
//    	Date endOfDay = DateHelper.addDays(od.getOvrStartDate(), 1);
//    	StartEndTime shiftStartEnd = null;
//    	
//    	try {
//    		// Get the start/end time.
//	    	if (od.getOvrtypId() == OverrideData.SCHEDULE_DETAIL_TYPE) {
//    			shiftStartEnd = getStartEndFromScheduleDetail(od);
//	    	}
//	    	
//    		if (isShiftCrossMidnight(shiftStartEnd.getEndTime(), endOfDay)) {
//    			
//    			if (isHalfShiftPastMidnight(shiftStartEnd.getStartTime(), 
//    										shiftStartEnd.getEndTime(), 
//											endOfDay)) {
//
//    				// Flag the work summary to indicate that a shift has been moved.
//    				setWorkSummaryFlag(od, c);
//    				
//    				// Move the override to the next day.
//    				od.setOvrStartDate(DateHelper.addDays(od.getOvrStartDate(), 1));
//    				od.setOvrEndDate(DateHelper.addDays(od.getOvrEndDate(), 1));
//    			
//    			} else {
//    				// In case a shift was moved, but then a new shift was added that
//    				// does not get moved, clear the flag.
//    				clearWorkSummaryFlag(od, c);
//    			}
//    		}
//
//    	} catch (Exception e) {
//     		logger.error(e);
//    		throw new RuntimeException(e.toString());
//    	}
//    	
//	}
//
//	/** 
//	 * Used for moving clocks to the next day, for clocks entered through Work Summary overrides.
//	 *   
//	 * Move the clock if a shift from that day has been moved to the next day,
//	 * and if the clock occured after a given time.
//	 * 
//	 *  (non-Javadoc)
//	 * @see com.workbrain.app.ta.ruleengine.DataEvent#afterProcessOneClockAlterWrksId(com.workbrain.app.ta.model.Clock, int, com.workbrain.sql.DBConnection, com.workbrain.app.wbinterface.db.ImportData, int)
//	 */
//    public void beforeWorkSummaryOverrideInsert(OverrideData od, DBConnection conn) throws RuleEngineException {
//
//    	// Use a pre-deterined cutoff time.  If a shift has been moved off this day,
//    	// and a clock occurs after this cutoff time, the clock will also be moved to
//    	// the next day.
//    	Date clockCutoffTimeToday = DateHelper.setTimeValues(od.getOvrStartDate(), clockCutoffTime);
//
//   		WorkSummaryAccess wsAccess = new WorkSummaryAccess(conn);
//    	WorkSummaryData workSummary = null;
//    	Date clockTime = null;
//    	
//    	try {
//    		// Check if a shift has been moved off the current day.
//    		workSummary = wsAccess.loadByEmpIdAndDate(od.getEmpId(), new java.sql.Date(od.getOvrStartDate().getTime()));
//
//            OverrideData.OverrideToken token = od.getNewOverrideByName(WorkSummaryData.WRKS_CLOCKS);
//    		
//    		// Check if the clock is after the pre-determined cutoff-time.
//            
//            // TODO - For now we are just assuming that there is an ON, or ON+OFF but in reality
//            // there could be more clocks.  There could also be other fields in the override such
//            // as authorization so we wouldn't want to move the entire override.
//	    	if ("Y".equals(workSummary.getProperty(MOVED_SHIFT_FLAG)) 
//	    			&& token != null) {
//	    		
//                List ovrClocks = Clock.createClockListFromString(token.getValue());
//                Clock clk = (Clock) ovrClocks.get(0);
//	    		clockTime = clk.getClockDate();
//	    		
//	    		if (clockTime != null && clockTime.after(clockCutoffTimeToday)) {
//		    		od.setOvrStartDate(DateHelper.addDays(od.getOvrStartDate(), 1));
//		    		od.setOvrEndDate(DateHelper.addDays(od.getOvrEndDate(), 1));
//	    		}
//	    	}
//    	
//    	} catch (Exception e) {
//    		logger.error(e);
//    		throw new NestedRuntimeException(e);
//    	}
//    }
//
//    
//	/** 
//	 * Used for moving a clock to the next day.  Fired during clock import processing.
//	 * 
//	 * Move the clock if a shift from that day has been moved to the next day,
//	 * and if the clock occured after a given time.
//	 * 
//	 *  (non-Javadoc)
//	 * @see com.workbrain.app.ta.ruleengine.DataEvent#afterProcessOneClockAlterWrksId(com.workbrain.app.ta.model.Clock, int, com.workbrain.sql.DBConnection, com.workbrain.app.wbinterface.db.ImportData, int)
//	 */
//    public int afterProcessOneClockAlterWrksId(Clock clock , int empId, DBConnection conn , ImportData data, int wrksId){
//
//    	// Use a pre-deterined cutoff time.  If a shift has been moved off this day,
//    	// and a clock occurs after this cutoff time, the clock will also be moved to
//    	// the next day.
//    	Date clockCutoffTimeToday = DateHelper.setTimeValues(clock.getClockDate(), clockCutoffTime);
//    	
//    	int actualWrksId = wrksId;
//   		WorkSummaryAccess wsAccess = new WorkSummaryAccess(conn);
//    	WorkSummaryData workSummary = null;
//    	
//    	try {
//    		// Check if a shift has been moved off the current day.
//    		workSummary = wsAccess.loadByWrksId(wrksId);
//
//    		// Check if the clock is after the pre-determined cutoff-time.
//	    	if ("Y".equals(workSummary.getProperty(MOVED_SHIFT_FLAG)) 
//	    			&& clock.getClockDate().after(clockCutoffTimeToday)) {
//	    		
//	    		// Get the wrksId for the next day.
//	    		workSummary = wsAccess.loadByEmpIdAndDate(
//	    									empId, 
//	    									new java.sql.Date(DateHelper.addDays(workSummary.getWrksWorkDate(), 1).getTime())
//											);
//
//	    		// There should be a work summary because we've moved
//	    		// a shift to that date.
//	    		if (workSummary != null) {
//	    			actualWrksId = workSummary.getWrksId();
//	    		} else {
//	    			// TODO - Verify that a work summary will always exist.
//	    		}
//	    	}
//	    	
//    	} catch (Exception e) {
//    		logger.error(e);
//    		throw new NestedRuntimeException(e);
//    	}
//    	
//    	return actualWrksId;
//    }
//            
//    /**
//     * Returns true if a shift crosses midnight.
//     * 
//     * @param shiftEnd
//     * @param midnight
//     * @return
//     */
//    protected boolean isShiftCrossMidnight(Date shiftEnd, Date midnight) {
//    	return shiftEnd.after(midnight);
//    }
//    
//    /**
//     * Set a work summary flag to indicate that there was a shift crossing
//     * midnight that got moved to the next day.
//     * 
//     * Need to create an override to do this otherwise it will get cleared
//     * in the next recalc.
//     * 
//     * @param wbData
//     * @param c
//     * @param ovrBuilder
//     */
//    protected void setWorkSummaryFlag(WBData wbData, DBConnection c, OverrideBuilder ovrBuilder) {
//    	
//		InsertWorkSummaryOverride wrksOvr = new InsertWorkSummaryOverride(c);
//    	
//		wrksOvr.setEmpId(wbData.getEmpId());
//    	wrksOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
//    	wrksOvr.setStartDate(wbData.getWrksWorkDate());
//    	wrksOvr.setEndDate(wbData.getWrksWorkDate());
//    	wrksOvr.setWrksFlag1("Y");
//    	
//    	ovrBuilder.add(wrksOvr);
//    }
//    
//    protected void setWorkSummaryFlag(OverrideData od, DBConnection c) throws Exception {
//    	
//    	OverrideBuilder ovrBuilder = new OverrideBuilder(c);
//		InsertWorkSummaryOverride wrksOvr = new InsertWorkSummaryOverride(c);
//    	
//		wrksOvr.setEmpId(od.getEmpId());
//    	wrksOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
//    	wrksOvr.setStartDate(od.getOvrStartDate());
//    	wrksOvr.setEndDate(od.getOvrStartDate());
//    	wrksOvr.setWrksFlag1("Y");
//    	
//    	ovrBuilder.add(wrksOvr);
//    	ovrBuilder.execute(false, false);
//    }
//    
//
//    /**
//     * Clears the flag that indicated that a shift was moved.
//     * 
//     * @param wbData
//     */
//    protected void clearWorkSummaryFlag(WBData wbData) {
//
//		OverrideList olList = 
//					wbData.getRuleData().getCalcDataCache().getTempOverrides(
//			                wbData.getRuleData().getWorkSummary().getEmpId(),
//			                wbData.getRuleData().getWorkSummary().getWrksWorkDate()
//			                );
//		
//		if (olList != null) {
//			olList = olList.filter(null, null, 
//									new String[] {OverrideData.APPLIED}, 
//									OverrideData.WORK_SUMMARY_TYPE_START, 
//									OverrideData.WORK_SUMMARY_TYPE_END);
//		
//			if (olList.size() > 0) {
//				OverrideData od = (OverrideData) olList.get(0);
//				if (od.newTokenExists(MOVED_SHIFT_FLAG_DB)) {
//					od.setOvrStatusCancel("Cancelled by DataEventMoveShift.");
//				}
//			}
//		}
//    }
//    
//    protected void clearWorkSummaryFlag(OverrideData od, DBConnection c) throws Exception {
//    	
//    	OverrideBuilder ovrBuilder = new OverrideBuilder(c);
//		InsertWorkSummaryOverride wrksOvr = new InsertWorkSummaryOverride(c);
//    	
//		wrksOvr.setEmpId(od.getEmpId());
//    	wrksOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
//    	wrksOvr.setStartDate(od.getOvrStartDate());
//    	wrksOvr.setEndDate(od.getOvrStartDate());
//    	wrksOvr.setWrksFlag1("N");
//    	
//    	ovrBuilder.add(wrksOvr);
//    	ovrBuilder.execute(false, false);
//    }
//    
//    /**
//     * Cancels the shift given by shiftIndex.
//     * 
//     * This is done by creating an EmployeeScheduleOverride for the OFF shift.
//     * 
//     * @param wbData
//     * @param shiftIndex
//     * @param c
//     * @return
//     * @throws OverrideException
//     */
//    protected StartEndTime clearShift(WBData wbData, int shiftIndex, DBConnection c, OverrideBuilder ovrBuilder) throws OverrideException {
//    	
//    	StartEndTime shiftStartEnd = new StartEndTime();
//		shiftStartEnd.setStartTime(wbData.getScheduleStartTime(shiftIndex));
//		shiftStartEnd.setEndTime(wbData.getScheduleEndTime(shiftIndex));
//		
//    	InsertEmployeeScheduleOverride schedOvr = new InsertEmployeeScheduleOverride(c);
//    	
//    	switch (shiftIndex) {
//    		case 1:
//    			schedOvr.setEmpskdActShiftId(0);
//    			break;
//    		case 2:
//    			schedOvr.setEmpskdActShiftId2(0);
//    			break;
//    		case 3:
//    			schedOvr.setEmpskdActShiftId3(0);
//    			break;
//    		case 4:
//    			schedOvr.setEmpskdActShiftId4(0);
//    			break;
//    		case 5:
//    			schedOvr.setEmpskdActShiftId5(0);
//    			break;
//    	}
//
//    	schedOvr.setEmpId(wbData.getEmpId());
//    	schedOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
//    	schedOvr.setOvrType(OverrideData.SCHEDULE_SHIFT_TYPE);
//    	schedOvr.setStartDate(wbData.getWrksWorkDate());
//    	schedOvr.setEndDate(wbData.getWrksWorkDate());
//    	
//    	ovrBuilder.add(schedOvr);
//    	
//    	return shiftStartEnd;
//    }
//    
//    
//    /**
//     * Given a Start and End time, create a shift on the next day.
//     * 
//     * @param wbData
//     * @param shiftStartEnd
//     * @param c
//     * @throws OverrideException
//     * @throws SQLException
//     */
//    private void createShiftNextDay(WBData wbData, StartEndTime shiftStartEnd, DBConnection c, OverrideBuilder ovrBuilder) throws OverrideException, SQLException {
//    	
//    	InsertEmployeeScheduleOverride schedOvr = new InsertEmployeeScheduleOverride(c);
//    	
//    	Date nextDay = DateHelper.addDays(wbData.getWrksWorkDate(), 1);
//
//    	schedOvr.setEmpId(wbData.getEmpId());
//    	schedOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
//    	schedOvr.setStartDate(nextDay);
//    	schedOvr.setEndDate(nextDay);
//
//    	schedOvr.setOvrType(OverrideData.SCHEDULE_DETAIL_TYPE);
//    	schedOvr.setEmpskdActStartTime(shiftStartEnd.getStartTime());
//    	schedOvr.setEmpskdActEndTime(shiftStartEnd.getEndTime());
//    	schedOvr.setStartTime(shiftStartEnd.getStartTime());
//    	schedOvr.setEndTime(shiftStartEnd.getEndTime());
//    	
//    	ovrBuilder.add(schedOvr);
//    }
//    
//    
//    /**
//     * Given a Start and End time, create a shift on the next day.
//     * 
//     * @param wbData
//     * @param shiftStartEnd
//     * @param c
//     * @throws OverrideException
//     * @throws SQLException
//     */
//    private void createShiftNextDay(WBData wbData, WorkDetailData workDetail, OverrideBuilder ovrBuilder) throws OverrideException, SQLException {
//    	
//    	InsertScheduleDetailOverride schedOvr = new InsertScheduleDetailOverride();
//    	
//    	Date nextDay = DateHelper.addDays(wbData.getWrksWorkDate(), 1);
//
//    	workDetail.setWrkdWorkDate(nextDay);
//    	schedOvr.setWorkDetailData(workDetail);
//
//    	schedOvr.setEmpId(wbData.getEmpId());
//    	schedOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
//    	schedOvr.setStartDate(nextDay);
//    	schedOvr.setEndDate(nextDay);
//
//    	
//    	/*
//    	schedOvr.setOvrType(OverrideData.SCHEDULE_DETAIL_TYPE);
//    	schedOvr.setEmpskdActStartTime(shiftStartEnd.getStartTime());
//    	schedOvr.setEmpskdActEndTime(shiftStartEnd.getEndTime());
//    	schedOvr.setStartTime(shiftStartEnd.getStartTime());
//    	schedOvr.setEndTime(shiftStartEnd.getEndTime());
//    	*/
//    	
//    	ovrBuilder.add(schedOvr);
//    }
//    
//
//    /*
//     * Get the ScheduleDetail start and end times from the override.
//     * 
//     * @param od
//     * @return
//     */
//    private StartEndTime getStartEndFromScheduleDetail(OverrideData od) {
//    	
//    	StartEndTime detailStartEndTime = new StartEndTime();
//    	OverrideData.OverrideToken token = null;
//    	
//    	// Get the start time.
//    	token = od.getNewOverrideByName(EmployeeScheduleData.EMPSKD_ACT_START_TIME);
//    	if (token != null) {
//    		detailStartEndTime.setStartTime(token.getValueDate());
//    	}
//    	
//    	// Get the end time.
//    	token = od.getNewOverrideByName(EmployeeScheduleData.EMPSKD_ACT_END_TIME);
//    	if (token != null) {
//    		detailStartEndTime.setEndTime(token.getValueDate());
//    	}
//    	
//    	return detailStartEndTime;
//    }
//    
//    
//    private void flagDayForCalculate(Date calcDate) {
//    	// TODO - If we move a shift to the first day of the following
//    	// week, we will need to recalc that day.  Implement this
//    	// if the Proof of Concept succeeds.
//    }
}
