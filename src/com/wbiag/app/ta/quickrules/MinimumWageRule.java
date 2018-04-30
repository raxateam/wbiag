/*
 * Created on Mar 9, 2006
 *
 * State Pay Rules Project
 * Minimum Wage Rule
 * Description: Raise exception error if employee is paid below the 
 * state/federal minimum wage.
 * 
 */
package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.wbiag.app.ta.db.*;
import com.wbiag.app.ta.model.*;
import java.util.*;
import com.workbrain.util.StringHelper;

/**
 *  Title:        Minimum Wage Rule
 *  Description:  Ensure that employee is paid atleast the higher of the state/federal minimum wage
 *  Copyright:    Copyright (c) 2006
 *  Company:      Workbrain Inc
 *  TT: 1294
 *
 *@deprecated As of 5.0.2.0, use core classes 
 *@author     Shelley Lee
 *@version    1.0
 */

public class MinimumWageRule extends Rule
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(MinimumWageRule.class);
	/**@return the name of this custom rule */
	public String getComponentName(){		
		return "WBIAG: Minimum Wage Rule";
	}
	
	/**@return information indicating how to use this rule */
	public List getParameterInfo(DBConnection conn){
		//get Federal Minimum Wage Code
		ArrayList result = new ArrayList();
		result.add(new RuleParameterInfo("FederalMinWageCode", RuleParameterInfo.STRING_TYPE));
		
		//add flag to see if we should ignore zero dollar pay rate
		RuleParameterInfo rpi = new RuleParameterInfo("IgnoreZeroPayRate", RuleParameterInfo.CHOICE_TYPE,true);
        rpi.addChoice("TRUE");  //default
        rpi.addChoice("FALSE");
        result.add(rpi);
		
        return result;
	}
	
	/**
	 * * Validates that an employee's worked rate is not below the required minimum wage
	 * *@throws exception if an employee has a wrkd_rate lower than the required minimum wage 
	 * */
	
	 public void execute(WBData wbData, Parameters parameters) throws Exception {
		 
		 String fedMinWgeCode = parameters.getParameter("FederalMinWageCode"); 
		 
		 //285_STATE.xml inserts FED minimum wage into WBIAG_STATE tables by default
		 if (StringHelper.isEmpty(fedMinWgeCode)){
			 fedMinWgeCode = "FED";
		 }
		 
		 String ignoreZeroPayRate = parameters.getParameter("IgnoreZeroPayRate");
		 
		 if (StringHelper.isEmpty(ignoreZeroPayRate)){
			 ignoreZeroPayRate = "TRUE";
		 }
		 
		 DBConnection conn = wbData.getDBconnection();
		 double state_minwge;
		 double fed_minwge;
		 
		 WbiagStateCache cache = WbiagStateCache.getInstance();
	     
		 // get employee's state minimum wage using WbiagStateCache object
	     WbiagStateMinwgeData stateMD = cache.getMinWageByEmpEffDate(conn, wbData.getRuleData().getEmployeeData(), wbData.getWrksWorkDate());	     
	     
	     if (stateMD == null)
	     {
	    	 state_minwge = 0;	    	 
	     }
	     else
	     {
	    	 state_minwge = stateMD.getWistmMinWage();
	     }
	     
		 //get federal minimum wage using WbiagStateCache object
	     WbiagStateMinwgeData fedMD = cache.getMinWageByStateEffDate(conn,fedMinWgeCode,wbData.getWrksWorkDate()); 
	     if (fedMD == null)
	     {
	    	 fed_minwge = 0;	    	 
	     }
	     else
	     {
	    	 fed_minwge = fedMD.getWistmMinWage(); 
	     }
	     
	     //we pay higher of the two
		 double compareRate = Math.max(state_minwge, fed_minwge);
		 
		 //loop through current day's work details
		 for (int i = 0; i < wbData.getRuleData().getWorkDetailCount(); i++)
		 {
			 //if wrked_rate = 0, ignore. UAT,LE have wrked_rate =0
			 if ("TRUE".equalsIgnoreCase(ignoreZeroPayRate) && wbData.getRuleData().getWorkDetail(i).getWrkdRate() == 0)
				 continue;			 
			 
			 //if wrkd_rate < compareRate
			 if (wbData.getRuleData().getWorkDetail(i).getWrkdRate() < compareRate){
			     if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
			    	 logger.debug("Employee Pay Rate :" + wbData.getRuleData().getWorkDetail(i).getWrkdRate());
			     	 logger.debug("Employee pay rate below mandatory rate " + String.valueOf(compareRate));
			     }
				 throw new Exception( "Employee rate " + wbData.getRuleData().getWorkDetail(i).getWrkdRate() + " < mandatory rate " + String.valueOf(compareRate));
		
			 }
		}
		 
	 }
}
