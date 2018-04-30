package com.wbiag.app.modules.reports.hcprintschedule;

import java.security.InvalidParameterException;
import java.util.*;

/**
 * @author 			Ali Ajellu
 * @version 		1.0
 * Date: 			Friday, June 16 206
 * Copyright: 		Workbrain Corp.
 * TestTrack:		1630
 *
 * Job is a node in the model struture that is created by PrintingScheduleModel and passed to a
 * View (PrintingScheduleView). It is used in Daypart.
 *
 * The model structure levels are roughly like this;
 * Team -> Day Parts -> Jobs -> Employees -> Employee Schedule Detalis
 * They are represented using the following classes (in order)
 * Team -> Daypart -> Job -> Employee -> Schedule
 * All classes representing a node in this structure have public fields, so you must be careful when
 * manipulating them.
 */
public class Job {
    private static org.apache.log4j.Logger logger =
        org.apache.log4j.Logger.getLogger(Job.class);

	public int id;					//job's db id
	public String name;				//job's db name
	public ArrayList employees;		//job's employee list
	public int[] staffRequired;		//job's staff requirements for team's date range [Team.startDate, Team.endDate]

	/**
	 * Default constructor.
	 * none of the params can be null.
	 * @param id	//job's db id
	 * @param name	//job's db name
	 */
	public Job(int id, String name){
		if (name == null){
			throw new InvalidParameterException("Job's name can't be null");
		}
		this.id = id;
		this.name = name;

        log("Init with: id="+id+", name="+name);

		this.employees = new ArrayList();
	}

	/**
	 * Get the employee at index index in the job's employee list
	 * @param index	index of employee in the employee list
	 * @return employee at index index. null if index in invalid.
	 */
	public Employee getEmployee(int index){
		if (index < employees.size()){
			return (Employee)employees.get(index);
		}else{
			return null;
		}
	}

	/**
	 * Appends an employee to the end of the job's employee list
	 * @param newEmp The employee to be added to the employee list. cannot be null.
	 */
	public void addEmployee(Employee newEmp){
		if (newEmp == null){
			throw new InvalidParameterException("Can't add null employee");
		}

		employees.add(newEmp);
	}

	public String toString(){
		return "#"+ id + " " +name;
	}

    private void log(String msg) {
        if (logger.isDebugEnabled()) logger.debug(msg);
    }
}
