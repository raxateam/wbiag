package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.*;
/**
 *  Title:        Is Work SUmmary Property Generic Condition
 *  Description:  Parses the ExpressionString and checks whether the work sumary
 *                satifies the conditions in ExpressionString
 *                ExpressionString is comma delimited quote enclosed string of work summary table
 *                attributes where each condition is ANDed.
 *                "wrksFlag1=Y","wrksUdf1!=XX",
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class IsWorkSummaryPropertyGenericCondition extends Condition {
    //private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsWorkSummaryPropertyGenericCondition.class);

    public static final String PARAM_EXPRESSION_STRING = "ExpressionString";
    public static final String IS_EMPTY = "[IS_EMPTY]";

    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {

        String expressionString = parameters.getParameter(PARAM_EXPRESSION_STRING);
        boolean expr = wbData.getRuleData().getWorkSummary().evaluateExpression(expressionString);
        return  expr;
    }

    public List getParameterInfo( DBConnection conn ) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_EXPRESSION_STRING, RuleParameterInfo.STRING_TYPE, false));
        return result;
    }

    public String getComponentName() {
        return "WBIAG: Is Work Summary Property Generic Condition";
    }

}
