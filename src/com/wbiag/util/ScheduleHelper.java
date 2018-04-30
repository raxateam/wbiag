package com.wbiag.util;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.wbiag.app.ta.model.StartEndTime;
import com.workbrain.app.ta.model.EmployeeScheduleData;
import com.workbrain.util.DateHelper;

/**
 * @author bviveiros
 *
 */
public class ScheduleHelper {

	/**
	 * Returns a list of StartEndTime that represent each Actual shift sorted 
	 * chronologically by start time.  If there are no shifts, returns null.
	 * 
	 * The list is a  Map with keys of java.util.Date with a start time, and the values
	 * are com.wbiag.StartEndTime.
	 * 
	 * @param scheduleData
	 * @return
	 */
    public static Map getShiftStartEndTimesSorted(EmployeeScheduleData scheduleData) {
    	
    	Map timesList = null;
    	StartEndTime shiftTime = null;
    	
    	if (scheduleData.retrieveNumberOfScheduledShiftIndexes() > 0) {
    		
    		// Use a TreeMap since it is sorted by key.
    		timesList = new TreeMap();
    		
    		// Check all 5 shift slots.
    		for (int i=1; i <= 5; i++) {
    			
    			// If it is not an off shift.
    			if (scheduleData.getEmpskdActStartTime(i) != null &&
    					!scheduleData.getEmpskdActStartTime(i).equals(
    								scheduleData.getEmpskdActEndTime(i))) {
		    		
    				shiftTime = new StartEndTime(scheduleData.getEmpskdActStartTime(i),
		    										scheduleData.getEmpskdActEndTime(i));

		    		timesList.put(shiftTime.getStartTime(), shiftTime);
    			}
    		}			
    	}
    	
    	return timesList;
    }

    /**
     * Returns a list of StartEndTime that represent the time from minStartTime to
     * the first shift start time, any gaps between shifts, and the time from the
     * last shift end time to maxEndTime sorted chronologically by start time.
     * 
     * If no such segments exist, returns null.
     * 
	 * The list is a Map with keys of java.util.Date with a start time, and the values
	 * are com.wbiag.StartEndTime.
	 * 
     * @param scheduleData
     * @param minStartTime
     * @param maxEndTime
     * @return
     */
    public static Map getNonShiftStartEndTimesSorted(EmployeeScheduleData scheduleData, Date minStartTime, Date maxEndTime) {
    	
    	Map shiftTimesSorted = getShiftStartEndTimesSorted(scheduleData);
    	Map nonShiftTimes = null;
    	Iterator i = null;
        StartEndTime shiftStartEnd = null;
        StartEndTime nonShiftStartEnd = null;
        Date rangeStart = null;
        Date rangeEnd = null;
    	
        if (shiftTimesSorted != null) {
        	
    		i = shiftTimesSorted.values().iterator();
    		
    		if (i.hasNext()) {
    			
    			// Use a TreeMap since it is sorted by key.
    			nonShiftTimes = new TreeMap();
    			
    			// Process from minStartTime to first shift start.
    			shiftStartEnd = (StartEndTime) i.next();
    			
    			rangeStart = minStartTime;
    			rangeEnd = shiftStartEnd.getStartTime();
    		
    			if (DateHelper.compare(rangeStart, rangeEnd) < 0) {
    				nonShiftStartEnd = new StartEndTime(rangeStart, rangeEnd);
    				nonShiftTimes.put(rangeStart, nonShiftStartEnd);
    			}
    			
    			// Process any gaps between shifts.
    			rangeStart = shiftStartEnd.getEndTime();
	    		while (i.hasNext()) {
	    			
	    			shiftStartEnd = (StartEndTime) i.next();
	    			
	    			rangeEnd = shiftStartEnd.getStartTime();
	    			
	    			if (DateHelper.compare(rangeStart, rangeEnd) < 0) {
	    				nonShiftStartEnd = new StartEndTime(rangeStart, rangeEnd);
	    				nonShiftTimes.put(rangeStart, nonShiftStartEnd);
	    			}
	    			
		            rangeStart = shiftStartEnd.getEndTime();
	    		}
	    		
	    		// Process from last shift end time to maxEndTime
	    		rangeEnd = maxEndTime;
    			if (DateHelper.compare(rangeStart, rangeEnd) < 0) {
    				nonShiftStartEnd = new StartEndTime(rangeStart, rangeEnd);
    				nonShiftTimes.put(rangeStart, nonShiftStartEnd);
    			}
	    		
    		}
        }
    	
    	return nonShiftTimes;
    }
	
}
