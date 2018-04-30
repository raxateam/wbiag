package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;

/**
 * UnauthorizeRule to be used in conjunction with conditions that core rule doesn't support
 *
 */
public class UnauthorizeRule extends Rule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(UnauthorizeRule.class);

    public static final String PARAM_UNAUTHORIZE_MESSAGE = "UnauthorizeMessage";
    public static final String PARAM_VAL_DEFAULT_UNAUTHORIZE_MESSAGE = "UNAUTHORIZE_RULE";

    public List getParameterInfo (DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_UNAUTHORIZE_MESSAGE, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }


    public void execute(WBData wbData, Parameters parameters) throws Exception {

        String unauthMsg = parameters.getParameter(PARAM_UNAUTHORIZE_MESSAGE , PARAM_VAL_DEFAULT_UNAUTHORIZE_MESSAGE);

        // **** if already authorized by user, just check processEditAfterAuthorization and return
        if (!"AUTO".equals(wbData.getRuleData().getWorkSummary().getWrksAuthBy())) {
            return;
        }
        WorkSummaryData summary = wbData.getRuleData().getWorkSummary();
        summary.setWrksAuthorized("N");
        summary.setWrksAuthBy("AUTO");
        summary.setWrksMessages(unauthMsg);

    }

    public String getComponentName() {
        return "WBIAG : Unauthorize Rule";
    }



}

