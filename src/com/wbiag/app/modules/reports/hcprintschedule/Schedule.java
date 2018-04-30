package com.wbiag.app.modules.reports.hcprintschedule;

import java.security.InvalidParameterException;
import java.util.*;

/**
 * @author          Ali Ajellu
 * @version         1.0
 * Date:            Friday, June 16 206
 * Copyright:       Workbrain Corp.
 * TestTrack:       1630
 *
 * Schedule is a node in the model struture that is created by PrintingScheduleModel and passed to a
 * View (PrintingScheduleView). It is used in Employee.
 *
 * The model structure levels are roughly like this;
 * Team -> Day Parts -> Jobs -> Employees -> Employee Schedule Detalis
 * They are represented using the following classes (in order)
 * Team -> Daypart -> Job -> Employee -> Schedule
 * All classes representing a node in this structure have public fields, so you must be careful when
 * manipulating them.
 */
public class Schedule {
    private static org.apache.log4j.Logger logger =
        org.apache.log4j.Logger.getLogger(Schedule.class);

	public Date workDate;              //Schedule's date
	public String empName;             //Name of the employee to whom this schedule belongs
	public String job;                 //the job that's supposed to be done during schedule
	public String startTime;           //Schedule's start time (HHmm)
	public String endTime;             //Schedule's end time (HHmm)
	public int mins;                   //Schedule's duration (in mins)
	public String timecode;            //Schedule's time code name
	public String team;                //Schedule's team name
	public String homeTeam;            //Employee's home team name

    /**
     * Default constructor.
     * None of the params can be null.
     * @param workDate      schedule's work date
     * @param empName       schedule's owner's emp name (last, first)
     * @param job           schedule's job
     * @param startTime     schedule's start time (HHmm)
     * @param endTime       schedule's end time (HHmm)
     * @param mins          schedule's duration (in mins)
     * @param timecode      schedule's time code name
     * @param team          schedule's team name
     * @param homeTeam      schedule's owner's current home team name
     */
	public Schedule(Date workDate, String empName, String job, String startTime, String endTime, int mins, String timecode, String team, String homeTeam){
		if (workDate == null){
			throw new InvalidParameterException("workDate parameter is null");
		}
		if (startTime == null){
			throw new InvalidParameterException("startTime parameter is null");
		}
		if (endTime == null){
			throw new InvalidParameterException("endTime parameter is null");
		}
		if (timecode == null){
			throw new InvalidParameterException("timecode parameter is null");
		}

		this.workDate = workDate;
		this.empName = empName;
		this.job = job;
		this.startTime = startTime;
		this.endTime = endTime;
		this.timecode = timecode;
		this.team  = team;
		this.homeTeam = homeTeam;
		this.mins = mins;

        log("Init with: workDate=" + workDate + ", empName="+empName + ", job=" + job
                    +", startTime=" + startTime + ", endTime=" + endTime + ", timecode="+timecode
                    +", team="+team+", homeTeam="+homeTeam+", mins=" + mins);
	}

	public String toString(){
		return workDate + ": from " + startTime + " to " + endTime + " @ " + timecode + " on " + team + ". on hometeam: " + homeTeam;
	}

    private void log(String msg) {
        if (logger.isDebugEnabled()) logger.debug(msg);
    }
}
