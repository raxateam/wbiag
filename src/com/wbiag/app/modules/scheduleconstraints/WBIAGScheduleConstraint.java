package com.wbiag.app.modules.scheduleconstraints;

/**
 * Name: 			WBIAGScheduleConstraint
 * Descirption: 	Extending the core schedule constraints with extra functionality
 *	 				checkUdf1(c, param) where param=x,y
 *					checks whether there are x consecutive y-hour long shifts
 * ex:				4,12: constraint violated when there are 4 consecutive 12 hour shifts		
 * Date: 			Feb. 07, 2006
 * Copyright:    	Copyright (c) 2006
 * Company:      	Workbrain Inc
 * 
*/

import java.sql.SQLException;
import java.util.Date;
import java.util.StringTokenizer;

import com.workbrain.app.modules.scheduleconstraints.ScheduleConstraint;
import com.workbrain.app.modules.scheduleconstraints.ScheduleConstraintContext;
import com.workbrain.app.modules.scheduleconstraints.ScheduleConstraintHelper;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.model.WorkDetailList;
import com.workbrain.app.ta.ruleengine.CallFromConstants;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.util.DateHelper;
import com.workbrain.util.TimeZoneUtil;

public class WBIAGScheduleConstraint extends ScheduleConstraint {
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WBIAGScheduleConstraint.class);

	public WBIAGScheduleConstraint() {
	}

	/**
	 * checUdf1 constraint rule test checks for X consecutive days with shift durations of Y hours
	 * each day.
	 * @param ScheduleConstraintConext c
	 * @param String param - X,Y, true/false where X=consecutive days to check for, and Y length of shift each day in hours
	 * and true=use actual hours worked adjusted accordingly to DST, false=use shift length, not
	 * actual hours used
	 * @return true if consecutive days worked less than x and false otherwise */
	protected boolean checkUdf1(ScheduleConstraintContext c, String param)
			throws Exception {
		if (c.runAlertsOnly())
			return true;

		StringTokenizer paramTok = new StringTokenizer(param, ",");
		int days = 0;
		int shiftDuration = 0;
		boolean actualHours = false;
		
		if (paramTok.hasMoreElements())
			days = new Integer(paramTok.nextToken()).intValue();
		
		if (paramTok.hasMoreElements())
			shiftDuration = new Integer(paramTok.nextToken()).intValue();
		
		if (paramTok.hasMoreElements())
			actualHours = new Boolean(paramTok.nextToken()).booleanValue();	
		
		if (days==0 || shiftDuration==0)
			throw new Exception ("The number of consecutive days and duration of the shift must not be 0.");

		Date targetDate;
		if (c.getCalledFromLocation() == CallFromConstants.CALLED_FROM_CLOCK) {
			Date nowClient = c.convertForTZDiff(c.getNow(),
					TimeZoneUtil.SERVER_TO_CLIENT);
			ScheduleConstraintHelper.StartOfDayArgumentGenerator SODArgsGen = new ScheduleConstraintHelper.StartOfDayArgumentGenerator(
					nowClient, c.getSCRegistryGetter());
			targetDate = DateHelper.truncateToDays(SODArgsGen.getStartTime());
		} else {
			targetDate = c.getRuleData().getWorkSummary().getWrksWorkDate();
		}

		//same method is being called for all cases, get call from location passed
		//and handled by core
		int consecDays = this.consecutiveDaysWorked(targetDate, days - 1, c,
				shiftDuration, c.getCalledFromLocation(), actualHours);

		if (logger.isDebugEnabled()) {
			logger.debug("Checking checkConsecutiveDays.");
			logger.debug("checkConsecutiveDays: " + consecDays);
		}

		return consecDays < days;

	}

	/**
	 * Method returns the number of consecutive days with length shiftDuration
	 * @param Date targetDate
	 * @param int maxDaysToCheck: number of days to look back and look forward from target date
	 * @param ScheduleConstraintContext c
	 * @param int shiftDuration
	 * @param int callFrom: integer constant defining where the constraint is being called, i.e. MVS, shift trade
	 * @return the number of consecutive days worked*/
	private int consecutiveDaysWorked(Date targetDate, int maxDaysToCheck,
			ScheduleConstraintContext c, int shiftDuration, int callFrom, boolean actualHours)
			throws Exception {

		ScheduleConstraintContext.SCRegistryGetter regGetter = c
				.getSCRegistryGetter();

		//		the shift being introduced on the target date is shorter than the expected shift period.
		if (this.getMinutesWorkedForDay(targetDate, c, regGetter, callFrom,
				actualHours) < shiftDuration)
			return 0;

		int searchedDaysBack = 0;
		int searchedDaysForward = 0;

		//Count the number of consecutive work days with defined shiftDuration back from target date
		for (int i = 1; i <= maxDaysToCheck; i++) {
			Date calcDate = DateHelper.addDays(targetDate, -i);
			if (this.getMinutesWorkedForDay(calcDate, c, regGetter, callFrom,
					actualHours) < shiftDuration) {
				break;
			}
			searchedDaysBack++;
		}
		//Count the number of consecutive work days with defined shiftDuration forward.
		for (int i = 1; i <= maxDaysToCheck; i++) {
			Date calcDate = DateHelper.addDays(targetDate, i);
			if (this.getMinutesWorkedForDay(calcDate, c, regGetter, callFrom,
					actualHours) < shiftDuration) {
				break;
			}
			searchedDaysForward++;
		}
		//total consecutive days worked.
		return searchedDaysForward + searchedDaysBack + 1;
	}

	/**Method calculates minutes worked for all the work details that falls in that day.  This means that if
	 * a work detail crosses days, all the minutes of that work detail is counted for the day when the 
	 * detail starts.
	 * @param Date calcDate
	 * @param ScheduleConstraintContext c
	 * @param ScheduleConstraintContext.SCRegistryGetter regGetter: schedule constraint context registry parameter getter
	 * @param int callFrom: integer constant defining where the constraint is being called, i.e. MVS, shift trade
	 * @param boolean actualHours: false: counter core DST settings, true: use core DST settings
	 * @return minutes worked in a day for all work details that start in that day*/
	private int getMinutesWorkedForDay(Date calcDate,
			ScheduleConstraintContext c,
			ScheduleConstraintContext.SCRegistryGetter regGetter, int callFrom,
			boolean actualHours) throws SQLException {
		int minutesWorked = 0;

		WBData wb = c.getWBData();
		//loaded from calc sim employee if mvs/rtss
		WorkDetailList wdl = wb.getWorkDetails(calcDate, calcDate,
				WorkDetailData.DETAIL_TYPE, callFrom);

		if (wdl == null)
			return 0;

		for (int i = 0; i < wdl.size(); i++) {
			WorkDetailData wdd = wdl.getWorkDetail(i);
			if (logger.isDebugEnabled()) {
				logger.debug("Checking start and end time of work detail for date: "+ calcDate);
				logger.debug("Start Time: " + wdd.getWrkdStartTime());
				logger.debug("End Time: " + wdd.getWrkdEndTime());
			}
			if ((com.workbrain.app.ta.ruleengine.RuleHelper.isCodeInList(
					regGetter.getCSVFormattedTimeCodes(), wdd
							.getWrkdTcodeName()) == regGetter
					.isTimeCodeInclusive())
					&& (com.workbrain.app.ta.ruleengine.RuleHelper
							.isCodeInList(regGetter.getCSVFormattedHourTypes(),
									wdd.getWrkdHtypeName()) == regGetter
							.isHourTypeInclusive())) {
				minutesWorked = minutesWorked + wdd.getWrkdMinutes();
			}

			if (!actualHours) {
				Date dstAdjustDate = DateHelper.addDays(calcDate, 1);
				//if the next day is a DST transition day, test to see if the work
				//detail crosses DST, if it does, adjust accordingly
				if (TimeZoneUtil.isFallBackDate(dstAdjustDate) == true)
				{	
					if (TimeZoneUtil.isInDaylightSavingTime(wdd.getWrkdStartTime())
						&& !TimeZoneUtil.isInDaylightSavingTime(wdd.getWrkdEndTime()))
					{
						minutesWorked = minutesWorked - 60;
					}	
					
				}	

				else if (TimeZoneUtil.isSpringForwardDate(dstAdjustDate) == true)
				{	
					if (!TimeZoneUtil.isInDaylightSavingTime(wdd.getWrkdStartTime())
							&& TimeZoneUtil.isInDaylightSavingTime(wdd.getWrkdEndTime()))
						{
							minutesWorked = minutesWorked + 60;
						}	
				}	
			}

		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("Checking MinutesWorked.");
			logger.debug("Date: " + calcDate+ "Minutes Worked: "+minutesWorked);
		}

		return minutesWorked;
	}

}
