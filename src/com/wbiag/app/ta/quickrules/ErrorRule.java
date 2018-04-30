package com.wbiag.app.ta.quickrules;

import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;

/**
 * Simple Error Rule
 */
public class ErrorRule extends Rule {

	private static final Logger logger = Logger.getLogger(ErrorRule.class);

	public static final String PARAM_ERROR_MESSAGE = "ErrorMessage";
	public static final String PARAM_STOP_EXECUTION = "StopExecution";

	public List getParameterInfo(DBConnection conn) {
		List list = new ArrayList();
		list.add(new RuleParameterInfo(PARAM_ERROR_MESSAGE,
				RuleParameterInfo.STRING_TYPE));
        list.add(new RuleParameterInfo(PARAM_STOP_EXECUTION,
				RuleParameterInfo.STRING_TYPE));
		return list;

	}

	public void execute(WBData wbData, Parameters parameters) throws Exception {
		String errorMessage = parameters.getParameter(PARAM_ERROR_MESSAGE);
        boolean stop = Boolean.valueOf(parameters.getParameter(PARAM_STOP_EXECUTION,
            Boolean.FALSE.toString())).booleanValue();
        if (stop) {
            throw new RuleEngineException(errorMessage);
        }
        else {
            wbData.getRuleData().getWorkSummary().error(errorMessage);
        }
	}

	public String getComponentName() {
		return "WBIAG: Error Rule";
	}

}
