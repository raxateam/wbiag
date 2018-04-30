package com.wbiag.app.modules.retailSchedule.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.exceptions.SQLRetailException;
import com.workbrain.app.modules.retailSchedule.model.Schedule;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCallout;
import com.workbrain.app.modules.retailSchedule.services.PreProcessor.ShiftChoice;
import com.workbrain.app.modules.retailSchedule.services.model.CalloutHelper;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;

public class TransitionByStoreProcess {

    public static Logger logger = Logger.getLogger(TransitionByStoreProcess.class);
    
    public static final String SO_USE_TRANSITION_SHIFTS ="system/modules/scheduleOptimization/SO_USE_TRANSITION_SHIFTS";
    public static final String TRANSITION_BY_STORE ="system/customer/TR_TRANSITION_BY_STORE";
    public static final String TRANSITION_BY_STORE_WBT_FLAG ="system/customer/TR_TRANSITION_BY_STORE_WBT_FLAG";    
    public static final String ALLOWABLE_TRANSITION_MULTIPLE ="system/customer/TR_TRANSITION_MULTIPLE";
    public static final String MIN_TRANSITION_INTERVALS ="system/customer/TR_MIN_TRANSITION_INTERVALS";
    public static final String MAX_TRANSITION_INTERVALS ="system/customer/TR_MAX_TRANSITION_INTERVALS";

	/**@todo
	 * change these values to draw from the registry
	 */
	private double DEFAULT_ALLOWABLE_TRANSITION_MULTIPLE = 1;
	private double DEFAULT_MIN_TRANSITION_INTERVALS = 1;
	private double DEFAULT_MAX_TRANSITION_INTERVALS = 40;

	public TransitionByStoreProcess() {	  
	}
	
