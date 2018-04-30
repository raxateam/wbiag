package com.csl.app.modules.retailSchedule;

import java.util.*;

import com.workbrain.util.DateHelper;
import com.workbrain.app.modules.retailSchedule.utils.ShiftActivityUtil;
import com.workbrain.app.modules.retailSchedule.model.Activity;
import com.workbrain.app.modules.retailSchedule.model.ActivityRule;
import com.workbrain.app.modules.retailSchedule.model.Employee;
import com.workbrain.app.modules.retailSchedule.model.IntervalRequirementsManager;
import com.workbrain.app.modules.retailSchedule.model.ShiftActivity;
import com.workbrain.app.modules.retailSchedule.model.IntervalRequirementsManager.Record;
import com.workbrain.app.modules.retailSchedule.model.Schedule;
import com.workbrain.app.modules.retailSchedule.model.PositionSet;
import com.workbrain.app.modules.retailSchedule.model.ShiftDetail;
import com.workbrain.app.modules.retailSchedule.db.*;
import com.workbrain.app.modules.retailSchedule.exceptions.*;

import com.workbrain.app.ta.model.EmployeeJobData;
import com.workbrain.app.modules.retailSchedule.model.EmployeeSkill;

import com.workbrain.server.registry.Registry;
import com.workbrain.sql.DBConnection;

/**
 *  This class takes care of covering meal breaks for a specific
 *  Activity Date - Department - Job (Set) ID combination.
 *  
 *  Note: SchedGroupMealBreakCoverageMgr is often abbreviated to "mgr" in technical docs. 
 * Modified by Quoc Vuong and Rakesh Moddi to Incorporate into Common Solution Library March 2009
 *
 */

public class SchedGroupMealBreakCoverageMgr {
	 
	 int skdgrp_id;
	 int job_id;
	 Date activityDate = null;
	 
	 Schedule schedul = null;
     
	 // List of ShiftDetail for this mgr
	 List shiftDetailList = null;
	 
	 // ActivityIntervalRequirements List for the purpose of storing all Break activities 
     // (via ShiftActivitiy object contained in ActivityIntervalRequirements)
	 Vector breakActivityList = null;
     
	 // ActivityIntervalRequirements List for the purpose of storing all Work activities
     // (via ShiftActivitiy object contained in ActivityIntervalRequirements)
	 Vector workActivityList = null;
	 
	 // Stack contains Uncovered Break Activity objects
     //	(via ShiftActivitiy object contained in ActivityIntervalRequirements)	 
	 Stack unCoverdBkActivityMasterList = null;
	 
     // Contains list of employees in this mgr.
	 Set employee_list = null; 
     
	 DBConnection dbConnection = null;
     

	public SchedGroupMealBreakCoverageMgr( DBConnection dbc, 
                                           int s_id, int j_id, 
                                           Schedule sched, Date act_date ) throws RetailException {
		schedul = sched;
		skdgrp_id = s_id;
		job_id = j_id;
		dbConnection = dbc;
		activityDate = DateHelper.truncateToDays(act_date);
		breakActivityList = new Vector();
		workActivityList = new Vector();
		unCoverdBkActivityMasterList = new Stack();
		employee_list = new HashSet();
		shiftDetailList = new Vector();
		
	}
    
    
    /**
     * Concatenation of activity date,skdgrp_id and job_id to uniquely recognize dept-job combination
     */   
	public String getUniqueID() {
		return activityDate.toString() + " " + String.valueOf(skdgrp_id) + " " + String.valueOf(job_id) ;
	}
    
	public Date getActivityDate() {
		return activityDate;
	}
		
	public int getSkdGrpId() {
		return skdgrp_id;
	}
	
    public Set getEmployeeList() {
        return employee_list;
    }
    
    /**
     * Go through the breakActivityList to find those that have "isCovered" == false
     * and remove the activity from its containing shift detail.
     * 
     * This is used as a last resort in the event the break cannot be covered.
     */
	public void removeUnCoveredShiftActivities() throws RetailException {
		ActivityIntervalRequirements unCoveredAir;
        
		for (int iCount=0; iCount<breakActivityList.size(); iCount++) {
			unCoveredAir = (ActivityIntervalRequirements)breakActivityList.get(iCount);
			ShiftActivity unCoveredBreakActivity = unCoveredAir.sa;
    		ShiftDetail sd = unCoveredBreakActivity.getShiftDetail();
    		
    		if (unCoveredAir.isCovered==false) {
    			sd.removeShiftActivity(unCoveredBreakActivity);
    		}    		
		}	
	}

