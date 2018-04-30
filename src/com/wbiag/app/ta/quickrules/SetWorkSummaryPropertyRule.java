package com.wbiag.app.ta.quickrules;

import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;

/**
 * Simple SetWorkSummaryAttribute Rule <br>
 * Set Work Summary Fields to given value for ELIGIBLE_PROPS
 */
public class SetWorkSummaryPropertyRule extends Rule {

	private static final Logger logger = Logger.getLogger(SetWorkSummaryPropertyRule.class);

	public static final String PARAM_WRKS_PROPERTY = "WrksProperty";
	public static final String PARAM_WRKS_VALUE = "WrksValue";
	public static final List ELIGIBLE_PROPS = new ArrayList(); 
	static {
        for (int i=1;i<=10;i++) {
            ELIGIBLE_PROPS.add("wrksFlag" + i);
        }
        for (int i=1;i<=10;i++) {
            ELIGIBLE_PROPS.add("wrksUdf" + i);
        }           
        ELIGIBLE_PROPS.add("wrksComments");           
        ELIGIBLE_PROPS.add("wrksDesc");
        ELIGIBLE_PROPS.add("wrksAuth");         
        ELIGIBLE_PROPS.add("wrksAuthBy");        
	}
	
	public List getParameterInfo(DBConnection conn) {
		List list = new ArrayList();
        RuleParameterInfo props = new RuleParameterInfo(
                PARAM_WRKS_PROPERTY, RuleParameterInfo.CHOICE_TYPE, true);
        Iterator iter = ELIGIBLE_PROPS.iterator();
        while (iter.hasNext()) {
            String item = (String)iter.next();
            props.addChoice(item);
        }   
        list.add(props);    
        list.add(new RuleParameterInfo(PARAM_WRKS_VALUE,
				RuleParameterInfo.STRING_TYPE));
		return list;

	}

	public void execute(WBData wbData, Parameters parameters) throws Exception {
		String property = parameters.getParameter(PARAM_WRKS_PROPERTY);
		if (!ELIGIBLE_PROPS.contains(property)) {
		    throw new RuleEngineException("Wrks Property must be one of " + ELIGIBLE_PROPS);
		}
		String val = parameters.getParameter(PARAM_WRKS_VALUE);		
		wbData.getRuleData().getWorkSummary().setProperty(property, val);
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  {logger.debug("Set " + property + " to :" + val); }		
	}

	public String getComponentName() {
		return "WBIAG: SetWorkSummaryProperty Rule";
	}

}
