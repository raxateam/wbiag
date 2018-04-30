package com.wbiag.app.ta.conditions;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.workbrain.app.ta.ruleengine.Condition;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;

/**
 * @author schang
 * 
 * Checks to see if yesterday/today/tomorrow is the employee's termination date, 
 * if so, returns true, if not returns false.
 *@deprecated As of 5.0.2.0, use core classes 
 */

public class IsTerminationDateCondition extends Condition {

	private final static Logger logger = Logger.getLogger(IsTerminationDateCondition.class);
	private final static String COMPONENT_NAME = "WBIAG: Is Termination Date Condition";
	public final static String PARAM_DATES = "Day to Evaluate";
	public final static String PARAM_DATES_TODAY = "Today";
	public final static String PARAM_DATES_TOMORROW = "Tomorrow";
	public final static String PARAM_DATES_YESTERDAY = "Yesterday";

	/**
	 * Creates a new instance of IsTerminationDateCondition 
	 */
	public IsTerminationDateCondition(){
	}
	
	/**
     * evaluate
     */
    public boolean evaluate(WBData wBData, Parameters parameters) throws java.lang.Exception {
        
    	boolean result = false;
    	int difference = -1;
        String dayToEvaluate = parameters.getParameter(PARAM_DATES, PARAM_DATES_TODAY);
        
        if (PARAM_DATES_TOMORROW.equalsIgnoreCase(dayToEvaluate)) difference = -1;
        else if (PARAM_DATES_YESTERDAY.equalsIgnoreCase(dayToEvaluate)) difference = 1;
        else if (PARAM_DATES_TODAY.equalsIgnoreCase(dayToEvaluate)) difference = 0;
    	
        if(DateHelper.dateDifferenceInDays(wBData.getWrksWorkDate(), wBData.getEmpTerminationDate()) == difference){
            result = true;
        }
        
        return result; 
    }
	
    /**
     * @return component name
     */
	public String getComponentName() {
        return  COMPONENT_NAME;
    }
    
    /**
     * getParameterInfo
     */
    public List getParameterInfo(DBConnection dBConnection) {
        List result = new ArrayList();
        RuleParameterInfo rpiDayToEvaluateChoice = new RuleParameterInfo(PARAM_DATES, RuleParameterInfo.CHOICE_TYPE, true);
        rpiDayToEvaluateChoice.addChoice(PARAM_DATES_YESTERDAY);
        rpiDayToEvaluateChoice.addChoice(PARAM_DATES_TODAY);
        rpiDayToEvaluateChoice.addChoice(PARAM_DATES_TOMORROW);        
        result.add(rpiDayToEvaluateChoice);        
        
        return result;
    }
    
    /**
	 * @param s
	 */
	private static void log(Exception s) {
	    if (logger.isEnabledFor(Level.DEBUG)){
	        logger.debug(s);
	    }
	}
}