	/**
     * Adds break activity object to running stack of uncovered breaks.
	 */
	public void addUnCoveredBreakToList(ActivityIntervalRequirements obj) {
		obj.isCovered = false;
		unCoverdBkActivityMasterList.push(obj);
	}
	
    /**
     * Are all breaks covered for this mgr? If yes, will return true.
     */
	public boolean isUnCoveredBreakListEmpty() {
		return unCoverdBkActivityMasterList.empty();
	}
    
    /**
     * Returns the next uncovered break from the stack. 
     * If there are none left, return null
     */
	public ActivityIntervalRequirements getNextUnCoveredBreakfromList() {
		ActivityIntervalRequirements air = null;
		try {
			air = (ActivityIntervalRequirements) unCoverdBkActivityMasterList.pop();
			return air;
		}
		catch (EmptyStackException e) {
			return null;
		}
	}
    
    /**
     * Adds a ShiftActivity contains in an ActivityIntervalRequirements(AIR) wrapper
     * into the shiftDetailList, breakActivityList and workActvityList.
     * 
     * Also populates the employee list.
     * 
     * The variable isCoveredBreak is used in the event that a new activity is created 
     * specifically to cover someone else's break. In this case, we would like to 
     * have the shift Activity AIR added to the break Activity list because a covered 
     * break is effectively like a "break" for the employee that normally does some 
     * other kind of work.
     * So normally, isCoveredBreak is false, but in the even that we're adding
     * a covering shift activity, it's equal to true. 
     */
	public void addShiftActivity(ActivityIntervalRequirements oAir, boolean isCoveredBreak) 
            throws RetailException {	
		ShiftActivity oActivity = oAir.sa;
		Employee emp = Employee.get(oActivity.getShiftDetail().getEmpId());
		employee_list.add(emp);
					
		Activity activity = oActivity.getActivity();
		// If a break activiy, add to break activity list.
		if (activity.getActWorking().intValue()==0) { 
			breakActivityList.add(oAir);
		}
		else {
			if (isCoveredBreak == true) {
				breakActivityList.add(oAir);
			}				
			workActivityList.add(oAir);				
		}
        
		if (shiftDetailList.contains(oActivity.getShiftDetail()) == false) {
			shiftDetailList.add(oActivity.getShiftDetail());
		}				
     }
	    
	/**
	 * This method is used to go through the breakActivityList and find the 
     * first unCovered lunch break.
	 */
	public ActivityIntervalRequirements findUnCoveredLunchBreak() throws RetailException {

		ActivityIntervalRequirements air;
        
		for (int iCount=0; iCount<breakActivityList.size(); iCount++) {
			air = (ActivityIntervalRequirements)breakActivityList.get(iCount);
			if (unCoverdBkActivityMasterList.contains(air)) {
				continue;
			}
            
			// Check to see if this break is covered by others in the same mgr.
			boolean result = checkMealBreakCoverage(air);
			air.isCovered=result;
			if (result == false) {
				return air;
			}
        } 
        
		// If code reaches here, we know there are no uncovered breaks.		
		return null;
	}  

