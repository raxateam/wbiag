package com.wbiag.app.scheduler.tasks;

import java.util.Map;

import com.wbiag.app.modules.pts.PTSProcessor;
import com.workbrain.app.scheduler.enterprise.AbstractScheduledJob;
import com.workbrain.sql.DBConnection;

/** 
 * Title:			PTS Process Task
 * Description:		Task which performs Payroll to Sales processing.
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Apr 27, 2005
 * @author         	Kevin Tsoi
 */
public class PTSProcessTask extends AbstractScheduledJob
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger( PTSProcessTask.class );
    private static final String PAGE_LOCATION = "/jobs/pts/PTSProcessTaskParams.jsp";
    private static final String MESSAGE_JOB_OK = "Job ran successfully.";
    private static final String MESSAGE_JOB_FAILED = "Job failed.";     
    private static final String PARAM_CHECKBOX_YES = "Y";
    private static final String PARAM_DROPDOWN_WEEK = "2";
    
    public static final String PARAM_SKDGRP_ID = "skdGrpId";
    public static final String PARAM_OFFSET = "offset";
    public static final String PARAM_DAILY_OFFSET = "dailyOffset";
    public static final String PARAM_DAY_OR_WEEK = "dayOrWeek";
    public static final String PARAM_ACTUAL_COST = "actualCost";
    public static final String PARAM_BUDGET_COST = "budgetCost";
    public static final String PARAM_ACTUAL_EARNINGS = "actualEarnings";
    public static final String PARAM_BUDGET_EARNINGS = "budgetEarnings";
    public static final String PARAM_TIMECODES = "timeCodes";
    public static final String PARAM_HOURTYPES = "hourTypes";
    public static final String PARAM_TIMECODESINCLUSIVE = "timeCodesInclusive";
    public static final String PARAM_HOURTYPESINCLUSIVE = "hourTypesInclusive";
    
    
        
    
    public Status run(int taskID, Map params)
    	throws Exception
    {        
        DBConnection conn = null;
        PTSProcessor ptsProcessor = null;
        
        int skdGrpId = 0;
        int offset = 0;
        int dailyOffset = 0;
        boolean dayOrWeek = false;
        boolean actualCost = false; 
        boolean budgetCost = false; 
        boolean actualEarnings = false;
        boolean budgetEarnings = false;
        boolean timeCodesInclusive = false;
        boolean hourTypesInclusive = false;
        String timeCodes = null;
        String hourTypes = null;        
        
        //set fields from input parameters
        skdGrpId = Integer.parseInt((String)params.get(PARAM_SKDGRP_ID));
        offset = Integer.parseInt((String)params.get(PARAM_OFFSET));
        dailyOffset = Integer.parseInt((String)params.get(PARAM_DAILY_OFFSET));
        if(PARAM_DROPDOWN_WEEK.equals((String)params.get(PARAM_DAY_OR_WEEK)))
        {
            dayOrWeek = true;
        }
        else
        {
            dayOrWeek = false;
        }
        if(PARAM_CHECKBOX_YES.equals((String)params.get(PARAM_ACTUAL_COST)))
        {
            actualCost = true;
        }
        else
        {
            actualCost = false;
        }      
        if(PARAM_CHECKBOX_YES.equals((String)params.get(PARAM_BUDGET_COST)))
        {
            budgetCost = true;
        }
        else
        {
            budgetCost = false;
        }
        if(PARAM_CHECKBOX_YES.equals((String)params.get(PARAM_ACTUAL_EARNINGS)))
        {
            actualEarnings = true;
        }
        else
        {
            actualEarnings = false;
        }
        if(PARAM_CHECKBOX_YES.equals((String)params.get(PARAM_BUDGET_EARNINGS)))
        {
            budgetEarnings = true;
        }
        else
        {
            budgetEarnings = false;
        }                     
        timeCodes = (String)params.get(PARAM_TIMECODES);
        if(PARAM_CHECKBOX_YES.equals((String)params.get(PARAM_TIMECODESINCLUSIVE)))
        {
            timeCodesInclusive = true;
        }
        else
        {
            timeCodesInclusive = false;
        }         
        hourTypes = (String)params.get(PARAM_HOURTYPES);      
        if(PARAM_CHECKBOX_YES.equals((String)params.get(PARAM_HOURTYPESINCLUSIVE)))
        {
            hourTypesInclusive = true;
        }
        else
        {
            hourTypesInclusive = false;
        }
        try
        {
	        conn = getConnection();	        
	        
	        ptsProcessor = new PTSProcessor(conn);
	        ptsProcessor.updatePTS(skdGrpId, offset, dailyOffset, dayOrWeek, actualCost, actualEarnings, budgetCost, budgetEarnings, timeCodes, timeCodesInclusive, hourTypes, hourTypesInclusive);	        
	        conn.commit();	        	        
        }
        catch(Exception e)
        {            
            conn.rollback();
            logger.error(e);
            throw e;
        }                
        return jobOk(MESSAGE_JOB_OK);
    }
    
    public String getTaskUI()
    {
        return PAGE_LOCATION;
    }
}
