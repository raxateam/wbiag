package com.csl.app.modules.retailSchedule;

import java.util.*;
import com.workbrain.util.DateHelper;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCallout;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;
import com.workbrain.app.modules.retailSchedule.model.IntervalRequirementsManager;
import com.workbrain.app.modules.retailSchedule.model.Activity;
import com.workbrain.app.modules.retailSchedule.model.IntervalRequirementsManager.Record;
import com.workbrain.app.modules.retailSchedule.model.PositionSet;
import com.workbrain.app.modules.retailSchedule.model.Schedule;
import com.workbrain.app.modules.retailSchedule.model.ShiftDetail;
import com.workbrain.app.modules.retailSchedule.model.ShiftActivity;
import com.workbrain.app.modules.retailSchedule.db.PositionSetAccess;
import com.workbrain.app.modules.retailSchedule.exceptions.*;


/**
 * This callout class represents the entry point for the LFSO Meal Break Coverage logic.
 * Specifically, the populateSchedulePostLoop() method.
 * 
 * Usage: 
 *   a) if the application has no other Schedule Callout, then edit WB Registry to change the value
 *      of SO_CUST_CALLOUT_CLASS (system > modules > scheduleOptimization) default value 
 *      from com.workbrain.app.modules.retailSchedule.services.ScheduleCallout 
 *      to com.csl.app.modules.retailSchedule.ScheduleCalloutExt
 *      
 *   b) if the application is using its own callout, then create a new wrapper class 
 *      that extend the ScheduleCallout and trigger all callouts from there 
 *      (e.g. see com.csl.app.modules.retailSchedule.ScheduleCalloutExt)   
 * 
 * Created by Raj Krishnan October 2008
 * Modified by Quoc Vuong and Rakesh Moddi to Incorporate into Common Solution Library March 2009
 * 
 * <p>Copyright: Copyright (c) 2009 Infor Global Solutions.</p>
 * 
 */