    /**
     * For a given AIR object airToCheck (consisting of a break ShiftActivity), 
     * will look at other shiftDetails' break activities in this mgr and see if 
     * this particular break activity is covered.
     * What "covered" means is that there is at least X-1 other employees who are 
     * on shift at the same time as airToCheck break times, but do not have a break
     * at the exact same time as the employee referred to in airToCheck, where
     * X represents the interval requirement for that particular activity-job combination
     */	 
	public boolean checkMealBreakCoverage (ActivityIntervalRequirements airToCheck) 
	       throws RetailException {
        
		ShiftDetail sd = null;
		
		int minRequired = airToCheck.getMinRequired();
		int numCovering = 0;
		ShiftActivity unCoveredBreakActivity = airToCheck.sa;
        
		// Get airToCheck break start and end times.
		Date bkStartTime = unCoveredBreakActivity.getShftactStartTime();
		Date bkEndTime = unCoveredBreakActivity.getShftactEndTime();
		ShiftDetail oDetail = unCoveredBreakActivity.getShiftDetail();
		Integer emp_id = oDetail.getEmpId();
        
		// Go through shift detail list looking for break coverage		
		for (int jCount=0; jCount<shiftDetailList.size(); jCount++) {
			sd = (ShiftDetail)shiftDetailList.get(jCount);		
				 
			// Skip current employee
			if ((sd.getEmpId().equals(emp_id) == true)) { 
				continue;
			}
            
			// Get the break activity start and end times and see that the 
            // break start and end times are entirely contained in the shift times.			
			Date shiftStartTime = sd.getShftdetStartTime();			
			Date shiftEndTime = sd.getShftdetEndTime();
			if (DateHelper.isEntirePeriodBetween(bkStartTime, bkEndTime,shiftStartTime, shiftEndTime)==false) {
				continue;
			}
            
			Vector bActivityList = sd.getShiftActivityList();
			boolean doesOverlap=doesBreakActivityOverlap(bActivityList, unCoveredBreakActivity);
			
			if (doesOverlap==false) {
				numCovering++;
			}				
		} // end jCount for loop
        
		if (numCovering<minRequired) {
			// We've found an uncovered break case
			return false;
		}
		
		return true;
	}

		
    /**
     * For a given AIR object, will try out different break times based on the 
     * allowed break times and check to see that one of them can be covered by 
     * other employess in this mgr. 
     * If it can be,  then the shift activty start and end times are changed; 
     * if not, they are left as the original start and end times.
     * 
	 *  Note: 
     *     allowedBreakEndTime represents the latest that a break can START, not END. 
     *     allowedBreakStartTime is the earliest that a break can START.
     */
	
	public boolean findAvailableBreakTimeSlot(ActivityIntervalRequirements breakAir)  throws RetailException {
	
		Date d = breakAir.allowedBreakStartTime;
		Date endTime = DateHelper.addMinutes(d, breakAir.breakLen);
		ShiftActivity sa = breakAir.sa;
		boolean isFound = false;
		
		while (d.before(breakAir.allowedBreakEndTime)== true) { 
			
			Date oldBreakStartTime = sa.getShftactStartTime(); 
			Date oldBreakEndTime = sa.getShftactEndTime();
			
			sa.setShftactEndTime(endTime);
			sa.setShftactStartTime(d);
			isFound = checkMealBreakCoverage(breakAir);
			if (isFound == false) {
				sa.setShftactEndTime((Date)oldBreakEndTime.clone());
				sa.setShftactStartTime((Date)oldBreakStartTime.clone());
			}
			else {
				isFound=true;
				return isFound;
			}
            
			d = DateHelper.addMinutes(d,15);
			endTime = DateHelper.addMinutes(d, breakAir.breakLen);	
		} 
        
		return isFound;
	} 
	
		
	
