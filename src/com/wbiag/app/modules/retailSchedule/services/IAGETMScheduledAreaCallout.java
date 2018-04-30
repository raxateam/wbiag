package com.wbiag.app.modules.retailSchedule.services;

import com.workbrain.util.callouts.DefaultETMScheduledAreaCallout;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.jsp.action.dateselection.DateRange;
import com.workbrain.server.jsp.locale.LocalizationDictionary;
import com.workbrain.util.DateHelper;
import com.workbrain.util.NestedRuntimeException;
//import com.workbrain.server.WorkbrainParametersRetriever;
import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;
//import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
//import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

public class IAGETMScheduledAreaCallout extends DefaultETMScheduledAreaCallout {
    public static Logger logger = Logger.getLogger(IAGETMScheduledAreaCallout.class);

    public static final String ETM_SHOW_SCHED_AREA_LANG_ID = "etmShowSchedAreaLanguageId";
    
    private HashMap scheduledAreasForDates = null;
    
    /**
     * Returns true if the schedule area should be shown on the ETM timesheets for shifts.
     * 
     * @param soContext         global context object used to access internal SO data.
     */
    public boolean showScheduleAreaOnETMTimesheets_PreAction() {
        return true;
    }
    
    /**
     * Loads shift locations for a range of dates & employee.
     * Variables required in LocalData:
     * 	ScheduleCallout.ETM_SHOW_SCHED_AREA_START_DATE
     * 	ScheduleCallout.ETM_SHOW_SCHED_AREA_END_DATE
     * 	ScheduleCallout.ETM_SHOW_SCHED_AREA_EMP_ID
     * 
     * @param soContext         global context object used to access internal SO data.
     */
    public void showScheduleAreaOnETMTimesheets_Load(SOData soContext) {
        Date start = (Date)DateHelper.truncateToDays((Date)soContext.getLocalVariable(ETM_SHOW_SCHED_AREA_START_DATE));
        Date end = (Date)DateHelper.truncateToDays((Date)soContext.getLocalVariable(ETM_SHOW_SCHED_AREA_END_DATE));
        int empId = ((Integer)soContext.getLocalVariable(ETM_SHOW_SCHED_AREA_EMP_ID)).intValue();
        PreparedStatement ps = null;
        ResultSet rs = null;
        scheduledAreasForDates = new HashMap();
        
        try {
            String sql = new StringBuffer().
            append("select sosd.shftdet_start_time, sosd.shftdet_end_time, soss.skdshft_date, sosd.shftdet_wrk_loc, sosg.wbt_id from ").
            append("so_schedule sos, so_shift_detail sosd, so_scheduled_shift soss, so_schedule_group sosg where ").
            append("sosd.emp_id = ? and ").
            append("sos.skd_id = sosd.skd_id and ").	
            append("sos.skd_from_date <= ? and ").
            append("sos.skd_to_date >= ? and ").
            append("sos.skd_published = 1 and ").
            append("sosd.skdshft_id = soss.skdshft_id and ").
            append("soss.skd_id = sos.skd_id and ").
            append("sosg.skdgrp_id = sos.skdgrp_id").toString();
            
            ps = soContext.getDBconnection().prepareStatement(sql);
        	ps.setInt(1, empId);
        	ps.setTimestamp(2, new Timestamp(end.getTime()));
        	ps.setTimestamp(3, new Timestamp(start.getTime()));
        	rs = ps.executeQuery();
        	
        	while (rs.next()) {
        	    List scheduleDetails = null;
        	    Date dateKey = null;
        	    DateRange schedDetailRange = null;
        	    Integer scheduledAreaTeamId = null;
        	    Date startTime = (Date)rs.getTimestamp(1);
        	    Date endTime = (Date)rs.getTimestamp(2);
        	    Date shiftDate = (Date)rs.getTimestamp(3);
        	    int teamId = rs.getInt(4);
        	    
        	    if (rs.wasNull()) {
        	        teamId = rs.getInt(5);
        	    }
        	    
        	    dateKey = DateHelper.truncateToDays(shiftDate);
        	    scheduledAreaTeamId = new Integer(teamId);
        	    startTime = DateHelper.setTimeValues(shiftDate, startTime);
        	    endTime = DateHelper.setTimeValues(shiftDate, endTime);
        	    schedDetailRange = new DateRange(startTime, endTime);
        	    
        	    if (scheduledAreasForDates.containsKey(dateKey)) {
        			scheduleDetails = (List)scheduledAreasForDates.get(dateKey);
        		} else {
        		    scheduleDetails = new ArrayList();
        		}
        	    scheduleDetails.add(new Object[] {
        	    		scheduledAreaTeamId,
        	    		schedDetailRange
        	    	}
        	    );
        		scheduledAreasForDates.put(dateKey, scheduleDetails);
        	}
        } catch (Exception e) {
            logger.error(e);
            throw new NestedRuntimeException(e);
        } finally {
            SQLHelper.cleanUp(ps, rs);
        }
    }
    
    
    /**
     * Get the location for the shift.
     * 	ScheduleCallout.ETM_SHOW_SCHED_AREA_SHIFT_START
     * 	ScheduleCallout.ETM_SHOW_SCHED_AREA_SHIFT_END
     * 	ScheduleCallout.ETM_SHOW_SCHED_AREA_LOCATION		(returned value)
     * 
     * @param soContext         global context object used to access internal SO data.
     */
    public void showScheduleAreaOnETMTimesheets_ShiftLocation(SOData soContext) {
        try {
	        DBConnection conn = soContext.getDBconnection();
    	    StringBuffer returnedString = new StringBuffer();
	        //HashSet teamIdsUsed = new HashSet();
	        int teamsAdded = 0;

	        Date shiftStart = (Date)soContext.getLocalVariable(ETM_SHOW_SCHED_AREA_SHIFT_START);
	        Date shiftEnd = (Date)soContext.getLocalVariable(ETM_SHOW_SCHED_AREA_SHIFT_END);
	        DateRange shiftRange = new DateRange(shiftStart, shiftEnd);
	        Date today = DateHelper.truncateToDays(shiftStart);
	    	List scheduleAreas = (List)scheduledAreasForDates.get(today);
	    	
	        Integer languageId = (Integer)soContext.getLocalVariable(ETM_SHOW_SCHED_AREA_LANG_ID);
	        int langId = 1;
	        
	        if (languageId != null) {
	        	langId = languageId.intValue();
	        }
	
	    	if (scheduleAreas != null) {
	            returnedString.append("<div class='scheduledArea'>");
	    		for (int x = 0; x < scheduleAreas.size(); x++) {
	    		    Object[] wrapper = (Object[])scheduleAreas.get(x);
	    		    Integer teamId = (Integer)wrapper[0];
	    		    DateRange range = (DateRange)wrapper[1];
	    		    if (range.overlaps(shiftRange)) {
	    		        String locTeamName = getLocalizedTeamNameFromTeamId(conn, teamId.intValue(), langId);
	    		        if (teamsAdded != 0) {
	    		            returnedString.append(", ");
	    		        }
	    		        returnedString.append(locTeamName);
	    		        teamsAdded++;
	    		    }
	    		}
	    		returnedString.append("</div>");
	    	}
	    	soContext.updateLocalVariable(ETM_SHOW_SCHED_AREA_LOCATION, returnedString.toString());
        } catch (Exception e) {
            logger.error(e);
        	throw new NestedRuntimeException(e);
        }
    }
    
    private String getLocalizedTeamNameFromTeamId(DBConnection conn, int teamId, int langId) {
        LocalizationDictionary ld = LocalizationDictionary.get();
        
        try {
        	CodeMapper cm = CodeMapper.createCodeMapper(conn);
            
            String teamName = null, locTeamName = null;
            
        	teamName = cm.getWBTeamById(teamId).getWbtName();
        	if(teamName == null) {
        		throw new RuntimeException("Could not find team with wbt_id = " + teamId);
        	}
        	locTeamName = ld.localizeData(conn, teamName, "WORKBRAIN_TEAM", "WBT_NAME", langId);
        	if (locTeamName == null || locTeamName.equals("null") || locTeamName.equals("")) {
                return teamName; 
        	} else {
        	    return locTeamName;
        	}
        } catch (Exception e) {
            logger.error(e);
            throw new NestedRuntimeException(e);
        }
    }
}