public class BreakCoverageCallout extends ScheduleCallout {

	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BreakCoverageCallout.class);
    
	public BreakCoverageCallout() {}

	public void populateSchedulePostLoop(SOData soContext) throws CalloutException  {
        
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {  
            logger.debug( "BreakCoverageCallout.populateSchedulePostLoop Start" );
        }

		// This SortedMap contains the SchedGroupMealBreakCoverageMgr objects for 
		// each activity date/schedule group(dept)/job (set) id combination.		
		SortedMap transitionMgrMap = new TreeMap();
		
		// Vector of ShiftActivity objects
		Vector activityList = new Vector();
		
		// Get departments attached to this schedule.
		try {
			Schedule sched = soContext.getSchedule();
			
			// Get interval requirements list : vector of Record objects.
			Vector intervalRequirementsList = (Vector) sched.getIntervalRequirements().getRequirementsList();
			
			DBConnection dbc = soContext.getDBconnection();
			
			Date schedule_from_date = sched.getSkdFromDate().getTime();
			Date schedule_to_date = sched.getSkdToDate().getTime();
			
			// In this first while loop, we populate a Vector of ShiftActvity objects 
            // from a vector of ShiftDetails. While doing so, we also change the 
			// start and end times for both the ShiftDetail and its ShiftActivity
			// objects so that the dates point to the actual date the activity 
			// is occurring, and not a default Jan 1, 3999 date.
            //
			// Also if a shift is crossing overnight, adjust the end time by adding a day 
            // to ensure that end time is always after start time.
			
			Iterator it = sched.getShiftDetailList().iterator();
			while (it.hasNext()) {
                
				ShiftDetail dt = (ShiftDetail) it.next();
                
				Date properStartTime = DateHelper.setTimeValues(dt.getShftDate(), dt.getShftdetStartTime());
				Date properEndTime = DateHelper.setTimeValues(dt.getShftDate(), dt.getShftdetEndTime());
				
				// Adjust end time if crossing midnight 
				if (properStartTime.after(properEndTime)) {
					properEndTime = DateHelper.addDays(properEndTime, 1);
				}
                
				// Populate the ShiftDetail start and end times with the proper Start and End times.
				dt.setShftdetStartTime(properStartTime);				
				dt.setShftdetEndTime(properEndTime);
				
				Vector shiftActivityList = dt.getShiftActivityList();
				ShiftActivity shiftActivity;
                
				for (int jCount=0; jCount < shiftActivityList.size(); jCount++) {
                    
					shiftActivity =(ShiftActivity)shiftActivityList.get(jCount);

					Date properActStartTime = DateHelper.setTimeValues(shiftActivity.getShftactDate(), shiftActivity.getShftactStartTime());
					Date properActEndTime = DateHelper.setTimeValues(shiftActivity.getShftactDate(), shiftActivity.getShftactEndTime());
                    
                    // Adjust end time if crossing midnight
                    if (properActStartTime.after(properActEndTime)) {
						properActEndTime = DateHelper.addDays(properActEndTime, 1);
					}
                    
					// If Activity start time is before the Shift start time,
					// that means that the shift crossed the midnight boundary
					// and that the activity start time is realy the DAY AFTER
					// and that the activity start time should really be a day afterwards.
					else if (properActStartTime.before(properStartTime )) {
						properActStartTime = DateHelper.addDays(properActStartTime, 1);
                        
						// Similarly, if the Activity end time is before the Shift start time,
						// then that means that the shift crossed the midnight boundary
						// and that the activity end time is actually a day AFTER						
						if (properActEndTime.before(properStartTime )) {
							properActEndTime = DateHelper.addDays(properActEndTime, 1);
						}
					}
                    
					// Populate the ShiftActivity start and end times with the proper Start and end times.
					shiftActivity.setShftactStartTime(properActStartTime);					
					shiftActivity.setShftactEndTime(properActEndTime);
					
					// Pointer from shiftActivity back to the containing shiftDetail 
                    // and add to activity list 
					shiftActivity.setShiftDetail(dt);
					activityList.add(shiftActivity);
				}
			} // end while
			
			// In this section of code, we go through the activity list 
			// created in the previous for loop and create a SchedGroupMealBreakCoverageMgr
			// object for each unique activity date-job id-schedulegroup (location) id combination.
			// However for the job id, we don't actually use the Shift Detail's 
			// job id, but rather the job id corresponding to the position set
			// that the job belongs to.
			
			Iterator it1 = activityList.iterator();
			while (it1.hasNext()) {
                
				SchedGroupMealBreakCoverageMgr sched_skill = null;
	            ShiftActivity sa = (ShiftActivity) it1.next();
                
	            // Vector of IntervalRequirementsManager.Record objects for each meal coverage mgr.
	            Vector intervalRequirements = new Vector(); 
	            
	            int skdgrp_id = sa.getSkdgrpId().intValue();
	            int child_job_id = sa.getShiftDetail().getJobId();
                
	            // Get parent job id
	    		PositionSetAccess posAccess = new PositionSetAccess(dbc);
	    		Integer parent_job_id = null;
                
	    		ArrayList parentJobList = (ArrayList) posAccess.loadPositionSetByJobChildId(child_job_id);	    		
	    		for (int i=0; i< parentJobList.size(); i++) {
	    			PositionSet pos = (PositionSet) parentJobList.get(i);
	    			parent_job_id = pos.getJobId();	    			
	    		}
                
	    		// If we find that the job id doesn't have a parent job set id,
	    		// then just use the initial shift detail job id
	    		if (parent_job_id == null) {
	    			parent_job_id = new Integer(child_job_id);
	    		}
                
	            Date activityDate = DateHelper.truncateToDays(sa.getShftactDate());
	            
	            String key = new String(activityDate.toString() + " " + skdgrp_id + " " + parent_job_id );
	            
	            if (transitionMgrMap.containsKey(key) == false) {
	            	sched_skill = new SchedGroupMealBreakCoverageMgr(dbc, skdgrp_id, parent_job_id.intValue(), sched, activityDate);
	            }
	            else {
	            	sched_skill = (SchedGroupMealBreakCoverageMgr) transitionMgrMap.get(key);
	            }
                
	            int act_id=0;
                
	            // If the ShiftActivity is a break activity, then find the work acitvity 
                // for which it is a break so that we can get the proper activity id.
	            // ie. all breaks have act id of 10004 but we need the work activity ID 
                // in order to get the interval requirements corresponding to this break.                
	            if (sa.isBreak()) {
	            	Activity activity = sched_skill.getCorrespondingWorkActivity(sa);
	            	act_id=activity.getID().intValue();
	            }
	            else {
	            	act_id=sa.getActId();
	            }

	            // Find interval requirements corresponding to this job set id, skdgrp_id and activity,
                // and add to list
	            for (int j=0; j < intervalRequirementsList.size(); j++) {
	            	Record o = (Record) intervalRequirementsList.get(j);
	            	if ((o.m_skdgrp.intValue()==skdgrp_id) &&
	            			(parent_job_id.intValue()==o.job.intValue()) && (act_id==o.m_Activity.intValue() )) {
	            		intervalRequirements.add(o);
	            	}                	            	
	            } // end for
                
	            // Create ActivityIntervalRequirements object corresponding to this 
                // activity with its interval requirements.	            
	            ActivityIntervalRequirements air = new ActivityIntervalRequirements(schedule_from_date, schedule_to_date, sa, intervalRequirements );
                
	            // Using the location set up, find the times that this activity can have break times
                // and insert in the map.
	            air.setAllowableBreakTimes();	            
	            sched_skill.addShiftActivity(air, false);                
	            transitionMgrMap.put(key, sched_skill);
	           
	        }			
			
			// In this next for loop, we find uncovered lunch breaks 
			// for each SchedGroupMealBreakCoverageMgr object.
			// we try to move around each of the lunch breaks in order
			// to try and get the break to be covered.
			
			Iterator it3 = transitionMgrMap.values().iterator();
			while (it3.hasNext()) {
				SchedGroupMealBreakCoverageMgr mgr = (SchedGroupMealBreakCoverageMgr) it3.next();
	            
				ActivityIntervalRequirements ucAir =mgr.findUnCoveredLunchBreak();
				while (ucAir !=null) { 
					
					if (mgr.getEmployeeList().size() > 1) {
                        
	           		    // If not covered, try to move around breaks an effort to cover this break.
						if (mgr.findAvailableBreakTimeSlot(ucAir)==false) {
							mgr.addUnCoveredBreakToList(ucAir);
						}
					}
                    
					// If employee list size is only one, there is no way that this break 
                    // can be covered by moving breaks, and all activities become unCovered.
					else {
						mgr.addUnCoveredBreakToList(ucAir);						
					}
					ucAir = mgr.findUnCoveredLunchBreak();
	            	
				} // end inner while
			} // end while
			
			// In this loop, we go through each of the SchedGroupMealBreakCoverageMgr
			// objects and attempt to look at OTHER departments to find employees
			// to fill in for the uncovered breaks.
			
			Iterator it4 = transitionMgrMap.values().iterator();
			while (it4.hasNext()) {
				SchedGroupMealBreakCoverageMgr mgr = (SchedGroupMealBreakCoverageMgr) it4.next();
                
				// if there are uncovered breaks to take care of
				if (mgr.isUnCoveredBreakListEmpty()==false) {					
					ActivityIntervalRequirements unCoveredAir = mgr.getNextUnCoveredBreakfromList();
					while (unCoveredAir != null) {					
	            		
	            		// Find subset of SchedGroupMealBreakCoverageMgr in the 
						// transitionMgrMap that have the same actvity date
						// as the mgr object we're looking at.
						// We cannot use other activity dates to cover
						
	            		Date begDate = mgr.getActivityDate();
	            		Date endDate = DateHelper.addDays(begDate, 1);
	            		SortedMap tm = transitionMgrMap.subMap(begDate.toString(), endDate.toString());
	            		Iterator it5 = tm.values().iterator();
                        
	            		// In this loop we go through other departments from the same
	            		// activity date and try to look for employees that are appropriately
	            		// skilled to cover for the unCovered Break.
	            		while (it5.hasNext()) {
	            			SchedGroupMealBreakCoverageMgr mgr2 = (SchedGroupMealBreakCoverageMgr) it5.next();
                            
	            			// Don't look at the same dept-job-date combination!	            			
	            			// We only want same dept-job combination for the same date.
	            			if ((mgr.getUniqueID().equals(mgr2.getUniqueID())||(mgr.getActivityDate().equals(mgr2.getActivityDate())==false))) {
	            				continue;
	            			}
	            			boolean result =mgr2.findMatchingEmployeeShiftDetail(unCoveredAir);
	            			if (result == true) {
	            				break;
	            			}
	            			
	            		} // end while it5.hasNext()
	            		
	            		unCoveredAir = mgr.getNextUnCoveredBreakfromList();
					} // end while
					
				} // end if
			} // end while
			
            // If break is still not covered after trying to staff from all departments, then 
            // remove break activit, triggering a schedule warning and let it be take care of manually.
			Iterator it7 = transitionMgrMap.values().iterator();
			while (it7.hasNext()) {
				SchedGroupMealBreakCoverageMgr mgr = (SchedGroupMealBreakCoverageMgr) it7.next();
				mgr.removeUnCoveredShiftActivities();
			}			
		}
        
        catch (RetailException re) {
            throw new CalloutException( re.getMessage() );            
        } 
        
        finally {
            try {
                resetShiftDate( soContext );
            }
            catch (RetailException re) {
                logger.error( "BreakCoverageCallout: Error while setting shifts date to 01/01/3000." );                                
            }
            
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {  
                logger.debug( "BreakCoverageCallout.populateSchedulePostLoop End" );
            }
        }

	}		

    
    /**
     * Set the shift and activity start and end times to have a date of Jan 1, 3999.
     * This change is to avoid any conflict with any possible core code that may 
     * depend on this being true.
     * 
     */
    private void resetShiftDate( SOData soContext ) throws RetailException {
        Schedule sched = soContext.getSchedule();
        Iterator it = sched.getShiftDetailList().iterator();           
        while (it.hasNext()) {
            ShiftDetail dt = (ShiftDetail) it.next();
            Calendar tempCal = Calendar.getInstance();
            
            tempCal.setTime(dt.getShftdetStartTime());
            tempCal.set(3999, 01, 01);
            dt.setShftdetStartTime((Date) tempCal.getTime().clone());
            
            tempCal.setTime(dt.getShftdetEndTime());
            tempCal.set(3999, 01, 01);
            dt.setShftdetEndTime((Date) tempCal.getTime().clone());
            
            Vector shiftActivityList = dt.getShiftActivityList();
            ShiftActivity shiftActivity ; 
            for (int jCount=0; jCount < shiftActivityList.size(); jCount++) {
                shiftActivity =(ShiftActivity)shiftActivityList.get(jCount);
                
                tempCal.setTime(shiftActivity.getShftactStartTime());
                tempCal.set(3999, 01,01);
                shiftActivity.setShftactStartTime((Date) tempCal.getTime().clone());
                
                tempCal.setTime(shiftActivity.getShftactEndTime());
                tempCal.set(3999,01,01);
                shiftActivity.setShftactEndTime((Date) tempCal.getTime().clone());                  
            }
        }
    }
    
}
