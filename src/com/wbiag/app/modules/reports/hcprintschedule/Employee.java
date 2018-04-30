package com.wbiag.app.modules.reports.hcprintschedule;

import java.security.InvalidParameterException;
import java.util.*;
import com.workbrain.util.*;

/**
 * @author 			Ali Ajellu
 * @version 		1.0
 * Date: 			Friday, June 16 206
 * Copyright: 		Workbrain Corp.
 * TestTrack:		1630
 *
 * Employee is a node in the model struture that is created by PrintingScheduleModel and passed to a
 * View (PrintingScheduleView). It is used in Job.
 *
 * The model structure levels are roughly like this;
 * Team -> Day Parts -> Jobs -> Employees -> Employee Schedule Detalis
 * They are represented using the following classes (in order)
 * Team -> Daypart -> Job -> Employee -> Schedule
 * All classes representing a node in this structure have public fields, so you must be careful when
 * manipulating them.
 */
public class Employee {
    private static org.apache.log4j.Logger logger =
        org.apache.log4j.Logger.getLogger(Employee.class);

	public String name;				//employee's FULL NAME (db: Employee.emp_full_name)
	public String fteValue;			//employee's UDF4 (db: Employee.emp_udf4)
	public ArrayList schedules;		//two dim list
									//dim 1: days in which the emp has a schedule detail record
									//dim 2: a list of schedule details for that day.
	public int homeTeamId;			//employee's home team on the schedule start date
	/**
	 * Default constrcutor.
	 * none of the params can be null.
	 * @param name		employee's FULL NAME (db: Employee.emp_full_name)
	 * @param fteValue	employee's UDF4 (db: Employee.emp_udf4)
	 */
	public Employee(String name, String fteValue){
		if (name == null){
			throw new InvalidParameterException("Employee's name can't be null");
		}
		this.name = name;
		this.fteValue = fteValue;

        log("Init with: name="+name + ", fteValue="+fteValue);
		this.schedules = new ArrayList();
	}

	/**
	 * Gets the list of schedules for a day residing at index index.
	 * Be careful, since the dates are not ordered.
	 * @param index
	 * @return schedule at index index
	 */
	public List getSchedules(int index){
		if (index < schedules.size()){
			return (List)schedules.get(index);
		}else{
			return null;
		}
	}

	/**
	 * Returns all the schedule details of the given date
	 * @param date
	 * @return all schedules on date
	 */
	public List getSchedules(Date date){
		int targetIndex = findScheduleIndexForDate(date);
		if (targetIndex != -1){

            log("Schedule for date "+date+" is " + (List)schedules.get(targetIndex));
			return (List)schedules.get(targetIndex);
		}else{
			return null;
		}
	}

	/**
	 * Adds the given schedule to the employee.
	 * It finds the right day and appends newSchedule, so you won't have to worry about that.
	 * Use getSchedule(date) in conjunction with this method.
	 * @param newSchedule schedule to be added
	 */
	public void addSchedule(Schedule newSchedule){
		if (newSchedule == null){
			throw new InvalidParameterException("Can't add null schedule");
		}

		int targetIndex = findScheduleIndexForDate(newSchedule.workDate);
		//day already exists
		if (targetIndex != -1){
			List dayToAddTo = (List)schedules.get(targetIndex);
			//add schedule to day
			dayToAddTo.add(newSchedule);
		//day doesn't exist
		}else{
			//create a new day
			List newDay = new ArrayList();
			newDay.add(newSchedule);
			schedules.add(newDay);
		}
	}

	/**
	 * Given a date, it finds the index in the schedule list that holds all schedules for that date.
	 * @param date
	 * @return
	 */
	private int findScheduleIndexForDate(Date date){
		Date tempDate;
		date = DateHelper.truncateToDays(date);

		for(int i=0;i<schedules.size();i++){
			List scheduleDetails = (List)schedules.get(i);
			tempDate = ((Schedule)scheduleDetails.get(0)).workDate;
			tempDate = DateHelper.truncateToDays(tempDate);
			if (tempDate.equals(date)){
				return i;
			}
		}
		//nothing on that date
		return -1;
	}

	/**
	 * Sets the hometeam to the employee.
	 * Use getHomeTeam() in conjunction with this method.
	 * @param home team ID on the start date of the scheduled report
	 */
	public void setHomeTeam(int newId){
		homeTeamId = newId;  	//Hub 36230
	}
	/**
	 * Gets the hometeam of the employee.
	 * Use setHomeTeam() in conjunction with this method.
	 */
	public int getHomeTeam(){
		return homeTeamId;      //Hub 36230
	}

	public String toString(){
		return name + " " + " @ " + fteValue;
	}

    private void log(String msg) {
        if (logger.isDebugEnabled()) logger.debug(msg);
    }
}