	/** 
	 * For a given employee, skill and job, checks if this employee either has a 
     * skill equal to skillID or has a qualifying job.
     * 
	 * If there is a skill match OR if the passed skillId is null, then we move on 
     * to see if there is a qualifying job match. 
     * 
     * If there is no skill match (even though a non-null skillId was passed in) 
     * then return false.
     * 
	 * To decide if a paricular job is qualifying, we do the following steps:
	 *  1) Find the job set corresponding to this jobId.
	 *  2) If no job set is found, then assume the jobId passed in is the job set id.
	 *  3) Then get list of job employee is qualified to do.
	 *  4) If any of the jobs the employee is qualified to do matches either the job set id
	 *     OR a job id within the job set, then return true, else return false.  
	 */
	private boolean lookForMatchingSkillAndJob (Employee emp, Integer skillId, int jobId) 
            throws RetailException {
		EmployeeSkill emp_skill = null;
        
		boolean isFound = false;
		Date sysdate = new Date();
		Vector emp_skill_list = emp.getEmployeeSkillList();
        
		// Just skip it if there is no skill needed for this position.
		if (skillId!=null) {
			for (int i=0;i<emp_skill_list.size(); i++) {
				emp_skill = (EmployeeSkill)emp_skill_list.get(i);
				if (DateHelper.isBetween(sysdate, emp_skill.getEmpsklStartDate(), emp_skill.getEmpsklExpDate())==false) {
					continue;				
				}
				if (emp_skill.getSkillId().equals(skillId)) {
					isFound=true;
					break;
				}
			}
            
			// No matching skills, so return false, else to ahead find a job match too.
			if (isFound==false) {
				return isFound;
			}
		}
        
		//Now let's look for a job match
        //
		PositionSetAccess posAccess = new PositionSetAccess(dbConnection);
		Integer parent_job_id = null;
        
		// Get parent of this job
		ArrayList parentJobList = (ArrayList) posAccess.loadPositionSetByJobChildId(jobId);
		for (int i=0; i< parentJobList.size(); i++) {
			PositionSet pos = (PositionSet) parentJobList.get(i);
			parent_job_id = pos.getJobId();			
		}
        
		ArrayList jobList=null;

		// Load position set based on parent job id.
		if (parent_job_id != null) {
			jobList = (ArrayList) posAccess.loadPositionSetByJob(parent_job_id.intValue());
		}
		else {
			// If no parent job, just set the parent_job_id to be the job Id itself
			// ie. this job id not part of a position set
			parent_job_id=new Integer(jobId);
		}
        
		// What jobs can this employee do
		Vector employeeJobs= emp.getEmployeeJobList();
		Vector employee_job_id_list = new Vector();
		for (int k=0; k < employeeJobs.size(); k++) {
			EmployeeJobData eData = (EmployeeJobData) employeeJobs.get(k);			
			if (DateHelper.isBetween(sysdate, eData.getEmpjobStartDate(), eData.getEmpjobEndDate())) {
				employee_job_id_list.add(new Integer(eData.getJobId()));				
			}
		}
        
		// Go through the job position set and see if any of them are in the list of jobs 
        // this employee is qualified to do. If it is, we've found a matching employee.
        //
		// If position set is empty, then parent_job_id was a real job.
        //
		// Let's see if employee job list contains this job. if it does,
		// he's qualified and return true.
		
		if (employee_job_id_list.contains(parent_job_id)) {
			isFound=true;
			return isFound;
		}
		
		for (int l=0; l < jobList.size(); l++ ) {
			PositionSet ps = (PositionSet) jobList.get(l);
			if (employee_job_id_list.contains(ps.getJobIdChild())) {
				isFound = true;
				break;
			}
		}
        
		return isFound;		
	}
    
	/**
	 * For a given a Vector of ShiftActivity break objects (bActivityList), and a 
     * ShiftActivity (breakActivity) that represents a break, goes thrrough bActivityList 
     * and returns true if even one of the breaks partially or completely overlaps
	 * with the breakActivity object.
	 * 
	 */
	private boolean doesBreakActivityOverlap(Vector bActivityList, ShiftActivity breakActivity) 
            throws RetailException {
		boolean doesOverlap = true;
		ShiftActivity breakActivity2 = null;
        
		for (int i=0; i<bActivityList.size(); i++) {
			breakActivity2 = (ShiftActivity) bActivityList.get(i);
			if (breakActivity2.isBreak()==false) {
				continue;
			}
            
			// If any of this guy's breaks overlap even partially with the break 
            // we're trying to cover, then he's ineligable
			if ((ShiftActivityUtil.isPartiallyOverlapping(breakActivity2,breakActivity)==true) ||
				(ShiftActivityUtil.isCompletelyOverlapping(breakActivity2,breakActivity)==true)) {
				doesOverlap = true;
				break;
			}
			else {
				doesOverlap=false;
			}
		} 
        
		return doesOverlap;
	}
	
