package com.wbiag.app.modules.reports.hcprintschedule;

import java.util.Date;
import java.util.ArrayList;
import java.security.InvalidParameterException;

/**
 * @author 			Ali Ajellu
 * @version 		1.0
 * Date: 			Friday, June 16 206
 * Copyright: 		Workbrain Corp.
 * TestTrack:		1630
 *
 * Team is the root of each model struture that is created by PrintingScheduleModel and passed to a
 * View (PrintingScheduleView).
 *
 * The model structure levels are roughly like this;
 * Team -> Day Parts -> Jobs -> Employees -> Employee Schedule Detalis
 * They are represented using the following classes (in order)
 * Team -> Daypart -> Job -> Employee -> Schedule
 * All classes representing a node in this structure have public fields, so you must be careful when
 * manipulating them.
 */
public class Team {
    private static org.apache.log4j.Logger logger =
        org.apache.log4j.Logger.getLogger(Schedule.class);

	public int id;					//team's db id
	public String name;				//team's db name
	public Date startDate;			//start of the date range that the info for this team should be printed
	public Date endDate;			//end of the date rnage that the info for this team should be printed
	public ArrayList dayparts;		//this team's selected dayparts.

	/**
	 * Default constructor.
	 * None of the params can be null.
	 * @param id		team's db id
	 * @param name		team's db name
	 * @param startDate	start of the date rnage that the info for this team should be printed
	 * @param endDate	end of the date rnage that the info for this team should be printed
	 */
	public Team(int id, String name, Date startDate, Date endDate){
		if (name == null || startDate == null || endDate == null){
			throw new InvalidParameterException("At least one parameter is null");
		}

		this.id = id;
		this.name = name;
		this.startDate = startDate;
		this.endDate = endDate;

        log("Init with: id=" + id + ", name="+name+", startDate="+startDate + ", endDate="+endDate);
		this.dayparts = new ArrayList();
	}

	/**
	 * Gets the daypart at index
	 * @param index Index into the daypart list.
	 * @return PDaypart at index index. null if index is invalid.
	 */
	public Daypart getDaypart(int index){
		if (index < dayparts.size()){
			return (Daypart)dayparts.get(index);
		}else{
			return null;
		}
	}

	/**
	 * Append a newDaypart to the end of the team's list of dayparts
	 *
	 * @param newDaypart Day Part to add to list. Cannot be null.
	 */
	public void addDaypart(Daypart newDaypart){
		if (newDaypart == null){
			throw new InvalidParameterException("Can't add null daypart");
		}
		dayparts.add(newDaypart);
	}

	public String toString(){
		return "#" + id + " " + name + " from " + startDate + " to " + endDate;
	}

    private void log(String msg) {
        if (logger.isDebugEnabled()) logger.debug(msg);
    }
}
