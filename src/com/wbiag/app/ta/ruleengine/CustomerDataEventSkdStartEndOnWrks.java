package com.wbiag.app.ta.ruleengine;

import java.util.ArrayList;
import java.util.Date;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.db.OverrideAccess;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

public class CustomerDataEventSkdStartEndOnWrks extends DataEvent {

	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CustomerDataEventSkdStartEndOnWrks.class);

	protected static String START_TOKEN_NAME = "WRKS_START_TIME";
    protected static String END_TOKEN_NAME = "WRKS_END_TIME";
    
    protected static final String DATE_FMT = "yyyyMMdd HHmmss";

    /**
     * Allows the class to be extended to use a different field.
     * @return
     */
    protected String getStartTokenName() {
    	return START_TOKEN_NAME;
    }
    protected String getEndTokenName() {
    	return END_TOKEN_NAME;
    }
    
    public void beforeWorkSummaryOverrideInsert(OverrideData od, DBConnection c) throws RuleEngineException {
		createScheduleOverride(od, c, getStartTokenName(), getEndTokenName());
    }

    /**
     * Helper method not tied to instance state.
     * If the override contains at least one of the specified tokens
     * in its new value field, this will duplicate the entire override
     * and turn it into a schedule override.
     * <br>
     * The override type is for the new override is set to:
     * OverrideData.SCHEDULE_SCHEDTIMES_TYPE
     * ; and the override new value is changed to:
     * "EMPSKD_ACT_START_TIME=Time(startTimeTokenName.value)
     * ;EMPSKD_ACT_END_TIME=Time(endTimeTokenName.value)"
     *
     * @param overrideData the override to be hijacked
     * @param dbConnection unused
     * @param startTimeTokenName the token in the override data new value
     *							 that represents the schedule start time
     * @param endTimeTokenName the token in the override data new value
     *						   that represents the schedule end time
     * @throws RuleEngineException if both tokens are present but empty
    */
    private void createScheduleOverride(OverrideData overrideData, DBConnection dbConnection, String startTimeTokenName, String endTimeTokenName) throws RuleEngineException {

		if(logger.isDebugEnabled()) {
			StringBuffer args = new StringBuffer(200);
			args.append("Passed parameters: ");
			args.append("\n overrideData=").append(String.valueOf(overrideData));
			args.append("\n startTimeTokenName=").append(String.valueOf(startTimeTokenName));
			args.append("\n endTimeTokenName=").append(String.valueOf(endTimeTokenName));
			logger.debug(args.toString());
		}

		// parse the start and end times
		OverrideData.OverrideToken startTimeToken = overrideData.getNewOverrideByName(startTimeTokenName);
		OverrideData.OverrideToken endTimeToken = overrideData.getNewOverrideByName(endTimeTokenName);
		boolean startTimeTokenExists = (startTimeToken != null && !StringHelper.isEmpty(startTimeToken.getValue()));
		boolean endTimeTokenExists = (endTimeToken != null && !StringHelper.isEmpty(endTimeToken.getValue()));

		if (logger.isDebugEnabled()) {
			logger.debug("startTimeTokenExists: " + startTimeTokenExists);
			logger.debug("endTimeTokenExists: " + endTimeTokenExists);
		}
		
		if (!startTimeTokenExists && !endTimeTokenExists) {
			return;
		}

		// The date on the token will default to the current system date.  We need it to
		// be the date that the override is applied to.
		Date startTime = null;
		Date endTime =  null;
		
		if (startTimeTokenExists) {
			startTime = DateHelper.parseDate(startTimeToken.getValue(), DATE_FMT);
			startTime = DateHelper.setTimeValues(overrideData.getOvrStartDate(), startTime);
		}
		if (endTimeTokenExists) {
			endTime = DateHelper.parseDate(endTimeToken.getValue(), DATE_FMT);
			endTime = DateHelper.setTimeValues(overrideData.getOvrStartDate(), endTime);
		}
		
		// increment the end time by one day
		// iff both start and end times are provided
		// and the end time is before the start time
		if (startTimeTokenExists && endTimeTokenExists
				&& DateHelper.compare(endTime, startTime) <= 0) {
			
				endTime = DateHelper.addDays(endTime, 1);
				logger.debug("Added one day to the end time");
		}
		
		// format the start and end times from DATE_FMT (the interface format)
		// into OVERRIDE_TIME_FORMAT_STR (the override format)
		StringBuffer newValue = new StringBuffer(200);
		if (startTimeTokenExists) {
			String startTimeTokenValue = DateHelper.convertDateString(startTime, OverrideData.OVERRIDE_TIME_FORMAT_STR);
			newValue.append(OverrideData.formatToken(EmployeeScheduleData.EMPSKD_ACT_START_TIME, startTimeTokenValue));
		}
		if (endTimeTokenExists) {
			String endTimeTokenValue = DateHelper.convertDateString(endTime, OverrideData.OVERRIDE_TIME_FORMAT_STR);
			newValue.append(OverrideData.formatToken(EmployeeScheduleData.EMPSKD_ACT_END_TIME, endTimeTokenValue));
		}
		if (newValue.length() > 0) {
			newValue.deleteCharAt(newValue.length() - 1);
			if (logger.isDebugEnabled()) {
				logger.debug("New value after substitution: " + newValue);
			}
		}

		// overwrite the override type id and override new value
		// iff a new value has been provided
		if(newValue.length() <= 0) {
			logger.debug("No new schedule override created.");
		
		} else {
			// No need for the start/end time tokens anymore.
			overrideData.removeTokenName(startTimeTokenName);
			overrideData.removeTokenName(endTimeTokenName);

			// If there are no other tokens besides the start/end times,
			// just modify this override to be a scheduled times ovr.
			if (overrideData.getNewOverrides() == null || overrideData.getNewOverrides().size() == 0) {
			
				overrideData.setOvrtypId(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
				overrideData.setOvrNewValue(newValue.toString());
				
				if (logger.isDebugEnabled()) {
					logger.debug("Override changed to a sched times ovr: " + String.valueOf(overrideData));
				}
			
			// There are other tokens besides the start/end times so need to create a new ovr.
			} else {
				// Update the existing override with just the tokens that are not the start/end times.
				overrideData.setOvrNewValue(OverrideData.createOverrideValue((ArrayList)overrideData.getNewOverrides()));

				// Create a new Scheduled Times Override.
				OverrideData schedTimesOverride = overrideData.duplicate();
				schedTimesOverride.setOvrtypId(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
				schedTimesOverride.setOvrNewValue(newValue.toString());
				

				// PMD rule violation: consider using OverrideBuilder
				// decision: it is to cumbersome and possibly error prone
				//           to copy all of the fields out of OverrideData
				//			 into a new instance of InsertOverride
				//			 just for the sake of using OverrideBuilder
				OverrideAccess overrideAccess = new OverrideAccess(dbConnection);
				try {
					overrideAccess.insert(schedTimesOverride, false);
				} catch(Exception ex) {
					logger.error(ex);
					throw new RuleEngineException("Could not insert new schedule override.");
				}

				if (logger.isDebugEnabled()) {
					logger.debug("New schedule override successfully inserted: " + String.valueOf(overrideData));
				}
			}
		}
	}
}