	/**
	 * 
	 * For a given AIR object (unCoveredAir) representing an unCovered break Activity,
	 * goes through the ShiftDetailList for this mgr looking for a scheduled employee 
     * with a matching skill and/or qualifying job. After checking for skills/jobs, 
     * we make sure that the AIR break time is entirely within the shift start and 
     * end times of the potential covering shift detail.
     * 
	 * Then meal break coverage is checked assuming that the transition happens.
	 * If meal break coverage is ok, then the transition occurs.
     *  
	 */
	public boolean findMatchingEmployeeShiftDetail(ActivityIntervalRequirements unCoveredAir) 
            throws RetailException {
        
		boolean isFound;		
		ShiftDetail oDetail = null;
        
		ShiftActivity unCoveredBreakActivity = unCoveredAir.sa;
		Date bk_start_time = unCoveredBreakActivity.getShftactStartTime();
		Date bk_end_time = unCoveredBreakActivity.getShftactEndTime();
		
		oDetail = unCoveredBreakActivity.getShiftDetail();
		Integer skillId = oDetail.getSkillId();
		int jobId= oDetail.getJobId();
        
		// Go through break activities of this dept-job combination to see if we 
        // can find a possible match. i.e. an available skilled employee whose own break 
        // doesn't clash with the one we're trying to cover
        
		for (int jCount=0; jCount<shiftDetailList.size(); jCount++) {
			oDetail = (ShiftDetail)shiftDetailList.get(jCount);
			Employee emp = oDetail.getEmployee();
			
			isFound = lookForMatchingSkillAndJob(emp, skillId, jobId);
		
			if (isFound==false) {
				continue;
			}
            
			// Now check if this employee's break start and end times conflicts 
            // with the one we're trying to cover also make sure that this employee's 
            // shift times (start/end) completely overlap the uncoverd break 
            // start and end times.
			Date shift_start_time = oDetail.getShftdetStartTime();
			Date shift_end_time = oDetail.getShftdetEndTime();
			if (DateHelper.isEntirePeriodBetween(bk_start_time, bk_end_time,shift_start_time, shift_end_time)==false) { 
				continue;
			}
            
			// Go through the shift detail's breaks and make sure that this employee's
			// breaks aren't during the time we need him to cover.            
			Vector bActivityList = oDetail.getShiftActivityList();
			boolean doesOverlap=doesBreakActivityOverlap(bActivityList, unCoveredBreakActivity);								
			if (doesOverlap==true) {
				continue;
			}
            
			ActivityIntervalRequirements coveringAir= null;
            
    		// Create new shift detail with shift activity mirroring the break
    		coveringAir = createNewShiftActivity(unCoveredAir, oDetail);
    		ShiftActivity oCoveringWorkActivity = coveringAir.sa;
            
    		// Check meal break coverage : if the transition is made, do we have 
            // enough people to cover this position during this break time?
    		boolean canCover = checkMealBreakCoverage(coveringAir);
    		if (canCover==true) {
    			// If this shiftActivity can cover the break, then adjust the 
                // shift detail to add the covering activity.
    			// also add this new shift to the mgr.
    			adjustAddShiftDetail(oDetail, oCoveringWorkActivity);    			
    			unCoveredAir.isCovered=true;
    			addShiftActivity(coveringAir, true);
    			return true;    			
    		}
		} // end for
        
		// If we got here, it means no matching shift detail was found 
		return false;			
	}
	

	/**
	 * This method is used during shift activity coverage transitions. 
     * For a given uncovered break AIR and a covering shift detail, creates a new 
     * AIR consisting of a work ShiftActivity. This work shiftActivity is used to 
     * cover the unCovered break ShiftActivity.
     * 
	 * The shiftActivity will be under coveringShiftDetail.
	 * 
	 */
	private ActivityIntervalRequirements createNewShiftActivity(ActivityIntervalRequirements unCoveredAir, ShiftDetail coveringShiftDetail) 
            throws RetailException {
        
		ShiftActivity unCoveredBreakActivity=unCoveredAir.sa;		
		ActivityIntervalRequirements coveringAir = null;
        	
        // Create working activity
        ShiftActivity oCoveringWorkActivity = (ShiftActivity) unCoveredBreakActivity.newInstance();
        oCoveringWorkActivity.assignIsNew(true);
        oCoveringWorkActivity.setShftactDisplayId(coveringShiftDetail.getSchedule().generateShftactDisplayId());
        oCoveringWorkActivity.setShftactDate((Date)unCoveredBreakActivity.getShftactDate().clone());
        oCoveringWorkActivity.setShftactStartTime((Date)unCoveredBreakActivity.getShftactStartTime().clone());
        oCoveringWorkActivity.setShftactLen(unCoveredBreakActivity.getShftactLen());
        oCoveringWorkActivity.setShftactEndTime((Date)unCoveredBreakActivity.getShftactEndTime().clone());
        oCoveringWorkActivity.setSkdgrpId(unCoveredBreakActivity.getSkdgrpId());
        
        oCoveringWorkActivity.setShiftDetail(coveringShiftDetail);
        oCoveringWorkActivity.setSchedule(unCoveredBreakActivity.getSchedule());
        
        Activity activity = getCorrespondingWorkActivity(unCoveredBreakActivity);
        
        oCoveringWorkActivity.setActId(activity.getID().intValue());
        oCoveringWorkActivity.setActivity(activity);
		coveringAir = new ActivityIntervalRequirements(unCoveredAir.scheduleStartDate, unCoveredAir.scheduleEndDate,oCoveringWorkActivity, unCoveredAir.intervalRequirementsList);
		
		return coveringAir;
	}
	
