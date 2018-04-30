package com.wbiag.app.modules.pts;

import java.sql.SQLException;
import java.util.Date;

import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.model.Schedule;

import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain.util.callouts.scheduling.DefaultScheduleEditCallout;
import com.workbrain.util.callouts.scheduling.ScheduleEditContext;

/** 
 * Title:			PTS Schedule Edit Callout			
 * Description:		Updates PTS whenever there is a schedule change
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		May 30, 2005
 * @author         	Kevin Tsoi
 */
public class PTSScheduleEditCallout extends DefaultScheduleEditCallout
{    
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger( PTSScheduleEditCallout.class );

    /**
     * Updates PTS to account for the schedule changes
     * 
     * @param scheduleEditContext - context object
     * @return 
     * @throws CalloutException
     */    
    public void scheduleEditPostAction(ScheduleEditContext context) 
    	throws CalloutException 
    {    	
        PTSProcessor ptsProcessor = null;
        DBConnection conn = null;
        Schedule oSchedule = null;
        Date today = null;
        Date startDate = null;
        
        //update params
        int skdGrpId = 0;
        int offset = 0;
        int dailyOffset = 0;
        boolean dayOrWeek = false;
        boolean actualCost = false;
        boolean actualEarnings = false;
        boolean budgetCost = false;
        boolean budgetEarnings = false;
        String timeCodes = null;
        boolean timeCodesInclusive = false;
        String hourTypes = null;
        boolean hourTypesInclusive = false;
        
        try
        {	        	                                     
            conn = DBInterface.getCurrentConnection();
	        ptsProcessor = new PTSProcessor(conn);
	        oSchedule = (Schedule)context.get(ScheduleEditContext.SCHEDULE_OBJECT_KEY);
	        	        
	        today = DateHelper.truncateToDays(new Date());
	        startDate = oSchedule.getSkdFromDate().getTime();	 	        	        
	        
	        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) 
	        {
	            logger.debug("today: "+today);
	            logger.debug("startDate: "+startDate);
	        }	        
	        
	        //only recalc pts for schedule edits after today
	        if(!today.before(startDate))
	        {
	            return;
	        }	
	        
	      	//determine offset
	        offset = DateHelper.getDifferenceInDays(startDate, today)-2;
	        
	        //set pts parameters	        
	        dayOrWeek = true;
	        actualCost = true;
	        timeCodes = PTSHelper.getRegistryValue(PTSHelper.REG_PTS_TIMECODES);
	        hourTypes = PTSHelper.getRegistryValue(PTSHelper.REG_PTS_HOURTYPES);
	        timeCodesInclusive = true;
	        hourTypesInclusive = true;
	        skdGrpId = PTSHelper.getParentStore(oSchedule.getSkdgrpId());	        	        

	        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) 
	        {
	            logger.debug("skdGrpId: "+skdGrpId);
	            logger.debug("offset: "+offset);
	            logger.debug("timeCodes: "+timeCodes);
	            logger.debug("hourTypes: "+hourTypes);	            
	        }
	        	        
	        ptsProcessor.updatePTS(skdGrpId, offset, dailyOffset, dayOrWeek, actualCost, actualEarnings, budgetCost, budgetEarnings, timeCodes, timeCodesInclusive, hourTypes, hourTypesInclusive);	        
	        conn.commit();	        	        
        }
        catch(Throwable t)
        {            
            try
            {
                if(conn != null)
                {
                    conn.rollback();    
                }                
            }
            catch(SQLException ex)
            {
                logger.error(ex);                
            }
            logger.error(t);     
            throw new CalloutException(t);
        } 
    }    
}