	/*
     * Retrieve workbrain team settings to determine if the store should have transitions
     */	
	public boolean isTransition(SOData soContext) throws CalloutException {
	    boolean ret = false;
	    try {
	        ret = processTransitionByStore(soContext);
	    }
	    catch (Exception e) {
	        if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) logger.error("Error in isTransition" , e);
	        throw new CalloutException("Error in isTransition" , e);
	    }
	    return ret;
   	}

	private boolean processTransitionByStore(SOData soContext) throws RetailException,SQLException{
        boolean isTransitionGlobal = false;
        boolean isTransitionByStore = false;
        boolean isCurrentStoreTransition = false;
        
        int skdgrpId = -1;
        
        // core check for global transitions setting
        String strRunTransitionShifts = (String)Registry.getVarString(SO_USE_TRANSITION_SHIFTS , "false");
        isTransitionGlobal =(strRunTransitionShifts != null && strRunTransitionShifts.equalsIgnoreCase("true")); 
        
        if (isTransitionGlobal == false)
        {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  logger.debug("no transitions at all, return false");
            return isTransitionGlobal;
        }
        
        // Global transitions are enabled.  Check if transitions by store is enabled.
        // Determine if transitions should be filtered by store
        String strStoreTransitionShifts = Registry.getVarString(TRANSITION_BY_STORE, "false");
        isTransitionByStore =(strStoreTransitionShifts != null && strStoreTransitionShifts.equalsIgnoreCase("true")); 
        
        if(isTransitionByStore == false)
        {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  logger.debug("transition does not filter by store.  Return global transition setting.");
            return isTransitionGlobal;
        }
        
        String wbtFlag = Registry.getVarString(TRANSITION_BY_STORE_WBT_FLAG , "wbt_flag1");
        skdgrpId = soContext.getSchedule().getSkdgrpId();
        
        PreparedStatement ps = null;
        ResultSet rs = null;               
        try {
        
            DBConnection conn = soContext.getDBconnection();
            String sql = "SELECT wbt." + wbtFlag
                + " FROM workbrain_team wbt, so_schedule_group sg "
                + " WHERE wbt.wbt_id = sg.wbt_id and sg.skdgrp_id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1,skdgrpId);
            rs = ps.executeQuery();
            
            String isTransition = "";
            
            while(rs.next())
            {
                isTransition = rs.getString(1);
            }
            
            // If the setting is "N", set isCurrentStoreTransition to false.  Otherwise, set isCurrentStoreTransition to true.
            isCurrentStoreTransition = !(isTransition != null && isTransition.equalsIgnoreCase("N"));
        }
        finally
        {
            SQLHelper.cleanUp(ps);
            SQLHelper.cleanUp(rs);
        }
                
        return isCurrentStoreTransition;
	    
	}
	
	/*
	 * Determine if the transition shift is valid. 
	 * Valid transition shifts are:
	 * 
	 * 	+ Shift transition <= 2.5 hours, and
	 * 	+ Shift transitions % multiple == 0
	 *
	 *******Logic******
	 * 
     * The shortest shift will be the "transition" shift.  This will be used to validate the shift choice.
     * 
     * Choice 1 shift length is unchanged by the transition.
     * Choice 2 shift length is spliced by the start or end time of shift1.
     * No guarantee that Choice 1 is before choice 2, must perform logic to get the right shift lengths.
     * 
     * To Check:
     * 1.  TmpSchd_Choices: 
     * 		- Column A
     * 			o Format: Day.Choice1 location.startInterval.totalLength.{ShiftInfo row number}
     * 		- Column J
     * 			o Format: Choice1__Choice2
     * 			o Choice1 and Choice2 is in the format: Day.{positionInfo}.ET_{###}.startInterval.shiftLength
     * 
     * 2.  TmpSchd_ShiftInfo
     * 		- Column A
     * 			o Format: Day.Choice1 location.startInterval.totalLength.{ShiftInfo row number}
     * 			o Matches with TmpSchd_Choices Column A
     * 		- Column I
     * 			O Choice 1 shift length
     * 		- Column N
     * 			o Choice 2 shift length
     */ 

	public boolean buildTransitionShiftPreAction(SOData soContext) throws CalloutException 
	{
		double transitionMultiple = 0;
		double transitionMinLength = 0;
		double transitionMaxLength = 0;

		String strTransitionMultiple = "";
		String strTransitionMinLength = "";
		String strTransitionMaxLength = "";
		
		boolean isCorrectInterval;
		boolean isCorrectLength;
		
		isCorrectInterval = false;
		isCorrectLength = false;
		
	    ShiftChoice choice1 = CalloutHelper.getTransitionShiftChoice1(soContext, ScheduleCallout.BUILD_TRANSITION_SHIFTS);
	    ShiftChoice choice2 = CalloutHelper.getTransitionShiftChoice2(soContext, ScheduleCallout.BUILD_TRANSITION_SHIFTS);
	    
	    strTransitionMultiple = Registry.getVarString(ALLOWABLE_TRANSITION_MULTIPLE,"");
	    strTransitionMinLength = Registry.getVarString(MIN_TRANSITION_INTERVALS,"");
	    strTransitionMaxLength = Registry.getVarString(MAX_TRANSITION_INTERVALS,"");
	    
	    // Use defaults for registries that are not configured.
	    // Throw error if the registries exists, but are not valid numbers.
	    if(strTransitionMultiple == null || strTransitionMultiple.equals(""))
	    {
	    	transitionMultiple = DEFAULT_ALLOWABLE_TRANSITION_MULTIPLE;
	    }
	    else
	    {
	    	try
	    	{
	    		transitionMultiple = Double.parseDouble(strTransitionMultiple);
	    	}
	    	catch(NumberFormatException e)
	    	{
	    		logger.error("Error parsing registry: "+ ALLOWABLE_TRANSITION_MULTIPLE,e);
	    		throw e;
	    	}
	    }
	    
	    if(strTransitionMinLength == null || strTransitionMinLength.equals(""))
	    {
	    	transitionMinLength = DEFAULT_MIN_TRANSITION_INTERVALS;
	    }
	    else
	    {
	    	try
	    	{
	    		transitionMinLength = Double.parseDouble(strTransitionMinLength);
	    	}
	    	catch(NumberFormatException e)
	    	{
	    		logger.error("Error parsing registry: "+ MIN_TRANSITION_INTERVALS,e);
	    		throw e;
	    	}
	    }
	    
	    if(strTransitionMaxLength == null || strTransitionMaxLength.equals(""))
	    {
	    	transitionMaxLength = DEFAULT_MAX_TRANSITION_INTERVALS;
	    }
	    else
	    {	    	
	    	try
	    	{
	    		transitionMaxLength = Double.parseDouble(strTransitionMaxLength);
	    	}
	    	catch(NumberFormatException e)
	    	{
	    		logger.error("Error parsing registry: "+ MAX_TRANSITION_INTERVALS,e);
	    		throw e;
	    	}
	    }
	    
	    double firstChoiceLength = 0;
	    double secondChoiceLength = 0;
	    
	    double shortShift = 0;

	    double firstStartTime = 0;
	    double firstEndTime = 0;
	    
	    double secondStartTime = 0;
	    double secondEndTime = 0;
	    
	    /*
	     * Determine length of choices.  See method comments for business logic and temp file checks.
	     * 
	     */
	    
	    firstStartTime = choice1.m_fStartTimeOffset;
	    firstEndTime = choice1.m_fEndTime;
	    
	    secondStartTime = choice2.m_fStartTimeOffset;
	    secondEndTime = choice2.m_fEndTime;
	    
	    // firstChoiceLength is always choice1.end - choice1.start
	    firstChoiceLength = firstEndTime - firstStartTime; 
	    
	    // second choice is always split by choice1.
	    
	    if(firstStartTime <= secondStartTime)
	    {
	    	// Choice 2 shift is split by Choice 1 end time
	    	secondChoiceLength = secondEndTime - firstEndTime;
	    }
	    else
	    {
	    	// Choice 2 shift is split by Choice 1 start time
	    	secondChoiceLength = firstStartTime - secondStartTime;
	    }
	    
	    // determine shortest shift choice
	    if(firstChoiceLength > secondChoiceLength)
	    {
	    	shortShift = secondChoiceLength;
	    }
	    else
	    {
	    	shortShift = firstChoiceLength;
	    }
	    
	    // if the shorter shift is a valid multiple, proceed.  
	    isCorrectInterval = validateTransitionMultiple(shortShift, transitionMultiple);
	    
	    // If the shorter shift is within size, it is a valid transition shift.
	    isCorrectLength = (validateTransitionLength(shortShift, transitionMinLength, transitionMaxLength));

	    return (isCorrectInterval && isCorrectLength);
	}
	
	private boolean validateTransitionLength(double choiceInterval, double min, double max)
	{
	    // if the transitions are between 15-150 minutes in length (1-10 intervals), the transition is valid
	    if ((choiceInterval >= min) && (choiceInterval <= max))
	    {
	    	return true;
	    }
	    return false;
	}
	
	private boolean validateTransitionMultiple(double choiceInterval, double intervalMultiple)
	{
		return (choiceInterval % intervalMultiple == 0);
	}
}