	/**
	 * For a given breakActivity ShiftActvity object, returns the work activity object
	 * that this breakActivity is contained in. 
	 */
	public Activity getCorrespondingWorkActivity(ShiftActivity breakActivity) throws RetailException {
		
        Vector w_list = breakActivity.getShiftDetail().getShiftActivityList();
        
        Activity activity = null;
        for (int t=0; t< w_list.size(); t++ ) {
        	ShiftActivity sa = (ShiftActivity) w_list.get(t);
        	if (sa.isBreak()==false) {
        	    // Find the work activity in the shift detail that completely
        	    // overlaps the break shift activity
        	    if (ShiftActivityUtil.isCompletelyOverlapping(sa,breakActivity)==true) {
        		    activity = (Activity)sa.getActivity().duplicate();
            		break;
			    }	
        	}
        } 
        
		return activity;        
	} 
    
	/**
	 * Finds out where in a shift detail a covering work activity fits in,
	 * and adjusts the surrounding work activity times. There are different cases
	 * depending on whether the new covering work activity fits into the beginning,
	 * middle, or end of an existing work activity.
	 *  
	 */
	private void adjustAddShiftDetail(ShiftDetail coveringShiftDetail, ShiftActivity coveringWorkActivity) 
        throws RetailException	{
		
		Vector shiftActivityList = (Vector) coveringShiftDetail.getSortedShiftActivityListStartThenWorking();
		
		for (int i=0; i< shiftActivityList.size(); i++) {
			ShiftActivity sa2 = (ShiftActivity) shiftActivityList.get(i);
            
			// If a working activity
			if (sa2.isBreak()==false) {
                
				// Do shift activity adjustment
				if (ShiftActivityUtil.isCompletelyOverlapping(sa2, coveringWorkActivity)) {
					// If the covering work activity is completely contained in this work 
                    // activity, then let's test if it's at either end of the work actvitity,
					// in which case a new work activity does not have to be created
					if (sa2.getShftactStartTime().equals(coveringWorkActivity.getShftactStartTime()) &&
							(sa2.getShftactEndTime().after(coveringWorkActivity.getShftactEndTime()))
							) {
						// If start times are equal and covering work actvity ends before this activity 
                        // end time, then set the activity start time to be the covering actvity end time
						sa2.setShftactStartTime((Date) coveringWorkActivity.getShftactEndTime().clone());
						break;
					}
					else if (sa2.getShftactEndTime().equals(coveringWorkActivity.getShftactEndTime()) &&
							(sa2.getShftactStartTime().before(coveringWorkActivity.getShftactStartTime()))) {
						// If end times are equal and covering work actvity begins after 
                        // this activity start time,
						// then set the activity end time to be the covering actvity start time	
						sa2.setShftactEndTime((Date) coveringWorkActivity.getShftactStartTime().clone());
						break;
					}
					else if (sa2.getShftactEndTime().after(coveringWorkActivity.getShftactEndTime()) &&
							(sa2.getShftactStartTime().before(coveringWorkActivity.getShftactStartTime())))	{
						// If it's sandwiched in we have to adjust the times but also 
                        // have to create a new work activity, ie. split it into two parts
						ShiftActivity saInsert = (ShiftActivity) sa2.clone();
						saInsert.assignIsNew(true);
						saInsert.setSchedule(sa2.getSchedule());
						saInsert.setActivity((Activity)sa2.getActivity().duplicate());
						sa2.setShftactEndTime((Date) coveringWorkActivity.getShftactStartTime().clone());
						
						saInsert.setShftactStartTime((Date) coveringWorkActivity.getShftactEndTime().clone());
                        
						// Find Interval requirements corresponding to sa2
						Vector intReq=null;
						Date scheduleStartDate=null;
						Date scheduleEndDate = null;
						for (int j=0; j < workActivityList.size(); j++) {
							ActivityIntervalRequirements air = (ActivityIntervalRequirements) workActivityList.get(j);
							if (air.sa.equals(sa2)) {
								intReq = air.intervalRequirementsList;
							}
						}
						coveringShiftDetail.addShiftActivity(saInsert);
						addShiftActivity(new ActivityIntervalRequirements(scheduleStartDate, scheduleEndDate, saInsert,intReq), false);
						
						break;						
					} // end else if					
				} // end inner if
                
				else if (ShiftActivityUtil.isPartiallyOverlapping(sa2, coveringWorkActivity)) {					
					if (DateHelper.isBetween(coveringWorkActivity.getShftactStartTime(), sa2.getShftactStartTime(),sa2.getShftactEndTime())) {
						// If the first part of the covering activity partially
						// overlaps with this activity,
						// 
						sa2.setShftactEndTime((Date) coveringWorkActivity.getShftactStartTime().clone());
					}
					else if (DateHelper.isBetween(coveringWorkActivity.getShftactEndTime(), sa2.getShftactStartTime(),sa2.getShftactEndTime())) {
						sa2.setShftactStartTime((Date) coveringWorkActivity.getShftactEndTime().clone());
					}
				}
			} // end outer if
		} // end for
        
		// add new working actvity to replacing employee's shift detail		
		coveringShiftDetail.addShiftActivity(coveringWorkActivity);
		coveringShiftDetail.sortShiftActivityListStartThenWorking();
	} // end proc

} // end class


