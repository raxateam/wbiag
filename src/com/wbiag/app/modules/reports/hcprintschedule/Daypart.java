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
 * Daypart is a node in the model struture that is created by PrintingScheduleModel and passed to a
 * View (PrintingScheduleView). It is used in Team.
 *
 * The model structure levels are roughly like this;
 * Team -> Day Parts -> Jobs -> Employees -> Employee Schedule Detalis
 * They are represented using the following classes (in order)
 * Team -> Daypart -> Job -> Employee -> Schedule
 * All classes representing a node in this structure have public fields, so you must be careful when
 * manipulating them.
 */
public class Daypart {
    private static org.apache.log4j.Logger logger =
        org.apache.log4j.Logger.getLogger(Daypart.class);

	public int id;					//daypart's db id
	public String name;				//daypart's db name
	public Date startTime;			//start time (time of day)
	public Date endTime;			//end time (time of day)
	public List jobs;				//list of jobs for this daypart
	public List floatingSchedules;	//list of loating employees under this daypart

	/**
	 * Default constructor.
	 * No params can be null.
	 * @param id		daypart's db id
	 * @param name		daypart's db name
	 * @param startTime	start time (time of day)
	 * @param endTime	end time (time of day)
	 */
	public Daypart(int id, String name, Date startTime, Date endTime){
		if (name == null){
			throw new InvalidParameterException("daypart's name can't be null");
		}
		if (startTime == null){
			throw new InvalidParameterException("daypart's start time can't be null");
		}
		if (endTime == null){
			throw new InvalidParameterException("daypart's end time can't be null");
		}
		this.id = id;
		this.name = name;
		this.startTime = startTime;
		this.endTime = endTime;

		this.jobs = new ArrayList();

        log("Init with: id="+id+", name="+name+", startTime="+startTime+", endTime="+endTime);
	}

	/**
	 * Gets the job at index index
	 * @param index Index to get job from daypart's job list
	 * @return the job at index index. null if index is invalid.
	 */
	public Job getJob(int index){
		if (index < jobs.size()){
			return (Job)jobs.get(index);
		}else{
			return null;
		}
	}

	/**
	 * Appends a job to the end of the daypart's job list.
	 * @param newJob The job to be added to job list. cannot be null.
	 */
	public void addJob(Job newJob){
		if (newJob == null){
			throw new InvalidParameterException("Cannot add null job");
		}
		jobs.add(newJob);

        log("Add a job: " + newJob);
	}

	public String toString(){
		return id + " " + name + " from " + DateHelper.convertDateString(startTime, "HH:mm") + " to " + DateHelper.convertDateString(endTime, "HH:mm");
	}

    private void log(String msg) {
        if (logger.isDebugEnabled()) logger.debug(msg);
    }
}
