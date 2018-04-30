package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.io.Serializable;
import java.util.List;

import com.workbrain.util.StringHelper;

abstract public class ErrorDetectionRule implements Serializable {

	public static final Integer BUG_TYPE = new Integer(0);
	public static final Integer SYSTEM_TYPE = new Integer(1);
	public static final Integer FORECAST_TYPE = new Integer(2);
	public static final Integer SCHEDULE_TYPE = new Integer(3);
	
    String ruleName; // The name of the rule
    
    public ErrorDetectionRule(){
    	
    }
    
    abstract public Integer getRuleType();
    
    abstract public ErrorDetectionScriptResult detect(ErrorDetectionContext context);
    
    /**
     * Returns the localized name of the rule, based on an entries in the
     * WB_ERR_MSG_LOC_DAT table.
     *
     * @return The localized name of the rule
     */
    abstract protected String getLocalizedRuleName();

    /**
     * Returns the name of the rule.
     *
     * @return The name of the rule
     */
     public String getRuleName() {
         if (StringHelper.isEmpty(ruleName)) {
             ruleName = getLocalizedRuleName();
         }
         return ruleName;
     }

}