/**
 * This class, ActivityIntervalRequirements (abbreviated to "AIR" in technical docs)
 * is a wrapper class for shiftActivities in a mgr that need to have break coverage
 * examined. An AIR is created for both break and work activities but typically, the AIR
 * is used mostly by break activity ShiftActivity objects. The AIR class was developed as 
 * a container for ShiftActivity objects in order that access to such activity-specific 
 * information as interval requirements, allowed Break Start and End times, can be 
 * conveniently acccessed when needed in the code. 
 * 
 */
class ActivityIntervalRequirements {
	
	public ShiftActivity sa;
	
	public boolean isCovered;
	public Vector intervalRequirementsList;    // vector of IntervalRequirementsManager.Record objects
	public Date scheduleStartDate;
	public Date scheduleEndDate;
	private Date actStartTime;
	private Date actEndTime;
	public Date allowedBreakStartTime;
	public Date allowedBreakEndTime;
	public int breakLen;	
	private static final String REGKEY_SO_USE_INTERVAL_REQ = "/system/modules/scheduleOptimization/SO_USE_INTREQ_MEAL_BK_COVERAGE";
    private static final String useIntReq = Registry.getVarString(REGKEY_SO_USE_INTERVAL_REQ, "FALSE").toUpperCase();	
    
	public ActivityIntervalRequirements( Date sched_start, Date sched_end, 
                                         ShiftActivity sa1, Vector intreq) throws RetailException {
		sa = sa1;
		intervalRequirementsList = intreq;
		isCovered = true;
		scheduleStartDate = DateHelper.truncateToDays(sched_start);
		scheduleEndDate = DateHelper.truncateToDays(sched_end);
	    actStartTime = sa1.getShftactStartTime();
	    actEndTime = sa1.getShftactEndTime();	    
	}
    
	/**
     * For a given AIR's ShiftActivity's Employee Group activity rules,
	 * computes the minimum and possible times that breaks can START, and stores
	 * these values in the fields allowedBreakStartTime and allowedBreakEndTime.
	 * The activity rules can be found in the LFSO GUI under Location Setup->Break Rules.
	 *  
	 */	
	public void setAllowableBreakTimes() throws RetailException	{
		Vector activityRules = sa.getSchedule().getEmployeeGroup().getActivityRulesList();
		double minShiftLength=0;
		double maxShiftLength=0;
        
		// minBreakLength and maxBreakLength establish the minute
		// position into the shift that a break can start and end.
		int minBreakMinPosition=0;
		int maxBreakMinPosition=0;
        
		breakLen = 0;		
			
		// Get Shift detail length in minutes
		int length = sa.getShiftDetail().getLength().intValue();
        
		// Now let's see which activity rule applies
		for (int l=0; l < activityRules.size(); l++) {
            ActivityRule actRule = (ActivityRule) activityRules.get(l);
            
			// Get min/max shift lengths in minutes
			minShiftLength = actRule.getEgarShiftLen().doubleValue()*60;
			maxShiftLength = actRule.getEgarShiftLenfin().doubleValue()*60;
			
            if ((length >= minShiftLength) && (length <= maxShiftLength)) {
			    // We've got the right activityRule. Get the break length min/maxes in minutes
				minBreakMinPosition = (int) (actRule.getEgarShStartTime().doubleValue()*60);
				maxBreakMinPosition = (int) (actRule.getEgarShEndTime().doubleValue()*60);
				breakLen = actRule.getEgarActvyLen().intValue();
				break;
			}	
		} 
			
        // Create time interval for when the break must be.
		if ((minBreakMinPosition >0)&&(maxBreakMinPosition>0)) {
			// If we're able to get a break length from the activity rules
			// then establish time frame from ShiftDetail of when break is allowed to be.
			allowedBreakStartTime = DateHelper.addMinutes(sa.getShiftDetail().getShftdetStartTime(),minBreakMinPosition);
			allowedBreakEndTime = DateHelper.addMinutes(sa.getShiftDetail().getShftdetStartTime(),maxBreakMinPosition);	
		}
		else {
			// assume break could be any time during the shift
			allowedBreakStartTime = sa.getShiftDetail().getShftdetStartTime();
			allowedBreakEndTime = DateHelper.addMinutes(sa.getShiftDetail().getShftdetStartTime(),breakLen*-1);	
		}
        
		if (allowedBreakEndTime.after(DateHelper.addMinutes(sa.getShiftDetail().getShftdetEndTime(), breakLen*-1))) {
			allowedBreakEndTime=DateHelper.setTimeValues(allowedBreakEndTime, DateHelper.addMinutes(sa.getShiftDetail().getShftdetEndTime(), breakLen*-1));
		}
	}
    
    
	/**
	 * Computes the minimum # of ppl needed during a paricular period of time
	 * for a paritcular activity. Note that for breaks, what the interval requirements
	 * represent is the IR's for the WORK activity corresponding to that break actvity.
	 *  
	 * The interval requirements array consists of integers, one for each 15-min interval,
	 * (where the 15 min value is the interval type and is dependent on interval requirements settings)
	 */	
	public int getMinRequired()  throws RetailException {
        
	    actStartTime = sa.getShftactStartTime();
	    actEndTime = sa.getShftactEndTime();

        if( "FALSE".equals(useIntReq) || "F".equals(useIntReq) || "N".equals(useIntReq) ) {            
			return 1;
		}
        
		// If any of these are null, assume that our min required is a default of 1.		
		if ((intervalRequirementsList==null)||(scheduleStartDate==null)||(scheduleEndDate==null)) {
			return 1;
		}
        
        // Get the interval requirements for this schedule
		IntervalRequirementsManager irMgr = sa.getSchedule().getIntervalRequirements();
		int arraySize = irMgr.getTotalNumOfIntervals();
        
		// Figure out how many minutes betwen the start of schedule and the activity start/end times
		long minutesBetweenStart = DateHelper.getMinutesBetween(actStartTime,scheduleStartDate);
		long minutesBetweenEnd = DateHelper.getMinutesBetween(actEndTime,scheduleStartDate );
        
		int [] total_minTotalNeeded = new int[arraySize];
		Arrays.fill(total_minTotalNeeded, 0);
		int intervalOffset_start = 0;
		int intervalOffset_end = 0;
		int tempMax=0;
        
		// Go through interval requirements list and compute the offset in the 
        // interval requirements list based on the minutesBetweenStart and interval type.
		for (int j=0; j < intervalRequirementsList.size(); j++) {
			Record intervalRequirements = (Record) intervalRequirementsList.get(j);
			intervalOffset_start = (int) (minutesBetweenStart / intervalRequirements.m_IntervalType.getLengthInMinutes());
			intervalOffset_end = (int) (minutesBetweenEnd / intervalRequirements.m_IntervalType.getLengthInMinutes());
			for (int i=0; i< arraySize; i++) {
				total_minTotalNeeded[i] = total_minTotalNeeded[i] + intervalRequirements.m_MinTotalNeeded[i];
			}			
		}
        
		// Compute interval requirements from offset...take the maximum value from the
		// various numbers in the array and use that as the # of ppl required value
		for (int k=intervalOffset_start; k <= intervalOffset_end; k++) {
			if (total_minTotalNeeded[k] > tempMax) {
				tempMax = total_minTotalNeeded[k];
			}
		}
		
		return tempMax;
	}
    
}
	
